/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.BossEvent;

@Environment(value=EnvType.CLIENT)
public class BossHealthOverlay {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final Identifier[] BAR_BACKGROUND_SPRITES = new Identifier[]{Identifier.withDefaultNamespace("boss_bar/pink_background"), Identifier.withDefaultNamespace("boss_bar/blue_background"), Identifier.withDefaultNamespace("boss_bar/red_background"), Identifier.withDefaultNamespace("boss_bar/green_background"), Identifier.withDefaultNamespace("boss_bar/yellow_background"), Identifier.withDefaultNamespace("boss_bar/purple_background"), Identifier.withDefaultNamespace("boss_bar/white_background")};
    private static final Identifier[] BAR_PROGRESS_SPRITES = new Identifier[]{Identifier.withDefaultNamespace("boss_bar/pink_progress"), Identifier.withDefaultNamespace("boss_bar/blue_progress"), Identifier.withDefaultNamespace("boss_bar/red_progress"), Identifier.withDefaultNamespace("boss_bar/green_progress"), Identifier.withDefaultNamespace("boss_bar/yellow_progress"), Identifier.withDefaultNamespace("boss_bar/purple_progress"), Identifier.withDefaultNamespace("boss_bar/white_progress")};
    private static final Identifier[] OVERLAY_BACKGROUND_SPRITES = new Identifier[]{Identifier.withDefaultNamespace("boss_bar/notched_6_background"), Identifier.withDefaultNamespace("boss_bar/notched_10_background"), Identifier.withDefaultNamespace("boss_bar/notched_12_background"), Identifier.withDefaultNamespace("boss_bar/notched_20_background")};
    private static final Identifier[] OVERLAY_PROGRESS_SPRITES = new Identifier[]{Identifier.withDefaultNamespace("boss_bar/notched_6_progress"), Identifier.withDefaultNamespace("boss_bar/notched_10_progress"), Identifier.withDefaultNamespace("boss_bar/notched_12_progress"), Identifier.withDefaultNamespace("boss_bar/notched_20_progress")};
    private final Minecraft minecraft;
    private final Map<UUID, LerpingBossEvent> events = Maps.newLinkedHashMap();

    public BossHealthOverlay(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics) {
        if (this.events.isEmpty()) {
            return;
        }
        graphics.nextStratum();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("bossHealth");
        int screenWidth = graphics.guiWidth();
        int yOffset = 12;
        for (LerpingBossEvent event : this.events.values()) {
            int xLeft = screenWidth / 2 - 91;
            int yo = yOffset;
            this.extractBar(graphics, xLeft, yo, event);
            Component msg = event.getName();
            int width = this.minecraft.font.width(msg);
            int x = screenWidth / 2 - width / 2;
            int y = yo - 9;
            graphics.text(this.minecraft.font, msg, x, y, -1);
            if ((yOffset += 10 + this.minecraft.font.lineHeight) < graphics.guiHeight() / 3) continue;
            break;
        }
        profiler.pop();
    }

    private void extractBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent event) {
        this.extractBar(graphics, x, y, event, 182, BAR_BACKGROUND_SPRITES, OVERLAY_BACKGROUND_SPRITES);
        int width = Mth.lerpDiscrete(event.getProgress(), 0, 182);
        if (width > 0) {
            this.extractBar(graphics, x, y, event, width, BAR_PROGRESS_SPRITES, OVERLAY_PROGRESS_SPRITES);
        }
    }

    private void extractBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent event, int width, Identifier[] sprites, Identifier[] overlaySprites) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprites[event.getColor().ordinal()], 182, 5, 0, 0, x, y, width, 5);
        if (event.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, overlaySprites[event.getOverlay().ordinal() - 1], 182, 5, 0, 0, x, y, width, 5);
        }
    }

    public void update(ClientboundBossEventPacket packet) {
        packet.dispatch(new ClientboundBossEventPacket.Handler(this){
            final /* synthetic */ BossHealthOverlay this$0;
            {
                BossHealthOverlay bossHealthOverlay = this$0;
                Objects.requireNonNull(bossHealthOverlay);
                this.this$0 = bossHealthOverlay;
            }

            @Override
            public void add(UUID id, Component name, float progress, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
                this.this$0.events.put(id, new LerpingBossEvent(id, name, progress, color, overlay, darkenScreen, playMusic, createWorldFog));
            }

            @Override
            public void remove(UUID id) {
                this.this$0.events.remove(id);
            }

            @Override
            public void updateProgress(UUID id, float progress) {
                this.this$0.events.get(id).setProgress(progress);
            }

            @Override
            public void updateName(UUID id, Component name) {
                this.this$0.events.get(id).setName(name);
            }

            @Override
            public void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
                LerpingBossEvent event = this.this$0.events.get(id);
                event.setColor(color);
                event.setOverlay(overlay);
            }

            @Override
            public void updateProperties(UUID id, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
                LerpingBossEvent event = this.this$0.events.get(id);
                event.setDarkenScreen(darkenScreen);
                event.setPlayBossMusic(playMusic);
                event.setCreateWorldFog(createWorldFog);
            }
        });
    }

    public void reset() {
        this.events.clear();
    }

    public boolean shouldPlayMusic() {
        if (!this.events.isEmpty()) {
            for (BossEvent bossEvent : this.events.values()) {
                if (!bossEvent.shouldPlayBossMusic()) continue;
                return true;
            }
        }
        return false;
    }

    public boolean shouldDarkenScreen() {
        if (!this.events.isEmpty()) {
            for (BossEvent bossEvent : this.events.values()) {
                if (!bossEvent.shouldDarkenScreen()) continue;
                return true;
            }
        }
        return false;
    }

    public boolean shouldCreateWorldFog() {
        if (!this.events.isEmpty()) {
            for (BossEvent bossEvent : this.events.values()) {
                if (!bossEvent.shouldCreateWorldFog()) continue;
                return true;
            }
        }
        return false;
    }
}

