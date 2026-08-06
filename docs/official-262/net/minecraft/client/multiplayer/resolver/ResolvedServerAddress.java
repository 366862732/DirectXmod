/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.multiplayer.resolver;

import java.net.InetSocketAddress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface ResolvedServerAddress {
    public String getHostName();

    public String getHostIp();

    public int getPort();

    public InetSocketAddress asInetSocketAddress();

    public static ResolvedServerAddress from(final InetSocketAddress address) {
        return new ResolvedServerAddress(){

            @Override
            public String getHostName() {
                return address.getAddress().getHostName();
            }

            @Override
            public String getHostIp() {
                return address.getAddress().getHostAddress();
            }

            @Override
            public int getPort() {
                return address.getPort();
            }

            @Override
            public InetSocketAddress asInetSocketAddress() {
                return address;
            }
        };
    }
}

