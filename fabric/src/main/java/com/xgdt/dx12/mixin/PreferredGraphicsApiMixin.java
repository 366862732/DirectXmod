package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Backend;
import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.vulkan.VulkanBackend;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects the DX12 backend into the vanilla backend-selection loop.
 *
 * Instead of adding a new enum constant (brittle — enum fields are ordered and
 * the constant pool layout must match the runtime), this only rewrites the
 * array returned by {@code getBackendsToTry()}, so Minecraft's existing
 * try-each-backend loop picks DX12 up automatically:
 *
 * <pre>
 *   for (GpuBackend backend : preferredGraphicsBackend.getBackendsToTry()) {
 *       try { ... createDevice ... break; }
 *       catch (BackendCreationException) { try the next backend }
 *   }
 * </pre>
 *
 * Ordering: DEFAULT/OPENGL try DX12 first, then fall back to GL then Vulkan.
 * Explicit VULKAN keeps Vulkan first, DX12 second, GL last.
 */
@Mixin(PreferredGraphicsApi.class)
public abstract class PreferredGraphicsApiMixin {
    @Inject(method = "getBackendsToTry", at = @At("RETURN"), cancellable = true)
    private void gl4dx12$injectDx12Backend(CallbackInfoReturnable<GpuBackend[]> cir) {
        GlBackend gl = new GlBackend();
        VulkanBackend vulkan = new VulkanBackend();
        Dx12Backend dx12 = new Dx12Backend();
        if (((PreferredGraphicsApi) (Object) this) == PreferredGraphicsApi.VULKAN) {
            cir.setReturnValue(new GpuBackend[] { vulkan, dx12, gl });
        } else {
            cir.setReturnValue(new GpuBackend[] { dx12, gl, vulkan });
        }
    }
}
