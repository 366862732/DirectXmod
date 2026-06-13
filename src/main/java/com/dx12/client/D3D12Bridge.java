package com.dx12.client;

import com.dx12.DX12LibClient;
import org.lwjgl.opengl.GL11;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * v19 — Pure geometry translation.
 *
 * BufferBuilder.end() @ HEAD → extract vertex data via reflection →
 * transform pixel coords → submit to D3D12 DLL for rendering.
 *
 * No framebuffer capture, no mirror quad. Just translated MC geometry.
 */
public class D3D12Bridge {

    // VERSION STAMP — check this in latest.log to confirm JAR deployment
    static { System.out.println("[GL4DX12] BRIDGE v23 loaded (MeshData from build())"); }

    private static final AtomicInteger bufferIdCounter = new AtomicInteger(1);
    private static volatile boolean d3d12Ready = false;

    // Lifecycle
    public static boolean nativeInit(long h, int w, int ht) { return DX12LibClient.nativeInit(h, w, ht); }
    public static void nativeDestroy() { DX12LibClient.nativeDestroy(); }
    public static void nativeRender()  { DX12LibClient.nativeRender(); }

    public static boolean isD3D12Ready() { return d3d12Ready; }

    public static void ensureDeviceInitialized() {
        if (d3d12Ready) return;
        System.out.println("[GL4DX12] Activating D3D12...");
        if (nativeInit(0, 0, 0)) { d3d12Ready = true; System.out.println("[GL4DX12] D3D12 ACTIVE"); }
        else System.err.println("[GL4DX12] D3D12 init FAILED");
    }

    public static void shutdownDevice() {
        if (!d3d12Ready) return;
        nativeDestroy();
        d3d12Ready = false;
        System.out.println("[GL4DX12] D3D12 deactivated");
    }

    // GL state (passive monitor)
    public static void glClearColor(float r, float g, float b, float a) {}
    public static void glClear(int mask) {}
    public static int glGenBuffers() { return bufferIdCounter.getAndIncrement(); }
    public static void glBindBuffer(int target, int buffer) {}

    // GL state bits — must match C++ GLB_* defines
    private static final int GLB_BLEND        = 1;
    private static final int GLB_DEPTH        = 2;
    private static final int GLB_CULL         = 4;
    private static final int GLB_DEPTH_WRITE  = 8;
    private static int glEnableMask = 0;

    // GL blend enable/disable (glEnable/glDisable)
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
    // glCullFace sets GL_CULL implicitly
    public static void onGlCullFace(int mode) {
        if (!d3d12Ready) return;
        glEnableMask |= GLB_CULL;
        DX12LibClient.nativeSetGlState(GLB_CULL, 0);
    }
    // glDepthMask
    public static void onGlDepthMask(boolean flag) {
        if (d3d12Ready) DX12LibClient.nativeSetDepthMask(flag);
    }
    // glBlendFunc
    public static void onGlBlendFunc(int sfactor, int dfactor) {
        if (d3d12Ready) DX12LibClient.nativeSetBlendFunc(sfactor, dfactor);
    }
    // glViewport — forward to D3D12 only for main window sizes
    public static void onGlViewport(int x, int y, int w, int h) {
        if (d3d12Ready && w >= 800 && w <= 2000 && h >= 400 && h <= 1500)
            DX12LibClient.nativeSetViewport(x, y, w, h);
    }

    private static int capToBit(int cap) {
        // GL_BLEND=3042, GL_DEPTH_TEST=2929, GL_CULL_FACE=2884
        if (cap == 3042) return GLB_BLEND;
        if (cap == 2929) return GLB_DEPTH;
        if (cap == 2884) return GLB_CULL;
        return 0;
    }

    public static void resetStateOnFrame() {
        // Reset to default: blend on, depth+write on, no cull
        if (d3d12Ready) {
            DX12LibClient.nativeSetGlState(GLB_BLEND | GLB_DEPTH | GLB_DEPTH_WRITE, GLB_CULL);
            glEnableMask = GLB_BLEND | GLB_DEPTH | GLB_DEPTH_WRITE;
        }
    }

