/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.packs.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;

public interface ResourceManager
extends ResourceProvider {
    public Set<String> getNamespaces();

    public List<Resource> getResourceStack(Identifier var1);

    public Map<Identifier, Resource> listResources(String var1, Predicate<Identifier> var2);

    public Map<Identifier, List<Resource>> listResourceStacks(String var1, Predicate<Identifier> var2);

    public Stream<PackResources> listPacks();

    public static enum Empty implements ResourceManager
    {
        INSTANCE;


        @Override
        public Set<String> getNamespaces() {
            return Set.of();
        }

        @Override
        public Optional<Resource> getResource(Identifier location) {
            return Optional.empty();
        }

        @Override
        public List<Resource> getResourceStack(Identifier location) {
            return List.of();
        }

        @Override
        public Map<Identifier, Resource> listResources(String directory, Predicate<Identifier> filter) {
            return Map.of();
        }

        @Override
        public Map<Identifier, List<Resource>> listResourceStacks(String directory, Predicate<Identifier> filter) {
            return Map.of();
        }

        @Override
        public Stream<PackResources> listPacks() {
            return Stream.of(new PackResources[0]);
        }
    }
}

