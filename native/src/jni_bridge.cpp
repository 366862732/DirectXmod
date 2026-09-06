// JNI 桥接层：导出 Java com.xgdt.dx12.dx12.Dx12Native 的 native 方法。
// 符号名必须与 Java 类/方法完全匹配（包 com.xgdt.dx12.dx12，类 Dx12Native）。

#include <jni.h>

#include <cstdio>
#include <string>

#include "dx12_device.h"

namespace {

// 句柄（Java long）<-> Dx12Object*
dx12mc::Dx12Object* fromHandle(jlong h) {
    return reinterpret_cast<dx12mc::Dx12Object*>(h);
}

jlong toHandle(dx12mc::Dx12Object* p) {
    return reinterpret_cast<jlong>(p);
}

jstring toJString(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

void throwJava(JNIEnv* env, const std::string& message) {
    jclass ex = env->FindClass("java/lang/IllegalStateException");
    if (ex) env->ThrowNew(ex, message.c_str());
}

}  // namespace

extern "C" {

// P1 探测 + P2 资源层自检：创建 D3D12 device/queue/heaps，
// 自检 texture/buffer/sampler/view 后返回汇总信息。
JNIEXPORT jstring JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateDevice(
    JNIEnv* env, jclass) {
    std::string err;
    if (!dx12mc::ensureDevice(err)) {
        return toJString(env, "ERROR: " + err);
    }
    // P15: 读取 DX12_LOG_VERBOSE 环境变量控制日志级别
    const char* verbose = std::getenv("DX12_LOG_VERBOSE");
    dx12mc::setLogLevel(verbose && *verbose ? 3 : 1);
    dx12mc::dbgLog("dx12: log level=%d (0=ERR 1=WARN 2=INFO 3=DEBUG), DX12_LOG_VERBOSE=%s",
        verbose && *verbose ? 3 : 1, verbose ? verbose : "off");
    auto& ctx = dx12mc::deviceContextForJni();
    int level = ctx.featureLevel & 0xffff;
    char levelName[64];
    snprintf(levelName, sizeof(levelName), "D3D_FEATURE_LEVEL_%d_%d", level / 10, level % 10);
    std::string result = ctx.adapterName.empty() ? "<unknown adapter>" : ctx.adapterName;
    result += " (";
    result += levelName;
    result += "); ";
    result += dx12mc::runResourceSelfTest();
    return toJString(env, result);
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateTexture(
    JNIEnv* env, jclass, jint usage, jint format, jint width, jint height,
    jint depthOrLayers, jint mipLevels) {
    std::string err;
    dx12mc::Dx12Object* obj =
        dx12mc::createTexture(usage, format, width, height, depthOrLayers, mipLevels, err);
    if (!obj) { throwJava(env, "dx12CreateTexture: " + err); return 0; }
    return toHandle(obj);
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateBuffer(
    JNIEnv* env, jclass, jint usage, jlong size) {
    std::string err;
    dx12mc::Dx12Object* obj = dx12mc::createBuffer(usage, size, err);
    if (!obj) { throwJava(env, "dx12CreateBuffer: " + err); return 0; }
    return toHandle(obj);
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateSampler(
    JNIEnv* env, jclass, jint addressU, jint addressV, jint minFilter,
    jint magFilter, jint maxAnisotropy, jfloat maxLod) {
    std::string err;
    dx12mc::Dx12Object* obj = dx12mc::createSampler(
        addressU, addressV, minFilter, magFilter, maxAnisotropy, maxLod, err);
    if (!obj) { throwJava(env, "dx12CreateSampler: " + err); return 0; }
    return toHandle(obj);
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateTextureView(
    JNIEnv* env, jclass, jlong texture, jint baseMipLevel, jint mipLevels) {
    std::string err;
    dx12mc::Dx12Object* obj = dx12mc::createTextureView(
        fromHandle(texture), baseMipLevel, mipLevels, err);
    if (!obj) { throwJava(env, "dx12CreateTextureView: " + err); return 0; }
    return toHandle(obj);
}

JNIEXPORT void JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12DestroyResource(
    JNIEnv*, jclass, jlong handle) {
    dx12mc::destroyObject(fromHandle(handle));
}

JNIEXPORT jobject JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12MapBuffer(
    JNIEnv* env, jclass, jlong buffer, jlong offset, jlong length,
    jboolean read, jboolean write) {
    std::string err;
    void* ptr = dx12mc::mapBuffer(fromHandle(buffer), offset, length, read, write, err);
    if (!ptr) { throwJava(env, "dx12MapBuffer: " + err); return nullptr; }
    return env->NewDirectByteBuffer(ptr, (jlong)length);
}

JNIEXPORT void JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12UnmapBuffer(
    JNIEnv*, jclass, jlong buffer) {
    dx12mc::unmapBuffer(fromHandle(buffer));
}

JNIEXPORT jstring JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12EnumerateAdapters(
    JNIEnv* env, jclass) {
    return toJString(env, dx12mc::enumerateAdaptersJson());
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12GetQueueHandle(
    JNIEnv*, jclass) {
    return static_cast<jlong>(dx12mc::getQueueHandle());
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12GetDeviceHandle(
    JNIEnv*, jclass) {
    return static_cast<jlong>(dx12mc::getDeviceHandle());
}

JNIEXPORT jlong JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12CreateHiddenWindow(
    JNIEnv*, jclass, jint width, jint height) {
    return static_cast<jlong>(dx12mc::createHiddenWindow(width, height));
}

JNIEXPORT void JNICALL Java_com_xgdt_dx12_dx12_Dx12Native_dx12DestroyHiddenWindow(
    JNIEnv*, jclass, jlong hwnd) {
    dx12mc::destroyHiddenWindow(static_cast<uintptr_t>(hwnd));
}

}  // extern "C"
