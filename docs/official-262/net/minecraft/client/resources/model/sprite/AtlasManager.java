/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model.sprite;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class AtlasManager
implements AutoCloseable,
PreparableReloadListener,
SpriteGetter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<AtlasConfig> KNOWN_ATLASES = List.of(new AtlasConfig(Sheets.ARMOR_TRIMS_SHEET, AtlasIds.ARMOR_TRIMS, false), new AtlasConfig(Sheets.BANNER_SHEET, AtlasIds.BANNER_PATTERNS, false), new AtlasConfig(TextureAtlas.LOCATION_BLOCKS, AtlasIds.BLOCKS, true), new AtlasConfig(TextureAtlas.LOCATION_ITEMS, AtlasIds.ITEMS, false), new AtlasConfig(Sheets.CHEST_SHEET, AtlasIds.CHESTS, false), new AtlasConfig(Sheets.DECORATED_POT_SHEET, AtlasIds.DECORATED_POT, false), new AtlasConfig(Sheets.GUI_SHEET, AtlasIds.GUI, false, Set.of(GuiMetadataSection.TYPE)), new AtlasConfig(Sheets.MAP_DECORATIONS_SHEET, AtlasIds.MAP_DECORATIONS, false), new AtlasConfig(Sheets.PAINTINGS_SHEET, AtlasIds.PAINTINGS, false), new AtlasConfig(TextureAtlas.LOCATION_PARTICLES, AtlasIds.PARTICLES, false), new AtlasConfig(Sheets.SHIELD_SHEET, AtlasIds.SHIELD_PATTERNS, false), new AtlasConfig(Sheets.SHULKER_SHEET, AtlasIds.SHULKER_BOXES, false), new AtlasConfig(Sheets.CELESTIAL_SHEET, AtlasIds.CELESTIALS, false));
    public static final PreparableReloadListener.StateKey<PendingStitchResults> PENDING_STITCH = new PreparableReloadListener.StateKey();
    private final Map<Identifier, AtlasEntry> atlasByTexture = new HashMap<Identifier, AtlasEntry>();
    private final Map<Identifier, AtlasEntry> atlasById = new HashMap<Identifier, AtlasEntry>();
    private Map<SpriteId, TextureAtlasSprite> spriteLookup = Map.of();
    private int maxMipmapLevels;

    public AtlasManager(TextureManager textureManager, int maxMipmapLevels) {
        for (AtlasConfig info : KNOWN_ATLASES) {
            TextureAtlas atlasTexture = new TextureAtlas(info.textureId);
            textureManager.register(info.textureId, atlasTexture);
            AtlasEntry atlasEntry = new AtlasEntry(atlasTexture, info);
            this.atlasByTexture.put(info.textureId, atlasEntry);
            this.atlasById.put(info.definitionLocation, atlasEntry);
        }
        this.maxMipmapLevels = maxMipmapLevels;
    }

    public TextureAtlas getAtlasOrThrow(Identifier atlasId) {
        AtlasEntry atlasEntry = this.atlasById.get(atlasId);
        if (atlasEntry == null) {
            throw new IllegalArgumentException("Invalid atlas id: " + String.valueOf(atlasId));
        }
        return atlasEntry.atlas();
    }

    public void forEach(BiConsumer<Identifier, TextureAtlas> output) {
        this.atlasById.forEach((? super K atlasId, ? super V entry) -> output.accept((Identifier)atlasId, entry.atlas));
    }

    public void updateMaxMipLevel(int maxMipmapLevels) {
        this.maxMipmapLevels = maxMipmapLevels;
    }

    @Override
    public void close() {
        this.spriteLookup = Map.of();
        this.atlasById.values().forEach(AtlasEntry::close);
        this.atlasById.clear();
        this.atlasByTexture.clear();
    }

    @Override
    public TextureAtlasSprite get(SpriteId sprite) {
        TextureAtlasSprite result = this.spriteLookup.get(sprite);
        if (result != null) {
            return result;
        }
        Identifier atlasTextureId = sprite.atlasLocation();
        AtlasEntry atlasEntry = this.atlasByTexture.get(atlasTextureId);
        if (atlasEntry == null) {
            throw new IllegalArgumentException("Invalid atlas texture id: " + String.valueOf(atlasTextureId));
        }
        return atlasEntry.atlas().missingSprite();
    }

    @Override
    public void prepareSharedState(PreparableReloadListener.SharedState currentReload) {
        int atlasCount = this.atlasById.size();
        ArrayList<PendingStitch> pendingStitches = new ArrayList<PendingStitch>(atlasCount);
        HashMap<Identifier, CompletableFuture<SpriteLoader.Preparations>> pendingStitchById = new HashMap<Identifier, CompletableFuture<SpriteLoader.Preparations>>(atlasCount);
        ArrayList readyForUploads = new ArrayList(atlasCount);
        this.atlasById.forEach((? super K atlasId, ? super V atlasEntry) -> {
            CompletableFuture<SpriteLoader.Preparations> stitchingDone = new CompletableFuture<SpriteLoader.Preparations>();
            pendingStitchById.put((Identifier)atlasId, stitchingDone);
            pendingStitches.add(new PendingStitch((AtlasEntry)atlasEntry, stitchingDone));
            readyForUploads.add(stitchingDone.thenCompose(SpriteLoader.Preparations::readyForUpload));
        });
        CompletableFuture<Void> allReadyForUploads = CompletableFuture.allOf((CompletableFuture[])readyForUploads.toArray(CompletableFuture[]::new));
        currentReload.set(PENDING_STITCH, new PendingStitchResults(pendingStitches, pendingStitchById, allReadyForUploads));
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.SharedState currentReload, Executor taskExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor reloadExecutor) {
        PendingStitchResults pendingStitches = currentReload.get(PENDING_STITCH);
        ResourceManager resourceManager = currentReload.resourceManager();
        pendingStitches.pendingStitches.forEach((? super T pending) -> pending.entry.scheduleLoad(resourceManager, taskExecutor, this.maxMipmapLevels).whenComplete((value, throwable) -> {
            if (value != null) {
                pending.preparations.complete((SpriteLoader.Preparations)value);
            } else {
                pending.preparations.completeExceptionally((Throwable)throwable);
            }
        }));
        return ((CompletableFuture)pendingStitches.allReadyToUpload.thenCompose(preparationBarrier::wait)).thenAcceptAsync(unused -> this.updateSpriteMaps(pendingStitches), reloadExecutor);
    }

    private void updateSpriteMaps(PendingStitchResults pendingStitches) {
        this.spriteLookup = pendingStitches.joinAndUpload();
        HashMap globalSpriteLookup = new HashMap();
        this.spriteLookup.forEach((? super K id, ? super V sprite) -> {
            TextureAtlasSprite previous;
            if (!id.texture().equals(MissingTextureAtlasSprite.getLocation()) && (previous = globalSpriteLookup.putIfAbsent(id.texture(), sprite)) != null) {
                LOGGER.warn("Duplicate sprite {} from atlas {}, already defined in atlas {}. This will be rejected in a future version", new Object[]{id.texture(), id.atlasLocation(), previous.atlasLocation()});
            }
        });
    }

    @Environment(value=EnvType.CLIENT)
    public record AtlasConfig(Identifier textureId, Identifier definitionLocation, boolean createMipmaps, Set<MetadataSectionType<?>> additionalMetadata) {
        public AtlasConfig(Identifier textureId, Identifier definitionLocation, boolean createMipmaps) {
            this(textureId, definitionLocation, createMipmaps, Set.of());
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record AtlasEntry(TextureAtlas atlas, AtlasConfig config) implements AutoCloseable
    {
        @Override
        public void close() {
            this.atlas.close();
        }

        private CompletableFuture<SpriteLoader.Preparations> scheduleLoad(ResourceManager resourceManager, Executor executor, int maxMipmapLevels) {
            return SpriteLoader.create(this.atlas).loadAndStitch(resourceManager, this.config.definitionLocation, this.config.createMipmaps ? maxMipmapLevels : 0, executor, this.config.additionalMetadata);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class PendingStitchResults {
        private final List<PendingStitch> pendingStitches;
        private final Map<Identifier, CompletableFuture<SpriteLoader.Preparations>> stitchFuturesById;
        private final CompletableFuture<?> allReadyToUpload;

        private PendingStitchResults(List<PendingStitch> pendingStitches, Map<Identifier, CompletableFuture<SpriteLoader.Preparations>> stitchFuturesById, CompletableFuture<?> allReadyToUpload) {
            this.pendingStitches = pendingStitches;
            this.stitchFuturesById = stitchFuturesById;
            this.allReadyToUpload = allReadyToUpload;
        }

        public Map<SpriteId, TextureAtlasSprite> joinAndUpload() {
            HashMap<SpriteId, TextureAtlasSprite> result = new HashMap<SpriteId, TextureAtlasSprite>();
            this.pendingStitches.forEach(pendingStitch -> pendingStitch.joinAndUpload(result));
            return result;
        }

        public CompletableFuture<SpriteLoader.Preparations> get(Identifier atlasId) {
            return Objects.requireNonNull(this.stitchFuturesById.get(atlasId));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record PendingStitch(AtlasEntry entry, CompletableFuture<SpriteLoader.Preparations> preparations) {
        public void joinAndUpload(Map<SpriteId, TextureAtlasSprite> result) {
            SpriteLoader.Preparations preparations = this.preparations.join();
            this.entry.atlas.upload(preparations);
            preparations.regions().forEach((spriteId, spriteContents) -> result.put(new SpriteId(this.entry.config.textureId, (Identifier)spriteId), (TextureAtlasSprite)spriteContents));
        }
    }
}

