/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.StitcherException;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SpriteLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Identifier location;
    private final int maxSupportedTextureSize;

    public SpriteLoader(Identifier location, int maxSupportedTextureSize) {
        this.location = location;
        this.maxSupportedTextureSize = maxSupportedTextureSize;
    }

    public static SpriteLoader create(TextureAtlas atlas) {
        return new SpriteLoader(atlas.location(), atlas.maxSupportedTextureSize());
    }

    private Preparations stitch(List<SpriteContents> sprites, int maxMipmapLevels, Executor executor) {
        try (Zone ignored = Profiler.get().zone(() -> "stitch " + String.valueOf(this.location));){
            int mipLevel;
            int maxTextureSize = this.maxSupportedTextureSize;
            int minTexelSize = Integer.MAX_VALUE;
            int lowestOneBit = 1 << maxMipmapLevels;
            for (SpriteContents spriteInfo : sprites) {
                minTexelSize = Math.min(minTexelSize, Math.min(spriteInfo.width(), spriteInfo.height()));
                int lowestTextureBit = Math.min(Integer.lowestOneBit(spriteInfo.width()), Integer.lowestOneBit(spriteInfo.height()));
                if (lowestTextureBit >= lowestOneBit) continue;
                LOGGER.warn("Texture {} with size {}x{} limits mip level from {} to {}", new Object[]{spriteInfo.name(), spriteInfo.width(), spriteInfo.height(), Mth.log2(lowestOneBit), Mth.log2(lowestTextureBit)});
                lowestOneBit = lowestTextureBit;
            }
            int minSize = Math.min(minTexelSize, lowestOneBit);
            int minPowerOfTwo = Mth.log2(minSize);
            if (minPowerOfTwo < maxMipmapLevels) {
                LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.location, maxMipmapLevels, minPowerOfTwo, minSize});
                mipLevel = minPowerOfTwo;
            } else {
                mipLevel = maxMipmapLevels;
            }
            Options options = Minecraft.getInstance().options;
            int anisotropyBit = options.textureFiltering().get() != TextureFilteringMethod.ANISOTROPIC ? 0 : options.maxAnisotropyBit().get();
            Stitcher<SpriteContents> stitcher = new Stitcher<SpriteContents>(maxTextureSize, maxTextureSize, mipLevel, anisotropyBit);
            for (SpriteContents spriteInfo : sprites) {
                stitcher.registerSprite(spriteInfo);
            }
            try {
                stitcher.stitch();
            }
            catch (StitcherException e) {
                CrashReport report = CrashReport.forThrowable(e, "Stitching");
                CrashReportCategory category = report.addCategory("Stitcher");
                category.setDetail("Sprites", e.getAllSprites().stream().map(s -> String.format(Locale.ROOT, "%s[%dx%d]", s.name(), s.width(), s.height())).collect(Collectors.joining(",")));
                category.setDetail("Max Texture Size", maxTextureSize);
                throw new ReportedException(report);
            }
            int width = stitcher.getWidth();
            int height = stitcher.getHeight();
            Map<Identifier, TextureAtlasSprite> result = this.getStitchedSprites(stitcher, width, height);
            TextureAtlasSprite missingSprite = result.get(MissingTextureAtlasSprite.getLocation());
            CompletableFuture<Void> readyForUpload = CompletableFuture.runAsync(() -> result.values().forEach(s -> s.contents().increaseMipLevel(mipLevel)), executor);
            Preparations preparations = new Preparations(width, height, mipLevel, missingSprite, result, readyForUpload);
            return preparations;
        }
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(SpriteResourceLoader resourceLoader, List<SpriteSource.Loader> sprites, Executor executor) {
        List<@Nullable CompletableFuture> spriteFutures = sprites.stream().map(supplier -> CompletableFuture.supplyAsync(() -> supplier.get(resourceLoader), executor)).toList();
        return Util.sequence(spriteFutures).thenApply(l -> l.stream().filter(Objects::nonNull).toList());
    }

    public CompletableFuture<Preparations> loadAndStitch(ResourceManager manager, Identifier atlasInfoLocation, int maxMipmapLevels, Executor taskExecutor, Set<MetadataSectionType<?>> additionalMetadata) {
        SpriteResourceLoader spriteResourceLoader = SpriteResourceLoader.create(additionalMetadata);
        return ((CompletableFuture)CompletableFuture.supplyAsync(() -> SpriteSourceList.load(manager, atlasInfoLocation).list(manager), taskExecutor).thenCompose(sprites -> SpriteLoader.runSpriteSuppliers(spriteResourceLoader, sprites, taskExecutor))).thenApply(resources -> this.stitch((List<SpriteContents>)resources, maxMipmapLevels, taskExecutor));
    }

    private Map<Identifier, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> stitcher, int atlasWidth, int atlasHeight) {
        HashMap<Identifier, TextureAtlasSprite> result = new HashMap<Identifier, TextureAtlasSprite>();
        stitcher.gatherSprites((contents, x, y, padding) -> result.put(contents.name(), new TextureAtlasSprite(this.location, (SpriteContents)contents, atlasWidth, atlasHeight, x, y, padding)));
        return result;
    }

    @Environment(value=EnvType.CLIENT)
    public record Preparations(int width, int height, int mipLevel, TextureAtlasSprite missing, Map<Identifier, TextureAtlasSprite> regions, CompletableFuture<Void> readyForUpload) {
        public @Nullable TextureAtlasSprite getSprite(Identifier id) {
            return this.regions.get(id);
        }
    }
}

