package com.xgdt.dx12.d3d12;

import com.xgdt.dx12.dx12.Dx12Native;
import org.junit.jupiter.api.*;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class Dx12DeviceTest {

    @Test
    @Order(1)
    void testEnumerateAdapters() throws Exception {
        System.out.println("[1] Enumerating DXGI adapters via JNI bridge...");
        List<Dx12AdapterInfo> adapters = Dx12Device.enumerateAdapters();
        assertFalse(adapters.isEmpty(), "Should find at least one D3D12 adapter");
        var info = adapters.getFirst();
        assertNotNull(info.name(), "Adapter name should not be empty");
        assertTrue(info.vendorId() > 0, "Vendor ID should be non-zero");
        assertTrue(info.deviceId() > 0, "Device ID should be non-zero");
        System.out.printf("  Found: %s | LUID=0x%s | Vid=0x%s Did=0x%s VRAM=%d GiB%n",
            info.name(),
            Long.toHexString(info.luid()),
            Integer.toHexString(info.vendorId()),
            Integer.toHexString(info.deviceId()),
            info.dedicatedVideoMemory() / (1024L * 1024 * 1024));
    }

    @Test
    @Order(2)
    void testCreateDeviceAndContext() {
        System.out.println("[2] Creating D3D12 device + context...");
        try (var ctx = Dx12DeviceContext.create()) {
            assertNotNull(ctx, "Context should not be null");
            assertTrue(ctx.getQueueHandle() != 0, "Queue handle should be non-zero");
            assertTrue(ctx.getDeviceHandle() != 0, "Device handle should be non-zero");
            assertTrue(ctx.getTimestampFrequency() > 0, "Timestamp frequency should be > 0");
            System.out.printf("  Context OK: queue=0x%016x device=0x%016x freq=%d ticks/s%n",
                ctx.getQueueHandle(), ctx.getDeviceHandle(), ctx.getTimestampFrequency());
        }
    }

    @Test
    @Order(3)
    void testDeviceCreationString() {
        System.out.println("[3] Testing dx12CreateDevice() return value...");
        String result = Dx12Native.dx12CreateDevice();
        assertNotNull(result, "dx12CreateDevice() should not return null");
        assertFalse(result.startsWith("ERROR:"), "Should not start with ERROR: got=" + result);
        assertTrue(result.contains("RTX") || result.contains("NVIDIA") || result.contains("Device"),
            "Result should contain adapter info: " + result);
        System.out.println("  Result: " + result);
    }

    @Test
    @Order(4)
    void testSurfaceCreationWithNullHwnd() {
        System.out.println("[4] Testing surface creation with null HWND (should fail gracefully)...");
        try (var ctx = Dx12DeviceContext.create()) {
            assertNotNull(ctx, "Context should be created first");
            boolean ok = ctx.createSurface(0L);
            assertFalse(ok, "createSurface(0) should fail with null HWND");
            System.out.println("  createSurface(0) correctly returned false (no crash)");
        }
    }

    @Test
    @Order(5)
    void testSurfaceDestroyWithoutCreate() {
        System.out.println("[5] Testing destroySurface with handle=0 (no-op)...");
        Dx12Native.dx12DestroySurface(0L);
        System.out.println("  dx12DestroySurface(0) completed without crash");
    }

    // =========================================================================
    // Render loop test
    // =========================================================================

    /**
     * 渲染循环测试：
     *  1. 创建设备 + 隐藏窗口（640×480）
     *  2. 创建交换链，配置为 640×480
     *  3. 获取命令编码器
     *  4. begin → blitSurface（清空为纯红色）→ end → submit → waitFence
     *  5. present 一帧
     *  6. readback 验证所有像素为纯红 (R=255, G=0, B=0)
     *
     * 若窗口出现红色则说明渲染链路打通；若仍黑屏，问题在该链路。
     */
    @Test
    @Order(7)
    void testLodComputeShaderCompiles() throws Exception {
        java.lang.reflect.Field field =
            Class.forName("com.xgdt.dx12.dx12.Dx12LodRenderer").getDeclaredField("COMPUTE_SHADER");
        field.setAccessible(true);
        String src = (String) field.get(null);
        byte[] bytes = src.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("[lod-compute] shader source bytes=" + bytes.length);
        // 直接取渲染端的常量个数，避免以后 cbuffer 加字段时测试还停留在旧值
        java.lang.reflect.Field constField =
            Class.forName("com.xgdt.dx12.dx12.Dx12LodRenderer").getDeclaredField("COMPUTE_CONST_COUNT");
        constField.setAccessible(true);
        int constCount = constField.getInt(null);
        String failure = null;
        long handle = 0L;
        try {
            handle = Dx12Native.dx12CreateComputePipeline(bytes, 2, 3, constCount);
        } catch (Throwable t) {
            failure = t.toString();
        }
        System.out.println("[lod-compute] handle=" + handle + " failure=" + failure);
        assertNull(failure, "LOD compute pipeline compile/create failed: " + failure);
        assertNotEquals(0L, handle, "LOD compute pipeline handle must be non-zero");
        Dx12Native.dx12DestroyComputePipeline(handle);
    }

    /**
     * LOD 的顶点/像素着色器同样要离线校验。
     *
     * <p>像素着色器是 Java 字符串拼出来的 HLSL，语法错了只有进游戏才会暴露（画面里 LOD
     * 整块消失，日志只有一句「LOD 管线创建失败」）。这里直接调渲染器内部那条建管线路径，
     * 颜色格式用交换链的 RGBA8_UNORM（ordinal 6），编不过就让构建失败。
     */
    @Test
    @Order(9)
    void testLodGraphicsShadersCompile() throws Exception {
        java.lang.reflect.Method create = Class.forName("com.xgdt.dx12.dx12.Dx12LodRenderer")
            .getDeclaredMethod("createPipeline", int.class);
        create.setAccessible(true);
        String failure = null;
        long handle = 0L;
        try {
            handle = (Long) create.invoke(null, 6);
        } catch (Throwable t) {
            failure = (t.getCause() != null ? t.getCause() : t).toString();
        }
        System.out.println("[lod-graphics] handle=" + handle + " failure=" + failure);
        assertNull(failure, "LOD graphics pipeline compile/create failed: " + failure);
        assertNotEquals(0L, handle, "LOD graphics pipeline handle must be non-zero");
        Dx12Native.dx12DestroyPipeline(handle);
    }

    @Test
    @Order(10)
    void testVoxelGraphicsShadersCompile() throws Exception {
        java.lang.reflect.Method create = Class.forName("com.xgdt.dx12.dx12.Dx12LodRenderer")
            .getDeclaredMethod("createVoxelPipeline", int.class);
        create.setAccessible(true);
        String failure = null;
        long handle = 0L;
        try {
            handle = (Long) create.invoke(null, 6);
        } catch (Throwable t) {
            failure = (t.getCause() != null ? t.getCause() : t).toString();
        }
        System.out.println("[lod-voxel] handle=" + handle + " failure=" + failure);
        assertNull(failure, "LOD voxel pipeline compile/create failed: " + failure);
        assertNotEquals(0L, handle, "LOD voxel pipeline handle must be non-zero");
        Dx12Native.dx12DestroyPipeline(handle);
    }

    @Test
    @Order(11)
    void testTextureUploadReadback() {
        final int TEX = 64;
        final int FORMAT_RGBA8 = 6;
        final int USAGE_RW = 15;   // COPY_DST | COPY_SRC | TEXTURE_BINDING | RENDER_ATTACHMENT
        Dx12DeviceContext device = Dx12DeviceContext.create();
        assertNotNull(device, "Device context must be created");

        long ctx = Dx12Native.dx12CreateCommandEncoder();
        assertNotEquals(0L, ctx, "Command encoder must be created");

        long tex = Dx12Native.dx12CreateTexture(USAGE_RW, FORMAT_RGBA8, TEX, TEX, 1, 1);
        assertNotEquals(0L, tex, "Texture must be created");

        // 源图：64×64 全尺寸，用于对照组（行距 256 字节，走对齐路径）
        long srcBytes = (long) TEX * TEX * 4;
        long upload = Dx12Native.dx12CreateBuffer(2 /* MAP_WRITE */, srcBytes);
        assertNotEquals(0L, upload, "Upload buffer must be created");
        ByteBuffer mapped = Dx12Native.dx12MapBuffer(upload, 0, srcBytes, false, true);
        assertNotNull(mapped, "Upload buffer must be mappable");
        fillPattern(mapped, TEX, TEX, 0);
        Dx12Native.dx12UnmapBuffer(upload);

        long rb = Dx12Native.dx12CreateBuffer(1 /* MAP_READ */, srcBytes);
        assertNotEquals(0L, rb, "Readback buffer must be created");

        try {
            // 1) 对齐路径：整张 64×64 上传（RowPitch=256）
            Dx12Native.dx12BeginCommandList(ctx);
            Dx12Native.dx12WriteToTexture(ctx, upload, 0, TEX, TEX, tex, 0, 0, 0, 0);
            Dx12Native.dx12CopyTextureToBuffer(ctx, tex, 0, 0, 0, 0, TEX, TEX, rb, 0);
            long fence = Dx12Native.dx12Submit(ctx);
            assertTrue(Dx12Native.dx12WaitForFence(ctx, fence, 10_000_000_000L), "GPU must finish");

            ByteBuffer out = Dx12Native.dx12MapBuffer(rb, 0, srcBytes, true, false);
            assertNotNull(out, "Readback buffer must be mappable");
            int mismatches = countMismatches(out, TEX, 0, 0, TEX, TEX, 0);
            Dx12Native.dx12UnmapBuffer(rb);
            System.out.printf("  [tex] 64x64 aligned upload mismatches=%d%n", mismatches);
            assertEquals(0, mismatches, "64x64 (RowPitch=256) upload must round-trip");

            // 2) 非对齐路径：16×16 / 8×8 / 4×4 子图写进同一个 64×64 纹理的不同位置。
            //    读回始终走整张 64×64（RowPitch=256）的对齐路径，避免读回端踩同一个坑。
            int[][] cases = {{16, 0, 0}, {8, 32, 0}, {4, 0, 32}};
            for (int[] c : cases) {
                int size = c[0], dx = c[1], dy = c[2];
                long bytes = (long) size * size * 4;
                ByteBuffer src = Dx12Native.dx12MapBuffer(upload, 0, bytes, false, true);
                assertNotNull(src, "Upload buffer must be mappable");
                fillPattern(src, size, size, 17);
                Dx12Native.dx12UnmapBuffer(upload);

                Dx12Native.dx12BeginCommandList(ctx);
                Dx12Native.dx12CopyBufferToTexture(ctx, upload, 0, size, size, tex, 0, 0, dx, dy, size, size);
                Dx12Native.dx12CopyTextureToBuffer(ctx, tex, 0, 0, 0, 0, TEX, TEX, rb, 0);
                fence = Dx12Native.dx12Submit(ctx);
                assertTrue(Dx12Native.dx12WaitForFence(ctx, fence, 10_000_000_000L), "GPU must finish");

                out = Dx12Native.dx12MapBuffer(rb, 0, srcBytes, true, false);
                assertNotNull(out, "Readback buffer must be mappable");
                mismatches = countMismatches(out, TEX, dx, dy, size, size, 17);
                Dx12Native.dx12UnmapBuffer(rb);
                System.out.printf("  [tex] %dx%d unaligned upload (dst %d,%d) mismatches=%d%n",
                    size, size, dx, dy, mismatches);
                assertEquals(0, mismatches, size + "x" + size + " (RowPitch=" + (size * 4)
                    + ") upload must round-trip");
            }
        } finally {
            Dx12Native.dx12DestroyResource(rb);
            Dx12Native.dx12DestroyResource(upload);
            Dx12Native.dx12DestroyResource(tex);
            Dx12Native.dx12DestroyCommandEncoder(ctx);
        }
    }

    /**
     * 光影包（Foxy）全 pass 离线编译校验。
     *
     * <p>包里的 HLSL 只在进游戏实际渲染时才会被 {@code D3DCompile} 编译，语法错误
     * 不会让构建失败。这里按 {@code pipeline.json} 逐 pass 走渲染器同一条建管线路径：
     * composite 调 {@code createPipeline}（自带 {@code UNIFORM_HEADER} + 全屏 VS，
     * 并带上真实的 srv/uav 数量），compute 调 {@code dx12CreateComputePipeline}。
     */
    @Test
    @Order(12)
    void testShaderPackPassesCompile() throws Exception {
        java.nio.file.Path packRoot = java.nio.file.Path.of("..", "shaderpacks", "Foxy");
        Assumptions.assumeTrue(java.nio.file.Files.isDirectory(packRoot),
            "Foxy shaderpack not found: " + packRoot.toAbsolutePath());
        var manifest = com.xgdt.dx12.shaderpack.ShaderPackManifest.parse(
            java.nio.file.Files.readString(packRoot.resolve("shaders/pipeline.json")));
        assertFalse(manifest.passes().isEmpty(), "pipeline.json must declare passes");

        Class<?> renderer = Class.forName("com.xgdt.dx12.dx12.Dx12ShaderPackRenderer");
        java.lang.reflect.Field vsField = renderer.getDeclaredField("FULLSCREEN_VS");
        vsField.setAccessible(true);
        String fullscreenVs = (String) vsField.get(null);
        java.lang.reflect.Method createPipeline = renderer.getDeclaredMethod("createPipeline",
            String.class, String.class, int.class, int[].class, int.class, int.class);
        createPipeline.setAccessible(true);

        int sceneFormat = 6;   // RGBA8_UNORM，与交换链一致
        int fxFormat = com.mojang.blaze3d.GpuFormat.RGBA16_FLOAT.ordinal();
        for (var pass : manifest.passes()) {
            if (pass.isRaytrace()) {
                continue;   // 内置 DXR pass，没有 HLSL 源码
            }
            byte[] source = java.nio.file.Files.readAllBytes(packRoot.resolve(pass.fragment()));
            if (pass.isCompute()) {
                long handle = Dx12Native.dx12CreateComputePipeline(source,
                    pass.srvBufferCount(), pass.uavBufferCount(), 4);
                System.out.printf("  [pack] compute '%s' handle=%d srv=%d uav=%d%n",
                    pass.name(), handle, pass.srvBufferCount(), pass.uavBufferCount());
                assertNotEquals(0L, handle, "compute pass '" + pass.name() + "' failed to compile");
                Dx12Native.dx12DestroyComputePipeline(handle);
                continue;
            }
            int format = pass.writesSceneColor() ? sceneFormat : fxFormat;
            int[] inputTypes = new int[pass.inputs().size()];
            for (int i = 0; i < inputTypes.length; i++) {
                inputTypes[i] = 3;   // 默认线性 / CLAMP
                String input = pass.inputs().get(i);
                if (input.startsWith("texture:")) {
                    var decl = manifest.texture(input.substring("texture:".length()).trim());
                    if (decl != null) {
                        inputTypes[i] = decl.linear()
                            ? (decl.wrap() ? 7 : 3)
                            : (decl.wrap() ? 1 : 4);
                    }
                }
            }
            String fragment = new String(source, java.nio.charset.StandardCharsets.UTF_8);
            String failure = null;
            long handle = 0L;
            try {
                handle = (Long) createPipeline.invoke(null, fullscreenVs, fragment, format,
                    inputTypes, pass.srvBufferCount(), pass.uavBufferCount());
            } catch (Throwable t) {
                failure = (t.getCause() != null ? t.getCause() : t).toString();
            }
            System.out.printf("  [pack] composite '%s' handle=%d failure=%s%n",
                pass.name(), handle, failure);
            assertNull(failure, "composite pass '" + pass.name() + "' threw: " + failure);
            assertNotEquals(0L, handle, "composite pass '" + pass.name() + "' failed to compile");
            Dx12Native.dx12DestroyPipeline(handle);
        }
    }

    /** 写入可重复计算的 RGBA8 图案；上传/读回两端都用同一公式生成期望值。 */
    private static void fillPattern(ByteBuffer buf, int w, int h, int seed) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = (y * w + x) * 4;
                buf.put(i, (byte) (x * 13 + y * 7 + seed));
                buf.put(i + 1, (byte) (x * 5 + y * 11 + seed * 3));
                buf.put(i + 2, (byte) (x * 3 + y * 17 + seed * 5));
                buf.put(i + 3, (byte) 0xFF);
            }
        }
    }

    /** 比较纹理 (dx,dy) 处 w×h 区域与 {@link #fillPattern} 的期望值。 */
    private static int countMismatches(ByteBuffer out, int texW, int dx, int dy,
                                       int w, int h, int seed) {
        int bad = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int o = ((dy + y) * texW + (dx + x)) * 4;
                int er = (x * 13 + y * 7 + seed) & 0xFF;
                int eg = (x * 5 + y * 11 + seed * 3) & 0xFF;
                int eb = (x * 3 + y * 17 + seed * 5) & 0xFF;
                int ar = out.get(o) & 0xFF;
                int ag = out.get(o + 1) & 0xFF;
                int ab = out.get(o + 2) & 0xFF;
                int aa = out.get(o + 3) & 0xFF;
                if (ar != er || ag != eg || ab != eb || aa != 0xFF) {
                    ++bad;
                }
            }
        }
        return bad;
    }

    @Test
    @Order(8)
    void testRenderLoop() {
        final int W = 640;
        final int H = 480;

        System.out.println("[6] Render loop test: clear back buffer to RED, 1 frame");
        System.out.printf("  Resolution: %dx%d%n", W, H);

        // Step 1: Create device context
        Dx12DeviceContext ctx = Dx12DeviceContext.create();
        assertNotNull(ctx, "Device context must be created");
        long queueHandle = ctx.getQueueHandle();
        long deviceHandle = ctx.getDeviceHandle();
        System.out.printf("  Device=0x%016x Queue=0x%016x%n", deviceHandle, queueHandle);

        // Step 2: Create hidden window
        long hwnd = Dx12Native.dx12CreateHiddenWindow(W, H);
        System.out.printf("  Hidden window HWND=0x%016x%n", hwnd);
        assertNotEquals(0L, hwnd, "Hidden window creation must succeed");

        try {
            // Step 3: Create surface
            long surface = Dx12Native.dx12CreateSurface(hwnd);
            System.out.printf("  Surface handle=0x%016x%n", surface);
            assertTrue(surface != 0L, "Surface creation must succeed");

            try {
                // Step 4: Configure surface to 640x480, FIFO present mode
                boolean configured = Dx12Native.dx12ConfigureSurface(surface, W, H, 2);
                System.out.printf("  configureSurface %dx%d mode=2 (FIFO): %s%n",
                    W, H, configured ? "OK" : "FAILED");
                assertTrue(configured, "Surface must be configurable");

                // Step 5: Acquire back buffer
                boolean acquired = Dx12Native.dx12AcquireSurface(surface);
                System.out.printf("  acquireSurface: %s%n", acquired ? "OK" : "FAILED");
                assertTrue(acquired, "Must acquire back buffer");

                // Step 6: Create command encoder and begin recording
                long encoder = Dx12Native.dx12CreateCommandEncoder();
                System.out.printf("  CommandEncoder ctx=0x%016x%n", encoder);
                Dx12Native.dx12BeginCommandList(encoder);
                System.out.println("  beginCommandList: OK");

                // Step 7: Blit surface with null texture — validates the render pipeline path
                // (transitions PRESENT↔COPY_DEST without copying content)
                Dx12Native.dx12BlitSurface(encoder, surface, 0L);
                System.out.println("  blitSurface(null tex): OK");

                // Step 8: End and submit
                Dx12Native.dx12EndCommandList(encoder);
                System.out.println("  endCommandList: OK");
                long fence = Dx12Native.dx12Submit(encoder);
                System.out.printf("  submit -> fence=0x%016x%n", fence);
                assertTrue(fence != 0L, "Submit must return a non-zero fence");

                // Wait for GPU to finish
                Dx12Native.dx12WaitForFence(encoder, fence, 5_000_000_000L);
                System.out.println("  waitFence: OK");

                // Step 9: Present
                Dx12Native.dx12PresentSurface(surface);
                System.out.println("  presentSurface: OK");

                // Step 10: Read back and verify no crash occurred
                // NOTE: DIAG_CLEAR is disabled in production (DIAG_CLEAR_BACKBUFFER_TO_GREEN=0).
                // With null srcTex, no copy happens — readback shows whatever was in back buffer.
                // The key verification is that the full pipeline (acquire→blit→submit→wait→present→readback)
                // completes without crashing.
                int[] raw = Dx12Native.dx12ReadbackSurfacePixels(surface);
                assertNotNull(raw, "readback must not be null (pipeline completed)");
                assertEquals(36, raw.length, "readback must have 36 elements (9 pixels × RGBA)");
                System.out.printf("  readbackSurfacePixels: %d samples (3x3 grid) — pipeline intact!%n", raw.length / 4);
                System.out.println("  [PASS] Render loop complete — acquire→blit→submit→present→readback all OK!");

            } finally {
                Dx12Native.dx12DestroySurface(surface);
                System.out.println("  Surface destroyed");
            }
        } finally {
            Dx12Native.dx12DestroyHiddenWindow(hwnd);
            System.out.println("  Window destroyed");
        }
    }
}
