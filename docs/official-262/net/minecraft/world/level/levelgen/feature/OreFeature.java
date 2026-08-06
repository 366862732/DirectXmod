/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class OreFeature
extends Feature<OreConfiguration> {
    public OreFeature(Codec<OreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        OreConfiguration config = context.config();
        float dir = random.nextFloat() * (float)Math.PI;
        float spreadXY = (float)config.size / 8.0f;
        int maxRadius = Mth.ceil(((float)config.size / 16.0f * 2.0f + 1.0f) / 2.0f);
        double x0 = (double)origin.getX() + Math.sin(dir) * (double)spreadXY;
        double x1 = (double)origin.getX() - Math.sin(dir) * (double)spreadXY;
        double z0 = (double)origin.getZ() + Math.cos(dir) * (double)spreadXY;
        double z1 = (double)origin.getZ() - Math.cos(dir) * (double)spreadXY;
        int spreadY = 2;
        double y0 = origin.getY() + random.nextInt(3) - 2;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        int xStart = origin.getX() - Mth.ceil(spreadXY) - maxRadius;
        int yStart = origin.getY() - 2 - maxRadius;
        int zStart = origin.getZ() - Mth.ceil(spreadXY) - maxRadius;
        int sizeXZ = 2 * (Mth.ceil(spreadXY) + maxRadius);
        int sizeY = 2 * (2 + maxRadius);
        for (int xprobe = xStart; xprobe <= xStart + sizeXZ; ++xprobe) {
            for (int zprobe = zStart; zprobe <= zStart + sizeXZ; ++zprobe) {
                if (yStart > level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, xprobe, zprobe)) continue;
                return this.doPlace(level, random, config, x0, x1, z0, z1, y0, y1, xStart, yStart, zStart, sizeXZ, sizeY);
            }
        }
        return false;
    }

    protected boolean doPlace(WorldGenLevel level, RandomSource random, OreConfiguration config, double x0, double x1, double z0, double z1, double y0, double y1, int xStart, int yStart, int zStart, int sizeXZ, int sizeY) {
        int placed = 0;
        BitSet tested = new BitSet(sizeXZ * sizeY * sizeXZ);
        BlockPos.MutableBlockPos orePos = new BlockPos.MutableBlockPos();
        int size = config.size;
        double[] data = new double[size * 4];
        for (int i = 0; i < size; ++i) {
            float step = (float)i / (float)size;
            double xx = Mth.lerp((double)step, x0, x1);
            double yy = Mth.lerp((double)step, y0, y1);
            double zz = Mth.lerp((double)step, z0, z1);
            double ss = random.nextDouble() * (double)size / 16.0;
            double r = ((double)(Mth.sin((float)Math.PI * step) + 1.0f) * ss + 1.0) / 2.0;
            data[i * 4 + 0] = xx;
            data[i * 4 + 1] = yy;
            data[i * 4 + 2] = zz;
            data[i * 4 + 3] = r;
        }
        for (int i1 = 0; i1 < size - 1; ++i1) {
            if (data[i1 * 4 + 3] <= 0.0) continue;
            for (int i2 = i1 + 1; i2 < size; ++i2) {
                double dz;
                double dy;
                double dx;
                double dr;
                if (data[i2 * 4 + 3] <= 0.0 || !((dr = data[i1 * 4 + 3] - data[i2 * 4 + 3]) * dr > (dx = data[i1 * 4 + 0] - data[i2 * 4 + 0]) * dx + (dy = data[i1 * 4 + 1] - data[i2 * 4 + 1]) * dy + (dz = data[i1 * 4 + 2] - data[i2 * 4 + 2]) * dz)) continue;
                if (dr > 0.0) {
                    data[i2 * 4 + 3] = -1.0;
                    continue;
                }
                data[i1 * 4 + 3] = -1.0;
            }
        }
        try (BulkSectionAccess sectionGetter = new BulkSectionAccess(level);){
            for (int i = 0; i < size; ++i) {
                double r = data[i * 4 + 3];
                if (r < 0.0) continue;
                double xx = data[i * 4 + 0];
                double yy = data[i * 4 + 1];
                double zz = data[i * 4 + 2];
                int xMin = Math.max(Mth.floor(xx - r), xStart);
                int yMin = Math.max(Mth.floor(yy - r), yStart);
                int zMin = Math.max(Mth.floor(zz - r), zStart);
                int xMax = Math.max(Mth.floor(xx + r), xMin);
                int yMax = Math.max(Mth.floor(yy + r), yMin);
                int zMax = Math.max(Mth.floor(zz + r), zMin);
                for (int x = xMin; x <= xMax; ++x) {
                    double xd = ((double)x + 0.5 - xx) / r;
                    if (!(xd * xd < 1.0)) continue;
                    for (int y = yMin; y <= yMax; ++y) {
                        double yd = ((double)y + 0.5 - yy) / r;
                        if (!(xd * xd + yd * yd < 1.0)) continue;
                        block11: for (int z = zMin; z <= zMax; ++z) {
                            LevelChunkSection section;
                            int bitSetIndex;
                            double zd = ((double)z + 0.5 - zz) / r;
                            if (!(xd * xd + yd * yd + zd * zd < 1.0) || level.isOutsideBuildHeight(y) || tested.get(bitSetIndex = x - xStart + (y - yStart) * sizeXZ + (z - zStart) * sizeXZ * sizeY)) continue;
                            tested.set(bitSetIndex);
                            orePos.set(x, y, z);
                            if (!level.ensureCanWrite(orePos) || (section = sectionGetter.getSection(orePos)) == null) continue;
                            int sectionRelativeX = SectionPos.sectionRelative(x);
                            int sectionRelativeY = SectionPos.sectionRelative(y);
                            int sectionRelativeZ = SectionPos.sectionRelative(z);
                            BlockState blockState = section.getBlockState(sectionRelativeX, sectionRelativeY, sectionRelativeZ);
                            for (OreConfiguration.TargetBlockState targetState : config.targetStates) {
                                if (!OreFeature.canPlaceOre(blockState, sectionGetter::getBlockState, random, config, targetState, orePos)) continue;
                                section.setBlockState(sectionRelativeX, sectionRelativeY, sectionRelativeZ, targetState.state, false);
                                ++placed;
                                continue block11;
                            }
                        }
                    }
                }
            }
        }
        return placed > 0;
    }

    public static boolean canPlaceOre(BlockState orePosState, Function<BlockPos, BlockState> blockGetter, RandomSource random, OreConfiguration config, OreConfiguration.TargetBlockState targetState, BlockPos.MutableBlockPos orePos) {
        if (!targetState.target.test(orePosState, random)) {
            return false;
        }
        if (OreFeature.shouldSkipAirCheck(random, config.discardChanceOnAirExposure)) {
            return true;
        }
        return !OreFeature.isAdjacentToAir(blockGetter, orePos);
    }

    protected static boolean shouldSkipAirCheck(RandomSource random, float discardChanceOnAirExposure) {
        if (discardChanceOnAirExposure <= 0.0f) {
            return true;
        }
        if (discardChanceOnAirExposure >= 1.0f) {
            return false;
        }
        return random.nextFloat() >= discardChanceOnAirExposure;
    }
}

