/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.BossEvent;

public class ClientboundBossEventPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBossEventPacket> STREAM_CODEC = Packet.codec(ClientboundBossEventPacket::write, ClientboundBossEventPacket::new);
    private static final int FLAG_DARKEN = 1;
    private static final int FLAG_MUSIC = 2;
    private static final int FLAG_FOG = 4;
    private final UUID id;
    private final Operation operation;
    private static final Operation REMOVE_OPERATION = new Operation(){

        @Override
        public OperationType getType() {
            return OperationType.REMOVE;
        }

        @Override
        public void dispatch(UUID id, Handler handler) {
            handler.remove(id);
        }

        @Override
        public void write(RegistryFriendlyByteBuf output) {
        }
    };

    private ClientboundBossEventPacket(UUID id, Operation operation) {
        this.id = id;
        this.operation = operation;
    }

    private ClientboundBossEventPacket(RegistryFriendlyByteBuf input) {
        this.id = input.readUUID();
        OperationType type = input.readEnum(OperationType.class);
        this.operation = type.reader.decode(input);
    }

    public static ClientboundBossEventPacket createAddPacket(BossEvent event) {
        return new ClientboundBossEventPacket(event.getId(), new AddOperation(event));
    }

    public static ClientboundBossEventPacket createRemovePacket(UUID id) {
        return new ClientboundBossEventPacket(id, REMOVE_OPERATION);
    }

    public static ClientboundBossEventPacket createUpdateProgressPacket(BossEvent event) {
        return new ClientboundBossEventPacket(event.getId(), new UpdateProgressOperation(event.getProgress()));
    }

    public static ClientboundBossEventPacket createUpdateNamePacket(BossEvent event) {
        return new ClientboundBossEventPacket(event.getId(), new UpdateNameOperation(event.getName()));
    }

    public static ClientboundBossEventPacket createUpdateStylePacket(BossEvent event) {
        return new ClientboundBossEventPacket(event.getId(), new UpdateStyleOperation(event.getColor(), event.getOverlay()));
    }

    public static ClientboundBossEventPacket createUpdatePropertiesPacket(BossEvent event) {
        return new ClientboundBossEventPacket(event.getId(), new UpdatePropertiesOperation(event.shouldDarkenScreen(), event.shouldPlayBossMusic(), event.shouldCreateWorldFog()));
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeUUID(this.id);
        output.writeEnum(this.operation.getType());
        this.operation.write(output);
    }

    private static int encodeProperties(boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
        int properties = 0;
        if (darkenScreen) {
            properties |= 1;
        }
        if (playMusic) {
            properties |= 2;
        }
        if (createWorldFog) {
            properties |= 4;
        }
        return properties;
    }

    @Override
    public PacketType<ClientboundBossEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_BOSS_EVENT;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleBossUpdate(this);
    }

    public void dispatch(Handler handler) {
        this.operation.dispatch(this.id, handler);
    }

    private static interface Operation {
        public OperationType getType();

        public void dispatch(UUID var1, Handler var2);

        public void write(RegistryFriendlyByteBuf var1);
    }

    private static enum OperationType {
        ADD(AddOperation::new),
        REMOVE(input -> REMOVE_OPERATION),
        UPDATE_PROGRESS(UpdateProgressOperation::new),
        UPDATE_NAME(UpdateNameOperation::new),
        UPDATE_STYLE(UpdateStyleOperation::new),
        UPDATE_PROPERTIES(UpdatePropertiesOperation::new);

        private final StreamDecoder<RegistryFriendlyByteBuf, Operation> reader;

        private OperationType(StreamDecoder<RegistryFriendlyByteBuf, Operation> reader) {
            this.reader = reader;
        }
    }

    private static class AddOperation
    implements Operation {
        private final Component name;
        private final float progress;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        private AddOperation(BossEvent event) {
            this.name = event.getName();
            this.progress = event.getProgress();
            this.color = event.getColor();
            this.overlay = event.getOverlay();
            this.darkenScreen = event.shouldDarkenScreen();
            this.playMusic = event.shouldPlayBossMusic();
            this.createWorldFog = event.shouldCreateWorldFog();
        }

        private AddOperation(RegistryFriendlyByteBuf input) {
            this.name = (Component)ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input);
            this.progress = input.readFloat();
            this.color = input.readEnum(BossEvent.BossBarColor.class);
            this.overlay = input.readEnum(BossEvent.BossBarOverlay.class);
            short flags = input.readUnsignedByte();
            this.darkenScreen = (flags & 1) > 0;
            this.playMusic = (flags & 2) > 0;
            this.createWorldFog = (flags & 4) > 0;
        }

        @Override
        public OperationType getType() {
            return OperationType.ADD;
        }

        @Override
        public void dispatch(UUID id, Handler handler) {
            handler.add(id, this.name, this.progress, this.color, this.overlay, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(RegistryFriendlyByteBuf output) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(output, this.name);
            output.writeFloat(this.progress);
            output.writeEnum(this.color);
            output.writeEnum(this.overlay);
            output.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    private record UpdateProgressOperation(float progress) implements Operation
    {
        private UpdateProgressOperation(RegistryFriendlyByteBuf input) {
            this(input.readFloat());
        }

        @Override
        public OperationType getType() {
            return OperationType.UPDATE_PROGRESS;
        }

        @Override
        public void dispatch(UUID id, Handler handler) {
            handler.updateProgress(id, this.progress);
        }

        @Override
        public void write(RegistryFriendlyByteBuf output) {
            output.writeFloat(this.progress);
        }
    }

    private record UpdateNameOperation(Component name) implements Operation
    {
        private UpdateNameOperation(RegistryFriendlyByteBuf input) {
            this((Component)ComponentSerialization.TRUSTED_STREAM_CODEC.decode(input));
        }

        @Override
        public OperationType getType() {
            return OperationType.UPDATE_NAME;
        }

        @Override
        public void dispatch(UUID id, Handler handler) {
            handler.updateName(id, this.name);
        }

        @Override
        public void write(RegistryFriendlyByteBuf output) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(output, this.name);
        }
    }

    private static class UpdateStyleOperation
    implements Operation {
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;

        private UpdateStyleOperation(BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
            this.color = color;
            this.overlay = overlay;
        }

        private UpdateStyleOperation(RegistryFriendlyByteBuf input) {
            this.color = input.readEnum(BossEvent.BossBarColor.class);
            this.overlay = input.readEnum(BossEvent.BossBarOverlay.class);
        }

        @Override
        public OperationType getType() {
            return OperationType.UPDATE_STYLE;
        }

        @Override
        public void dispatch(UUID id, Handler handler) {
            handler.updateStyle(id, this.color, this.overlay);
        }

        @Override
        public void write(RegistryFriendlyByteBuf output) {
            output.writeEnum(this.color);
            output.writeEnum(this.overlay);
        }
    }

    private static class UpdatePropertiesOperation
    implements Operation {
        private final boolean darkenScreen;
        private final boolean playMusic;
        private final boolean createWorldFog;

        private UpdatePropertiesOperation(boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
            this.darkenScreen = darkenScreen;
            this.playMusic = playMusic;
            this.createWorldFog = createWorldFog;
        }

        private UpdatePropertiesOperation(RegistryFriendlyByteBuf input) {
            short flags = input.readUnsignedByte();
            this.darkenScreen = (flags & 1) > 0;
            this.playMusic = (flags & 2) > 0;
            this.createWorldFog = (flags & 4) > 0;
        }

        @Override
        public OperationType getType() {
            return OperationType.UPDATE_PROPERTIES;
        }

        @Override
        public void dispatch(UUID id, Handler handler) {
            handler.updateProperties(id, this.darkenScreen, this.playMusic, this.createWorldFog);
        }

        @Override
        public void write(RegistryFriendlyByteBuf output) {
            output.writeByte(ClientboundBossEventPacket.encodeProperties(this.darkenScreen, this.playMusic, this.createWorldFog));
        }
    }

    public static interface Handler {
        default public void add(UUID id, Component name, float progress, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
        }

        default public void remove(UUID id) {
        }

        default public void updateProgress(UUID id, float progress) {
        }

        default public void updateName(UUID id, Component name) {
        }

        default public void updateStyle(UUID id, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        }

        default public void updateProperties(UUID id, boolean darkenScreen, boolean playMusic, boolean createWorldFog) {
        }
    }
}

