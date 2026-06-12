#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_4.h>
#include <cstdio>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")

// 鍏ㄥ眬鍙橀噺
static ID3D12Device* g_device = nullptr;
static ID3D12CommandQueue* g_commandQueue = nullptr;
static ID3D12CommandAllocator* g_commandAllocator = nullptr;
static ID3D12GraphicsCommandList* g_commandList = nullptr;
static IDXGISwapChain3* g_swapChain = nullptr;
static HANDLE g_fenceEvent = nullptr;
static ID3D12Fence* g_fence = nullptr;
static UINT64 g_fenceValue = 0;

// 鏃ュ織鏂囦欢
FILE* g_logFile = nullptr;

void Log(const char* format, ...) {
    if (!g_logFile) {
        g_logFile = fopen("C:\\temp\\gl4dx12.log", "a");
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

JNIEXPORT jboolean JNICALL Java_com_dx12_client_D3D12Bridge_nativeInit(JNIEnv* env, jobject obj, jlong hwnd, jint width, jint height) {
    Log("=== D3D12Bridge.nativeInit called ===");
    Log("HWND: %lld, Size: %dx%d", hwnd, width, height);
    
    HRESULT hr;
    
    // 鍒涘缓 D3D12 璁惧
    hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_device));
    if (FAILED(hr)) {
        Log("Failed to create D3D12 device: 0x%08X", hr);
        return JNI_FALSE;
    }
    Log("D3D12 device created");
    
    // 鍒涘缓鍛戒护闃熷垪
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    queueDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
    hr = g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue));
    if (FAILED(hr)) {
        Log("Failed to create command queue: 0x%08X", hr);
        return JNI_FALSE;
    }
    Log("Command queue created");
    
    // 鍒涘缓浜ゆ崲閾?    DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
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
        hr = dxgiFactory->CreateSwapChainForHwnd(
            g_commandQueue,
            (HWND)hwnd,
            &swapChainDesc,
            nullptr,
            nullptr,
            (IDXGISwapChain1**)&g_swapChain
        );
        dxgiFactory->Release();
    }
    
    if (FAILED(hr)) {
        Log("Failed to create swap chain: 0x%08X", hr);
        return JNI_FALSE;
    }
    Log("Swap chain created");
    
    // 鍒涘缓鍛戒护鍒嗛厤鍣?    hr = g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&g_commandAllocator));
    if (FAILED(hr)) {
        Log("Failed to create command allocator: 0x%08X", hr);
        return JNI_FALSE;
    }
    
    // 鍒涘缓鍛戒护鍒楄〃
    hr = g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, g_commandAllocator, nullptr, IID_PPV_ARGS(&g_commandList));
    if (FAILED(hr)) {
        Log("Failed to create command list: 0x%08X", hr);
        return JNI_FALSE;
    }
    g_commandList->Close();
    
    // 鍒涘缓 Fence 鍜屼簨浠?    hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    if (FAILED(hr)) {
        Log("Failed to create fence: 0x%08X", hr);
        return JNI_FALSE;
    }
    g_fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    
    Log("=== D3D12Bridge initialization complete ===");
    
    // 鍥炶皟 Java 璁剧疆鐘舵€?    jclass clazz = env->GetObjectClass(obj);
    jmethodID setInitialized = env->GetMethodID(clazz, "setInitialized", "(Z)V");
    if (setInitialized) {
        env->CallVoidMethod(obj, setInitialized, JNI_TRUE);
    }
    
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_nativeDestroy(JNIEnv* env, jobject obj) {
    Log("D3D12Bridge.nativeDestroy called");
    
    WaitForPreviousFrame();
    
    if (g_swapChain) g_swapChain->Release();
    if (g_commandList) g_commandList->Release();
    if (g_commandAllocator) g_commandAllocator->Release();
    if (g_commandQueue) g_commandQueue->Release();
    if (g_fence) g_fence->Release();
    if (g_fenceEvent) CloseHandle(g_fenceEvent);
    if (g_device) g_device->Release();
    
    g_device = nullptr;
    g_commandQueue = nullptr;
    g_commandAllocator = nullptr;
    g_commandList = nullptr;
    g_swapChain = nullptr;
    g_fence = nullptr;
    g_fenceEvent = nullptr;
    
    Log("D3D12 resources destroyed");
    
    if (g_logFile) {
        fclose(g_logFile);
        g_logFile = nullptr;
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_nativeRender(JNIEnv* env, jobject obj) {
    if (!g_commandAllocator || !g_commandList) return;
    
    g_commandAllocator->Reset();
    g_commandList->Reset(g_commandAllocator, nullptr);
    
    ID3D12Resource* backBuffer = nullptr;
    if (g_swapChain) {
        g_swapChain->GetBuffer(0, IID_PPV_ARGS(&backBuffer));
        if (backBuffer) {
            backBuffer->Release();
        }
    }
    
    g_commandList->Close();
    
    ID3D12CommandList* lists[] = {g_commandList};
    g_commandQueue->ExecuteCommandLists(1, lists);
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_nativePresent(JNIEnv* env, jobject obj) {
    if (g_swapChain) {
        g_swapChain->Present(1, 0);
    }
}

JNIEXPORT void JNICALL Java_com_dx12_client_D3D12Bridge_nativeResize(JNIEnv* env, jobject obj, jint width, jint height) {
    Log("D3D12Bridge.nativeResize: %dx%d", width, height);
    
    if (g_swapChain) {
        WaitForPreviousFrame();
        g_swapChain->ResizeBuffers(2, width, height, DXGI_FORMAT_R8G8B8A8_UNORM, 0);
    }
}

} // extern "C"
