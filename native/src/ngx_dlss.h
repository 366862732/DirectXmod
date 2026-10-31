#pragma once

#include <string>

// NGX（DLSS）接入层。
//
// 负责「配置 + 初始化 + 能力探测」，渲染侧再通过 ngx_dlss_render 创建 feature。
// 没有 NVIDIA 分配的 App ID 时，使用内置 Project ID + CUSTOM 引擎初始化，
// 用户无需在配置文件中填写任何凭据。
struct NgxState {
    bool configured = false;         // 是否具备初始化凭据（App ID 或内置 Project ID）
    bool initialized = false;        // NGX 是否初始化成功
    bool dlssAvailable = false;      // DLSS 超分（SuperSampling）是否可用
    bool frameGenAvailable = false;  // DLSS 帧生成（FrameGeneration）是否可用
    std::string message;             // 最近一次状态描述（中文，写入 debug.log）
};

// appId：NVIDIA 分配的 Application ID 十进制字符串（0/空则用 projectId）。
// projectId：Project ID（引擎集成方式），为空则使用内置默认 GUID。
// appDataPath：传给 NGX 的 ApplicationDataPath，NVNGX 日志与 nvngx_*.dll 查找目录。
void ngxSetConfig(const std::string& appId, const std::string& projectId,
                  const std::string& appDataPath);

// 设备创建后调用；记录 device 并在已配置时尝试初始化。
// 之后 ngxSetConfig 被调用时会用同一个 device 重试。
void ngxInitForDevice(void* d3d12Device);

// NGX 是否已经初始化成功（渲染模块据此决定能否创建 feature）。
bool ngxIsInitialized();

// 渲染层在 NGX_D3D12_CREATE_DLSS_EXT 成功后调用，把「实测可用」回写到状态里。
// SuperSampling_Available 在 ProjectID + CUSTOM 引擎组合下可能误报 false，
// 因此以能否真正创建 feature 作为最终判据。
void ngxSetSrAvailable(bool available);

// 设备销毁前调用。
void ngxShutdown();

NgxState ngxGetState();
