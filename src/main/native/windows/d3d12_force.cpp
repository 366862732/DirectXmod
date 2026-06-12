#include <jni.h>
#include <windows.h>
#include <d3d12.h>
#include <dxgi1_4.h>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")

static ID3D12Device* g_testDevice = nullptr;

extern "C" {

__declspec(dllexport) jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj, jlong hwnd, jint width, jint height) {
    HRESULT hr = D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&g_testDevice));
    if (SUCCEEDED(hr) && g_testDevice) {
        g_testDevice->Release();
        g_testDevice = nullptr;
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {}
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {}
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {}
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj, jint width, jint height) {}

}