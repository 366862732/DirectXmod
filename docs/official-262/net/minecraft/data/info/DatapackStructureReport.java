/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;

public class DatapackStructureReport
implements DataProvider {
    private final PackOutput output;
    private static final Entry PSEUDO_REGISTRY = new Entry(true, false, true);
    private static final Entry STABLE_DYNAMIC_REGISTRY = new Entry(true, true, true);
    private static final Entry UNSTABLE_DYNAMIC_REGISTRY = new Entry(true, true, false);
    private static final Entry BUILT_IN_REGISTRY = new Entry(false, true, true);
    private static final Map<ResourceKey<? extends Registry<?>>, Entry> MANUAL_ENTRIES = Map.of(Registries.RECIPE, PSEUDO_REGISTRY, Registries.ADVANCEMENT, PSEUDO_REGISTRY, Registries.LOOT_TABLE, STABLE_DYNAMIC_REGISTRY, Registries.ITEM_MODIFIER, STABLE_DYNAMIC_REGISTRY, Registries.PREDICATE, STABLE_DYNAMIC_REGISTRY);
    private static final Map<String, CustomPackEntry> NON_REGISTRY_ENTRIES = Map.of("structure", new CustomPackEntry(Format.STRUCTURE, new Entry(true, false, true)), "function", new CustomPackEntry(Format.MCFUNCTION, new Entry(true, true, true)));
    private static final Codec<ResourceKey<? extends Registry<?>>> REGISTRY_KEY_CODEC = Identifier.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::identifier);

    public DatapackStructureReport(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Report report = new Report(this.listRegistries(), NON_REGISTRY_ENTRIES);
        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("datapack.json");
        return DataProvider.saveStable(cache, (JsonElement)Report.CODEC.encodeStart((DynamicOps)JsonOps.INSTANCE, (Object)report).getOrThrow(), path);
    }

    @Override
    public String getName() {
        return "Datapack Structure";
    }

    private void putIfNotPresent(Map<ResourceKey<? extends Registry<?>>, Entry> output, ResourceKey<? extends Registry<?>> key, Entry entry) {
        Entry previous = output.putIfAbsent(key, entry);
        if (previous != null) {
            throw new IllegalStateException("Duplicate entry for key " + String.valueOf(key.identifier()));
        }
    }

    private Map<ResourceKey<? extends Registry<?>>, Entry> listRegistries() {
        HashMap result = new HashMap();
        BuiltInRegistries.REGISTRY.forEach(entry -> this.putIfNotPresent(result, entry.key(), BUILT_IN_REGISTRY));
        RegistryDataLoader.WORLDGEN_REGISTRIES.forEach(entry -> this.putIfNotPresent(result, entry.key(), UNSTABLE_DYNAMIC_REGISTRY));
        RegistryDataLoader.DIMENSION_REGISTRIES.forEach(entry -> this.putIfNotPresent(result, entry.key(), UNSTABLE_DYNAMIC_REGISTRY));
        MANUAL_ENTRIES.forEach((key, entry) -> this.putIfNotPresent(result, (ResourceKey<? extends Registry<?>>)key, (Entry)entry));
        return result;
    }

    private record Report(Map<ResourceKey<? extends Registry<?>>, Entry> registries, Map<String, CustomPackEntry> others) {
        public static final Codec<Report> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.unboundedMap(REGISTRY_KEY_CODEC, Entry.CODEC).fieldOf("registries").forGetter(Report::registries), (App)Codec.unboundedMap((Codec)Codec.STRING, CustomPackEntry.CODEC).fieldOf("others").forGetter(Report::others)).apply((Applicative)i, Report::new));
    }

    private record Entry(boolean elements, boolean tags, boolean stable) {
        public static final MapCodec<Entry> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.fieldOf("elements").forGetter(Entry::elements), (App)Codec.BOOL.fieldOf("tags").forGetter(Entry::tags), (App)Codec.BOOL.fieldOf("stable").forGetter(Entry::stable)).apply((Applicative)i, Entry::new));
        public static final Codec<Entry> CODEC = MAP_CODEC.codec();
    }

    private record CustomPackEntry(Format format, Entry entry) {
        public static final Codec<CustomPackEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)Format.CODEC.fieldOf("format").forGetter(CustomPackEntry::format), (App)Entry.MAP_CODEC.forGetter(CustomPackEntry::entry)).apply((Applicative)i, CustomPackEntry::new));
    }

    private static enum Format implements StringRepresentable
    {
        STRUCTURE("structure"),
        MCFUNCTION("mcfunction");

        public static final Codec<Format> CODEC;
        private final String name;

        private Format(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Format::values);
        }
    }
}

