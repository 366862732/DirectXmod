/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 */
package net.minecraft.client.resources.model.geometry;

import com.mojang.blaze3d.platform.Transparency;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public record BakedQuad(Vector3fc position0, Vector3fc position1, Vector3fc position2, Vector3fc position3, long packedUV0, long packedUV1, long packedUV2, long packedUV3, Direction direction, MaterialInfo materialInfo) {
    public static final int VERTEX_COUNT = 4;
    public static final int FLAG_TRANSLUCENT = 1;
    public static final int FLAG_ANIMATED = 2;

    public Vector3fc position(int vertex) {
        return switch (vertex) {
            case 0 -> this.position0;
            case 1 -> this.position1;
            case 2 -> this.position2;
            case 3 -> this.position3;
            default -> throw new IndexOutOfBoundsException(vertex);
        };
    }

    public long packedUV(int vertex) {
        return switch (vertex) {
            case 0 -> this.packedUV0;
            case 1 -> this.packedUV1;
            case 2 -> this.packedUV2;
            case 3 -> this.packedUV3;
            default -> throw new IndexOutOfBoundsException(vertex);
        };
    }

    @Environment(value=EnvType.CLIENT)
    public record MaterialInfo(TextureAtlasSprite sprite, ChunkSectionLayer layer, RenderType itemRenderType, int tintIndex, boolean shade, int lightEmission) {
        public static MaterialInfo of(Material.Baked material, Transparency transparency, int tintIndex, boolean shade, int lightEmission) {
            ChunkSectionLayer layer = ChunkSectionLayer.byTransparency(transparency);
            RenderType itemRenderType = material.sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS) ? (transparency.hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet()) : (transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet());
            return new MaterialInfo(material.sprite(), layer, itemRenderType, tintIndex, shade, lightEmission);
        }

        public boolean isTinted() {
            return this.tintIndex != -1;
        }

        public @MaterialFlags int flags() {
            int flags = 0;
            flags |= this.layer.translucent() ? 1 : 0;
            return flags |= this.sprite.contents().isAnimated() ? 2 : 0;
        }
    }

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    @Environment(value=EnvType.CLIENT)
    public static @interface MaterialFlags {
    }
}

