#include "dx12_device.h"

#include <dxgi.h>

#include <cstdio>
#include <cstdarg>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

namespace dx12mc {

Dx12Surface* gActiveSurface = nullptr;  // P18：当前 active surface（渲染线程单例）

namespace {

DeviceContext gCtx;

// P6：延迟销毁（官方 queueForDestroy 语义）。D3D12 资源若被一个"打开但
// 未提交"的命令列表引用（如共享渲染 encoder 长时间打开期间 vanilla 临时
// 创建并 close 的 staging buffer），立即释放会在 Close 时报 E_INVALIDARG
// （"was deleted prior to closing the command list"）。因此 destroyObject
// 只登记到 pending，等所有打开的命令列表都提交完成（submit 同步等待后）
// 才统一 delete。
std::vector<Dx12Object*> gPendingDeletes;
int gOpenListCount = 0;  // 当前打开（已 begin 未提交）的命令列表数

// P6：SRV / sampler 描述符堆槽位复用。纹理 view 与 sampler 长会话累积分配，
// gNextSrv 单调递增会耗尽 4096 槽位（窗口 resize 触发 RenderTarget 重建 view
// 时崩溃："srv heap exhausted"）。对象销毁时把槽位归还 free-list。
std::vector<UINT> gFreeSrvSlots;
std::vector<UINT> gFreeSamplerSlots;

// 释放所有 pending 删除对象。调用前提：没有打开的命令列表（全部已提交并
// 同步等待完成），此时被删资源不再被任何命令列表引用，可安全释放。
void flushPendingDeletes() {
    for (Dx12Object* o : gPendingDeletes) {
        // 先归还描述符堆槽位（delete 后句柄失效，须在此前完成）
        if (o->descSlot >= 0) {
            if (o->kind == Dx12Object::Kind::TextureView) {
                gFreeSrvSlots.push_back((UINT)o->descSlot);
            } else if (o->kind == Dx12Object::Kind::Sampler) {
                gFreeSamplerSlots.push_back((UINT)o->descSlot);
            }
        }
        delete o;
    }
    gPendingDeletes.clear();
}

// 描述符堆槽位分配（P2 简单递增；P3 渲染层再做槽位回收）
UINT gNextSrv = 0;
UINT gNextRtv = 0;
UINT gNextDsv = 0;
UINT gNextSampler = 0;

// P6 修复（srv heap exhausted）：Minecraft 资源包加载/窗口 resize 的瞬时
// 峰值会同时持有数千个 texture view（旧 view 在 pending 删除队列尚未归还
// 槽位 + 新 view 批量创建），4096 槽位实测耗尽 → 资源包被移除 → GUI draws
// 为空 → 渲染目标只剩 clear 色 → 黑屏（按钮有声音）。D3D12 CBV_SRV_UAV
// SHADER_VISIBLE 堆上限 1,000,000（Tier1），65536 槽位内存约 2MB/堆（含
// CPU 镜像堆），一次性开大彻底消除该崩溃点；free-list 槽位复用仍保留，
// 覆盖长期会话的泄漏兜底。
constexpr UINT kSrvHeapSize = 65536;
constexpr UINT kRtvHeapSize = 2048;
constexpr UINT kDsvHeapSize = 256;
// 注意：D3D12 对 SAMPLER 描述符堆有硬上限 2048（所有 feature level），
// 扩到此值即为最大；再大 CreateDescriptorHeap 直接失败（曾设为 4096 导致
// 堆创建失败 → ensureDevice 半初始化 → createSampler 解引用 null 堆 AV）。
constexpr UINT kSamplerHeapSize = 2048;

// P6：瞬时描述符堆（drawHeap）每帧半区容量 + 总数（ring x4，配合三帧飞行）
// 注意：D3D12 root signature 的 descriptor table 从 heapBase 绝对寻址，
// GPU 命令列表在提交后仍会持续读取这些描述符直到 fence 完成。
// 三帧飞行（N、N+1、N+2 同时在空中），需要 4 个半区确保帧 N+2 写时
// 不会覆盖帧 N（GPU 仍在读）所使用的半区。
constexpr UINT kDrawHeapPerFrame = 32768;
constexpr UINT kDrawHeapSections = 4;
constexpr UINT kDrawHeapSize = kDrawHeapPerFrame * kDrawHeapSections;

// 从 free-list 复用或从堆尾分配一个 SRV 槽位；失败填充 err 返回 -1。
int allocSrvSlot(std::string& err) {
    if (!gFreeSrvSlots.empty()) {
        int slot = (int)gFreeSrvSlots.back();
        gFreeSrvSlots.pop_back();
        return slot;
    }
    // P6 兜底：堆满但存在未 flush 的延迟删除对象且没有打开的命令列表时，
    // 先 flush 一次再重试（正常路径 submit 后已 flush，此分支仅防御性的
    // 覆盖"批量 create+destroy 夹在两次 submit 之间"的时序）。
    if (gNextSrv >= kSrvHeapSize && !gPendingDeletes.empty() && gOpenListCount == 0) {
        flushPendingDeletes();
        if (!gFreeSrvSlots.empty()) {
            int slot = (int)gFreeSrvSlots.back();
            gFreeSrvSlots.pop_back();
            return slot;
        }
    }
    if (gNextSrv >= kSrvHeapSize) {
        // 诊断：若仍耗尽，打印堆使用画像定位泄漏（live 近似 = next - free，
        // 实际持有者 = next - free - pending 中未 flush 的 view）。
        err = "srv heap exhausted (next=" + std::to_string((long long)gNextSrv)
            + " free=" + std::to_string((long long)gFreeSrvSlots.size())
            + " pending=" + std::to_string((long long)gPendingDeletes.size())
            + " openLists=" + std::to_string(gOpenListCount) + ")";
        return -1;
    }
    return (int)gNextSrv++;
}

// 同上，sampler 槽位。
int allocSamplerSlot(std::string& err) {
    if (!gFreeSamplerSlots.empty()) {
        int slot = (int)gFreeSamplerSlots.back();
        gFreeSamplerSlots.pop_back();
        return slot;
    }
    if (gNextSampler >= kSamplerHeapSize && !gPendingDeletes.empty() && gOpenListCount == 0) {
        flushPendingDeletes();
        if (!gFreeSamplerSlots.empty()) {
            int slot = (int)gFreeSamplerSlots.back();
            gFreeSamplerSlots.pop_back();
            return slot;
        }
    }
    if (gNextSampler >= kSamplerHeapSize) {
        err = "sampler heap exhausted (next=" + std::to_string((long long)gNextSampler)
            + " free=" + std::to_string((long long)gFreeSamplerSlots.size())
            + " pending=" + std::to_string((long long)gPendingDeletes.size())
            + " openLists=" + std::to_string(gOpenListCount) + ")";
        return -1;
    }
    return (int)gNextSampler++;
}

bool createHeap(ID3D12Device* dev, D3D12_DESCRIPTOR_HEAP_TYPE type,
    UINT count, D3D12_DESCRIPTOR_HEAP_FLAGS flags, ComPtr<ID3D12DescriptorHeap>& out) {
    D3D12_DESCRIPTOR_HEAP_DESC desc{};
    desc.Type = type;
    desc.NumDescriptors = count;
    desc.Flags = flags;
    return SUCCEEDED(dev->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&out)));
}

std::string hrText(HRESULT hr) {
    char buf[64];
    snprintf(buf, sizeof(buf), "HRESULT 0x%08lX", (unsigned long)hr);
    return buf;
}

// 创建 adapter 查询用的 factory + LUID 适配器枚举
std::string queryAdapterName(ID3D12Device* dev) {
    try {
        ComPtr<IDXGIFactory4> factory;
        if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) return "<factory failed>";
        ComPtr<IDXGIAdapter> adapter;
        if (FAILED(factory->EnumAdapterByLuid(dev->GetAdapterLuid(), IID_PPV_ARGS(&adapter))))
            return "<enum adapter failed>";
        DXGI_ADAPTER_DESC desc{};
        if (FAILED(adapter->GetDesc(&desc))) return "<get desc failed>";
        char name[256] = {};
        WideCharToMultiByte(CP_UTF8, 0, desc.Description, -1, name, sizeof(name), nullptr, nullptr);
        return name;
    } catch (...) {
        return "<exception>";
    }
}

// 设备移除诊断：GetDeviceRemovedReason + 调试层最近消息（定位 0x887A0005 根因）。
std::string deviceStatusText() {
    std::string s;
    if (!gCtx.device) return s;
    HRESULT reason = gCtx.device->GetDeviceRemovedReason();
    s += "GetDeviceRemovedReason=" + hrText(reason);
    if (gCtx.infoQueue) {
        UINT64 count = gCtx.infoQueue->GetNumStoredMessages();
        UINT64 start = count > 24 ? count - 24 : 0;
        for (UINT64 i = start; i < count; ++i) {
            SIZE_T len = 0;
            if (FAILED(gCtx.infoQueue->GetMessage((UINT)i, nullptr, &len))) continue;
            std::vector<char> buf(len > 0 ? len : 1);
            D3D12_MESSAGE* msg = reinterpret_cast<D3D12_MESSAGE*>(buf.data());
            if (SUCCEEDED(gCtx.infoQueue->GetMessage((UINT)i, msg, &len))) {
                s += " | msg[" + std::to_string((long long)i) + "](";
                s += msg->pDescription ? msg->pDescription : "";
                s += ")";
            }
        }
    }
    return s;
}

// P6 诊断：转储并清空调试层 InfoQueue 消息（每帧开始时调用）。验证错误
// （ERROR/CORRUPTION）在 API 调用时写入 InfoQueue，但多数不导致崩溃，若
// 不主动读就会静默累积；打印出来可定位非法调用（UPLOAD heap 非法 transition、
// 未绑定/越界描述符、root 参数未设置、PSO 与 root signature 不匹配等）。
void dumpInfoQueueMessages() {
    if (!gCtx.infoQueue) return;
    UINT64 count = gCtx.infoQueue->GetNumStoredMessages();
    if (count == 0) return;
    UINT64 start = count > 16 ? count - 16 : 0;
    for (UINT64 i = start; i < count; ++i) {
        SIZE_T len = 0;
        if (FAILED(gCtx.infoQueue->GetMessage((UINT)i, nullptr, &len))) continue;
        std::vector<char> buf(len > 0 ? len : 1);
        D3D12_MESSAGE* msg = reinterpret_cast<D3D12_MESSAGE*>(buf.data());
        if (SUCCEEDED(gCtx.infoQueue->GetMessage((UINT)i, msg, &len))) {
            const char* sev = "?";
            switch (msg->Severity) {
                case D3D12_MESSAGE_SEVERITY_CORRUPTION: sev = "CORRUPTION"; break;
                case D3D12_MESSAGE_SEVERITY_ERROR: sev = "ERROR"; break;
                case D3D12_MESSAGE_SEVERITY_WARNING: sev = "WARNING"; break;
                case D3D12_MESSAGE_SEVERITY_INFO: sev = "INFO"; break;
                default: break;
            }
            dbgLog("InfoQueue[%s] %s", sev, msg->pDescription ? msg->pDescription : "");
        }
    }
    gCtx.infoQueue->ClearStoredMessages();
}

}  // namespace

// 毫秒时间戳（QPC），供诊断插桩打印精确阻塞点（渲染线程卡死排查用）。
double nowMs() {
    LARGE_INTEGER f, c;
    QueryPerformanceFrequency(&f);
    QueryPerformanceCounter(&c);
    return (double)c.QuadPart * 1000.0 / (double)f.QuadPart;
}

// P15: 日志级别 - 默认只输出 WARNING 及以上；设置环境变量 DX12_LOG_VERBOSE=1 开启 INFO/DEBUG
static int gLogLevel = 1; // WARN=1; ERR=0, WARN=1, INFO=2, DEBUG=3

// 诊断插桩：打印到 stderr（PCL 启动器会写入游戏日志；不影响 debug.log）。
// P12：同时镜像写入 %TEMP%\dx12-native.log——PCL 启动器死锁/游戏异常退出时
// stderr 重定向的游戏日志丢失，此独立文件随写随刷，进程挂起也能看到卡死点
// 的最后一条原生调用（fence 等待 / destroy 路径）。
void dbgLog(const char* fmt, ...) {
    if (gLogLevel < 1) return; // WARN=1, ERR=0
    char line[2048];
    va_list ap;
    va_start(ap, fmt);
    std::vsnprintf(line, sizeof(line), fmt, ap);
    va_end(ap);
    double t = nowMs();
    std::fprintf(stderr, "[dx12][t=%8.1fms] %s\n", t, line);
    fflush(stderr);
    const char* tmp = std::getenv("TEMP");
    std::string path = (tmp && *tmp) ? tmp : ".";
    path += "\\dx12-native.log";
    if (FILE* f = std::fopen(path.c_str(), "a")) {
        std::fprintf(f, "[dx12][t=%8.1fms] %s\n", t, line);
        std::fclose(f);
    }
}
void dbgLogInfo(const char* fmt, ...) {
    if (gLogLevel < 2) return; // INFO=2
    char line[2048];
    va_list ap;
    va_start(ap, fmt);
    std::vsnprintf(line, sizeof(line), fmt, ap);
    va_end(ap);
    double t = nowMs();
    std::fprintf(stderr, "[dx12][t=%8.1fms] %s\n", t, line);
    fflush(stderr);
    const char* tmp = std::getenv("TEMP");
    std::string path = (tmp && *tmp) ? tmp : ".";
    path += "\\dx12-native.log";
    if (FILE* f = std::fopen(path.c_str(), "a")) {
        std::fprintf(f, "[dx12][t=%8.1fms] %s\n", t, line);
        std::fclose(f);
    }
}
void dbgLogDebug(const char* fmt, ...) {
    if (gLogLevel < 3) return; // DEBUG=3
    char line[2048];
    va_list ap;
    va_start(ap, fmt);
    std::vsnprintf(line, sizeof(line), fmt, ap);
    va_end(ap);
    double t = nowMs();
    std::fprintf(stderr, "[dx12][t=%8.1fms] %s\n", t, line);
    fflush(stderr);
    const char* tmp = std::getenv("TEMP");
    std::string path = (tmp && *tmp) ? tmp : ".";
    path += "\\dx12-native.log";
    if (FILE* f = std::fopen(path.c_str(), "a")) {
        std::fprintf(f, "[dx12][t=%8.1fms] %s\n", t, line);
        std::fclose(f);
    }
}
void setLogLevel(int level) {
    gLogLevel = (level < 0) ? 0 : (level > 3) ? 3 : level;
}

// ---------------------------------------------------------------------------
// 设备生命周期
// ---------------------------------------------------------------------------

bool ensureDevice(std::string& errorOut) {
    // P6 修复：guard 必须同时校验描述符堆齐备。若上次调用在堆创建中途失败
    // （如 Sampler 堆 4096 超过 D3D12 上限 2048），gCtx.device 已置位但堆为
    // null，此 guard 若只看 device 会短路返回 true，后续 createSampler/
    // createTextureView 解引用 null 堆 → 原生 AV。堆全建成功才视为初始化完成。
    if (gCtx.device && gCtx.srvHeap && gCtx.srvCpuHeap && gCtx.rtvHeap &&
        gCtx.dsvHeap && gCtx.samplerHeap && gCtx.drawHeap) return true;

    // 诊断：先启用 D3D12 调试层（Win10/11 自带；失败则无调试层继续）。
    // 启用后非法 API 调用 / 描述符越界等会写入 InfoQueue，设备移除时可读回定位。
    bool debugEnabled = false;
    {
        ComPtr<ID3D12Debug> debug;
        if (SUCCEEDED(D3D12GetDebugInterface(IID_PPV_ARGS(&debug)))) {
            debug->EnableDebugLayer();
            debugEnabled = true;
        }
    }

    const D3D_FEATURE_LEVEL levels[] = {
        D3D_FEATURE_LEVEL_12_1, D3D_FEATURE_LEVEL_12_0,
        D3D_FEATURE_LEVEL_11_1, D3D_FEATURE_LEVEL_11_0,
    };
    ComPtr<ID3D12Device> device;
    for (auto level : levels) {
        if (SUCCEEDED(D3D12CreateDevice(nullptr, level, IID_PPV_ARGS(&device)))) {
            gCtx.featureLevel = level;
            break;
        }
    }
    if (!device && debugEnabled) {
        // 部分系统缺少 SDK 调试层：D3D12CreateDevice 会失败，回退到无调试层重试。
        debugEnabled = false;
        for (auto level : levels) {
            if (SUCCEEDED(D3D12CreateDevice(nullptr, level, IID_PPV_ARGS(&device)))) {
                gCtx.featureLevel = level;
                break;
            }
        }
    }
    if (!device) {
        errorOut = "D3D12CreateDevice failed at all feature levels";
        return false;
    }
    gCtx.device = device;
    gCtx.adapterName = queryAdapterName(device.Get());
    // 存储与 device 绑定的 DXGI adapter（供 swapchain 创建使用，避免 factory/adapter 不匹配）。
    {
        LUID luid = device->GetAdapterLuid();
        ComPtr<IDXGIFactory4> tmpFactory;
        if (SUCCEEDED(CreateDXGIFactory1(IID_PPV_ARGS(&tmpFactory)))) {
            tmpFactory->EnumAdapterByLuid(luid, IID_PPV_ARGS(&gCtx.adapter));
        }
    }
    // 取 InfoQueue（调试层启用后可用）用于设备移除时回读验证消息。
    if (debugEnabled) {
        if (FAILED(device->QueryInterface(IID_PPV_ARGS(&gCtx.infoQueue)))) {
            gCtx.infoQueue = nullptr;
        }
    }

    // 命令队列（P3 提交用）
    D3D12_COMMAND_QUEUE_DESC qd{};
    qd.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    if (FAILED(device->CreateCommandQueue(&qd, IID_PPV_ARGS(&gCtx.queue)))) {
        errorOut = "CreateCommandQueue failed";
        return false;
    }
    // 时间戳频率（DeviceInfo.timestampPeriod = 1/freq 用）；失败则保持 0。
    gCtx.queue->GetTimestampFrequency(&gCtx.timestampFrequency);

    // P6：全局队列 fence（createFence token 用，见 waitForQueueFenceValue）。
    if (FAILED(device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&gCtx.queueFence)))) {
        errorOut = "CreateFence(queue) failed";
        return false;
    }

    // 描述符堆
    if (!createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV,
            kSrvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.srvHeap) ||
        // P6：SRV 的 CPU-only 镜像堆（CopyDescriptorsSimple 复制源必须非
        // SHADER_VISIBLE；texture view 的 cpuHandle 指向这里）
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV,
            kSrvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_NONE, gCtx.srvCpuHeap) ||
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_RTV,
            kRtvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_NONE, gCtx.rtvHeap) ||
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_DSV,
            kDsvHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_NONE, gCtx.dsvHeap) ||
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER,
            kSamplerHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.samplerHeap) ||
        // P6：瞬时 draw 描述符堆（ring x4，支持三帧飞行）
        !createHeap(device.Get(), D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV,
            kDrawHeapSize, D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE, gCtx.drawHeap)) {
        errorOut = "CreateDescriptorHeap failed";
        return false;
    }
    gCtx.srvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    gCtx.rtvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    gCtx.dsvInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_DSV);
    gCtx.samplerInc = device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_SAMPLER);
    gCtx.drawInc = gCtx.srvInc;  // 同属 CBV_SRV_UAV 类型，增量相同

    // P6：ExecuteIndirect 用 command signature
    {
        D3D12_INDIRECT_ARGUMENT_DESC arg{};
        arg.Type = D3D12_INDIRECT_ARGUMENT_TYPE_DRAW_INDEXED;
        D3D12_COMMAND_SIGNATURE_DESC cs{};
        cs.NumArgumentDescs = 1;
        cs.pArgumentDescs = &arg;
        cs.ByteStride = 20;  // indexCount, instanceCount, startIndexLocation, baseVertexLocation, startInstanceLocation
        if (FAILED(device->CreateCommandSignature(&cs, nullptr, IID_PPV_ARGS(&gCtx.cmdSigIndexed)))) {
            errorOut = "CreateCommandSignature(indexed) failed";
            return false;
        }
        arg.Type = D3D12_INDIRECT_ARGUMENT_TYPE_DRAW;
        cs.ByteStride = 16;  // vertexCount, instanceCount, startVertexLocation, startInstanceLocation
        if (FAILED(device->CreateCommandSignature(&cs, nullptr, IID_PPV_ARGS(&gCtx.cmdSigNonIndexed)))) {
            errorOut = "CreateCommandSignature(non-indexed) failed";
            return false;
        }
    }
    return true;
}

void destroyDevice() {
    dbgLog("destroyDevice: enter");
    if (gCtx.queueFenceEvent) {
        CloseHandle(gCtx.queueFenceEvent);
        gCtx.queueFenceEvent = nullptr;
    }
    // 进程退出前释放所有延迟删除对象（若此后不再有 submit，pending 不会
    // 被 flush，需在此兜底）。
    flushPendingDeletes();
    gCtx = DeviceContext{};
    dbgLog("destroyDevice: done");
}

DeviceContext& deviceContextForJni() {
    return gCtx;
}

uintptr_t getDeviceHandle() {
    return reinterpret_cast<uintptr_t>(gCtx.device.Get());
}

uintptr_t getQueueHandle() {
    return reinterpret_cast<uintptr_t>(gCtx.queue.Get());
}

uintptr_t createHiddenWindow(int width, int height) {
    WNDCLASSW wc{};
    wc.lpfnWndProc   = DefWindowProcW;
    wc.hInstance      = GetModuleHandleW(nullptr);
    wc.lpszClassName  = L"Dx12HiddenTest";
    wc.hCursor = LoadCursorW(nullptr, (LPCWSTR)32512);  // IDC_ARROW
    if (!RegisterClassW(&wc)) return 0;
    HWND hwnd = CreateWindowExW(
        WS_EX_APPWINDOW,
        L"Dx12HiddenTest",
        L"DX12 Render Test",
        WS_POPUP,
        0, 0, width, height,
        nullptr, nullptr, GetModuleHandleW(nullptr), nullptr);
    if (!hwnd) { UnregisterClassW(L"Dx12HiddenTest", wc.hInstance); return 0; }
    ShowWindow(hwnd, SW_HIDE);
    UpdateWindow(hwnd);
    return reinterpret_cast<uintptr_t>(hwnd);
}

void destroyHiddenWindow(uintptr_t hwnd) {
    if (hwnd) {
        DestroyWindow(reinterpret_cast<HWND>(hwnd));
        UnregisterClassW(L"Dx12HiddenTest", GetModuleHandleW(nullptr));
    }
}

D3D12_CPU_DESCRIPTOR_HANDLE allocRtvHandle(std::string& err) {
    if (gNextRtv >= kRtvHeapSize) {
        err = "RTV descriptor heap exhausted";
        return D3D12_CPU_DESCRIPTOR_HANDLE{};
    }
    D3D12_CPU_DESCRIPTOR_HANDLE h = gCtx.rtvHeap->GetCPUDescriptorHandleForHeapStart();
    h.ptr += (SIZE_T)gNextRtv * gCtx.rtvInc;
    ++gNextRtv;
    return h;
}

