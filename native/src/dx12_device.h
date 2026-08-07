#pragma once

// dx12-mc 原生 D3D12 层（C++）
// P2 目标：D3D12 设备上下文 + 资源创建（texture/buffer/sampler/view）+ 自检。
// 设计参考：官方 com.mojang.blaze3d.vulkan.VulkanDevice / VulkanGpuTexture /
// VulkanGpuBuffer.Direct / VulkanGpuSampler / VulkanGpuTextureView。

#include <d3d12.h>
#include <dxgi1_4.h>
#include <wrl/client.h>

#include <cstdint>
#include <string>

namespace dx12mc {

using Microsoft::WRL::ComPtr;

// ---------------------------------------------------------------------------
// 设备上下文（进程内单例，Java 侧首次调用 dx12CreateDevice 时创建）
// ---------------------------------------------------------------------------
struct DeviceContext {
    ComPtr<ID3D12Device> device;
    ComPtr<ID3D12CommandQueue> queue;  // P3 提交用，现在先建好

    ComPtr<ID3D12DescriptorHeap> srvHeap;      // SRV（SHADER_VISIBLE）
    ComPtr<ID3D12DescriptorHeap> rtvHeap;      // RTV
    ComPtr<ID3D12DescriptorHeap> dsvHeap;      // DSV
    ComPtr<ID3D12DescriptorHeap> samplerHeap;  // Sampler（SHADER_VISIBLE）

    UINT srvInc = 0, rtvInc = 0, dsvInc = 0, samplerInc = 0;

    std::string adapterName;
    D3D_FEATURE_LEVEL featureLevel = D3D_FEATURE_LEVEL_11_0;
};

// ---------------------------------------------------------------------------
// 资源对象（Java long 句柄 = Dx12Object*）
// ---------------------------------------------------------------------------
struct Dx12Object {
    enum class Kind { Texture, Buffer, Sampler, TextureView };

    Kind kind = Kind::Buffer;
    ComPtr<ID3D12Resource> resource;  // Texture / Buffer
    D3D12_CPU_DESCRIPTOR_HANDLE cpuHandle{};  // View / Sampler 的 CPU 句柄
    D3D12_GPU_DESCRIPTOR_HANDLE gpuHandle{};  // View / Sampler 的 GPU 句柄
    D3D12_HEAP_TYPE heapType = D3D12_HEAP_TYPE_DEFAULT;
    void* mappedPtr = nullptr;  // map 后的指针
    int usage = 0;
    long long size = 0;
};

// ---------------------------------------------------------------------------
// 设备生命周期
// ---------------------------------------------------------------------------
// 创建/复用全局设备上下文；失败时填充 errorOut 并返回 false。
bool ensureDevice(std::string& errorOut);
void destroyDevice();

// ---------------------------------------------------------------------------
// 资源创建（返回 new 的 Dx12Object*，句柄 = 指针；失败返回 nullptr + err）
// ---------------------------------------------------------------------------
Dx12Object* createTexture(int usage, int format, int width, int height,
    int depthOrLayers, int mipLevels, std::string& err);
Dx12Object* createBuffer(int usage, long long size, std::string& err);
Dx12Object* createSampler(int addressU, int addressV, int minFilter,
    int magFilter, int maxAnisotropy, float maxLod, std::string& err);
Dx12Object* createTextureView(Dx12Object* texture, int baseMipLevel,
    int mipLevels, std::string& err);
void destroyObject(Dx12Object* obj);

// ---------------------------------------------------------------------------
// Buffer 映射
// ---------------------------------------------------------------------------
// 对应官方 GpuBuffer.map(offset, length, read, write)：校验 usage 位后
// ID3D12Resource::Map，返回连续指针（供 NewDirectByteBuffer 使用）。
void* mapBuffer(Dx12Object* buffer, long long offset, long long length,
    bool read, bool write, std::string& err);
void unmapBuffer(Dx12Object* buffer);

// ---------------------------------------------------------------------------
// 格式映射（官方 GpuFormat 枚举值 -> DXGI_FORMAT）
// ---------------------------------------------------------------------------
DXGI_FORMAT toDxgiFormat(int gpuFormat);

// 自检：创建 texture/buffer/sampler/view 各一，验证资源层可用后销毁。
// 返回描述字符串（成功/失败明细）。
std::string runResourceSelfTest();

}  // namespace dx12mc
