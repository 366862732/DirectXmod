/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 */
package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;

@FunctionalInterface
public interface PoolAliasLookup {
    public static final PoolAliasLookup EMPTY = key -> key;

    public ResourceKey<StructureTemplatePool> lookup(ResourceKey<StructureTemplatePool> var1);

    public static PoolAliasLookup create(List<PoolAliasBinding> poolAliasBindings, BlockPos pos, long seed) {
        if (poolAliasBindings.isEmpty()) {
            return EMPTY;
        }
        RandomSource random = RandomSource.create(seed).forkPositional().at(pos);
        ImmutableMap.Builder builder = ImmutableMap.builder();
        poolAliasBindings.forEach(binding -> binding.forEachResolved(random, (arg_0, arg_1) -> ((ImmutableMap.Builder)builder).put(arg_0, arg_1)));
        ImmutableMap aliasMappings = builder.build();
        return arg_0 -> PoolAliasLookup.lambda$create$1((Map)aliasMappings, arg_0);
    }

    private static /* synthetic */ ResourceKey lambda$create$1(Map aliasMappings, ResourceKey resourceKey) {
        return Objects.requireNonNull(aliasMappings.getOrDefault(resourceKey, resourceKey), () -> "alias " + String.valueOf(resourceKey.identifier()) + " was mapped to null value");
    }
}

