/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.LoginPacketTypes;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket
implements Packet<ServerLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundKeyPacket> STREAM_CODEC = Packet.codec(ServerboundKeyPacket::write, ServerboundKeyPacket::new);
    private final byte[] keybytes;
    private final byte[] encryptedChallenge;

    public ServerboundKeyPacket(SecretKey secretKey, PublicKey publicKey, byte[] challenge) throws CryptException {
        this.keybytes = Crypt.encryptUsingKey(publicKey, secretKey.getEncoded());
        this.encryptedChallenge = Crypt.encryptUsingKey(publicKey, challenge);
    }

    private ServerboundKeyPacket(FriendlyByteBuf input) {
        this.keybytes = input.readByteArray();
        this.encryptedChallenge = input.readByteArray();
    }

    private void write(FriendlyByteBuf output) {
        output.writeByteArray(this.keybytes);
        output.writeByteArray(this.encryptedChallenge);
    }

    @Override
    public PacketType<ServerboundKeyPacket> type() {
        return LoginPacketTypes.SERVERBOUND_KEY;
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleKey(this);
    }

    public SecretKey getSecretKey(PrivateKey privateKey) throws CryptException {
        return Crypt.decryptByteToSecretKey(privateKey, this.keybytes);
    }

    public boolean isChallengeValid(byte[] challenge, PrivateKey privateKey) {
        try {
            return Arrays.equals(challenge, Crypt.decryptUsingKey(privateKey, this.encryptedChallenge));
        }
        catch (CryptException e) {
            return false;
        }
    }
}

