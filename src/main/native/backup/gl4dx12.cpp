#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_4.h>
#include <cstdio>
#include <string>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")

static ID3D12Device* g_device = nullptr;
static ID3D12CommandQueue* g_commandQueue = nullptr;
static IDXGISwapChain3* g_swapChain = nullptr;
static ID3D12DescriptorHeap* g_rtvHeap = nullptr;
static ID3D12Resource* g_renderTargets[2] = { nullptr, nullptr };
static ID3D12CommandAllocator* g_commandAllocator = nullptr;
static ID3D12GraphicsCommandList* g_commandList = nullptr;
static ID3D12Fence* g_fence = nullptr;
static HANDLE g_fenceEvent = nullptr;
static UINT64 g_fenceValue = 0;
static int g_frameIndex = 0;

FILE* g_logFile = nullptr;

void Log(const char* format, ...) {
    if (!g_logFile) {
        g_logFile = fopen("D:/gl4dx12_d3d12.log", "a");
    }
    if (g_logFile) {
        va_list args;
        va_start(args, format);
        vfprintf(g_logFile, format, args);
        va_end(args);
        fprintf(g_logFile, "\n");
        fflush(g_logFile);
    }
}

// ?? Minecraft ??
HWND FindMinecraftWindow() {
    HWND hwnd = FindWindowA(NULL, NULL);
    while (hwnd) {
        char windowTitle[256];
        GetWindowTextA(hwnd, windowTitle, sizeof(windowTitle));
        
        // ?????????? Minecraft ????
        std::string title(windowTitle);
        if (title.find("Minecraft") != std::string::npos ||
            title.find("minecraft") != std::string::npos ||
            title.find("craft") != std::string::npos) {
            Log("Found window: %s (HWND: %lld)", windowTitle, (long long)hwnd);
            return hwnd;
        }
        hwnd = GetNextWindow(hwnd, GW_HWNDNEXT);
    }
    return 0;
}

void WaitForGPU() {
    if (g_commandQueue && g_fence) {
        const UINT64 fence = g_fenceValue;
        g_commandQueue->Signal(g_fence, fence);
        g_fenceValue++;
        if (g_fence->GetCompletedValue() < fence) {
            g_fence->SetEventOnCompletion(fence, g_fenceEvent);
            WaitForSingleObject(g_fenceEvent, INFINITE);
        }
    }
}

void CreateRenderTargetViews() {
    DXGI_SWAP_CHAIN_DESC1 swapChainDesc;
    g_swapChain->GetDesc1(&swapChainDesc);
    
    D3D12_DESCRIPTOR_HEAP_DESC rtvHeapDesc = {};
    rtvHeapDesc.NumDescriptors = swapChainDesc.BufferCount;
    rtvHeapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    rtvHeapDesc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;
    g_device->CreateDescriptorHeap(&rtvHeapDesc, IID_PPV_ARGS(&g_rtvHeap));
    
    SIZE_T rtvDescriptorSize = g_device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    
    for (int i = 0; i < swapChainDesc.BufferCount; i++) {
        g_swapChain->GetBuffer(i, IID_PPV_ARGS(&g_renderTargets[i]));
        g_device->CreateRenderTargetView(g_renderTargets[i], nullptr, rtvHandle);
        rtvHandle.ptr += rtvDescriptorSize;
    }
}

