// DLSS 超分（Super Resolution）与 DLSS 帧生成（DLSS-G）渲染实现。
//
// 资源与数据流：
//   1. 世界渲染完成（深度尚未被手部/GUI 清除）时，Java 侧调用
//      ngxDlssCaptureFrame：用 compute shader 读取世界深度，写出保留深度
//      (R32_FLOAT) 与相机运动矢量 (R16G16_FLOAT)。
//   2. 最终 blit 之前，ngxDlssEvaluateSr 以低分辨率主颜色 + 深度 + MV 求值
//      DLSS 超分，输出全分辨率纹理；blit 改用它作为源。
//   3. blit 绘制完成后，ngxDlssAfterBlit 把真实 backbuffer 复制到保留纹理，
//      作为 DLSS-G 的 pBackbuffer 求值 1..3 张生成帧，并把待呈现条目入队。
//   4. 渲染线程在 submit 之后调用 ngxDlssPresentGeneratedFrames：依次把生成帧
//      和保留的真实帧复制到 swapchain backbuffer 并 present。
//
// 运动矢量只包含相机运动（没有逐物体 MV），这是当前模组的已知精度限制。

#include "ngx_dlss_render.h"

#include <windows.h>
#include <d3d11.h>
#include <d3d12.h>
#include <d3dcompiler.h>

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <vector>

#include "nvsdk_ngx.h"
#include "nvsdk_ngx_defs.h"
#include "nvsdk_ngx_defs_dlssg.h"
#include "nvsdk_ngx_helpers.h"
#include "nvsdk_ngx_helpers_d3d.h"
#include "nvsdk_ngx_helpers_dlssg_d3d.h"

#include "dx12_device.h"
#include "ngx_dlss.h"
#include "xess_render.h"

namespace dx12mc {
namespace {

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

const char* ngxResultText(NVSDK_NGX_Result r) {
    static char buf[32];
    std::snprintf(buf, sizeof(buf), "0x%08x", (unsigned)r);
    return buf;
}

std::string hrTextLocal(HRESULT hr) {
    char buf[32];
    std::snprintf(buf, sizeof(buf), "0x%08x", (unsigned)hr);
    return std::string(buf);
}

bool compileCs(const char* src, const char* name, ComPtr<ID3DBlob>& out,
    std::string& err) {
    ComPtr<ID3DBlob> errBlob;
    HRESULT hr = D3DCompile(src, std::strlen(src), name, nullptr, nullptr,
        "main", "cs_5_1", 0, 0, &out, &errBlob);
    if (FAILED(hr)) {
        std::string msg;
        if (errBlob && errBlob->GetBufferSize() > 0) {
            const char* m = static_cast<const char*>(errBlob->GetBufferPointer());
            msg.assign(m, errBlob->GetBufferSize());
        }
        err = std::string("D3DCompile(cs_5_1) hr=") + hrTextLocal(hr)
            + (msg.empty() ? "" : "\n" + msg);
        return false;
    }
    return true;
}

// 行主序、行向量约定：C = A * B（C[i][j] = sum_k A[i][k] * B[k][j]）。
void mulMat(const float* a, const float* b, float* out) {
    float r[16];
    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 4; ++j) {
            float s = 0.0f;
            for (int k = 0; k < 4; ++k) {
                s += a[i * 4 + k] * b[k * 4 + j];
            }
            r[i * 4 + j] = s;
        }
    }
    std::memcpy(out, r, sizeof(r));
}

ID3D12Resource* createTex2D(DXGI_FORMAT fmt, UINT w, UINT h,
    D3D12_RESOURCE_FLAGS flags, std::string& err) {
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device) {
        err = "DLSS: device not initialized";
        return nullptr;
    }
    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = std::max(1u, w);
    desc.Height = std::max(1u, h);
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = fmt;
    desc.SampleDesc.Count = 1;
    desc.Flags = flags;

    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_DEFAULT;

    ID3D12Resource* res = nullptr;
    HRESULT hr = dc.device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE,
        &desc, D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(&res));
    if (FAILED(hr)) {
        err = "DLSS: CreateCommittedResource failed " + hrTextLocal(hr);
        return nullptr;
    }
    return res;
}

// ---------------------------------------------------------------------------
// 配置 / 状态
// ---------------------------------------------------------------------------

NgxDlssConfig gCfg;

// 外部超分后端（XeSS）请求捕获：即使 DLSS 关闭也要生成深度/运动矢量。
std::atomic<bool> gExternalCapture{false};

bool gFrameActive = false;
bool gNotRenderingGameFrames = false;
UINT64 gFrameCounter = 0;

bool gSrNeedsReset = true;
bool gFgNeedsReset = true;
bool gMvReset = true;

// 最近一次成功捕获的槽位/帧号。blit 时据此确认深度/MV 属于本帧。
int gCaptureSlot = -1;
UINT64 gCaptureFrameId = 0;

// 资源尺寸（由 capture 更新；资源重建时使用）。
UINT gPendingRenderW = 0, gPendingRenderH = 0;
UINT gPendingOutW = 0, gPendingOutH = 0;
bool gResourcesDirty = true;

// 上一帧的未抖动 VP / invVP（行主序，行向量），供运动矢量与 DLSS-G 使用。
bool gHasPrev = false;
float gPrevViewProj[16] = {};
float gPrevInvViewProj[16] = {};

struct SlotResources {
    ComPtr<ID3D12Resource> depth;   // R32_FLOAT，渲染分辨率，SRV+UAV
    ComPtr<ID3D12Resource> mv;      // R16G16_FLOAT，渲染分辨率，UAV
    ComPtr<ID3D12Resource> srOut;   // RGBA8，输出分辨率，UAV
    ComPtr<ID3D12Resource> real;    // RGBA8，输出分辨率，COPY_DEST/SRV
    ComPtr<ID3D12Resource> gen[3];  // RGBA8，输出分辨率，UAV/COPY_SOURCE
    UINT renderW = 0, renderH = 0;
    UINT outW = 0, outH = 0;

    // 捕获只需要深度/运动矢量；DLSS 输出纹理按需分配，避免 XeSS-only 时
    // 白白占用 5 张全分辨率纹理 × 3 个槽位。
    bool valid() const {
        return depth && mv;
    }

    bool dlssValid() const {
        return srOut && real && gen[0] && gen[1] && gen[2];
    }
};

SlotResources gSlots[3];

struct FrameCapture {
    bool valid = false;
    int slot = -1;
    UINT64 frameId = 0;
    UINT renderW = 0, renderH = 0;
    UINT outW = 0, outH = 0;
    float viewProj[16] = {};
    float invViewProj[16] = {};
    float invViewProjJittered[16] = {};
    float proj[16] = {};
    float invProj[16] = {};
    float prevViewProj[16] = {};
    float prevInvViewProj[16] = {};
    float cameraPos[3] = {};
    float cameraUp[3] = {};
    float cameraRight[3] = {};
    float cameraFwd[3] = {};
    float cameraNear = 0.05f;
    float cameraFar = 512.0f;
    float cameraFov = 1.2f;
    float cameraAspect = 1.0f;
    float jitterPx = 0.0f;
    float jitterPy = 0.0f;
};

