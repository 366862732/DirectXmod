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
// DX12 info for F3 debug screen
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
static ComPtr<ID3D12Resource> g_tex;        // upload buffer for pixel data
static ComPtr<ID3D12Resource> g_texDefault; // default heap texture (SRV)
static int g_texW = 0, g_texH = 0;
static CRITICAL_SECTION g_texLock;

// MC framebuffer capture via GDI BitBlt
static ComPtr<ID3D12Resource> g_texMCFrame; // MC window capture texture
static UINT g_mcCaptureW = 0, g_mcCaptureH = 0;

// Fullscreen quad for framebuffer mirror
static ComPtr<ID3D12Resource> g_vbFSQuad; // 6 vertices (2 triangles)

// PSO (textured quad)
static ComPtr<ID3D12RootSignature> g_rs;
static ComPtr<ID3D12PipelineState> g_pso;
static ComPtr<ID3D12Resource>      g_vbUpload;
static D3D12_VERTEX_BUFFER_VIEW    g_vbv = {};

// PSO (solid-color geometry — GL immediate mode translation)
static ComPtr<ID3D12RootSignature> g_rsSolid;
static ComPtr<ID3D12PipelineState> g_psoSolid;

// GL state mirror (updated from Java via JNI)
static float  g_glClearColor[4] = {0.15f, 0.15f, 0.15f, 1.0f};
static float  g_glColor[4] = {1, 1, 1, 1};
static int    g_glViewport[4] = {0, 0, 1280, 720};
static CRITICAL_SECTION g_stateLock;

// GL→D3D12 draw command recording
static ComPtr<ID3D12Resource> g_imVB;        // immediate-mode vertex buffer (upload heap)
static D3D12_VERTEX_BUFFER_VIEW g_imVbv = {};
static UINT  g_imVBCap = 16 * 1024 * 1024; // 16MB upload buffer (growable)
static UINT  g_imVBSize = 0;           // bytes written this frame
static UINT  g_imVertCount = 0;        // total vertices this frame

// Per-draw chunk (supports multiple draw calls with different topologies)
struct DrawChunk { UINT byteOffset; UINT vertexCount; D3D_PRIMITIVE_TOPOLOGY topo; bool textured; UINT vertexStride; bool blend; int textureId; };
static std::vector<DrawChunk> g_drawChunks;
static D3D_PRIMITIVE_TOPOLOGY g_pendingTopo = D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;
static int  g_pendingTextureId = 0;

// GL state bits for PSO variant selection
static UINT g_glStateBits = 0; // bit0=blend, bit1=depth, bit2=cull, bit3=depthWrite
#define GLB_BLEND        1
#define GLB_DEPTH        2
#define GLB_CULL         4
#define GLB_DEPTH_WRITE  8

// Depth buffer (format chosen at init via CheckFormatSupport)
static ComPtr<ID3D12Resource>       g_depthBuf;
static ComPtr<ID3D12DescriptorHeap> g_dsvHeap;
static DXGI_FORMAT g_dsvFormat = DXGI_FORMAT_UNKNOWN;

// Constant buffer for MVP transform
struct MvpCB { float mvp[16]; };
static ComPtr<ID3D12Resource> g_cbUpload; // upload buffer, 256-byte aligned
static BYTE* g_cbData = nullptr;          // mapped pointer
static const UINT g_cbSize = 256;

// Blend func tracking (for future PSO variations)
static D3D12_BLEND g_glSrcBlend = D3D12_BLEND_SRC_ALPHA;
static D3D12_BLEND g_glDstBlend = D3D12_BLEND_INV_SRC_ALPHA;

// PSO cache: indexed by (stateBits << 1) | textured
static ComPtr<ID3D12PipelineState> g_psoSolidVariants[32]; // 16 states (4bits) × 2 (textured/solid)
static ComPtr<ID3D12RootSignature> g_rsSolidVariants[16];
// Line PSO cache (separate since PrimitiveTopologyType differs)
static ComPtr<ID3D12PipelineState> g_psoLineVariants[32];
// Line RS cache — reuses same layout as solid (CBV(b0)) but separate for line-specific variants if needed
static ComPtr<ID3D12RootSignature> g_rsLineVariants[16];

struct Vertex2D { float x,y,u,v; };
struct VertexPC { float x,y,z; UINT color; };               // 16 bytes: POS(12) + COL(4)
struct VertexPT { float x,y,z; UINT color; float u,v; };    // 24 bytes: POS(12) + COL(4) + TEX(8)

// Texture cache: GL texture ID → D3D12 SRV
static std::unordered_map<int, ComPtr<ID3D12Resource>> g_texMap; // texture ID → default-heap texture
static std::unordered_map<int, UINT> g_texSlotMap;  // texture ID → heap slot index
static UINT g_texSlotNext = 0;                       // next free slot
static int g_currentTexId = 0; // set by Java side

// === Window — borderless overlay child on MC's window (click-through) ===
// We create a borderless popup as a child of MC's GLFW window.
// Input passes through (WS_EX_TRANSPARENT) so MC stays interactive.
static HWND g_hwndOverlay = nullptr; // our overlay
static HWND g_hwndMC = nullptr;      // MC's window (from JNI)

static LRESULT CALLBACK OverlayWndProc(HWND h, UINT m, WPARAM w, LPARAM l) {
    switch (m) {
    case WM_ERASEBKGND: return 1; // no flicker
    case WM_NCHITTEST:  return HTTRANSPARENT; // click-through
    }
    return DefWindowProcW(h, m, w, l);
}

static HWND CreateOverlayWindow(HWND hParent) {
    const wchar_t* cn = L"GL4DX12_Overlay";
    WNDCLASSW wc = {};
    wc.lpfnWndProc = OverlayWndProc;
    wc.hInstance = GetModuleHandleW(0);
    wc.lpszClassName = cn;
    wc.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    RegisterClassW(&wc);

    // NO WS_EX_LAYERED — it conflicts with D3D12 flip-model SwapChain.
    // WS_EX_TRANSPARENT for click-through, WS_EX_TOPMOST to float above MC,
    // WS_EX_NOACTIVATE so MC keeps keyboard focus.
    HWND hw = CreateWindowExW(
        WS_EX_TOPMOST | WS_EX_NOACTIVATE,
        cn, L"",
        WS_POPUP,
        0, 0, g_w, g_h,
        nullptr, nullptr, wc.hInstance, nullptr);

    ShowWindow(hw, SW_SHOWNOACTIVATE);

    // Position over parent's client area (in screen coordinates)
    if (hParent) {
        RECT rc;
        GetClientRect(hParent, &rc);
        POINT pt = {0, 0};
        ClientToScreen(hParent, &pt);
        RECT mcScreen = {pt.x, pt.y, pt.x + rc.right - rc.left, pt.y + rc.bottom - rc.top};
        SetWindowPos(hw, HWND_TOPMOST,
            mcScreen.left, mcScreen.top,
            mcScreen.right - mcScreen.left, mcScreen.bottom - mcScreen.top,
            SWP_NOACTIVATE | SWP_SHOWWINDOW);
        Log("Overlay created at (%d,%d) %dx%d",
            mcScreen.left, mcScreen.top,
            mcScreen.right - mcScreen.left, mcScreen.bottom - mcScreen.top);
    }

    return hw;
}

