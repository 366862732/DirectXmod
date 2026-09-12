// JNI 桥（P33：异步渲染基础设施）。
// 与 Java 侧 com.xgdt.dx12.dx12.Dx12Native 的 native 声明一一对应。
// 独立文件，避免与其它 jni_bridge_p*.cpp 冲突。
//
// ===========================================================================
// Java 侧需新增的 Dx12Native 声明清单（方法名 + JNI 签名）
//   包名/类：com.xgdt.dx12.dx12.Dx12Native
// ---------------------------------------------------------------------------
//   // ---- 分区描述符分配器（drawHeap 分区）----
//   static native long    dx12AsyncDescriptorCreate(int workerCount);
//   static native void    dx12AsyncDescriptorDestroy(long alloc);
//   static native int     dx12AsyncDescriptorRegionBase(long alloc, int frameSlot, int worker);
//   static native int     dx12AsyncDescriptorAllocate(long alloc, int frameSlot, int worker, int count);
//   static native void    dx12AsyncDescriptorResetFrame(long alloc, int frameSlot);
//   static native long    dx12AsyncDescriptorGpuHandle(long alloc, int slot);
//   static native boolean dx12AsyncDescriptorWriteCBV(long alloc, int slot, long buffer, long offset, long size);
//   static native boolean dx12AsyncDescriptorWriteSRV(long alloc, int slot, long view);
//
//   // ---- Bundle 录制器池 ----
//   static native long    dx12AsyncBundlePoolCreate(int workerCount);
//   static native void    dx12AsyncBundlePoolDestroy(long pool);
//   static native boolean dx12AsyncBundleBegin(long pool, int worker, int frameSlot);
//   static native long    dx12AsyncBundleEnd(long pool, int worker);            // 返回 bundle 命令列表句柄
//   static native boolean dx12AsyncBundleSetPipelineState(long pool, int worker, long pipeline, boolean hasDepth);
//   static native boolean dx12AsyncBundleSetDescriptorTable(long pool, int worker, int slot, long gpuHandle);
//   static native boolean dx12AsyncBundleSetVertexBuffer(long pool, int worker, int slot, long buffer, long offset, int stride);
//   static native boolean dx12AsyncBundleSetIndexBuffer(long pool, int worker, long buffer, int indexType);
//   static native boolean dx12AsyncBundleDrawIndexed(long pool, int worker, int indexCount, int instanceCount, int firstIndex, int baseVertex, int firstInstance);
//   static native boolean dx12AsyncBundleDraw(long pool, int worker, int vertexCount, int instanceCount, int firstVertex, int firstInstance);
//   static native boolean dx12AsyncBundleDrawIndexedIndirect(long pool, int worker, long commands, long offset, int drawCount);
//
//   // ---- Fence 管理器（SetEventOnCompletion + 等待线程，非轮询）----
//   static native long    dx12AsyncFenceCreate();
//   static native void    dx12AsyncFenceDestroy(long mgr);
//   static native boolean dx12AsyncFenceSignal(long mgr, long value);
//   static native boolean dx12AsyncFenceWait(long mgr, long value, long timeoutMs);
//   static native boolean dx12AsyncFenceIsComplete(long mgr, long value);
//   static native boolean dx12AsyncFenceRegisterCallback(long mgr, long value, Dx12FenceCallback callback, long timeoutMs);
//
//   // ---- 在既有（同步）命令列表上执行 bundle ----
//   static native boolean dx12ExecuteBundle(long ctx, long bundle);
//   static native boolean dx12AsyncPrepareCBVBuffers(long ctx, long[] buffers);
//
//   // ---- 主 Command List 执行器 ----
//   static native long    dx12AsyncExecutorCreate();
//   static native void    dx12AsyncExecutorDestroy(long exec);
//   static native boolean dx12AsyncExecutorTryBeginFrame(long exec);
//   static native boolean dx12AsyncExecutorAddBundle(long exec, long bundle);
//   static native boolean dx12AsyncExecutorAddTransition(long exec, long object, int stateBefore, int stateAfter);
//   static native long    dx12AsyncExecutorEndFrame(long exec);
//   static native long    dx12AsyncExecutorLastFenceValue(long exec);
//   static native boolean dx12AsyncExecutorIsFrameInProgress(long exec);
//   static native boolean dx12AsyncExecutorWaitFrame(long exec, long fenceValue, long timeoutMs);
//
//   回调接口（新增）：
//     package com.xgdt.dx12.dx12;
//     public interface Dx12FenceCallback {
//         void onFenceComplete(long fenceValue, boolean success);
//     }
// ===========================================================================

