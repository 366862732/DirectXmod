/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

@Environment(value=EnvType.CLIENT)
public class LogoRenderer {
    public static final Identifier MINECRAFT_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minecraft.png");
    public static final Identifier EASTER_EGG_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minceraft.png");
    public static final Identifier MINECRAFT_EDITION = Identifier.withDefaultNamespace("textures/gui/title/edition.png");
    public static final int LOGO_WIDTH = 256;
    public static final int LOGO_HEIGHT = 44;
    private static final int LOGO_TEXTURE_WIDTH = 256;
    private static final int LOGO_TEXTURE_HEIGHT = 64;
    private static final int EDITION_WIDTH = 128;
    private static final int EDITION_HEIGHT = 14;
    private static final int EDITION_TEXTURE_WIDTH = 128;
    private static final int EDITION_TEXTURE_HEIGHT = 16;
    public static final int DEFAULT_HEIGHT_OFFSET = 30;
    private static final int EDITION_LOGO_OVERLAP = 7;
    private final boolean showEasterEgg = (double)RandomSource.createThreadLocalInstance().nextFloat() < 1.0E-4;
    private final boolean keepLogoThroughFade;

    public LogoRenderer(boolean keepLogoThroughFade) {
        this.keepLogoThroughFade = keepLogoThroughFade;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int width, float alpha) {
        this.extractRenderState(graphics, width, alpha, 30);
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int width, float alpha, int heightOffset) {
        int logoX = width / 2 - 128;
        float effectiveAlpha = this.keepLogoThroughFade ? 1.0f : alpha;
        int color = ARGB.white(effectiveAlpha);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.showEasterEgg ? EASTER_EGG_LOGO : MINECRAFT_LOGO, logoX, heightOffset, 0.0f, 0.0f, 256, 44, 256, 64, color);
        int editionX = width / 2 - 64;
        int y = heightOffset + 44 - 7;
        graphics.blit(RenderPipelines.GUI_TEXTURED, MINECRAFT_EDITION, editionX, y, 0.0f, 0.0f, 128, 14, 128, 16, color);
    }

    public boolean keepLogoThroughFade() {
        return this.keepLogoThroughFade;
    }
}

