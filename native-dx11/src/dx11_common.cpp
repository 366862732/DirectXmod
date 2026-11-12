#include "dx11_common.h"

#include <cstdio>

namespace dx11mc {

std::string narrow(const wchar_t* text) {
    if (!text) {
        return {};
    }
    const int size = WideCharToMultiByte(CP_UTF8, 0, text, -1, nullptr, 0, nullptr, nullptr);
    if (size <= 1) {
        return {};
    }
    std::string result(static_cast<size_t>(size - 1), '\0');
    WideCharToMultiByte(CP_UTF8, 0, text, -1, result.data(), size, nullptr, nullptr);
    return result;
}

D3DCompiler::~D3DCompiler() {
    reset();
}

void D3DCompiler::reset() {
    if (module) {
        FreeLibrary(module);
        module = nullptr;
    }
    compile = nullptr;
    dllName.clear();
}

bool D3DCompiler::load(std::string& error) {
    reset();

    const wchar_t* const candidates[] = {
        L"d3dcompiler_47.dll",
        L"d3dcompiler_46.dll",
        L"d3dcompiler_43.dll",
    };

    for (const wchar_t* candidate : candidates) {
        HMODULE loaded = LoadLibraryW(candidate);
        if (!loaded) {
            continue;
        }
        auto fn = reinterpret_cast<D3DCompileFn>(GetProcAddress(loaded, "D3DCompile"));
        if (!fn) {
            FreeLibrary(loaded);
            continue;
        }
        module = loaded;
        compile = fn;
        dllName = narrow(candidate);
        return true;
    }

    error = "d3dcompiler_47.dll, d3dcompiler_46.dll and d3dcompiler_43.dll are all unavailable";
    return false;
}

std::string hresultToString(HRESULT hr) {
    char buffer[64];
    std::snprintf(buffer, sizeof(buffer), "0x%08lX", static_cast<unsigned long>(hr));
    std::string result(buffer);

    switch (hr) {
        case S_OK:
            result += " (S_OK)";
            break;
        case E_INVALIDARG:
            result += " (E_INVALIDARG)";
            break;
        case E_OUTOFMEMORY:
            result += " (E_OUTOFMEMORY)";
            break;
        case E_FAIL:
            result += " (E_FAIL)";
            break;
        case DXGI_ERROR_INVALID_CALL:
            result += " (DXGI_ERROR_INVALID_CALL)";
            break;
        case DXGI_ERROR_NOT_FOUND:
            result += " (DXGI_ERROR_NOT_FOUND)";
            break;
        case DXGI_ERROR_UNSUPPORTED:
            result += " (DXGI_ERROR_UNSUPPORTED)";
            break;
        case DXGI_ERROR_DEVICE_REMOVED:
            result += " (DXGI_ERROR_DEVICE_REMOVED)";
            break;
        case DXGI_ERROR_DEVICE_RESET:
            result += " (DXGI_ERROR_DEVICE_RESET)";
            break;
        case DXGI_ERROR_DRIVER_INTERNAL_ERROR:
            result += " (DXGI_ERROR_DRIVER_INTERNAL_ERROR)";
            break;
        case DXGI_ERROR_NOT_CURRENTLY_AVAILABLE:
            result += " (DXGI_ERROR_NOT_CURRENTLY_AVAILABLE)";
            break;
        case DXGI_ERROR_WAS_STILL_DRAWING:
            result += " (DXGI_ERROR_WAS_STILL_DRAWING)";
            break;
        default:
            break;
    }
    return result;
}

const char* featureLevelName(D3D_FEATURE_LEVEL level) {
    switch (level) {
        case D3D_FEATURE_LEVEL_9_1:
            return "D3D_FEATURE_LEVEL_9_1";
        case D3D_FEATURE_LEVEL_9_2:
            return "D3D_FEATURE_LEVEL_9_2";
        case D3D_FEATURE_LEVEL_9_3:
            return "D3D_FEATURE_LEVEL_9_3";
        case D3D_FEATURE_LEVEL_10_0:
            return "D3D_FEATURE_LEVEL_10_0";
        case D3D_FEATURE_LEVEL_10_1:
            return "D3D_FEATURE_LEVEL_10_1";
        case D3D_FEATURE_LEVEL_11_0:
            return "D3D_FEATURE_LEVEL_11_0";
        case D3D_FEATURE_LEVEL_11_1:
            return "D3D_FEATURE_LEVEL_11_1";
#ifdef D3D_FEATURE_LEVEL_12_0
        case D3D_FEATURE_LEVEL_12_0:
            return "D3D_FEATURE_LEVEL_12_0";
#endif
#ifdef D3D_FEATURE_LEVEL_12_1
        case D3D_FEATURE_LEVEL_12_1:
            return "D3D_FEATURE_LEVEL_12_1";
#endif
#ifdef D3D_FEATURE_LEVEL_12_2
        case D3D_FEATURE_LEVEL_12_2:
            return "D3D_FEATURE_LEVEL_12_2";
#endif
        default:
            return "D3D_FEATURE_LEVEL_UNKNOWN";
    }
}

}  // namespace dx11mc