FrameCapture gCapture;

// NGX feature / 参数
NVSDK_NGX_Handle* gSrFeature = nullptr;
NVSDK_NGX_Parameter* gSrParams = nullptr;
int gSrFeatureQuality = -1;
int gSrFeaturePreset = -1;
UINT gSrFeatureRenderW = 0, gSrFeatureRenderH = 0;
UINT gSrFeatureOutW = 0, gSrFeatureOutH = 0;
// CreateFeature 实测失败后置位，避免每帧都重试（每次重试都要 deviceWaitIdle）。
// 配置变化时清除，让用户改档位后有重新尝试的机会。
bool gSrProbeFailed = false;

NVSDK_NGX_Handle* gFgFeature = nullptr;
NVSDK_NGX_Parameter* gFgParams = nullptr;
UINT gFgFeatureRenderW = 0, gFgFeatureRenderH = 0;
UINT gFgFeatureOutW = 0, gFgFeatureOutH = 0;
UINT gFgMaxFrames = 1;
std::atomic<bool> gFgFeatureActive{false};

// 运动矢量/深度拷贝 compute 管线
struct ComputePipeline {
    ComPtr<ID3D12RootSignature> rootSig;
    ComPtr<ID3D12PipelineState> pso;
    ComPtr<ID3D12DescriptorHeap> heap;
    UINT inc = 0;
    bool ready = false;
};

ComputePipeline gCompute;

// 生成帧待呈现队列
struct PendingFrame {
    UINT64 key = 0;
    Dx12Surface* surface = nullptr;
    int slot = -1;
    int count = 0;
    UINT64 frameId = 0;
};

std::mutex gPendingMtx;
std::vector<PendingFrame> gPending;

CommandContext* gPresentCtx = nullptr;

// ---------------------------------------------------------------------------
// 运动矢量 / 深度拷贝 compute
// ---------------------------------------------------------------------------

const char* kMvShader = R"(
cbuffer Params : register(b0, space0)
{
    row_major float4x4 curVP;
    row_major float4x4 invVPJittered;
    row_major float4x4 prevVP;
    float2 renderSize;
    float2 pad;
};

Texture2D<float> inDepth : register(t0, space0);
RWTexture2D<float> outDepth : register(u0, space0);
RWTexture2D<float2> outMv : register(u1, space0);

[numthreads(8, 8, 1)]
void main(uint3 tid : SV_DispatchThreadID)
{
    uint w = (uint)renderSize.x;
    uint h = (uint)renderSize.y;
    if (tid.x >= w || tid.y >= h) return;

    float d = inDepth.Load(int3(tid.x, tid.y, 0));
    outDepth[tid.x, tid.y] = d;

    float2 uv = (float2(tid.xy) + 0.5) / renderSize;
    float4 clip = float4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, d, 1.0);
    float4 world = mul(clip, invVPJittered);
    if (abs(world.w) < 1e-7) {
        outMv[tid.x, tid.y] = float2(0.0, 0.0);
        return;
    }
    world /= world.w;

    float4 curClip = mul(world, curVP);
    float4 prevClip = mul(world, prevVP);
    if (abs(curClip.w) < 1e-7 || abs(prevClip.w) < 1e-7) {
        outMv[tid.x, tid.y] = float2(0.0, 0.0);
        return;
    }

    float2 curNdc = curClip.xy / curClip.w;
    float2 prevNdc = prevClip.xy / prevClip.w;
    // 官方约定：current -> previous，单位为渲染像素；屏幕空间 y 向下。
    float2 mv = (prevNdc - curNdc) * float2(0.5 * renderSize.x, -0.5 * renderSize.y);
    outMv[tid.x, tid.y] = mv;
}
)";

