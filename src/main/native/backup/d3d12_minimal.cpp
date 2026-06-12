#include <jni.h>

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) {
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {}
JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj) {}

}