/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;

public interface ProtocolInfo<T extends PacketListener> {
    public ConnectionProtocol id();

    public PacketFlow flow();

    public StreamCodec<ByteBuf, Packet<? super T>> codec();

    public @Nullable BundlerInfo bundlerInfo();

    public static interface DetailsProvider {
        public Details details();
    }

    public static interface Details {
        public ConnectionProtocol id();

        public PacketFlow flow();

        @VisibleForDebug
        public void listPackets(PacketVisitor var1);

        @FunctionalInterface
        public static interface PacketVisitor {
            public void accept(PacketType<?> var1, int var2);
        }
    }
}

