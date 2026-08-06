/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.PngInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ServerData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ICON_SIZE = 1024;
    public String name;
    public String ip;
    public Component status;
    public Component motd;
    public  @Nullable ServerStatus.Players players;
    public long ping;
    public int protocol = SharedConstants.getCurrentVersion().protocolVersion();
    public Component version = Component.literal(SharedConstants.getCurrentVersion().name());
    public List<Component> playerList = Collections.emptyList();
    private ServerPackStatus packStatus = ServerPackStatus.PROMPT;
    private byte @Nullable [] iconBytes;
    private Type type;
    private int acceptedCodeOfConduct;
    private State state = State.INITIAL;

    public ServerData(String name, String ip, Type type) {
        this.name = name;
        this.ip = ip;
        this.type = type;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", this.name);
        tag.putString("ip", this.ip);
        tag.storeNullable("icon", ExtraCodecs.BASE64_STRING, this.iconBytes);
        tag.store(ServerPackStatus.FIELD_CODEC, this.packStatus);
        if (this.acceptedCodeOfConduct != 0) {
            tag.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
        }
        return tag;
    }

    public ServerPackStatus getResourcePackStatus() {
        return this.packStatus;
    }

    public void setResourcePackStatus(ServerPackStatus packStatus) {
        this.packStatus = packStatus;
    }

    public static ServerData read(CompoundTag tag) {
        ServerData server = new ServerData(tag.getStringOr("name", ""), tag.getStringOr("ip", ""), Type.OTHER);
        server.setIconBytes(tag.read("icon", ExtraCodecs.BASE64_STRING).orElse(null));
        server.setResourcePackStatus(tag.read(ServerPackStatus.FIELD_CODEC).orElse(ServerPackStatus.PROMPT));
        server.acceptedCodeOfConduct = tag.getIntOr("acceptedCodeOfConduct", 0);
        return server;
    }

    public byte @Nullable [] getIconBytes() {
        return this.iconBytes;
    }

    public void setIconBytes(byte @Nullable [] iconBytes) {
        this.iconBytes = iconBytes;
    }

    public boolean isLan() {
        return this.type == Type.LAN;
    }

    public boolean isRealm() {
        return this.type == Type.REALM;
    }

    public Type type() {
        return this.type;
    }

    public boolean hasAcceptedCodeOfConduct(String codeOfConduct) {
        return this.acceptedCodeOfConduct == codeOfConduct.hashCode();
    }

    public void acceptCodeOfConduct(String codeOfConduct) {
        this.acceptedCodeOfConduct = codeOfConduct.hashCode();
    }

    public void clearCodeOfConduct() {
        this.acceptedCodeOfConduct = 0;
    }

    public void copyNameIconFrom(ServerData other) {
        this.ip = other.ip;
        this.name = other.name;
        this.iconBytes = other.iconBytes;
    }

    public void copyFrom(ServerData other) {
        this.copyNameIconFrom(other);
        this.setResourcePackStatus(other.getResourcePackStatus());
        this.type = other.type;
    }

    public State state() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static byte @Nullable [] validateIcon(byte @Nullable [] bytes) {
        if (bytes != null) {
            try {
                PngInfo iconInfo = PngInfo.fromBytes(bytes);
                if (iconInfo.width() <= 1024 && iconInfo.height() <= 1024) {
                    return bytes;
                }
            }
            catch (IOException e) {
                LOGGER.warn("Failed to decode server icon", (Throwable)e);
            }
        }
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum ServerPackStatus {
        ENABLED("enabled"),
        DISABLED("disabled"),
        PROMPT("prompt");

        public static final MapCodec<ServerPackStatus> FIELD_CODEC;
        private final Component name;

        private ServerPackStatus(String name) {
            this.name = Component.translatable("manageServer.resourcePack." + name);
        }

        public Component getName() {
            return this.name;
        }

        static {
            FIELD_CODEC = Codec.BOOL.optionalFieldOf("acceptTextures").xmap(acceptTextures -> acceptTextures.map(b -> b != false ? ENABLED : DISABLED).orElse(PROMPT), status -> switch (status.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> Optional.of(true);
                case 1 -> Optional.of(false);
                case 2 -> Optional.empty();
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum State {
        INITIAL,
        PINGING,
        UNREACHABLE,
        INCOMPATIBLE,
        SUCCESSFUL;

    }

    @Environment(value=EnvType.CLIENT)
    public static enum Type {
        LAN,
        REALM,
        OTHER;

    }
}

