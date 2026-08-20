# DX12 Backend 深度代码审查报告

> 生成时间：2026-08-21  
> 审查范围：全量 Java + C++ 原生层

---

## 一、严重问题（可能导致渲染失败或数据错误）

### BUG-01：语义名索引错位（部分管线 HLSL 语义与 native input layout 不匹配）

**文件：** [Dx12Device.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12Device.java) `buildNativeDesc` L431-433  
**文件：** [Dx12IntermediaryShaderModule.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12IntermediaryShaderModule.java) `toHlsl` L282-296

#### 现象
`buildNativeDesc` 在填充 `semanticNames` 时从 `"TEXCOORD0"` 开始递增编号：
```java
while (semanticNames.size() < inputElements.size()) {
    semanticNames.add("TEXCOORD" + semanticNames.size());  // 总是从 TEXCOORD0 开始
}
```

但 `toHlsl()` 通过 `spvc_compiler_hlsl_add_vertex_attribute_remap` 注入的语义，是基于**去重后的 SPIR-V inputs 顺序**（`inputIdx`），而不是 `inputElements` 的数量。两者计数基准不同。

#### 触发场景
以 `position_tex_color` 管线为例：

| 阶段 | 值 |
|------|------|
| SPIR-V vertex inputs（去重） | `[Position(loc=0), UV0(loc=3), Color(loc=4)]` → 3 个 |
| toHlsl() remap 结果 | `POSITION`, `TEXCOORD0`, `TEXCOORD1` ✅ |
| `buildVertexInputElements` 去重后 inputElements | 3 个（无重复）→ semanticNames 补齐到 3 ✅ |
| `gui_textured` format（含重复 UV0） | 4 个元素，但去重后 3 个 → 同样补齐到 3 ✅ |
| **某些特殊管线**（shader 声明比 element 少） | 可能产生多余 TEXCOORDn |

当前日志验证 `position_tex_color` 和 `gui_textured` 均正常，但以下情况仍有风险：

若 shader 有 2 个 vertex input（如只有 `POSITION` + `UV0`），而 `inputElements`（去重前）有 4 个，补齐后 `semanticNames = [POSITION, TEXCOORD0, TEXCOORD1, TEXCOORD2]`，但 HLSL 里只有 `TEXCOORD0`——第 3、4 个 element 没有对应语义，会导致 **`E_INVALIDARG`**。

#### 修复方向
使用与 `toHlsl()` 相同的去重逻辑来决定补充分割点的起始 index：

```java
// 在 buildNativeDesc 中，先按去重后的数量决定补充起始 index
Set<String> seenForExtra = new HashSet<>();
int extraStartIdx = 0;
for (int[] el : inputElements) {
    String name = vertexShaderInputs.get(el[0]); // 需要传入顶点 shader 输入名
    if (!seenForExtra.add(name)) continue;
    extraStartIdx++;
}
while (semanticNames.size() < inputElements.size()) {
    semanticNames.add("TEXCOORD" + extraStartIdx++);
}
```

---

### BUG-02：`rebind()` 中的 attribLocation 赋值逻辑导致纹理采样器绑定错乱

**文件：** [Dx12IntermediaryShaderModule.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12IntermediaryShaderModule.java) L181-193

```java
String previousName = null;
int attribLocation = 0;
for (int i = 0; i < inputVariables.size(); ++i) {
    String variableName = inputVariables.get(i);
    SpvVariable inputVariable = this.getInputVariable(variableName);
    if (inputVariable == null) continue;   // ← 跳过不存在的变量名
    if (!variableName.equals(previousName)) {
        spvAsIntBuffer.put(inputVariable.locationOffset(), attribLocation);
        remainingInputs.remove(variableName);
    }
    ++attribLocation;                        // ← 每次都递增，包括跳过的情形
    previousName = variableName;
}
```

当 `inputVariables` 中有连续重复的相同名称（如 `UV0, UV0`），第二次遇到时 `variableName.equals(previousName)` 为 true，**不会重新写入 location**，但 `attribLocation` 仍然递增。这意味着：

