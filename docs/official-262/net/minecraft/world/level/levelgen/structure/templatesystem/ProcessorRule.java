/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Passthrough;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;
import org.jspecify.annotations.Nullable;

public class ProcessorRule {
    public static final Passthrough DEFAULT_BLOCK_ENTITY_MODIFIER = Passthrough.INSTANCE;
    public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create(i -> i.group((App)RuleTest.CODEC.fieldOf("input_predicate").forGetter(r -> r.inputPredicate), (App)RuleTest.CODEC.fieldOf("location_predicate").forGetter(r -> r.locPredicate), (App)PosRuleTest.CODEC.lenientOptionalFieldOf("position_predicate", (Object)PosAlwaysTrueTest.INSTANCE).forGetter(r -> r.posPredicate), (App)BlockState.CODEC.fieldOf("output_state").forGetter(r -> r.outputState), (App)RuleBlockEntityModifier.CODEC.lenientOptionalFieldOf("block_entity_modifier", (Object)DEFAULT_BLOCK_ENTITY_MODIFIER).forGetter(r -> r.blockEntityModifier)).apply((Applicative)i, ProcessorRule::new));
    private final RuleTest inputPredicate;
    private final RuleTest locPredicate;
    private final PosRuleTest posPredicate;
    private final BlockState outputState;
    private final RuleBlockEntityModifier blockEntityModifier;

    public ProcessorRule(RuleTest inputPredicate, RuleTest locPredicate, BlockState outputState) {
        this(inputPredicate, locPredicate, PosAlwaysTrueTest.INSTANCE, outputState);
    }

    public ProcessorRule(RuleTest inputPredicate, RuleTest locPredicate, PosRuleTest posPredicate, BlockState outputState) {
        this(inputPredicate, locPredicate, posPredicate, outputState, DEFAULT_BLOCK_ENTITY_MODIFIER);
    }

    public ProcessorRule(RuleTest inputPredicate, RuleTest locPredicate, PosRuleTest posPredicate, BlockState outputState, RuleBlockEntityModifier blockEntityModifier) {
        this.inputPredicate = inputPredicate;
        this.locPredicate = locPredicate;
        this.posPredicate = posPredicate;
        this.outputState = outputState;
        this.blockEntityModifier = blockEntityModifier;
    }

    public boolean test(LevelReader level, BlockState inputState, BlockPos inTemplatePos, BlockPos worldPos, BlockPos reference, RandomSource random) {
        return this.inputPredicate.test(inputState, random) && this.locPredicate.testAgainstWorldState(level, worldPos, random) && this.posPredicate.test(inTemplatePos, worldPos, reference, random);
    }

    public BlockState getOutputState() {
        return this.outputState;
    }

    public @Nullable CompoundTag getOutputTag(RandomSource random, @Nullable CompoundTag existingTag) {
        return this.blockEntityModifier.apply(random, existingTag);
    }
}

