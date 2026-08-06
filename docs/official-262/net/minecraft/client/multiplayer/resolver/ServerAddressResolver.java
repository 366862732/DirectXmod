/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.slf4j.Logger;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface ServerAddressResolver {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ServerAddressResolver SYSTEM = address -> {
        try {
            InetAddress resolvedAddress = InetAddress.getByName(address.getHost());
            return Optional.of(ResolvedServerAddress.from(new InetSocketAddress(resolvedAddress, address.getPort())));
        }
        catch (UnknownHostException e) {
            LOGGER.debug("Couldn't resolve server {} address", (Object)address.getHost(), (Object)e);
            return Optional.empty();
        }
    };

    public Optional<ResolvedServerAddress> resolve(ServerAddress var1);
}

