/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record Unstitcher(Identifier resource, List<Region> regions, double xDivisor, double yDivisor) implements SpriteSource
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<Unstitcher> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("resource").forGetter(Unstitcher::resource), (App)ExtraCodecs.nonEmptyList(Region.CODEC.listOf()).fieldOf("regions").forGetter(Unstitcher::regions), (App)Codec.DOUBLE.optionalFieldOf("divisor_x", (Object)1.0).forGetter(Unstitcher::xDivisor), (App)Codec.DOUBLE.optionalFieldOf("divisor_y", (Object)1.0).forGetter(Unstitcher::yDivisor)).apply((Applicative)i, Unstitcher::new));

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        Identifier resourceId = TEXTURE_ID_CONVERTER.idToFile(this.resource);
        Optional<Resource> resource = resourceManager.getResource(resourceId);
        if (resource.isPresent()) {
            LazyLoadedImage image = new LazyLoadedImage(resourceId, resource.get(), this.regions.size());
            for (Region region : this.regions) {
                output.add(region.sprite, new RegionInstance(image, region, this.xDivisor, this.yDivisor));
            }
        } else {
            LOGGER.warn("Missing sprite: {}", (Object)resourceId);
        }
    }

    public MapCodec<Unstitcher> codec() {
        return MAP_CODEC;
    }

    @Environment(value=EnvType.CLIENT)
    public record Region(Identifier sprite, double x, double y, double width, double height) {
        public static final Codec<Region> CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.fieldOf("sprite").forGetter(Region::sprite), (App)Codec.DOUBLE.fieldOf("x").forGetter(Region::x), (App)Codec.DOUBLE.fieldOf("y").forGetter(Region::y), (App)Codec.DOUBLE.fieldOf("width").forGetter(Region::width), (App)Codec.DOUBLE.fieldOf("height").forGetter(Region::height)).apply((Applicative)i, Region::new));
    }

    @Environment(value=EnvType.CLIENT)
    private static class RegionInstance
    implements SpriteSource.DiscardableLoader {
        private final LazyLoadedImage image;
        private final Region region;
        private final double xDivisor;
        private final double yDivisor;

        private RegionInstance(LazyLoadedImage image, Region region, double xDivisor, double yDivisor) {
            this.image = image;
            this.region = region;
            this.xDivisor = xDivisor;
            this.yDivisor = yDivisor;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public SpriteContents get(SpriteResourceLoader loader) {
            try {
                NativeImage fullImage = this.image.get();
                double xScale = (double)fullImage.getWidth() / this.xDivisor;
                double yScale = (double)fullImage.getHeight() / this.yDivisor;
                int x = Mth.floor(this.region.x * xScale);
                int y = Mth.floor(this.region.y * yScale);
                int width = Mth.floor(this.region.width * xScale);
                int height = Mth.floor(this.region.height * yScale);
                NativeImage target = new NativeImage(NativeImage.Format.RGBA, width, height, false);
                fullImage.copyRect(target, x, y, 0, 0, width, height, false, false);
                SpriteContents spriteContents = new SpriteContents(this.region.sprite, new FrameSize(width, height), target);
                return spriteContents;
            }
            catch (Exception e) {
                LOGGER.error("Failed to unstitch region {}", (Object)this.region.sprite, (Object)e);
            }
            finally {
                this.image.release();
            }
            return MissingTextureAtlasSprite.create();
        }

        @Override
        public void discard() {
            this.image.release();
        }
    }
}

