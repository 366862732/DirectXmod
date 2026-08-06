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
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class PillagerOutpostPools {
    public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("pillager_outpost/base_plates");

    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
        Holder.Reference<StructureProcessorList> outpostRot = processorLists.getOrThrow(ProcessorLists.OUTPOST_ROT);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder.Reference<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
        context.register(START, new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/base_plate"), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "pillager_outpost/towers", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.list((List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>>)ImmutableList.of(StructurePoolElement.legacy("pillager_outpost/watchtower"), StructurePoolElement.legacy("pillager_outpost/watchtower_overgrown", outpostRot))), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "pillager_outpost/feature_plates", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_plate"), (Object)1)), StructureTemplatePool.Projection.TERRAIN_MATCHING));
        Pools.register(context, "pillager_outpost/features", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_cage1"), (Object)1), (Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_cage2"), (Object)1), (Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_cage_with_allays"), (Object)1), (Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_logs"), (Object)1), (Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_tent1"), (Object)1), (Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_tent2"), (Object)1), (Object)Pair.of(StructurePoolElement.legacy("pillager_outpost/feature_targets"), (Object)1), (Object)Pair.of(StructurePoolElement.empty(), (Object)6)), StructureTemplatePool.Projection.RIGID));
    }
}

