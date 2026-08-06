/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class SpriteIconButton
extends Button {
    private static final Identifier BUTTON_DISABLED_SPRITE = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier LOADING_SPRITE = Identifier.withDefaultNamespace("friends/loading");
    private static final Tooltip LOADING_TOOLTIP = Tooltip.create(Component.translatable("gui.friends.button.loading"));
    private static final int LOADING_SPRITE_W = 5;
    private static final int LOADING_SPRITE_H = 2;
    protected final WidgetSprites sprite;
    protected final int spriteWidth;
    protected final int spriteHeight;
    protected final int spriteOffsetX;
    protected final int spriteOffsetY;
    private final boolean switchToLoadingAfterPress;
    private final @Nullable Tooltip defaultTooltip;
    private boolean loading;

    private SpriteIconButton(int width, int height, Component message, int spriteWidth, int spriteHeight, int spriteOffsetX, int spriteOffsetY, WidgetSprites sprite, Button.OnPress onPress, @Nullable Component tooltip, @Nullable Button.CreateNarration narration, boolean switchToLoadingAfterPress) {
        super(0, 0, width, height, message, onPress, narration == null ? DEFAULT_NARRATION : narration);
        this.defaultTooltip = tooltip != null ? Tooltip.create(tooltip) : null;
        this.setTooltip(this.defaultTooltip);
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.spriteOffsetX = spriteOffsetX;
        this.spriteOffsetY = spriteOffsetY;
        this.sprite = sprite;
        this.switchToLoadingAfterPress = switchToLoadingAfterPress;
    }

    public void setLoading(boolean loading) {
        this.setLoading(loading, LOADING_TOOLTIP);
    }

    public void setLoading(boolean loading, Tooltip loadingTooltip) {
        this.loading = loading;
        if (loading) {
            this.setTooltip(loadingTooltip);
        } else {
            this.setTooltip(this.defaultTooltip);
        }
    }

    @Override
    public boolean isActive() {
        return super.isActive() && !this.loading;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (this.switchToLoadingAfterPress) {
            this.setLoading(true);
        }
        super.onPress(input);
    }

    protected void extractSprite(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite.get(this.isActive(), this.isHoveredOrFocused()), x, y, this.spriteWidth, this.spriteHeight, this.alpha);
    }

    protected boolean extractLoadingStateIfLoading(GuiGraphicsExtractor graphics) {
        if (!this.loading) {
            return false;
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON_DISABLED_SPRITE, this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.white(this.alpha));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOADING_SPRITE, this.getX() + (this.getWidth() - 5) / 2, this.getY() + (this.getHeight() - 2) / 2, 5, 2, ARGB.white(this.alpha));
        return true;
    }

    public static Builder builder(Component message, Button.OnPress onPress, boolean iconOnly) {
        return new Builder(message, onPress, iconOnly);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        private final boolean iconOnly;
        private int width = 150;
        private int height = 20;
        private @Nullable WidgetSprites sprite;
        private int spriteWidth;
        private int spriteHeight;
        private int spriteOffsetX;
        private int spriteOffsetY;
        private @Nullable Component tooltip;
        private @Nullable Button.CreateNarration narration;
        private boolean switchToLoadingAfterPress;

        private Builder(Component message, Button.OnPress onPress, boolean iconOnly) {
            this.message = message;
            this.onPress = onPress;
            this.iconOnly = iconOnly;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder sprite(Identifier sprite, int spriteWidth, int spriteHeight) {
            this.sprite = new WidgetSprites(sprite);
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            return this;
        }

        public Builder sprite(WidgetSprites sprite, int spriteWidth, int spriteHeight) {
            this.sprite = sprite;
            this.spriteWidth = spriteWidth;
            this.spriteHeight = spriteHeight;
            return this;
        }

        public Builder spriteOffset(int spriteOffsetX, int spriteOffsetY) {
            this.spriteOffsetX = spriteOffsetX;
            this.spriteOffsetY = spriteOffsetY;
            return this;
        }

        public Builder tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder withTootip() {
            this.tooltip = this.message;
            return this;
        }

        public Builder narration(Button.CreateNarration narration) {
            this.narration = narration;
            return this;
        }

        public Builder switchToLoadingAfterPress() {
            this.switchToLoadingAfterPress = true;
            return this;
        }

        public SpriteIconButton build() {
            if (this.sprite == null) {
                throw new IllegalStateException("Sprite not set");
            }
            if (this.iconOnly) {
                return new CenteredIcon(this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.spriteOffsetX, this.spriteOffsetY, this.sprite, this.onPress, this.tooltip, this.narration, this.switchToLoadingAfterPress);
            }
            return new TextAndIcon(this.width, this.height, this.message, this.spriteWidth, this.spriteHeight, this.spriteOffsetX, this.spriteOffsetY, this.sprite, this.onPress, this.tooltip, this.narration, this.switchToLoadingAfterPress);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class TextAndIcon
    extends SpriteIconButton {
        protected TextAndIcon(int width, int height, Component message, int spriteWidth, int spriteHeight, int spriteOffsetX, int spriteOffsetY, WidgetSprites sprite, Button.OnPress onPress, @Nullable Component tooltip, @Nullable Button.CreateNarration narration, boolean switchToLoadingAfterPress) {
            super(width, height, message, spriteWidth, spriteHeight, spriteOffsetX, spriteOffsetY, sprite, onPress, tooltip, narration, switchToLoadingAfterPress);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            if (this.extractLoadingStateIfLoading(graphics)) {
                return;
            }
            this.extractDefaultSprite(graphics);
            int left = this.getX() + 2;
            int right = this.getX() + this.getWidth() - this.spriteWidth - 4;
            int centerX = this.getX() + this.getWidth() / 2;
            ActiveTextCollector output = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE);
            output.acceptScrolling(this.getMessage(), centerX, left, right, this.getY(), this.getY() + this.getHeight());
            int x = this.spriteOffsetX + this.getX() + this.getWidth() - this.spriteWidth - 2;
            int y = this.spriteOffsetY + this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
            this.extractSprite(graphics, x, y);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class CenteredIcon
    extends SpriteIconButton {
        protected CenteredIcon(int width, int height, Component message, int spriteWidth, int spriteHeight, int spriteOffsetX, int spriteOffsetY, WidgetSprites sprite, Button.OnPress onPress, @Nullable Component tooltip, @Nullable Button.CreateNarration narration, boolean switchToLoadingAfterPress) {
            super(width, height, message, spriteWidth, spriteHeight, spriteOffsetX, spriteOffsetY, sprite, onPress, tooltip, narration, switchToLoadingAfterPress);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            if (this.extractLoadingStateIfLoading(graphics)) {
                return;
            }
            this.extractDefaultSprite(graphics);
            int x = this.spriteOffsetX + this.getX() + this.getWidth() / 2 - this.spriteWidth / 2;
            int y = this.spriteOffsetY + this.getY() + this.getHeight() / 2 - this.spriteHeight / 2;
            this.extractSprite(graphics, x, y);
        }
    }
}

