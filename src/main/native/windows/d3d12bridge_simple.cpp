#include <jni.h>
#include <windows.h>
#include <cstdio>

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) {
    FILE* log = fopen("C:\\temp\\d3d12_init.log", "w");
    if (log) {
        fprintf(log, "nativeInit called successfully! Time: %s\n", __TIMESTAMP__);
        fclose(log);
    } else {
        // ?????? C:\temp?????????
        FILE* localLog = fopen("d3d12_init.log", "w");
        if (localLog) {
            fprintf(localLog, "nativeInit called successfully! Time: %s\n", __TIMESTAMP__);
            fclose(localLog);
        }
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeDestroy(JNIEnv* env, jobject obj) {
    // ?????????
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeRender(JNIEnv* env, jobject obj) {
    // ????
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativePresent(JNIEnv* env, jobject obj) {
    // ?????
}

JNIEXPORT void JNICALL Java_com_dx12_DX12LibClient_nativeResize(JNIEnv* env, jobject obj) {
    // ??????
}

}