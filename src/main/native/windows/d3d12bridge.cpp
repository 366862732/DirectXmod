// d3d12bridge.cpp - Full D3D12 rendering backend for Minecraft 26.1.2
#define WIN32_LEAN_AND_MEAN
#define _CRT_SECURE_NO_WARNINGS
#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <wrl.h>
#include <cstdio>
#include <cstdarg>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")

using namespace Microsoft::WRL;

// ============================================================
// Logging
// ============================================================
static void Log(const char* fmt, ...) {
    char buf[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buf, sizeof(buf), fmt, args);
    va_end(args);

    FILE* f = fopen("C:\\temp\\gl4dx12_d3d12.log", "a");
    if (f) {
        fprintf(f, "%s\n", buf);
        fclose(f);
    }
    OutputDebugStringA(buf);
    OutputDebugStringA("\n");
}

// ============================================================
// D3D12 Global State
// ============================================================
static ComPtr<ID3D12Device>               g_device;
static ComPtr<ID3D12CommandQueue>         g_commandQueue;
static ComPtr<IDXGISwapChain3>            g_swapChain;
static ComPtr<ID3D12Resource>             g_renderTargets[2];
static ComPtr<ID3D12DescriptorHeap>       g_rtvHeap;
static ComPtr<ID3D12CommandAllocator>     g_commandAllocator;
static ComPtr<ID3D12GraphicsCommandList>  g_commandList;
static ComPtr<ID3D12Fence>                g_fence;
static HANDLE                             g_fenceEvent = nullptr;
static UINT64                             g_fenceValue = 0;
static UINT                               g_rtvDescriptorSize = 0;
static UINT                               g_frameIndex = 0;
static UINT                               g_width = 0;
static UINT                               g_height = 0;
static HWND                               g_hwnd = nullptr;
static bool                               g_initialized = false;

// Stored clear color
static float g_clearColor[4] = {0.0f, 0.2f, 0.6f, 1.0f};

// ============================================================
// Window finding
// ============================================================
static HWND FindMinecraftWindow() {
    Log("[D3D12] Searching for Minecraft window...");

    // Method 1: Exact title match
    HWND hwnd = FindWindowW(nullptr, L"Minecraft 26.1.2");
    if (hwnd) {
        Log("[D3D12] Found window by title 'Minecraft 26.1.2': HWND=%p", hwnd);
        return hwnd;
    }
    hwnd = FindWindowW(nullptr, L"Minecraft");
    if (hwnd) {
        Log("[D3D12] Found window by title 'Minecraft': HWND=%p", hwnd);
        return hwnd;
    }

    // Method 2: GLFW class (LWJGL 3)
    hwnd = FindWindowW(L"GLFW30", nullptr);
    if (hwnd) {
        Log("[D3D12] Found window by class 'GLFW30': HWND=%p", hwnd);
        return hwnd;
    }

    // Method 3: EnumWindows searching for Minecraft in title
    struct EnumCtx {
        HWND result;
    } ctx = {nullptr};

    EnumWindows([](HWND h, LPARAM lp) -> BOOL {
        auto* c = (EnumCtx*)lp;
        WCHAR buf[256];
        if (GetWindowTextW(h, buf, 256) > 0) {
            if (wcsstr(buf, L"Minecraft")) {
                c->result = h;
                return FALSE;
            }
        }
        return TRUE;
    }, (LPARAM)&ctx);

    if (ctx.result) {
        Log("[D3D12] Found window by EnumWindows: HWND=%p", ctx.result);
        return ctx.result;
    }

    // Method 4: Foreground window (last resort)
    hwnd = GetForegroundWindow();
    if (hwnd) {
        Log("[D3D12] Using foreground window: HWND=%p", hwnd);
        return hwnd;
    }

    Log("[D3D12] ERROR: Could not find any window!");
    return nullptr;
}

