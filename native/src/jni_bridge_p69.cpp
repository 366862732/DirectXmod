// JNI 桥（P69：compute 管线 / dispatch）。
// 与 Java 侧 com.xgdt.dx12.dx12.Dx12Native 的 native 声明一一对应。

#include <jni.h>

#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

#include "dx12_device.h"

using namespace dx12mc;

namespace {

CommandContext* toCtx(jlong handle) {
    return reinterpret_cast<CommandContext*>(static_cast<uintptr_t>(handle));
}

Dx12Object* toObject(jlong handle) {
    return reinterpret_cast<Dx12Object*>(static_cast<uintptr_t>(handle));
}

Dx12ComputePipeline* toCompute(jlong handle) {
    return reinterpret_cast<Dx12ComputePipeline*>(static_cast<uintptr_t>(handle));
}

void logFail(const char* what, const std::string& err) {
    std::fprintf(stderr, "[dx12] %s: %s\n", what, err.c_str());
}

// 把 Java 平铺的 4 个平行数组（buffer / offset / count / stride）转成绑定数组。
std::vector<ComputeBinding> readBindings(JNIEnv* env, jlongArray buffers,
    jintArray offsets, jintArray counts, jintArray strides) {
    std::vector<ComputeBinding> out;
    if (!buffers || !offsets || !counts || !strides) return out;
    const jsize n = env->GetArrayLength(buffers);
    if (n <= 0) return out;
    if (env->GetArrayLength(offsets) < n || env->GetArrayLength(counts) < n ||
        env->GetArrayLength(strides) < n) {
        return out;
    }
    std::vector<jlong> h(n);
    std::vector<jint> o(n), c(n), s(n);
    env->GetLongArrayRegion(buffers, 0, n, h.data());
    env->GetIntArrayRegion(offsets, 0, n, o.data());
    env->GetIntArrayRegion(counts, 0, n, c.data());
    env->GetIntArrayRegion(strides, 0, n, s.data());
    out.resize((size_t)n);
    for (jsize i = 0; i < n; ++i) {
        out[i].buffer = toObject(h[i]);
        out[i].offset = o[i];
        out[i].count = c[i];
        out[i].stride = s[i];
    }
    return out;
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateComputePipeline(
    JNIEnv* env, jclass, jbyteArray cs, jint srvCount, jint uavCount, jint constCount) {
    if (!cs) {
        std::fprintf(stderr, "[dx12] dx12CreateComputePipeline: null source\n");
        return 0;
    }
    const jsize n = env->GetArrayLength(cs);
    std::vector<jbyte> src((size_t)n);
    env->GetByteArrayRegion(cs, 0, n, src.data());
    std::string err;
    Dx12ComputePipeline* p = createComputePipeline(src.data(), (int)n,
        (int)srvCount, (int)uavCount, (int)constCount, err);
    if (!p) {
        logFail("dx12CreateComputePipeline", err);
        // stderr 不会被启动器捕获进日志，这里把原因作为 Java 异常抛出去
        jclass cls = env->FindClass("java/lang/IllegalStateException");
        if (cls) {
            std::string msg = "dx12CreateComputePipeline: " + err;
            env->ThrowNew(cls, msg.c_str());
        }
        return 0;
    }
    return (jlong)(uintptr_t)p;
}

JNIEXPORT void JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12DestroyComputePipeline(
    JNIEnv*, jclass, jlong pipeline) {
    destroyComputePipeline(toCompute(pipeline));
}

// 返回 null 表示成功，否则为错误描述（Java 侧据此打印/兜底）。
JNIEXPORT jstring JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12DispatchCompute(
    JNIEnv* env, jclass, jlong ctx, jlong pipeline, jintArray constants,
    jlongArray srvBuffers, jintArray srvOffsets, jintArray srvCounts,
    jintArray srvStrides, jlongArray uavBuffers, jintArray uavOffsets,
    jintArray uavCounts, jintArray uavStrides,
    jint groupX, jint groupY, jint groupZ) {
    std::vector<int> consts;
    if (constants) {
        const jsize cn = env->GetArrayLength(constants);
        consts.resize((size_t)cn);
        if (cn > 0) env->GetIntArrayRegion(constants, 0, cn, consts.data());
    }
    std::vector<ComputeBinding> srvs =
        readBindings(env, srvBuffers, srvOffsets, srvCounts, srvStrides);
    std::vector<ComputeBinding> uavs =
        readBindings(env, uavBuffers, uavOffsets, uavCounts, uavStrides);

    Dx12ComputePipeline* p = toCompute(pipeline);
    if (!p) {
        return env->NewStringUTF("dx12DispatchCompute: null pipeline");
    }
    const int srvCount = (int)srvs.size();
    const int uavCount = (int)uavs.size();
    if (srvCount != p->srvCount || uavCount != p->uavCount) {
        return env->NewStringUTF(
            "dx12DispatchCompute: binding count mismatch");
    }
    std::string err;
    if (!dispatchCompute(toCtx(ctx), p,
            consts.empty() ? nullptr : consts.data(), (int)consts.size(),
            srvs.empty() ? nullptr : srvs.data(), srvCount,
            uavs.empty() ? nullptr : uavs.data(), uavCount,
            (int)groupX, (int)groupY, (int)groupZ, err)) {
        logFail("dx12DispatchCompute", err);
        return env->NewStringUTF(err.c_str());
    }
    return nullptr;
}

}  // extern "C"
