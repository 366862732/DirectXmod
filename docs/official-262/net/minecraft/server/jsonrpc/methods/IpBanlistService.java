/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.net.InetAddresses
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.jsonrpc.methods;

import com.google.common.net.InetAddresses;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class IpBanlistService {
    private static final String BAN_SOURCE = "Management server";

    public static List<IpBanDto> get(MinecraftApi minecraftApi) {
        return minecraftApi.banListService().getIpBanEntries().stream().map(IpBan::from).map(IpBanDto::from).toList();
    }

    public static List<IpBanDto> add(MinecraftApi minecraftApi, List<IncomingIpBanDto> bans, ClientInfo clientInfo) {
        bans.stream().map(ban -> IpBanlistService.banIp(minecraftApi, ban, clientInfo)).flatMap(Collection::stream).forEach(player -> player.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
        return IpBanlistService.get(minecraftApi);
    }

    private static List<ServerPlayer> banIp(MinecraftApi minecraftApi, IncomingIpBanDto ban, ClientInfo clientInfo) {
        Optional<ServerPlayer> player;
        IpBan ipBan = ban.toIpBan();
        if (ipBan != null) {
            return IpBanlistService.banIp(minecraftApi, ipBan, clientInfo);
        }
        if (ban.player().isPresent() && (player = minecraftApi.playerListService().getPlayer(ban.player().get().id(), ban.player().get().name())).isPresent()) {
            return IpBanlistService.banIp(minecraftApi, ban.toIpBan(player.get()), clientInfo);
        }
        return List.of();
    }

    private static List<ServerPlayer> banIp(MinecraftApi minecraftApi, IpBan ban, ClientInfo clientInfo) {
        minecraftApi.banListService().addIpBan(ban.toIpBanEntry(), clientInfo);
        return minecraftApi.playerListService().getPlayersWithAddress(ban.ip());
    }

    public static List<IpBanDto> clear(MinecraftApi minecraftApi, ClientInfo clientInfo) {
        minecraftApi.banListService().clearIpBans(clientInfo);
        return IpBanlistService.get(minecraftApi);
    }

    public static List<IpBanDto> remove(MinecraftApi minecraftApi, List<String> ban, ClientInfo clientInfo) {
        ban.forEach(ip -> minecraftApi.banListService().removeIpBan((String)ip, clientInfo));
        return IpBanlistService.get(minecraftApi);
    }

    public static List<IpBanDto> set(MinecraftApi minecraftApi, List<IpBanDto> ips, ClientInfo clientInfo) {
        Set finalBanlist = ips.stream().filter(ban -> InetAddresses.isInetAddress((String)ban.ip())).map(IpBanDto::toIpBan).collect(Collectors.toSet());
        Set currentBans = minecraftApi.banListService().getIpBanEntries().stream().map(IpBan::from).collect(Collectors.toSet());
        currentBans.stream().filter(ban -> !finalBanlist.contains(ban)).forEach(ban -> minecraftApi.banListService().removeIpBan(ban.ip(), clientInfo));
        finalBanlist.stream().filter(ban -> !currentBans.contains(ban)).forEach(ban -> minecraftApi.banListService().addIpBan(ban.toIpBanEntry(), clientInfo));
        finalBanlist.stream().filter(ban -> !currentBans.contains(ban)).flatMap(ban -> minecraftApi.playerListService().getPlayersWithAddress(ban.ip()).stream()).forEach(player -> player.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
        return IpBanlistService.get(minecraftApi);
    }

    public record IncomingIpBanDto(Optional<PlayerDto> player, Optional<String> ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
        public static final MapCodec<IncomingIpBanDto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)PlayerDto.CODEC.codec().optionalFieldOf("player").forGetter(IncomingIpBanDto::player), (App)Codec.STRING.optionalFieldOf("ip").forGetter(IncomingIpBanDto::ip), (App)Codec.STRING.optionalFieldOf("reason").forGetter(IncomingIpBanDto::reason), (App)Codec.STRING.optionalFieldOf("source").forGetter(IncomingIpBanDto::source), (App)ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IncomingIpBanDto::expires)).apply((Applicative)i, IncomingIpBanDto::new));

        private IpBan toIpBan(ServerPlayer player) {
            return new IpBan(player.getIpAddress(), this.reason().orElse(null), this.source().orElse(IpBanlistService.BAN_SOURCE), this.expires());
        }

        private @Nullable IpBan toIpBan() {
            if (this.ip().isEmpty() || !InetAddresses.isInetAddress((String)this.ip().get())) {
                return null;
            }
            return new IpBan(this.ip().get(), this.reason().orElse(null), this.source().orElse(IpBanlistService.BAN_SOURCE), this.expires());
        }
    }

    private record IpBan(String ip, @Nullable String reason, String source, Optional<Instant> expires) {
        private static IpBan from(IpBanListEntry entry) {
            return new IpBan(Objects.requireNonNull((String)entry.getUser()), entry.getReason(), entry.getSource(), Optional.ofNullable(entry.getExpires()).map(Date::toInstant));
        }

        private IpBanListEntry toIpBanEntry() {
            return new IpBanListEntry(this.ip(), null, this.source(), (Date)this.expires().map(Date::from).orElse(null), this.reason());
        }
    }

    public record IpBanDto(String ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
        public static final MapCodec<IpBanDto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("ip").forGetter(IpBanDto::ip), (App)Codec.STRING.optionalFieldOf("reason").forGetter(IpBanDto::reason), (App)Codec.STRING.optionalFieldOf("source").forGetter(IpBanDto::source), (App)ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanDto::expires)).apply((Applicative)i, IpBanDto::new));

        private static IpBanDto from(IpBan ban) {
            return new IpBanDto(ban.ip(), Optional.ofNullable(ban.reason()), Optional.of(ban.source()), ban.expires());
        }

        public static IpBanDto from(IpBanListEntry ban) {
            return IpBanDto.from(IpBan.from(ban));
        }

        private IpBan toIpBan() {
            return new IpBan(this.ip(), this.reason().orElse(null), this.source().orElse(IpBanlistService.BAN_SOURCE), this.expires());
        }
    }
}

