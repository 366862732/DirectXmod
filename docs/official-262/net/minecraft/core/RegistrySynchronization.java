/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DynamicOps
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core;

import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.KnownPack;

public class RegistrySynchronization {
    private static final Set<ResourceKey<? extends Registry<?>>> NETWORKABLE_REGISTRIES = RegistryDataLoader.SYNCHRONIZED_REGISTRIES.stream().map(RegistryDataLoader.RegistryData::key).collect(Collectors.toUnmodifiableSet());

    public static void packRegistries(DynamicOps<Tag> ops, RegistryAccess registries, Set<KnownPack> clientKnownPacks, BiConsumer<ResourceKey<? extends Registry<?>>, List<PackedRegistryEntry>> output) {
        RegistryDataLoader.SYNCHRONIZED_REGISTRIES.forEach(registryEntry -> RegistrySynchronization.packRegistry(ops, registryEntry, registries, clientKnownPacks, output));
    }

    private static <T> void packRegistry(DynamicOps<Tag> ops, RegistryDataLoader.RegistryData<T> registryData, RegistryAccess registries, Set<KnownPack> clientKnownPacks, BiConsumer<ResourceKey<? extends Registry<?>>, List<PackedRegistryEntry>> output) {
        registries.lookup(registryData.key()).ifPresent(registry -> {
            ArrayList packedElements = new ArrayList(registry.size());
            registry.listElements().forEach(element -> {
                Optional<Tag> contents;
                boolean canSkipContents = registry.registrationInfo(element.key()).flatMap(RegistrationInfo::knownPackInfo).filter(clientKnownPacks::contains).isPresent();
                if (canSkipContents) {
                    contents = Optional.empty();
                } else {
                    Tag encodedElement = (Tag)registryData.elementCodec().encodeStart(ops, element.value()).getOrThrow(s -> new IllegalArgumentException("Failed to serialize " + String.valueOf(element.key()) + ": " + s));
                    contents = Optional.of(encodedElement);
                }
                packedElements.add(new PackedRegistryEntry(element.key().identifier(), contents));
            });
            output.accept(registry.key(), packedElements);
        });
    }

    private static Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries(RegistryAccess access) {
        return access.registries().filter(e -> RegistrySynchronization.isNetworkable(e.key()));
    }

    public static Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries(LayeredRegistryAccess<RegistryLayer> registries) {
        return RegistrySynchronization.ownedNetworkableRegistries(registries.getAccessFrom(RegistryLayer.WORLDGEN));
    }

    public static Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries(LayeredRegistryAccess<RegistryLayer> registries) {
        Stream<RegistryAccess.RegistryEntry<?>> staticRegistries = registries.getLayer(RegistryLayer.STATIC).registries();
        Stream<RegistryAccess.RegistryEntry<?>> networkedRegistries = RegistrySynchronization.networkedRegistries(registries);
        return Stream.concat(networkedRegistries, staticRegistries);
    }

    public static boolean isNetworkable(ResourceKey<? extends Registry<?>> key) {
        return NETWORKABLE_REGISTRIES.contains(key);
    }

    public record PackedRegistryEntry(Identifier id, Optional<Tag> data) {
        public static final StreamCodec<ByteBuf, PackedRegistryEntry> STREAM_CODEC = StreamCodec.composite(Identifier.STREAM_CODEC, PackedRegistryEntry::id, ByteBufCodecs.TAG.apply(ByteBufCodecs::optional), PackedRegistryEntry::data, PackedRegistryEntry::new);
    }
}