static void RepositionOverlay() {
    if (!g_hwndOverlay || !g_hwndMC) return;
    RECT rc;
    GetClientRect(g_hwndMC, &rc);
    POINT pt = {0, 0};
    ClientToScreen(g_hwndMC, &pt);
    int newW = rc.right - rc.left;
    int newH = rc.bottom - rc.top;
    if (newW <= 0 || newH <= 0) return;

    // Check if size changed — invalidate capture texture + overwrite g_w/g_h
    bool sizeChanged = ((int)g_w != newW || (int)g_h != newH);

    // Only move/resize if position/size actually changed
    static int s_lastW = 0, s_lastH = 0;
    static int s_lastX = -1, s_lastY = -1;
    if (!sizeChanged && s_lastW == newW && s_lastH == newH
        && s_lastX == pt.x && s_lastY == pt.y) return;
    s_lastW = newW; s_lastH = newH;
    s_lastX = pt.x; s_lastY = pt.y;

    if (sizeChanged) {
        g_w = (UINT)newW; g_h = (UINT)newH;
        // Force capture texture recreation next frame
        g_texMCFrame.Reset();
        g_mcCaptureW = 0; g_mcCaptureH = 0;
        Log("MC window resized -> %dx%d (capture tex reset)", g_w, g_h);
    }

    SetWindowPos(g_hwndOverlay, HWND_TOPMOST, pt.x, pt.y, newW, newH,
        SWP_NOACTIVATE | SWP_NOZORDER);
    if (sizeChanged)
        Log("Overlay repositioned: pos=(%d,%d) size=%dx%d", pt.x, pt.y, newW, newH);
}

// === GPU sync ===
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

// === Upload buffer helper ===
static bool MkUpload(ComPtr<ID3D12Resource>& dst, const void* data, UINT sz) {
    D3D12_HEAP_PROPERTIES hp = {}; hp.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC   rd = {};
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

// === HLSL (textured quad) ===
static const char* kVS = R"(
struct VS_IN { float2 p : POS; float2 uv : TEX; };
struct PS_IN { float4 p : SV_POSITION; float2 uv : TEX; };
PS_IN VSMain(VS_IN i) { PS_IN o; o.p=float4(i.p,0,1); o.uv=i.uv; return o; }
)";
static const char* kPS = R"(
struct PS_IN { float4 p : SV_POSITION; float2 uv : TEX; };
Texture2D gTex : register(t0);
SamplerState gSamp : register(s0);
float4 PSMain(PS_IN i) : SV_TARGET { return gTex.Sample(gSamp, i.uv); }
)";

// === Create PSO ===
static bool MkPSO() {
    ComPtr<ID3DBlob> vs,ps,err;
    if (FAILED(D3DCompile(kVS,strlen(kVS),0,0,0,"VSMain","vs_5_0",0,0,&vs,&err)))
    { Log("VS fail: %s",err?(char*)err->GetBufferPointer():"?"); return false; }
    if (FAILED(D3DCompile(kPS,strlen(kPS),0,0,0,"PSMain","ps_5_0",0,0,&ps,&err)))
    { Log("PS fail: %s",err?(char*)err->GetBufferPointer():"?"); return false; }

    // Root signature: 1 descriptor table (SRV) + 1 static sampler
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
        {"POS",0,DXGI_FORMAT_R32G32_FLOAT,    0,0,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"TEX",0,DXGI_FORMAT_R32G32_FLOAT,    0,8,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
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
    pd.DSVFormat = (g_dsvFormat != DXGI_FORMAT_UNKNOWN) ? g_dsvFormat : DXGI_FORMAT_UNKNOWN;
    pd.InputLayout = {ie,2};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;

    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_pso)))) { Log("PSO fail"); return false; }
    Log("PSO OK");
    return true;
}

// === Solid-color PSO (for GL immediate-mode translation) ===
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

// Build a solid-color PSO variant with given blend/depth/cull/textured flags
static bool BuildSolidPSO(UINT stateBits, bool textured) {
    int idx = (int)((stateBits << 1) | (textured ? 1 : 0));
    if (idx < 0 || idx >= 32) return false;
    if (g_psoSolidVariants[idx]) return true; // already built

    ComPtr<ID3DBlob> vs, ps, err;
    if (FAILED(D3DCompile(kVS_Solid, strlen(kVS_Solid), 0, 0, 0, "VSMain", "vs_5_0", 0, 0, &vs, &err)))
    { Log("VS_solid fail: %s", err ? (char*)err->GetBufferPointer() : "?"); return false; }
    if (FAILED(D3DCompile(kPS_Solid, strlen(kPS_Solid), 0, 0, 0, "PSMain", "ps_5_0", 0, 0, &ps, &err)))
    { Log("PS_solid fail: %s", err ? (char*)err->GetBufferPointer() : "?"); return false; }

    // Root signature: CBV(b0) for MVP + IA input layout
    ComPtr<ID3D12RootSignature>& rs = g_rsSolidVariants[stateBits & 0xF];
    if (!rs) {
        D3D12_ROOT_PARAMETER rpCB = {};
        rpCB.ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
        rpCB.Descriptor.ShaderRegister = 0;
        rpCB.Descriptor.RegisterSpace = 0;
        rpCB.ShaderVisibility = D3D12_SHADER_VISIBILITY_VERTEX;

        D3D12_ROOT_SIGNATURE_DESC rsd = {};
        rsd.NumParameters = 1;
        rsd.pParameters = &rpCB;
        rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
        ComPtr<ID3DBlob> rb;
        if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
        if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(),
            IID_PPV_ARGS(&rs)))) return false;
    }

    UINT ieCount = textured ? 3 : 2;
    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"COL", 0, DXGI_FORMAT_R8G8B8A8_UNORM,    0, 12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"TEX", 0, DXGI_FORMAT_R32G32_FLOAT,       0, 16,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
    };

    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = rs.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.BlendState.AlphaToCoverageEnable = FALSE;
    pd.BlendState.IndependentBlendEnable = FALSE;
    if (stateBits & GLB_BLEND) {
        pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
        pd.BlendState.RenderTarget[0].SrcBlend  = g_glSrcBlend;
        pd.BlendState.RenderTarget[0].DestBlend = g_glDstBlend;
        pd.BlendState.RenderTarget[0].BlendOp   = D3D12_BLEND_OP_ADD;
        pd.BlendState.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
        pd.BlendState.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_ZERO;
        pd.BlendState.RenderTarget[0].BlendOpAlpha = D3D12_BLEND_OP_ADD;
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

    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoSolidVariants[idx])))) {
        Log("PSO_variant fail idx=%d state=%d tex=%d", idx, stateBits, textured);
        return false;
    }
    Log("PSO_variant OK  idx=%d state=%d tex=%d", idx, stateBits, textured);
    return true;
}

