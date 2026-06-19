// d3d12bridge.cpp — MC D3D12 Renderer (Phase 2: render to MC window + MVP matrix)
//
// Architecture:
//   1. D3D12 renders directly INTO Minecraft's GLFW window (SwapChain on MC HWND)
//   2. GL→D3D12 translation: intercepted MC BufferBuilder → vertex data → JNI → D3D12 draw
//   3. Projection/modelView matrices synced from MC via JNI each frame
//   4. Geometry uses MC world-space coords, transformed by MVP in VS shader
//
#define WIN32_LEAN_AND_MEAN
#define _CRT_SECURE_NO_WARNINGS
#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <d3dcompiler.h>
#include <wrl.h>
#include <cstdio>
#include <cstdarg>
#include <cmath>
#include <vector>
#include <algorithm>
#include <unordered_map>
#include <mutex>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "d3dcompiler.lib")

using namespace Microsoft::WRL;

static void Log(const char* fmt, ...) {
    char buf[1024];
    va_list a; va_start(a, fmt);
    _vsnprintf_s(buf, sizeof(buf), _TRUNCATE, fmt, a);
    va_end(a);
    FILE* f = fopen("C:\\temp\\gl4dx12_d3d12.log", "a");
    if (f) { fprintf(f, "%s\n", buf); fclose(f); }
    OutputDebugStringA(buf); OutputDebugStringA("\n");
}

// === D3D12 state ===
static ComPtr<ID3D12Device>          g_dev;
static char g_d3d12Info[256] = "D3D12 not initialized";
static ComPtr<ID3D12CommandQueue>    g_queue;
static ComPtr<IDXGISwapChain3>       g_swap;
static ComPtr<ID3D12Resource>        g_rt[2];
static ComPtr<ID3D12DescriptorHeap>  g_rtvHeap;
static ComPtr<ID3D12DescriptorHeap>  g_srvHeap;
static ComPtr<ID3D12CommandAllocator>     g_alloc;
static ComPtr<ID3D12GraphicsCommandList>  g_cl;
static ComPtr<ID3D12Fence>    g_fence;
static HANDLE   g_fenceEv = nullptr;
static UINT64   g_fenceVal = 0;
static UINT     g_rtvSize = 0, g_srvSize = 0;
static UINT     g_fi = 0, g_w = 1280, g_h = 720;
static bool     g_ok = false;
static HANDLE   g_thread = nullptr;
static volatile bool g_run = false;
static HANDLE   g_frameReadyEvent = nullptr;  // 帧数据就绪事件
static HANDLE   g_frameDoneEvent = nullptr;   // 帧渲染完成事件（可选）

// ========== 新简单顶点数据存储（与 g_drawChunks 并行） ==========
static std::vector<float> g_vertexData;      // 顶点位置 (x,y,z 连续)
static std::vector<float> g_uvData;          // UV坐标 (u,v 连续)
static float g_colorDataBuf[4] = {1.0f, 1.0f, 1.0f, 1.0f};
static int g_newVertexCount = 0;
static bool g_hasNewVertexData = false;
static std::mutex g_dataMutex;               // 线程安全

// Texture state
static ComPtr<ID3D12Resource> g_tex;
static ComPtr<ID3D12Resource> g_texDefault;
static int g_texW = 0, g_texH = 0;
static CRITICAL_SECTION g_texLock;

// MC framebuffer capture via GDI BitBlt
static ComPtr<ID3D12Resource> g_texMCFrame;
static UINT g_mcCaptureW = 0, g_mcCaptureH = 0;

// Fullscreen quad
static ComPtr<ID3D12Resource> g_vbFSQuad;

// PSO (textured quad)
static ComPtr<ID3D12RootSignature> g_rs;
static ComPtr<ID3D12PipelineState> g_pso;

// PSO (solid-color geometry)
static ComPtr<ID3D12RootSignature> g_rsSolid;
static ComPtr<ID3D12PipelineState> g_psoSolid;

// GL state mirror
static float  g_glClearColor[4] = {0.15f, 0.15f, 0.15f, 1.0f};
static float  g_glColor[4] = {1, 1, 1, 1};
static int    g_glViewport[4] = {0, 0, 1280, 720};
static CRITICAL_SECTION g_stateLock;

// GL→D3D12 draw command recording
static ComPtr<ID3D12Resource> g_imVB;
static D3D12_VERTEX_BUFFER_VIEW g_imVbv = {};
static UINT  g_imVBCap = 16 * 1024 * 1024;
static UINT  g_imVBSize = 0;
static UINT  g_imVertCount = 0;

struct DrawChunk { UINT byteOffset; UINT vertexCount; D3D_PRIMITIVE_TOPOLOGY topo; bool textured; UINT vertexStride; bool blend; int textureId; };
static std::vector<DrawChunk> g_drawChunks;
static D3D_PRIMITIVE_TOPOLOGY g_pendingTopo = D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;
static int  g_pendingTextureId = 0;

static UINT g_glStateBits = 0;
#define GLB_BLEND        1
#define GLB_DEPTH        2
#define GLB_CULL         4
#define GLB_DEPTH_WRITE  8

static ComPtr<ID3D12Resource>       g_depthBuf;
static ComPtr<ID3D12DescriptorHeap> g_dsvHeap;
static DXGI_FORMAT g_dsvFormat = DXGI_FORMAT_UNKNOWN;

struct MvpCB { float mvp[16]; };
static ComPtr<ID3D12Resource> g_cbUpload;
static BYTE* g_cbData = nullptr;
static const UINT g_cbSize = 256;

static D3D12_BLEND g_glSrcBlend = D3D12_BLEND_SRC_ALPHA;
static D3D12_BLEND g_glDstBlend = D3D12_BLEND_INV_SRC_ALPHA;

static ComPtr<ID3D12PipelineState> g_psoSolidVariants[32];
static ComPtr<ID3D12RootSignature> g_rsSolidVariants[16];
static ComPtr<ID3D12PipelineState> g_psoLineVariants[32];
static ComPtr<ID3D12RootSignature> g_rsLineVariants[16];

// Textured PSO
static const char* kVS_Tex = R"(
cbuffer Transform : register(b0) { float4x4 mvp; }
struct VS_IN  { float3 p : POS; uint c : COL; float2 uv : TEX; };
struct PS_IN  { float4 p : SV_POSITION; float4 c : COL; float2 uv : TEX; };
PS_IN VSMain(VS_IN i) {
    PS_IN o;
    o.p = mul(float4(i.p, 1), mvp);
    o.c = float4(((i.c>>16)&0xff)/255.0, ((i.c>>8)&0xff)/255.0, (i.c&0xff)/255.0, ((i.c>>24)&0xff)/255.0);
    o.uv = i.uv;
    return o;
}
)";
static const char* kPS_Tex = R"(
Texture2D tex : register(t0);
SamplerState samp : register(s0);
struct PS_IN { float4 p : SV_POSITION; float4 c : COL; float2 uv : TEX; };
float4 PSMain(PS_IN i) : SV_TARGET { return tex.Sample(samp, i.uv) * i.c; }
)";
static ComPtr<ID3D12RootSignature> g_rsTex;
static ComPtr<ID3D12PipelineState> g_psoTex;
static ComPtr<ID3D12DescriptorHeap> g_texSrvHeap;
static UINT g_texSrvSize = 0;
static std::unordered_map<int, ComPtr<ID3D12Resource>> g_texMap;
static std::unordered_map<int, UINT> g_texSlotMap;
static UINT g_texSlotNext = 0;
static int g_currentTexId = 0;

// Window overlay
static HWND g_hwndOverlay = nullptr;
static HWND g_hwndMC = nullptr;

static LRESULT CALLBACK OverlayWndProc(HWND h, UINT m, WPARAM w, LPARAM l) {
    switch (m) {
    case WM_ERASEBKGND: 
        return 1;  // 避免闪烁
    case WM_PAINT:
        // 让 D3D12 渲染，不需要 GDI 绘制
        ValidateRect(h, nullptr);
        return 0;
    }
    return DefWindowProcW(h, m, w, l);
}

static HWND CreateOverlayWindow(HWND hParent) {
    static int classCounter = 0;
    wchar_t cn[64];
    swprintf(cn, 64, L"GL4DX12_Overlay_%d", classCounter++);
    
    WNDCLASSW wc = {};
    wc.lpfnWndProc = OverlayWndProc;
    wc.hInstance = GetModuleHandleW(0);
    wc.lpszClassName = cn;
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    
    if (!RegisterClassW(&wc)) {
        DWORD err = GetLastError();
        Log("ERROR: RegisterClassW failed for %S, error=%d", cn, err);
        return nullptr;
    }
    Log("Window class registered: %S", cn);

    // 短暂延迟
    Sleep(50);

    // 创建子窗口
    HWND hw = CreateWindowExW(
        0,  // 无扩展样式
        cn, L"GL4DX12 Overlay", 
        WS_CHILD | WS_VISIBLE,  // 子窗口样式
        0, 0, (int)g_w, (int)g_h,
        hParent,  // 父窗口
        nullptr, GetModuleHandleW(0), nullptr);

    if (!hw) {
        DWORD err = GetLastError();
        Log("ERROR: CreateWindowExW failed, error=%d", err);
        UnregisterClassW(cn, GetModuleHandleW(0));
        return nullptr;
    }

    Log("Window created as child, HWND=0x%p, class=%S", hw, cn);
    ShowWindow(hw, SW_SHOW);
    return hw;
}

static void WaitGPU() {
    if (g_fence && g_queue) {
        UINT64 v = g_fenceVal;
        g_queue->Signal(g_fence.Get(), v); g_fenceVal++;
        if (g_fence->GetCompletedValue() < v) {
            g_fence->SetEventOnCompletion(v, g_fenceEv);
            WaitForSingleObject(g_fenceEv, INFINITE);
        }
    }
}

static bool MkUpload(ComPtr<ID3D12Resource>& dst, const void* data, UINT sz) {
    D3D12_HEAP_PROPERTIES hp = {}; hp.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rd = {};
    rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rd.Width = sz; rd.Height = 1; rd.DepthOrArraySize = 1;
    rd.MipLevels = 1; rd.SampleDesc.Count = 1;
    rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    if (FAILED(g_dev->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
        &rd, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&dst))))
        return false;
    if (data) { void* m=nullptr; dst->Map(0,nullptr,&m); memcpy(m,data,sz); dst->Unmap(0,nullptr); }
    return true;
}

