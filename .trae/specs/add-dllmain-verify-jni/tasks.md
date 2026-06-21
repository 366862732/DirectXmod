# Tasks

- [x] Task 1: 在 d3d12bridge.cpp 添加 DllMain
  - 在 include 区域之后、`using namespace Microsoft::WRL;` 之前插入 `DllMain` 函数
  - `DLL_PROCESS_ATTACH` 时输出 `[GL4DX12] DLL_LOADED via DllMain`
  - `DLL_PROCESS_DETACH` 时输出 `[GL4DX12] DLL_UNLOADED`

- [x] Task 2: 验证 JNI 签名一致性
  - 检查 `DX12LibClient.java` 中 `nativeInit` 声明
  - 检查 `D3D12Bridge.java` 中调用 `nativeInit` 的代码
  - 确认 C++ 端 `Java_com_dx12_DX12LibClient_nativeInit(JNIEnv*, jclass, jlong)` 签名匹配

# Task Dependencies
- 无依赖，两个任务可并行执行
