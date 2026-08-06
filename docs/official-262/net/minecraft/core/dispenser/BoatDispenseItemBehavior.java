/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public class BoatDispenseItemBehavior
extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
    private final EntityType<? extends AbstractBoat> type;

    public BoatDispenseItemBehavior(EntityType<? extends AbstractBoat> type) {
        this.type = type;
    }

    @Override
    public ItemStack execute(BlockSource source, ItemStack dispensed) {
        double yOffset;
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        ServerLevel level = source.level();
        Vec3 center = source.center();
        double justOutsideDispenser = 0.5625 + (double)this.type.getWidth() / 2.0;
        double spawnX = center.x() + (double)direction.getStepX() * justOutsideDispenser;
        double spawnY = center.y() + (double)((float)direction.getStepY() * 1.125f);
        double spawnZ = center.z() + (double)direction.getStepZ() * justOutsideDispenser;
        BlockPos frontPos = source.pos().relative(direction);
        if (level.getFluidState(frontPos).is(FluidTags.WATER)) {
            yOffset = 1.0;
        } else if (level.getBlockState(frontPos).isAir() && level.getFluidState(frontPos.below()).is(FluidTags.WATER)) {
            yOffset = 0.0;
        } else {
            return this.defaultDispenseItemBehavior.dispense(source, dispensed);
        }
        AbstractBoat boat = this.type.create((Level)level, EntitySpawnReason.DISPENSER);
        if (boat != null) {
            boat.setInitialPos(spawnX, spawnY + yOffset, spawnZ);
            EntityType.createDefaultStackConfig(level, dispensed, null).apply(boat);
            boat.setYRot(direction.toYRot());
            level.addFreshEntity(boat);
            dispensed.shrink(1);
        }
        return dispensed;
    }

    @Override
    protected void playSound(BlockSource source) {
        source.level().levelEvent(1000, source.pos(), 0);
    }
}