static bool MkPSO() {
    const char* kVS = R"(
struct VS_IN { float2 p : POS; float2 uv : TEX; };
struct PS_IN { float4 p : SV_POSITION; float2 uv : TEX; };
PS_IN VSMain(VS_IN i) { PS_IN o; o.p=float4(i.p,0,1); o.uv=i.uv; return o; }
)";
    const char* kPS = R"(
struct PS_IN { float4 p : SV_POSITION; float2 uv : TEX; };
Texture2D gTex : register(t0);
SamplerState gSamp : register(s0);
float4 PSMain(PS_IN i) : SV_TARGET { return gTex.Sample(gSamp, i.uv); }
)";
    ComPtr<ID3DBlob> vs,ps,err;
    if (FAILED(D3DCompile(kVS,strlen(kVS),0,0,0,"VSMain","vs_5_0",0,0,&vs,&err)))
    { Log("VS fail"); return false; }
    if (FAILED(D3DCompile(kPS,strlen(kPS),0,0,0,"PSMain","ps_5_0",0,0,&ps,&err)))
    { Log("PS fail"); return false; }

    D3D12_DESCRIPTOR_RANGE range = {};
    range.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    range.NumDescriptors = 1; range.BaseShaderRegister = 0;
    D3D12_STATIC_SAMPLER_DESC samp = {};
    samp.Filter = D3D12_FILTER_MIN_MAG_MIP_LINEAR;
    samp.AddressU = samp.AddressV = samp.AddressW = D3D12_TEXTURE_ADDRESS_MODE_CLAMP;
    D3D12_ROOT_PARAMETER rp = {};
    rp.ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    rp.DescriptorTable.NumDescriptorRanges = 1;
    rp.DescriptorTable.pDescriptorRanges = &range;
    rp.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;
    D3D12_ROOT_SIGNATURE_DESC rsd = {};
    rsd.NumParameters = 1; rsd.pParameters = &rp;
    rsd.NumStaticSamplers = 1; rsd.pStaticSamplers = &samp;
    rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
    ComPtr<ID3DBlob> rb;
    if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
    if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(), IID_PPV_ARGS(&g_rs)))) return false;

    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS",0,DXGI_FORMAT_R32G32_FLOAT,0,0,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"TEX",0,DXGI_FORMAT_R32G32_FLOAT,0,8,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
    };
    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = g_rs.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = D3D12_CULL_MODE_NONE;
    pd.RasterizerState.DepthClipEnable = TRUE;
    pd.DepthStencilState.DepthEnable = FALSE;
    pd.InputLayout = {ie,2};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;
    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_pso)))) return false;
    return true;
}

static const char* kVS_Solid = R"(
cbuffer Transform : register(b0) { float4x4 mvp; }
struct VS_IN { float3 p : POS; uint c : COL; };
struct PS_IN { float4 p : SV_POSITION; float4 c : COL; };
PS_IN VSMain(VS_IN i) {
    PS_IN o;
    o.p = mul(float4(i.p, 1), mvp);
    o.c = float4(((i.c>>16)&0xff)/255.0, ((i.c>>8)&0xff)/255.0, (i.c&0xff)/255.0, ((i.c>>24)&0xff)/255.0);
    return o;
}
)";
static const char* kPS_Solid = R"(
struct PS_IN { float4 p : SV_POSITION; float4 c : COL; };
float4 PSMain(PS_IN i) : SV_TARGET { return i.c; }
)";

static bool BuildSolidPSO(UINT stateBits, bool textured) {
    int idx = (int)((stateBits << 1) | (textured ? 1 : 0));
    if (idx < 0 || idx >= 32) return false;
    if (g_psoSolidVariants[idx]) return true;

    ComPtr<ID3DBlob> vs, ps, err;
    if (FAILED(D3DCompile(kVS_Solid, strlen(kVS_Solid), 0,0,0,"VSMain","vs_5_0",0,0,&vs,&err))) return false;
    if (FAILED(D3DCompile(kPS_Solid, strlen(kPS_Solid), 0,0,0,"PSMain","ps_5_0",0,0,&ps,&err))) return false;

    ComPtr<ID3D12RootSignature>& rs = g_rsSolidVariants[stateBits & 0xF];
    if (!rs) {
        D3D12_ROOT_PARAMETER rpCB = {};
        rpCB.ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
        rpCB.Descriptor.ShaderRegister = 0;
        rpCB.ShaderVisibility = D3D12_SHADER_VISIBILITY_VERTEX;
        D3D12_ROOT_SIGNATURE_DESC rsd = {};
        rsd.NumParameters = 1; rsd.pParameters = &rpCB;
        rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
        ComPtr<ID3DBlob> rb;
        if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
        if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(), IID_PPV_ARGS(&rs)))) return false;
    }

    UINT ieCount = textured ? 3 : 2;
    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS",0,DXGI_FORMAT_R32G32B32_FLOAT,0,0,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"COL",0,DXGI_FORMAT_R8G8B8A8_UNORM,0,12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"TEX",0,DXGI_FORMAT_R32G32_FLOAT,0,16,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
    };
    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = rs.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    if (stateBits & GLB_BLEND) {
        pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
        pd.BlendState.RenderTarget[0].SrcBlend = g_glSrcBlend;
        pd.BlendState.RenderTarget[0].DestBlend = g_glDstBlend;
        pd.BlendState.RenderTarget[0].BlendOp = D3D12_BLEND_OP_ADD;
    }
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = (stateBits & GLB_CULL) ? D3D12_CULL_MODE_BACK : D3D12_CULL_MODE_NONE;
    pd.RasterizerState.DepthClipEnable = TRUE;
    // 临时强制禁用深度测试，诊断黑屏问题
    pd.DepthStencilState.DepthEnable = FALSE;
    pd.DepthStencilState.DepthWriteMask = D3D12_DEPTH_WRITE_MASK_ZERO;
    pd.DepthStencilState.DepthFunc = D3D12_COMPARISON_FUNC_LESS_EQUAL;
    pd.DSVFormat = (g_dsvFormat != DXGI_FORMAT_UNKNOWN) ? g_dsvFormat : DXGI_FORMAT_UNKNOWN;
    pd.InputLayout = {ie, ieCount};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;
    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoSolidVariants[idx])))) return false;
    return true;
}

static bool BuildLinePSO(UINT stateBits, bool textured) {
    int idx = (int)((stateBits << 1) | (textured ? 1 : 0));
    if (idx < 0 || idx >= 32) return false;
    if (g_psoLineVariants[idx]) return true;

    ComPtr<ID3DBlob> vs, ps, err;
    if (FAILED(D3DCompile(kVS_Solid, strlen(kVS_Solid), 0,0,0,"VSMain","vs_5_0",0,0,&vs,&err))) return false;
    if (FAILED(D3DCompile(kPS_Solid, strlen(kPS_Solid), 0,0,0,"PSMain","ps_5_0",0,0,&ps,&err))) return false;

    ComPtr<ID3D12RootSignature>& rs = g_rsLineVariants[stateBits & 0xF];
    if (!rs) {
        D3D12_ROOT_PARAMETER rpCB = {};
        rpCB.ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
        rpCB.Descriptor.ShaderRegister = 0;
        rpCB.ShaderVisibility = D3D12_SHADER_VISIBILITY_VERTEX;
        D3D12_ROOT_SIGNATURE_DESC rsd = {};
        rsd.NumParameters = 1; rsd.pParameters = &rpCB;
        rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
        ComPtr<ID3DBlob> rb;
        if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
        if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(), IID_PPV_ARGS(&rs)))) return false;
    }

    UINT ieCount = textured ? 3 : 2;
    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS",0,DXGI_FORMAT_R32G32B32_FLOAT,0,0,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"COL",0,DXGI_FORMAT_R8G8B8A8_UNORM,0,12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"TEX",0,DXGI_FORMAT_R32G32_FLOAT,0,16,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
    };
    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = rs.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    if (stateBits & GLB_BLEND) {
        pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
        pd.BlendState.RenderTarget[0].SrcBlend = g_glSrcBlend;
        pd.BlendState.RenderTarget[0].DestBlend = g_glDstBlend;
        pd.BlendState.RenderTarget[0].BlendOp = D3D12_BLEND_OP_ADD;
    }
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = (stateBits & GLB_CULL) ? D3D12_CULL_MODE_BACK : D3D12_CULL_MODE_NONE;
    pd.RasterizerState.DepthClipEnable = FALSE;
    pd.DepthStencilState.DepthEnable = (stateBits & GLB_DEPTH) ? TRUE : FALSE;
    pd.DepthStencilState.DepthWriteMask = (stateBits & GLB_DEPTH_WRITE) ? D3D12_DEPTH_WRITE_MASK_ALL : D3D12_DEPTH_WRITE_MASK_ZERO;
    pd.DepthStencilState.DepthFunc = D3D12_COMPARISON_FUNC_LESS_EQUAL;
    pd.DSVFormat = (g_dsvFormat != DXGI_FORMAT_UNKNOWN) ? g_dsvFormat : DXGI_FORMAT_UNKNOWN;
    pd.InputLayout = {ie, ieCount};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_LINE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;
    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoLineVariants[idx])))) return false;
    return true;
}

