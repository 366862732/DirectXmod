/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PlayerSkinRenderCache {
    public static final RenderType DEFAULT_PLAYER_SKIN_RENDER_TYPE = PlayerSkinRenderCache.playerSkinRenderType(DefaultPlayerSkin.getDefaultSkin());
    public static final Duration CACHE_DURATION = Duration.ofMinutes(5L);
    private final LoadingCache<ResolvableProfile, CompletableFuture<Optional<RenderInfo>>> renderInfoCache = CacheBuilder.newBuilder().expireAfterAccess(CACHE_DURATION).build((CacheLoader)new CacheLoader<ResolvableProfile, CompletableFuture<Optional<RenderInfo>>>(this){
        final /* synthetic */ PlayerSkinRenderCache this$0;
        {
            PlayerSkinRenderCache playerSkinRenderCache = this$0;
            Objects.requireNonNull(playerSkinRenderCache);
            this.this$0 = playerSkinRenderCache;
        }

        public CompletableFuture<Optional<RenderInfo>> load(ResolvableProfile profile) {
            return profile.resolveProfile(this.this$0.profileResolver).thenCompose(resolvedProfile -> this.this$0.skinManager.get((GameProfile)resolvedProfile).thenApply(playerSkin -> playerSkin.map(skin -> new RenderInfo(this.this$0, (GameProfile)resolvedProfile, (PlayerSkin)skin, profile.skinPatch()))));
        }
    });
    private final LoadingCache<ResolvableProfile, RenderInfo> defaultSkinCache = CacheBuilder.newBuilder().expireAfterAccess(CACHE_DURATION).build((CacheLoader)new CacheLoader<ResolvableProfile, RenderInfo>(this){
        final /* synthetic */ PlayerSkinRenderCache this$0;
        {
            PlayerSkinRenderCache playerSkinRenderCache = this$0;
            Objects.requireNonNull(playerSkinRenderCache);
            this.this$0 = playerSkinRenderCache;
        }

        public RenderInfo load(ResolvableProfile profile) {
            GameProfile temporaryProfile = profile.partialProfile();
            return new RenderInfo(this.this$0, temporaryProfile, DefaultPlayerSkin.get(temporaryProfile), profile.skinPatch());
        }
    });
    private final TextureManager textureManager;
    private final SkinManager skinManager;
    private final ProfileResolver profileResolver;

    public PlayerSkinRenderCache(TextureManager textureManager, SkinManager skinManager, ProfileResolver profileResolver) {
        this.textureManager = textureManager;
        this.skinManager = skinManager;
        this.profileResolver = profileResolver;
    }

    public RenderInfo getOrDefault(ResolvableProfile profile) {
        RenderInfo result = this.lookup(profile).getNow(Optional.empty()).orElse(null);
        if (result != null) {
            return result;
        }
        return (RenderInfo)this.defaultSkinCache.getUnchecked((Object)profile);
    }

    public Supplier<RenderInfo> createLookup(ResolvableProfile profile) {
        RenderInfo defaultForProfile = (RenderInfo)this.defaultSkinCache.getUnchecked((Object)profile);
        CompletableFuture future = (CompletableFuture)this.renderInfoCache.getUnchecked((Object)profile);
        Optional currentValue = future.getNow(null);
        if (currentValue != null) {
            RenderInfo finalValue = currentValue.orElse(defaultForProfile);
            return () -> finalValue;
        }
        return () -> future.getNow(Optional.empty()).orElse(defaultForProfile);
    }

    public CompletableFuture<Optional<RenderInfo>> lookup(ResolvableProfile profile) {
        return (CompletableFuture)this.renderInfoCache.getUnchecked((Object)profile);
    }

    private static RenderType playerSkinRenderType(PlayerSkin playerSkin) {
        return SkullBlockRenderer.getPlayerSkinRenderType(playerSkin.body().texturePath());
    }

    @Environment(value=EnvType.CLIENT)
    public final class RenderInfo {
        private final GameProfile gameProfile;
        private final PlayerSkin playerSkin;
        private @Nullable RenderType itemRenderType;
        private @Nullable GpuTextureView textureView;
        private @Nullable GlyphRenderTypes glyphRenderTypes;
        final /* synthetic */ PlayerSkinRenderCache this$0;

        public RenderInfo(PlayerSkinRenderCache this$0, GameProfile gameProfile, PlayerSkin playerSkin, PlayerSkin.Patch patch) {
            PlayerSkinRenderCache playerSkinRenderCache = this$0;
            Objects.requireNonNull(playerSkinRenderCache);
            this.this$0 = playerSkinRenderCache;
            this.gameProfile = gameProfile;
            this.playerSkin = playerSkin.with(patch);
        }

        public GameProfile gameProfile() {
            return this.gameProfile;
        }

        public PlayerSkin playerSkin() {
            return this.playerSkin;
        }

        public RenderType renderType() {
            if (this.itemRenderType == null) {
                this.itemRenderType = PlayerSkinRenderCache.playerSkinRenderType(this.playerSkin);
            }
            return this.itemRenderType;
        }

        public GpuTextureView textureView() {
            if (this.textureView == null) {
                this.textureView = this.this$0.textureManager.getTexture(this.playerSkin.body().texturePath()).getTextureView();
            }
            return this.textureView;
        }

        public GlyphRenderTypes glyphRenderTypes() {
            if (this.glyphRenderTypes == null) {
                this.glyphRenderTypes = GlyphRenderTypes.createForColorTexture(this.playerSkin.body().texturePath());
            }
            return this.glyphRenderTypes;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RenderInfo)) return false;
            RenderInfo that = (RenderInfo)o;
            if (!this.gameProfile.equals((Object)that.gameProfile)) return false;
            if (!this.playerSkin.equals(that.playerSkin)) return false;
            return true;
        }

        public int hashCode() {
            int result = 1;
            result = 31 * result + this.gameProfile.hashCode();
            result = 31 * result + this.playerSkin.hashCode();
            return result;
        }
    }
}

