/*
晚上编译不准给我报错
**/
//不准抽风
package com.dx12;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dx12.render.SkyboxExtractor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;

public class D3D12Bridge {
    // 顶点坐标空间类型
    public static final int COORD_WORLD = 0;   // 世界坐标，需要 MVP 变换
    public static final int COORD_SCREEN = 1;  // 屏幕坐标，需要正交投影
    public static final int COORD_NDC = 2;     // NDC 坐标，不需要变换

    static {
        System.out.println("[GL4DX12] ===== D3D12Bridge CLASS LOADED (NEW VERSION 20:45) =====");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("GL4DX12");

    private static volatile boolean d3d12Ready = false;
    private static boolean d3d12Active = false;
    private static LevelRenderState cachedLevelState;
    private static SkyRenderState cachedSkyState;
    private static final List<EntityRenderState> cachedEntityStates = new ArrayList<>();
    private static ParticlesRenderState cachedParticlesState;

    // 缓存窗口尺寸供矩阵计算
    private static int g_cachedW = 1280, g_cachedH = 720;

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

    public static boolean ensureDeviceInitialized(long hwnd) {
        System.out.println("[GL4DX12] ensureDeviceInitialized ENTER, hwnd=" + hwnd);

        if (d3d12Ready) {
            System.out.println("[GL4DX12] ensureDeviceInitialized: already ready, returning true");
            return true;
        }

        // hwnd 有效性校验
        if (hwnd == 0) {
            System.err.println("[GL4DX12] ensureDeviceInitialized: hwnd is 0, throwing exception");
            throw new IllegalStateException("[GL4DX12] Invalid window handle (hwnd=0)");
        }

        System.out.println("[GL4DX12] ensureDeviceInitialized: BEFORE try block");
        try {
            System.out.println("[GL4DX12] ensureDeviceInitialized: BEFORE nativeInit, hwnd=" + hwnd);
            System.out.println("[GL4DX12] DX12LibClient class: " + DX12LibClient.class);
            try {
                java.lang.reflect.Method m = DX12LibClient.class.getDeclaredMethod("nativeInit", long.class);
                System.out.println("[GL4DX12] nativeInit method: " + m);
            } catch (NoSuchMethodException e) {
                System.err.println("[GL4DX12] nativeInit method NOT FOUND: " + e);
            }
            boolean result = DX12LibClient.nativeInit(hwnd);
            System.out.println("[GL4DX12] AFTER nativeInit, result=" + result);
            d3d12Ready = result;
            if (result) {
                setD3D12Active(true);
                System.out.println("[GL4DX12] D3D12 initialized and activated");
            } else {
                System.err.println("[GL4DX12] D3D12 initialization FAILED! Check C:\\temp\\gl4dx12_d3d12.log for details");
            }
            return result;
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("[GL4DX12] nativeInit UNSATISFIED_LINK_ERROR: {}", e.getMessage());
            return false;
        } catch (Throwable t) {
            LOGGER.error("[GL4DX12] nativeInit exception: {}: {}", t.getClass().getName(), t.getMessage());
            return false;
        }
    }

    public static void shutdownDevice() {
        System.out.println("[GL4DX12] shutdownDevice called");
        d3d12Ready = false;
        setD3D12Active(false);
        DX12LibClient.nativeCleanup();
    }

    public static void onWindowResize(int width, int height) {
        g_cachedW = width;
        g_cachedH = height;
        if (d3d12Ready) {
            DX12LibClient.nativeResize(width, height);
        }
    }

    // ========== 矩阵同步 ==========

    public static void syncMatrices(Matrix4fc modelViewMatrix, CameraRenderState cameraState) {
        if (modelViewMatrix == null) {
            System.out.println("[GL4DX12] syncMatrices: modelView is NULL!");
            return;
        }
        try {
            // 从 MC 获取 ModelView 矩阵
            float[] mv = new float[16];
            if (modelViewMatrix != null) {
                modelViewMatrix.get(mv);
            } else {
                // fallback: identity
                for (int i = 0; i < 4; i++) mv[i * 5] = 1f;
            }

            // 获取投影矩阵
            float[] proj = new float[16];
            boolean projValid = false;
            if (cameraState != null) {
                try {
                    java.lang.reflect.Field projField = cameraState.getClass().getDeclaredField("projectionMatrix");
                    projField.setAccessible(true);
                    Matrix4fc projMat = (Matrix4fc) projField.get(cameraState);
                    if (projMat != null) {
                        projMat.get(proj);
                        // 检查投影矩阵是否有效（检测 Infinity/NaN + 投影特征）
                        projValid = true;
                        for (int i = 0; i < 16; i++) {
                            if (!Float.isFinite(proj[i])) {
                                projValid = false;
                                break;
                            }
                        }
                        // 额外检查：m[3][2] 应 ≈ -1（投影矩阵特征）
                        if (projValid && Math.abs(proj[11] + 1.0f) > 0.5f) {
                            System.out.println("[GL4DX12] projectionMatrix lacks projection (m[3][2]=" + proj[11] + ")");
                            projValid = false;
                        }
                        if (!projValid) {
                            System.out.println("[GL4DX12] projectionMatrix invalid, using hardcoded");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[GL4DX12] Failed to get projectionMatrix: " + e.getMessage());
                }
            }

            // 如果投影矩阵无效，使用硬编码的正确投影矩阵
            if (!projValid) {
                System.out.println("[GL4DX12] Using HARDCODED projection matrix");
                float fov = 70.0f;
                float aspect = (float)g_cachedW / (float)g_cachedH;
                if (aspect <= 0 || !Float.isFinite(aspect)) aspect = 16.0f / 9.0f;
                float zNear = 0.05f, zFar = 1000.0f;
                try {
                    java.lang.reflect.Field fovField = cameraState.getClass().getDeclaredField("fov");
                    fovField.setAccessible(true);
                    fov = fovField.getFloat(cameraState);
                } catch (Exception ignored) {}
                float tanHalfFov = (float)Math.tan(Math.toRadians(fov) / 2.0f);
                float[] hardcodedProj = {
                    1.0f / (tanHalfFov * aspect), 0, 0, 0,
                    0, 1.0f / tanHalfFov, 0, 0,
                    0, 0, (zFar + zNear) / (zNear - zFar), (2.0f * zFar * zNear) / (zNear - zFar),
                    0, 0, -1.0f, 0
                };
                System.arraycopy(hardcodedProj, 0, proj, 0, 16);
            }

            // 合并 MVP = projection * modelView
            float[] mvp = new float[16];
            Matrix4f pMat = new Matrix4f().set(proj);
            Matrix4f mvMat = new Matrix4f().set(mv);
            pMat.mul(mvMat).get(mvp);

            System.out.println("[GL4DX12] modelViewMatrix: " + Arrays.toString(mv));
            System.out.println("[GL4DX12] projectionMatrix: " + Arrays.toString(proj));
            System.out.println("[GL4DX12] final MVP: " + Arrays.toString(mvp));

            System.out.println("[GL4DX12] syncMatrices: calling nativeSetMvp with WORLD matrix");

            // 检查 MVP 矩阵是否有效
            boolean valid = true;
            for (int i = 0; i < 16; i++) {
                if (!Float.isFinite(mvp[i])) {
                    System.err.println("[GL4DX12] Invalid MVP matrix at index " + i + ": " + mvp[i] + ", using identity");
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                java.util.Arrays.fill(mvp, 0.0f);
                mvp[0] = 1.0f;
                mvp[5] = 1.0f;
                mvp[10] = 1.0f;
                mvp[15] = 1.0f;
            }

            DX12LibClient.nativeSetMvp(mvp, 0); // 0 = COORD_WORLD

            // 诊断：验证矩阵有效性
            boolean hasNonZero = false;
            for (int i = 0; i < 16; i++) {
                if (Math.abs(mvp[i]) > 0.0001f) { hasNonZero = true; break; }
            }
            System.out.println("[GL4DX12] syncMatrices: modelView=" + (modelViewMatrix != null) +
                               ", cameraState=" + (cameraState != null) +
                               ", mvpHasNonZero=" + hasNonZero +
                               ", mvp[0]=" + mvp[0] + ", mvp[5]=" + mvp[5] +
                               ", mvp[10]=" + mvp[10] + ", mvp[15]=" + mvp[15]);
        } catch (Exception e) {
            LOGGER.error("syncMatrices failed: {}", e.getMessage());
        }
    }

    public static void setWindowSize(int w, int h) {
        g_cachedW = w;
        g_cachedH = h;
    }

    // ========== 顶点数据处理 ==========

    /**
     * 处理从 BufferBuilder 捕获的 MeshData
     * 提取顶点、UV、颜色数据并传递给 C++ 端
     * 使用硬编码的顶点布局，绕过 VertexFormat.elements() 的兼容性问题
     */
    /**
     * 检测顶点坐标空间类型（改进版，使用 ByteBuffer 和窗口尺寸）
     */
    private static int cachedWindowWidth = -1;
    private static int cachedWindowHeight = -1;
    private static long lastWindowCheck = 0;

    private static int detectCoordSpace(float[] vertices, int vertexCount) {
        if (vertexCount == 0) return COORD_WORLD;

        // 采样前100个顶点或全部
        int sampleCount = Math.min(vertexCount, 100);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < sampleCount; i++) {
            int idx = i * 3;
            float x = vertices[idx], y = vertices[idx + 1], z = vertices[idx + 2];
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }

        // 规则1：NDC检测 - 所有值在 [-1.1, 1.1] 范围内
        if (maxX <= 1.1f && minX >= -1.1f &&
            maxY <= 1.1f && minY >= -1.1f &&
            maxZ <= 1.1f && minZ >= -1.1f) {
            System.out.printf("[GL4DX12] detectCoordSpace: NDC (X[%.1f,%.1f] Y[%.1f,%.1f] Z[%.1f,%.1f])%n",
                minX, maxX, minY, maxY, minZ, maxZ);
            return COORD_NDC;
        }

        // 规则1b：近NDC检测 — XY在窄范围内，Z也在范围内，说明顶点已过变换
        if (maxX <= 2.0f && minX >= -2.0f &&
            maxY <= 2.0f && minY >= -2.0f &&
            maxZ <= 2.0f && minZ >= -2.0f &&
            (maxX - minX) < 3.0f && (maxY - minY) < 3.0f) {
            System.out.printf("[GL4DX12] detectCoordSpace: near-NDC (X[%.2f,%.2f] Y[%.2f,%.2f] Z[%.2f,%.2f]) → NDC%n",
                minX, maxX, minY, maxY, minZ, maxZ);
            return COORD_NDC;
        }

        // 规则2：屏幕坐标检测
        int[] screenSize = getScreenSize();
        int width = screenSize[0], height = screenSize[1];
        float margin = 50.0f;
        boolean likelyScreenX = (minX >= -margin && maxX <= width + margin);
        boolean likelyScreenY = (minY >= -margin && maxY <= height + margin);
        boolean likelyScreenZ = (minZ >= -0.1f && maxZ <= 0.1f);
        boolean notTooLarge = (maxX < 5000 && maxY < 5000);

        if (likelyScreenX && likelyScreenY && likelyScreenZ && notTooLarge) {
            // 检测是否有深度变化：3D世界顶点通常有非零的Z值变化
            // 而2D屏幕/HUD顶点Z值通常全部为0
            boolean hasDepth = (maxZ - minZ) > 0.1f;
            if (hasDepth) {
                System.out.printf("[GL4DX12] detectCoordSpace: WORLD (has depth: Z[%.2f,%.2f])%n", minZ, maxZ);
                return COORD_WORLD;
            }
            System.out.printf("[GL4DX12] detectCoordSpace: SCREEN (X[%.1f,%.1f] Y[%.1f,%.1f] screen=%dx%d)%n",
                              minX, maxX, minY, maxY, width, height);
            return COORD_SCREEN;
        }

        // 规则3：世界坐标 - 大范围值
        if (maxX - minX > 100 || maxY - minY > 100 || maxZ - minZ > 1) {
            System.out.printf("[GL4DX12] detectCoordSpace: WORLD (X[%.1f,%.1f] Y[%.1f,%.1f] Z[%.1f,%.1f])%n",
                              minX, maxX, minY, maxY, minZ, maxZ);
            return COORD_WORLD;
        }

        System.out.printf("[GL4DX12] detectCoordSpace: default WORLD (X[%.1f,%.1f] Y[%.1f,%.1f])%n", minX, maxX, minY, maxY);
        return COORD_WORLD;
    }

    private static int[] getScreenSize() {
        long now = System.currentTimeMillis();
        if (cachedWindowWidth < 0 || now - lastWindowCheck > 500) {
            try {
                Object mc = Class.forName("net.minecraft.client.Minecraft")
                    .getMethod("getInstance").invoke(null);
                Object window = mc.getClass().getMethod("getWindow").invoke(mc);
                cachedWindowWidth = (int) window.getClass().getMethod("getWidth").invoke(window);
                cachedWindowHeight = (int) window.getClass().getMethod("getHeight").invoke(window);
                lastWindowCheck = now;
            } catch (Exception e) {
                cachedWindowWidth = 1920;
                cachedWindowHeight = 1080;
            }
        }
        return new int[]{cachedWindowWidth, cachedWindowHeight};
    }

    public static void processMeshData(Object meshData) {
        // ===== 强制初始化和激活 =====
        if (!d3d12Active) {
            System.out.println("[GL4DX12] processMeshData: d3d12Active=false, attempting forced init...");
            try {
                Minecraft client = Minecraft.getInstance();
                var window = client.getWindow();
                
                // 使用反射获取 handle 字段（最稳妥）
                long glfwWindow;
                try {
                    java.lang.reflect.Field handleField = window.getClass().getDeclaredField("handle");
                    handleField.setAccessible(true);
                    glfwWindow = (long) handleField.get(window);
                } catch (Exception e) {
                    System.err.println("[GL4DX12] Failed to get window handle via reflection: " + e.getMessage());
                    return;
                }
                
                long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfwWindow);
                if (hwnd != 0) {
                    boolean success = ensureDeviceInitialized(hwnd);
                    if (success) {
                        System.out.println("[GL4DX12] processMeshData: forced init succeeded");
                        setD3D12Active(true);
                    } else {
                        System.out.println("[GL4DX12] processMeshData: forced init failed");
                    }
                } else {
                    System.out.println("[GL4DX12] processMeshData: failed to get window handle");
                }
            } catch (Exception e) {
                LOGGER.error("[GL4DX12] processMeshData: forced init exception: {}", e.getMessage());
            }
        }
        System.out.println("[GL4DX12] processMeshData ENTERED, d3d12Active=" + d3d12Active + ", meshData=" + (meshData != null));
        if (!d3d12Active || meshData == null) {
            System.out.println("[GL4DX12] processMeshData: D3D12 not active or meshData null, returning");
            return;
        }

        try {
            // === 1. 获取 DrawState ===
            Method drawStateMethod = meshData.getClass().getMethod("drawState");
            Object drawState = drawStateMethod.invoke(meshData);
            if (drawState == null) {
                System.err.println("[GL4DX12] processMeshData: drawState is null");
                return;
            }

            // === 2. 获取顶点信息 ===
            Method formatMethod = drawState.getClass().getMethod("format");
            Object vertexFormat = formatMethod.invoke(drawState);

            Method vertexSizeMethod = vertexFormat.getClass().getMethod("getVertexSize");
            int vertexSize = (int) vertexSizeMethod.invoke(vertexFormat);

            Method vertexCountMethod = drawState.getClass().getMethod("vertexCount");
            int vertexCount = (int) vertexCountMethod.invoke(drawState);

            Method modeMethod = drawState.getClass().getMethod("mode");
            Object mode = modeMethod.invoke(drawState);

            System.out.println("[GL4DX12] processMeshData: vertexSize=" + vertexSize +
                              ", vertexCount=" + vertexCount +
                              ", mode=" + mode);

            if (vertexCount == 0) {
                System.out.println("[GL4DX12] processMeshData: vertexCount=0, skipping");
                return;
            }

            // === 3. 获取顶点缓冲区 ===
            Method vertexBufferMethod = meshData.getClass().getMethod("vertexBuffer");
            ByteBuffer vertexBuffer = (ByteBuffer) vertexBufferMethod.invoke(meshData);

            System.out.println("[GL4DX12] vertexBuffer class: " + vertexBuffer.getClass().getName());
            System.out.println("[GL4DX12] vertexBuffer position: " + vertexBuffer.position() +
                              ", limit: " + vertexBuffer.limit() +
                              ", capacity: " + vertexBuffer.capacity());

            if (vertexBuffer == null || vertexBuffer.capacity() == 0) {
                System.err.println("[GL4DX12] processMeshData: vertexBuffer is empty");
                return;
            }

            // === 4. 硬编码顶点布局（根据 vertexSize 推断） ===
            // MC 标准顶点布局：
            //   - POSITION: 3 floats (12 bytes) @ offset 0
            //   - COLOR: 4 bytes (ABGR) @ offset 12 (或 16)
            //   - UV: 2 floats (8 bytes) @ offset 16 (或 20)
            //
            // vertexSize=24: 标准布局 (3*4 + 4 + 2*4 = 24)
            // vertexSize=28: 有额外数据（可能是法线或光照）
            // vertexSize=32: 有更多额外数据
            int positionOffset = 0;
            int colorOffset = 12;
            int uvOffset = 16;

            // 根据 vertexSize 调整偏移量
            if (vertexSize == 12) {
                // 仅位置数据 (3 floats)，无颜色和UV
                positionOffset = 0;
                colorOffset = -1;
                uvOffset = -1;
            } else if (vertexSize == 16) {
                // 位置 + 颜色 (3 floats + 4 bytes ABGR)
                positionOffset = 0;
                colorOffset = 12;
                uvOffset = -1;
            } else if (vertexSize == 20) {
                // 位置 + 颜色 + padding (3 floats + 4 bytes + 4 bytes padding)
                positionOffset = 0;
                colorOffset = 12;
                uvOffset = -1;
            } else if (vertexSize == 24) {
                positionOffset = 0;
                colorOffset = 12;
                uvOffset = 16;
            } else if (vertexSize == 28) {
                positionOffset = 0;
                colorOffset = 16;
                uvOffset = 20;
            } else if (vertexSize == 32) {
                positionOffset = 0;
                colorOffset = 16;
                uvOffset = 24;
            } else if (vertexSize == 72) {
                // 大型顶点布局（如字体渲染），颜色在 offset 12
                positionOffset = 0;
                colorOffset = 12;
                uvOffset = 16;
            } else {
                // 未知布局，尝试标准布局
                positionOffset = 0;
                colorOffset = 12;
                uvOffset = 16;
            }

            System.out.println("[GL4DX12] Using layout: positionOffset=" + positionOffset +
                              ", colorOffset=" + colorOffset +
                              ", uvOffset=" + uvOffset);

            // === 5. 提取顶点数据 ===
            vertexBuffer.rewind();

            // 诊断：输出第一个顶点的原始浮点值
            if (vertexBuffer.capacity() >= 12) {
                float x0 = vertexBuffer.getFloat(0);
                float y0 = vertexBuffer.getFloat(4);
                float z0 = vertexBuffer.getFloat(8);
                System.out.println("[GL4DX12] First vertex raw: " + x0 + ", " + y0 + ", " + z0);
            }

            float[] vertices = new float[vertexCount * 3];
            float[] colors = new float[vertexCount * 4];
            float[] uvs = new float[vertexCount * 2];

            for (int i = 0; i < vertexCount; i++) {
                int baseOffset = i * vertexSize;

                // 提取位置 (x, y, z)
                for (int j = 0; j < 3; j++) {
                    int pos = baseOffset + positionOffset + j * 4;
                    if (pos + 4 <= vertexBuffer.capacity()) {
                        vertices[i * 3 + j] = vertexBuffer.getFloat(pos);
                    }
                }

                // 提取颜色 (ABGR → RGBA float)
                if (colorOffset >= 0) {
                    int colorPos = baseOffset + colorOffset;
                    if (colorPos + 4 <= vertexBuffer.capacity()) {
                        int abgr = vertexBuffer.getInt(colorPos);
                        colors[i * 4]     = ((abgr >> 16) & 0xFF) / 255f;
                        colors[i * 4 + 1] = ((abgr >> 8)  & 0xFF) / 255f;
                        colors[i * 4 + 2] = (abgr         & 0xFF) / 255f;
                        colors[i * 4 + 3] = ((abgr >> 24) & 0xFF) / 255f;
                    } else {
                        colors[i * 4] = colors[i * 4 + 1] = colors[i * 4 + 2] = colors[i * 4 + 3] = 1f;
                    }
                } else {
                    // 没有颜色数据，使用默认白色
                    colors[i * 4] = colors[i * 4 + 1] = colors[i * 4 + 2] = colors[i * 4 + 3] = 1f;
                }

                // 提取 UV (u, v)
                if (uvOffset >= 0) {
                    for (int j = 0; j < 2; j++) {
                        int uvPos = baseOffset + uvOffset + j * 4;
                        if (uvPos + 4 <= vertexBuffer.capacity()) {
                            uvs[i * 2 + j] = vertexBuffer.getFloat(uvPos);
                        } else {
                            uvs[i * 2 + j] = 0.0f;
                        }
                    }
                } else {
                    // 没有UV数据，使用默认值
                    uvs[i * 2] = 0.0f;
                    uvs[i * 2 + 1] = 0.0f;
                }
            }

            // === 6. 传递给 C++ 端 ===
            // 诊断：输出 Java 端顶点坐标范围
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (int i = 0; i < vertexCount; i++) {
                float x = vertices[i * 3], y = vertices[i * 3 + 1], z = vertices[i * 3 + 2];
                if (x < minX) minX = x; if (x > maxX) maxX = x;
                if (y < minY) minY = y; if (y > maxY) maxY = y;
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
            }
            System.out.println("[GL4DX12] Vertex range: X[" + minX + ", " + maxX +
                              "] Y[" + minY + ", " + maxY + "] Z[" + minZ + ", " + maxZ + "]");
            // 检测顶点类型
            int coordType = detectCoordSpace(vertices, vertexCount);
            System.out.println("[GL4DX12] coordType=" + coordType + " (0=WORLD, 1=SCREEN, 2=NDC)");
            // ===== 传递 =====
            if (vertexCount > 0) {
                // 将 float[] 颜色转换为 byte[] (ABGR packed)
                byte[] colorBytes = new byte[vertexCount * 4];
                for (int i = 0; i < vertexCount; i++) {
                    int r = (int)(colors[i * 4] * 255.0f) & 0xFF;
                    int g = (int)(colors[i * 4 + 1] * 255.0f) & 0xFF;
                    int b = (int)(colors[i * 4 + 2] * 255.0f) & 0xFF;
                    int a = (int)(colors[i * 4 + 3] * 255.0f) & 0xFF;
                    colorBytes[i * 4] = (byte)r;
                    colorBytes[i * 4 + 1] = (byte)g;
                    colorBytes[i * 4 + 2] = (byte)b;
                    colorBytes[i * 4 + 3] = (byte)a;
                }
                DX12LibClient.nativeRecordVertices(vertices, vertexCount, colorBytes, coordType);

                System.out.println("[GL4DX12] processMeshData: sent " + vertexCount +
                                  " vertices to native (total bytes: " + vertexBuffer.capacity() + ")");
            }

        } catch (Exception e) {
            LOGGER.error("[GL4DX12] processMeshData failed: {} ({})", e.getMessage(), e.getClass().getSimpleName());
        }
    }

    // ========== 完整帧渲染 ==========
    
    public static void renderFullFrame(LevelRenderState levelState, CameraRenderState cameraState, float partialTick, Matrix4fc modelViewMatrix) {
        if (modelViewMatrix == null) return;
        if (!d3d12Ready || !d3d12Active) return;

        // 1. 同步矩阵（使用 MC 的 modelView + cameraState 的投影）
        syncMatrices(modelViewMatrix, cameraState);

        // 2. 渲染天空盒
        if (levelState.skyRenderState != null) {
            renderSky(levelState.skyRenderState);
        }

        // 3. 渲染世界（方块）— 顶点已通过 BufferBuilderMixin 上传
        if (levelState.chunkSectionsToRender != null) {
            renderTerrain(levelState.chunkSectionsToRender);
        }

        // 4. 渲染实体
        if (!cachedEntityStates.isEmpty()) {
            renderEntities(cachedEntityStates, partialTick);
            cachedEntityStates.clear();
        }

        // 5. 渲染粒子
        if (cachedParticlesState != null) {
            renderParticles(cachedParticlesState);
            cachedParticlesState = null;
        }

        // Note: Present is handled exclusively by the C++ RenderLoop thread.
        // Java only uploads data and signals the render thread.
    }

    private static void renderSky(SkyRenderState skyState) {
        float[] skyData = SkyboxExtractor.extractSkyData(skyState);
        if (skyData != null) {
            DX12LibClient.nativeSetSkyParameters(skyData);
            DX12LibClient.nativeRenderSky();
        }
    }

    private static void renderTerrain(Object chunkSections) {
        // Terrain vertices are already uploaded via BufferBuilderMixin → processMeshData → nativeRecordVertices.
        // The RenderLoop handles drawing them from g_drawChunks automatically.
    }

    private static void renderEntities(List<EntityRenderState> entities, float partialTick) {
        // 提取实体数据，批量上传到 C++ 层
    }

    private static void renderParticles(ParticlesRenderState particlesState) {
        // 已有的粒子提取代码保持不变
    }

    // ========== Phase 3: 实体/粒子捕获 ==========

    public static void captureEntityRenderState(EntityRenderState state) {
        if (state != null) {
            cachedEntityStates.add(state);
        }
    }

    public static void captureParticles(ParticlesRenderState state) {
        cachedParticlesState = state;
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

    // ========== glDrawArrays / glDrawElements 钩子 ==========

    public static void onGlDrawArrays(int mode, int first, int count) {
        if (!isD3D12Active()) return;
        System.out.println("[GL4DX12] D3D12Bridge.onGlDrawArrays: mode=" + mode +
                           ", first=" + first + ", count=" + count);
    }

    public static void onGlDrawElements(int mode, int count, int type, long indices) {
        if (!isD3D12Active()) return;
        System.out.println("[GL4DX12] D3D12Bridge.onGlDrawElements: mode=" + mode +
                           ", count=" + count + ", type=" + type);
    }
}