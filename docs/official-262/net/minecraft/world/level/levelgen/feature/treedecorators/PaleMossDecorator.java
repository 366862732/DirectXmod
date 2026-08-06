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
package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

public class PaleMossDecorator
extends TreeDecorator {
    public static final MapCodec<PaleMossDecorator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("leaves_probability").forGetter(p -> Float.valueOf(p.leavesProbability)), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("trunk_probability").forGetter(p -> Float.valueOf(p.trunkProbability)), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("ground_probability").forGetter(p -> Float.valueOf(p.groundProbability))).apply((Applicative)i, PaleMossDecorator::new));
    private final float leavesProbability;
    private final float trunkProbability;
    private final float groundProbability;

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.PALE_MOSS;
    }

    public PaleMossDecorator(float leavesProbability, float trunkProbability, float groundProbability) {
        this.leavesProbability = leavesProbability;
        this.trunkProbability = trunkProbability;
        this.groundProbability = groundProbability;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        List<BlockPos> logs = Util.shuffledCopy(context.logs(), random);
        if (logs.isEmpty()) {
            return;
        }
        BlockPos origin = Collections.min(logs, Comparator.comparingInt(Vec3i::getY));
        if (random.nextFloat() < this.groundProbability) {
            level.registryAccess().lookup(Registries.CONFIGURED_FEATURE).flatMap(registry -> registry.get(VegetationFeatures.PALE_MOSS_PATCH)).ifPresent(mossPatch -> ((ConfiguredFeature)mossPatch.value()).place(level, level.getLevel().getChunkSource().getGenerator(), random, origin.above()));
        }
        context.logs().forEach(pos -> {
            BlockPos down;
            if (random.nextFloat() < this.trunkProbability && context.isAir(down = pos.below())) {
                PaleMossDecorator.addMossHanger(down, context);
            }
        });
        context.leaves().forEach(pos -> {
            BlockPos down;
            if (random.nextFloat() < this.leavesProbability && context.isAir(down = pos.below())) {
                PaleMossDecorator.addMossHanger(down, context);
            }
        });
    }

    private static void addMossHanger(BlockPos pos, TreeDecorator.Context context) {
        while (context.isAir(pos.below()) && !((double)context.random().nextFloat() < 0.5)) {
            context.setBlock(pos, (BlockState)Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, false));
            pos = pos.below();
        }
        context.setBlock(pos, (BlockState)Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, true));
    }
}

