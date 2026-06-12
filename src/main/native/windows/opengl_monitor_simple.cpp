#include <jni.h>
#include <windows.h>
#include <cstdio>

typedef void (WINAPI *glClearColor_t)(float, float, float, float);
typedef void (WINAPI *glClear_t)(unsigned int);
typedef void (WINAPI *glDrawArrays_t)(unsigned int, int, int);

glClearColor_t real_glClearColor = nullptr;
glClear_t real_glClear = nullptr;
glDrawArrays_t real_glDrawArrays = nullptr;

void Log(const char* msg) {
    FILE* f = fopen("C:\\temp\\gl4dx12_opengl.log", "a");
    if (f) {
        fprintf(f, "%s\n", msg);
        fclose(f);
    }
}

// ????????????????????
// ??????????????? MinHook ? Detours?

extern "C" {

__declspec(dllexport) jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) {
    Log("=== GL4DX12 nativeInit called ===");
    
    HMODULE opengl32 = GetModuleHandleA("opengl32.dll");
    if (!opengl32) {
        opengl32 = LoadLibraryA("opengl32.dll");
    }
    
    if (opengl32) {
        real_glClearColor = (glClearColor_t)GetProcAddress(opengl32, "glClearColor");
        real_glClear = (glClear_t)GetProcAddress(opengl32, "glClear");
        real_glDrawArrays = (glDrawArrays_t)GetProcAddress(opengl32, "glDrawArrays");
        
        char buf[256];
        sprintf_s(buf, "glClearColor: %p", real_glClearColor);
        Log(buf);
        sprintf_s(buf, "glClear: %p", real_glClear);
        Log(buf);
        sprintf_s(buf, "glDrawArrays: %p", real_glDrawArrays);
        Log(buf);
        
        Log("OpenGL functions located. Ready to intercept.");
    } else {
        Log("Failed to load opengl32.dll");
    }
    
    return JNI_TRUE;
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {
    Log("nativeDestroy called");
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {
    // ???????????
}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {}

__declspec(dllexport) void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj, jint width, jint height) {}

}