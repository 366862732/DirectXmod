/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.Products$P3
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Instance
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Mu
 */
package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.datafixers.Products;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public abstract class TrunkPlacer {
    public static final Codec<TrunkPlacer> CODEC = BuiltInRegistries.TRUNK_PLACER_TYPE.byNameCodec().dispatch(TrunkPlacer::type, TrunkPlacerType::codec);
    private static final int MAX_BASE_HEIGHT = 32;
    private static final int MAX_RAND = 24;
    public static final int MAX_HEIGHT = 80;
    protected final int baseHeight;
    protected final int heightRandA;
    protected final int heightRandB;

    protected static <P extends TrunkPlacer> Products.P3<RecordCodecBuilder.Mu<P>, Integer, Integer, Integer> trunkPlacerParts(RecordCodecBuilder.Instance<P> instance) {
        return instance.group((App)Codec.intRange((int)0, (int)32).fieldOf("base_height").forGetter(p -> p.baseHeight), (App)Codec.intRange((int)0, (int)24).fieldOf("height_rand_a").forGetter(p -> p.heightRandA), (App)Codec.intRange((int)0, (int)24).fieldOf("height_rand_b").forGetter(p -> p.heightRandB));
    }

    public TrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        this.baseHeight = baseHeight;
        this.heightRandA = heightRandA;
        this.heightRandB = heightRandB;
    }

    protected abstract TrunkPlacerType<?> type();

    public abstract List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel var1, BiConsumer<BlockPos, BlockState> var2, RandomSource var3, int var4, BlockPos var5, TreeConfiguration var6);

    public int getBaseHeight() {
        return this.baseHeight;
    }

    public int getTreeHeight(RandomSource random) {
        return this.baseHeight + random.nextInt(this.heightRandA + 1) + random.nextInt(this.heightRandB + 1);
    }

    protected static void placeBelowTrunkBlock(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, BlockPos pos, TreeConfiguration config) {
        BlockState blockBelowTrunk = config.belowTrunkProvider.getOptionalState(level, random, pos);
        if (blockBelowTrunk != null) {
            trunkSetter.accept(pos, blockBelowTrunk);
        }
    }

    protected boolean placeLog(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, BlockPos pos, TreeConfiguration config) {
        return this.placeLog(level, trunkSetter, random, pos, config, Function.identity());
    }

    protected boolean placeLog(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, BlockPos pos, TreeConfiguration config, Function<BlockState, BlockState> stateModifier) {
        if (this.validTreePos(level, pos)) {
            trunkSetter.accept(pos, stateModifier.apply(config.trunkProvider.getState(level, random, pos)));
            return true;
        }
        return false;
    }

    protected void placeLogIfFree(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter, RandomSource random, BlockPos.MutableBlockPos pos, TreeConfiguration config) {
        if (this.isFree(level, pos)) {
            this.placeLog(level, trunkSetter, random, pos, config);
        }
    }

    protected boolean validTreePos(WorldGenLevel level, BlockPos pos) {
        return TreeFeature.validTreePos(level, pos);
    }

    public boolean isFree(WorldGenLevel level, BlockPos pos) {
        return this.validTreePos(level, pos) || level.isStateAtPosition(pos, state -> state.is(BlockTags.LOGS));
    }
}

