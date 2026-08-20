// JNI 桥接层（P3 命令层）：导出 Java com.dx12.dx12.Dx12Native 的命令层 native 方法。
// 独立翻译单元：jni_bridge.cpp 被 IDE 进程锁定（EBUSY），P3 导出放在此文件。
// 符号名必须与 Java 类/方法完全匹配（包 com.dx12.dx12，类 Dx12Native）。

#include <jni.h>

#include <cstdint>
#include <string>
#include <vector>

#include "dx12_device.h"

namespace {

// 句柄（Java long）<-> Dx12Object*
dx12mc::Dx12Object* fromHandle(jlong h) {
    return reinterpret_cast<dx12mc::Dx12Object*>(h);
}

void throwJava(JNIEnv* env, const std::string& message) {
    jclass ex = env->FindClass("java/lang/IllegalStateException");
    if (ex) env->ThrowNew(ex, message.c_str());
}

}  // namespace

extern "C" {

// ===========================================================================
// P3 命令层：CommandEncoder（对应官方 VulkanCommandEncoder）
// ===========================================================================

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12GetTimestampFrequency(
    JNIEnv*, jclass) {
    return (jlong)dx12mc::getTimestampFrequency();
}

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12CreateCommandEncoder(
    JNIEnv* env, jclass) {
    std::string err;
    dx12mc::CommandContext* ctx = dx12mc::createCommandEncoder(err);
    if (!ctx) { throwJava(env, "dx12CreateCommandEncoder: " + err); return 0; }
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12DestroyCommandEncoder(
    JNIEnv*, jclass, jlong ctx) {
    dx12mc::destroyCommandEncoder(reinterpret_cast<dx12mc::CommandContext*>(ctx));
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12BeginCommandList(
    JNIEnv* env, jclass, jlong ctx) {
    std::string err;
    if (!dx12mc::beginCommandList(
            reinterpret_cast<dx12mc::CommandContext*>(ctx), err)) {
        throwJava(env, "dx12BeginCommandList: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12EndCommandList(
    JNIEnv* env, jclass, jlong ctx) {
    std::string err;
    if (!dx12mc::endCommandList(
            reinterpret_cast<dx12mc::CommandContext*>(ctx), err)) {
        throwJava(env, "dx12EndCommandList: " + err);
    }
}

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12Submit(
    JNIEnv* env, jclass, jlong ctx) {
    dx12mc::dbgLog("submit: JNI enter ctx=%p", (void*)ctx);
    std::string err;
    UINT64 value = dx12mc::submitCommandList(
        reinterpret_cast<dx12mc::CommandContext*>(ctx), err);
    if (value == 0 && !err.empty()) { throwJava(env, "dx12Submit: " + err); }
    DBG_LOG_DEBUG("submit: JNI done value=%llu", (unsigned long long)value);
    return (jlong)value;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12WaitForFence(
    JNIEnv* env, jclass, jlong ctx, jlong value, jlong timeoutNs) {
    std::string err;
    // createFence token 的 awaitCompletion：等待设备级队列 fence（目标值 =
    // 创建时 queueFenceValue+1，下一次任意 ctx 的提交完成），而非 per-ctx
    // fence——一次性 encoder 从不 submit，per-ctx 等待永不完成（黑屏冻结根因，
    // 日志表现为连续 waitFence: value=1 completed=0）。
    bool ok = dx12mc::waitForQueueFenceValue((UINT64)value, (UINT64)timeoutNs, err);
    // fence 等待的“超时”是正常语义（官方 GpuFence.awaitCompletion 非阻塞
    // 轮询 timeout=0 时返回 false 而非抛异常，StagedVertexBuffer 回收 buffer
    // 依赖这一点）；只有真正的错误（queue fence 未初始化 / CreateEvent 失败）
    // 才抛异常。
    if (!ok && !err.empty() &&
        err.find("timed out") == std::string::npos) {
        throwJava(env, "dx12WaitForFence: " + err);
    }
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12GetFenceValue(
    JNIEnv*, jclass, jlong) {
    // createFence 捕获全局队列 fence 值（目标 = 当前值 + 1），对应官方共享
    // encoder 的 currentSubmitIndex——一次性 encoder 的 token 也随下一次提交完成。
    return (jlong)dx12mc::currentQueueFenceValue();
}

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12GetTimestampNow(
    JNIEnv* env, jclass, jlong ctx) {
    std::string err;
    long long ts = dx12mc::getTimestampNow(
        reinterpret_cast<dx12mc::CommandContext*>(ctx), err);
    if (ts == 0 && !err.empty()) { throwJava(env, "dx12GetTimestampNow: " + err); }
    return (jlong)ts;
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12CopyBuffer(
    JNIEnv* env, jclass, jlong ctx, jlong src, jlong srcOffset,
    jlong dst, jlong dstOffset, jlong size) {
    DBG_LOG_DEBUG("copyBuffer: src=%p(%lld) dst=%p(%lld) size=%lld",
        (void*)src, (long long)srcOffset, (void*)dst, (long long)dstOffset,
        (long long)size);
    std::string err;
    if (!dx12mc::copyBufferToBuffer(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(src), srcOffset, fromHandle(dst), dstOffset, size, err)) {
        throwJava(env, "dx12CopyBuffer: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12WriteToTexture(
    JNIEnv* env, jclass, jlong ctx, jlong stagingBuf, jlong stagingOffset,
    jint width, jint height, jlong dstTex, jint mip, jint layer,
    jint dstX, jint dstY) {
    std::string err;
    if (!dx12mc::copyBufferToTexture(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(stagingBuf), stagingOffset, width, height,
            fromHandle(dstTex), mip, layer, dstX, dstY, width, height, err)) {
        throwJava(env, "dx12WriteToTexture: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12CopyBufferToTexture(
    JNIEnv* env, jclass, jlong ctx, jlong srcBuf, jlong srcOffset,
    jint srcWidth, jint srcHeight, jlong dstTex, jint mip, jint layer,
    jint dstX, jint dstY, jint w, jint h) {
    std::string err;
    if (!dx12mc::copyBufferToTexture(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(srcBuf), srcOffset, srcWidth, srcHeight,
            fromHandle(dstTex), mip, layer, dstX, dstY, w, h, err)) {
        throwJava(env, "dx12CopyBufferToTexture: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12CopyTextureToBuffer(
    JNIEnv* env, jclass, jlong ctx, jlong srcTex, jint mip, jint layer,
    jint srcX, jint srcY, jint w, jint h, jlong dstBuf, jlong dstOffset) {
    std::string err;
    if (!dx12mc::copyTextureToBuffer(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(srcTex), mip, layer, srcX, srcY, w, h,
            fromHandle(dstBuf), dstOffset, err)) {
        throwJava(env, "dx12CopyTextureToBuffer: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12CopyTextureToTexture(
    JNIEnv* env, jclass, jlong ctx, jlong srcTex, jlong dstTex,
    jint mip, jint layer, jint srcX, jint srcY, jint dstX, jint dstY,
    jint w, jint h) {
    std::string err;
    if (!dx12mc::copyTextureToTexture(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(srcTex), fromHandle(dstTex), mip, layer,
            srcX, srcY, dstX, dstY, w, h, err)) {
        throwJava(env, "dx12CopyTextureToTexture: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12ClearColorTexture(
    JNIEnv* env, jclass, jlong ctx, jlong texture,
    jfloat r, jfloat g, jfloat b, jfloat a) {
    std::string err;
    if (!dx12mc::clearColorTexture(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(texture), r, g, b, a, err)) {
        throwJava(env, "dx12ClearColorTexture: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12ClearDepthTexture(
    JNIEnv* env, jclass, jlong ctx, jlong texture, jdouble depth) {
    std::string err;
    if (!dx12mc::clearDepthTexture(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            fromHandle(texture), depth, err)) {
        throwJava(env, "dx12ClearDepthTexture: " + err);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12BeginRenderPass(
    JNIEnv* env, jclass, jlong ctx, jlongArray colorTextures,
    jbyteArray colorClearFlags, jfloatArray clearColors, jlong depthTexture,
    jbyte depthClearFlag, jdouble depthClearValue,
    jint x, jint y, jint w, jint h) {
    std::string err;
    jsize colorCount = colorTextures ? env->GetArrayLength(colorTextures) : 0;
    // 允许 colorCount==0（纯深度 pass）与 null 附件占位。
    std::vector<jlong> texBuf;
    std::vector<jbyte> flagBuf;
    std::vector<jfloat> colorBuf;
    dx12mc::Dx12Object** views = nullptr;
    int* flags = nullptr;
    float* colors = nullptr;
    if (colorCount > 0) {
        texBuf.resize(colorCount);
        flagBuf.resize(colorCount);
        env->GetLongArrayRegion(colorTextures, 0, colorCount, texBuf.data());
        env->GetByteArrayRegion(colorClearFlags, 0, colorCount, flagBuf.data());
        views = new dx12mc::Dx12Object*[colorCount];
        flags = new int[colorCount];
        for (jsize i = 0; i < colorCount; ++i) {
            views[i] = texBuf[i] ? fromHandle(texBuf[i]) : nullptr;
            flags[i] = flagBuf[i];
        }
        if (clearColors) {
            jsize len = env->GetArrayLength(clearColors);
            if (len >= colorCount * 4) {
                colorBuf.resize(colorCount * 4);
                env->GetFloatArrayRegion(clearColors, 0, colorCount * 4, colorBuf.data());
                colors = colorBuf.data();
            }
        }
    }
    bool ok = dx12mc::beginRenderPass(
        reinterpret_cast<dx12mc::CommandContext*>(ctx), views, (int)colorCount,
        flags, colors, depthTexture ? fromHandle(depthTexture) : nullptr,
        depthClearFlag, depthClearValue, x, y, w, h, err);
    delete[] views;
    delete[] flags;
    if (!ok) { throwJava(env, "dx12BeginRenderPass: " + err); }
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12EndRenderPass(
    JNIEnv* env, jclass, jlong ctx) {
    std::string err;
    if (!dx12mc::endRenderPass(
            reinterpret_cast<dx12mc::CommandContext*>(ctx), err)) {
        throwJava(env, "dx12EndRenderPass: " + err);
    }
}

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12GetActiveColorTexture(
    JNIEnv*, jclass, jlong ctx) {
    return (jlong)getActiveColorTextureHandle(
        reinterpret_cast<dx12mc::CommandContext*>(ctx));
}

// ---------------------------------------------------------------------------
// P3：timestamp query pool
// ---------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12CreateQueryPool(
    JNIEnv* env, jclass, jint size) {
    std::string err;
    dx12mc::QueryPool* pool = dx12mc::createQueryPool(size, err);
    if (!pool) { throwJava(env, "dx12CreateQueryPool: " + err); return 0; }
    return reinterpret_cast<jlong>(pool);
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12DestroyQueryPool(
    JNIEnv*, jclass, jlong pool) {
    dx12mc::destroyQueryPool(reinterpret_cast<dx12mc::QueryPool*>(pool));
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12WriteTimestamp(
    JNIEnv* env, jclass, jlong ctx, jlong pool, jint index) {
    std::string err;
    if (!dx12mc::writeTimestampToPool(
            reinterpret_cast<dx12mc::CommandContext*>(ctx),
            reinterpret_cast<dx12mc::QueryPool*>(pool), index, err)) {
        throwJava(env, "dx12WriteTimestamp: " + err);
    }
}

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12ReadQueryValue(
    JNIEnv* env, jclass, jlong pool, jint index) {
    std::string err;
    long long out = 0;
    if (!dx12mc::readQueryValue(
            reinterpret_cast<dx12mc::QueryPool*>(pool), index, out, err)) {
        throwJava(env, "dx12ReadQueryValue: " + err); return 0;
    }
    return (jlong)out;
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12ReadQueryValues(
    JNIEnv* env, jclass, jlong pool, jint start, jint count, jlongArray out) {
    std::string err;
    std::vector<long long> buf(count);
    if (!dx12mc::readQueryValues(
            reinterpret_cast<dx12mc::QueryPool*>(pool), start, count,
            buf.data(), err)) {
        throwJava(env, "dx12ReadQueryValues: " + err); return;
    }
    env->SetLongArrayRegion(out, 0, count, (const jlong*)buf.data());
}

}  // extern "C"
