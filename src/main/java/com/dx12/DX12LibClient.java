package com.dx12;

import java.io.*;
import java.nio.file.*;

public class DX12LibClient {

    static {
        loadLibrary();
    }

    private static void loadLibrary() {
        // 方法1: 从 JAR 中提取
        try (InputStream in = DX12LibClient.class.getResourceAsStream("/native/windows/gl4dx12.dll")) {
            if (in != null) {
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "gl4dx12");
                tempDir.mkdirs();
                File tempFile = new File(tempDir, "gl4dx12.dll");
                tempFile.deleteOnExit();
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.load(tempFile.getAbsolutePath());
                System.out.println("[GL4DX12] Loaded from JAR: " + tempFile.getAbsolutePath());
                return;
            }
        } catch (Throwable e) {
            System.err.println("[GL4DX12] Extract failed: " + e.getMessage());
        }

        // 方法2: 直接从项目路径加载（备用）
        try {
            System.load("D:\\dx12-lib-template-26.1.2\\src\\main\\resources\\native\\windows\\gl4dx12.dll");
            System.out.println("[GL4DX12] Loaded from project path");
            return;
        } catch (Throwable e) {
            // ignore
        }

        // 方法3: 从 natives 目录加载
        try {
            System.load("D:\\.minecraft\\versions\\xiaozi craft 26.1.2\\xiaozi craft 26.1.2-natives\\gl4dx12.dll");
            System.out.println("[GL4DX12] Loaded from natives path");
            return;
        } catch (Throwable e) {
            // ignore
        }

        System.err.println("[GL4DX12] Failed to load DLL!");
    }

    public static boolean isLibraryLoaded() {
        return true;
    }

    // Native methods
    public static native boolean nativeInit(long hwnd);
    public static native void nativeCleanup();
    public static native void nativeResize(int width, int height);
    public static native void nativeBeginFrame();
    public static native void nativeEndFrame();
    public static native void nativePresent();
    public static native void nativeSetMvp(float[] matrix);
    public static native void nativeRecordVertices(float[] vertices, int vertexCount);
    public static native void nativeRecordColors(float[] colors);
    public static native void nativeRecordUV(float[] uv);
    public static native void nativeDraw(int vertexCount);
    public static native String nativeGetD3D12Info();
    public static native boolean nativeIsD3D12Active();

    // Phase 3 stubs
    public static native void nativeSetSkyParameters(float[] params);
    public static native void nativeRenderSkybox();
    public static native void nativeUploadParticles(float[] particles, int count);
    public static native void nativeRenderParticles();
}