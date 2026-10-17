// P68：世界几何光追的 JNI 入口（对应 Dx12Native 的 dx12Rt* 方法）。
//
// 区块几何在 worker 线程上送（dx12RtUpdateSection），加速结构构建与
// DispatchRays 在渲染线程执行（dx12RtBuildScene / dx12RtDispatch）。

#include <jni.h>

#include <cstdio>
#include <string>

#include "dx12_device.h"
#include "dx12_rt_scene.h"

namespace {

dx12mc::Dx12Object* fromHandle(jlong h) {
    return reinterpret_cast<dx12mc::Dx12Object*>(h);
}

jboolean failQuiet(JNIEnv*, const std::string& what, const std::string& err) {
    dx12mc::dbgLog("%s: %s", what.c_str(), err.c_str());
    return JNI_FALSE;
}

}  // namespace

extern "C" {

// 上传/替换一个 section 的几何。返回 false 表示跳过（几何为空或 DXR 不可用），
// 不抛异常：区块构建线程上抛异常会打断整个批次。
JNIEXPORT jboolean JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12RtUpdateSection(
    JNIEnv* env, jclass, jlong key, jint layer, jint cx, jint cy, jint cz,
    jobject vertexBuffer, jint vertexStride, jint vertexCount,
    jobject indexBuffer, jint indexCount, jboolean index32) {
    if (!vertexBuffer || !indexBuffer) {
        return JNI_FALSE;
    }
    auto* vptr = static_cast<const unsigned char*>(env->GetDirectBufferAddress(vertexBuffer));
    auto* iptr = static_cast<const unsigned char*>(env->GetDirectBufferAddress(indexBuffer));
    jlong vcap = env->GetDirectBufferCapacity(vertexBuffer);
    jlong icap = env->GetDirectBufferCapacity(indexBuffer);
    if (!vptr || !iptr || vcap <= 0 || icap <= 0) {
        return failQuiet(env, "dx12RtUpdateSection",
            "几何 ByteBuffer 不是 direct buffer");
    }

    int vertexBytes = (int)(vertexStride * vertexCount);
    int indexBytes = (int)(indexCount * (index32 ? 4 : 2));
    if (vertexBytes > vcap) vertexBytes = (int)vcap;
    if (indexBytes > icap) indexBytes = (int)icap;

    std::string err;
    if (!dx12mc::rtUpdateSection(key, layer, cx, cy, cz, vptr, vertexBytes, vertexStride,
            vertexCount, iptr, indexBytes, indexCount, index32 != JNI_FALSE, err)) {
        return failQuiet(env, "dx12RtUpdateSection", err);
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12RtRemoveSection(
    JNIEnv*, jclass, jlong key) {
    return dx12mc::rtRemoveSection(key) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12RtClearScene(
    JNIEnv*, jclass) {
    dx12mc::rtResetScene();
}

// 每帧在命令列表录制状态下调用：分帧建 BLAS + 脏时重建 TLAS。
JNIEXPORT jboolean JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12RtBuildScene(
    JNIEnv* env, jclass, jlong ctx) {
    auto* context = reinterpret_cast<dx12mc::CommandContext*>(ctx);
    std::string err;
    if (!dx12mc::rtBuildAccelerationStructures(context, err)) {
        return failQuiet(env, "dx12RtBuildScene", err);
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12RtDispatch(
    JNIEnv* env, jclass, jlong ctx, jlong depthView, jlong outTexture,
    jlong uniformBuffer, jint uniformOffset, jint width, jint height) {
    auto* context = reinterpret_cast<dx12mc::CommandContext*>(ctx);
    std::string err;
    if (!dx12mc::rtDispatch(context, fromHandle(depthView), fromHandle(outTexture),
            fromHandle(uniformBuffer), uniformOffset, width, height, err)) {
        return failQuiet(env, "dx12RtDispatch", err);
    }
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12RtStats(
    JNIEnv* env, jclass) {
    dx12mc::RtStats s = dx12mc::rtGetStats();
    char buf[192];
    snprintf(buf, sizeof(buf),
        "sections=%d built=%d tris=%d tlas=%s",
        s.sections, s.builtSections, s.triangles, s.tlasReady ? "yes" : "no");
    return env->NewStringUTF(buf);
}

}  // extern "C"
