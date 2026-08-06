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
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class BastionBridgePools {
    public static void bootstrap(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
        Holder.Reference<StructureProcessorList> entranceReplacement = processorLists.getOrThrow(ProcessorLists.ENTRANCE_REPLACEMENT);
        Holder.Reference<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);
        Holder.Reference<StructureProcessorList> bridge = processorLists.getOrThrow(ProcessorLists.BRIDGE);
        Holder.Reference<StructureProcessorList> rampartDegradation = processorLists.getOrThrow(ProcessorLists.RAMPART_DEGRADATION);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder.Reference<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
        Pools.register(context, "bastion/bridge/starting_pieces", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance", entranceReplacement), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_face", bastionGenericDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "bastion/bridge/bridge_pieces", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/bridge_pieces/bridge", bridge), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "bastion/bridge/legs", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/legs/leg_0", bastionGenericDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/bridge/legs/leg_1", bastionGenericDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "bastion/bridge/walls", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/walls/wall_base_0", rampartDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/bridge/walls/wall_base_1", rampartDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "bastion/bridge/ramparts", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/ramparts/rampart_0", rampartDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/bridge/ramparts/rampart_1", rampartDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "bastion/bridge/rampart_plates", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/rampart_plates/plate_0", rampartDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
        Pools.register(context, "bastion/bridge/connectors", new StructureTemplatePool(empty, (List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>>)ImmutableList.of((Object)Pair.of(StructurePoolElement.single("bastion/bridge/connectors/back_bridge_top", bastionGenericDegradation), (Object)1), (Object)Pair.of(StructurePoolElement.single("bastion/bridge/connectors/back_bridge_bottom", bastionGenericDegradation), (Object)1)), StructureTemplatePool.Projection.RIGID));
    }
}