// ============================================================
// GPU Synchronization
// ============================================================
static void WaitForGPU() {
    if (g_fence && g_commandQueue) {
        const UINT64 fence = g_fenceValue;
        HRESULT hr = g_commandQueue->Signal(g_fence.Get(), fence);
        g_fenceValue++;
        if (SUCCEEDED(hr) && g_fence->GetCompletedValue() < fence) {
            g_fence->SetEventOnCompletion(fence, g_fenceEvent);
            WaitForSingleObject(g_fenceEvent, INFINITE);
        }
    }
}

// ============================================================
// D3D12 Initialization
// ============================================================
static bool InitD3D12(HWND hwnd, int width, int height) {
    Log("[D3D12] Initializing D3D12... HWND=%p, %dx%d", hwnd, width, height);
    HRESULT hr;

    if (width <= 0 || height <= 0) {
        RECT rect;
        if (GetClientRect(hwnd, &rect)) {
            width = rect.right - rect.left;
            height = rect.bottom - rect.top;
            Log("[D3D12] Got window size from client rect: %dx%d", width, height);
        }
    }
    if (width <= 0) width = 800;
    if (height <= 0) height = 600;

    g_width = (UINT)width;
    g_height = (UINT)height;
    g_hwnd = hwnd;

    // Enable D3D12 debug layer in debug builds
#if defined(DEBUG) || defined(_DEBUG)
    {
        ComPtr<ID3D12Debug> debugController;
        if (SUCCEEDED(D3D12GetDebugInterface(IID_PPV_ARGS(&debugController)))) {
            debugController->EnableDebugLayer();
            Log("[D3D12] Debug layer enabled");
        }
    }
#endif

    // 1. Create device via DXGI factory
    ComPtr<IDXGIFactory4> dxgiFactory;
    hr = CreateDXGIFactory1(IID_PPV_ARGS(&dxgiFactory));
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create DXGI factory: 0x%08X", (unsigned)hr);
        return false;
    }

    // Try to find a suitable hardware adapter
    ComPtr<IDXGIAdapter1> adapter;
    for (UINT i = 0; dxgiFactory->EnumAdapters1(i, &adapter) != DXGI_ERROR_NOT_FOUND; i++) {
        DXGI_ADAPTER_DESC1 desc;
        adapter->GetDesc1(&desc);
        if (SUCCEEDED(D3D12CreateDevice(adapter.Get(), D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device)))) {
            Log("[D3D12] Using adapter: %S", desc.Description);
            break;
        }
        adapter.Reset();
    }

    if (!g_device) {
        Log("[D3D12] No adapter found, trying default adapter...");
        hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device));
        if (FAILED(hr)) {
            Log("[D3D12] Failed to create D3D12 device: 0x%08X", (unsigned)hr);
            return false;
        }
    }

    // 2. Create command queue
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    queueDesc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
    queueDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
    hr = g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue));
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create command queue: 0x%08X", (unsigned)hr);
        return false;
    }

    // 3. Create swap chain
    DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
    swapChainDesc.BufferCount = 2;
    swapChainDesc.Width = g_width;
    swapChainDesc.Height = g_height;
    swapChainDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
    swapChainDesc.SampleDesc.Count = 1;

    ComPtr<IDXGISwapChain1> swapChain1;
    hr = dxgiFactory->CreateSwapChainForHwnd(
        g_commandQueue.Get(), hwnd, &swapChainDesc,
        nullptr, nullptr, &swapChain1);
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create swap chain: 0x%08X", (unsigned)hr);
        return false;
    }
    swapChain1.As(&g_swapChain);
    g_frameIndex = g_swapChain->GetCurrentBackBufferIndex();

    // 4. Create RTV descriptor heap
    D3D12_DESCRIPTOR_HEAP_DESC rtvHeapDesc = {};
    rtvHeapDesc.NumDescriptors = 2;
    rtvHeapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    rtvHeapDesc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;
    hr = g_device->CreateDescriptorHeap(&rtvHeapDesc, IID_PPV_ARGS(&g_rtvHeap));
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create RTV heap: 0x%08X", (unsigned)hr);
        return false;
    }
    g_rtvDescriptorSize = g_device->GetDescriptorHandleIncrementSize(
        D3D12_DESCRIPTOR_HEAP_TYPE_RTV);

    // 5. Create RTVs for each back buffer
    D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    for (UINT n = 0; n < 2; n++) {
        hr = g_swapChain->GetBuffer(n, IID_PPV_ARGS(&g_renderTargets[n]));
        if (FAILED(hr)) {
            Log("[D3D12] Failed to get back buffer %u: 0x%08X", n, (unsigned)hr);
            return false;
        }
        g_device->CreateRenderTargetView(g_renderTargets[n].Get(), nullptr, rtvHandle);
        rtvHandle.ptr += g_rtvDescriptorSize;
    }

    // 6. Create command allocator and list
    hr = g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
        IID_PPV_ARGS(&g_commandAllocator));
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create command allocator: 0x%08X", (unsigned)hr);
        return false;
    }
    hr = g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
        g_commandAllocator.Get(), nullptr, IID_PPV_ARGS(&g_commandList));
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create command list: 0x%08X", (unsigned)hr);
        return false;
    }
    g_commandList->Close();

    // 7. Create fence
    hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    if (FAILED(hr)) {
        Log("[D3D12] Failed to create fence: 0x%08X", (unsigned)hr);
        return false;
    }
    g_fenceValue = 1;
    g_fenceEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (!g_fenceEvent) {
        Log("[D3D12] Failed to create fence event");
        return false;
    }

    g_initialized = true;
    Log("[D3D12] ==== Initialization Complete ====");
    Log("[D3D12] Resolution: %dx%d", g_width, g_height);
    return true;
}

