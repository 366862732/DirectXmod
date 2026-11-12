#include "dx11_probe.h"

#include "dx11_common.h"

#include <cstdio>
#include <cstring>
#include <sstream>
#include <string>

namespace dx11mc {
namespace {

const char* const kVertexShader =
    "struct VSInput { float3 position : POSITION; float2 uv : TEXCOORD0; };\n"
    "struct VSOutput { float4 position : SV_Position; float2 uv : TEXCOORD0; };\n"
    "VSOutput main(VSInput input) {\n"
    "    VSOutput output;\n"
    "    output.position = float4(input.position, 1.0);\n"
    "    output.uv = input.uv;\n"
    "    return output;\n"
    "}\n";

const char* const kPixelShader =
    "Texture2D tex0 : register(t0);\n"
    "SamplerState samp0 : register(s0);\n"
    "float4 main(float4 position : SV_Position, float2 uv : TEXCOORD0) : SV_Target {\n"
    "    return tex0.Sample(samp0, uv);\n"
    "}\n";

LRESULT CALLBACK probeWndProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam) {
    return DefWindowProcW(hwnd, message, wParam, lParam);
}

std::string hex16(unsigned int value) {
    char buffer[16];
    std::snprintf(buffer, sizeof(buffer), "%04X", value & 0xFFFFu);
    return buffer;
}

bool compileShader(
    D3DCompiler& compiler,
    const char* entryPoint,
    const char* target,
    const char* source,
    std::string& error) {
    ComPtr<ID3DBlob> bytecode;
    ComPtr<ID3DBlob> messages;
    const HRESULT hr = compiler.compile(
        source,
        std::strlen(source),
        "dx11_probe",
        nullptr,
        nullptr,
        entryPoint,
        target,
        D3DCOMPILE_ENABLE_STRICTNESS,
        0,
        bytecode.GetAddressOf(),
        messages.GetAddressOf());

    if (SUCCEEDED(hr)) {
        return true;
    }

    std::string message = "D3DCompile(";
    message += target;
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

    error = message;
    return false;
}

bool createDevice(
    IDXGIAdapter1* adapter,
    ComPtr<ID3D11Device>& device,
    ComPtr<ID3D11DeviceContext>& context,
    D3D_FEATURE_LEVEL& featureLevel,
    std::string& error) {
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

    HRESULT hr = D3D11CreateDevice(
        adapter,
        D3D_DRIVER_TYPE_UNKNOWN,
        nullptr,
        flags,
        levels,
        levelCount,
        D3D11_SDK_VERSION,
        device.GetAddressOf(),
        &featureLevel,
        context.GetAddressOf());

    if (hr == E_INVALIDARG) {
        // The D3D11.1 runtime is not installed (for example Windows 7 without
        // the platform update). Retry without the 11_1 request.
        hr = D3D11CreateDevice(
            adapter,
            D3D_DRIVER_TYPE_UNKNOWN,
            nullptr,
            flags,
            levels + 1,
            levelCount - 1,
            D3D11_SDK_VERSION,
            device.GetAddressOf(),
            &featureLevel,
            context.GetAddressOf());
    }

    if (FAILED(hr)) {
        error = hresultToString(hr);
        return false;
    }
    return true;
}

bool testSwapchain(
    ID3D11Device* device,
    ID3D11DeviceContext* context,
    IDXGIFactory1* factory,
    std::string& error,
    UINT& width,
    UINT& height) {
    constexpr wchar_t kClassName[] = L"DX11MinecraftProbeWindow";
    HINSTANCE instance = GetModuleHandleW(nullptr);

    WNDCLASSEXW windowClass{};
    windowClass.cbSize = sizeof(windowClass);
    windowClass.lpfnWndProc = probeWndProc;
    windowClass.hInstance = instance;
    windowClass.lpszClassName = kClassName;

    if (!RegisterClassExW(&windowClass) && GetLastError() != ERROR_CLASS_ALREADY_EXISTS) {
        error = "RegisterClassExW failed: " +
            hresultToString(HRESULT_FROM_WIN32(GetLastError()));
        return false;
    }

    width = 64;
    height = 64;
    HWND window = CreateWindowExW(
        0,
        kClassName,
        L"dx11 probe",
        WS_POPUP,
        0,
        0,
        static_cast<int>(width),
        static_cast<int>(height),
        nullptr,
        nullptr,
        instance,
        nullptr);
    if (!window) {
        error = "CreateWindowExW failed: " +
            hresultToString(HRESULT_FROM_WIN32(GetLastError()));
        return false;
    }

    bool ok = false;
    {
        DXGI_SWAP_CHAIN_DESC desc{};
        desc.BufferCount = 2;
        desc.BufferDesc.Width = width;
        desc.BufferDesc.Height = height;
        desc.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        desc.BufferDesc.RefreshRate.Numerator = 60;
        desc.BufferDesc.RefreshRate.Denominator = 1;
        desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        desc.OutputWindow = window;
        desc.SampleDesc.Count = 1;
        desc.Windowed = TRUE;
        desc.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;

        ComPtr<IDXGISwapChain> swapChain;
        HRESULT hr = factory->CreateSwapChain(device, &desc, swapChain.GetAddressOf());
        if (FAILED(hr)) {
            error = "CreateSwapChain failed: " + hresultToString(hr);
        } else {
            ComPtr<ID3D11Texture2D> backBuffer;
            hr = swapChain->GetBuffer(0, IID_PPV_ARGS(backBuffer.GetAddressOf()));
            if (FAILED(hr)) {
                error = "GetBuffer failed: " + hresultToString(hr);
            } else {
                ComPtr<ID3D11RenderTargetView> renderTarget;
                hr = device->CreateRenderTargetView(
                    backBuffer.Get(), nullptr, renderTarget.GetAddressOf());
                if (FAILED(hr)) {
                    error = "CreateRenderTargetView failed: " + hresultToString(hr);
                } else {
                    const float clearColor[4] = {0.05f, 0.10f, 0.20f, 1.0f};
                    context->ClearRenderTargetView(renderTarget.Get(), clearColor);
                    context->OMSetRenderTargets(1, renderTarget.GetAddressOf(), nullptr);

                    hr = swapChain->Present(0, 0);
                    if (FAILED(hr)) {
                        error = "Present failed: " + hresultToString(hr);
                    } else {
                        ok = true;
                    }
                }
            }
        }
    }

    DestroyWindow(window);
    return ok;
}

}  // namespace

