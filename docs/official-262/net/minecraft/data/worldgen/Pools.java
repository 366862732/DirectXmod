/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.AncientCityStructurePieces;
import net.minecraft.data.worldgen.BastionPieces;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.PillagerOutpostPools;
import net.minecraft.data.worldgen.TrailRuinsStructurePools;
import net.minecraft.data.worldgen.TrialChambersStructurePools;
import net.minecraft.data.worldgen.VillagePools;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Pools {
    public static final ResourceKey<StructureTemplatePool> EMPTY = Pools.createKey("empty");

    public static ResourceKey<StructureTemplatePool> createKey(Identifier location) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, location);
    }

    public static ResourceKey<StructureTemplatePool> createKey(String name) {
        return Pools.createKey(Identifier.withDefaultNamespace(name));
    }

    public static ResourceKey<StructureTemplatePool> parseKey(String name) {
        return Pools.createKey(Identifier.parse(name));
    }

    public static void register(BootstrapContext<StructureTemplatePool> context, String name, StructureTemplatePool pool) {
        context.register(Pools.createKey(name), pool);
    }

    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder.Reference<StructureTemplatePool> empty = pools.getOrThrow(EMPTY);
        context.register(EMPTY, new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of(), StructureTemplatePool.Projection.RIGID));
        BastionPieces.bootstrap(context);
        PillagerOutpostPools.bootstrap(context);
        VillagePools.bootstrap(context);
        AncientCityStructurePieces.bootstrap(context);
        TrailRuinsStructurePools.bootstrap(context);
        TrialChambersStructurePools.bootstrap(context);
    }
}

