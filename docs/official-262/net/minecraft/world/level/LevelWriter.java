/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface LevelWriter {
    public boolean setBlock(BlockPos var1, BlockState var2, @Block.UpdateFlags int var3, int var4);

    default public boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags) {
        return this.setBlock(pos, blockState, updateFlags, 512);
    }

    public boolean removeBlock(BlockPos var1, boolean var2);

    default public boolean destroyBlock(BlockPos pos, boolean dropResources) {
        return this.destroyBlock(pos, dropResources, null);
    }

    default public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker) {
        return this.destroyBlock(pos, dropResources, breaker, 512);
    }

    public boolean destroyBlock(BlockPos var1, boolean var2, @Nullable Entity var3, int var4);

    default public boolean addFreshEntity(Entity entity) {
        return false;
    }
}

