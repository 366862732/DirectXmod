// d3d12bridge.cpp — MC D3D12 Renderer (Phase 2: render to MC window + MVP matrix)
//
// Architecture:
//   1. D3D12 renders directly INTO Minecraft's GLFW window (SwapChain on MC HWND)
//   2. GL→D3D12 translation: intercepted MC BufferBuilder → vertex data → JNI → D3D12 draw
//   3. Projection/modelView matrices synced from MC via JNI each frame
//   4. Geometry uses MC world-space coords, transformed by MVP in VS shader
//
//在给我晚上编译闹鬼我清算你!d3d12bridge.cpp！
//你C++端渲染再给老子抽风老子收拾你
#define WIN32_LEAN_AND_MEAN
#define _CRT_SECURE_NO_WARNINGS
#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <d3d12sdklayers.h>
#include <dxgi1_6.h>
#include <d3dcompiler.h>
#include <wrl.h>
#include <cstdio>
#include <cstdarg>
#include <cmath>
#include <float.h>
#include <vector>
#include <algorithm>
#include <unordered_map>
#include <mutex>
#include <atomic>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "d3dcompiler.lib")

using namespace Microsoft::WRL;

// 设备有效性工具函数，全局可用
inline bool IsDeviceValid()
{
    return g_dev != nullptr;
}
// 全局原子设备丢失标记，线程安全
std::atomic<bool> g_deviceLost = false;

static void Log(const char* fmt, ...) {
    char buf[1024];
    va_list a; va_start(a, fmt);
    _vsnprintf_s(buf, sizeof(buf), _TRUNCATE, fmt, a);
    va_end(a);
    FILE* f = fopen("C:\\temp\\gl4dx12_d3d12.log", "a");
    if (f) { fprintf(f, "%s\n", buf); fclose(f); }
    OutputDebugStringA(buf); OutputDebugStringA("\n");
}

// ===== 前向声明（供 RenderLoop 调用，必须放在文件顶部） =====
extern "C" {
    // 天空系统
    JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetSkyParameters__FFFFFF
    (JNIEnv* env, jclass clazz, jfloat r, jfloat g, jfloat b, jfloat a, jfloat sunAngle, jfloat moonAngle);

    JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderSky
    (JNIEnv* env, jclass clazz);

    // 半透明系统
    JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadTransparent
    (JNIEnv* env, jclass clazz, jfloatArray vertices, jint count, jint vertexSize, jfloat distance);

    JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderTransparent
    (JNIEnv* env, jclass clazz);
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
static bool g_globalDeviceReady = false;
enum InitStage {
    STAGE_EMPTY = 0,
    STAGE_BASE_DEVICE,        // 基础设备/交换链
    ST_CMD_RES,               // 命令队列分配器
    ST_UI_PSO_CBUF,           // UI管线+UI常量缓冲
    ST_3D_PSO_CBUF,           // 3D透视管线+世界常量缓冲
    ST_SKY_TEX,               // 天空盒纹理上传
    ST_SKY_VB,                // 天空顶点缓冲
    ST_ENTITY_STATIC_VB,      // 实体静态顶点
    ST_INSTANCE_BUFFER,       // 实例缓冲
    ST_PARTICLE_RES,          // 粒子资源
    ST_TEXTURE_POOL,          // 通用贴图池
    ST_ALPHA_PSO,             // 半透明管线
    ST_FULL_READY             // 全部初始化完成
};
InitStage g_initStage = STAGE_EMPTY;
bool g_renderInitDone = false;
static HANDLE   g_frameReadyEvent = nullptr;  // 帧数据就绪事件

// 强制GPU同步，分散负载（每个初始化阶段后调用）
static void WaitForGpu() {
    g_cl->Close();
    ID3D12CommandList* cmdLists[] = {g_cl.Get()};
    g_queue->ExecuteCommandLists(1, cmdLists);
    g_swap->Present(1, 0);
    UINT64 fv = g_fenceVal;
    g_queue->Signal(g_fence.Get(), fv);
    g_fenceVal++;
    if (g_fence->GetCompletedValue() < fv) {
        g_fence->SetEventOnCompletion(fv, g_fenceEv);
        WaitForSingleObject(g_fenceEv, INFINITE);
    }
}

// 分阶段资源创建：每阶段创建一类资源，分散GPU负载
static void ProcessNextInitStage() {
    switch (g_initStage) {
        case STAGE_EMPTY:         OutputDebugStringA("[INIT STAGE] STAGE_EMPTY - wait for device ready\n"); break;
        case STAGE_BASE_DEVICE:   OutputDebugStringA("[INIT STAGE] STAGE_BASE_DEVICE - base device/swapchain ready\n"); break;
        case ST_CMD_RES:          OutputDebugStringA("[INIT STAGE] ST_CMD_RES - command queue/allocator ready\n"); break;
        case ST_UI_PSO_CBUF:      OutputDebugStringA("[INIT STAGE] ST_UI_PSO_CBUF - UI PSO + CB ready\n"); break;
        case ST_3D_PSO_CBUF:      OutputDebugStringA("[INIT STAGE] ST_3D_PSO_CBUF - 3D PSO + world CB ready\n"); break;
        case ST_SKY_TEX:          OutputDebugStringA("[INIT STAGE] ST_SKY_TEX - sky texture uploaded\n"); break;
        case ST_SKY_VB:           OutputDebugStringA("[INIT STAGE] ST_SKY_VB - sky vertex buffer ready\n"); break;
        case ST_ENTITY_STATIC_VB: OutputDebugStringA("[INIT STAGE] ST_ENTITY_STATIC_VB - entity static VB ready\n"); break;
        case ST_INSTANCE_BUFFER:  OutputDebugStringA("[INIT STAGE] ST_INSTANCE_BUFFER - instance buffer ready\n"); break;
        case ST_PARTICLE_RES:     OutputDebugStringA("[INIT STAGE] ST_PARTICLE_RES - particle resources ready\n"); break;
        case ST_TEXTURE_POOL:     OutputDebugStringA("[INIT STAGE] ST_TEXTURE_POOL - texture pool ready\n"); break;
        case ST_ALPHA_PSO:        OutputDebugStringA("[INIT STAGE] ST_ALPHA_PSO - alpha blend PSO ready\n"); break;
        default: break;
    }
}
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

struct DrawChunk { UINT byteOffset; UINT vertexCount; D3D_PRIMITIVE_TOPOLOGY topo; bool textured; UINT vertexStride; bool blend; int textureId; int vertexType; };
static std::vector<DrawChunk> g_drawChunks;
static D3D_PRIMITIVE_TOPOLOGY g_pendingTopo = D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;
static int  g_pendingTextureId = 0;

// ==============================================
// 顶点类型定义（必须在所有结构体之前）
// ==============================================
enum VertexType {
    VERTEX_TYPE_UNKNOWN = 0,
    VERTEX_TYPE_WORLD   = 1,   // 3D世界物体（使用透视投影）
    VERTEX_TYPE_SCREEN  = 2    // 2D GUI元素（使用正交投影）
};

// ==============================================
// 粒子系统
// ==============================================
struct ParticleDrawCall {
    ComPtr<ID3D12Resource> uploadBuffer;
    UINT vertexCount;
    UINT vertexSize;
    D3D12_GPU_VIRTUAL_ADDRESS gpuAddress;
    VertexType type = VERTEX_TYPE_UNKNOWN;
};

static std::vector<ParticleDrawCall> g_particleDrawCalls;
static std::mutex g_particleMutex;
static bool g_particlesPending = false;

// ==============================================
// 天空系统
// ==============================================
static float g_skyColor[4] = {0.5f, 0.7f, 1.0f, 1.0f};
static float g_sunAngle = 0.0f;
static float g_moonAngle = 0.0f;
static std::mutex g_skyMutex;

// ==============================================
// 半透明渲染
// ==============================================
struct TransparentDrawCall {
    ComPtr<ID3D12Resource> uploadBuffer;
    UINT vertexCount;
    UINT vertexSize;
    D3D12_GPU_VIRTUAL_ADDRESS gpuAddress;
    float distance;
    long long textureId;
    VertexType type = VERTEX_TYPE_UNKNOWN;
};

static std::vector<TransparentDrawCall> g_transparentDrawCalls;
static std::mutex g_transparentMutex;

// 矩阵乘法工具函数
static void MatrixMultiply(float* out, const float* a, const float* b) {
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            out[i * 4 + j] = 0;
            for (int k = 0; k < 4; k++) {
                out[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
            }
        }
    }
}

