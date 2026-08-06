/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MusicToastDisplayState;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ToastManager {
    private static final int SLOT_COUNT = 5;
    private static final int ALL_SLOTS_OCCUPIED = -1;
    private final Minecraft minecraft;
    private final List<ToastInstance<?>> visibleToasts = new ArrayList();
    private final BitSet occupiedSlots = new BitSet(5);
    private final Deque<Toast> queued = Queues.newArrayDeque();
    private final Set<SoundEvent> playedToastSounds = new HashSet<SoundEvent>();
    private @Nullable ToastInstance<NowPlayingToast> nowPlayingToast;

    public ToastManager(Minecraft minecraft, Options options) {
        this.minecraft = minecraft;
        this.initializeMusicToast(options.musicToast().get());
    }

    public void update() {
        MutableBoolean visibilityChangeSoundPlayed = new MutableBoolean(false);
        this.visibleToasts.removeIf(toast -> {
            Toast.Visibility previousVisibility = toast.visibility;
            toast.update();
            if (toast.visibility != previousVisibility && visibilityChangeSoundPlayed.isFalse()) {
                visibilityChangeSoundPlayed.setTrue();
                toast.visibility.playSound(this.minecraft.getSoundManager());
            }
            if (toast.hasFinishedRendering()) {
                this.occupiedSlots.clear(toast.firstSlotIndex, toast.firstSlotIndex + toast.occupiedSlotCount);
                return true;
            }
            return false;
        });
        if (!this.queued.isEmpty() && this.freeSlotCount() > 0) {
            this.queued.removeIf(toast -> {
                int occcupiedSlotCount = toast.occcupiedSlotCount();
                int firstSlotIndex = this.findFreeSlotsIndex(occcupiedSlotCount);
                if (firstSlotIndex == -1) {
                    return false;
                }
                this.visibleToasts.add(new ToastInstance(this, toast, firstSlotIndex, occcupiedSlotCount));
                this.occupiedSlots.set(firstSlotIndex, firstSlotIndex + occcupiedSlotCount);
                SoundEvent toastSound = toast.getSoundEvent();
                if (toastSound != null && this.playedToastSounds.add(toastSound)) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(toastSound, 1.0f, 1.0f));
                }
                return true;
            });
        }
        this.playedToastSounds.clear();
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.update();
        }
    }

    public void extractRenderState(GuiGraphicsExtractor graphics) {
        if (this.minecraft.gui.hud.isHidden()) {
            return;
        }
        int screenWidth = graphics.guiWidth();
        if (!this.visibleToasts.isEmpty()) {
            graphics.nextStratum();
        }
        for (ToastInstance<?> toast : this.visibleToasts) {
            toast.extractRenderState(graphics, screenWidth);
        }
        if (this.minecraft.options.musicToast().get().renderToast() && this.nowPlayingToast != null && (this.minecraft.gui.screen() == null || !(this.minecraft.gui.screen() instanceof PauseScreen))) {
            this.nowPlayingToast.extractRenderState(graphics, screenWidth);
        }
    }

    private int findFreeSlotsIndex(int requiredCount) {
        if (this.freeSlotCount() >= requiredCount) {
            int consecutiveFreeSlotCount = 0;
            for (int i = 0; i < 5; ++i) {
                if (this.occupiedSlots.get(i)) {
                    consecutiveFreeSlotCount = 0;
                    continue;
                }
                if (++consecutiveFreeSlotCount != requiredCount) continue;
                return i + 1 - consecutiveFreeSlotCount;
            }
        }
        return -1;
    }

    private int freeSlotCount() {
        return 5 - this.occupiedSlots.cardinality();
    }

    public <T extends Toast> @Nullable T getToast(Class<? extends T> clazz, Object token) {
        for (ToastInstance<?> instance : this.visibleToasts) {
            if (!clazz.isAssignableFrom(instance.getToast().getClass()) || !instance.getToast().getToken().equals(token)) continue;
            return (T)instance.getToast();
        }
        for (Toast toast : this.queued) {
            if (!clazz.isAssignableFrom(toast.getClass()) || !toast.getToken().equals(token)) continue;
            return (T)toast;
        }
        return null;
    }

    public void clear() {
        this.occupiedSlots.clear();
        this.visibleToasts.clear();
        this.queued.clear();
    }

    public void addToast(Toast toast) {
        this.queued.add(toast);
    }

    public void showNowPlayingToast() {
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.resetToast();
            this.nowPlayingToast.getToast().showToast(this.minecraft.options);
        }
    }

    public void hideNowPlayingToast() {
        if (this.nowPlayingToast != null) {
            this.nowPlayingToast.getToast().setWantedVisibility(Toast.Visibility.HIDE);
        }
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public double getNotificationDisplayTimeMultiplier() {
        return this.minecraft.options.notificationDisplayTime().get();
    }

    private void initializeMusicToast(MusicToastDisplayState state) {
        switch (state) {
            case PAUSE: 
            case PAUSE_AND_TOAST: {
                this.nowPlayingToast = new ToastInstance(this, (Toast)new NowPlayingToast(), 0, 0);
            }
        }
    }

    public void setMusicToastDisplayState(MusicToastDisplayState state) {
        switch (state) {
            case NEVER: {
                this.nowPlayingToast = null;
                break;
            }
            case PAUSE: {
                this.nowPlayingToast = new ToastInstance(this, (Toast)new NowPlayingToast(), 0, 0);
                break;
            }
            case PAUSE_AND_TOAST: {
                this.nowPlayingToast = new ToastInstance(this, (Toast)new NowPlayingToast(), 0, 0);
                if (!(this.minecraft.options.getFinalSoundSourceVolume(SoundSource.MUSIC) > 0.0f)) break;
                this.nowPlayingToast.getToast().showToast(this.minecraft.options);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class ToastInstance<T extends Toast> {
        private static final long SLIDE_ANIMATION_DURATION_MS = 600L;
        private final T toast;
        private final int firstSlotIndex;
        private final int occupiedSlotCount;
        private long animationStartTime;
        private long becameFullyVisibleAt;
        private Toast.Visibility visibility;
        private long fullyVisibleFor;
        private float visiblePortion;
        protected boolean hasFinishedRendering;
        final /* synthetic */ ToastManager this$0;

        /*
         * WARNING - Possible parameter corruption
         */
        private ToastInstance(T toast, int firstSlotIndex, int occupiedSlotCount) {
            int n2 = n;
            Objects.requireNonNull(n2);
            this.this$0 = (ToastManager)n2;
            this.toast = toast;
            this.firstSlotIndex = firstSlotIndex;
            this.occupiedSlotCount = occupiedSlotCount;
            this.resetToast();
        }

        public T getToast() {
            return this.toast;
        }

        public void resetToast() {
            this.animationStartTime = -1L;
            this.becameFullyVisibleAt = -1L;
            this.visibility = Toast.Visibility.HIDE;
            this.fullyVisibleFor = 0L;
            this.visiblePortion = 0.0f;
            this.hasFinishedRendering = false;
        }

        public boolean hasFinishedRendering() {
            return this.hasFinishedRendering;
        }

        private void calculateVisiblePortion(long now) {
            float animationProgress = Mth.clamp((float)(now - this.animationStartTime) / 600.0f, 0.0f, 1.0f);
            animationProgress *= animationProgress;
            this.visiblePortion = this.visibility == Toast.Visibility.HIDE ? 1.0f - animationProgress : animationProgress;
        }

        public void update() {
            long now = Util.getMillis();
            if (this.animationStartTime == -1L) {
                this.animationStartTime = now;
                this.visibility = Toast.Visibility.SHOW;
            }
            if (this.visibility == Toast.Visibility.SHOW && now - this.animationStartTime <= 600L) {
                this.becameFullyVisibleAt = now;
            }
            this.fullyVisibleFor = now - this.becameFullyVisibleAt;
            this.calculateVisiblePortion(now);
            this.toast.update(this.this$0, this.fullyVisibleFor);
            Toast.Visibility wantedVisibility = this.toast.getWantedVisibility();
            if (wantedVisibility != this.visibility) {
                this.animationStartTime = now - (long)((int)((1.0f - this.visiblePortion) * 600.0f));
                this.visibility = wantedVisibility;
            }
            boolean wasAlreadyFinishedRendering = this.hasFinishedRendering;
            boolean bl = this.hasFinishedRendering = this.visibility == Toast.Visibility.HIDE && now - this.animationStartTime > 600L;
            if (this.hasFinishedRendering && !wasAlreadyFinishedRendering) {
                this.toast.onFinishedRendering();
            }
        }

        public void extractRenderState(GuiGraphicsExtractor graphics, int screenWidth) {
            if (this.hasFinishedRendering) {
                return;
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.toast.xPos(screenWidth, this.visiblePortion), this.toast.yPos(this.firstSlotIndex));
            this.toast.extractRenderState(graphics, this.this$0.minecraft.font, this.fullyVisibleFor);
            graphics.pose().popMatrix();
        }
    }
}

