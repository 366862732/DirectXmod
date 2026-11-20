package com.xgdt.dx11.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the two renderer hooks vanilla uses for entity occlusion.
 *
 * <p>{@code EntityRenderer.getBoundingBoxForCulling} and
 * {@code affectedByCulling} are protected, but they are what vanilla's own
 * frustum test consults.  Reusing them keeps this mod's section-based test
 * consistent with vanilla for multi-part entities (which have a culling box
 * larger than the entity box) and for renderers that opt out of culling.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("affectedByCulling")
    boolean dx11$affectedByCulling(Entity entity);

    @Invoker("getBoundingBoxForCulling")
    AABB dx11$getBoundingBoxForCulling(Entity entity);
}
