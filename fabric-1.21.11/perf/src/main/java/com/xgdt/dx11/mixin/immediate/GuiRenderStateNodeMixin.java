package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import com.xgdt.dx11.perf.immediate.SpatialArrayList;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Creates indexed lists for the four per-layer lists that participate in GUI
 * overlap tests.  The glyph list is deliberately left as a plain
 * {@link ArrayList}; {@code GuiRenderState} never tests glyph bounds, so
 * indexing it would only add work.
 */
@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.render.state.GuiRenderState$Node")
public abstract class GuiRenderStateNodeMixin {
    @WrapOperation(
        method = {
            "submitItem",
            "submitText",
            "submitPicturesInPictureState",
            "submitGuiElement"
        },
        at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"))
    private ArrayList<?> dx11$spatialList(Operation<ArrayList<?>> original) {
        return ImmediateFlags.guiLayerLookup() ? new SpatialArrayList<>() : original.call();
    }
}
