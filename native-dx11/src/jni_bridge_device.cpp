// JNI bridge for the persistent D3D11 device layer.
//
// Symbol names must match com.xgdt.dx11.dx11.Dx11Native exactly.
//
// Handles are opaque pointers to the structs in dx11_device.h.  Byte data is
// passed as raw addresses (MemoryUtil.memAddress on the Java side) so this
// file does not have to deal with ByteBuffer positions.

#include <jni.h>

#include "dx11_device.h"

#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

using namespace dx11mc;

namespace {

void throwByName(JNIEnv* env, const char* name, const std::string& message) {
    jclass clazz = env->FindClass(name);
    if (clazz) {
        env->ThrowNew(clazz, message.c_str());
        env->DeleteLocalRef(clazz);
    }
}

void throwIllegalState(JNIEnv* env, const std::string& message) {
    throwByName(env, "java/lang/IllegalStateException", message);
}

template <typename T>
T* fromHandle(jlong handle) {
    return reinterpret_cast<T*>(static_cast<intptr_t>(handle));
}

template <typename T>
jlong toHandle(T* pointer) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(pointer));
}

std::string toString(JNIEnv* env, jstring value) {
    if (!value) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result = chars ? chars : "";
    if (chars) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return result;
}

std::vector<jint> readIntArray(JNIEnv* env, jintArray array) {
    std::vector<jint> result;
    if (!array) {
        return result;
    }
    const jsize length = env->GetArrayLength(array);
    result.resize(static_cast<size_t>(length));
    if (length > 0) {
        env->GetIntArrayRegion(array, 0, length, result.data());
    }
    return result;
}

std::vector<std::string> readStringArray(JNIEnv* env, jobjectArray array) {
    std::vector<std::string> result;
    if (!array) {
        return result;
    }
    const jsize length = env->GetArrayLength(array);
    result.reserve(static_cast<size_t>(length));
    for (jsize i = 0; i < length; ++i) {
        jstring element = static_cast<jstring>(env->GetObjectArrayElement(array, i));
        result.push_back(toString(env, element));
        if (element) {
            env->DeleteLocalRef(element);
        }
    }
    return result;
}

