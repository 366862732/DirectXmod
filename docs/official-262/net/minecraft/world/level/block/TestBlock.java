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
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TestBlock
extends BaseEntityBlock
implements GameMasterBlock {
    public static final MapCodec<TestBlock> CODEC = TestBlock.simpleCodec(TestBlock::new);
    public static final EnumProperty<TestBlockMode> MODE = BlockStateProperties.TEST_BLOCK_MODE;

    public TestBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new TestBlockEntity(worldPosition, blockState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        TestBlockMode mode;
        BlockItemStateProperties stateProperties = context.getItemInHand().get(DataComponents.BLOCK_STATE);
        BlockState toPlace = this.defaultBlockState();
        if (stateProperties != null && (mode = stateProperties.get(MODE)) != null) {
            toPlace = (BlockState)toPlace.setValue(MODE, mode);
        }
        return toPlace;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TestBlockEntity)) {
            return InteractionResult.PASS;
        }
        TestBlockEntity testBlockEntity = (TestBlockEntity)blockEntity;
        if (!player.canUseGameMasterBlocks()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            player.openTestBlock(testBlockEntity);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TestBlockEntity testBlock = TestBlock.getServerTestBlockEntity(level, pos);
        if (testBlock == null) {
            return;
        }
        testBlock.reset();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        TestBlockEntity testBlock = TestBlock.getServerTestBlockEntity(level, pos);
        if (testBlock == null) {
            return;
        }
        if (testBlock.getMode() == TestBlockMode.START) {
            return;
        }
        boolean shouldTrigger = level.hasNeighborSignal(pos);
        boolean isPowered = testBlock.isPowered();
        if (shouldTrigger && !isPowered) {
            testBlock.setPowered(true);
            testBlock.trigger();
        } else if (!shouldTrigger && isPowered) {
            testBlock.setPowered(false);
        }
    }

    private static @Nullable TestBlockEntity getServerTestBlockEntity(Level level, BlockPos pos) {
        ServerLevel serverLevel;
        BlockEntity blockEntity;
        if (level instanceof ServerLevel && (blockEntity = (serverLevel = (ServerLevel)level).getBlockEntity(pos)) instanceof TestBlockEntity) {
            TestBlockEntity testBlockEntity = (TestBlockEntity)blockEntity;
            return testBlockEntity;
        }
        return null;
    }

    @Override
    public int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(MODE) != TestBlockMode.START) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TestBlockEntity) {
            TestBlockEntity testBlock = (TestBlockEntity)blockEntity;
            return testBlock.isPowered() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack itemStack = super.getCloneItemStack(level, pos, state, includeData);
        return TestBlock.setModeOnStack(itemStack, state.getValue(MODE));
    }

    public static ItemStack setModeOnStack(ItemStack itemStack, TestBlockMode mode) {
        itemStack.set(DataComponents.BLOCK_STATE, itemStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).with(MODE, mode));
        return itemStack;
    }

    protected MapCodec<TestBlock> codec() {
        return CODEC;
    }
}