bool ensureComputePipeline(std::string& err) {
    if (gCompute.ready) return true;
    DeviceContext& dc = deviceContextForJni();
    if (!dc.device) {
        err = "DLSS compute: device not initialized";
        return false;
    }

    D3D12_DESCRIPTOR_RANGE ranges[2]{};
    ranges[0].RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    ranges[0].NumDescriptors = 1;
    ranges[0].BaseShaderRegister = 0;
    ranges[0].RegisterSpace = 0;
    ranges[0].OffsetInDescriptorsFromTableStart = 0;
    ranges[1].RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_UAV;
    ranges[1].NumDescriptors = 2;
    ranges[1].BaseShaderRegister = 0;
    ranges[1].RegisterSpace = 0;
    ranges[1].OffsetInDescriptorsFromTableStart = 1;

    D3D12_ROOT_PARAMETER params[2]{};
    params[0].ParameterType = D3D12_ROOT_PARAMETER_TYPE_32BIT_CONSTANTS;
    params[0].Constants.ShaderRegister = 0;
    params[0].Constants.RegisterSpace = 0;
    params[0].Constants.Num32BitValues = 52;
    params[0].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    params[1].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    params[1].DescriptorTable.NumDescriptorRanges = 2;
    params[1].DescriptorTable.pDescriptorRanges = ranges;
    params[1].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    D3D12_ROOT_SIGNATURE_DESC rsDesc{};
    rsDesc.NumParameters = 2;
    rsDesc.pParameters = params;
    rsDesc.NumStaticSamplers = 0;
    rsDesc.pStaticSamplers = nullptr;
    rsDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_NONE;

    ComPtr<ID3DBlob> sigBlob;
    ComPtr<ID3DBlob> sigErr;
    HRESULT hr = D3D12SerializeRootSignature(&rsDesc, D3D_ROOT_SIGNATURE_VERSION_1,
        &sigBlob, &sigErr);
    if (FAILED(hr)) {
        err = "DLSS compute: serialize root signature failed " + hrTextLocal(hr);
        return false;
    }
    hr = dc.device->CreateRootSignature(0, sigBlob->GetBufferPointer(),
        sigBlob->GetBufferSize(), IID_PPV_ARGS(&gCompute.rootSig));
    if (FAILED(hr)) {
        err = "DLSS compute: CreateRootSignature failed " + hrTextLocal(hr);
        return false;
    }

    ComPtr<ID3DBlob> csBlob;
    if (!compileCs(kMvShader, "dlss_mv_cs", csBlob, err)) return false;

    D3D12_COMPUTE_PIPELINE_STATE_DESC psoDesc{};
    psoDesc.pRootSignature = gCompute.rootSig.Get();
    psoDesc.CS.pShaderBytecode = csBlob->GetBufferPointer();
    psoDesc.CS.BytecodeLength = csBlob->GetBufferSize();
    hr = dc.device->CreateComputePipelineState(&psoDesc, IID_PPV_ARGS(&gCompute.pso));
    if (FAILED(hr)) {
        err = "DLSS compute: CreateComputePipelineState failed " + hrTextLocal(hr);
        return false;
    }

    D3D12_DESCRIPTOR_HEAP_DESC heapDesc{};
    heapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    heapDesc.NumDescriptors = 3 * 3;  // 3 个槽位 × [depth SRV, depth UAV, mv UAV]
    heapDesc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
    hr = dc.device->CreateDescriptorHeap(&heapDesc, IID_PPV_ARGS(&gCompute.heap));
    if (FAILED(hr)) {
        err = "DLSS compute: CreateDescriptorHeap failed " + hrTextLocal(hr);
        return false;
    }
    gCompute.inc = dc.device->GetDescriptorHandleIncrementSize(
        D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    gCompute.ready = true;
    dbgLog("DLSS compute: pipeline ready");
    return true;
}

D3D12_CPU_DESCRIPTOR_HANDLE computeCpuHandle(int slot, int index) {
    D3D12_CPU_DESCRIPTOR_HANDLE h = gCompute.heap->GetCPUDescriptorHandleForHeapStart();
    h.ptr += (SIZE_T)(slot * 3 + index) * gCompute.inc;
    return h;
}

D3D12_GPU_DESCRIPTOR_HANDLE computeGpuHandle(int slot) {
    D3D12_GPU_DESCRIPTOR_HANDLE h = gCompute.heap->GetGPUDescriptorHandleForHeapStart();
    h.ptr += (SIZE_T)(slot * 3) * gCompute.inc;
    return h;
}

void restoreDrawHeap(CommandContext* ctx) {
    if (!ctx) return;
    DeviceContext& dc = deviceContextForJni();
    if (dc.drawHeap) {
        ID3D12DescriptorHeap* heaps[] = { dc.drawHeap.Get() };
        ctx->commandList->SetDescriptorHeaps(1, heaps);
    }
    ctx->cachedPso = nullptr;
    ctx->currentPipeline = nullptr;
}

// ---------------------------------------------------------------------------
// 资源生命周期
// ---------------------------------------------------------------------------

void releaseFeatures() {
    if (gSrFeature) {
        NVSDK_NGX_D3D12_ReleaseFeature(gSrFeature);
        gSrFeature = nullptr;
    }
    if (gSrParams) {
        NVSDK_NGX_D3D12_DestroyParameters(gSrParams);
        gSrParams = nullptr;
    }
    gSrFeatureQuality = -1;
    gSrFeaturePreset = -1;
    gSrFeatureRenderW = gSrFeatureRenderH = 0;
    gSrFeatureOutW = gSrFeatureOutH = 0;

    gFgFeatureActive.store(false);
    if (gFgFeature) {
        NVSDK_NGX_D3D12_ReleaseFeature(gFgFeature);
        gFgFeature = nullptr;
    }
    if (gFgParams) {
        NVSDK_NGX_D3D12_DestroyParameters(gFgParams);
        gFgParams = nullptr;
    }
    gFgFeatureRenderW = gFgFeatureRenderH = 0;
    gFgFeatureOutW = gFgFeatureOutH = 0;
    gFgMaxFrames = 1;
}

void releaseSlotResources() {
    for (auto& s : gSlots) {
        s.depth.Reset();
        s.mv.Reset();
        s.srOut.Reset();
        s.real.Reset();
        for (auto& g : s.gen) g.Reset();
        s.renderW = s.renderH = s.outW = s.outH = 0;
    }
}

bool createSlotResources(UINT renderW, UINT renderH, UINT outW, UINT outH,
    std::string& err) {
    releaseSlotResources();
    // DLSS SR / DLSS-G 关闭时只分配捕获用的深度与运动矢量纹理。
    const bool needDlss = gCfg.srEnabled || gCfg.fgEnabled;
    for (int i = 0; i < 3; ++i) {
        SlotResources& s = gSlots[i];
        s.depth.Attach(createTex2D(DXGI_FORMAT_R32_FLOAT, renderW, renderH,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, err));
        if (!s.depth) return false;
        s.mv.Attach(createTex2D(DXGI_FORMAT_R16G16_FLOAT, renderW, renderH,
            D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, err));
        if (!s.mv) return false;
        if (needDlss) {
            s.srOut.Attach(createTex2D(DXGI_FORMAT_R8G8B8A8_UNORM, outW, outH,
                D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, err));
            if (!s.srOut) return false;
            s.real.Attach(createTex2D(DXGI_FORMAT_R8G8B8A8_UNORM, outW, outH,
                D3D12_RESOURCE_FLAG_NONE, err));
            if (!s.real) return false;
            for (int k = 0; k < 3; ++k) {
                s.gen[k].Attach(createTex2D(DXGI_FORMAT_R8G8B8A8_UNORM, outW, outH,
                    D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS, err));
                if (!s.gen[k]) return false;
            }
        }
        s.renderW = renderW;
        s.renderH = renderH;
        s.outW = outW;
        s.outH = outH;
    }
    dbgLog("DLSS: resources created render=%ux%u out=%ux%u dlss=%d", renderW, renderH,
        outW, outH, (int)needDlss);
    return true;
}

bool ensureResources(CommandContext* ctx, Dx12Surface* surface, std::string& err) {
    (void)ctx;
    (void)surface;
    if (!gResourcesDirty && gSlots[0].valid()) return true;
    if (gPendingRenderW == 0 || gPendingRenderH == 0 ||
        gPendingOutW == 0 || gPendingOutH == 0) {
        err = "DLSS: resource size unknown";
        return false;
    }
    std::string waitErr;
    if (!deviceWaitIdle(waitErr)) {
        err = "DLSS: deviceWaitIdle before resource recreate failed: " + waitErr;
        return false;
    }
    releaseFeatures();
    if (!createSlotResources(gPendingRenderW, gPendingRenderH,
            gPendingOutW, gPendingOutH, err)) {
        return false;
    }
    gResourcesDirty = false;
    gSrNeedsReset = true;
    gFgNeedsReset = true;
    gMvReset = true;
    gHasPrev = false;
    return true;
}

// ---------------------------------------------------------------------------
// NGX feature 创建
// ---------------------------------------------------------------------------

NVSDK_NGX_PerfQuality_Value mapQuality(int q) {
    switch (q) {
        case 0: return NVSDK_NGX_PerfQuality_Value_DLAA;             // DLAA
        case 1: return NVSDK_NGX_PerfQuality_Value_MaxQuality;       // Quality
        case 2: return NVSDK_NGX_PerfQuality_Value_Balanced;         // Balanced
        case 3: return NVSDK_NGX_PerfQuality_Value_MaxPerf;          // Performance
        case 4: return NVSDK_NGX_PerfQuality_Value_UltraPerformance; // Ultra Performance
        default: return NVSDK_NGX_PerfQuality_Value_MaxQuality;      // Custom
    }
}

bool acquireCapabilityParams(NVSDK_NGX_Parameter** out, std::string& err) {
    *out = nullptr;
    NVSDK_NGX_Result r = NVSDK_NGX_D3D12_GetCapabilityParameters(out);
    if (NVSDK_NGX_FAILED(r) || !*out) {
        err = std::string("DLSS: GetCapabilityParameters failed ") + ngxResultText(r);
        return false;
    }
    return true;
}

bool ensureSrFeature(CommandContext* ctx, UINT renderW, UINT renderH,
    UINT outW, UINT outH, std::string& err) {
    if (gSrFeature && gSrFeatureQuality == gCfg.quality
        && gSrFeaturePreset == gCfg.preset
        && gSrFeatureRenderW == renderW && gSrFeatureRenderH == renderH
        && gSrFeatureOutW == outW && gSrFeatureOutH == outH) {
        return true;
    }

    std::string waitErr;
    if (!deviceWaitIdle(waitErr)) {
        err = "DLSS SR: deviceWaitIdle before feature recreate failed: " + waitErr;
        return false;
    }
    releaseFeatures();

    NVSDK_NGX_Parameter* params = nullptr;
    if (!acquireCapabilityParams(&params, err)) return false;

    // 渲染预设提示：五档全部设置，NGX 按实际使用的档位选取。
    params->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_DLAA, gCfg.preset);
    params->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_Quality, gCfg.preset);
    params->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_Balanced, gCfg.preset);
    params->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_Performance, gCfg.preset);
    params->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_UltraPerformance, gCfg.preset);
    params->Set(NVSDK_NGX_Parameter_DLSS_Hint_Render_Preset_UltraQuality, gCfg.preset);

    NVSDK_NGX_DLSS_Create_Params cp{};
    cp.Feature.InWidth = renderW;
    cp.Feature.InHeight = renderH;
    cp.Feature.InTargetWidth = outW;
    cp.Feature.InTargetHeight = outH;
    cp.Feature.InPerfQualityValue = mapQuality(gCfg.quality);
    cp.InFeatureCreateFlags = NVSDK_NGX_DLSS_Feature_Flags_MVLowRes
        | NVSDK_NGX_DLSS_Feature_Flags_DepthInverted;
    cp.InEnableOutputSubrects = false;

    NVSDK_NGX_Handle* handle = nullptr;
    NVSDK_NGX_Result r = NGX_D3D12_CREATE_DLSS_EXT(ctx->commandList, 1, 1,
        &handle, params, &cp);
    restoreDrawHeap(ctx);
    if (NVSDK_NGX_FAILED(r) || !handle) {
        err = std::string("DLSS SR: CreateFeature failed ") + ngxResultText(r);
        gSrProbeFailed = true;
        NVSDK_NGX_D3D12_DestroyParameters(params);
        return false;
    }

    gSrFeature = handle;
    gSrParams = params;
    gSrFeatureQuality = gCfg.quality;
    gSrFeaturePreset = gCfg.preset;
    gSrFeatureRenderW = renderW;
    gSrFeatureRenderH = renderH;
    gSrFeatureOutW = outW;
    gSrFeatureOutH = outH;
    gSrNeedsReset = true;
    gSrProbeFailed = false;
    // 初始化阶段 SuperSampling_Available 在 ProjectID + CUSTOM 组合下会误报不可用，
    // 因此以「能否真正创建 feature」作为最终判据回写状态。
    ngxSetSrAvailable(true);
    dbgLog("DLSS SR: feature created render=%ux%u out=%ux%u quality=%d preset=%d",
        renderW, renderH, outW, outH, gCfg.quality, gCfg.preset);
    return true;
}