void CleanupD3D12() {
    WaitForGPU();
    if (g_rtvHeap) g_rtvHeap->Release();
    for (int i = 0; i < 2; i++) {
        if (g_renderTargets[i]) g_renderTargets[i]->Release();
    }
    if (g_commandAllocator) g_commandAllocator->Release();
    if (g_commandList) g_commandList->Release();
    if (g_swapChain) g_swapChain->Release();
    if (g_commandQueue) g_commandQueue->Release();
    if (g_fence) g_fence->Release();
    if (g_fenceEvent) CloseHandle(g_fenceEvent);
    if (g_device) g_device->Release();
    g_device = nullptr;
    g_commandQueue = nullptr;
    g_swapChain = nullptr;
    g_rtvHeap = nullptr;
    g_commandAllocator = nullptr;
    g_commandList = nullptr;
    g_fence = nullptr;
    g_fenceEvent = nullptr;
    Log("D3D12 cleaned up");
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jclass cls, jlong hwnd, jint width, jint height) {
    Log("=== nativeInit called ===");
    
    // ????? hwnd ???????
    HWND gameHwnd = (HWND)hwnd;
    if (!IsWindow(gameHwnd)) {
        Log("Invalid HWND: %lld, searching for Minecraft window...", (long long)gameHwnd);
        gameHwnd = FindMinecraftWindow();
        if (!gameHwnd) {
            Log("Failed to find Minecraft window!");
            return JNI_FALSE;
        }
        Log("Found Minecraft window: %lld", (long long)gameHwnd);
    }
    
    Log("Using HWND: %lld, Width: %d, Height: %d", (long long)gameHwnd, width, height);
    
    HRESULT hr;
    
    hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device));
    if (FAILED(hr)) {
        Log("Failed to create D3D12 device: 0x%08X", hr);
        return JNI_FALSE;
    }
    Log("D3D12 device created");
    
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    hr = g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue));
    if (FAILED(hr)) {
        Log("Failed to create command queue: 0x%08X", hr);
        return JNI_FALSE;
    }
    Log("Command queue created");
    
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
    if (FAILED(hr)) {
        Log("Failed to create DXGI factory: 0x%08X", hr);
        return JNI_FALSE;
    }
    
    hr = dxgiFactory->CreateSwapChainForHwnd(g_commandQueue, gameHwnd, &swapChainDesc, nullptr, nullptr, (IDXGISwapChain1**)&g_swapChain);
    dxgiFactory->Release();
    
    if (FAILED(hr)) {
        Log("Failed to create swap chain: 0x%08X", hr);
        return JNI_FALSE;
    }
    Log("Swap chain created");
    
    hr = g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&g_commandAllocator));
    if (FAILED(hr)) {
        Log("Failed to create command allocator: 0x%08X", hr);
        return JNI_FALSE;
    }
    
    hr = g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, g_commandAllocator, nullptr, IID_PPV_ARGS(&g_commandList));
    if (FAILED(hr)) {
        Log("Failed to create command list: 0x%08X", hr);
        return JNI_FALSE;
    }
    g_commandList->Close();
    
    CreateRenderTargetViews();
    
    hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    if (FAILED(hr)) {
        Log("Failed to create fence: 0x%08X", hr);
        return JNI_FALSE;
    }
    g_fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    g_fenceValue = 1;
    
    Log("=== nativeInit completed successfully ===");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jclass cls) {
    Log("nativeDestroy called");
    CleanupD3D12();
    if (g_logFile) {
        fclose(g_logFile);
        g_logFile = nullptr;
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jclass cls) {
    if (!g_device || !g_commandAllocator || !g_commandList || !g_swapChain) return;
    
    WaitForGPU();
    
    g_commandAllocator->Reset();
    g_commandList->Reset(g_commandAllocator, nullptr);
    
    g_frameIndex = g_swapChain->GetCurrentBackBufferIndex();
    
    D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    SIZE_T rtvDescriptorSize = g_device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    rtvHandle.ptr += g_frameIndex * rtvDescriptorSize;
    
    float clearColor[] = { 0.1f, 0.2f, 0.6f, 1.0f };
    g_commandList->ClearRenderTargetView(rtvHandle, clearColor, 0, nullptr);
    
    g_commandList->Close();
    
    ID3D12CommandList* lists[] = { g_commandList };
    g_commandQueue->ExecuteCommandLists(1, lists);
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jclass cls) {
    if (!g_swapChain) return;
    g_swapChain->Present(1, 0);
    const UINT64 fence = g_fenceValue;
    g_commandQueue->Signal(g_fence, fence);
    g_fenceValue++;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jclass cls, jint width, jint height) {
    Log("nativeResize: %dx%d", width, height);
    if (g_swapChain && width > 0 && height > 0) {
        WaitForGPU();
        for (int i = 0; i < 2; i++) {
            if (g_renderTargets[i]) {
                g_renderTargets[i]->Release();
                g_renderTargets[i] = nullptr;
            }
        }
        if (g_rtvHeap) {
            g_rtvHeap->Release();
            g_rtvHeap = nullptr;
        }
        g_swapChain->ResizeBuffers(2, width, height, DXGI_FORMAT_R8G8B8A8_UNORM, 0);
        CreateRenderTargetViews();
        Log("Resize completed");
    }
}

}
