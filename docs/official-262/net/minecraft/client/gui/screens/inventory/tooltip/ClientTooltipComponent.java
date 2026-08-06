/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.inventory.tooltip;

import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

@Environment(value=EnvType.CLIENT)
public interface ClientTooltipComponent {
    public static ClientTooltipComponent create(FormattedCharSequence charSequence) {
        return new ClientTextTooltip(charSequence);
    }

    public static ClientTooltipComponent create(TooltipComponent component) {
        TooltipComponent tooltipComponent = component;
        Objects.requireNonNull(tooltipComponent);
        TooltipComponent tooltipComponent2 = tooltipComponent;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{BundleTooltip.class, ClientActivePlayersTooltip.ActivePlayersTooltip.class}, (TooltipComponent)tooltipComponent2, n)) {
            case 0 -> {
                BundleTooltip bundleTooltip = (BundleTooltip)tooltipComponent2;
                yield new ClientBundleTooltip(bundleTooltip.contents());
            }
            case 1 -> {
                ClientActivePlayersTooltip.ActivePlayersTooltip activePlayersTooltip = (ClientActivePlayersTooltip.ActivePlayersTooltip)tooltipComponent2;
                yield new ClientActivePlayersTooltip(activePlayersTooltip);
            }
            default -> throw new IllegalArgumentException("Unknown TooltipComponent");
        };
    }

    public int getHeight(Font var1);

    public int getWidth(Font var1);

    default public boolean showTooltipWithItemInHand() {
        return false;
    }

    default public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
    }

    default public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
    }
}

