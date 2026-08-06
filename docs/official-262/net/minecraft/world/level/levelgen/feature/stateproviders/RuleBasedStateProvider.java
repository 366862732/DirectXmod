/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import org.jspecify.annotations.Nullable;

public class RuleBasedStateProvider
extends BlockStateProvider {
    public static final MapCodec<RuleBasedStateProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BlockStateProvider.CODEC.optionalFieldOf("fallback").forGetter(provider -> Optional.ofNullable(provider.fallback)), (App)Rule.CODEC.listOf().fieldOf("rules").forGetter(p -> p.rules)).apply((Applicative)i, RuleBasedStateProvider::new));
    private final @Nullable BlockStateProvider fallback;
    private final List<Rule> rules;

    public RuleBasedStateProvider(@Nullable BlockStateProvider fallback, List<Rule> rules) {
        this.fallback = fallback;
        this.rules = rules;
    }

    private RuleBasedStateProvider(Optional<BlockStateProvider> fallback, List<Rule> rules) {
        this((BlockStateProvider)fallback.orElse(null), rules);
    }

    public static RuleBasedStateProvider ifTrueThenProvide(BlockPredicate ifTrue, Block thenProvide) {
        return RuleBasedStateProvider.ifTrueThenProvide(ifTrue, BlockStateProvider.simple(thenProvide));
    }

    public static RuleBasedStateProvider ifTrueThenProvide(BlockPredicate ifTrue, BlockStateProvider thenProvide) {
        return new RuleBasedStateProvider((BlockStateProvider)null, List.of(new Rule(ifTrue, thenProvide)));
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.RULE_BASED_STATE_PROVIDER;
    }

    @Override
    public BlockState getState(WorldGenLevel level, RandomSource random, BlockPos pos) {
        BlockState result = this.getOptionalState(level, random, pos);
        return result != null ? result : level.getBlockState(pos);
    }

    @Override
    public @Nullable BlockState getOptionalState(WorldGenLevel level, RandomSource random, BlockPos pos) {
        for (Rule rule : this.rules) {
            if (!rule.ifTrue().test(level, pos)) continue;
            return rule.then().getState(level, random, pos);
        }
        return this.fallback == null ? null : this.fallback.getState(level, random, pos);
    }

    public static Builder builder() {
        return new Builder(null);
    }

    public static Builder builder(@Nullable BlockStateProvider fallback) {
        return new Builder(fallback);
    }

    public record Rule(BlockPredicate ifTrue, BlockStateProvider then) {
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockPredicate.CODEC.fieldOf("if_true").forGetter(Rule::ifTrue), (App)BlockStateProvider.CODEC.fieldOf("then").forGetter(Rule::then)).apply((Applicative)i, Rule::new));
    }

    public static class Builder {
        private final @Nullable BlockStateProvider fallback;
        private final List<Rule> rules = new ArrayList<Rule>();

        public Builder(@Nullable BlockStateProvider fallback) {
            this.fallback = fallback;
        }

        public Builder ifTrueThenProvide(BlockPredicate ifTrue, BlockStateProvider thenProvide) {
            this.rules.add(new Rule(ifTrue, thenProvide));
            return this;
        }

        public Builder ifTrueThenProvide(BlockPredicate ifTrue, Block thenProvide) {
            this.rules.add(new Rule(ifTrue, BlockStateProvider.simple(thenProvide)));
            return this;
        }

        public Builder ifTrueThenProvide(BlockPredicate ifTrue, BlockState thenProvide) {
            this.rules.add(new Rule(ifTrue, BlockStateProvider.simple(thenProvide)));
            return this;
        }

        public RuleBasedStateProvider build() {
            return new RuleBasedStateProvider(this.fallback, this.rules);
        }
    }
}