// ---------------------------------------------------------------------------
// 格式映射：官方 GpuFormat ordinal -> DXGI_FORMAT
// 注意：DXGI 无 RGB/RGB16/RGB32 三通道格式，近似为 RGBA（同位数）。
// ---------------------------------------------------------------------------

DXGI_FORMAT toDxgiFormat(int gpuFormat) {
    switch (gpuFormat) {
        case 0:  return DXGI_FORMAT_R8_UNORM;              // R8_UNORM
        case 1:  return DXGI_FORMAT_R8_SNORM;              // R8_SNORM
        case 2:  return DXGI_FORMAT_R8G8_UNORM;            // RG8_UNORM
        case 3:  return DXGI_FORMAT_R8G8_SNORM;            // RG8_SNORM
        case 4:  return DXGI_FORMAT_R8G8B8A8_UNORM;        // RGB8_UNORM (近似)
        case 5:  return DXGI_FORMAT_R8G8B8A8_SNORM;        // RGB8_SNORM (近似)
        case 6:  return DXGI_FORMAT_R8G8B8A8_UNORM;        // RGBA8_UNORM
        case 7:  return DXGI_FORMAT_R8G8B8A8_SNORM;        // RGBA8_SNORM
        case 8:  return DXGI_FORMAT_R16_UNORM;             // R16_UNORM
        case 9:  return DXGI_FORMAT_R16_SNORM;             // R16_SNORM
        case 10: return DXGI_FORMAT_R16G16_UNORM;          // RG16_UNORM
        case 11: return DXGI_FORMAT_R16G16_SNORM;          // RG16_SNORM
        case 12: return DXGI_FORMAT_R16G16B16A16_UNORM;    // RGB16_UNORM (近似)
        case 13: return DXGI_FORMAT_R16G16B16A16_SNORM;    // RGB16_SNORM (近似)
        case 14: return DXGI_FORMAT_R16G16B16A16_UNORM;    // RGBA16_UNORM
        case 15: return DXGI_FORMAT_R16G16B16A16_SNORM;    // RGBA16_SNORM
        case 16: return DXGI_FORMAT_R8_UINT;               // R8_UINT
        case 17: return DXGI_FORMAT_R8_SINT;               // R8_SINT
        case 18: return DXGI_FORMAT_R8G8_UINT;             // RG8_UINT
        case 19: return DXGI_FORMAT_R8G8_SINT;             // RG8_SINT
        case 20: return DXGI_FORMAT_R8G8B8A8_UINT;         // RGB8_UINT (近似)
        case 21: return DXGI_FORMAT_R8G8B8A8_SINT;         // RGB8_SINT (近似)
        case 22: return DXGI_FORMAT_R8G8B8A8_UINT;         // RGBA8_UINT
        case 23: return DXGI_FORMAT_R8G8B8A8_SINT;         // RGBA8_SINT
        case 24: return DXGI_FORMAT_R16_UINT;              // R16_UINT
        case 25: return DXGI_FORMAT_R16_SINT;              // R16_SINT
        case 26: return DXGI_FORMAT_R16G16_UINT;           // RG16_UINT
        case 27: return DXGI_FORMAT_R16G16_SINT;           // RG16_SINT
        case 28: return DXGI_FORMAT_R16G16B16A16_UINT;     // RGB16_UINT (近似)
        case 29: return DXGI_FORMAT_R16G16B16A16_SINT;     // RGB16_SINT (近似)
        case 30: return DXGI_FORMAT_R16G16B16A16_UINT;     // RGBA16_UINT
        case 31: return DXGI_FORMAT_R16G16B16A16_SINT;     // RGBA16_SINT
        case 32: return DXGI_FORMAT_R32_UINT;              // R32_UINT
        case 33: return DXGI_FORMAT_R32_SINT;              // R32_SINT
        case 34: return DXGI_FORMAT_R32G32_UINT;           // RG32_UINT
        case 35: return DXGI_FORMAT_R32G32_SINT;           // RG32_SINT
        case 36: return DXGI_FORMAT_R32G32B32A32_UINT;     // RGB32_UINT (近似)
        case 37: return DXGI_FORMAT_R32G32B32A32_SINT;     // RGB32_SINT (近似)
        case 38: return DXGI_FORMAT_R32G32B32A32_UINT;     // RGBA32_UINT
        case 39: return DXGI_FORMAT_R32G32B32A32_SINT;     // RGBA32_SINT
        case 40: return DXGI_FORMAT_R16_FLOAT;             // R16_FLOAT
        case 41: return DXGI_FORMAT_R16G16_FLOAT;          // RG16_FLOAT
        case 42: return DXGI_FORMAT_R16G16B16A16_FLOAT;    // RGB16_FLOAT (近似)
        case 43: return DXGI_FORMAT_R16G16B16A16_FLOAT;    // RGBA16_FLOAT
        case 44: return DXGI_FORMAT_R32_FLOAT;             // R32_FLOAT
        case 45: return DXGI_FORMAT_R32G32_FLOAT;          // RG32_FLOAT
        case 46: return DXGI_FORMAT_R32G32B32A32_FLOAT;    // RGB32_FLOAT (近似)
        case 47: return DXGI_FORMAT_R32G32B32A32_FLOAT;    // RGBA32_FLOAT
        case 48: return DXGI_FORMAT_R10G10B10A2_UNORM;     // RGB10A2_UNORM
        case 49: return DXGI_FORMAT_R10G10B10A2_UINT;      // RGB10A2_UINT
        case 50: return DXGI_FORMAT_R11G11B10_FLOAT;       // RG11B10_FLOAT
        case 51: return DXGI_FORMAT_D32_FLOAT;             // D32_FLOAT
        case 52: return DXGI_FORMAT_D32_FLOAT_S8X24_UINT;  // D32_FLOAT_S8_UINT
        case 53: return DXGI_FORMAT_D24_UNORM_S8_UINT;     // D24_UNORM_S8_UINT
        case 54: return DXGI_FORMAT_D16_UNORM;             // D16_UNORM
        case 55: return DXGI_FORMAT_UNKNOWN;               // S8_UINT (无独立格式)
        default: return DXGI_FORMAT_UNKNOWN;
    }
}

// 顶点输入布局格式：RGB32_* 用精确三分量格式（DXGI 仅对 32 位三分量提供
// R32G32B32_* 输入格式；纹理用途的 toDxgiFormat 会把 RGB32 加宽成 RGBA32，
// 但输入布局加宽会导致与 shader 语义分量数不匹配，PSO 创建失败）。
//
// 注意：R32G32B32_FLOAT/UINT/SINT 在 D3D12 中不是合法的顶点输入格式，
// 必须回退到 R32G32B32A32_* 变体。shader 使用 .xyz 提取三个分量，.w 忽略。
DXGI_FORMAT toDxgiVertexFormat(int gpuFormat) {
    switch (gpuFormat) {
        case 36: return DXGI_FORMAT_R32G32B32A32_UINT;    // RGB32_UINT → RGBA32_UINT
        case 37: return DXGI_FORMAT_R32G32B32A32_SINT;    // RGB32_SINT → RGBA32_SINT
        case 46: return DXGI_FORMAT_R32G32B32A32_FLOAT;   // RGB32_FLOAT → RGBA32_FLOAT
        default: return toDxgiFormat(gpuFormat);
    }
}

namespace {

// 是否有 depth 面（官方 GpuFormat.hasDepthAspect）
bool hasDepthAspect(int fmt) { return fmt == 51 || fmt == 52 || fmt == 53 || fmt == 54; }

D3D12_HEAP_TYPE pickBufferHeapType(int usage) {
    if (usage & 2) return D3D12_HEAP_TYPE_UPLOAD;     // MAP_WRITE → CPU 直接写入
    if (usage & 1) return D3D12_HEAP_TYPE_READBACK;   // MAP_READ（无 MAP_WRITE）→ CPU 直接读回
    return D3D12_HEAP_TYPE_DEFAULT;                   // uniform/vertex → GPU 专属，copy 上传
}

D3D12_RESOURCE_STATES initialStateFor(D3D12_HEAP_TYPE heap) {
    switch (heap) {
        case D3D12_HEAP_TYPE_UPLOAD:   return D3D12_RESOURCE_STATE_GENERIC_READ;
        case D3D12_HEAP_TYPE_READBACK: return D3D12_RESOURCE_STATE_COPY_DEST;
        default:                       return D3D12_RESOURCE_STATE_COMMON;
    }
}

D3D12_RESOURCE_FLAGS textureFlags(int usage, int format) {
    D3D12_RESOURCE_FLAGS flags = D3D12_RESOURCE_FLAG_NONE;
    if (usage & 8) {  // RENDER_ATTACHMENT
        flags |= hasDepthAspect(format) ? D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL
                                        : D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;
    }
    return flags;
}

}  // namespace

// ---------------------------------------------------------------------------
// 资源创建
// ---------------------------------------------------------------------------

Dx12Object* createTexture(int usage, int format, int width, int height,
    int depthOrLayers, int mipLevels, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }

    DXGI_FORMAT dxgi = toDxgiFormat(format);
    if (dxgi == DXGI_FORMAT_UNKNOWN) { err = "unsupported format ordinal " + std::to_string(format); return nullptr; }

    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = std::max(1u, (UINT)width);
    desc.Height = std::max(1u, (UINT)height);
    desc.DepthOrArraySize = std::max(1u, (UINT)depthOrLayers);
    desc.MipLevels = std::max(1u, (UINT)mipLevels);
    desc.Format = dxgi;
    desc.SampleDesc.Count = 1;
    desc.Flags = textureFlags(usage, format);

    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_DEFAULT;

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::Texture;
    obj->usage = usage;
    obj->size = (long long)desc.Width * desc.Height;
    obj->dxgiFormat = dxgi;
    HRESULT hr = gCtx.device->CreateCommittedResource(
        &heap, D3D12_HEAP_FLAG_NONE, &desc, D3D12_RESOURCE_STATE_COMMON,
        nullptr, IID_PPV_ARGS(&obj->resource));
    if (FAILED(hr)) {
        err = "CreateCommittedResource(texture): " + hrText(hr);
        if (hr == DXGI_ERROR_DEVICE_REMOVED || hr == DXGI_ERROR_DEVICE_RESET ||
            hr == DXGI_ERROR_DEVICE_HUNG) {
            err += " — " + deviceStatusText();
        }
        return nullptr;
    }
    // P6 诊断：只打印 RENDER_ATTACHMENT（usage & 8）纹理——GUI 中间渲染目标/
    // 主场景 RT 都带此标志，blit 源纹理也在其中（定位 54EE180 之类 handle）。
    if (usage & 8) {
        dbgLogInfo("createTexture: RTA handle=%p w=%d h=%d layers=%d mips=%d fmt=%d usage=0x%x",
            (void*)obj.get(), width, height, depthOrLayers, mipLevels, format, usage);
    }
    if (usage & 16) {  // CUBEMAP_COMPATIBLE
        dbgLog("createTexture: CUBE handle=%p w=%d h=%d layers=%d mips=%d fmt=%d usage=0x%x",
            (void*)obj.get(), width, height, depthOrLayers, mipLevels, format, usage);
    }
    return obj.release();
}

Dx12Object* createBuffer(int usage, long long size, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }
    if (size <= 0) { err = "buffer size must be positive"; return nullptr; }

    D3D12_HEAP_TYPE heapType = pickBufferHeapType(usage);
    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = heapType;

    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    // P6：统一 256 对齐分配（D3D12 资源分配粒度本就 64KB，无额外浪费），
    // 保证任意 uniform 切片的 CBV（SizeInBytes 向上取整 256）不越界。
    desc.Width = ((UINT64)size + 255) & ~255ULL;
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::Buffer;
    obj->usage = usage;
    obj->size = size;
    obj->heapType = heapType;
    HRESULT hr = gCtx.device->CreateCommittedResource(
        &heap, D3D12_HEAP_FLAG_NONE, &desc, initialStateFor(heapType),
        nullptr, IID_PPV_ARGS(&obj->resource));
    if (FAILED(hr)) {
        err = "CreateCommittedResource(buffer): " + hrText(hr);
        if (hr == DXGI_ERROR_DEVICE_REMOVED || hr == DXGI_ERROR_DEVICE_RESET ||
            hr == DXGI_ERROR_DEVICE_HUNG) {
            err += " — " + deviceStatusText();
        }
        return nullptr;
    }
    return obj.release();
}

Dx12Object* createSampler(int addressU, int addressV, int minFilter,
    int magFilter, int maxAnisotropy, float maxLod, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }

    D3D12_SAMPLER_DESC desc{};
    // address: 0=REPEAT(WRAP), 1=CLAMP_TO_EDGE(CLAMP)
    desc.AddressU = (addressU == 0) ? D3D12_TEXTURE_ADDRESS_MODE_WRAP : D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    desc.AddressV = (addressV == 0) ? D3D12_TEXTURE_ADDRESS_MODE_WRAP : D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    desc.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    desc.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
    desc.MinLOD = 0.0f;
    desc.MaxLOD = maxLod > 0.0f ? maxLod : 16.0f;
    if (maxAnisotropy > 1) {
        desc.Filter = D3D12_FILTER_ANISOTROPIC;
        desc.MaxAnisotropy = (UINT)maxAnisotropy;
    } else {
        bool linear = (minFilter == 1) || (magFilter == 1);
        desc.Filter = linear ? D3D12_FILTER_MIN_MAG_MIP_LINEAR : D3D12_FILTER_MIN_MAG_MIP_POINT;
    }

    int slot = allocSamplerSlot(err);
    if (slot < 0) { return nullptr; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.samplerHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)slot * gCtx.samplerInc;
    D3D12_GPU_DESCRIPTOR_HANDLE gpu = gCtx.samplerHeap->GetGPUDescriptorHandleForHeapStart();
    gpu.ptr += (SIZE_T)slot * gCtx.samplerInc;

    gCtx.device->CreateSampler(&desc, cpu);

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::Sampler;
    obj->cpuHandle = cpu;
    obj->gpuHandle = gpu;
    obj->descSlot = slot;  // 销毁时归还 sampler 槽位
    return obj.release();
}

Dx12Object* createTextureView(Dx12Object* texture, int baseMipLevel,
    int mipLevels, std::string& err) {
    std::string e;
    if (!ensureDevice(e)) { err = e; return nullptr; }
    if (!texture || texture->kind != Dx12Object::Kind::Texture) {
        err = "createTextureView: invalid texture handle"; return nullptr;
    }
    if (!(texture->usage & 4)) {  // TEXTURE_BINDING
        err = "createTextureView: texture lacks TEXTURE_BINDING usage"; return nullptr;
    }

    int slot = allocSrvSlot(err);
    if (slot < 0) { return nullptr; }
    // P6：CPU-only 镜像堆创建描述符（CopyDescriptorsSimple 复制源必须非
    // SHADER_VISIBLE）；同槽位在 SHADER_VISIBLE 堆再创建一份供 GPU 直接引用。
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.srvCpuHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)slot * gCtx.srvInc;
    D3D12_CPU_DESCRIPTOR_HANDLE gpuCpu = gCtx.srvHeap->GetCPUDescriptorHandleForHeapStart();
    gpuCpu.ptr += (SIZE_T)slot * gCtx.srvInc;
    D3D12_GPU_DESCRIPTOR_HANDLE gpu = gCtx.srvHeap->GetGPUDescriptorHandleForHeapStart();
    gpu.ptr += (SIZE_T)slot * gCtx.srvInc;

    D3D12_SHADER_RESOURCE_VIEW_DESC srv{};
    // 深度格式（D32_FLOAT/D16_UNORM/D32_FLOAT_S8X24_UINT/D24_UNORM_S8_UINT）
    // 不能直接作为 SRV 格式（调试层：'The format cannot be used with a
    // ShaderResource view'）——需换成同格式族内的只读视图格式（读深度为
    // float/uint），纹理本体仍保持深度格式用于 DSV。
    DXGI_FORMAT viewFormat = texture->resource->GetDesc().Format;
    switch (viewFormat) {
        case DXGI_FORMAT_D32_FLOAT:            viewFormat = DXGI_FORMAT_R32_FLOAT; break;
        case DXGI_FORMAT_D16_UNORM:            viewFormat = DXGI_FORMAT_R16_UNORM; break;
        case DXGI_FORMAT_D32_FLOAT_S8X24_UINT: viewFormat = DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS; break;
        case DXGI_FORMAT_D24_UNORM_S8_UINT:    viewFormat = DXGI_FORMAT_R24_UNORM_X8_TYPELESS; break;
        default: break;
    }
    srv.Format = viewFormat;
    srv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    if (texture->usage & 16) {  // CUBEMAP_COMPATIBLE
        srv.ViewDimension = D3D12_SRV_DIMENSION_TEXTURECUBE;
        srv.TextureCube.MipLevels = (UINT)std::max(1, mipLevels);
        srv.TextureCube.MostDetailedMip = (UINT)std::max(0, baseMipLevel);
        dbgLog("createTextureView: CUBE srv slot=%d fmt=%d mip=%d most=%d layers=%d",
            slot, (int)texture->dxgiFormat, (int)srv.TextureCube.MipLevels,
            (int)srv.TextureCube.MostDetailedMip,
            (int)texture->resource->GetDesc().DepthOrArraySize);
    } else {
        srv.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
        srv.Texture2D.MipLevels = (UINT)std::max(1, mipLevels);
        srv.Texture2D.MostDetailedMip = (UINT)std::max(0, baseMipLevel);
    }
    gCtx.device->CreateShaderResourceView(texture->resource.Get(), &srv, cpu);
    // 同槽位在 SHADER_VISIBLE 堆再创建一份（gpuHandle 供 GPU 直接引用）。
    gCtx.device->CreateShaderResourceView(texture->resource.Get(), &srv, gpuCpu);

    auto obj = std::make_unique<Dx12Object>();
    obj->kind = Dx12Object::Kind::TextureView;
    obj->cpuHandle = cpu;
    obj->gpuHandle = gpu;
    obj->descSlot = slot;  // 销毁时归还 SRV 槽位
    obj->sourceTexture = texture;  // 采样前据此 transition 底层纹理状态
    return obj.release();
}

void destroyObject(Dx12Object* obj) {
    if (!obj) return;
    if (obj->mappedPtr) {
        if (obj->resource) obj->resource->Unmap(0, nullptr);
        obj->mappedPtr = nullptr;
    }
    // P12 诊断：关闭阶段定位用（destroyCommandEncoder 之后的销毁路径原本
    // 全静默，卡死时无法判断 Java 侧 transientMemory/管线缓存关闭进行到哪）。
    DBG_LOG_DEBUG("destroyObject: kind=%d size=%lld", (int)obj->kind, (long long)obj->size);
    // P6：延迟销毁（官方 VulkanGpuBuffer.close() 的 queueForDestroy 语义）。
    // 若资源正被打开的命令列表引用，立即 delete 会在 Close 时报
    // "deleted prior to closing the command list"（E_INVALIDARG）。登记到
    // pending，等所有打开的命令列表提交完成（submitCommandList 同步等待后
    // 调用 flushPendingDeletes）再统一释放。
    gPendingDeletes.push_back(obj);
}

// ---------------------------------------------------------------------------
// Buffer 映射（对应官方 GpuBuffer.map：read 需 MAP_READ，write 需 MAP_WRITE）
// ---------------------------------------------------------------------------

void* mapBuffer(Dx12Object* buffer, long long offset, long long length,
    bool read, bool write, std::string& err) {
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) {
        err = "mapBuffer: invalid buffer handle"; return nullptr;
    }
    if (read && !(buffer->usage & 1)) { err = "mapBuffer: buffer lacks MAP_READ"; return nullptr; }
    if (write && !(buffer->usage & 2)) { err = "mapBuffer: buffer lacks MAP_WRITE"; return nullptr; }
    if (offset < 0 || length < 0 || offset + length > buffer->size) {
        err = "mapBuffer: range out of bounds"; return nullptr;
    }
    if (buffer->heapType != D3D12_HEAP_TYPE_UPLOAD && buffer->heapType != D3D12_HEAP_TYPE_READBACK) {
        err = "mapBuffer: buffer is not host-visible (use MAP_WRITE/MAP_READ usage)"; return nullptr;
    }

    if (!buffer->mappedPtr) {
        void* base = nullptr;
        HRESULT hr = buffer->resource->Map(0, nullptr, &base);
        if (FAILED(hr)) { err = "Map: " + hrText(hr); return nullptr; }
        buffer->mappedPtr = base;
    }
    return static_cast<char*>(buffer->mappedPtr) + offset;
}

void unmapBuffer(Dx12Object* buffer) {
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) return;
    if (buffer->mappedPtr) {
        buffer->resource->Unmap(0, nullptr);
        buffer->mappedPtr = nullptr;
    }
}

// ---------------------------------------------------------------------------
// 资源层自检：创建 texture/buffer/sampler/view，map 写读验证，再销毁
// ---------------------------------------------------------------------------

std::string runResourceSelfTest() {
    std::string err;
    if (!ensureDevice(err)) return "SELF-TEST FAILED: " + err;

    // 1) texture: RGBA8_UNORM(6), 64x64, TEXTURE_BINDING|RENDER_ATTACHMENT
    Dx12Object* tex = createTexture(4 | 8, 6, 64, 64, 1, 1, err);
    if (!tex) return "SELF-TEST FAILED: texture - " + err;

    // 2) buffer: VERTEX|COPY_DST|MAP_WRITE, 1024
    Dx12Object* buf = createBuffer(32 | 8 | 2, 1024, err);
    if (!buf) { destroyObject(tex); return "SELF-TEST FAILED: buffer - " + err; }

    // 3) map 写入并读回验证
    void* p = mapBuffer(buf, 0, 1024, false, true, err);
    if (!p) { destroyObject(buf); destroyObject(tex); return "SELF-TEST FAILED: map - " + err; }
    memset(p, 0x5A, 1024);
    bool dataOk = (static_cast<unsigned char*>(p)[0] == 0x5A) &&
                  (static_cast<unsigned char*>(p)[1023] == 0x5A);
    unmapBuffer(buf);

    // 4) sampler: REPEAT/REPEAT, NEAREST/NEAREST, aniso=1, maxLod=16
    Dx12Object* sampler = createSampler(0, 0, 0, 0, 1, 16.0f, err);
    if (!sampler) { destroyObject(buf); destroyObject(tex); return "SELF-TEST FAILED: sampler - " + err; }

    // 5) texture view
    Dx12Object* view = createTextureView(tex, 0, 1, err);
    if (!view) { destroyObject(sampler); destroyObject(buf); destroyObject(tex);
        return "SELF-TEST FAILED: texture view - " + err; }

    std::string result = "SELF-TEST OK (map write/read " + std::string(dataOk ? "verified" : "MISMATCH") + ")";

    destroyObject(view);
    destroyObject(sampler);
    destroyObject(buf);
    destroyObject(tex);
    return result;
}

// ---------------------------------------------------------------------------
// 命令层（P3）
// ---------------------------------------------------------------------------

