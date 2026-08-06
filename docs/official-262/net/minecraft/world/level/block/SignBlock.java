/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class SignBlock
extends BaseEntityBlock
implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 16.0);
    private final WoodType type;

    protected SignBlock(WoodType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
    }

    protected abstract MapCodec<? extends SignBlock> codec();

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new SignBlockEntity(worldPosition, blockState);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        boolean hasApplicatorToUse;
        SignApplicator applicator;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity)) {
            return InteractionResult.PASS;
        }
        SignBlockEntity sign = (SignBlockEntity)blockEntity;
        Item item = itemStack.getItem();
        SignApplicator signApplicator = item instanceof SignApplicator ? (applicator = (SignApplicator)((Object)item)) : null;
        boolean bl = hasApplicatorToUse = signApplicator != null && player.mayBuild();
        if (!(level instanceof ServerLevel)) {
            return hasApplicatorToUse || sign.isWaxed() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (!hasApplicatorToUse || sign.isWaxed() || this.otherPlayerIsEditingSign(player, sign)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        boolean isFrontText = sign.isFacingFrontText(player);
        if (signApplicator.canApplyToSign(sign.getText(isFrontText), itemStack, player) && signApplicator.tryApplyToSign(serverLevel, sign, isFrontText, itemStack, player)) {
            sign.executeClickCommandsIfPresent(serverLevel, player, pos, isFrontText);
            player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, sign.getBlockPos(), GameEvent.Context.of(player, sign.getBlockState()));
            itemStack.consume(1, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity)) {
            return InteractionResult.PASS;
        }
        SignBlockEntity sign = (SignBlockEntity)blockEntity;
        if (!(level instanceof ServerLevel)) {
            Util.pauseInIde(new IllegalStateException("Expected to only call this on server"));
            return InteractionResult.CONSUME;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        boolean isFrontText = sign.isFacingFrontText(player);
        boolean executedClickCommand = sign.executeClickCommandsIfPresent(serverLevel, player, pos, isFrontText);
        if (sign.isWaxed()) {
            serverLevel.playSound(null, sign.getBlockPos(), sign.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (executedClickCommand) {
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!this.otherPlayerIsEditingSign(player, sign) && player.mayBuild() && this.hasEditableText(player, sign, isFrontText)) {
            this.openTextEdit(player, sign, isFrontText);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    private boolean hasEditableText(Player player, SignBlockEntity sign, boolean isFrontText) {
        SignText text = sign.getText(isFrontText);
        return Arrays.stream(text.getMessages(player.isTextFilteringEnabled())).allMatch(message -> message.equals(CommonComponents.EMPTY) || message.getContents() instanceof PlainTextContents);
    }

    public abstract float getYRotationDegrees(BlockState var1);

    public Vec3 getSignHitboxCenterPosition(BlockState state) {
        return new Vec3(0.5, 0.5, 0.5);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    public WoodType type() {
        return this.type;
    }

    public static WoodType getWoodType(Block block) {
        WoodType woodType;
        if (block instanceof SignBlock) {
            SignBlock signBlock = (SignBlock)block;
            woodType = signBlock.type();
        } else {
            woodType = WoodType.OAK;
        }
        return woodType;
    }

    public void openTextEdit(Player player, SignBlockEntity sign, boolean isFrontText) {
        sign.setAllowedPlayerEditor(player.getUUID());
        player.openTextEdit(sign, isFrontText);
    }

    private boolean otherPlayerIsEditingSign(Player player, SignBlockEntity sign) {
        UUID playerWhoMayEdit = sign.getPlayerWhoMayEdit();
        return playerWhoMayEdit != null && !playerWhoMayEdit.equals(player.getUUID());
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return SignBlock.createTickerHelper(type, BlockEntityTypes.SIGN, SignBlockEntity::tick);
    }
}

