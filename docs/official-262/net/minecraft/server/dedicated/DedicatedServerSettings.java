/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.dedicated;

import java.nio.file.Path;
import java.util.function.UnaryOperator;
import net.minecraft.server.dedicated.DedicatedServerProperties;

public class DedicatedServerSettings {
    private final Path source;
    private DedicatedServerProperties properties;

    public DedicatedServerSettings(Path source) {
        this.source = source;
        this.properties = DedicatedServerProperties.fromFile(source);
    }

    public DedicatedServerProperties getProperties() {
        return this.properties;
    }

    public void forceSave() {
        this.properties.store(this.source);
    }

    public DedicatedServerSettings update(UnaryOperator<DedicatedServerProperties> mutator) {
        this.properties = (DedicatedServerProperties)mutator.apply(this.properties);
        this.properties.store(this.source);
        return this;
    }
}

