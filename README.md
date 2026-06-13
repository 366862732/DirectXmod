# D3D12 Bridge DLL - AI-D3D12 生成

## 功能特性
- D3D12 设备初始化
- 交换链创建（双缓冲，800x600）
- 命令队列和命令列表管理
- 渲染目标视图（RTV）描述符堆
- GPU 同步（围栏机制）
- 红色背景清屏操作

## 编译
```cmd
build.bat
测试
cmd
rundll32.exe gl4dx12.dll,nativeRender
技术规格
DirectX 版本: D3D12

特性级别: 12_0

清屏颜色: 红色 (1.0f, 0.0f, 0.0f, 1.0f)

窗口尺寸: 800x600

显示时长: 3秒

*由 AI-D3D12 生成*
