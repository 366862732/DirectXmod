// d3d12bridge.cpp — GL→D3D12 command translation + framebuffer mirror
//
// Architecture:
//   1. D3D12 owns independent window + render thread (~60fps)
//   2. Framebuffer mirror: glReadPixels → Java → JNI → D3D12 texture → quad
//   3. GL→D3D12 translation: intercepted GL commands drive native D3D12 draw calls
//   4. Both layers composited: solid-color geometry on top of framebuffer copy
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
static HWND g_hwnd = nullptr;
static ComPtr<ID3D12Device>          g_dev;
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
static const UINT  g_imVBCap = 4 * 1024 * 1024; // 4MB upload buffer
static UINT        g_imVBSize = 0;           // bytes written this frame
static UINT        g_imVertCount = 0;        // total vertices this frame

// Per-draw chunk (supports multiple draw calls with different topologies)
struct DrawChunk { UINT vertexStart; UINT vertexCount; D3D_PRIMITIVE_TOPOLOGY topo; };
static std::vector<DrawChunk> g_drawChunks;
static D3D_PRIMITIVE_TOPOLOGY g_pendingTopo = D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST;

struct Vertex2D { float x,y,u,v; };
struct VertexPC { float x,y,z; UINT color; };

// === Window ===
static LRESULT CALLBACK WP(HWND h, UINT m, WPARAM w, LPARAM l) { return DefWindowProcW(h,m,w,l); }
static HWND MakeWindow() {
    const wchar_t* cn = L"GL4DX12_Mirror";
    WNDCLASSW wc = {}; wc.lpfnWndProc=WP; wc.hInstance=GetModuleHandleW(0); wc.lpszClassName=cn;
    RegisterClassW(&wc);
    HWND hw = CreateWindowExW(0, cn, L"GL4DX12 - Minecraft Mirror",
        WS_OVERLAPPEDWINDOW&~WS_THICKFRAME, CW_USEDEFAULT,CW_USEDEFAULT,
        g_w,g_h,0,0,wc.hInstance,0);
    ShowWindow(hw, SW_SHOW);
    return hw;
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
struct VS_IN { float3 p : POS; uint c : COL; };
struct PS_IN { float4 p : SV_POSITION; float4 c : COL; };
PS_IN VSMain(VS_IN i) {
    PS_IN o;
    o.p = float4(i.p, 1);
    o.c = float4(((i.c>>16)&0xff)/255.0, ((i.c>>8)&0xff)/255.0, (i.c&0xff)/255.0, ((i.c>>24)&0xff)/255.0);
    return o;
}
)";
static const char* kPS_Solid = R"(
struct PS_IN { float4 p : SV_POSITION; float4 c : COL; };
float4 PSMain(PS_IN i) : SV_TARGET { return i.c; }
)";

