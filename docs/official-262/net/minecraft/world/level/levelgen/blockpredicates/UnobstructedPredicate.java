/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.phys.shapes.Shapes;

public record UnobstructedPredicate(Vec3i offset) implements BlockPredicate
{
    public static final MapCodec<UnobstructedPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Vec3i.CODEC.optionalFieldOf("offset", (Object)Vec3i.ZERO).forGetter(UnobstructedPredicate::offset)).apply((Applicative)i, UnobstructedPredicate::new));

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.UNOBSTRUCTED;
    }

    @Override
    public boolean test(WorldGenLevel worldGenLevel, BlockPos pos) {
        return worldGenLevel.isUnobstructed(null, Shapes.block().move(pos));
    }
}

