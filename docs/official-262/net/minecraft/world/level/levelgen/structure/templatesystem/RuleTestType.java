/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public interface RuleTestType<P extends RuleTest> {
    public static final RuleTestType<AlwaysTrueTest> ALWAYS_TRUE_TEST = RuleTestType.register("always_true", AlwaysTrueTest.CODEC);
    public static final RuleTestType<BlockMatchTest> BLOCK_TEST = RuleTestType.register("block_match", BlockMatchTest.CODEC);
    public static final RuleTestType<BlockStateMatchTest> BLOCKSTATE_TEST = RuleTestType.register("blockstate_match", BlockStateMatchTest.CODEC);
    public static final RuleTestType<TagMatchTest> TAG_TEST = RuleTestType.register("tag_match", TagMatchTest.CODEC);
    public static final RuleTestType<RandomBlockMatchTest> RANDOM_BLOCK_TEST = RuleTestType.register("random_block_match", RandomBlockMatchTest.CODEC);
    public static final RuleTestType<RandomBlockStateMatchTest> RANDOM_BLOCKSTATE_TEST = RuleTestType.register("random_blockstate_match", RandomBlockStateMatchTest.CODEC);

    public MapCodec<P> codec();

    public static <P extends RuleTest> RuleTestType<P> register(String id, MapCodec<P> codec) {
        return Registry.register(BuiltInRegistries.RULE_TEST, id, () -> codec);
    }
}

