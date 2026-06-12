好的！我把所有踩过的坑都加进去，让后来者少走弯路。

```powershell
cd D:\dx12-lib-template-26.1.2

@'
# GL4DX12 - Minecraft OpenGL to DirectX 12 转换模组

> 将 Minecraft 26.1.2 的 OpenGL 渲染调用实时转换为 DirectX 12 的 Fabric 模组

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-green)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-blue)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-25-red)](https://bell-sw.com/)

---

## 📋 目录

- [项目简介](#项目简介)
- [技术架构](#技术架构)
- [项目状态](#项目状态)
- [⚠️ 踩坑记录（必读！）](#️-踩坑记录必读)
- [详细技术笔记](#详细技术笔记)
- [待解决问题](#待解决问题)
- [构建指南](#构建指南)
- [调试指南](#调试指南)
- [项目结构](#项目结构)
- [参考资料](#参考资料)

---

## 🎯 项目简介

### 核心目标
将 Minecraft 的所有 OpenGL 调用（通过 LWJGL）拦截并转换为 DirectX 12 命令，实现：
- ✅ 完整的 D3D12 渲染后端
- ✅ 高性能（相比 OpenGL）
- ✅ 兼容所有 Minecraft 功能

### 为什么需要这个模组？
- Minecraft 使用 OpenGL，性能有限
- DirectX 12 提供更低的驱动开销和更好的多线程支持
- 可以实现高级渲染特性（光线追踪等）

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     Minecraft (Java 25)                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              LWJGL (OpenGL 调用)                         │    │
│  │  glGenBuffers() / glBindBuffer() / glDrawArrays() ...   │    │
│  └───────────────────────┬─────────────────────────────────┘    │
│                          │                                       │
│                          ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Mixin 拦截层 (Fabric Mixin)                 │    │
│  │  • 拦截所有 OpenGL 调用                                  │    │
│  │  • 取消原始 OpenGL 执行                                  │    │
│  │  • 转发到 D3D12 替代实现                                 │    │
│  └───────────────────────┬─────────────────────────────────┘    │
│                          │                                       │
│                          ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              JNI 桥接层 (Java)                          │    │
│  │  • nativeInit()     - 初始化 D3D12                       │    │
│  │  • nativeRender()   - 每帧渲染                          │    │
│  │  • nativePresent()  - 交换链呈现                        │    │
│  │  • nativeResize()   - 窗口大小改变                      │    │
│  │  • nativeDestroy()  - 清理资源                          │    │
│  └───────────────────────┬─────────────────────────────────┘    │
│                          │                                       │
│                    JNI 调用 (Java ↔ C++)                         │
│                          ▼                                       │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     DLL (C++ / D3D12)                            │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              D3D12 核心渲染器                            │    │
│  │  • ID3D12Device      - 设备                              │    │
│  │  • ID3D12CommandQueue - 命令队列                         │    │
│  │  • IDXGISwapChain3   - 交换链                            │    │
│  │  • ID3D12GraphicsCommandList - 命令列表                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              资源转换 (待实现)                           │    │
│  │  • 顶点/索引缓冲区 → D3D12 缓冲区                        │    │
│  │  • 纹理 → D3D12 纹理                                     │    │
│  │  • GLSL → HLSL → DXIL (着色器编译)                       │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GPU (DirectX 12)                             │
│                    渲染最终画面                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 项目状态

### ✅ 已完成 (2026-06-12)

| 组件 | 状态 | 验证方法 |
|------|------|----------|
| Fabric 模组框架 | ✅ | 成功加载到游戏 |
| JNI 桥接层 | ✅ | `nativeInit()` 返回 `true` |
| DLL 动态加载 | ✅ | 从 JAR 提取并加载成功 |
| 模组入口点 | ✅ | `ClientModInitializer` 正常工作 |
| 项目结构 | ✅ | Maven 标准布局 |
| GitHub 仓库 | ✅ | 代码已同步 |

### ⚠️ 部分完成

| 组件 | 状态 | 阻塞原因 |
|------|------|----------|
| D3D12 设备创建 | 🔄 | 需要验证 `D3D12CreateDevice` 是否成功 |
| Mixin 拦截 | 🔄 | 依赖配置问题 |
| 窗口句柄获取 | 🔄 | Minecraft 窗口类名未知 |

### ❌ 待实现

- [ ] Mixin 配置正确加载
- [ ] OpenGL 函数拦截 (glClearColor, glClear, glDrawArrays)
- [ ] D3D12 交换链创建
- [ ] 每帧渲染循环
- [ ] 顶点缓冲区转换
- [ ] 纹理转换
- [ ] 着色器转换 (GLSL → HLSL)
- [ ] F3 调试屏幕信息修改

---

## ⚠️ 踩坑记录（必读！）

> 以下是我在这个项目中遇到的所有坑，希望下一个 AI 不要再踩一遍！

### 坑1: Java 版本问题

**症状**: PowerShell 执行 javac 时闪退，或编译失败

**原因**: Minecraft 26.1.2 需要 **Java 25**，不是 Java 17 或 Java 21

**解决**:
```powershell
# 下载 Liberica JDK 25
$url = "https://download.bell-sw.com/java/25.0.1+14/bellsoft-jdk25.0.1+14-windows-amd64.msi"
Invoke-WebRequest -Uri $url -OutFile "$env:TEMP\jdk25.msi"
msiexec /i "$env:TEMP\jdk25.msi" /quiet

