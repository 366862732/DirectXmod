package com.dx12.client;

import com.dx12.DX12LibClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Frame capture: PBO double-buffer async readback.
 *
 * Per frame (every 4th tick):
 *   1. glReadPixels(full viewport -> PBO[cur])  — async, returns immediately
 *   2. glMapBuffer(PBO[prev]) -> flip Y -> nativeUpload  — read last frame
 *
 * No FBO downscale (doesn't work with Minecraft's framebuffer 0).
 * Throttled to every 4th frame to keep overhead low.
 */
public class D3D12Bridge {

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
        releasePBOs();
        nativeDestroy();
        d3d12Ready = false;
        System.out.println("[GL4DX12] D3D12 deactivated");
    }

    // GL state (passive → now ACTIVE translation)
    public static void glClearColor(float r, float g, float b, float a) {
        // Inactive: tracking caused flickering between sky/UI clear colors
    }
    public static void glClear(int mask) {
        // Clear happens implicitly in D3D12 render loop each frame
        // The clear color is already synced via glClearColor hook
    }
    public static int glGenBuffers() { return bufferIdCounter.getAndIncrement(); }
    public static void glBindBuffer(int target, int buffer) {}

    // ================================================================
    // GL→D3D12: BufferBuilder vertex capture (Minecraft render layer)
    // ================================================================
    private static final int MAX_TRANSLATED_VERTS = 65536; // per frame budget
    private static int translatedVertsThisFrame = 0;

    /**
     * Called from BufferBuilderMixin. MeshData has:
     *   drawState() → DrawState { mode(), vertexCount(), indexCount(),
     *                             format(), indexType() }
     *   vertexBuffer() → ByteBuffer (position always first 12 bytes)
     *   indexBuffer()  → ByteBuffer (if indexCount > 0)
     */
    public static void onMeshDataBuild(Object meshData) {
        if (!d3d12Ready || meshData == null) return;
        if (translatedVertsThisFrame >= MAX_TRANSLATED_VERTS) return;

        try {
            Class<?> mc = meshData.getClass();
            // meshData.drawState()
            Object drawState = mc.getMethod("drawState").invoke(meshData);
            if (drawState == null) return;
            Class<?> dsc = drawState.getClass();

            // Only handle QUADS and TRIANGLES (skip lines for now)
            Object modeObj = dsc.getMethod("mode").invoke(drawState);
            String modeName = modeObj instanceof Enum<?> ? ((Enum<?>) modeObj).name() : modeObj.toString();
            boolean isQuads = modeName.equals("QUADS");
            boolean isTriangles = modeName.equals("TRIANGLES") || modeName.equals("TRIANGLE_STRIP") || modeName.equals("TRIANGLE_FAN");
            if (!isQuads && !isTriangles) return;

            int vertexCount = (int) dsc.getMethod("vertexCount").invoke(drawState);
            int indexCount  = (int) dsc.getMethod("indexCount").invoke(drawState);
            if (vertexCount <= 0) return;

            // Vertex format → stride
            Object formatObj = dsc.getMethod("format").invoke(drawState);
            int vertStride = (int) formatObj.getClass().getMethod("getVertexSize").invoke(formatObj);
            if (vertStride <= 0) vertStride = 16;

            // Determine effective draw count and indices
            int drawVertCount;
            int[] indices = null; // for indexed drawing

            if (indexCount > 0) {
                // Indexed draw — read index buffer
                ByteBuffer ibuf = (ByteBuffer) mc.getMethod("indexBuffer").invoke(meshData);
                if (ibuf == null) return;
                Object indexTypeObj = dsc.getMethod("indexType").invoke(drawState);
                int idxBytes = (int) indexTypeObj.getClass().getField("bytes").get(indexTypeObj);
                if (idxBytes < 1) idxBytes = 2;

                int ibufLen = ibuf.remaining();
                if (indexCount * idxBytes > ibufLen) return;

                indices = new int[indexCount];
                ibuf = ibuf.duplicate();
                for (int i = 0; i < indexCount; i++) {
                    if (idxBytes == 4) indices[i] = ibuf.getInt();
                    else if (idxBytes == 2) indices[i] = ibuf.getShort() & 0xFFFF;
                    else indices[i] = ibuf.get() & 0xFF;
                }

                if (isQuads) {
                    // QUADS indexed: 4 indices per quad → 6 indices per quad (2 tris)
                    int quadCount = indexCount / 4;
                    drawVertCount = quadCount * 6;
                } else {
                    drawVertCount = indexCount;
                }
            } else {
                // Non-indexed draw
                if (isQuads) {
                    int quadCount = vertexCount / 4;
                    drawVertCount = quadCount * 6;
                } else {
                    drawVertCount = vertexCount;
                }
            }

            if (drawVertCount < 3 || drawVertCount > MAX_TRANSLATED_VERTS) return;

            // Read vertex buffer
            ByteBuffer vbuf = (ByteBuffer) mc.getMethod("vertexBuffer").invoke(meshData);
            if (vbuf == null || vbuf.remaining() < 12) return;
            vbuf = vbuf.duplicate(); // don't disturb original

            // Assemble float[7] per vertex: [x, y, z, r, g, b, a]
            float[] verts = new float[drawVertCount * 7];
            int outIdx = 0;

            if (indices != null) {
                // Indexed path
                for (int i = 0; i < (isQuads ? indexCount / 4 : indexCount); i++) {
                    if (isQuads) {
                        // 4-index quad → 2 triangles
                        int i0 = indices[i * 4], i1 = indices[i * 4 + 1],
                            i2 = indices[i * 4 + 2], i3 = indices[i * 4 + 3];
                        // tri1: 0,1,2  tri2: 2,3,0
                        readVertex(vbuf, i0, vertStride, verts, outIdx++);
                        readVertex(vbuf, i1, vertStride, verts, outIdx++);
                        readVertex(vbuf, i2, vertStride, verts, outIdx++);
                        readVertex(vbuf, i2, vertStride, verts, outIdx++);
                        readVertex(vbuf, i3, vertStride, verts, outIdx++);
                        readVertex(vbuf, i0, vertStride, verts, outIdx++);
                    } else {
                        readVertex(vbuf, indices[i], vertStride, verts, outIdx++);
                    }
                }
            } else {
                // Direct path
                for (int i = 0; i < (isQuads ? vertexCount / 4 : vertexCount); i++) {
                    if (isQuads) {
                        int vi = i * 4;
                        // tri1: 0,1,2  tri2: 2,3,0
                        readVertex(vbuf, vi,     vertStride, verts, outIdx++);
                        readVertex(vbuf, vi + 1, vertStride, verts, outIdx++);
                        readVertex(vbuf, vi + 2, vertStride, verts, outIdx++);
                        readVertex(vbuf, vi + 2, vertStride, verts, outIdx++);
                        readVertex(vbuf, vi + 3, vertStride, verts, outIdx++);
                        readVertex(vbuf, vi,     vertStride, verts, outIdx++);
                    } else {
                        readVertex(vbuf, i, vertStride, verts, outIdx++);
                    }
                }
            }

            translatedVertsThisFrame += drawVertCount;
            DX12LibClient.nativeSetPrimitiveTopology(4); // GL_TRIANGLES
            DX12LibClient.nativeRecordVertices(verts, drawVertCount);

        } catch (Exception e) {
            // Silently skip — reflection failure on unsupported format
        }
    }

    private static void readVertex(ByteBuffer buf, int vi, int stride, float[] out, int outIdx) {
        int off = vi * stride;
        int base = outIdx * 7;
        if (off + 8 > buf.limit()) return;
        out[base + 0] = buf.getFloat(off);
        out[base + 1] = buf.getFloat(off + 4);
        out[base + 2] = buf.getFloat(off + 8);
        if (stride >= 16) {
            out[base + 3] = (buf.get(off + 12) & 0xFF) / 255f;
            out[base + 4] = (buf.get(off + 13) & 0xFF) / 255f;
            out[base + 5] = (buf.get(off + 14) & 0xFF) / 255f;
            out[base + 6] = (buf.get(off + 15) & 0xFF) / 255f;
        } else {
            out[base + 3] = 1; out[base + 4] = 1;
            out[base + 5] = 1; out[base + 6] = 1;
        }
    }

    static void resetTranslatedCounter() {
        translatedVertsThisFrame = 0;
    }

    // ================================================================
    // PBO double-buffer state
    // ================================================================
    private static final int[] pboIds = {0, 0};
    private static int pboIdx = 0, capW = 0, capH = 0, capSize = 0;
    private static int frameNr = 0;
    private static boolean hasPrev = false;
    private static byte[] cachedFrame = null; // fallback when PBO isn't ready yet

    private static void releasePBOs() {
        if (pboIds[0] != 0) {
            // Unbind PBO before deleting
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL15.glDeleteBuffers(pboIds);
            pboIds[0] = pboIds[1] = 0;
        }
        capW = capH = capSize = 0;
        hasPrev = false;
    }

    private static void ensurePBOs(int w, int h) {
        int sz = w * h * 4;
        // Only grow, never shrink — avoids Invalid PBO errors from viewport bouncing
        if (capW >= w && capH >= h && pboIds[0] != 0) return;
        releasePBOs();
        capW = w; capH = h; capSize = sz;

        GL15.glGenBuffers(pboIds);
        for (int i = 0; i < 2; i++) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pboIds[i]);
            GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, (long)sz, GL21.GL_STREAM_READ);
        }
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        System.out.println("[GL4DX12] PBOs: " + w + "x" + h + " (" + (sz/1024) + "KB x2)");
    }

    // ================================================================
    // Frame capture (PBO async, every N frames)
    // ================================================================
    public static void captureFrame() {
        if (!d3d12Ready) return;

        // Reset draw call vertex budget every tick
        resetTranslatedCounter();

        // Capture every 4th tick (~15fps on 60fps game, enough for mirror)
        if (++frameNr % 4 != 0) return;

        int[] vp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        int w = vp[2], h = vp[3];
        if (w <= 0 || h <= 0) return;
        // Skip transient tiny viewports (Minecraft UI passes, etc.)
        if (w < 512 || h < 512) return;

        ensurePBOs(w, h);

        int cur = pboIdx, prev = (pboIdx + 1) % 2;
        pboIdx = prev;

        // 1. Read previous frame's PBO
        if (hasPrev) {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pboIds[prev]);
            ByteBuffer map = GL15.glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY);
            if (map != null) {
                byte[] out = new byte[capSize];
                for (int y = 0; y < capH; y++) {
                    map.position((capH - 1 - y) * capW * 4);
                    map.get(out, y * capW * 4, capW * 4);
                }
                GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
                DX12LibClient.nativeUploadPixels(out, capW, capH);
                cachedFrame = out; // cache for fallback
            } else if (cachedFrame != null && cachedFrame.length == capSize) {
                // PBO still in flight — reuse last good frame to avoid flicker
                DX12LibClient.nativeUploadPixels(cachedFrame, capW, capH);
            }
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
        }

        // 2. Start async read into current PBO
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pboIds[cur]);
        GL11.glReadPixels(0, 0, capW, capH, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
        GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);

        hasPrev = true;
    }
}