namespace {

// initialStateFor 已在 P2 资源层匿名 namespace 中定义（同一翻译单元内不得重复）。
// GENERIC_READ 已包含 COPY_SOURCE 等只读状态，无需 transition；
// COPY_DEST（READBACK 初始态）无需 transition。
bool needTransition(D3D12_RESOURCE_STATES from, D3D12_RESOURCE_STATES to) {
    if (from == to) return false;
    if (from == D3D12_RESOURCE_STATE_GENERIC_READ) return false;
    return true;
}

// 将 D3D12_RESOURCE_STATES 转为可读名称，用于诊断日志。
// 注意：D3D12_RESOURCE_STATE_PRESENT 和 D3D12_RESOURCE_STATE_COMMON 在此 SDK
// 版本中均为 0（flip model 下等价），不可同时作为独立 case。
static const char* stateNameFor(D3D12_RESOURCE_STATES s) {
    switch (s) {
        case D3D12_RESOURCE_STATE_COMMON:                         return "COMMON";
        case D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER:     return "VB_CB";
        case D3D12_RESOURCE_STATE_INDEX_BUFFER:                   return "IB";
        case D3D12_RESOURCE_STATE_RENDER_TARGET:                  return "RT";
        case D3D12_RESOURCE_STATE_UNORDERED_ACCESS:               return "UA";
        case D3D12_RESOURCE_STATE_DEPTH_WRITE:                    return "DEPTH_W";
        case D3D12_RESOURCE_STATE_DEPTH_READ:                     return "DEPTH_R";
        case D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE:      return "NPSR";
        case D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE:          return "PSR";
        case D3D12_RESOURCE_STATE_STREAM_OUT:                     return "SO";
        case D3D12_RESOURCE_STATE_COPY_DEST:                      return "COPY_D";
        case D3D12_RESOURCE_STATE_COPY_SOURCE:                    return "COPY_S";
        case D3D12_RESOURCE_STATE_RESOLVE_DEST:                   return "RESOLVE_D";
        case D3D12_RESOURCE_STATE_RESOLVE_SOURCE:                 return "RESOLVE_S";
        case D3D12_RESOURCE_STATE_INDIRECT_ARGUMENT:              return "INDIRECT";
        default: return "???";
    }
}

void resourceBarrier(ID3D12GraphicsCommandList* list, ID3D12Resource* res,
    D3D12_RESOURCE_STATES from, D3D12_RESOURCE_STATES to) {
    // D3D12 要求 StateBefore != StateAfter；相同状态发出 barrier 会触发
    // ID3D12CommandList::ResourceBarrier 验证 ERROR（0x80004005）。
    // 此处做保护性跳过，并记录诊断日志。
    if (from == to) {
        dbgLog("resourceBarrier: SKIP (from==to=%s) res=%p", stateNameFor(from), (void*)res);
        return;
    }
    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = res;
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = from;
    b.Transition.StateAfter = to;
    dbgLog("resourceBarrier: %p %s -> %s", (void*)res, stateNameFor(from), stateNameFor(to));
    list->ResourceBarrier(1, &b);
}

// DXGI_FORMAT -> 每 texel 字节数（仅非压缩格式；压缩格式返回 16 = 4x4 块）。
// 用于 CopyTextureRegion 的 footprint 行距计算。
UINT blockSizeFor(DXGI_FORMAT f) {
    switch (f) {
        case DXGI_FORMAT_R32G32B32A32_TYPELESS:
        case DXGI_FORMAT_R32G32B32A32_FLOAT:
        case DXGI_FORMAT_R32G32B32A32_UINT:
        case DXGI_FORMAT_R32G32B32A32_SINT: return 16;
        case DXGI_FORMAT_R32G32B32_TYPELESS:
        case DXGI_FORMAT_R32G32B32_FLOAT:
        case DXGI_FORMAT_R32G32B32_UINT:
        case DXGI_FORMAT_R32G32B32_SINT: return 12;
        case DXGI_FORMAT_R16G16B16A16_TYPELESS:
        case DXGI_FORMAT_R16G16B16A16_FLOAT:
        case DXGI_FORMAT_R16G16B16A16_UNORM:
        case DXGI_FORMAT_R16G16B16A16_UINT:
        case DXGI_FORMAT_R16G16B16A16_SNORM:
        case DXGI_FORMAT_R16G16B16A16_SINT:
        case DXGI_FORMAT_R32G32_TYPELESS:
        case DXGI_FORMAT_R32G32_FLOAT:
        case DXGI_FORMAT_R32G32_UINT:
        case DXGI_FORMAT_R32G32_SINT:
        case DXGI_FORMAT_D32_FLOAT: return 8;
        case DXGI_FORMAT_R32_FLOAT:
        case DXGI_FORMAT_R32_UINT:
        case DXGI_FORMAT_R32_SINT:
        case DXGI_FORMAT_D32_FLOAT_S8X24_UINT: return 4;
        case DXGI_FORMAT_R8G8B8A8_TYPELESS:
        case DXGI_FORMAT_R8G8B8A8_UNORM:
        case DXGI_FORMAT_R8G8B8A8_UNORM_SRGB:
        case DXGI_FORMAT_R8G8B8A8_UINT:
        case DXGI_FORMAT_R8G8B8A8_SNORM:
        case DXGI_FORMAT_R8G8B8A8_SINT:
        case DXGI_FORMAT_B8G8R8A8_UNORM:
        case DXGI_FORMAT_B8G8R8A8_UNORM_SRGB:
        case DXGI_FORMAT_R16G16_TYPELESS:
        case DXGI_FORMAT_R16G16_FLOAT:
        case DXGI_FORMAT_R16G16_UNORM:
        case DXGI_FORMAT_R16G16_UINT:
        case DXGI_FORMAT_R16G16_SNORM:
        case DXGI_FORMAT_R16G16_SINT:
        case DXGI_FORMAT_D24_UNORM_S8_UINT:
        case DXGI_FORMAT_R10G10B10A2_UNORM:
        case DXGI_FORMAT_R11G11B10_FLOAT: return 4;
        case DXGI_FORMAT_R16_FLOAT:
        case DXGI_FORMAT_R16_UNORM:
        case DXGI_FORMAT_R16_UINT:
        case DXGI_FORMAT_R16_SNORM:
        case DXGI_FORMAT_R16_SINT:
        case DXGI_FORMAT_R8G8_TYPELESS:
        case DXGI_FORMAT_R8G8_UNORM:
        case DXGI_FORMAT_R8G8_UINT:
        case DXGI_FORMAT_R8G8_SNORM:
        case DXGI_FORMAT_R8G8_SINT: return 2;
        case DXGI_FORMAT_R8_UNORM:
        case DXGI_FORMAT_R8_UINT:
        case DXGI_FORMAT_R8_SNORM:
        case DXGI_FORMAT_R8_SINT:
        case DXGI_FORMAT_A8_UNORM: return 1;
        case DXGI_FORMAT_BC1_TYPELESS:
        case DXGI_FORMAT_BC1_UNORM:
        case DXGI_FORMAT_BC1_UNORM_SRGB:
        case DXGI_FORMAT_BC4_TYPELESS:
        case DXGI_FORMAT_BC4_UNORM:
        case DXGI_FORMAT_BC4_SNORM: return 8;   // 4x4 块
        case DXGI_FORMAT_BC2_TYPELESS:
        case DXGI_FORMAT_BC2_UNORM:
        case DXGI_FORMAT_BC2_UNORM_SRGB:
        case DXGI_FORMAT_BC3_TYPELESS:
        case DXGI_FORMAT_BC3_UNORM:
        case DXGI_FORMAT_BC3_UNORM_SRGB:
        case DXGI_FORMAT_BC5_TYPELESS:
        case DXGI_FORMAT_BC5_UNORM:
        case DXGI_FORMAT_BC5_SNORM:
        case DXGI_FORMAT_BC6H_TYPELESS:
        case DXGI_FORMAT_BC6H_UF16:
        case DXGI_FORMAT_BC6H_SF16:
        case DXGI_FORMAT_BC7_TYPELESS:
        case DXGI_FORMAT_BC7_UNORM:
        case DXGI_FORMAT_BC7_UNORM_SRGB: return 16;  // 4x4 块
        default: return 4;
    }
}

// 在指定队列上执行瞬时提交并等待完成（query 读回 / getTimestampNow 用）。
// 等待有界（5s）：GPU 队列卡住时返回错误而不是无限挂起（黑屏无响应根因嫌疑）。
bool flushAndWait(ID3D12CommandList* list, std::string& err) {
    dbgLog("flushAndWait: enter");
    ID3D12CommandList* lists[] = { list };
    gCtx.queue->ExecuteCommandLists(1, lists);
    ComPtr<ID3D12Fence> fence;
    if (FAILED(gCtx.device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence)))) {
        err = "flushAndWait: CreateFence failed"; return false;
    }
    UINT64 fv = 1;
    if (FAILED(gCtx.queue->Signal(fence.Get(), fv))) {
        err = "flushAndWait: Signal failed"; return false;
    }
    HANDLE evt = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!evt) { err = "flushAndWait: CreateEvent failed"; return false; }
    while (fence->GetCompletedValue() < fv) {
        fence->SetEventOnCompletion(fv, evt);
        if (WaitForSingleObject(evt, 5000) != WAIT_OBJECT_0) {
            CloseHandle(evt);
            err = "flushAndWait: timed out waiting for queue completion";
            dbgLog("flushAndWait: TIMEOUT (queue stalled)");
            return false;
        }
    }
    CloseHandle(evt);
    dbgLog("flushAndWait: done");
    return true;
}

}  // namespace

// 按本 command list 已跟踪状态把资源过渡到 to。fromInitial 为资源创建时的
// 初始状态（texture=COMMON，buffer=initialStateFor(heapType)）；若本 list 内
// 已跟踪过该资源则以跟踪状态为准。绝不在 list 内回退到 COMMON（D3D12 禁止
// 从已提升状态显式回 COMMON；decay 由命令列表执行完成时隐式处理）。
// beginCommandList 无条件清空 tracking（submit 阻塞等待 GPU 完成，资源已
// 隐式 decay 回 COMMON；此处按初始态开始录制是正确且保守的）。
// 定义在匿名 namespace 之外：transitionTextureTo 供 dx12_surface.cpp 使用。
void transitionTo(CommandContext* ctx, ID3D12Resource* res,
    D3D12_RESOURCE_STATES fromInitial, D3D12_RESOURCE_STATES to) {
    if (!ctx || !res) return;
    auto& m = ctx->resourceState;
    D3D12_RESOURCE_STATES from;
    auto it = m.find(res);
    if (it != m.end()) {
        from = it->second;
    } else {
        from = fromInitial;
        if (!needTransition(from, to)) return;
    }
    if (from == to) return;
    resourceBarrier(ctx->commandList.Get(), res, from, to);
    m[res] = to;
}

// buffer 按跟踪状态过渡（不回落初始状态）。
void transitionBufferTo(CommandContext* ctx, Dx12Object* buf,
    D3D12_RESOURCE_STATES to) {
    if (!buf || !buf->resource) return;
    transitionTo(ctx, buf->resource.Get(), initialStateFor(buf->heapType), to);
}

// texture 按跟踪状态过渡（初始锚点 COMMON；不回落 COMMON）。
void transitionTextureTo(CommandContext* ctx, Dx12Object* tex,
    D3D12_RESOURCE_STATES to) {
    if (!tex || !tex->resource) return;
    transitionTo(ctx, tex->resource.Get(), D3D12_RESOURCE_STATE_COMMON, to);
}

// 阻塞等待 GPU 队列空闲：Signal 新 fence 并等待其完成。
// 用于销毁 swapchain/设备前，确保 backbuffer 等资源不再被 GPU 使用。
// 等待有界（5s）：队列卡住时返回错误，避免销毁路径无限挂起。
bool deviceWaitIdle(std::string& err) {
    if (!gCtx.device || !gCtx.queue) {
        err = "deviceWaitIdle: device not initialized";
        return false;
    }
    dbgLog("deviceWaitIdle: enter");
    // 关键：先读当前已完成值，再信号一个绝对高于它的值。
    // 原逻辑 fv = ++gCtx.queueFenceValue 存在竞态——若 GPU 已处理到更高的 fence
    // 值（Java 线程在 awaitCompletion 后提交更多帧），fv 会低于已完成值，
    // GetCompletedValue() >= fv 导致 while 循环立即退出，后续 ResizeBuffers
    // 看到 DWM 仍持有 backbuffer 引用而失败（DXGI_ERROR_INVALID_CALL）。
    UINT64 cv = gCtx.queueFence->GetCompletedValue();
    UINT64 fv = cv + 1;
    // 防御：若 fv 已等于或超过 queueFenceValue（说明有并发提交更新了计数器），
    // 继续递增直到 fv > queueFenceValue，保证我们等待的值是尚未被 GPU 处理过的。
    while (fv <= gCtx.queueFenceValue) {
        ++fv;
    }
    if (FAILED(gCtx.queue->Signal(gCtx.queueFence.Get(), fv))) {
        err = "deviceWaitIdle: Signal failed";
        return false;
    }
    HANDLE evt = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!evt) {
        err = "deviceWaitIdle: CreateEvent failed";
        return false;
    }
    while (gCtx.queueFence->GetCompletedValue() < fv) {
        gCtx.queueFence->SetEventOnCompletion(fv, evt);
        if (WaitForSingleObject(evt, 5000) != WAIT_OBJECT_0) {
            CloseHandle(evt);
            err = "deviceWaitIdle: timed out waiting for queue completion";
            dbgLog("deviceWaitIdle: TIMEOUT (queue stalled)");
            return false;
        }
    }
    CloseHandle(evt);
    dbgLog("deviceWaitIdle: done (waited for fv=%llu)", (unsigned long long)fv);
    return true;
}

CommandContext* createCommandEncoder(std::string& err) {
    if (!ensureDevice(err)) return nullptr;
    DBG_LOG_DEBUG("createCommandEncoder: enter");
    auto ctx = std::make_unique<CommandContext>();
    for (int i = 0; i < 3; ++i) {
        if (FAILED(gCtx.device->CreateCommandAllocator(
                D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&ctx->allocators[i])))) {
            err = "createCommandEncoder: CreateCommandAllocator failed"; return nullptr;
        }
    }
    if (FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            ctx->allocators[0].Get(), nullptr, IID_PPV_ARGS(&ctx->commandList)))) {
        err = "createCommandEncoder: CreateCommandList failed"; return nullptr;
    }
    // 初始 closed 状态；beginCommandList 时 Reset。
    ctx->commandList->Close();
    if (FAILED(gCtx.device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&ctx->fence)))) {
        err = "createCommandEncoder: CreateFence failed"; return nullptr;
    }
    ctx->fenceEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!ctx->fenceEvent) { err = "createCommandEncoder: CreateEvent failed"; return nullptr; }
    dbgLogInfo("createCommandEncoder: done ctx=%p", (void*)ctx.get());
    return ctx.release();
}

void destroyCommandEncoder(CommandContext* ctx) {
    if (!ctx) return;
    DBG_LOG_DEBUG("destroyCommandEncoder: enter fenceValue=%llu", (unsigned long long)ctx->fenceValue);
    // 该 ctx 可能仍有未执行的已提交命令（如 createBuffer(data) 的 submit 后
    // 立即 close）；先等它全部完成再销毁 allocator，否则 GPU 在使用已释放的
    // allocator 会导致 DXGI_ERROR_DEVICE_REMOVED。
    if (ctx->fenceValue > 0) {
        std::string err;
        if (!waitForFenceValue(ctx, ctx->fenceValue, 5000000000ULL, err)) {
            dbgLog("destroyCommandEncoder: wait FAILED: %s", err.c_str());
        }
    }
    // 若该 ctx 仍处于打开（begin 后从未 submit）状态：命令列表即将销毁，
    // 从未进入 GPU 执行队列，引用它的资源随之不再被任何命令列表引用，
    // 因此计入的打开计数一并释放；计数归零时顺带 flush 延迟删除对象。
    if (ctx->listOpen) {
        ctx->listOpen = 0;
        if (gOpenListCount > 0) --gOpenListCount;
        if (gOpenListCount == 0) flushPendingDeletes();
    }
    if (ctx->fenceEvent) CloseHandle(ctx->fenceEvent);
    delete ctx;
    dbgLogInfo("destroyCommandEncoder: done ctx=%p", (void*)ctx);
}

bool beginCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "beginCommandList: null ctx"; return false; }
    // P6 诊断：转储上一帧累积的验证错误（Close 成功后不打印不代表无错）。
    dumpInfoQueueMessages();
    DBG_LOG_DEBUG("beginCommandList: fenceValue=%llu", (unsigned long long)ctx->fenceValue);
    HRESULT hr = ctx->currentAllocator()->Reset();
    if (FAILED(hr)) { err = "beginCommandList: allocator Reset " + hrText(hr); return false; }
    hr = ctx->commandList->Reset(ctx->currentAllocator().Get(), nullptr);
    if (FAILED(hr)) { err = "beginCommandList: list Reset " + hrText(hr); return false; }
    ctx->listOpen = 1;
    ctx->inRenderPass = 0;
    ctx->colorTargetsWritten = false;  // 新 command list 从零开始追踪绘制状态
    ++gOpenListCount;  // 延迟销毁：登记打开计数，submit 完成前不释放资源
    // submit 阻塞等待 GPU 完成（见 submitCommandList），此处清空 resourceState
    // 后一切资源视为初始态是正确且保守的（D3D12 驱动会按实际 GPU 状态纠正）。
    ctx->resourceState.clear();
    // P20 fix：使用 4 个半区（ring x4）确保三帧飞行时安全交替。
    // fenceValue % 4 保证帧 N+2 写入的半区 ≠ 帧 N/N+1 GPU 正在读的半区。
    ctx->drawHeapSlotBase = (UINT)(ctx->fenceValue % kDrawHeapSections) * kDrawHeapPerFrame;
    ctx->nextDrawSlot = 0;
    // RTV/DSV 是 CPU-only 描述符堆：命令列表提交时驱动已捕获描述符内容，
    // 双 allocator + value-2 完成等待保证旧命令已结束，可每帧从 0 复用。
    gNextRtv = 0;
    gNextDsv = 0;
    ID3D12DescriptorHeap* drawHeaps[] = { gCtx.drawHeap.Get() };
    ctx->commandList->SetDescriptorHeaps(1, drawHeaps);
    return true;
}

bool endCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "endCommandList: null ctx"; return false; }
    if (!ctx->listOpen) return true;  // 幂等
    // 采样纹理在本 list 内被 transition 到 SHADER_RESOURCE 后，命令列表结束时
    // 必须回切 COMMON，保持"列表结束一切资源回 COMMON"的约定。否则下一
    // command list 在 beginCommandList 清空 resourceState 后按 COMMON 写 Before
    // 状态，与 GPU 上遗留的 SHADER_RESOURCE 错配 → 每帧验证 ERROR
    // （"Before state ... does not match ... preceding ResourceBarrier"）。
    for (auto it = ctx->resourceState.begin(); it != ctx->resourceState.end(); ) {
        if (it->second & (D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE
                         | D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE)) {
            if (it->second != D3D12_RESOURCE_STATE_COMMON)
                resourceBarrier(ctx->commandList.Get(), it->first, it->second,
                    D3D12_RESOURCE_STATE_COMMON);
            it = ctx->resourceState.erase(it);
        } else {
            ++it;
        }
    }
    // copy 操作（copyBufferToBuffer 等）会把源/目的缓冲区留在 COPY_SOURCE /
    // COPY_DEST；若不清理，下一帧 beginCommandList 清空 resourceState 后
    // 按 INITIAL（DEFAULT=COMMON）写 Before，GPU 实际仍在 COPY_SOURCE/COPY_DEST
    // → 验证 ERROR（与上方 SHADER_RESOURCE 清理是同一类问题）。
    for (auto& kv : ctx->resourceState) {
        if (kv.second == D3D12_RESOURCE_STATE_COPY_SOURCE
            || kv.second == D3D12_RESOURCE_STATE_COPY_DEST) {
            if (kv.second != D3D12_RESOURCE_STATE_COMMON)
                resourceBarrier(ctx->commandList.Get(), kv.first, kv.second,
                    D3D12_RESOURCE_STATE_COMMON);
            kv.second = D3D12_RESOURCE_STATE_COMMON;
        }
    }
    HRESULT hr = ctx->commandList->Close();
    if (FAILED(hr)) {
        // P6 诊断：Close 返回 E_INVALIDARG 通常是 debug layer 的验证错误
        // （UPLOAD heap 非法 transition、未绑定/越界描述符、root 参数未设置、
        // PSO 与 root signature 不匹配等）。附加 InfoQueue 最近消息定位具体
        // 非法调用——这是 Close 失败时唯一的确定性证据。
        err = "endCommandList: Close " + hrText(hr) + " — " + deviceStatusText();
        return false;
    }
    ctx->listOpen = 0;
    return true;
}

UINT64 submitCommandList(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "submitCommandList: null ctx"; return 0; }
    if (!endCommandList(ctx, err)) return 0;
    UINT64 value = ctx->fenceValue + 1;
    // P15 诊断：每 30 帧打印 submit 摘要（含 fence 值 + queueFence）
    if ((value % 30ULL) == 1) {
        dbgLog("submitCommandList: frame=%llu ctx=%p queueFence=%llu",
            (unsigned long long)value, (void*)ctx,
            (unsigned long long)gCtx.queueFenceValue);
    }
    DBG_LOG_DEBUG("submit: ExecuteCommandLists enter (v->%llu)", (unsigned long long)value);
    ID3D12CommandList* lists[] = { ctx->commandList.Get() };
    gCtx.queue->ExecuteCommandLists(1, lists);
    value = ++ctx->fenceValue;
    DBG_LOG_DEBUG("submit: executed, Signal v=%llu", (unsigned long long)value);
    if (FAILED(gCtx.queue->Signal(ctx->fence.Get(), value))) {
        err = "submitCommandList: Signal failed"; return 0;
    }
    // 全局队列 fence 同步推进：createFence token 的完成条件 = 下一次提交。
    // 官方语义（共享 encoder 的 submit index）下，fence 在任意 ctx 的下一次
    // ExecuteCommandLists 后完成；这里用设备级计数器复现。
    {
        UINT64 qv = ++gCtx.queueFenceValue;
        if (FAILED(gCtx.queue->Signal(gCtx.queueFence.Get(), qv))) {
            err = "submitCommandList: Signal(queue) failed"; return 0;
        }
    }
    // P18：记录 per-backbuffer fence 值，供 acquireSurface 按需同步（非阻塞）。
    // submit 本身不等待 GPU，改为在 acquireSurface 中检查重用的 back buffer
    // 是否仍被 GPU 使用（上次 blit 未完成时等待），避免渲染线程阻塞导致鼠标卡顿。
    Dx12Surface* s = getActiveSurface();
    if (s) {
        int idx = s->currentImageIndex;
        if (idx >= 0 && idx < (int)kSurfaceBufferCount) {
            if (s->surfaceFences.size() < (size_t)kSurfaceBufferCount)
                s->surfaceFences.resize(kSurfaceBufferCount, 0);
            s->surfaceFences[(size_t)idx] = gCtx.queueFenceValue;
        }
    }
    DBG_LOG_DEBUG("submit: done v=%llu", (unsigned long long)value);
    // 提交并同步等待完成：本命令列表已执行完，其引用的资源可安全释放。
    // 若所有打开的命令列表都已提交完成，则统一释放 pending 删除对象
    // （延迟销毁的 flush 点，对应官方 queueForDestroy 的 execute 时机）。
    if (gOpenListCount > 0) --gOpenListCount;
    if (gOpenListCount == 0) flushPendingDeletes();
    return value;
}