    // ================================================================
    // GL→D3D12: BufferBuilder vertex capture
    // ================================================================

    private static final int MAX_TRANSLATED_VERTS = 65536;
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
            applyPixelToClip(verts, drawVertCount);

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

    /* ==== LEGACY STUBS — no longer called ==== */
    public static void onBuiltBufferSubmit(Object o) {}
    public static void onBufferBuilderEnd(Object o) {}
    @Deprecated
    public static void onBufferBuilderBuild(Object o, Object f, Object m) {}

    // Pixel→clip transform using D3D12 window dimensions
    private static void applyPixelToClip(float[] verts, int drawVertCount) {
        int w = DX12LibClient.nativeGetWindowWidth();
        int h = DX12LibClient.nativeGetWindowHeight();
        if (w <= 0) w = 1280;
        if (h <= 0) h = 720;

        // Capture raw first vertex BEFORE transform
        float raw0x = verts[0], raw0y = verts[1];

        for (int i = 0; i < drawVertCount; i++) {
            int b = i * 9;
            verts[b + 0] = (verts[b + 0] / (w / 2f)) - 1f;
            verts[b + 1] = 1f - (verts[b + 1] / (h / 2f));
        }
        if (firstDrawDiag) {
            firstDrawDiag = false;
            System.out.println("[GL4DX12] FIRST: raw0=(" + raw0x + "," + raw0y
                + ") clip0=(" + verts[0] + "," + verts[1]
                + ") drawVerts=" + drawVertCount + " win=" + w + "x" + h);
        }
    }


    private static void readV(ByteBuffer buf, int vi, int stride,
                              int posOff, int colOff, int colSize, int uvOff,
                              float[] out, int outIdx) {
        int base = outIdx * 9;
        int off = vi * stride;
        if (off + posOff + 12 > buf.limit()) return;

        // position: always 3 floats
        out[base + 0] = buf.getFloat(off + posOff);
        out[base + 1] = buf.getFloat(off + posOff + 4);
        out[base + 2] = buf.getFloat(off + posOff + 8);

        // color: 3-4 ubytes → normalized float
        if (colOff >= 0 && off + colOff + colSize <= buf.limit()) {
            out[base + 3] = (buf.get(off + colOff) & 0xFF) / 255f;
            out[base + 4] = (buf.get(off + colOff + 1) & 0xFF) / 255f;
            out[base + 5] = (buf.get(off + colOff + 2) & 0xFF) / 255f;
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

    // Texture tracking
    private static int currentBoundTexture = 0;
    public static void onBindTexture(int texture) {
        currentBoundTexture = texture;
        if (d3d12Ready && texture > 0) {
            DX12LibClient.nativeSetTexture(texture);
        }
    }

    private static int texDiagCounter = 0;

    /** Called from glTexImage2D hook: captures pixel data and uploads to D3D12 SRV */
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
            // Convert common formats to RGBA8
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
                // Handle common format=GL_BGRA (0x80E1) and others with GL_UNSIGNED_BYTE
                byte[] src = new byte[pixels.remaining()];
                pixels.duplicate().get(src);
                rgba = new byte[px * 4];
                int bpp = pixels.remaining() / px; // bytes per pixel
                if (bpp == 4) {
                    // Could be RGBA, BGRA, etc. Try to detect and swizzle
                    // For BGRA (0x80E1), swap R<->B
                    if (format == 0x80E1) { // GL_BGRA
                        for (int i = 0, j = 0; i < px; i++) {
                            rgba[j++] = src[i * 4 + 2]; // B → R
                            rgba[j++] = src[i * 4 + 1]; // G → G
                            rgba[j++] = src[i * 4 + 0]; // R → B
                            rgba[j++] = src[i * 4 + 3]; // A → A
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
                return; // unsupported type
            }
            DX12LibClient.nativeUploadTextureEx(rgba, width, height, texId);
        } catch (Exception e) {
            System.out.println("[GL4DX12] Texture upload fail: " + e.getMessage());
        }
    }

    public static void resetTranslatedCounter() {
        translatedVertsThisFrame = 0;
    }
}
