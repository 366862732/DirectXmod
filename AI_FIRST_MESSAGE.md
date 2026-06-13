# 🤖 GL4DX12 - AI 第一句话模板

欢迎参与 GL4DX12 项目！以下是让你快速上手的必要信息。

## 🔧 环境配置

| 项目 | 路径/版本 |
|------|----------|
| **项目根目录** | `D:\dx12-lib-template-26.1.2` |
| **JDK** | Java 25（`C:\Program Files\BellSoft\LibericaJDK-25-Full`） |
| **Gradle** | 9.5.1（`D:\gradle-9.5.1\bin\gradle.bat`） |
| **VS 编译器** | Visual Studio 2022（`cl.exe` 需在 Developer Command Prompt 中运行） |
| **Minecraft 路径** | `D:\.minecraft\versions\26.1.2\26.1.2.jar` |
| **mods 文件夹** | `D:\.minecraft\mods\` |
| **GitHub 仓库** | https://github.com/366862732/DirectXmod |

## 📁 关键文件位置

| 文件 | 路径 |
|------|------|
| C++ DLL 源码 | `src/main/native/windows/d3d12bridge.cpp` |
| Java 源码 | `src/main/java/com/dx12/` |
| 编译产物 | `build/libs/gl4dx12-1.0.0.jar` |
| DLL 日志 | `C:\temp\gl4dx12_d3d12.log` |
| Minecraft 日志 | `D:\.minecraft\logs\latest.log` |

## 🔨 常用命令

```powershell
# 编译 Java + 打包 JAR
cd D:\dx12-lib-template-26.1.2
D:\gradle-9.5.1\bin\gradle.bat build

# 手动编译 DLL（需在 VS Developer Command Prompt 中）
cd src/main/native/windows
cl /nologo /EHsc /O2 /MD /LD /Fe:gl4dx12.dll d3d12bridge.cpp /link d3d12.lib dxgi.lib

# 部署到 mods 文件夹
copy build\libs\gl4dx12-1.0.0.jar D:\.minecraft\mods\

# 查看日志
Get-Content D:\.minecraft\logs\latest.log -Tail 50
Get-Content C:\temp\gl4dx12_d3d12.log
📚 必读文档
README.md — 16 个踩坑记录，必读！

AI_GUIDE.md — AI 协作者指南

AI_COLLABORATION.md — 多 AI 协作总纲

🎯 你的任务
请查看 Issues 列表，选择一个任务：

https://github.com/366862732/DirectXmod/issues

推荐顺序：

Issue #4（D3D12 渲染后端）— 核心任务，优先级最高

Issue #1（opengl32.dll 代理）— 需要配合 #4

Issue #3（状态追踪器）— 可独立开发

Issue #2（GLSL 编译器）— 可独立开发

💬 开始工作
认领任务后，请：

在对应 Issue 下回复 /take

创建分支：git checkout -b task/编号-任务名

提交 PR 时关联 Issue

有任何问题在 Issue 中讨论！