# 设置环境变量
$env:JAVA_HOME = "C:\Program Files\BellSoft\LibericaJDK-25-Full"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

---

### 坑2: ClientModInitializer vs ModInitializer

**症状**: 
```
Class com.dx12.Dx12Mod cannot be cast to net.fabricmc.api.ClientModInitializer!
```

**原因**: 实现了 `ModInitializer` 而不是 `ClientModInitializer`

**解决**:
```java
// ❌ 错误
public class Dx12Mod implements ModInitializer

// ✅ 正确
public class Dx12Mod implements ClientModInitializer
```

---

### 坑3: JAR 打包遗漏 class 文件

**症状**: 
```
java.lang.ClassNotFoundException: com.dx12.Dx12Mod
```

**原因**: 打包时只复制了资源文件，没有包含编译后的 `.class` 文件

**解决**: 必须先编译 Java 生成 `.class`，再打包
```powershell
# 正确的打包流程
javac -d build/classes src/main/java/com/dx12/*.java
Copy-Item -Path "src/main/resources/*" -Destination "build/classes/" -Recurse
jar cf gl4dx12.jar -C build/classes .
```

---

### 坑4: DLL 文件被占用

**症状**: `AccessDeniedException` 或无法覆盖 DLL

**原因**: Minecraft 正在运行，DLL 被加载到进程中

**解决**: 关闭 Minecraft 后再编译/复制 DLL

---

### 坑5: JNI 函数签名不匹配

**症状**: `UnsatisfiedLinkError: no matching JNI native method found`

**原因**: C++ 函数名与 Java 声明不匹配

**规则**: 
- Java: `public static native boolean nativeInit(long hwnd, int width, int height);`
- C++: `Java_com_dx12_DX12LibClient_nativeInit(JNIEnv*, jobject, jlong, jint, jint)`

**检查方法**:
```bash
# 查看 DLL 导出的函数
dumpbin /exports gl4dx12.dll | findstr Java
```

---

### 坑6: Mixin 编译失败 - 找不到 sponge-mixin

**症状**: 
```
error: package org.spongepowered.asm.mixin does not exist
```

**原因**: classpath 中缺少 sponge-mixin JAR