bool ensureFgFeature(CommandContext* ctx, UINT renderW, UINT renderH,
    UINT outW, UINT outH, std::string& err) {
    if (gFgFeature && gFgFeatureRenderW == renderW && gFgFeatureRenderH == renderH
        && gFgFeatureOutW == outW && gFgFeatureOutH == outH) {
        return true;
    }

    std::string waitErr;
    if (!deviceWaitIdle(waitErr)) {
        err = "DLSS-G: deviceWaitIdle before feature recreate failed: " + waitErr;
        return false;
    }
    gFgFeatureActive.store(false);
    if (gFgFeature) {
        NVSDK_NGX_D3D12_ReleaseFeature(gFgFeature);
        gFgFeature = nullptr;
    }
    if (gFgParams) {
        NVSDK_NGX_D3D12_DestroyParameters(gFgParams);
        gFgParams = nullptr;
    }

    NVSDK_NGX_Parameter* params = nullptr;
    if (!acquireCapabilityParams(&params, err)) return false;

    NVSDK_NGX_DLSSG_Create_Params cp{};
    cp.Width = outW;
    cp.Height = outH;
    cp.NativeBackbufferFormat = DXGI_FORMAT_R8G8B8A8_UNORM;
    cp.RenderWidth = renderW;
    cp.RenderHeight = renderH;
    cp.DynamicResolutionScaling = false;

    NVSDK_NGX_Handle* handle = nullptr;
    NVSDK_NGX_Result r = NGX_D3D12_CREATE_DLSSG(ctx->commandList, 1, 1,
        &handle, params, &cp);
    restoreDrawHeap(ctx);
    if (NVSDK_NGX_FAILED(r) || !handle) {
        err = std::string("DLSS-G: CreateFeature failed ") + ngxResultText(r);
        NVSDK_NGX_D3D12_DestroyParameters(params);
        return false;
    }

    unsigned int maxFrames = 0;
    if (!NVSDK_NGX_FAILED(params->Get(NVSDK_NGX_DLSSG_Parameter_MultiFrameCountMax,
            &maxFrames)) && maxFrames > 0) {
        gFgMaxFrames = std::min<UINT>(maxFrames, 3u);
    } else {
        gFgMaxFrames = 1;
    }

    gFgFeature = handle;
    gFgParams = params;
    gFgFeatureRenderW = renderW;
    gFgFeatureRenderH = renderH;
    gFgFeatureOutW = outW;
    gFgFeatureOutH = outH;
    gFgNeedsReset = true;
    gFgFeatureActive.store(true);
    dbgLog("DLSS-G: feature created render=%ux%u out=%ux%u maxFrames=%u",
        renderW, renderH, outW, outH, gFgMaxFrames);
    return true;
}

// ---------------------------------------------------------------------------
// 捕获
// ---------------------------------------------------------------------------