// ============================================================
// Cleanup
// ============================================================
static void CleanupD3D12() {
    Log("[D3D12] Cleaning up...");
    if (g_initialized) {
        WaitForGPU();
        CloseHandle(g_fenceEvent);
        g_fenceEvent = nullptr;
        for (int i = 0; i < 2; i++) g_renderTargets[i].Reset();
        g_rtvHeap.Reset();
        g_commandList.Reset();
        g_commandAllocator.Reset();
        g_swapChain.Reset();
        g_commandQueue.Reset();
        g_device.Reset();
        g_fence.Reset();
        g_initialized = false;
        Log("[D3D12] Cleanup complete");
    }
}

// ============================================================
// Render Frame - Clear to stored color + Present
// ============================================================
static void DoRender() {
    if (!g_initialized) return;

    HRESULT hr;

    hr = g_commandAllocator->Reset();
    if (FAILED(hr)) return;

    hr = g_commandList->Reset(g_commandAllocator.Get(), nullptr);
    if (FAILED(hr)) return;

    g_frameIndex = g_swapChain->GetCurrentBackBufferIndex();

    // Transition back buffer from PRESENT to RENDER_TARGET
    D3D12_RESOURCE_BARRIER barrier = {};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrier.Flags = D3D12_RESOURCE_BARRIER_FLAG_NONE;
    barrier.Transition.pResource = g_renderTargets[g_frameIndex].Get();
    barrier.Transition.StateBefore = D3D12_RESOURCE_STATE_PRESENT;
    barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_RENDER_TARGET;
    barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    g_commandList->ResourceBarrier(1, &barrier);

    // Clear render target to stored color
    D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle =
        g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    rtvHandle.ptr += (SIZE_T)g_frameIndex * g_rtvDescriptorSize;
    g_commandList->OMSetRenderTargets(1, &rtvHandle, FALSE, nullptr);
    g_commandList->ClearRenderTargetView(rtvHandle, g_clearColor, 0, nullptr);

    // Transition back buffer from RENDER_TARGET to PRESENT
    barrier.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
    barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
    g_commandList->ResourceBarrier(1, &barrier);

    g_commandList->Close();

    // Execute command list
    ID3D12CommandList* cmdLists[] = { g_commandList.Get() };
    g_commandQueue->ExecuteCommandLists(1, cmdLists);

    // Present
    g_swapChain->Present(1, 0);

    // Wait for GPU to finish
    const UINT64 fence = g_fenceValue;
    g_commandQueue->Signal(g_fence.Get(), fence);
    g_fenceValue++;
    if (g_fence->GetCompletedValue() < fence) {
        g_fence->SetEventOnCompletion(fence, g_fenceEvent);
        WaitForSingleObject(g_fenceEvent, INFINITE);
    }
}

