/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityType<T extends BlockEntity> {
    private final BlockEntitySupplier<? extends T> factory;
    private final Set<Block> validBlocks;
    private final Holder.Reference<BlockEntityType<?>> builtInRegistryHolder = BuiltInRegistries.BLOCK_ENTITY_TYPE.createIntrusiveHolder(this);

    public BlockEntityType(BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
        this.factory = factory;
        this.validBlocks = validBlocks;
    }

    public T create(BlockPos worldPosition, BlockState blockState) {
        return this.factory.create(worldPosition, blockState);
    }

    public boolean isValid(BlockState state) {
        return this.validBlocks.contains(state.getBlock());
    }

    @Deprecated
    public Holder.Reference<BlockEntityType<?>> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    public @Nullable T getBlockEntity(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null || entity.getType() != this) {
            return null;
        }
        return (T)entity;
    }

    public boolean onlyOpCanSetNbt() {
        return BlockEntityTypes.OP_ONLY_CUSTOM_DATA.contains(this);
    }

    @FunctionalInterface
    public static interface BlockEntitySupplier<T extends BlockEntity> {
        public T create(BlockPos var1, BlockState var2);
    }
}

