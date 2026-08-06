/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Streams
 *  com.mojang.blocklist.BlockListSupplier
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.resolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mojang.blocklist.BlockListSupplier;
import java.util.Objects;
import java.util.ServiceLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

@Environment(value=EnvType.CLIENT)
public interface AddressCheck {
    public boolean isAllowed(ResolvedServerAddress var1);

    public boolean isAllowed(ServerAddress var1);

    public static AddressCheck createFromService() {
        final ImmutableList blockLists = (ImmutableList)Streams.stream(ServiceLoader.load(BlockListSupplier.class)).map(BlockListSupplier::createBlockList).filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
        return new AddressCheck(){

            @Override
            public boolean isAllowed(ResolvedServerAddress address) {
                String hostName = address.getHostName();
                String hostIp = address.getHostIp();
                return blockLists.stream().noneMatch(p -> p.test(hostName) || p.test(hostIp));
            }

            @Override
            public boolean isAllowed(ServerAddress address) {
                String hostName = address.getHost();
                return blockLists.stream().noneMatch(p -> p.test(hostName));
            }
        };
    }
}