static bool MkPSOTex() {
    ComPtr<ID3DBlob> vs, ps, err, rb;
    if (FAILED(D3DCompile(kVS_Tex, strlen(kVS_Tex), 0,0,0,"VSMain","vs_5_0",0,0,&vs,&err))) return false;
    if (FAILED(D3DCompile(kPS_Tex, strlen(kPS_Tex), 0,0,0,"PSMain","ps_5_0",0,0,&ps,&err))) return false;

    D3D12_DESCRIPTOR_RANGE range = {};
    range.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    range.NumDescriptors = 1;
    range.BaseShaderRegister = 0;
    D3D12_ROOT_PARAMETER rpTex = {};
    rpTex.ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    rpTex.DescriptorTable.NumDescriptorRanges = 1;
    rpTex.DescriptorTable.pDescriptorRanges = &range;
    rpTex.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

    D3D12_ROOT_PARAMETER rpCBV = {};
    rpCBV.ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
    rpCBV.Descriptor.ShaderRegister = 0;
    rpCBV.ShaderVisibility = D3D12_SHADER_VISIBILITY_VERTEX;

    D3D12_STATIC_SAMPLER_DESC ss = {};
    ss.Filter = D3D12_FILTER_MIN_MAG_MIP_LINEAR;
    ss.AddressU = ss.AddressV = ss.AddressW = D3D12_TEXTURE_ADDRESS_MODE_WRAP;
    ss.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;
    ss.ShaderRegister = 0;

    D3D12_ROOT_PARAMETER params[] = {rpTex, rpCBV};
    D3D12_ROOT_SIGNATURE_DESC rsd = {};
    rsd.NumParameters = 2; rsd.pParameters = params;
    rsd.NumStaticSamplers = 1; rsd.pStaticSamplers = &ss;
    rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
    if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
    if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(), IID_PPV_ARGS(&g_rsTex)))) return false;

    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS",0,DXGI_FORMAT_R32G32B32_FLOAT,0,0,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"COL",0,DXGI_FORMAT_R8G8B8A8_UNORM,0,12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"TEX",0,DXGI_FORMAT_R32G32_FLOAT,0,16,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
    };
    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = g_rsTex.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
    pd.BlendState.RenderTarget[0].SrcBlend = D3D12_BLEND_SRC_ALPHA;
    pd.BlendState.RenderTarget[0].DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = D3D12_CULL_MODE_NONE;
    pd.RasterizerState.DepthClipEnable = TRUE;
    pd.DepthStencilState.DepthEnable = FALSE;
    pd.InputLayout = {ie, 3};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;
    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoTex)))) return false;

    D3D12_DESCRIPTOR_HEAP_DESC hd = {};
    hd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    hd.NumDescriptors = 64;
    hd.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
    if (FAILED(g_dev->CreateDescriptorHeap(&hd, IID_PPV_ARGS(&g_texSrvHeap)))) return false;
    g_texSrvSize = g_dev->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
    return true;
}

static void UploadTextureEx(const void* pixels, int w, int h, int texId) {
    if (!g_ok || w <= 0 || h <= 0 || texId <= 0) return;
    if (g_texMap.find(texId) != g_texMap.end()) return;

    UINT rowPitch = w * 4;
    UINT uploadRowPitch = (rowPitch + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1) & ~(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    UINT uploadSize = uploadRowPitch * h;

    D3D12_HEAP_PROPERTIES hpUp = {}; hpUp.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rdUp = {};
    rdUp.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rdUp.Width = uploadSize; rdUp.Height = 1; rdUp.DepthOrArraySize = 1;
    rdUp.MipLevels = 1; rdUp.SampleDesc.Count = 1;
    rdUp.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    ComPtr<ID3D12Resource> uploadBuf;
    if (FAILED(g_dev->CreateCommittedResource(&hpUp, D3D12_HEAP_FLAG_NONE, &rdUp,
        D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&uploadBuf)))) return;

    void* dst = nullptr;
    uploadBuf->Map(0, nullptr, &dst);
    for (int y = 0; y < h; y++)
        memcpy((BYTE*)dst + y * uploadRowPitch, (BYTE*)pixels + y * rowPitch, rowPitch);
    uploadBuf->Unmap(0, nullptr);

    D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
    D3D12_RESOURCE_DESC td = {};
    td.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    td.Width = w; td.Height = h; td.DepthOrArraySize = 1;
    td.MipLevels = 1; td.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    td.SampleDesc.Count = 1;
    ComPtr<ID3D12Resource> tex;
    if (FAILED(g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE, &td,
        D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&tex)))) return;

    ComPtr<ID3D12CommandAllocator> ca;
    ComPtr<ID3D12GraphicsCommandList> cl;
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&ca));
    g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, ca.Get(), nullptr, IID_PPV_ARGS(&cl));

    D3D12_TEXTURE_COPY_LOCATION srcLoc = {}, dstLoc = {};
    srcLoc.pResource = uploadBuf.Get();
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    srcLoc.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    srcLoc.PlacedFootprint.Footprint.Width = w;
    srcLoc.PlacedFootprint.Footprint.Height = h;
    srcLoc.PlacedFootprint.Footprint.Depth = 1;
    srcLoc.PlacedFootprint.Footprint.RowPitch = uploadRowPitch;
    dstLoc.pResource = tex.Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;
    cl->CopyTextureRegion(&dstLoc, 0, 0, 0, &srcLoc, nullptr);

    D3D12_RESOURCE_BARRIER b = {};
    b.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    b.Transition.pResource = tex.Get();
    b.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_DEST;
    b.Transition.StateAfter = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;
    b.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    cl->ResourceBarrier(1, &b);
    cl->Close();
    ID3D12CommandList* lists[] = {cl.Get()};
    g_queue->ExecuteCommandLists(1, lists);
    WaitGPU();

    D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc = {};
    srvDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
    srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    srvDesc.Texture2D.MipLevels = 1;
    D3D12_CPU_DESCRIPTOR_HANDLE cpuHandle = g_texSrvHeap->GetCPUDescriptorHandleForHeapStart();
    UINT slot;
    auto slotIt = g_texSlotMap.find(texId);
    if (slotIt != g_texSlotMap.end()) {
        slot = slotIt->second;
    } else {
        if (g_texSlotNext >= 64) return;
        slot = g_texSlotNext++;
        g_texSlotMap[texId] = slot;
    }
    cpuHandle.ptr += (SIZE_T)slot * g_texSrvSize;
    g_dev->CreateShaderResourceView(tex.Get(), &srvDesc, cpuHandle);
    g_texMap[texId] = tex;
    Log("Upload texture #%d %dx%d slot=%u", texId, w, h, slot);
}
static bool CaptureMCFrame() {
    if (!g_hwndMC) return false;
    HDC hdcWin = GetDC(g_hwndMC);
    if (!hdcWin) return false;
    HDC hdcMem = CreateCompatibleDC(hdcWin);
    if (!hdcMem) { ReleaseDC(g_hwndMC, hdcWin); return false; }
    HBITMAP hbm = CreateCompatibleBitmap(hdcWin, (int)g_w, (int)g_h);
    if (!hbm) { DeleteDC(hdcMem); ReleaseDC(g_hwndMC, hdcWin); return false; }
    HBITMAP hbmOld = (HBITMAP)SelectObject(hdcMem, hbm);
    BitBlt(hdcMem, 0, 0, (int)g_w, (int)g_h, hdcWin, 0, 0, SRCCOPY);

    BITMAPINFO bi = {};
    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bi.bmiHeader.biWidth = (LONG)g_w;
    bi.bmiHeader.biHeight = -(LONG)g_h;
    bi.bmiHeader.biPlanes = 1;
    bi.bmiHeader.biBitCount = 32;
    bi.bmiHeader.biCompression = BI_RGB;

    UINT rowSize = g_w * 4;
    UINT dataSize = rowSize * g_h;
    BYTE* pixels = new BYTE[dataSize];
    GetDIBits(hdcWin, hbm, 0, (UINT)g_h, pixels, &bi, DIB_RGB_COLORS);

    for (UINT i = 0; i < dataSize; i += 4) {
        BYTE tmp = pixels[i];
        pixels[i] = pixels[i + 2];
        pixels[i + 2] = tmp;
    }

    SelectObject(hdcMem, hbmOld);
    DeleteObject(hbm);
    DeleteDC(hdcMem);
    ReleaseDC(g_hwndMC, hdcWin);

    bool needRecreate = (g_mcCaptureW != g_w || g_mcCaptureH != g_h || !g_texMCFrame);
    if (needRecreate) {
        g_texMCFrame.Reset();
        D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
        D3D12_RESOURCE_DESC td = {};
        td.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        td.Width = g_w; td.Height = g_h;
        td.DepthOrArraySize = 1; td.MipLevels = 1;
        td.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        td.SampleDesc.Count = 1;
        if (FAILED(g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
            &td, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&g_texMCFrame)))) {
            delete[] pixels;
            return false;
        }
        D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc = {};
        srvDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
        srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
        srvDesc.Texture2D.MipLevels = 1;
        g_dev->CreateShaderResourceView(g_texMCFrame.Get(), &srvDesc,
            g_srvHeap->GetCPUDescriptorHandleForHeapStart());
        g_mcCaptureW = g_w; g_mcCaptureH = g_h;
    }

    UINT alignedRowSize = (rowSize + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1) & ~(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    D3D12_HEAP_PROPERTIES hpUp = {}; hpUp.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rdUp = {};
    rdUp.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rdUp.Width = alignedRowSize * (UINT64)g_h;
    rdUp.Height = 1; rdUp.DepthOrArraySize = 1;
    rdUp.MipLevels = 1; rdUp.SampleDesc.Count = 1;
    rdUp.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    ComPtr<ID3D12Resource> upBuf;
    g_dev->CreateCommittedResource(&hpUp, D3D12_HEAP_FLAG_NONE,
        &rdUp, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&upBuf));

    BYTE* dst = nullptr;
    upBuf->Map(0, nullptr, (void**)&dst);
    for (UINT y = 0; y < g_h; y++)
        memcpy(dst + y * alignedRowSize, pixels + y * rowSize, rowSize);
    upBuf->Unmap(0, nullptr);
    delete[] pixels;

    ComPtr<ID3D12CommandAllocator> capAlloc;
    ComPtr<ID3D12GraphicsCommandList> capCL;
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&capAlloc));
    g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, capAlloc.Get(), nullptr, IID_PPV_ARGS(&capCL));

    D3D12_TEXTURE_COPY_LOCATION dstLoc = {};
    dstLoc.pResource = g_texMCFrame.Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    D3D12_TEXTURE_COPY_LOCATION srcLoc = {};
    srcLoc.pResource = upBuf.Get();
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    srcLoc.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    srcLoc.PlacedFootprint.Footprint.Width = g_w;
    srcLoc.PlacedFootprint.Footprint.Height = g_h;
    srcLoc.PlacedFootprint.Footprint.Depth = 1;
    srcLoc.PlacedFootprint.Footprint.RowPitch = alignedRowSize;

    capCL->CopyTextureRegion(&dstLoc, 0, 0, 0, &srcLoc, nullptr);

    D3D12_RESOURCE_BARRIER rb = {};
    rb.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    rb.Transition.pResource = g_texMCFrame.Get();
    rb.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_DEST;
    rb.Transition.StateAfter = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;
    rb.Transition.Subresource = 0;
    capCL->ResourceBarrier(1, &rb);
    capCL->Close();
    ID3D12CommandList* lists[] = {capCL.Get()};
    g_queue->ExecuteCommandLists(1, lists);
    WaitGPU();
    return true;
}