// Build a line PSO variant with given state bits
static bool BuildLinePSO(UINT stateBits, bool textured) {
    int idx = (int)((stateBits << 1) | (textured ? 1 : 0));
    if (idx < 0 || idx >= 32) return false;
    if (g_psoLineVariants[idx]) return true;

    ComPtr<ID3DBlob> vs, ps, err;
    if (FAILED(D3DCompile(kVS_Solid, strlen(kVS_Solid), 0, 0, 0, "VSMain", "vs_5_0", 0, 0, &vs, &err)))
    { Log("LineVS fail: %s", err ? (char*)err->GetBufferPointer() : "?"); return false; }
    if (FAILED(D3DCompile(kPS_Solid, strlen(kPS_Solid), 0, 0, 0, "PSMain", "ps_5_0", 0, 0, &ps, &err)))
    { Log("LinePS fail: %s", err ? (char*)err->GetBufferPointer() : "?"); return false; }

    // Root signature — reuse same layout as solid (CBV b0)
    ComPtr<ID3D12RootSignature>& rs = g_rsLineVariants[stateBits & 0xF];
    if (!rs) {
        D3D12_ROOT_PARAMETER rpCB = {};
        rpCB.ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
        rpCB.Descriptor.ShaderRegister = 0; rpCB.Descriptor.RegisterSpace = 0;
        rpCB.ShaderVisibility = D3D12_SHADER_VISIBILITY_VERTEX;
        D3D12_ROOT_SIGNATURE_DESC rsd = {};
        rsd.NumParameters = 1; rsd.pParameters = &rpCB;
        rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
        ComPtr<ID3DBlob> rb2;
        if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb2, &err))) return false;
        if (FAILED(g_dev->CreateRootSignature(0, rb2->GetBufferPointer(), rb2->GetBufferSize(),
            IID_PPV_ARGS(&rs)))) return false;
    }

    UINT ieCount = textured ? 3 : 2;
    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"COL", 0, DXGI_FORMAT_R8G8B8A8_UNORM,    0, 12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"TEX", 0, DXGI_FORMAT_R32G32_FLOAT,       0, 16,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
    };

    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = rs.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.BlendState.AlphaToCoverageEnable = FALSE;
    pd.BlendState.IndependentBlendEnable = FALSE;
    if (stateBits & GLB_BLEND) {
        pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
        pd.BlendState.RenderTarget[0].SrcBlend  = g_glSrcBlend;
        pd.BlendState.RenderTarget[0].DestBlend = g_glDstBlend;
        pd.BlendState.RenderTarget[0].BlendOp   = D3D12_BLEND_OP_ADD;
        pd.BlendState.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
        pd.BlendState.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_ZERO;
        pd.BlendState.RenderTarget[0].BlendOpAlpha = D3D12_BLEND_OP_ADD;
    }
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = (stateBits & GLB_CULL) ? D3D12_CULL_MODE_BACK : D3D12_CULL_MODE_NONE;
    // Lines: no depth clip needed; still respect depth test
    pd.RasterizerState.DepthClipEnable = FALSE;
    pd.DepthStencilState.DepthEnable = (stateBits & GLB_DEPTH) ? TRUE : FALSE;
    pd.DepthStencilState.DepthWriteMask = (stateBits & GLB_DEPTH_WRITE) ? D3D12_DEPTH_WRITE_MASK_ALL : D3D12_DEPTH_WRITE_MASK_ZERO;
    pd.DepthStencilState.DepthFunc = D3D12_COMPARISON_FUNC_LESS_EQUAL;
    pd.DSVFormat = (g_dsvFormat != DXGI_FORMAT_UNKNOWN) ? g_dsvFormat : DXGI_FORMAT_UNKNOWN;
    pd.InputLayout = {ie, ieCount};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_LINE; // KEY: line PSO
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;

    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoLineVariants[idx])))) {
        Log("PSO_line fail idx=%d state=%d tex=%d", idx, stateBits, textured);
        return false;
    }
    Log("PSO_line OK  idx=%d state=%d tex=%d", idx, stateBits, textured);
    return true;
}

static bool MkPSOSolid() {
    // Pre-build blend + no-depth + no-cull (default solid)
    return BuildSolidPSO(GLB_BLEND, false);
}

// === Textured PSO (for GL→D3D12 with UV coordinates + texture lookup) ===
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

static bool MkPSOTex() {
    ComPtr<ID3DBlob> vs, ps, err, rb;
    err.Reset();
    if (FAILED(D3DCompile(kVS_Tex, strlen(kVS_Tex), 0,0,0, "VSMain","vs_5_0",0,0,&vs,&err)))
    { Log("VS_tex fail: %s", err?(char*)err->GetBufferPointer():"?"); return false; }
    err.Reset();
    if (FAILED(D3DCompile(kPS_Tex, strlen(kPS_Tex), 0,0,0, "PSMain","ps_5_0",0,0,&ps,&err)))
    { Log("PS_tex fail: %s", err?(char*)err->GetBufferPointer():"?"); return false; }

    // Root: descriptor table [0] with 1 SRV, CBV [1] for MVP
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
    rpCBV.Descriptor.RegisterSpace = 0;
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
    err.Reset();
    if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err)))
    { Log("Tex RS serialize fail: %s", err?(char*)err->GetBufferPointer():"?"); return false; }
    if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(),
        IID_PPV_ARGS(&g_rsTex))))
    { Log("Tex CreateRootSignature fail"); return false; }

    // VertexPT layout: POS(12) + COL(4) + TEX(8) = 24 bytes
    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS",0,DXGI_FORMAT_R32G32B32_FLOAT, 0,0, D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"COL",0,DXGI_FORMAT_R8G8B8A8_UNORM,     0,12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"TEX",0,DXGI_FORMAT_R32G32_FLOAT,       0,16,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
    };

    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = g_rsTex.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
    pd.BlendState.RenderTarget[0].SrcBlend  = D3D12_BLEND_SRC_ALPHA;
    pd.BlendState.RenderTarget[0].DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
    pd.BlendState.RenderTarget[0].BlendOp   = D3D12_BLEND_OP_ADD;
    pd.BlendState.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
    pd.BlendState.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_ZERO;
    pd.BlendState.RenderTarget[0].BlendOpAlpha = D3D12_BLEND_OP_ADD;
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = D3D12_CULL_MODE_NONE;
    pd.RasterizerState.DepthClipEnable = TRUE;
    pd.DepthStencilState.DepthEnable = FALSE;
    pd.DSVFormat = (g_dsvFormat != DXGI_FORMAT_UNKNOWN) ? g_dsvFormat : DXGI_FORMAT_UNKNOWN;
    pd.InputLayout = {ie, 3};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;
    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoTex)))) { Log("PSO_tex fail"); return false; }

    // Create descriptor heap for texture SRVs (max 64 textures)
    D3D12_DESCRIPTOR_HEAP_DESC hd = {};
    hd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    hd.NumDescriptors = 64;
    hd.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;
    if (FAILED(g_dev->CreateDescriptorHeap(&hd, IID_PPV_ARGS(&g_texSrvHeap))))
    { Log("Tex SRV heap fail"); return false; }
    g_texSrvSize = g_dev->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);

    Log("PSO_tex OK");
    return true;
}

// Upload texture from raw RGBA data, create SRV, cache by GL tex ID
static void UploadTextureEx(const void* pixels, int w, int h, int texId) {
    if (!g_ok || w <= 0 || h <= 0 || texId <= 0) return;
    auto it = g_texMap.find(texId);
    if (it != g_texMap.end()) return; // already uploaded

    UINT rowPitch = w * 4;
    UINT uploadRowPitch = (rowPitch + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1)
                        & ~(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
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
    td.Width = w; td.Height = (UINT)h; td.DepthOrArraySize = 1;
    td.MipLevels = 1; td.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    td.SampleDesc.Count = 1;
    ComPtr<ID3D12Resource> tex;
    if (FAILED(g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE, &td,
        D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&tex)))) return;

    D3D12_TEXTURE_COPY_LOCATION srcLoc = {}, dstLoc = {};
    srcLoc.pResource = uploadBuf.Get();
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    srcLoc.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    srcLoc.PlacedFootprint.Footprint.Width = w; srcLoc.PlacedFootprint.Footprint.Height = h;
    srcLoc.PlacedFootprint.Footprint.Depth = 1;
    srcLoc.PlacedFootprint.Footprint.RowPitch = uploadRowPitch;
    dstLoc.pResource = tex.Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    ComPtr<ID3D12CommandAllocator> ca;
    ComPtr<ID3D12GraphicsCommandList> cl;
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&ca));
    g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, ca.Get(), nullptr, IID_PPV_ARGS(&cl));
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
    // Allocate slot: reuse existing or get next
    UINT slot;
    auto slotIt = g_texSlotMap.find(texId);
    if (slotIt != g_texSlotMap.end()) {
        slot = slotIt->second;
    } else {
        if (g_texSlotNext >= 64) { Log("WARN: SRV heap full, id=%d", texId); return; }
        slot = g_texSlotNext++;
        g_texSlotMap[texId] = slot;
    }
    cpuHandle.ptr += (SIZE_T)slot * g_texSrvSize;
    g_dev->CreateShaderResourceView(tex.Get(), &srvDesc, cpuHandle);

    g_texMap[texId] = tex;
    Log("Upload texture #%d %dx%d slot=%u", texId, w, h, slot);
}