- 第 1 个 UV0 → location=0（正确）  
- 第 2 个 UV0 → 跳过，但 attribLocation 变为 1  
- 下一个新变量（如 Color）→ location=1（正确）

这在 `position_tex_uv`（UV0 出现两次）的场景下恰好是正确的。但**如果存在不连续的重复**（如 `[UV0, Position, UV0]`，虽 unlikely），则第二个 UV0 的 location 会被错误地设置为 2 而非 0。

当前 Minecraft 的 format 结构保证了重复名都是连续的，所以目前没有实际触发，但代码逻辑本身有缺陷。

---

## 二、中等问题（功能正确性 / 性能 / 可维护性）

### BUG-03：`selfTestSurface` 诊断消息与实际行为不符

**文件：** [Dx12Backend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12Backend.java) L389  
**文件：** [dx12_surface.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_surface.cpp) L406-416

```java
// Dx12Backend.java:389
System.err.println("[dx12-java] selfTestSurface: presented GREEN to real window");
```

实际 C++ 路径（`srcTex=null`）：
```cpp
if (!srcTex) {
    // 只做 PRESENT↔COPY_DEST barrier，不做任何 clear
    // 原画面内容保持不变，既不清红也不清绿
    return true;
}
```

**消息误导**："presented GREEN" 但实际是 pass-through（原画面）。如果开发者看到这个日志后认为屏幕应该是绿色，会误判调试结果。

---

### BUG-04：`Dx12Config.getInstance()` 非线程安全

**文件：** [Dx12Config.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/config/Dx12Config.java) L31-35

```java
public static Dx12Config getInstance() {
    if (instance == null) {
        instance = new Dx12Config();  // 多线程竞争窗口
    }
    return instance;
}
```

Fabric 的 init 流程中 `Dx12Mod.onInitialize()` 在 main 线程调用，理论上安全，但若未来有异步初始化路径，会产生双实例。

---

### BUG-05：`readbackSurfacePixels` 每 30 帧同步读回 GPU 数据

**文件：** [Dx12GpuSurface.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12GpuSurface.java) L123-137

```java
// 主渲染线程！
this.surface.readbackSurfacePixels(this.ctx, pixels, width, height);
```

**文件：** [dx12_surface.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_surface.cpp) L556-628

C++ 侧调用 `deviceWaitIdle()` → 阻塞直到 GPU 完成所有 pending 命令，然后拷贝 3×3 像素。这是**同步 GPU-CPU 屏障**，每 30 帧在主线程执行一次，会造成明显卡顿（尤其在 GPU 密集型场景）。

---

### BUG-06：`pushDescriptors` 中 CBV SizeInBytes 向上取整 256 导致的隐式性能损失

**文件：** [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) L2448-2453

```cpp
UINT64 cbvSize = (UINT64)b.length;
cbvSize = (cbvSize + 255) & ~255ULL;  // 最小 256 字节
if (cbvSize == 0) cbvSize = 256;
```

D3D12 要求 CBV 大小必须是 256 字节对齐，且 Shader 读到的未初始化部分可能是垃圾值。但如果 UBO 实际大小很小（如 64 字节的 Projection matrix），这会导致：
1. 浪费 192 字节 CBV 内存
2. 相邻 UBO 之间产生保护间隙，增加 descriptor heap 压力

---

### BUG-07：`createRenderPass` 每次调用打印完整 Java 栈轨迹

**文件：** [Dx12CommandEncoderBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12CommandEncoderBackend.java) L151-156

```java
System.err.println("[dx12-java] createRenderPass: " + w + "x" + h
    + " depth=" + (depthTexture != null) + " from:"
    + Thread.currentThread().getStackTrace()[2].toString()
    + " " + Thread.currentThread().getStackTrace()[3].toString());
```

每次渲染 pass 都执行 `getStackTrace()`（昂贵操作），在繁忙的渲染循环中（每分钟数百次调用）会产生显著 overhead。

---

### BUG-08：`drawIndexed` / `pushDesc` 每次调用无条件 `System.err.println`

