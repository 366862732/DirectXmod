/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

public class RegistryDumpReport
implements DataProvider {
    private final PackOutput output;

    public RegistryDumpReport(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject root = new JsonObject();
        BuiltInRegistries.REGISTRY.listElements().forEach(e -> root.add(e.key().identifier().toString(), RegistryDumpReport.dumpRegistry((Registry)e.value())));
        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("registries.json");
        return DataProvider.saveStable(cache, (JsonElement)root, path);
    }

    private static <T> JsonElement dumpRegistry(Registry<T> registry) {
        JsonObject result = new JsonObject();
        if (registry instanceof DefaultedRegistry) {
            Identifier defaultKey = ((DefaultedRegistry)registry).getDefaultKey();
            result.addProperty("default", defaultKey.toString());
        }
        int registryId = BuiltInRegistries.REGISTRY.getId(registry);
        result.addProperty("protocol_id", (Number)registryId);
        JsonObject entries = new JsonObject();
        registry.listElements().forEach(holder -> {
            Object value = holder.value();
            int protocolId = registry.getId(value);
            JsonObject entry = new JsonObject();
            entry.addProperty("protocol_id", (Number)protocolId);
            entries.add(holder.key().identifier().toString(), (JsonElement)entry);
        });
        result.add("entries", (JsonElement)entries);
        return result;
    }

    @Override
    public final String getName() {
        return "Registry Dump";
    }
}

