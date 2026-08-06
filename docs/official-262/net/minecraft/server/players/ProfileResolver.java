/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.cache.CacheBuilder
 *  com.google.common.cache.CacheLoader
 *  com.google.common.cache.LoadingCache
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.minecraft.MinecraftSessionService
 *  com.mojang.authlib.yggdrasil.ProfileResult
 *  com.mojang.datafixers.util.Either
 */
package net.minecraft.server.players;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Either;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.StringUtil;

public interface ProfileResolver {
    public Optional<GameProfile> fetchByName(String var1);

    public Optional<GameProfile> fetchById(UUID var1);

    default public Optional<GameProfile> fetchByNameOrId(Either<String, UUID> nameOrId) {
        return (Optional)nameOrId.map(this::fetchByName, this::fetchById);
    }

    public static class Cached
    implements ProfileResolver {
        private final LoadingCache<String, Optional<GameProfile>> profileCacheByName;
        private final LoadingCache<UUID, Optional<GameProfile>> profileCacheById;

        public Cached(final MinecraftSessionService sessionService, final UserNameToIdResolver nameToIdCache) {
            this.profileCacheById = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(10L)).maximumSize(256L).build((CacheLoader)new CacheLoader<UUID, Optional<GameProfile>>(this){
                {
                    Objects.requireNonNull(this$0);
                }

                public Optional<GameProfile> load(UUID profileId) {
                    ProfileResult result = sessionService.fetchProfile(profileId, true);
                    return Optional.ofNullable(result).map(ProfileResult::profile);
                }
            });
            this.profileCacheByName = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(10L)).maximumSize(256L).build((CacheLoader)new CacheLoader<String, Optional<GameProfile>>(this){
                final /* synthetic */ Cached this$0;
                {
                    Cached cached = this$0;
                    Objects.requireNonNull(cached);
                    this.this$0 = cached;
                }

                public Optional<GameProfile> load(String name) {
                    return nameToIdCache.get(name).flatMap(nameAndId -> (Optional)this.this$0.profileCacheById.getUnchecked((Object)nameAndId.id()));
                }
            });
        }

        @Override
        public Optional<GameProfile> fetchByName(String name) {
            if (StringUtil.isValidPlayerName(name)) {
                return (Optional)this.profileCacheByName.getUnchecked((Object)name);
            }
            return Optional.empty();
        }

        @Override
        public Optional<GameProfile> fetchById(UUID id) {
            return (Optional)this.profileCacheById.getUnchecked((Object)id);
        }
    }
}