std::string runProbe() {
    ComPtr<IDXGIFactory1> factory;
    HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(factory.GetAddressOf()));
    if (FAILED(hr)) {
        return "ERROR: CreateDXGIFactory1 failed: " + hresultToString(hr);
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
            return "ERROR: EnumAdapters1 failed: " + hresultToString(hr);
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
        return "ERROR: no DXGI adapter found (hardware or WARP)";
    }

    ComPtr<ID3D11Device> device;
    ComPtr<ID3D11DeviceContext> context;
    D3D_FEATURE_LEVEL featureLevel = D3D_FEATURE_LEVEL_9_1;
    std::string error;
    const bool onSoftwareAdapter =
        softwareAdapter && adapter.Get() == softwareAdapter.Get();
    if (!createDevice(adapter.Get(), device, context, featureLevel, error)) {
        // A hardware adapter can be enumerated but still fail device creation
        // (broken driver, remote session, ...). Retry with WARP before giving up.
        if (onSoftwareAdapter || !softwareAdapter) {
            return "ERROR: D3D11CreateDevice failed: " + error;
        }
        device.Reset();
        context.Reset();
        featureLevel = D3D_FEATURE_LEVEL_9_1;
        error.clear();
        adapter = softwareAdapter;
        if (!createDevice(adapter.Get(), device, context, featureLevel, error)) {
            return "ERROR: D3D11CreateDevice failed on hardware and WARP: " + error;
        }
    }

    DXGI_ADAPTER_DESC1 adapterDesc{};
    hr = adapter->GetDesc1(&adapterDesc);
    if (FAILED(hr)) {
        return "ERROR: GetDesc1 failed: " + hresultToString(hr);
    }

    if (featureLevel < D3D_FEATURE_LEVEL_10_0) {
        return "ERROR: D3D11 feature level 10_0 or newer required, got " +
            std::string(featureLevelName(featureLevel));
    }

    D3DCompiler compiler;
    if (!compiler.load(error)) {
        return "ERROR: " + error;
    }

    if (!compileShader(compiler, "main", "vs_4_0", kVertexShader, error) ||
        !compileShader(compiler, "main", "ps_4_0", kPixelShader, error)) {
        return "ERROR: SM4.0 shader compilation failed: " + error;
    }

    const bool sm41 =
        compileShader(compiler, "main", "vs_4_1", kVertexShader, error) &&
        compileShader(compiler, "main", "ps_4_1", kPixelShader, error);

    UINT width = 0;
    UINT height = 0;
    if (!testSwapchain(device.Get(), context.Get(), factory.Get(), error, width, height)) {
        return "ERROR: swapchain probe failed: " + error;
    }

    std::ostringstream result;
    result << "OK adapter=\"" << narrow(adapterDesc.Description) << "\""
           << " vendor=0x" << hex16(adapterDesc.VendorId)
           << " device=0x" << hex16(adapterDesc.DeviceId)
           << " vram=" << (adapterDesc.DedicatedVideoMemory / (1024ull * 1024ull)) << "MB"
           << " featureLevel=" << featureLevelName(featureLevel)
           << " sm4_0=yes"
           << " sm4_1=" << (sm41 ? "yes" : "no")
           << " d3dcompiler=" << compiler.dllName
           << " swapchain=" << width << "x" << height
           << " present=yes";
    return result.str();
}

}  // namespace dx11mc
