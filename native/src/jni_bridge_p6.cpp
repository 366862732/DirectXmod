// JNI 桥（P6：render pass 内的 draw 命令录制）。
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

CommandContext* toCtx(jlong handle) {
    return reinterpret_cast<CommandContext*>(static_cast<uintptr_t>(handle));
}

Dx12Object* toObject(jlong handle) {
    return reinterpret_cast<Dx12Object*>(static_cast<uintptr_t>(handle));
}

Dx12Pipeline* toPipeline(jlong handle) {
    return reinterpret_cast<Dx12Pipeline*>(static_cast<uintptr_t>(handle));
}

void logFail(const char* what, const std::string& err) {
    std::fprintf(stderr, "[dx12] %s: %s\n", what, err.c_str());
}

}  // namespace

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12SetPipeline(
    JNIEnv*, jclass, jlong ctx, jlong pipeline, jboolean hasDepth) {
    std::string err;
    if (!setPipeline(toCtx(ctx), toPipeline(pipeline), hasDepth == JNI_TRUE, err)) {
        logFail("dx12SetPipeline", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12SetScissor(
    JNIEnv*, jclass, jlong ctx, jint x, jint y, jint w, jint h) {
    std::string err;
    if (!setScissor(toCtx(ctx), x, y, w, h, err)) {
        logFail("dx12SetScissor", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12SetVertexBuffer(
    JNIEnv*, jclass, jlong ctx, jint slot, jlong buffer, jlong offset, jint stride) {
    std::string err;
    if (!setVertexBuffer(toCtx(ctx), slot, toObject(buffer), offset, stride, err)) {
        logFail("dx12SetVertexBuffer", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12SetIndexBuffer(
    JNIEnv*, jclass, jlong ctx, jlong buffer, jint indexType) {
    std::string err;
    if (!setIndexBuffer(toCtx(ctx), toObject(buffer), indexType, err)) {
        logFail("dx12SetIndexBuffer", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12PushDescriptors(
    JNIEnv* env, jclass, jlong ctx, jintArray types, jlongArray buffers,
    jlongArray offsets, jlongArray lengths, jintArray texelFormats, jlongArray views) {
    jsize count = env->GetArrayLength(types);
    if (count == 0) {
        return JNI_TRUE;
    }
    std::vector<jint> typesArr((size_t)count);
    std::vector<jlong> bufferArr((size_t)count);
    std::vector<jlong> offsetArr((size_t)count);
    std::vector<jlong> lengthArr((size_t)count);
    std::vector<jint> texelArr((size_t)count);
    std::vector<jlong> viewArr((size_t)count);
    env->GetIntArrayRegion(types, 0, count, typesArr.data());
    env->GetLongArrayRegion(buffers, 0, count, bufferArr.data());
    env->GetLongArrayRegion(offsets, 0, count, offsetArr.data());
    env->GetLongArrayRegion(lengths, 0, count, lengthArr.data());
    env->GetIntArrayRegion(texelFormats, 0, count, texelArr.data());
    env->GetLongArrayRegion(views, 0, count, viewArr.data());

    std::vector<DrawBinding> bindings((size_t)count);
    for (jsize i = 0; i < count; ++i) {
        bindings[i].type = (uint8_t)typesArr[i];
        bindings[i].buffer = toObject(bufferArr[i]);
        bindings[i].offset = offsetArr[i];
        bindings[i].length = lengthArr[i];
        bindings[i].texelFormat = texelArr[i];
        bindings[i].view = toObject(viewArr[i]);
    }
    std::string err;
    if (!pushDescriptors(toCtx(ctx), bindings, err)) {
        logFail("dx12PushDescriptors", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12DrawIndexed(
    JNIEnv*, jclass, jlong ctx, jint indexCount, jint instanceCount,
    jint firstIndex, jint baseVertex, jint firstInstance) {
    std::string err;
    if (!drawIndexedInstanced(toCtx(ctx), (UINT)indexCount, (UINT)instanceCount,
            (INT)firstIndex, (INT)baseVertex, (UINT)firstInstance, err)) {
        logFail("dx12DrawIndexed", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12Draw(
    JNIEnv*, jclass, jlong ctx, jint vertexCount, jint instanceCount,
    jint firstVertex, jint firstInstance) {
    std::string err;
    if (!drawInstanced(toCtx(ctx), (UINT)vertexCount, (UINT)instanceCount,
            (UINT)firstVertex, (UINT)firstInstance, err)) {
        logFail("dx12Draw", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12DrawIndexedIndirect(
    JNIEnv*, jclass, jlong ctx, jlong commands, jlong offset, jint drawCount) {
    std::string err;
    if (!drawIndexedIndirect(toCtx(ctx), toObject(commands), offset, (UINT)drawCount, err)) {
        logFail("dx12DrawIndexedIndirect", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_dx12_Dx12Native_dx12DrawIndirect(
    JNIEnv*, jclass, jlong ctx, jlong commands, jlong offset, jint drawCount) {
    std::string err;
    if (!drawIndirect(toCtx(ctx), toObject(commands), offset, (UINT)drawCount, err)) {
        logFail("dx12DrawIndirect", err);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

}  // extern "C"