**解决**:
```powershell
# 下载 sponge-mixin
Invoke-WebRequest -Uri "https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.17.3+mixin.0.8.7/sponge-mixin-0.17.3+mixin.0.8.7.jar" -OutFile "libs/sponge-mixin.jar"

# 编译时添加
javac -cp "libs/fabric-loader-0.19.2.jar;libs/sponge-mixin.jar" ...
```

---

### 坑7: Mixin 配置文件格式错误

**症状**: 
```
JsonSyntaxException: Expected BEGIN_OBJECT but was STRING at line 1 column 1
```

**原因**: JSON 文件有 BOM 或格式错误

**解决**: 使用 UTF-8 without BOM 编码，确保 JSON 格式正确
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.dx12.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["GLInterceptor"]
}
```

---

### 坑8: DLL 日志文件不生成

**症状**: `fopen` 写入 `C:\temp\` 失败

**原因**: 权限问题或目录不存在

**解决**: 使用 Minecraft 目录或 `OutputDebugString`
```cpp
// 方法1: 使用 OutputDebugString (可在 DebugView 中查看)
OutputDebugStringA("Debug message");

// 方法2: 写入 Minecraft 目录
char path[MAX_PATH];
GetCurrentDirectoryA(MAX_PATH, path);
strcat(path, "\\gl4dx12.log");
FILE* f = fopen(path, "a");
```

---

### 坑9: Gradle 找不到 fabric-loom

**症状**: 
```
Plugin [id: 'fabric-loom', version: '1.9.2'] was not found
```

**原因**: 插件仓库未配置或网络问题

**解决**: 在 `settings.gradle` 中添加 `pluginManagement`
```gradle
pluginManagement {
    repositories {
        maven { url = "https://maven.fabricmc.net/" }
        gradlePluginPortal()
    }
}
```

---

### 坑10: BOM 字符导致编译失败

**症状**: 
```
error: illegal character: '\ufeff'
```

**原因**: 文件以 UTF-8 with BOM 保存

**解决**: 使用 ASCII 或 UTF-8 without BOM 编码
```powershell
# PowerShell 保存为无 BOM
$content | Out-File -FilePath "file.java" -Encoding ascii -NoNewline
```

---

### 坑11: LWJGL 类找不到

**症状**: 
```
error: package org.lwjgl.opengl does not exist
```

**原因**: classpath 中没有 LWJGL JAR

**解决**: 添加 LWJGL 依赖
```powershell
$lwjglCore = "D:\.minecraft\libraries\org\lwjgl\lwjgl\3.4.1\lwjgl-3.4.1.jar"
$lwjglOpengl = "D:\.minecraft\libraries\org\lwjgl\lwjgl-opengl\3.4.1\lwjgl-opengl-3.4.1.jar"
javac -cp "$lwjglCore;$lwjglOpengl" ...
```

---

### 坑12: Mixin 目标类名不匹配

**症状**: `@Mixin target not found`

**原因**: Minecraft 26.1.2 使用的 LWJGL 类名与旧版不同

**可能的目标类**:
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL11C`
- `org.lwjgl.opengl.GLUtil`

**调试方法**:
```java
// 在 Java 端打印实际类名
System.out.println(Class.forName("org.lwjgl.opengl.GL11"));
```

---

### 坑13: DLL 优化导致代码被移除

**症状**: DLL 很小（< 10KB），D3D12 代码未执行

**原因**: 编译器优化掉了未使用的代码

**解决**: 
```cpp
// 使用 volatile 防止优化
volatile int _ = 0;

// 或禁用优化编译
cl /Od ...
```

---

### 坑14: PowerShell 换行符导致命令失败

**症状**: `参数列表中缺少参量`

**原因**: PowerShell 的换行符处理与 CMD 不同

**解决**: 使用单行命令或批处理文件
```powershell
# 单行命令（用分号分隔）
cd dir; $env:Path = "..."; g++ -shared ...
```

---

### 坑15: fabric.mod.json 版本变量未替换

