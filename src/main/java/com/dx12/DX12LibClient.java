package com.dx12;

import java.io.File;

import net.fabricmc.loader.api.FabricLoader;

public class DX12LibClient {

    static {
        loadLibrary();
    }

    private static void loadLibrary() {
        // 从游戏目录下的 versions/<版本>/dx12mod/ 加载 DLL
        File gameDir = FabricLoader.getInstance().getGameDir().toFile();
        System.out.println("[GL4DX12] Game directory: " + gameDir.getAbsolutePath());

        // 尝试1: 扫描 versions/ 下的 dx12mod 子目录
        File versionsDir = new File(gameDir, "versions");
        if (versionsDir.isDirectory()) {
            File[] versionDirs = versionsDir.listFiles(File::isDirectory);
            if (versionDirs != null) {
                for (File verDir : versionDirs) {
                    File dx12modDir = new File(verDir, "dx12mod");
                    File dllFile = new File(dx12modDir, "d3d12bridge.dll");
                    if (dllFile.exists()) {
                        try {
                            System.load(dllFile.getAbsolutePath());
                            System.out.println("[GL4DX12] DLL loaded from: " + dllFile.getAbsolutePath());
                            return;
                        } catch (UnsatisfiedLinkError e) {
                            System.err.println("[GL4DX12] Failed to load: " + dllFile.getAbsolutePath() + " - " + e.getMessage());
                        }
                    }
                }
            }
        }

        // 尝试2: 直接从 gameDir/dx12mod/ 加载
        File fallbackDir = new File(gameDir, "dx12mod");
        File fallbackDll = new File(fallbackDir, "d3d12bridge.dll");
        if (fallbackDll.exists()) {
            try {
                System.load(fallbackDll.getAbsolutePath());
                System.out.println("[GL4DX12] DLL loaded from fallback: " + fallbackDll.getAbsolutePath());
                return;
            } catch (UnsatisfiedLinkError e) {
                System.err.println("[GL4DX12] Fallback load failed: " + e.getMessage());
            }
        }

        System.err.println("[GL4DX12] FATAL: d3d12bridge.dll not found in any dx12mod directory under: " + gameDir.getAbsolutePath());
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
    public static native void nativeSetMvp(float[] matrix, int coordType);
    public static native void nativeRecordVertices(float[] vertices, int vertexCount, byte[] colors, int coordType);
    public static native void nativeRecordColors(float[] colors);
    public static native void nativeRecordUV(float[] uv);
    public static native void nativeDraw(int vertexCount);
    public static native String nativeGetD3D12Info();
    public static native boolean nativeIsD3D12Active();

    // Phase 3 stubs
    public static native void nativeSetSkyParameters(float[] params);
    public static native void nativeSetSkyParameters(float r, float g, float b, float a, float sunAngle, float moonAngle);
    public static native void nativeRenderSkybox();
    public static native void nativeRenderSky();
    public static native void nativeRenderTerrain();
    public static native void nativeUploadEntities(float[] entityData, int count);
    public static native void nativeRenderEntities();
    public static native void nativeUploadParticles(float[] vertices, int count, int vertexSize);
    public static native void nativeRenderParticles();
    public static native void nativeUploadTransparent(float[] vertices, int count, int vertexSize, float distance);
    public static native void nativeRenderTransparent();
    // Texture pipeline
    public static native void nativeUploadTextureEx(byte[] pixels, int w, int h, int texId);
}