static void RepositionOverlay() {
    // 直接渲染到 MC 窗口，无需调整覆盖层位置，仅处理窗口大小变化
    if (!g_hwndMC) return;
    if (IsIconic(g_hwndMC)) return;
    RECT rc; GetClientRect(g_hwndMC, &rc);
    int newW = rc.right - rc.left, newH = rc.bottom - rc.top;
    if (newW <= 0 || newH <= 0) return;
    bool sizeChanged = ((int)g_w != newW || (int)g_h != newH);
    if (sizeChanged) {
        g_w = (UINT)newW; g_h = (UINT)newH;
        g_texMCFrame.Reset(); g_mcCaptureW = 0; g_mcCaptureH = 0;
        WaitGPU();
        for (auto& r : g_rt) r.Reset();
        g_depthBuf.Reset();
        g_cl.Reset(); g_alloc.Reset();
        g_swap->ResizeBuffers(2, g_w, g_h, DXGI_FORMAT_R8G8B8A8_UNORM, 0);
        g_fi = g_swap->GetCurrentBackBufferIndex();
        D3D12_CPU_DESCRIPTOR_HANDLE rh = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
        for (UINT n = 0; n < 2; n++) {
            g_swap->GetBuffer(n, IID_PPV_ARGS(&g_rt[n]));
            g_dev->CreateRenderTargetView(g_rt[n].Get(), nullptr, rh);
            rh.ptr += g_rtvSize;
        }
        if (g_dsvFormat != DXGI_FORMAT_UNKNOWN) {
            D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
            D3D12_RESOURCE_DESC depthDesc = {};
            depthDesc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
            depthDesc.Width = g_w; depthDesc.Height = g_h;
            depthDesc.DepthOrArraySize = 1; depthDesc.MipLevels = 1;
            depthDesc.Format = g_dsvFormat; depthDesc.SampleDesc.Count = 1;
            depthDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL;
            D3D12_CLEAR_VALUE cv = {}; cv.Format = g_dsvFormat;
            cv.DepthStencil.Depth = 1.0f;
            g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
                &depthDesc, D3D12_RESOURCE_STATE_DEPTH_WRITE, &cv, IID_PPV_ARGS(&g_depthBuf));
            g_dev->CreateDepthStencilView(g_depthBuf.Get(), nullptr,
                g_dsvHeap->GetCPUDescriptorHandleForHeapStart());
        }
        g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&g_alloc));
        g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, g_alloc.Get(), nullptr, IID_PPV_ARGS(&g_cl));
        g_cl->Close();
    }
}

static DXGI_FORMAT PickDepthFormat() {
    D3D12_FEATURE_DATA_FORMAT_SUPPORT fs = {};
    fs.Format = DXGI_FORMAT_D32_FLOAT;
    if (SUCCEEDED(g_dev->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fs, sizeof(fs)))
        && (fs.Support1 & D3D12_FORMAT_SUPPORT1_DEPTH_STENCIL))
        return DXGI_FORMAT_D32_FLOAT;
    fs.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
    if (SUCCEEDED(g_dev->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fs, sizeof(fs)))
        && (fs.Support1 & D3D12_FORMAT_SUPPORT1_DEPTH_STENCIL))
        return DXGI_FORMAT_D24_UNORM_S8_UINT;
    fs.Format = DXGI_FORMAT_D16_UNORM;
    if (SUCCEEDED(g_dev->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fs, sizeof(fs)))
        && (fs.Support1 & D3D12_FORMAT_SUPPORT1_DEPTH_STENCIL))
        return DXGI_FORMAT_D16_UNORM;
    return DXGI_FORMAT_UNKNOWN;
}

static bool EnsureIMVBCapacity(UINT requiredBytes) {
    if (requiredBytes <= g_imVBCap) return true;
    UINT newCap = requiredBytes + (requiredBytes >> 1);
    ComPtr<ID3D12Resource> newVB;
    D3D12_HEAP_PROPERTIES hp = {}; hp.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rd = {};
    rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rd.Width = newCap; rd.Height = 1; rd.DepthOrArraySize = 1;
    rd.MipLevels = 1; rd.SampleDesc.Count = 1;
    rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    if (FAILED(g_dev->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
        &rd, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&newVB))))
        return false;
    if (g_imVB && g_imVBSize > 0) {
        void* oldDst = nullptr, *newDst = nullptr;
        g_imVB->Map(0, nullptr, &oldDst);
        newVB->Map(0, nullptr, &newDst);
        memcpy(newDst, oldDst, g_imVBSize);
        newVB->Unmap(0, nullptr);
        g_imVB->Unmap(0, nullptr);
    }
    g_imVB = newVB;
    g_imVBCap = newCap;
    g_imVbv.BufferLocation = g_imVB->GetGPUVirtualAddress();
    return true;
}

struct Vertex2D { float x,y,u,v; };
struct VertexPC { float x,y,z; UINT color; };
struct VertexPT { float x,y,z; UINT color; float u,v; };

