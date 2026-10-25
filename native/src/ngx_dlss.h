#pragma once

#include <string>

// NGX（DLSS）接入层。
//
// 目前只做「配置 + 初始化 + 能力探测」：真正的 DLSS 超分/帧生成还需要
// App ID（NVIDIA 分配）以及运动矢量、jitter、HUDless 颜色等输入。
// 未配置 App ID 时整套逻辑不产生任何渲染副作用。
struct NgxState {
    bool configured = false;         // 是否填了 App ID / Project ID
    bool initialized = false;        // NGX 是否初始化成功
    bool dlssAvailable = false;      // DLSS 超分（SuperSampling）是否可用
    bool frameGenAvailable = false;  // DLSS 帧生成（FrameGeneration）是否可用
    std::string message;             // 最近一次状态描述（中文，写入 debug.log）
};

// appId：NVIDIA 分配的 Application ID 十进制字符串（留空则用 projectId）。
// projectId：Project ID（引擎集成方式），为空则用 appId。
// appDataPath：传给 NGX 的 ApplicationDataPath，NVNGX 日志与 nvngx_*.dll 查找目录。
void ngxSetConfig(const std::string& appId, const std::string& projectId,
                  const std::string& appDataPath);

// 设备创建后调用；记录 device 并在已配置时尝试初始化。
// 之后 ngxSetConfig 被调用时会用同一个 device 重试。
void ngxInitForDevice(void* d3d12Device);

// 设备销毁前调用。
void ngxShutdown();

NgxState ngxGetState();
