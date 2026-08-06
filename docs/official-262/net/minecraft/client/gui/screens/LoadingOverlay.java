/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class LoadingOverlay
extends Overlay {
    public static final Identifier MOJANG_STUDIOS_LOGO_LOCATION = Identifier.withDefaultNamespace("textures/gui/title/mojangstudios.png");
    private static final int LOGO_BACKGROUND_COLOR = ARGB.color(255, 239, 50, 61);
    private static final int LOGO_BACKGROUND_COLOR_DARK = ARGB.color(255, 0, 0, 0);
    private static final IntSupplier BRAND_BACKGROUND = () -> Minecraft.getInstance().options.darkMojangStudiosBackground().get() != false ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;
    private static final int LOGO_SCALE = 240;
    private static final float LOGO_QUARTER_FLOAT = 60.0f;
    private static final int LOGO_QUARTER = 60;
    private static final int LOGO_HALF = 120;
    private static final float LOGO_OVERLAP = 0.0625f;
    private static final float SMOOTHING = 0.95f;
    public static final long FADE_OUT_TIME = 1000L;
    public static final long FADE_IN_TIME = 500L;
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean fadeIn;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;

    public LoadingOverlay(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn) {
        this.minecraft = minecraft;
        this.reload = reload;
        this.onFinish = onFinish;
        this.fadeIn = fadeIn;
    }

    public static void registerTextures(TextureManager textureManager) {
        textureManager.registerAndLoad(MOJANG_STUDIOS_LOGO_LOCATION, new LogoTexture());
    }

    private static int replaceAlpha(int color, int alpha) {
        return color & 0xFFFFFF | alpha << 24;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        float logoAlpha;
        float fadeInAnim;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }
        float fadeOutAnim = this.fadeOutStart > -1L ? (float)(now - this.fadeOutStart) / 1000.0f : -1.0f;
        float f = fadeInAnim = this.fadeInStart > -1L ? (float)(now - this.fadeInStart) / 500.0f : -1.0f;
        if (fadeOutAnim >= 1.0f) {
            if (this.minecraft.gui.screen() != null) {
                this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, 0, 0, a);
            } else {
                this.minecraft.gui.hud.extractDeferredSubtitles();
            }
            alpha = Mth.ceil((1.0f - Mth.clamp(fadeOutAnim - 1.0f, 0.0f, 1.0f)) * 255.0f);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, LoadingOverlay.replaceAlpha(BRAND_BACKGROUND.getAsInt(), alpha));
            logoAlpha = 1.0f - Mth.clamp(fadeOutAnim - 1.0f, 0.0f, 1.0f);
        } else if (this.fadeIn) {
            if (this.minecraft.gui.screen() != null && fadeInAnim < 1.0f) {
                this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a);
            } else {
                this.minecraft.gui.hud.extractDeferredSubtitles();
            }
            alpha = Mth.ceil(Mth.clamp((double)fadeInAnim, 0.15, 1.0) * 255.0);
            graphics.nextStratum();
            graphics.fill(0, 0, width, height, LoadingOverlay.replaceAlpha(BRAND_BACKGROUND.getAsInt(), alpha));
            logoAlpha = Mth.clamp(fadeInAnim, 0.0f, 1.0f);
        } else {
            ARGB.setVector4fFromARGB32(this.minecraft.gameRenderer.gameRenderState().guiRenderState.clearColorOverride, BRAND_BACKGROUND.getAsInt());
            logoAlpha = 1.0f;
        }
        int contentX = (int)((double)graphics.guiWidth() * 0.5);
        int logoY = (int)((double)graphics.guiHeight() * 0.5);
        double logoHeight = Math.min((double)graphics.guiWidth() * 0.75, (double)graphics.guiHeight()) * 0.25;
        int logoHeightHalf = (int)(logoHeight * 0.5);
        double contentWidth = logoHeight * 4.0;
        int logoWidthHalf = (int)(contentWidth * 0.5);
        int color = ARGB.white(logoAlpha);
        graphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, contentX - logoWidthHalf, logoY - logoHeightHalf, -0.0625f, 0.0f, logoWidthHalf, (int)logoHeight, 120, 60, 120, 120, color);
        graphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, contentX, logoY - logoHeightHalf, 0.0625f, 60.0f, logoWidthHalf, (int)logoHeight, 120, 60, 120, 120, color);
        int barY = (int)((double)graphics.guiHeight() * 0.8325);
        float actualProgress = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95f + actualProgress * 0.050000012f, 0.0f, 1.0f);
        if (fadeOutAnim < 1.0f) {
            this.extractProgressBar(graphics, width / 2 - logoWidthHalf, barY - 5, width / 2 + logoWidthHalf, barY + 5, 1.0f - Mth.clamp(fadeOutAnim, 0.0f, 1.0f));
        }
        if (fadeOutAnim >= 2.0f) {
            this.minecraft.gui.setOverlay(null);
        }
    }

    @Override
    public void tick() {
        if (this.fadeOutStart == -1L && this.reload.isDone() && this.isReadyToFadeOut()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            }
            catch (Throwable t) {
                this.onFinish.accept(Optional.of(t));
            }
            this.fadeOutStart = Util.getMillis();
            if (this.minecraft.gui.screen() != null) {
                Window window = this.minecraft.getWindow();
                this.minecraft.gui.screen().init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }
    }

    private boolean isReadyToFadeOut() {
        return !this.fadeIn || this.fadeInStart > -1L && Util.getMillis() - this.fadeInStart >= 1000L;
    }

    private void extractProgressBar(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float fade) {
        int width = Mth.ceil((float)(x1 - x0 - 2) * this.currentProgress);
        int alpha = Math.round(fade * 255.0f);
        int white = ARGB.color(alpha, 255, 255, 255);
        graphics.fill(x0 + 2, y0 + 2, x0 + width, y1 - 2, white);
        graphics.fill(x0 + 1, y0, x1 - 1, y0 + 1, white);
        graphics.fill(x0 + 1, y1, x1 - 1, y1 - 1, white);
        graphics.fill(x0, y0, x0 + 1, y1, white);
        graphics.fill(x1, y0, x1 - 1, y1, white);
    }

    @Environment(value=EnvType.CLIENT)
    private static class LogoTexture
    extends ReloadableTexture {
        public LogoTexture() {
            super(MOJANG_STUDIOS_LOGO_LOCATION);
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            ResourceProvider vanillaProvider = Minecraft.getInstance().getVanillaPackResources().asProvider();
            try (InputStream resource = vanillaProvider.open(MOJANG_STUDIOS_LOGO_LOCATION);){
                TextureContents textureContents = new TextureContents(NativeImage.read(resource), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0f));
                return textureContents;
            }
        }
    }
}

