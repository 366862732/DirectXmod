// JNI bridge (P4): graphics pipeline creation/destruction.
// 独立文件（jni_bridge.cpp 可能被 IDE 进程占用），由 CMake 单独编译。
#include <jni.h>

#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include "dx12_device.h"

namespace {

const uint8_t* readBuffer(JNIEnv* env, jobject buf, jsize& outLen) {
    if (!buf) return nullptr;
    jlong cap = env->GetDirectBufferCapacity(buf);
    if (cap < 0 || cap > INT32_MAX) return nullptr;
    outLen = (jsize)cap;
    return static_cast<const uint8_t*>(env->GetDirectBufferAddress(buf));
}

int readI32(const uint8_t*& p) {
    int v;
    std::memcpy(&v, p, sizeof(v));  // little-endian（Java 侧 LITTLE_ENDIAN）
    p += sizeof(v);
    return v;
}

uint8_t readU8(const uint8_t*& p) {
    return *p++;
}

bool readBytes(const uint8_t*& p, jsize remaining, int len, std::vector<uint8_t>& out) {
    if (len < 0 || len > remaining) return false;
    out.assign(p, p + len);
    p += len;
    return true;
}

}  // namespace

extern "C" {

// long dx12CreateGraphicsPipeline(ByteBuffer desc) — desc 布局见 Dx12Native Javadoc。
// 失败不抛异常：记录 stderr 并返回 0（Java 侧视为无效管线）。
JNIEXPORT jlong JNICALL Java_com_dx12_dx12_Dx12Native_dx12CreateGraphicsPipeline(
    JNIEnv* env, jclass, jobject desc) {
    jsize total = 0;
    const uint8_t* p = readBuffer(env, desc, total);
    if (!p) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: buffer is not direct\n");
        return 0;
    }

    dx12mc::PipelineDesc d;
    jsize remaining = total;
    int vsLen = readI32(p); remaining -= 4;
    if (!readBytes(p, remaining, vsLen, d.vsBytes)) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: bad vsLen %d (remaining %d)\n", vsLen, remaining);
        return 0;
    }
    remaining -= vsLen;
    int psLen = readI32(p); remaining -= 4;
    if (!readBytes(p, remaining, psLen, d.psBytes)) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: bad psLen %d (remaining %d)\n", psLen, remaining);
        return 0;
    }
    remaining -= psLen;

    d.colorCount = readI32(p); remaining -= 4;
    if (d.colorCount < 0 || d.colorCount > 8) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: bad colorCount %d\n", d.colorCount);
        return 0;
    }
    d.colorTargets.resize((size_t)d.colorCount);
    for (int i = 0; i < d.colorCount; ++i) {
        dx12mc::PipelineDesc::ColorTarget& ct = d.colorTargets[i];
        ct.format = readI32(p);
        ct.writeMask = readU8(p);
        ct.blendEnabled = readU8(p) != 0;
        ct.srcColor = readU8(p);
        ct.dstColor = readU8(p);
        ct.colorOp = readU8(p);
        ct.srcAlpha = readU8(p);
        ct.dstAlpha = readU8(p);
        ct.alphaOp = readU8(p);
    }
    remaining -= (jsize)d.colorCount * 12;

    d.hasDepth = readU8(p) != 0; remaining -= 1;
    if (d.hasDepth) {
        d.depthFormat = readI32(p);
        d.depthWrite = readU8(p) != 0;
        d.depthCompareOp = readU8(p);
        remaining -= 6;
    }
    d.topology = readI32(p); remaining -= 4;
    d.cullEnabled = readU8(p) != 0; remaining -= 1;
    d.polygonMode = readI32(p); remaining -= 4;

    int elemCount = readI32(p); remaining -= 4;
    if (elemCount < 0 || elemCount > 64) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: bad inputElementCount %d\n", elemCount);
        return 0;
    }
    d.inputElements.resize((size_t)elemCount);
    for (int i = 0; i < elemCount; ++i) {
        dx12mc::PipelineDesc::InputElement& el = d.inputElements[i];
        el.location = readI32(p);
        el.binding = readI32(p);
        el.format = readI32(p);
        el.offset = readI32(p);
        el.stride = readI32(p);
        el.stepRate = readI32(p);
    }
    remaining -= (jsize)elemCount * 24;

    int entryCount = readI32(p); remaining -= 4;
    if (entryCount < 0 || entryCount > 64) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: bad entryCount %d\n", entryCount);
        return 0;
    }
    d.bindings.resize((size_t)entryCount);
    for (int i = 0; i < entryCount; ++i) {
        d.bindings[i].type = readU8(p);
        d.bindings[i].reg = readU8(p);
    }
    remaining -= (jsize)entryCount * 2;

    (void)remaining;  // 剩余字节允许非零（预留扩展）

    std::string err;
    dx12mc::Dx12Pipeline* pipeline = dx12mc::createGraphicsPipeline(d, err);
    if (!pipeline) {
        std::fprintf(stderr, "[dx12] dx12CreateGraphicsPipeline: %s\n", err.c_str());
        return 0;
    }
    return reinterpret_cast<jlong>(pipeline);
}

// void dx12DestroyPipeline(long pipeline)
JNIEXPORT void JNICALL Java_com_dx12_dx12_Dx12Native_dx12DestroyPipeline(
    JNIEnv*, jclass, jlong pipeline) {
    dx12mc::destroyPipeline(reinterpret_cast<dx12mc::Dx12Pipeline*>(pipeline));
}

}  // extern "C"
