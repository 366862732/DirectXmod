#include <jni.h>
#include <windows.h>
#include <cstdio>

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) {
    FILE* log = fopen("C:\\temp\\d3d12_test.log", "w");
    if (log) {
        fprintf(log, "Test stub nativeInit called successfully!\n");
        fclose(log);
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj) {}

}