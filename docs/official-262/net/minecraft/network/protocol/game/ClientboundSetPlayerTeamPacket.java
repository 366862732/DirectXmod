/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

public class ClientboundSetPlayerTeamPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSetPlayerTeamPacket> STREAM_CODEC = Packet.codec(ClientboundSetPlayerTeamPacket::write, ClientboundSetPlayerTeamPacket::new);
    private static final int METHOD_ADD = 0;
    private static final int METHOD_REMOVE = 1;
    private static final int METHOD_CHANGE = 2;
    private static final int METHOD_JOIN = 3;
    private static final int METHOD_LEAVE = 4;
    private final int method;
    private final String name;
    private final Collection<String> players;
    private final Optional<Parameters> parameters;

    private ClientboundSetPlayerTeamPacket(String name, int method, Optional<Parameters> parameters, Collection<String> players) {
        this.name = name;
        this.method = method;
        this.parameters = parameters;
        this.players = ImmutableList.copyOf(players);
    }

    public static ClientboundSetPlayerTeamPacket createAddOrModifyPacket(PlayerTeam team, boolean createNew) {
        return new ClientboundSetPlayerTeamPacket(team.getName(), createNew ? 0 : 2, Optional.of(new Parameters(team)), createNew ? team.getPlayers() : ImmutableList.of());
    }

    public static ClientboundSetPlayerTeamPacket createRemovePacket(PlayerTeam team) {
        return new ClientboundSetPlayerTeamPacket(team.getName(), 1, Optional.empty(), (Collection<String>)ImmutableList.of());
    }

    public static ClientboundSetPlayerTeamPacket createPlayerPacket(PlayerTeam team, String player, Action action) {
        return new ClientboundSetPlayerTeamPacket(team.getName(), action == Action.ADD ? 3 : 4, Optional.empty(), (Collection<String>)ImmutableList.of((Object)player));
    }

    private ClientboundSetPlayerTeamPacket(RegistryFriendlyByteBuf input) {
        this.name = input.readUtf();
        this.method = input.readByte();
        this.parameters = ClientboundSetPlayerTeamPacket.shouldHaveParameters(this.method) ? Optional.of((Parameters)Parameters.STREAM_CODEC.decode(input)) : Optional.empty();
        this.players = ClientboundSetPlayerTeamPacket.shouldHavePlayerList(this.method) ? input.readList(FriendlyByteBuf::readUtf) : ImmutableList.of();
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeUtf(this.name);
        output.writeByte(this.method);
        if (ClientboundSetPlayerTeamPacket.shouldHaveParameters(this.method)) {
            Parameters.STREAM_CODEC.encode(output, this.parameters.orElseThrow(() -> new IllegalStateException("Parameters not present, but method is" + this.method)));
        }
        if (ClientboundSetPlayerTeamPacket.shouldHavePlayerList(this.method)) {
            output.writeCollection(this.players, FriendlyByteBuf::writeUtf);
        }
    }

    private static boolean shouldHavePlayerList(int method) {
        return method == 0 || method == 3 || method == 4;
    }

    private static boolean shouldHaveParameters(int method) {
        return method == 0 || method == 2;
    }

    public @Nullable Action getPlayerAction() {
        return switch (this.method) {
            case 0, 3 -> Action.ADD;
            case 4 -> Action.REMOVE;
            default -> null;
        };
    }

    public @Nullable Action getTeamAction() {
        return switch (this.method) {
            case 0 -> Action.ADD;
            case 1 -> Action.REMOVE;
            default -> null;
        };
    }

    @Override
    public PacketType<ClientboundSetPlayerTeamPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_PLAYER_TEAM;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetPlayerTeamPacket(this);
    }

    public String getName() {
        return this.name;
    }

    public Collection<String> getPlayers() {
        return this.players;
    }

    public Optional<Parameters> getParameters() {
        return this.parameters;
    }

    public record Parameters(Component displayName, Component playerPrefix, Component playerSuffix, Team.Visibility nameTagVisibility, Team.CollisionRule collisionRule, Optional<TeamColor> color, @PlayerTeam.OptionFlags byte options) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Parameters> STREAM_CODEC = StreamCodec.composite(ComponentSerialization.TRUSTED_STREAM_CODEC, Parameters::displayName, ComponentSerialization.TRUSTED_STREAM_CODEC, Parameters::playerPrefix, ComponentSerialization.TRUSTED_STREAM_CODEC, Parameters::playerSuffix, Team.Visibility.STREAM_CODEC, Parameters::nameTagVisibility, Team.CollisionRule.STREAM_CODEC, Parameters::collisionRule, ByteBufCodecs.optional(TeamColor.STREAM_CODEC), Parameters::color, ByteBufCodecs.BYTE, Parameters::options, Parameters::new);

        public Parameters(PlayerTeam team) {
            this(team.getDisplayName(), team.getPlayerPrefix(), team.getPlayerSuffix(), team.getNameTagVisibility(), team.getCollisionRule(), team.getColor(), team.packOptions());
        }
    }

    public static enum Action {
        ADD,
        REMOVE;

    }
}

