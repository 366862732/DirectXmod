/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlackstoneReplaceProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LavaSubmergedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.NopProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

public class StructureProcessorTypes {
    public static MapCodec<? extends StructureProcessor> bootstrap(Registry<MapCodec<? extends StructureProcessor>> registry) {
        Registry.register(registry, "blackstone_replace", BlackstoneReplaceProcessor.MAP_CODEC);
        Registry.register(registry, "block_age", BlockAgeProcessor.MAP_CODEC);
        Registry.register(registry, "block_ignore", BlockIgnoreProcessor.MAP_CODEC);
        Registry.register(registry, "block_rot", BlockRotProcessor.MAP_CODEC);
        Registry.register(registry, "capped", CappedProcessor.MAP_CODEC);
        Registry.register(registry, "gravity", GravityProcessor.MAP_CODEC);
        Registry.register(registry, "jigsaw_replacement", JigsawReplacementProcessor.MAP_CODEC);
        Registry.register(registry, "lava_submerged_block", LavaSubmergedBlockProcessor.MAP_CODEC);
        Registry.register(registry, "nop", NopProcessor.MAP_CODEC);
        Registry.register(registry, "protected_blocks", ProtectedBlockProcessor.MAP_CODEC);
        return Registry.register(registry, "rule", RuleProcessor.MAP_CODEC);
    }
}

