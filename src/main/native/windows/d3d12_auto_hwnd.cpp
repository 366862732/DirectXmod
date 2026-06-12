#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_4.h>
#include <glfw3.h>
#include <cstdio>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "glfw3.lib")

// ?? D3D12 ??
static ID3D12Device* g_device = nullptr;
static ID3D12CommandQueue* g_commandQueue = nullptr;
static IDXGISwapChain3* g_swapChain = nullptr;
static ID3D12DescriptorHeap* g_rtvHeap = nullptr;
static ID3D12Resource* g_backBuffers[2] = { nullptr };
static HANDLE g_fenceEvent = nullptr;
static ID3D12Fence* g_fence = nullptr;
static UINT64 g_fenceValue = 0;
static bool g_initialized = false;

// ??
void Log(const char* msg) {
    FILE* f = fopen("C:\\temp\\gl4dx12_debug.log", "a");
    if (f) {
        fprintf(f, "%s\n", msg);
        fclose(f);
    }
}

// ?? Minecraft ????
HWND GetMinecraftWindow() {
    Log("Searching for Minecraft window...");
    
    // ??1: ?? FindWindow ??
    HWND hwnd = FindWindowA(NULL, "Minecraft 26.1.2");
    if (hwnd) {
        Log("Found by title: Minecraft 26.1.2");
        return hwnd;
    }
    
    // ??2: ?????? (LWJGL ????)
    hwnd = FindWindowA("LWJGL", NULL);
    if (hwnd) {
        Log("Found by class: LWJGL");
        return hwnd;
    }
    
    // ??3: ??????????? "Minecraft" ?
    Log("EnumWindows fallback...");
    // ???????????????????
    hwnd = GetForegroundWindow();
    Log("Using foreground window");
    return hwnd;
}

// ?? GPU
void WaitForGPU() {
    if (g_fence && g_commandQueue) {
        const UINT64 fence = g_fenceValue;
        g_commandQueue->Signal(g_fence, fence);
        g_fenceValue++;
        if (g_fence->GetCompletedValue() < fence) {
            g_fence->SetEventOnCompletion(fence, g_fenceEvent);
            WaitForSingleObject(g_fenceEvent, INFINITE);
        }
    }
}

// ????
void CleanupD3D12() {
    if (g_initialized) {
        WaitForGPU();
        for (int i = 0; i < 2; i++) {
            if (g_backBuffers[i]) g_backBuffers[i]->Release();
        }
        if (g_rtvHeap) g_rtvHeap->Release();
        if (g_swapChain) g_swapChain->Release();
        if (g_commandQueue) g_commandQueue->Release();
        if (g_device) g_device->Release();
        if (g_fence) g_fence->Release();
        if (g_fenceEvent) CloseHandle(g_fenceEvent);
        g_initialized = false;
        Log("D3D12 cleaned up");
    }
}

// ??? D3D12
bool InitD3D12(HWND hwnd, int width, int height) {
    Log("Initializing D3D12...");
    HRESULT hr;
    
    // ?? D3D12 ??
    hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device));
    if (FAILED(hr)) {
        Log("Failed to create D3D12 device");
        return false;
    }
    
    // ??????
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    hr = g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue));
    if (FAILED(hr)) {
        Log("Failed to create command queue");
        return false;
    }
    
    // ?????
    DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
    swapChainDesc.BufferCount = 2;
    swapChainDesc.Width = width;
    swapChainDesc.Height = height;
    swapChainDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
    swapChainDesc.SampleDesc.Count = 1;
    
    IDXGIFactory4* dxgiFactory = nullptr;
    hr = CreateDXGIFactory1(IID_PPV_ARGS(&dxgiFactory));
    if (SUCCEEDED(hr)) {
        hr = dxgiFactory->CreateSwapChainForHwnd(g_commandQueue, hwnd, &swapChainDesc, nullptr, nullptr, (IDXGISwapChain1**)&g_swapChain);
        dxgiFactory->Release();
    }
    if (FAILED(hr)) {
        Log("Failed to create swap chain");
        return false;
    }
    
    // ?? RTV ????
    D3D12_DESCRIPTOR_HEAP_DESC rtvHeapDesc = {};
    rtvHeapDesc.NumDescriptors = 2;
    rtvHeapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    hr = g_device->CreateDescriptorHeap(&rtvHeapDesc, IID_PPV_ARGS(&g_rtvHeap));
    if (FAILED(hr)) {
        Log("Failed to create RTV heap");
        return false;
    }
    
    // ?? Fence
    hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    if (FAILED(hr)) {
        Log("Failed to create fence");
        return false;
    }
    g_fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    
    g_initialized = true;
    Log("D3D12 initialized successfully");
    return true;
}

extern "C" {

// ???? nativeInit????????
__declspec(dllexport) jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) {
    Log("=== nativeInit called ===");
    
    // ??????
    HWND hwnd = GetMinecraftWindow();
    if (!hwnd) {
        Log("Failed to find Minecraft window");
        return JNI_FALSE;
    }
    
    char buf[256];
    sprintf_s(buf, "Found HWND: %p", hwnd);
    Log(buf);
    
    // ??????
    RECT rect;
    GetClientRect(hwnd, &rect);
    int width = rect.right - rect.left;
    int height = rect.bottom - rect.top;
    sprintf_s(buf, "Window size: %dx%d", width, height);
    Log(buf);
    
    // ??? D3D12
    if (InitD3D12(hwnd, width, height)) {
        Log("D3D12 initialization complete");
        return JNI_TRUE;
    }
    
    Log("D3D12 initialization failed");
    return JNI_FALSE;
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {
    Log("nativeDestroy called");
    CleanupD3D12();
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {
    if (!g_initialized || !g_swapChain) return;
    
    // ?????
    // TODO: ?????????
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {
    if (g_swapChain) {
        g_swapChain->Present(1, 0);
    }
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj, jint width, jint height) {
    // TODO: ????????
}

}