/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Tooltip
implements NarrationSupplier {
    private static final int MAX_WIDTH = 170;
    private final Component message;
    private @Nullable List<FormattedCharSequence> cachedTooltip;
    private @Nullable Language splitWithLanguage;
    private final @Nullable Identifier style;
    private final @Nullable Component narration;
    private final Optional<TooltipComponent> component;

    private Tooltip(Component message, @Nullable Component narration, Optional<TooltipComponent> component, @Nullable Identifier style) {
        this.message = message;
        this.narration = narration;
        this.component = component;
        this.style = style;
    }

    public static Tooltip create(Component message) {
        return new Tooltip(message, message, Optional.empty(), null);
    }

    public static Tooltip create(Component message, @Nullable Component narration) {
        return new Tooltip(message, narration, Optional.empty(), null);
    }

    public static Tooltip create(Component message, Optional<TooltipComponent> component, @Nullable Identifier style) {
        return new Tooltip(message, message, component, style);
    }

    public Optional<TooltipComponent> component() {
        return this.component;
    }

    public @Nullable Identifier style() {
        return this.style;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        if (this.narration != null) {
            output.add(NarratedElementType.HINT, this.narration);
        }
    }

    public List<FormattedCharSequence> toCharSequence(Minecraft minecraft) {
        Language currentLanguage = Language.getInstance();
        if (this.cachedTooltip == null || currentLanguage != this.splitWithLanguage) {
            this.cachedTooltip = Tooltip.splitTooltip(minecraft, this.message);
            this.splitWithLanguage = currentLanguage;
        }
        return this.cachedTooltip;
    }

    public static List<FormattedCharSequence> splitTooltip(Minecraft minecraft, Component message) {
        return minecraft.font.split(message, 170);
    }
}

