/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.toasts;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface Toast {
    public static final Object NO_TOKEN = new Object();
    public static final int DEFAULT_WIDTH = 160;
    public static final int SLOT_HEIGHT = 32;

    public Visibility getWantedVisibility();

    public void update(ToastManager var1, long var2);

    default public @Nullable SoundEvent getSoundEvent() {
        return null;
    }

    public void extractRenderState(GuiGraphicsExtractor var1, Font var2, long var3);

    default public Object getToken() {
        return NO_TOKEN;
    }

    default public float xPos(int screenWidth, float visiblePortion) {
        return (float)screenWidth - (float)this.width() * visiblePortion;
    }

    default public float yPos(int firstSlotIndex) {
        return firstSlotIndex * this.height();
    }

    default public int width() {
        return 160;
    }

    default public int height() {
        return 32;
    }

    default public int occcupiedSlotCount() {
        return Mth.positiveCeilDiv(this.height(), 32);
    }

    default public void onFinishedRendering() {
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Visibility {
        SHOW(SoundEvents.UI_TOAST_IN),
        HIDE(SoundEvents.UI_TOAST_OUT);

        private final SoundEvent soundEvent;

        private Visibility(SoundEvent soundEvent) {
            this.soundEvent = soundEvent;
        }

        public void playSound(SoundManager manager) {
            manager.play(SimpleSoundInstance.forUI(this.soundEvent, 1.0f, 1.0f));
        }
    }
}