static DWORD WINAPI RenderLoop(LPVOID) {
    MessageBoxA(NULL, "RenderLoop ENTERED", "DEBUG", MB_OK);
    Log("=== RenderLoop ENTERED (FORCED) ===");
    Log("=== RenderLoop DIAGNOSTIC VERSION 2 ===");
    Log("Render thread started");
    int loopCount = 0;
    Vertex2D fsQuad[] = {{-1,-1,0,1},{3,-1,2,1},{-1,3,0,-1}};
    MkUpload(g_vbFSQuad, fsQuad, sizeof(fsQuad));

    while (true) {
        Log("=== RenderLoop LOOP ITERATION ===");
        if (!g_run) {
            // g_run 被设为 false（可能是 CleanupD3D12 调用），退出
            Log("Render thread: g_run=false, exiting");
            break;
        }

        loopCount++;
        if (loopCount % 60 == 0) {
            Log("Render loop iteration %d", loopCount);
        }
        // 等待 Java 端通知数据已准备好（最多等待 100ms 避免完全卡死）
        DWORD waitResult = WaitForSingleObject(g_frameReadyEvent, 100);

        if (waitResult == WAIT_OBJECT_0) {
            // 数据已准备好，执行渲染
            std::vector<float> vertices;
            std::vector<float> uvs;
            int vertexCount = 0;
            {
                std::lock_guard<std::mutex> lock(g_dataMutex);
                if (g_hasNewVertexData && !g_vertexData.empty()) {
                    vertices = g_vertexData;
                    uvs = g_uvData;
                    vertexCount = g_newVertexCount;
                    g_hasNewVertexData = false;
                }
            }

            if (!vertices.empty() && vertexCount > 0) {
                // 诊断：输出顶点坐标范围
                float minX = 1e10f, maxX = -1e10f, minY = 1e10f, maxY = -1e10f, minZ = 1e10f, maxZ = -1e10f;
                for (int i = 0; i < vertexCount; i++) {
                    float x = vertices[i * 3], y = vertices[i * 3 + 1], z = vertices[i * 3 + 2];
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                    if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
                }
                Log("Vertex range: X[%.1f, %.1f] Y[%.1f, %.1f] Z[%.1f, %.1f] (count=%d)", minX, maxX, minY, maxY, minZ, maxZ, vertexCount);

                // 诊断：CPU端模拟MVP变换，输出前3个顶点的NDC坐标
                if (g_cbData && vertexCount > 0) {
                    float* m = (float*)g_cbData;
                    Log("Current MVP row0: [%.4f %.4f %.4f %.4f]", m[0], m[1], m[2], m[3]);
                    Log("Current MVP row1: [%.4f %.4f %.4f %.4f]", m[4], m[5], m[6], m[7]);
                    for (int i = 0; i < min(vertexCount, 3); i++) {
                        float vx = vertices[i * 3], vy = vertices[i * 3 + 1], vz = vertices[i * 3 + 2];
                        float clipX = m[0]*vx + m[1]*vy + m[2]*vz + m[3];
                        float clipY = m[4]*vx + m[5]*vy + m[6]*vz + m[7];
                        float clipZ = m[8]*vx + m[9]*vy + m[10]*vz + m[11];
                        float clipW = m[12]*vx + m[13]*vy + m[14]*vz + m[15];
                        float ndcX = (clipW != 0) ? clipX / clipW : clipX;
                        float ndcY = (clipW != 0) ? clipY / clipW : clipY;
                        float ndcZ = (clipW != 0) ? clipZ / clipW : clipZ;
                        Log("  vertex[%d] world(%.1f,%.1f,%.1f) -> ndc(%.3f,%.3f,%.3f)", i, vx, vy, vz, ndcX, ndcY, ndcZ);
                    }
                }

                // ===== 根据坐标范围选择投影矩阵 =====
                bool isScreenCoords = (maxZ == 0.0f && minZ == 0.0f); // 所有Z为0 → GUI顶点，使用正交投影
                if (isScreenCoords && g_cbData) {
                    // 使用正交投影矩阵（直接映射到屏幕空间）
                    float ortho[16] = {
                        2.0f/854, 0, 0, -1,
                        0, -2.0f/480, 0, 1,
                        0, 0, 1, 0,
                        0, 0, 0, 1
                    };
                    memcpy(g_cbData, ortho, sizeof(ortho));
                    Log("Using orthographic projection for GUI vertices (screen coords)");
                } else {
                    // 使用Java端传来的MVP矩阵（已经包含在g_cbData中）
                    Log("Using perspective projection for 3D vertices");
                }
                // ===== 新增结束 =====

                // ===== 将四边形转换为三角形（GUI顶点） =====
                if (isScreenCoords && vertexCount % 4 == 0 && vertexCount > 0) {
                    std::vector<float> triVertices;
                    triVertices.reserve(vertexCount * 6 / 4); // 4顶点 → 6顶点 (两个三角形)

                    for (int i = 0; i < vertexCount; i += 4) {
                        // 四边形顶点: v0, v1, v2, v3
                        // 转换为两个三角形: v0, v1, v2 和 v2, v3, v0
                        int idx0 = i * 3, idx1 = (i+1) * 3, idx2 = (i+2) * 3, idx3 = (i+3) * 3;

                        // 三角形1: v0, v1, v2
                        triVertices.push_back(vertices[idx0]); triVertices.push_back(vertices[idx0+1]); triVertices.push_back(vertices[idx0+2]);
                        triVertices.push_back(vertices[idx1]); triVertices.push_back(vertices[idx1+1]); triVertices.push_back(vertices[idx1+2]);
                        triVertices.push_back(vertices[idx2]); triVertices.push_back(vertices[idx2+1]); triVertices.push_back(vertices[idx2+2]);

                        // 三角形2: v2, v3, v0
                        triVertices.push_back(vertices[idx2]); triVertices.push_back(vertices[idx2+1]); triVertices.push_back(vertices[idx2+2]);
                        triVertices.push_back(vertices[idx3]); triVertices.push_back(vertices[idx3+1]); triVertices.push_back(vertices[idx3+2]);
                        triVertices.push_back(vertices[idx0]); triVertices.push_back(vertices[idx0+1]); triVertices.push_back(vertices[idx0+2]);
                    }

                    vertices = triVertices;
                    vertexCount = (int)vertices.size() / 3;
                    Log("Converted QUADS to TRIANGLES: %d vertices", vertexCount);
                }
                // ===== 四边形转换结束 =====

                Log("RenderLoop: rendering %d vertices from new storage", vertexCount);

                // 1. 更新顶点缓冲区
                UINT byteCount = vertexCount * sizeof(VertexPC);
                if (EnsureIMVBCapacity(byteCount)) {
                    void* dst = nullptr;
                    g_imVB->Map(0, nullptr, &dst);
                    VertexPC* vtx = (VertexPC*)dst;
                    for (int i = 0; i < vertexCount; i++) {
                        vtx[i].x = vertices[i * 3];
                        vtx[i].y = vertices[i * 3 + 1];
                        vtx[i].z = vertices[i * 3 + 2];
                        // ===== 强制颜色测试 =====
                        unsigned char r = (i % 2 == 0) ? 255 : 0;
                        unsigned char g = (i % 3 == 0) ? 255 : 0;
                        unsigned char b = (i % 5 == 0) ? 255 : 0;
                        vtx[i].color = (0xFF << 24) | (r << 16) | (g << 8) | b;
                        // 验证立即写入的值
                        if (i == 0) Log("=== WRITE VERIFY: i=0, color=0x%08X ===", vtx[0].color);
                    }
                    Log("=== FORCE COLOR TEST: vertex[0].color = 0x%08X ===", vtx[0].color);
                    g_imVB->Unmap(0, nullptr);
                    g_imVBSize = byteCount;
                    g_imVbv.SizeInBytes = g_imVBSize;

                    // 2. 记录绘制调用
                    EnterCriticalSection(&g_stateLock);
                    DrawChunk ch;
                    ch.byteOffset = 0;
                    ch.vertexCount = vertexCount;
                    ch.topo = D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;
                    ch.textured = false;
                    ch.vertexStride = sizeof(VertexPC);
                    ch.blend = false;
                    ch.textureId = 0;
                    g_drawChunks.clear();
                    g_drawChunks.push_back(ch);
                    g_imVertCount = vertexCount;
                    LeaveCriticalSection(&g_stateLock);
                }
            }
        } else if (waitResult == WAIT_TIMEOUT) {
            // 超时，跳过本帧（没有新数据）
            continue;
        } else {
            // 错误
            Log("WaitForSingleObject error, result=%d", (int)waitResult);
            break;
        }

        // 安全检查：确保核心资源可用
        if (!g_imVB || !g_cl || !g_swap || !g_dev) {
            Sleep(16);
            continue;
        }

        RepositionOverlay();
        CaptureMCFrame();

        g_alloc->Reset();
        g_cl->Reset(g_alloc.Get(), nullptr);
        g_fi = g_swap->GetCurrentBackBufferIndex();

        D3D12_RESOURCE_BARRIER rb = {};
        rb.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
        rb.Transition.pResource = g_rt[g_fi].Get();
        rb.Transition.StateBefore = D3D12_RESOURCE_STATE_PRESENT;
        rb.Transition.StateAfter = D3D12_RESOURCE_STATE_RENDER_TARGET;
        rb.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
        g_cl->ResourceBarrier(1, &rb);

        D3D12_CPU_DESCRIPTOR_HANDLE rtv = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
        rtv.ptr += (SIZE_T)g_fi * g_rtvSize;
        bool hasDSV = (g_dsvHeap && g_depthBuf);
        D3D12_CPU_DESCRIPTOR_HANDLE dsvH = {};
        if (hasDSV) {
            dsvH = g_dsvHeap->GetCPUDescriptorHandleForHeapStart();
            g_cl->OMSetRenderTargets(1, &rtv, TRUE, &dsvH);
        } else {
            g_cl->OMSetRenderTargets(1, &rtv, FALSE, nullptr);
        }

        float bg[4] = {0.0f, 0.0f, 0.0f, 1.0f};  // 黑色
        g_cl->ClearRenderTargetView(rtv, bg, 0, nullptr);
        if (hasDSV) g_cl->ClearDepthStencilView(dsvH, D3D12_CLEAR_FLAG_DEPTH, 1.0f, 0, 0, nullptr);

        D3D12_VIEWPORT vp = {0,0,(float)g_w,(float)g_h,0,1};
        D3D12_RECT sc = {0,0,(LONG)g_w,(LONG)g_h};
        g_cl->RSSetViewports(1, &vp);
        g_cl->RSSetScissorRects(1, &sc);

        // 诊断：打印当前使用的MVP矩阵（由Java端 nativeSetMvp 设置）
        if (g_cbData) {
            float* m = (float*)g_cbData;
            Log("RenderLoop using MVP: [%.3f %.3f %.3f %.3f]", m[0], m[1], m[2], m[3]);
            Log("                        [%.3f %.3f %.3f %.3f]", m[4], m[5], m[6], m[7]);
            Log("                        [%.3f %.3f %.3f %.3f]", m[8], m[9], m[10], m[11]);
            Log("                        [%.3f %.3f %.3f %.3f]", m[12], m[13], m[14], m[15]);
        }

        // Layer 0: MC capture
        if (g_texMCFrame && g_pso) {
            g_cl->SetGraphicsRootSignature(g_rs.Get());
            g_cl->SetPipelineState(g_pso.Get());
            g_cl->SetDescriptorHeaps(1, g_srvHeap.GetAddressOf());
            g_cl->SetGraphicsRootDescriptorTable(0, g_srvHeap->GetGPUDescriptorHandleForHeapStart());
            D3D12_VERTEX_BUFFER_VIEW fsVbv = {};
            fsVbv.BufferLocation = g_vbFSQuad->GetGPUVirtualAddress();
            fsVbv.StrideInBytes = sizeof(Vertex2D);
            fsVbv.SizeInBytes = sizeof(Vertex2D) * 3;
            g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
            g_cl->IASetVertexBuffers(0, 1, &fsVbv);
            g_cl->DrawInstanced(3, 1, 0, 0);
        }

        // Layer 1: GL geometry
        {
            EnterCriticalSection(&g_stateLock);
            auto chunks = g_drawChunks;
            UINT stateSnapshot = g_glStateBits;
            LeaveCriticalSection(&g_stateLock);

            // 临时禁用深度测试 - D3D12 不支持 OMSetDepthStencilState，深度测试由 PSO 控制
            // 如需禁用，可通过 Java 端调用 nativeSetGlState(0, GLB_DEPTH)

            for (auto& ch : chunks) {
                UINT state = stateSnapshot;
                int variantIdx = (int)((state << 1) | (ch.textured ? 1 : 0));
                bool isLine = (ch.topo == D3D_PRIMITIVE_TOPOLOGY_LINELIST || ch.topo == D3D_PRIMITIVE_TOPOLOGY_LINESTRIP);
                bool useTex = false;
                D3D12_GPU_DESCRIPTOR_HANDLE texSrv = {};
                if (ch.textured && ch.textureId > 0) {
                    auto it = g_texSlotMap.find(ch.textureId);
                    if (it != g_texSlotMap.end()) {
                        texSrv = g_texSrvHeap->GetGPUDescriptorHandleForHeapStart();
                        texSrv.ptr += (SIZE_T)it->second * g_texSrvSize;
                        useTex = true;
                    }
                }
                if (useTex && g_psoTex) {
                    g_cl->SetGraphicsRootSignature(g_rsTex.Get());
                    g_cl->SetPipelineState(g_psoTex.Get());
                    g_cl->SetDescriptorHeaps(1, g_texSrvHeap.GetAddressOf());
                    g_cl->SetGraphicsRootDescriptorTable(0, texSrv);
                    g_cl->SetGraphicsRootConstantBufferView(1, g_cbUpload->GetGPUVirtualAddress());
                } else {
                    if (isLine) {
                        if (!g_psoLineVariants[variantIdx]) BuildLinePSO(state, ch.textured);
                    } else {
                        if (!g_psoSolidVariants[variantIdx]) BuildSolidPSO(state, ch.textured);
                    }
                    ComPtr<ID3D12PipelineState>& pso = isLine ? g_psoLineVariants[variantIdx] : g_psoSolidVariants[variantIdx];
                    ComPtr<ID3D12RootSignature>& rs = isLine ? g_rsLineVariants[state & 0xF] : g_rsSolidVariants[state & 0xF];
                    if (!pso || !rs) continue;
                    g_cl->SetGraphicsRootSignature(rs.Get());
                    g_cl->SetPipelineState(pso.Get());
                    g_cl->SetGraphicsRootConstantBufferView(0, g_cbUpload->GetGPUVirtualAddress());
                }
                D3D12_VERTEX_BUFFER_VIEW chVbv = g_imVbv;
                chVbv.BufferLocation += (UINT64)ch.byteOffset;
                chVbv.SizeInBytes = ch.vertexCount * ch.vertexStride;
                chVbv.StrideInBytes = ch.vertexStride;
                g_cl->IASetVertexBuffers(0, 1, &chVbv);
                g_cl->IASetPrimitiveTopology(ch.topo);
                g_cl->DrawInstanced(ch.vertexCount, 1, 0, 0);
            }
        }

        EnterCriticalSection(&g_stateLock);
        g_imVertCount = 0;
        g_imVBSize = 0;
        g_drawChunks.clear();
        LeaveCriticalSection(&g_stateLock);

        rb.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
        rb.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
        g_cl->ResourceBarrier(1, &rb);
        g_cl->Close();

        ID3D12CommandList* lists[] = {g_cl.Get()};
        g_queue->ExecuteCommandLists(1, lists);
        g_swap->Present(1, 0);

        UINT64 fv = g_fenceVal;
        g_queue->Signal(g_fence.Get(), fv); g_fenceVal++;
        if (g_fence->GetCompletedValue() < fv) {
            g_fence->SetEventOnCompletion(fv, g_fenceEv);
            WaitForSingleObject(g_fenceEv, INFINITE);
        }
    }
    Log("Render thread stopped, loopCount=%d, g_run=%d", loopCount, (int)g_run);
    return 0;
}

