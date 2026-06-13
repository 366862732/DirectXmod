package com.dx12.client;

import com.dx12.DX12LibClient;
import org.lwjgl.opengl.GL11;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 2 — D3D12 overlay on MC window with MVP matrix sync.
 *
 * BufferBuilder.build() @ RETURN → extract vertex data via reflection →
 * submit world-space coords to D3D12 DLL. MVP transform happens in D3D12 VS shader.
 */
public class D3D12Bridge {

    static { System.out.println("[GL4DX12] BRIDGE v24 loaded (overlay + MVP matrix)"); }

    private static final AtomicInteger bufferIdCounter = new AtomicInteger(1);
    private static volatile boolean d3d12Ready = false;

    // Lifecycle
    public static boolean nativeInit(long hwnd) {
        return DX12LibClient.nativeInit(hwnd);
    }
    public static void nativeDestroy() { DX12LibClient.nativeDestroy(); }

    public static boolean isD3D12Ready() { return d3d12Ready; }

    public static void ensureDeviceInitialized(long hwnd) {
        if (d3d12Ready) return;
        System.out.println("[GL4DX12] Activating D3D12 overlay (HWND="
            + Long.toHexString(hwnd) + ")");
        if (nativeInit(hwnd)) {
            d3d12Ready = true;
            System.out.println("[GL4DX12] D3D12 OVERLAY ACTIVE");
        } else {
            System.err.println("[GL4DX12] D3D12 init FAILED");
        }
    }

    public static void shutdownDevice() {
        if (!d3d12Ready) return;
        nativeDestroy();
        d3d12Ready = false;
        System.out.println("[GL4DX12] D3D12 deactivated");
    }

    /** Returns D3D12 adapter name + feature level string for F3 debug screen */
    public static String getD3D12Info() {
        if (!d3d12Ready) return null;
        return DX12LibClient.nativeGetD3D12Info();
    }

    public static boolean isD3D12Active() {
        return d3d12Ready && DX12LibClient.nativeIsD3D12Active();
    }

    public static int nativeGetWindowWidth() { return DX12LibClient.nativeGetWindowWidth(); }
    public static int nativeGetWindowHeight() { return DX12LibClient.nativeGetWindowHeight(); }

    // ================================================================
    // Stage 2: MVP Matrix Sync
    // ================================================================

    private static final float[] mvpMatrix = new float[16];
    private static boolean mvpDiagDone = false;
    private static boolean mvpWorking = false;
    static {
        // Identity
        mvpMatrix[0] = 1; mvpMatrix[5] = 1; mvpMatrix[10] = 1; mvpMatrix[15] = 1;
    }

