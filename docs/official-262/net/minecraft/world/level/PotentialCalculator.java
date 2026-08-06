/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package net.minecraft.world.level;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;

public class PotentialCalculator {
    private final List<PointCharge> charges = Lists.newArrayList();

    public void addCharge(BlockPos pos, double charge) {
        if (charge != 0.0) {
            this.charges.add(new PointCharge(pos, charge));
        }
    }

    public double getPotentialEnergyChange(BlockPos pos, double charge) {
        if (charge == 0.0) {
            return 0.0;
        }
        double potentialChange = 0.0;
        for (PointCharge point : this.charges) {
            potentialChange += point.getPotentialChange(pos);
        }
        return potentialChange * charge;
    }

    private static class PointCharge {
        private final BlockPos pos;
        private final double charge;

        public PointCharge(BlockPos pos, double charge) {
            this.pos = pos;
            this.charge = charge;
        }

        public double getPotentialChange(BlockPos pos) {
            double distSqr = this.pos.distSqr(pos);
            if (distSqr == 0.0) {
                return Double.POSITIVE_INFINITY;
            }
            return this.charge / Math.sqrt(distSqr);
        }
    }
}

