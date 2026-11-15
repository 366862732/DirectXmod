package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.ImmediateStats;
import com.xgdt.dx11.perf.immediate.SpatialArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ScreenArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers the GUI layer overlap question from the spatial index.
 *
 * <p>Vanilla walks every element of a layer for every navigation step.  When
 * the layer uses the indexed list and the index is current, the aggregate
 * bounds plus the coarse grid answer the same question without touching all
 * entries.  If the list was structurally modified in a way the index does not
 * track, this returns without cancelling and vanilla still runs.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
    @Inject(method = "hasIntersection", at = @At("HEAD"), cancellable = true)
    private void dx11$indexedIntersection(
        ScreenRectangle rectangle,
        List<? extends ScreenArea> screenAreas,
        CallbackInfoReturnable<Boolean> cir) {
        if (screenAreas instanceof SpatialArrayList<?> indexed) {
            if (indexed.isIndexValid()) {
                ImmediateStats.onGuiQuery(true);
                cir.setReturnValue(indexed.intersects(rectangle));
            } else {
                ImmediateStats.onGuiQuery(false);
            }
        }
    }
}
