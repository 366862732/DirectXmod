# Checklist

- [x] DllMain 函数已添加到 d3d12bridge.cpp 的 include 区域之后
- [x] DLL_PROCESS_ATTACH 分支输出 `[GL4DX12] DLL_LOADED via DllMain`
- [x] DLL_PROCESS_DETACH 分支输出 `[GL4DX12] DLL_UNLOADED`
- [x] Java 侧 `DX12LibClient.nativeInit` 声明为 `public static native boolean nativeInit(long hwnd)`
- [x] C++ 侧 `nativeInit` 实现签名为 `Java_com_dx12_DX12LibClient_nativeInit(JNIEnv*, jclass, jlong)`
- [x] Java 调用 `nativeInit` 传入 `jlong hwnd` 参数类型匹配