**症状**: 
```
Mod gl4dx12 uses the version ${version}
```

**原因**: `fabric.mod.json` 中的 `${version}` 没有被 Gradle 替换

**解决**: 
- 使用 Gradle 构建会自动替换
- 或直接写死版本号 `"version": "1.0.0"`

---

### 坑16: 窗口句柄获取失败

**症状**: D3D12 初始化失败，交换链创建错误

**原因**: `FindWindow` 找不到 Minecraft 窗口

**解决**: 多种方法尝试
```cpp
// 方法1: 精确标题
HWND hwnd = FindWindowA(NULL, "Minecraft 26.1.2");

// 方法2: LWJGL 类名
hwnd = FindWindowA("LWJGL", NULL);

// 方法3: 枚举所有窗口
EnumWindows([](HWND hwnd, LPARAM lParam) -> BOOL {
    char title[256];
    GetWindowTextA(hwnd, title, 256);
    if (strstr(title, "Minecraft")) {
        *(HWND*)lParam = hwnd;
        return FALSE;
    }
    return TRUE;
}, (LPARAM)&hwnd);
```

---

## 📝 详细技术笔记

### 1. 环境配置

#### Java 25 (必需)
```bash
# 下载 Liberica JDK 25
https://bell-sw.com/pages/downloads/#jdk-25

# 设置环境变量
JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full
PATH=%JAVA_HOME%\bin;%PATH%
```

#### Minecraft 26.1.2
- **版本 ID**: 26.1.2 (2026年4月9日发布)
- **Java 版本**: 25
- **Fabric Loader**: 0.19.3
- **安装路径**: `D:\.minecraft`

#### LWJGL 版本
Minecraft 26.1.2 使用 **LWJGL 3.4.1**，关键 JAR：
- `lwjgl-3.4.1.jar`
- `lwjgl-opengl-3.4.1.jar`
- `lwjgl-glfw-3.4.1.jar`

### 2. JNI 实现细节

#### Java 端 (DX12LibClient.java)
```java
package com.dx12;

public class DX12LibClient {
    static {
        // 静态块在类首次加载时执行
        NativeUtils.loadLibraryFromJar("/native/windows/gl4dx12.dll");
    }
    
    // native 方法声明
    public static native boolean nativeInit();
    public static native void nativeDestroy();
    public static native void nativeRender();
    public static native void nativePresent();
    public static native void nativeResize(int width, int height);
}
```

#### C++ 端 (d3d12bridge.cpp)
```cpp
#include <jni.h>
#include <windows.h>
#include <d3d12.h>

extern "C" {
    // 函数名必须严格匹配: Java_包名_类名_方法名
    JNIEXPORT jboolean JNICALL 
    Java_com_dx12_DX12LibClient_nativeInit(JNIEnv* env, jobject obj) {
        ID3D12Device* device = nullptr;
        HRESULT hr = D3D12CreateDevice(
            nullptr, 
            D3D_FEATURE_LEVEL_11_0, 
            IID_PPV_ARGS(&device)
        );
        return SUCCEEDED(hr) ? JNI_TRUE : JNI_FALSE;
    }
}
```

#### DLL 导出函数验证
```bash
# 使用 dumpbin 检查导出函数
dumpbin /exports gl4dx12.dll | findstr Java

# 预期输出:
#   1    0 00001000 Java_com_dx12_DX12LibClient_nativeInit
#   2    1 00001020 Java_com_dx12_DX12LibClient_nativeDestroy
#   ...
```

### 3. DLL 加载机制

