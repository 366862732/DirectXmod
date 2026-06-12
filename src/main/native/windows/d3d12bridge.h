#pragma once

#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <wrl.h>
#include <cstdio>

using namespace Microsoft::WRL;

struct D3D12Context {
    ComPtr<ID3D12Device> device;
    ComPtr<ID3D12CommandQueue> commandQueue;
    ComPtr<ID3D12CommandAllocator> commandAllocator;
    ComPtr<ID3D12GraphicsCommandList> commandList;
    ComPtr<IDXGISwapChain3> swapChain;
    ComPtr<ID3D12Resource> renderTargets[2];
    ComPtr<ID3D12DescriptorHeap> rtvHeap;
    UINT rtvDescriptorSize;
    UINT frameIndex;
    HANDLE fenceEvent;
    ComPtr<ID3D12Fence> fence;
    UINT64 fenceValue;
    bool initialized;
    
    D3D12Context() : rtvDescriptorSize(0), frameIndex(0), fenceValue(0), initialized(false) {
        fenceEvent = nullptr;
    }
};

extern D3D12Context g_ctx;

bool CreateD3D12Device(HWND hwnd, int width, int height);
void CleanupD3D12();
void WaitForPreviousFrame();
void RenderFrame();