**文件：** [Dx12RenderPassBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12RenderPassBackend.java) L302-308  
**文件：** [Dx12RenderPassBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12RenderPassBackend.java) L164-174

```java
System.err.println("[dx12-java] drawIndexed pipeline=" + ...);
System.err.flush();  // 强制同步刷盘
```

每帧调用多次 `flush()` 会导致 I/O 瓶颈，严重影响渲染性能。

---

## 三、轻微问题（代码质量 / 潜在风险）

### BUG-09：`Dx12IntermediaryShaderModule.toHlsl()` 双重打印同一内容

**文件：** [Dx12IntermediaryShaderModule.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12IntermediaryShaderModule.java) L309-318

```java
// P7 诊断：打印 raw spvc 输出
if (name.contains("gui") || ...) {
    System.err.println("[dx12-java] [" + name + "] RAW spvc HLSL:\n" + hlsl);
}
// P7 诊断：打印 raw spvc 输出，确认语义已正确生成
if (name.contains("gui") || ...) {  // 完全相同的条件！
    System.err.println("[dx12-java] [" + name + "] spvc HLSL (no inject):\n" + result);
}
```

两个条件完全一致，`hlsl` 和 `result` 指向同一个字符串（`result = hlsl`，L314）。会打印**两份完全相同的 HLSL**，造成日志冗余。

---

### BUG-10：静态计数器在多线程环境下未同步

**文件：** [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) 多处

```cpp
static int pdFrameCount = 0;          // L2417
static int vbDbg = 0;                 // L2383
static UINT64 lastFence = 0;          // L2507
static int passCount = 0;             // L2508
```

虽然目前渲染是单线程的，但这些静态变量在函数内，不同 command list 并发使用时会共享状态。若未来引入多 command list 并行提交，这些计数器会竞争。

---

### BUG-11：`dx12BeginCommandList` 未检查 `cmdSigIndexed`/`cmdSigNonIndexed` 是否已初始化

**文件：** [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) L695-706

```cpp
bool beginCommandList(CommandContext* cmd, std::string& err) {
    if (!cmd) { err = "null cmd"; return false; }
    if (cmd->listOpen) { err = "already open"; return false; }
    if (!gCtx.device) { err = "no device"; return false; }
    // 未检查 cmdSigIndexed / cmdSigNonIndexed 是否存在
```

若 `initDevice()` 在创建 command signature 阶段失败，后续调用 `beginCommandList` 不会报错，但 `drawIndexedIndirect` 会在执行时失败。建议在 `initDevice` 失败时设置全局标志位，`beginCommandList` 时检查。

---

### BUG-12：`configureSurface` 中高度倍增检查可能误拦截合法窗口拉伸

**文件：** [dx12_surface.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_surface.cpp) L135-141

```cpp
if (s->width > 0 && s->height > 0
    && width == s->width && height > static_cast<int>(s->height) * 2) {
```

此逻辑在宽度不变、高度超过 2 倍时阻止 resize。合理防止意外拉伸，但若用户手动将窗口从 480p 拉到 960p 高度（如 Retina 屏适配），会被误拦截（960 = 480*2，不满足 `> 2*height`，刚好通过；但若到 961 就拦截）。边界边界情况。

---

### BUG-13：`createFence` 使用全局队列 fence 而非 per-ctx fence，导致语义偏差

**文件：** [Dx12CommandEncoderBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12CommandEncoderBackend.java) L301-330  
**文件：** [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) L1298-1300, L1264-1296

```java
// Dx12CommandEncoderBackend.java:308-309
long fenceValue = Dx12Native.dx12GetFenceValue(this.ctx);  // 返回的是全局 gCtx.queueFenceValue
long target = fenceValue + 1;
```

官方 Vulkan 语义：`createFence()` 在**共享 encoder** 上创建，等待该 encoder 下一次 `submit()`。  
当前实现：用全局 `queueFenceValue + 1` 作为目标，任何 ctx 的下一次 submit 都会完成它。