// === Create default-heap texture from upload buffer ===
static bool UploadTexture(const void* pixels, int w, int h) {
    if (w <= 0 || h <= 0) return false;
    UINT rowPitch = w * 4;

    // Upload buffer
    D3D12_HEAP_PROPERTIES hpUp = {}; hpUp.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rdUp = {};
    rdUp.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rdUp.Width = ((UINT64)rowPitch + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1)
               & ~(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    rdUp.Height = 1; rdUp.DepthOrArraySize = 1;
    rdUp.MipLevels = 1; rdUp.SampleDesc.Count = 1;
    rdUp.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    rdUp.Width *= h;
    ComPtr<ID3D12Resource> upBuf;
    if (FAILED(g_dev->CreateCommittedResource(&hpUp, D3D12_HEAP_FLAG_NONE,
        &rdUp, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&upBuf))))
    { return false; }

    // Copy with proper row pitch alignment
    UINT alignedPitch = (UINT)(rdUp.Width / h);
    BYTE* dst = nullptr;
    upBuf->Map(0, nullptr, (void**)&dst);
    for (int y = 0; y < h; y++)
        memcpy(dst + y * alignedPitch, (const BYTE*)pixels + y * rowPitch, rowPitch);
    upBuf->Unmap(0, nullptr);

    // Default heap texture
    D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
    D3D12_RESOURCE_DESC td = {};
    td.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    td.Width = (UINT64)w; td.Height = (UINT)h;
    td.DepthOrArraySize = 1; td.MipLevels = 1;
    td.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    td.SampleDesc.Count = 1;
    td.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;

    ComPtr<ID3D12Resource> texDef;
    if (FAILED(g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
        &td, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&texDef))))
    { return false; }

    // Create SRV
    D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc = {};
    srvDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
    srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
    srvDesc.Texture2D.MipLevels = 1;
    g_dev->CreateShaderResourceView(texDef.Get(), &srvDesc,
        g_srvHeap->GetCPUDescriptorHandleForHeapStart());

    // Upload via command list
    ComPtr<ID3D12CommandAllocator> upAlloc;
    ComPtr<ID3D12GraphicsCommandList> upCL;
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&upAlloc));
    g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, upAlloc.Get(), nullptr, IID_PPV_ARGS(&upCL));

    D3D12_TEXTURE_COPY_LOCATION dstLoc = {};
    dstLoc.pResource = texDef.Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    D3D12_TEXTURE_COPY_LOCATION srcLoc = {};
    srcLoc.pResource = upBuf.Get();
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    srcLoc.PlacedFootprint.Offset = 0;
    srcLoc.PlacedFootprint.Footprint.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    srcLoc.PlacedFootprint.Footprint.Width = (UINT)w;
    srcLoc.PlacedFootprint.Footprint.Height = (UINT)h;
    srcLoc.PlacedFootprint.Footprint.Depth = 1;
    srcLoc.PlacedFootprint.Footprint.RowPitch = alignedPitch;

    upCL->CopyTextureRegion(&dstLoc, 0, 0, 0, &srcLoc, nullptr);

    D3D12_RESOURCE_BARRIER rb = {};
    rb.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    rb.Transition.pResource = texDef.Get();
    rb.Transition.StateBefore = D3D12_RESOURCE_STATE_COPY_DEST;
    rb.Transition.StateAfter = D3D12_RESOURCE_STATE_PIXEL_SHADER_RESOURCE;
    rb.Transition.Subresource = 0;
    upCL->ResourceBarrier(1, &rb);

    upCL->Close();
    ID3D12CommandList* lists[] = { upCL.Get() };
    g_queue->ExecuteCommandLists(1, lists);
    WaitGPU();

    // Swap in new texture
    EnterCriticalSection(&g_texLock);
    g_texDefault = texDef;
    g_texW = w; g_texH = h;
    LeaveCriticalSection(&g_texLock);

    Log("Texture uploaded: %dx%d", w, h);
    return true;
}

// === Render thread ===

