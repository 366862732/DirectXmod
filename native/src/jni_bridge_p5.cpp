// JNI 桥（P5：DXGI swapchain surface）。
// 与 Java 侧 com.dx12.dx12.Dx12Native 的 native 声明一一对应。
// 独立文件避免与 jni_bridge.cpp 冲突（IDE 曾锁定该文件）。

#include <jni.h>

#include <cstdint>
#include <cstdio>
#include <string>
#include <vector>

#include "dx12_device.h"

using namespace dx12mc;

namespace {

Dx12Surface* toSurface(jlong handle) {
    return reinterpret_cast<Dx12Surface*>(static_cast<uintptr_t>(handle));
}

CommandContext* toCtx(jlong handle) {
    return reinterpret_cast<CommandContext*>(static_cast<uintptr_t>(handle));
}

Dx12Object* toObject(jlong handle) {
    return reinterpret_cast<Dx12Object*>(static_cast<uintptr_t>(handle));
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12CreateSurface(
    JNIEnv* env, jclass, jlong hwnd) {
    std::string err;
    Dx12Surface* s = createSurface(static_cast<uintptr_t>(hwnd), err);
    if (!s) {
        std::fprintf(stderr, "[dx12] dx12CreateSurface: %s\n", err.c_str());
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(s));
}

JNIEXPORT jintArray JNICALL Java_com_dx12_dx12_Dx12Native_dx12SurfacePresentModes(
    JNIEnv* env, jclass) {
    std::vector<int> modes = surfacePresentModes();
    jintArray out = env->NewIntArray((jsize)modes.size());
    if (out) {
        env->SetIntArrayRegion(out, 0, (jsize)modes.size(), modes.data());
    }
    return out;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12ConfigureSurface(
    JNIEnv* env, jclass, jlong surface, jint width, jint height, jint presentMode) {
    std::string err;
    if (!configureSurface(toSurface(surface), width, height, presentMode, err)) {
        // 此前仅 fprintf(stderr) -> 不进 dx12-native.log；configure 失败正是
        // MC surfaceIsInvalid -> 画面冻结的根因，必须镜像到 native log。
        dbgLog("dx12ConfigureSurface FAILED %dx%d mode=%d: %s",
            width, height, presentMode, err.c_str());
        return JNI_FALSE;
    }
    dbgLog("dx12ConfigureSurface: ok %dx%d mode=%d", width, height, presentMode);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12AcquireSurface(
    JNIEnv* env, jclass, jlong surface) {
    std::string err;
    if (!acquireSurface(toSurface(surface), err)) {
        std::fprintf(stderr, "[dx12] dx12AcquireSurface: %s\n", err.c_str());
        return JNI_FALSE;
    }
    dbgLog("acquireSurface: ok surface=%p", toSurface(surface));
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12BlitSurface(
    JNIEnv* env, jclass, jlong ctx, jlong surface, jlong texture) {
    std::string err;
    if (!blitSurface(toCtx(ctx), toSurface(surface), toObject(texture), err)) {
        std::fprintf(stderr, "[dx12] dx12BlitSurface: %s\n", err.c_str());
        return;
    }
    dbgLog("blitSurface: ok ctx=%p surface=%p texture=%p",
        toCtx(ctx), toSurface(surface), toObject(texture));
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12PresentSurface(
    JNIEnv*, jclass, jlong surface) {
    presentSurface(toSurface(surface));
    // 注意：present 的真实结果（ok / OCCLUDED / MODE_CHANGED / FAILED）由
    // presentSurface() 内部 dbgLog 打印；此处不再无条件打 "ok"（会掩盖
    // OCCLUDED/MODE_CHANGED 导致 MC 误判 suboptimal 的根因）。
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12IsSurfaceSuboptimal(
    JNIEnv* env, jclass, jlong surface) {
    Dx12Surface* s = toSurface(surface);
    return s ? (s->suboptimal ? JNI_TRUE : JNI_FALSE) : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12DestroySurface(
    JNIEnv*, jclass, jlong surface) {
    destroySurface(toSurface(surface));
}

// P6 诊断：读回当前 back buffer 采样像素（Java 侧每 ~60 帧调用一次）。
JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12ReadbackSurfacePixels(
    JNIEnv* env, jclass, jlong surface) {
    std::string err;
    if (!readbackSurfacePixels(toSurface(surface), err)) {
        std::fprintf(stderr, "[dx12] dx12ReadbackSurfacePixels: %s\n", err.c_str());
    }
}

}  // extern "C"