static bool InitD3D12(HWND hwndMC) {
    MessageBoxA(NULL, "=== INITD3D12 V3 ===", "DEBUG", MB_OK);
    Log("=== !!! NEW VERSION WITH DIRECT RENDERING !!! ===");
    Log("=== INITD3D12 V2 START ===");

    // --- 新增：强制清理所有可能的旧覆盖层窗口 ---
    Log("Forcefully cleaning up any existing overlay windows...");
    // 枚举所有顶级窗口
    EnumWindows([](HWND hwnd, LPARAM lParam) -> BOOL {
        wchar_t className[256];
        if (GetClassNameW(hwnd, className, 256)) {
            // 如果窗口类名以 "GL4DX12_Overlay" 开头，就销毁它
            if (wcsstr(className, L"GL4DX12_Overlay") == className) {
                Log("Found and is destroying old overlay window (Class: %S, HWND: 0x%p)", className, hwnd);
                DestroyWindow(hwnd);
            }
        }
        return TRUE;
    }, 0);
    // 确保 g_hwndOverlay 全局变量也被清空
    if (g_hwndOverlay) {
        Log("Clearing global overlay handle");
        g_hwndOverlay = nullptr;
    }
    // --- 强制清理结束 ---

    MessageBoxA(NULL, "InitD3D12 called!", "Debug", MB_OK);
    OutputDebugStringA("=== InitD3D12 ENTERED ===\n");
    Log("=== InitD3D12 ENTERED ===");
    Log("=== Init D3D12 overlay on MC window (HWND=0x%p) ===", hwndMC);
    g_hwndMC = hwndMC;
    if (!g_hwndMC) {
           Log("ERROR: g_hwndMC is NULL");  // 添加
           return false;
        }

RECT rc;
if (!GetClientRect(g_hwndMC, &rc)) {
    Log("ERROR: GetClientRect failed, error=%d", GetLastError());
    return false;
}
Log("GetClientRect succeeded: left=%d, top=%d, right=%d, bottom=%d", rc.left, rc.top, rc.right, rc.bottom);
g_w = (UINT)(rc.right - rc.left);
g_h = (UINT)(rc.bottom - rc.top);
Log("Client rect: %dx%d", g_w, g_h);

g_hwndOverlay = g_hwndMC;  // 直接使用 MC 窗口，不创建覆盖层
Log("Using MC window directly as overlay");

ComPtr<IDXGIFactory4> dxgi;
    if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&dxgi)))) {
        Log("ERROR: CreateDXGIFactory1 failed");  // 添加
        return false;
    }
    Log("DXGI Factory created");
    ComPtr<IDXGIAdapter1> adp;
    for (UINT i=0; dxgi->EnumAdapters1(i,&adp)!=DXGI_ERROR_NOT_FOUND; i++) {
        DXGI_ADAPTER_DESC1 d; adp->GetDesc1(&d);
        if (SUCCEEDED(D3D12CreateDevice(adp.Get(), D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_dev)))) {
            D3D12_FEATURE_DATA_FEATURE_LEVELS fl = {};
            g_dev->CheckFeatureSupport(D3D12_FEATURE_FEATURE_LEVELS, &fl, sizeof(fl));
            const char* flNames[] = {"Unknown","9.1","9.2","9.3","10.0","10.1","11.0","11.1","12.0","12.1","12.2"};
            int idx = (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_12_2) ? 10 :
                     (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_12_1) ? 9 :
                     (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_12_0) ? 8 :
                     (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_11_1) ? 7 : 6;
            WideCharToMultiByte(CP_UTF8, 0, d.Description, -1, g_d3d12Info, sizeof(g_d3d12Info), 0, 0);
            snprintf(g_d3d12Info + strlen(g_d3d12Info), sizeof(g_d3d12Info) - strlen(g_d3d12Info), " (FL%s)", flNames[idx]);
            break;
        }
        adp.Reset();
    }
    if (!g_dev && FAILED(D3D12CreateDevice(0, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_dev)))) return false;

    D3D12_COMMAND_QUEUE_DESC qd = {}; qd.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    if (FAILED(g_dev->CreateCommandQueue(&qd, IID_PPV_ARGS(&g_queue)))) return false;

    DXGI_SWAP_CHAIN_DESC1 sd = {};
    sd.BufferCount=2; sd.Width=g_w; sd.Height=g_h;
    sd.Format=DXGI_FORMAT_R8G8B8A8_UNORM;
    sd.BufferUsage=DXGI_USAGE_RENDER_TARGET_OUTPUT;
    sd.SwapEffect=DXGI_SWAP_EFFECT_FLIP_DISCARD; sd.SampleDesc.Count=1;
    ComPtr<IDXGISwapChain1> sc1;
    if (FAILED(dxgi->CreateSwapChainForHwnd(g_queue.Get(), g_hwndMC, &sd, 0, 0, &sc1))) return false;
    sc1.As(&g_swap); g_fi = g_swap->GetCurrentBackBufferIndex();

    D3D12_DESCRIPTOR_HEAP_DESC rd = {};
    rd.NumDescriptors=2; rd.Type=D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    if (FAILED(g_dev->CreateDescriptorHeap(&rd, IID_PPV_ARGS(&g_rtvHeap)))) return false;
    g_rtvSize = g_dev->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);

    rd = {}; rd.NumDescriptors=1; rd.Type=D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    rd.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
    if (FAILED(g_dev->CreateDescriptorHeap(&rd, IID_PPV_ARGS(&g_srvHeap)))) return false;
    g_srvSize = g_dev->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);

    D3D12_CPU_DESCRIPTOR_HANDLE rh = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    for (UINT n=0; n<2; n++) {
        g_swap->GetBuffer(n, IID_PPV_ARGS(&g_rt[n]));
        g_dev->CreateRenderTargetView(g_rt[n].Get(), nullptr, rh);
        rh.ptr += g_rtvSize;
    }

    if (FAILED(g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&g_alloc)))) return false;
    if (FAILED(g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, g_alloc.Get(), 0, IID_PPV_ARGS(&g_cl)))) return false;
    g_cl->Close();
    if (FAILED(g_dev->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence)))) return false;
    g_fenceVal=1; g_fenceEv=CreateEventW(0,0,0,0);

    g_dsvFormat = PickDepthFormat();
    if (g_dsvFormat != DXGI_FORMAT_UNKNOWN) {
        D3D12_DESCRIPTOR_HEAP_DESC dd = {};
        dd.NumDescriptors = 1; dd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_DSV;
        if (SUCCEEDED(g_dev->CreateDescriptorHeap(&dd, IID_PPV_ARGS(&g_dsvHeap)))) {
            D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
            D3D12_RESOURCE_DESC depthDesc = {};
            depthDesc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
            depthDesc.Width = g_w; depthDesc.Height = g_h; depthDesc.DepthOrArraySize = 1;
            depthDesc.MipLevels = 1; depthDesc.Format = g_dsvFormat;
            depthDesc.SampleDesc.Count = 1;
            depthDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL;
            D3D12_CLEAR_VALUE cv = {}; cv.Format = g_dsvFormat;
            cv.DepthStencil.Depth = 1.0f;
            g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
                &depthDesc, D3D12_RESOURCE_STATE_DEPTH_WRITE, &cv, IID_PPV_ARGS(&g_depthBuf));
            g_dev->CreateDepthStencilView(g_depthBuf.Get(), nullptr,
                g_dsvHeap->GetCPUDescriptorHandleForHeapStart());
        }
    }

    D3D12_HEAP_PROPERTIES hpCB = {}; hpCB.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rdCB = {};
    rdCB.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rdCB.Width = g_cbSize; rdCB.Height = 1; rdCB.DepthOrArraySize = 1;
    rdCB.MipLevels = 1; rdCB.SampleDesc.Count = 1;
    rdCB.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    if (SUCCEEDED(g_dev->CreateCommittedResource(&hpCB, D3D12_HEAP_FLAG_NONE,
        &rdCB, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&g_cbUpload)))) {
        g_cbUpload->Map(0, nullptr, (void**)&g_cbData);
        float identity[16] = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
        memcpy(g_cbData, identity, sizeof(identity));
    }

    if (!MkPSO()) return false;
    BuildSolidPSO(0, false);
    MkPSOTex();

    D3D12_HEAP_PROPERTIES hpIm = {}; hpIm.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rdIm = {};
    rdIm.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rdIm.Width = g_imVBCap; rdIm.Height = 1; rdIm.DepthOrArraySize = 1;
    rdIm.MipLevels = 1; rdIm.SampleDesc.Count = 1;
    rdIm.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    g_dev->CreateCommittedResource(&hpIm, D3D12_HEAP_FLAG_NONE,
        &rdIm, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&g_imVB));
    if (g_imVB) {
        g_imVbv.BufferLocation = g_imVB->GetGPUVirtualAddress();
        g_imVbv.StrideInBytes = sizeof(VertexPC);
        g_imVbv.SizeInBytes = g_imVBCap;
    }

    InitializeCriticalSection(&g_texLock);
    InitializeCriticalSection(&g_stateLock);
    g_ok = true; g_run = true;

    // 创建帧同步事件（必须在 CreateThread 之前）
    g_frameReadyEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    g_frameDoneEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!g_frameReadyEvent || !g_frameDoneEvent) {
        Log("ERROR: CreateEventW failed, err=%d", GetLastError());
        g_ok = false; g_run = false;
        return false;
    }
    Log("Frame sync events created: ready=0x%p, done=0x%p", g_frameReadyEvent, g_frameDoneEvent);

    g_thread = CreateThread(0, 0, RenderLoop, 0, 0, 0);
    Log("=== D3D12 on MC window Ready ===");
    return true;
}


