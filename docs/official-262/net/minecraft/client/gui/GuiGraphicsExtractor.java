/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fStack
 *  org.joml.Matrix3x2fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector2ic
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.client.renderer.state.gui.TiledBlitRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBannerResultRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiBookModelRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import net.minecraft.client.renderer.state.gui.pip.GuiSkinRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;
import org.joml.Quaternionfc;
import org.joml.Vector2ic;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class GuiGraphicsExtractor {
    private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
    private final Minecraft minecraft;
    private final Matrix3x2fStack pose;
    private final ScissorStack scissorStack;
    private final SpriteGetter sprites;
    private final TextureAtlas guiSprites;
    private final GuiRenderState guiRenderState;
    private CursorType pendingCursor = CursorType.DEFAULT;
    private final int mouseX;
    private final int mouseY;
    private @Nullable Runnable deferredTooltip;
    private @Nullable Style hoveredTextStyle;
    private @Nullable Style clickableTextStyle;
    private @Nullable Renderable preeditOverlay;

    private GuiGraphicsExtractor(Minecraft minecraft, Matrix3x2fStack pose, GuiRenderState guiRenderState, int mouseX, int mouseY) {
        this.minecraft = minecraft;
        this.pose = pose;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        AtlasManager atlasManager = minecraft.getAtlasManager();
        this.sprites = atlasManager;
        this.guiSprites = atlasManager.getAtlasOrThrow(AtlasIds.GUI);
        this.guiRenderState = guiRenderState;
        this.scissorStack = new ScissorStack(new ScreenRectangle(0, 0, this.guiWidth(), this.guiHeight()));
    }

    public GuiGraphicsExtractor(Minecraft minecraft, GuiRenderState guiRenderState, int mouseX, int mouseY) {
        this(minecraft, new Matrix3x2fStack(16), guiRenderState, mouseX, mouseY);
    }

    public void requestCursor(CursorType cursorType) {
        this.pendingCursor = cursorType;
    }

    public void applyCursor(Window window) {
        window.selectCursor(this.pendingCursor);
    }

    public int guiWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return this.minecraft.getWindow().getGuiScaledHeight();
    }

    public Matrix3x2fStack pose() {
        return this.pose;
    }

    public void nextStratum() {
        this.guiRenderState.nextStratum();
    }

    public void blurBeforeThisStratum() {
        this.guiRenderState.blurBeforeThisStratum();
    }

    public void enableScissor(int x0, int y0, int x1, int y1) {
        ScreenRectangle rectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformAxisAligned((Matrix3x2fc)this.pose);
        this.scissorStack.push(rectangle);
    }

    public void disableScissor() {
        this.scissorStack.pop();
    }

    public boolean containsPointInScissor(int x, int y) {
        return this.scissorStack.containsPoint(x, y);
    }

    public void horizontalLine(int x0, int x1, int y, int col) {
        if (x1 < x0) {
            int tmp = x0;
            x0 = x1;
            x1 = tmp;
        }
        this.fill(x0, y, x1 + 1, y + 1, col);
    }

    public void verticalLine(int x, int y0, int y1, int col) {
        if (y1 < y0) {
            int tmp = y0;
            y0 = y1;
            y1 = tmp;
        }
        this.fill(x, y0 + 1, x + 1, y1, col);
    }

    public void fill(int x0, int y0, int x1, int y1, int col) {
        this.fill(RenderPipelines.GUI, x0, y0, x1, y1, col);
    }

    public void fill(RenderPipeline pipeline, int x0, int y0, int x1, int y1, int col) {
        int tmp;
        if (x0 < x1) {
            tmp = x0;
            x0 = x1;
            x1 = tmp;
        }
        if (y0 < y1) {
            tmp = y0;
            y0 = y1;
            y1 = tmp;
        }
        this.innerFill(pipeline, TextureSetup.noTexture(), x0, y0, x1, y1, col, null);
    }

    public void fillGradient(int x0, int y0, int x1, int y1, int col1, int col2) {
        this.innerFill(RenderPipelines.GUI, TextureSetup.noTexture(), x0, y0, x1, y1, col1, col2);
    }

    public void fill(RenderPipeline renderPipeline, TextureSetup textureSetup, int x0, int y0, int x1, int y1) {
        this.innerFill(renderPipeline, textureSetup, x0, y0, x1, y1, -1, null);
    }

    public void outline(int x, int y, int width, int height, int color) {
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);
        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private void innerFill(RenderPipeline renderPipeline, TextureSetup textureSetup, int x0, int y0, int x1, int y1, int color1, @Nullable Integer color2) {
        this.guiRenderState.addGuiElement(new ColoredRectangleRenderState(renderPipeline, textureSetup, (Matrix3x2fc)new Matrix3x2f((Matrix3x2fc)this.pose), x0, y0, x1, y1, color1, color2 != null ? color2 : color1, this.scissorStack.peek()));
    }

    public void textHighlight(int x0, int y0, int x1, int y1, boolean invertText) {
        if (invertText) {
            this.fill(RenderPipelines.GUI_INVERT, x0, y0, x1, y1, -1);
        }
        this.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, x0, y0, x1, y1, -16776961);
    }

    public void text(Font font, @Nullable String str, int x, int y, int color) {
        this.text(font, str, x, y, color, true);
    }

    public void text(Font font, @Nullable String str, int x, int y, int color, boolean dropShadow) {
        if (str == null) {
            return;
        }
        this.text(font, Language.getInstance().getVisualOrder(FormattedText.of(str)), x, y, color, dropShadow);
    }

    public void text(Font font, FormattedCharSequence str, int x, int y, int color) {
        this.text(font, str, x, y, color, true);
    }

    public void text(Font font, FormattedCharSequence str, int x, int y, int color, boolean dropShadow) {
        if (ARGB.alpha(color) == 0) {
            return;
        }
        this.guiRenderState.addText(new GuiTextRenderState(font, str, (Matrix3x2fc)new Matrix3x2f((Matrix3x2fc)this.pose), x, y, color, 0, dropShadow, false, this.scissorStack.peek()));
    }

    public void text(Font font, Component str, int x, int y, int color) {
        this.text(font, str, x, y, color, true);
    }

    public void text(Font font, Component str, int x, int y, int color, boolean dropShadow) {
        this.text(font, str.getVisualOrderText(), x, y, color, dropShadow);
    }

    public void centeredText(Font font, String str, int x, int y, int color) {
        this.text(font, str, x - font.width(str) / 2, y, color);
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        FormattedCharSequence toRender = text.getVisualOrderText();
        this.text(font, toRender, x - font.width(toRender) / 2, y, color);
    }

    public void centeredText(Font font, FormattedCharSequence text, int x, int y, int color) {
        this.text(font, text, x - font.width(text) / 2, y, color);
    }

    public void textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col) {
        this.textWithWordWrap(font, string, x, y, width, col, true);
    }

    public void textWithWordWrap(Font font, FormattedText string, int x, int y, int width, int col, boolean dropShadow) {
        for (FormattedCharSequence line : font.split(string, width)) {
            this.text(font, line, x, y, col, dropShadow);
            y += font.lineHeight;
        }
    }

    public void textWithBackdrop(Font font, Component str, int textX, int textY, int textWidth, int textColor) {
        int backgroundColor = this.minecraft.options.getBackgroundColor(0.0f);
        if (backgroundColor != 0) {
            int padding = 2;
            this.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + font.lineHeight + 2, ARGB.multiply(backgroundColor, textColor));
        }
        this.text(font, str, textX, textY, textColor, true);
    }

    public void blit(RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        this.blit(renderPipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight, color);
    }

    public void blit(RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        this.blit(renderPipeline, texture, x, y, u, v, width, height, width, height, textureWidth, textureHeight);
    }

    public void blit(RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        this.blit(renderPipeline, texture, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth, textureHeight, -1);
    }

    public void blit(RenderPipeline renderPipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color) {
        this.innerBlit(renderPipeline, texture, x, x + width, y, y + height, (u + 0.0f) / (float)textureWidth, (u + (float)srcWidth) / (float)textureWidth, (v + 0.0f) / (float)textureHeight, (v + (float)srcHeight) / (float)textureHeight, color);
    }

    public void blit(Identifier location, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
        this.innerBlit(RenderPipelines.GUI_TEXTURED, location, x0, x1, y0, y1, u0, u1, v0, v1, -1);
    }

    public void blit(GpuTextureView textureView, GpuSampler sampler, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
        this.innerBlit(RenderPipelines.GUI_TEXTURED, textureView, sampler, x0, y0, x1, y1, u0, u1, v0, v1, -1);
    }

    public void blitSprite(RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height) {
        this.blitSprite(renderPipeline, location, x, y, width, height, -1);
    }

    public void blitSprite(RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height, float alpha) {
        this.blitSprite(renderPipeline, location, x, y, width, height, ARGB.white(alpha));
    }

    public void blitSprite(RenderPipeline renderPipeline, Identifier location, int x, int y, int width, int height, int color) {
        GuiSpriteScaling scaling;
        TextureAtlasSprite sprite = this.guiSprites.getSprite(location);
        GuiSpriteScaling guiSpriteScaling = scaling = GuiGraphicsExtractor.getSpriteScaling(sprite);
        Objects.requireNonNull(guiSpriteScaling);
        GuiSpriteScaling guiSpriteScaling2 = guiSpriteScaling;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{GuiSpriteScaling.Stretch.class, GuiSpriteScaling.Tile.class, GuiSpriteScaling.NineSlice.class}, (GuiSpriteScaling)guiSpriteScaling2, n)) {
            case 0: {
                GuiSpriteScaling.Stretch stretch = (GuiSpriteScaling.Stretch)guiSpriteScaling2;
                this.blitSprite(renderPipeline, sprite, x, y, width, height, color);
                break;
            }
            case 1: {
                GuiSpriteScaling.Tile tile = (GuiSpriteScaling.Tile)guiSpriteScaling2;
                this.blitTiledSprite(renderPipeline, sprite, x, y, width, height, 0, 0, tile.width(), tile.height(), tile.width(), tile.height(), color);
                break;
            }
            case 2: {
                GuiSpriteScaling.NineSlice nineSlice = (GuiSpriteScaling.NineSlice)guiSpriteScaling2;
                this.blitNineSlicedSprite(renderPipeline, sprite, nineSlice, x, y, width, height, color);
                break;
            }
        }
    }

    public void blitSprite(RenderPipeline renderPipeline, Identifier location, int spriteWidth, int spriteHeight, int textureX, int textureY, int x, int y, int width, int height) {
        this.blitSprite(renderPipeline, location, spriteWidth, spriteHeight, textureX, textureY, x, y, width, height, -1);
    }

    public void blitSprite(RenderPipeline renderPipeline, Identifier location, int spriteWidth, int spriteHeight, int textureX, int textureY, int x, int y, int width, int height, int color) {
        TextureAtlasSprite sprite = this.guiSprites.getSprite(location);
        GuiSpriteScaling scaling = GuiGraphicsExtractor.getSpriteScaling(sprite);
        if (scaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(renderPipeline, sprite, spriteWidth, spriteHeight, textureX, textureY, x, y, width, height, color);
        } else {
            this.enableScissor(x, y, x + width, y + height);
            this.blitSprite(renderPipeline, location, x - textureX, y - textureY, spriteWidth, spriteHeight, color);
            this.disableScissor();
        }
    }

    public void blitSprite(RenderPipeline renderPipeline, TextureAtlasSprite sprite, int x, int y, int width, int height) {
        this.blitSprite(renderPipeline, sprite, x, y, width, height, -1);
    }

    public void blitSprite(RenderPipeline renderPipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        if (width == 0 || height == 0) {
            return;
        }
        this.innerBlit(renderPipeline, sprite.atlasLocation(), x, x + width, y, y + height, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), color);
    }

    private void blitSprite(RenderPipeline renderPipeline, TextureAtlasSprite sprite, int spriteWidth, int spriteHeight, int textureX, int textureY, int x, int y, int width, int height, int color) {
        if (width == 0 || height == 0) {
            return;
        }
        this.innerBlit(renderPipeline, sprite.atlasLocation(), x, x + width, y, y + height, sprite.getU((float)textureX / (float)spriteWidth), sprite.getU((float)(textureX + width) / (float)spriteWidth), sprite.getV((float)textureY / (float)spriteHeight), sprite.getV((float)(textureY + height) / (float)spriteHeight), color);
    }

    private void blitNineSlicedSprite(RenderPipeline renderPipeline, TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice nineSlice, int x, int y, int width, int height, int color) {
        GuiSpriteScaling.NineSlice.Border border = nineSlice.border();
        int borderLeft = Math.min(border.left(), width / 2);
        int borderRight = Math.min(border.right(), width / 2);
        int borderTop = Math.min(border.top(), height / 2);
        int borderBottom = Math.min(border.bottom(), height / 2);
        if (width == nineSlice.width() && height == nineSlice.height()) {
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, height, color);
            return;
        }
        if (height == nineSlice.height()) {
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, borderLeft, height, color);
            this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x + borderLeft, y, width - borderRight - borderLeft, height, borderLeft, 0, nineSlice.width() - borderRight - borderLeft, nineSlice.height(), nineSlice.width(), nineSlice.height(), color);
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - borderRight, 0, x + width - borderRight, y, borderRight, height, color);
            return;
        }
        if (width == nineSlice.width()) {
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, width, borderTop, color);
            this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x, y + borderTop, width, height - borderBottom - borderTop, 0, borderTop, nineSlice.width(), nineSlice.height() - borderBottom - borderTop, nineSlice.width(), nineSlice.height(), color);
            this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - borderBottom, x, y + height - borderBottom, width, borderBottom, color);
            return;
        }
        this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, 0, x, y, borderLeft, borderTop, color);
        this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x + borderLeft, y, width - borderRight - borderLeft, borderTop, borderLeft, 0, nineSlice.width() - borderRight - borderLeft, borderTop, nineSlice.width(), nineSlice.height(), color);
        this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - borderRight, 0, x + width - borderRight, y, borderRight, borderTop, color);
        this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), 0, nineSlice.height() - borderBottom, x, y + height - borderBottom, borderLeft, borderBottom, color);
        this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x + borderLeft, y + height - borderBottom, width - borderRight - borderLeft, borderBottom, borderLeft, nineSlice.height() - borderBottom, nineSlice.width() - borderRight - borderLeft, borderBottom, nineSlice.width(), nineSlice.height(), color);
        this.blitSprite(renderPipeline, sprite, nineSlice.width(), nineSlice.height(), nineSlice.width() - borderRight, nineSlice.height() - borderBottom, x + width - borderRight, y + height - borderBottom, borderRight, borderBottom, color);
        this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x, y + borderTop, borderLeft, height - borderBottom - borderTop, 0, borderTop, borderLeft, nineSlice.height() - borderBottom - borderTop, nineSlice.width(), nineSlice.height(), color);
        this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x + borderLeft, y + borderTop, width - borderRight - borderLeft, height - borderBottom - borderTop, borderLeft, borderTop, nineSlice.width() - borderRight - borderLeft, nineSlice.height() - borderBottom - borderTop, nineSlice.width(), nineSlice.height(), color);
        this.blitNineSliceInnerSegment(renderPipeline, nineSlice, sprite, x + width - borderRight, y + borderTop, borderRight, height - borderBottom - borderTop, nineSlice.width() - borderRight, borderTop, borderRight, nineSlice.height() - borderBottom - borderTop, nineSlice.width(), nineSlice.height(), color);
    }

    private void blitNineSliceInnerSegment(RenderPipeline renderPipeline, GuiSpriteScaling.NineSlice nineSlice, TextureAtlasSprite sprite, int x, int y, int width, int height, int textureX, int textureY, int textureWidth, int textureHeight, int spriteWidth, int spriteHeight, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (nineSlice.stretchInner()) {
            this.innerBlit(renderPipeline, sprite.atlasLocation(), x, x + width, y, y + height, sprite.getU((float)textureX / (float)spriteWidth), sprite.getU((float)(textureX + textureWidth) / (float)spriteWidth), sprite.getV((float)textureY / (float)spriteHeight), sprite.getV((float)(textureY + textureHeight) / (float)spriteHeight), color);
        } else {
            this.blitTiledSprite(renderPipeline, sprite, x, y, width, height, textureX, textureY, textureWidth, textureHeight, spriteWidth, spriteHeight, color);
        }
    }

    private void blitTiledSprite(RenderPipeline renderPipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int textureX, int textureY, int tileWidth, int tileHeight, int spriteWidth, int spriteHeight, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("Tile size must be positive, got " + tileWidth + "x" + tileHeight);
        }
        AbstractTexture spriteTexture = this.minecraft.getTextureManager().getTexture(sprite.atlasLocation());
        GpuTextureView texture = spriteTexture.getTextureView();
        this.innerTiledBlit(renderPipeline, texture, spriteTexture.getSampler(), tileWidth, tileHeight, x, y, x + width, y + height, sprite.getU((float)textureX / (float)spriteWidth), sprite.getU((float)(textureX + tileWidth) / (float)spriteWidth), sprite.getV((float)textureY / (float)spriteHeight), sprite.getV((float)(textureY + tileHeight) / (float)spriteHeight), color);
    }

    private void innerBlit(RenderPipeline renderPipeline, Identifier location, int x0, int x1, int y0, int y1, float u0, float u1, float v0, float v1, int color) {
        AbstractTexture texture = this.minecraft.getTextureManager().getTexture(location);
        this.innerBlit(renderPipeline, texture.getTextureView(), texture.getSampler(), x0, y0, x1, y1, u0, u1, v0, v1, color);
    }

    private void innerBlit(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color) {
        this.guiRenderState.addGuiElement(new BlitRenderState(pipeline, TextureSetup.singleTexture(textureView, sampler), new Matrix3x2f((Matrix3x2fc)this.pose), x0, y0, x1, y1, u0, u1, v0, v1, color, this.scissorStack.peek()));
    }

    private void innerTiledBlit(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color) {
        this.guiRenderState.addGuiElement(new TiledBlitRenderState(pipeline, TextureSetup.singleTexture(textureView, sampler), new Matrix3x2f((Matrix3x2fc)this.pose), tileWidth, tileHeight, x0, y0, x1, y1, u0, u1, v0, v1, color, this.scissorStack.peek()));
    }

    private static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
        return sprite.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling();
    }

    public void item(ItemStack itemStack, int x, int y) {
        this.item(this.minecraft.player, this.minecraft.level, itemStack, x, y, 0);
    }

    public void item(ItemStack itemStack, int x, int y, int seed) {
        this.item(this.minecraft.player, this.minecraft.level, itemStack, x, y, seed);
    }

    public void item(LivingEntity owner, ItemStack itemStack, int x, int y, int seed) {
        this.item(owner, owner.level(), itemStack, x, y, seed);
    }

    private void item(@Nullable LivingEntity owner, @Nullable Level level, ItemStack itemStack, int x, int y, int seed) {
        if (itemStack.isEmpty()) {
            return;
        }
        TrackingItemStackRenderState itemStackRenderState = new TrackingItemStackRenderState();
        this.minecraft.getItemModelResolver().updateForTopItem(itemStackRenderState, itemStack, ItemDisplayContext.GUI, level, owner, seed);
        try {
            this.guiRenderState.addItem(new GuiItemRenderState(new Matrix3x2f((Matrix3x2fc)this.pose), itemStackRenderState, x, y, this.scissorStack.peek()));
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Rendering item");
            CrashReportCategory category = report.addCategory("Item being rendered");
            category.setDetail("Item Type", () -> String.valueOf(itemStack.getItem()));
            category.setDetail("Item Components", () -> String.valueOf(itemStack.getComponents()));
            category.setDetail("Item Foil", () -> String.valueOf(itemStack.hasFoil()));
            throw new ReportedException(report);
        }
    }

    public void fakeItem(ItemStack itemStack, int x, int y) {
        this.fakeItem(itemStack, x, y, 0);
    }

    public void fakeItem(ItemStack itemStack, int x, int y, int seed) {
        this.item(null, this.minecraft.level, itemStack, x, y, seed);
    }

    public void itemDecorations(Font font, ItemStack itemStack, int x, int y) {
        this.itemDecorations(font, itemStack, x, y, null);
    }

    public void itemDecorations(Font font, ItemStack itemStack, int x, int y, @Nullable String countText) {
        if (itemStack.isEmpty()) {
            return;
        }
        this.pose.pushMatrix();
        this.itemBar(itemStack, x, y);
        this.itemCooldown(itemStack, x, y);
        this.itemCount(font, itemStack, x, y, countText);
        this.pose.popMatrix();
    }

    private void itemBar(ItemStack itemStack, int x, int y) {
        if (itemStack.isBarVisible()) {
            int left = x + 2;
            int top = y + 13;
            this.fill(RenderPipelines.GUI, left, top, left + 13, top + 2, -16777216);
            this.fill(RenderPipelines.GUI, left, top, left + itemStack.getBarWidth(), top + 1, ARGB.opaque(itemStack.getBarColor()));
        }
    }

    private void itemCount(Font font, ItemStack itemStack, int x, int y, @Nullable String countText) {
        if (itemStack.getCount() != 1 || countText != null) {
            String amount = countText == null ? String.valueOf(itemStack.getCount()) : countText;
            this.text(font, amount, x + 19 - 2 - font.width(amount), y + 6 + 3, -1, true);
        }
    }

    private void itemCooldown(ItemStack itemStack, int x, int y) {
        float cooldown;
        LocalPlayer player = this.minecraft.player;
        float f = cooldown = player == null ? 0.0f : player.getCooldowns().getCooldownPercent(itemStack, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (cooldown > 0.0f) {
            int top = y + Mth.floor(16.0f * (1.0f - cooldown));
            int bottom = top + Mth.ceil(16.0f * cooldown);
            this.fill(RenderPipelines.GUI, x, top, x + 16, bottom, Integer.MAX_VALUE);
        }
    }

    public void map(MapRenderState mapRenderState) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager textureManager = minecraft.getTextureManager();
        AbstractTexture texture = textureManager.getTexture(mapRenderState.texture);
        this.innerBlit(RenderPipelines.GUI_TEXTURED, texture.getTextureView(), texture.getSampler(), 0, 0, 128, 128, 0.0f, 1.0f, 0.0f, 1.0f, -1);
        for (MapRenderState.MapDecorationRenderState decoration : mapRenderState.decorations) {
            if (!decoration.renderOnFrame) continue;
            this.pose.pushMatrix();
            this.pose.translate((float)decoration.x / 2.0f + 64.0f, (float)decoration.y / 2.0f + 64.0f);
            this.pose.rotate((float)Math.PI / 180 * (float)decoration.rot * 360.0f / 16.0f);
            this.pose.scale(4.0f, 4.0f);
            this.pose.translate(-0.125f, 0.125f);
            TextureAtlasSprite atlasSprite = decoration.atlasSprite;
            if (atlasSprite != null) {
                AbstractTexture decorationTexture = textureManager.getTexture(atlasSprite.atlasLocation());
                this.innerBlit(RenderPipelines.GUI_TEXTURED, decorationTexture.getTextureView(), decorationTexture.getSampler(), -1, -1, 1, 1, atlasSprite.getU0(), atlasSprite.getU1(), atlasSprite.getV1(), atlasSprite.getV0(), -1);
            }
            this.pose.popMatrix();
            if (decoration.name == null) continue;
            Font font = minecraft.font;
            float width = font.width(decoration.name);
            float f = 25.0f / width;
            Objects.requireNonNull(font);
            float scale = Mth.clamp(f, 0.0f, 6.0f / 9.0f);
            this.pose.pushMatrix();
            this.pose.translate((float)decoration.x / 2.0f + 64.0f - width * scale / 2.0f, (float)decoration.y / 2.0f + 64.0f + 4.0f);
            this.pose.scale(scale, scale);
            this.guiRenderState.addText(new GuiTextRenderState(font, decoration.name.getVisualOrderText(), (Matrix3x2fc)new Matrix3x2f((Matrix3x2fc)this.pose), 0, 0, -1, Integer.MIN_VALUE, false, false, this.scissorStack.peek()));
            this.pose.popMatrix();
        }
    }

    public void entity(EntityRenderState renderState, float scale, Vector3fc translation, Quaternionfc rotation, @Nullable Quaternionfc overrideCameraAngle, int x0, int y0, int x1, int y1) {
        renderState.lightCoords = 0xF000F0;
        this.guiRenderState.addPicturesInPictureState(new GuiEntityRenderState(renderState, translation, rotation, overrideCameraAngle, x0, y0, x1, y1, scale, this.scissorStack.peek()));
    }

    public void skin(Model.Simple playerModel, Identifier texture, float scale, float rotationX, float rotationY, float pivotY, int x0, int y0, int x1, int y1) {
        this.guiRenderState.addPicturesInPictureState(new GuiSkinRenderState(playerModel, texture, rotationX, rotationY, pivotY, x0, y0, x1, y1, scale, this.scissorStack.peek()));
    }

    public void book(BookModel bookModel, Identifier texture, float scale, float open, float flip, int x0, int y0, int x1, int y1) {
        this.guiRenderState.addPicturesInPictureState(new GuiBookModelRenderState(bookModel, texture, open, flip, x0, y0, x1, y1, scale, this.scissorStack.peek()));
    }

    public void bannerPattern(BannerFlagModel flag, DyeColor baseColor, BannerPatternLayers resultBannerPatterns, int x0, int y0, int x1, int y1) {
        this.guiRenderState.addPicturesInPictureState(new GuiBannerResultRenderState(flag, baseColor, resultBannerPatterns, x0, y0, x1, y1, this.scissorStack.peek()));
    }

    public void profilerChart(List<ResultField> chartData, int x0, int y0, int x1, int y1) {
        this.guiRenderState.addPicturesInPictureState(new GuiProfilerChartRenderState(chartData, x0, y0, x1, y1, this.scissorStack.peek()));
    }

    public void setTooltipForNextFrame(Component component, int x, int y) {
        this.setTooltipForNextFrame(List.of(component.getVisualOrderText()), x, y);
    }

    public void setTooltipForNextFrame(List<FormattedCharSequence> formattedCharSequences, int x, int y) {
        this.setTooltipForNextFrame(this.minecraft.font, formattedCharSequences, DefaultTooltipPositioner.INSTANCE, x, y, false);
    }

    public void setTooltipForNextFrame(Font font, ItemStack itemStack, int xo, int yo) {
        this.setTooltipForNextFrame(font, Screen.getTooltipFromItem(this.minecraft, itemStack), itemStack.getTooltipImage(), xo, yo, itemStack.get(DataComponents.TOOLTIP_STYLE));
    }

    public void setTooltipForNextFrame(Font font, List<Component> texts, Optional<TooltipComponent> optionalImage, int xo, int yo) {
        this.setTooltipForNextFrame(font, texts, optionalImage, xo, yo, null);
    }

    public void setTooltipForNextFrame(Font font, List<Component> texts, Optional<TooltipComponent> optionalImage, int xo, int yo, @Nullable Identifier style) {
        List<ClientTooltipComponent> components = texts.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).collect(Util.toMutableList());
        optionalImage.ifPresent(image -> components.add(components.isEmpty() ? 0 : 1, ClientTooltipComponent.create(image)));
        this.setTooltipForNextFrameInternal(font, components, xo, yo, DefaultTooltipPositioner.INSTANCE, style, false);
    }

    public void setTooltipForNextFrame(Font font, List<FormattedCharSequence> tooltip, Optional<TooltipComponent> component, ClientTooltipPositioner positioner, int xo, int yo, boolean replaceExisting, @Nullable Identifier style) {
        List<ClientTooltipComponent> components = tooltip.stream().map(ClientTooltipComponent::create).collect(Collectors.toList());
        component.ifPresent(tooltipComponent -> components.add(components.isEmpty() ? 0 : 1, ClientTooltipComponent.create(tooltipComponent)));
        this.setTooltipForNextFrameInternal(font, components, xo, yo, positioner, style, replaceExisting);
    }

    public void setTooltipForNextFrame(Font font, Component text, int xo, int yo) {
        this.setTooltipForNextFrame(font, text, xo, yo, null);
    }

    public void setTooltipForNextFrame(Font font, Component text, int xo, int yo, @Nullable Identifier style) {
        this.setTooltipForNextFrame(font, List.of(text.getVisualOrderText()), xo, yo, style);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int xo, int yo) {
        this.setComponentTooltipForNextFrame(font, lines, xo, yo, null);
    }

    public void setComponentTooltipForNextFrame(Font font, List<Component> lines, int xo, int yo, @Nullable Identifier style) {
        this.setTooltipForNextFrameInternal(font, lines.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList(), xo, yo, DefaultTooltipPositioner.INSTANCE, style, false);
    }

    public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int xo, int yo) {
        this.setTooltipForNextFrame(font, lines, xo, yo, null);
    }

    public void setTooltipForNextFrame(Font font, List<? extends FormattedCharSequence> lines, int xo, int yo, @Nullable Identifier style) {
        this.setTooltipForNextFrameInternal(font, lines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), xo, yo, DefaultTooltipPositioner.INSTANCE, style, false);
    }

    public void setTooltipForNextFrame(Font font, List<FormattedCharSequence> tooltip, ClientTooltipPositioner positioner, int xo, int yo, boolean replaceExisting) {
        this.setTooltipForNextFrameInternal(font, tooltip.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), xo, yo, positioner, null, replaceExisting);
    }

    private void setTooltipForNextFrameInternal(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @Nullable Identifier style, boolean replaceExisting) {
        if (lines.isEmpty()) {
            return;
        }
        if (this.deferredTooltip == null || replaceExisting) {
            this.deferredTooltip = () -> this.tooltip(font, lines, xo, yo, positioner, style);
        }
    }

    public void tooltip(Font font, List<ClientTooltipComponent> lines, int xo, int yo, ClientTooltipPositioner positioner, @Nullable Identifier style) {
        ClientTooltipComponent line;
        int i;
        int textWidth = 0;
        int tempHeight = lines.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent line2 : lines) {
            int lineWidth = line2.getWidth(font);
            if (lineWidth > textWidth) {
                textWidth = lineWidth;
            }
            tempHeight += line2.getHeight(font);
        }
        int w = textWidth;
        int h = tempHeight;
        Vector2ic positionedTooltip = positioner.positionTooltip(this.guiWidth(), this.guiHeight(), xo, yo, w, h);
        int x = positionedTooltip.x();
        int y = positionedTooltip.y();
        this.pose.pushMatrix();
        TooltipRenderUtil.extractTooltipBackground(this, x, y, w, h, style);
        int localY = y;
        for (i = 0; i < lines.size(); ++i) {
            line = lines.get(i);
            line.extractText(this, font, x, localY);
            localY += line.getHeight(font) + (i == 0 ? 2 : 0);
        }
        localY = y;
        for (i = 0; i < lines.size(); ++i) {
            line = lines.get(i);
            line.extractImage(font, x, localY, w, h, this);
            localY += line.getHeight(font) + (i == 0 ? 2 : 0);
        }
        this.pose.popMatrix();
    }

    public void setPreeditOverlay(Renderable preeditOverlay) {
        this.preeditOverlay = preeditOverlay;
    }

    public void extractDeferredElements(int mouseX, int mouseY, float a) {
        if (this.hoveredTextStyle != null) {
            this.componentHoverEffect(this.minecraft.font, this.hoveredTextStyle, mouseX, mouseY);
        }
        if (this.clickableTextStyle != null && this.clickableTextStyle.getClickEvent() != null) {
            this.requestCursor(CursorTypes.POINTING_HAND);
        }
        if (this.preeditOverlay != null) {
            this.nextStratum();
            this.preeditOverlay.extractRenderState(this, mouseX, mouseY, a);
        }
        if (this.deferredTooltip != null) {
            this.nextStratum();
            this.deferredTooltip.run();
            this.deferredTooltip = null;
        }
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void componentHoverEffect(Font font, Style hoveredStyle, int xMouse, int yMouse) {
        if (hoveredStyle.getHoverEvent() == null) return;
        HoverEvent hoverEvent = hoveredStyle.getHoverEvent();
        Objects.requireNonNull(hoverEvent);
        HoverEvent hoverEvent2 = hoverEvent;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{HoverEvent.ShowItem.class, HoverEvent.ShowEntity.class, HoverEvent.ShowText.class}, (HoverEvent)hoverEvent2, n)) {
            case 0: {
                HoverEvent.ShowItem showItem = (HoverEvent.ShowItem)hoverEvent2;
                try {
                    ItemStackTemplate itemStackTemplate;
                    ItemStackTemplate item = itemStackTemplate = showItem.item();
                    this.setTooltipForNextFrame(font, item.create(), xMouse, yMouse);
                    return;
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
            case 1: {
                HoverEvent.ShowEntity showEntity = (HoverEvent.ShowEntity)hoverEvent2;
                {
                    HoverEvent.EntityTooltipInfo entityTooltipInfo;
                    HoverEvent.EntityTooltipInfo entity = entityTooltipInfo = showEntity.entity();
                    if (!this.minecraft.options.advancedItemTooltips) return;
                    this.setComponentTooltipForNextFrame(font, entity.getTooltipLines(), xMouse, yMouse);
                    return;
                }
            }
            case 2: {
                HoverEvent.ShowText showText = (HoverEvent.ShowText)hoverEvent2;
                {
                    Component component;
                    Component text = component = showText.value();
                    this.setTooltipForNextFrame(font, font.split(text, Math.max(this.guiWidth() / 2, 200)), xMouse, yMouse);
                    return;
                }
            }
        }
    }

    public TextureAtlasSprite getSprite(SpriteId sprite) {
        return this.sprites.get(sprite);
    }

    public ActiveTextCollector textRendererForWidget(AbstractWidget owner, HoveredTextEffects hoveredTextEffects) {
        return new RenderingTextCollector(this, this.createDefaultTextParameters(owner.getAlpha()), hoveredTextEffects, null);
    }

    public ActiveTextCollector textRenderer() {
        return this.textRenderer(HoveredTextEffects.TOOLTIP_ONLY);
    }

    public ActiveTextCollector textRenderer(HoveredTextEffects hoveredTextEffects) {
        return this.textRenderer(hoveredTextEffects, null);
    }

    public ActiveTextCollector textRenderer(HoveredTextEffects hoveredTextEffects, @Nullable Consumer<Style> additionalHoverStyleConsumer) {
        return new RenderingTextCollector(this, this.createDefaultTextParameters(1.0f), hoveredTextEffects, additionalHoverStyleConsumer);
    }

    private ActiveTextCollector.Parameters createDefaultTextParameters(float opacity) {
        return new ActiveTextCollector.Parameters((Matrix3x2fc)new Matrix3x2f((Matrix3x2fc)this.pose), opacity, this.scissorStack.peek());
    }

    @Environment(value=EnvType.CLIENT)
    private static class ScissorStack {
        private final ScreenRectangle screenSize;
        private final Deque<ScreenRectangle> stack = new ArrayDeque<ScreenRectangle>();

        private ScissorStack(ScreenRectangle screenSize) {
            this.screenSize = screenSize;
        }

        public void push(ScreenRectangle rectangle) {
            ScreenRectangle lastRectangle = Objects.requireNonNullElse(this.stack.peekLast(), this.screenSize);
            ScreenRectangle intersection = Objects.requireNonNullElse(rectangle.intersection(lastRectangle), ScreenRectangle.empty());
            this.stack.addLast(intersection);
        }

        public void pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            }
            this.stack.removeLast();
        }

        public @Nullable ScreenRectangle peek() {
            return this.stack.peekLast();
        }

        public boolean containsPoint(int x, int y) {
            if (this.stack.isEmpty()) {
                return true;
            }
            return this.stack.peek().containsPoint(x, y);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class RenderingTextCollector
    implements ActiveTextCollector,
    Consumer<Style> {
        private ActiveTextCollector.Parameters defaultParameters;
        private final HoveredTextEffects hoveredTextEffects;
        private final @Nullable Consumer<Style> additionalConsumer;
        final /* synthetic */ GuiGraphicsExtractor this$0;

        private RenderingTextCollector(GuiGraphicsExtractor guiGraphicsExtractor, ActiveTextCollector.Parameters initialParameters, @Nullable HoveredTextEffects hoveredTextEffects, Consumer<Style> additonalConsumer) {
            GuiGraphicsExtractor guiGraphicsExtractor2 = guiGraphicsExtractor;
            Objects.requireNonNull(guiGraphicsExtractor2);
            this.this$0 = guiGraphicsExtractor2;
            this.defaultParameters = initialParameters;
            this.hoveredTextEffects = hoveredTextEffects;
            this.additionalConsumer = additonalConsumer;
        }

        @Override
        public ActiveTextCollector.Parameters defaultParameters() {
            return this.defaultParameters;
        }

        @Override
        public void defaultParameters(ActiveTextCollector.Parameters newParameters) {
            this.defaultParameters = newParameters;
        }

        @Override
        public void accept(Style style) {
            if (this.hoveredTextEffects.allowTooltip && style.getHoverEvent() != null) {
                this.this$0.hoveredTextStyle = style;
            }
            if (this.hoveredTextEffects.allowCursorChanges && style.getClickEvent() != null) {
                this.this$0.clickableTextStyle = style;
            }
            if (this.additionalConsumer != null) {
                this.additionalConsumer.accept(style);
            }
        }

        @Override
        public void accept(TextAlignment alignment, int anchorX, int y, ActiveTextCollector.Parameters parameters, FormattedCharSequence text) {
            boolean needsFullStyleScan = this.hoveredTextEffects.allowCursorChanges || this.hoveredTextEffects.allowTooltip || this.additionalConsumer != null;
            int leftX = alignment.calculateLeft(anchorX, this.this$0.minecraft.font, text);
            GuiTextRenderState renderState = new GuiTextRenderState(this.this$0.minecraft.font, text, parameters.pose(), leftX, y, ARGB.white(parameters.opacity()), 0, true, needsFullStyleScan, parameters.scissor());
            if (ARGB.as8BitChannel(parameters.opacity()) != 0) {
                this.this$0.guiRenderState.addText(renderState);
            }
            if (needsFullStyleScan) {
                ActiveTextCollector.findElementUnderCursor(renderState, this.this$0.mouseX, this.this$0.mouseY, this);
            }
        }

        @Override
        public void acceptScrolling(Component message, int centerX, int left, int right, int top, int bottom, ActiveTextCollector.Parameters parameters) {
            int lineWidth = this.this$0.minecraft.font.width(message);
            int lineHeight = this.this$0.minecraft.font.lineHeight;
            this.defaultScrollingHelper(message, centerX, left, right, top, bottom, lineWidth, lineHeight, parameters);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum HoveredTextEffects {
        NONE(false, false),
        TOOLTIP_ONLY(true, false),
        TOOLTIP_AND_CURSOR(true, true);

        public final boolean allowTooltip;
        public final boolean allowCursorChanges;

        private HoveredTextEffects(boolean allowTooltip, boolean allowCursorChanges) {
            this.allowTooltip = allowTooltip;
            this.allowCursorChanges = allowCursorChanges;
        }

        public static HoveredTextEffects notClickable(boolean canTooltip) {
            return canTooltip ? TOOLTIP_ONLY : NONE;
        }
    }
}