bool captureFrame(CommandContext* ctx, Dx12Surface* s, Dx12Object* depthTex,
    const float* data, int count, std::string& err) {
    if (!ctx || !ctx->listOpen || !ctx->commandList) {
        err = "DLSS capture: command list not open";
        return false;
    }
    if (!s || !depthTex || !depthTex->resource) {
        err = "DLSS capture: invalid surface/depth";
        return false;
    }
    if (count < 98) {
        err = "DLSS capture: data buffer too small";
        return false;
    }
    const bool dlssWanted = gCfg.srEnabled || gCfg.fgEnabled;
    if (!dlssWanted && !gExternalCapture.load()) {
        return false;
    }
    // 外部后端（XeSS）不依赖 NGX；只有 DLSS 本身需要 NGX 初始化成功。
    if (dlssWanted && !ngxIsInitialized()) {
        err = "DLSS capture: NGX not initialized";
        return false;
    }

    const D3D12_RESOURCE_DESC dd = depthTex->resource->GetDesc();
    const UINT renderW = (UINT)dd.Width;
    const UINT renderH = (UINT)dd.Height;
    const UINT outW = s->width;
    const UINT outH = s->height;
    if (renderW == 0 || renderH == 0 || outW == 0 || outH == 0) {
        err = "DLSS capture: zero dimensions";
        return false;
    }

    gPendingRenderW = renderW;
    gPendingRenderH = renderH;
    gPendingOutW = outW;
    gPendingOutH = outH;

    const int slot = (int)ctx->currentListSlot;
    if (slot < 0 || slot >= 3) {
        err = "DLSS capture: bad list slot";
        return false;
    }
    SlotResources& sr = gSlots[slot];
    if (gResourcesDirty || !sr.valid() || sr.renderW != renderW
        || sr.renderH != renderH || sr.outW != outW || sr.outH != outH) {
        // 资源重建留给 blit 前的 ensureResources（需要 deviceWaitIdle）。
        gResourcesDirty = true;
        gSrNeedsReset = true;
        gFgNeedsReset = true;
        gMvReset = true;
        err = "DLSS capture: resources need recreate";
        return false;
    }

    if (!ensureComputePipeline(err)) return false;

    // 输入/输出状态。
    transitionTextureTo(ctx, depthTex,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, sr.depth.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_UNORDERED_ACCESS);
    transitionTo(ctx, sr.mv.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_UNORDERED_ACCESS);

    // 每个槽位固定 3 个描述符；同一槽位上一帧（N-3）已由 beginCommandList 等待完成。
    D3D12_CPU_DESCRIPTOR_HANDLE cpu = computeCpuHandle(slot, 0);
    DeviceContext& dc = deviceContextForJni();

    D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc{};
    srvDesc.Format = DXGI_FORMAT_R32_FLOAT;
    srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
    srvDesc.Texture2D.MipLevels = 1;
    dc.device->CreateShaderResourceView(depthTex->resource.Get(), &srvDesc, cpu);

    D3D12_UNORDERED_ACCESS_VIEW_DESC uavDesc{};
    uavDesc.Format = DXGI_FORMAT_R32_FLOAT;
    uavDesc.ViewDimension = D3D12_UAV_DIMENSION_TEXTURE2D;
    D3D12_CPU_DESCRIPTOR_HANDLE depthUav = computeCpuHandle(slot, 1);
    dc.device->CreateUnorderedAccessView(sr.depth.Get(), nullptr, &uavDesc, depthUav);

    uavDesc.Format = DXGI_FORMAT_R16G16_FLOAT;
    D3D12_CPU_DESCRIPTOR_HANDLE mvUav = computeCpuHandle(slot, 2);
    dc.device->CreateUnorderedAccessView(sr.mv.Get(), nullptr, &uavDesc, mvUav);

    // 常量：curVP、invVPJittered、prevVP、renderSize、pad。
    float constants[52] = {};
    std::memcpy(constants + 0, data + 0, 16 * sizeof(float));
    std::memcpy(constants + 16, data + 32, 16 * sizeof(float));
    const bool usePrev = gHasPrev && !gMvReset;
    if (usePrev) {
        std::memcpy(constants + 32, gPrevViewProj, 16 * sizeof(float));
    } else {
        std::memcpy(constants + 32, data + 0, 16 * sizeof(float));
    }
    constants[48] = (float)renderW;
    constants[49] = (float)renderH;

    ID3D12GraphicsCommandList* cmd = ctx->commandList;
    ID3D12DescriptorHeap* heaps[] = { gCompute.heap.Get() };
    cmd->SetDescriptorHeaps(1, heaps);
    cmd->SetComputeRootSignature(gCompute.rootSig.Get());
    cmd->SetPipelineState(gCompute.pso.Get());
    cmd->SetComputeRoot32BitConstants(0, 52, constants, 0);
    cmd->SetComputeRootDescriptorTable(1, computeGpuHandle(slot));
    cmd->Dispatch((renderW + 7) / 8, (renderH + 7) / 8, 1);
    restoreDrawHeap(ctx);

    // 保存本帧数据。
    gCapture.valid = true;
    gCapture.slot = slot;
    gCapture.frameId = gFrameCounter;
    gCapture.renderW = renderW;
    gCapture.renderH = renderH;
    gCapture.outW = outW;
    gCapture.outH = outH;
    std::memcpy(gCapture.viewProj, data + 0, 16 * sizeof(float));
    std::memcpy(gCapture.invViewProj, data + 16, 16 * sizeof(float));
    std::memcpy(gCapture.invViewProjJittered, data + 32, 16 * sizeof(float));
    std::memcpy(gCapture.proj, data + 48, 16 * sizeof(float));
    std::memcpy(gCapture.invProj, data + 64, 16 * sizeof(float));
    std::memcpy(gCapture.cameraPos, data + 80, 3 * sizeof(float));
    std::memcpy(gCapture.cameraUp, data + 83, 3 * sizeof(float));
    std::memcpy(gCapture.cameraRight, data + 86, 3 * sizeof(float));
    std::memcpy(gCapture.cameraFwd, data + 89, 3 * sizeof(float));
    gCapture.cameraNear = data[92];
    gCapture.cameraFar = data[93];
    gCapture.cameraFov = data[94];
    gCapture.cameraAspect = data[95];
    gCapture.jitterPx = data[96];
    gCapture.jitterPy = data[97];

    if (usePrev) {
        std::memcpy(gCapture.prevViewProj, gPrevViewProj, 16 * sizeof(float));
        std::memcpy(gCapture.prevInvViewProj, gPrevInvViewProj, 16 * sizeof(float));
    } else {
        std::memcpy(gCapture.prevViewProj, gCapture.viewProj, 16 * sizeof(float));
        std::memcpy(gCapture.prevInvViewProj, gCapture.invViewProj, 16 * sizeof(float));
    }

    std::memcpy(gPrevViewProj, gCapture.viewProj, 16 * sizeof(float));
    std::memcpy(gPrevInvViewProj, gCapture.invViewProj, 16 * sizeof(float));
    gHasPrev = true;
    gMvReset = false;

    gCaptureSlot = slot;
    gCaptureFrameId = gFrameCounter;

    static bool logged = false;
    if (!logged) {
        logged = true;
        dbgLog("DLSS capture: ok render=%ux%u out=%ux%u slot=%d",
            renderW, renderH, outW, outH, slot);
    }
    return true;
}

// ---------------------------------------------------------------------------
// SR 求值
// ---------------------------------------------------------------------------