#include <jni.h>

#include <atomic>
#include <cstdint>
#include <cstdio>
#include <vector>

#include "dx12_async_bundle.h"
#include "dx12_async_descriptor.h"
#include "dx12_async_executor.h"
#include "dx12_async_fence.h"
#include "dx12_device.h"

using namespace dx12mc;

namespace {

template <typename T>
T* toPtr(jlong handle) {
    return reinterpret_cast<T*>(static_cast<uintptr_t>(handle));
}

void logFail(const char* what) {
    std::fprintf(stderr, "[dx12] %s failed\n", what);
}

// ---- Java 回调调用（监控线程通过 AttachCurrentThread 回到 JVM）----

JavaVM* g_jvm = nullptr;

bool ensureJvm(JNIEnv* env) {
    if (g_jvm) return true;
    if (!env) return false;
    return env->GetJavaVM(&g_jvm) == JNI_OK;
}

struct JavaCallbackCtx {
    jobject globalRef = nullptr;
    std::atomic<bool> fired{false};
};

void invokeJavaFenceCallback(JavaCallbackCtx* ctx, jlong fenceValue, bool success) {
    if (!ctx) return;
    if (ctx->fired.exchange(true)) return;
    if (!g_jvm || !ctx->globalRef) {
        delete ctx;
        return;
    }

    JNIEnv* env = nullptr;
    bool attached = false;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr) != JNI_OK) {
            delete ctx;
            return;
        }
        attached = true;
    }

    if (env) {
        jclass cls = env->GetObjectClass(ctx->globalRef);
        jmethodID mid = cls ? env->GetMethodID(cls, "onFenceComplete", "(JZ)V") : nullptr;
        if (mid) {
            env->CallVoidMethod(ctx->globalRef, mid, fenceValue,
                success ? JNI_TRUE : JNI_FALSE);
        }
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (cls) env->DeleteLocalRef(cls);
        env->DeleteGlobalRef(ctx->globalRef);
    }
    if (attached) g_jvm->DetachCurrentThread();
    delete ctx;
}

}  // namespace