bool waitForFenceValue(CommandContext* ctx, UINT64 value, UINT64 timeoutNs,
    std::string& err) {
    if (!ctx) { err = "waitForFenceValue: null ctx"; return false; }
    UINT64 cv = ctx->fence->GetCompletedValue();
    DBG_LOG_DEBUG("waitFence: value=%llu completed=%llu", (unsigned long long)value, (unsigned long long)cv);
    if (cv >= value) return true;
    HRESULT hr = ctx->fence->SetEventOnCompletion(value, ctx->fenceEvent);
    if (FAILED(hr)) { err = "waitForFenceValue: SetEventOnCompletion " + hrText(hr); return false; }
    DWORD ms = (DWORD)((timeoutNs + 999999ULL) / 1000000ULL);
    if (WaitForSingleObject(ctx->fenceEvent, ms) != WAIT_OBJECT_0) {
        err = "waitForFenceValue: timed out after " + std::to_string(timeoutNs) + "ns";
        dbgLog("waitFence: TIMEOUT value=%llu completed=%llu",
            (unsigned long long)value,
            (unsigned long long)ctx->fence->GetCompletedValue());
        return false;
    }
    dbgLog("waitFence: OK value=%llu", (unsigned long long)value);
    return true;
}

UINT64 currentFenceValue(CommandContext* ctx) {
    return ctx ? ctx->fenceValue : 0;
}

// 全局队列 fence 等待（createFence token 的 awaitCompletion）：等待对象是
// 设备级 queueFence，目标值 = 创建时 queueFenceValue+1，下一次任意 ctx 的
// 提交后完成（官方"共享 encoder 的 submit index"语义）。每调用用独立的 event，
// 避免多线程并发等待同一事件互相干扰。
bool waitForQueueFenceValue(UINT64 value, UINT64 timeoutNs, std::string& err) {
    if (!gCtx.queueFence) {
        err = "waitForQueueFenceValue: queue fence not initialized";
        return false;
    }
    UINT64 cv = gCtx.queueFence->GetCompletedValue();
    dbgLog("waitQFence: value=%llu completed=%llu", (unsigned long long)value,
        (unsigned long long)cv);
    if (cv >= value) return true;
    HANDLE evt = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!evt) { err = "waitForQueueFenceValue: CreateEvent failed"; return false; }
    HRESULT hr = gCtx.queueFence->SetEventOnCompletion(value, evt);
    if (FAILED(hr)) {
        CloseHandle(evt);
        err = "waitForQueueFenceValue: SetEventOnCompletion " + hrText(hr);
        return false;
    }
    // 上限钳制：Java awaitCompletion(Long.MAX_VALUE) 换算后的纳秒数溢出 DWORD，
    // 直接 cast 会得到很小的毫秒数（把"永久等待"变成瞬时轮询）。钳到 ~49.7 天。
    UINT64 ms64 = (timeoutNs + 999999ULL) / 1000000ULL;
    DWORD ms = ms64 > 0xFFFFFFF0ULL ? 0xFFFFFFF0ULL : (DWORD)ms64;
    if (WaitForSingleObject(evt, ms) != WAIT_OBJECT_0) {
        CloseHandle(evt);
        err = "waitForQueueFenceValue: timed out after " + std::to_string(timeoutNs) + "ns";
        dbgLog("waitQFence: TIMEOUT value=%llu completed=%llu",
            (unsigned long long)value,
            (unsigned long long)gCtx.queueFence->GetCompletedValue());
        return false;
    }
    CloseHandle(evt);
    DBG_LOG_DEBUG("waitQFence: OK value=%llu", (unsigned long long)value);
    return true;
}

UINT64 currentQueueFenceValue() {
    return gCtx.queueFenceValue;
}

// 读回单个 timestamp 的通用实现：录制 EndQuery + ResolveQueryData 到 READBACK
// buffer，提交并等待，然后 map 读。list 在调用前必须已 Close。
bool resolveQuery(QueryPool* pool, int start, int count, long long* out,
    std::string& err) {
    if (!pool) { err = "resolveQuery: null pool"; return false; }
    if (start < 0 || count <= 0 || start + count > pool->size) {
        err = "resolveQuery: invalid range"; return false;
    }
    ComPtr<ID3D12CommandAllocator> alloc;
    ComPtr<ID3D12GraphicsCommandList> list;
    if (FAILED(gCtx.device->CreateCommandAllocator(
            D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&alloc))) ||
        FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&list)))) {
        err = "resolveQuery: create command list failed"; return false;
    }
    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_READBACK;
    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = (UINT64)count * sizeof(long long);
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    ComPtr<ID3D12Resource> readback;
    if (FAILED(gCtx.device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&readback)))) {
        err = "resolveQuery: readback alloc failed"; return false;
    }
    list->ResolveQueryData(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP,
        (UINT)start, (UINT)count, readback.Get(), 0);
    list->Close();
    if (!flushAndWait(list.Get(), err)) return false;
    void* p = nullptr;
    readback->Map(0, nullptr, &p);
    memcpy(out, p, (size_t)count * sizeof(long long));
    readback->Unmap(0, nullptr);
    return true;
}

long long getTimestampNow(CommandContext* ctx, std::string& err) {
    (void)ctx;
    if (!ensureDevice(err)) return 0;
    QueryPool* pool = createQueryPool(1, err);
    if (!pool) return 0;
    ComPtr<ID3D12CommandAllocator> alloc;
    ComPtr<ID3D12GraphicsCommandList> list;
    if (FAILED(gCtx.device->CreateCommandAllocator(
            D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&alloc))) ||
        FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
            alloc.Get(), nullptr, IID_PPV_ARGS(&list)))) {
        destroyQueryPool(pool); err = "getTimestampNow: create command list failed"; return 0;
    }
    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_READBACK;
    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    desc.Width = sizeof(long long);
    desc.Height = 1;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.SampleDesc.Count = 1;
    desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    ComPtr<ID3D12Resource> readback;
    if (FAILED(gCtx.device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&readback)))) {
        destroyQueryPool(pool); err = "getTimestampNow: readback alloc failed"; return 0;
    }
    list->EndQuery(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP, 0);
    list->ResolveQueryData(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP, 0, 1,
        readback.Get(), 0);
    list->Close();
    if (!flushAndWait(list.Get(), err)) { destroyQueryPool(pool); return 0; }
    long long ts = 0;
    void* p = nullptr;
    readback->Map(0, nullptr, &p);
    memcpy(&ts, p, sizeof(ts));
    readback->Unmap(0, nullptr);
    destroyQueryPool(pool);
    return ts;
}

// ---------------------------------------------------------------------------
// 命令录制（P3）：copy / clear / render pass / timestamp
// ---------------------------------------------------------------------------

bool copyBufferToBuffer(CommandContext* ctx, Dx12Object* src, long long srcOffset,
    Dx12Object* dst, long long dstOffset, long long size, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyBufferToBuffer: no open command list"; return false; }
    if (!src || !dst || src->kind != Dx12Object::Kind::Buffer ||
        dst->kind != Dx12Object::Kind::Buffer) {
        err = "copyBufferToBuffer: invalid buffer handle"; return false;
    }
    if (srcOffset < 0 || dstOffset < 0 || size < 0 ||
        srcOffset + size > src->size || dstOffset + size > dst->size) {
        err = "copyBufferToBuffer: range out of bounds"; return false;
    }
    transitionBufferTo(ctx, src, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionBufferTo(ctx, dst, D3D12_RESOURCE_STATE_COPY_DEST);
    ctx->commandList->CopyBufferRegion(dst->resource.Get(), (UINT64)dstOffset,
        src->resource.Get(), (UINT64)srcOffset, (UINT64)size);
    // P6 诊断：dump UPLOAD staging 内容（真实数据源头）。DEFAULT 堆 dst 在
    // submit 前读回是旧数据（假象），只有 staging（UPLOAD，CPU 可读）才有
    // Java 刚写入的真实顶点/UBO 数据。MVP 矩阵若全 0 → 顶点变换后全在原点
    // → 全部被裁剪 → 画面只剩 clear 色。
    if (src->heapType == D3D12_HEAP_TYPE_UPLOAD && size >= 16) {
        void* ptr = nullptr;
        if (SUCCEEDED(src->resource->Map(0, nullptr, &ptr))) {
            const float* f = (const float*)((const uint8_t*)ptr + srcOffset);
            int n = std::min((int)(size / 4), 16);
            std::string fs;
            for (int i = 0; i < n; ++i) {
                char b[32];
                snprintf(b, sizeof(b), " %.2f", f[i]);
                fs += b;
            }
            dbgLog("copyBuf: src=%p(UPLOAD) dst=%p off=%lld size=%lld floats=[%s ]",
                (void*)src, (void*)dst, (long long)srcOffset, (long long)size, fs.c_str());
            src->resource->Unmap(0, nullptr);
        }
    }
    return true;
}

bool clearColorTexture(CommandContext* ctx, Dx12Object* tex,
    float r, float g, float b, float a, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "clearColorTexture: no open command list"; return false; }
    if (!tex || tex->kind != Dx12Object::Kind::Texture) {
        err = "clearColorTexture: invalid texture"; return false;
    }
    if (!(tex->usage & 8)) {  // RENDER_ATTACHMENT
        err = "clearColorTexture: texture lacks RENDER_ATTACHMENT"; return false;
    }
    if (gNextRtv >= kRtvHeapSize) { err = "clearColorTexture: rtv heap exhausted"; return false; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.rtvHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)gNextRtv * gCtx.rtvInc;
    ++gNextRtv;
    gCtx.device->CreateRenderTargetView(tex->resource.Get(), nullptr, cpu);
    transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_RENDER_TARGET);
    const float color[4] = { r, g, b, a };
    ctx->commandList->ClearRenderTargetView(cpu, color, 0, nullptr);
    // P11：显式回切 COMMON（RENDER_TARGET 不会随命令列表完成 decay）。
    transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_COMMON);
    return true;
}

bool clearDepthTexture(CommandContext* ctx, Dx12Object* tex, double depth,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "clearDepthTexture: no open command list"; return false; }
    if (!tex || tex->kind != Dx12Object::Kind::Texture) {
        err = "clearDepthTexture: invalid texture"; return false;
    }
    if (gNextDsv >= kDsvHeapSize) { err = "clearDepthTexture: dsv heap exhausted"; return false; }
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.dsvHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += (SIZE_T)gNextDsv * gCtx.dsvInc;
    ++gNextDsv;
    gCtx.device->CreateDepthStencilView(tex->resource.Get(), nullptr, cpu);
    transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_DEPTH_WRITE);
    ctx->commandList->ClearDepthStencilView(cpu,
        D3D12_CLEAR_FLAG_DEPTH | D3D12_CLEAR_FLAG_STENCIL, (FLOAT)depth, 0, 0, nullptr);
    // P11：显式回切 COMMON（DEPTH_WRITE 不会随命令列表完成 decay）。
    transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_COMMON);
    return true;
}

bool copyBufferToTexture(CommandContext* ctx, Dx12Object* srcBuf, long long srcOffset,
    int srcWidth, int srcHeight, Dx12Object* dstTex, int mip, int layer,
    int dstX, int dstY, int w, int h, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyBufferToTexture: no open command list"; return false; }
    if (!srcBuf || srcBuf->kind != Dx12Object::Kind::Buffer ||
        !dstTex || dstTex->kind != Dx12Object::Kind::Texture) {
        err = "copyBufferToTexture: invalid handle"; return false;
    }
    if (w <= 0 || h <= 0 || srcWidth <= 0 || srcHeight <= 0) {
        err = "copyBufferToTexture: non-positive size"; return false;
    }
    UINT block = blockSizeFor(dstTex->dxgiFormat);
    UINT rowBytes = (UINT)w * block;
    UINT srcRowBytes = (UINT)srcWidth * block;  // 源 buffer 每行实际间距（texel*block）

    // D3D12 要求 footprint RowPitch 为 256 的倍数；Minecraft 的 CPU 侧数据是
    // 紧凑打包的，因此整块 RowPitch 未对齐时逐行拷贝以保证正确。
    constexpr UINT kPitchAlign = D3D12_TEXTURE_DATA_PITCH_ALIGNMENT;
    bool aligned = (srcRowBytes % kPitchAlign) == 0 && srcWidth == w;

    transitionBufferTo(ctx, srcBuf, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionTextureTo(ctx, dstTex, D3D12_RESOURCE_STATE_COPY_DEST);

    UINT subresource = (UINT)(mip + layer * dstTex->resource->GetDesc().MipLevels);
    if (dstTex->usage & 16) {  // CUBEMAP_COMPATIBLE：确认 6 个面是否逐一上传
        dbgLog("copyBufferToTexture: CUBE dst=%p mip=%d layer=%d subres=%u srcOff=%lld srcW=%d srcH=%d w=%d h=%d rowBytes=%u aligned=%d",
            (void*)dstTex, mip, layer, subresource, (long long)srcOffset,
            srcWidth, srcHeight, w, h, srcRowBytes, (int)aligned);
    }
    if (aligned) {
        D3D12_TEXTURE_COPY_LOCATION src{};
        src.pResource = srcBuf->resource.Get();
        src.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
        src.PlacedFootprint.Offset = (UINT64)srcOffset;
        src.PlacedFootprint.Footprint.Format = dstTex->dxgiFormat;
        src.PlacedFootprint.Footprint.Width = (UINT)w;
        src.PlacedFootprint.Footprint.Height = (UINT)h;
        src.PlacedFootprint.Footprint.Depth = 1;
        src.PlacedFootprint.Footprint.RowPitch = srcRowBytes;
        D3D12_TEXTURE_COPY_LOCATION dst{};
        dst.pResource = dstTex->resource.Get();
        dst.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
        dst.SubresourceIndex = subresource;
        ctx->commandList->CopyTextureRegion(&dst, dstX, dstY, 0, &src, nullptr);
    } else {
        // 逐行拷贝：每行 1 像素高，footprint 行距 = 源行距（可能未 256 对齐，
        // 部分驱动允许；这是紧凑数据上传的常见做法）。
        for (int row = 0; row < h; ++row) {
            D3D12_TEXTURE_COPY_LOCATION src{};
            src.pResource = srcBuf->resource.Get();
            src.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
            src.PlacedFootprint.Offset = (UINT64)srcOffset + (UINT64)row * srcRowBytes;
            src.PlacedFootprint.Footprint.Format = dstTex->dxgiFormat;
            src.PlacedFootprint.Footprint.Width = (UINT)w;
            src.PlacedFootprint.Footprint.Height = 1;
            src.PlacedFootprint.Footprint.Depth = 1;
            src.PlacedFootprint.Footprint.RowPitch = rowBytes;
            D3D12_TEXTURE_COPY_LOCATION dst{};
            dst.pResource = dstTex->resource.Get();
            dst.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
            dst.SubresourceIndex = subresource;
            ctx->commandList->CopyTextureRegion(&dst, dstX, dstY + row, 0, &src, nullptr);
        }
    }
    // P11：目标纹理显式回切 COMMON（显式进入的 COPY_DEST 不会随命令列表完成
    // decay；回切后下一 command list 的 Before=COMMON 假设才成立）。
    transitionTextureTo(ctx, dstTex, D3D12_RESOURCE_STATE_COMMON);
    return true;
}

bool copyTextureToBuffer(CommandContext* ctx, Dx12Object* srcTex, int mip, int layer,
    int srcX, int srcY, int w, int h, Dx12Object* dstBuf, long long dstOffset,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyTextureToBuffer: no open command list"; return false; }
    if (!srcTex || srcTex->kind != Dx12Object::Kind::Texture ||
        !dstBuf || dstBuf->kind != Dx12Object::Kind::Buffer) {
        err = "copyTextureToBuffer: invalid handle"; return false;
    }
    if (w <= 0 || h <= 0) { err = "copyTextureToBuffer: non-positive size"; return false; }
    UINT block = blockSizeFor(srcTex->dxgiFormat);
    UINT rowBytes = (UINT)w * block;

    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionBufferTo(ctx, dstBuf, D3D12_RESOURCE_STATE_COPY_DEST);

    UINT subresource = (UINT)(mip + layer * srcTex->resource->GetDesc().MipLevels);
    // 客户端（Minecraft）按紧凑行距访问读回数据，且读回 buffer 只分配了
    // 紧凑大小（rowBytes*h）。D3D12 要求 footprint RowPitch 为 256 的倍数，
    // 因此行距未对齐时逐行紧凑拷贝（每行 Height=1）。
    constexpr UINT kPitchAlign = D3D12_TEXTURE_DATA_PITCH_ALIGNMENT;
    bool aligned = (rowBytes % kPitchAlign) == 0;
    if (dstOffset + (UINT64)rowBytes * h > (UINT64)dstBuf->size) {
        err = "copyTextureToBuffer: destination buffer too small";
        return false;
    }

    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = srcTex->resource.Get();
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = subresource;
    if (aligned) {
        D3D12_TEXTURE_COPY_LOCATION dst{};
        dst.pResource = dstBuf->resource.Get();
        dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
        dst.PlacedFootprint.Offset = (UINT64)dstOffset;
        dst.PlacedFootprint.Footprint.Format = srcTex->dxgiFormat;
        dst.PlacedFootprint.Footprint.Width = (UINT)w;
        dst.PlacedFootprint.Footprint.Height = (UINT)h;
        dst.PlacedFootprint.Footprint.Depth = 1;
        dst.PlacedFootprint.Footprint.RowPitch = rowBytes;
        D3D12_BOX srcBox{ (UINT)srcX, (UINT)srcY, 0, (UINT)(srcX + w), (UINT)(srcY + h), 1 };
        ctx->commandList->CopyTextureRegion(&dst, 0, 0, 0, &src, &srcBox);
    } else {
        // 逐行拷贝：目标 buffer 紧凑布局，RowPitch=rowBytes（每行 1 像素高）。
        for (int row = 0; row < h; ++row) {
            D3D12_TEXTURE_COPY_LOCATION dst{};
            dst.pResource = dstBuf->resource.Get();
            dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
            dst.PlacedFootprint.Offset = (UINT64)dstOffset + (UINT64)row * rowBytes;
            dst.PlacedFootprint.Footprint.Format = srcTex->dxgiFormat;
            dst.PlacedFootprint.Footprint.Width = (UINT)w;
            dst.PlacedFootprint.Footprint.Height = 1;
            dst.PlacedFootprint.Footprint.Depth = 1;
            dst.PlacedFootprint.Footprint.RowPitch = rowBytes;
            D3D12_BOX srcBox{ (UINT)srcX, (UINT)(srcY + row), 0,
                (UINT)(srcX + w), (UINT)(srcY + row + 1), 1 };
            ctx->commandList->CopyTextureRegion(&dst, 0, 0, 0, &src, &srcBox);
        }
    }
    // P11：源纹理显式回切 COMMON（显式进入的 COPY_SOURCE 不会随命令列表完成
    // decay；回切后下一 command list 的 Before=COMMON 假设才成立）。
    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COMMON);
    return true;
}

bool copyTextureToTexture(CommandContext* ctx, Dx12Object* srcTex, Dx12Object* dstTex,
    int mip, int layer, int srcX, int srcY, int dstX, int dstY, int w, int h,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "copyTextureToTexture: no open command list"; return false; }
    if (!srcTex || srcTex->kind != Dx12Object::Kind::Texture ||
        !dstTex || dstTex->kind != Dx12Object::Kind::Texture) {
        err = "copyTextureToTexture: invalid handle"; return false;
    }
    if (w <= 0 || h <= 0) { err = "copyTextureToTexture: non-positive size"; return false; }
    if (srcTex->dxgiFormat != dstTex->dxgiFormat) {
        err = "copyTextureToTexture: format mismatch"; return false;
    }
    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionTextureTo(ctx, dstTex, D3D12_RESOURCE_STATE_COPY_DEST);

    UINT srcSub = (UINT)(mip + layer * srcTex->resource->GetDesc().MipLevels);
    UINT dstSub = (UINT)(mip + layer * dstTex->resource->GetDesc().MipLevels);
    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = srcTex->resource.Get();
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = srcSub;
    D3D12_TEXTURE_COPY_LOCATION dst{};
    dst.pResource = dstTex->resource.Get();
    dst.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dst.SubresourceIndex = dstSub;
    D3D12_BOX srcBox{ (UINT)srcX, (UINT)srcY, 0, (UINT)(srcX + w), (UINT)(srcY + h), 1 };
    ctx->commandList->CopyTextureRegion(&dst, dstX, dstY, 0, &src, &srcBox);
    // P11：src/dst 显式回切 COMMON（显式进入的 COPY_SOURCE/COPY_DEST 不会随
    // 命令列表完成 decay；回切后下一 command list 的 Before=COMMON 假设才成立）。
    transitionTextureTo(ctx, srcTex, D3D12_RESOURCE_STATE_COMMON);
    transitionTextureTo(ctx, dstTex, D3D12_RESOURCE_STATE_COMMON);
    return true;
}

