// d3d12bridge.cpp
// ? AI-D3D12 ?? - ??? D3D12 ??
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <d3dcompiler.h>
#include <cstdio>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "d3dcompiler.lib")
#pragma comment(lib, "user32.lib")

static ID3D12Device* g_device = nullptr;
static ID3D12CommandQueue* g_commandQueue = nullptr;
static ID3D12CommandAllocator* g_commandAllocator = nullptr;
static ID3D12GraphicsCommandList* g_commandList = nullptr;
static IDXGISwapChain3* g_swapChain = nullptr;
static ID3D12Resource* g_renderTargets[2] = { nullptr, nullptr };
static ID3D12DescriptorHeap* g_rtvHeap = nullptr;
static UINT g_rtvDescriptorSize = 0;
static UINT g_frameIndex = 0;
static HANDLE g_fenceEvent = nullptr;
static ID3D12Fence* g_fence = nullptr;
static UINT64 g_fenceValue = 0;
static HWND g_hwnd = nullptr;

LRESULT CALLBACK WindowProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
    case WM_DESTROY:
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProc(hwnd, msg, wParam, lParam);
}

HWND CreateTestWindow(HINSTANCE hInstance) {
    const wchar_t CLASS_NAME[] = L"D3D12BridgeWindow";
    WNDCLASS wc = {};
    wc.lpfnWndProc = WindowProc;
    wc.hInstance = hInstance;
    wc.lpszClassName = CLASS_NAME;
    RegisterClass(&wc);
    HWND hwnd = CreateWindowEx(0, CLASS_NAME, L"D3D12 Bridge", WS_OVERLAPPEDWINDOW,
        CW_USEDEFAULT, CW_USEDEFAULT, 800, 600,
        nullptr, nullptr, hInstance, nullptr);
    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);
    return hwnd;
}

void EnableDebugLayer() {
#if defined(_DEBUG)
    ID3D12Debug* debugController = nullptr;
    if (SUCCEEDED(D3D12GetDebugInterface(IID_PPV_ARGS(&debugController)))) {
        debugController->EnableDebugLayer();
        debugController->Release();
    }
#endif
}

bool CreateD3D12Device() {
    EnableDebugLayer();
    IDXGIFactory4* dxgiFactory = nullptr;
    HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(&dxgiFactory));
    if (FAILED(hr)) return false;
    IDXGIAdapter1* adapter = nullptr;
    for (UINT i = 0; dxgiFactory->EnumAdapters1(i, &adapter) != DXGI_ERROR_NOT_FOUND; ++i) {
        DXGI_ADAPTER_DESC1 desc;
        adapter->GetDesc1(&desc);
        if (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) {
            adapter->Release();
            continue;
        }
        if (SUCCEEDED(D3D12CreateDevice(adapter, D3D_FEATURE_LEVEL_12_0, _uuidof(ID3D12Device), nullptr))) {
            break;
        }
        adapter->Release();
    }
    if (!adapter) return false;
    hr = D3D12CreateDevice(adapter, D3D_FEATURE_LEVEL_12_0, IID_PPV_ARGS(&g_device));
    adapter->Release();
    dxgiFactory->Release();
    return SUCCEEDED(hr);
}

bool CreateCommandQueue() {
    D3D12_COMMAND_QUEUE_DESC queueDesc = {};
    queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    return SUCCEEDED(g_device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&g_commandQueue)));
}

bool CreateSwapChain(HWND hwnd, int width, int height) {
    g_hwnd = hwnd;
    IDXGIFactory4* dxgiFactory = nullptr;
    CreateDXGIFactory1(IID_PPV_ARGS(&dxgiFactory));
    DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
    swapChainDesc.BufferCount = 2;
    swapChainDesc.Width = width;
    swapChainDesc.Height = height;
    swapChainDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
    swapChainDesc.SampleDesc.Count = 1;
    IDXGISwapChain1* swapChain1 = nullptr;
    HRESULT hr = dxgiFactory->CreateSwapChainForHwnd(g_commandQueue, hwnd, &swapChainDesc, nullptr, nullptr, &swapChain1);
    if (SUCCEEDED(hr)) {
        swapChain1->QueryInterface(IID_PPV_ARGS(&g_swapChain));
        swapChain1->Release();
        g_frameIndex = g_swapChain->GetCurrentBackBufferIndex();
    }
    dxgiFactory->Release();
    return SUCCEEDED(hr);
}

bool CreateRTVDescriptorHeap() {
    D3D12_DESCRIPTOR_HEAP_DESC rtvHeapDesc = {};
    rtvHeapDesc.NumDescriptors = 2;
    rtvHeapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    HRESULT hr = g_device->CreateDescriptorHeap(&rtvHeapDesc, IID_PPV_ARGS(&g_rtvHeap));
    if (SUCCEEDED(hr)) {
        g_rtvDescriptorSize = g_device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
    }
    return SUCCEEDED(hr);
}

