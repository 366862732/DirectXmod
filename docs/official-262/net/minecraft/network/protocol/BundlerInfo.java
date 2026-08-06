/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jspecify.annotations.Nullable;

public interface BundlerInfo {
    public static final int BUNDLE_SIZE_LIMIT = 4096;

    public static <T extends PacketListener, P extends BundlePacket<? super T>> BundlerInfo createForPacket(final PacketType<P> bundlePacketType, final Function<Iterable<Packet<? super T>>, P> constructor, final BundleDelimiterPacket<? super T> delimiterPacket) {
        return new BundlerInfo(){

            @Override
            public void unbundlePacket(Packet<?> packet, Consumer<Packet<?>> output) {
                if (packet.type() == bundlePacketType) {
                    BundlePacket bundlerPacket = (BundlePacket)packet;
                    output.accept(delimiterPacket);
                    bundlerPacket.subPackets().forEach(output);
                    output.accept(delimiterPacket);
                } else {
                    output.accept(packet);
                }
            }

            @Override
            public @Nullable Bundler startPacketBundling(Packet<?> packet) {
                if (packet == delimiterPacket) {
                    return new Bundler(){
                        private final List<Packet<? super T>> bundlePackets;
                        {
                            Objects.requireNonNull(this$0);
                            this.bundlePackets = new ArrayList();
                        }

                        @Override
                        public @Nullable Packet<?> addPacket(Packet<?> packet) {
                            if (packet == delimiterPacket) {
                                return (Packet)constructor.apply(this.bundlePackets);
                            }
                            Packet<?> castPacket = packet;
                            if (this.bundlePackets.size() >= 4096) {
                                throw new IllegalStateException("Too many packets in a bundle");
                            }
                            this.bundlePackets.add(castPacket);
                            return null;
                        }
                    };
                }
                return null;
            }
        };
    }

    public void unbundlePacket(Packet<?> var1, Consumer<Packet<?>> var2);

    public @Nullable Bundler startPacketBundling(Packet<?> var1);

    public static interface Bundler {
        public @Nullable Packet<?> addPacket(Packet<?> var1);
    }
}