    public static void syncMatrices() {
        if (!d3d12Ready) return;
        if (mvpWorking) {
            // MVP already captured — re-send each frame
            DX12LibClient.nativeSetMvp(mvpMatrix);
            return;
        }
        try {
            Class<?> rs = Class.forName("com.mojang.blaze3d.systems.RenderSystem");

            // Try MC 26.1.2 method names
            java.lang.reflect.Method getProj = null, getMV = null;
            String projMethod = null, mvMethod = null;

            // Attempt 1: getProjectionMatrix / getModelViewMatrix (MC 1.21.4-)
            try { getProj = rs.getMethod("getProjectionMatrix"); projMethod = "getProjectionMatrix"; } catch (NoSuchMethodException e) {}
            try { getMV = rs.getMethod("getModelViewMatrix"); mvMethod = "getModelViewMatrix"; } catch (NoSuchMethodException e) {}

            // Attempt 2: getProjectionMatrixStack / getModelViewMatrixStack (MC 1.21.5+ rename)
            if (getProj == null) { try { getProj = rs.getMethod("getProjectionMatrixStack"); projMethod = "getProjectionMatrixStack"; } catch (NoSuchMethodException e) {} }
            if (getMV == null) { try { getMV = rs.getMethod("getModelViewMatrixStack"); mvMethod = "getModelViewMatrixStack"; } catch (NoSuchMethodException e) {} }

            // Attempt 3: field access via getModelViewProjectionMatrix (if it's a single combined getter)
            if (getProj == null && getMV == null) {
                try {
                    java.lang.reflect.Method getMVP = rs.getMethod("getModelViewProjectionMatrix");
                    Object mvpObj = getMVP.invoke(null);
                    java.lang.reflect.Method getArr = mvpObj.getClass().getMethod("get", float[].class);
                    getArr.invoke(mvpObj, (Object) mvpMatrix);
                    mvpWorking = true;
                    DX12LibClient.nativeSetMvp(mvpMatrix);
                    if (!mvpDiagDone) {
                        mvpDiagDone = true;
                        System.out.println("[GL4DX12] MVP: using getModelViewProjectionMatrix()");
                    }
                    return;
                } catch (NoSuchMethodException e) {}
            }

            if (getProj != null && getMV != null) {
                Object projMat = getProj.invoke(null);
                Object mvMat = getMV.invoke(null);
                if (projMat != null && mvMat != null) {
                    // MVP = projection * modelView
                    java.lang.reflect.Method mulMethod = projMat.getClass()
                        .getMethod("mul", projMat.getClass());
                    Object mvp = mulMethod.invoke(projMat, mvMat);
                    java.lang.reflect.Method getArr = mvp.getClass().getMethod("get", float[].class);
                    getArr.invoke(mvp, (Object) mvpMatrix);
                    mvpWorking = true;
                    DX12LibClient.nativeSetMvp(mvpMatrix);
                    if (!mvpDiagDone) {
                        mvpDiagDone = true;
                        System.out.println("[GL4DX12] MVP: " + projMethod + " * " + mvMethod + " OK");
                    }
                    return;
                }
            }
        } catch (Exception e) {
            if (!mvpDiagDone) {
                mvpDiagDone = true;
                System.out.println("[GL4DX12] MVP sync FAILED: " + e.getMessage());
            }
        }
        if (!mvpDiagDone) {
            mvpDiagDone = true;
            System.out.println("[GL4DX12] MVP: ALL methods absent — geometry will be identity");
        }
    }

    // ================================================================
    // GL→D3D12: BufferBuilder vertex capture (Phase 2: world-space)
    // ================================================================

    private static final int MAX_TRANSLATED_VERTS = 262144;
    private static int translatedVertsThisFrame = 0;
    private static boolean firstDrawDiag = true;

    // VertexFormatElement static instances for offset lookup
    private static Object VFE_POSITION, VFE_COLOR, VFE_UV0;
    static {
        try {
            Class<?> vfe = Class.forName("com.mojang.blaze3d.vertex.VertexFormatElement");
            VFE_POSITION = vfe.getField("POSITION").get(null);
            VFE_COLOR    = vfe.getField("COLOR").get(null);
            VFE_UV0      = vfe.getField("UV0").get(null);
        } catch (Exception ignored) {}
    }

