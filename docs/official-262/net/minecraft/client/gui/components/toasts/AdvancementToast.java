/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.toasts;

import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class AdvancementToast
implements Toast {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    public static final int DISPLAY_TIME = 5000;
    private final AdvancementHolder advancement;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
    private final ItemStack iconItem;

    public AdvancementToast(AdvancementHolder advancement) {
        this.advancement = advancement;
        this.iconItem = advancement.value().display().map(d -> d.getIcon().create()).orElse(ItemStack.EMPTY);
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        DisplayInfo display = this.advancement.value().display().orElse(null);
        if (display == null) {
            this.wantedVisibility = Toast.Visibility.HIDE;
            return;
        }
        this.wantedVisibility = (double)fullyVisibleForMs >= 5000.0 * manager.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }

    @Override
    public @Nullable SoundEvent getSoundEvent() {
        return this.isChallengeAdvancement() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : null;
    }

    private boolean isChallengeAdvancement() {
        Optional<DisplayInfo> displayInfo = this.advancement.value().display();
        return displayInfo.isPresent() && displayInfo.get().getType().equals(AdvancementType.CHALLENGE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        int titleColor;
        DisplayInfo display = this.advancement.value().display().orElse(null);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        if (display == null) {
            return;
        }
        List<FormattedCharSequence> lines = font.split(display.getTitle(), 125);
        int n = titleColor = display.getType() == AdvancementType.CHALLENGE ? -30465 : -256;
        if (lines.size() == 1) {
            graphics.text(font, display.getType().getDisplayName(), 30, 7, titleColor, false);
            graphics.text(font, lines.get(0), 30, 18, -1, false);
        } else {
            int unlockTextTime = 1500;
            float unlockFadeTime = 300.0f;
            if (fullyVisibleForMs < 1500L) {
                int alpha = Mth.floor(Mth.clamp((float)(1500L - fullyVisibleForMs) / 300.0f, 0.0f, 1.0f) * 255.0f);
                graphics.text(font, display.getType().getDisplayName(), 30, 11, ARGB.color(alpha, titleColor), false);
            } else {
                int alpha = Mth.floor(Mth.clamp((float)(fullyVisibleForMs - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f);
                int y = this.height() / 2 - lines.size() * font.lineHeight / 2;
                for (FormattedCharSequence line : lines) {
                    graphics.text(font, line, 30, y, ARGB.white(alpha), false);
                    y += font.lineHeight;
                }
            }
        }
        graphics.fakeItem(this.iconItem, 8, 8);
    }
}

