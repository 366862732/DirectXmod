package com.xgdt.dx11.mixin.immediate;

import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the texture bindings of a render setup.  The binding map itself is
 * package-private, so the values are read through
 * {@link RenderSetupTextureBindingAccessor}.
 */
@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("textures")
    Map<String, ?> dx11$textures();
}
