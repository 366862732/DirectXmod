/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LevelLoadingScreen
extends Screen {
    private static final Component DOWNLOADING_TERRAIN_TEXT = Component.translatable("multiplayer.downloadingTerrain");
    private static final Component READY_TO_PLAY_TEXT = Component.translatable("narrator.ready_to_play");
    private static final long NARRATION_DELAY_MS = 2000L;
    private static final int PROGRESS_BAR_WIDTH = 200;
    private LevelLoadTracker loadTracker;
    private float smoothedProgress;
    private long lastNarration = -1L;
    private Reason reason;
    private @Nullable TextureAtlasSprite cachedNetherPortalSprite;
    private static final Object2IntMap<ChunkStatus> COLORS = (Object2IntMap)Util.make(new Object2IntOpenHashMap(), map -> {
        map.defaultReturnValue(0);
        map.put((Object)ChunkStatus.EMPTY, 0x545454);
        map.put((Object)ChunkStatus.STRUCTURE_STARTS, 0x999999);
        map.put((Object)ChunkStatus.STRUCTURE_REFERENCES, 6250897);
        map.put((Object)ChunkStatus.BIOMES, 8434258);
        map.put((Object)ChunkStatus.NOISE, 0xD1D1D1);
        map.put((Object)ChunkStatus.SURFACE, 7497737);
        map.put((Object)ChunkStatus.CARVERS, 3159410);
        map.put((Object)ChunkStatus.FEATURES, 2213376);
        map.put((Object)ChunkStatus.INITIALIZE_LIGHT, 0xCCCCCC);
        map.put((Object)ChunkStatus.LIGHT, 16769184);
        map.put((Object)ChunkStatus.SPAWN, 15884384);
        map.put((Object)ChunkStatus.FULL, 0xFFFFFF);
    });

    public LevelLoadingScreen(LevelLoadTracker loadTracker, Reason reason) {
        super(GameNarrator.NO_TITLE);
        this.loadTracker = loadTracker;
        this.reason = reason;
    }

    public void update(LevelLoadTracker loadTracker, Reason reason) {
        this.loadTracker = loadTracker;
        this.reason = reason;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    @Override
    protected void updateNarratedWidget(NarrationElementOutput output) {
        if (this.loadTracker.hasProgress()) {
            output.add(NarratedElementType.TITLE, (Component)Component.translatable("loading.progress", Mth.floor(this.loadTracker.serverProgress() * 100.0f)));
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.smoothedProgress += (this.loadTracker.serverProgress() - this.smoothedProgress) * 0.2f;
        if (this.loadTracker.isLevelReady()) {
            this.onClose();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int textTop;
        super.extractRenderState(graphics, mouseX, mouseY, a);
        long current = Util.getMillis();
        if (current - this.lastNarration > 2000L) {
            this.lastNarration = current;
            this.triggerImmediateNarration(true);
        }
        int xCenter = this.width / 2;
        int yCenter = this.height / 2;
        ChunkLoadStatusView statusView = this.loadTracker.statusView();
        if (statusView != null) {
            int size = 2;
            LevelLoadingScreen.extractChunksForRendering(graphics, xCenter, yCenter, 2, 0, statusView);
            textTop = yCenter - statusView.radius() * 2 - this.font.lineHeight * 3;
        } else {
            textTop = yCenter - 50;
        }
        graphics.centeredText(this.font, DOWNLOADING_TERRAIN_TEXT, xCenter, textTop, -1);
        if (this.loadTracker.hasProgress()) {
            this.drawProgressBar(graphics, xCenter - 100, textTop + this.font.lineHeight + 3, 200, 2, this.smoothedProgress);
        }
    }

    private void drawProgressBar(GuiGraphicsExtractor graphics, int left, int top, int width, int height, float progress) {
        graphics.fill(left, top, left + width, top + height, -16777216);
        graphics.fill(left, top, left + Math.round(progress * (float)width), top + height, -16711936);
    }

    public static void extractChunksForRendering(GuiGraphicsExtractor graphics, int xCenter, int yCenter, int size, int margin, ChunkLoadStatusView statusView) {
        int width = size + margin;
        int diameter = statusView.radius() * 2 + 1;
        int totalWidth = diameter * width - margin;
        int xStart = xCenter - totalWidth / 2;
        int yStart = yCenter - totalWidth / 2;
        if (Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
            int centerWidth = width / 2 + 1;
            graphics.fill(xCenter - centerWidth, yCenter - centerWidth, xCenter + centerWidth, yCenter + centerWidth, -65536);
        }
        for (int x = 0; x < diameter; ++x) {
            for (int z = 0; z < diameter; ++z) {
                ChunkStatus status = statusView.get(x, z);
                int xCellStart = xStart + x * width;
                int yCellStart = yStart + z * width;
                graphics.fill(xCellStart, yCellStart, xCellStart + size, yCellStart + size, ARGB.opaque(COLORS.getInt((Object)status)));
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        switch (this.reason.ordinal()) {
            case 2: {
                this.extractPanorama(graphics, a);
                this.extractBlurredBackground(graphics);
                this.extractMenuBackground(graphics);
                break;
            }
            case 0: {
                graphics.blitSprite(RenderPipelines.GUI_OPAQUE_TEXTURED_BACKGROUND, this.getNetherPortalSprite(), 0, 0, graphics.guiWidth(), graphics.guiHeight());
                break;
            }
            case 1: {
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                AbstractTexture skyTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
                AbstractTexture portalTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
                TextureSetup textureSetup = TextureSetup.doubleTexture(skyTexture.getTextureView(), skyTexture.getSampler(), portalTexture.getTextureView(), portalTexture.getSampler());
                graphics.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, this.width, this.height);
            }
        }
    }

    private TextureAtlasSprite getNetherPortalSprite() {
        if (this.cachedNetherPortalSprite != null) {
            return this.cachedNetherPortalSprite;
        }
        this.cachedNetherPortalSprite = this.minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(Blocks.NETHER_PORTAL.defaultBlockState()).sprite();
        return this.cachedNetherPortalSprite;
    }

    @Override
    public void onClose() {
        this.minecraft.getNarrator().saySystemNow(READY_TO_PLAY_TEXT);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Reason {
        NETHER_PORTAL,
        END_PORTAL,
        OTHER;

    }
}

