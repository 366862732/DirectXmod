#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_4.h>
#include <cstdio>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")

// ?? D3D12 ??
static ID3D12Device* g_device = nullptr;
static ID3D12CommandQueue* g_commandQueue = nullptr;
static IDXGISwapChain3* g_swapChain = nullptr;
static ID3D12DescriptorHeap* g_rtvHeap = nullptr;
static UINT g_rtvDescriptorSize = 0;
static ID3D12Resource* g_backBuffers[2] = { nullptr, nullptr };
static int g_bufferIndex = 0;
static HANDLE g_fenceEvent = nullptr;
static ID3D12Fence* g_fence = nullptr;
static UINT64 g_fenceValue = 0;
static bool g_initialized = false;

void Log(const char* msg) {
    FILE* log = fopen("C:\\temp\\d3d12_clear.log", "a");
    if (log) {
        fprintf(log, "%s\n", msg);
        fclose(log);
    }
}

void WaitForPreviousFrame() {
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
    
    // ????
    hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device));
    if (FAILED(hr)) {
        Log("Failed to create D3D12 device");
        return JNI_FALSE;
    }
    
    // ??????
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    hr = g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue));
    if (FAILED(hr)) {
        Log("Failed to create command queue");
        return JNI_FALSE;
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
        hr = dxgiFactory->CreateSwapChainForHwnd(g_commandQueue, (HWND)hwnd, &swapChainDesc, nullptr, nullptr, (IDXGISwapChain1**)&g_swapChain);
        dxgiFactory->Release();
    }
    if (FAILED(hr)) {
        Log("Failed to create swap chain");
        return JNI_FALSE;
    }
    
    // ?? RTV ????
    D3D12_DESCRIPTOR_HEAP_DESC rtvHeapDesc = {};
    rtvHeapDesc.NumDescriptors = 2;
    rtvHeapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    rtvHeapDesc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;
    hr = g_device->CreateDescriptorHeap(&rtvHeapDesc, IID_PPV_ARGS(&g_rtvHeap));
    if (FAILED(hr)) {
        Log("Failed to create RTV heap");
        return JNI_FALSE;
    }
    g_rtvDescriptorSize = g_device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    
    // ?????????
    for (int i = 0; i < 2; i++) {
        hr = g_swapChain->GetBuffer(i, IID_PPV_ARGS(&g_backBuffers[i]));
        if (FAILED(hr)) {
            Log("Failed to get back buffer");
            return JNI_FALSE;
        }
        D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
        rtvHandle.ptr += i * g_rtvDescriptorSize;
        g_device->CreateRenderTargetView(g_backBuffers[i], nullptr, rtvHandle);
    }
    
    // ?? Fence ???
    hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    if (FAILED(hr)) {
        Log("Failed to create fence");
        return JNI_FALSE;
    }
    g_fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    
    g_initialized = true;
    Log("D3D12 initialized successfully");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {
    if (!g_initialized) return;
    
    // ???????
    WaitForPreviousFrame();
    
    // ???????????
    g_bufferIndex = g_swapChain->GetCurrentBackBufferIndex();
    
    // ????????????????????
    ID3D12CommandAllocator* commandAllocator = nullptr;
    ID3D12GraphicsCommandList* commandList = nullptr;
    HRESULT hr = g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&commandAllocator));
    if (SUCCEEDED(hr)) {
        hr = g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, commandAllocator, nullptr, IID_PPV_ARGS(&commandList));
        if (SUCCEEDED(hr)) {
            // ?????
            float clearColor[] = { 0.0f, 0.2f, 0.6f, 1.0f }; // ???
            D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
            rtvHandle.ptr += g_bufferIndex * g_rtvDescriptorSize;
            commandList->ClearRenderTargetView(rtvHandle, clearColor, 0, nullptr);
            commandList->Close();
            
            // ??????
            ID3D12CommandList* lists[] = { commandList };
            g_commandQueue->ExecuteCommandLists(1, lists);
        }
        commandAllocator->Release();
    }
    if (commandList) commandList->Release();
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {
    if (g_swapChain) {
        g_swapChain->Present(1, 0); // ????
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {
    if (g_initialized) {
        WaitForPreviousFrame();
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
        Log("D3D12 destroyed");
    }
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj, jint width, jint height) {
    if (g_initialized && g_swapChain) {
        WaitForPreviousFrame();
        for (int i = 0; i < 2; i++) {
            if (g_backBuffers[i]) {
                g_backBuffers[i]->Release();
                g_backBuffers[i] = nullptr;
            }
        }
        g_swapChain->ResizeBuffers(2, width, height, DXGI_FORMAT_R8G8B8A8_UNORM, 0);
        // ???? RTV ??
        for (int i = 0; i < 2; i++) {
            g_swapChain->GetBuffer(i, IID_PPV_ARGS(&g_backBuffers[i]));
            D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
            rtvHandle.ptr += i * g_rtvDescriptorSize;
            g_device->CreateRenderTargetView(g_backBuffers[i], nullptr, rtvHandle);
        }
        Log("Resized");
    }
}

}