bool CreateRenderTargets() {
    D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    for (UINT i = 0; i < 2; ++i) {
        HRESULT hr = g_swapChain->GetBuffer(i, IID_PPV_ARGS(&g_renderTargets[i]));
        if (FAILED(hr)) return false;
        g_device->CreateRenderTargetView(g_renderTargets[i], nullptr, rtvHandle);
        rtvHandle.ptr += g_rtvDescriptorSize;
    }
    return true;
}

bool CreateCommandObjects() {
    HRESULT hr = g_device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&g_commandAllocator));
    if (FAILED(hr)) return false;
    hr = g_device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, g_commandAllocator, nullptr, IID_PPV_ARGS(&g_commandList));
    if (SUCCEEDED(hr)) g_commandList->Close();
    return SUCCEEDED(hr);
}

bool CreateFence() {
    HRESULT hr = g_device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&g_fence));
    if (SUCCEEDED(hr)) {
        g_fenceEvent = CreateEvent(nullptr, FALSE, FALSE, nullptr);
    }
    return SUCCEEDED(hr) && g_fenceEvent;
}

void WaitForPreviousFrame() {
    const UINT64 fence = g_fenceValue;
    g_commandQueue->Signal(g_fence, fence);
    g_fenceValue++;
    if (g_fence->GetCompletedValue() < fence) {
        g_fence->SetEventOnCompletion(fence, g_fenceEvent);
        WaitForSingleObject(g_fenceEvent, INFINITE);
    }
    g_frameIndex = g_swapChain->GetCurrentBackBufferIndex();
}

void ClearRenderTarget() {
    g_commandAllocator->Reset();
    g_commandList->Reset(g_commandAllocator, nullptr);
    D3D12_RESOURCE_BARRIER barrier = {};
    barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
    barrier.Transition.pResource = g_renderTargets[g_frameIndex];
    barrier.Transition.StateBefore = D3D12_RESOURCE_STATE_PRESENT;
    barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_RENDER_TARGET;
    barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
    g_commandList->ResourceBarrier(1, &barrier);
    D3D12_CPU_DESCRIPTOR_HANDLE rtvHandle = g_rtvHeap->GetCPUDescriptorHandleForHeapStart();
    rtvHandle.ptr += g_frameIndex * g_rtvDescriptorSize;
    const float clearColor[] = { 1.0f, 0.0f, 0.0f, 1.0f };
    g_commandList->ClearRenderTargetView(rtvHandle, clearColor, 0, nullptr);
    barrier.Transition.StateBefore = D3D12_RESOURCE_STATE_RENDER_TARGET;
    barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_PRESENT;
    g_commandList->ResourceBarrier(1, &barrier);
    g_commandList->Close();
    ID3D12CommandList* commandLists[] = { g_commandList };
    g_commandQueue->ExecuteCommandLists(1, commandLists);
    g_swapChain->Present(1, 0);
    WaitForPreviousFrame();
}

void Cleanup() {
    if (g_fence) WaitForPreviousFrame();
    if (g_commandList) g_commandList->Release();
    if (g_commandAllocator) g_commandAllocator->Release();
    if (g_commandQueue) g_commandQueue->Release();
    for (UINT i = 0; i < 2; ++i) if (g_renderTargets[i]) g_renderTargets[i]->Release();
    if (g_swapChain) g_swapChain->Release();
    if (g_rtvHeap) g_rtvHeap->Release();
    if (g_fence) g_fence->Release();
    if (g_fenceEvent) CloseHandle(g_fenceEvent);
    if (g_device) g_device->Release();
    if (g_hwnd) DestroyWindow(g_hwnd);
}

extern "C" __declspec(dllexport) bool nativeRender() {
    HINSTANCE hInstance = GetModuleHandle(nullptr);
    HWND hwnd = CreateTestWindow(hInstance);
    if (!hwnd) return false;
    if (!CreateD3D12Device() || !CreateCommandQueue()) { Cleanup(); return false; }
    RECT rect; GetClientRect(hwnd, &rect);
    if (!CreateSwapChain(hwnd, rect.right - rect.left, rect.bottom - rect.top)) { Cleanup(); return false; }
    if (!CreateRTVDescriptorHeap() || !CreateRenderTargets()) { Cleanup(); return false; }
    if (!CreateCommandObjects() || !CreateFence()) { Cleanup(); return false; }
    ClearRenderTarget();
    MSG msg = {};
    DWORD startTime = GetTickCount();
    while (GetTickCount() - startTime < 3000) {
        while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    }
    Cleanup();
    return true;
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    return TRUE;
}