static UINT g_glStateBits = 0;
#define GLB_BLEND        1
#define GLB_DEPTH        2
#define GLB_CULL         4
#define GLB_DEPTH_WRITE  8

// 顶点坐标空间类型（从 Java 端传入）
static int g_currentCoordType = 0; // 0=WORLD, 1=SCREEN, 2=NDC

static ComPtr<ID3D12Resource>       g_depthBuf;
static ComPtr<ID3D12DescriptorHeap> g_dsvHeap;
static DXGI_FORMAT g_dsvFormat = DXGI_FORMAT_UNKNOWN;

struct MvpCB { float mvp[16]; };
static ComPtr<ID3D12Resource> g_cbUpload;
static BYTE* g_cbData = nullptr;
static const UINT g_cbSize = 256;

// 三种坐标空间各自独立存储矩阵（避免互相覆盖）
static float g_mvpWorld[16]  = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
static float g_mvpScreen[16] = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
static float g_mvpNDC[16]    = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};

static D3D12_BLEND g_glSrcBlend = D3D12_BLEND_SRC_ALPHA;
static D3D12_BLEND g_glDstBlend = D3D12_BLEND_INV_SRC_ALPHA;

static ComPtr<ID3D12PipelineState> g_psoSolidVariants[32];
static ComPtr<ID3D12PipelineState> g_psoAlphaBlend;  // 半透明/粒子/天空专用
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
    // 顶点已在 CPU 端预转换到 NDC，直接输出
    o.p = float4(i.p, 1);
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
static std::mutex g_texMutex;
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
    if (data) { void* m=nullptr; HRESULT hr = dst->Map(0,nullptr,&m); if (FAILED(hr) || m == nullptr) { Log("[FATAL] MkUpload Map failed, hr=0x%08X\n", hr); return false; } D3D12_RESOURCE_DESC bufDesc = dst->GetDesc(); if ((UINT64)sz > bufDesc.Width) { Log("[FATAL] memory write out of buffer range"); dst->Unmap(0,nullptr); return false; } memcpy(m,data,sz); dst->Unmap(0,nullptr); }
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
    // 顶点已在 CPU 端预转换到 NDC，使用单位矩阵或直接输出
    o.p = float4(i.p, 1);
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

