 
#pragma comment(lib, "d3d12.lib") 
 
static ID3D12Device* g_device = nullptr; 
 
extern "C" { 
 
__declspec(dllexport) jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) { 
    if (SUCCEEDED(hr)) { 
        return JNI_TRUE; 
    } 
    return JNI_FALSE; 
} 
 
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) { 
    if (g_device) g_device-
} 
 
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {} 
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {} 
__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj, jint width, jint height) {} 
 
} 
