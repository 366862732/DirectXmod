package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.PerfDebugHud;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Appends the optional perf counters to the right-hand F3 column.
 *
 * <p>Vanilla builds the left and right line lists in {@code render} and passes
 * them to {@code renderLines}; the boolean is {@code true} for the left
 * column.  Mutating the list at the head of that method is safe because the
 * method only reads it afterwards.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Inject(method = "renderLines", at = @At("HEAD"))
    private void dx11$appendPerfLine(
        GuiGraphics graphics, List<String> lines, boolean leftSide, CallbackInfo ci) {
        if (leftSide) {
            return;
        }
        String line = PerfDebugHud.line();
        if (line != null) {
            lines.add(line);
        }
    }
}