// 半透明渲染专用 PSO（始终启用 Alpha Blending）
static bool BuildAlphaBlendPSO() {
    if (g_psoAlphaBlend) return true;

    ComPtr<ID3DBlob> vs, ps, err;
    if (FAILED(D3DCompile(kVS_Solid, strlen(kVS_Solid), 0,0,0,"VSMain","vs_5_0",0,0,&vs,&err))) return false;
    if (FAILED(D3DCompile(kPS_Solid, strlen(kPS_Solid), 0,0,0,"PSMain","ps_5_0",0,0,&ps,&err))) return false;

    // 使用第一个 solid root signature（带 CBV）
    if (!g_rsSolidVariants[0]) {
        D3D12_ROOT_PARAMETER rpCB = {};
        rpCB.ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
        rpCB.Descriptor.ShaderRegister = 0;
        rpCB.ShaderVisibility = D3D12_SHADER_VISIBILITY_VERTEX;
        D3D12_ROOT_SIGNATURE_DESC rsd = {};
        rsd.NumParameters = 1; rsd.pParameters = &rpCB;
        rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
        ComPtr<ID3DBlob> rb;
        if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
        if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(),
            IID_PPV_ARGS(&g_rsSolidVariants[0])))) return false;
    }

    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS",0,DXGI_FORMAT_R32G32B32_FLOAT,0,0,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
        {"COL",0,DXGI_FORMAT_R8G8B8A8_UNORM,0,12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA,0},
    };
    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = g_rsSolidVariants[0].Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    // 启用 Alpha Blending（标准 over 运算：src*alpha + dst*(1-alpha)）
    pd.BlendState.RenderTarget[0].BlendEnable = TRUE;
    pd.BlendState.RenderTarget[0].SrcBlend = D3D12_BLEND_SRC_ALPHA;
    pd.BlendState.RenderTarget[0].DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
    pd.BlendState.RenderTarget[0].BlendOp = D3D12_BLEND_OP_ADD;
    pd.BlendState.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
    pd.BlendState.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_ZERO;
    pd.BlendState.RenderTarget[0].BlendOpAlpha = D3D12_BLEND_OP_ADD;
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.SampleMask = UINT_MAX;
    pd.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    pd.RasterizerState.CullMode = D3D12_CULL_MODE_NONE;
    pd.RasterizerState.DepthClipEnable = TRUE;
    pd.DepthStencilState.DepthEnable = FALSE;
    pd.DepthStencilState.DepthWriteMask = D3D12_DEPTH_WRITE_MASK_ZERO;
    pd.DepthStencilState.DepthFunc = D3D12_COMPARISON_FUNC_LESS_EQUAL;
    pd.DSVFormat = (g_dsvFormat != DXGI_FORMAT_UNKNOWN) ? g_dsvFormat : DXGI_FORMAT_UNKNOWN;
    pd.InputLayout = {ie, 2};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;
    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoAlphaBlend)))) return false;
    Log("BuildAlphaBlendPSO: created alpha-blend PSO");
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
    HRESULT hr = uploadBuf->Map(0, nullptr, &dst);
    if (FAILED(hr) || dst == nullptr) {
        Log("[FATAL] UploadTextureEx Map failed, hr=0x%08X\n", hr);
        return;
    }
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
    {
        std::lock_guard<std::mutex> lock(g_texMutex);
        auto slotIt = g_texSlotMap.find(texId);
        if (slotIt != g_texSlotMap.end()) {
            slot = slotIt->second;
        } else {
            if (g_texSlotNext >= 64) return;
            slot = g_texSlotNext++;
            g_texSlotMap[texId] = slot;
        }
    }
    cpuHandle.ptr += (SIZE_T)slot * g_texSrvSize;
    g_dev->CreateShaderResourceView(tex.Get(), &srvDesc, cpuHandle);
    {
        std::lock_guard<std::mutex> lock(g_texMutex);
        g_texMap[texId] = tex;
    }
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
    HRESULT hr = upBuf->Map(0, nullptr, (void**)&dst);
    if (FAILED(hr) || dst == nullptr) {
        Log("[FATAL] CaptureMCFrame Map failed, hr=0x%08X\n", hr);
        delete[] pixels;
        return false;
    }
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
        HRESULT hr = g_imVB->Map(0, nullptr, &oldDst);
        if (FAILED(hr) || oldDst == nullptr) {
            Log("[FATAL] EnsureIMVBCapacity old buffer Map failed, hr=0x%08X\n", hr);
            return false;
        }
        hr = newVB->Map(0, nullptr, &newDst);
        if (FAILED(hr) || newDst == nullptr) {
            g_imVB->Unmap(0, nullptr);
            Log("[FATAL] EnsureIMVBCapacity new buffer Map failed, hr=0x%08X\n", hr);
            return false;
        }
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
    OutputDebugStringA("[FATAL] TEST: RenderLoop entered (before any code)\n");
    MessageBoxA(NULL, "RenderLoop ENTERED", "DEBUG", MB_OK);
    Log("=== RenderLoop ENTERED (FORCED) ===");
    Log("=== RenderLoop DIAGNOSTIC VERSION 2 ===");
    Log("Render thread started");
    int loopCount = 0;
    Vertex2D fsQuad[] = {{-1,-1,0,1},{3,-1,2,1},{-1,3,0,-1}};
    MkUpload(g_vbFSQuad, fsQuad, sizeof(fsQuad));

    while (true) {
        if (g_deviceLost.load() || !IsDeviceValid()) {
            OutputDebugStringA("[FATAL] Device lost, terminating render loop\n");
            break;
        }
        if (!g_globalDeviceReady) { Log("[ERROR] Device not ready, skip render"); Sleep(16); continue; }
        Log("=== RenderLoop LOOP ITERATION ===");
        if (!g_dev || !g_queue || !g_rtvHeap || !g_srvHeap || !g_alloc || !g_cl) {
            Sleep(16);
            continue;
        }
        if (!g_run) {
            // g_run 被设为 false（可能是 CleanupD3D12 调用），退出
            Log("Render thread: g_run=false, exiting");
            break;
        }

        // ===== 12阶段异步初始化状态机：每阶段独占1帧，创建资源+强制GPU同步，根除TDR =====
        if (g_initStage < ST_FULL_READY) {
            char stageLog[256];
            sprintf_s(stageLog, "[INIT STATE MACHINE] 当前阶段:%d", (int)g_initStage);
            OutputDebugStringA(stageLog);

            HRESULT hr = g_alloc->Reset();
            if (FAILED(hr)) {
                Log("[ERROR] InitStateMachine: allocator Reset failed at stage %d", (int)g_initStage);
                Sleep(16);
                continue;
            }
            hr = g_cl->Reset(g_alloc.Get(), nullptr);
            if (FAILED(hr)) {
                Log("[ERROR] InitStateMachine: command list Reset failed at stage %d", (int)g_initStage);
                Sleep(16);
                continue;
            }

            // 清屏准备
            auto bb = g_rt[g_swap->GetCurrentBackBufferIndex()];
            D3D12_RESOURCE_BARRIER rb = {};
            rb.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
            rb.Transition.pResource = bb.Get();
            rb.Transition.StateBefore = D3D12_RESOURCE_STATE_PRESENT;
            rb.Transition.StateAfter = D3D12_RESOURCE_STATE_RENDER_TARGET;
            rb.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
            g_cl->ResourceBarrier(1, &rb);
            D3D12_CPU_DESCRIPTOR_HANDLE rh = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
            rh.ptr += g_swap->GetCurrentBackBufferIndex() * g_rtvSize;
            float clearColor[4] = {0.0f, 0.0f, 0.0f, 1.0f};
            g_cl->OMSetRenderTargets(1, &rh, FALSE, nullptr);
            g_cl->ClearRenderTargetView(rh, clearColor, 0, nullptr);
            rb.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
            rb.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
            g_cl->ResourceBarrier(1, &rb);

            // 分阶段资源创建
            ProcessNextInitStage();

            // 强制GPU完全同步，分散负载
            WaitForGpu();

            g_initStage = (InitStage)((int)g_initStage + 1);
            if (g_initStage >= ST_FULL_READY) {
                g_renderInitDone = true;
                OutputDebugStringA("[FATAL] 12-stage init complete, entering normal rendering\n");
            }
            continue;
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

                // ===== 自动修正诊断：仅告警，不篡改Java传入坐标类型 =====
                if (g_currentCoordType == 0 && vertexCount > 0) {
                    int screenCount = 0;
                    float zMin = 1e10f, zMax = -1e10f;
                    for (int i = 0; i < vertexCount; i++) {
                        float vx = vertices[i * 3], vy = vertices[i * 3 + 1], vz = vertices[i * 3 + 2];
                        if (vx >= 0 && vx <= 2000 && vy >= 0 && vy <= 2000) {
                            screenCount++;
                        }
                        if (vz < zMin) zMin = vz;
                        if (vz > zMax) zMax = vz;
                    }
                    float zDelta = zMax - zMin;
                    bool allXYInScreen = (screenCount > vertexCount / 2);
                    static int suspectUICount = 0;
                    if (allXYInScreen && fabs(zDelta) < 0.001f) {
                        suspectUICount++;
                        char warnBuf[512];
                        sprintf_s(warnBuf, "[AUTO-CORRECT WARN#%d] XY screen-plane + Z=0, Java coordType=%d, NOT auto-switching pipeline",
                            suspectUICount, g_currentCoordType);
                        OutputDebugStringA(warnBuf);
                    } else {
                        OutputDebugStringA("[AUTO-CORRECT INFO] Vertex has depth, using Java-passed coordinate type");
                    }
                    // 管线切换仅依赖Java传入值，不受顶点自动检测干扰
                }

                // ===== 根据顶点类型选择投影矩阵 =====
                switch (g_currentCoordType) {
                    case 0: // COORD_WORLD — 使用 Java 端传入的 MVP 矩阵
                        Log("Using perspective projection for 3D vertices (WORLD coords)");
                        if (g_cbData) {
                            memcpy(g_cbData, g_mvpWorld, sizeof(g_mvpWorld));
                        }
                        // 检查 MVP 变换后的 NDC 范围
                        if (g_cbData && vertexCount > 0) {
                            float* m = (float*)g_cbData;
                            int outOfRange = 0;
                            for (int i = 0; i < vertexCount; i++) {
                                float vx = vertices[i * 3], vy = vertices[i * 3 + 1], vz = vertices[i * 3 + 2];
                                float clipW = m[12]*vx + m[13]*vy + m[14]*vz + m[15];
                                float ndcX = (clipW != 0) ? (m[0]*vx + m[1]*vy + m[2]*vz + m[3]) / clipW : 0;
                                float ndcY = (clipW != 0) ? (m[4]*vx + m[5]*vy + m[6]*vz + m[7]) / clipW : 0;
                                if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) {
                                    outOfRange++;
                                }
                            }
                            if (outOfRange > 0) {
                                Log("  WARNING: %d/%d vertices out of NDC range after WORLD transform",
                                    outOfRange, vertexCount);
                            }
                        }
                        break;

                    case 1: // COORD_SCREEN — 使用窗口尺寸计算正交投影
                        Log("Using orthographic projection for GUI vertices (SCREEN coords), window=%dx%d", g_w, g_h);
                        {
                            float w2 = (float)g_w / 2.0f;
                            float h2 = (float)g_h / 2.0f;
                            float ortho[16] = {
                                1.0f/w2, 0, 0, -1,
                                0, -1.0f/h2, 0, 1,
                                0, 0, 1, 0,
                                0, 0, 0, 1
                            };
                            memcpy(g_mvpScreen, ortho, sizeof(ortho));
                            if (g_cbData) {
                                memcpy(g_cbData, ortho, sizeof(ortho));
                            }
                        }
                        break;

                    case 2: // COORD_NDC — 使用单位矩阵（不变换）
                        Log("Using identity matrix for NDC vertices (already in clip space)");
                        if (g_cbData) {
                            memcpy(g_cbData, g_mvpNDC, sizeof(g_mvpNDC));
                        }
                        break;

                    default:
                        Log("WARNING: Unknown coordType=%d, using WORLD", g_currentCoordType);
                        if (g_cbData) {
                            memcpy(g_cbData, g_mvpWorld, sizeof(g_mvpWorld));
                        }
                        break;
                }
                // ===== 矩阵选择结束 =====

                // ===== 将四边形转换为三角形（GUI顶点） =====
                if (g_currentCoordType == 1 && vertexCount % 4 == 0 && vertexCount > 0) {
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

                // ===== CPU端预转换顶点到NDC =====
                if (g_cbData && vertexCount > 0) {
                    float* mvp = (float*)g_cbData;
                    for (int i = 0; i < vertexCount; i++) {
                        float x = vertices[i * 3], y = vertices[i * 3 + 1], z = vertices[i * 3 + 2];
                        float clipX = mvp[0]*x + mvp[1]*y + mvp[2]*z + mvp[3];
                        float clipY = mvp[4]*x + mvp[5]*y + mvp[6]*z + mvp[7];
                        float clipZ = mvp[8]*x + mvp[9]*y + mvp[10]*z + mvp[11];
                        float clipW = mvp[12]*x + mvp[13]*y + mvp[14]*z + mvp[15];
                        if (clipW != 0) {
                            vertices[i * 3]     = clipX / clipW;
                            vertices[i * 3 + 1] = clipY / clipW;
                            vertices[i * 3 + 2] = clipZ / clipW;
                        }
                    }
                    Log("Pre-transformed %d vertices to NDC on CPU", vertexCount);
                }
                // ===== NDC转换结束 =====

                Log("RenderLoop: rendering %d vertices from new storage", vertexCount);

                // 1. 更新顶点缓冲区
                UINT byteCount = vertexCount * sizeof(VertexPC);
                if (EnsureIMVBCapacity(byteCount)) {
                    void* dst = nullptr;
                    HRESULT hr = g_imVB->Map(0, nullptr, &dst);
                    if (FAILED(hr) || dst == nullptr) {
                        Log("[FATAL] RenderLoop vertex Map failed, hr=0x%08X\n", hr);
                        g_imVB->Unmap(0, nullptr);
                        continue;
                    }
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
                    ch.vertexType = g_currentCoordType;
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

        HRESULT hr = g_alloc->Reset();
        if (FAILED(hr)) {
            Log("[ERROR] 命令分配器 Reset 失败");
            continue;
        }
        hr = g_cl->Reset(g_alloc.Get(), nullptr);
        if (FAILED(hr)) {
            Log("[ERROR] 命令列表 Reset 失败");
            continue;
        }
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
            // ===== Layer 0.5: 渲染天空 =====
            Java_com_dx12_DX12LibClient_nativeRenderSky(nullptr, nullptr);
            Log("  Sky rendered");

            EnterCriticalSection(&g_stateLock);
            auto chunks = g_drawChunks;
            UINT stateSnapshot = g_glStateBits;
            LeaveCriticalSection(&g_stateLock);

            // 临时禁用深度测试 - D3D12 不支持 OMSetDepthStencilState，深度测试由 PSO 控制
            // 如需禁用，可通过 Java 端调用 nativeSetGlState(0, GLB_DEPTH)

            for (auto& ch : chunks) {
                // ===== 每个 DrawChunk 独立设置常量缓冲区矩阵 =====
                if (ch.vertexType == 0) { // COORD_WORLD
                    if (g_cbData) {
                        memcpy(g_cbData, g_mvpWorld, sizeof(g_mvpWorld));
                    }
                } else if (ch.vertexType == 1) { // COORD_SCREEN
                    float w = (float)g_w;
                    float h = (float)g_h;
                    float orthoMatrix[16] = {
                        2.0f / w, 0.0f,       0.0f, -1.0f,
                        0.0f,     -2.0f / h,  0.0f,  1.0f,
                        0.0f,      0.0f,       1.0f,  0.0f,
                        0.0f,      0.0f,       0.0f,  1.0f
                    };
                    if (g_cbData) {
                        memcpy(g_cbData, orthoMatrix, sizeof(orthoMatrix));
                    }
                }
                // 立即刷新常量缓冲区到 GPU（upload heap 写入可见性）
                if (g_cbData) {
                    void* cbMapped = nullptr;
                    D3D12_RANGE writeRange = {};
                    writeRange.Begin = 0;
                    writeRange.End = g_cbSize;
                    HRESULT hr = g_cbUpload->Map(0, &writeRange, &cbMapped);
                    if (FAILED(hr) || !cbMapped) {
                        Log("[FATAL] DrawChunk CBV Map failed, hr=0x%08X\n", hr);
                    } else {
                        D3D12_RESOURCE_DESC cbvDesc = g_cbUpload->GetDesc();
                        if ((UINT64)g_cbSize > cbvDesc.Width) {
                            Log("[FATAL] memory write out of buffer range");
                            g_cbUpload->Unmap(0, &writeRange);
                        } else {
                            memcpy(cbMapped, g_cbData, g_cbSize);
                        }
                    }
                    g_cbUpload->Unmap(0, &writeRange);
                }
                Log("DrawChunk: vertexType=%d (%s), vertices=%d",
                    ch.vertexType,
                    ch.vertexType == 0 ? "WORLD" : (ch.vertexType == 1 ? "SCREEN" : "NDC"),
                    ch.vertexCount);

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
                Log("=== DRAW CALL: vertexCount=%d, stride=%d, bytes=%d, buffer=0x%llX, topo=%d",
                    ch.vertexCount, ch.vertexStride, chVbv.SizeInBytes, chVbv.BufferLocation, ch.topo);
                g_cl->DrawInstanced(ch.vertexCount, 1, 0, 0);
                Log("  DrawInstanced executed");
            }
        }

        // ===== 渲染半透明物体 =====
        Java_com_dx12_DX12LibClient_nativeRenderTransparent(nullptr, nullptr);
        Log("  Transparent objects rendered");

        EnterCriticalSection(&g_stateLock);
        g_imVertCount = 0;
        g_imVBSize = 0;
        g_drawChunks.clear();
        LeaveCriticalSection(&g_stateLock);

        // ===== 渲染粒子系统 =====
        {
            std::lock_guard<std::mutex> lock(g_particleMutex);
            if (!g_particleDrawCalls.empty()) {
                // 设置半透明 PSO
                if (BuildAlphaBlendPSO() && g_rsSolidVariants[0]) {
                    g_cl->SetGraphicsRootSignature(g_rsSolidVariants[0].Get());
                    g_cl->SetPipelineState(g_psoAlphaBlend.Get());
                    g_cl->SetGraphicsRootConstantBufferView(0, g_cbUpload->GetGPUVirtualAddress());
                }
                Log("  Rendering %zu particle draw calls", g_particleDrawCalls.size());
                for (size_t i = 0; i < g_particleDrawCalls.size(); i++) {
                    auto& dc = g_particleDrawCalls[i];
                    D3D12_VERTEX_BUFFER_VIEW vbView = {};
                    vbView.BufferLocation = dc.gpuAddress;
                    vbView.StrideInBytes = dc.vertexSize;
                    vbView.SizeInBytes = dc.vertexCount * dc.vertexSize;
                    g_cl->IASetVertexBuffers(0, 1, &vbView);
                    g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
                    g_cl->DrawInstanced(dc.vertexCount, 1, 0, 0);
                    Log("  Particle DC[%zu]: %d vertices, type=%s",
                        i, dc.vertexCount,
                        dc.type == VERTEX_TYPE_WORLD ? "WORLD" : "SCREEN");
                }
                g_particleDrawCalls.clear();
                g_particlesPending = false;
                Log("  All particles rendered, draw calls cleared");
            }
        }

        rb.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
        rb.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
        g_cl->ResourceBarrier(1, &rb);
        g_cl->Close();

        ID3D12CommandList* lists[] = {g_cl.Get()};
        g_queue->ExecuteCommandLists(1, lists);

        OutputDebugStringA("[FATAL] TEST: About to call Present(1, 0)\n");
        HRESULT presentHR = g_swap->Present(1, 0);
        if (FAILED(presentHR)) {
            HRESULT devErr = g_dev->GetDeviceRemovedReason();
            char errBuf[512];
            sprintf_s(errBuf, "[FATAL] Present failed, device removed reason: 0x%08X", devErr);
            OutputDebugStringA(errBuf);
            switch (devErr) {
                case DXGI_ERROR_DEVICE_REMOVED:
                case DXGI_ERROR_DEVICE_HUNG:
                    OutputDebugStringA("[FATAL] GPU timeout/device destroyed, marking device lost\n");
                    g_deviceLost.store(true);
                    g_run = false;
                    break;
                case DXGI_ERROR_DEVICE_RESET:    OutputDebugStringA("[FATAL] Reason: DXGI_ERROR_DEVICE_RESET\n"); break;
                case DXGI_ERROR_ACCESS_DENIED:   OutputDebugStringA("[FATAL] Reason: DXGI_ERROR_ACCESS_DENIED\n"); break;
                default: OutputDebugStringA("[FATAL] Reason: unknown error code\n"); break;
            }
            if (!g_run) break;
        } else {
            OutputDebugStringA("[FATAL] TEST: Present succeeded\n");
        }

        UINT64 fv = g_fenceVal;
        g_queue->Signal(g_fence.Get(), fv); g_fenceVal++;
        if (g_fence->GetCompletedValue() < fv) {
            g_fence->SetEventOnCompletion(fv, g_fenceEv);
            WaitForSingleObject(g_fenceEv, INFINITE);
        }

        // 确保GPU完成当前帧绘制后重置命令分配器和命令列表（防止CBV数据竞争）
        if (FAILED(g_alloc->Reset())) Log("WARN: end-of-frame allocator Reset failed");
        if (FAILED(g_cl->Reset(g_alloc.Get(), nullptr))) Log("WARN: end-of-frame command list Reset failed");
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

    // 启用D3D12调试层（必须在创建设备之前调用）
#ifdef _DEBUG
    ID3D12Debug* debugController;
    if (SUCCEEDED(D3D12GetDebugInterface(IID_PPV_ARGS(&debugController)))) {
        debugController->EnableDebugLayer();
        debugController->Release();
        OutputDebugStringA("[FATAL] D3D调试层已开启(仅Debug)\n");
    }
#endif

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
        HRESULT hr = g_cbUpload->Map(0, nullptr, (void**)&g_cbData);
        if (FAILED(hr) || !g_cbData) { Log("[FATAL] Init CBV Map failed, hr=0x%08X\n", hr); return false; }
        float identity[16] = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
        D3D12_RESOURCE_DESC cbvDesc = g_cbUpload->GetDesc();
        if (sizeof(identity) > cbvDesc.Width) { Log("[FATAL] memory write out of buffer range"); g_cbUpload->Unmap(0, nullptr); return false; }
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
    g_globalDeviceReady = true;
    Log("=== D3D12 on MC window Ready ===");
    return true;
}


static void SafeCleanD3D() {
    OutputDebugStringA("[CLEANUP] 开始安全销毁全部D3D资源\n");
    // 1. 标记渲染线程停止
    g_run = false;
    g_deviceLost.store(true);
    // 2. 等待渲染线程退出
    if (g_thread) {
        WaitForSingleObject(g_thread, 3000);
        CloseHandle(g_thread);
        g_thread = nullptr;
    }
    // 3. 安全销毁 D3D 资源
    if (g_ok) {
        WaitGPU();
        CloseHandle(g_fenceEv);
        DeleteCriticalSection(&g_texLock);
        DeleteCriticalSection(&g_stateLock);
        for (auto& p : g_psoSolidVariants) p.Reset();
        for (auto& p : g_rsSolidVariants) p.Reset();
        for (auto& p : g_psoLineVariants) p.Reset();
        for (auto& p : g_rsLineVariants) p.Reset();
        g_psoAlphaBlend.Reset();
        g_psoTex.Reset(); g_rsTex.Reset(); g_texSrvHeap.Reset();
        g_texMap.clear(); g_texSlotMap.clear(); g_texSlotNext = 0;
        for (auto& r : g_rt) r.Reset();
        g_rtvHeap.Reset(); g_srvHeap.Reset();
        g_depthBuf.Reset(); g_dsvHeap.Reset();
        if (g_cbData) { g_cbUpload->Unmap(0, nullptr); g_cbData = nullptr; }
        g_cbUpload.Reset();
        g_texMCFrame.Reset(); g_vbFSQuad.Reset(); g_imVB.Reset();
        // ComPtr 自动释放全部 D3D 资源
        g_cl.Reset(); g_alloc.Reset(); g_swap.Reset(); g_queue.Reset(); g_fence.Reset(); g_dev.Reset();
        g_ok = false;
    }
    // 4. 关闭同步事件、窗口附属资源
    if (g_frameReadyEvent) { CloseHandle(g_frameReadyEvent); g_frameReadyEvent = nullptr; }
    if (g_frameDoneEvent)   { CloseHandle(g_frameDoneEvent);   g_frameDoneEvent = nullptr; }
    if (g_hwndOverlay && g_hwndOverlay != g_hwndMC) { DestroyWindow(g_hwndOverlay); }
    g_hwndOverlay = nullptr;
    g_hwndMC = nullptr;
    OutputDebugStringA("[CLEANUP] D3D资源销毁完成\n");
}

static void CleanupD3D12() {
    SafeCleanD3D();
}

// === JNI ===
extern "C" {
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeCleanup(JNIEnv* env, jclass cls) {
    // 调用 CleanupD3D12 清理资源
    CleanupD3D12();
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit
    (JNIEnv*, jclass, jlong hwnd) {
    MessageBoxA(NULL, "nativeInit已执行，DLL加载正常", "DEBUG_LOAD_CHECK", MB_OK);
    OutputDebugStringA("[FATAL] TEST nativeInit 入口日志\n");
    OutputDebugStringA("[FATAL] TEST: nativeInit entered (before any code)\n");
    Log("=== nativeInit V2 START ===");
    OutputDebugStringA("[RAW] nativeInit called - DLL loaded!\n");
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

// ==============================================
// 自动判断顶点类型（C++ 端完整检测逻辑）
// 在 vertexSize 字节的顶点数组中采样判断坐标空间
// ==============================================
static VertexType autoDetectVertexType(const float* vertices, UINT vertexCount, UINT vertexSize) {
    if (vertexCount == 0 || vertexSize < 12) {
        return VERTEX_TYPE_UNKNOWN;
    }

    float minX = 1e10f, maxX = -1e10f;
    float minY = 1e10f, maxY = -1e10f;
    float minZ = 1e10f, maxZ = -1e10f;

    UINT stride = vertexSize / sizeof(float);
    for (UINT i = 0; i < vertexCount; i++) {
        UINT offset = i * stride;
        float x = vertices[offset];
        float y = vertices[offset + 1];
        float z = vertices[offset + 2];

        if (x < minX) minX = x; if (x > maxX) maxX = x;
        if (y < minY) minY = y; if (y > maxY) maxY = y;
        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
    }

    float xRange = maxX - minX;
    float yRange = maxY - minY;
    float zRange = maxZ - minZ;

    Log("=== 顶点类型自动检测 ===");
    Log("  坐标范围: X[%.2f, %.2f] Y[%.2f, %.2f] Z[%.2f, %.2f]",
        minX, maxX, minY, maxY, minZ, maxZ);
    Log("  范围大小: X=%.2f, Y=%.2f, Z=%.2f", xRange, yRange, zRange);

    // 条件0：天空盒特殊处理（Z值极大且范围固定）
    if (minZ > 1000.0f && zRange < 1.0f) {
        Log("  判定: 3D世界物体 (天空盒)");
        return VERTEX_TYPE_WORLD;
    }

    // 条件0.5：粒子特殊处理（极小尺寸但有深度）
    if (xRange < 1.0f && yRange < 1.0f && zRange > 0.01f) {
        Log("  判定: 3D世界物体 (粒子)");
        return VERTEX_TYPE_WORLD;
    }

    // 条件1：Z轴有明显深度变化 → 3D世界物体
    if (zRange > 0.1f) {
        Log("  判定: 3D世界物体 (Z轴深度变化明显)");
        return VERTEX_TYPE_WORLD;
    }

    // 条件2：坐标超出屏幕范围 → 3D世界物体
    const float SCREEN_MAX_X = 2560.0f;
    const float SCREEN_MAX_Y = 1440.0f;
    if (minX < -100.0f || maxX > SCREEN_MAX_X ||
        minY < -100.0f || maxY > SCREEN_MAX_Y) {
        Log("  判定: 3D世界物体 (坐标超出屏幕范围)");
        return VERTEX_TYPE_WORLD;
    }

    // 条件3：Z值接近1.0（GUI常用深度值）且无深度变化 → 2D GUI
    if (fabs(minZ - 1.0f) < 0.01f && fabs(maxZ - 1.0f) < 0.01f) {
        Log("  判定: 2D GUI元素 (Z值固定为1.0)");
        return VERTEX_TYPE_SCREEN;
    }

    // 条件4：极小尺寸且无深度变化 → 2D GUI
    if (xRange < 50.0f && yRange < 50.0f && zRange < 0.01f) {
        Log("  判定: 2D GUI元素 (极小尺寸且无深度变化)");
        return VERTEX_TYPE_SCREEN;
    }

    // 默认：保守判定为3D世界物体
    Log("  判定: 3D世界物体 (默认保守判定)");
    return VERTEX_TYPE_WORLD;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRecordVertices(JNIEnv* env, jclass, jfloatArray verts, jint count, jbyteArray colorArray, jint coordType) {
    if (g_deviceLost.load() || !IsDeviceValid()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "D3D device lost, rendering stopped");
        return;
    }
    // 存储坐标类型
    g_currentCoordType = coordType;
    Log("nativeRecordVertices: Java coordType=%d (0=WORLD, 1=SCREEN, 2=NDC)", coordType);

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

    // ===== C++ 端自动检测顶点类型，覆盖 Java 判定 =====
    {
        VertexType detected = autoDetectVertexType(buf.data(), safeCount, 3 * sizeof(float));
        if (detected == VERTEX_TYPE_WORLD) {
            g_currentCoordType = 0; // COORD_WORLD
            Log("  C++ 检测结果: WORLD → 覆盖 coordType=0");
        } else if (detected == VERTEX_TYPE_SCREEN) {
            g_currentCoordType = 1; // COORD_SCREEN
            Log("  C++ 检测结果: SCREEN → 覆盖 coordType=1");
        } else {
            Log("  C++ 检测结果: UNKNOWN → 保持 Java coordType=%d", coordType);
        }
    }
    // ===== 顶点类型检测结束 =====

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
    HRESULT hr = g_imVB->Map(0, nullptr, &dst);
    if (FAILED(hr) || dst == nullptr) {
        Log("[FATAL] nativeRecordVertices Map failed, hr=0x%08X\n", hr);
        if (colorData != nullptr) {
            env->ReleaseByteArrayElements(colorArray, colorData, JNI_ABORT);
        }
        LeaveCriticalSection(&g_stateLock);
        return;
    }
    // 容量校验：确保写入偏移+数据量不超出缓冲区总大小
    D3D12_RESOURCE_DESC vbDesc = g_imVB->GetDesc();
    UINT64 writeTotalBytes = (UINT64)g_imVBSize + (UINT64)byteCount;
    if (writeTotalBytes > vbDesc.Width) {
        Log("[FATAL] nativeRecordVertices vertex buffer overflow, need %llu bytes, buffer only %llu", writeTotalBytes, vbDesc.Width);
        g_imVB->Unmap(0, nullptr);
        if (colorData != nullptr) {
            env->ReleaseByteArrayElements(colorArray, colorData, JNI_ABORT);
        }
        LeaveCriticalSection(&g_stateLock);
        return;
    }
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
    if (g_deviceLost.load() || !IsDeviceValid()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "D3D device lost, rendering stopped");
        return;
    }
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
    ch.vertexType = g_currentCoordType;
    g_drawChunks.push_back(ch);

    void* dst = nullptr;
    HRESULT hr = g_imVB->Map(0, nullptr, &dst);
    if (FAILED(hr) || dst == nullptr) {
        Log("[FATAL] nativeRecordVerticesPT Map failed, hr=0x%08X\n", hr);
        g_imVB->Unmap(0, nullptr);
        LeaveCriticalSection(&g_stateLock);
        return;
    }
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
    if (g_deviceLost.load() || !IsDeviceValid()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "D3D device lost, rendering stopped");
        return;
    }
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
    if (g_deviceLost.load() || !IsDeviceValid()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "D3D device lost, rendering stopped");
        return;
    }
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
    if (g_deviceLost.load() || !IsDeviceValid()) return;
    if (!g_dev || !g_queue || !g_rtvHeap) {
        Log("WARN: nativeUploadTextureEx called before D3D12 full init, skip");
        return;
    }
    if (!pixels) return;
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

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetMvp(JNIEnv* env, jclass, jfloatArray matrix, jint coordType) {
    if (g_deviceLost.load() || !IsDeviceValid()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "D3D device lost, rendering stopped");
        return;
    }
    Log("=== nativeSetMvp CALLED (coordType=%d) ===", coordType);
    jfloat* src = env->GetFloatArrayElements(matrix, nullptr);
    if (src) {
        // 根据 coordType 存储到不同的矩阵
        switch (coordType) {
            case 0: memcpy(g_mvpWorld, src, 64); break;
            case 1: memcpy(g_mvpScreen, src, 64); break;
            case 2: memcpy(g_mvpNDC, src, 64); break;
            default: Log("nativeSetMvp: unknown coordType=%d", coordType); break;
        }
        // 同时更新 g_cbData（用于当前帧渲染）
        if (g_cbData) {
            memcpy(g_cbData, src, 64);
        }
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

// 通用顶点数据上传函数（供粒子、实体、天空盒复用）
static bool UploadVertexData(const float* data, UINT count, UINT vertexSize,
                              ComPtr<ID3D12Resource>& outUploadBuffer,
                              D3D12_GPU_VIRTUAL_ADDRESS& outGpuAddress) {
    UINT totalSize = count * vertexSize;

    D3D12_HEAP_PROPERTIES uploadProps = {};
    uploadProps.Type = D3D12_HEAP_TYPE_UPLOAD;
    uploadProps.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
    uploadProps.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
    uploadProps.CreationNodeMask = 1;
    uploadProps.VisibleNodeMask = 1;
    D3D12_RESOURCE_DESC bufferDesc = {};
    bufferDesc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    bufferDesc.Width = totalSize;
    bufferDesc.Height = 1;
    bufferDesc.DepthOrArraySize = 1;
    bufferDesc.MipLevels = 1;
    bufferDesc.Format = DXGI_FORMAT_UNKNOWN;
    bufferDesc.SampleDesc.Count = 1;
    bufferDesc.SampleDesc.Quality = 0;
    bufferDesc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
    bufferDesc.Flags = D3D12_RESOURCE_FLAG_NONE;

    HRESULT hr = g_dev->CreateCommittedResource(
        &uploadProps, D3D12_HEAP_FLAG_NONE, &bufferDesc,
        D3D12_RESOURCE_STATE_GENERIC_READ, nullptr,
        IID_PPV_ARGS(&outUploadBuffer));

    if (FAILED(hr)) {
        Log("ERROR: Failed to create vertex upload buffer, hr=0x%08X", hr);
        return false;
    }

    void* mappedData;
    D3D12_RANGE readRange = {};
    readRange.Begin = 0;
    readRange.End = 0;
    hr = outUploadBuffer->Map(0, &readRange, &mappedData);
    if (FAILED(hr) || mappedData == nullptr) {
        Log("[FATAL] UploadVertexData Map failed, hr=0x%08X\n", hr);
        return false;
    }
    D3D12_RESOURCE_DESC bufDesc = outUploadBuffer->GetDesc();
    if ((UINT64)totalSize > bufDesc.Width) {
        Log("[FATAL] memory write out of buffer range");
        outUploadBuffer->Unmap(0, nullptr);
        return false;
    }
    memcpy(mappedData, data, totalSize);
    outUploadBuffer->Unmap(0, nullptr);
    outGpuAddress = outUploadBuffer->GetGPUVirtualAddress();

    return true;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDraw(JNIEnv*, jclass, jint vertexCount) {
    if (g_deviceLost.load() || !IsDeviceValid()) {
        OutputDebugStringA("[FATAL] nativeDraw: device lost, abort\n");
        return;
    }
    (void)vertexCount;
    OutputDebugStringA("nativeDraw: called, signaling render thread\n");
    if (g_frameReadyEvent != nullptr) {
        SetEvent(g_frameReadyEvent);
    } else {
        OutputDebugStringA("nativeDraw: g_frameReadyEvent is null!\n");
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderSky(JNIEnv*, jclass) {
    if (!g_globalDeviceReady) {
        Log("[ERROR] Device not ready, skip rendering in nativeRenderSky");
        return;
    }
    if (!g_dev || !g_cl || !g_cbData) return;

    // 设置半透明 PSO（支持带 alpha 的天空颜色）
    if (!BuildAlphaBlendPSO() || !g_rsSolidVariants[0]) return;
    g_cl->SetGraphicsRootSignature(g_rsSolidVariants[0].Get());
    g_cl->SetPipelineState(g_psoAlphaBlend.Get());
    g_cl->SetGraphicsRootConstantBufferView(0, g_cbUpload->GetGPUVirtualAddress());

    std::lock_guard<std::mutex> lock(g_skyMutex);

    // 1. 渲染天空渐变背景（全屏四边形，使用 VertexPC 格式）
    {
        float r0 = g_skyColor[0], g0 = g_skyColor[1], b0 = g_skyColor[2], a0 = g_skyColor[3];
        float r1 = r0 * 0.5f, g1 = g0 * 0.5f, b1 = b0 * 0.8f;
        struct SkyVertex { float x, y, z; uint32_t color; };
        SkyVertex skyVerts[4] = {
            {-1, -1, 0, ((uint32_t)(uint8_t)(r0*255) | ((uint32_t)(uint8_t)(g0*255) << 8) | ((uint32_t)(uint8_t)(b0*255) << 16) | ((uint32_t)(uint8_t)(a0*255) << 24))},
            { 1, -1, 0, ((uint32_t)(uint8_t)(r0*255) | ((uint32_t)(uint8_t)(g0*255) << 8) | ((uint32_t)(uint8_t)(b0*255) << 16) | ((uint32_t)(uint8_t)(a0*255) << 24))},
            {-1,  1, 0, ((uint32_t)(uint8_t)(r1*255) | ((uint32_t)(uint8_t)(g1*255) << 8) | ((uint32_t)(uint8_t)(b1*255) << 16) | ((uint32_t)(uint8_t)(a0*255) << 24))},
            { 1,  1, 0, ((uint32_t)(uint8_t)(r1*255) | ((uint32_t)(uint8_t)(g1*255) << 8) | ((uint32_t)(uint8_t)(b1*255) << 16) | ((uint32_t)(uint8_t)(a0*255) << 24))},
        };
        ComPtr<ID3D12Resource> skyBuf;
        D3D12_GPU_VIRTUAL_ADDRESS skyAddr;
        Log("[SKY DEBUG] uploading sky background: 4 verts, %llu bytes", (UINT64)(4 * sizeof(SkyVertex)));
        if (UploadVertexData((float*)skyVerts, 4, sizeof(SkyVertex), skyBuf, skyAddr)) {
            float ident[16] = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
            memcpy(g_cbData, ident, sizeof(ident));
            void* cbMapped;
            D3D12_RANGE writeRange = {};
            writeRange.Begin = 0;
            writeRange.End = g_cbSize;
            HRESULT hr = g_cbUpload->Map(0, &writeRange, &cbMapped);
            if (FAILED(hr) || !cbMapped) {
                Log("[FATAL] nativeRenderSky CBV Map failed, hr=0x%08X\n", hr);
            } else {
                D3D12_RESOURCE_DESC cbvDesc = g_cbUpload->GetDesc();
                if ((UINT64)g_cbSize > cbvDesc.Width) {
                    Log("[FATAL] memory write out of buffer range");
                    g_cbUpload->Unmap(0, &writeRange);
                } else {
                    memcpy(cbMapped, g_cbData, g_cbSize);
                }
            }
            g_cbUpload->Unmap(0, &writeRange);
            D3D12_VERTEX_BUFFER_VIEW vbView = {};
            vbView.BufferLocation = skyAddr;
            vbView.StrideInBytes = sizeof(SkyVertex);
            vbView.SizeInBytes = 4 * sizeof(SkyVertex);
            g_cl->IASetVertexBuffers(0, 1, &vbView);
            g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
            g_cl->DrawInstanced(4, 1, 0, 0);
        }
    }

    // 2. 渲染太阳
    {
        float sunSize = 0.15f;
        float sunX = cosf(g_sunAngle) * 0.8f;
        float sunY = sinf(g_sunAngle) * 0.8f;
        struct SkyVertex { float x, y, z; uint32_t color; };
        uint32_t sunCol = (255 | (255 << 8) | (200 << 16) | (255 << 24));
        SkyVertex sunVerts[4] = {
            {sunX - sunSize, sunY - sunSize, 0, sunCol},
            {sunX + sunSize, sunY - sunSize, 0, sunCol},
            {sunX - sunSize, sunY + sunSize, 0, sunCol},
            {sunX + sunSize, sunY + sunSize, 0, sunCol},
        };
        ComPtr<ID3D12Resource> sunBuf;
        D3D12_GPU_VIRTUAL_ADDRESS sunAddr;
        Log("[SKY DEBUG] uploading sun: 4 verts, %llu bytes", (UINT64)(4 * sizeof(SkyVertex)));
        if (UploadVertexData((float*)sunVerts, 4, sizeof(SkyVertex), sunBuf, sunAddr)) {
            D3D12_VERTEX_BUFFER_VIEW vbView = {};
            vbView.BufferLocation = sunAddr;
            vbView.StrideInBytes = sizeof(SkyVertex);
            vbView.SizeInBytes = 4 * sizeof(SkyVertex);
            g_cl->IASetVertexBuffers(0, 1, &vbView);
            g_cl->DrawInstanced(4, 1, 0, 0);
        }
    }

    // 3. 渲染月亮
    {
        float moonSize = 0.1f;
        float moonX = cosf(g_moonAngle) * 0.8f;
        float moonY = sinf(g_moonAngle) * 0.8f;
        struct SkyVertex { float x, y, z; uint32_t color; };
        uint32_t moonCol = (230 | (230 << 8) | (255 << 16) | (255 << 24));
        SkyVertex moonVerts[4] = {
            {moonX - moonSize, moonY - moonSize, 0, moonCol},
            {moonX + moonSize, moonY - moonSize, 0, moonCol},
            {moonX - moonSize, moonY + moonSize, 0, moonCol},
            {moonX + moonSize, moonY + moonSize, 0, moonCol},
        };
        ComPtr<ID3D12Resource> moonBuf;
        D3D12_GPU_VIRTUAL_ADDRESS moonAddr;
        Log("[SKY DEBUG] uploading moon: 4 verts, %llu bytes", (UINT64)(4 * sizeof(SkyVertex)));
        if (UploadVertexData((float*)moonVerts, 4, sizeof(SkyVertex), moonBuf, moonAddr)) {
            D3D12_VERTEX_BUFFER_VIEW vbView = {};
            vbView.BufferLocation = moonAddr;
            vbView.StrideInBytes = sizeof(SkyVertex);
            vbView.SizeInBytes = 4 * sizeof(SkyVertex);
            g_cl->IASetVertexBuffers(0, 1, &vbView);
            g_cl->DrawInstanced(4, 1, 0, 0);
        }
    }
    Log("nativeRenderSky: sky+sun+moon rendered");
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderTerrain(JNIEnv*, jclass) {
    // 渲染地形（方块）— 顶点数据已通过 nativeRecordVertices 上传
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadEntities(JNIEnv* env, jclass, jfloatArray entityData, jint count) {
    if (!g_dev || !g_queue || !g_rtvHeap) {
        Log("WARN: nativeUploadEntities called before D3D12 full init, skip");
        return;
    }
    // 批量上传实体数据到 GPU
    (void)env; (void)entityData; (void)count;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderEntities(JNIEnv*, jclass) {
    if (!g_globalDeviceReady) {
        Log("[ERROR] Device not ready, skip rendering in nativeRenderEntities");
        return;
    }
    if (!g_dev || !g_queue || !g_rtvHeap) {
        Log("WARN: nativeRenderEntities called before D3D12 full init, skip");
        return;
    }
    // 绘制所有实体
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetSkyParameters__FFFFFF
(JNIEnv* env, jclass clazz, jfloat r, jfloat g, jfloat b, jfloat a, jfloat sunAngle, jfloat moonAngle) {
    if (!g_dev || !g_queue || !g_rtvHeap) {
        Log("WARN: nativeSetSkyParameters called before D3D12 full init, skip");
        return;
    }
    std::lock_guard<std::mutex> lock(g_skyMutex);
    g_skyColor[0] = r;
    g_skyColor[1] = g;
    g_skyColor[2] = b;
    g_skyColor[3] = a;
    g_sunAngle = sunAngle;
    g_moonAngle = moonAngle;
    Log("nativeSetSkyParameters: color[%.2f,%.2f,%.2f,%.2f] sun=%.2f moon=%.2f", r, g, b, a, sunAngle, moonAngle);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadParticles
(JNIEnv* env, jclass clazz, jfloatArray vertices, jint count, jint vertexSize) {
    if (g_deviceLost.load() || !IsDeviceValid()) return;
    if (!g_dev) {
        Log("ERROR: nativeUploadParticles called before D3D12 initialization");
        return;
    }

    if (count <= 0 || vertexSize <= 0) {
        Log("WARNING: nativeUploadParticles invalid params (count=%d, vertexSize=%d)", count, vertexSize);
        return;
    }

    jfloat* data = env->GetFloatArrayElements(vertices, nullptr);
    if (!data) {
        Log("ERROR: nativeUploadParticles failed to get vertex array");
        return;
    }

    Log("=== nativeUploadParticles ===");
    Log("  count=%d, vertexSize=%d, totalBytes=%d", count, vertexSize, count * vertexSize);

    VertexType vertexType = autoDetectVertexType(data, count, vertexSize);
    Log("  vertexType=%s", vertexType == VERTEX_TYPE_WORLD ? "WORLD" : "SCREEN");

    ParticleDrawCall drawCall;
    drawCall.vertexCount = count;
    drawCall.vertexSize = vertexSize;
    drawCall.type = vertexType;

    if (!UploadVertexData(data, count, vertexSize, drawCall.uploadBuffer, drawCall.gpuAddress)) {
        env->ReleaseFloatArrayElements(vertices, data, JNI_ABORT);
        return;
    }

    {
        std::lock_guard<std::mutex> lock(g_particleMutex);
        g_particleDrawCalls.push_back(std::move(drawCall));
        g_particlesPending = true;
        Log("  Particle draw call queued (total=%zu)", g_particleDrawCalls.size());
    }

    env->ReleaseFloatArrayElements(vertices, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderParticles
(JNIEnv* env, jclass clazz) {
    if (!g_globalDeviceReady) {
        Log("[ERROR] Device not ready, skip rendering in nativeRenderParticles");
        return;
    }
    if (!g_dev || !g_cl) {
        Log("ERROR: nativeRenderParticles called without D3D12 context");
        return;
    }

    std::lock_guard<std::mutex> lock(g_particleMutex);

    if (g_particleDrawCalls.empty()) {
        return;
    }

    Log("=== nativeRenderParticles ===");
    Log("  Rendering %zu particle draw calls", g_particleDrawCalls.size());

    for (size_t i = 0; i < g_particleDrawCalls.size(); i++) {
        auto& dc = g_particleDrawCalls[i];

        D3D12_VERTEX_BUFFER_VIEW vbView = {};
        vbView.BufferLocation = dc.gpuAddress;
        vbView.StrideInBytes = dc.vertexSize;
        vbView.SizeInBytes = dc.vertexCount * dc.vertexSize;

        g_cl->IASetVertexBuffers(0, 1, &vbView);
        g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        g_cl->DrawInstanced(dc.vertexCount, 1, 0, 0);

        Log("  Particle DC[%zu]: %d vertices, type=%s",
            i, dc.vertexCount,
            dc.type == VERTEX_TYPE_WORLD ? "WORLD" : "SCREEN");
    }

    g_particleDrawCalls.clear();
    g_particlesPending = false;
    Log("  All particles rendered, draw calls cleared");
}

// ==============================================
// 半透明渲染 JNI 函数
// ==============================================
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeUploadTransparent
(JNIEnv* env, jclass clazz, jfloatArray vertices, jint count, jint vertexSize, jfloat distance) {
    if (g_deviceLost.load() || !IsDeviceValid()) return;
    if (!g_dev || count <= 0 || vertexSize <= 0) return;

    jfloat* data = env->GetFloatArrayElements(vertices, nullptr);
    if (!data) return;

    TransparentDrawCall drawCall;
    drawCall.vertexCount = count;
    drawCall.vertexSize = vertexSize;
    drawCall.distance = distance;
    drawCall.type = autoDetectVertexType(data, count, vertexSize);

    if (UploadVertexData(data, count, vertexSize, drawCall.uploadBuffer, drawCall.gpuAddress)) {
        std::lock_guard<std::mutex> lock(g_transparentMutex);
        g_transparentDrawCalls.push_back(std::move(drawCall));
        Log("nativeUploadTransparent: queued %d verts, distance=%.2f (total=%zu)",
            count, distance, g_transparentDrawCalls.size());
    }

    env->ReleaseFloatArrayElements(vertices, data, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRenderTransparent
(JNIEnv* env, jclass clazz) {
    if (!g_globalDeviceReady) {
        Log("[ERROR] Device not ready, skip rendering in nativeRenderTransparent");
        return;
    }
    if (!g_cl) return;

    std::lock_guard<std::mutex> lock(g_transparentMutex);

    if (g_transparentDrawCalls.empty()) return;

    // 按距离相机从远到近排序（半透明物体必须先画远的再画近的）
    std::sort(g_transparentDrawCalls.begin(), g_transparentDrawCalls.end(),
        [](const TransparentDrawCall& a, const TransparentDrawCall& b) {
            return a.distance > b.distance;
        });

    Log("nativeRenderTransparent: rendering %zu transparent draw calls",
        g_transparentDrawCalls.size());

    // 设置半透明 PSO
    if (BuildAlphaBlendPSO() && g_rsSolidVariants[0]) {
        g_cl->SetGraphicsRootSignature(g_rsSolidVariants[0].Get());
        g_cl->SetPipelineState(g_psoAlphaBlend.Get());
        g_cl->SetGraphicsRootConstantBufferView(0, g_cbUpload->GetGPUVirtualAddress());
    }

    for (auto& dc : g_transparentDrawCalls) {
        // 根据类型设置投影矩阵
        if (dc.type == VERTEX_TYPE_WORLD) {
            if (g_cbData) {
                memcpy(g_cbData, g_mvpWorld, sizeof(g_mvpWorld));
            }
        } else if (dc.type == VERTEX_TYPE_SCREEN) {
            float w = (float)g_w;
            float h = (float)g_h;
            float ortho[16] = {
                2.0f / w, 0.0f, 0.0f, -1.0f,
                0.0f, -2.0f / h, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
            };
            if (g_cbData) {
                memcpy(g_cbData, ortho, sizeof(ortho));
            }
        }
        // 刷新常量缓冲区
        if (g_cbUpload && g_cbData) {
            void* cbMapped;
            D3D12_RANGE writeRange = {};
            writeRange.Begin = 0;
            writeRange.End = g_cbSize;
            HRESULT hr = g_cbUpload->Map(0, &writeRange, &cbMapped);
            if (FAILED(hr) || !cbMapped) {
                Log("[FATAL] nativeRenderTransparent CBV Map failed, hr=0x%08X\n", hr);
            } else {
                D3D12_RESOURCE_DESC cbvDesc = g_cbUpload->GetDesc();
                if ((UINT64)g_cbSize > cbvDesc.Width) {
                    Log("[FATAL] memory write out of buffer range");
                    g_cbUpload->Unmap(0, &writeRange);
                } else {
                    memcpy(cbMapped, g_cbData, g_cbSize);
                }
            }
            g_cbUpload->Unmap(0, &writeRange);
        }

        D3D12_VERTEX_BUFFER_VIEW vbView = {};
        vbView.BufferLocation = dc.gpuAddress;
        vbView.StrideInBytes = dc.vertexSize;
        vbView.SizeInBytes = dc.vertexCount * dc.vertexSize;

        g_cl->IASetVertexBuffers(0, 1, &vbView);
        g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        g_cl->DrawInstanced(dc.vertexCount, 1, 0, 0);

        Log("  Transparent DC: %d verts, distance=%.2f, type=%s",
            dc.vertexCount, dc.distance,
            dc.type == VERTEX_TYPE_WORLD ? "WORLD" : "SCREEN");
    }

    g_transparentDrawCalls.clear();
    Log("  All transparent draw calls cleared");
}

}
