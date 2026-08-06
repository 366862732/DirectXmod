package com.dx12.dx12;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * JNI bridge for the dx12-mc Rust library.
 *
 * The native symbol exported by dx12-mc is
 * {@code Java_com_dx12_dx12_Dx12Native_dx12CreateDevice}, so the class MUST be
 * {@code com.dx12.dx12.Dx12Native} with a {@code dx12CreateDevice()} method.
 *
 * The DLL is extracted from the mod JAR into {@code <user.dir>/dx12mod/}
 * (same pattern as D3D12Bridge) so the launcher's working directory always
 * holds a copy that matches the JAR.
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
     * Probe D3D12 device creation on the native side.
     *
     * @return adapter name + feature level (e.g. "NVIDIA GeForce RTX 4090
     *         (D3D_FEATURE_LEVEL 12_1)"), or a string starting with "ERROR: "
     *         if device creation failed.
     */
    public static native String dx12CreateDevice();

    private Dx12Native() {
    }
}