// Capture MC window content via GDI BitBlt → upload to D3D12 texture
// Returns true if capture succeeded and texture is ready for rendering
static bool CaptureMCFrame() {
    if (!g_hwndMC) return false;

    // Capture MC window content
    HDC hdcWin = GetDC(g_hwndMC);
    if (!hdcWin) return false;
    HDC hdcMem = CreateCompatibleDC(hdcWin);
    if (!hdcMem) { ReleaseDC(g_hwndMC, hdcWin); return false; }
    HBITMAP hbm = CreateCompatibleBitmap(hdcWin, (int)g_w, (int)g_h);
    if (!hbm) { DeleteDC(hdcMem); ReleaseDC(g_hwndMC, hdcWin); return false; }
    HBITMAP hbmOld = (HBITMAP)SelectObject(hdcMem, hbm);
    BitBlt(hdcMem, 0, 0, (int)g_w, (int)g_h, hdcWin, 0, 0, SRCCOPY);

    // Read pixel data into buffer
    BITMAPINFO bi = {};
    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bi.bmiHeader.biWidth = (LONG)g_w;
    bi.bmiHeader.biHeight = -(LONG)g_h; // top-down
    bi.bmiHeader.biPlanes = 1;
    bi.bmiHeader.biBitCount = 32;
    bi.bmiHeader.biCompression = BI_RGB;

    UINT rowSize = g_w * 4;
    UINT dataSize = rowSize * g_h;
    BYTE* pixels = new BYTE[dataSize];
    GetDIBits(hdcWin, hbm, 0, (UINT)g_h, pixels, &bi, DIB_RGB_COLORS);

    // GDI returns BGRA; swap to RGBA for D3D12
    for (UINT i = 0; i < dataSize; i += 4) {
        BYTE tmp = pixels[i];
        pixels[i] = pixels[i + 2];
        pixels[i + 2] = tmp;
    }

    // Cleanup GDI
    SelectObject(hdcMem, hbmOld);
    DeleteObject(hbm);
    DeleteDC(hdcMem);
    ReleaseDC(g_hwndMC, hdcWin);

    // Check if we need to recreate the capture texture
    bool needRecreate = (g_mcCaptureW != g_w || g_mcCaptureH != g_h || !g_texMCFrame);

    if (needRecreate) {
        g_texMCFrame.Reset();

        D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
        D3D12_RESOURCE_DESC td = {};
        td.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        td.Width = g_w; td.Height = g_h;
        td.DepthOrArraySize = 1; td.MipLevels = 1;
        td.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        td.SampleDesc.Count = 1; td.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
        if (FAILED(g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
            &td, D3D12_RESOURCE_STATE_COPY_DEST, nullptr, IID_PPV_ARGS(&g_texMCFrame))))
        { delete[] pixels; return false; }

        // Recreate SRV for the capture texture (slot 0 in g_srvHeap)
        D3D12_SHADER_RESOURCE_VIEW_DESC srvDesc = {};
        srvDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        srvDesc.ViewDimension = D3D12_SRV_DIMENSION_TEXTURE2D;
        srvDesc.Shader4ComponentMapping = D3D12_DEFAULT_SHADER_4_COMPONENT_MAPPING;
        srvDesc.Texture2D.MipLevels = 1;
        g_dev->CreateShaderResourceView(g_texMCFrame.Get(), &srvDesc,
            g_srvHeap->GetCPUDescriptorHandleForHeapStart());

        g_mcCaptureW = g_w; g_mcCaptureH = g_h;
        Log("MC capture texture created: %dx%d", g_w, g_h);
    }

    // Upload via separate command list
    ComPtr<ID3D12CommandAllocator> capAlloc;
    ComPtr<ID3D12GraphicsCommandList> capCL;
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&capAlloc));
    g_dev->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, capAlloc.Get(), nullptr, IID_PPV_ARGS(&capCL));

    // Upload buffer
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

    D3D12_TEXTURE_COPY_LOCATION dstLoc = {};
    dstLoc.pResource = g_texMCFrame.Get();
    dstLoc.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
    dstLoc.SubresourceIndex = 0;

    D3D12_TEXTURE_COPY_LOCATION srcLoc = {};
    srcLoc.pResource = upBuf.Get();
    srcLoc.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
    srcLoc.PlacedFootprint.Offset = 0;
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
    ID3D12CommandList* lists[] = { capCL.Get() };
    g_queue->ExecuteCommandLists(1, lists);
    WaitGPU();
    return true;
}
static DWORD WINAPI RenderLoop(LPVOID) {
    Log("Render thread started");

    // Fullscreen quad (2 triangles covering clip space [-1,1]^2)
    Vertex2D fsQuad[] = {{-1,-1,0,1},{3,-1,2,1},{-1,3,0,-1}};
    {
        D3D12_HEAP_PROPERTIES hp = {}; hp.Type = D3D12_HEAP_TYPE_UPLOAD;
        D3D12_RESOURCE_DESC rd = {};
        rd.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        rd.Width = sizeof(fsQuad); rd.Height = 1;
        rd.DepthOrArraySize = 1; rd.MipLevels = 1;
        rd.SampleDesc.Count = 1; rd.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        g_dev->CreateCommittedResource(&hp, D3D12_HEAP_FLAG_NONE,
            &rd, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&g_vbFSQuad));
        void* p; g_vbFSQuad->Map(0, nullptr, &p);
        memcpy(p, fsQuad, sizeof(fsQuad)); g_vbFSQuad->Unmap(0, nullptr);
    }

    LARGE_INTEGER freq, last;
    QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&last);

    while (g_run) {
        LARGE_INTEGER now;
        QueryPerformanceCounter(&now);
        double dt = (double)(now.QuadPart - last.QuadPart) / freq.QuadPart;
        if (dt < 0.016) { Sleep(1); continue; }
        last = now;

        RepositionOverlay();
        CaptureMCFrame();

        g_alloc->Reset();
        g_cl->Reset(g_alloc.Get(), nullptr);
        g_fi = g_swap->GetCurrentBackBufferIndex();

        // Transition RT
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

        float bg[4] = {0.0f, 0.0f, 0.0f, 1.0f};
        g_cl->ClearRenderTargetView(rtv, bg, 0, nullptr);
        if (hasDSV)
            g_cl->ClearDepthStencilView(dsvH, D3D12_CLEAR_FLAG_DEPTH, 1.0f, 0, 0, nullptr);

        D3D12_VIEWPORT vp = {0,0,(float)g_w,(float)g_h,0,1};
        D3D12_RECT     sc = {0,0,(LONG)g_w,(LONG)g_h};
        g_cl->RSSetViewports(1, &vp);
        g_cl->RSSetScissorRects(1, &sc);

        // --- LAYER 0: MC window capture as fullscreen textured quad ---
        if (g_texMCFrame && g_pso) {
            g_cl->SetGraphicsRootSignature(g_rs.Get());
            g_cl->SetPipelineState(g_pso.Get());
            g_cl->SetDescriptorHeaps(1, g_srvHeap.GetAddressOf());
            g_cl->SetGraphicsRootDescriptorTable(0,
                g_srvHeap->GetGPUDescriptorHandleForHeapStart());
            D3D12_VERTEX_BUFFER_VIEW fsVbv = {};
            fsVbv.BufferLocation = g_vbFSQuad->GetGPUVirtualAddress();
            fsVbv.StrideInBytes = sizeof(Vertex2D);
            fsVbv.SizeInBytes = sizeof(Vertex2D) * 3;
            g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
            g_cl->IASetVertexBuffers(0, 1, &fsVbv);
            g_cl->DrawInstanced(3, 1, 0, 0);
        }

        // --- LAYER 1: GL→D3D12 translated geometry (world-space via MVP) ---
        {
            EnterCriticalSection(&g_stateLock);
            auto chunks = g_drawChunks;
            UINT stateSnapshot = g_glStateBits;
            LeaveCriticalSection(&g_stateLock);

            if (!chunks.empty()) {
                for (auto& ch : chunks) {
                    UINT state = stateSnapshot;
                    int variantIdx = (int)((state << 1) | (ch.textured ? 1 : 0));
                    bool isLine = (ch.topo == D3D_PRIMITIVE_TOPOLOGY_LINELIST || ch.topo == D3D_PRIMITIVE_TOPOLOGY_LINESTRIP);

                    // Per-chunk texture lookup with fallback
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
                        bool useLine = (isLine);
                        if (useLine) {
                            if (!g_psoLineVariants[variantIdx]) BuildLinePSO(state, ch.textured);
                        } else {
                            if (!g_psoSolidVariants[variantIdx]) BuildSolidPSO(state, ch.textured);
                        }
                        ComPtr<ID3D12PipelineState>& pso = useLine ? g_psoLineVariants[variantIdx] : g_psoSolidVariants[variantIdx];
                        ComPtr<ID3D12RootSignature>& rs = useLine ? g_rsLineVariants[state & 0xF] : g_rsSolidVariants[state & 0xF];
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
        }

        // Reset translated geometry for next frame
        EnterCriticalSection(&g_stateLock);
        g_imVertCount = 0;
        g_imVBSize = 0;
        g_drawChunks.clear();
        LeaveCriticalSection(&g_stateLock);

        // Transition back
        rb.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
        rb.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
        g_cl->ResourceBarrier(1, &rb);
        g_cl->Close();

        ID3D12CommandList* lists[] = {g_cl.Get()};
        g_queue->ExecuteCommandLists(1, lists);

        HRESULT hr = g_swap->Present(1, 0);
        if (hr == DXGI_ERROR_DEVICE_REMOVED || hr == DXGI_ERROR_DEVICE_RESET) {
            HRESULT dr = g_dev->GetDeviceRemovedReason();
            Log("Device removed! HR=0x%08X reason=0x%08X", hr, dr);
            g_run = false;
            break;
        }

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

// === Init ===
// === Grow immediate-mode VB if needed (called from JNI, locked externally) ===
static bool EnsureIMVBCapacity(UINT requiredBytes) {
    if (requiredBytes <= g_imVBCap) return true;
    UINT newCap = requiredBytes + (requiredBytes >> 1); // 1.5x
    Log("VB grow: %uKB → %uKB", g_imVBCap / 1024, newCap / 1024);
    // Create new buffer
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
    g_imVB = newVB;
    g_imVBCap = newCap;
    g_imVbv.BufferLocation = g_imVB->GetGPUVirtualAddress();
    return true;
}

// === Probe supported depth format (D32_FLOAT → D24_S8 → D16) ===
static DXGI_FORMAT PickDepthFormat() {
    D3D12_FEATURE_DATA_FORMAT_SUPPORT fs = {};
    fs.Format = DXGI_FORMAT_D32_FLOAT;
    if (SUCCEEDED(g_dev->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fs, sizeof(fs)))
        && (fs.Support1 & D3D12_FORMAT_SUPPORT1_DEPTH_STENCIL))
    { Log("Depth format: D32_FLOAT"); return DXGI_FORMAT_D32_FLOAT; }

    fs.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
    if (SUCCEEDED(g_dev->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fs, sizeof(fs)))
        && (fs.Support1 & D3D12_FORMAT_SUPPORT1_DEPTH_STENCIL))
    { Log("Depth format: D24_UNORM_S8_UINT"); return DXGI_FORMAT_D24_UNORM_S8_UINT; }

    fs.Format = DXGI_FORMAT_D16_UNORM;
    if (SUCCEEDED(g_dev->CheckFeatureSupport(D3D12_FEATURE_FORMAT_SUPPORT, &fs, sizeof(fs)))
        && (fs.Support1 & D3D12_FORMAT_SUPPORT1_DEPTH_STENCIL))
    { Log("Depth format: D16_UNORM"); return DXGI_FORMAT_D16_UNORM; }

    Log("Depth: none");
    return DXGI_FORMAT_UNKNOWN;
}

static bool InitD3D12(HWND hwndMC) {
    Log("=== Init D3D12 overlay on MC window (HWND=0x%p) ===", hwndMC);
    g_hwndMC = hwndMC;
    if (!g_hwndMC) { Log("ERROR: null HWND"); return false; }

    // Get MC window client rect (PHYSICAL pixels) — NOT framebuffer size
    RECT rc;
    GetClientRect(g_hwndMC, &rc);
    g_w = (UINT)(rc.right - rc.left);
    g_h = (UINT)(rc.bottom - rc.top);
    if (g_w == 0 || g_h == 0) {
        Log("ERROR: client rect is zero %dx%d", g_w, g_h);
        return false;
    }
    Log("MC client rect: %dx%d (physical pixels)", g_w, g_h);

    // Create borderless overlay window
    g_hwndOverlay = CreateOverlayWindow(g_hwndMC);
    if (!g_hwndOverlay) return false;

    ComPtr<IDXGIFactory4> dxgi;
    if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&dxgi)))) return false;

    ComPtr<IDXGIAdapter1> adp;
    for (UINT i=0; dxgi->EnumAdapters1(i,&adp)!=DXGI_ERROR_NOT_FOUND; i++) {
        DXGI_ADAPTER_DESC1 d; adp->GetDesc1(&d);
        if (SUCCEEDED(D3D12CreateDevice(adp.Get(), D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_dev))))
        {
            // Store adapter name + feature level for F3 debug screen
            D3D12_FEATURE_DATA_FEATURE_LEVELS fl = { D3D_FEATURE_LEVEL_11_0 };
            g_dev->CheckFeatureSupport(D3D12_FEATURE_FEATURE_LEVELS, &fl, sizeof(fl));
            const char* flNames[] = {
                "Unknown",  "9.1", "9.2", "9.3", "10.0", "10.1",
                "11.0", "11.1", "12.0", "12.1", "12.2"
            };
            int idx = (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_12_2) ? 10 :
                     (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_12_1) ?  9 :
                     (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_12_0) ?  8 :
                     (fl.MaxSupportedFeatureLevel >= D3D_FEATURE_LEVEL_11_1) ?  7 : 6;
            WideCharToMultiByte(CP_UTF8, 0, d.Description, -1,
                g_d3d12Info, sizeof(g_d3d12Info), 0, 0);
            snprintf(g_d3d12Info + strlen(g_d3d12Info),
                sizeof(g_d3d12Info) - strlen(g_d3d12Info),
                " (D3D12 FL_%s)", flNames[idx]);
            Log("Device: %S (FL: %s)", d.Description, flNames[idx]);
            break;
        }
        adp.Reset();
    }
    if (!g_dev && FAILED(D3D12CreateDevice(0,D3D_FEATURE_LEVEL_11_0,IID_PPV_ARGS(&g_dev)))) return false;

    D3D12_COMMAND_QUEUE_DESC qd = {}; qd.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    if (FAILED(g_dev->CreateCommandQueue(&qd, IID_PPV_ARGS(&g_queue)))) return false;

    DXGI_SWAP_CHAIN_DESC1 sd = {};
    sd.BufferCount=2; sd.Width=g_w; sd.Height=g_h;
    sd.Format=DXGI_FORMAT_R8G8B8A8_UNORM;
    sd.BufferUsage=DXGI_USAGE_RENDER_TARGET_OUTPUT;
    sd.SwapEffect=DXGI_SWAP_EFFECT_FLIP_DISCARD; sd.SampleDesc.Count=1;
    ComPtr<IDXGISwapChain1> sc1;
    if (FAILED(dxgi->CreateSwapChainForHwnd(g_queue.Get(), g_hwndOverlay, &sd, 0, 0, &sc1)))
    { Log("SwapChain fail"); return false; }
    sc1.As(&g_swap); g_fi = g_swap->GetCurrentBackBufferIndex();

    // RTV heap
    D3D12_DESCRIPTOR_HEAP_DESC rd = {};
    rd.NumDescriptors=2; rd.Type=D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    if (FAILED(g_dev->CreateDescriptorHeap(&rd, IID_PPV_ARGS(&g_rtvHeap)))) return false;
    g_rtvSize = g_dev->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);

    // SRV heap
    rd = {};
    rd.NumDescriptors=1; rd.Type=D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
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
    if (FAILED(g_dev->CreateCommandList(0,D3D12_COMMAND_LIST_TYPE_DIRECT, g_alloc.Get(),0,IID_PPV_ARGS(&g_cl)))) return false;
    g_cl->Close();
    if (FAILED(g_dev->CreateFence(0,D3D12_FENCE_FLAG_NONE,IID_PPV_ARGS(&g_fence)))) return false;
    g_fenceVal=1; g_fenceEv=CreateEventW(0,0,0,0);

    // Probe depth format & create depth buffer
    g_dsvFormat = PickDepthFormat();
    if (g_dsvFormat != DXGI_FORMAT_UNKNOWN && g_w > 0 && g_h > 0) {
        D3D12_DESCRIPTOR_HEAP_DESC dd = {};
        dd.NumDescriptors = 1; dd.Type = D3D12_DESCRIPTOR_HEAP_TYPE_DSV;
        if (FAILED(g_dev->CreateDescriptorHeap(&dd, IID_PPV_ARGS(&g_dsvHeap))))
        { Log("DSV heap fail"); g_dsvFormat = DXGI_FORMAT_UNKNOWN; }
        else {
            D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
            D3D12_RESOURCE_DESC depthDesc = {};
            depthDesc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
            depthDesc.Width = g_w; depthDesc.Height = g_h; depthDesc.DepthOrArraySize = 1;
            depthDesc.MipLevels = 1; depthDesc.Format = g_dsvFormat;
            depthDesc.SampleDesc.Count = 1;
            depthDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL;
            D3D12_CLEAR_VALUE cv = {}; cv.Format = g_dsvFormat;
            cv.DepthStencil.Depth = 1.0f; cv.DepthStencil.Stencil = 0;
            if (FAILED(g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
                &depthDesc, D3D12_RESOURCE_STATE_DEPTH_WRITE, &cv, IID_PPV_ARGS(&g_depthBuf))))
            { Log("Depth buf fail"); g_dsvFormat = DXGI_FORMAT_UNKNOWN; }
            else {
                g_dev->CreateDepthStencilView(g_depthBuf.Get(), nullptr,
                    g_dsvHeap->GetCPUDescriptorHandleForHeapStart());
                Log("Depth buffer OK %dx%d", g_w, g_h);
            }
        }
    }

    // Constant buffer upload heap
    D3D12_HEAP_PROPERTIES hpCB = {}; hpCB.Type = D3D12_HEAP_TYPE_UPLOAD;
    D3D12_RESOURCE_DESC rdCB = {};
    rdCB.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    rdCB.Width = g_cbSize; rdCB.Height = 1; rdCB.DepthOrArraySize = 1;
    rdCB.MipLevels = 1; rdCB.SampleDesc.Count = 1;
    rdCB.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    if (SUCCEEDED(g_dev->CreateCommittedResource(&hpCB, D3D12_HEAP_FLAG_NONE,
        &rdCB, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&g_cbUpload)))) {
        g_cbUpload->Map(0, nullptr, (void**)&g_cbData);
        // Identity MVP — will be overwritten each frame from Java via nativeSetMvp()
        float identity[16] = {
            1,0,0,0,  0,1,0,0,  0,0,1,0,  0,0,0,1
        };
        memcpy(g_cbData, identity, sizeof(identity));
        Log("CB upload OK (identity MVP)");
    }

    if (!MkPSO()) return false;
    if (!MkPSOSolid()) { Log("WARN: Solid PSO failed, immediate-mode disabled"); }
    else {
        // Create immediate-mode vertex buffer (upload heap, 4MB)
        D3D12_HEAP_PROPERTIES hpIm = {}; hpIm.Type = D3D12_HEAP_TYPE_UPLOAD;
        D3D12_RESOURCE_DESC rdIm = {};
        rdIm.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
        rdIm.Width = g_imVBCap; rdIm.Height = 1; rdIm.DepthOrArraySize = 1;
        rdIm.MipLevels = 1; rdIm.SampleDesc.Count = 1;
        rdIm.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
        if (SUCCEEDED(g_dev->CreateCommittedResource(&hpIm, D3D12_HEAP_FLAG_NONE,
            &rdIm, D3D12_RESOURCE_STATE_GENERIC_READ, nullptr, IID_PPV_ARGS(&g_imVB)))) {
            g_imVbv.BufferLocation = g_imVB->GetGPUVirtualAddress();
            g_imVbv.StrideInBytes = sizeof(VertexPC);
            g_imVbv.SizeInBytes = sizeof(VertexPC);
        }
    }
    MkPSOTex(); // non-fatal if fails

    InitializeCriticalSection(&g_texLock);
    InitializeCriticalSection(&g_stateLock);
    g_ok = true; g_run = true;
    g_thread = CreateThread(0, 0, RenderLoop, 0, 0, 0);
    Log("=== D3D12 on MC window Ready ===");
    return true;
}

