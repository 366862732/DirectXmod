#pragma once

// DLSS 超分（Super Resolution）与 DLSS 帧生成（DLSS-G）渲染接入。
//
// 与 ngx_dlss.cpp 的分工：
//   ngx_dlss.cpp        只负责 NGX 初始化 / 能力探测 / 配置。
//   ngx_dlss_render.cpp 负责真正的渲染资源、运动矢量生成、SR/FG 求值与
//                       生成帧的呈现（present）。
//
// 设计要点：
//   - 超分在最终 blit 时执行：低分辨率主颜色 + 相机运动矢量 + 深度 → 全分辨率
//     DLSS 输出，再作为 blit 源绘制到 backbuffer。
//   - 帧生成在 blit 之后执行：把已绘制的真实 backbuffer 复制到保留纹理，作为
//     DLSS-G 的 pBackbuffer；求值得到 1..3 张生成帧。渲染线程随后依次把生成帧
//     和真实帧 present 到 swapchain。
//   - 运动矢量由专用 compute shader 用深度 + 当前/上一帧 viewProj 生成，只包含
//     相机运动（没有逐物体 MV），这是本模组当前的已知精度限制。

#include <string>
#include <vector>

#include "dx12_device.h"

namespace dx12mc {

// Java 侧推送的 DLSS 配置。
struct NgxDlssConfig {
    bool srEnabled = false;
    // 0 = DLAA, 1 = Quality, 2 = Balanced, 3 = Performance,
    // 4 = UltraPerformance, 5 = Custom（使用 render_scale）
    int quality = 1;
    // NVSDK_NGX_DLSS_Hint_Render_Preset：0 = Default, 10 = J, 11 = K,
    // 12 = L, 13 = M
    int preset = 11;
    bool fgEnabled = false;
    // 每个真实帧额外生成的帧数（1 = 2x, 2 = 3x, 3 = 4x）。
    int fgFrames = 1;
};

void ngxDlssSetConfig(const NgxDlssConfig& cfg);

// NGX 能力（与配置无关）。
bool ngxDlssIsSrAvailable();
bool ngxDlssIsFgAvailable();

// 当前是否已有可用的 SR / FG feature（Java 侧据此决定是否施加 jitter）。
bool ngxDlssIsSrActive();
bool ngxDlssIsFgActive();

// 每帧开始：递增帧号、清空本帧捕获有效位。
// active=false 表示本帧不做 DLSS（离线渲染/配置关闭/能力不足）。
// notRenderingGameFrames=true 表示当前打开着菜单/界面，DLSS-G 应跳过求值。
void ngxDlssBeginFrame(bool active, bool notRenderingGameFrames);

// 世界渲染完成后调用（深度尚未被手部/GUI 清除）。
// data 布局（float，共 98 个）：
//   [0,16)   viewProj（行主序，行向量约定）
//   [16,32)  invViewProj
//   [32,48)  invViewProjJittered
//   [48,64)  proj
//   [64,80)  invProj
//   [80,83)  cameraPos
//   [83,86)  cameraUp
//   [86,89)  cameraRight
//   [89,92)  cameraFwd
//   [92]     cameraNear
//   [93]     cameraFar
//   [94]     cameraFov（弧度，垂直 FOV）
//   [95]     cameraAspectRatio（渲染宽/高）
//   [96]     jitterPx（渲染像素空间）
//   [97]     jitterPy
bool ngxDlssCaptureFrame(CommandContext* ctx, Dx12Surface* s, Dx12Object* depthTex,
    const float* data, int count, std::string& err);

// 场景切换 / 尺寸变化 / 配置变化时调用：只清时间状态，不立即释放 feature。
void ngxDlssReset();

// blit 绘制前调用。SR 成功求值时 outSrTex 指向全分辨率输出纹理，
// 调用方应把它作为 blit 源；返回 false 表示继续使用原始 srcTex。
bool ngxDlssEvaluateSr(CommandContext* ctx, Dx12Surface* s, Dx12Object* colorTex,
    ID3D12Resource** outSrTex, std::string& err);

// blit 绘制完成后、backbuffer 切换到 PRESENT 之前调用。
// 复制真实帧并按配置求值 DLSS-G；成功时把待呈现条目压入队列。
void ngxDlssAfterBlit(CommandContext* ctx, Dx12Surface* s, std::string& err);

// 渲染线程在 submit 之后调用：若存在与 mainCtx->fenceValue 匹配的待呈现条目，
// 依次 present 生成帧和保留的真实帧。presentedIndices 返回实际 present 过的
// backbuffer 下标（供渲染线程写 surfacePresentFences）。
// 返回 true 表示本帧已由本函数完成 present，调用方不要再 present。
bool ngxDlssPresentGeneratedFrames(Dx12Surface* s, CommandContext* mainCtx,
    std::vector<int>& presentedIndices, std::string& err);

// NGX Shutdown 之前调用：等待 GPU 空闲并释放全部 DLSS 资源/feature。
void ngxRenderShutdown();

}  // namespace dx12mc
