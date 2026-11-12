package com.xgdt.dx11.dx11;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * JNI bridge for {@code dx11_mc.dll}.
 *
 * <p>The DLL is intentionally separate from the existing {@code dx12_mc.dll}:
 * the D3D12 DLL imports {@code d3d12.dll} and therefore cannot be loaded on a
 * DirectX 10.1-only machine. This library imports only {@code d3d11.dll} and
 * {@code dxgi.dll}; {@code d3dcompiler_47.dll} is loaded dynamically at
 * runtime so a missing compiler DLL produces a clean probe error instead of a
 * DLL load failure.</p>
 *
 * <p>For development, {@code -Dgl4dx11.nativePath=C:\path\to\dx11_mc.dll}
 * bypasses JAR extraction and loads the given DLL directly.</p>
 */
public final class Dx11Native {
    /** Optional absolute path to a locally built dx11_mc.dll (development). */
    public static final String NATIVE_PATH_PROPERTY = "gl4dx11.nativePath";

    private static boolean loaded;

    static {
        loadNativeLibrary();
    }

    private Dx11Native() {
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void loadNativeLibrary() {
        String override = System.getProperty(NATIVE_PATH_PROPERTY, "").trim();
        if (!override.isEmpty()) {
            Path overridePath = Path.of(override).toAbsolutePath();
            if (Files.isRegularFile(overridePath)) {
                try {
                    System.load(overridePath.toString());
                    loaded = true;
                    System.out.println("[dx11] Native library loaded from override: " + overridePath);
                    return;
                } catch (Throwable t) {
                    System.err.println("[dx11] Failed to load override " + overridePath + ": " + t);
                }
            } else {
                System.err.println("[dx11] Native override is not a file: " + overridePath);
            }
        }

        try {
            String libName = "dx11_mc.dll";
            Path dllDir = Path.of(System.getProperty("user.dir"), "dx11mod");
            Files.createDirectories(dllDir);
            Path dllPath = dllDir.resolve(libName);

            try (InputStream in = Dx11Native.class.getResourceAsStream("/" + libName)) {
                if (in == null) {
                    System.err.println("[dx11] " + libName + " not found in JAR resources");
                    return;
                }
                byte[] jarBytes = in.readAllBytes();
                boolean upToDate = Files.exists(dllPath)
                    && Arrays.equals(jarBytes, Files.readAllBytes(dllPath));
                if (!upToDate) {
                    Files.write(dllPath, jarBytes);
                }
            }

            System.load(dllPath.toAbsolutePath().toString());
            loaded = true;
            System.out.println("[dx11] Native library loaded from: " + dllPath);
        } catch (Throwable t) {
            System.err.println("[dx11] Failed to load native library: " + t);
        }
    }

    /**
     * Creates a temporary D3D11 device, queries the adapter, and compiles
     * trivial SM4.0/SM4.1 shaders. No window is required.
     *
     * @return a string beginning with {@code OK} on success, otherwise
     *         {@code ERROR: ...}
     */
    public static native String dx11Probe();

    // -----------------------------------------------------------------------
    // Device / swapchain
    // -----------------------------------------------------------------------
    public static native long nativeCreateDevice(long hwnd);

    public static native void nativeDestroyDevice(long device);

    /** Returns {adapter, vendor, version, featureLevel, vendorId, deviceId, maxTextureSize}. */
    public static native String[] nativeGetDeviceInfo(long device);

    public static native boolean nativePresent(long device, int width, int height);

    public static native boolean nativeBlitToBackBuffer(long device, long texture);

    public static native String nativeGetPresentInfo(long device);

    public static native boolean nativeDumpTexture(long device, long texture, String path);

    public static native boolean nativeDumpBackBuffer(long device, String path);

    public static native void nativeSetVsync(long device, boolean vsync);

    // -----------------------------------------------------------------------
    // Textures
    // -----------------------------------------------------------------------
    public static native long nativeCreateTexture(
        long device, int usage, int format, int width, int height, int depthOrLayers, int mipLevels);

    public static native long nativeCreateTextureView(long device, long texture, int baseMip, int mipLevels);

    public static native void nativeDestroyTexture(long texture);

    public static native void nativeDestroyTextureView(long view);

    public static native boolean nativeWriteToTexture(
        long texture, int mip, int layer, int x, int y, int width, int height, long address, int rowStride);

    public static native boolean nativeCopyTextureToBuffer(
        long texture, long buffer, long bufferOffset, int mip, int x, int y, int width, int height);

    public static native boolean nativeCopyTextureToTexture(
        long source, long destination, int mip, int srcX, int srcY, int dstX, int dstY, int width, int height);

    public static native boolean nativeClearColorTexture(long device, long texture, int argb);

    public static native boolean nativeClearDepthTexture(long device, long texture, double depth);

    public static native boolean nativeClearColorAndDepthTexture(
        long device, long color, int argb, long depth, double depthValue);

    public static native boolean nativeClearColorAndDepthTextureRegion(
        long device, long color, int argb, long depth, double depthValue,
        int x, int y, int width, int height);

    // -----------------------------------------------------------------------
    // Buffers
    // -----------------------------------------------------------------------
    public static native long nativeCreateBuffer(
        long device, int usage, int size, long initialAddress, int initialSize);

    public static native void nativeDestroyBuffer(long buffer);

    public static native boolean nativeWriteToBuffer(long buffer, long offset, long address, long size);

    public static native ByteBuffer nativeMapBuffer(
        long buffer, long offset, long size, boolean read, boolean write);

    public static native void nativeUnmapBuffer(long buffer);

    public static native boolean nativeCopyBufferToBuffer(
        long source, long sourceOffset, long destination, long destinationOffset, long size);

    // -----------------------------------------------------------------------
    // Samplers
    // -----------------------------------------------------------------------
    public static native long nativeCreateSampler(
        long device, int addressU, int addressV, int minFilter, int magFilter,
        int maxAnisotropy, boolean hasMaxLod, double maxLod);

    public static native void nativeDestroySampler(long sampler);

    // -----------------------------------------------------------------------
    // Pipelines
    // -----------------------------------------------------------------------
    public static native long nativeCreatePipeline(
        long device, String name, String vertexHlsl, String fragmentHlsl,
        int stride, int topology, String[] inputSemantics, int[] inputSemanticIndices,
        int[] inputFormats, int[] inputOffsets, int[] uniformSizes,
        boolean blend, int srcColor, int dstColor, int srcAlpha, int dstAlpha,
        boolean depthTest, boolean depthWrite, int depthFunc, boolean cull,
        int fillMode, int depthBias, float slopeScaledDepthBias, int colorWriteMask);

    public static native void nativeDestroyPipeline(long pipeline);

    // -----------------------------------------------------------------------
    // Render passes
    // -----------------------------------------------------------------------
    public static native long nativeCreateRenderPass(
        long device, long colorView, long depthView, boolean clearColor,
        int clearColorValue, boolean clearDepth, double clearDepthValue);

    public static native void nativeDestroyRenderPass(long pass);

    public static native void nativePassSetPipeline(long pass, long pipeline);

    public static native void nativePassSetUniform(long pass, int slot, long buffer, long offset, long size);

    public static native void nativePassSetTexelBuffer(long pass, int slot, long buffer, int format);

    public static native void nativePassBindTexture(long pass, int slot, long view, long sampler);

    public static native void nativePassSetVertexBuffer(long pass, long buffer);

    public static native void nativePassSetIndexBuffer(long pass, long buffer, boolean index32);

    public static native void nativePassSetScissor(
        long pass, boolean enabled, int x, int y, int width, int height);

    public static native void nativePassDrawIndexed(
        long pass, int firstIndex, int indexCount, int baseVertex, int instanceCount);

    public static native void nativePassDraw(long pass, int firstVertex, int vertexCount, int instanceCount);

    // -----------------------------------------------------------------------
    // Fences / queries
    // -----------------------------------------------------------------------
    public static native long nativeCreateFence(long device);

    public static native boolean nativeFenceAwait(long fence, long timeoutNanos);

    public static native void nativeDestroyFence(long fence);

    public static native long nativeCreateTimerQuery(long device);

    public static native void nativeTimerQueryEnd(long query);

    /** Returns elapsed nanoseconds, or {@link Long#MIN_VALUE} while the result is not ready. */
    public static native long nativeQueryGetValue(long query);

    public static native void nativeDestroyQuery(long query);
}