static bool MkPSOSolid() {
    ComPtr<ID3DBlob> vs, ps, err;
    if (FAILED(D3DCompile(kVS_Solid, strlen(kVS_Solid), 0, 0, 0, "VSMain", "vs_5_0", 0, 0, &vs, &err)))
    { Log("VS_solid fail: %s", err ? (char*)err->GetBufferPointer() : "?"); return false; }
    if (FAILED(D3DCompile(kPS_Solid, strlen(kPS_Solid), 0, 0, 0, "PSMain", "ps_5_0", 0, 0, &ps, &err)))
    { Log("PS_solid fail: %s", err ? (char*)err->GetBufferPointer() : "?"); return false; }

    // Root signature: 0 parameters, just IA + VS/PS
    D3D12_ROOT_SIGNATURE_DESC rsd = {};
    rsd.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;
    ComPtr<ID3DBlob> rb;
    if (FAILED(D3D12SerializeRootSignature(&rsd, D3D_ROOT_SIGNATURE_VERSION_1, &rb, &err))) return false;
    if (FAILED(g_dev->CreateRootSignature(0, rb->GetBufferPointer(), rb->GetBufferSize(),
        IID_PPV_ARGS(&g_rsSolid)))) return false;

    D3D12_INPUT_ELEMENT_DESC ie[] = {
        {"POS", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"COL", 0, DXGI_FORMAT_R8G8B8A8_UNORM,    0, 12,D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
    };

    D3D12_GRAPHICS_PIPELINE_STATE_DESC pd = {};
    pd.pRootSignature = g_rsSolid.Get();
    pd.VS = {vs->GetBufferPointer(), vs->GetBufferSize()};
    pd.PS = {ps->GetBufferPointer(), ps->GetBufferSize()};
    pd.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    pd.BlendState.AlphaToCoverageEnable = FALSE;
    pd.BlendState.IndependentBlendEnable = FALSE;
    // Enable alpha blending for translucency
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
    pd.RasterizerState.DepthClipEnable = FALSE;
    pd.DepthStencilState.DepthEnable = FALSE;
    pd.InputLayout = {ie, 2};
    pd.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;
    pd.NumRenderTargets = 1;
    pd.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;
    pd.SampleDesc.Count = 1;

    if (FAILED(g_dev->CreateGraphicsPipelineState(&pd, IID_PPV_ARGS(&g_psoSolid)))) { Log("PSO_solid fail"); return false; }
    Log("PSO_solid OK");
    return true;
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
static DWORD WINAPI RenderLoop(LPVOID) {
    Log("Render thread started");
    LARGE_INTEGER freq, last;
    QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&last);

    // Fullscreen quad vertices
    Vertex2D quad[] = {
        {-1,-1, 0,1}, {3,-1, 2,1}, {-1,3, 0,-1},
    };
    MkUpload(g_vbUpload, quad, sizeof(quad));
    g_vbv.BufferLocation = g_vbUpload->GetGPUVirtualAddress();
    g_vbv.StrideInBytes = sizeof(Vertex2D);
    g_vbv.SizeInBytes = sizeof(quad);

    while (g_run) {
        LARGE_INTEGER now;
        QueryPerformanceCounter(&now);
        double dt = (double)(now.QuadPart - last.QuadPart) / freq.QuadPart;
        if (dt < 0.016) { Sleep(1); continue; }
        last = now;

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
        g_cl->OMSetRenderTargets(1, &rtv, FALSE, nullptr);

        // Fixed dark gray clear — framebuffer quad covers entire window,
        // so clear color only shows when no texture is uploaded yet.
        float bg[4] = {0.08f, 0.08f, 0.08f, 1.0f};
        g_cl->ClearRenderTargetView(rtv, bg, 0, nullptr);

        D3D12_VIEWPORT vp = {0,0,(float)g_w,(float)g_h,0,1};
        D3D12_RECT     sc = {0,0,(LONG)g_w,(LONG)g_h};
        g_cl->RSSetViewports(1, &vp);
        g_cl->RSSetScissorRects(1, &sc);

        // --- Layer 1: GL→D3D12 translated geometry (from BufferBuilder captures) ---
        {
            EnterCriticalSection(&g_stateLock);
            auto chunks = g_drawChunks;   // copy for thread safety
            LeaveCriticalSection(&g_stateLock);

            if (!chunks.empty() && g_psoSolid) {
                g_cl->SetGraphicsRootSignature(g_rsSolid.Get());
                g_cl->SetPipelineState(g_psoSolid.Get());

                for (auto& ch : chunks) {
                    D3D12_VERTEX_BUFFER_VIEW chVbv = g_imVbv;
                    chVbv.BufferLocation += (UINT64)ch.vertexStart * sizeof(VertexPC);
                    chVbv.SizeInBytes = ch.vertexCount * sizeof(VertexPC);
                    g_cl->IASetVertexBuffers(0, 1, &chVbv);
                    g_cl->IASetPrimitiveTopology(ch.topo);
                    g_cl->DrawInstanced(ch.vertexCount, 1, 0, 0);
                }
            }
        }

        // --- Layer 2: Framebuffer-mirror textured quad ---
        EnterCriticalSection(&g_texLock);
        bool hasTex = (g_texDefault != nullptr && g_texW > 0);
        LeaveCriticalSection(&g_texLock);

        if (hasTex) {
            g_cl->SetGraphicsRootSignature(g_rs.Get());
            g_cl->SetPipelineState(g_pso.Get());
            g_cl->SetDescriptorHeaps(1, g_srvHeap.GetAddressOf());
            g_cl->SetGraphicsRootDescriptorTable(0,
                g_srvHeap->GetGPUDescriptorHandleForHeapStart());
            g_cl->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
            g_cl->IASetVertexBuffers(0, 1, &g_vbv);
            g_cl->DrawInstanced(3, 1, 0, 0);
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

// === Init ===
static bool InitD3D12() {
    Log("=== Init D3D12 mirror ===");
    g_hwnd = MakeWindow();
    if (!g_hwnd) return false;

    ComPtr<IDXGIFactory4> dxgi;
    if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&dxgi)))) return false;

    ComPtr<IDXGIAdapter1> adp;
    for (UINT i=0; dxgi->EnumAdapters1(i,&adp)!=DXGI_ERROR_NOT_FOUND; i++) {
        DXGI_ADAPTER_DESC1 d; adp->GetDesc1(&d);
        if (SUCCEEDED(D3D12CreateDevice(adp.Get(), D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_dev))))
        { Log("Device: %S", d.Description); break; }
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
    if (FAILED(dxgi->CreateSwapChainForHwnd(g_queue.Get(),g_hwnd,&sd,0,0,&sc1)))
    { Log("SwapChain fail"); return false; }
    sc1.As(&g_swap); g_fi = g_swap->GetCurrentBackBufferIndex();

    // RTV heap
    D3D12_DESCRIPTOR_HEAP_DESC rd = {};
    rd.NumDescriptors=2; rd.Type=D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    if (FAILED(g_dev->CreateDescriptorHeap(&rd, IID_PPV_ARGS(&g_rtvHeap)))) return false;
    g_rtvSize = g_dev->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);

    // SRV heap
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

    if (!MkPSO()) return false;
    if (!MkPSOSolid()) { Log("WARN: Solid PSO failed, immediate-mode disabled"); }
    else {
        // Create immediate-mode vertex buffer (upload heap, 1MB)
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

    InitializeCriticalSection(&g_texLock);
    InitializeCriticalSection(&g_stateLock);
    g_ok = true; g_run = true;
    g_thread = CreateThread(0, 0, RenderLoop, 0, 0, 0);
    Log("=== D3D12 Mirror Ready ===");
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
        g_imVB.Reset(); g_psoSolid.Reset(); g_rsSolid.Reset();
        for (auto& r : g_rt) r.Reset();
        g_rtvHeap.Reset(); g_srvHeap.Reset();
        g_cl.Reset(); g_alloc.Reset(); g_swap.Reset(); g_queue.Reset(); g_fence.Reset(); g_dev.Reset();
        g_ok = false;
    }
    if (g_hwnd) { DestroyWindow(g_hwnd); g_hwnd=0; }
}