bool beginRenderPass(CommandContext* ctx, Dx12Object* const* colorViews,
    int colorCount, const int* colorClearFlags, const float* clearColors,
    Dx12Object* depthView, int depthClearFlag, double depthClearValue,
    int x, int y, int w, int h, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "beginRenderPass: no open command list"; return false; }
    if (ctx->inRenderPass) { err = "beginRenderPass: render pass already open"; return false; }
    if (colorCount < 0) { err = "beginRenderPass: negative color count"; return false; }

    // P11：本 pass 的附件在 endRenderPass 时需显式回切 COMMON（见 endRenderPass），
    // 先清空上次记录（防御：上次 pass 未正常 end）。
    ctx->activeColorTargets.clear();
    ctx->activeColorTargetsTouched.clear();
    ctx->activeDepthTarget = nullptr;

    std::vector<D3D12_CPU_DESCRIPTOR_HANDLE> rtvs;
    rtvs.reserve((size_t)colorCount);
    for (int i = 0; i < colorCount; ++i) {
        Dx12Object* tex = colorViews[i];
        if (!tex) continue;  // withUnusedColorAttachment 占位
        if (tex->kind != Dx12Object::Kind::Texture || !(tex->usage & 8)) {
            err = "beginRenderPass: invalid color attachment (needs RENDER_ATTACHMENT)"; return false;
        }
        if (gNextRtv >= kRtvHeapSize) { err = "beginRenderPass: rtv heap exhausted"; return false; }
        D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.rtvHeap->GetCPUDescriptorHandleForHeapStart();
        cpu.ptr += (SIZE_T)gNextRtv * gCtx.rtvInc;
        ++gNextRtv;
        gCtx.device->CreateRenderTargetView(tex->resource.Get(), nullptr, cpu);
        rtvs.push_back(cpu);
        ctx->activeColorTargets.push_back(tex);
        dbgLog("beginRenderPass color[%d] tex=%p rtv=%p dims=%ux%u fmt=%d",
            i, (void*)tex, (void*)cpu.ptr, (UINT)tex->resource->GetDesc().Width,
            (UINT)tex->resource->GetDesc().Height, (int)tex->dxgiFormat);
        transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_RENDER_TARGET);
        if (colorClearFlags && colorClearFlags[i] && clearColors) {
            const float* c = clearColors + i * 4;
            ctx->commandList->ClearRenderTargetView(cpu, c, 0, nullptr);
            // P6 诊断：每 60 帧打印一次 clear 颜色（确认画面底色=clear 值）。
            static int rpDbg = 0;
            if ((++rpDbg % 60) == 1) {
                dbgLog("beginRenderPass clear[%d] = (%.3f, %.3f, %.3f, %.3f)",
                    i, (double)c[0], (double)c[1], (double)c[2], (double)c[3]);
            }
        }
    }

    D3D12_CPU_DESCRIPTOR_HANDLE dsv{};
    bool hasDsv = false;
    if (depthView) {
        if (depthView->kind != Dx12Object::Kind::Texture) {
            err = "beginRenderPass: invalid depth attachment"; return false;
        }
        if (gNextDsv >= kDsvHeapSize) { err = "beginRenderPass: dsv heap exhausted"; return false; }
        D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.dsvHeap->GetCPUDescriptorHandleForHeapStart();
        cpu.ptr += (SIZE_T)gNextDsv * gCtx.dsvInc;
        ++gNextDsv;
        gCtx.device->CreateDepthStencilView(depthView->resource.Get(), nullptr, cpu);
        dsv = cpu;
        hasDsv = true;
        ctx->activeDepthTarget = depthView;
        transitionTextureTo(ctx, depthView, D3D12_RESOURCE_STATE_DEPTH_WRITE);
        // reverse-Z 修复：Minecraft 26.x 使用反向深度（depthFunc=GREATER_EQUAL），
        // 深度 clear 值为 0.0（远平面）。vanilla 在创建主渲染 pass 前已通过
        // clearDepthTexture(..., 0.0) pre-clear（同一 command encoder，顺序有保证），
        // 主 pass 的 depth attachment clearValue 为 empty → depthClearFlag=0（LOAD）。
        // 原 P21 在 LOAD 时无条件回退 clear=1.0，覆盖了 pre-clear 的 0.0，导致
        // GREATER_EQUAL 深度测试丢弃几乎所有片元 → 全黑屏。
        // 正确行为：LOAD 不 clear（保持 pre-clear 值），仅显式 CLEAR 时 clear。
        if (depthClearFlag) {
            ctx->commandList->ClearDepthStencilView(cpu,
                D3D12_CLEAR_FLAG_DEPTH | D3D12_CLEAR_FLAG_STENCIL,
                (FLOAT)depthClearValue, 0, 0, nullptr);
            dbgLog("beginRenderPass depth clear=explicit val=%.3f", (FLOAT)depthClearValue);
        } else {
            dbgLog("beginRenderPass depth load (no clear)");
        }
    }

    ctx->commandList->OMSetRenderTargets((UINT)rtvs.size(),
        rtvs.empty() ? nullptr : rtvs.data(), false, hasDsv ? &dsv : nullptr);
    D3D12_VIEWPORT vp{ (FLOAT)x, (FLOAT)y, (FLOAT)w, (FLOAT)h, 0.0f, 1.0f };
    ctx->commandList->RSSetViewports(1, &vp);
    D3D12_RECT scissor{ x, y, x + w, y + h };
    ctx->commandList->RSSetScissorRects(1, &scissor);
    ctx->inRenderPass = 1;
    // P6 诊断：确认渲染 pass 绑定的附件（blit 源纹理应出现在 color[0]）。
    dbgLog("beginRenderPass: ctx=%p colorCount=%d area=%d,%d %dx%d depth=%s",
        (void*)ctx, colorCount, x, y, w, h, depthView ? "yes" : "no");
    for (int i = 0; i < colorCount; ++i) {
        if (colorViews[i]) {
            auto* tex = colorViews[i];
            auto desc = tex->resource->GetDesc();
            const float* cc = clearColors ? (clearColors + i * 4) : nullptr;
            dbgLog("beginRenderPass color[%d] tex=%p dims=%ux%u fmt=%d clear=(%.3f,%.3f,%.3f,%.3f)",
                i, (void*)tex, (UINT)desc.Width, (UINT)desc.Height,
                (int)tex->dxgiFormat,
                cc ? (double)cc[0] : 0.0, cc ? (double)cc[1] : 0.0,
                cc ? (double)cc[2] : 0.0, cc ? (double)cc[3] : 0.0);
            // BUG-01 fix：diag loop 只打印日志，不再重复 push_back（主循环 L1701 已添加）。
        }
    }
    return true;
}

bool endRenderPass(CommandContext* ctx, std::string& err) {
    if (!ctx) { err = "endRenderPass: null ctx"; return false; }
    if (!ctx->inRenderPass) return true;  // 幂等
    DBG_LOG_DEBUG("endRenderPass: ctx=%p", (void*)ctx);
    // P11 修复：附件必须显式回切 COMMON。D3D12 的 RENDER_TARGET/DEPTH_WRITE 属
    // "非可提升状态"，命令列表执行完成时【不会】隐式 decay 回 COMMON（只有
    // COPY_SOURCE/COPY_DEST/UAV 等可提升状态才会）。若此处不显式回切，下一
    // command list 的 beginRenderPass/blit 会按 COMMON 写 barrier 的 Before 状态，
    // 与资源实际状态错配 → 每帧验证 ERROR（"Before state ... does not match"）→
    // GPU 状态错乱 → TDR 冻结（游戏挂死，启动器同 GPU 渲染也卡死）。
    // transitionTextureTo 按本 list 已跟踪状态回切（进入时为 RENDER_TARGET/
    // DEPTH_WRITE），from==to 时自动跳过，幂等。
    for (Dx12Object* tex : ctx->activeColorTargets) {
        if (tex) transitionTextureTo(ctx, tex, D3D12_RESOURCE_STATE_COMMON);
    }
    if (ctx->activeDepthTarget) {
        transitionTextureTo(ctx, ctx->activeDepthTarget, D3D12_RESOURCE_STATE_COMMON);
    }
    // P6 诊断：记录 endRenderPass 前绑定的颜色附件，便于确认渲染目标正确性。
    if (ctx->activeColorTargets.size() > 0) {
        for (Dx12Object* tex : ctx->activeColorTargets) {
            if (tex && tex->resource) {
                auto desc = tex->resource->GetDesc();
                dbgLog("endRenderPass colorAttach tex=%p dims=%ux%u fmt=%d touched=%d",
                    (void*)tex, (UINT)desc.Width, (UINT)desc.Height,
                    (int)tex->dxgiFormat,
                    (int)std::distance(ctx->activeColorTargets.begin(),
                        std::find(ctx->activeColorTargets.begin(), ctx->activeColorTargets.end(), tex)));
            }
        }
    }
    ctx->activeColorTargets.clear();
    ctx->activeColorTargetsTouched.clear();
    ctx->activeDepthTarget = nullptr;
    ctx->inRenderPass = 0;
    return true;
}

// ---------------------------------------------------------------------------
// Timestamp query pool（P3）
// ---------------------------------------------------------------------------

QueryPool* createQueryPool(int size, std::string& err) {
    if (!ensureDevice(err)) return nullptr;
    if (size <= 0) { err = "createQueryPool: size must be positive"; return nullptr; }
    D3D12_QUERY_HEAP_DESC desc{};
    desc.Type = D3D12_QUERY_HEAP_TYPE_TIMESTAMP;
    desc.Count = (UINT)size;
    auto pool = std::make_unique<QueryPool>();
    if (FAILED(gCtx.device->CreateQueryHeap(&desc, IID_PPV_ARGS(&pool->heap)))) {
        err = "createQueryPool: CreateQueryHeap failed"; return nullptr;
    }
    pool->size = size;
    return pool.release();
}

void destroyQueryPool(QueryPool* pool) {
    delete pool;
}

bool writeTimestampToPool(CommandContext* ctx, QueryPool* pool, int index,
    std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "writeTimestampToPool: no open command list"; return false; }
    if (!pool || index < 0 || index >= pool->size) {
        err = "writeTimestampToPool: invalid index"; return false;
    }
    ctx->commandList->EndQuery(pool->heap.Get(), D3D12_QUERY_TYPE_TIMESTAMP, (UINT)index);
    return true;
}

bool readQueryValue(QueryPool* pool, int index, long long& out, std::string& err) {
    return readQueryValues(pool, index, 1, &out, err);
}

bool readQueryValues(QueryPool* pool, int start, int count, long long* out,
    std::string& err) {
    return resolveQuery(pool, start, count, out, err);
}

unsigned long long getTimestampFrequency() {
    return gCtx.timestampFrequency;
}

// ---------------------------------------------------------------------------
// P4: 图形管线（D3DCompile + root signature + 双 PSO）
// ---------------------------------------------------------------------------

namespace {

// MC BlendFactor ordinal -> D3D12_BLEND（枚举声明顺序）
D3D12_BLEND toD3d12BlendFactor(uint8_t f) {
    switch (f) {
        case 0: return D3D12_BLEND_BLEND_FACTOR;      // CONSTANT_ALPHA
        case 1: return D3D12_BLEND_BLEND_FACTOR;      // CONSTANT_COLOR
        case 2: return D3D12_BLEND_DEST_ALPHA;        // DST_ALPHA
        case 3: return D3D12_BLEND_DEST_COLOR;        // DST_COLOR
        case 4: return D3D12_BLEND_ONE;               // ONE
        case 5: return D3D12_BLEND_INV_BLEND_FACTOR;  // ONE_MINUS_CONSTANT_ALPHA
        case 6: return D3D12_BLEND_INV_BLEND_FACTOR;  // ONE_MINUS_CONSTANT_COLOR
        case 7: return D3D12_BLEND_INV_DEST_ALPHA;    // ONE_MINUS_DST_ALPHA
        case 8: return D3D12_BLEND_INV_DEST_COLOR;    // ONE_MINUS_DST_COLOR
        case 9: return D3D12_BLEND_INV_SRC_ALPHA;     // ONE_MINUS_SRC_ALPHA
        case 10: return D3D12_BLEND_INV_SRC_COLOR;    // ONE_MINUS_SRC_COLOR
        case 11: return D3D12_BLEND_SRC_ALPHA;        // SRC_ALPHA
        case 12: return D3D12_BLEND_SRC_ALPHA_SAT;    // SRC_ALPHA_SATURATE
        case 13: return D3D12_BLEND_SRC_COLOR;        // SRC_COLOR
        case 14: return D3D12_BLEND_ZERO;             // ZERO
        default: return D3D12_BLEND_ONE;
    }
}

// MC BlendOp ordinal -> D3D12_BLEND_OP
D3D12_BLEND_OP toD3d12BlendOp(uint8_t op) {
    switch (op) {
        case 0: return D3D12_BLEND_OP_ADD;            // ADD
        case 1: return D3D12_BLEND_OP_SUBTRACT;       // SUBTRACT
        case 2: return D3D12_BLEND_OP_REV_SUBTRACT;   // REVERSE_SUBTRACT
        case 3: return D3D12_BLEND_OP_MIN;            // MIN
        case 4: return D3D12_BLEND_OP_MAX;            // MAX
        default: return D3D12_BLEND_OP_ADD;
    }
}

// MC CompareOp ordinal -> D3D12_COMPARISON_FUNC
D3D12_COMPARISON_FUNC toD3d12Compare(uint8_t c) {
    switch (c) {
        case 0: return D3D12_COMPARISON_FUNC_ALWAYS;         // ALWAYS_PASS
        case 1: return D3D12_COMPARISON_FUNC_LESS;           // LESS_THAN
        case 2: return D3D12_COMPARISON_FUNC_LESS_EQUAL;     // LESS_THAN_OR_EQUAL
        case 3: return D3D12_COMPARISON_FUNC_EQUAL;          // EQUAL
        case 4: return D3D12_COMPARISON_FUNC_NOT_EQUAL;      // NOT_EQUAL
        case 5: return D3D12_COMPARISON_FUNC_GREATER_EQUAL;  // GREATER_THAN_OR_EQUAL
        case 6: return D3D12_COMPARISON_FUNC_GREATER;        // GREATER_THAN
        case 7: return D3D12_COMPARISON_FUNC_NEVER;          // NEVER_PASS
        default: return D3D12_COMPARISON_FUNC_ALWAYS;
    }
}

// MC PrimitiveTopology ordinal -> D3D12_PRIMITIVE_TOPOLOGY_TYPE
D3D12_PRIMITIVE_TOPOLOGY_TYPE toTopologyType(int t) {
    switch (t) {
        case 0: case 1: case 2: return D3D12_PRIMITIVE_TOPOLOGY_TYPE_LINE;   // LINES/DEBUG_LINES/DEBUG_LINE_STRIP
        case 3: return D3D12_PRIMITIVE_TOPOLOGY_TYPE_POINT;                  // POINTS
        default: return D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;              // TRIANGLES/STRIP/FAN/QUADS
    }
}

// MC PrimitiveTopology ordinal -> 命令列表级 D3D_PRIMITIVE_TOPOLOGY。
// D3D12 命令列表初始 topology 是 UNDEFINED，任何 draw 前必须 IASetPrimitiveTopology，
// 否则 GPU 丢弃全部图元（纯 clear 色黑屏根因）。QUADS 无原生支持，回退 TRIANGLELIST。
D3D12_PRIMITIVE_TOPOLOGY toPrimitiveTopology(int t) {
    switch (t) {
        case 0: case 1: return D3D_PRIMITIVE_TOPOLOGY_LINELIST;     // LINES/DEBUG_LINES
        case 2: return D3D_PRIMITIVE_TOPOLOGY_LINESTRIP;            // DEBUG_LINE_STRIP
        case 3: return D3D_PRIMITIVE_TOPOLOGY_POINTLIST;            // POINTS
        case 5: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP;        // TRIANGLE_STRIP
        case 6: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLEFAN;          // TRIANGLE_FAN
        case 7: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;         // QUADS（不支持，回退）
        default: return D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;        // TRIANGLES 等
    }
}

bool compileShaderBytecode(const std::vector<uint8_t>& src, const char* stageName,
    const char* target, ComPtr<ID3DBlob>& out, std::string& err) {
    ComPtr<ID3DBlob> errBlob;
    HRESULT hr = D3DCompile(src.data(), src.size(), stageName, nullptr, nullptr,
        "main", target, 0, 0, &out, &errBlob);
    if (FAILED(hr)) {
        std::string msg;
        if (errBlob && errBlob->GetBufferSize() > 0) {
            const char* m = static_cast<const char*>(errBlob->GetBufferPointer());
            msg.assign(m, errBlob->GetBufferSize());
        }
        err = std::string("D3DCompile(") + target + ") hr=0x" + hrText(hr)
            + (msg.empty() ? "" : "\n" + msg);
        return false;
    }
    return true;
}

}  // namespace

