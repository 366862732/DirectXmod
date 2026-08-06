/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public interface StructurePlacementType<SP extends StructurePlacement> {
    public static final StructurePlacementType<RandomSpreadStructurePlacement> RANDOM_SPREAD = StructurePlacementType.register("random_spread", RandomSpreadStructurePlacement.CODEC);
    public static final StructurePlacementType<ConcentricRingsStructurePlacement> CONCENTRIC_RINGS = StructurePlacementType.register("concentric_rings", ConcentricRingsStructurePlacement.CODEC);

    public MapCodec<SP> codec();

    private static <SP extends StructurePlacement> StructurePlacementType<SP> register(String id, MapCodec<SP> codec) {
        return Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, id, () -> codec);
    }
}