// ============================================================
// Resize
// ============================================================
static void DoResize(int width, int height) {
    if (!g_initialized || !g_swapChain) return;
    if (width <= 0 || height <= 0) return;

    Log("[D3D12] Resizing to %dx%d", width, height);
    WaitForGPU();

    for (int i = 0; i < 2; i++) g_renderTargets[i].Reset();
    g_commandList.Reset();
    g_commandAllocator.Reset();

    HRESULT hr = g_swapChain->ResizeBuffers(2, (UINT)width, (UINT)height,
        DXGI_FORMAT_R8G8B8A8_UNORM, 0);
    if (FAILED(hr)) {
        Log("[D3D12] ResizeBuffers failed: 0x%08X", (unsigned)hr);
        return;
    }
    g_width = (UINT)width;
    g_height = (UINT)height;
    g_frameIndex = g_swapChain->GetCurrentBackBufferIndex();

    // Recreate RTVs
    D3D12_CPU_DESCRIPTOR_HANDLE rtvH = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    for (UINT n = 0; n < 2; n++) {
        g_swapChain->GetBuffer(n, IID_PPV_ARGS(&g_renderTargets[n]));
        g_device->CreateRenderTargetView(g_renderTargets[n].Get(), nullptr, rtvH);
        rtvH.ptr += g_rtvDescriptorSize;
    }

    // Recreate command allocator and list
    g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
        IID_PPV_ARGS(&g_commandAllocator));
    g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
        g_commandAllocator.Get(), nullptr, IID_PPV_ARGS(&g_commandList));
    g_commandList->Close();

    Log("[D3D12] Resize complete: %dx%d", g_width, g_height);
}

// ============================================================
// JNI Exports - must match com.dx12.DX12LibClient
// All methods are static, so second param is jclass
// ============================================================

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(
    JNIEnv* env, jclass cls, jlong hwnd, jint width, jint height)
{
    CreateDirectoryA("C:\\temp", nullptr);
    Log("===== nativeInit: hwnd=%lld, %dx%d =====", (long long)hwnd, width, height);

    HWND win = (HWND)(ULONG_PTR)hwnd;
    if (!win || !IsWindow(win)) {
        Log("[D3D12] HWND is 0 or invalid, auto-finding...");
        win = FindMinecraftWindow();
        if (!win) {
            Log("[D3D12] FATAL: Could not find Minecraft window");
            return JNI_FALSE;
        }
    }

    return InitD3D12(win, (int)width, (int)height) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(
    JNIEnv* env, jclass cls)
{
    Log("===== nativeDestroy =====");
    CleanupD3D12();
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(
    JNIEnv* env, jclass cls)
{
    DoRender();
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(
    JNIEnv* env, jclass cls)
{
    if (g_initialized && g_swapChain) {
        g_swapChain->Present(1, 0);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(
    JNIEnv* env, jclass cls, jint width, jint height)
{
    Log("===== nativeResize: %dx%d =====", width, height);
    DoResize((int)width, (int)height);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeSetClearColor(
    JNIEnv* env, jclass cls, jfloat r, jfloat g, jfloat b, jfloat a)
{
    g_clearColor[0] = r;
    g_clearColor[1] = g;
    g_clearColor[2] = b;
    g_clearColor[3] = a;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeIsInitialized(
    JNIEnv* env, jclass cls)
{
    return g_initialized ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