Dx12Pipeline* createGraphicsPipeline(const PipelineDesc& desc, std::string& err) {
    if (!ensureDevice(err)) return nullptr;

    std::vector<uint8_t> vsBytes = desc.vsBytes;
    std::vector<uint8_t> psBytes = desc.psBytes;
    // P7 诊断：打印前 2 个真实管线 HLSL（stderr 输出，帮助排查着色器数据流问题）
    {
        static int hlslDump = 0;
        if (hlslDump < 2) {
            ++hlslDump;
            std::string vsStr((const char*)vsBytes.data(), vsBytes.size());
            std::string psStr((const char*)psBytes.data(), psBytes.size());
            std::fprintf(stderr,
                "[dx12] === HLSL #%d vs (%zuB) ===\n%s\n"
                "[dx12] === HLSL #%d ps (%zuB) ===\n%s\n",
                hlslDump, vsBytes.size(), vsStr.c_str(),
                hlslDump, psBytes.size(), psStr.c_str());
        }
    }
    // 1) HLSL -> DXBC（vs_5_1 / ps_5_1，入口 main）
    ComPtr<ID3DBlob> vsBlob, psBlob;
    if (!compileShaderBytecode(vsBytes, "vertex", "vs_5_1", vsBlob, err)) return nullptr;
    if (!compileShaderBytecode(psBytes, "fragment", "ps_5_1", psBlob, err)) return nullptr;

    // 2) root signature：单 descriptor table（CBV/SRV 混合，register=条目序号）
    //    + static sampler（仅 SAMPLED_IMAGE 条目，register=同一序号）
    std::vector<D3D12_DESCRIPTOR_RANGE> ranges;
    std::vector<D3D12_STATIC_SAMPLER_DESC> staticSamplers;
    ranges.reserve(desc.bindings.size());
    for (const PipelineDesc::Binding& b : desc.bindings) {
        D3D12_DESCRIPTOR_RANGE r{};
        r.NumDescriptors = 1;
        r.RegisterSpace = 0;
        r.OffsetInDescriptorsFromTableStart = D3D12_DESCRIPTOR_RANGE_OFFSET_APPEND;
        r.BaseShaderRegister = b.reg;
        r.RangeType = (b.type == 0) ? D3D12_DESCRIPTOR_RANGE_TYPE_CBV
                                    : D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
        ranges.push_back(r);
        if (b.type == 1) {
            D3D12_STATIC_SAMPLER_DESC s{};
            s.Filter = D3D12_FILTER_MIN_MAG_MIP_LINEAR;
            s.AddressU = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
            s.AddressV = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
            s.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
            s.MipLODBias = 0.0f;
            s.MaxAnisotropy = 1;
            s.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
            s.BorderColor = D3D12_STATIC_BORDER_COLOR_OPAQUE_BLACK;
            s.MinLOD = 0.0f;
            s.MaxLOD = D3D12_FLOAT32_MAX;
            s.ShaderRegister = b.reg;
            s.RegisterSpace = 0;
            s.ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
            staticSamplers.push_back(s);
        }
    }
    D3D12_ROOT_DESCRIPTOR_TABLE table{};
    table.NumDescriptorRanges = (UINT)ranges.size();
    table.pDescriptorRanges = ranges.empty() ? nullptr : ranges.data();
    D3D12_ROOT_PARAMETER param{};
    param.ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    param.DescriptorTable = table;
    param.ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;
    D3D12_ROOT_SIGNATURE_DESC rsDesc{};
    rsDesc.NumParameters = 1;
    rsDesc.pParameters = &param;
    rsDesc.NumStaticSamplers = (UINT)staticSamplers.size();
    rsDesc.pStaticSamplers = staticSamplers.empty() ? nullptr : staticSamplers.data();
    rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;

    ComPtr<ID3DBlob> rsBlob, rsErr;
    HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1_0,
        &rsBlob, &rsErr);
    if (FAILED(hr)) {
        err = "createGraphicsPipeline: D3D12SerializeRootSignature hr=0x" + hrText(hr);
        return nullptr;
    }
    ComPtr<ID3D12RootSignature> rootSig;
    hr = gCtx.device->CreateRootSignature(0, rsBlob->GetBufferPointer(),
        rsBlob->GetBufferSize(), IID_PPV_ARGS(&rootSig));
    if (FAILED(hr)) {
        err = "createGraphicsPipeline: CreateRootSignature hr=0x" + hrText(hr);
        return nullptr;
    }

    // 3) 输入布局：语义名称来自 Java 侧 HLSL 解析；空串时回退到 TEXCOORD<location>
    //    D3D12 要求 SemanticName 不能以数字结尾，数字必须放在 SemanticIndex 字段。
    //    注意：SemanticName 是 const char*，必须指向持久化内存，不能指向局部 string 的 c_str()。
    std::vector<D3D12_INPUT_ELEMENT_DESC> inputLayout;
    inputLayout.reserve(desc.inputElements.size());
    std::vector<std::string> semanticStore;  // 持久化存储，防止 c_str() 悬空
    semanticStore.reserve(desc.inputElements.size());
    std::string inputDescStr;
    for (const PipelineDesc::InputElement& el : desc.inputElements) {
        D3D12_INPUT_ELEMENT_DESC ie{};
        // 解析 semantic：如 "TEXCOORD0" → name="TEXCOORD", index=0；"COLOR" → name="COLOR", index=0
        std::string baseName;
        UINT semanticIndex = 0;
        if (!el.semanticName.empty()) {
            const std::string& sn = el.semanticName;
            size_t lastNonDigit = sn.find_last_not_of("0123456789");
            if (lastNonDigit == std::string::npos) {
                // 全是数字（如 "0"），视为 index=0，name=""
                baseName = "";
            } else {
                baseName = sn.substr(0, lastNonDigit + 1);
                if (lastNonDigit + 1 < sn.size()) {
                    semanticIndex = (UINT)std::stoi(sn.substr(lastNonDigit + 1));
                }
            }
        } else {
            baseName = "TEXCOORD";
            semanticIndex = (UINT)el.location;
        }
        semanticStore.push_back(baseName);
        ie.SemanticName = semanticStore.back().c_str();
        ie.SemanticIndex = semanticIndex;
        ie.Format = toDxgiVertexFormat(el.format);
        ie.InputSlot = (UINT)el.binding;
        ie.AlignedByteOffset = (UINT)el.offset;
        if (el.stepRate > 0) {
            ie.InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_INSTANCE_DATA;
            ie.InstanceDataStepRate = (UINT)el.stepRate;
        } else {
            ie.InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
            ie.InstanceDataStepRate = 0;
        }
        char buf[128];
        snprintf(buf, sizeof(buf), "  loc=%d bind=%d fmt=%d off=%d stride=%d step=%d semantic=%s idx=%u\n",
            el.location, el.binding, el.format, el.offset, el.stride, el.stepRate,
            el.semanticName.empty() ? "(fallback TEXCOORD)" : el.semanticName.c_str(),
            semanticIndex);
        inputDescStr += buf;
        inputLayout.push_back(ie);
    }
    // 修正 stride：Java 侧 getVertexSize() 可能不准确，按 DXGI_FORMAT 实际字节数计算 end-offset。
    auto dxgiByteSize = [](DXGI_FORMAT fmt) -> UINT {
        switch (fmt) {
            case DXGI_FORMAT_R32G32B32A32_FLOAT: return 16u;
            case DXGI_FORMAT_R32G32B32_FLOAT:    return 12u;
            case DXGI_FORMAT_R32G32_FLOAT:       return 8u;
            case DXGI_FORMAT_R32_FLOAT:          return 4u;
            case DXGI_FORMAT_R16G16B16A16_UNORM: return 8u;
            case DXGI_FORMAT_R16G16B16A16_SNORM:return 8u;
            case DXGI_FORMAT_R16G16_FLOAT:       return 8u;
            case DXGI_FORMAT_R16_UNORM:          return 2u;
            case DXGI_FORMAT_R16_SNORM:          return 2u;
            case DXGI_FORMAT_R16_FLOAT:          return 2u;
            case DXGI_FORMAT_R8G8B8A8_UNORM:     return 4u;
            case DXGI_FORMAT_R8G8B8A8_SNORM:     return 4u;
            case DXGI_FORMAT_R8G8B8A8_UINT:      return 4u;
            case DXGI_FORMAT_R8G8B8A8_SINT:      return 4u;
            case DXGI_FORMAT_R8G8_UNORM:         return 2u;
            case DXGI_FORMAT_R8G8_SNORM:         return 2u;
            case DXGI_FORMAT_R8_UNORM:           return 1u;
            case DXGI_FORMAT_R8_SNORM:           return 1u;
            case DXGI_FORMAT_R8_UINT:            return 1u;
            case DXGI_FORMAT_R8_SINT:            return 1u;
            case DXGI_FORMAT_R10G10B10A2_UNORM:  return 4u;
            case DXGI_FORMAT_R10G10B10A2_UINT:   return 4u;
            case DXGI_FORMAT_R11G11B10_FLOAT:    return 4u;
            default:                             return 4u;  // fallback
        }
    };
    UINT correctedStride = 0;
    for (const PipelineDesc::InputElement& el : desc.inputElements) {
        DXGI_FORMAT dfmt = toDxgiVertexFormat(el.format);
        UINT sz = dxgiByteSize(dfmt);
        UINT end = (UINT)el.offset + sz;
        if (end > correctedStride) correctedStride = end;
    }
    if (correctedStride > 0) {
        // 重新构建 inputLayout，语义名称从 semanticStore 恢复（避免悬空指针）。
        // 先清空 semanticStore 以便重新填充。
        semanticStore.clear();
        semanticStore.reserve(desc.inputElements.size());
        inputDescStr.clear();
        inputLayout.clear();
        for (size_t k = 0; k < desc.inputElements.size(); ++k) {
            const PipelineDesc::InputElement& el = desc.inputElements[k];
            D3D12_INPUT_ELEMENT_DESC ie{};
            std::string baseName;
            UINT semanticIndex = 0;
            if (!el.semanticName.empty()) {
                const std::string& sn = el.semanticName;
                size_t lastNonDigit = sn.find_last_not_of("0123456789");
                if (lastNonDigit == std::string::npos) {
                    baseName = "";
                } else {
                    baseName = sn.substr(0, lastNonDigit + 1);
                    if (lastNonDigit + 1 < sn.size()) {
                        semanticIndex = (UINT)std::stoi(sn.substr(lastNonDigit + 1));
                    }
                }
            } else {
                baseName = "TEXCOORD";
                semanticIndex = (UINT)el.location;
            }
            semanticStore.push_back(baseName);
            ie.SemanticName = semanticStore.back().c_str();
            ie.SemanticIndex = semanticIndex;
            ie.Format = toDxgiVertexFormat(el.format);
            ie.InputSlot = (UINT)el.binding;
            ie.AlignedByteOffset = (UINT)el.offset;
            if (el.stepRate > 0) {
                ie.InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_INSTANCE_DATA;
                ie.InstanceDataStepRate = (UINT)el.stepRate;
            } else {
                ie.InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
                ie.InstanceDataStepRate = 0;
            }
            char buf[128];
            snprintf(buf, sizeof(buf), "  loc=%d bind=%d fmt=%d off=%d stride=%d step=%d semantic=%s idx=%u\n",
                el.location, el.binding, el.format, el.offset, correctedStride, el.stepRate,
                el.semanticName.empty() ? "(fallback TEXCOORD)" : el.semanticName.c_str(),
                semanticIndex);
            inputDescStr += buf;
            inputLayout.push_back(ie);
        }
    }
    dbgLogInfo("createGraphicsPipeline: inputElements=%zu\n%s",
        desc.inputElements.size(), inputDescStr.c_str());

    // 4) PSO 共享状态：混合 + 光栅化
    D3D12_BLEND_DESC blend{};
    blend.AlphaToCoverageEnable = FALSE;
    blend.IndependentBlendEnable = TRUE;
    int numRT = 0;
    for (int i = 0; i < desc.colorCount; ++i) {
        const PipelineDesc::ColorTarget& ct = desc.colorTargets[i];
        if (ct.format < 0) continue;  // 未使用槽位
        numRT = i + 1;
        D3D12_RENDER_TARGET_BLEND_DESC& rt = blend.RenderTarget[i];
        rt.BlendEnable = ct.blendEnabled;
        rt.LogicOpEnable = FALSE;
        rt.SrcBlend = toD3d12BlendFactor(ct.srcColor);
        rt.DestBlend = toD3d12BlendFactor(ct.dstColor);
        rt.BlendOp = toD3d12BlendOp(ct.colorOp);
        rt.SrcBlendAlpha = toD3d12BlendFactor(ct.srcAlpha);
        rt.DestBlendAlpha = toD3d12BlendFactor(ct.dstAlpha);
        rt.BlendOpAlpha = toD3d12BlendOp(ct.alphaOp);
        rt.LogicOp = D3D12_LOGIC_OP_NOOP;
        rt.RenderTargetWriteMask = ct.writeMask;  // MC 掩码位序与 D3D12 一致（R=1,G=2,B=4,A=8）
    }

    D3D12_RASTERIZER_DESC rs{};
    rs.FillMode = (desc.polygonMode == 1) ? D3D12_FILL_MODE_WIREFRAME : D3D12_FILL_MODE_SOLID;
    rs.CullMode = desc.cullEnabled ? D3D12_CULL_MODE_BACK : D3D12_CULL_MODE_NONE;
    // MC 前端为逆时针（官方 Vulkan frontFace=CCW），D3D12 默认按顺时针判定，需翻转。
    rs.FrontCounterClockwise = TRUE;
    rs.DepthBias = 0;                    // P6 细化 depthBiasConstant/ScaleFactor
    rs.DepthBiasClamp = 0.0f;
    rs.SlopeScaledDepthBias = 0.0f;
    rs.DepthClipEnable = TRUE;
    rs.MultisampleEnable = FALSE;
    rs.AntialiasedLineEnable = FALSE;
    rs.ForcedSampleCount = 0;
    rs.ConservativeRaster = D3D12_CONSERVATIVE_RASTERIZATION_MODE_OFF;

    // 5) 双 PSO：withDepth 总是创建（DSV=D32_FLOAT，镜像官方 depthAttachmentFormat=126）；
    //    仅当管线无深度状态时再创建 withoutDepth（DSV=UNKNOWN，无深度附件）。
    auto pipeline = std::make_unique<Dx12Pipeline>();
    pipeline->rootSignature = rootSig;
    pipeline->topology = desc.topology;

    // 记录 per-slot 修正 stride，供 setVertexBuffer 覆盖 Java 传入的错误值。
    for (const auto& ie : inputLayout) {
        int slot = (int)ie.InputSlot;
        if (!pipeline->vertexStrides.count(slot) || correctedStride > pipeline->vertexStrides[slot])
            pipeline->vertexStrides[slot] = correctedStride;
    }

    auto buildPso = [&](DXGI_FORMAT dsvFormat, bool depthEnable,
        D3D12_DEPTH_WRITE_MASK depthWrite, D3D12_COMPARISON_FUNC depthFunc,
        ComPtr<ID3D12PipelineState>& out, std::string& e) -> bool {
        // P19：DX12_DBG_DISABLE_DEPTH=1 时强制关闭深度测试，用于排查黑屏是否由深度配置错误导致。
        static bool forceNoDepth = []() -> bool {
            const char* v = std::getenv("DX12_DBG_DISABLE_DEPTH");
            return v && *v;
        }();
        D3D12_DEPTH_STENCIL_DESC ds{};
        ds.DepthEnable = depthEnable && !forceNoDepth;
        if (forceNoDepth && depthEnable) {
            dbgLog("buildPso: depth test forcibly DISABLED (DX12_DBG_DISABLE_DEPTH=1)");
        }
        ds.DepthWriteMask = depthWrite;
        ds.DepthFunc = depthFunc;
        ds.StencilEnable = FALSE;
        ds.StencilReadMask = D3D12_DEFAULT_STENCIL_READ_MASK;
        ds.StencilWriteMask = D3D12_DEFAULT_STENCIL_WRITE_MASK;

        D3D12_GRAPHICS_PIPELINE_STATE_DESC pso{};
        pso.pRootSignature = rootSig.Get();
        pso.VS.pShaderBytecode = vsBlob->GetBufferPointer();
        pso.VS.BytecodeLength = vsBlob->GetBufferSize();
        pso.PS.pShaderBytecode = psBlob->GetBufferPointer();
        pso.PS.BytecodeLength = psBlob->GetBufferSize();
        pso.InputLayout.NumElements = (UINT)inputLayout.size();
        pso.InputLayout.pInputElementDescs =
            inputLayout.empty() ? nullptr : inputLayout.data();
        pso.BlendState = blend;
        pso.RasterizerState = rs;
        pso.DepthStencilState = ds;
        pso.PrimitiveTopologyType = toTopologyType(desc.topology);
        pso.NumRenderTargets = (UINT)numRT;
        for (int i = 0; i < desc.colorCount; ++i) {
            const PipelineDesc::ColorTarget& ct = desc.colorTargets[i];
            pso.RTVFormats[i] = ct.format < 0 ? DXGI_FORMAT_UNKNOWN
                                              : toDxgiFormat(ct.format);
        }
        pso.DSVFormat = dsvFormat;
        pso.SampleDesc.Count = 1;
        pso.SampleDesc.Quality = 0;
        pso.SampleMask = UINT_MAX;
        // 诊断：打印 PSO 关键字段
        {
            std::string descInfo =
                "inputElements=" + std::to_string(inputLayout.size()) +
                " numRT=" + std::to_string(numRT) +
                " colorCount=" + std::to_string(desc.colorCount) +
                " topology=" + std::to_string((int)desc.topology) +
                " hasDepth=" + std::to_string(desc.hasDepth) +
                " dsv=" + std::to_string((int)dsvFormat);
            for (int i = 0; i < desc.colorCount; ++i) {
                descInfo += " rt" + std::to_string(i) + "=" + std::to_string((int)pso.RTVFormats[i]);
            }
            dbgLog("createGraphicsPipeline: PSO=[%s]\n", descInfo.c_str());
        }
        // 诊断：打印 HLSL 前 400 字符，便于检查语义声明格式
        {
            std::string vsStr((const char*)vsBytes.data(), vsBytes.size());
            std::string psStr((const char*)psBytes.data(), psBytes.size());
            int vsLen = std::min((int)vsStr.size(), 400);
            int psLen = std::min((int)psStr.size(), 400);
            dbgLog("createGraphicsPipeline: vs(%zuB)=[%.*s...]\nps(%zuB)=[%.*s...]",
                vsBytes.size(), vsLen, vsStr.c_str(), psBytes.size(), psLen, psStr.c_str());
        }
        // 详细 PSO 字段诊断（定位 E_INVALIDARG 根因）
        {
            char buf[512];
            snprintf(buf, sizeof(buf),
                "  pso_fields: rootSig=%p vsPtr=%p vsSize=%u psPtr=%p psSize=%u "
                "inputElts=%u rtCount=%u dsvFmt=%d topType=%d "
                "blendIndep=%d depthEnable=%d depthWrite=%d depthFunc=%d",
                (void*)pso.pRootSignature,
                (void*)pso.VS.pShaderBytecode, (UINT)pso.VS.BytecodeLength,
                (void*)pso.PS.pShaderBytecode, (UINT)pso.PS.BytecodeLength,
                (UINT)pso.InputLayout.NumElements,
                (UINT)pso.NumRenderTargets, (int)pso.DSVFormat,
                (int)pso.PrimitiveTopologyType,
                (int)pso.BlendState.IndependentBlendEnable,
                (int)pso.DepthStencilState.DepthEnable,
                (int)pso.DepthStencilState.DepthWriteMask,
                (int)pso.DepthStencilState.DepthFunc);
            dbgLog(buf);
            // 打印每个输入元素的完整描述
            for (UINT i = 0; i < pso.InputLayout.NumElements; ++i) {
                const D3D12_INPUT_ELEMENT_DESC& ie = inputLayout[i];
                snprintf(buf, sizeof(buf),
                    "  inputEl[%u]: fmt=%d slot=%u offset=%u class=%u step=%u semantic=%s",
                    i, (int)ie.Format, ie.InputSlot, ie.AlignedByteOffset,
                    ie.InputSlotClass, ie.InstanceDataStepRate,
                    ie.SemanticName);
                dbgLog(buf);
            }
        }
        HRESULT h = gCtx.device->CreateGraphicsPipelineState(&pso, IID_PPV_ARGS(&out));
        if (FAILED(h)) {
            e = "createGraphicsPipeline: CreateGraphicsPipelineState hr=" + hrText(h);
            dbgLog("  === D3D12 validation messages ===");
            if (gCtx.infoQueue) {
                UINT64 count = gCtx.infoQueue->GetNumStoredMessages();
                dbgLog("  InfoQueue stored=%llu", (unsigned long long)count);
                UINT64 start = count > 64 ? count - 64 : 0;
                for (UINT64 i = start; i < count; ++i) {
                    SIZE_T len = 0;
                    if (FAILED(gCtx.infoQueue->GetMessage((UINT)i, nullptr, &len))) continue;
                    std::vector<char> buf(len > 0 ? len : 1);
                    D3D12_MESSAGE* msg = reinterpret_cast<D3D12_MESSAGE*>(buf.data());
                    if (SUCCEEDED(gCtx.infoQueue->GetMessage((UINT)i, msg, &len))) {
                        const char* sev = "?";
                        switch (msg->Severity) {
                            case D3D12_MESSAGE_SEVERITY_CORRUPTION: sev = "CORRUPTION"; break;
                            case D3D12_MESSAGE_SEVERITY_ERROR: sev = "ERROR"; break;
                            case D3D12_MESSAGE_SEVERITY_WARNING: sev = "WARNING"; break;
                            default: break;
                        }
                        dbgLog("  InfoQueue[%s] %s", sev, msg->pDescription ? msg->pDescription : "");
                    }
                }
            }
            return false;
        }
        return true;
    };

    D3D12_DEPTH_WRITE_MASK depthWrite = (desc.hasDepth && desc.depthWrite)
        ? D3D12_DEPTH_WRITE_MASK_ALL : D3D12_DEPTH_WRITE_MASK_ZERO;
    D3D12_COMPARISON_FUNC depthFunc = desc.hasDepth
        ? toD3d12Compare((uint8_t)desc.depthCompareOp) : D3D12_COMPARISON_FUNC_ALWAYS;

    // 始终同时创建两个 PSO：withDepth（DSV=D32_FLOAT）和
    // withoutDepth（DSV=UNKNOWN，depthEnable=false），以支持运行时 hasDepth 与
    // 编译时 desc.hasDepth 不一致的场景（如 pipeline/gui 编译时无深度，但渲染
    // pass 请求 hasDepth=true）。
    // 关键：withDepth 的 depthEnable/depthFunc 必须跟随 desc.hasDepth，
    // 否则无深度管线（如 GUI）会被强制开启深度测试并拿到无效 depthFunc（如 8）→ 全黑。
    bool wDepthEnable = desc.hasDepth;
    D3D12_COMPARISON_FUNC wDepthFunc = desc.hasDepth
        ? toD3d12Compare((uint8_t)desc.depthCompareOp) : D3D12_COMPARISON_FUNC_ALWAYS;
    if (!buildPso(DXGI_FORMAT_D32_FLOAT, wDepthEnable, depthWrite, wDepthFunc,
        pipeline->withDepth, err)) {
        return nullptr;
    }
    if (!buildPso(DXGI_FORMAT_UNKNOWN, false, D3D12_DEPTH_WRITE_MASK_ZERO,
        D3D12_COMPARISON_FUNC_ALWAYS, pipeline->withoutDepth, err)) {
        return nullptr;
    }
    return pipeline.release();
}

void destroyPipeline(Dx12Pipeline* pipeline) {
    DBG_LOG_DEBUG("destroyPipeline: pso=%p", (void*)pipeline);
    delete pipeline;
    DBG_LOG_DEBUG("destroyPipeline: done");
}

// ---------------------------------------------------------------------------
// P6: draw 命令录制（渲染 pass 内）
// ---------------------------------------------------------------------------

bool setPipeline(CommandContext* ctx, Dx12Pipeline* pipeline, bool hasDepth, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setPipeline: no open command list"; return false; }
    if (!pipeline) { err = "setPipeline: null pipeline"; return false; }
    ID3D12PipelineState* pso = hasDepth ? pipeline->withDepth.Get() : pipeline->withoutDepth.Get();
    if (!pso) pso = pipeline->withDepth.Get();  // 无深度渲染但管线未建 withoutDepth 时回退
    if (!pipeline->rootSignature) { err = "setPipeline: null root signature"; return false; }
    // P6 崩溃修复（第 5 轮）：D3D12 规定使用任何 root 参数（SetGraphicsRootDescriptorTable
    // 等）前必须先 SetGraphicsRootSignature。此前缺失 → UMD 首次真实 draw 时对 NULL root
    // signature 解引用崩溃（hs_err：NVIDIA UMD 内读 NULL+0x2b88，AV，PC 0x7ffcd70e8fa3）。
    ctx->commandList->SetGraphicsRootSignature(pipeline->rootSignature.Get());
    ctx->commandList->SetPipelineState(pso);
    ctx->currentPipeline = pipeline;
    // P6 纯色黑屏修复：D3D12 命令列表初始 topology 是 UNDEFINED，必须显式
    // IASetPrimitiveTopology，否则 GPU 丢弃全部图元（只有 clear 色可见）。
    D3D12_PRIMITIVE_TOPOLOGY topo = toPrimitiveTopology(pipeline->topology);
    ctx->commandList->IASetPrimitiveTopology(topo);
    // P6 诊断：dbgLog 双写（stderr + %TEMP%\dx12-native.log）。fprintf 只写
    // stderr 不进文件（PCL 启动器不捕获原生 stderr），setPipeline 是否被调用
    // 曾是完全的观测盲区。
    dbgLog("setPipeline rootSig=%p pso=%p hasDepth=%d topoOrdinal=%d topo=%d",
        (void*)pipeline->rootSignature.Get(), (void*)pso, (int)hasDepth,
        (int)pipeline->topology, (int)topo);
    return true;
}

bool setScissor(CommandContext* ctx, int x, int y, int w, int h, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setScissor: no open command list"; return false; }
    D3D12_RECT rect{ (LONG)x, (LONG)y, (LONG)x + w, (LONG)y + h };
    ctx->commandList->RSSetScissorRects(1, &rect);
    return true;
}

bool setVertexBuffer(CommandContext* ctx, int slot, Dx12Object* buffer, long long offset, int stride, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setVertexBuffer: no open command list"; return false; }
    if (slot < 0 || slot >= 16) { err = "setVertexBuffer: slot out of range"; return false; }
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) {
        err = "setVertexBuffer: invalid buffer handle"; return false;
    }
    if (offset < 0 || offset >= buffer->size) { err = "setVertexBuffer: offset out of bounds"; return false; }
    transitionBufferTo(ctx, buffer, D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER);
    // P6：使用管线中记录的修正 stride（覆盖 Java 传入的错误值）
    UINT effectiveStride = (UINT)stride;
    if (ctx->currentPipeline && ctx->currentPipeline->vertexStrides.count(slot))
        effectiveStride = ctx->currentPipeline->vertexStrides[slot];
    D3D12_VERTEX_BUFFER_VIEW vb{};
    vb.BufferLocation = buffer->resource->GetGPUVirtualAddress() + (UINT64)offset;
    vb.SizeInBytes = (UINT)(buffer->size - offset);
    vb.StrideInBytes = effectiveStride;
    ctx->commandList->IASetVertexBuffers((UINT)slot, 1, &vb);
    // P6 诊断：确认每帧 draw 前确实绑定了顶点缓冲（内容尺寸/stride）。
    DBG_LOG_DEBUG("setVertexBuffer: slot=%d buf=%p size=%lld off=%lld javaStride=%d effStride=%d heap=%d",
        slot, (void*)buffer, (long long)buffer->size, (long long)offset,
        (int)stride, (int)effectiveStride, (int)buffer->heapType);
    // P6 诊断：每次读回顶点 buffer 前 16 floats（128 字节），确认数据是否真正写入。
    static int vbDbg = 0;
    if ((++vbDbg % 60) == 1) {
        dbgReadbackBufferBytes(buffer, offset < 0 ? 0 : offset, 128, "vb");
    }
#ifdef DIAG_READBACK_COLOR_TEX
    // P20 诊断：每次 drawIndexed 立即读回顶点前 16 floats，不依赖频率计数。
    // 与 beginRenderPass color target 日志配合，确认顶点数据非零。
    {
        static bool vb_done = false;
        if (!vb_done) {
            vb_done = true;
            dbgLog("setVB[diag]: reading vertex buffer first 16 floats (%lld bytes)",
                (long long)std::min<long long>(buffer->size - offset, 128));
            dbgReadbackBufferBytes(buffer, offset < 0 ? 0 : offset, 128, "vb_once");
        }
    }
#endif
    return true;
}

bool setIndexBuffer(CommandContext* ctx, Dx12Object* buffer, int indexType, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "setIndexBuffer: no open command list"; return false; }
    if (!buffer || buffer->kind != Dx12Object::Kind::Buffer) {
        err = "setIndexBuffer: invalid buffer handle"; return false;
    }
    transitionBufferTo(ctx, buffer, D3D12_RESOURCE_STATE_INDEX_BUFFER);
    D3D12_INDEX_BUFFER_VIEW ib{};
    ib.BufferLocation = buffer->resource->GetGPUVirtualAddress();
    ib.SizeInBytes = (UINT)buffer->size;
    ib.Format = indexType == 1 ? DXGI_FORMAT_R32_UINT : DXGI_FORMAT_R16_UINT;
    ctx->commandList->IASetIndexBuffer(&ib);
    // P6 诊断：确认每帧 draw 前确实绑定了索引缓冲（内容尺寸/索引宽度）。
    DBG_LOG_DEBUG("setIndexBuffer: buf=%p size=%lld idxWidth=%d heap=%d",
        (void*)buffer, (long long)buffer->size, indexType == 1 ? 4 : 2,
        (int)buffer->heapType);
    return true;
}

