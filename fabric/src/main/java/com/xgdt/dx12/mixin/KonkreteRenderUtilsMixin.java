package com.xgdt.dx12.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FancyMenu / Konkrete 后端判定兼容（DX12 视作现代 blaze3d 后端）。
 *
 * <p>Konkrete 的 {@code RenderUtils.isVulkanActive()} 实现是
 * {@code "Vulkan".equals(RenderSystem.getDevice().getDeviceInfo().backendName())}，
 * 用来判断"是否处于非 OpenGL 的现代渲染后端"。DX12 后端名是 {@code "DX12"}，
 * 因此会被误判为 OpenGL，进而让 FancyMenu 走 OpenGL 专用分支：
 * <ul>
 *   <li>{@code RenderingUtils.restoreRawRenderStateDefaults()} 裸调
 *       {@code GL33C.glColorMaski/glDepthMask/glEnable}；</li>
 *   <li>{@code GuiBlurRenderer} 改用自建 SCREEN_COPY pass 而不是
 *       {@code copyTextureToTexture}；</li>
 *   <li>{@code GlslShaderRuntime}/{@code GlslShaderSourceTransformer} 使用
 *       OPENGL 坐标约定（D3D12 的 fragCoord/UV 原点与 Vulkan 一致，在左上）。</li>
 * </ul>
 *
 * <p>这里只在 Konkrete 这一层报告 true，不改 {@code DeviceInfo.backendName}，
 * 避免影响其它读取真实后端名的 mod。{@code @Pseudo} 保证未安装
 * FancyMenu/Konkrete 时整个 mixin 被静默跳过。
 */
@Pseudo
@Mixin(targets = "de.keksuccino.konkrete.rendering.RenderUtils")
public abstract class KonkreteRenderUtilsMixin {
    @Inject(method = "isVulkanActive", at = @At("HEAD"), cancellable = true, require = 0)
    private static void gl4dx12$reportModernBackend(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