    /**
     * MC 26.1.2: processMeshData is called from BufferBuilder.build() @ RETURN.
     * Gets MeshData.vertexBuffer(), MeshData.drawState() → DrawState(vertexCount, format, mode).
     * Vertices are submitted in MC world-space; MVP transform happens in D3D12 VS shader.
     */
    public static void processMeshData(Object meshData) {
        if (!d3d12Ready) return;
        if (translatedVertsThisFrame >= MAX_TRANSLATED_VERTS) return;

        try {
            java.lang.reflect.Method vbufM = meshData.getClass().getMethod("vertexBuffer");
            ByteBuffer vbuf = (ByteBuffer) vbufM.invoke(meshData);
            if (vbuf == null) return;

            java.lang.reflect.Method dsM = meshData.getClass().getMethod("drawState");
            Object ds = dsM.invoke(meshData);
            if (ds == null) return;

            java.lang.reflect.Method vcM = ds.getClass().getMethod("vertexCount");
            int vertexCount = (int) vcM.invoke(ds);
            if (vertexCount < 2) return;

            java.lang.reflect.Method fmtM = ds.getClass().getMethod("format");
            Object fmt = fmtM.invoke(ds);
            if (fmt == null) return;

            java.lang.reflect.Method vsM = fmt.getClass().getMethod("getVertexSize");
            int vertStride = (int) vsM.invoke(fmt);
            if (vertStride <= 0) return;

            java.lang.reflect.Method modeM = ds.getClass().getMethod("mode");
            Object modeObj = modeM.invoke(ds);
            String modeName = ((Enum<?>) modeObj).name();

            boolean isQuads = modeName.equals("QUADS"), isTriangles = modeName.equals("TRIANGLES");
            boolean isTriStrip = modeName.equals("TRIANGLE_STRIP"), isTriFan = modeName.equals("TRIANGLE_FAN");
            boolean isLines = modeName.equals("LINES"), isLineStrip = modeName.equals("LINE_STRIP");
            boolean isLineLoop = modeName.equals("LINE_LOOP");
            boolean isDLine = modeName.equals("DEBUG_LINES"), isDLStrip = modeName.equals("DEBUG_LINE_STRIP");
            if (!isQuads && !isTriangles && !isTriStrip && !isTriFan
                && !isLines && !isLineStrip && !isLineLoop && !isDLine && !isDLStrip) return;
            if (isDLine) isLines = true;
            if (isDLStrip) isLineStrip = true;

            int posOff = 0, colOff = -1, uvOff = -1, colSize = 0;
            if (VFE_POSITION != null && VFE_COLOR != null && VFE_UV0 != null) {
                java.lang.reflect.Method getOff = fmt.getClass().getMethod("getOffset",
                    Class.forName("com.mojang.blaze3d.vertex.VertexFormatElement"));
                posOff = (int) getOff.invoke(fmt, VFE_POSITION);
                try { colOff = (int) getOff.invoke(fmt, VFE_COLOR); colSize = 4; }
                catch (Exception e) { colOff = -1; }
                try { uvOff  = (int) getOff.invoke(fmt, VFE_UV0); }
                catch (Exception e) { uvOff = -1; }
            }

            int drawVertCount, topologyNative;
            if (isQuads)      { drawVertCount = vertexCount / 4 * 6; topologyNative = 4; }
            else if (isTriStrip) { drawVertCount = (vertexCount - 2) * 3; topologyNative = 4; }
            else if (isTriFan)   { drawVertCount = (vertexCount - 2) * 3; topologyNative = 4; }
            else if (isLines)    { drawVertCount = vertexCount; topologyNative = 2; }
            else if (isLineStrip){ drawVertCount = (vertexCount - 1) * 2; topologyNative = 2; }
            else if (isLineLoop) { drawVertCount = vertexCount * 2; topologyNative = 2; }
            else               { drawVertCount = vertexCount; topologyNative = 4; }
            if (drawVertCount < 2 || drawVertCount > MAX_TRANSLATED_VERTS - translatedVertsThisFrame) return;

            ByteBuffer work = vbuf.duplicate();
            work.order(java.nio.ByteOrder.nativeOrder());
            work.rewind();
            if (work.remaining() < vertexCount * vertStride) return;

            // Build float[9] array: [x,y,z, r,g,b,a, u,v] per vertex — world-space coords
            float[] verts = new float[drawVertCount * 9];
            int outIdx = 0;
            if (isQuads) {
                for (int q = 0; q < vertexCount / 4; q++) {
                    int v0 = q * 4, v1 = v0+1, v2 = v0+2, v3 = v0+3;
                    readV(work, v0, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, v1, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, v2, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, v2, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, v3, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, v0, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                }
            } else if (isTriStrip) {
                for (int i = 0; i < vertexCount - 2; i++) {
                    readV(work, i, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    if ((i & 1) != 0) {
                        readV(work, i+2, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                        readV(work, i+1, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    } else {
                        readV(work, i+1, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                        readV(work, i+2, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    }
                }
            } else if (isTriFan) {
                for (int i = 0; i < vertexCount - 2; i++) {
                    readV(work, 0, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, i+1, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, i+2, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                }
            } else if (isLineStrip) {
                for (int i = 0; i < vertexCount - 1; i++) {
                    readV(work, i, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, i+1, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                }
            } else if (isLineLoop) {
                for (int i = 0; i < vertexCount; i++) {
                    readV(work, i, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                    readV(work, (i+1) % vertexCount, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
                }
            } else {
                for (int i = 0; i < vertexCount; i++)
                    readV(work, i, vertStride, posOff, colOff, colSize, uvOff, verts, outIdx++);
            }

            translatedVertsThisFrame += drawVertCount;

            if (firstDrawDiag) {
                firstDrawDiag = false;
                System.out.println("[GL4DX12] FIRST vtx: raw0=(" + verts[0] + "," + verts[1] + "," + verts[2]
                    + ") stride=" + vertStride + " drawVerts=" + drawVertCount);
            }

            // Submit world-space vertices — VS shader applies MVP transform
            DX12LibClient.nativeSetPrimitiveTopology(topologyNative);
            DX12LibClient.nativeSetDrawTexture(currentBoundTexture);
            if (uvOff >= 0) {
                DX12LibClient.nativeRecordVerticesUV(verts, drawVertCount);
            } else {
                float[] v7 = new float[drawVertCount * 7];
                for (int i = 0; i < drawVertCount; i++)
                    System.arraycopy(verts, i * 9, v7, i * 7, 7);
                DX12LibClient.nativeRecordVertices(v7, drawVertCount);
            }
        } catch (Exception e) { /* silently skip unsupported draws */ }
    }

    // ================================================================
    // GL state mirror (unchanged from v23)
    // ================================================================

    public static void glClearColor(float r, float g, float b, float a) {}
    public static void glClear(int mask) {}
    public static int glGenBuffers() { return bufferIdCounter.getAndIncrement(); }
    public static void glBindBuffer(int target, int buffer) {}

    private static final int GLB_BLEND        = 1;
    private static final int GLB_DEPTH        = 2;
    private static final int GLB_CULL         = 4;
    private static final int GLB_DEPTH_WRITE  = 8;
    private static int glEnableMask = 0;

    public static void onGlEnable(int cap) {
        if (!d3d12Ready) return;
        int bit = capToBit(cap);
        if (bit != 0) { glEnableMask |= bit; DX12LibClient.nativeSetGlState(bit, 0); }
    }
    public static void onGlDisable(int cap) {
        if (!d3d12Ready) return;
        int bit = capToBit(cap);
        if (bit != 0) { glEnableMask &= ~bit; DX12LibClient.nativeSetGlState(0, bit); }
    }
    public static void onGlCullFace(int mode) {
        if (!d3d12Ready) return;
        glEnableMask |= GLB_CULL;
        DX12LibClient.nativeSetGlState(GLB_CULL, 0);
    }
    public static void onGlDepthMask(boolean flag) {
        if (d3d12Ready) DX12LibClient.nativeSetDepthMask(flag);
    }
    public static void onGlBlendFunc(int sfactor, int dfactor) {
        if (d3d12Ready) DX12LibClient.nativeSetBlendFunc(sfactor, dfactor);
    }
    public static void onGlViewport(int x, int y, int w, int h) {
        if (d3d12Ready && w >= 800 && w <= 4000 && h >= 400 && h <= 3000)
            DX12LibClient.nativeSetViewport(x, y, w, h);
    }

    private static int capToBit(int cap) {
        if (cap == 3042) return GLB_BLEND;
        if (cap == 2929) return GLB_DEPTH;
        if (cap == 2884) return GLB_CULL;
        return 0;
    }

    // ================================================================
    // Texture tracking
    // ================================================================

    private static int currentBoundTexture = 0;
    public static void onBindTexture(int texture) {
        currentBoundTexture = texture;
        if (d3d12Ready && texture > 0) {
            DX12LibClient.nativeSetTexture(texture);
        }
    }

    private static int texDiagCounter = 0;

    public static void onTexImage2D(int target, int level, int internalformat,
                                     int width, int height, int format, int type,
                                     java.nio.ByteBuffer pixels) {
        if (!d3d12Ready || currentBoundTexture <= 0 || width <= 0 || height <= 0 || pixels == null) return;
        if (pixels.remaining() < width * height * 3) return;
        if (++texDiagCounter % 256 == 0)
            System.out.println("[GL4DX12] TEX: #" + currentBoundTexture + " " + width + "x" + height);

        try {
            int texId = currentBoundTexture;
            byte[] rgba;
            int px = width * height;
            if (format == org.lwjgl.opengl.GL11.GL_RGBA && type == org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE) {
                rgba = new byte[pixels.remaining()];
                pixels.duplicate().get(rgba);
            } else if (format == org.lwjgl.opengl.GL11.GL_RGB && type == org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE) {
                byte[] src = new byte[pixels.remaining()];
                pixels.duplicate().get(src);
                rgba = new byte[px * 4];
                for (int i = 0, j = 0; i < px; i++) {
                    rgba[j++] = src[i * 3];
                    rgba[j++] = src[i * 3 + 1];
                    rgba[j++] = src[i * 3 + 2];
                    rgba[j++] = (byte)0xFF;
                }
            } else if (type == org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE) {
                byte[] src = new byte[pixels.remaining()];
                pixels.duplicate().get(src);
                rgba = new byte[px * 4];
                int bpp = pixels.remaining() / px;
                if (bpp == 4) {
                    if (format == 0x80E1) { // GL_BGRA
                        for (int i = 0, j = 0; i < px; i++) {
                            rgba[j++] = src[i * 4 + 2];
                            rgba[j++] = src[i * 4 + 1];
                            rgba[j++] = src[i * 4 + 0];
                            rgba[j++] = src[i * 4 + 3];
                        }
                    } else {
                        System.arraycopy(src, 0, rgba, 0, src.length);
                    }
                } else if (bpp == 3) {
                    for (int i = 0, j = 0; i < px; i++) {
                        rgba[j++] = src[i * 3];
                        rgba[j++] = src[i * 3 + 1];
                        rgba[j++] = src[i * 3 + 2];
                        rgba[j++] = (byte)0xFF;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
            DX12LibClient.nativeUploadTextureEx(rgba, width, height, texId);
        } catch (Exception e) {
            System.out.println("[GL4DX12] Texture upload fail: " + e.getMessage());
        }
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static void readV(ByteBuffer buf, int vi, int stride,
                              int posOff, int colOff, int colSize, int uvOff,
                              float[] out, int outIdx) {
        int base = outIdx * 9;
        int off = vi * stride;
        if (off + posOff + 12 > buf.limit()) return;

        // position: 3 floats in world space
        out[base + 0] = buf.getFloat(off + posOff);
        out[base + 1] = buf.getFloat(off + posOff + 4);
        out[base + 2] = buf.getFloat(off + posOff + 8);

        // color: MC BufferBuilder stores ABGR as little-endian UINT
        // byte[0]=B, byte[1]=G, byte[2]=R, byte[3]=A
        if (colOff >= 0 && off + colOff + colSize <= buf.limit()) {
            out[base + 3] = (buf.get(off + colOff + 2) & 0xFF) / 255f;  // R = byte[2]
            out[base + 4] = (buf.get(off + colOff + 1) & 0xFF) / 255f;  // G = byte[1]
            out[base + 5] = (buf.get(off + colOff) & 0xFF) / 255f;      // B = byte[0]
            out[base + 6] = (colSize >= 4) ? (buf.get(off + colOff + 3) & 0xFF) / 255f : 1f;
        } else {
            out[base + 3] = 1; out[base + 4] = 1;
            out[base + 5] = 1; out[base + 6] = 1;
        }

        // UV: 2 floats
        if (uvOff >= 0 && off + uvOff + 8 <= buf.limit()) {
            out[base + 7] = buf.getFloat(off + uvOff);
            out[base + 8] = buf.getFloat(off + uvOff + 4);
        } else {
            out[base + 7] = 0; out[base + 8] = 0;
        }
    }

    public static void resetTranslatedCounter() {
        translatedVertsThisFrame = 0;
    }

    /* ==== Unused stubs ==== */
    public static void resetStateOnFrame() {}
    public static void onBuiltBufferSubmit(Object o) {}
    public static void onBufferBuilderEnd(Object o) {}
    @Deprecated
    public static void onBufferBuilderBuild(Object o, Object f, Object m) {}
}
