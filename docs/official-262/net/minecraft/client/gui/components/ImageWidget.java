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
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class ImageWidget
extends AbstractWidget {
    private ImageWidget(int x, int y, int width, int height) {
        super(x, y, width, height, CommonComponents.EMPTY);
    }

    public static ImageWidget texture(int width, int height, Identifier texture, int textureWidth, int textureHeight) {
        return new Texture(0, 0, width, height, texture, textureWidth, textureHeight);
    }

    public static ImageWidget sprite(int width, int height, Identifier sprite) {
        return new Sprite(0, 0, width, height, sprite);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    public abstract void updateResource(Identifier var1);

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    private static class Texture
    extends ImageWidget {
        private Identifier texture;
        private final int textureWidth;
        private final int textureHeight;

        public Texture(int x, int y, int width, int height, Identifier texture, int textureWidth, int textureHeight) {
            super(x, y, width, height);
            this.texture = texture;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, this.getX(), this.getY(), 0.0f, 0.0f, this.getWidth(), this.getHeight(), this.textureWidth, this.textureHeight);
        }

        @Override
        public void updateResource(Identifier identifier) {
            this.texture = identifier;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Sprite
    extends ImageWidget {
        private Identifier sprite;

        public Sprite(int x, int y, int width, int height, Identifier sprite) {
            super(x, y, width, height);
            this.sprite = sprite;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }

        @Override
        public void updateResource(Identifier identifier) {
            this.sprite = identifier;
        }
    }
}

