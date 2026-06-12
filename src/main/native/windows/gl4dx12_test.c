#include <jni.h>
#include <windows.h>
#include <stdio.h>

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    if (fdwReason == DLL_PROCESS_ATTACH) {
        OutputDebugStringA("[GL4DX12] TEST DLL LOADED - VERSION 2\n");
    }
    return TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_client_DX12LibClient_nativeInit
    (JNIEnv* env, jclass clazz, jlong hwnd, jint width, jint height) {
    OutputDebugStringA("[GL4DX12] TEST: nativeInit called\n");
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateShader
    (JNIEnv* env, jclass clazz, jint type) {
    static int id = 100;
    id++;
    char buf[128];
    snprintf(buf, sizeof(buf), "[GL4DX12] TEST: glCreateShader type=%d returning %d\n", type, id);
    OutputDebugStringA(buf);
    return id;
}

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateProgram
    (JNIEnv* env, jclass clazz) {
    static int id = 200;
    id++;
    char buf[128];
    snprintf(buf, sizeof(buf), "[GL4DX12] TEST: glCreateProgram returning %d\n", id);
    OutputDebugStringA(buf);
    return id;
}

// ???????????
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeResize(JNIEnv* env, jclass clazz, jint w, jint h) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeRender(JNIEnv* env, jclass clazz) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativePresent(JNIEnv* env, jclass clazz) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeDestroy(JNIEnv* env, jclass clazz) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeSetEnabled(JNIEnv* env, jclass clazz, jboolean e) {}
JNIEXPORT jlong JNICALL Java_com_dx12_client_DX12LibClient_getBufferAddress(JNIEnv* env, jclass clazz, jobject buf) { return 0; }
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindTexture(JNIEnv* env, jclass clazz, jint t, jint tex) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenTextures(JNIEnv* env, jclass clazz, jint n, jlong tex) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDeleteTextures(JNIEnv* env, jclass clazz, jint n, jlong tex) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glTexImage2D(JNIEnv* env, jclass clazz, jint t, jint l, jint ifmt, jint w, jint h, jint b, jint fmt, jint type, jlong pixels) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindBuffer(JNIEnv* env, jclass clazz, jint t, jint buf) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenBuffers(JNIEnv* env, jclass clazz, jint n, jlong buf) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBufferData(JNIEnv* env, jclass clazz, jint t, jlong size, jlong data, jint usage) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUseProgram(JNIEnv* env, jclass clazz, jint prog) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glShaderSource(JNIEnv* env, jclass clazz, jint shader, jint count, jlong strings, jlong lengths) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glCompileShader(JNIEnv* env, jclass clazz, jint shader) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glAttachShader(JNIEnv* env, jclass clazz, jint prog, jint shader) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glLinkProgram(JNIEnv* env, jclass clazz, jint prog) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawElements(JNIEnv* env, jclass clazz, jint mode, jint count, jint type, jlong indices) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawArrays(JNIEnv* env, jclass clazz, jint mode, jint first, jint count) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniform1f(JNIEnv* env, jclass clazz, jint loc, jfloat v) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniformMatrix4fv(JNIEnv* env, jclass clazz, jint loc, jint count, jboolean trans, jlong val) {}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGetShaderiv(JNIEnv* env, jclass clazz, jint shader, jint pname, jlong params) {
    if (pname == 0x8B81) { int* p = (int*)params; *p = 1; }
}
JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGetProgramiv(JNIEnv* env, jclass clazz, jint prog, jint pname, jlong params) {
    if (pname == 0x8B82) { int* p = (int*)params; *p = 1; }
}
