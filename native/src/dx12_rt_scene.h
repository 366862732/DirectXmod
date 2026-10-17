#pragma once

// ===========================================================================
// P68：世界几何光追场景（DXR）
//
// Java 侧在区块网格上传时把 CPU 顶点/索引字节交给这里，按 section 建独立
// BLAS；每帧把这些 BLAS 以平移实例组成 TLAS。光追 pass 用深度缓冲重建
// 像素世界位置，向太阳发 shadow ray，输出可见性纹理给后续 pass 使用。
//
// 顶点格式沿用原版地形 BLOCK：stride 40，position = RGB32_FLOAT @ offset 0。
// 顶点位置是 section 局部坐标（0..16），世界位置由实例变换给出。
// ===========================================================================

#include "dx12_device.h"

#include <string>

namespace dx12mc {

// 上传/替换一个 section 某一渲染层的几何。key 由 Java 侧生成（SectionPos.asLong）。
// layer 是 ChunkSectionLayer 的 ordinal（0=SOLID 1=CUTOUT 2=TRANSLUCENT）——
// 同一 section 的不同层是互相独立的网格，各自一份 BLAS。
// cx/cy/cz 是 section 坐标（1 section = 16 格）。index32=false 时索引按 uint16。
// 线程安全：可在区块构建 worker 线程调用。
bool rtUpdateSection(long long key, int layer, int cx, int cy, int cz,
    const void* vertices, int vertexBytes, int vertexStride, int vertexCount,
    const void* indices, int indexBytes, int indexCount, bool index32,
    std::string& err);

bool rtRemoveSection(long long key);
void rtResetScene();

struct RtStats {
    int sections = 0;       // 已登记的 section 数
    int builtSections = 0;  // 已建好 BLAS 的 section 数
    int triangles = 0;      // 已建 BLAS 的三角形总数
    bool tlasReady = false;
};

// 在命令列表上录制尚未构建的 BLAS，以及（脏时）重建的 TLAS。
// 必须在渲染线程、command list 录制状态下调用。
bool rtBuildAccelerationStructures(CommandContext* ctx, std::string& err);

RtStats rtGetStats();

// 光追管线（懒初始化）。
bool rtEnsurePipeline(std::string& err);

// 执行 DispatchRays。
//   depthView       深度纹理的 TextureView（Dx12Object::Kind::TextureView）
//   outTex          输出纹理（创建时必须带 UAV 标志）
//   uniformBuffer   ShaderPackUniforms 常量缓冲
//   uniformOffset   该 CB 在 buffer 内的字节偏移（256 对齐）
bool rtDispatch(CommandContext* ctx, Dx12Object* depthView,
    Dx12Object* outTex, Dx12Object* uniformBuffer, long long uniformOffset,
    int width, int height, std::string& err);

// 设备重建时释放全部资源。
void rtResetAll();

}  // namespace dx12mc