// === JNI ===
extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit
    (JNIEnv*, jclass, jlong, jint, jint) {
    CreateDirectoryA("C:\\temp", 0);
    if (g_ok) return JNI_TRUE;
    return InitD3D12() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv*, jclass) { CleanupD3D12(); }
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv*, jclass) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv*, jclass) {}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv*, jclass, jint w, jint h) {
    if (!g_ok||!g_swap) return;
    WaitGPU();
    for (auto& r : g_rt) r.Reset();
    g_cl.Reset(); g_alloc.Reset();
    g_swap->ResizeBuffers(2,(UINT)w,(UINT)h,DXGI_FORMAT_R8G8B8A8_UNORM,0);
    g_w=(UINT)w; g_h=(UINT)h; g_fi=g_swap->GetCurrentBackBufferIndex();
    D3D12_CPU_DESCRIPTOR_HANDLE rh=g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    for (UINT n=0;n<2;n++) { g_swap->GetBuffer(n,IID_PPV_ARGS(&g_rt[n])); g_dev->CreateRenderTargetView(g_rt[n].Get(),0,rh); rh.ptr+=g_rtvSize; }
    g_dev->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,IID_PPV_ARGS(&g_alloc));
    g_dev->CreateCommandList(0,D3D12_COMMAND_LIST_TYPE_DIRECT,g_alloc.Get(),0,IID_PPV_ARGS(&g_cl));
    g_cl->Close();
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
    if (g_hwnd) ShowWindow(g_hwnd, s?SW_SHOW:SW_HIDE);
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
    if (byteCount > g_imVBCap - g_imVBSize) return; // overflow

    jsize len = env->GetArrayLength(verts);
    std::vector<jfloat> buf(len);
    env->GetFloatArrayRegion(verts, 0, len, buf.data());

    // Convert float[7] per vertex → VertexPC
    EnterCriticalSection(&g_stateLock);

    // Record draw chunk BEFORE uploading vertices
    DrawChunk ch;
    ch.vertexStart = g_imVertCount;
    ch.vertexCount = (UINT)count;
    ch.topo = g_pendingTopo;
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

} // extern "C"