`NativeUtils.java` 实现：
1. 从 JAR 中读取 DLL 资源 (`/native/windows/gl4dx12.dll`)
2. 提取到临时目录 (`%TEMP%\gl4dx12_native\`)
3. 调用 `System.load()` 加载
4. 设置退出时删除 (`deleteOnExit()`)

### 4. D3D12 初始化流程

完整的 D3D12 初始化需要：
```cpp
// 1. 创建设备
ID3D12Device* device;
D3D12CreateDevice(nullptr, D3D_FEATURE_LEVEL_11_0, IID_PPV_ARGS(&device));

// 2. 创建命令队列
ID3D12CommandQueue* queue;
D3D12_COMMAND_QUEUE_DESC queueDesc = {};
queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
device->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(&queue));

// 3. 创建交换链
IDXGISwapChain3* swapChain;
DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
swapChainDesc.BufferCount = 2;
swapChainDesc.Width = width;
swapChainDesc.Height = height;
swapChainDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;

// 4. 创建描述符堆
ID3D12DescriptorHeap* rtvHeap;
D3D12_DESCRIPTOR_HEAP_DESC rtvHeapDesc = {};
rtvHeapDesc.NumDescriptors = 2;
rtvHeapDesc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
device->CreateDescriptorHeap(&rtvHeapDesc, IID_PPV_ARGS(&rtvHeap));

// 5. 创建命令列表和分配器
ID3D12CommandAllocator* allocator;
ID3D12GraphicsCommandList* cmdList;
device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&allocator));
device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, allocator, nullptr, IID_PPV_ARGS(&cmdList));
```

### 5. Mixin 拦截配置

#### mixins.gl4dx12.json
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.dx12.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": ["GLInterceptor"],
  "injectors": {
    "defaultRequire": 1
  }
}
```

#### fabric.mod.json (包含 mixins)
```json
{
  "entrypoints": {
    "client": ["com.dx12.Dx12Mod"]
  },
  "mixins": ["mixins.gl4dx12.json"],
  "depends": {
    "fabricloader": ">=0.19.2",
    "minecraft": ">=26.1.2",
    "java": ">=25"
  }
}
```

#### Mixin 拦截示例
```java
@Mixin(targets = "org.lwjgl.opengl.GL11")
public class GLInterceptor {
    @Inject(method = "glClearColor", at = @At("HEAD"), remap = false)
    private static void onGlClearColor(float r, float g, float b, float a, CallbackInfo ci) {
        System.out.println("glClearColor intercepted!");
    }
}
```

---

## 🐛 待解决问题

### 问题1: Mixin 编译失败
**错误信息**:
```
error: package org.spongepowered.asm.mixin does not exist
```

**原因**: classpath 中缺少 sponge-mixin JAR

**解决方案**:
```bash
# 下载 sponge-mixin
curl -o libs/sponge-mixin.jar https://maven.fabricmc.net/net/fabricmc/sponge-mixin/0.17.3+mixin.0.8.7/sponge-mixin-0.17.3+mixin.0.8.7.jar

# 编译时添加
javac -cp "libs/fabric-loader-0.19.2.jar;libs/sponge-mixin.jar" ...
```

### 问题2: D3D12 日志缺失
**现象**: DLL 中的日志函数没有输出

**原因**: 
- 文件写入权限问题
- `fopen` 失败
- 日志函数未被调用

**解决方案**:
```cpp
// 使用 OutputDebugString 输出到调试器
OutputDebugStringA(msg);

// 或使用 MessageBox 弹出对话框
MessageBoxA(NULL, msg, "GL4DX12", MB_OK);

// 或写入到 Minecraft 目录
char path[MAX_PATH];
GetModuleFileNameA(NULL, path, MAX_PATH);
strcat(path, "\\gl4dx12.log");
```

### 问题3: 窗口句柄获取
**需要的窗口句柄**: Minecraft 主窗口的 HWND

**当前方法** (可能失效):
```cpp
HWND hwnd = FindWindowA(NULL, "Minecraft 26.1.2");
HWND hwnd = FindWindowA("LWJGL", NULL);
```

**替代方案**:
1. 通过 JNI 从 Java 传递 `MinecraftClient.getInstance().getWindow().getHandle()`
2. 使用 `EnumWindows` 枚举所有窗口
3. 获取前景窗口 `GetForegroundWindow()` (仅当游戏在前台时)

