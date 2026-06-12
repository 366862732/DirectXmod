#include <jni.h>
#include <windows.h>

BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
    return TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_client_DX12LibClient_nativeInit
    (JNIEnv* env, jclass clazz, jlong hwnd, jint width, jint height) {
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeResize
    (JNIEnv* env, jclass clazz, jint width, jint height) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeRender
    (JNIEnv* env, jclass clazz) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativePresent
    (JNIEnv* env, jclass clazz) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeDestroy
    (JNIEnv* env, jclass clazz) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_nativeSetEnabled
    (JNIEnv* env, jclass clazz, jboolean enabled) {}

JNIEXPORT jlong JNICALL Java_com_dx12_client_DX12LibClient_getBufferAddress
    (JNIEnv* env, jclass clazz, jobject buffer) {
    return 0;
}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindTexture
    (JNIEnv* env, jclass clazz, jint target, jint texture) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenTextures
    (JNIEnv* env, jclass clazz, jint n, jlong textures) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDeleteTextures
    (JNIEnv* env, jclass clazz, jint n, jlong textures) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glTexImage2D
    (JNIEnv* env, jclass clazz, jint target, jint level, jint internalFormat,
     jint width, jint height, jint border, jint format, jint type, jlong pixels) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBindBuffer
    (JNIEnv* env, jclass clazz, jint target, jint buffer) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glGenBuffers
    (JNIEnv* env, jclass clazz, jint n, jlong buffers) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glBufferData
    (JNIEnv* env, jclass clazz, jint target, jlong size, jlong data, jint usage) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUseProgram
    (JNIEnv* env, jclass clazz, jint program) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawElements
    (JNIEnv* env, jclass clazz, jint mode, jint count, jint type, jlong indices) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glDrawArrays
    (JNIEnv* env, jclass clazz, jint mode, jint first, jint count) {}

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateProgram
    (JNIEnv* env, jclass clazz) { return 0; }

JNIEXPORT jint JNICALL Java_com_dx12_client_DX12LibClient_glCreateShader
    (JNIEnv* env, jclass clazz, jint type) { return 0; }

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glShaderSource
    (JNIEnv* env, jclass clazz, jint shader, jint count, jlong strings, jlong lengths) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glCompileShader
    (JNIEnv* env, jclass clazz, jint shader) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glAttachShader
    (JNIEnv* env, jclass clazz, jint program, jint shader) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glLinkProgram
    (JNIEnv* env, jclass clazz, jint program) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniform1f
    (JNIEnv* env, jclass clazz, jint location, jfloat v0) {}

JNIEXPORT void JNICALL Java_com_dx12_client_DX12LibClient_glUniformMatrix4fv
    (JNIEnv* env, jclass clazz, jint location, jint count, jboolean transpose, jlong value) {}