**问题**：若 `createCommandEncoder()` 被调用两次（产生两个不同的共享 encoder），encoder-A 创建的 fence token 可能由 encoder-B 的 submit 提前完成，导致 Java 侧的 fence 认为已完成而 GPU 实际数据尚未就绪。虽然目前 Minecraft 单 encoder 路径下不会触发，但架构上存在隐患。

**此外**：`submit()` 方法在调用 `transientMemory.rotate()` **之前**先执行 `dx12Submit()`，但 `dx12Submit` 不等待 GPU 完成。若 `createFence().awaitCompletion(0)` 以超时=0 调用（如 `StagedVertexBuffer` 检查 fence 状态），`waitForQueueFenceValue` 会**立即返回 false**，然后 Java 侧的 fence 对象会被丢弃。这与官方 Vulkan 语义一致，但需要保证所有依赖 fence 的地方都能正确处理「未完成」的状态。

---

### BUG-14：静态 command list/allocator 在 readback 路径上不释放（资源泄漏）

**文件：** [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) L2620-2650, L2837-2867  
**文件：** [dx12_surface.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_surface.cpp) L566-601  
**文件：** [jni_bridge_p5.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/jni_bridge_p5.cpp) L156-182, L303-334

以下函数使用函数静态 `ComPtr<ID3D12CommandAllocator>` 和 `ComPtr<ID3D12GraphicsCommandList>`：
- `dx12ReadbackTexture` (L2620)
- `dx12ReadbackBuffer` (L2837)
- `dx12ReadbackSurfacePixels` (dx12_surface.cpp L566, L590)
- `readBufferGpuToCpu` / `copyTextureToBuffer` (jni_bridge_p5.cpp)

这些静态对象**进程存活期间不释放**。每个函数持有 1 个 allocator + 1 个 command list = 2 个 COM 对象。若所有路径都使用静态缓存，总共泄漏约 6-8 个 allocator 和 command list 对象。虽然单个对象大小不大（~几十KB），但长期运行会累积。更重要的是，这些静态对象被不同函数**交替使用**，若出现并发 readback（理论上不可能，但若未来引入多线程），会状态污染。

**建议**：将其改为 CommandContext 的成员（复用现有 3-allocator 机制），或在设备销毁时显式释放。

---

### BUG-15：`ensureDevice` 无幂等保护，重复调用会重新创建所有 D3D12 资源

**文件：** [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) L299-437

```cpp
bool ensureDevice(std::string& errorOut) {
    if (gCtx.device && gCtx.srvHeap && gCtx.srvCpuHeap && gCtx.rtvHeap &&
        gCtx.dsvHeap && gCtx.samplerHeap && gCtx.drawHeap) return true;
    // ... 创建新 device, queue, fences, heaps ...
```

第一个 `if` 检查了所有指针是否非空。但**没有任何初始化标记**来区分"已初始化"和"指针恰好非空"。如果在某个中间步骤（如 device 已创建但 sampler heap 创建失败）中途调用 `ensureDevice`，第二次调用会**跳过**（因为 `gCtx.device` 非空但 `gCtx.samplerHeap` 为 null），重新从头开始——这会**泄漏**第一次创建的 device、queue、fences 和 heaps。

**修复**：添加 `bool gInitialized` 标志位，或确保 `ensureDevice` 在失败后能完全清理状态再重试。

---

### BUG-16：`createBuffer(data)` 临时 encoder 与共享 encoder 的 fence 竞争

**文件：** [Dx12Device.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12Device.java) L140-148  
**文件：** [Dx12CommandEncoderBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12CommandEncoderBackend.java) L341-358

```java
// Dx12Device.java:141-147
Dx12CommandEncoderBackend encoder = new Dx12CommandEncoderBackend();  // device=null, 新 ctx
try {
    encoder.writeToBuffer(buffer.slice(), data);
    encoder.submit();  // submit() 递增全局 queueFenceValue，然后 rotate transientMemory
} finally {
    encoder.close();   // device==null → 调用 dx12DestroyCommandEncoder → 等 fence 完成
}
```

`submit()` 内部调用 `dx12BeginCommandList`（在 native 侧）。临时 encoder 每次 submit 后立刻 close，close 时会等待其 fence 完成。但此过程中**全局 `queueFenceValue` 被递增**，可能影响同一帧中其他正在进行的 `createFence().awaitCompletion()` 调用。

