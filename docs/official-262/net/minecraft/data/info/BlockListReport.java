/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 */
package net.minecraft.data.info;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockListReport
implements DataProvider {
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public BlockListReport(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path path = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("blocks.json");
        return this.registries.thenCompose(registries -> {
            JsonObject root = new JsonObject();
            RegistryOps registryOps = registries.createSerializationContext(JsonOps.INSTANCE);
            registries.lookupOrThrow(Registries.BLOCK).listElements().forEach(block -> {
                JsonObject entry = new JsonObject();
                StateDefinition<Block, BlockState> definition = ((Block)block.value()).getStateDefinition();
                if (!definition.getProperties().isEmpty()) {
                    JsonObject properties = new JsonObject();
                    for (Property property : definition.getProperties()) {
                        JsonArray values = new JsonArray();
                        for (Comparable value : property.getPossibleValues()) {
                            values.add(Util.getPropertyName(property, value));
                        }
                        properties.add(property.getName(), (JsonElement)values);
                    }
                    entry.add("properties", (JsonElement)properties);
                }
                JsonArray protocol = new JsonArray();
                for (BlockState blockState : definition.getPossibleStates()) {
                    JsonObject stateEntry = new JsonObject();
                    JsonObject properties = new JsonObject();
                    for (Property<?> property : definition.getProperties()) {
                        properties.addProperty(property.getName(), Util.getPropertyName(property, blockState.getValue(property)));
                    }
                    if (!properties.isEmpty()) {
                        stateEntry.add("properties", (JsonElement)properties);
                    }
                    stateEntry.addProperty("id", (Number)Block.getId(blockState));
                    if (blockState == ((Block)block.value()).defaultBlockState()) {
                        stateEntry.addProperty("default", Boolean.valueOf(true));
                    }
                    protocol.add((JsonElement)stateEntry);
                }
                entry.add("states", (JsonElement)protocol);
                String id = block.getRegisteredName();
                JsonElement jsonElement = (JsonElement)BlockTypes.CODEC.codec().encodeStart((DynamicOps)registryOps, (Object)((Block)block.value())).getOrThrow(msg -> new AssertionError((Object)("Failed to serialize block " + id + " (is type registered in BlockTypes?): " + msg)));
                entry.add("definition", jsonElement);
                root.add(id, (JsonElement)entry);
            });
            return DataProvider.saveStable(cache, (JsonElement)root, path);
        });
    }

    @Override
    public final String getName() {
        return "Block List";
    }
}

