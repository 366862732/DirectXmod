/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class StructureBlock
extends BaseEntityBlock
implements GameMasterBlock {
    public static final MapCodec<StructureBlock> CODEC = StructureBlock.simpleCodec(StructureBlock::new);
    public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

    public MapCodec<StructureBlock> codec() {
        return CODEC;
    }

    protected StructureBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(MODE, StructureMode.LOAD));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new StructureBlockEntity(worldPosition, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof StructureBlockEntity) {
            StructureBlockEntity structureBlockEntity = (StructureBlockEntity)blockEntity;
            return structureBlockEntity.usedBy(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        BlockEntity blockEntity;
        if (level.isClientSide()) {
            return;
        }
        if (by != null && (blockEntity = level.getBlockEntity(pos)) instanceof StructureBlockEntity) {
            StructureBlockEntity structureBlockEntity = (StructureBlockEntity)blockEntity;
            structureBlockEntity.createdBy(by);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof StructureBlockEntity)) {
            return;
        }
        StructureBlockEntity structureBlock = (StructureBlockEntity)blockEntity;
        boolean shouldTrigger = level.hasNeighborSignal(pos);
        boolean isPowered = structureBlock.isPowered();
        if (shouldTrigger && !isPowered) {
            structureBlock.setPowered(true);
            this.trigger(serverLevel, structureBlock);
        } else if (!shouldTrigger && isPowered) {
            structureBlock.setPowered(false);
        }
    }

    private void trigger(ServerLevel level, StructureBlockEntity structureBlock) {
        switch (structureBlock.getMode()) {
            case SAVE: {
                structureBlock.saveStructure(false);
                break;
            }
            case LOAD: {
                structureBlock.placeStructure(level);
                break;
            }
            case CORNER: {
                structureBlock.unloadStructure();
                break;
            }
            case DATA: {
                break;
            }
        }
    }
}

