package com.dx12.dx12;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * JNI bridge for the dx12-mc C++ native library.
 *
 * The native symbols exported by dx12_mc.dll are named
 * {@code Java_com_dx12_dx12_Dx12Native_<method>}, so this class MUST be
 * {@code com.dx12.dx12.Dx12Native} and method names MUST match exactly.
 *
 * The DLL is extracted from the mod JAR into {@code <user.dir>/dx12mod/}
 * so the launcher's working directory always holds a copy that matches the JAR.
 */
public final class Dx12Native {
    private static boolean loaded = false;

    static {
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        if (loaded) return;
        try {
            String libName = "dx12_mc.dll";
            Path dllDir = Path.of(System.getProperty("user.dir"), "dx12mod");
            Files.createDirectories(dllDir);
            Path dllPath = dllDir.resolve(libName);

            // Always extract from JAR to guarantee the deployed DLL matches this build.
            try (InputStream in = Dx12Native.class.getResourceAsStream("/" + libName)) {
                if (in != null) {
                    Files.copy(in, dllPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    System.err.println("[dx12] " + libName + " not found in JAR resources");
                    return;
                }
            }

            System.load(dllPath.toAbsolutePath().toString());
            loaded = true;
            String verbose = System.getenv("DX12_LOG_VERBOSE");
            System.out.println("[dx12] Native library loaded from: " + dllPath);
            System.out.println("[dx12] 高频调试日志已关闭，如需开启请设置环境变量 DX12_LOG_VERBOSE=1");
        } catch (Exception e) {
            System.err.println("[dx12] Failed to load native library: " + e.getMessage());
        }
    }

    /**
     * Probe D3D12 device creation + run the native resource self-test.
     *
     * @return adapter name + feature level + self-test result
     *         (e.g. "NVIDIA GeForce RTX 4090 (D3D_FEATURE_LEVEL 12_1); SELF-TEST OK (...)")
     */
    public static native String dx12CreateDevice();

    /** Create a texture; returns a native handle (long = Dx12Object*). */
    public static native long dx12CreateTexture(int usage, int format, int width,
        int height, int depthOrLayers, int mipLevels);

    /** Create a buffer; returns a native handle (long = Dx12Object*). */
    public static native long dx12CreateBuffer(int usage, long size);

    /** Create a sampler; returns a native handle (long = Dx12Object*). */
    public static native long dx12CreateSampler(int addressU, int addressV, int minFilter,
        int magFilter, int maxAnisotropy, float maxLod);

    /** Create a texture view; returns a native handle (long = Dx12Object*). */
    public static native long dx12CreateTextureView(long textureHandle, int baseMipLevel, int mipLevels);

    /** Destroy a native resource created by one of the dx12Create* methods. */
    public static native void dx12DestroyResource(long handle);

    /**
     * Map a buffer region; returns a direct ByteBuffer over the mapped memory.
     * Must be paired with {@link #dx12UnmapBuffer}.
     */
    public static native ByteBuffer dx12MapBuffer(long bufferHandle, long offset, long length,
        boolean read, boolean write);

    /** Unmap a previously mapped buffer. */
    public static native void dx12UnmapBuffer(long bufferHandle);

    // -----------------------------------------------------------------------
    // P3: command layer
    // -----------------------------------------------------------------------

    /** Query the GPU timestamp frequency (ticks per second; 0 if unknown). */
    public static native long dx12GetTimestampFrequency();

    /** Create a command context (CommandContext*); returns the native handle. */
    public static native long dx12CreateCommandEncoder();

    /** Destroy a command context created by {@link #dx12CreateCommandEncoder}. */
    public static native void dx12DestroyCommandEncoder(long ctx);

    /** Begin recording on the current command list (resets its allocator). */
    public static native void dx12BeginCommandList(long ctx);

    /** Close the current command list. */
    public static native void dx12EndCommandList(long ctx);

    /**
     * Submit the recorded command list: ExecuteCommandLists + Signal(fence,
     * ++value). Non-blocking (GPU async); the fence value is recorded per-backbuffer
     * so acquireSurface can wait only when reusing a buffer still in use by GPU.
     * Returns the fence value of this submit.
     */
    public static native long dx12Submit(long ctx);

    /** Wait until the fence reaches {@code value}; returns true on completion. */
    public static native boolean dx12WaitForFence(long ctx, long value, long timeoutNs);

    /** Current fence value of the context (for createFence). */
    public static native long dx12GetFenceValue(long ctx);

    /** Read the GPU timestamp now (blocking). */
    public static native long dx12GetTimestampNow(long ctx);

    /** Record a buffer->buffer copy (CopyBufferRegion) into the command list. */
    public static native void dx12CopyBuffer(long ctx, long src, long srcOffset,
        long dst, long dstOffset, long size);

    /**
     * Upload {@code width*height} tightly-packed texels from a staging buffer
     * into a texture sub-region (writeToTexture).
     */
    public static native void dx12WriteToTexture(long ctx, long stagingBuf,
        long stagingOffset, int width, int height, long dstTex, int mip, int layer,
        int dstX, int dstY);

    /** Copy a sub-region of a (possibly padded) source buffer into a texture. */
    public static native void dx12CopyBufferToTexture(long ctx, long srcBuf,
        long srcOffset, int srcWidth, int srcHeight, long dstTex, int mip, int layer,
        int dstX, int dstY, int w, int h);

    /** Copy a texture sub-region into a readback buffer (row pitch is 256-aligned). */
    public static native void dx12CopyTextureToBuffer(long ctx, long srcTex,
        int mip, int layer, int srcX, int srcY, int w, int h, long dstBuf,
        long dstOffset);

    /** Copy a texture sub-region into another texture. */
    public static native void dx12CopyTextureToTexture(long ctx, long srcTex,
        long dstTex, int mip, int layer, int srcX, int srcY, int dstX, int dstY,
        int w, int h);

    /** Record a full-texture color clear (RENDER_ATTACHMENT texture). */
    public static native void dx12ClearColorTexture(long ctx, long texture,
        float r, float g, float b, float a);

    /** Record a full-texture depth clear. */
    public static native void dx12ClearDepthTexture(long ctx, long texture, double depth);

    /**
     * Begin a render pass: create RTV/DSV descriptors, transition attachments,
     * OMSetRenderTargets, viewport/scissor, optional clears.
     *
     * @param colorTextures   color attachment textures (nulls are skipped)
     * @param colorClearFlags per-attachment 0=load / 1=clear
     * @param clearColors     r,g,b,a per attachment (length colorTextures.length * 4)
     */
    public static native void dx12BeginRenderPass(long ctx, long[] colorTextures,
        byte[] colorClearFlags, float[] clearColors, long depthTexture,
        byte depthClearFlag, double depthClearValue, int x, int y, int w, int h);

    /** End the current render pass. */
    public static native void dx12EndRenderPass(long ctx);

    /** Create a timestamp query pool; returns the native handle. */
    public static native long dx12CreateQueryPool(int size);

    /** Destroy a query pool. */
    public static native void dx12DestroyQueryPool(long pool);

    /** Record EndQuery(TIMESTAMP) at {@code index} into the command list. */
    public static native void dx12WriteTimestamp(long ctx, long pool, int index);

    /** Blocking read of one timestamp value. */
    public static native long dx12ReadQueryValue(long pool, int index);

    /** Blocking read of {@code count} timestamps starting at {@code start}. */
    public static native void dx12ReadQueryValues(long pool, int start, int count, long[] out);

    // -----------------------------------------------------------------------
    // P4: graphics pipeline
    // -----------------------------------------------------------------------

    /**
     * Compile an HLSL pair (vs_5_1/ps_5_1) and create a Dx12Pipeline*
     * (root signature + PSOs) from a packed little-endian descriptor buffer.
     *
     * <p>Layout (little-endian):
     * <pre>
     * int vsLen; byte[vsLen]; int psLen; byte[psLen];
     * int colorCount;
     *   per color: int format; byte writeMask; byte blendEnabled;
     *              byte srcColor; byte dstColor; byte colorOp;
     *              byte srcAlpha; byte dstAlpha; byte alphaOp;
     *              (format = -1 => unused slot, DXGI_FORMAT_UNKNOWN)
     * byte hasDepth; if(hasDepth): int depthFormat; byte depthWrite; byte depthCompareOp;
     * int topology; byte cullEnabled; int polygonMode;
     * int inputElementCount;
     *   per element: int location; int binding; int format; int offset;
     *                int stride; int stepRate;
     * int entryCount;
     *   per entry: byte type (0=CBV, 1=SRV+static sampler, 2=SRV); byte register;
     * </pre>
     *
     * <p>枚举序数为官方 MC 枚举 ordinal：topology = {@code PrimitiveTopology.ordinal()}
     * （LINES=0, DEBUG_LINES=1, DEBUG_LINE_STRIP=2, POINTS=3, TRIANGLES=4,
     * TRIANGLE_STRIP=5, TRIANGLE_FAN=6, QUADS=7）；blend factor/op 与
     * depth compare op 均为 {@code BlendFactor}/{@code BlendOp}/{@code CompareOp}
     * 的 ordinal；polygonMode = {@code PolygonMode.ordinal()}（FILL=0, WIREFRAME=1）。
     * format = {@code GpuFormat.ordinal()}。
     *
     * @param desc direct ByteBuffer (LITTLE_ENDIAN), flipped before passing
     * @return native Dx12Pipeline* handle; 0 on failure (error logged natively)
     */
    public static native long dx12CreateGraphicsPipeline(ByteBuffer desc);

    /** Destroy a pipeline created by {@link #dx12CreateGraphicsPipeline}. */
    public static native void dx12DestroyPipeline(long pipeline);

    // -----------------------------------------------------------------------
    // P5: surface (DXGI swapchain)
    // -----------------------------------------------------------------------

    /**
     * Create a DXGI swapchain for the given native window handle (HWND).
     *
     * @param hwnd native HWND (e.g. from {@code GLFWNativeWin32.glfwGetWin32Window})
     * @return native Dx12Surface* handle; 0 on failure
     */
    public static native long dx12CreateSurface(long hwnd);

    /**
     * Present modes this implementation supports, as {@code GpuSurface.PresentMode}
     * ordinals (IMMEDIATE=0, MAILBOX=1, FIFO=2, FIFO_RELAXED=3). DXGI FLIP
     * supports {IMMEDIATE, FIFO, FIFO_RELAXED}.
     */
    public static native int[] dx12SurfacePresentModes();

    /**
     * ResizeBuffers to {@code width x height} with the given present mode.
     *
     * @return true on success
     */
    public static native boolean dx12ConfigureSurface(long surface, int width,
        int height, int presentMode);

    /** Acquire the next back buffer. @return true on success */
    public static native boolean dx12AcquireSurface(long surface);

    /**
     * Record a copy of {@code texture} into the current back buffer on the
     * given command context's open command list. Source and back buffer must
     * have compatible (same-family) formats.
     */
    public static native void dx12BlitSurface(long ctx, long surface, long texture);

    /** Present the current back buffer (vsync follows the configured mode). */
    public static native void dx12PresentSurface(long surface);

    /** 返回当前 acquire 的 back buffer 原始资源指针，用于 blit/readback（不依赖缓存）。 */
    public static native long dx12GetBackBufferHandle(long surface);

    /**
     * 返回当前 command context 渲染 pass 中第一个活跃颜色附件的纹理句柄。
     * 若在 pass 外或无颜色附件则返回 0。用于替代缓存的 lastColorTextureHandle。
     */
    public static native long dx12GetActiveColorTexture(long ctx);

    /** Whether the last present reported a suboptimal status. */
    public static native boolean dx12IsSurfaceSuboptimal(long surface);

    /** P6 诊断：读回当前 back buffer 3x3 采样像素，返回 int[36]（9像素×RGBA）。为 null 表示失败。 */
    public static native int[] dx12ReadbackSurfacePixels(long surface);

    /** P6 诊断：读回任意纹理 3x3 采样像素，返回 int[36]（9像素×RGBA）。为 null 表示失败。 */
    public static native int[] dx12ReadbackTexturePixels(long texHandle);

    /** Destroy a surface created by {@link #dx12CreateSurface}. */
    public static native void dx12DestroySurface(long surface);

    /** P6 诊断：返回当前 active surface 的 native handle（用于 readback 游戏实际渲染画面）。 */
    public static native long dx12GetActiveSurfaceHandle();

    /** 枚举所有 D3D12 支持的适配器，返回 JSON 数组字符串。 */
    public static native String dx12EnumerateAdapters();

    /** 返回当前已创建设备的全局命令队列 COM 指针（long）。0 表示未初始化。 */
    public static native long dx12GetQueueHandle();

    /** 返回当前已创建设备的全局设备 COM 指针（long）。0 表示未初始化。 */
    public static native long dx12GetDeviceHandle();

    /** 创建一个隐藏的 Win32 窗口，返回 HWND（用于测试渲染循环）。0 表示失败。 */
    public static native long dx12CreateHiddenWindow(int width, int height);

    /** 销毁隐藏窗口。 */
    public static native void dx12DestroyHiddenWindow(long hwnd);

    // -----------------------------------------------------------------------
    // P6: render pass 内 draw 命令录制
    // -----------------------------------------------------------------------

    /**
     * Bind a graphics pipeline (PSO). {@code hasDepth} selects the with-depth
     * or without-depth PSO variant (mirror of the Vulkan withDepth/withoutDepth
     * pipeline pair).
     *
     * @return true on success
     */
    public static native boolean dx12SetPipeline(long ctx, long pipeline, boolean hasDepth);

    /** Set the scissor rect (x, y, width, height) on the open render pass. */
    public static native boolean dx12SetScissor(long ctx, int x, int y, int width, int height);

    /**
     * Bind a vertex buffer to {@code slot} (0..15). {@code stride} is the
     * per-vertex byte size from the pipeline's vertex format binding.
     */
    public static native boolean dx12SetVertexBuffer(long ctx, int slot, long buffer,
        long offset, int stride);

    /** Bind an index buffer; {@code indexType} 0=SHORT(R16_UINT), 1=INT(R32_UINT). */
    public static native boolean dx12SetIndexBuffer(long ctx, long buffer, int indexType);

    /**
     * Push the draw's descriptors into the transient per-frame heap and bind
     * root descriptor table 0 (mirror of the Vulkan push descriptors).
     * Parallel arrays, one entry per pipeline binding in declaration order:
     * types 0=CBV / 1=SRV(texture view) / 2=SRV(texel buffer); for CBV the
     * matching buffer/offset/length; for texel buffers buffer/texelFormat;
     * for texture views the Dx12GpuTextureView handle.
     *
     * @return true on success
     */
    public static native boolean dx12PushDescriptors(long ctx, int[] types,
        long[] buffers, long[] offsets, long[] lengths, int[] texelFormats, long[] views);

    /** DrawIndexedInstanced on the open render pass. */
    public static native boolean dx12DrawIndexed(long ctx, int indexCount,
        int instanceCount, int firstIndex, int baseVertex, int firstInstance);

    /** DrawInstanced on the open render pass. */
    public static native boolean dx12Draw(long ctx, int vertexCount, int instanceCount,
        int firstVertex, int firstInstance);

    /** ExecuteIndirect (DrawIndexedInstanced) with the given command buffer. */
    public static native boolean dx12DrawIndexedIndirect(long ctx, long commands,
        long offset, int drawCount);

    /** ExecuteIndirect (DrawInstanced) with the given command buffer. */
    public static native boolean dx12DrawIndirect(long ctx, long commands,
        long offset, int drawCount);

    private Dx12Native() {
    }
}
