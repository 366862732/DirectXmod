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
            System.out.println("[dx12] Native library loaded from: " + dllPath);
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
     * ++value), then wait for value-2 (mirror of the vanilla double-buffered
     * submit). Returns the fence value of this submit.
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

    private Dx12Native() {
    }
}
