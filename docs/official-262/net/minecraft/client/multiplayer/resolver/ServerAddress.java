/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.net.HostAndPort
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer.resolver;

import com.google.common.net.HostAndPort;
import com.mojang.logging.LogUtils;
import java.net.IDN;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public final class ServerAddress {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final HostAndPort hostAndPort;
    private static final ServerAddress INVALID = new ServerAddress(HostAndPort.fromParts((String)"server.invalid", (int)25565));

    public ServerAddress(String host, int port) {
        this(HostAndPort.fromParts((String)host, (int)port));
    }

    private ServerAddress(HostAndPort hostAndPort) {
        this.hostAndPort = hostAndPort;
    }

    public String getHost() {
        try {
            return IDN.toASCII(this.hostAndPort.getHost());
        }
        catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    public int getPort() {
        return this.hostAndPort.getPort();
    }

    public static ServerAddress parseString(@Nullable String input) {
        if (input == null) {
            return INVALID;
        }
        try {
            HostAndPort result = HostAndPort.fromString((String)input).withDefaultPort(25565);
            if (result.getHost().isEmpty()) {
                return INVALID;
            }
            return new ServerAddress(result);
        }
        catch (IllegalArgumentException e) {
            LOGGER.info("Failed to parse URL {}", (Object)input, (Object)e);
            return INVALID;
        }
    }

    public static boolean isValidAddress(String input) {
        try {
            HostAndPort hostAndPort = HostAndPort.fromString((String)input);
            String host = hostAndPort.getHost();
            if (!host.isEmpty()) {
                IDN.toASCII(host);
                return true;
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
        return false;
    }

    public static int parsePort(String str) {
        try {
            return Integer.parseInt(str.trim());
        }
        catch (Exception exception) {
            return 25565;
        }
    }

    public String toString() {
        return this.hostAndPort.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ServerAddress) {
            ServerAddress serverAddress = (ServerAddress)o;
            return this.hostAndPort.equals((Object)serverAddress.hostAndPort);
        }
        return false;
    }

    public int hashCode() {
        return this.hostAndPort.hashCode();
    }
}

