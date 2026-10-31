#pragma once

// Intel XeSS 超分（XeSS-SR）接入。
//
// 与 ngx_dlss_render.cpp 的分工：
//   - 深度 / 相机运动矢量的捕获由 ngx_dlss_render.cpp 统一完成（与 DLSS 共用），
//     XeSS 通过 ngxDlssGetCapturedInputs 取用。
//   - 本文件只负责 libxess.dll 的动态加载、XeSS context 生命周期、输出纹理与
//     xessD3D12Execute 的调用。
//
// 设计要点：
//   - libxess.dll 通过 LoadLibraryW 动态加载（优先 dx12_mc.dll 同目录），
//     未安装时只是 XeSS 不可用，不会影响模组启动。
//   - 输出纹理格式与输入颜色一致（主目标为 R8G8B8A8_UNORM），alpha 由 XeSS
//     固定为 1.0。
//   - 使用低分辨率运动矢量 + 深度（XESS_INIT_FLAG_INVERTED_DEPTH），
//     jitter 直接传像素空间 (px, py)。

#include <string>

#include "dx12_device.h"

namespace dx12mc {

// Java 侧推送的 XeSS 配置。
struct XessConfig {
    bool enabled = false;
    // 0 = AA（原生）, 1 = Quality, 2 = Balanced, 3 = Performance,
    // 4 = Ultra Performance
    int quality = 1;
};

void xessSetConfig(const XessConfig& cfg);

// libxess.dll 是否加载成功且当前设备可创建 XeSS context。
bool xessIsAvailable();

// 当前是否已有初始化完成的 XeSS context（Java 侧据此决定是否施加 jitter）。
bool xessIsActive();

// 场景切换 / 尺寸变化 / 配置变化：清空 XeSS 时间历史。
void xessReset();

// blit 绘制前调用。成功时 outSrTex 指向全分辨率输出纹理。
bool xessEvaluateSr(CommandContext* ctx, Dx12Surface* s, Dx12Object* colorTex,
    ID3D12Resource** outSrTex, std::string& err);

// 一行状态文案（供视频设置页显示）。
std::string xessStatusText();

// 设备销毁前调用：等待 GPU 空闲并释放 context / 输出纹理。
void xessShutdown();

}  // namespace dx12mc
