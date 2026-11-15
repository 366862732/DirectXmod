package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Skips depth sorting for the translucent/see-through text sheets.
 *
 * <p>Glyph quads never overlap within a sheet and carry no depth writes, so
 * sorting them by view depth only costs a full quad sort on upload.  The three
 * affected render types are the polygon-offset (nametag) and see-through text
 * variants; regular opaque text is not sorted by vanilla anyway.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(RenderTypes.class)
public abstract class RenderTypesTextMixin {
    @WrapOperation(
        method = {
            "method_75949",
            "method_75948",
            "method_75946"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderSetup$RenderSetupBuilder;sortOnUpload()Lnet/minecraft/client/renderer/rendertype/RenderSetup$RenderSetupBuilder;"))
    private static RenderSetup.RenderSetupBuilder dx11$skipTextSort(
        RenderSetup.RenderSetupBuilder builder, Operation<RenderSetup.RenderSetupBuilder> original) {
        return ImmediateFlags.skipTextSorting() ? builder : original.call(builder);
    }
}