static void CleanupD3D12() {
    g_run = false;
    if (g_thread) { WaitForSingleObject(g_thread, 3000); CloseHandle(g_thread); g_thread=0; }
    if (g_ok) {
        WaitGPU();
        CloseHandle(g_fenceEv);
        DeleteCriticalSection(&g_texLock);
        DeleteCriticalSection(&g_stateLock);
        for (auto& p : g_psoSolidVariants) p.Reset();
        for (auto& p : g_rsSolidVariants) p.Reset();
        for (auto& p : g_psoLineVariants) p.Reset();
        for (auto& p : g_rsLineVariants) p.Reset();
        g_psoTex.Reset(); g_rsTex.Reset(); g_texSrvHeap.Reset();
        g_texMap.clear(); g_texSlotMap.clear(); g_texSlotNext = 0;
        for (auto& r : g_rt) r.Reset();
        g_rtvHeap.Reset(); g_srvHeap.Reset();
        g_depthBuf.Reset(); g_dsvHeap.Reset();
        if (g_cbData) { g_cbUpload->Unmap(0, nullptr); g_cbData = nullptr; }
        g_cbUpload.Reset();
        g_texMCFrame.Reset(); g_vbFSQuad.Reset(); g_imVB.Reset();
        g_cl.Reset(); g_alloc.Reset(); g_swap.Reset(); g_queue.Reset(); g_fence.Reset(); g_dev.Reset();
        g_ok = false;
    }
    if (g_frameReadyEvent) { CloseHandle(g_frameReadyEvent); g_frameReadyEvent = nullptr; }
    if (g_frameDoneEvent)   { CloseHandle(g_frameDoneEvent);   g_frameDoneEvent = nullptr; }
    if (g_hwndOverlay && g_hwndOverlay != g_hwndMC) { DestroyWindow(g_hwndOverlay); }
    g_hwndOverlay = nullptr;
    g_hwndMC = nullptr;
}

