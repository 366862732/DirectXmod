/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ClientboundSoundPacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSoundPacket> STREAM_CODEC = Packet.codec(ClientboundSoundPacket::write, ClientboundSoundPacket::new);
    public static final float LOCATION_ACCURACY = 8.0f;
    private final Holder<SoundEvent> sound;
    private final SoundSource source;
    private final int x;
    private final int y;
    private final int z;
    private final float volume;
    private final float pitch;
    private final long seed;

    public ClientboundSoundPacket(Holder<SoundEvent> sound, SoundSource source, double x, double y, double z, float volume, float pitch, long seed) {
        this.sound = sound;
        this.source = source;
        this.x = (int)(x * 8.0);
        this.y = (int)(y * 8.0);
        this.z = (int)(z * 8.0);
        this.volume = volume;
        this.pitch = pitch;
        this.seed = seed;
    }

    private ClientboundSoundPacket(RegistryFriendlyByteBuf input) {
        this.sound = (Holder)SoundEvent.STREAM_CODEC.decode(input);
        this.source = input.readEnum(SoundSource.class);
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
        this.volume = input.readFloat();
        this.pitch = input.readFloat();
        this.seed = input.readLong();
    }

    private void write(RegistryFriendlyByteBuf output) {
        SoundEvent.STREAM_CODEC.encode(output, this.sound);
        output.writeEnum(this.source);
        output.writeInt(this.x);
        output.writeInt(this.y);
        output.writeInt(this.z);
        output.writeFloat(this.volume);
        output.writeFloat(this.pitch);
        output.writeLong(this.seed);
    }

    @Override
    public PacketType<ClientboundSoundPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SOUND;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSoundEvent(this);
    }

    public Holder<SoundEvent> getSound() {
        return this.sound;
    }

    public SoundSource getSource() {
        return this.source;
    }

    public double getX() {
        return (float)this.x / 8.0f;
    }

    public double getY() {
        return (float)this.y / 8.0f;
    }

    public double getZ() {
        return (float)this.z / 8.0f;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public long getSeed() {
        return this.seed;
    }
}

