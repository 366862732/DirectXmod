/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.jsonrpc.methods;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class BanlistService {
    private static final String BAN_SOURCE = "Management server";

    public static List<UserBanDto> get(MinecraftApi minecraftApi) {
        return minecraftApi.banListService().getUserBanEntries().stream().filter(p -> p.getUser() != null).map(UserBan::from).map(UserBanDto::from).toList();
    }

    public static List<UserBanDto> add(MinecraftApi minecraftApi, List<UserBanDto> bans, ClientInfo clientInfo) {
        List<CompletableFuture> fetch = bans.stream().map(ban -> minecraftApi.playerListService().getUser(ban.player().id(), ban.player().name()).thenApply(u -> u.map(ban::toUserBan))).toList();
        for (Optional ban2 : Util.sequence(fetch).join()) {
            if (ban2.isEmpty()) continue;
            UserBan userBan = (UserBan)ban2.get();
            minecraftApi.banListService().addUserBan(userBan.toBanEntry(), clientInfo);
            ServerPlayer player = minecraftApi.playerListService().getPlayer(((UserBan)ban2.get()).player().id());
            if (player == null) continue;
            player.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
        }
        return BanlistService.get(minecraftApi);
    }

    public static List<UserBanDto> clear(MinecraftApi minecraftApi, ClientInfo clientInfo) {
        minecraftApi.banListService().clearUserBans(clientInfo);
        return BanlistService.get(minecraftApi);
    }

    public static List<UserBanDto> remove(MinecraftApi minecraftApi, List<PlayerDto> remove, ClientInfo clientInfo) {
        List<CompletableFuture> fetch = remove.stream().map(playerDto -> minecraftApi.playerListService().getUser(playerDto.id(), playerDto.name())).toList();
        for (Optional user : Util.sequence(fetch).join()) {
            if (user.isEmpty()) continue;
            minecraftApi.banListService().removeUserBan((NameAndId)user.get(), clientInfo);
        }
        return BanlistService.get(minecraftApi);
    }

    public static List<UserBanDto> set(MinecraftApi minecraftApi, List<UserBanDto> bans, ClientInfo clientInfo) {
        List<CompletableFuture> fetch = bans.stream().map(ban -> minecraftApi.playerListService().getUser(ban.player().id(), ban.player().name()).thenApply(u -> u.map(ban::toUserBan))).toList();
        Set finalAllowList = Util.sequence(fetch).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
        Set currentAllowList = minecraftApi.banListService().getUserBanEntries().stream().filter(entry -> entry.getUser() != null).map(UserBan::from).collect(Collectors.toSet());
        currentAllowList.stream().filter(ban -> !finalAllowList.contains(ban)).forEach(ban -> minecraftApi.banListService().removeUserBan(ban.player(), clientInfo));
        finalAllowList.stream().filter(ban -> !currentAllowList.contains(ban)).forEach(ban -> {
            minecraftApi.banListService().addUserBan(ban.toBanEntry(), clientInfo);
            ServerPlayer player = minecraftApi.playerListService().getPlayer(ban.player().id());
            if (player != null) {
                player.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
        });
        return BanlistService.get(minecraftApi);
    }

    private record UserBan(NameAndId player, @Nullable String reason, String source, Optional<Instant> expires) {
        private static UserBan from(UserBanListEntry entry) {
            return new UserBan(Objects.requireNonNull((NameAndId)entry.getUser()), entry.getReason(), entry.getSource(), Optional.ofNullable(entry.getExpires()).map(Date::toInstant));
        }

        private UserBanListEntry toBanEntry() {
            return new UserBanListEntry(new NameAndId(this.player().id(), this.player().name()), null, this.source(), (Date)this.expires().map(Date::from).orElse(null), this.reason());
        }
    }

    public record UserBanDto(PlayerDto player, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
        public static final MapCodec<UserBanDto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)PlayerDto.CODEC.codec().fieldOf("player").forGetter(UserBanDto::player), (App)Codec.STRING.optionalFieldOf("reason").forGetter(UserBanDto::reason), (App)Codec.STRING.optionalFieldOf("source").forGetter(UserBanDto::source), (App)ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(UserBanDto::expires)).apply((Applicative)i, UserBanDto::new));

        private static UserBanDto from(UserBan ban) {
            return new UserBanDto(PlayerDto.from(ban.player()), Optional.ofNullable(ban.reason()), Optional.of(ban.source()), ban.expires());
        }

        public static UserBanDto from(UserBanListEntry entry) {
            return UserBanDto.from(UserBan.from(entry));
        }

        private UserBan toUserBan(NameAndId nameAndId) {
            return new UserBan(nameAndId, this.reason().orElse(null), this.source().orElse(BanlistService.BAN_SOURCE), this.expires());
        }
    }
}