// === Cleanup ===
static void CleanupD3D12() {
    g_run = false;
    if (g_thread) { WaitForSingleObject(g_thread, 3000); CloseHandle(g_thread); g_thread=0; }
    if (g_ok) {
        WaitGPU();
        CloseHandle(g_fenceEv);
        EnterCriticalSection(&g_texLock);
        g_texDefault.Reset(); g_tex.Reset();
        LeaveCriticalSection(&g_texLock);
        DeleteCriticalSection(&g_texLock);
        DeleteCriticalSection(&g_stateLock);
        g_vbUpload.Reset(); g_pso.Reset(); g_rs.Reset();
        g_imVB.Reset();
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
        g_texMCFrame.Reset(); g_vbFSQuad.Reset();
        g_cl.Reset(); g_alloc.Reset(); g_swap.Reset(); g_queue.Reset(); g_fence.Reset(); g_dev.Reset();
        g_ok = false;
    }
    if (g_hwndOverlay) { DestroyWindow(g_hwndOverlay); g_hwndOverlay = nullptr; }
    g_hwndMC = nullptr;
}

// === JNI ===
extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit
    (JNIEnv*, jclass, jlong hwnd) {
    CreateDirectoryA("C:\\temp", 0);
    if (g_ok) return JNI_TRUE;
    return InitD3D12((HWND)hwnd) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv*, jclass) { CleanupD3D12(); }
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv*, jclass) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv*, jclass) {}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv*, jclass, jint w, jint h) {
    if (!g_ok||!g_swap) return;
    if (g_w == (UINT)w && g_h == (UINT)h) return; // no change
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

    // Recreate depth buffer at new size
    if (g_dsvFormat != DXGI_FORMAT_UNKNOWN) {
        D3D12_HEAP_PROPERTIES hpDef = {}; hpDef.Type = D3D12_HEAP_TYPE_DEFAULT;
        D3D12_RESOURCE_DESC depthDesc = {};
        depthDesc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        depthDesc.Width = g_w; depthDesc.Height = g_h; depthDesc.DepthOrArraySize = 1;
        depthDesc.MipLevels = 1; depthDesc.Format = g_dsvFormat;
        depthDesc.SampleDesc.Count = 1;
        depthDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL;
        D3D12_CLEAR_VALUE cv = {}; cv.Format = g_dsvFormat;
        cv.DepthStencil.Depth = 1.0f; cv.DepthStencil.Stencil = 0;
        g_dev->CreateCommittedResource(&hpDef, D3D12_HEAP_FLAG_NONE,
            &depthDesc, D3D12_RESOURCE_STATE_DEPTH_WRITE, &cv, IID_PPV_ARGS(&g_depthBuf));
        g_dev->CreateDepthStencilView(g_depthBuf.Get(), nullptr,
            g_dsvHeap->GetCPUDescriptorHandleForHeapStart());
        Log("Resized depth buffer: %dx%d", g_w, g_h);
    }
}

