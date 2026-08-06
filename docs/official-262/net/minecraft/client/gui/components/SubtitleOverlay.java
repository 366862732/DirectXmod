/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.audio.ListenerTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SubtitleOverlay
implements SoundEventListener {
    private static final long DISPLAY_TIME = 3000L;
    private final Minecraft minecraft;
    private final List<Subtitle> subtitles = Lists.newArrayList();
    private boolean isListening;
    private final List<Subtitle> audibleSubtitles = new ArrayList<Subtitle>();

    public SubtitleOverlay(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics) {
        SoundManager soundManager = this.minecraft.getSoundManager();
        if (!this.isListening && this.minecraft.options.showSubtitles().get().booleanValue()) {
            soundManager.addListener(this);
            this.isListening = true;
        } else if (this.isListening && !this.minecraft.options.showSubtitles().get().booleanValue()) {
            soundManager.removeListener(this);
            this.isListening = false;
        }
        if (!this.isListening) {
            return;
        }
        ListenerTransform listener = soundManager.getListenerTransform();
        Vec3 position = listener.position();
        Vec3 forwards = listener.forward();
        Vec3 right = listener.right();
        this.audibleSubtitles.clear();
        for (Subtitle subtitle : this.subtitles) {
            if (!subtitle.isAudibleFrom(position)) continue;
            this.audibleSubtitles.add(subtitle);
        }
        if (this.audibleSubtitles.isEmpty()) {
            return;
        }
        int row = 0;
        int width = 0;
        double displayTimeMultiplier = this.minecraft.options.notificationDisplayTime().get();
        Iterator<Subtitle> iterator = this.audibleSubtitles.iterator();
        while (iterator.hasNext()) {
            Subtitle subtitle = iterator.next();
            subtitle.purgeOldInstances(3000.0 * displayTimeMultiplier);
            if (!subtitle.isStillActive()) {
                iterator.remove();
                continue;
            }
            width = Math.max(width, this.minecraft.font.width(subtitle.getText()));
        }
        width += this.minecraft.font.width("<") + this.minecraft.font.width(" ") + this.minecraft.font.width(">") + this.minecraft.font.width(" ");
        if (!this.audibleSubtitles.isEmpty()) {
            graphics.nextStratum();
        }
        for (Subtitle subtitle : this.audibleSubtitles) {
            int alpha = 255;
            Component text = subtitle.getText();
            SoundPlayedAt closestRecentLocation = subtitle.getClosest(position);
            if (closestRecentLocation == null) continue;
            Vec3 delta = closestRecentLocation.location.subtract(position).normalize();
            double rightness = right.dot(delta);
            double forwardness = forwards.dot(delta);
            boolean inView = forwardness > 0.5;
            int halfWidth = width / 2;
            int height = this.minecraft.font.lineHeight;
            int halfHeight = height / 2;
            float scale = 1.0f;
            int textWidth = this.minecraft.font.width(text);
            int brightness = Mth.floor(Mth.clampedLerp((float)(Util.getMillis() - closestRecentLocation.time) / (float)(3000.0 * displayTimeMultiplier), 255.0f, 75.0f));
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)graphics.guiWidth() - (float)halfWidth * 1.0f - 2.0f, (float)(graphics.guiHeight() - 35) - (float)(row * (height + 1)) * 1.0f);
            graphics.pose().scale(1.0f, 1.0f);
            graphics.fill(-halfWidth - 1, -halfHeight - 1, halfWidth + 1, halfHeight + 1, this.minecraft.options.getBackgroundColor(0.8f));
            int textColor = ARGB.color(255, brightness, brightness, brightness);
            if (!inView) {
                if (rightness > 0.0) {
                    graphics.text(this.minecraft.font, ">", halfWidth - this.minecraft.font.width(">"), -halfHeight, textColor);
                } else if (rightness < 0.0) {
                    graphics.text(this.minecraft.font, "<", -halfWidth, -halfHeight, textColor);
                }
            }
            graphics.text(this.minecraft.font, text, -textWidth / 2, -halfHeight, textColor);
            graphics.pose().popMatrix();
            ++row;
        }
    }

    @Override
    public void onPlaySound(SoundInstance sound, WeighedSoundEvents soundEvent, float range) {
        if (soundEvent.getSubtitle() == null) {
            return;
        }
        Component text = soundEvent.getSubtitle();
        if (!this.subtitles.isEmpty()) {
            for (Subtitle subtitle : this.subtitles) {
                if (!subtitle.getText().equals(text)) continue;
                subtitle.refresh(new Vec3(sound.getX(), sound.getY(), sound.getZ()));
                return;
            }
        }
        this.subtitles.add(new Subtitle(text, range, new Vec3(sound.getX(), sound.getY(), sound.getZ())));
    }

    @Environment(value=EnvType.CLIENT)
    private static class Subtitle {
        private final Component text;
        private final float range;
        private final List<SoundPlayedAt> playedAt = new ArrayList<SoundPlayedAt>();

        public Subtitle(Component text, float range, Vec3 location) {
            this.text = text;
            this.range = range;
            this.playedAt.add(new SoundPlayedAt(location, Util.getMillis()));
        }

        public Component getText() {
            return this.text;
        }

        public @Nullable SoundPlayedAt getClosest(Vec3 position) {
            if (this.playedAt.isEmpty()) {
                return null;
            }
            if (this.playedAt.size() == 1) {
                return this.playedAt.getFirst();
            }
            return this.playedAt.stream().min(Comparator.comparingDouble(soundPlayedAt -> soundPlayedAt.location().distanceTo(position))).orElse(null);
        }

        public void refresh(Vec3 location) {
            this.playedAt.removeIf(soundPlayedAt -> location.equals(soundPlayedAt.location()));
            this.playedAt.add(new SoundPlayedAt(location, Util.getMillis()));
        }

        public boolean isAudibleFrom(Vec3 camera) {
            if (Float.isInfinite(this.range)) {
                return true;
            }
            if (this.playedAt.isEmpty()) {
                return false;
            }
            SoundPlayedAt closest = this.getClosest(camera);
            if (closest == null) {
                return false;
            }
            return camera.closerThan(closest.location, this.range);
        }

        public void purgeOldInstances(double maxAge) {
            long currentTime = Util.getMillis();
            this.playedAt.removeIf(soundPlayedAt -> (double)(currentTime - soundPlayedAt.time()) > maxAge);
        }

        public boolean isStillActive() {
            return !this.playedAt.isEmpty();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record SoundPlayedAt(Vec3 location, long time) {
    }
}

