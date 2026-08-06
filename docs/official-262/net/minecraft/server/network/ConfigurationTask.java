/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;

public interface ConfigurationTask {
    public void start(Consumer<Packet<?>> var1);

    default public boolean tick() {
        return false;
    }

    public Type type();

    public record Type(String id) {
        @Override
        public String toString() {
            return this.id;
        }
    }
}