### 问题4: LWJGL 类名
Minecraft 26.1.2 可能使用不同的 LWJGL 类名：
- `org.lwjgl.opengl.GL11`
- `org.lwjgl.opengl.GL11C`
- `org.lwjgl.opengl.GLUtil`

需要运行时验证：
```java
System.out.println(Class.forName("org.lwjgl.opengl.GL11"));
```

---

## 🔧 构建指南

### 手动编译 (当前使用)

```bash
# 1. 设置 Java 25
set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full
set PATH=%JAVA_HOME%\bin;%PATH%

# 2. 编译 Java
javac -cp "libs/fabric-loader-0.19.2.jar" -d build/classes ^
    src/main/java/com/dx12/Dx12Mod.java ^
    src/main/java/com/dx12/DX12LibClient.java ^
    src/main/java/com/dx12/NativeUtils.java ^
    src/main/java/com/dx12/client/D3D12Bridge.java

# 3. 复制资源
xcopy /E /Y src\main\resources\* build\classes\

# 4. 打包 JAR
cd build/classes
jar cf ../gl4dx12.jar *
cd ../..

# 5. 部署到 mods
copy build\gl4dx12.jar D:\.minecraft\mods\
```

### DLL 编译 (VS 2022)

```bash
# 打开 Developer Command Prompt for VS 2022

cd D:\dx12-lib-template-26.1.2\src\main\native\windows

set JAVA_HOME=C:\Program Files\BellSoft\LibericaJDK-25-Full

# 编译
cl /nologo /EHsc /O2 /MD /LD /Fe:gl4dx12.dll ^
    /I"%JAVA_HOME%\include" ^
    /I"%JAVA_HOME%\include\win32" ^
    d3d12bridge.cpp ^
    /link d3d12.lib dxgi.lib user32.lib

# 复制到资源目录
copy gl4dx12.dll ..\..\resources\native\windows\
```

### Gradle 构建 (推荐，但需修复)

```bash
# 修复 build.gradle 后
./gradlew clean build
```

---

## 🔍 调试指南

### 1. 检查模组是否加载
查看 `D:\.minecraft\logs\latest.log`:
```
[Render thread/INFO]: [STDOUT]: [GL4DX12] Mod Initializing (Client)...
[Render thread/INFO]: [STDOUT]: [GL4DX12] nativeInit returned: true
```

### 2. 检查 DLL 是否加载
```
[Render thread/INFO]: [STDOUT]: [NativeUtils] ✓ DLL loaded successfully
```

### 3. 检查 DLL 导出函数
```bash
dumpbin /exports src/main/resources/native/windows/gl4dx12.dll | findstr Java
```

### 4. 调试 DLL 内部
在 C++ 代码中添加：
```cpp
// 写入日志文件
FILE* f = fopen("C:\\temp\\d3d12_debug.log", "w");
fprintf(f, "Debug message\n");
fclose(f);

// 或弹出消息框
MessageBoxA(NULL, "Checkpoint reached", "GL4DX12 Debug", MB_OK);
```

### 5. 常见错误及解决

| 错误 | 原因 | 解决 |
|------|------|------|
| `ClassNotFoundException: com.dx12.Dx12Mod` | JAR 中没有 class 文件 | 确保编译后再打包 |
| `UnsatisfiedLinkError` | DLL 导出函数名不匹配 | 检查 JNI 函数签名 |
| `AccessDeniedException` | DLL 文件被占用 | 关闭 Minecraft 后重试 |
| `MixinInitialisationError` | mixins.json 格式错误 | 检查 JSON 语法 |

---

## 📁 项目结构

