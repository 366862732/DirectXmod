#include <jni.h>
#include <windows.h>
#include <iostream>

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    if (fdwReason == DLL_PROCESS_ATTACH) {
        // 分配控制台（可选）
        AllocConsole();
        FILE* f;
        freopen_s(&f, "CONOUT$", "w", stdout);
        std::cout << "[GL4DX12] DLL loaded" << std::endl;
    }
    return TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_dx12_client_DX12LibClient_nativeInit
    (JNIEnv* env, jclass clazz, jlong hwnd, jint width, jint height) {
    std::cout << "[GL4DX12] nativeInit called" << std::endl;
    return JNI_TRUE;
}

// ... 其他函数同理，添加 std::cout 输出