例如时序：
1. 主渲染帧：`encoder.submit()` → `queueFenceValue` = 100
2. 此时 Minecraft 的 `StagedVertexBuffer` 调用 `fence.awaitCompletion(0)` 检查目标值=100 → 因步骤1已提交但 GPU 未执行完，返回 false（正确）
3. 但若在某次极端情况下，临时 encoder 的 submit 把 `queueFenceValue` 提前推进到目标值，fence 会**提前完成**，导致 `StagedVertexBuffer` 过早回收 buffer，造成 use-after-free。

---

### BUG-17：`d3d12` 包是未使用的死代码，与 `dx12` 包形成双重实现

**文件：** [com/dx12/d3d12/Dx12Device.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/d3d12/Dx12Device.java)  
**文件：** [com/dx12/d3d12/Dx12DeviceContext.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/d3d12/Dx12DeviceContext.java)

`d3d12` 包下的类（`Dx12Device`, `Dx12DeviceContext`, `Dx12Exception`, `Dx12AdapterInfo`）**未被任何生产代码引用**，仅是实验性 wrapper。`Dx12Device.close()` 是空操作（仅打印 "D3D12 device released (Java side)"，不调用任何 native 清理），若有人误用会导致设备泄漏。同时这类死代码会让维护者困惑哪个包是"真正的"实现。

---

## 四、日志与诊断残留

| 位置 | 问题 |
|------|------|
| [Dx12ShaderCompiler.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12ShaderCompiler.java) L110-122 | `fragmentHlsl` 全管线强制注入纯绿（`frag forced GREEN`），调试期产物遗留 |
| [Dx12IntermediaryShaderModule.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12IntermediaryShaderModule.java) L309-318 | 双重打印相同 HLSL |
| [Dx12CommandEncoderBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12CommandEncoderBackend.java) L151-156 | 每次 `createRenderPass` 打栈轨迹 |
| [Dx12RenderPassBackend.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12RenderPassBackend.java) L302-308 | 每次 `drawIndexed` 打日志+flush |
| [Dx12GpuSurface.java](file:///d:/dx12-lib-template-26.1.2/fabric/src/main/java/com/dx12/dx12/Dx12GpuSurface.java) L123 | 每 30 帧同步读回 GPU 像素（主线程阻塞） |
| [dx12_device.cpp](file:///d:/dx12-lib-template-26.1.2/native/src/dx12_device.cpp) 多处 | `static int` 诊断计数器，应改为可通过环境变量关闭 |

---

## 五、优先级总结

| 等级 | 编号 | 描述 | 影响 |
|------|------|------|------|
| 🔴 严重 | BUG-01 | 语义名索引错位 | 特定管线 `E_INVALIDARG` |
| 🟠 中等 | BUG-02 | rebind 逻辑潜在缺陷 | 当前未触发，有隐患 |
| 🟠 中等 | BUG-03 | selfTest 消息误导 | 调试方向错误 |
| 🟡 轻微 | BUG-04 | Config 单例非线程安全 | 当前安全，未来有风险 |
| 🟡 轻微 | BUG-05 | 每 30 帧 GPU 同步读回 | 主线程卡顿 |
| 🟡 轻微 | BUG-06 | CBV 256 字节对齐浪费 | 性能/内存开销 |
| 🟡 轻微 | BUG-09~12 | 调试日志残留 / 静态变量 | 性能 / 可维护性 |
| 🟠 中等 | BUG-13 | createFence 使用全局队列 fence | 架构隐患 |
| 🟡 轻微 | BUG-14 | 静态 command list/allocator 不释放 | 长期运行泄漏 |
| 🟡 轻微 | BUG-15 | ensureDevice 无幂等保护 | 中途失败后资源泄漏 |
| 🟠 中等 | BUG-16 | 临时 encoder submit 与全局 fence 竞争 | 潜在 use-after-free |
| 🔵 低 | BUG-17 | d3d12 包是未使用的死代码 | 维护混乱 |
