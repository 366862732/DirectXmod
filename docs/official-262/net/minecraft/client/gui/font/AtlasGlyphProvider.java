/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.PlainTextRenderable;
import net.minecraft.client.gui.font.SingleSpriteSource;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class AtlasGlyphProvider {
    private static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0f);
    private final TextureAtlas atlas;
    private final GlyphRenderTypes renderTypes;
    private final GlyphSource missingWrapper;
    private final Map<Identifier, GlyphSource> wrapperCache = new HashMap<Identifier, GlyphSource>();
    private final Function<Identifier, GlyphSource> spriteResolver;

    public AtlasGlyphProvider(TextureAtlas atlas) {
        this.atlas = atlas;
        this.renderTypes = GlyphRenderTypes.createForColorTexture(atlas.location());
        TextureAtlasSprite missingSprite = atlas.missingSprite();
        this.missingWrapper = this.createSprite(missingSprite);
        this.spriteResolver = id -> {
            TextureAtlasSprite sprite = atlas.getSprite((Identifier)id);
            if (sprite == missingSprite) {
                return this.missingWrapper;
            }
            return this.createSprite(sprite);
        };
    }

    public GlyphSource sourceForSprite(Identifier spriteId) {
        return this.wrapperCache.computeIfAbsent(spriteId, this.spriteResolver);
    }

    private GlyphSource createSprite(final TextureAtlasSprite sprite) {
        return new SingleSpriteSource(new BakedGlyph(){
            final /* synthetic */ AtlasGlyphProvider this$0;
            {
                AtlasGlyphProvider atlasGlyphProvider = this$0;
                Objects.requireNonNull(atlasGlyphProvider);
                this.this$0 = atlasGlyphProvider;
            }

            @Override
            public GlyphInfo info() {
                return GLYPH_INFO;
            }

            @Override
            public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
                return new Instance(this.this$0.renderTypes, this.this$0.atlas.getTextureView(), sprite, x, y, color, shadowColor, shadowOffset, style);
            }
        });
    }

    @Environment(value=EnvType.CLIENT)
    private record Instance(GlyphRenderTypes renderTypes, GpuTextureView textureView, TextureAtlasSprite sprite, float x, float y, int color, int shadowColor, float shadowOffset, Style style) implements PlainTextRenderable
    {
        @Override
        public void renderSprite(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, float offsetX, float offsetY, float z, int color) {
            float x0 = offsetX + this.left();
            float x1 = offsetX + this.right();
            float y0 = offsetY + this.top();
            float y1 = offsetY + this.bottom();
            buffer.addVertex(pose, x0, y0, z).setUv(this.sprite.getU0(), this.sprite.getV0()).setColor(color).setLight(packedLightCoords);
            buffer.addVertex(pose, x0, y1, z).setUv(this.sprite.getU0(), this.sprite.getV1()).setColor(color).setLight(packedLightCoords);
            buffer.addVertex(pose, x1, y1, z).setUv(this.sprite.getU1(), this.sprite.getV1()).setColor(color).setLight(packedLightCoords);
            buffer.addVertex(pose, x1, y0, z).setUv(this.sprite.getU1(), this.sprite.getV0()).setColor(color).setLight(packedLightCoords);
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return this.renderTypes.select(displayMode);
        }

        @Override
        public RenderPipeline guiPipeline() {
            return this.renderTypes.guiPipeline();
        }
    }
}

