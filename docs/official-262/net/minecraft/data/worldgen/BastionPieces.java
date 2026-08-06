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
import net.minecraft.data.worldgen.BastionBridgePools;
import net.minecraft.data.worldgen.BastionHoglinStablePools;
import net.minecraft.data.worldgen.BastionHousingUnitsPools;
import net.minecraft.data.worldgen.BastionSharedPools;
import net.minecraft.data.worldgen.BastionTreasureRoomPools;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class BastionPieces {
    public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("bastion/starts");

    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
        Holder.Reference<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder.Reference<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
        context.register(START, new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/units/air_base", bastionGenericDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/hoglin_stable/air_base", bastionGenericDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/treasure/big_air_full", bastionGenericDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_base", bastionGenericDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
        BastionHousingUnitsPools.bootstrap(context);
        BastionHoglinStablePools.bootstrap(context);
        BastionTreasureRoomPools.bootstrap(context);
        BastionBridgePools.bootstrap(context);
        BastionSharedPools.bootstrap(context);
    }
}

