/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Containers {
    public static void dropContents(Level level, BlockPos pos, Container container) {
        Containers.dropContents(level, pos.getX(), pos.getY(), pos.getZ(), container);
    }

    public static void dropContents(Level level, Entity entity, Container container) {
        Containers.dropContents(level, entity.getX(), entity.getY(), entity.getZ(), container);
    }

    private static void dropContents(Level level, double x, double y, double z, Container container) {
        for (int i = 0; i < container.getContainerSize(); ++i) {
            Containers.dropItemStack(level, x, y, z, container.getItem(i));
        }
    }

    public static void dropContents(Level level, BlockPos pos, NonNullList<ItemStack> list) {
        list.forEach(itemStack -> Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemStack));
    }

    public static void dropItemStack(Level level, double x, double y, double z, ItemStack itemStack) {
        double size = EntityTypes.ITEM.getWidth();
        double centerRange = 1.0 - size;
        double halfSize = size / 2.0;
        RandomSource random = level.getRandom();
        double xo = Math.floor(x) + random.nextDouble() * centerRange + halfSize;
        double yo = Math.floor(y) + random.nextDouble() * centerRange;
        double zo = Math.floor(z) + random.nextDouble() * centerRange + halfSize;
        while (!itemStack.isEmpty()) {
            ItemEntity entity = new ItemEntity(level, xo, yo, zo, itemStack.split(random.nextInt(21) + 10));
            float pow = 0.05f;
            entity.setDeltaMovement(random.triangle(0.0, 0.11485000171139836), random.triangle(0.2, 0.11485000171139836), random.triangle(0.0, 0.11485000171139836));
            level.addFreshEntity(entity);
        }
    }

    public static void updateNeighboursAfterDestroy(BlockState state, Level level, BlockPos pos) {
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
    }
}

