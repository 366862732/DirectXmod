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

    private Dx12Native() {
    }
}