bool evaluateSr(CommandContext* ctx, Dx12Surface* s, Dx12Object* colorTex,
    ID3D12Resource** outSrTex, std::string& err) {
    *outSrTex = nullptr;
    if (!gCfg.srEnabled || !gFrameActive) return false;
    // 能力位在 ProjectID + CUSTOM 组合下可能误报，这里只要求 NGX 已初始化，
    // 真正能否超分交给 CreateFeature 实测；实测失败会置 gSrProbeFailed 停止重试。
    if (!ngxIsInitialized() || !ngxDlssIsSrAvailable()) return false;
    if (!ctx || !ctx->listOpen || !ctx->commandList || !s || !colorTex) {
        return false;
    }

    std::string prepErr;
    if (!ensureResources(ctx, s, prepErr)) {
        if (!prepErr.empty()) err = prepErr;
        return false;
    }

    if (!gCapture.valid || gCapture.slot != (int)ctx->currentListSlot
        || gCapture.frameId != gFrameCounter) {
        gSrNeedsReset = true;
        err = "DLSS SR: no depth/MV capture for this frame";
        return false;
    }

    SlotResources& sr = gSlots[ctx->currentListSlot];
    if (!sr.dlssValid()) {
        gSrNeedsReset = true;
        err = "DLSS SR: output resources not allocated";
        return false;
    }
    if (!ensureSrFeature(ctx, gCapture.renderW, gCapture.renderH,
            s->width, s->height, err)) {
        gSrNeedsReset = true;
        return false;
    }

    transitionTextureTo(ctx, colorTex,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, sr.depth.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, sr.mv.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, sr.srOut.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_UNORDERED_ACCESS);

    NVSDK_NGX_D3D12_DLSS_Eval_Params p{};
    p.Feature.pInColor = colorTex->resource.Get();
    p.Feature.pInOutput = sr.srOut.Get();
    p.pInDepth = sr.depth.Get();
    p.pInMotionVectors = sr.mv.Get();
    p.InJitterOffsetX = gCapture.jitterPx;
    p.InJitterOffsetY = gCapture.jitterPy;
    p.InRenderSubrectDimensions.Width = gCapture.renderW;
    p.InRenderSubrectDimensions.Height = gCapture.renderH;
    p.InReset = gSrNeedsReset ? 1 : 0;
    p.InMVScaleX = 1.0f;
    p.InMVScaleY = 1.0f;

    NVSDK_NGX_Result r = NGX_D3D12_EVALUATE_DLSS_EXT(ctx->commandList,
        gSrFeature, gSrParams, &p);
    restoreDrawHeap(ctx);
    if (NVSDK_NGX_FAILED(r)) {
        static bool logged = false;
        if (!logged) {
            logged = true;
            dbgLog("DLSS SR: EvaluateFeature failed %s", ngxResultText(r));
        }
        gSrNeedsReset = true;
        err = std::string("DLSS SR: EvaluateFeature failed ") + ngxResultText(r);
        return false;
    }

    gSrNeedsReset = false;
    // NGX 求值后输出仍在 UAV；blit 在像素着色器中采样它，切到 PIXEL_SHADER_RESOURCE。
    transitionTo(ctx, sr.srOut.Get(), D3D12_RESOURCE_STATE_UNORDERED_ACCESS,
        D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE);
    *outSrTex = sr.srOut.Get();
    return true;
}

// ---------------------------------------------------------------------------
// DLSS-G 求值
// ---------------------------------------------------------------------------

void fillFgOptParams(const FrameCapture& c, int count, int index, bool reset,
    NVSDK_NGX_DLSSG_Opt_Eval_Params& o) {
    o.multiFrameCount = (unsigned)count;
    o.multiFrameIndex = (unsigned)index;

    std::memcpy(o.cameraViewToClip, c.proj, sizeof(o.cameraViewToClip));
    std::memcpy(o.clipToCameraView, c.invProj, sizeof(o.clipToCameraView));
    // clipToLensClip：无镜头畸变，单位矩阵。
    std::memset(o.clipToLensClip, 0, sizeof(o.clipToLensClip));
    o.clipToLensClip[0][0] = o.clipToLensClip[1][1] = 1.0f;
    o.clipToLensClip[2][2] = o.clipToLensClip[3][3] = 1.0f;

    // 行向量约定：clip_prev = clip_cur * invVP_cur * VP_prev
    mulMat(c.invViewProj, c.prevViewProj, &o.clipToPrevClip[0][0]);
    mulMat(c.prevInvViewProj, c.viewProj, &o.prevClipToClip[0][0]);

    o.jitterOffset[0] = (c.renderW > 0) ? (2.0f * c.jitterPx / (float)c.renderW) : 0.0f;
    o.jitterOffset[1] = (c.renderH > 0) ? (-2.0f * c.jitterPy / (float)c.renderH) : 0.0f;
    o.mvecScale[0] = 1.0f;
    o.mvecScale[1] = 1.0f;
    o.cameraPinholeOffset[0] = 0.0f;
    o.cameraPinholeOffset[1] = 0.0f;
    std::memcpy(o.cameraPos, c.cameraPos, sizeof(o.cameraPos));
    std::memcpy(o.cameraUp, c.cameraUp, sizeof(o.cameraUp));
    std::memcpy(o.cameraRight, c.cameraRight, sizeof(o.cameraRight));
    std::memcpy(o.cameraFwd, c.cameraFwd, sizeof(o.cameraFwd));
    o.cameraNear = c.cameraNear;
    o.cameraFar = c.cameraFar;
    o.cameraFOV = c.cameraFov;
    o.cameraAspectRatio = c.cameraAspect;
    o.colorBuffersHDR = false;
    o.depthInverted = true;
    o.cameraMotionIncluded = true;
    // 多帧生成时只有 index=1 需要 reset；后续索引必须沿用同一批输入。
    o.reset = reset;
    o.automodeOverrideReset = false;
    o.notRenderingGameFrames = gNotRenderingGameFrames;
    o.orthoProjection = false;
    o.motionVectorsInvalidValue = 0.0f;
    o.motionVectorsDilated = false;
    o.menuDetectionEnabled = false;
}

