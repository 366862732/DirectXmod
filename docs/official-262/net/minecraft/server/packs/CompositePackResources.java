/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public class CompositePackResources
implements PackResources {
    private final PackResources primaryPackResources;
    private final List<PackResources> packResourcesStack;

    public CompositePackResources(PackResources primaryPackResources, List<PackResources> overlayPackResources) {
        this.primaryPackResources = primaryPackResources;
        ArrayList<PackResources> stack = new ArrayList<PackResources>(overlayPackResources.size() + 1);
        stack.addAll(Lists.reverse(overlayPackResources));
        stack.add(primaryPackResources);
        this.packResourcesStack = List.copyOf(stack);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String ... path) {
        return this.primaryPackResources.getRootResource(path);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        for (PackResources packResources : this.packResourcesStack) {
            IoSupplier<InputStream> resource = packResources.getResource(type, location);
            if (resource == null) continue;
            return resource;
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output) {
        HashMap<Identifier, IoSupplier<InputStream>> result = new HashMap<Identifier, IoSupplier<InputStream>>();
        for (PackResources packResources : this.packResourcesStack) {
            packResources.listResources(type, namespace, directory, result::putIfAbsent);
        }
        result.forEach(output);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        HashSet<String> result = new HashSet<String>();
        for (PackResources overlayPackResource : this.packResourcesStack) {
            result.addAll(overlayPackResource.getNamespaces(type));
        }
        return result;
    }

    @Override
    public <T> @Nullable T getMetadataSection(MetadataSectionType<T> metadataSerializer) throws IOException {
        return this.primaryPackResources.getMetadataSection(metadataSerializer);
    }

    @Override
    public PackLocationInfo location() {
        return this.primaryPackResources.location();
    }

    @Override
    public void close() {
        this.packResourcesStack.forEach(PackResources::close);
    }
}