// === JNI ===
extern "C" {

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeCleanup(JNIEnv* env, jclass cls) {
    // 调用 CleanupD3D12 清理资源
    CleanupD3D12();
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit
    (JNIEnv*, jclass, jlong hwnd) {
    Log("=== nativeInit V2 START ===");
    OutputDebugStringA("=== nativeInit ENTERED ===\n");
    Log("=== nativeInit ENTERED ===");
    CreateDirectoryA("C:\\temp", 0);
    if (g_ok) {
        Log("nativeInit: already initialized, returning true");
        return JNI_TRUE;
    }
    Log("nativeInit: calling InitD3D12 with HWND=0x%p", (HWND)hwnd);
    bool success = InitD3D12((HWND)hwnd);
    Log("nativeInit: InitD3D12 returned %s", success ? "true" : "false");
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv*, jclass) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv*, jclass) {}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeBeginFrame(JNIEnv*, jclass) {
    // 清空操作由 RenderLoop 后台线程负责，这里不做任何事
    // 避免重复清空导致已录制的顶点数据丢失
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeEndFrame(JNIEnv*, jclass) {
    // 通知 RenderLoop 线程：当前帧的顶点数据已经录制完成
    if (g_ok && g_frameReadyEvent) {
        SetEvent(g_frameReadyEvent);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeClearDrawList(JNIEnv*, jclass) {
    // 手动清空 DrawCall 列表
    if (!g_ok) return;
    EnterCriticalSection(&g_stateLock);
    g_drawChunks.clear();
    g_imVertCount = 0;
    g_imVBSize = 0;
    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv*, jclass, jint w, jint h) {
    if (!g_ok||!g_swap) return;
    if (g_w == (UINT)w && g_h == (UINT)h) return;
    WaitGPU();
    for (auto& r : g_rt) r.Reset();
    g_depthBuf.Reset();
    g_cl.Reset(); g_alloc.Reset();
    g_swap->ResizeBuffers(2,(UINT)w,(UINT)h,DXGI_FORMAT_R8G8B8A8_UNORM,0);
    g_w=(UINT)w; g_h=(UINT)h; g_fi=g_swap->GetCurrentBackBufferIndex();
    D3D12_CPU_DESCRIPTOR_HANDLE rh=g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    for (UINT n=0;n<2;n++) { g_swap->GetBuffer(n,IID_PPV_ARGS(&g_rt[n])); g_dev->CreateRenderTargetView(g_rt[n].Get(),0,rh); rh.ptr+=g_rtvSize; }
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,IID_PPV_ARGS(&g_alloc));
    g_dev->CreateCommandList(0,D3D12_COMMAND_LIST_TYPE_DIRECT,g_alloc.Get(),0,IID_PPV_ARGS(&g_cl));
    g_cl->Close();

    if (g_dsvFormat != DXGI_FORMAT_UNKNOWN) {
        D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
        D3D12_RESOURCE_DESC depthDesc = {};
        depthDesc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        depthDesc.Width = g_w; depthDesc.Height = g_h; depthDesc.DepthOrArraySize = 1;
        depthDesc.MipLevels = 1; depthDesc.Format = g_dsvFormat;
        depthDesc.SampleDesc.Count = 1;
        depthDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL;
        D3D12_CLEAR_VALUE cv = {}; cv.Format = g_dsvFormat;
        cv.DepthStencil.Depth = 1.0f;
        g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
            &depthDesc, D3D12_RESOURCE_STATE_DEPTH_WRITE, &cv, IID_PPV_ARGS(&g_depthBuf));
        g_dev->CreateDepthStencilView(g_depthBuf.Get(), nullptr,
            g_dsvHeap->GetCPUDescriptorHandleForHeapStart());
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetClearColor(JNIEnv*, jclass, jfloat r, jfloat g, jfloat b, jfloat a) {
    EnterCriticalSection(&g_stateLock);
    g_glClearColor[0]=r; g_glClearColor[1]=g; g_glClearColor[2]=b; g_glClearColor[3]=a;
    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetGlColor(JNIEnv*, jclass, jfloat r, jfloat g, jfloat b, jfloat a) {
    EnterCriticalSection(&g_stateLock);
    g_glColor[0]=r; g_glColor[1]=g; g_glColor[2]=b; g_glColor[3]=a;
    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeIsInitialized(JNIEnv*, jclass) { return g_ok?JNI_TRUE:JNI_FALSE; }

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeShowDebugWindow(JNIEnv*, jclass, jboolean s) {
    if (g_hwndOverlay) ShowWindow(g_hwndOverlay, s ? SW_SHOWNOACTIVATE : SW_HIDE);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadPixels(JNIEnv* env, jclass, jbyteArray pixels, jint w, jint h) {
    if (!g_ok || !pixels) return;
    jsize len = env->GetArrayLength(pixels);
    std::vector<jbyte> buf(len);
    env->GetByteArrayRegion(pixels, 0, len, buf.data());
    // UploadTexture would go here - simplified for now
    (void)buf; (void)w; (void)h;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordVertices(JNIEnv* env, jclass, jfloatArray verts, jint count, jbyteArray colorArray) {
    if (!g_ok || !g_imVB || !verts || count <= 0) return;

    jsize len = env->GetArrayLength(verts);
    if (len <= 0) return;
    UINT safeCount = (UINT)count;
    if ((jsize)(safeCount * 3) > len) {
        safeCount = (UINT)(len / 3);
    }

    UINT byteCount = safeCount * sizeof(VertexPC);
    if (!EnsureIMVBCapacity(g_imVBSize + byteCount)) return;
    std::vector<jfloat> buf(len);
    env->GetFloatArrayRegion(verts, 0, len, buf.data());

    // 读取颜色数据
    jbyte* colorData = nullptr;
    if (colorArray != nullptr) {
        colorData = env->GetByteArrayElements(colorArray, nullptr);
    }

    EnterCriticalSection(&g_stateLock);
    DrawChunk ch;
    ch.byteOffset = g_imVBSize;
    ch.vertexCount = safeCount;
    ch.topo = g_pendingTopo;
    ch.textured = false;
    ch.vertexStride = sizeof(VertexPC);
    ch.blend = (g_glStateBits & GLB_BLEND) != 0;
    ch.textureId = g_pendingTextureId;
    g_drawChunks.push_back(ch);

    void* dst = nullptr;
    g_imVB->Map(0, nullptr, &dst);
    VertexPC* vtx = (VertexPC*)((BYTE*)dst + g_imVBSize);
    for (UINT i = 0; i < safeCount; i++) {
        int off = i * 3;
        vtx[i].x = buf[off];
        vtx[i].y = buf[off + 1];
        vtx[i].z = buf[off + 2];
        if (colorData != nullptr) {
            // 使用 Java 端传来的颜色 (RGBA byte order → ABGR packed)
            int colorOffset = i * 4;
            vtx[i].color = ((colorData[colorOffset + 3] & 0xFF) << 24) |
                           ((colorData[colorOffset] & 0xFF) << 16) |
                           ((colorData[colorOffset + 1] & 0xFF) << 8) |
                           (colorData[colorOffset + 2] & 0xFF);
        } else {
            vtx[i].color = 0xFFFFFFFF;
        }
    }
    g_imVB->Unmap(0, nullptr);

    // 释放颜色数据
    if (colorData != nullptr) {
        env->ReleaseByteArrayElements(colorArray, colorData, JNI_ABORT);
    }
    g_imVertCount += safeCount;
    g_imVBSize += byteCount;
    g_imVbv.SizeInBytes = g_imVBSize;

    // 同步拷贝到新存储（供 RenderLoop 直接使用）
    {
        std::lock_guard<std::mutex> lock(g_dataMutex);
        g_vertexData.assign(buf.data(), buf.data() + (jsize)(safeCount * 3));
        g_newVertexCount = safeCount;
        g_hasNewVertexData = true;
    }
    Log("nativeRecordVertices: stored %d floats (%d vertices), byteCount=%d", (int)(safeCount * 3), (int)safeCount, (int)byteCount);
    // 打印前几个顶点的坐标用于诊断
    int logCount = (int)min(safeCount, (UINT)5);
    for (UINT i = 0; i < (UINT)logCount; i++) {
        int off = i * 3;
        Log("  [JNI]Received vertex[%d]: %.4f, %.4f, %.4f", i, buf[off], buf[off+1], buf[off+2]);
    }

    // 立即通知渲染线程有新数据
    if (g_frameReadyEvent) {
        SetEvent(g_frameReadyEvent);
    }

    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordVerticesUV(JNIEnv* env, jclass, jfloatArray verts, jint count) {
    if (!g_ok || !g_imVB || !verts || count <= 0) return;
    UINT byteCount = (UINT)count * sizeof(VertexPT);
    if (!EnsureIMVBCapacity(g_imVBSize + byteCount)) return;

    jsize len = env->GetArrayLength(verts);
    std::vector<jfloat> buf(len);
    env->GetFloatArrayRegion(verts, 0, len, buf.data());

    EnterCriticalSection(&g_stateLock);
    DrawChunk ch;
    ch.byteOffset = g_imVBSize;
    ch.vertexCount = (UINT)count;
    ch.topo = g_pendingTopo;
    ch.textured = true;
    ch.vertexStride = sizeof(VertexPT);
    ch.blend = (g_glStateBits & GLB_BLEND) != 0;
    ch.textureId = g_pendingTextureId;
    g_drawChunks.push_back(ch);

    void* dst = nullptr;
    g_imVB->Map(0, nullptr, &dst);
    VertexPT* vtx = (VertexPT*)((BYTE*)dst + g_imVBSize);
    for (int i = 0; i < count; i++) {
        int off = i * 9;
        vtx[i].x = buf[off];
        vtx[i].y = buf[off+1];
        vtx[i].z = buf[off+2];
        UINT r = (UINT)(buf[off+3] * 255.0f) & 0xFF;
        UINT g = (UINT)(buf[off+4] * 255.0f) & 0xFF;
        UINT b = (UINT)(buf[off+5] * 255.0f) & 0xFF;
        UINT a = (UINT)(buf[off+6] * 255.0f) & 0xFF;
        vtx[i].color = (a << 24) | (r << 16) | (g << 8) | b;
        vtx[i].u = buf[off+7];
        vtx[i].v = buf[off+8];
    }
    g_imVB->Unmap(0, nullptr);
    g_imVertCount += count;
    g_imVBSize += byteCount;
    g_imVbv.SizeInBytes = g_imVBSize;
    LeaveCriticalSection(&g_stateLock);
}

// ========== 简单顶点数据接收（供 Java 端直接使用） ==========
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordUV(JNIEnv* env, jclass, jfloatArray uvs) {
    if (!uvs) { OutputDebugStringA("nativeRecordUV: uvs is null\n"); return; }
    jsize len = env->GetArrayLength(uvs);
    if (len == 0) { OutputDebugStringA("nativeRecordUV: uvs is empty\n"); return; }
    jfloat* data = env->GetFloatArrayElements(uvs, nullptr);
    if (!data) { OutputDebugStringA("nativeRecordUV: GetFloatArrayElements failed\n"); return; }
    {
        std::lock_guard<std::mutex> lock(g_dataMutex);
        g_uvData.assign(data, data + len);
    }
    char buf[128]; snprintf(buf, sizeof(buf), "nativeRecordUV: %d floats\n", (int)len); OutputDebugStringA(buf);
    env->ReleaseFloatArrayElements(uvs, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordColors(JNIEnv* env, jclass, jfloatArray colors) {
    if (!colors) { OutputDebugStringA("nativeRecordColors: colors is null\n"); return; }
    jsize len = env->GetArrayLength(colors);
    if (len == 0) { OutputDebugStringA("nativeRecordColors: colors is empty\n"); return; }
    jfloat* data = env->GetFloatArrayElements(colors, nullptr);
    if (!data) { OutputDebugStringA("nativeRecordColors: GetFloatArrayElements failed\n"); return; }
    if (len >= 4) {
        std::lock_guard<std::mutex> lock(g_dataMutex);
        g_colorDataBuf[0] = data[0]; g_colorDataBuf[1] = data[1];
        g_colorDataBuf[2] = data[2]; g_colorDataBuf[3] = data[3];
    }
    char buf[128]; snprintf(buf, sizeof(buf), "nativeRecordColors: %d floats\n", (int)len); OutputDebugStringA(buf);
    env->ReleaseFloatArrayElements(colors, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetPrimitiveTopology(JNIEnv*, jclass, jint topo) {
    static const D3D_PRIMITIVE_TOPOLOGY map[] = {
        D3D_PRIMITIVE_TOPOLOGY_UNDEFINED,
        D3D_PRIMITIVE_TOPOLOGY_POINTLIST,
        D3D_PRIMITIVE_TOPOLOGY_LINELIST,
        D3D_PRIMITIVE_TOPOLOGY_LINESTRIP,
        D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST,
        D3D_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP,
        D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST,
    };
    if (topo >= 0 && topo <= 6) g_pendingTopo = map[topo];
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetDrawTexture(JNIEnv*, jclass, jint texId) { g_pendingTextureId = texId; }
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetTexture(JNIEnv*, jclass, jint texId) { g_currentTexId = texId; }

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadTextureEx(JNIEnv* env, jclass, jbyteArray pixels, jint w, jint h, jint texId) {
    if (!g_ok || !pixels) return;
    jsize len = env->GetArrayLength(pixels);
    std::vector<jbyte> buf(len);
    env->GetByteArrayRegion(pixels, 0, len, buf.data());
    UploadTextureEx(buf.data(), (int)w, (int)h, (int)texId);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetGlState(JNIEnv*, jclass, jint enableBits, jint disableBits) {
    EnterCriticalSection(&g_stateLock);
    g_glStateBits |= (UINT)enableBits;
    g_glStateBits &= ~(UINT)disableBits;
    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetViewport(JNIEnv*, jclass, jint x, jint y, jint w, jint h) {}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetBlendFunc(JNIEnv*, jclass, jint sfactor, jint dfactor) {
    auto map = [](int gl) -> D3D12_BLEND {
        switch (gl) {
        case 0: return D3D12_BLEND_ZERO;
        case 1: return D3D12_BLEND_ONE;
        case 768: return D3D12_BLEND_SRC_COLOR;
        case 769: return D3D12_BLEND_INV_SRC_COLOR;
        case 770: return D3D12_BLEND_SRC_ALPHA;
        case 771: return D3D12_BLEND_INV_SRC_ALPHA;
        default: return D3D12_BLEND_ONE;
        }
    };
    g_glSrcBlend = map(sfactor);
    g_glDstBlend = map(dfactor);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetDepthMask(JNIEnv*, jclass, jboolean write) {
    EnterCriticalSection(&g_stateLock);
    if (write) g_glStateBits |= GLB_DEPTH_WRITE;
    else g_glStateBits &= ~GLB_DEPTH_WRITE;
    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetMvp(JNIEnv* env, jclass, jfloatArray matrix) {
    Log("=== nativeSetMvp CALLED ===");
    if (!g_cbData) {
        Log("nativeSetMvp: g_cbData is NULL!");
        return;
    }
    jfloat* src = env->GetFloatArrayElements(matrix, nullptr);
    if (src) {
        memcpy(g_cbData, src, 64);
        Log("MVP matrix set: [%.3f %.3f %.3f %.3f]", src[0], src[1], src[2], src[3]);
        Log("MVP matrix set: [%.3f %.3f %.3f %.3f]", src[4], src[5], src[6], src[7]);
        Log("MVP matrix set: [%.3f %.3f %.3f %.3f]", src[8], src[9], src[10], src[11]);
        Log("MVP matrix set: [%.3f %.3f %.3f %.3f]", src[12], src[13], src[14], src[15]);
        Log("nativeSetMvp: matrix updated successfully");
        env->ReleaseFloatArrayElements(matrix, src, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL Java_com_dx12_DX12LibClient_nativeGetWindowWidth(JNIEnv*, jclass) { return (jint)g_w; }
JNIEXPORT jint JNICALL Java_com_dx12_DX12LibClient_nativeGetWindowHeight(JNIEnv*, jclass) { return (jint)g_h; }

JNIEXPORT jstring JNICALL Java_com_dx12_DX12LibClient_nativeGetD3D12Info(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_d3d12Info);
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeIsD3D12Active(JNIEnv*, jclass) {
    return g_ok ? JNI_TRUE : JNI_FALSE;
}

// === Phase 3: Sky, Terrain, Entity, Particle ===
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDraw(JNIEnv*, jclass, jint vertexCount) {
    (void)vertexCount;
    OutputDebugStringA("nativeDraw: called, signaling render thread\n");
    if (g_frameReadyEvent != nullptr) {
        SetEvent(g_frameReadyEvent);
    } else {
        OutputDebugStringA("nativeDraw: g_frameReadyEvent is null!\n");
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderSky(JNIEnv*, jclass) {
    // 使用 g_skyParams 渲染天空盒
    // 目前用简单颜色填充，后续可扩展为球体/穹顶
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderTerrain(JNIEnv*, jclass) {
    // 渲染地形（方块）— 顶点数据已通过 nativeRecordVertices 上传
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadEntities(JNIEnv* env, jclass, jfloatArray entityData, jint count) {
    // 批量上传实体数据到 GPU
    (void)env; (void)entityData; (void)count;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderEntities(JNIEnv*, jclass) {
    // 绘制所有实体
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetSkyParameters(JNIEnv* env, jclass, jfloatArray params) {
    // 存储天空参数供 RenderLoop 使用
    (void)env; (void)params;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadParticles(JNIEnv* env, jclass, jfloatArray particles, jint count) {
    // 上传粒子数据到 GPU
    (void)env; (void)particles; (void)count;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderParticles(JNIEnv*, jclass) {
    // 绘制所有粒子
}

}
