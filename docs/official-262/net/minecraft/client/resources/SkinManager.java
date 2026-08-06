/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  com.google.common.hash.Hashing
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.SignatureState
 *  com.mojang.authlib.minecraft.MinecraftProfileTexture
 *  com.mojang.authlib.minecraft.MinecraftProfileTexture$Type
 *  com.mojang.authlib.minecraft.MinecraftProfileTextures
 *  com.mojang.authlib.properties.Property
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Services;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class SkinManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Services services;
    private final SkinTextureDownloader skinTextureDownloader;
    private final LoadingCache<CacheKey, CompletableFuture<Optional<PlayerSkin>>> skinCache;
    private final TextureCache skinTextures;
    private final TextureCache capeTextures;
    private final TextureCache elytraTextures;

    public SkinManager(Path skinsDirectory, final Services services, SkinTextureDownloader skinTextureDownloader, final Executor mainThreadExecutor) {
        this.services = services;
        this.skinTextureDownloader = skinTextureDownloader;
        this.skinTextures = new TextureCache(this, skinsDirectory, MinecraftProfileTexture.Type.SKIN);
        this.capeTextures = new TextureCache(this, skinsDirectory, MinecraftProfileTexture.Type.CAPE);
        this.elytraTextures = new TextureCache(this, skinsDirectory, MinecraftProfileTexture.Type.ELYTRA);
        this.skinCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofSeconds(15L)).build((CacheLoader)new CacheLoader<CacheKey, CompletableFuture<Optional<PlayerSkin>>>(this){
            final /* synthetic */ SkinManager this$0;
            {
                SkinManager skinManager = this$0;
                Objects.requireNonNull(skinManager);
                this.this$0 = skinManager;
            }

            public CompletableFuture<Optional<PlayerSkin>> load(CacheKey key) {
                return ((CompletableFuture)CompletableFuture.supplyAsync(() -> {
                    Property packedTextures = key.packedTextures();
                    if (packedTextures == null) {
                        return MinecraftProfileTextures.EMPTY;
                    }
                    MinecraftProfileTextures textures = services.sessionService().unpackTextures(packedTextures);
                    if (textures.signatureState() == SignatureState.INVALID) {
                        LOGGER.warn("Profile contained invalid signature for textures property (profile id: {})", (Object)key.profileId());
                    }
                    return textures;
                }, Util.backgroundExecutor().forName("unpackSkinTextures")).thenComposeAsync(textures -> this.this$0.registerTextures(key.profileId(), (MinecraftProfileTextures)textures), mainThreadExecutor)).handle((playerSkin, throwable) -> {
                    if (throwable != null) {
                        LOGGER.warn("Failed to load texture for profile {}", (Object)key.profileId, throwable);
                    }
                    return Optional.ofNullable(playerSkin);
                });
            }
        });
    }

    public Supplier<PlayerSkin> createLookup(GameProfile profile, boolean requireSecure) {
        CompletableFuture<Optional<PlayerSkin>> future = this.get(profile);
        PlayerSkin defaultSkin = DefaultPlayerSkin.get(profile);
        if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
            return () -> defaultSkin;
        }
        Optional currentValue = future.getNow(null);
        if (currentValue != null) {
            PlayerSkin playerSkin = currentValue.filter(skin -> !requireSecure || skin.secure()).orElse(defaultSkin);
            return () -> playerSkin;
        }
        return () -> future.getNow(Optional.empty()).filter(skin -> !requireSecure || skin.secure()).orElse(defaultSkin);
    }

    public CompletableFuture<Optional<PlayerSkin>> get(GameProfile profile) {
        if (SharedConstants.DEBUG_DEFAULT_SKIN_OVERRIDE) {
            PlayerSkin defaultSkin = DefaultPlayerSkin.get(profile);
            return CompletableFuture.completedFuture(Optional.of(defaultSkin));
        }
        Property packedTextures = this.services.sessionService().getPackedTextures(profile);
        return (CompletableFuture)this.skinCache.getUnchecked((Object)new CacheKey(profile.id(), packedTextures));
    }

    private CompletableFuture<PlayerSkin> registerTextures(UUID profileId, MinecraftProfileTextures textures) {
        PlayerModelType model;
        CompletableFuture<ClientAsset.Texture> skinTexture;
        MinecraftProfileTexture skinInfo = textures.skin();
        if (skinInfo != null) {
            skinTexture = this.skinTextures.getOrLoad(skinInfo);
            model = PlayerModelType.byLegacyServicesName(skinInfo.getMetadata("model"));
        } else {
            PlayerSkin defaultSkin = DefaultPlayerSkin.get(profileId);
            skinTexture = CompletableFuture.completedFuture(defaultSkin.body());
            model = defaultSkin.model();
        }
        MinecraftProfileTexture capeInfo = textures.cape();
        CompletableFuture<Object> capeTexture = capeInfo != null ? this.capeTextures.getOrLoad(capeInfo) : CompletableFuture.completedFuture(null);
        MinecraftProfileTexture elytraInfo = textures.elytra();
        CompletableFuture<Object> elytraTexture = elytraInfo != null ? this.elytraTextures.getOrLoad(elytraInfo) : CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(skinTexture, capeTexture, elytraTexture).thenApply(unused -> new PlayerSkin((ClientAsset.Texture)skinTexture.join(), (ClientAsset.Texture)capeTexture.join(), (ClientAsset.Texture)elytraTexture.join(), model, textures.signatureState() == SignatureState.SIGNED));
    }

    @Environment(value=EnvType.CLIENT)
    private class TextureCache {
        private final Path root;
        private final MinecraftProfileTexture.Type type;
        private final Map<String, CompletableFuture<ClientAsset.Texture>> textures;
        final /* synthetic */ SkinManager this$0;

        private TextureCache(SkinManager skinManager, Path root, MinecraftProfileTexture.Type type) {
            SkinManager skinManager2 = skinManager;
            Objects.requireNonNull(skinManager2);
            this.this$0 = skinManager2;
            this.textures = new Object2ObjectOpenHashMap();
            this.root = root;
            this.type = type;
        }

        public CompletableFuture<ClientAsset.Texture> getOrLoad(MinecraftProfileTexture texture) {
            String hash = texture.getHash();
            CompletableFuture<ClientAsset.Texture> future = this.textures.get(hash);
            if (future == null) {
                future = this.registerTexture(texture);
                this.textures.put(hash, future);
            }
            return future;
        }

        private CompletableFuture<ClientAsset.Texture> registerTexture(MinecraftProfileTexture textureInfo) {
            String hash = Hashing.sha1().hashUnencodedChars((CharSequence)textureInfo.getHash()).toString();
            Identifier textureId = this.getTextureLocation(hash);
            Path file = this.root.resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx").resolve(hash);
            return this.this$0.skinTextureDownloader.downloadAndRegisterSkin(textureId, file, textureInfo.getUrl(), this.type == MinecraftProfileTexture.Type.SKIN);
        }

        private Identifier getTextureLocation(String textureHash) {
            String root = switch (this.type) {
                default -> throw new MatchException(null, null);
                case MinecraftProfileTexture.Type.SKIN -> "skins";
                case MinecraftProfileTexture.Type.CAPE -> "capes";
                case MinecraftProfileTexture.Type.ELYTRA -> "elytra";
            };
            return Identifier.withDefaultNamespace(root + "/" + textureHash);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record CacheKey(UUID profileId, @Nullable Property packedTextures) {
    }
}

