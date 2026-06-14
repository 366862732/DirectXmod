package com.dx12;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class D3D12Bridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("GL4DX12");

    private static boolean d3d12Ready = false;
    private static boolean d3d12Active = false;
    private static LevelRenderState cachedLevelState;
    private static SkyRenderState cachedSkyState;

    private static float[] mvpMatrix = new float[16];

    // ========== 初始化和状态 ==========

    public static boolean isD3D12Ready() {
        return d3d12Ready;
    }

    public static boolean isD3D12Active() {
        return d3d12Active;
    }

    public static void setD3D12Active(boolean active) {
        d3d12Active = active;
        LOGGER.info("D3D12 active: {}", active);
    }

    public static void ensureDeviceInitialized(long hwnd) {
        LOGGER.info("ensureDeviceInitialized called, hwnd={}, current d3d12Ready={}", hwnd, d3d12Ready);
        if (!d3d12Ready) {
            LOGGER.info("Calling nativeInit...");
            boolean result = DX12LibClient.nativeInit(hwnd);
            d3d12Ready = result;
            LOGGER.info("D3D12 nativeInit returned: {}", result);
            if (result) {
                d3d12Active = true;
                LOGGER.info("D3D12 activated successfully");
            } else {
                LOGGER.error("D3D12 initialization FAILED! Check C:\\temp\\gl4dx12_d3d12.log for details");
            }
        }
    }

    public static void shutdownDevice() {
        if (d3d12Ready) {
            DX12LibClient.nativeCleanup();
            d3d12Ready = false;
            d3d12Active = false;
        }
    }

    public static void onWindowResize(int width, int height) {
        if (d3d12Ready) {
            DX12LibClient.nativeResize(width, height);
        }
    }

    // ========== 矩阵同步 ==========

    public static void syncMatrices() {
        try {
            for (int i = 0; i < 16; i++) {
                mvpMatrix[i] = (i % 5 == 0) ? 1f : 0f;
            }
            DX12LibClient.nativeSetMvp(mvpMatrix);
        } catch (Exception e) {
            LOGGER.error("syncMatrices failed: {}", e.getMessage());
        }
    }

    // ========== 顶点数据处理 ==========

    public static void processMeshData(Object meshData) {
        if (!DX12LibClient.isLibraryLoaded()) {
            LOGGER.error("D3D12 library not loaded, cannot process mesh data");
            return;
        }

        try {
            Class<?> meshDataClass = meshData.getClass();
            java.lang.reflect.Method getVertexBuffer = meshDataClass.getMethod("vertexBuffer");
            java.nio.ByteBuffer vertexBuffer = (java.nio.ByteBuffer) getVertexBuffer.invoke(meshData);
            vertexBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

            java.lang.reflect.Method getDrawState = meshDataClass.getMethod("drawState");
            Object drawState = getDrawState.invoke(meshData);

            Class<?> drawStateClass = drawState.getClass();
            java.lang.reflect.Field vertexCountField = drawStateClass.getDeclaredField("vertexCount");
            vertexCountField.setAccessible(true);
            int vertexCount = vertexCountField.getInt(drawState);

            java.lang.reflect.Field formatField = drawStateClass.getDeclaredField("format");
            formatField.setAccessible(true);
            Object format = formatField.get(drawState);

            java.lang.reflect.Field vertexSizeField = format.getClass().getDeclaredField("vertexSize");
            vertexSizeField.setAccessible(true);
            int vertexSize = vertexSizeField.getInt(format);

            float[] vertices = new float[vertexCount * 3];
            float[] colors = new float[vertexCount * 4];
            float[] uvs = new float[vertexCount * 2];

            for (int i = 0; i < vertexCount; i++) {
                int offset = i * vertexSize;
                vertices[i * 3] = vertexBuffer.getFloat(offset);
                vertices[i * 3 + 1] = vertexBuffer.getFloat(offset + 4);
                vertices[i * 3 + 2] = vertexBuffer.getFloat(offset + 8);
                int colorInt = vertexBuffer.getInt(offset + 12);
                colors[i * 4] = ((colorInt >> 16) & 0xFF) / 255f;
                colors[i * 4 + 1] = ((colorInt >> 8) & 0xFF) / 255f;
                colors[i * 4 + 2] = (colorInt & 0xFF) / 255f;
                colors[i * 4 + 3] = ((colorInt >> 24) & 0xFF) / 255f;
                uvs[i * 2] = vertexBuffer.getFloat(offset + 16);
                uvs[i * 2 + 1] = vertexBuffer.getFloat(offset + 20);
            }

            DX12LibClient.nativeRecordVertices(vertices);
            // DX12LibClient.nativeRecordColors(colors);  // 暂时注释
            // DX12LibClient.nativeRecordUV(uvs);         // 暂时注释
            DX12LibClient.nativeDraw(vertexCount);

        } catch (Exception e) {
            LOGGER.error("processMeshData failed: {}", e.getMessage());
        }
    }

    // ========== 兼容旧代码 ==========

    public static void resetTranslatedCounter() {}

    public static String getD3D12Info() {
        if (d3d12Ready) {
            return DX12LibClient.nativeGetD3D12Info();
        }
        return "D3D12 not initialized";
    }

    // ========== GL 兼容方法 ==========

    public static void glGenBuffers() {}
    public static void glBindBuffer(int target, int buffer) {}
    public static void glClear(int mask) {}
    public static void glClearColor(float r, float g, float b, float a) {}
    public static void onGlViewport(int x, int y, int w, int h) {}
    public static void onGlEnable(int cap) {}
    public static void onGlDisable(int cap) {}
    public static void onGlBlendFunc(int sfactor, int dfactor) {}
    public static void onGlDepthMask(boolean flag) {}
    public static void onGlCullFace(int mode) {}
    public static void onBindTexture(int texture) {}
    public static void onTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, java.nio.Buffer pixels) {}
}