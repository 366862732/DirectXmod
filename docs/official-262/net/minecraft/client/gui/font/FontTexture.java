/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class FontTexture
extends AbstractTexture
implements Dumpable {
    private static final int SIZE = 256;
    private final GlyphRenderTypes renderTypes;
    private final boolean colored;
    private final Node root;

    public FontTexture(Supplier<String> label, GlyphRenderTypes renderTypes, boolean colored) {
        this.colored = colored;
        this.root = new Node(0, 0, 256, 256);
        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture(label, 7, colored ? GpuFormat.RGBA8_UNORM : GpuFormat.R8_UNORM, 256, 256, 1, 1);
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = device.createTextureView(this.texture);
        this.renderTypes = renderTypes;
    }

    public @Nullable BakedSheetGlyph add(GlyphInfo info, GlyphBitmap glyph) {
        if (glyph.isColored() != this.colored) {
            return null;
        }
        Node node = this.root.insert(glyph);
        if (node != null) {
            glyph.upload(node.x, node.y, this.getTexture());
            float width = 256.0f;
            float height = 256.0f;
            float nudge = 0.01f;
            return new BakedSheetGlyph(info, this.renderTypes, this.getTextureView(), ((float)node.x + 0.01f) / 256.0f, ((float)node.x - 0.01f + (float)glyph.getPixelWidth()) / 256.0f, ((float)node.y + 0.01f) / 256.0f, ((float)node.y - 0.01f + (float)glyph.getPixelHeight()) / 256.0f, glyph.getLeft(), glyph.getRight(), glyph.getTop(), glyph.getBottom());
        }
        return null;
    }

    @Override
    public void dumpContents(Identifier selfId, Path dir) {
        if (this.texture == null) {
            return;
        }
        String outputId = selfId.toDebugFileName();
        TextureUtil.writeAsPNG(dir, outputId, this.texture, 0, argb -> (argb & 0xFF000000) == 0 ? -16777216 : argb);
    }

    @Environment(value=EnvType.CLIENT)
    private static class Node {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private @Nullable Node left;
        private @Nullable Node right;
        private boolean occupied;

        private Node(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public @Nullable Node insert(GlyphBitmap glyph) {
            if (this.left != null && this.right != null) {
                Node newNode = this.left.insert(glyph);
                if (newNode == null) {
                    newNode = this.right.insert(glyph);
                }
                return newNode;
            }
            if (this.occupied) {
                return null;
            }
            int glyphWidth = glyph.getPixelWidth();
            int glyphHeight = glyph.getPixelHeight();
            if (glyphWidth > this.width || glyphHeight > this.height) {
                return null;
            }
            if (glyphWidth == this.width && glyphHeight == this.height) {
                this.occupied = true;
                return this;
            }
            int deltaWidth = this.width - glyphWidth;
            int deltaHeight = this.height - glyphHeight;
            if (deltaWidth > deltaHeight) {
                this.left = new Node(this.x, this.y, glyphWidth, this.height);
                this.right = new Node(this.x + glyphWidth + 1, this.y, this.width - glyphWidth - 1, this.height);
            } else {
                this.left = new Node(this.x, this.y, this.width, glyphHeight);
                this.right = new Node(this.x, this.y + glyphHeight + 1, this.width, this.height - glyphHeight - 1);
            }
            return this.left.insert(glyph);
        }
    }
}

