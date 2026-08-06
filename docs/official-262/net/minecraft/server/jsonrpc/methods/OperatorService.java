/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.jsonrpc.methods;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.util.Util;

public class OperatorService {
    public static List<OperatorDto> get(MinecraftApi minecraftApi) {
        return minecraftApi.operatorListService().getEntries().stream().filter(u -> u.getUser() != null).map(OperatorDto::from).toList();
    }

    public static List<OperatorDto> clear(MinecraftApi minecraftApi, ClientInfo clientInfo) {
        minecraftApi.operatorListService().clear(clientInfo);
        return OperatorService.get(minecraftApi);
    }

    public static List<OperatorDto> remove(MinecraftApi minecraftApi, List<PlayerDto> playerDtos, ClientInfo clientInfo) {
        List<CompletableFuture> fetch = playerDtos.stream().map(playerDto -> minecraftApi.playerListService().getUser(playerDto.id(), playerDto.name())).toList();
        for (Optional user : Util.sequence(fetch).join()) {
            user.ifPresent(nameAndId -> minecraftApi.operatorListService().deop((NameAndId)nameAndId, clientInfo));
        }
        return OperatorService.get(minecraftApi);
    }

    public static List<OperatorDto> add(MinecraftApi minecraftApi, List<OperatorDto> operators, ClientInfo clientInfo) {
        List<CompletableFuture> fetch = operators.stream().map(operator -> minecraftApi.playerListService().getUser(operator.player().id(), operator.player().name()).thenApply(user -> user.map(nameAndId -> new Op((NameAndId)nameAndId, operator.permissionLevel(), operator.bypassesPlayerLimit())))).toList();
        for (Optional op : Util.sequence(fetch).join()) {
            op.ifPresent(operator -> minecraftApi.operatorListService().op(operator.user(), operator.permissionLevel(), operator.bypassesPlayerLimit(), clientInfo));
        }
        return OperatorService.get(minecraftApi);
    }

    public static List<OperatorDto> set(MinecraftApi minecraftApi, List<OperatorDto> operators, ClientInfo clientInfo) {
        List<CompletableFuture> fetch = operators.stream().map(operator -> minecraftApi.playerListService().getUser(operator.player().id(), operator.player().name()).thenApply(user -> user.map(nameAndId -> new Op((NameAndId)nameAndId, operator.permissionLevel(), operator.bypassesPlayerLimit())))).toList();
        Set finalOperators = Util.sequence(fetch).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
        Set currentOperators = minecraftApi.operatorListService().getEntries().stream().filter(entry -> entry.getUser() != null).map(entry -> new Op((NameAndId)entry.getUser(), Optional.of(entry.permissions().level()), Optional.of(entry.getBypassesPlayerLimit()))).collect(Collectors.toSet());
        currentOperators.stream().filter(operator -> !finalOperators.contains(operator)).forEach(operator -> minecraftApi.operatorListService().deop(operator.user(), clientInfo));
        finalOperators.stream().filter(operator -> !currentOperators.contains(operator)).forEach(operator -> minecraftApi.operatorListService().op(operator.user(), operator.permissionLevel(), operator.bypassesPlayerLimit(), clientInfo));
        return OperatorService.get(minecraftApi);
    }

    private record Op(NameAndId user, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
    }

    public record OperatorDto(PlayerDto player, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
        public static final MapCodec<OperatorDto> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)PlayerDto.CODEC.codec().fieldOf("player").forGetter(OperatorDto::player), (App)PermissionLevel.INT_CODEC.optionalFieldOf("permissionLevel").forGetter(OperatorDto::permissionLevel), (App)Codec.BOOL.optionalFieldOf("bypassesPlayerLimit").forGetter(OperatorDto::bypassesPlayerLimit)).apply((Applicative)i, OperatorDto::new));

        public static OperatorDto from(ServerOpListEntry serverOpListEntry) {
            return new OperatorDto(PlayerDto.from(Objects.requireNonNull((NameAndId)serverOpListEntry.getUser())), Optional.of(serverOpListEntry.permissions().level()), Optional.of(serverOpListEntry.getBypassesPlayerLimit()));
        }
    }
}

