#include "dx11_device.h"

#include <algorithm>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <fstream>
#include <limits>
#include <thread>
#include <vector>

namespace dx11mc {
namespace {

struct FormatInfo {
    DXGI_FORMAT resource = DXGI_FORMAT_UNKNOWN;
    DXGI_FORMAT srv = DXGI_FORMAT_UNKNOWN;
    DXGI_FORMAT dsv = DXGI_FORMAT_UNKNOWN;
    UINT bytesPerPixel = 4;
    bool depth = false;
};

FormatInfo formatInfo(int format) {
    switch (format) {
        case 0:  // RGBA8
            return {DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_R8G8B8A8_UNORM,
                    DXGI_FORMAT_UNKNOWN, 4, false};
        case 1:  // RED8
            return {DXGI_FORMAT_R8_UNORM, DXGI_FORMAT_R8_UNORM,
                    DXGI_FORMAT_UNKNOWN, 1, false};
        case 2:  // RED8I (GL_R8I, used by CloudFaces)
            return {DXGI_FORMAT_R8_SINT, DXGI_FORMAT_R8_SINT,
                    DXGI_FORMAT_UNKNOWN, 1, false};
        case 3:  // DEPTH32
            return {DXGI_FORMAT_R32_TYPELESS, DXGI_FORMAT_R32_FLOAT,
                    DXGI_FORMAT_D32_FLOAT, 4, true};
        default:
            return {};
    }
}

UINT mipDimension(UINT base, UINT mip) {
    const UINT value = base >> mip;
    return value == 0 ? 1 : value;
}

bool needsShadow(int usage) {
    // USAGE_MAP_READ | USAGE_UNIFORM | USAGE_UNIFORM_TEXEL_BUFFER
    // COPY_DST alone does not need a CPU copy: full-buffer writes go straight
    // to UpdateSubresource, and partial writes use a temporary staging copy.
    return (usage & (1 | 128 | 256)) != 0;
}

std::string compileError(const char* stage, HRESULT hr, ID3DBlob* messages) {
    std::string message = "D3DCompile(";
    message += stage;
    message += ") failed: ";
    message += hresultToString(hr);
    if (messages) {
        const char* text = static_cast<const char*>(messages->GetBufferPointer());
        const size_t size = messages->GetBufferSize();
        size_t length = 0;
        while (length < size && text[length] != '\0') {
            ++length;
        }
        if (length > 0) {
            message += " - ";
            message.append(text, length);
        }
    }
    return message;
}

bool compileShader(
    D3DCompiler& compiler,
    const std::string& source,
    const char* sourceName,
    const char* entryPoint,
    const char* target,
    ComPtr<ID3DBlob>& bytecode,
    std::string& error) {
    ComPtr<ID3DBlob> messages;
    const HRESULT hr = compiler.compile(
        source.data(),
        source.size(),
        sourceName,
        nullptr,
        nullptr,
        entryPoint,
        target,
        0,
        0,
        bytecode.GetAddressOf(),
        messages.GetAddressOf());
    if (SUCCEEDED(hr)) {
        return true;
    }
    error = compileError(target, hr, messages.Get());
    return false;
}

bool compileStage(
    D3DCompiler& compiler,
    const std::string& source,
    const std::string& name,
    const char* entryPoint,
    bool vertex,
    D3D_FEATURE_LEVEL featureLevel,
    ComPtr<ID3DBlob>& bytecode,
    std::string& error) {
    const std::string sourceName = name.empty() ? "pipeline" : name;
    if (compileShader(compiler, source, sourceName.c_str(), entryPoint,
                      vertex ? "vs_4_0" : "ps_4_0", bytecode, error)) {
        return true;
    }
    if (featureLevel >= D3D_FEATURE_LEVEL_10_1) {
        // SM4.1 is available on feature level 10_1 and can compile a few
        // constructs (for example Gather) that SM4.0 rejects.
        std::string secondError;
        if (compileShader(compiler, source, sourceName.c_str(), entryPoint,
                          vertex ? "vs_4_1" : "ps_4_1", bytecode, secondError)) {
            return true;
        }
        error += "\n";
        error += secondError;
    }
    return false;
}

// Detaches every pixel/vertex shader resource and sampler.  D3D11 forbids a
// resource from being bound as an SRV and a render target at the same time.
void unbindPixelAndVertexResources(Device* device) {
    ID3D11ShaderResourceView* nullSrvs[D3D11_COMMONSHADER_INPUT_RESOURCE_SLOT_COUNT] = {};
    ID3D11SamplerState* nullSamplers[D3D11_COMMONSHADER_SAMPLER_SLOT_COUNT] = {};
    device->context->PSSetShaderResources(0, D3D11_COMMONSHADER_INPUT_RESOURCE_SLOT_COUNT, nullSrvs);
    device->context->VSSetShaderResources(0, D3D11_COMMONSHADER_INPUT_RESOURCE_SLOT_COUNT, nullSrvs);
    device->context->PSSetSamplers(0, D3D11_COMMONSHADER_SAMPLER_SLOT_COUNT, nullSamplers);
    device->context->VSSetSamplers(0, D3D11_COMMONSHADER_SAMPLER_SLOT_COUNT, nullSamplers);
}

void setDefaultViewport(Device* device, UINT width, UINT height) {
    D3D11_VIEWPORT viewport{};
    viewport.TopLeftX = 0.0f;
    viewport.TopLeftY = 0.0f;
    viewport.Width = static_cast<FLOAT>(width);
    viewport.Height = static_cast<FLOAT>(height);
    viewport.MinDepth = 0.0f;
    viewport.MaxDepth = 1.0f;
    device->context->RSSetViewports(1, &viewport);
}

void setFullScissor(Device* device, UINT width, UINT height) {
    D3D11_RECT rect{};
    rect.left = 0;
    rect.top = 0;
    rect.right = static_cast<LONG>(width);
    rect.bottom = static_cast<LONG>(height);
    device->context->RSSetScissorRects(1, &rect);
}

void releaseBackBuffer(Device* device) {
    device->context->OMSetRenderTargets(0, nullptr, nullptr);
    device->backBufferRtv.Reset();
    device->backBuffer.Reset();
}

bool createBackBufferViews(Device* device, std::string& error) {
    device->backBufferRtv.Reset();
    device->backBuffer.Reset();
    HRESULT hr = device->swapChain->GetBuffer(0, IID_PPV_ARGS(device->backBuffer.GetAddressOf()));
    if (FAILED(hr)) {
        error = "GetBuffer failed: " + hresultToString(hr);
        return false;
    }
    hr = device->device->CreateRenderTargetView(
        device->backBuffer.Get(), nullptr, device->backBufferRtv.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateRenderTargetView failed: " + hresultToString(hr);
        return false;
    }
    return true;
}

bool createSwapChain(Device* device, UINT width, UINT height, std::string& error) {
    DXGI_SWAP_CHAIN_DESC desc{};
    desc.BufferCount = 2;
    desc.BufferDesc.Width = width;
    desc.BufferDesc.Height = height;
    desc.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    desc.BufferDesc.RefreshRate.Numerator = 0;
    desc.BufferDesc.RefreshRate.Denominator = 0;
    desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    desc.OutputWindow = device->hwnd;
    desc.SampleDesc.Count = 1;
    desc.Windowed = TRUE;
    desc.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;

    HRESULT hr = device->factory->CreateSwapChain(
        device->device.Get(), &desc, device->swapChain.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateSwapChain failed: " + hresultToString(hr);
        return false;
    }

    // Minecraft toggles fullscreen by changing the window style; do not let
    // DXGI swallow Alt+Enter.
    device->factory->MakeWindowAssociation(device->hwnd, DXGI_MWA_NO_ALT_ENTER);

    device->swapWidth = width;
    device->swapHeight = height;
    return createBackBufferViews(device, error);
}

bool resizeSwapChain(Device* device, UINT width, UINT height, std::string& error) {
    releaseBackBuffer(device);
    device->context->Flush();

    HRESULT hr = device->swapChain->ResizeBuffers(
        0, width, height, DXGI_FORMAT_UNKNOWN, 0);
    if (FAILED(hr)) {
        // Some drivers refuse ResizeBuffers while a reference is still alive.
        // Recreating the swapchain is a safe fallback.
        device->swapChain.Reset();
        device->swapWidth = 0;
        device->swapHeight = 0;
        return createSwapChain(device, width, height, error);
    }

    device->swapWidth = width;
    device->swapHeight = height;
    return createBackBufferViews(device, error);
}

bool ensureSwapChain(Device* device, UINT width, UINT height, std::string& error) {
    if (width == 0 || height == 0) {
        return true;
    }
    if (!device->swapChain) {
        return createSwapChain(device, width, height, error);
    }
    if (width != device->swapWidth || height != device->swapHeight) {
        return resizeSwapChain(device, width, height, error);
    }
    return true;
}

const char* const kBlitVertexShader =
    "struct VSOutput { float4 position : SV_Position; float2 uv : TEXCOORD0; };\n"
    "VSOutput main(uint id : SV_VertexID) {\n"
    "    float2 uv = float2((id << 1) & 2, id & 2);\n"
    "    VSOutput output;\n"
    "    output.position = float4(uv * float2(2.0, -2.0) + float2(-1.0, 1.0), 0.0, 1.0);\n"
    "    output.uv = float2(uv.x, 1.0 - uv.y);\n"
    "    return output;\n"
    "}\n";

const char* const kBlitPixelShader =
    "Texture2D source : register(t0);\n"
    "SamplerState sourceSampler : register(s0);\n"
    "float4 main(float4 position : SV_Position, float2 uv : TEXCOORD0) : SV_Target {\n"
    "    float4 color = source.Sample(sourceSampler, uv);\n"
    "    return float4(color.rgb, 1.0);\n"
    "}\n";

// Rect-aware clear fallback for feature level 10.x.  The pixel shader writes
// both the clear colour and SV_Depth, so one draw resets colour and depth in
// the scissor rectangle (used by the GUI item atlas).
const char* const kClearVertexShader =
    "struct VSOutput { float4 position : SV_Position; };\n"
    "VSOutput main(uint id : SV_VertexID) {\n"
    "    float2 uv = float2((id << 1) & 2, id & 2);\n"
    "    VSOutput output;\n"
    "    output.position = float4(uv * float2(2.0, -2.0) + float2(-1.0, 1.0), 0.0, 1.0);\n"
    "    return output;\n"
    "}\n";

const char* const kClearPixelShader =
    "cbuffer ClearConstants : register(b0) {\n"
    "    float4 clearColor;\n"
    "    float clearDepthValue;\n"
    "    float3 clearPadding;\n"
    "};\n"
    "struct PSOutput { float4 color : SV_Target; float depth : SV_Depth; };\n"
    "PSOutput main(float4 position : SV_Position) {\n"
    "    PSOutput output;\n"
    "    output.color = clearColor;\n"
    "    output.depth = clearDepthValue;\n"
    "    return output;\n"
    "}\n";

bool createBlitResources(Device* device, std::string& error) {
    ComPtr<ID3DBlob> vsBlob;
    ComPtr<ID3DBlob> psBlob;
    if (!compileShader(device->compiler, kBlitVertexShader, "dx11_present.vs",
                       "main", "vs_4_0", vsBlob, error)) {
        return false;
    }
    if (!compileShader(device->compiler, kBlitPixelShader, "dx11_present.ps",
                       "main", "ps_4_0", psBlob, error)) {
        return false;
    }

    HRESULT hr = device->device->CreateVertexShader(
        vsBlob->GetBufferPointer(), vsBlob->GetBufferSize(), nullptr,
        device->blitVs.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateVertexShader(blit) failed: " + hresultToString(hr);
        return false;
    }
    hr = device->device->CreatePixelShader(
        psBlob->GetBufferPointer(), psBlob->GetBufferSize(), nullptr,
        device->blitPs.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreatePixelShader(blit) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_SAMPLER_DESC sampler{};
    sampler.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
    sampler.AddressU = D3D11_TEXTURE_ADDRESS_CLAMP;
    sampler.AddressV = D3D11_TEXTURE_ADDRESS_CLAMP;
    sampler.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
    sampler.ComparisonFunc = D3D11_COMPARISON_ALWAYS;
    sampler.MinLOD = 0.0f;
    sampler.MaxLOD = D3D11_FLOAT32_MAX;
    hr = device->device->CreateSamplerState(&sampler, device->blitSampler.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateSamplerState(blit) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_BLEND_DESC blend{};
    blend.RenderTarget[0].BlendEnable = FALSE;
    blend.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
    hr = device->device->CreateBlendState(&blend, device->blitBlend.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateBlendState(blit) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_DEPTH_STENCIL_DESC depth{};
    depth.DepthEnable = FALSE;
    depth.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
    depth.DepthFunc = D3D11_COMPARISON_ALWAYS;
    depth.StencilEnable = FALSE;
    hr = device->device->CreateDepthStencilState(&depth, device->blitDepth.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateDepthStencilState(blit) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_RASTERIZER_DESC raster{};
    raster.FillMode = D3D11_FILL_SOLID;
    raster.CullMode = D3D11_CULL_NONE;
    raster.FrontCounterClockwise = FALSE;
    raster.DepthClipEnable = TRUE;
    raster.ScissorEnable = FALSE;
    hr = device->device->CreateRasterizerState(&raster, device->blitRaster.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateRasterizerState(blit) failed: " + hresultToString(hr);
        return false;
    }

    // --- Rect-clear fallback resources -----------------------------------
    vsBlob.Reset();
    psBlob.Reset();
    if (!compileShader(device->compiler, kClearVertexShader, "dx11_clear.vs",
                       "main", "vs_4_0", vsBlob, error)) {
        return false;
    }
    if (!compileShader(device->compiler, kClearPixelShader, "dx11_clear.ps",
                       "main", "ps_4_0", psBlob, error)) {
        return false;
    }
    hr = device->device->CreateVertexShader(
        vsBlob->GetBufferPointer(), vsBlob->GetBufferSize(), nullptr,
        device->clearVs.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateVertexShader(clear) failed: " + hresultToString(hr);
        return false;
    }
    hr = device->device->CreatePixelShader(
        psBlob->GetBufferPointer(), psBlob->GetBufferSize(), nullptr,
        device->clearPs.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreatePixelShader(clear) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_BUFFER_DESC clearBuffer{};
    clearBuffer.ByteWidth = 32;  // float4 color + float depth + 3 floats padding
    clearBuffer.Usage = D3D11_USAGE_DYNAMIC;
    clearBuffer.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
    clearBuffer.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
    hr = device->device->CreateBuffer(
        &clearBuffer, nullptr, device->clearConstants.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateBuffer(clear constants) failed: " + hresultToString(hr);
        return false;
    }

    hr = device->device->CreateBlendState(&blend, device->clearBlend.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateBlendState(clear) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_DEPTH_STENCIL_DESC clearDepth{};
    clearDepth.DepthEnable = TRUE;
    clearDepth.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
    clearDepth.DepthFunc = D3D11_COMPARISON_ALWAYS;
    clearDepth.StencilEnable = FALSE;
    hr = device->device->CreateDepthStencilState(
        &clearDepth, device->clearDepth.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateDepthStencilState(clear) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_RASTERIZER_DESC clearRaster{};
    clearRaster.FillMode = D3D11_FILL_SOLID;
    clearRaster.CullMode = D3D11_CULL_NONE;
    clearRaster.FrontCounterClockwise = FALSE;
    clearRaster.DepthClipEnable = TRUE;
    clearRaster.ScissorEnable = TRUE;
    hr = device->device->CreateRasterizerState(
        &clearRaster, device->clearRaster.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateRasterizerState(clear) failed: " + hresultToString(hr);
        return false;
    }
    return true;
}

std::string vendorName(UINT vendorId) {
    switch (vendorId) {
        case 0x8086: return "Intel";
        case 0x10DE: return "NVIDIA";
        case 0x1002: return "AMD";
        case 0x1414: return "Microsoft";
        default: return "Unknown";
    }
}

}  // namespace

// ---------------------------------------------------------------------------
// Device / swapchain
// ---------------------------------------------------------------------------
Device* createDevice(HWND hwnd, std::string& error) {
    ComPtr<IDXGIFactory1> factory;
    HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(factory.GetAddressOf()));
    if (FAILED(hr)) {
        error = "CreateDXGIFactory1 failed: " + hresultToString(hr);
        return nullptr;
    }

    ComPtr<IDXGIAdapter1> adapter;
    ComPtr<IDXGIAdapter1> softwareAdapter;
    for (UINT index = 0;; ++index) {
        ComPtr<IDXGIAdapter1> candidate;
        hr = factory->EnumAdapters1(index, candidate.GetAddressOf());
        if (hr == DXGI_ERROR_NOT_FOUND) {
            break;
        }
        if (FAILED(hr)) {
            error = "EnumAdapters1 failed: " + hresultToString(hr);
            return nullptr;
        }
        DXGI_ADAPTER_DESC1 desc{};
        if (FAILED(candidate->GetDesc1(&desc))) {
            continue;
        }
        if (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) {
            // Keep the first render-only adapter as a fallback. GPU-less CI
            // machines and some VMs only expose WARP this way.
            if (!softwareAdapter) {
                softwareAdapter = candidate;
            }
            continue;
        }
        adapter = candidate;
        break;
    }
    if (!adapter) {
        adapter = softwareAdapter;
    }
    if (!adapter) {
        error = "no DXGI adapter found (hardware or WARP)";
        return nullptr;
    }

    DXGI_ADAPTER_DESC1 adapterDesc{};
    if (FAILED(adapter->GetDesc1(&adapterDesc))) {
        error = "GetDesc1 failed";
        return nullptr;
    }

    const D3D_FEATURE_LEVEL levels[] = {
        D3D_FEATURE_LEVEL_11_1,
        D3D_FEATURE_LEVEL_11_0,
        D3D_FEATURE_LEVEL_10_1,
        D3D_FEATURE_LEVEL_10_0,
        D3D_FEATURE_LEVEL_9_3,
        D3D_FEATURE_LEVEL_9_2,
        D3D_FEATURE_LEVEL_9_1,
    };
    constexpr UINT levelCount = static_cast<UINT>(sizeof(levels) / sizeof(levels[0]));
    constexpr UINT flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;

    ComPtr<ID3D11Device> d3dDevice;
    ComPtr<ID3D11DeviceContext> context;
    D3D_FEATURE_LEVEL featureLevel = D3D_FEATURE_LEVEL_9_1;
    hr = D3D11CreateDevice(
        adapter.Get(), D3D_DRIVER_TYPE_UNKNOWN, nullptr, flags,
        levels, levelCount, D3D11_SDK_VERSION,
        d3dDevice.GetAddressOf(), &featureLevel, context.GetAddressOf());
    if (hr == E_INVALIDARG) {
        // D3D11.1 runtime is not installed; retry without the 11_1 request.
        hr = D3D11CreateDevice(
            adapter.Get(), D3D_DRIVER_TYPE_UNKNOWN, nullptr, flags,
            levels + 1, levelCount - 1, D3D11_SDK_VERSION,
            d3dDevice.GetAddressOf(), &featureLevel, context.GetAddressOf());
    }
    if (FAILED(hr)) {
        error = "D3D11CreateDevice failed: " + hresultToString(hr);
        return nullptr;
    }
    if (featureLevel < D3D_FEATURE_LEVEL_10_0) {
        error = std::string("D3D11 feature level 10_0 or newer required, got ") +
                featureLevelName(featureLevel);
        return nullptr;
    }

    Device* device = new Device();
    device->device = d3dDevice;
    device->context = context;
    context.As(&device->context1);
    device->factory = factory;
    device->adapter = adapter;
    device->hwnd = hwnd;
    device->hwndIsWindow = IsWindow(hwnd) != FALSE;
    device->featureLevel = featureLevel;
    if (device->context1) {
        // ClearView is an optional driver capability (and on feature levels
        // below 11_0 its rectangle handling is not something we want to rely
        // on).  Full clears use the always-available Clear*View methods;
        // rect clears only use ClearView when the driver reports support.
        D3D11_FEATURE_DATA_D3D11_OPTIONS options{};
        if (SUCCEEDED(device->device->CheckFeatureSupport(
                D3D11_FEATURE_D3D11_OPTIONS, &options, sizeof(options)))) {
            device->clearViewSupported = options.ClearView != FALSE;
        }
    }
    device->adapterName = narrow(adapterDesc.Description);
    device->vendorId = adapterDesc.VendorId;
    device->deviceId = adapterDesc.DeviceId;
    device->vendorName = vendorName(adapterDesc.VendorId);
    device->maxTextureSize =
        featureLevel >= D3D_FEATURE_LEVEL_11_0 ? 16384u : 8192u;
    device->versionText = featureLevelName(featureLevel);

    if (!device->compiler.load(error)) {
        destroyDevice(device);
        return nullptr;
    }
    if (!createBlitResources(device, error)) {
        destroyDevice(device);
        return nullptr;
    }
    return device;
}

void destroyDevice(Device* device) {
    if (!device) {
        return;
    }
    if (device->context) {
        device->context->ClearState();
        device->context->Flush();
    }
    device->clearRaster.Reset();
    device->clearDepth.Reset();
    device->clearBlend.Reset();
    device->clearConstants.Reset();
    device->clearPs.Reset();
    device->clearVs.Reset();
    device->blitRaster.Reset();
    device->blitDepth.Reset();
    device->blitBlend.Reset();
    device->blitSampler.Reset();
    device->blitPs.Reset();
    device->blitVs.Reset();
    device->backBufferRtv.Reset();
    device->backBuffer.Reset();
    device->swapChain.Reset();
    device->adapter.Reset();
    device->factory.Reset();
    device->context1.Reset();
    device->context.Reset();
    device->device.Reset();
    device->compiler.reset();
    delete device;
}

bool present(Device* device, UINT width, UINT height, std::string& error) {
    if (!device) {
        error = "no device";
        return false;
    }
    if (width == 0 || height == 0) {
        return true;  // minimized
    }
    if (!ensureSwapChain(device, width, height, error)) {
        return false;
    }
    ++device->presentCount;
    const HRESULT hr = device->swapChain->Present(device->vsync ? 1 : 0, 0);
    device->lastPresentHr = static_cast<long>(hr);
    if (hr == DXGI_STATUS_OCCLUDED) {
        device->swapOccluded = true;
        return true;
    }
    device->swapOccluded = false;
    if (FAILED(hr)) {
        error = "Present failed: " + hresultToString(hr);
        return false;
    }
    return true;
}

bool blitToBackBuffer(Device* device, Texture* source, std::string& error) {
    if (!device || !source || !source->srv) {
        error = "blit: missing device or source texture";
        return false;
    }
    ++device->blitCount;
    // presentTexture() is the only place that knows the real framebuffer size;
    // create or resize the swapchain here before presenting.
    if (!ensureSwapChain(device, source->width, source->height, error)) {
        return false;
    }

    ID3D11RenderTargetView* rtv = device->backBufferRtv.Get();
    device->context->OMSetRenderTargets(1, &rtv, nullptr);
    setDefaultViewport(device, device->swapWidth, device->swapHeight);
    setFullScissor(device, device->swapWidth, device->swapHeight);
    // Diagnostic: make an uncovered back buffer obvious.  A working blit
    // overwrites the whole target; a broken blit leaves dark blue instead of
    // the window's default white.
    const FLOAT clearColor[4] = {0.05f, 0.10f, 0.20f, 1.0f};
    device->context->ClearRenderTargetView(rtv, clearColor);
    device->context->RSSetState(device->blitRaster.Get());
    device->context->OMSetBlendState(device->blitBlend.Get(), nullptr, 0xFFFFFFFF);
    device->context->OMSetDepthStencilState(device->blitDepth.Get(), 0);
    device->context->IASetInputLayout(nullptr);
    device->context->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
    device->context->VSSetShader(device->blitVs.Get(), nullptr, 0);
    device->context->PSSetShader(device->blitPs.Get(), nullptr, 0);
    ID3D11ShaderResourceView* srv = source->srv.Get();
    ID3D11SamplerState* sampler = device->blitSampler.Get();
    device->context->PSSetShaderResources(0, 1, &srv);
    device->context->PSSetSamplers(0, 1, &sampler);
    device->context->Draw(3, 0);

    // Unbind so the texture can be used as a render target again.
    ID3D11ShaderResourceView* nullSrv = nullptr;
    device->context->PSSetShaderResources(0, 1, &nullSrv);
    device->context->OMSetRenderTargets(0, nullptr, nullptr);
    return true;
}

namespace {

bool writeBmp(
    const std::string& path,
    const uint8_t* pixels,
    UINT width,
    UINT height,
    UINT rowPitch,
    bool bgra,
    std::string& error) {
    const UINT rowBytes = width * 4;
    const UINT imageBytes = rowBytes * height;

    BITMAPFILEHEADER fileHeader{};
    fileHeader.bfType = 0x4D42;
    fileHeader.bfSize = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER) + imageBytes;
    fileHeader.bfOffBits = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER);

    BITMAPINFOHEADER infoHeader{};
    infoHeader.biSize = sizeof(BITMAPINFOHEADER);
    infoHeader.biWidth = static_cast<LONG>(width);
    infoHeader.biHeight = static_cast<LONG>(height);
    infoHeader.biPlanes = 1;
    infoHeader.biBitCount = 32;
    infoHeader.biCompression = BI_RGB;
    infoHeader.biSizeImage = imageBytes;

    std::ofstream file(path, std::ios::binary);
    if (!file) {
        error = "writeBmp: cannot open " + path;
        return false;
    }
    file.write(reinterpret_cast<const char*>(&fileHeader), sizeof(fileHeader));
    file.write(reinterpret_cast<const char*>(&infoHeader), sizeof(infoHeader));

    std::vector<uint8_t> row(rowBytes);
    for (UINT y = 0; y < height; ++y) {
        const uint8_t* sourceRow = pixels + static_cast<size_t>(y) * rowPitch;
        for (UINT x = 0; x < width; ++x) {
            const uint8_t* pixel = sourceRow + static_cast<size_t>(x) * 4;
            if (bgra) {
                row[x * 4 + 0] = pixel[0];
                row[x * 4 + 1] = pixel[1];
                row[x * 4 + 2] = pixel[2];
            } else {
                row[x * 4 + 0] = pixel[2];
                row[x * 4 + 1] = pixel[1];
                row[x * 4 + 2] = pixel[0];
            }
            row[x * 4 + 3] = pixel[3];
        }
        file.write(reinterpret_cast<const char*>(row.data()), rowBytes);
    }
    return true;
}

bool dumpTextureToBmp(
    Device* device,
    ID3D11Texture2D* source,
    const std::string& path,
    std::string& error) {
    if (!device || !source) {
        error = "dumpTexture: invalid source";
        return false;
    }
    D3D11_TEXTURE2D_DESC desc{};
    source->GetDesc(&desc);
    if (desc.Format != DXGI_FORMAT_R8G8B8A8_UNORM &&
        desc.Format != DXGI_FORMAT_B8G8R8A8_UNORM) {
        error = "dumpTexture: unsupported format";
        return false;
    }

    D3D11_TEXTURE2D_DESC stagingDesc = desc;
    stagingDesc.MipLevels = 1;
    stagingDesc.ArraySize = 1;
    stagingDesc.Usage = D3D11_USAGE_STAGING;
    stagingDesc.BindFlags = 0;
    stagingDesc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
    stagingDesc.MiscFlags = 0;

    ComPtr<ID3D11Texture2D> staging;
    HRESULT hr = device->device->CreateTexture2D(
        &stagingDesc, nullptr, staging.GetAddressOf());
    if (FAILED(hr)) {
        error = "dumpTexture: staging creation failed: " + hresultToString(hr);
        return false;
    }

    device->context->CopySubresourceRegion(
        staging.Get(), 0, 0, 0, 0, source, 0, nullptr);

    D3D11_MAPPED_SUBRESOURCE mapped{};
    hr = device->context->Map(staging.Get(), 0, D3D11_MAP_READ, 0, &mapped);
    if (FAILED(hr)) {
        error = "dumpTexture: map failed: " + hresultToString(hr);
        return false;
    }
    const bool bgra = desc.Format == DXGI_FORMAT_B8G8R8A8_UNORM;
    const bool ok = writeBmp(
        path, static_cast<const uint8_t*>(mapped.pData), desc.Width, desc.Height,
        mapped.RowPitch, bgra, error);
    device->context->Unmap(staging.Get(), 0);
    return ok;
}

}  // namespace

bool dumpTexture(Device* device, Texture* texture, const std::string& path, std::string& error) {
    if (!texture || !texture->texture) {
        error = "dumpTexture: invalid texture";
        return false;
    }
    return dumpTextureToBmp(device, texture->texture.Get(), path, error);
}

bool dumpBackBuffer(Device* device, const std::string& path, std::string& error) {
    if (!device || !device->backBuffer) {
        error = "dumpBackBuffer: no back buffer";
        return false;
    }
    return dumpTextureToBmp(device, device->backBuffer.Get(), path, error);
}

std::string presentInfo(Device* device) {
    if (!device) {
        return "no device";
    }
    char buffer[256];
    std::snprintf(
        buffer, sizeof(buffer),
        "hwnd=0x%llX isWindow=%d swapchain=%d backBuffer=%d presents=%u lastPresent=0x%08lX occluded=%d blits=%u",
        static_cast<unsigned long long>(reinterpret_cast<uintptr_t>(device->hwnd)),
        device->hwndIsWindow ? 1 : 0,
        device->swapChain ? 1 : 0,
        device->backBufferRtv ? 1 : 0,
        device->presentCount,
        static_cast<unsigned long>(device->lastPresentHr),
        device->swapOccluded ? 1 : 0,
        device->blitCount);
    return buffer;
}

void setVsync(Device* device, bool vsync) {
    if (device) {
        device->vsync = vsync;
    }
}

// ---------------------------------------------------------------------------
// Resources
// ---------------------------------------------------------------------------
Texture* createTexture(
    Device* device,
    int usage,
    int format,
    UINT width,
    UINT height,
    UINT depthOrLayers,
    UINT mipLevels,
    std::string& error) {
    if (!device || width == 0 || height == 0 || depthOrLayers == 0 || mipLevels == 0) {
        error = "createTexture: invalid dimensions";
        return nullptr;
    }

    // D3D11 rejects MipLevels above the full chain for the largest dimension.
    // Vanilla can ask for one extra level (GL tolerates a zero-sized level).
    UINT maxDimension = width > height ? width : height;
    UINT maxMipLevels = 1;
    while (maxDimension > 1) {
        maxDimension >>= 1;
        ++maxMipLevels;
    }
    if (mipLevels > maxMipLevels) {
        mipLevels = maxMipLevels;
    }

    const FormatInfo info = formatInfo(format);
    if (info.resource == DXGI_FORMAT_UNKNOWN) {
        error = "createTexture: unsupported format";
        return nullptr;
    }

    D3D11_TEXTURE2D_DESC desc{};
    desc.Width = width;
    desc.Height = height;
    desc.MipLevels = mipLevels;
    desc.ArraySize = depthOrLayers;
    desc.Format = info.resource;
    desc.SampleDesc.Count = 1;
    desc.SampleDesc.Quality = 0;
    desc.Usage = D3D11_USAGE_DEFAULT;
    desc.BindFlags = 0;
    if (usage & 8) {  // USAGE_RENDER_ATTACHMENT
        desc.BindFlags |= info.depth ? D3D11_BIND_DEPTH_STENCIL : D3D11_BIND_RENDER_TARGET;
    }
    if (usage & 4) {  // USAGE_TEXTURE_BINDING
        desc.BindFlags |= D3D11_BIND_SHADER_RESOURCE;
    }
    desc.CPUAccessFlags = 0;
    if ((usage & 16) && depthOrLayers == 6) {
        desc.MiscFlags |= D3D11_RESOURCE_MISC_TEXTURECUBE;
    }

    Texture* texture = new Texture();
    texture->device = device;
    texture->resourceFormat = info.resource;
    texture->srvFormat = info.srv;
    texture->dsvFormat = info.dsv;
    texture->width = width;
    texture->height = height;
    texture->depthOrLayers = depthOrLayers;
    texture->mipLevels = mipLevels;
    texture->bytesPerPixel = info.bytesPerPixel;
    texture->usage = usage;
    texture->depth = info.depth;
    texture->cube = (usage & 16) != 0 && depthOrLayers == 6;

    HRESULT hr = device->device->CreateTexture2D(
        &desc, nullptr, texture->texture.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateTexture2D failed: " + hresultToString(hr);
        delete texture;
        return nullptr;
    }

    if (usage & 4) {
        D3D11_SHADER_RESOURCE_VIEW_DESC srv{};
        srv.Format = info.srv;
        if (texture->cube) {
            srv.ViewDimension = D3D11_SRV_DIMENSION_TEXTURECUBE;
            srv.TextureCube.MostDetailedMip = 0;
            srv.TextureCube.MipLevels = mipLevels;
        } else {
            srv.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
            srv.Texture2D.MostDetailedMip = 0;
            srv.Texture2D.MipLevels = mipLevels;
        }
        hr = device->device->CreateShaderResourceView(
            texture->texture.Get(), &srv, texture->srv.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateShaderResourceView failed: " + hresultToString(hr);
            destroyTexture(texture);
            return nullptr;
        }
    }

    if ((usage & 8) && !info.depth) {
        hr = device->device->CreateRenderTargetView(
            texture->texture.Get(), nullptr, texture->rtv.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateRenderTargetView failed: " + hresultToString(hr);
            destroyTexture(texture);
            return nullptr;
        }
    }

    if ((usage & 8) && info.depth) {
        D3D11_DEPTH_STENCIL_VIEW_DESC dsv{};
        dsv.Format = info.dsv;
        dsv.ViewDimension = D3D11_DSV_DIMENSION_TEXTURE2D;
        dsv.Texture2D.MipSlice = 0;
        hr = device->device->CreateDepthStencilView(
            texture->texture.Get(), &dsv, texture->dsv.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateDepthStencilView failed: " + hresultToString(hr);
            destroyTexture(texture);
            return nullptr;
        }
    }

    return texture;
}

TextureView* createTextureView(
    Device* device,
    Texture* texture,
    UINT baseMip,
    UINT mipLevels,
    std::string& error) {
    if (!device || !texture) {
        error = "createTextureView: invalid texture";
        return nullptr;
    }
    if (baseMip >= texture->mipLevels) {
        error = "createTextureView: base mip out of range";
        return nullptr;
    }
    if (mipLevels == 0 || baseMip + mipLevels > texture->mipLevels) {
        mipLevels = texture->mipLevels - baseMip;
    }

    TextureView* view = new TextureView();
    view->texture = texture;
    view->baseMip = baseMip;
    view->mipLevels = mipLevels;

    if (texture->usage & 8) {  // USAGE_RENDER_ATTACHMENT
        if (texture->depth) {
            D3D11_DEPTH_STENCIL_VIEW_DESC dsv{};
            dsv.Format = texture->dsvFormat;
            dsv.ViewDimension = D3D11_DSV_DIMENSION_TEXTURE2D;
            dsv.Texture2D.MipSlice = baseMip;
            const HRESULT hr = device->device->CreateDepthStencilView(
                texture->texture.Get(), &dsv, view->dsv.GetAddressOf());
            if (FAILED(hr)) {
                error = "CreateDepthStencilView(view) failed: " + hresultToString(hr);
                delete view;
                return nullptr;
            }
        } else {
            D3D11_RENDER_TARGET_VIEW_DESC rtv{};
            rtv.Format = texture->srvFormat;
            rtv.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2D;
            rtv.Texture2D.MipSlice = baseMip;
            const HRESULT hr = device->device->CreateRenderTargetView(
                texture->texture.Get(), &rtv, view->rtv.GetAddressOf());
            if (FAILED(hr)) {
                error = "CreateRenderTargetView(view) failed: " + hresultToString(hr);
                delete view;
                return nullptr;
            }
        }
    }

    // A view of a depth-only render target (USAGE_RENDER_ATTACHMENT without
    // USAGE_TEXTURE_BINDING) is only used as an attachment.  Creating an SRV
    // for such a texture fails, but the render pass only needs the view's DSV,
    // so leave the SRV empty.
    if ((texture->usage & 4) == 0) {
        return view;
    }

    D3D11_SHADER_RESOURCE_VIEW_DESC srv{};
    srv.Format = texture->srvFormat;
    if (texture->cube) {
        srv.ViewDimension = D3D11_SRV_DIMENSION_TEXTURECUBE;
        srv.TextureCube.MostDetailedMip = baseMip;
        srv.TextureCube.MipLevels = mipLevels;
    } else {
        srv.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
        srv.Texture2D.MostDetailedMip = baseMip;
        srv.Texture2D.MipLevels = mipLevels;
    }
    const HRESULT hr = device->device->CreateShaderResourceView(
        texture->texture.Get(), &srv, view->srv.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateShaderResourceView(view) failed: " + hresultToString(hr);
        delete view;
        return nullptr;
    }
    return view;
}

void destroyTexture(Texture* texture) {
    if (!texture) {
        return;
    }
    texture->uploadStaging.Reset();
    texture->dsv.Reset();
    texture->rtv.Reset();
    texture->srv.Reset();
    texture->texture.Reset();
    delete texture;
}

void destroyTextureView(TextureView* view) {
    if (!view) {
        return;
    }
    view->dsv.Reset();
    view->rtv.Reset();
    view->srv.Reset();
    delete view;
}

Buffer* createBuffer(
    Device* device,
    int usage,
    UINT size,
    const void* initialData,
    UINT initialSize,
    std::string& error) {
    if (!device || size == 0) {
        error = "createBuffer: invalid size";
        return nullptr;
    }

    Buffer* buffer = new Buffer();
    buffer->device = device;
    buffer->size = size;
    buffer->usage = usage;

    const bool dynamic = (usage & 2) != 0;  // USAGE_MAP_WRITE
    const UINT alignment = (usage & 128) ? 16u : 4u;
    const UINT capacity = (size + alignment - 1) & ~(alignment - 1);
    buffer->capacity = capacity;

    D3D11_BUFFER_DESC desc{};
    desc.ByteWidth = capacity;
    desc.Usage = dynamic ? D3D11_USAGE_DYNAMIC : D3D11_USAGE_DEFAULT;
    desc.BindFlags = 0;
    if (usage & 32) {
        desc.BindFlags |= D3D11_BIND_VERTEX_BUFFER;
    }
    if (usage & 64) {
        desc.BindFlags |= D3D11_BIND_INDEX_BUFFER;
    }
    if (usage & 128) {
        desc.BindFlags |= D3D11_BIND_CONSTANT_BUFFER;
    }
    // USAGE_UNIFORM_TEXEL_BUFFER is emulated with a texture; no bind flag.
    desc.CPUAccessFlags = dynamic ? D3D11_CPU_ACCESS_WRITE : 0;

    D3D11_SUBRESOURCE_DATA init{};
    const D3D11_SUBRESOURCE_DATA* initPtr = nullptr;
    std::vector<uint8_t> padded;
    if (initialData && initialSize > 0) {
        if (initialSize >= capacity) {
            init.pSysMem = initialData;
        } else {
            padded.assign(capacity, 0);
            std::memcpy(padded.data(), initialData, initialSize);
            init.pSysMem = padded.data();
        }
        initPtr = &init;
    }

    // A typed buffer (D3D11 feature level 10.x has no Buffer<T>) only ever
    // lives in the CPU shadow; creating an unbound ID3D11Buffer would be
    // rejected by some drivers.  COPY/vertex/index/constant usage still needs
    // a real buffer.
    const bool needsRealBuffer = (usage & (8 | 16 | 32 | 64 | 128)) != 0;
    if (needsRealBuffer) {
        const HRESULT hr = device->device->CreateBuffer(
            &desc, initPtr, buffer->buffer.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateBuffer failed: " + hresultToString(hr);
            delete buffer;
            return nullptr;
        }
    }

    if (needsShadow(usage)) {
        buffer->hasShadow = true;
        buffer->shadow.assign(size, 0);
        if (initialData && initialSize > 0) {
            std::memcpy(buffer->shadow.data(), initialData,
                        std::min(initialSize, size));
        }
    }
    return buffer;
}

void destroyBuffer(Buffer* buffer) {
    if (!buffer) {
        return;
    }
    buffer->texelSrv.Reset();
    buffer->texelTexture.Reset();
    buffer->staging.Reset();
    buffer->buffer.Reset();
    delete buffer;
}

Sampler* createSampler(
    Device* device,
    int addressU,
    int addressV,
    int minFilter,
    int magFilter,
    int maxAnisotropy,
    bool hasMaxLod,
    double maxLod,
    std::string& error) {
    if (!device) {
        error = "createSampler: no device";
        return nullptr;
    }

    D3D11_SAMPLER_DESC desc{};
    if (maxAnisotropy > 1) {
        desc.Filter = D3D11_FILTER_ANISOTROPIC;
        desc.MaxAnisotropy = static_cast<UINT>(std::min(maxAnisotropy, 16));
    } else {
        // GlSampler always uses a linear mip filter (GL_NEAREST_MIPMAP_LINEAR
        // / GL_LINEAR_MIPMAP_LINEAR) and only varies min/mag.
        desc.MaxAnisotropy = 1;
        if (minFilter == 0 && magFilter == 0) {  // NEAREST / NEAREST
            desc.Filter = D3D11_FILTER_MIN_MAG_POINT_MIP_LINEAR;
        } else if (minFilter == 0) {             // NEAREST / LINEAR
            desc.Filter = D3D11_FILTER_MIN_POINT_MAG_MIP_LINEAR;
        } else if (magFilter == 0) {             // LINEAR / NEAREST
            desc.Filter = D3D11_FILTER_MIN_LINEAR_MAG_POINT_MIP_LINEAR;
        } else {                                 // LINEAR / LINEAR
            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
        }
    }
    desc.AddressU = addressU == 0 ? D3D11_TEXTURE_ADDRESS_WRAP
                                  : D3D11_TEXTURE_ADDRESS_CLAMP;
    desc.AddressV = addressV == 0 ? D3D11_TEXTURE_ADDRESS_WRAP
                                  : D3D11_TEXTURE_ADDRESS_CLAMP;
    desc.AddressW = desc.AddressV;
    desc.MipLODBias = 0.0f;
    desc.ComparisonFunc = D3D11_COMPARISON_ALWAYS;
    desc.BorderColor[0] = 0.0f;
    desc.BorderColor[1] = 0.0f;
    desc.BorderColor[2] = 0.0f;
    desc.BorderColor[3] = 0.0f;
    desc.MinLOD = 0.0f;
    desc.MaxLOD = hasMaxLod ? static_cast<FLOAT>(maxLod) : D3D11_FLOAT32_MAX;

    Sampler* sampler = new Sampler();
    sampler->device = device;
    const HRESULT hr = device->device->CreateSamplerState(
        &desc, sampler->sampler.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateSamplerState failed: " + hresultToString(hr);
        delete sampler;
        return nullptr;
    }
    return sampler;
}

void destroySampler(Sampler* sampler) {
    if (!sampler) {
        return;
    }
    sampler->sampler.Reset();
    delete sampler;
}

// ---------------------------------------------------------------------------
// Commands
// ---------------------------------------------------------------------------
namespace {

bool ensureStagingBuffer(Buffer* buffer, std::string& error) {
    if (buffer->staging) {
        return true;
    }
    D3D11_BUFFER_DESC desc{};
    desc.ByteWidth = buffer->capacity;
    desc.Usage = D3D11_USAGE_STAGING;
    desc.BindFlags = 0;
    desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ | D3D11_CPU_ACCESS_WRITE;
    const HRESULT hr = buffer->device->device->CreateBuffer(
        &desc, nullptr, buffer->staging.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateBuffer(staging) failed: " + hresultToString(hr);
        return false;
    }
    return true;
}

bool readBackBuffer(Buffer* buffer, std::string& error) {
    if (!buffer->buffer) {
        return true;
    }
    if (!ensureStagingBuffer(buffer, error)) {
        return false;
    }
    if (buffer->shadow.size() != buffer->size) {
        buffer->shadow.assign(buffer->size, 0);
        buffer->hasShadow = true;
    }
    ID3D11DeviceContext* context = buffer->device->context.Get();
    context->CopyResource(buffer->staging.Get(), buffer->buffer.Get());
    D3D11_MAPPED_SUBRESOURCE mapped{};
    HRESULT hr = context->Map(
        buffer->staging.Get(), 0, D3D11_MAP_READ, 0, &mapped);
    if (FAILED(hr)) {
        error = "Map(readback) failed: " + hresultToString(hr);
        return false;
    }
    std::memcpy(buffer->shadow.data(), mapped.pData, buffer->size);
    context->Unmap(buffer->staging.Get(), 0);
    return true;
}

// Uploads the current CPU shadow to a DEFAULT buffer.  D3D11's
// UpdateSubresource always writes the whole resource, so the shadow is padded
// to the D3D11 capacity.
bool updateDefaultBufferFromShadow(Buffer* buffer, std::string& error) {
    if (!buffer->buffer) {
        return true;
    }
    std::vector<uint8_t> padded(buffer->capacity, 0);
    const UINT copySize = std::min<UINT>(buffer->size, buffer->capacity);
    if (buffer->hasShadow && buffer->shadow.size() >= copySize) {
        std::memcpy(padded.data(), buffer->shadow.data(), copySize);
    }
    buffer->device->context->UpdateSubresource(
        buffer->buffer.Get(), 0, nullptr, padded.data(), 0, 0);
    return true;
}

}  // namespace

bool writeToBuffer(
    Buffer* buffer,
    UINT offset,
    const void* data,
    UINT size,
    std::string& error) {
    if (!buffer || buffer->closed) {
        error = "writeToBuffer: invalid buffer";
        return false;
    }
    if (!data && size != 0) {
        error = "writeToBuffer: null data";
        return false;
    }
    if (offset > buffer->size || size > buffer->size - offset) {
        error = "writeToBuffer: out of range";
        return false;
    }
    if (buffer->mapped) {
        error = "writeToBuffer: buffer is mapped";
        return false;
    }

    if (buffer->hasShadow && buffer->shadow.size() >= offset + size) {
        if (size > 0) {
            std::memcpy(buffer->shadow.data() + offset, data, size);
        }
        ++buffer->version;
    }

    if (!buffer->buffer) {
        return true;
    }

    const bool dynamic = (buffer->usage & 2) != 0;
    if (dynamic) {
        D3D11_MAPPED_SUBRESOURCE mapped{};
        const bool whole = offset == 0 && size == buffer->size;
        const D3D11_MAP mapType =
            whole ? D3D11_MAP_WRITE_DISCARD : D3D11_MAP_WRITE_NO_OVERWRITE;
        const HRESULT hr = buffer->device->context->Map(
            buffer->buffer.Get(), 0, mapType, 0, &mapped);
        if (FAILED(hr)) {
            error = "Map(write) failed: " + hresultToString(hr);
            return false;
        }
        if (size > 0) {
            std::memcpy(static_cast<uint8_t*>(mapped.pData) + offset, data, size);
        }
        if (whole && buffer->capacity > buffer->size) {
            std::memset(static_cast<uint8_t*>(mapped.pData) + buffer->size, 0,
                        buffer->capacity - buffer->size);
        }
        buffer->device->context->Unmap(buffer->buffer.Get(), 0);
        return true;
    }

    // DEFAULT buffer.
    if (buffer->hasShadow) {
        // The shadow already contains the new contents.
        return updateDefaultBufferFromShadow(buffer, error);
    }

    // Partial write without a CPU shadow: preserve the old contents with a
    // staging copy.  Full-buffer writes avoid the copy.
    if (offset == 0 && size == buffer->size) {
        std::vector<uint8_t> padded(buffer->capacity, 0);
        if (size > 0) {
            std::memcpy(padded.data(), data, size);
        }
        buffer->device->context->UpdateSubresource(
            buffer->buffer.Get(), 0, nullptr, padded.data(), 0, 0);
        return true;
    }

    if (!ensureStagingBuffer(buffer, error)) {
        return false;
    }
    ID3D11DeviceContext* context = buffer->device->context.Get();
    context->CopyResource(buffer->staging.Get(), buffer->buffer.Get());
    D3D11_MAPPED_SUBRESOURCE mapped{};
    HRESULT hr = context->Map(
        buffer->staging.Get(), 0, D3D11_MAP_READ_WRITE, 0, &mapped);
    if (FAILED(hr)) {
        error = "Map(staging write) failed: " + hresultToString(hr);
        return false;
    }
    if (size > 0) {
        std::memcpy(static_cast<uint8_t*>(mapped.pData) + offset, data, size);
    }
    context->Unmap(buffer->staging.Get(), 0);
    context->CopyResource(buffer->buffer.Get(), buffer->staging.Get());
    return true;
}

void* mapBuffer(
    Buffer* buffer,
    UINT offset,
    UINT size,
    bool read,
    bool write,
    std::string& error) {
    if (!buffer || buffer->closed) {
        error = "mapBuffer: invalid buffer";
        return nullptr;
    }
    if (buffer->mapped) {
        error = "mapBuffer: buffer is already mapped";
        return nullptr;
    }
    if (offset > buffer->size || size > buffer->size - offset) {
        error = "mapBuffer: out of range";
        return nullptr;
    }

    buffer->mapped = true;
    buffer->mappedOffset = offset;
    buffer->mappedSize = size;
    buffer->mappedWrite = write;
    buffer->mappedShadowOnly = false;

    // Typed buffers are emulated with a texture and have no ID3D11Buffer; map
    // their CPU shadow directly.
    if (!buffer->buffer && buffer->hasShadow) {
        buffer->mappedShadowOnly = true;
        buffer->mappedPtr = buffer->shadow.data();
        return buffer->shadow.data() + offset;
    }

    if (!write) {
        if (!buffer->hasShadow) {
            if (!readBackBuffer(buffer, error)) {
                buffer->mapped = false;
                return nullptr;
            }
        }
        if (!buffer->hasShadow || buffer->shadow.size() < offset + size) {
            error = "mapBuffer: no readable shadow";
            buffer->mapped = false;
            return nullptr;
        }
        buffer->mappedPtr = buffer->shadow.data();
        return buffer->shadow.data() + offset;
    }

    const bool dynamic = (buffer->usage & 2) != 0;
    D3D11_MAPPED_SUBRESOURCE mapped{};
    if (dynamic) {
        const bool whole = offset == 0 && size == buffer->size;
        buffer->mapType =
            whole ? D3D11_MAP_WRITE_DISCARD : D3D11_MAP_WRITE_NO_OVERWRITE;
        const HRESULT hr = buffer->device->context->Map(
            buffer->buffer.Get(), 0, buffer->mapType, 0, &mapped);
        if (FAILED(hr)) {
            error = "Map(write) failed: " + hresultToString(hr);
            buffer->mapped = false;
            return nullptr;
        }
        buffer->mappedViaStaging = false;
    } else {
        if (!ensureStagingBuffer(buffer, error)) {
            buffer->mapped = false;
            return nullptr;
        }
        ID3D11DeviceContext* context = buffer->device->context.Get();
        context->CopyResource(buffer->staging.Get(), buffer->buffer.Get());
        const HRESULT hr = context->Map(
            buffer->staging.Get(), 0, D3D11_MAP_READ_WRITE, 0, &mapped);
        if (FAILED(hr)) {
            error = "Map(staging write) failed: " + hresultToString(hr);
            buffer->mapped = false;
            return nullptr;
        }
        buffer->mappedViaStaging = true;
    }

    buffer->mappedPtr = mapped.pData;
    if (read && buffer->hasShadow && buffer->shadow.size() >= offset + size) {
        std::memcpy(static_cast<uint8_t*>(mapped.pData) + offset,
                    buffer->shadow.data() + offset, size);
    }
    return static_cast<uint8_t*>(mapped.pData) + offset;
}

void unmapBuffer(Buffer* buffer) {
    if (!buffer || !buffer->mapped) {
        return;
    }
    if (buffer->mappedWrite) {
        ID3D11DeviceContext* context = buffer->device->context.Get();
        if (buffer->mappedShadowOnly) {
            // Writes landed directly in the shadow.
            ++buffer->version;
        } else {
            // The pointer returned by Map is invalid after Unmap, so snapshot
            // the written slice into the CPU shadow first.  passSetUniform()
            // reads the shadow on the next draw.
            if (buffer->hasShadow &&
                buffer->shadow.size() >= buffer->mappedOffset + buffer->mappedSize) {
                std::memcpy(
                    buffer->shadow.data() + buffer->mappedOffset,
                    static_cast<uint8_t*>(buffer->mappedPtr) + buffer->mappedOffset,
                    buffer->mappedSize);
                ++buffer->version;
            }
            if (buffer->mappedViaStaging) {
                context->Unmap(buffer->staging.Get(), 0);
                context->CopyResource(buffer->buffer.Get(), buffer->staging.Get());
            } else {
                context->Unmap(buffer->buffer.Get(), 0);
            }
        }
    }
    buffer->mapped = false;
    buffer->mappedWrite = false;
    buffer->mappedViaStaging = false;
    buffer->mappedShadowOnly = false;
    buffer->mappedPtr = nullptr;
    buffer->mappedOffset = 0;
    buffer->mappedSize = 0;
}

bool copyBufferToBuffer(
    Buffer* source,
    UINT sourceOffset,
    Buffer* destination,
    UINT destinationOffset,
    UINT size,
    std::string& error) {
    if (!source || !destination || source->closed || destination->closed) {
        error = "copyBufferToBuffer: invalid buffer";
        return false;
    }
    if (sourceOffset > source->size || size > source->size - sourceOffset ||
        destinationOffset > destination->size ||
        size > destination->size - destinationOffset) {
        error = "copyBufferToBuffer: out of range";
        return false;
    }
    if (size == 0) {
        return true;
    }

    std::vector<uint8_t> temp;
    const void* sourceData = nullptr;
    if (source->hasShadow && source->shadow.size() >= sourceOffset + size) {
        sourceData = source->shadow.data() + sourceOffset;
    } else if (source == destination && source->hasShadow) {
        sourceData = source->shadow.data() + sourceOffset;
    } else {
        if (!readBackBuffer(source, error)) {
            return false;
        }
        sourceData = source->shadow.data() + sourceOffset;
    }

    if (source == destination) {
        temp.assign(static_cast<const uint8_t*>(sourceData),
                    static_cast<const uint8_t*>(sourceData) + size);
        sourceData = temp.data();
    }

    if (!destination->buffer) {
        // Shadow-only (typed) buffer: the CPU copy below is the destination.
    } else if ((destination->usage & 2) != 0) {
        D3D11_MAPPED_SUBRESOURCE mapped{};
        const HRESULT hr = destination->device->context->Map(
            destination->buffer.Get(), 0, D3D11_MAP_WRITE_NO_OVERWRITE, 0, &mapped);
        if (FAILED(hr)) {
            error = "Map(copy) failed: " + hresultToString(hr);
            return false;
        }
        std::memcpy(static_cast<uint8_t*>(mapped.pData) + destinationOffset,
                    sourceData, size);
        destination->device->context->Unmap(destination->buffer.Get(), 0);
    } else if (!source->buffer) {
        // The source only lives in its CPU shadow (typed-buffer emulation);
        // upload through the normal path instead of passing a null resource
        // to CopySubresourceRegion.
        if (!writeToBuffer(
                destination, destinationOffset, sourceData, size, error)) {
            return false;
        }
    } else {
        D3D11_BOX box{};
        box.left = sourceOffset;
        box.right = sourceOffset + size;
        box.top = 0;
        box.bottom = 1;
        box.front = 0;
        box.back = 1;
        destination->device->context->CopySubresourceRegion(
            destination->buffer.Get(), 0, destinationOffset, 0, 0,
            source->buffer.Get(), 0, &box);
    }

    if (destination->hasShadow &&
        destination->shadow.size() >= destinationOffset + size) {
        std::memcpy(destination->shadow.data() + destinationOffset, sourceData, size);
        ++destination->version;
    }
    return true;
}

bool writeToTexture(
    Texture* texture,
    UINT mip,
    UINT layer,
    UINT x,
    UINT y,
    UINT width,
    UINT height,
    const void* data,
    UINT srcRowStride,
    std::string& error) {
    if (!texture || texture->closed) {
        error = "writeToTexture: invalid texture";
        return false;
    }
    if (mip >= texture->mipLevels || layer >= texture->depthOrLayers) {
        error = "writeToTexture: mip/layer out of range";
        return false;
    }
    if (!data || width == 0 || height == 0) {
        return true;
    }
    const UINT mipWidth = mipDimension(texture->width, mip);
    const UINT mipHeight = mipDimension(texture->height, mip);
    if (x > mipWidth || y > mipHeight || width > mipWidth - x ||
        height > mipHeight - y) {
        error = "writeToTexture: rect out of range";
        return false;
    }

    const UINT subresource = mip + layer * texture->mipLevels;
    const UINT rowBytes = width * texture->bytesPerPixel;
    if (x == 0 && y == 0 && width == mipWidth && height == mipHeight &&
        srcRowStride == rowBytes) {
        D3D11_BOX box{};
        box.left = 0;
        box.top = 0;
        box.front = 0;
        box.right = width;
        box.bottom = height;
        box.back = 1;
        texture->device->context->UpdateSubresource(
            texture->texture.Get(), subresource, &box, data, srcRowStride, 0);
        return true;
    }

    // Partial upload: stage the sub-rect and copy it into the texture.
    if (!texture->uploadStaging || texture->uploadStagingMip != mip) {
        D3D11_TEXTURE2D_DESC desc{};
        desc.Width = mipWidth;
        desc.Height = mipHeight;
        desc.MipLevels = 1;
        desc.ArraySize = 1;
        desc.Format = texture->resourceFormat;
        desc.SampleDesc.Count = 1;
        desc.Usage = D3D11_USAGE_STAGING;
        desc.BindFlags = 0;
        desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        texture->uploadStaging.Reset();
        const HRESULT hr = texture->device->device->CreateTexture2D(
            &desc, nullptr, texture->uploadStaging.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateTexture2D(staging) failed: " + hresultToString(hr);
            return false;
        }
        texture->uploadStagingMip = mip;
    }

    D3D11_MAPPED_SUBRESOURCE mapped{};
    const HRESULT hr = texture->device->context->Map(
        texture->uploadStaging.Get(), 0, D3D11_MAP_WRITE, 0, &mapped);
    if (FAILED(hr)) {
        error = "Map(texture staging) failed: " + hresultToString(hr);
        return false;
    }
    // The staging texture only exists to carry this sub-rect, and the copy
    // below reads from (0, 0).  Write the rows there rather than at (x, y),
    // otherwise glyph/atlas uploads at a non-zero offset would copy
    // uninitialized staging pixels into the destination.
    for (UINT row = 0; row < height; ++row) {
        std::memcpy(
            static_cast<uint8_t*>(mapped.pData) + row * mapped.RowPitch,
            static_cast<const uint8_t*>(data) + row * srcRowStride,
            rowBytes);
    }
    texture->device->context->Unmap(texture->uploadStaging.Get(), 0);

    D3D11_BOX sourceBox{};
    sourceBox.left = 0;
    sourceBox.top = 0;
    sourceBox.front = 0;
    sourceBox.right = width;
    sourceBox.bottom = height;
    sourceBox.back = 1;
    texture->device->context->CopySubresourceRegion(
        texture->texture.Get(), subresource, x, y, 0,
        texture->uploadStaging.Get(), 0, &sourceBox);
    return true;
}

bool copyTextureToBuffer(
    Texture* texture,
    Buffer* buffer,
    UINT bufferOffset,
    UINT mip,
    UINT x,
    UINT y,
    UINT width,
    UINT height,
    std::string& error) {
    if (!texture || !buffer || texture->closed || buffer->closed) {
        error = "copyTextureToBuffer: invalid resource";
        return false;
    }
    if (mip >= texture->mipLevels) {
        error = "copyTextureToBuffer: mip out of range";
        return false;
    }
    const UINT mipWidth = mipDimension(texture->width, mip);
    const UINT mipHeight = mipDimension(texture->height, mip);
    if (x > mipWidth || y > mipHeight || width > mipWidth - x ||
        height > mipHeight - y) {
        error = "copyTextureToBuffer: rect out of range";
        return false;
    }
    const UINT rowBytes = width * texture->bytesPerPixel;
    const UINT total = rowBytes * height;
    if (bufferOffset > buffer->size || total > buffer->size - bufferOffset) {
        error = "copyTextureToBuffer: destination out of range";
        return false;
    }

    D3D11_TEXTURE2D_DESC desc{};
    desc.Width = width;
    desc.Height = height;
    desc.MipLevels = 1;
    desc.ArraySize = 1;
    desc.Format = texture->resourceFormat;
    desc.SampleDesc.Count = 1;
    desc.Usage = D3D11_USAGE_STAGING;
    desc.BindFlags = 0;
    desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;

    ComPtr<ID3D11Texture2D> staging;
    HRESULT hr = texture->device->device->CreateTexture2D(
        &desc, nullptr, staging.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateTexture2D(readback) failed: " + hresultToString(hr);
        return false;
    }

    D3D11_BOX box{};
    box.left = x;
    box.top = y;
    box.front = 0;
    box.right = x + width;
    box.bottom = y + height;
    box.back = 1;
    texture->device->context->CopySubresourceRegion(
        staging.Get(), 0, 0, 0, 0, texture->texture.Get(), mip, &box);

    D3D11_MAPPED_SUBRESOURCE mapped{};
    hr = texture->device->context->Map(
        staging.Get(), 0, D3D11_MAP_READ, 0, &mapped);
    if (FAILED(hr)) {
        error = "Map(readback) failed: " + hresultToString(hr);
        return false;
    }

    if (!buffer->hasShadow || buffer->shadow.size() != buffer->size) {
        buffer->shadow.assign(buffer->size, 0);
        buffer->hasShadow = true;
    }
    for (UINT row = 0; row < height; ++row) {
        std::memcpy(
            buffer->shadow.data() + bufferOffset + row * rowBytes,
            static_cast<const uint8_t*>(mapped.pData) + row * mapped.RowPitch,
            rowBytes);
    }
    texture->device->context->Unmap(staging.Get(), 0);

    // The CPU shadow is what mapBuffer(read) returns.
    ++buffer->version;
    return true;
}

bool copyTextureToTexture(
    Texture* source,
    Texture* destination,
    UINT mip,
    UINT srcX,
    UINT srcY,
    UINT dstX,
    UINT dstY,
    UINT width,
    UINT height,
    std::string& error) {
    if (!source || !destination || source->closed || destination->closed) {
        error = "copyTextureToTexture: invalid texture";
        return false;
    }
    if (mip >= source->mipLevels || mip >= destination->mipLevels) {
        error = "copyTextureToTexture: mip out of range";
        return false;
    }
    if (source->resourceFormat != destination->resourceFormat) {
        error = "copyTextureToTexture: format mismatch";
        return false;
    }
    const UINT sourceWidth = mipDimension(source->width, mip);
    const UINT sourceHeight = mipDimension(source->height, mip);
    if (srcX > sourceWidth || srcY > sourceHeight ||
        width > sourceWidth - srcX || height > sourceHeight - srcY) {
        error = "copyTextureToTexture: source rect out of range";
        return false;
    }
    const UINT destinationWidth = mipDimension(destination->width, mip);
    const UINT destinationHeight = mipDimension(destination->height, mip);
    if (dstX > destinationWidth || dstY > destinationHeight ||
        width > destinationWidth - dstX ||
        height > destinationHeight - dstY) {
        error = "copyTextureToTexture: destination rect out of range";
        return false;
    }

    D3D11_BOX box{};
    box.left = srcX;
    box.top = srcY;
    box.front = 0;
    box.right = srcX + width;
    box.bottom = srcY + height;
    box.back = 1;
    source->device->context->CopySubresourceRegion(
        destination->texture.Get(), mip, dstX, dstY, 0,
        source->texture.Get(), mip, &box);
    return true;
}

bool clearColorTexture(
    Device* device,
    Texture* texture,
    UINT32 argb,
    std::string& error) {
    if (!device || !texture || !texture->rtv) {
        error = "clearColorTexture: invalid color texture";
        return false;
    }
    const FLOAT color[4] = {
        ((argb >> 16) & 0xFF) / 255.0f,
        ((argb >> 8) & 0xFF) / 255.0f,
        (argb & 0xFF) / 255.0f,
        ((argb >> 24) & 0xFF) / 255.0f,
    };
    device->context->ClearRenderTargetView(texture->rtv.Get(), color);
    return true;
}

bool clearDepthTexture(
    Device* device,
    Texture* texture,
    double depth,
    std::string& error) {
    if (!device || !texture || !texture->dsv) {
        error = "clearDepthTexture: invalid depth texture";
        return false;
    }
    device->context->ClearDepthStencilView(
        texture->dsv.Get(), D3D11_CLEAR_DEPTH, static_cast<FLOAT>(depth), 0);
    return true;
}

namespace {

// Rect-aware clear fallback.  ID3D11DeviceContext1::ClearView is an optional
// driver capability (and may be missing entirely on the D3D11.0 runtime), so
// emulate the GL scissored clear with a fullscreen triangle that writes
// SV_Depth and a constant colour.  The rasterizer scissor limits the write to
// the requested region.
bool clearRegionByDraw(
    Device* device,
    Texture* color,
    const FLOAT colorValue[4],
    Texture* depth,
    FLOAT depthValue,
    const int* rect,
    std::string& error) {
    if (!device->clearVs || !device->clearPs || !device->clearConstants ||
        !device->clearBlend || !device->clearDepth || !device->clearRaster) {
        error = "clearRegionByDraw: clear resources unavailable";
        return false;
    }

    LONG left = rect[0];
    LONG top = rect[1];
    LONG right = left + rect[2];
    LONG bottom = top + rect[3];
    const LONG maxWidth = static_cast<LONG>(color->width);
    const LONG maxHeight = static_cast<LONG>(color->height);
    left = std::max<LONG>(left, 0);
    top = std::max<LONG>(top, 0);
    right = std::min<LONG>(right, maxWidth);
    bottom = std::min<LONG>(bottom, maxHeight);
    if (right <= left || bottom <= top) {
        return true;
    }

    struct ClearConstants {
        FLOAT color[4];
        FLOAT depth;
        FLOAT padding[3];
    } constants{};
    std::memcpy(constants.color, colorValue, sizeof(constants.color));
    constants.depth = depthValue;

    ID3D11DeviceContext* context = device->context.Get();
    D3D11_MAPPED_SUBRESOURCE mapped{};
    HRESULT hr = context->Map(
        device->clearConstants.Get(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapped);
    if (FAILED(hr)) {
        error = "Map(clear constants) failed: " + hresultToString(hr);
        return false;
    }
    std::memcpy(mapped.pData, &constants, sizeof(constants));
    context->Unmap(device->clearConstants.Get(), 0);

    // The colour/depth attachments must not also be bound as SRVs.
    unbindPixelAndVertexResources(device);

    ID3D11RenderTargetView* rtv = color->rtv.Get();
    ID3D11DepthStencilView* dsv = depth->dsv.Get();
    context->OMSetRenderTargets(1, &rtv, dsv);

    D3D11_VIEWPORT viewport{};
    viewport.Width = static_cast<FLOAT>(color->width);
    viewport.Height = static_cast<FLOAT>(color->height);
    viewport.MinDepth = 0.0f;
    viewport.MaxDepth = 1.0f;
    context->RSSetViewports(1, &viewport);

    D3D11_RECT scissor{};
    scissor.left = left;
    scissor.top = top;
    scissor.right = right;
    scissor.bottom = bottom;
    context->RSSetScissorRects(1, &scissor);

    context->OMSetBlendState(device->clearBlend.Get(), nullptr, 0xFFFFFFFF);
    context->OMSetDepthStencilState(device->clearDepth.Get(), 0);
    context->RSSetState(device->clearRaster.Get());

    ID3D11Buffer* constantsBuffer = device->clearConstants.Get();
    context->IASetInputLayout(nullptr);
    context->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
    context->IASetIndexBuffer(nullptr, DXGI_FORMAT_UNKNOWN, 0);
    context->IASetVertexBuffers(0, 0, nullptr, nullptr, nullptr);
    context->VSSetShader(device->clearVs.Get(), nullptr, 0);
    context->PSSetShader(device->clearPs.Get(), nullptr, 0);
    context->PSSetConstantBuffers(0, 1, &constantsBuffer);
    context->Draw(3, 0);

    // Leave a neutral state behind; clear calls always happen outside a
    // render pass, and the next pass binds its own state.
    context->OMSetRenderTargets(0, nullptr, nullptr);
    context->VSSetShader(nullptr, nullptr, 0);
    context->PSSetShader(nullptr, nullptr, 0);
    context->IASetInputLayout(nullptr);
    return true;
}

}  // namespace

bool clearColorAndDepthTexture(
    Device* device,
    Texture* color,
    UINT32 argb,
    Texture* depth,
    double depthValue,
    const int* rect,
    std::string& error) {
    if (!device || !color || !color->rtv || !depth || !depth->dsv) {
        error = "clearColorAndDepthTexture: invalid textures";
        return false;
    }
    const FLOAT colorValue[4] = {
        ((argb >> 16) & 0xFF) / 255.0f,
        ((argb >> 8) & 0xFF) / 255.0f,
        (argb & 0xFF) / 255.0f,
        ((argb >> 24) & 0xFF) / 255.0f,
    };
    const FLOAT depthClear[4] = {static_cast<FLOAT>(depthValue), 0.0f, 0.0f, 0.0f};

    if (!rect) {
        device->context->ClearRenderTargetView(color->rtv.Get(), colorValue);
        device->context->ClearDepthStencilView(
            depth->dsv.Get(), D3D11_CLEAR_DEPTH, static_cast<FLOAT>(depthValue), 0);
        return true;
    }

    // GL clear/scissor rects use a bottom-left origin, but the global Y flip
    // in every vertex shader makes the render-target memory layout match GL's,
    // so the rect maps directly to D3D's top-left coordinates.
    if (device->clearViewSupported && device->featureLevel >= D3D_FEATURE_LEVEL_11_0) {
        D3D11_RECT colorRect{};
        colorRect.left = rect[0];
        colorRect.top = rect[1];
        colorRect.right = rect[0] + rect[2];
        colorRect.bottom = rect[1] + rect[3];

        D3D11_RECT depthRect{};
        depthRect.left = rect[0];
        depthRect.top = rect[1];
        depthRect.right = rect[0] + rect[2];
        depthRect.bottom = rect[1] + rect[3];

        device->context1->ClearView(color->rtv.Get(), colorValue, &colorRect, 1);
        device->context1->ClearView(depth->dsv.Get(), depthClear, &depthRect, 1);
        return true;
    }

    return clearRegionByDraw(
        device, color, colorValue, depth, static_cast<FLOAT>(depthValue),
        rect, error);
}

// ---------------------------------------------------------------------------
// Pipelines
// ---------------------------------------------------------------------------
Pipeline* createPipeline(Device* device, const PipelineDesc& desc, std::string& error) {
    if (!device) {
        error = "createPipeline: no device";
        return nullptr;
    }

    ComPtr<ID3DBlob> vsBlob;
    ComPtr<ID3DBlob> psBlob;
    if (!compileStage(device->compiler, desc.vertexHlsl, desc.name + ".vs", "main",
                      true, device->featureLevel, vsBlob, error)) {
        return nullptr;
    }
    if (!compileStage(device->compiler, desc.fragmentHlsl, desc.name + ".ps", "main",
                      false, device->featureLevel, psBlob, error)) {
        return nullptr;
    }

    Pipeline* pipeline = new Pipeline();
    pipeline->device = device;
    pipeline->topology = desc.topology;
    pipeline->stride = desc.stride;

    HRESULT hr = device->device->CreateVertexShader(
        vsBlob->GetBufferPointer(), vsBlob->GetBufferSize(), nullptr,
        pipeline->vs.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateVertexShader failed: " + hresultToString(hr);
        destroyPipeline(pipeline);
        return nullptr;
    }
    hr = device->device->CreatePixelShader(
        psBlob->GetBufferPointer(), psBlob->GetBufferSize(), nullptr,
        pipeline->ps.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreatePixelShader failed: " + hresultToString(hr);
        destroyPipeline(pipeline);
        return nullptr;
    }

    if (!desc.inputs.empty()) {
        std::vector<D3D11_INPUT_ELEMENT_DESC> elements;
        elements.reserve(desc.inputs.size());
        for (const InputElement& input : desc.inputs) {
            D3D11_INPUT_ELEMENT_DESC element{};
            element.SemanticName = input.semantic.c_str();
            element.SemanticIndex = input.semanticIndex;
            element.Format = input.format;
            element.InputSlot = 0;
            element.AlignedByteOffset = input.offset;
            element.InputSlotClass = D3D11_INPUT_PER_VERTEX_DATA;
            element.InstanceDataStepRate = 0;
            elements.push_back(element);
        }
        hr = device->device->CreateInputLayout(
            elements.data(), static_cast<UINT>(elements.size()),
            vsBlob->GetBufferPointer(), vsBlob->GetBufferSize(),
            pipeline->inputLayout.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateInputLayout failed: " + hresultToString(hr);
            destroyPipeline(pipeline);
            return nullptr;
        }
    }

    D3D11_BLEND_DESC blend{};
    blend.AlphaToCoverageEnable = FALSE;
    blend.IndependentBlendEnable = FALSE;
    D3D11_RENDER_TARGET_BLEND_DESC& target = blend.RenderTarget[0];
    target.BlendEnable = desc.blend ? TRUE : FALSE;
    target.SrcBlend = desc.srcColor;
    target.DestBlend = desc.dstColor;
    target.BlendOp = D3D11_BLEND_OP_ADD;
    target.SrcBlendAlpha = desc.srcAlpha;
    target.DestBlendAlpha = desc.dstAlpha;
    target.BlendOpAlpha = D3D11_BLEND_OP_ADD;
    target.RenderTargetWriteMask = desc.colorWriteMask & D3D11_COLOR_WRITE_ENABLE_ALL;
    hr = device->device->CreateBlendState(&blend, pipeline->blendState.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateBlendState failed: " + hresultToString(hr);
        destroyPipeline(pipeline);
        return nullptr;
    }

    D3D11_DEPTH_STENCIL_DESC depth{};
    depth.DepthEnable = desc.depthTest ? TRUE : FALSE;
    depth.DepthWriteMask = desc.depthWrite ? D3D11_DEPTH_WRITE_MASK_ALL
                                           : D3D11_DEPTH_WRITE_MASK_ZERO;
    depth.DepthFunc = desc.depthFunc;
    depth.StencilEnable = FALSE;
    hr = device->device->CreateDepthStencilState(
        &depth, pipeline->depthState.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateDepthStencilState failed: " + hresultToString(hr);
        destroyPipeline(pipeline);
        return nullptr;
    }

    D3D11_RASTERIZER_DESC raster{};
    raster.FillMode = desc.fillMode;
    raster.CullMode = desc.cull ? D3D11_CULL_BACK : D3D11_CULL_NONE;
    // The Java layer negates gl_Position.y in every vertex shader, which
    // reverses triangle winding, so D3D's default clockwise front face is
    // already correct.
    raster.FrontCounterClockwise = FALSE;
    raster.DepthBias = desc.depthBias;
    raster.DepthBiasClamp = 0.0f;
    raster.SlopeScaledDepthBias = desc.slopeScaledDepthBias;
    raster.DepthClipEnable = TRUE;
    // Scissor is always enabled; a disabled GL scissor becomes a full-target
    // rectangle.  This avoids a second rasterizer state per pipeline.
    raster.ScissorEnable = TRUE;
    raster.MultisampleEnable = FALSE;
    raster.AntialiasedLineEnable = FALSE;
    hr = device->device->CreateRasterizerState(
        &raster, pipeline->rasterState.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateRasterizerState failed: " + hresultToString(hr);
        destroyPipeline(pipeline);
        return nullptr;
    }

    pipeline->constantBuffers.resize(desc.uniformBufferSizes.size());
    pipeline->constantBufferSizes.resize(desc.uniformBufferSizes.size());
    for (size_t slot = 0; slot < desc.uniformBufferSizes.size(); ++slot) {
        UINT size = desc.uniformBufferSizes[slot];
        size = std::max<UINT>(size, 16);
        size = (size + 15) & ~15u;
        size = std::min<UINT>(size, D3D11_REQ_CONSTANT_BUFFER_ELEMENT_COUNT * 16);
        pipeline->constantBufferSizes[slot] = size;

        D3D11_BUFFER_DESC bufferDesc{};
        bufferDesc.ByteWidth = size;
        bufferDesc.Usage = D3D11_USAGE_DYNAMIC;
        bufferDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        hr = device->device->CreateBuffer(
            &bufferDesc, nullptr, pipeline->constantBuffers[slot].GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateBuffer(constant slot " + std::to_string(slot) +
                    ") failed: " + hresultToString(hr);
            destroyPipeline(pipeline);
            return nullptr;
        }
    }

    return pipeline;
}

void destroyPipeline(Pipeline* pipeline) {
    if (!pipeline) {
        return;
    }
    pipeline->constantBuffers.clear();
    pipeline->constantBufferSizes.clear();
    pipeline->rasterState.Reset();
    pipeline->depthState.Reset();
    pipeline->blendState.Reset();
    pipeline->inputLayout.Reset();
    pipeline->ps.Reset();
    pipeline->vs.Reset();
    delete pipeline;
}

// ---------------------------------------------------------------------------
// Render passes
// ---------------------------------------------------------------------------
namespace {

void bindFullScissor(RenderPass* pass) {
    D3D11_RECT rect{};
    rect.left = 0;
    rect.top = 0;
    rect.right = static_cast<LONG>(pass->targetWidth);
    rect.bottom = static_cast<LONG>(pass->targetHeight);
    pass->device->context->RSSetScissorRects(1, &rect);
}

// Creates / grows the scratch 32-bit index buffer used to emulate
// GL_TRIANGLE_FAN (D3D11 has no fan topology).
bool uploadFanIndices(RenderPass* pass, const uint32_t* indices, UINT count,
                      std::string& error) {
    Device* device = pass->device;
    if (!pass->fanIndexBuffer || pass->fanIndexCapacity < count) {
        UINT capacity = 64;
        while (capacity < count) {
            capacity *= 2;
        }
        D3D11_BUFFER_DESC desc{};
        desc.ByteWidth = capacity * sizeof(uint32_t);
        desc.Usage = D3D11_USAGE_DYNAMIC;
        desc.BindFlags = D3D11_BIND_INDEX_BUFFER;
        desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        pass->fanIndexBuffer.Reset();
        const HRESULT hr = device->device->CreateBuffer(
            &desc, nullptr, pass->fanIndexBuffer.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateBuffer(fan indices) failed: " + hresultToString(hr);
            return false;
        }
        pass->fanIndexCapacity = capacity;
    }

    D3D11_MAPPED_SUBRESOURCE mapped{};
    HRESULT hr = device->context->Map(
        pass->fanIndexBuffer.Get(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapped);
    if (FAILED(hr)) {
        error = "Map(fan indices) failed: " + hresultToString(hr);
        return false;
    }
    std::memcpy(mapped.pData, indices, count * sizeof(uint32_t));
    device->context->Unmap(pass->fanIndexBuffer.Get(), 0);
    device->context->IASetIndexBuffer(
        pass->fanIndexBuffer.Get(), DXGI_FORMAT_R32_UINT, 0);
    return true;
}

// Restores the index buffer that was bound before fan emulation replaced it.
void restoreIndexBuffer(RenderPass* pass) {
    if (pass->indexBuffer && pass->indexBuffer->buffer) {
        pass->device->context->IASetIndexBuffer(
            pass->indexBuffer->buffer.Get(), pass->indexFormat, 0);
    } else {
        pass->device->context->IASetIndexBuffer(
            nullptr, DXGI_FORMAT_UNKNOWN, 0);
    }
}

// Converts a triangle fan to a triangle list.  `source` contains `count`
// vertex indices; the result is written to `out`.
bool fanFromIndices(const uint32_t* source, UINT count, int baseVertex,
                    std::vector<uint32_t>& out) {
    if (count < 3) {
        out.clear();
        return true;
    }
    out.resize(static_cast<size_t>(count - 2) * 3);
    size_t cursor = 0;
    for (UINT i = 1; i + 1 < count; ++i) {
        const int64_t center = static_cast<int64_t>(source[0]) + baseVertex;
        const int64_t current = static_cast<int64_t>(source[i]) + baseVertex;
        const int64_t next = static_cast<int64_t>(source[i + 1]) + baseVertex;
        if (center < 0 || current < 0 || next < 0 ||
            center > 0x7FFFFFFF || current > 0x7FFFFFFF || next > 0x7FFFFFFF) {
            return false;
        }
        out[cursor++] = static_cast<uint32_t>(center);
        out[cursor++] = static_cast<uint32_t>(current);
        out[cursor++] = static_cast<uint32_t>(next);
    }
    return true;
}

}  // namespace

RenderPass* createRenderPass(
    Device* device,
    TextureView* colorView,
    TextureView* depthView,
    bool clearColor,
    UINT32 clearColorValue,
    bool clearDepth,
    double clearDepthValue,
    std::string& error) {
    if (!device || (!colorView && !depthView)) {
        error = "createRenderPass: no attachment";
        return nullptr;
    }
    Texture* colorTexture = colorView ? colorView->texture : nullptr;
    Texture* depthTexture = depthView ? depthView->texture : nullptr;
    if (colorView && !colorView->rtv) {
        error = "createRenderPass: color view has no render-target view";
        return nullptr;
    }
    if (depthView && !depthView->dsv) {
        error = "createRenderPass: depth view has no depth-stencil view";
        return nullptr;
    }

    RenderPass* pass = new RenderPass();
    pass->device = device;
    pass->colorView = colorView;
    pass->colorTexture = colorTexture;
    pass->depthTexture = depthTexture;
    pass->targetWidth = colorView
                            ? mipDimension(colorTexture->width, colorView->baseMip)
                            : mipDimension(depthTexture->width, depthView->baseMip);
    pass->targetHeight = colorView
                             ? mipDimension(colorTexture->height, colorView->baseMip)
                             : mipDimension(depthTexture->height, depthView->baseMip);

    // Detach any SRV that may still point at the attachment from a previous
    // pass, otherwise D3D11 will warn and the render-target bind may fail.
    unbindPixelAndVertexResources(device);

    ID3D11RenderTargetView* rtv = colorView ? colorView->rtv.Get() : nullptr;
    ID3D11DepthStencilView* dsv = depthView ? depthView->dsv.Get() : nullptr;
    device->context->OMSetRenderTargets(rtv ? 1 : 0, rtv ? &rtv : nullptr, dsv);

    setDefaultViewport(device, pass->targetWidth, pass->targetHeight);
    bindFullScissor(pass);

    if (clearColor && colorView) {
        const FLOAT value[4] = {
            ((clearColorValue >> 16) & 0xFF) / 255.0f,
            ((clearColorValue >> 8) & 0xFF) / 255.0f,
            (clearColorValue & 0xFF) / 255.0f,
            ((clearColorValue >> 24) & 0xFF) / 255.0f,
        };
        device->context->ClearRenderTargetView(colorView->rtv.Get(), value);
    }
    if (clearDepth && depthView) {
        device->context->ClearDepthStencilView(
            depthView->dsv.Get(), D3D11_CLEAR_DEPTH,
            static_cast<FLOAT>(clearDepthValue), 0);
    }
    return pass;
}

void destroyRenderPass(RenderPass* pass) {
    if (!pass) {
        return;
    }
    Device* device = pass->device;
    device->context->OMSetRenderTargets(0, nullptr, nullptr);
    unbindPixelAndVertexResources(device);
    device->context->IASetInputLayout(nullptr);
    device->context->IASetIndexBuffer(nullptr, DXGI_FORMAT_UNKNOWN, 0);
    device->context->IASetVertexBuffers(0, 0, nullptr, nullptr, nullptr);
    device->context->VSSetShader(nullptr, nullptr, 0);
    device->context->PSSetShader(nullptr, nullptr, 0);
    pass->fanIndexBuffer.Reset();
    pass->fanIndexCapacity = 0;
    delete pass;
}

void passSetPipeline(RenderPass* pass, Pipeline* pipeline) {
    if (!pass || !pipeline || pass->closed) {
        return;
    }
    pass->pipeline = pipeline;
    ID3D11DeviceContext* context = pass->device->context.Get();
    context->IASetInputLayout(pipeline->inputLayout.Get());
    context->IASetPrimitiveTopology(
        pipeline->topology == kTopologyTriangleFan
            ? D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST
            : pipeline->topology);
    context->VSSetShader(pipeline->vs.Get(), nullptr, 0);
    context->PSSetShader(pipeline->ps.Get(), nullptr, 0);
    context->OMSetBlendState(pipeline->blendState.Get(), nullptr, 0xFFFFFFFF);
    context->OMSetDepthStencilState(pipeline->depthState.Get(), 0);
    context->RSSetState(pipeline->rasterState.Get());
    setDefaultViewport(pass->device, pass->targetWidth, pass->targetHeight);

    const UINT count = static_cast<UINT>(pipeline->constantBuffers.size());
    if (count > 0) {
        std::vector<ID3D11Buffer*> buffers(count);
        for (UINT i = 0; i < count; ++i) {
            buffers[i] = pipeline->constantBuffers[i].Get();
        }
        context->VSSetConstantBuffers(0, count, buffers.data());
        context->PSSetConstantBuffers(0, count, buffers.data());
    }

    if (pass->vertexBuffer && pass->vertexBuffer->buffer) {
        ID3D11Buffer* vertexBuffer = pass->vertexBuffer->buffer.Get();
        UINT stride = pipeline->stride;
        UINT offset = 0;
        context->IASetVertexBuffers(0, 1, &vertexBuffer, &stride, &offset);
    } else {
        context->IASetVertexBuffers(0, 0, nullptr, nullptr, nullptr);
    }
    if (pass->indexBuffer && pass->indexBuffer->buffer) {
        context->IASetIndexBuffer(
            pass->indexBuffer->buffer.Get(), pass->indexFormat, 0);
    }
}

void passSetUniform(RenderPass* pass, UINT slot, Buffer* buffer, UINT offset, UINT size) {
    if (!pass || !pass->pipeline || !buffer) {
        return;
    }
    if (slot >= pass->pipeline->constantBuffers.size()) {
        std::fprintf(stderr, "[dx11] setUniform slot %u out of range (%zu)\n",
                     slot, pass->pipeline->constantBuffers.size());
        return;
    }
    if (offset > buffer->size || size > buffer->size - offset) {
        std::fprintf(stderr, "[dx11] setUniform slot %u out of range\n", slot);
        return;
    }

    const uint8_t* source = nullptr;
    if (buffer->hasShadow && buffer->shadow.size() >= offset + size) {
        source = buffer->shadow.data() + offset;
    } else if (buffer->mapped && buffer->mappedPtr) {
        source = static_cast<const uint8_t*>(buffer->mappedPtr) + offset;
    } else {
        std::string ignored;
        if (!readBackBuffer(buffer, ignored)) {
            std::fprintf(stderr, "[dx11] setUniform: %s\n", ignored.c_str());
            return;
        }
        source = buffer->shadow.data() + offset;
    }

    // The Java layer does not know the byte size of a uniform buffer (the
    // RenderPipeline API only stores names), so grow the per-slot DYNAMIC
    // constant buffer on demand.  D3D11 requires a constant buffer to be a
    // multiple of 16 bytes and at most 64 KiB.
    UINT needed = (size + 15u) & ~15u;
    needed = std::max<UINT>(needed, 16u);
    needed = std::min<UINT>(needed, D3D11_REQ_CONSTANT_BUFFER_ELEMENT_COUNT * 16);
    if (needed > pass->pipeline->constantBufferSizes[slot]) {
        D3D11_BUFFER_DESC bufferDesc{};
        bufferDesc.ByteWidth = needed;
        bufferDesc.Usage = D3D11_USAGE_DYNAMIC;
        bufferDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        ComPtr<ID3D11Buffer> grown;
        const HRESULT createHr = pass->device->device->CreateBuffer(
            &bufferDesc, nullptr, grown.GetAddressOf());
        if (FAILED(createHr)) {
            std::fprintf(stderr, "[dx11] setUniform grow failed: %s\n",
                         hresultToString(createHr).c_str());
            return;
        }
        pass->pipeline->constantBuffers[slot] = std::move(grown);
        pass->pipeline->constantBufferSizes[slot] = needed;
        // Rebind every slot because the pipeline may have been set before the
        // buffer was grown.
        const UINT count = static_cast<UINT>(pass->pipeline->constantBuffers.size());
        std::vector<ID3D11Buffer*> buffers(count);
        for (UINT i = 0; i < count; ++i) {
            buffers[i] = pass->pipeline->constantBuffers[i].Get();
        }
        pass->device->context->VSSetConstantBuffers(0, count, buffers.data());
        pass->device->context->PSSetConstantBuffers(0, count, buffers.data());
    }

    ID3D11Buffer* destination = pass->pipeline->constantBuffers[slot].Get();
    const UINT capacity = pass->pipeline->constantBufferSizes[slot];
    D3D11_MAPPED_SUBRESOURCE mapped{};
    HRESULT hr = pass->device->context->Map(
        destination, 0, D3D11_MAP_WRITE_DISCARD, 0, &mapped);
    if (FAILED(hr)) {
        std::fprintf(stderr, "[dx11] setUniform Map failed: %s\n",
                     hresultToString(hr).c_str());
        return;
    }
    std::memset(mapped.pData, 0, capacity);
    if (size > 0) {
        std::memcpy(mapped.pData, source, std::min(size, capacity));
    }
    pass->device->context->Unmap(destination, 0);
}

void passSetTexelBuffer(RenderPass* pass, UINT slot, Buffer* buffer, int format) {
    if (!pass || !buffer || buffer->closed) {
        return;
    }

    // The HLSL rewrite declares `Texture2D<int4>`, so integer texel buffers are
    // expanded into RGBA8_SINT with (value, 0, 0, 1) per element.
    const bool expandInt = format == 2;
    UINT bytesPerPixel = 1;
    DXGI_FORMAT textureFormat = DXGI_FORMAT_R8G8B8A8_SINT;
    switch (format) {
        case 0:  // RGBA8
            bytesPerPixel = 4;
            textureFormat = DXGI_FORMAT_R8G8B8A8_UNORM;
            break;
        case 1:  // RED8
            bytesPerPixel = 1;
            textureFormat = DXGI_FORMAT_R8_UNORM;
            break;
        case 2:  // RED8I
        default:
            bytesPerPixel = 1;
            textureFormat = DXGI_FORMAT_R8G8B8A8_SINT;
            break;
    }

    // Feature level 10.x has no typed buffers (Buffer<T>), so the data is
    // exposed as a fixed-width 2D texture.  The HLSL rewrite in the Java layer
    // loads with `index & 8191` / `index >> 13`.
    constexpr UINT kTexelWidth = 8192;
    const UINT height = std::max<UINT>(1, (buffer->size + kTexelWidth - 1) / kTexelWidth);
    if (!buffer->texelTexture || buffer->texelWidth != kTexelWidth ||
        buffer->texelHeight != height) {
        D3D11_TEXTURE2D_DESC desc{};
        desc.Width = kTexelWidth;
        desc.Height = height;
        desc.MipLevels = 1;
        desc.ArraySize = 1;
        desc.Format = textureFormat;
        desc.SampleDesc.Count = 1;
        desc.Usage = D3D11_USAGE_DEFAULT;
        desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        buffer->texelSrv.Reset();
        buffer->texelTexture.Reset();
        HRESULT hr = buffer->device->device->CreateTexture2D(
            &desc, nullptr, buffer->texelTexture.GetAddressOf());
        if (FAILED(hr)) {
            std::fprintf(stderr, "[dx11] texel texture creation failed: %s\n",
                         hresultToString(hr).c_str());
            return;
        }
        hr = buffer->device->device->CreateShaderResourceView(
            buffer->texelTexture.Get(), nullptr, buffer->texelSrv.GetAddressOf());
        if (FAILED(hr)) {
            std::fprintf(stderr, "[dx11] texel SRV creation failed: %s\n",
                         hresultToString(hr).c_str());
            buffer->texelTexture.Reset();
            return;
        }
        buffer->texelWidth = kTexelWidth;
        buffer->texelHeight = height;
        buffer->texelVersion = 0;
    }

    if (buffer->texelVersion != buffer->version) {
        const size_t rowBytes = static_cast<size_t>(kTexelWidth) * bytesPerPixel;
        std::vector<uint8_t> padded(rowBytes * height, 0);
        const size_t copyBytes =
            std::min<size_t>(buffer->shadow.size(), padded.size());
        if (copyBytes > 0) {
            std::memcpy(padded.data(), buffer->shadow.data(), copyBytes);
        }
        if (expandInt) {
            // R8I -> R8G8B8A8_SINT: (value, 0, 0, 1) per element.
            std::vector<uint8_t> expanded(
                static_cast<size_t>(kTexelWidth) * height * 4, 0);
            const size_t count = static_cast<size_t>(kTexelWidth) * height;
            for (size_t i = 0; i < count; ++i) {
                expanded[i * 4 + 0] = padded[i];
                expanded[i * 4 + 3] = 1;
            }
            buffer->device->context->UpdateSubresource(
                buffer->texelTexture.Get(), 0, nullptr, expanded.data(),
                static_cast<UINT>(kTexelWidth) * 4, 0);
        } else {
            buffer->device->context->UpdateSubresource(
                buffer->texelTexture.Get(), 0, nullptr, padded.data(),
                static_cast<UINT>(rowBytes), 0);
        }
        buffer->texelVersion = buffer->version;
    }

    ID3D11ShaderResourceView* srv = buffer->texelSrv.Get();
    pass->device->context->PSSetShaderResources(slot, 1, &srv);
    pass->device->context->VSSetShaderResources(slot, 1, &srv);
}

void passBindTexture(RenderPass* pass, UINT slot, TextureView* view, Sampler* sampler) {
    if (!pass || pass->closed) {
        return;
    }
    ID3D11ShaderResourceView* srv = view ? view->srv.Get() : nullptr;
    ID3D11SamplerState* samplerState = sampler ? sampler->sampler.Get() : nullptr;
    ID3D11DeviceContext* context = pass->device->context.Get();
    context->PSSetShaderResources(slot, 1, &srv);
    context->VSSetShaderResources(slot, 1, &srv);
    context->PSSetSamplers(slot, 1, &samplerState);
    context->VSSetSamplers(slot, 1, &samplerState);
}

void passSetVertexBuffer(RenderPass* pass, Buffer* buffer) {
    if (!pass || pass->closed) {
        return;
    }
    pass->vertexBuffer = buffer;
    if (buffer && buffer->buffer && pass->pipeline) {
        ID3D11Buffer* vertexBuffer = buffer->buffer.Get();
        UINT stride = pass->pipeline->stride;
        UINT offset = 0;
        pass->device->context->IASetVertexBuffers(
            0, 1, &vertexBuffer, &stride, &offset);
    }
}

void passSetIndexBuffer(RenderPass* pass, Buffer* buffer, bool index32) {
    if (!pass || pass->closed) {
        return;
    }
    pass->indexBuffer = buffer;
    pass->indexFormat = index32 ? DXGI_FORMAT_R32_UINT : DXGI_FORMAT_R16_UINT;
    if (buffer && buffer->buffer) {
        pass->device->context->IASetIndexBuffer(
            buffer->buffer.Get(), pass->indexFormat, 0);
    }
}

void passSetScissor(RenderPass* pass, bool enabled, int x, int y, int width, int height) {
    if (!pass || pass->closed) {
        return;
    }
    pass->scissorEnabled = enabled;
    pass->scissorX = x;
    pass->scissorY = y;
    pass->scissorWidth = width;
    pass->scissorHeight = height;
    if (!enabled) {
        bindFullScissor(pass);
        return;
    }

    // GL scissor coordinates have a bottom-left origin, but the global Y flip
    // in every vertex shader makes the render-target memory layout match GL's
    // (row 0 = logical bottom), so D3D's top-left rect uses the same y.
    LONG top = y;
    LONG left = x;
    LONG right = static_cast<LONG>(x) + width;
    LONG bottom = top + height;
    left = std::max<LONG>(left, 0);
    top = std::max<LONG>(top, 0);
    right = std::min<LONG>(right, static_cast<LONG>(pass->targetWidth));
    bottom = std::min<LONG>(bottom, static_cast<LONG>(pass->targetHeight));
    if (right < left) {
        right = left;
    }
    if (bottom < top) {
        bottom = top;
    }

    D3D11_RECT rect{};
    rect.left = left;
    rect.top = top;
    rect.right = right;
    rect.bottom = bottom;
    pass->device->context->RSSetScissorRects(1, &rect);
}

void passDrawIndexed(RenderPass* pass, UINT firstIndex, UINT indexCount,
                     int baseVertex, UINT instanceCount) {
    if (!pass || !pass->pipeline || !pass->indexBuffer || indexCount == 0) {
        return;
    }
    if (instanceCount == 0) {
        instanceCount = 1;
    }

    if (pass->pipeline->topology == kTopologyTriangleFan) {
        std::string error;
        if (!pass->indexBuffer->hasShadow &&
            !readBackBuffer(pass->indexBuffer, error)) {
            std::fprintf(stderr, "[dx11] fan index readback failed: %s\n", error.c_str());
            return;
        }
        const UINT bytesPerIndex =
            pass->indexFormat == DXGI_FORMAT_R32_UINT ? 4u : 2u;
        if (static_cast<uint64_t>(firstIndex + indexCount) * bytesPerIndex >
            pass->indexBuffer->size) {
            std::fprintf(stderr, "[dx11] fan index range out of bounds\n");
            return;
        }
        std::vector<uint32_t> source(indexCount);
        const uint8_t* raw = pass->indexBuffer->shadow.data() +
                             static_cast<size_t>(firstIndex) * bytesPerIndex;
        for (UINT i = 0; i < indexCount; ++i) {
            source[i] = bytesPerIndex == 4
                            ? reinterpret_cast<const uint32_t*>(raw)[i]
                            : reinterpret_cast<const uint16_t*>(raw)[i];
        }
        std::vector<uint32_t> fan;
        if (!fanFromIndices(source.data(), indexCount, baseVertex, fan) || fan.empty()) {
            return;
        }
        if (!uploadFanIndices(pass, fan.data(), static_cast<UINT>(fan.size()), error)) {
            std::fprintf(stderr, "[dx11] fan index upload failed: %s\n", error.c_str());
            return;
        }
        pass->device->context->IASetPrimitiveTopology(
            D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        pass->device->context->DrawIndexedInstanced(
            static_cast<UINT>(fan.size()), instanceCount, 0, 0, 0);
        restoreIndexBuffer(pass);
        pass->device->context->IASetPrimitiveTopology(
            D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        return;
    }

    pass->device->context->DrawIndexedInstanced(
        indexCount, instanceCount, firstIndex, baseVertex, 0);
}

void passDraw(RenderPass* pass, UINT firstVertex, UINT vertexCount, UINT instanceCount) {
    if (!pass || !pass->pipeline || vertexCount == 0) {
        return;
    }
    if (instanceCount == 0) {
        instanceCount = 1;
    }

    if (pass->pipeline->topology == kTopologyTriangleFan) {
        if (vertexCount < 3) {
            return;
        }
        std::vector<uint32_t> source(vertexCount);
        for (UINT i = 0; i < vertexCount; ++i) {
            source[i] = firstVertex + i;
        }
        std::vector<uint32_t> fan;
        if (!fanFromIndices(source.data(), vertexCount, 0, fan) || fan.empty()) {
            return;
        }
        std::string error;
        if (!uploadFanIndices(pass, fan.data(), static_cast<UINT>(fan.size()), error)) {
            std::fprintf(stderr, "[dx11] fan index upload failed: %s\n", error.c_str());
            return;
        }
        pass->device->context->IASetPrimitiveTopology(
            D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        pass->device->context->DrawIndexedInstanced(
            static_cast<UINT>(fan.size()), instanceCount, 0, 0, 0);
        restoreIndexBuffer(pass);
        pass->device->context->IASetPrimitiveTopology(
            D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        return;
    }

    pass->device->context->DrawInstanced(vertexCount, instanceCount, firstVertex, 0);
}

// ---------------------------------------------------------------------------
// Fences / queries
// ---------------------------------------------------------------------------
Fence* createFence(Device* device, std::string& error) {
    if (!device) {
        error = "createFence: no device";
        return nullptr;
    }
    D3D11_QUERY_DESC desc{};
    desc.Query = D3D11_QUERY_EVENT;
    Fence* fence = new Fence();
    fence->device = device;
    const HRESULT hr = device->device->CreateQuery(
        &desc, fence->query.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateQuery(event) failed: " + hresultToString(hr);
        delete fence;
        return nullptr;
    }
    // Mark the current position in the command stream.
    device->context->End(fence->query.Get());
    return fence;
}

bool fenceAwait(Fence* fence, int64_t timeoutNanos) {
    if (!fence || !fence->query) {
        return true;
    }
    // GpuFence.awaitCompletion follows glClientWaitSync: the timeout is in
    // nanoseconds.  Long.MAX_VALUE is the "wait forever" sentinel.
    const bool infinite =
        timeoutNanos < 0 || timeoutNanos == std::numeric_limits<int64_t>::max();
    const auto deadline = std::chrono::steady_clock::now() +
                          std::chrono::nanoseconds(infinite ? 0 : timeoutNanos);
    for (;;) {
        const HRESULT hr = fence->device->context->GetData(
            fence->query.Get(), nullptr, 0, 0);
        if (hr == S_OK) {
            return true;
        }
        if (hr != S_FALSE) {
            return false;
        }
        if (!infinite && std::chrono::steady_clock::now() >= deadline) {
            return false;
        }
        std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
}

void destroyFence(Fence* fence) {
    if (!fence) {
        return;
    }
    fence->query.Reset();
    delete fence;
}

Query* createTimerQuery(Device* device, std::string& error) {
    if (!device) {
        error = "createTimerQuery: no device";
        return nullptr;
    }
    Query* query = new Query();
    query->device = device;

    D3D11_QUERY_DESC desc{};
    desc.Query = D3D11_QUERY_TIMESTAMP_DISJOINT;
    HRESULT hr = device->device->CreateQuery(
        &desc, query->disjoint.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateQuery(timestamp disjoint) failed: " + hresultToString(hr);
        destroyQuery(query);
        return nullptr;
    }
    desc.Query = D3D11_QUERY_TIMESTAMP;
    hr = device->device->CreateQuery(&desc, query->start.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateQuery(timestamp start) failed: " + hresultToString(hr);
        destroyQuery(query);
        return nullptr;
    }
    hr = device->device->CreateQuery(&desc, query->end.GetAddressOf());
    if (FAILED(hr)) {
        error = "CreateQuery(timestamp end) failed: " + hresultToString(hr);
        destroyQuery(query);
        return nullptr;
    }

    device->context->Begin(query->disjoint.Get());
    device->context->End(query->start.Get());
    query->begun = true;
    return query;
}

void timerQueryEnd(Query* query) {
    if (!query || !query->begun || query->ended) {
        return;
    }
    query->device->context->End(query->end.Get());
    query->device->context->End(query->disjoint.Get());
    query->ended = true;
}

bool queryGetValue(Query* query, int64_t& nanos, bool& ready) {
    ready = false;
    if (!query || !query->ended) {
        return false;
    }
    if (query->hasResult) {
        nanos = query->resultNanos;
        ready = true;
        return true;
    }

    D3D11_QUERY_DATA_TIMESTAMP_DISJOINT disjoint{};
    HRESULT hr = query->device->context->GetData(
        query->disjoint.Get(), &disjoint, sizeof(disjoint), 0);
    if (hr == S_FALSE) {
        return false;
    }
    if (FAILED(hr)) {
        query->hasResult = true;
        query->resultNanos = 0;
        ready = true;
        return true;
    }

    UINT64 start = 0;
    UINT64 end = 0;
    hr = query->device->context->GetData(
        query->start.Get(), &start, sizeof(start), 0);
    if (hr == S_FALSE) {
        return false;
    }
    if (SUCCEEDED(hr)) {
        hr = query->device->context->GetData(
            query->end.Get(), &end, sizeof(end), 0);
    }
    if (hr == S_FALSE) {
        return false;
    }

    query->hasResult = true;
    if (FAILED(hr) || disjoint.Disjoint || disjoint.Frequency == 0 ||
        end < start) {
        query->resultNanos = 0;
    } else {
        query->resultNanos = static_cast<int64_t>(
            static_cast<double>(end - start) * 1.0e9 /
            static_cast<double>(disjoint.Frequency));
    }
    nanos = query->resultNanos;
    ready = true;
    return true;
}

void destroyQuery(Query* query) {
    if (!query) {
        return;
    }
    query->end.Reset();
    query->start.Reset();
    query->disjoint.Reset();
    delete query;
}

}  // namespace dx11mc