bool pushDescriptors(CommandContext* ctx, const std::vector<DrawBinding>& bindings, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "pushDescriptors: no open command list"; return false; }
    UINT count = (UINT)bindings.size();
    if (count == 0) return true;
    if (ctx->nextDrawSlot + count > kDrawHeapPerFrame) {
        err = "pushDescriptors: draw descriptor heap exhausted for this frame";
        return false;
    }
    // P16 诊断：每帧首 pushDescriptors 打印 binding 数量（确认 uniform/texture 被推送）
    static int pdFrameCount = 0;
    if ((int)ctx->fenceValue != pdFrameCount) {
        pdFrameCount = (int)ctx->fenceValue;
        dbgLog("pushDescriptors[%llu]: count=%u firstBuf=%p firstView=%p",
            (unsigned long long)ctx->fenceValue, count,
            (void*)(count > 0 ? (void*)bindings[0].buffer : nullptr),
            (void*)(count > 0 ? (void*)bindings[0].view : nullptr));
        // P21 诊断：打印每个 binding 的 buffer/offset，定位 UBO shader 读取偏移
        for (UINT j = 0; j < count && j < 8; ++j) {
            const DrawBinding& bj = bindings[j];
            const char* tname = bj.type == 0 ? "CBV" : bj.type == 1 ? "SRV" : bj.type == 2 ? "BUF" : "?";
            dbgLog("pushDesc BIND[%u]: type=%s buf=%p off=%lld len=%lld view=%p heap=%d",
                (unsigned)j, tname,
                (void*)(bj.buffer ? bj.buffer : nullptr),
                (long long)bj.offset, (long long)bj.length,
                (void*)(bj.view ? bj.view : nullptr),
                (int)(bj.buffer ? bj.buffer->heapType : -1));
        }
    }
    SIZE_T base = (SIZE_T)(ctx->drawHeapSlotBase + ctx->nextDrawSlot) * gCtx.drawInc;
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = gCtx.drawHeap->GetCPUDescriptorHandleForHeapStart();
    cpu.ptr += base;
    for (UINT i = 0; i < count; ++i) {
        D3D12_CPU_DESCRIPTOR_HANDLE dst{ cpu.ptr + (SIZE_T)i * gCtx.drawInc };
        const DrawBinding& b = bindings[i];
        DBG_LOG_DEBUG("pushDesc[%u] type=%d buf=%p view=%p off=%lld len=%lld texel=%d",
            (unsigned)i, (int)b.type, (void*)b.buffer, (void*)b.view,
            (long long)b.offset, (long long)b.length, b.texelFormat);
        switch (b.type) {
            case 0: {  // CBV（offset 须 256 对齐；SizeInBytes 向上取整 256）
                if (!b.buffer || b.buffer->kind != Dx12Object::Kind::Buffer || !b.buffer->resource) {
                    dbgLog("pushDesc[%u] INVALID CBV buffer", (unsigned)i);
                    err = "pushDescriptors: invalid buffer for CBV entry " + std::to_string(i);
                    return false;
                }
                static int ubDbg = 0;
                if (i == 1 && ((++ubDbg % 60) == 1)) {
                    // 诊断：读取 DynamicTransforms UBO（binding[1]），非 binding[0]
                    dbgReadbackBufferBytes(b.buffer, b.offset,
                        (int)std::min<long long>(b.length, 128), "ubo");
                }
                // P18：单独诊断 Projection buffer（binding index 0），确认投影矩阵数据是否写入。
                static int projDbg = 0;
                if (i == 0 && ((++projDbg % 60) == 1)) {
                    dbgReadbackBufferBytes(b.buffer, b.offset,
                        (int)std::min<long long>(b.length, 64), "proj");
                }
                transitionBufferTo(ctx, b.buffer, D3D12_RESOURCE_STATE_VERTEX_AND_CONSTANT_BUFFER);
                D3D12_CONSTANT_BUFFER_VIEW_DESC cbv{};
                cbv.BufferLocation = b.buffer->resource->GetGPUVirtualAddress() + (UINT64)b.offset;
                UINT64 cbvSize = (UINT64)b.length;
                cbvSize = (cbvSize + 255) & ~255ULL;
                if (cbvSize == 0) cbvSize = 256;
                cbv.SizeInBytes = (UINT)cbvSize;
                // P20：诊断 CBV 地址（每帧首 pushDescriptors 打印）
                static UINT64 lastPdFrame = 0;
                if ((UINT64)ctx->fenceValue != lastPdFrame) {
                    lastPdFrame = (UINT64)ctx->fenceValue;
                    dbgLog("pushDesc CBV[%u]: bufGVA=%llx off=%lld cbvLoc=%llx cbvSize=%llu heap=%d",
                        (unsigned)i,
                        (unsigned long long)b.buffer->resource->GetGPUVirtualAddress(),
                        (long long)b.offset,
                        (unsigned long long)cbv.BufferLocation,
                        (unsigned long long)cbvSize,
                        (int)b.buffer->heapType);
                    // P21：额外诊断 binding[1]（DynamicTransforms UBO）的 offset，确认 shader 读取位置
                    if (i == 0 && count > 1) {
                        const DrawBinding& b1 = bindings[1];
                        dbgLog("pushDesc UBO_BIND[1]: bufGVA=%llx off=%lld len=%lld heap=%d",
                            (unsigned long long)b1.buffer ? (unsigned long long)b1.buffer->resource->GetGPUVirtualAddress() : 0ULL,
                            (long long)b1.offset, (long long)b1.length,
                            (int)(b1.buffer ? b1.buffer->heapType : -1));
                    }
                }
                gCtx.device->CreateConstantBufferView(&cbv, dst);
                break;
            }
            case 1: {  // SRV：复制 texture view 的现有描述符
                if (!b.view || b.view->cpuHandle.ptr == 0) {
                    dbgLog("pushDesc[%u] INVALID view", (unsigned)i);
                    err = "pushDescriptors: missing view for SRV entry " + std::to_string(i);
                    return false;
                }
                // 关键修复：普通纹理（字体/logo/panorama 等）上传后处于 COMMON，
                // 采样前必须显式 transition 到 SHADER_RESOURCE，否则 D3D12 在
                // COMMON 状态采样是未定义行为（NVIDIA 驱动静默返回黑）→ 全黑屏。
                if (b.view->sourceTexture) {
                    if (b.view->sourceTexture->usage & 16) {
                        dbgLog("pushDesc SRV: CUBE view=%p srcTex=%p -> SHADER_RESOURCE",
                            (void*)b.view, (void*)b.view->sourceTexture);
                    }
                    transitionTextureTo(ctx, b.view->sourceTexture,
                        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE
                            | D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);
#ifdef DIAG_READBACK_COLOR_TEX
                    // P20 诊断：首次 SRV 绑定读回纹理像素，确认纹理内容是否非空。
                    // deviceWaitIdle 开销大，仅首次执行（static 标志）。
                    {
                        static bool srk_done = false;
                        if (!srk_done) {
                            srk_done = true;
                            dbgLog("pushDesc SRV[0]: reading back sourceTexture pixels (may block)");
                            dbgReadbackTexturePixels(b.view->sourceTexture, "srv_tex_readback");
                        }
                    }
#endif
                }
                gCtx.device->CopyDescriptorsSimple(1, dst, b.view->cpuHandle,
                    D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
                break;
            }
            case 2: {  // SRV：texel buffer
                if (!b.buffer || b.buffer->kind != Dx12Object::Kind::Buffer || !b.buffer->resource) {
                    dbgLog("pushDesc[%u] INVALID texel buffer", (unsigned)i);
                    err = "pushDescriptors: invalid texel buffer handle";
                    return false;
                }
                transitionBufferTo(ctx, b.buffer,
                    D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE
                        | D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);
                D3D12_SHADER_RESOURCE_VIEW_DESC srv{};
                srv.Format = toDxgiFormat(b.texelFormat);
                if (srv.Format == DXGI_FORMAT_UNKNOWN) {
                    err = "pushDescriptors: unsupported texel buffer format " + std::to_string(b.texelFormat);
                    return false;
                }
                srv.ViewDimension = D3D12_SRV_DIMENSION_BUFFER;
                srv.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
                UINT elementBytes = std::max<UINT>(1u, blockSizeFor(srv.Format));
                srv.Buffer.FirstElement = (UINT)(b.offset / elementBytes);
                srv.Buffer.NumElements = (UINT)(b.length / elementBytes);
                gCtx.device->CreateShaderResourceView(b.buffer->resource.Get(), &srv, dst);
                break;
            }
            default:
                err = "pushDescriptors: unknown binding type " + std::to_string(b.type);
                return false;
        }
    }
    // gpuRoot 必须指向本帧实际写入位置（ring buffer 偏移 base），
    // 而不是 heap 起始处。否则多帧飞环时每帧的命令列表都把根表绑定到
    // 同一 GPU 地址（heap start），导致 GPU 读到前帧残留的描述符 → 黑屏。
    D3D12_GPU_DESCRIPTOR_HANDLE gpuRoot =
        gCtx.drawHeap->GetGPUDescriptorHandleForHeapStart();
    gpuRoot.ptr += base;
    ctx->nextDrawSlot += count;
    // P20：诊断 root descriptor table 绑定地址（指向本帧写入位置）
    static UINT64 lastGpuFrame = 0;
    if ((UINT64)ctx->fenceValue != lastGpuFrame) {
        lastGpuFrame = (UINT64)ctx->fenceValue;
        dbgLog("pushDesc SET_ROOT_TABLE: heapBase=%llx writeBase=%llx slotCount=%u",
            (unsigned long long)gCtx.drawHeap->GetGPUDescriptorHandleForHeapStart().ptr,
            (unsigned long long)base, (unsigned)count);
    }
    ctx->commandList->SetGraphicsRootDescriptorTable(0, gpuRoot);
    return true;
}

bool drawIndexedInstanced(CommandContext* ctx, UINT indexCount, UINT instanceCount,
    INT startIndexLocation, INT baseVertexLocation, UINT startInstanceLocation, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "drawIndexed: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "drawIndexed: no open render pass"; return false; }
    // P17 诊断：每次 drawIndexed 都记录（验证 GUI pass 是否有实际 draw call）
    // 使用静态计数器区分同一 command list 内的多个 pass（如 512x256 + 854x480）
    static UINT64 lastFence = 0;
    static int passCount = 0;
    if (ctx->fenceValue != lastFence) {
        lastFence = ctx->fenceValue;
        passCount = 0;
    }
    passCount++;
    if (indexCount == 0) {
        dbgLog("drawIndexed[%llu] PASS#%d: ZERO-COUNT (skip) topo=%d",
            (unsigned long long)ctx->fenceValue, passCount,
            (int)(ctx->currentPipeline ? ctx->currentPipeline->topology : -1));
    } else {
        dbgLog("drawIndexed[%llu] PASS#%d: count=%u inst=%u first=%d base=%d topo=%d",
            (unsigned long long)ctx->fenceValue, passCount, indexCount, instanceCount,
            startIndexLocation, baseVertexLocation,
            (int)(ctx->currentPipeline ? ctx->currentPipeline->topology : -1));
    }
    // P17：标记当前 render pass 的所有 color target 为"已写入"
    for (size_t i = 0; i < ctx->activeColorTargets.size(); ++i) {
        if (i < ctx->activeColorTargetsTouched.size())
            ctx->activeColorTargetsTouched[i] = true;
    }
    ctx->colorTargetsWritten = true;
    dbgLog("drawIndexed[%llu] colorTargetsWritten -> %d",
        (unsigned long long)ctx->fenceValue, (int)ctx->colorTargetsWritten);
    ctx->commandList->DrawIndexedInstanced(indexCount, instanceCount,
        startIndexLocation, baseVertexLocation, startInstanceLocation);
#ifdef DIAG_READBACK_COLOR_TEX
    // P20 诊断：drawIndexed 后立即读回 color target，确认绘制是否写入像素。
    // 只执行一次（static 标志），避免每帧 deviceWaitIdle 的开销。
    {
        static bool dr_done = false;
        if (!dr_done && !ctx->activeColorTargets.empty()) {
            dr_done = true;
            Dx12Object* ct = ctx->activeColorTargets[0];
            dbgLog("drawIdx[diag]: reading back activeColorTarget[0] after draw");
            dbgReadbackTexturePixels(ct, "color_after_draw");
        }
    }
#endif
    return true;
}

bool drawInstanced(CommandContext* ctx, UINT vertexCount, UINT instanceCount,
    UINT firstVertex, UINT startInstanceLocation, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "draw: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "draw: no open render pass"; return false; }
    DBG_LOG_DEBUG("draw: vertexCount=%u instance=%u firstVertex=%u",
        vertexCount, instanceCount, firstVertex);
    ctx->commandList->DrawInstanced(vertexCount, instanceCount, firstVertex, startInstanceLocation);
    return true;
}

bool drawIndexedIndirect(CommandContext* ctx, Dx12Object* commands, long long offset,
    UINT drawCount, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "drawIndexedIndirect: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "drawIndexedIndirect: no open render pass"; return false; }
    if (!commands || commands->kind != Dx12Object::Kind::Buffer) {
        err = "drawIndexedIndirect: invalid buffer handle"; return false;
    }
    if (!gCtx.cmdSigIndexed) { err = "drawIndexedIndirect: no indexed command signature"; return false; }
    ctx->commandList->ExecuteIndirect(gCtx.cmdSigIndexed.Get(), drawCount,
        commands->resource.Get(), (UINT64)offset, nullptr, 0);
    return true;
}

bool drawIndirect(CommandContext* ctx, Dx12Object* commands, long long offset,
    UINT drawCount, std::string& err) {
    if (!ctx || !ctx->listOpen) { err = "drawIndirect: no open command list"; return false; }
    if (!ctx->inRenderPass) { err = "drawIndirect: no open render pass"; return false; }
    if (!commands || commands->kind != Dx12Object::Kind::Buffer) {
        err = "drawIndirect: invalid buffer handle"; return false;
    }
    if (!gCtx.cmdSigNonIndexed) { err = "drawIndirect: no non-indexed command signature"; return false; }
    ctx->commandList->ExecuteIndirect(gCtx.cmdSigNonIndexed.Get(), drawCount,
        commands->resource.Get(), (UINT64)offset, nullptr, 0);
    return true;
}

// ---------------------------------------------------------------------------
// P6 诊断：读回纹理/buffer 内容（纯色黑屏定位用）。
// 内部先等 GPU 空闲（已提交命令全部完成、资源隐式 decay 回初始状态），再用
// 一次性命令列表拷贝到 readback staging。调用点每 ~60 帧触发一次，同步开销可忽略。
// ---------------------------------------------------------------------------

void dbgReadbackTexturePixels(Dx12Object* tex, const char* tag) {
    if (!tex || tex->kind != Dx12Object::Kind::Texture || !tex->resource) return;
    std::string err;
    if (!deviceWaitIdle(err)) return;
    ID3D12Resource* r = tex->resource.Get();
    D3D12_RESOURCE_DESC td = r->GetDesc();
    UINT w = (UINT)td.Width, h = td.Height;
    if (w == 0 || h == 0 || w > 16384 || h > 16384) return;

    // 根据纹理实际格式计算每像素字节数和读回足迹格式。
    // 硬编码 R8G8B8A8_UNORM（原代码）对 R16 类格式（fmt=28/30/31等）会导致
    // 字节偏移错位（*4 vs *8）和 staging buffer 容量不足（total 减半）。
    DXGI_FORMAT fmt = td.Format;
    UINT bpp = 4;
    DXGI_FORMAT fbFmt = DXGI_FORMAT_R8G8B8A8_UNORM;
    switch (fmt) {
        case DXGI_FORMAT_R8G8B8A8_UNORM:
        case DXGI_FORMAT_R8G8B8A8_UINT:
        case DXGI_FORMAT_B8G8R8A8_UNORM:
        case DXGI_FORMAT_R8G8B8A8_SNORM:
        case DXGI_FORMAT_R8G8B8A8_SINT:
            bpp = 4; fbFmt = DXGI_FORMAT_R8G8B8A8_UNORM; break;
        case DXGI_FORMAT_R16G16B16A16_UNORM:
        case DXGI_FORMAT_R16G16B16A16_SNORM:
        case DXGI_FORMAT_R16G16B16A16_UINT:
        case DXGI_FORMAT_R16G16B16A16_SINT:
        case DXGI_FORMAT_R16G16B16A16_FLOAT:
            bpp = 8; fbFmt = DXGI_FORMAT_R16G16B16A16_UNORM; break;
        case DXGI_FORMAT_R32G32B32A32_FLOAT:
        case DXGI_FORMAT_R32G32B32A32_UINT:
        case DXGI_FORMAT_R32G32B32A32_SINT:
            bpp = 16; fbFmt = DXGI_FORMAT_R32G32_FLOAT; break;
        default:
            // 未知格式：按4字节处理（多数常见格式兼容）
            bpp = 4; fbFmt = DXGI_FORMAT_R8G8B8A8_UNORM; break;
    }
    UINT64 rowBytes = (UINT64)w * bpp;
    UINT64 pitch = (rowBytes + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1)
        & ~(UINT64)(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    UINT64 total = pitch * h;

    static ComPtr<ID3D12Resource> staging;
    if (!staging || staging->GetDesc().Width < total) {
        D3D12_RESOURCE_DESC desc{};
        desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        desc.Width = total;
        desc.Height = 1;
        desc.DepthOrArraySize = 1;
        desc.MipLevels = 1;
        desc.SampleDesc.Count = 1;
        desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        desc.Flags = D3D12_RESOURCE_FLAG_NONE;
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_READBACK;
        hp.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
        hp.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
        hp.CreationNodeMask = 0;
        hp.VisibleNodeMask = 0;
        if (FAILED(gCtx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&staging)))) return;
    }
    static ComPtr<ID3D12CommandAllocator> alloc;
    static ComPtr<ID3D12GraphicsCommandList> cl;
    if (!alloc && FAILED(gCtx.device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
            IID_PPV_ARGS(&alloc)))) return;
    if (!cl) {
        if (FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
                alloc.Get(), nullptr, IID_PPV_ARGS(&cl)))) return;
    } else {
        alloc->Reset();
        cl->Reset(alloc.Get(), nullptr);
    }
    // deviceWaitIdle 后未保持状态的纹理隐式 decay 回 COMMON。
    D3D12_RESOURCE_BARRIER b{};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = r;
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COMMON;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_SOURCE;
    cl->ResourceBarrier(1, &b);

    D3D12_TEXTURE_COPY_LOCATION src{};
    src.pResource = r;
    src.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    src.SubresourceIndex = 0;
    D3D12_TEXTURE_COPY_LOCATION dst{};
    dst.pResource = staging.Get();
    dst.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    dst.PlacedFootprint.Offset = 0;
    dst.PlacedFootprint.Footprint.Format = fbFmt;
    dst.PlacedFootprint.Footprint.Width = w;
    dst.PlacedFootprint.Footprint.Height = h;
    dst.PlacedFootprint.Footprint.Depth = 1;
    dst.PlacedFootprint.Footprint.RowPitch = (UINT)pitch;
    cl->CopyTextureRegion(&dst, 0, 0, 0, &src, nullptr);

    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_SOURCE;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_COMMON;
    cl->ResourceBarrier(1, &b);
    cl->Close();

    ID3D12CommandList* lists[] = { cl.Get() };
    gCtx.queue->ExecuteCommandLists(1, lists);
    UINT64 fv = ++gCtx.queueFenceValue;
    if (FAILED(gCtx.queue->Signal(gCtx.queueFence.Get(), fv))) return;
    if (!waitForQueueFenceValue(fv, 5'000'000'000ULL, err)) return;

    void* ptr = nullptr;
    if (FAILED(staging->Map(0, nullptr, &ptr))) return;
    const uint8_t* base = (const uint8_t*)ptr;
    int xs[3] = { 0, (int)w / 2, (int)w - 1 };
    int ys[3] = { 0, (int)h / 2, (int)h - 1 };
    for (int yi = 0; yi < 3; ++yi) {
        for (int xi = 0; xi < 3; ++xi) {
            const uint8_t* p = base + (UINT64)ys[yi] * pitch + (UINT64)xs[xi] * bpp;
            dbgLog("rbTex[%s][%ux%u] (%d,%d) = RGBA(%3d,%3d,%3d,%3d)",
                tag, w, h, xs[xi], ys[yi], p[0], p[1], p[2], p[3]);
        }
    }
    dbgDumpPixelsToFile(base, w, h, pitch, tag);
    staging->Unmap(0, nullptr);
}

// P6 诊断：把 RGBA8 像素完整导出（BMP 文件 + 日志 ASCII 缩略图）。
// BMP：32bpp BGRA，bottom-up 行序，写到当前工作目录 dx12_dump_<tag>.bmp。
// ASCII：缩略到 <=96 列，亮度映射 .:-=+*#%@ 字符，高饱和度用色相字母标注
// （R/G/B/Y/M/C），用于快速识别画面内容（全景/按钮/文字）。
void dbgDumpPixelsToFile(const uint8_t* rgba, UINT w, UINT h, UINT64 pitch,
    const char* tag) {
    if (!rgba || w == 0 || h == 0 || w > 16384 || h > 16384) return;

    // ---- 1) 写 BMP 文件 ----
    {
        char path[256];
        snprintf(path, sizeof(path), "dx12_dump_%s.bmp", tag);
        FILE* f = fopen(path, "wb");
        if (f) {
            UINT64 rowBytes = (UINT64)w * 4;
            UINT64 padded = (rowBytes + 3) & ~3ULL;  // BMP 行 4 字节对齐（RGBA 已对齐）
            UINT64 imgSize = padded * h;
            uint32_t bfSize = (uint32_t)(54 + imgSize);
            // BITMAPFILEHEADER (14)
            uint8_t hdr[54] = {0};
            hdr[0] = 'B'; hdr[1] = 'M';
            hdr[2] = (uint8_t)(bfSize & 0xFF); hdr[3] = (uint8_t)((bfSize >> 8) & 0xFF);
            hdr[4] = (uint8_t)((bfSize >> 16) & 0xFF); hdr[5] = (uint8_t)((bfSize >> 24) & 0xFF);
            hdr[10] = 54;  // pixel data offset
            // BITMAPINFOHEADER (40)
            hdr[14] = 40;
            hdr[18] = (uint8_t)(w & 0xFF); hdr[19] = (uint8_t)((w >> 8) & 0xFF);
            hdr[20] = (uint8_t)((w >> 16) & 0xFF); hdr[21] = (uint8_t)((w >> 24) & 0xFF);
            uint32_t hh = h;
            hdr[22] = (uint8_t)(hh & 0xFF); hdr[23] = (uint8_t)((hh >> 8) & 0xFF);
            hdr[24] = (uint8_t)((hh >> 16) & 0xFF); hdr[25] = (uint8_t)((hh >> 24) & 0xFF);
            hdr[26] = 1;    // planes
            hdr[28] = 32;   // bpp
            hdr[34] = (uint8_t)(imgSize & 0xFF); hdr[35] = (uint8_t)((imgSize >> 8) & 0xFF);
            hdr[36] = (uint8_t)((imgSize >> 16) & 0xFF); hdr[37] = (uint8_t)((imgSize >> 24) & 0xFF);
            fwrite(hdr, 1, 54, f);
            // bottom-up：BMP 首行是图片最后一行
            for (UINT y = h; y > 0; --y) {
                const uint8_t* row = rgba + (UINT64)(y - 1) * pitch;
                for (UINT x = 0; x < w; ++x) {
                    uint8_t bgr[4];
                    bgr[0] = row[x * 4 + 2];  // B
                    bgr[1] = row[x * 4 + 1];  // G
                    bgr[2] = row[x * 4 + 0];  // R
                    bgr[3] = row[x * 4 + 3];  // A
                    fwrite(bgr, 1, 4, f);
                }
            }
            fclose(f);
            dbgLog("dump[%s] BMP saved %ux%u -> %s (rgba=%p)", tag, w, h, path,
                (const void*)rgba);
        } else {
            dbgLog("dump[%s] BMP open failed (%s)", tag, path);
        }
    }

    // ---- 2) 日志 ASCII 缩略图（亮度字符 + 色相字母）----
    const int cols = 96;
    const char* lumaChars = " .:-=+*#%@";
    int rows = (int)((UINT64)h * cols / w);
    if (rows < 1) rows = 1;
    if (rows > 64) rows = 64;
    std::string art;
    art.reserve((size_t)cols * rows + rows);
    for (int ry = 0; ry < rows; ++ry) {
        UINT y0 = (UINT)((UINT64)ry * h / rows);
        UINT y1 = (UINT)((UINT64)(ry + 1) * h / rows);
        if (y1 <= y0) y1 = y0 + 1;
        if (y1 > h) y1 = h;
        for (int rx = 0; rx < cols; ++rx) {
            UINT x0 = (UINT)((UINT64)rx * w / cols);
            UINT x1 = (UINT)((UINT64)(rx + 1) * w / cols);
            if (x1 <= x0) x1 = x0 + 1;
            if (x1 > w) x1 = w;
            unsigned r = 0, g = 0, b = 0, a = 0;
            UINT cnt = 0;
            for (UINT y = y0; y < y1; ++y) {
                const uint8_t* row = rgba + (UINT64)y * pitch;
                for (UINT x = x0; x < x1; ++x) {
                    r += row[x * 4 + 0];
                    g += row[x * 4 + 1];
                    b += row[x * 4 + 2];
                    a += row[x * 4 + 3];
                    ++cnt;
                }
            }
            if (cnt == 0) { art += ' '; continue; }
            r /= cnt; g /= cnt; b /= cnt; a /= cnt;
            // 亮度 + 饱和度（判断用亮度字符还是色相字母）
            int mx = (int)r, mn = (int)r;
            if ((int)g > mx) mx = g; if ((int)b > mx) mx = b;
            if ((int)g < mn) mn = g; if ((int)b < mn) mn = b;
            int luma = (r * 299 + g * 587 + b * 114) / 1000;
            int sat = (mx == 0) ? 0 : (mx - mn) * 255 / mx;
            if (sat > 90 && luma > 40) {
                // 高饱和度：用色相字母标出主色
                char hue = '?';
                if (r > g && r > b) hue = 'R';
                else if (g > r && g > b) hue = 'G';
                else if (b > r && b > g) hue = 'B';
                else if (r > 120 && g > 120 && b < 90) hue = 'Y';
                else if (r > 120 && b > 120 && g < 90) hue = 'M';
                else if (g > 120 && b > 120 && r < 90) hue = 'C';
                else if (r > 180 && g > 180 && b > 180) hue = 'W';
                art += hue;
            } else {
                int idx = luma * 9 / 255;
                if (idx > 9) idx = 9;
                art += lumaChars[idx];
            }
        }
        art += '\n';
    }
    dbgLog("dump[%s] ASCII %dx%d (grid %dx%d):\n%s", tag, w, h, cols, rows,
        art.c_str());
}

void dbgReadbackBufferBytes(Dx12Object* buf, long long offset, int len, const char* tag) {
    if (!buf || buf->kind != Dx12Object::Kind::Buffer || !buf->resource) return;
    if (offset < 0 || offset >= buf->size) return;
    long long avail = buf->size - offset;
    if (len <= 0) len = (int)std::min(avail, 128LL);
    len = (int)std::min((long long)len, avail);
    if (len <= 0) return;

    void* ptr = nullptr;
    ID3D12Resource* unmapRes = nullptr;
    if (buf->heapType == D3D12_HEAP_TYPE_UPLOAD) {
        // UPLOAD：CPU 已写可见，直接 Map 读。
        if (FAILED(buf->resource->Map(0, nullptr, &ptr))) return;
        unmapRes = buf->resource.Get();
    } else {
        std::string err;
        if (!deviceWaitIdle(err)) return;
        UINT64 total = ((UINT64)len + 255) & ~255ULL;
        static ComPtr<ID3D12Resource> staging;
        if (!staging || staging->GetDesc().Width < total) {
            D3D12_RESOURCE_DESC desc{};
            desc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
            desc.Width = total;
            desc.Height = 1;
            desc.DepthOrArraySize = 1;
            desc.MipLevels = 1;
            desc.SampleDesc.Count = 1;
            desc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
            desc.Flags = D3D12_RESOURCE_FLAG_NONE;
            D3D12_HEAP_PROPERTIES hp{};
            hp.Type = D3D12_HEAP_TYPE_READBACK;
            hp.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
            hp.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
            hp.CreationNodeMask = 0;
            hp.VisibleNodeMask = 0;
            if (FAILED(gCtx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
                &desc, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&staging)))) return;
        }
        static ComPtr<ID3D12CommandAllocator> alloc;
        static ComPtr<ID3D12GraphicsCommandList> cl;
        if (!alloc && FAILED(gCtx.device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
                IID_PPV_ARGS(&alloc)))) return;
        if (!cl) {
            if (FAILED(gCtx.device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
                    alloc.Get(), nullptr, IID_PPV_ARGS(&cl)))) return;
        } else {
            alloc->Reset();
            cl->Reset(alloc.Get(), nullptr);
        }
        D3D12_RESOURCE_BARRIER b{};
        b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
        b.Transition.pResource = buf->resource.Get();
        b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
        b.Transition.StateBefore = D3D12_RESOURCE_STATE_COMMON;
        b.Transition.StateAfter = D3D12_RESOURCE_STATE_COPY_SOURCE;
        cl->ResourceBarrier(1, &b);
        cl->CopyBufferRegion(staging.Get(), 0, buf->resource.Get(), (UINT64)offset, (UINT64)len);
        b.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_SOURCE;
        b.Transition.StateAfter = D3D12_RESOURCE_STATE_COMMON;
        cl->ResourceBarrier(1, &b);
        cl->Close();

        ID3D12CommandList* lists[] = { cl.Get() };
        gCtx.queue->ExecuteCommandLists(1, lists);
        UINT64 fv = ++gCtx.queueFenceValue;
        if (FAILED(gCtx.queue->Signal(gCtx.queueFence.Get(), fv))) return;
        if (!waitForQueueFenceValue(fv, 5'000'000'000ULL, err)) return;
        if (FAILED(staging->Map(0, nullptr, &ptr))) return;
        unmapRes = staging.Get();
    }

    const float* f = (const float*)ptr;
    const uint8_t* b8 = (const uint8_t*)ptr;
    std::string fs;
    int nf = std::min(len / 4, 12);
    for (int i = 0; i < nf; ++i) {
        char t[32];
        snprintf(t, sizeof(t), "%g ", (double)f[i]);
        fs += t;
    }
    std::string hs;
    int nh = std::min(len, 16);
    for (int i = 0; i < nh; ++i) {
        char t[8];
        snprintf(t, sizeof(t), "%02X ", b8[i]);
        hs += t;
    }
    dbgLog("rbBuf[%s] off=%lld len=%d heap=%d floats=[%s] hex=[%s]",
        tag, offset, len, (int)buf->heapType, fs.c_str(), hs.c_str());
    unmapRes->Unmap(0, nullptr);
}

