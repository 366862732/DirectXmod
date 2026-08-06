/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;

public class AxisAlignedLinearPosTest
extends PosRuleTest {
    public static final MapCodec<AxisAlignedLinearPosTest> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.optionalFieldOf("min_chance", (Object)Float.valueOf(0.0f)).forGetter(p -> Float.valueOf(p.minChance)), (App)Codec.FLOAT.optionalFieldOf("max_chance", (Object)Float.valueOf(0.0f)).forGetter(p -> Float.valueOf(p.maxChance)), (App)Codec.INT.optionalFieldOf("min_dist", (Object)0).forGetter(p -> p.minDist), (App)Codec.INT.optionalFieldOf("max_dist", (Object)0).forGetter(p -> p.maxDist), (App)Direction.Axis.CODEC.optionalFieldOf("axis", Direction.Axis.Y).forGetter(p -> p.axis)).apply((Applicative)i, AxisAlignedLinearPosTest::new));
    private final float minChance;
    private final float maxChance;
    private final int minDist;
    private final int maxDist;
    private final Direction.Axis axis;

    public AxisAlignedLinearPosTest(float minChance, float maxChance, int minDist, int maxDist, Direction.Axis axis) {
        if (minDist >= maxDist) {
            throw new IllegalArgumentException("Invalid range: [" + minDist + "," + maxDist + "]");
        }
        this.minChance = minChance;
        this.maxChance = maxChance;
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.axis = axis;
    }

    @Override
    public boolean test(BlockPos inTemplatePos, BlockPos worldPos, BlockPos worldReference, RandomSource random) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, this.axis);
        float xd = Math.abs((worldPos.getX() - worldReference.getX()) * direction.getStepX());
        float yd = Math.abs((worldPos.getY() - worldReference.getY()) * direction.getStepY());
        float zd = Math.abs((worldPos.getZ() - worldReference.getZ()) * direction.getStepZ());
        int dist = (int)(xd + yd + zd);
        float rnd = random.nextFloat();
        return rnd <= Mth.clampedLerp(Mth.inverseLerp(dist, this.minDist, this.maxDist), this.minChance, this.maxChance);
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
    }
}

