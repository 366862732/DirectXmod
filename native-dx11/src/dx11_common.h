// Shared helpers for the additive D3D11 backend.
#pragma once

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef NOMINMAX
#define NOMINMAX
#endif

#include <windows.h>
#include <d3d11.h>
#include <d3d11_1.h>
#include <d3dcompiler.h>
#include <dxgi.h>
#include <wrl/client.h>

#include <string>
#include <vector>

namespace dx11mc {

using Microsoft::WRL::ComPtr;

// Converts a UTF-16 Windows string to UTF-8 for JNI / logging.
std::string narrow(const wchar_t* text);

// Human-readable HRESULT such as "0x887A0004 (DXGI_ERROR_UNSUPPORTED)".
std::string hresultToString(HRESULT hr);

// "D3D_FEATURE_LEVEL_10_1" etc.
const char* featureLevelName(D3D_FEATURE_LEVEL level);

// Optional validation clamp. A non-zero cap makes both the probe and the
// persistent device request only levels at or below it. This lets a WARP
// device that reports 11_0/12_1 exercise the real FL10.x code path.
void setFeatureLevelCap(D3D_FEATURE_LEVEL level);
D3D_FEATURE_LEVEL featureLevelCap();

// Descending list of levels to pass to D3D11CreateDevice, capped when a
// feature-level cap was set. Never empty.
std::vector<D3D_FEATURE_LEVEL> availableFeatureLevels();

using D3DCompileFn = HRESULT(WINAPI*)(
    LPCVOID pSrcData,
    SIZE_T srcDataSize,
    LPCSTR pSourceName,
    const D3D_SHADER_MACRO* pDefines,
    ID3DInclude* pInclude,
    LPCSTR pEntrypoint,
    LPCSTR pTarget,
    UINT flags1,
    UINT flags2,
    ID3DBlob** ppCode,
    ID3DBlob** ppErrorMsgs);

// Loads d3dcompiler_47/46/43.dll dynamically. Keeping this out of the import
// table means dx11_mc.dll still loads on systems where the compiler DLL is
// missing, and the failure can be reported as a probe/pipeline error.
struct D3DCompiler {
    HMODULE module = nullptr;
    D3DCompileFn compile = nullptr;
    std::string dllName;

    D3DCompiler() = default;
    D3DCompiler(const D3DCompiler&) = delete;
    D3DCompiler& operator=(const D3DCompiler&) = delete;

    ~D3DCompiler();

    bool load(std::string& error);
    void reset();
};

}  // namespace dx11mc
