package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.systems.SurfaceException;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.xgdt.dx12.config.Dx12Config;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Direct3D 12 {@link GpuBackend} implementation.
 *
 * Mirrors {@code com.mojang.blaze3d.vulkan.VulkanBackend}'s contract:
 * <ul>
 *   <li>{@link #setWindowHints()} runs inside GLFW's error scope before the
 *       window is created (no hints needed for DX12).</li>
 *   <li>{@link #handleWindowCreationErrors} is only called when
 *       {@code glfwCreateWindow} returns 0.</li>
 *   <li>{@link #createDevice} is called once the window exists; it runs the
 *       full D3D12 self-test chain and, when it passes, returns a live
 *       {@link Dx12Device} that drives the vanilla render flow (P6+).</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class Dx12Backend implements GpuBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12");

    @Override
    public String getName() {
        return "DX12";
    }

    @Override
    public void setWindowHints() {
        // DX12 needs no GLFW window hints (Vulkan sets NO_API; GL sets its context hints).
    }

    @Override
    public void handleWindowCreationErrors(GLFWErrorCapture.Error error) throws BackendCreationException {
        if (error != null) {
            throw new BackendCreationException(
                String.format(Locale.ROOT, "GLFW_ERROR: 0x%X", error.error()),
                BackendCreationException.Reason.GLFW_ERROR);
        }
        throw new BackendCreationException(
            "Failed to create window for DX12", BackendCreationException.Reason.GLFW_ERROR);
    }

    @Override
    public GpuDevice createDevice(long window, ShaderSource defaultShaderSource,
        GpuDebugOptions debugOptions, Runnable criticalShaderLoader) throws BackendCreationException {
        // P6: createDevice returns a real, live GpuDevice. The game then drives
        // the full vanilla render flow through our backend (RenderSystem
        // initRenderer, surface, per-frame render passes, present) with no
        // GL/Vulkan fallback. Self-tests run against the live device; only a
        // failure closes it and rethrows so the game can fall back.
        try {
            Dx12Device device = new Dx12Device(defaultShaderSource);
            try {
                selfTestJavaResources(device);
                selfTestCommandLayer(device);
                selfTestPipelines(device);
                selfTestSurface(device, window);
            } catch (Throwable t) {
                device.close();
                throw t;
            }
            // P6 诊断：自测阶段用嵌入 GLSL 填充了 pipeline cache，且 DIAG 绿色覆盖已生效。
            // 必须在返回给游戏前清除，否则 getOrCompilePipeline 会命中自测缓存条目，
            // 导致真实游戏 shader 从未被编译，屏幕显示的是自测阶段的嵌入 shader 输出（紫色）。
            device.clearPipelineCache();
            // 设备就绪后把配置里的上采样参数推送到原生 blit 管线。
            pushUpscaleParams();
            // 同上，把帧生成参数推送到原生渲染线程。
            pushFrameGenParams();
            // NGX（DLSS）：有 App ID 才会真正初始化，否则原生侧只记一条日志。
            pushNgxConfig();
            // DLSS 超分/帧生成配置（必须在 NGX 初始化之后推送）。
            pushDlssConfig();
            // XeSS 超分配置（与 DLSS 超分互斥，原生侧优先 DLSS）。
            pushXessConfig();
            return new GpuDevice(device, criticalShaderLoader);
        } catch (Throwable t) {
            LOGGER.error("[dx12] D3D12 self-test failed", t);
            throw new BackendCreationException(
                "DX12 self-test failed: " + t, BackendCreationException.Reason.OTHER);
        }
    }

    /**
     * 把配置中的上采样参数推送到原生 blit 管线。
     * 设备创建后调用一次；视频设置里改动时也可直接调用。
     */
    public static void pushUpscaleParams() {
        Dx12Config cfg = Dx12Config.getInstance();
        Dx12Native.dx12SetUpscaleParams(cfg.getUpscaleMode(), cfg.getUpscaleSharpness() / 100.0f);
    }

    /**
     * 把配置中的帧生成参数推送到原生渲染线程。
     * 设备创建后调用一次；视频设置里改动时也可直接调用。
     */
    public static void pushFrameGenParams() {
        Dx12Config cfg = Dx12Config.getInstance();
        // DLSS-G 开启时关闭自研插帧：两套帧生成同时 Present 会互相抢 back buffer。
        int mode = cfg.isCustomFrameGenActive() ? 1 : 0;
        Dx12Native.dx12SetFrameGenParams(mode, cfg.getFrameGenStrength() / 100.0f);
    }

    /**
     * 把 DLSS 超分/帧生成配置推送到原生侧。
     *
     * @return DLSS 超分是否可用（无 NVIDIA 显卡/驱动不支持时为 false）
     */
    public static boolean pushDlssConfig() {
        Dx12Config cfg = Dx12Config.getInstance();
        boolean srAvailable;
        boolean fgAvailable = false;
        try {
            srAvailable = Dx12Native.dx12SetDlssConfig(
                cfg.getDlssSr() == 1, cfg.getDlssQuality(), cfg.getDlssPreset(),
                cfg.getDlssFg() == 1, cfg.getDlssFgFrames());
            fgAvailable = Dx12Native.dx12DlssIsFgAvailable();
        } catch (Throwable t) {
            LOGGER.warn("[dx12] DLSS config push failed: {}", t.toString());
            srAvailable = false;
        }
        DlssFrameCapture.setSrAvailable(srAvailable);
        DlssFrameCapture.setFgAvailable(fgAvailable);
        // DLSS-G 开关会改变自研插帧是否生效，同步一次。
        pushFrameGenParams();
        // 配置变化会让时间历史失效（质量档/预设/尺寸都可能变化）。
        try {
            Dx12Native.dx12DlssReset();
        } catch (Throwable ignored) {
        }
        LOGGER.info("[dx12] DLSS config: sr={} quality={} preset={} fg={} frames={} srAvailable={} fgAvailable={}",
            cfg.getDlssSr(), cfg.getDlssQuality(), cfg.getDlssPreset(),
            cfg.getDlssFg(), cfg.getDlssFgFrames(), srAvailable, fgAvailable);
        return srAvailable;
    }

    /**
     * 把 XeSS 超分配置推送到原生侧。
     *
     * @return XeSS 是否可用（libxess.dll 缺失或设备不支持 SM 6.4 时为 false）
     */
    public static boolean pushXessConfig() {
        Dx12Config cfg = Dx12Config.getInstance();
        boolean available;
        try {
            available = Dx12Native.dx12SetXessConfig(
                cfg.getXessSr() == 1, cfg.getXessQuality());
        } catch (Throwable t) {
            LOGGER.warn("[dx12] XeSS config push failed: {}", t.toString());
            available = false;
        }
        DlssFrameCapture.setXessAvailable(available);
        // 超分后端切换会改变 jitter 与历史状态，统一重置一次。
        try {
            Dx12Native.dx12DlssReset();
        } catch (Throwable ignored) {
        }
        LOGGER.info("[dx12] XeSS config: sr={} quality={} available={}",
            cfg.getXessSr(), cfg.getXessQuality(), available);
        return available;
    }

    /** 读取原生侧 XeSS 状态（一行文案），供视频设置界面显示。 */
    public static String getXessStatus() {
        return Dx12Native.dx12GetXessStatus();
    }

    /**
     * 把配置中的 NGX（DLSS）App ID / Project ID 推送到原生侧。
     * 两者都为空时原生侧只写一条日志，DLSS 通道保持关闭。
     * 数据目录取 {@code <user.dir>/dx12mod}：与 dx12_mc.dll 同目录，NGX 从这里
     * 找 nvngx_dlss.dll，并写日志/临时文件。
     */
    public static void pushNgxConfig() {
        Dx12Config cfg = Dx12Config.getInstance();
        String appDataPath = java.nio.file.Path.of(System.getProperty("user.dir"), "dx12mod")
            .toAbsolutePath().toString();
        Dx12Native.dx12SetNgxConfig(cfg.getNgxAppId(), cfg.getNgxProjectId(), appDataPath);
        // 原生侧的 stderr（dbgLog）不会进启动器日志，这里把 NGX 状态转成 Java 日志，
        // 否则初始化成功与否、失败码是多少在 latest.log 里完全看不到。
        LOGGER.info("[dx12] {}", getNgxStatus());
    }

    /** 读取原生侧 NGX 状态（一行文案），供视频设置界面显示。 */
    public static String getNgxStatus() {
        return Dx12Native.dx12GetNgxStatus();
    }

    /**
     * Exercise every P2 resource method through the real JNI path so a broken
     * native binding fails loudly here instead of during vanilla initRenderer.
     */
    private static void selfTestJavaResources(Dx12Device device) {
        // texture: RGBA8_UNORM, 64x64, TEXTURE_BINDING|RENDER_ATTACHMENT
        GpuTexture texture = device.createTexture("dx12-selftest",
            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            GpuFormat.RGBA8_UNORM, 64, 64, 1, 1);
        // sampler: REPEAT/REPEAT, NEAREST/NEAREST, aniso=1
        GpuSampler sampler = device.createSampler(AddressMode.REPEAT, AddressMode.REPEAT,
            FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.of(16.0));
        // buffer: MAP_WRITE, map -> write -> unmap
        GpuBuffer buffer = device.createBuffer(() -> "dx12-selftest",
            GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST, 1024);
        try (GpuBufferSlice.MappedView view = buffer.map(0, 1024, false, true)) {
            ByteBuffer data = view.data();
            data.put(0, (byte) 0x5A);
            data.put(1023, (byte) 0x5A);
            if ((data.get(0) & 0xFF) != 0x5A || (data.get(1023) & 0xFF) != 0x5A) {
                throw new IllegalStateException("buffer map write/read mismatch");
            }
        }
        // view over the texture
        device.createTextureView(texture, 0, texture.getMipLevels()).close();
        buffer.close();
        sampler.close();
        texture.close();
        LOGGER.info("[dx12] Java resource self-test OK (texture/buffer/sampler/view via JNI)");
    }

    /**
     * Exercise the P3 command layer through the real JNI path: record
     * uploads/copies into a command list, submit through the fence, and verify
     * both a buffer copy readback and a texture write/readback round-trip.
     */
    private static void selfTestCommandLayer(Dx12Device device) {
        int size = 256;
        GpuBuffer src = device.createBuffer(() -> "dx12-selftest-src",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_COPY_SRC, size);
        GpuBuffer dst = device.createBuffer(() -> "dx12-selftest-dst",
            GpuBuffer.USAGE_MAP_READ, size);
        ByteBuffer data = ByteBuffer.allocate(size);
        for (int i = 0; i < size; ++i) {
            data.put(i, (byte) (i & 0xFF));
        }

        int texSize = 8;
        GpuTexture tex = device.createTexture("dx12-selftest-tex",
            GpuTexture.USAGE_TEXTURE_BINDING, GpuFormat.RGBA8_UNORM, texSize, texSize, 1, 1);
        GpuBuffer texDst = device.createBuffer(() -> "dx12-selftest-texdst",
            GpuBuffer.USAGE_MAP_READ, (long) texSize * texSize * 4);
        ByteBuffer texData = ByteBuffer.allocate(texSize * texSize * 4);
        for (int i = 0; i < texData.capacity(); ++i) {
            texData.put(i, (byte) (i & 0xFF));
        }

        Dx12CommandEncoderBackend encoder = new Dx12CommandEncoderBackend();
        try {
            GpuFence fence = encoder.createFence();
            encoder.writeToBuffer(src.slice(), data);
            encoder.copyToBuffer(src.slice(), dst.slice());
            encoder.writeToTexture(tex, texData, 0, 0, 0, 0, texSize, texSize);
            encoder.copyTextureToBuffer(tex, texDst, 0, () -> {}, 0);
            encoder.submit();
            if (!fence.awaitCompletion(5000L)) {
                throw new IllegalStateException("command submit timed out after 5s");
            }
            fence.close();

            // Buffer readback: every byte must survive the copy.
            try (GpuBufferSlice.MappedView view = dst.map(0, size, true, false)) {
                ByteBuffer read = view.data();
                System.err.println("[dx12-java] self-test: reading dst readback, size=" + size
                    + " dst.usage=" + Integer.toBinaryString(dst.usage()));
                System.err.flush();
                for (int i = 0; i < size; ++i) {
                    int actual = read.get(i) & 0xFF;
                    int expected = i & 0xFF;
                    if (actual != expected) {
                        // Dump surrounding bytes for diagnosis
                        System.err.printf("[dx12-java] mismatch at %d: expected=0x%02X actual=0x%02X",
                            i, expected, actual);
                        int start = Math.max(0, i - 8);
                        int end = Math.min(size, i + 8);
                        System.err.print(" neighbors=[");
                        for (int j = start; j < end; j++) {
                            if (j > start) System.err.print(',');
                            System.err.printf("0x%02X", read.get(j) & 0xFF);
                        }
                        System.err.println("]");
                        System.err.flush();
                        throw new IllegalStateException("buffer copy readback mismatch at " + i);
                    }
                }
            }

            // Texture readback: native writes tightly-packed rows (rowBytes per
            // row), matching what the caller allocated.
            int rowPitch = texSize * 4;
            try (GpuBufferSlice.MappedView view = texDst.map(0, texDst.size(), true, false)) {
                ByteBuffer read = view.data();
                for (int row = 0; row < texSize; ++row) {
                    for (int col = 0; col < texSize; ++col) {
                        int expected = (row * texSize + col) * 4 & 0xFF;  // texData first channel
                        if ((read.get(row * rowPitch + col * 4) & 0xFF) != expected) {
                            throw new IllegalStateException(
                                "texture readback mismatch at (" + col + "," + row + ")");
                        }
                    }
                }
            }
        } finally {
            encoder.close();
            texDst.close();
            tex.close();
            dst.close();
            src.close();
        }
        LOGGER.info("[dx12] Command layer self-test OK (submit/fence/copy/readback via JNI)");
    }

    // -----------------------------------------------------------------------
    // P4: pipeline self-test — 内嵌官方启动期 critical shader 的 GLSL 原文
    // （提取自 assets/minecraft/shaders/core/），编译官方真实 RenderPipeline
    // 全链路：GLSL -> shaderc SPIR-V -> spvc 反射/rebind -> HLSL SM5.1 ->
    // d3dcompiler DXBC -> root signature + 双 PSO。
    // -----------------------------------------------------------------------

    private static final String CORE_GUI_VSH = """
        #version 330

        // Can't moj_import in things used during startup, when resource packs don't exist.
        // This is a copy of dynamicimports.glsl and projection.glsl
        layout(std140) uniform DynamicTransforms {
            mat4 ModelViewMat;
            vec4 ColorModulator;
            vec3 ModelOffset;
            mat4 TextureMat;
        };
        layout(std140) uniform Projection {
            mat4 ProjMat;
        };

        in vec3 Position;
        in vec4 Color;

        out vec4 vertexColor;

        void main() {
            gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

            vertexColor = Color;
        }
        """;

    private static final String CORE_GUI_FSH = """
        #version 330

        // Can't moj_import in things used during startup, when resource packs don't exist.
        // This is a copy of dynamicimports.glsl
        layout(std140) uniform DynamicTransforms {
            mat4 ModelViewMat;
            vec4 ColorModulator;
            vec3 ModelOffset;
            mat4 TextureMat;
        };

        in vec4 vertexColor;

        out vec4 fragColor;

        void main() {
            vec4 color = vertexColor;
            if (color.a == 0.0) {
                discard;
            }
            fragColor = color * ColorModulator;
        }
        """;

    private static final String CORE_POSITION_TEX_COLOR_VSH = """
        #version 330

        // Can't moj_import in things used during startup, when resource packs don't exist.
        // This is a copy of dynamicimports.glsl and projection.glsl
        layout(std140) uniform DynamicTransforms {
            mat4 ModelViewMat;
            vec4 ColorModulator;
            vec3 ModelOffset;
            mat4 TextureMat;
        };
        layout(std140) uniform Projection {
            mat4 ProjMat;
        };

        in vec3 Position;
        in vec2 UV0;
        in vec4 Color;

        out vec2 texCoord0;
        out vec4 vertexColor;

        void main() {
            gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

            texCoord0 = UV0;
            vertexColor = Color;
        }
        """;

    private static final String CORE_POSITION_TEX_COLOR_FSH = """
        #version 330

        // Can't moj_import in things used during startup, when resource packs don't exist.
        // This is a copy of dynamicimports.glsl
        layout(std140) uniform DynamicTransforms {
            mat4 ModelViewMat;
            vec4 ColorModulator;
            vec3 ModelOffset;
            mat4 TextureMat;
        };

        uniform sampler2D Sampler0;

        in vec2 texCoord0;
        in vec4 vertexColor;

        out vec4 fragColor;

        void main() {
            vec4 color = texture(Sampler0, texCoord0) * vertexColor;
            if (color.a == 0.0) {
                discard;
            }
            fragColor = color * ColorModulator;
        }
        """;

    /** 自检 shader 源：createDevice 时资源包未加载，用内嵌 GLSL 顶替真实 ShaderSource。 */
    private static final ShaderSource EMBEDDED_SHADER_SOURCE = (id, type) -> {
        if (!id.getPath().startsWith("core/")) {
            return null;
        }
        return switch (id.getPath()) {
            case "core/gui" -> type == ShaderType.VERTEX ? CORE_GUI_VSH : CORE_GUI_FSH;
            case "core/position_tex_color" -> type == ShaderType.VERTEX
                ? CORE_POSITION_TEX_COLOR_VSH : CORE_POSITION_TEX_COLOR_FSH;
            default -> null;
        };
    };

    /**
     * 用两条官方真实管线（GUI、GUI_TEXTURED）验证 P4 编译全链路。每条管线
     * 编译出的原生 Dx12Pipeline* 必须 handle != 0（isValid()==true）。编译失败
     * 时 compilePipeline 返回无效管线并记 error，这里再抛异常终止自检。
     */
    private static void selfTestPipelines(Dx12Device device) {
        String[] names = { "pipeline/gui", "pipeline/gui_textured" };
        for (String name : names) {
            CompiledRenderPipeline pipeline = switch (name) {
                case "pipeline/gui" -> device.precompilePipeline(RenderPipelines.GUI, EMBEDDED_SHADER_SOURCE);
                default -> device.precompilePipeline(RenderPipelines.GUI_TEXTURED, EMBEDDED_SHADER_SOURCE);
            };
            if (!pipeline.isValid()) {
                throw new IllegalStateException("native pipeline compile failed for " + name);
            }
        }
        LOGGER.info("[dx12] Pipeline self-test OK (GLSL->SPIR-V->HLSL->DXBC->PSO for core/gui + core/position_tex_color)");
    }

    /**
     * P5：创建 DXGI swapchain（绑定真实窗口 HWND）→ 配置 → acquire →
     * 用纯红色清空 Backbuffer → submit → present。
     * 首帧会向 MC 真实窗口 present 一次红色，用于验证渲染链路是否打通。
     * 随后回退不影响游戏（游戏有自己的 surface/lifecycle）。
     *
     * 注意：使用 Dx12GpuSurface 而非 GpuSurface 包装类，因为 clearToRed 走
     * 非 blit 路径（null srcTex = ClearRenderTargetView），绕过了 hasBlittedTexture
     * 标志，GpuSurface.present() 会因此抛出 IllegalStateException。
     */
    private static void selfTestSurface(Dx12Device device, long window) throws SurfaceException {
        Dx12CommandEncoderBackend encoderBackend = new Dx12CommandEncoderBackend();
        CommandEncoder encoder = new CommandEncoder(null, device, encoderBackend);
        Dx12GpuSurface dxSurface = null;
        try {
            dxSurface = (Dx12GpuSurface) device.createSurface(window);
            GpuSurface.PresentMode mode = GpuSurface.PresentMode.getSupportedVsyncMode(
                dxSurface.supportedPresentModes(), false);
            dxSurface.configure(new GpuSurface.Configuration(640, 480, mode));
            dxSurface.acquireNextTexture();

            // 纯红色清空 Backbuffer（null texture = 不做 copy，只做 ClearRenderTargetView）
            dxSurface.clearToRed(encoderBackend);

            GpuFence fence = encoder.createFence();
            encoder.submit();
            // 必须等 GPU 完成 clear 后再销毁 swapchain，否则 backbuffer 在被使用时释放。
            if (!fence.awaitCompletion(5000L)) {
                throw new IllegalStateException("surface self-test submit timed out");
            }
            fence.close();
            dxSurface.present();
            System.err.println("[dx12-java] selfTestSurface: presented GREEN to real window — "
                + "if you see green, the render pipeline is working!");
        } finally {
            if (dxSurface != null) {
                dxSurface.close();
            }
            encoderBackend.close();
        }
        LOGGER.info("[dx12] Surface self-test OK (pure green clear + present via JNI on real window)");
    }
}
