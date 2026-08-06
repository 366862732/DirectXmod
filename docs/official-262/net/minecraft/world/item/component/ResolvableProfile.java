/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.PropertyMap
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.component;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public abstract sealed class ResolvableProfile
implements TooltipProvider {
    private static final Codec<ResolvableProfile> FULL_CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.mapEither(ExtraCodecs.STORED_GAME_PROFILE, Partial.MAP_CODEC).forGetter(ResolvableProfile::unpack), (App)PlayerSkin.Patch.MAP_CODEC.forGetter(ResolvableProfile::skinPatch)).apply((Applicative)i, ResolvableProfile::create));
    public static final Codec<ResolvableProfile> CODEC = Codec.withAlternative(FULL_CODEC, ExtraCodecs.PLAYER_NAME, ResolvableProfile::createUnresolved);
    public static final StreamCodec<ByteBuf, ResolvableProfile> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.either(ByteBufCodecs.GAME_PROFILE, Partial.STREAM_CODEC), ResolvableProfile::unpack, PlayerSkin.Patch.STREAM_CODEC, ResolvableProfile::skinPatch, ResolvableProfile::create);
    protected final GameProfile partialProfile;
    protected final PlayerSkin.Patch skinPatch;

    private static ResolvableProfile create(Either<GameProfile, Partial> value, PlayerSkin.Patch patch) {
        return (ResolvableProfile)value.map(full -> new Static((Either<GameProfile, Partial>)Either.left((Object)full), patch), partial -> {
            if (!partial.properties.isEmpty() || partial.id.isPresent() == partial.name.isPresent()) {
                return new Static((Either<GameProfile, Partial>)Either.right((Object)partial), patch);
            }
            return partial.name.map(s -> new Dynamic((Either<String, UUID>)Either.left((Object)s), patch)).orElseGet(() -> new Dynamic((Either<String, UUID>)Either.right((Object)partial.id.get()), patch));
        });
    }

    public static ResolvableProfile createResolved(GameProfile gameProfile) {
        return new Static((Either<GameProfile, Partial>)Either.left((Object)gameProfile), PlayerSkin.Patch.EMPTY);
    }

    public static ResolvableProfile createUnresolved(String name) {
        return new Dynamic((Either<String, UUID>)Either.left((Object)name), PlayerSkin.Patch.EMPTY);
    }

    public static ResolvableProfile createUnresolved(UUID id) {
        return new Dynamic((Either<String, UUID>)Either.right((Object)id), PlayerSkin.Patch.EMPTY);
    }

    protected abstract Either<GameProfile, Partial> unpack();

    protected ResolvableProfile(GameProfile partialProfile, PlayerSkin.Patch skinPatch) {
        this.partialProfile = partialProfile;
        this.skinPatch = skinPatch;
    }

    public abstract CompletableFuture<GameProfile> resolveProfile(ProfileResolver var1);

    public GameProfile partialProfile() {
        return this.partialProfile;
    }

    public PlayerSkin.Patch skinPatch() {
        return this.skinPatch;
    }

    private static GameProfile createPartialProfile(Optional<String> maybeName, Optional<UUID> maybeId, PropertyMap properties) {
        String name = maybeName.orElse("");
        UUID id = maybeId.orElseGet(() -> maybeName.map(UUIDUtil::createOfflinePlayerUUID).orElse(Util.NIL_UUID));
        return new GameProfile(id, name, properties);
    }

    public abstract Optional<String> name();

    public static final class Static
    extends ResolvableProfile {
        public static final Static EMPTY = new Static((Either<GameProfile, Partial>)Either.right((Object)Partial.EMPTY), PlayerSkin.Patch.EMPTY);
        private final Either<GameProfile, Partial> contents;

        private Static(Either<GameProfile, Partial> contents, PlayerSkin.Patch skinPatch) {
            super((GameProfile)contents.map(gameProfile -> gameProfile, Partial::createProfile), skinPatch);
            this.contents = contents;
        }

        @Override
        public CompletableFuture<GameProfile> resolveProfile(ProfileResolver profileResolver) {
            return CompletableFuture.completedFuture(this.partialProfile);
        }

        @Override
        protected Either<GameProfile, Partial> unpack() {
            return this.contents;
        }

        @Override
        public Optional<String> name() {
            return (Optional)this.contents.map(gameProfile -> Optional.of(gameProfile.name()), partial -> partial.name);
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Static)) return false;
            Static that = (Static)o;
            if (!this.contents.equals(that.contents)) return false;
            if (!this.skinPatch.equals(that.skinPatch)) return false;
            return true;
        }

        public int hashCode() {
            int result = 31 + this.contents.hashCode();
            result = 31 * result + this.skinPatch.hashCode();
            return result;
        }

        @Override
        public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        }
    }

    public static final class Dynamic
    extends ResolvableProfile {
        private static final Component DYNAMIC_TOOLTIP = Component.translatable("component.profile.dynamic").withStyle(ChatFormatting.GRAY);
        private final Either<String, UUID> nameOrId;

        private Dynamic(Either<String, UUID> nameOrId, PlayerSkin.Patch skinPatch) {
            super(ResolvableProfile.createPartialProfile(nameOrId.left(), nameOrId.right(), PropertyMap.EMPTY), skinPatch);
            this.nameOrId = nameOrId;
        }

        @Override
        public Optional<String> name() {
            return this.nameOrId.left();
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dynamic)) return false;
            Dynamic that = (Dynamic)o;
            if (!this.nameOrId.equals(that.nameOrId)) return false;
            if (!this.skinPatch.equals(that.skinPatch)) return false;
            return true;
        }

        public int hashCode() {
            int result = 31 + this.nameOrId.hashCode();
            result = 31 * result + this.skinPatch.hashCode();
            return result;
        }

        @Override
        protected Either<GameProfile, Partial> unpack() {
            return Either.right((Object)new Partial(this.nameOrId.left(), this.nameOrId.right(), PropertyMap.EMPTY));
        }

        @Override
        public CompletableFuture<GameProfile> resolveProfile(ProfileResolver profileResolver) {
            return CompletableFuture.supplyAsync(() -> profileResolver.fetchByNameOrId(this.nameOrId).orElse(this.partialProfile), Util.nonCriticalIoPool());
        }

        @Override
        public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
            consumer.accept(DYNAMIC_TOOLTIP);
        }
    }

    protected record Partial(Optional<String> name, Optional<UUID> id, PropertyMap properties) {
        public static final Partial EMPTY = new Partial(Optional.empty(), Optional.empty(), PropertyMap.EMPTY);
        private static final MapCodec<Partial> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.PLAYER_NAME.optionalFieldOf("name").forGetter(Partial::name), (App)UUIDUtil.CODEC.optionalFieldOf("id").forGetter(Partial::id), (App)ExtraCodecs.PROPERTY_MAP.optionalFieldOf("properties", (Object)PropertyMap.EMPTY).forGetter(Partial::properties)).apply((Applicative)i, Partial::new));
        public static final StreamCodec<ByteBuf, Partial> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.PLAYER_NAME.apply(ByteBufCodecs::optional), Partial::name, UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), Partial::id, ByteBufCodecs.GAME_PROFILE_PROPERTIES, Partial::properties, Partial::new);

        private GameProfile createProfile() {
            return ResolvableProfile.createPartialProfile(this.name, this.id, this.properties);
        }
    }
}

