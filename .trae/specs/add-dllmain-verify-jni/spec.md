# 添加 DllMain + 验证 JNI 签名 Spec

## Why
当前 DLL 缺少 `DllMain` 入口点，无法在 `DLL_PROCESS_ATTACH` 时自动输出日志。如果 Java 侧 `System.loadLibrary()` 后因 NPE 等原因未能调用 `nativeInit()`，DebugView 将看不到任何 DLL 加载确认日志，导致无法判断 DLL 是否被成功加载。

## What Changes
- 在 `d3d12bridge.cpp` 的 include 区域之后添加 `DllMain` 函数，在 `DLL_PROCESS_ATTACH` 和 `DLL_PROCESS_DETACH` 时输出 `[GL4DX12]` 日志
- 验证 Java 侧 `nativeInit` 声明与 C++ 端 `nativeInit` 实现的 JNI 签名完全一致

## Impact
- Affected specs: 无
- Affected code: `d3d12bridge.cpp`, `DX12LibClient.java`, `D3D12Bridge.java`

## ADDED Requirements
### Requirement: DllMain 入口点
系统 SHALL 在 `d3d12bridge.cpp` 中包含 `DllMain` 函数，在 DLL 加载/卸载时通过 `OutputDebugStringA` 输出确认日志。

#### Scenario: DLL 加载成功
- **WHEN** Minecraft 通过 `System.loadLibrary` 加载 `gl4dx12.dll`
- **THEN** DebugView 中显示 `[GL4DX12] DLL_LOADED via DllMain`

#### Scenario: DLL 卸载
- **WHEN** 游戏退出，`gl4dx12.dll` 被卸载
- **THEN** DebugView 中显示 `[GL4DX12] DLL_UNLOADED`

### Requirement: JNI 签名一致性验证
系统 SHALL 确保 Java 侧 `nativeInit` 声明与 C++ 端实现签名匹配。

#### Scenario: 签名匹配
- **WHEN** Java 调用 `DX12LibClient.nativeInit(long hwnd)`
- **THEN** C++ 端 `Java_com_dx12_DX12LibClient_nativeInit(JNIEnv*, jclass, jlong)` 被正确调用
