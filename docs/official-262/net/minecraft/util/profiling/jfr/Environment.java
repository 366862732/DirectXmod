/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling.jfr;

import net.minecraft.server.MinecraftServer;

public enum Environment {
    CLIENT("client"),
    SERVER("server");

    private final String description;

    private Environment(String description) {
        this.description = description;
    }

    public static Environment from(MinecraftServer server) {
        return server.isDedicatedServer() ? SERVER : CLIENT;
    }

    public String getDescription() {
        return this.description;
    }
}