// Sync clear color from glClearColor hook
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetClearColor
    (JNIEnv*, jclass, jfloat r, jfloat g, jfloat b, jfloat a) {
    EnterCriticalSection(&g_stateLock);
    g_glClearColor[0] = r; g_glClearColor[1] = g;
    g_glClearColor[2] = b; g_glClearColor[3] = a;
    LeaveCriticalSection(&g_stateLock);
}

// Sync current color from glColor4f hook
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetGlColor
    (JNIEnv*, jclass, jfloat r, jfloat g, jfloat b, jfloat a) {
    EnterCriticalSection(&g_stateLock);
    g_glColor[0] = r; g_glColor[1] = g;
    g_glColor[2] = b; g_glColor[3] = a;
    LeaveCriticalSection(&g_stateLock);
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeIsInitialized(JNIEnv*, jclass) { return g_ok?JNI_TRUE:JNI_FALSE; }

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeShowDebugWindow(JNIEnv*, jclass, jboolean s) {
    if (g_hwndOverlay) ShowWindow(g_hwndOverlay, s ? SW_SHOWNOACTIVATE : SW_HIDE);
}

// Upload RGBA pixel data from Java
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadPixels
    (JNIEnv* env, jclass, jbyteArray pixels, jint w, jint h) {
    if (!g_ok || !pixels) return;
    jsize len = env->GetArrayLength(pixels);
    std::vector<jbyte> buf(len);
    env->GetByteArrayRegion(pixels, 0, len, buf.data());
    UploadTexture(buf.data(), (int)w, (int)h);
}

// Record vertices for GL→D3D12 draw call translation
// Input: float[] packed as [x, y, z, r, g, b, a] per vertex, count = vertex count
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordVertices
    (JNIEnv* env, jclass, jfloatArray verts, jint count) {
    if (!g_ok || !g_imVB || !verts || count <= 0) return;
    UINT byteCount = (UINT)count * sizeof(VertexPC);
    if (!EnsureIMVBCapacity(g_imVBSize + byteCount)) return; // grow or drop

    jsize len = env->GetArrayLength(verts);
    std::vector<jfloat> buf(len);
    env->GetFloatArrayRegion(verts, 0, len, buf.data());

    // Convert float[7] per vertex → VertexPC
    EnterCriticalSection(&g_stateLock);

    // Record draw chunk BEFORE uploading vertices (use byte offset)
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
        vtx[i].x = buf[off + 0];
        vtx[i].y = buf[off + 1];
        vtx[i].z = buf[off + 2];
        UINT r = (UINT)(buf[off + 3] * 255.0f) & 0xFF;
        UINT g = (UINT)(buf[off + 4] * 255.0f) & 0xFF;
        UINT b = (UINT)(buf[off + 5] * 255.0f) & 0xFF;
        UINT a = (UINT)(buf[off + 6] * 255.0f) & 0xFF;
        vtx[i].color = (a << 24) | (r << 16) | (g << 8) | b;
    }
    g_imVB->Unmap(0, nullptr);
    g_imVertCount += count;
    g_imVBSize += byteCount;
    g_imVbv.SizeInBytes = g_imVBSize;
    LeaveCriticalSection(&g_stateLock);
}