// 枚举所有 D3D12 支持的适配器，返回 JSON 字符串
std::string enumerateAdaptersJson() {
    std::string result = "[";
    bool first = true;
    try {
        ComPtr<IDXGIFactory4> factory;
        if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) {
            return "[{\"error\":\"CreateDXGIFactory1 failed\"}]";
        }
        for (UINT i = 0; ; ++i) {
            ComPtr<IDXGIAdapter> adapter;
            if (FAILED(factory->EnumAdapters(i, &adapter))) break;
            DXGI_ADAPTER_DESC desc{};
            HRESULT hr = adapter->GetDesc(&desc);
            if (FAILED(hr)) continue;
            // 只返回支持 D3D12 的适配器
            ComPtr<ID3D12Device> probe;
            hr = D3D12CreateDevice(adapter.Get(), D3D_FEATURE_LEVEL_12_0,
                IID_PPV_ARGS(&probe));
            if (FAILED(hr)) continue;
            if (!first) result += ",";
            first = false;
            char nameBuf[256] = {};
            WideCharToMultiByte(CP_UTF8, 0, desc.Description, -1,
                nameBuf, sizeof(nameBuf), nullptr, nullptr);
            long long vramGb = (long long)(desc.DedicatedVideoMemory / (1024LL * 1024 * 1024));
            result += "{\"name\":\"";
            result += nameBuf;
            result += "\",\"luid\":\"";
            char luidBuf[32];
            snprintf(luidBuf, sizeof(luidBuf), "%08X%08X",
                desc.AdapterLuid.LowPart, desc.AdapterLuid.HighPart);
            result += luidBuf;
            result += "\",\"vid\":0x";
            char vidBuf[16];
            snprintf(vidBuf, sizeof(vidBuf), "%08X", desc.VendorId);
            result += vidBuf;
            result += "\",\"did\":0x";
            char didBuf[16];
            snprintf(didBuf, sizeof(didBuf), "%08X", desc.DeviceId);
            result += didBuf;
            result += "\",\"vram_gb\":";
            result += std::to_string(vramGb);
            result += "}";
        }
    } catch (...) {
        return "[{\"error\":\"exception during enumeration\"}]";
    }
    result += "]";
    return result;
}

// ---------------------------------------------------------------------------
// P22: Blit 管线（全屏四边形，绕过 CopyTextureRegion 格式不兼容）
// ---------------------------------------------------------------------------
namespace {

// VS: 把 2D 位置直接作为 SV_Position 输出；texcoord 透传给 PS。
static const char kBlitVS[] =
    "struct VS_OUT { float4 pos : SV_Position; float2 tc : TEXCOORD0; };\n"
    "VS_OUT main(float2 pos : POSITION, float2 tc : TEXCOORD0) {\n"
    "    VS_OUT o;\n"
    "    o.pos = float4(pos, 0.0f, 1.0f);\n"
    "    o.tc = tc;\n"
    "    return o;\n"
    "}\n";

// PS: 对源纹理线性采样，输出到 backbuffer；GPU 光栅器自动处理格式转换。
static const char kBlitPS[] =
    "Texture2D<float4> srcTex : register(t0, space0);\n"
    "SamplerState sampler : register(s0, space0);\n"
    "float4 main(VS_OUT IN) : SV_Target {\n"
    "    return srcTex.Sample(sampler, IN.tc);\n"
    "}\n";

}  // namespace

void initBlitPipeline(std::string& err) {
    if (!ensureDevice(err)) return;
    if (gCtx.blitPipeline) return;  // 已初始化

    ComPtr<ID3D12RootSignature> rootSig;
    {
        // 两个 descriptor range：SRV(t0), sampler(s0)
        D3D12_DESCRIPTOR_RANGE srvRange{};
        srvRange.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
        srvRange.NumDescriptors = 1;
        srvRange.BaseShaderRegister = 0;
        srvRange.RegisterSpace = 0;
        srvRange.OffsetInDescriptorsFromTableStart = D3D12_DESCRIPTOR_RANGE_OFFSET_APPEND;

        D3D12_DESCRIPTOR_RANGE samRange{};
        samRange.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SAMPLER;
        samRange.NumDescriptors = 1;
        samRange.BaseShaderRegister = 0;
        samRange.RegisterSpace = 0;
        samRange.OffsetInDescriptorsFromTableStart = D3D12_DESCRIPTOR_RANGE_OFFSET_APPEND;

        D3D12_DESCRIPTOR_RANGE ranges[] = { srvRange, samRange };

        D3D12_ROOT_DESCRIPTOR_TABLE table{};
        table.NumDescriptorRanges = 2;
        table.pDescriptorRanges = ranges;

        D3D12_ROOT_PARAMETER param{};
        param.ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
        param.DescriptorTable = table;
        param.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

        D3D12_STATIC_SAMPLER_DESC staticSam{};
        staticSam.Filter = D3D12_FILTER_MIN_MAG_MIP_LINEAR;
        staticSam.AddressU = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
        staticSam.AddressV = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
        staticSam.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
        staticSam.MipLODBias = 0.0f;
        staticSam.MaxAnisotropy = 1;
        staticSam.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
        staticSam.BorderColor = D3D12_STATIC_BORDER_COLOR_OPAQUE_BLACK;
        staticSam.MinLOD = 0.0f;
        staticSam.MaxLOD = D3D12_FLOAT32_MAX;
        staticSam.ShaderRegister = 0;
        staticSam.RegisterSpace = 0;
        staticSam.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

        D3D12_ROOT_SIGNATURE_DESC rsDesc{};
        rsDesc.NumParameters = 1;
        rsDesc.pParameters = &param;
        rsDesc.NumStaticSamplers = 1;
        rsDesc.pStaticSamplers = &staticSam;
        rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;

        ComPtr<ID3DBlob> rsBlob, rsErr;
        HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1_0,
            &rsBlob, &rsErr);
        if (FAILED(hr)) {
            err = "initBlitPipeline: SerializeRootSignature hr=0x" + hrText(hr);
            return;
        }
        hr = gCtx.device->CreateRootSignature(0, rsBlob->GetBufferPointer(),
            rsBlob->GetBufferSize(), IID_PPV_ARGS(&rootSig));
        if (FAILED(hr)) {
            err = "initBlitPipeline: CreateRootSignature hr=0x" + hrText(hr);
            return;
        }
    }

    // 编译 VS / PS
    ComPtr<ID3DBlob> vsBlob, psBlob, errs;
    std::vector<uint8_t> vsBytes(kBlitVS, kBlitVS + sizeof(kBlitVS));
    std::vector<uint8_t> psBytes(kBlitPS, kBlitPS + sizeof(kBlitPS));
    if (!compileShaderBytecode(vsBytes, "blit_vs", "vs_5_1", vsBlob, err)) return;
    if (!compileShaderBytecode(psBytes, "blit_ps", "ps_5_1", psBlob, err)) return;

    // 创建 PSO
    ComPtr<ID3D12PipelineState> pso;
    {
        D3D12_INPUT_ELEMENT_DESC ie[2] = {};
        ie[0].SemanticName = "POSITION";
        ie[0].SemanticIndex = 0;
        ie[0].Format = DXGI_FORMAT_R32G32_FLOAT;
        ie[0].InputSlot = 0;
        ie[0].AlignedByteOffset = 0;
        ie[0].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
        ie[0].InstanceDataStepRate = 0;

        ie[1].SemanticName = "TEXCOORD";
        ie[1].SemanticIndex = 0;
        ie[1].Format = DXGI_FORMAT_R32G32_FLOAT;
        ie[1].InputSlot = 0;
        ie[1].AlignedByteOffset = 8;
        ie[1].InputSlotClass = D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA;
        ie[1].InstanceDataStepRate = 0;

        D3D12_RENDER_TARGET_BLEND_DESC rtBlend{};
        rtBlend.BlendEnable = FALSE;
        rtBlend.LogicOpEnable = FALSE;
        rtBlend.SrcBlend = D3D12_BLEND_ONE;
        rtBlend.DestBlend = D3D12_BLEND_ZERO;
        rtBlend.BlendOp = D3D12_BLEND_OP_ADD;
        rtBlend.SrcBlendAlpha = D3D12_BLEND_ONE;
        rtBlend.DestBlendAlpha = D3D12_BLEND_ZERO;
        rtBlend.BlendOpAlpha = D3D12_BLEND_OP_ADD;
        rtBlend.LogicOp = D3D12_LOGIC_OP_NOOP;
        rtBlend.RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;

        D3D12_BLEND_DESC blendDesc{};
        blendDesc.AlphaToCoverageEnable = FALSE;
        blendDesc.IndependentBlendEnable = FALSE;
        blendDesc.RenderTarget[0] = rtBlend;

        D3D12_RASTERIZER_DESC rasterDesc{};
        rasterDesc.FillMode = D3D12_FILL_MODE_SOLID;
        rasterDesc.CullMode = D3D12_CULL_MODE_NONE;   // 全屏 quad，无背面剔除
        rasterDesc.FrontCounterClockwise = FALSE;
        rasterDesc.DepthBias = D3D12_DEFAULT_DEPTH_BIAS;
        rasterDesc.DepthBiasClamp = D3D12_DEFAULT_DEPTH_BIAS_CLAMP;
        rasterDesc.SlopeScaledDepthBias = D3D12_DEFAULT_SLOPE_SCALED_DEPTH_BIAS;
        rasterDesc.DepthClipEnable = TRUE;
        rasterDesc.MultisampleEnable = FALSE;
        rasterDesc.AntialiasedLineEnable = FALSE;

        D3D12_DEPTH_STENCIL_DESC dsDesc{};
        dsDesc.DepthEnable = FALSE;
        dsDesc.DepthWriteMask = D3D12_DEPTH_WRITE_MASK_ZERO;
        dsDesc.StencilEnable = FALSE;

        D3D12_GRAPHICS_PIPELINE_STATE_DESC psoDesc{};
        psoDesc.pRootSignature = rootSig.Get();
        psoDesc.VS = { vsBlob->GetBufferPointer(), vsBlob->GetBufferSize() };
        psoDesc.PS = { psBlob->GetBufferPointer(), psBlob->GetBufferSize() };
        psoDesc.InputLayout = { ie, 2 };
        psoDesc.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
        psoDesc.NumRenderTargets = 1;
        psoDesc.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;  // 匹配 backbuffer
        psoDesc.DSVFormat = DXGI_FORMAT_UNKNOWN;
        psoDesc.SampleDesc.Count = 1;
        psoDesc.BlendState = blendDesc;
        psoDesc.RasterizerState = rasterDesc;
        psoDesc.DepthStencilState = dsDesc;
        psoDesc.Flags = D3D12_PIPELINE_STATE_FLAGS(0);

        HRESULT hr = gCtx.device->CreateGraphicsPipelineState(&psoDesc,
            IID_PPV_ARGS(&pso));
        if (FAILED(hr)) {
            err = "initBlitPipeline: CreateGraphicsPipelineState hr=0x" + hrText(hr);
            return;
        }
    }

    // 顶点缓冲：4 顶点，R32G32(position) + R32G32(texcoord)，upload heap
    {
        struct Vertex { float x, y; float u, v; };
        Vertex verts[4] = {
            {-1.0f, -1.0f, 0.0f, 1.0f},   // bottom-left
            { 1.0f, -1.0f, 1.0f, 1.0f},   // bottom-right
            {-1.0f,  1.0f, 0.0f, 0.0f},   // top-left
            { 1.0f,  1.0f, 1.0f, 0.0f},   // top-right
        };
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_UPLOAD;
        hp.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
        hp.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
        D3D12_RESOURCE_DESC rd{};
        rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        rd.Width = sizeof(verts);
        rd.Height = 1;
        rd.DepthOrArraySize = 1;
        rd.MipLevels = 1;
        rd.SampleDesc.Count = 1;
        rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        rd.Flags = D3D12_RESOURCE_FLAG_NONE;
        HRESULT hr = gCtx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &rd, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr,
            IID_PPV_ARGS(&gCtx.blitPipeline->vertBuf));
        if (FAILED(hr)) { err = "initBlitPipeline: vertBuf create hr=0x" + hrText(hr); return; }
        void* p = nullptr;
        hr = gCtx.blitPipeline->vertBuf->Map(0, nullptr, &p);
        if (SUCCEEDED(hr) && p) {
            std::memcpy(p, verts, sizeof(verts));
            gCtx.blitPipeline->vertBuf->Unmap(0, nullptr);
        }
        gCtx.blitPipeline->vbView.BufferLocation =
            gCtx.blitPipeline->vertBuf->GetGPUVirtualAddress();
        gCtx.blitPipeline->vbView.SizeInBytes = sizeof(verts);
        gCtx.blitPipeline->vbView.StrideInBytes = sizeof(Vertex);
    }

    // 索引缓冲：2 个三角形（顺时针，CullMode=NONE，顺序无关）
    {
        uint16_t idxs[6] = {0, 1, 2, 1, 3, 2};
        D3D12_HEAP_PROPERTIES hp{};
        hp.Type = D3D12_HEAP_TYPE_UPLOAD;
        D3D12_RESOURCE_DESC rd{};
        rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        rd.Width = sizeof(idxs);
        rd.Height = 1; rd.DepthOrArraySize = 1; rd.MipLevels = 1;
        rd.SampleDesc.Count = 1; rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        rd.Flags = D3D12_RESOURCE_FLAG_NONE;
        HRESULT hr = gCtx.device->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &rd, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr,
            IID_PPV_ARGS(&gCtx.blitPipeline->idxBuf));
        if (FAILED(hr)) { err = "initBlitPipeline: idxBuf create hr=0x" + hrText(hr); return; }
        void* p = nullptr;
        hr = gCtx.blitPipeline->idxBuf->Map(0, nullptr, &p);
        if (SUCCEEDED(hr) && p) {
            std::memcpy(p, idxs, sizeof(idxs));
            gCtx.blitPipeline->idxBuf->Unmap(0, nullptr);
        }
        gCtx.blitPipeline->ibView.BufferLocation =
            gCtx.blitPipeline->idxBuf->GetGPUVirtualAddress();
        gCtx.blitPipeline->ibView.SizeInBytes = sizeof(idxs);
        gCtx.blitPipeline->ibView.Format = DXGI_FORMAT_R16_UINT;
    }

    gCtx.blitPipeline->rootSig = rootSig;
    gCtx.blitPipeline->pso = pso;
    dbgLog("initBlitPipeline: done rootSig=%p pso=%p",
        (void*)rootSig.Get(), (void*)pso.Get());
}

const BlitPipeline* getBlitPipeline() {
    return gCtx.blitPipeline.get();
}

bool blitBindSourceTexture(CommandContext* ctx, Dx12Object* srcTex,
    ID3D12GraphicsCommandList* cmd, std::string& err) {
    if (!srcTex || !srcTex->resource) {
        err = "blitBindSourceTexture: null or invalid srcTex";
        return false;
    }
    // 源纹理过渡到 PIXEL_SHADER_RESOURCE（采样必需）。
    transitionTextureTo(ctx, srcTex,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE
        | D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);

    // 在 srvHeap 分配 SRV 槽位并创建描述符。
    std::string srvErr;
    int srvSlot = allocSrvSlot(srvErr);
    if (srvSlot < 0) {
        err = "blitBindSourceTexture: allocSrvSlot failed: " + srvErr;
        return false;
    }
    D3D12_CPU_DESCRIPTOR_HANDLE srvCpu = gCtx.srvCpuHeap->GetCPUDescriptorHandleForHeapStart();
    srvCpu.ptr += (SIZE_T)srvSlot * gCtx.srvInc;
    D3D12_GPU_DESCRIPTOR_HANDLE srvGpu = gCtx.srvHeap->GetGPUDescriptorHandleForHeapStart();
    srvGpu.ptr += (SIZE_T)srvSlot * gCtx.srvInc;

    DXGI_FORMAT viewFmt = srcTex->resource->GetDesc().Format;
    switch (viewFmt) {
        case DXGI_FORMAT_D32_FLOAT:            viewFmt = DXGI_FORMAT_R32_FLOAT; break;
        case DXGI_FORMAT_D16_UNORM:            viewFmt = DXGI_FORMAT_R16_UNORM; break;
        default: break;
    }
    D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc{};
    srvDesc.Format = viewFmt;
    srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
    srvDesc.Texture2D.MipLevels = 1;
    srvDesc.Texture2D.MostDetailedMip = 0;
    gCtx.device->CreateShaderResourceView(srcTex->resource.Get(), &srvDesc, srvCpu);
    // srvHeap 是 SHADER_VISIBLE 堆，CPU handle 与 GPU handle 指向同一描述符；
    // CreateShaderResourceView 只接受 CPU 句柄。

    // 绑定根描述符表（Offset=APPEND → 自动使用 srvGpu）。
    cmd->SetGraphicsRootDescriptorTable(0, srvGpu);
    dbgLog("blitBindSourceTexture: srvSlot=%d srvGpu=%llx",
        srvSlot, (unsigned long long)srvGpu.ptr);
    return true;
}

}  // namespace dx12mc