bool afterBlit(CommandContext* ctx, Dx12Surface* s, std::string& err) {
    if (!gCfg.fgEnabled || !gFrameActive) return false;
    if (!ngxIsInitialized() || !ngxDlssIsFgAvailable()) return false;
    if (!ctx || !ctx->listOpen || !ctx->commandList || !s) return false;

    if (gNotRenderingGameFrames) {
        gFgNeedsReset = true;
        return false;
    }

    std::string prepErr;
    if (!ensureResources(ctx, s, prepErr)) {
        if (!prepErr.empty()) err = prepErr;
        return false;
    }

    if (!gCapture.valid || gCapture.slot != (int)ctx->currentListSlot
        || gCapture.frameId != gFrameCounter) {
        gFgNeedsReset = true;
        err = "DLSS-G: no depth/MV capture for this frame";
        return false;
    }

    SlotResources& sr = gSlots[ctx->currentListSlot];
    if (!ensureFgFeature(ctx, gCapture.renderW, gCapture.renderH,
            s->width, s->height, err)) {
        gFgNeedsReset = true;
        return false;
    }

    const int requested = std::max(1, std::min(gCfg.fgFrames, 3));
    const int count = std::max(1, std::min<int>(requested, (int)gFgMaxFrames));

    // 重置时 DLSS-G 输出的“生成帧”只是输入帧的拷贝，生成多张会得到重复画面。
    // 指南允许在重置帧把 MultiFrameCount 改成 1（帧对之间可改变），因此只求值
    // 一张生成帧，下一帧恢复正常数量。
    const int evalCount = gFgNeedsReset ? 1 : count;

    // 真实 backbuffer -> 保留纹理。backbuffer 当前是 RENDER_TARGET。
    ID3D12Resource* bb = s->backBuffers[(size_t)s->currentImageIndex].Get();
    transitionTo(ctx, bb, D3D12_RESOURCE_STATE_RENDER_TARGET,
        D3D12_RESOURCE_STATE_COPY_SOURCE);
    transitionTo(ctx, sr.real.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_COPY_DEST);
    ctx->commandList->CopyResource(sr.real.Get(), bb);
    transitionTo(ctx, bb, D3D12_RESOURCE_STATE_COPY_SOURCE,
        D3D12_RESOURCE_STATE_RENDER_TARGET);

    transitionTo(ctx, sr.real.Get(), D3D12_RESOURCE_STATE_COPY_DEST,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, sr.depth.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);
    transitionTo(ctx, sr.mv.Get(), D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE);

    NVSDK_NGX_D3D12_DLSSG_Eval_Params eval{};
    eval.pBackbuffer = sr.real.Get();
    eval.pDepth = sr.depth.Get();
    eval.pMVecs = sr.mv.Get();
    eval.pOutputRealFrame = nullptr;
    eval.pOutputDisableInterpolation = nullptr;

    for (int i = 0; i < evalCount; ++i) {
        transitionTo(ctx, sr.gen[i].Get(), D3D12_RESOURCE_STATE_COMMON,
            D3D12_RESOURCE_STATE_UNORDERED_ACCESS);
        eval.pOutputInterpFrame = sr.gen[i].Get();

        NVSDK_NGX_DLSSG_Opt_Eval_Params opt{};
        fillFgOptParams(gCapture, evalCount, i + 1, i == 0 && gFgNeedsReset, opt);
        NVSDK_NGX_Result r = NGX_D3D12_EVALUATE_DLSSG(ctx->commandList,
            gFgFeature, gFgParams, &eval, &opt);
        restoreDrawHeap(ctx);
        if (NVSDK_NGX_FAILED(r)) {
            static bool logged = false;
            if (!logged) {
                logged = true;
                dbgLog("DLSS-G: EvaluateFeature failed %s (index %d/%d)",
                    ngxResultText(r), i + 1, evalCount);
            }
            gFgNeedsReset = true;
            err = std::string("DLSS-G: EvaluateFeature failed ") + ngxResultText(r);
            return false;
        }
    }

    gFgNeedsReset = false;

    PendingFrame pending;
    pending.key = ctx->fenceValue + 1;
    pending.surface = s;
    pending.slot = (int)ctx->currentListSlot;
    pending.count = evalCount;
    pending.frameId = gFrameCounter;
    {
        std::lock_guard<std::mutex> lk(gPendingMtx);
        gPending.push_back(pending);
    }

    static bool logged = false;
    if (!logged) {
        logged = true;
        dbgLog("DLSS-G: evaluated %d generated frame(s)", evalCount);
    }
    return true;
}

// ---------------------------------------------------------------------------
// 生成帧呈现
// ---------------------------------------------------------------------------

// 把 src 复制到当前已 acquire 的 backbuffer（acquire=false）或新 acquire 的
// backbuffer（acquire=true）。不执行 Present。
bool copyToBackbuffer(Dx12Surface* s, ID3D12Resource* src, bool acquire,
    int& copiedIdx, std::string& err) {
    copiedIdx = -1;
    if (!gPresentCtx) {
        gPresentCtx = createCommandEncoder(err);
        if (!gPresentCtx) return false;
    }
    if (acquire) {
        if (!acquireSurfaceForFrameGen(s, err)) return false;
    } else if (s->currentImageIndex < 0) {
        err = "DLSS-G present: no acquired back buffer";
        return false;
    }
    const int idx = s->currentImageIndex;
    if (idx < 0 || idx >= (int)s->backBuffers.size()) {
        err = "DLSS-G present: bad back buffer index";
        return false;
    }
    ID3D12Resource* dst = s->backBuffers[(size_t)idx].Get();

    if (!beginCommandList(gPresentCtx, err)) return false;
    transitionTo(gPresentCtx, dst, D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_COPY_DEST);
    transitionTo(gPresentCtx, src, D3D12_RESOURCE_STATE_COMMON,
        D3D12_RESOURCE_STATE_COPY_SOURCE);
    gPresentCtx->commandList->CopyResource(dst, src);
    transitionTo(gPresentCtx, src, D3D12_RESOURCE_STATE_COPY_SOURCE,
        D3D12_RESOURCE_STATE_COMMON);
    transitionTo(gPresentCtx, dst, D3D12_RESOURCE_STATE_COPY_DEST,
        D3D12_RESOURCE_STATE_COMMON);
    if (submitCommandList(gPresentCtx, err) == 0) return false;

    copiedIdx = idx;
    return true;
}

bool presentGeneratedFrames(Dx12Surface* s, CommandContext* mainCtx,
    std::vector<int>& presentedIndices, std::string& err) {
    presentedIndices.clear();
    if (!s || !mainCtx) return false;

    PendingFrame pending;
    bool found = false;
    {
        std::lock_guard<std::mutex> lk(gPendingMtx);
        for (auto it = gPending.begin(); it != gPending.end();) {
            if (it->key == mainCtx->fenceValue && it->surface == s) {
                pending = *it;
                it = gPending.erase(it);
                found = true;
                break;
            }
            if (it->key < mainCtx->fenceValue) {
                // 过期条目（对应帧已提交但未消费）：丢弃。
                it = gPending.erase(it);
            } else {
                ++it;
            }
        }
    }
    if (!found) return false;
    if (pending.count <= 0 || pending.slot < 0 || pending.slot >= 3) return false;

    SlotResources& sr = gSlots[pending.slot];
    if (!sr.dlssValid()) return false;

    // 第一张生成帧写入「本帧已经 acquire 的真实 backbuffer」。真实内容此前已在
    // blitSurface 中复制到 sr.real，因此直接覆盖是安全的。后续每一张都需要
    // acquire 下一个 backbuffer（Present 会同步轮转 DXGI 索引）。
    bool presentedAny = false;
    for (int i = 0; i < pending.count; ++i) {
        int idx = -1;
        std::string e;
        if (!copyToBackbuffer(s, sr.gen[i].Get(), i > 0, idx, e)) {
            if (!e.empty()) err = e;
            return presentedAny;  // 已 present 过则调用方不要再 present
        }
        presentSurfaceInterpolated(s);
        presentedIndices.push_back(idx);
        presentedAny = true;
    }

    // 最后呈现保留的真实帧（生成帧在前，真实帧在最后，符合 DLSS-G 指南）。
    int idx = -1;
    std::string e;
    if (!copyToBackbuffer(s, sr.real.Get(), true, idx, e)) {
        if (!e.empty()) err = e;
        return presentedAny;
    }
    presentSurface(s);
    presentedIndices.push_back(idx);
    return true;
}

}  // namespace

