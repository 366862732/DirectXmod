#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_4.h>
#include <cstdio>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")

// 全局变量
static ID3D12Device* g_device = nullptr;
static ID3D12CommandQueue* g_commandQueue = nullptr;
static ID3D12CommandAllocator* g_commandAllocator = nullptr;
static ID3D12GraphicsCommandList* g_commandList = nullptr;
static IDXGISwapChain3* g_swapChain = nullptr;
static HANDLE g_fenceEvent = nullptr;
static ID3D12Fence* g_fence = nullptr;
static UINT64 g_fenceValue = 0;
static bool g_initialized = false;

// 日志
void Log(const char* msg) {
    FILE* log = fopen("C:\\temp\\d3d12_log.txt", "a");
    if (log) {
        fprintf(log, "%s\n", msg);
        fclose(log);
    }
}

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

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj, jlong hwnd, jint width, jint height) {
    Log("=== nativeInit called ===");
    
    HRESULT hr;
    
    // 创建设备
    hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device));
    if (FAILED(hr)) {
        Log("Failed to create D3D12 device");
        return JNI_FALSE;
    }
    
    // 创建命令队列
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    hr = g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue));
    if (FAILED(hr)) {
        Log("Failed to create command queue");
        return JNI_FALSE;
    }
    
    // 创建交换链
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
        hr = dxgiFactory->CreateSwapChainForHwnd(g_commandQueue, (HWND)hwnd, &swapChainDesc, nullptr, nullptr, (IDXGISwapChain1**)&g_swapChain);
        dxgiFactory->Release();
    }
    if (FAILED(hr)) {
        Log("Failed to create swap chain");
        return JNI_FALSE;
    }
    
    // 创建命令分配器和列表
    hr = g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&g_commandAllocator));
    hr = g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, g_commandAllocator, nullptr, IID_PPV_ARGS(&g_commandList));
    g_commandList->Close();
    
    // 创建 Fence
    hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    g_fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    
    g_initialized = true;
    Log("=== D3D12 initialized successfully ===");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {
    if (!g_initialized) return;
    
    // 获取后台缓冲区并清屏为蓝色
    ID3D12Resource* backBuffer = nullptr;
    if (g_swapChain) {
        g_swapChain->GetBuffer(0, IID_PPV_ARGS(&backBuffer));
        
        // 清屏为蓝色
        float clearColor[] = {0.0f, 0.2f, 0.6f, 1.0f};  // 亮蓝色
        // TODO: 需要创建 RTV 描述符堆才能真正清屏
        
        backBuffer->Release();
    }
    
    // Present
    if (g_swapChain) {
        g_swapChain->Present(1, 0);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {
    WaitForGPU();
    // 释放资源...
    g_initialized = false;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj) {}

}