extern "C" {

// ===========================================================================
// 分区描述符分配器
// ===========================================================================

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorCreate(
    JNIEnv*, jclass, jint workerCount) {
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device || !dc.drawHeap) {
        logFail("dx12AsyncDescriptorCreate: device/drawHeap not ready");
        return 0;
    }
    D3D12_DESCRIPTOR_HEAP_DESC desc = dc.drawHeap->GetDesc();
    auto* alloc = new PartitionedDescriptorAllocator();
    if (!alloc->init(dc.device.Get(), dc.drawHeap.Get(), desc.NumDescriptors,
            (UINT)workerCount, kAsyncSyncRingReserve)) {
        logFail("dx12AsyncDescriptorCreate: init");
        delete alloc;
        return 0;
    }
    // P33：通知同步路径「本帧槽段内属于同步 ring 的槽位数只有 kAsyncSyncRingReserve」，
    // 使 pushDescriptors/blitBindSourceTexture 的容量检查与 worker 分区对齐，避免
    // 同步 ring 越界静默覆盖 worker 描述符。与 alloc->init 的 reservedPerSection 一致。
    dc.syncRingReserve = kAsyncSyncRingReserve;
    return (jlong)(uintptr_t)alloc;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorDestroy(
    JNIEnv*, jclass, jlong alloc) {
    if (alloc != 0) {
        // P33：异步分区下线，同步路径恢复可用整段 drawHeap。
        deviceContextForJni().syncRingReserve = 0;
    }
    delete toPtr<PartitionedDescriptorAllocator>(alloc);
}

JNIEXPORT jint JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorRegionBase(
    JNIEnv*, jclass, jlong alloc, jint frameSlot, jint worker) {
    PartitionedDescriptorAllocator* a = toPtr<PartitionedDescriptorAllocator>(alloc);
    if (!a) return -1;
    UINT base = a->regionBaseSlot((UINT)frameSlot, (UINT)worker);
    return base == UINT_MAX ? (jint)-1 : (jint)base;
}

JNIEXPORT jint JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorAllocate(
    JNIEnv*, jclass, jlong alloc, jint frameSlot, jint worker, jint count) {
    PartitionedDescriptorAllocator* a = toPtr<PartitionedDescriptorAllocator>(alloc);
    if (!a || count <= 0) return -1;
    UINT slot = a->allocate((UINT)frameSlot, (UINT)worker, (UINT)count);
    return slot == UINT_MAX ? (jint)-1 : (jint)slot;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorResetFrame(
    JNIEnv*, jclass, jlong alloc, jint frameSlot) {
    PartitionedDescriptorAllocator* a = toPtr<PartitionedDescriptorAllocator>(alloc);
    if (a) a->resetFrame((UINT)frameSlot);
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorGpuHandle(
    JNIEnv*, jclass, jlong alloc, jint slot) {
    PartitionedDescriptorAllocator* a = toPtr<PartitionedDescriptorAllocator>(alloc);
    if (!a || slot < 0) return 0;
    return (jlong)a->gpuHandle((UINT)slot).ptr;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorWriteCBV(
    JNIEnv*, jclass, jlong alloc, jint slot, jlong buffer, jlong offset, jlong size) {
    PartitionedDescriptorAllocator* a = toPtr<PartitionedDescriptorAllocator>(alloc);
    Dx12Object* buf = toPtr<Dx12Object>(buffer);
    if (!a || !buf || buf->kind != Dx12Object::Kind::Buffer || !buf->resource) {
        logFail("dx12AsyncDescriptorWriteCBV: invalid args");
        return JNI_FALSE;
    }
    if (slot < 0 || offset < 0 || size < 0) return JNI_FALSE;
    if (!a->writeCBV((UINT)slot, buf->resource.Get(), (UINT64)offset, (UINT64)size)) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncDescriptorWriteSRV(
    JNIEnv*, jclass, jlong alloc, jint slot, jlong view) {
    PartitionedDescriptorAllocator* a = toPtr<PartitionedDescriptorAllocator>(alloc);
    Dx12Object* v = toPtr<Dx12Object>(view);
    if (!a || !v || v->cpuHandle.ptr == 0 || slot < 0) {
        logFail("dx12AsyncDescriptorWriteSRV: invalid args");
        return JNI_FALSE;
    }
    return a->writeDescriptor((UINT)slot, v->cpuHandle) ? JNI_TRUE : JNI_FALSE;
}

// ===========================================================================
// Bundle 录制器池
// ===========================================================================

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundlePoolCreate(
    JNIEnv*, jclass, jint workerCount) {
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device) {
        logFail("dx12AsyncBundlePoolCreate: device not ready");
        return 0;
    }
    auto* pool = new BundleRecorderPool();
    if (!pool->init(dc.device.Get(), (UINT)workerCount)) {
        logFail("dx12AsyncBundlePoolCreate: init");
        delete pool;
        return 0;
    }
    return (jlong)(uintptr_t)pool;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundlePoolDestroy(
    JNIEnv*, jclass, jlong pool) {
    delete toPtr<BundleRecorderPool>(pool);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleBegin(
    JNIEnv*, jclass, jlong pool, jint worker, jint frameSlot) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    if (!r || frameSlot < 0 || !r->begin((UINT)frameSlot)) {
        logFail("dx12AsyncBundleBegin");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleEnd(
    JNIEnv*, jclass, jlong pool, jint worker) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    if (!r) return 0;
    ID3D12GraphicsCommandList* bundle = r->end();
    return (jlong)(uintptr_t)bundle;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleSetPipelineState(
    JNIEnv*, jclass, jlong pool, jint worker, jlong pipeline, jboolean hasDepth) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    Dx12Pipeline* pl = toPtr<Dx12Pipeline>(pipeline);
    if (!r || !pl) {
        logFail("dx12AsyncBundleSetPipelineState: invalid args");
        return JNI_FALSE;
    }
    ID3D12PipelineState* pso = hasDepth == JNI_TRUE ? pl->withDepth.Get()
                                                     : pl->withoutDepth.Get();
    if (!pso) pso = pl->withDepth.Get();
    if (!pso || !pl->rootSignature) return JNI_FALSE;
    r->setRootSignature(pl->rootSignature.Get());
    r->setPipelineState(pso);
    r->setPrimitiveTopology(bundlePrimitiveTopology(pl->topology));
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleSetDescriptorTable(
    JNIEnv*, jclass, jlong pool, jint worker, jint slot, jlong gpuHandle) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    if (!r) {
        logFail("dx12AsyncBundleSetDescriptorTable: invalid recorder");
        return JNI_FALSE;
    }
    D3D12_GPU_DESCRIPTOR_HANDLE h{};
    h.ptr = (UINT64)gpuHandle;
    r->setDescriptorTable((UINT)slot, h);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleSetVertexBuffer(
    JNIEnv*, jclass, jlong pool, jint worker, jint slot, jlong buffer,
    jlong offset, jint stride) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    Dx12Object* buf = toPtr<Dx12Object>(buffer);
    if (!r || !buf || buf->kind != Dx12Object::Kind::Buffer || !buf->resource
        || slot < 0 || offset < 0 || offset >= buf->size) {
        logFail("dx12AsyncBundleSetVertexBuffer: invalid args");
        return JNI_FALSE;
    }
    D3D12_VERTEX_BUFFER_VIEW vb{};
    vb.BufferLocation = buf->resource->GetGPUVirtualAddress() + (UINT64)offset;
    vb.SizeInBytes = (UINT)(buf->size - offset);
    vb.StrideInBytes = (UINT)stride;
    r->setVertexBuffers((UINT)slot, 1, &vb);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleSetIndexBuffer(
    JNIEnv*, jclass, jlong pool, jint worker, jlong buffer, jint indexType) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    Dx12Object* buf = toPtr<Dx12Object>(buffer);
    if (!r || !buf || buf->kind != Dx12Object::Kind::Buffer || !buf->resource) {
        logFail("dx12AsyncBundleSetIndexBuffer: invalid args");
        return JNI_FALSE;
    }
    D3D12_INDEX_BUFFER_VIEW ib{};
    ib.BufferLocation = buf->resource->GetGPUVirtualAddress();
    ib.SizeInBytes = (UINT)buf->size;
    ib.Format = indexType == 1 ? DXGI_FORMAT_R32_UINT : DXGI_FORMAT_R16_UINT;
    r->setIndexBuffer(&ib);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleDrawIndexed(
    JNIEnv*, jclass, jlong pool, jint worker, jint indexCount, jint instanceCount,
    jint firstIndex, jint baseVertex, jint firstInstance) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    if (!r) return JNI_FALSE;
    r->drawIndexedInstanced((UINT)indexCount, (UINT)instanceCount, (UINT)firstIndex,
        (INT)baseVertex, (UINT)firstInstance);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleDraw(
    JNIEnv*, jclass, jlong pool, jint worker, jint vertexCount, jint instanceCount,
    jint firstVertex, jint firstInstance) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    if (!r) return JNI_FALSE;
    r->drawInstanced((UINT)vertexCount, (UINT)instanceCount, (UINT)firstVertex,
        (UINT)firstInstance);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncBundleDrawIndexedIndirect(
    JNIEnv*, jclass, jlong pool, jint worker, jlong commands, jlong offset,
    jint drawCount) {
    BundleRecorderPool* p = toPtr<BundleRecorderPool>(pool);
    BundleRecorder* r = p ? p->recorder((UINT)worker) : nullptr;
    Dx12Object* cmds = toPtr<Dx12Object>(commands);
    if (!r || !cmds || cmds->kind != Dx12Object::Kind::Buffer || !cmds->resource) {
        logFail("dx12AsyncBundleDrawIndexedIndirect: invalid args");
        return JNI_FALSE;
    }
    if (!r->drawIndexedIndirect(cmds->resource.Get(), (UINT64)offset, (UINT)drawCount)) {
        logFail("dx12AsyncBundleDrawIndexedIndirect: no command signature");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

// ===========================================================================
// Fence 管理器
// ===========================================================================

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncFenceCreate(JNIEnv*, jclass) {
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device || !dc.queue) {
        logFail("dx12AsyncFenceCreate: device/queue not ready");
        return 0;
    }
    auto* mgr = new AsyncFenceManager();
    if (!mgr->init(dc.device.Get(), dc.queue.Get())) {
        logFail("dx12AsyncFenceCreate: init");
        delete mgr;
        return 0;
    }
    return (jlong)(uintptr_t)mgr;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncFenceDestroy(JNIEnv*, jclass, jlong mgr) {
    delete toPtr<AsyncFenceManager>(mgr);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncFenceSignal(
    JNIEnv*, jclass, jlong mgr, jlong value) {
    AsyncFenceManager* m = toPtr<AsyncFenceManager>(mgr);
    if (!m || !m->signal((UINT64)value)) return JNI_FALSE;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncFenceWait(
    JNIEnv*, jclass, jlong mgr, jlong value, jlong timeoutMs) {
    AsyncFenceManager* m = toPtr<AsyncFenceManager>(mgr);
    if (!m) return JNI_FALSE;
    return m->waitFence((UINT64)value, (UINT64)(timeoutMs < 0 ? 0 : timeoutMs))
        ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncFenceIsComplete(
    JNIEnv*, jclass, jlong mgr, jlong value) {
    AsyncFenceManager* m = toPtr<AsyncFenceManager>(mgr);
    if (!m) return JNI_FALSE;
    return m->isFenceComplete((UINT64)value) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncFenceRegisterCallback(
    JNIEnv* env, jclass, jlong mgr, jlong value, jobject callback, jlong timeoutMs) {
    AsyncFenceManager* m = toPtr<AsyncFenceManager>(mgr);
    if (!m || !callback) {
        logFail("dx12AsyncFenceRegisterCallback: invalid args");
        return JNI_FALSE;
    }
    if (!ensureJvm(env)) {
        logFail("dx12AsyncFenceRegisterCallback: GetJavaVM");
        return JNI_FALSE;
    }
    auto* ctx = new JavaCallbackCtx();
    ctx->globalRef = env->NewGlobalRef(callback);
    if (!ctx->globalRef) {
        delete ctx;
        return JNI_FALSE;
    }
    m->registerCallback((UINT64)value,
        [ctx](UINT64 v, bool ok) { invokeJavaFenceCallback(ctx, (jlong)v, ok); },
        (UINT64)(timeoutMs < 0 ? 0 : timeoutMs));
    return JNI_TRUE;
}

// ===========================================================================
// 在既有（同步）命令列表上执行 bundle
//
// 集成方式：主列表仍由 DeviceContext 的 CommandContext 录制（render pass 起止、
// viewport/scissor、clear、copy、barrier 都在主列表上），只有 draw 密集的区段
// 由 worker 并行录制成 bundle，再在这里按顺序 ExecuteBundle 回主列表。
// ===========================================================================

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12ExecuteBundle(
    JNIEnv*, jclass, jlong ctx, jlong bundle) {
    CommandContext* c = toPtr<CommandContext>(ctx);
    auto* b = reinterpret_cast<ID3D12GraphicsCommandList*>(static_cast<uintptr_t>(bundle));
    if (!c || !c->commandList || !c->listOpen || !b) {
        logFail("dx12ExecuteBundle: invalid args");
        return JNI_FALSE;
    }
    c->commandList->ExecuteBundle(b);
    return JNI_TRUE;
}

// 主线程在派发 bundle 之前，把这一批 draw 用到的 CBV 缓冲区在主列表上过渡到
// VERTEX_AND_CONSTANT_BUFFER（bundle 内禁止 ResourceBarrier）。语义与
// pushDescriptors 对 type==0 的处理一致；状态已匹配时 transitionBufferTo 为空操作。
JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncPrepareCBVBuffers(
    JNIEnv* env, jclass, jlong ctx, jlongArray buffers) {
    CommandContext* c = toPtr<CommandContext>(ctx);
    if (!c || !c->listOpen || !buffers) return JNI_FALSE;
    const jsize n = env->GetArrayLength(buffers);
    if (n <= 0) return JNI_TRUE;
    std::vector<jlong> raw((size_t)n);
    env->GetLongArrayRegion(buffers, 0, n, raw.data());
    if (env->ExceptionCheck()) return JNI_FALSE;
    for (jsize i = 0; i < n; ++i) {
        Dx12Object* buf = toPtr<Dx12Object>(raw[(size_t)i]);
        if (!buf || buf->kind != Dx12Object::Kind::Buffer || !buf->resource) continue;
        transitionBufferTo(c, buf, D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER);
    }
    return JNI_TRUE;
}

// ===========================================================================
// 主 Command List 执行器
// ===========================================================================

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorCreate(JNIEnv*, jclass) {
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device || !dc.queue) {
        logFail("dx12AsyncExecutorCreate: device/queue not ready");
        return 0;
    }
    auto* exec = new MainCommandExecutor();
    if (!exec->init(dc.device.Get(), dc.queue.Get())) {
        logFail("dx12AsyncExecutorCreate: init");
        delete exec;
        return 0;
    }
    return (jlong)(uintptr_t)exec;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorDestroy(JNIEnv*, jclass, jlong exec) {
    delete toPtr<MainCommandExecutor>(exec);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorTryBeginFrame(
    JNIEnv*, jclass, jlong exec) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    if (!e) return JNI_FALSE;
    return e->tryBeginFrame() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorAddBundle(
    JNIEnv*, jclass, jlong exec, jlong bundle) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    if (!e) return JNI_FALSE;
    ID3D12GraphicsCommandList* list =
        reinterpret_cast<ID3D12GraphicsCommandList*>(static_cast<uintptr_t>(bundle));
    return e->addBundle(list) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorAddTransition(
    JNIEnv*, jclass, jlong exec, jlong object, jint stateBefore, jint stateAfter) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    Dx12Object* obj = toPtr<Dx12Object>(object);
    if (!e || !obj || !obj->resource) {
        logFail("dx12AsyncExecutorAddTransition: invalid args");
        return JNI_FALSE;
    }
    return e->addTransition(obj->resource.Get(),
        (D3D12_RESOURCE_STATES)stateBefore,
        (D3D12_RESOURCE_STATES)stateAfter) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorEndFrame(JNIEnv*, jclass, jlong exec) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    if (!e) return 0;
    return (jlong)e->endFrame();
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorLastFenceValue(
    JNIEnv*, jclass, jlong exec) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    return e ? (jlong)e->lastFenceValue() : 0;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorIsFrameInProgress(
    JNIEnv*, jclass, jlong exec) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    return (e && e->isFrameInProgress()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx12_dx12_Dx12Native_dx12AsyncExecutorWaitFrame(
    JNIEnv*, jclass, jlong exec, jlong fenceValue, jlong timeoutMs) {
    MainCommandExecutor* e = toPtr<MainCommandExecutor>(exec);
    if (!e) return JNI_FALSE;
    return e->waitForFrame((UINT64)fenceValue,
        (UINT64)(timeoutMs < 0 ? 0 : timeoutMs)) ? JNI_TRUE : JNI_FALSE;
}

}  // extern "C"