// ---------------------------------------------------------------------------
// 公开 API
// ---------------------------------------------------------------------------

void ngxDlssSetConfig(const NgxDlssConfig& cfg) {
    NgxDlssConfig next = cfg;
    next.quality = std::max(0, std::min(5, next.quality));
    next.preset = std::max(0, std::min(13, next.preset));
    next.fgFrames = std::max(1, std::min(3, next.fgFrames));

    const bool changed = next.srEnabled != gCfg.srEnabled
        || next.quality != gCfg.quality || next.preset != gCfg.preset
        || next.fgEnabled != gCfg.fgEnabled || next.fgFrames != gCfg.fgFrames;
    // SR/FG 开关会改变槽位纹理的分配（XeSS-only 时不分配 DLSS 输出纹理）。
    const bool dlssTexturesChanged = next.srEnabled != gCfg.srEnabled
        || next.fgEnabled != gCfg.fgEnabled;
    gCfg = next;
    if (changed) {
        if (dlssTexturesChanged) gResourcesDirty = true;
        // 档位/开关变化后允许重新尝试创建 SR feature。
        gSrProbeFailed = false;
        gSrNeedsReset = true;
        gFgNeedsReset = true;
        gMvReset = true;
        if (!gCfg.fgEnabled) {
            gFgFeatureActive.store(false);
            // 关闭帧生成时丢弃尚未消费的呈现条目，避免渲染线程继续 present
            // 已经不该显示的生成帧。
            std::lock_guard<std::mutex> lk(gPendingMtx);
            gPending.clear();
        }
        dbgLog("DLSS config: sr=%d quality=%d preset=%d fg=%d frames=%d",
            (int)gCfg.srEnabled, gCfg.quality, gCfg.preset,
            (int)gCfg.fgEnabled, gCfg.fgFrames);
    }
}

bool ngxDlssIsSrAvailable() {
    NgxState st = ngxGetState();
    // dlssAvailable 是初始化时的能力位（ProjectID + CUSTOM 下会误报 false），
    // 因此只要求 NGX 已初始化且 CreateFeature 没有实测失败过。
    // 实测失败后即使 dlssAvailable=true 也不再重试，避免每帧 deviceWaitIdle 卡顿。
    return st.initialized && !gSrProbeFailed;
}

bool ngxDlssIsFgAvailable() {
    NgxState st = ngxGetState();
    return st.initialized && st.frameGenAvailable;
}

bool ngxDlssIsSrActive() {
    return gCfg.srEnabled && gSrFeature != nullptr && gFrameActive;
}

bool ngxDlssIsFgActive() {
    return gCfg.fgEnabled && gFgFeatureActive.load();
}

void ngxDlssBeginFrame(bool active, bool notRenderingGameFrames) {
    ++gFrameCounter;
    gFrameActive = active;
    gNotRenderingGameFrames = notRenderingGameFrames;
    if (!active) {
        gSrNeedsReset = true;
        gFgNeedsReset = true;
        gMvReset = true;
    }
    if (notRenderingGameFrames) {
        gFgNeedsReset = true;
    }
    gCapture.valid = false;
    gCaptureSlot = -1;
    gCaptureFrameId = 0;
}

bool ngxDlssCaptureFrame(CommandContext* ctx, Dx12Surface* s, Dx12Object* depthTex,
    const float* data, int count, std::string& err) {
    return captureFrame(ctx, s, depthTex, data, count, err);
}

void ngxDlssReset() {
    gSrNeedsReset = true;
    gFgNeedsReset = true;
    gMvReset = true;
    gHasPrev = false;
    gCapture.valid = false;
}

void ngxDlssSetExternalCapture(bool enabled) {
    const bool prev = gExternalCapture.exchange(enabled);
    if (prev == enabled) return;
    // 外部后端开关会改变槽位纹理分配（XeSS-only 不需要 DLSS 输出纹理）。
    gResourcesDirty = true;
    gMvReset = true;
    gCapture.valid = false;
    dbgLog("DLSS capture: external backend %s", enabled ? "on" : "off");
}

bool ngxDlssGetCapturedInputs(CommandContext* ctx, NgxDlssCapturedInputs& out) {
    out = NgxDlssCapturedInputs{};
    if (!ctx || !gCapture.valid) return false;
    if (gCapture.slot != (int)ctx->currentListSlot) return false;
    if (gCapture.frameId != gFrameCounter) return false;
    SlotResources& sr = gSlots[gCapture.slot];
    if (!sr.depth || !sr.mv) return false;
    out.depth = sr.depth.Get();
    out.mv = sr.mv.Get();
    out.renderW = gCapture.renderW;
    out.renderH = gCapture.renderH;
    out.outW = gCapture.outW;
    out.outH = gCapture.outH;
    out.jitterPx = gCapture.jitterPx;
    out.jitterPy = gCapture.jitterPy;
    return true;
}

bool ngxDlssEnsureCaptureResources(CommandContext* ctx, std::string& err) {
    return ensureResources(ctx, nullptr, err);
}

bool ngxDlssEvaluateSr(CommandContext* ctx, Dx12Surface* s, Dx12Object* colorTex,
    ID3D12Resource** outSrTex, std::string& err) {
    return evaluateSr(ctx, s, colorTex, outSrTex, err);
}

void ngxDlssAfterBlit(CommandContext* ctx, Dx12Surface* s, std::string& err) {
    std::string localErr;
    if (!afterBlit(ctx, s, localErr) && !localErr.empty()) {
        static std::string lastErr;
        if (localErr != lastErr) {
            lastErr = localErr;
            dbgLog("DLSS-G: %s", localErr.c_str());
        }
        err = localErr;
    }
}

bool ngxDlssPresentGeneratedFrames(Dx12Surface* s, CommandContext* mainCtx,
    std::vector<int>& presentedIndices, std::string& err) {
    return presentGeneratedFrames(s, mainCtx, presentedIndices, err);
}

void ngxRenderShutdown() {
    // XeSS 与 DLSS 共用捕获资源；先释放 XeSS context 再拆捕获管线。
    xessShutdown();
    waitForPendingPresents();
    std::string err;
    deviceWaitIdle(err);
    {
        std::lock_guard<std::mutex> lk(gPendingMtx);
        gPending.clear();
    }
    releaseFeatures();
    releaseSlotResources();
    if (gPresentCtx) {
        destroyCommandEncoder(gPresentCtx);
        gPresentCtx = nullptr;
    }
    gCompute.pso.Reset();
    gCompute.rootSig.Reset();
    gCompute.heap.Reset();
    gCompute.ready = false;
    gResourcesDirty = true;
    gHasPrev = false;
    gCapture = FrameCapture{};
    gFrameActive = false;
    gNotRenderingGameFrames = false;
}

}  // namespace dx12mc
