/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.packs.resources;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

@FunctionalInterface
public interface ResourceProvider {
    public static final ResourceProvider EMPTY = location -> Optional.empty();

    public Optional<Resource> getResource(Identifier var1);

    default public Resource getResourceOrThrow(Identifier location) throws FileNotFoundException {
        return this.getResource(location).orElseThrow(() -> new FileNotFoundException(location.toString()));
    }

    default public InputStream open(Identifier location) throws IOException {
        return this.getResourceOrThrow(location).open();
    }

    default public BufferedReader openAsReader(Identifier location) throws IOException {
        return this.getResourceOrThrow(location).openAsReader();
    }

    public static ResourceProvider fromMap(Map<Identifier, Resource> map) {
        return location -> Optional.ofNullable((Resource)map.get(location));
    }
}

