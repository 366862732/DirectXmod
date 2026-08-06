/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen;

import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public final class OreVeinifier {
    private static final float VEININESS_THRESHOLD = 0.4f;
    private static final int EDGE_ROUNDOFF_BEGIN = 20;
    private static final double MAX_EDGE_ROUNDOFF = 0.2;
    private static final float VEIN_SOLIDNESS = 0.7f;
    private static final float MIN_RICHNESS = 0.1f;
    private static final float MAX_RICHNESS = 0.3f;
    private static final float MAX_RICHNESS_THRESHOLD = 0.6f;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02f;
    private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3f;

    private OreVeinifier() {
    }

    static NoiseChunk.BlockStateFiller create(DensityFunction veinToggle, DensityFunction veinRidged, DensityFunction veinGap, PositionalRandomFactory oreVeinsPositionalRandomFactory) {
        BlockState defaultState = SharedConstants.DEBUG_ORE_VEINS ? Blocks.AIR.defaultBlockState() : null;
        return context -> {
            double oreVeininessNoiseValue = veinToggle.compute(context);
            int posY = context.blockY();
            VeinType veinType = oreVeininessNoiseValue > 0.0 ? VeinType.COPPER : VeinType.IRON;
            double veininessRidged = Math.abs(oreVeininessNoiseValue);
            int distanceFromTop = veinType.maxY - posY;
            int distanceFromBottom = posY - veinType.minY;
            if (distanceFromBottom < 0 || distanceFromTop < 0) {
                return defaultState;
            }
            int distanceFromEdge = Math.min(distanceFromTop, distanceFromBottom);
            double edgeRoundoff = Mth.clampedMap((double)distanceFromEdge, 0.0, 20.0, -0.2, 0.0);
            if (veininessRidged + edgeRoundoff < (double)0.4f) {
                return defaultState;
            }
            RandomSource positionalRandom = oreVeinsPositionalRandomFactory.at(context.blockX(), posY, context.blockZ());
            if (positionalRandom.nextFloat() > 0.7f) {
                return defaultState;
            }
            if (veinRidged.compute(context) >= 0.0) {
                return defaultState;
            }
            double richness = Mth.clampedMap(veininessRidged, (double)0.4f, (double)0.6f, (double)0.1f, (double)0.3f);
            if ((double)positionalRandom.nextFloat() < richness && veinGap.compute(context) > (double)-0.3f) {
                return positionalRandom.nextFloat() < 0.02f ? veinType.rawOreBlock : veinType.ore;
            }
            return SharedConstants.DEBUG_ORE_VEINS ? Blocks.OAK_BUTTON.defaultBlockState() : veinType.filler;
        };
    }

    protected static enum VeinType {
        COPPER(Blocks.COPPER_ORE.defaultBlockState(), Blocks.RAW_COPPER_BLOCK.defaultBlockState(), Blocks.GRANITE.defaultBlockState(), 0, 50),
        IRON(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(), Blocks.TUFF.defaultBlockState(), -60, -8);

        private final BlockState ore;
        private final BlockState rawOreBlock;
        private final BlockState filler;
        protected final int minY;
        protected final int maxY;

        private VeinType(BlockState ore, BlockState rawOreBlock, BlockState filler, int minY, int maxY) {
            this.ore = ore;
            this.rawOreBlock = rawOreBlock;
            this.filler = filler;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}

