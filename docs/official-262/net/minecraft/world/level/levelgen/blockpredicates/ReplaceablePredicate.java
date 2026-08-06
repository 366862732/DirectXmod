/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.blockpredicates.StateTestingPredicate;

public class ReplaceablePredicate
extends StateTestingPredicate {
    public static final MapCodec<ReplaceablePredicate> CODEC = RecordCodecBuilder.mapCodec(i -> ReplaceablePredicate.stateTestingCodec(i).apply((Applicative)i, ReplaceablePredicate::new));

    public ReplaceablePredicate(Vec3i offset) {
        super(offset);
    }

    @Override
    protected boolean test(BlockState state) {
        return state.canBeReplaced();
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.REPLACEABLE;
    }
}

