/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Lifecycle
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  org.slf4j.Logger
 */
package net.minecraft.data.loot;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import org.slf4j.Logger;

public class LootTableProvider
implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput.PathProvider pathProvider;
    private final Set<ResourceKey<LootTable>> requiredTables;
    private final List<SubProviderEntry> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public LootTableProvider(PackOutput output, Set<ResourceKey<LootTable>> requiredTables, List<SubProviderEntry> subProviders, CompletableFuture<HolderLookup.Provider> registries) {
        this.pathProvider = output.createRegistryElementsPathProvider(Registries.LOOT_TABLE);
        this.subProviders = subProviders;
        this.requiredTables = requiredTables;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.registries.thenCompose(registries -> this.run(cache, (HolderLookup.Provider)registries));
    }

    private CompletableFuture<?> run(CachedOutput cache, HolderLookup.Provider registries) {
        MappedRegistry<LootTable> tables = new MappedRegistry<LootTable>(Registries.LOOT_TABLE, Lifecycle.experimental());
        Object2ObjectOpenHashMap randomSequenceSeeds = new Object2ObjectOpenHashMap();
        this.subProviders.forEach(arg_0 -> LootTableProvider.lambda$run$1(registries, (Map)randomSequenceSeeds, tables, arg_0));
        tables.freeze();
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        RegistryAccess.Frozen validationProvider = new RegistryAccess.ImmutableRegistryAccess(List.of(tables)).freeze();
        ValidationContextSource validationContext = new ValidationContextSource(problems, validationProvider);
        Sets.SetView missingTables = Sets.difference(this.requiredTables, tables.registryKeySet());
        for (ResourceKey missingTable : missingTables) {
            problems.report(new MissingTableProblem(missingTable));
        }
        LootDataType.TABLE.runValidation(validationContext, tables);
        if (!problems.isEmpty()) {
            problems.forEach((id, problem) -> LOGGER.warn("Found validation problem in {}: {}", id, (Object)problem.description()));
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        }
        return CompletableFuture.allOf((CompletableFuture[])tables.entrySet().stream().map(entry -> {
            ResourceKey id = (ResourceKey)entry.getKey();
            LootTable table = (LootTable)entry.getValue();
            Path path = this.pathProvider.json(id.identifier());
            return DataProvider.saveStable(cache, registries, LootTable.DIRECT_CODEC, table, path);
        }).toArray(CompletableFuture[]::new));
    }

    private static Identifier sequenceIdForLootTable(ResourceKey<LootTable> id) {
        return id.identifier();
    }

    @Override
    public final String getName() {
        return "Loot Tables";
    }

    private static /* synthetic */ void lambda$run$1(HolderLookup.Provider registries, Map randomSequenceSeeds, WritableRegistry tables, SubProviderEntry subProvider) {
        subProvider.provider().apply(registries).generate((id, lootTable) -> {
            Identifier sequenceId = LootTableProvider.sequenceIdForLootTable(id);
            Identifier previous = randomSequenceSeeds.put(RandomSequence.seedForKey(sequenceId), sequenceId);
            if (previous != null) {
                Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + String.valueOf(previous) + " and " + String.valueOf(id.identifier()));
            }
            lootTable.setRandomSequence(sequenceId);
            LootTable table = lootTable.setParamSet(subProvider.paramSet).build();
            tables.register(id, table, RegistrationInfo.BUILT_IN);
        });
    }

    public record MissingTableProblem(ResourceKey<LootTable> id) implements ProblemReporter.Problem
    {
        @Override
        public String description() {
            return "Missing built-in table: " + String.valueOf(this.id.identifier());
        }
    }

    public record SubProviderEntry(Function<HolderLookup.Provider, LootTableSubProvider> provider, ContextKeySet paramSet) {
    }
}

