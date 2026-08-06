/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public interface PackResources
extends AutoCloseable {
    public static final String METADATA_EXTENSION = ".mcmeta";
    public static final String PACK_META = "pack.mcmeta";

    public @Nullable IoSupplier<InputStream> getRootResource(String ... var1);

    public @Nullable IoSupplier<InputStream> getResource(PackType var1, Identifier var2);

    public void listResources(PackType var1, String var2, String var3, ResourceOutput var4);

    public Set<String> getNamespaces(PackType var1);

    public <T> @Nullable T getMetadataSection(MetadataSectionType<T> var1) throws IOException;

    public PackLocationInfo location();

    default public String packId() {
        return this.location().id();
    }

    default public Optional<KnownPack> knownPackInfo() {
        return this.location().knownPackInfo();
    }

    @Override
    public void close();

    @FunctionalInterface
    public static interface ResourceOutput
    extends BiConsumer<Identifier, IoSupplier<InputStream>> {
    }
}

