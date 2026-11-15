package com.xgdt.dx11.mixin.immediate;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes render-type internals used for deterministic batch ordering. */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Accessor("name")
    String dx11$name();

    @Accessor("state")
    RenderSetup dx11$state();
}
