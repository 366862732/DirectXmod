/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.blockpredicates.CombiningPredicate;

public class AnyOfPredicate
extends CombiningPredicate {
    public static final MapCodec<AnyOfPredicate> CODEC = AnyOfPredicate.codec(AnyOfPredicate::new);

    public AnyOfPredicate(List<BlockPredicate> predicates) {
        super(predicates);
    }

    @Override
    public boolean test(WorldGenLevel level, BlockPos origin) {
        for (BlockPredicate predicate : this.predicates) {
            if (!predicate.test(level, origin)) continue;
            return true;
        }
        return false;
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.ANY_OF;
    }
}

