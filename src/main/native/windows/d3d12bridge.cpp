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
    case WM_ERASEBKGND: return 1;
    case WM_NCHITTEST:  return HTTRANSPARENT;
    }
    return DefWindowProcW(h, w, m, l);
}

static HWND CreateOverlayWindow(HWND hParent) {
    const wchar_t* cn = L"GL4DX12_Overlay";
    WNDCLASSW wc = {};
    wc.lpfnWndProc = OverlayWndProc;
    wc.hInstance = GetModuleHandleW(0);
    wc.lpszClassName = cn;
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);

    // 注册窗口类
    if (!RegisterClassW(&wc)) {
        DWORD err = GetLastError();
        if (err != ERROR_CLASS_ALREADY_EXISTS) {
            Log("ERROR: RegisterClassW failed, error=%d", err);
            return nullptr;
        }
    }
    Log("Window class registered/ok");

    // 创建窗口
    HWND hw = CreateWindowExW(
        WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_NOACTIVATE,
        cn, L"GL4DX12 Overlay", WS_POPUP,
        0, 0, (int)g_w, (int)g_h,
        nullptr, nullptr, wc.hInstance, nullptr);

    if (!hw) {
        DWORD err = GetLastError();
        Log("ERROR: CreateWindowExW failed, error=%d", err);
        return nullptr;
    }

    Log("Window created, HWND=0x%p", hw);
    ShowWindow(hw, SW_SHOWNOACTIVATE);
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
    pd.DepthStencilState.DepthEnable = (stateBits & GLB_DEPTH) ? TRUE : FALSE;
    pd.DepthStencilState.DepthWriteMask = (stateBits & GLB_DEPTH_WRITE) ? D3D12_DEPTH_WRITE_MASK_ALL : D3D12_DEPTH_WRITE_MASK_ZERO;
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
    if (!g_hwndOverlay || !g_hwndMC) return;
    if (IsIconic(g_hwndMC)) { ShowWindow(g_hwndOverlay, SW_HIDE); return; }
    RECT rc; GetClientRect(g_hwndMC, &rc);
    POINT pt = {0,0}; ClientToScreen(g_hwndMC, &pt);
    int newW = rc.right - rc.left, newH = rc.bottom - rc.top;
    if (newW <= 0 || newH <= 0) { ShowWindow(g_hwndOverlay, SW_HIDE); return; }
    if (!IsWindowVisible(g_hwndOverlay)) ShowWindow(g_hwndOverlay, SW_SHOWNOACTIVATE);
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
        InvalidateRect(g_hwndOverlay, nullptr, FALSE);
        UpdateWindow(g_hwndOverlay);
    }
    SetWindowPos(g_hwndOverlay, HWND_TOPMOST, pt.x, pt.y, newW, newH, SWP_NOACTIVATE | SWP_NOZORDER);
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
    Log("Render thread started");
    Vertex2D fsQuad[] = {{-1,-1,0,1},{3,-1,2,1},{-1,3,0,-1}};
    MkUpload(g_vbFSQuad, fsQuad, sizeof(fsQuad));

    while (g_run) {
        Sleep(16);
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

        float bg[4] = {0,0,0,1};
        g_cl->ClearRenderTargetView(rtv, bg, 0, nullptr);
        if (hasDSV) g_cl->ClearDepthStencilView(dsvH, D3D12_CLEAR_FLAG_DEPTH, 1.0f, 0, 0, nullptr);

        D3D12_VIEWPORT vp = {0,0,(float)g_w,(float)g_h,0,1};
        D3D12_RECT sc = {0,0,(LONG)g_w,(LONG)g_h};
        g_cl->RSSetViewports(1, &vp);
        g_cl->RSSetScissorRects(1, &sc);

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
    Log("Render thread stopped");
    return 0;
}

static bool InitD3D12(HWND hwndMC) {
    Log("=== INITD3D12 V2 START ===");
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

g_hwndOverlay = CreateOverlayWindow(g_hwndMC);
if (!g_hwndOverlay) {
    Log("ERROR: CreateOverlayWindow failed");
    return false;
}
Log("Overlay created");

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
    if (FAILED(dxgi->CreateSwapChainForHwnd(g_queue.Get(), g_hwndOverlay, &sd, 0, 0, &sc1))) return false;
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
    if (g_hwndOverlay) { DestroyWindow(g_hwndOverlay); g_hwndOverlay = nullptr; }
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

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordVertices(JNIEnv* env, jclass, jfloatArray verts, jint count) {
    if (!g_ok || !g_imVB || !verts || count <= 0) return;
    UINT byteCount = (UINT)count * sizeof(VertexPC);
    if (!EnsureIMVBCapacity(g_imVBSize + byteCount)) return;

    jsize len = env->GetArrayLength(verts);
    std::vector<jfloat> buf(len);
    env->GetFloatArrayRegion(verts, 0, len, buf.data());

    EnterCriticalSection(&g_stateLock);
    DrawChunk ch;
    ch.byteOffset = g_imVBSize;
    ch.vertexCount = (UINT)count;
    ch.topo = g_pendingTopo;
    ch.textured = false;
    ch.vertexStride = sizeof(VertexPC);
    ch.blend = (g_glStateBits & GLB_BLEND) != 0;
    ch.textureId = g_pendingTextureId;
    g_drawChunks.push_back(ch);

    void* dst = nullptr;
    g_imVB->Map(0, nullptr, &dst);
    VertexPC* vtx = (VertexPC*)((BYTE*)dst + g_imVBSize);
    for (int i = 0; i < count; i++) {
        int off = i * 7;
        vtx[i].x = buf[off];
        vtx[i].y = buf[off+1];
        vtx[i].z = buf[off+2];
        UINT r = (UINT)(buf[off+3] * 255.0f) & 0xFF;
        UINT g = (UINT)(buf[off+4] * 255.0f) & 0xFF;
        UINT b = (UINT)(buf[off+5] * 255.0f) & 0xFF;
        UINT a = (UINT)(buf[off+6] * 255.0f) & 0xFF;
        vtx[i].color = (a << 24) | (r << 16) | (g << 8) | b;
    }
    g_imVB->Unmap(0, nullptr);
    g_imVertCount += count;
    g_imVBSize += byteCount;
    g_imVbv.SizeInBytes = g_imVBSize;
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
    if (!g_cbData) return;
    jfloat* src = env->GetFloatArrayElements(matrix, nullptr);
    if (src) { memcpy(g_cbData, src, 64); env->ReleaseFloatArrayElements(matrix, src, JNI_ABORT); }
}

JNIEXPORT jint JNICALL Java_com_dx12_DX12LibClient_nativeGetWindowWidth(JNIEnv*, jclass) { return (jint)g_w; }
JNIEXPORT jint JNICALL Java_com_dx12_DX12LibClient_nativeGetWindowHeight(JNIEnv*, jclass) { return (jint)g_h; }

JNIEXPORT jstring JNICALL Java_com_dx12_DX12LibClient_nativeGetD3D12Info(JNIEnv* env, jclass) {
    return env->NewStringUTF(g_d3d12Info);
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeIsD3D12Active(JNIEnv*, jclass) {
    return g_ok ? JNI_TRUE : JNI_FALSE;
}

// === Phase 3: Skybox and Particle (empty stubs for now) ===
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDraw(JNIEnv* env, jclass cls, jint vertexCount) {
    // 空实现或调用现有绘制逻辑
}

}
