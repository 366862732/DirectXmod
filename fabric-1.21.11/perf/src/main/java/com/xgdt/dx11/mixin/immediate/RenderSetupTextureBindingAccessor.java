package com.xgdt.dx11.mixin.immediate;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the texture location of a render setup's binding record. */
@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface RenderSetupTextureBindingAccessor {
    @Accessor("location")
    Identifier dx11$location();
}
