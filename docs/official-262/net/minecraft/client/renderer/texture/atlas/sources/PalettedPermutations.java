/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Supplier
 *  com.google.common.base.Suppliers
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.Int2IntMap
 *  it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture.atlas.sources;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.LazyLoadedImage;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public record PalettedPermutations(List<Identifier> textures, Identifier paletteKey, Map<String, Identifier> permutations, String separator) implements SpriteSource
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DEFAULT_SEPARATOR = "_";
    public static final MapCodec<PalettedPermutations> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.list(Identifier.CODEC).fieldOf("textures").forGetter(PalettedPermutations::textures), (App)Identifier.CODEC.fieldOf("palette_key").forGetter(PalettedPermutations::paletteKey), (App)Codec.unboundedMap((Codec)Codec.STRING, Identifier.CODEC).fieldOf("permutations").forGetter(PalettedPermutations::permutations), (App)Codec.STRING.optionalFieldOf("separator", (Object)DEFAULT_SEPARATOR).forGetter(PalettedPermutations::separator)).apply((Applicative)i, PalettedPermutations::new));

    public PalettedPermutations(List<Identifier> textures, Identifier paletteKey, Map<String, Identifier> permutations) {
        this(textures, paletteKey, permutations, DEFAULT_SEPARATOR);
    }

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        Supplier paletteKeySupplier = Suppliers.memoize(() -> PalettedPermutations.loadPaletteEntryFromImage(resourceManager, this.paletteKey));
        HashMap palettes = new HashMap();
        this.permutations.forEach((arg_0, arg_1) -> PalettedPermutations.lambda$run$1(palettes, (java.util.function.Supplier)paletteKeySupplier, resourceManager, arg_0, arg_1));
        for (Identifier textureLocation : this.textures) {
            Identifier textureId = TEXTURE_ID_CONVERTER.idToFile(textureLocation);
            Optional<Resource> resource = resourceManager.getResource(textureId);
            if (resource.isEmpty()) {
                LOGGER.warn("Unable to find texture {}", (Object)textureId);
                continue;
            }
            LazyLoadedImage baseImage = new LazyLoadedImage(textureId, resource.get(), palettes.size());
            for (Map.Entry entry : palettes.entrySet()) {
                Identifier permutationLocation = textureLocation.withSuffix(this.separator + (String)entry.getKey());
                output.add(permutationLocation, new PalettedSpriteSupplier(baseImage, (java.util.function.Supplier)entry.getValue(), permutationLocation));
            }
        }
    }

    private static IntUnaryOperator createPaletteMapping(int[] keys, int[] values) {
        if (values.length != keys.length) {
            LOGGER.warn("Palette mapping has different sizes: {} and {}", (Object)keys.length, (Object)values.length);
            throw new IllegalArgumentException();
        }
        Int2IntOpenHashMap palette = new Int2IntOpenHashMap(values.length);
        for (int i = 0; i < keys.length; ++i) {
            int key = keys[i];
            if (ARGB.alpha(key) == 0) continue;
            palette.put(ARGB.transparent(key), values[i]);
        }
        return arg_0 -> PalettedPermutations.lambda$createPaletteMapping$0((Int2IntMap)palette, arg_0);
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static int[] loadPaletteEntryFromImage(ResourceManager resourceManager, Identifier location) {
        Optional<Resource> resource = resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(location));
        if (resource.isEmpty()) {
            LOGGER.error("Failed to load palette image {}", (Object)location);
            throw new IllegalArgumentException();
        }
        try (InputStream is = resource.get().open();){
            NativeImage image = NativeImage.read(is);
            try {
                int[] nArray = image.getPixels();
                if (image != null) {
                    image.close();
                }
                return nArray;
            }
            catch (Throwable throwable) {
                if (image != null) {
                    try {
                        image.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
        catch (Exception exception) {
            LOGGER.error("Couldn't load texture {}", (Object)location, (Object)exception);
            throw new IllegalArgumentException();
        }
    }

    public MapCodec<PalettedPermutations> codec() {
        return MAP_CODEC;
    }

    private static /* synthetic */ int lambda$createPaletteMapping$0(Int2IntMap palette, int pixel) {
        int pixelAlpha = ARGB.alpha(pixel);
        if (pixelAlpha == 0) {
            return pixel;
        }
        int pixelRGB = ARGB.transparent(pixel);
        int value = palette.getOrDefault(pixelRGB, ARGB.opaque(pixelRGB));
        int valueAlpha = ARGB.alpha(value);
        return ARGB.color(pixelAlpha * valueAlpha / 255, value);
    }

    private static /* synthetic */ void lambda$run$1(Map palettes, java.util.function.Supplier paletteKeySupplier, ResourceManager resourceManager, String suffix, Identifier palette) {
        palettes.put(suffix, Suppliers.memoize(() -> PalettedPermutations.lambda$run$2((java.util.function.Supplier)paletteKeySupplier, resourceManager, palette)));
    }

    private static /* synthetic */ IntUnaryOperator lambda$run$2(java.util.function.Supplier paletteKeySupplier, ResourceManager resourceManager, Identifier palette) {
        return PalettedPermutations.createPaletteMapping((int[])paletteKeySupplier.get(), PalettedPermutations.loadPaletteEntryFromImage(resourceManager, palette));
    }

    @Environment(value=EnvType.CLIENT)
    private record PalettedSpriteSupplier(LazyLoadedImage baseImage, java.util.function.Supplier<IntUnaryOperator> palette, Identifier permutationLocation) implements SpriteSource.DiscardableLoader
    {
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public @Nullable SpriteContents get(SpriteResourceLoader loader) {
            try {
                NativeImage image = this.baseImage.get().mappedCopy(this.palette.get());
                SpriteContents spriteContents = new SpriteContents(this.permutationLocation, new FrameSize(image.getWidth(), image.getHeight()), image);
                return spriteContents;
            }
            catch (IOException | IllegalArgumentException e) {
                LOGGER.error("unable to apply palette to {}", (Object)this.permutationLocation, (Object)e);
                SpriteContents spriteContents = null;
                return spriteContents;
            }
            finally {
                this.baseImage.release();
            }
        }

        @Override
        public void discard() {
            this.baseImage.release();
        }
    }
}