jboolean check(JNIEnv* env, bool ok, const std::string& error) {
    if (!ok) {
        throwIllegalState(env, error);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

}  // namespace

extern "C" {

// ---------------------------------------------------------------------------
// Device / swapchain
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateDevice(
    JNIEnv* env, jclass, jlong hwndValue) {
    HWND hwnd = reinterpret_cast<HWND>(static_cast<intptr_t>(hwndValue));
    std::string error;
    Device* device = createDevice(hwnd, error);
    if (!device) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(device);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyDevice(
    JNIEnv*, jclass, jlong handle) {
    destroyDevice(fromHandle<Device>(handle));
}

JNIEXPORT jobjectArray JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeGetDeviceInfo(
    JNIEnv* env, jclass, jlong handle) {
    Device* device = fromHandle<Device>(handle);
    if (!device) {
        throwIllegalState(env, "nativeGetDeviceInfo: invalid device");
        return nullptr;
    }

    const std::string values[] = {
        device->adapterName,
        device->vendorName,
        device->versionText,
        std::to_string(static_cast<int>(device->featureLevel)),
        std::to_string(device->vendorId),
        std::to_string(device->deviceId),
        std::to_string(device->maxTextureSize),
        presentInfo(device),
    };
    constexpr jsize count = static_cast<jsize>(sizeof(values) / sizeof(values[0]));

    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) {
        return nullptr;
    }
    jobjectArray result = env->NewObjectArray(count, stringClass, nullptr);
    env->DeleteLocalRef(stringClass);
    if (!result) {
        return nullptr;
    }
    for (jsize i = 0; i < count; ++i) {
        jstring value = env->NewStringUTF(values[i].c_str());
        if (!value) {
            return nullptr;
        }
        env->SetObjectArrayElement(result, i, value);
        env->DeleteLocalRef(value);
    }
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePresent(
    JNIEnv* env, jclass, jlong handle, jint width, jint height) {
    Device* device = fromHandle<Device>(handle);
    if (!device) {
        throwIllegalState(env, "nativePresent: invalid device");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, present(device, static_cast<UINT>(width),
                              static_cast<UINT>(height), error),
                 error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeBlitToBackBuffer(
    JNIEnv* env, jclass, jlong deviceHandle, jlong textureHandle) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* texture = fromHandle<Texture>(textureHandle);
    if (!device || !texture) {
        throwIllegalState(env, "nativeBlitToBackBuffer: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, blitToBackBuffer(device, texture, error), error);
}

JNIEXPORT jstring JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeGetPresentInfo(
    JNIEnv* env, jclass, jlong handle) {
    Device* device = fromHandle<Device>(handle);
    const std::string info = presentInfo(device);
    return env->NewStringUTF(info.c_str());
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDumpTexture(
    JNIEnv* env, jclass, jlong deviceHandle, jlong textureHandle, jstring path) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* texture = fromHandle<Texture>(textureHandle);
    if (!device || !texture) {
        throwIllegalState(env, "nativeDumpTexture: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    const bool ok = dumpTexture(device, texture, toString(env, path), error);
    if (!ok) {
        throwIllegalState(env, error);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDumpBackBuffer(
    JNIEnv* env, jclass, jlong deviceHandle, jstring path) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeDumpBackBuffer: invalid device");
        return JNI_FALSE;
    }
    std::string error;
    const bool ok = dumpBackBuffer(device, toString(env, path), error);
    if (!ok) {
        throwIllegalState(env, error);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeSetVsync(
    JNIEnv*, jclass, jlong handle, jboolean vsync) {
    setVsync(fromHandle<Device>(handle), vsync == JNI_TRUE);
}

// ---------------------------------------------------------------------------
// Textures
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateTexture(
    JNIEnv* env, jclass, jlong deviceHandle, jint usage, jint format,
    jint width, jint height, jint depthOrLayers, jint mipLevels) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreateTexture: invalid device");
        return 0;
    }
    std::string error;
    Texture* texture = createTexture(
        device, usage, format, static_cast<UINT>(width),
        static_cast<UINT>(height), static_cast<UINT>(depthOrLayers),
        static_cast<UINT>(mipLevels), error);
    if (!texture) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(texture);
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateTextureView(
    JNIEnv* env, jclass, jlong deviceHandle, jlong textureHandle,
    jint baseMip, jint mipLevels) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* texture = fromHandle<Texture>(textureHandle);
    if (!device || !texture) {
        throwIllegalState(env, "nativeCreateTextureView: invalid handle");
        return 0;
    }
    std::string error;
    TextureView* view = createTextureView(
        device, texture, static_cast<UINT>(baseMip),
        static_cast<UINT>(mipLevels), error);
    if (!view) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(view);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyTexture(
    JNIEnv*, jclass, jlong handle) {
    destroyTexture(fromHandle<Texture>(handle));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyTextureView(
    JNIEnv*, jclass, jlong handle) {
    destroyTextureView(fromHandle<TextureView>(handle));
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeWriteToTexture(
    JNIEnv* env, jclass, jlong textureHandle, jint mip, jint layer,
    jint x, jint y, jint width, jint height, jlong address, jint rowStride) {
    Texture* texture = fromHandle<Texture>(textureHandle);
    if (!texture) {
        throwIllegalState(env, "nativeWriteToTexture: invalid texture");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, writeToTexture(
                     texture, static_cast<UINT>(mip), static_cast<UINT>(layer),
                     static_cast<UINT>(x), static_cast<UINT>(y),
                     static_cast<UINT>(width), static_cast<UINT>(height),
                     reinterpret_cast<const void*>(static_cast<intptr_t>(address)),
                     static_cast<UINT>(rowStride), error),
                 error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCopyTextureToBuffer(
    JNIEnv* env, jclass, jlong textureHandle, jlong bufferHandle,
    jlong bufferOffset, jint mip, jint x, jint y, jint width, jint height) {
    Texture* texture = fromHandle<Texture>(textureHandle);
    Buffer* buffer = fromHandle<Buffer>(bufferHandle);
    if (!texture || !buffer) {
        throwIllegalState(env, "nativeCopyTextureToBuffer: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, copyTextureToBuffer(
                     texture, buffer, static_cast<UINT>(bufferOffset),
                     static_cast<UINT>(mip), static_cast<UINT>(x),
                     static_cast<UINT>(y), static_cast<UINT>(width),
                     static_cast<UINT>(height), error),
                 error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCopyTextureToTexture(
    JNIEnv* env, jclass, jlong sourceHandle, jlong destinationHandle,
    jint mip, jint srcX, jint srcY, jint dstX, jint dstY, jint width, jint height) {
    Texture* source = fromHandle<Texture>(sourceHandle);
    Texture* destination = fromHandle<Texture>(destinationHandle);
    if (!source || !destination) {
        throwIllegalState(env, "nativeCopyTextureToTexture: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, copyTextureToTexture(
                     source, destination, static_cast<UINT>(mip),
                     static_cast<UINT>(srcX), static_cast<UINT>(srcY),
                     static_cast<UINT>(dstX), static_cast<UINT>(dstY),
                     static_cast<UINT>(width), static_cast<UINT>(height), error),
                 error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeClearColorTexture(
    JNIEnv* env, jclass, jlong deviceHandle, jlong textureHandle, jint argb) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* texture = fromHandle<Texture>(textureHandle);
    if (!device || !texture) {
        throwIllegalState(env, "nativeClearColorTexture: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, clearColorTexture(
                     device, texture, static_cast<UINT32>(argb), error),
                 error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeClearDepthTexture(
    JNIEnv* env, jclass, jlong deviceHandle, jlong textureHandle, jdouble depth) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* texture = fromHandle<Texture>(textureHandle);
    if (!device || !texture) {
        throwIllegalState(env, "nativeClearDepthTexture: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, clearDepthTexture(device, texture, depth, error), error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeClearColorAndDepthTexture(
    JNIEnv* env, jclass, jlong deviceHandle, jlong colorHandle, jint argb,
    jlong depthHandle, jdouble depthValue) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* color = fromHandle<Texture>(colorHandle);
    Texture* depth = fromHandle<Texture>(depthHandle);
    if (!device || !color || !depth) {
        throwIllegalState(env, "nativeClearColorAndDepthTexture: invalid handle");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, clearColorAndDepthTexture(
                     device, color, static_cast<UINT32>(argb), depth, depthValue,
                     nullptr, error),
                 error);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeClearColorAndDepthTextureRegion(
    JNIEnv* env, jclass, jlong deviceHandle, jlong colorHandle, jint argb,
    jlong depthHandle, jdouble depthValue, jint x, jint y, jint width, jint height) {
    Device* device = fromHandle<Device>(deviceHandle);
    Texture* color = fromHandle<Texture>(colorHandle);
    Texture* depth = fromHandle<Texture>(depthHandle);
    if (!device || !color || !depth) {
        throwIllegalState(env, "nativeClearColorAndDepthTextureRegion: invalid handle");
        return JNI_FALSE;
    }
    const int rect[4] = {
        static_cast<int>(x), static_cast<int>(y),
        static_cast<int>(width), static_cast<int>(height)};
    std::string error;
    return check(env, clearColorAndDepthTexture(
                     device, color, static_cast<UINT32>(argb), depth, depthValue,
                     rect, error),
                 error);
}

// ---------------------------------------------------------------------------
// Buffers
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateBuffer(
    JNIEnv* env, jclass, jlong deviceHandle, jint usage, jint size,
    jlong initialAddress, jint initialSize) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreateBuffer: invalid device");
        return 0;
    }
    std::string error;
    Buffer* buffer = createBuffer(
        device, usage, static_cast<UINT>(size),
        reinterpret_cast<const void*>(static_cast<intptr_t>(initialAddress)),
        static_cast<UINT>(initialSize), error);
    if (!buffer) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(buffer);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyBuffer(
    JNIEnv*, jclass, jlong handle) {
    destroyBuffer(fromHandle<Buffer>(handle));
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeWriteToBuffer(
    JNIEnv* env, jclass, jlong bufferHandle, jlong offset, jlong address,
    jlong size) {
    Buffer* buffer = fromHandle<Buffer>(bufferHandle);
    if (!buffer) {
        throwIllegalState(env, "nativeWriteToBuffer: invalid buffer");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, writeToBuffer(
                     buffer, static_cast<UINT>(offset),
                     reinterpret_cast<const void*>(static_cast<intptr_t>(address)),
                     static_cast<UINT>(size), error),
                 error);
}

JNIEXPORT jobject JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeMapBuffer(
    JNIEnv* env, jclass, jlong bufferHandle, jlong offset, jlong size,
    jboolean read, jboolean write) {
    Buffer* buffer = fromHandle<Buffer>(bufferHandle);
    if (!buffer) {
        throwIllegalState(env, "nativeMapBuffer: invalid buffer");
        return nullptr;
    }
    if (size <= 0 || size > 0x7FFFFFFF) {
        throwIllegalState(env, "nativeMapBuffer: invalid size");
        return nullptr;
    }
    std::string error;
    void* pointer = mapBuffer(
        buffer, static_cast<UINT>(offset), static_cast<UINT>(size),
        read == JNI_TRUE, write == JNI_TRUE, error);
    if (!pointer) {
        if (!error.empty()) {
            throwIllegalState(env, error);
        }
        return nullptr;
    }
    return env->NewDirectByteBuffer(pointer, static_cast<jlong>(size));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeUnmapBuffer(
    JNIEnv*, jclass, jlong handle) {
    unmapBuffer(fromHandle<Buffer>(handle));
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCopyBufferToBuffer(
    JNIEnv* env, jclass, jlong sourceHandle, jlong sourceOffset,
    jlong destinationHandle, jlong destinationOffset, jlong size) {
    Buffer* source = fromHandle<Buffer>(sourceHandle);
    Buffer* destination = fromHandle<Buffer>(destinationHandle);
    if (!source || !destination) {
        throwIllegalState(env, "nativeCopyBufferToBuffer: invalid buffer");
        return JNI_FALSE;
    }
    std::string error;
    return check(env, copyBufferToBuffer(
                     source, static_cast<UINT>(sourceOffset), destination,
                     static_cast<UINT>(destinationOffset),
                     static_cast<UINT>(size), error),
                 error);
}

// ---------------------------------------------------------------------------
// Samplers
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateSampler(
    JNIEnv* env, jclass, jlong deviceHandle, jint addressU, jint addressV,
    jint minFilter, jint magFilter, jint maxAnisotropy, jboolean hasMaxLod,
    jdouble maxLod) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreateSampler: invalid device");
        return 0;
    }
    std::string error;
    Sampler* sampler = createSampler(
        device, addressU, addressV, minFilter, magFilter, maxAnisotropy,
        hasMaxLod == JNI_TRUE, maxLod, error);
    if (!sampler) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(sampler);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroySampler(
    JNIEnv*, jclass, jlong handle) {
    destroySampler(fromHandle<Sampler>(handle));
}

// ---------------------------------------------------------------------------
// Pipelines
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreatePipeline(
    JNIEnv* env, jclass, jlong deviceHandle, jstring nameValue,
    jstring vertexValue, jstring fragmentValue, jint stride, jint topology,
    jobjectArray inputSemantics, jintArray inputSemanticIndices,
    jintArray inputFormats, jintArray inputOffsets, jintArray uniformSizes,
    jboolean blend, jint srcColor, jint dstColor, jint srcAlpha, jint dstAlpha,
    jboolean depthTest, jboolean depthWrite, jint depthFunc, jboolean cull,
    jint fillMode, jint depthBias, jfloat slopeScaledDepthBias,
    jint colorWriteMask) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreatePipeline: invalid device");
        return 0;
    }

    PipelineDesc desc;
    desc.name = toString(env, nameValue);
    desc.vertexHlsl = toString(env, vertexValue);
    desc.fragmentHlsl = toString(env, fragmentValue);
    desc.stride = static_cast<UINT>(stride);
    desc.topology = static_cast<D3D11_PRIMITIVE_TOPOLOGY>(topology);
    desc.blend = blend == JNI_TRUE;
    desc.srcColor = static_cast<D3D11_BLEND>(srcColor);
    desc.dstColor = static_cast<D3D11_BLEND>(dstColor);
    desc.srcAlpha = static_cast<D3D11_BLEND>(srcAlpha);
    desc.dstAlpha = static_cast<D3D11_BLEND>(dstAlpha);
    desc.depthTest = depthTest == JNI_TRUE;
    desc.depthWrite = depthWrite == JNI_TRUE;
    desc.depthFunc = static_cast<D3D11_COMPARISON_FUNC>(depthFunc);
    desc.cull = cull == JNI_TRUE;
    desc.fillMode = static_cast<D3D11_FILL_MODE>(fillMode);
    desc.depthBias = depthBias;
    desc.slopeScaledDepthBias = slopeScaledDepthBias;
    desc.colorWriteMask = static_cast<UINT>(colorWriteMask);

    const std::vector<std::string> semantics = readStringArray(env, inputSemantics);
    const std::vector<jint> semanticIndices = readIntArray(env, inputSemanticIndices);
    const std::vector<jint> formats = readIntArray(env, inputFormats);
    const std::vector<jint> offsets = readIntArray(env, inputOffsets);
    const size_t inputCount = std::min(
        std::min(semantics.size(), semanticIndices.size()),
        std::min(formats.size(), offsets.size()));
    desc.inputs.reserve(inputCount);
    for (size_t i = 0; i < inputCount; ++i) {
        InputElement input;
        input.semantic = semantics[i];
        input.semanticIndex = static_cast<UINT>(semanticIndices[i]);
        input.format = static_cast<DXGI_FORMAT>(formats[i]);
        input.offset = static_cast<UINT>(offsets[i]);
        desc.inputs.push_back(std::move(input));
    }

    const std::vector<jint> uniformBufferSizes = readIntArray(env, uniformSizes);
    desc.uniformBufferSizes.reserve(uniformBufferSizes.size());
    for (jint value : uniformBufferSizes) {
        desc.uniformBufferSizes.push_back(static_cast<UINT>(value > 0 ? value : 0));
    }

    std::string error;
    Pipeline* pipeline = createPipeline(device, desc, error);
    if (!pipeline) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(pipeline);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyPipeline(
    JNIEnv*, jclass, jlong handle) {
    destroyPipeline(fromHandle<Pipeline>(handle));
}

// ---------------------------------------------------------------------------
// Render passes
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateRenderPass(
    JNIEnv* env, jclass, jlong deviceHandle, jlong colorViewHandle,
    jlong depthViewHandle, jboolean clearColor, jint clearColorValue,
    jboolean clearDepth, jdouble clearDepthValue) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreateRenderPass: invalid device");
        return 0;
    }
    std::string error;
    RenderPass* pass = createRenderPass(
        device, fromHandle<TextureView>(colorViewHandle),
        fromHandle<TextureView>(depthViewHandle), clearColor == JNI_TRUE,
        static_cast<UINT32>(clearColorValue), clearDepth == JNI_TRUE,
        clearDepthValue, error);
    if (!pass) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(pass);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyRenderPass(
    JNIEnv*, jclass, jlong handle) {
    destroyRenderPass(fromHandle<RenderPass>(handle));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassSetPipeline(
    JNIEnv*, jclass, jlong passHandle, jlong pipelineHandle) {
    passSetPipeline(fromHandle<RenderPass>(passHandle),
                    fromHandle<Pipeline>(pipelineHandle));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassSetUniform(
    JNIEnv*, jclass, jlong passHandle, jint slot, jlong bufferHandle,
    jlong offset, jlong size) {
    passSetUniform(fromHandle<RenderPass>(passHandle), static_cast<UINT>(slot),
                   fromHandle<Buffer>(bufferHandle), static_cast<UINT>(offset),
                   static_cast<UINT>(size));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassSetTexelBuffer(
    JNIEnv*, jclass, jlong passHandle, jint slot, jlong bufferHandle,
    jint format) {
    passSetTexelBuffer(fromHandle<RenderPass>(passHandle), static_cast<UINT>(slot),
                       fromHandle<Buffer>(bufferHandle), format);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassBindTexture(
    JNIEnv*, jclass, jlong passHandle, jint slot, jlong viewHandle,
    jlong samplerHandle) {
    passBindTexture(fromHandle<RenderPass>(passHandle), static_cast<UINT>(slot),
                    fromHandle<TextureView>(viewHandle),
                    fromHandle<Sampler>(samplerHandle));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassSetVertexBuffer(
    JNIEnv*, jclass, jlong passHandle, jlong bufferHandle) {
    passSetVertexBuffer(fromHandle<RenderPass>(passHandle),
                        fromHandle<Buffer>(bufferHandle));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassSetIndexBuffer(
    JNIEnv*, jclass, jlong passHandle, jlong bufferHandle, jboolean index32) {
    passSetIndexBuffer(fromHandle<RenderPass>(passHandle),
                       fromHandle<Buffer>(bufferHandle), index32 == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassSetScissor(
    JNIEnv*, jclass, jlong passHandle, jboolean enabled, jint x, jint y,
    jint width, jint height) {
    passSetScissor(fromHandle<RenderPass>(passHandle), enabled == JNI_TRUE, x, y,
                   width, height);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassDrawIndexed(
    JNIEnv*, jclass, jlong passHandle, jint firstIndex, jint indexCount,
    jint baseVertex, jint instanceCount) {
    passDrawIndexed(fromHandle<RenderPass>(passHandle),
                    static_cast<UINT>(firstIndex), static_cast<UINT>(indexCount),
                    baseVertex, static_cast<UINT>(instanceCount));
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativePassDraw(
    JNIEnv*, jclass, jlong passHandle, jint firstVertex, jint vertexCount,
    jint instanceCount) {
    passDraw(fromHandle<RenderPass>(passHandle),
             static_cast<UINT>(firstVertex), static_cast<UINT>(vertexCount),
             static_cast<UINT>(instanceCount));
}

// ---------------------------------------------------------------------------
// Fences / queries
// ---------------------------------------------------------------------------
JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateFence(
    JNIEnv* env, jclass, jlong deviceHandle) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreateFence: invalid device");
        return 0;
    }
    std::string error;
    Fence* fence = createFence(device, error);
    if (!fence) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(fence);
}

JNIEXPORT jboolean JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeFenceAwait(
    JNIEnv*, jclass, jlong handle, jlong timeoutNanos) {
    return fenceAwait(fromHandle<Fence>(handle),
                      static_cast<int64_t>(timeoutNanos))
               ? JNI_TRUE
               : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyFence(
    JNIEnv*, jclass, jlong handle) {
    destroyFence(fromHandle<Fence>(handle));
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeCreateTimerQuery(
    JNIEnv* env, jclass, jlong deviceHandle) {
    Device* device = fromHandle<Device>(deviceHandle);
    if (!device) {
        throwIllegalState(env, "nativeCreateTimerQuery: invalid device");
        return 0;
    }
    std::string error;
    Query* query = createTimerQuery(device, error);
    if (!query) {
        throwIllegalState(env, error);
        return 0;
    }
    return toHandle(query);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeTimerQueryEnd(
    JNIEnv*, jclass, jlong handle) {
    timerQueryEnd(fromHandle<Query>(handle));
}

JNIEXPORT jlong JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeQueryGetValue(
    JNIEnv*, jclass, jlong handle) {
    Query* query = fromHandle<Query>(handle);
    int64_t nanos = 0;
    bool ready = false;
    queryGetValue(query, nanos, ready);
    if (!ready) {
        return static_cast<jlong>(INT64_MIN);
    }
    return static_cast<jlong>(nanos);
}

JNIEXPORT void JNICALL
Java_com_xgdt_dx11_dx11_Dx11Native_nativeDestroyQuery(
    JNIEnv*, jclass, jlong handle) {
    destroyQuery(fromHandle<Query>(handle));
}

}  // extern "C"
