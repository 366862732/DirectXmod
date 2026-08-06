/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 */
package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
    public static final ResourceMetadata EMPTY = new ResourceMetadata(){

        @Override
        public <T> Optional<T> getSection(MetadataSectionType<T> serializer) {
            return Optional.empty();
        }
    };
    public static final IoSupplier<ResourceMetadata> EMPTY_SUPPLIER = () -> EMPTY;

    public static ResourceMetadata fromJsonStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
            final JsonObject metadata = GsonHelper.parse(reader);
            ResourceMetadata resourceMetadata = new ResourceMetadata(){

                @Override
                public <T> Optional<T> getSection(MetadataSectionType<T> serializer) {
                    String name = serializer.name();
                    JsonElement rawSection = metadata.get(name);
                    if (rawSection != null) {
                        Object section = serializer.codec().parse((DynamicOps)JsonOps.INSTANCE, (Object)rawSection).getOrThrow(JsonParseException::new);
                        return Optional.of(section);
                    }
                    return Optional.empty();
                }
            };
            return resourceMetadata;
        }
    }

    public <T> Optional<T> getSection(MetadataSectionType<T> var1);

    default public <T> Optional<MetadataSectionType.WithValue<T>> getTypedSection(MetadataSectionType<T> type) {
        return this.getSection(type).map(type::withValue);
    }

    public static <T> ResourceMetadata of(MetadataSectionType<T> k, T v) {
        return new MapBased(Map.of(k, v));
    }

    public static <T1, T2> ResourceMetadata of(MetadataSectionType<T1> k1, T1 v1, MetadataSectionType<T2> k2, T2 v2) {
        return new MapBased(Map.of(k1, v1, k2, v2));
    }

    default public List<MetadataSectionType.WithValue<?>> getTypedSections(Collection<MetadataSectionType<?>> types) {
        return types.stream().map(this::getTypedSection).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
    }

    public static class MapBased
    implements ResourceMetadata {
        private final Map<MetadataSectionType<?>, ?> values;

        private MapBased(Map<MetadataSectionType<?>, ?> values) {
            this.values = values;
        }

        @Override
        public <T> Optional<T> getSection(MetadataSectionType<T> serializer) {
            return Optional.ofNullable(this.values.get(serializer));
        }
    }
}