// Record vertices WITH UV (float[9] → VertexPT, 20 bytes)
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordVerticesUV
    (JNIEnv* env, jclass, jfloatArray verts, jint count) {
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
        vtx[i].x = buf[off + 0];
        vtx[i].y = buf[off + 1];
        vtx[i].z = buf[off + 2];
        UINT r = (UINT)(buf[off + 3] * 255.0f) & 0xFF;
        UINT g = (UINT)(buf[off + 4] * 255.0f) & 0xFF;
        UINT b = (UINT)(buf[off + 5] * 255.0f) & 0xFF;
        UINT a = (UINT)(buf[off + 6] * 255.0f) & 0xFF;
        vtx[i].color = (a << 24) | (r << 16) | (g << 8) | b;
        vtx[i].u = buf[off + 7];
        vtx[i].v = buf[off + 8];
    }
    g_imVB->Unmap(0, nullptr);
    g_imVertCount += count;
    g_imVBSize += byteCount;
    g_imVbv.SizeInBytes = g_imVBSize;
    LeaveCriticalSection(&g_stateLock);
}

// Set primitive topology for next draw chunk
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetPrimitiveTopology
    (JNIEnv*, jclass, jint topo) {
    EnterCriticalSection(&g_stateLock);
    // GL_XXX → D3D12_PRIMITIVE_TOPOLOGY_XXX
    static const D3D_PRIMITIVE_TOPOLOGY map[] = {
        D3D_PRIMITIVE_TOPOLOGY_UNDEFINED,       // 0
        D3D_PRIMITIVE_TOPOLOGY_POINTLIST,       // 1  GL_POINTS
        D3D_PRIMITIVE_TOPOLOGY_LINELIST,        // 2  GL_LINES
        D3D_PRIMITIVE_TOPOLOGY_LINESTRIP,       // 3  GL_LINE_STRIP
        D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST,    // 4  GL_TRIANGLES
        D3D_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP,   // 5  GL_TRIANGLE_STRIP
        D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST,    // 6  GL_TRIANGLE_FAN → trianglist
    };
    if (topo >= 0 && topo <= 6) g_pendingTopo = map[topo];
    LeaveCriticalSection(&g_stateLock);
}

// Set texture ID for next draw chunk (per-chunk texture binding)
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetDrawTexture
    (JNIEnv*, jclass, jint texId) {
    g_pendingTextureId = texId;
}

// Set active texture by GL texture ID
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetTexture
    (JNIEnv*, jclass, jint texId) {
    g_currentTexId = texId;
}

// Upload RGBA texture pixels and create D3D12 SRV, keyed by GL texture ID
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadTextureEx
    (JNIEnv* env, jclass, jbyteArray pixels, jint w, jint h, jint texId) {
    if (!g_ok || !pixels) return;
    jsize len = env->GetArrayLength(pixels);
    std::vector<jbyte> buf(len);
    env->GetByteArrayRegion(pixels, 0, len, buf.data());
    UploadTextureEx(buf.data(), (int)w, (int)h, (int)texId);
}

// Set GL state bits (blend/depth/cull/depthWrite) for PSO variant selection
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetGlState
    (JNIEnv*, jclass, jint enableBits, jint disableBits) {
    EnterCriticalSection(&g_stateLock);
    g_glStateBits |= (UINT)enableBits;
    g_glStateBits &= ~(UINT)disableBits;
    LeaveCriticalSection(&g_stateLock);
}

// Sync viewport from GL glViewport (tracked for future D3D12 viewport sync)
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetViewport
    (JNIEnv*, jclass, jint x, jint y, jint w, jint h) {
    // Note: GL viewport != D3D12 window size. Only track for logging.
    // g_w/g_h are set by nativeInit/nativeResize and used for window viewport.
}

// Sync blend func
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetBlendFunc
    (JNIEnv*, jclass, jint sfactor, jint dfactor) {
    // Map GL blend factors → D3D12
    auto map = [](int gl) -> D3D12_BLEND {
        switch (gl) {
        case 0:     return D3D12_BLEND_ZERO;          // GL_ZERO
        case 1:     return D3D12_BLEND_ONE;           // GL_ONE
        case 768:   return D3D12_BLEND_SRC_COLOR;     // GL_SRC_COLOR
        case 769:   return D3D12_BLEND_INV_SRC_COLOR; // GL_ONE_MINUS_SRC_COLOR
        case 770:   return D3D12_BLEND_SRC_ALPHA;     // GL_SRC_ALPHA
        case 771:   return D3D12_BLEND_INV_SRC_ALPHA; // GL_ONE_MINUS_SRC_ALPHA
        case 774:   return D3D12_BLEND_DEST_COLOR;    // GL_DST_COLOR
        case 775:   return D3D12_BLEND_INV_DEST_COLOR;// GL_ONE_MINUS_DST_COLOR
        default:    return D3D12_BLEND_ONE;
        }
    };
    g_glSrcBlend = map(sfactor);
    g_glDstBlend = map(dfactor);
}

// Sync depth mask
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetDepthMask
    (JNIEnv*, jclass, jboolean write) {
    EnterCriticalSection(&g_stateLock);
    if (write) g_glStateBits |= GLB_DEPTH_WRITE;
    else       g_glStateBits &= ~GLB_DEPTH_WRITE;
    LeaveCriticalSection(&g_stateLock);
}

// Upload MVP matrix (16 floats, column-major)
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetMvp
    (JNIEnv* env, jclass, jfloatArray matrix) {
    if (!g_cbData) return;
    jfloat* src = env->GetFloatArrayElements(matrix, nullptr);
    if (src) {
        memcpy(g_cbData, src, 64);
        env->ReleaseFloatArrayElements(matrix, src, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL Java_com_dx12_DX12LibClient_nativeGetWindowWidth
    (JNIEnv*, jclass) { return (jint)g_w; }

JNIEXPORT jint JNICALL Java_com_dx12_DX12LibClient_nativeGetWindowHeight
    (JNIEnv*, jclass) { return (jint)g_h; }

/** Return D3D12 adapter name + feature level string for F3 debug screen */
JNIEXPORT jstring JNICALL Java_com_dx12_DX12LibClient_nativeGetD3D12Info
    (JNIEnv* env, jclass) {
    return env->NewStringUTF(g_d3d12Info);
}

/** Check if D3D12 overlay is active and rendering */
JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeIsD3D12Active
    (JNIEnv*, jclass) {
    return g_ok ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
