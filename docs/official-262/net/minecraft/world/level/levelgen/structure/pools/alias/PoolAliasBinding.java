/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.DirectPoolAlias;
import net.minecraft.world.level.levelgen.structure.pools.alias.RandomGroupPoolAlias;
import net.minecraft.world.level.levelgen.structure.pools.alias.RandomPoolAlias;

public interface PoolAliasBinding {
    public static final Codec<PoolAliasBinding> CODEC = BuiltInRegistries.POOL_ALIAS_BINDING_TYPE.byNameCodec().dispatch(PoolAliasBinding::codec, Function.identity());

    public void forEachResolved(RandomSource var1, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> var2);

    public Stream<ResourceKey<StructureTemplatePool>> allTargets();

    public static DirectPoolAlias direct(String id, String target) {
        return PoolAliasBinding.direct(Pools.createKey(id), Pools.createKey(target));
    }

    public static DirectPoolAlias direct(ResourceKey<StructureTemplatePool> alias, ResourceKey<StructureTemplatePool> target) {
        return new DirectPoolAlias(alias, target);
    }

    public static RandomPoolAlias random(String id, WeightedList<String> targets) {
        WeightedList.Builder targetPools = WeightedList.builder();
        targets.unwrap().forEach(wrapper -> targetPools.add(Pools.createKey((String)wrapper.value()), wrapper.weight()));
        return PoolAliasBinding.random(Pools.createKey(id), targetPools.build());
    }

    public static RandomPoolAlias random(ResourceKey<StructureTemplatePool> id, WeightedList<ResourceKey<StructureTemplatePool>> targets) {
        return new RandomPoolAlias(id, targets);
    }

    public static RandomGroupPoolAlias randomGroup(WeightedList<List<PoolAliasBinding>> combinations) {
        return new RandomGroupPoolAlias(combinations);
    }

    public MapCodec<? extends PoolAliasBinding> codec();
}

