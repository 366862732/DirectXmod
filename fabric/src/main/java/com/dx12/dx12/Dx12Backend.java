package com.dx12.dx12;

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
            return new GpuDevice(device, criticalShaderLoader);
        } catch (Throwable t) {
            LOGGER.error("[dx12] D3D12 self-test failed", t);
            throw new BackendCreationException(
                "DX12 self-test failed: " + t, BackendCreationException.Reason.OTHER);
        }
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
            GpuBuffer.USAGE_COPY_DST, size);
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
                for (int i = 0; i < size; ++i) {
                    if ((read.get(i) & 0xFF) != (i & 0xFF)) {
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
     */
    private static void selfTestSurface(Dx12Device device, long window) throws SurfaceException {
        Dx12CommandEncoderBackend encoderBackend = new Dx12CommandEncoderBackend();
        CommandEncoder encoder = new CommandEncoder(null, device, encoderBackend);
        GpuSurface surface = null;
        try {
            surface = new GpuSurface(device.createSurface(window));
            GpuSurface.PresentMode mode = GpuSurface.PresentMode.getSupportedVsyncMode(
                surface.supportedPresentModes(), false);
            surface.configure(new GpuSurface.Configuration(640, 480, mode));
            surface.acquireNextTexture();

            // 纯红色清空 Backbuffer（null texture = 不做 copy，只做 ClearRenderTargetView）
            try {
                java.lang.reflect.Field f = GpuSurface.class.getDeclaredField("backend");
                f.setAccessible(true);
                ((Dx12GpuSurface) f.get(surface)).clearToRed(encoderBackend);
            } catch (ReflectiveOperationException e) {
                throw new SurfaceException("cannot access Dx12GpuSurface backend: " + e.getMessage());
            }

            GpuFence fence = encoder.createFence();
            encoder.submit();
            // 必须等 GPU 完成 clear 后再销毁 swapchain，否则 backbuffer 在被使用时释放。
            if (!fence.awaitCompletion(5000L)) {
                throw new IllegalStateException("surface self-test submit timed out");
            }
            fence.close();
            surface.present();
            System.err.println("[dx12-java] selfTestSurface: presented RED to real window — "
                + "if you see red, the render pipeline is working!");
        } finally {
            if (surface != null) {
                surface.close();
            }
            encoderBackend.close();
        }
        LOGGER.info("[dx12] Surface self-test OK (pure red clear + present via JNI on real window)");
    }
}