```
GL4DX12/
├── .github/
│   └── workflows/          # GitHub Actions (未配置)
├── .idea/                  # IntelliJ IDEA 配置
├── .vscode/                # VS Code 配置
├── gradle/wrapper/         # Gradle Wrapper
├── libs/                   # 依赖 JAR
│   ├── fabric-loader-0.19.2.jar
│   └── sponge-mixin-*.jar
├── src/main/
│   ├── java/com/dx12/
│   │   ├── client/
│   │   │   └── D3D12Bridge.java      # JNI 桥接
│   │   ├── mixin/
│   │   │   └── GLInterceptor.java    # Mixin 拦截
│   │   ├── DX12LibClient.java        # native 方法
│   │   ├── Dx12Mod.java              # 模组入口
│   │   └── NativeUtils.java          # DLL 加载
│   ├── native/windows/
│   │   ├── d3d12bridge.cpp           # D3D12 实现
│   │   ├── d3d12bridge.h             # 头文件
│   │   └── MinHook.h                 # 挂钩库 (可选)
│   └── resources/
│       ├── assets/
│       │   └── gl4dx12/
│       │       └── icon.png
│       ├── native/windows/
│       │   └── gl4dx12.dll           # 编译好的 DLL
│       ├── fabric.mod.json           # Fabric 配置
│       └── mixins.gl4dx12.json       # Mixin 配置
├── build/                   # 编译输出
│   ├── classes/             # .class 文件
│   └── libs/                # 生成的 JAR
├── run/                     # Minecraft 运行目录
├── .gitignore
├── build.gradle
├── gradle.properties
├── gradlew.bat
├── README.md
└── settings.gradle
```

---

## 📚 参考资料

### 官方文档
- [Fabric Documentation](https://fabricmc.net/wiki/start)
- [JNI Specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/index.html)
- [DirectX 12 Programming Guide](https://docs.microsoft.com/en-us/windows/win32/direct3d12/direct3d-12-graphics)
- [LWJGL 3 Documentation](https://www.lwjgl.org/guide)

### 相关项目
- [ANGLE](https://chromium.googlesource.com/angle/angle) - OpenGL ES to DirectX 转换
- [Zink](https://gitlab.freedesktop.org/mesa/mesa/-/tree/main/src/gallium/drivers/zink) - OpenGL to Vulkan
- [DXVK](https://github.com/doitsujin/dxvk) - DirectX 9/10/11 to Vulkan

### 工具
- [RenderDoc](https://renderdoc.org/) - 图形调试器
- [PIX](https://devblogs.microsoft.com/pix/) - DirectX 12 调试工具
- [dumpbin](https://docs.microsoft.com/en-us/cpp/build/reference/dumpbin-reference) - 检查 DLL 导出

---

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE)

---

## 👤 作者

366862732

---

## 🙏 致谢

- Fabric 社区
- Microsoft DirectX 团队
- LWJGL 开发者
- 所有测试和贡献者

---

## 📅 更新日志

### 2026-06-12
- ✅ 模组框架搭建完成
- ✅ JNI 调用成功
- ✅ 项目整理并推送到 GitHub
- 📝 编写详细文档（包含踩坑记录）

### 计划中
- [ ] 实现 Mixin 拦截
- [ ] 完成 D3D12 初始化
- [ ] 实现第一个 OpenGL 函数转换
- [ ] 发布 v1.0.0
'@ | Out-File -FilePath "README.md" -Encoding utf8

Write-Host "✅ 超详细 README（含踩坑记录）已创建" -ForegroundColor Green
```

## 同步到 GitHub

```powershell
git add README.md
git commit -m "Add comprehensive documentation with all pitfalls and solutions"
git push -f origin master:main
Write-Host "✅ 已同步到 GitHub" -ForegroundColor Green
```

现在 README 包含了 **16 个踩坑记录**，每个都有：
- 症状描述
- 原因分析
- 解决方案

下一个 AI 接手时，可以先看"踩坑记录"部分，避免重复我们的痛苦经历！需要我再补充什么坑吗？