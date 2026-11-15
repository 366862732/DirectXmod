package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.culling.EntityCulling;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds occlusion-aware entity culling to vanilla's per-entity visibility test.
 *
 * <p>{@code LevelRenderer.extractVisibleEntities} calls {@code shouldRender}
 * for every entity before the section fade/compiled check.  Returning false
 * here removes entities whose entire bounding box lies in sections the section
 * occlusion graph did not mark visible.  Vanilla's later compiled-section check
 * and the camera/passenger exemptions are left untouched.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void dx11$cullOccludedEntity(
        Entity entity,
        Frustum frustum,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfoReturnable<Boolean> cir) {
        if (EntityCulling.shouldCull(entity)) {
            cir.setReturnValue(false);
        }
    }
}
