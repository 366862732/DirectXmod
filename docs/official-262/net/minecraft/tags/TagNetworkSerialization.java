/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 */
package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;

public class TagNetworkSerialization {
    public static Map<ResourceKey<? extends Registry<?>>, NetworkPayload> serializeTagsToNetwork(LayeredRegistryAccess<RegistryLayer> registries) {
        return RegistrySynchronization.networkSafeRegistries(registries).map(e -> Pair.of(e.key(), (Object)TagNetworkSerialization.serializeToNetwork(e.value()))).filter(e -> !((NetworkPayload)e.getSecond()).isEmpty()).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    private static <T> NetworkPayload serializeToNetwork(Registry<T> registry) {
        HashMap<Identifier, IntList> result = new HashMap<Identifier, IntList>();
        registry.getTags().forEach(tag -> {
            IntArrayList ids = new IntArrayList(tag.size());
            for (Holder holder : tag) {
                if (holder.kind() != Holder.Kind.REFERENCE) {
                    throw new IllegalStateException("Can't serialize unregistered value " + String.valueOf(holder));
                }
                ids.add(registry.getId(holder.value()));
            }
            result.put(tag.key().location(), (IntList)ids);
        });
        return new NetworkPayload(result);
    }

    private static <T> TagLoader.LoadResult<T> deserializeTagsFromNetwork(Registry<T> registry, NetworkPayload payload) {
        ResourceKey registryKey = registry.key();
        HashMap tags = new HashMap();
        payload.tags.forEach((key, ids) -> {
            TagKey tagKey = TagKey.create(registryKey, key);
            List values = ids.intStream().mapToObj(registry::get).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
            tags.put(tagKey, values);
        });
        return new TagLoader.LoadResult<T>(registryKey, tags);
    }

    public static final class NetworkPayload {
        public static final NetworkPayload EMPTY = new NetworkPayload(Map.of());
        private final Map<Identifier, IntList> tags;

        public NetworkPayload(Map<Identifier, IntList> tags) {
            this.tags = tags;
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeMap(this.tags, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeIntIdList);
        }

        public static NetworkPayload read(FriendlyByteBuf buf) {
            return new NetworkPayload(buf.readMap(FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readIntIdList));
        }

        public boolean isEmpty() {
            return this.tags.isEmpty();
        }

        public int size() {
            return this.tags.size();
        }

        public <T> TagLoader.LoadResult<T> resolve(Registry<T> registry) {
            return TagNetworkSerialization.deserializeTagsFromNetwork(registry, this);
        }
    }
}

