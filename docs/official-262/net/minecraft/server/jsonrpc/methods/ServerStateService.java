/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.jsonrpc.methods;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.Message;
import net.minecraft.server.jsonrpc.methods.PlayerService;
import net.minecraft.server.level.ServerPlayer;

public class ServerStateService {
    public static ServerState status(MinecraftApi minecraftApi) {
        if (!minecraftApi.serverStateService().isReady()) {
            return ServerState.NOT_STARTED;
        }
        return new ServerState(true, PlayerService.get(minecraftApi), ServerStatus.Version.current());
    }

    public static boolean save(MinecraftApi minecraftApi, boolean flush, ClientInfo clientInfo) {
        return minecraftApi.serverStateService().saveEverything(true, flush, true, clientInfo);
    }

    public static boolean stop(MinecraftApi minecraftApi, ClientInfo clientInfo) {
        minecraftApi.submit(() -> minecraftApi.serverStateService().halt(false, clientInfo));
        return true;
    }

    public static boolean systemMessage(MinecraftApi minecraftApi, SystemMessage systemMessage, ClientInfo clientInfo) {
        Component component = systemMessage.message().asComponent().orElse(null);
        if (component == null) {
            return false;
        }
        if (systemMessage.receivingPlayers().isPresent()) {
            if (systemMessage.receivingPlayers().get().isEmpty()) {
                return false;
            }
            for (PlayerDto playerDto : systemMessage.receivingPlayers().get()) {
                ServerPlayer player;
                if (playerDto.id().isPresent()) {
                    player = minecraftApi.playerListService().getPlayer(playerDto.id().get());
                } else {
                    if (!playerDto.name().isPresent()) continue;
                    player = minecraftApi.playerListService().getPlayerByName(playerDto.name().get());
                }
                if (player == null) continue;
                player.sendSystemMessage(component, systemMessage.overlay());
            }
        } else {
            minecraftApi.serverStateService().broadcastSystemMessage(component, systemMessage.overlay(), clientInfo);
        }
        return true;
    }

    public record ServerState(boolean started, List<PlayerDto> players, ServerStatus.Version version) {
        public static final Codec<ServerState> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.fieldOf("started").forGetter(ServerState::started), (App)PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("players", List.of()).forGetter(ServerState::players), (App)ServerStatus.Version.CODEC.fieldOf("version").forGetter(ServerState::version)).apply((Applicative)i, ServerState::new));
        public static final ServerState NOT_STARTED = new ServerState(false, List.of(), ServerStatus.Version.current());
    }

    public record SystemMessage(Message message, boolean overlay, Optional<List<PlayerDto>> receivingPlayers) {
        public static final Codec<SystemMessage> CODEC = RecordCodecBuilder.create(i -> i.group((App)Message.CODEC.fieldOf("message").forGetter(SystemMessage::message), (App)Codec.BOOL.fieldOf("overlay").forGetter(SystemMessage::overlay), (App)PlayerDto.CODEC.codec().listOf().lenientOptionalFieldOf("receivingPlayers").forGetter(SystemMessage::receivingPlayers)).apply((Applicative)i, SystemMessage::new));
    }
}

