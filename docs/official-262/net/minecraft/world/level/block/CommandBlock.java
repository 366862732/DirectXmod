/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CommandBlock
extends BaseEntityBlock
implements GameMasterBlock {
    public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.fieldOf("automatic").forGetter(b -> b.automatic), CommandBlock.propertiesCodec()).apply((Applicative)i, CommandBlock::new));
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
    private final boolean automatic;

    public MapCodec<CommandBlock> codec() {
        return CODEC;
    }

    public CommandBlock(boolean automatic, BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(CONDITIONAL, false));
        this.automatic = automatic;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        CommandBlockEntity blockEntity = new CommandBlockEntity(worldPosition, blockState);
        blockEntity.setAutomatic(this.automatic);
        return blockEntity;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlock = (CommandBlockEntity)blockEntity;
            this.setPoweredAndUpdate(level, pos, commandBlock, level.hasNeighborSignal(pos));
        }
    }

    private void setPoweredAndUpdate(Level level, BlockPos pos, CommandBlockEntity commandBlock, boolean isPowered) {
        boolean wasPowered = commandBlock.isPowered();
        if (isPowered == wasPowered) {
            return;
        }
        commandBlock.setPowered(isPowered);
        if (isPowered) {
            if (commandBlock.isAutomatic() || commandBlock.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
                return;
            }
            commandBlock.markConditionMet();
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlock = (CommandBlockEntity)blockEntity;
            BaseCommandBlock baseCommandBlock = commandBlock.getCommandBlock();
            boolean commandSet = !StringUtil.isNullOrEmpty(baseCommandBlock.getCommand());
            CommandBlockEntity.Mode mode = commandBlock.getMode();
            boolean wasConditionMet = commandBlock.wasConditionMet();
            if (mode == CommandBlockEntity.Mode.AUTO) {
                commandBlock.markConditionMet();
                if (wasConditionMet) {
                    this.execute(state, level, pos, baseCommandBlock, commandSet);
                } else if (commandBlock.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
                if (commandBlock.isPowered() || commandBlock.isAutomatic()) {
                    level.scheduleTick(pos, this, 1);
                }
            } else if (mode == CommandBlockEntity.Mode.REDSTONE) {
                if (wasConditionMet) {
                    this.execute(state, level, pos, baseCommandBlock, commandSet);
                } else if (commandBlock.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    private void execute(BlockState state, ServerLevel level, BlockPos pos, BaseCommandBlock baseCommandBlock, boolean commandSet) {
        if (commandSet) {
            baseCommandBlock.performCommand(level);
        } else {
            baseCommandBlock.setSuccessCount(0);
        }
        CommandBlock.executeChain(level, pos, state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
            if (player.canUseGameMasterBlocks()) {
                player.openCommandBlock(commandBlockEntity);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
            return commandBlockEntity.getCommandBlock().getSuccessCount();
        }
        return 0;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity)) {
            return;
        }
        CommandBlockEntity commandBlockEntity = (CommandBlockEntity)blockEntity;
        BaseCommandBlock commandBlock = commandBlockEntity.getCommandBlock();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (!itemStack.has(DataComponents.BLOCK_ENTITY_DATA)) {
                commandBlock.setTrackOutput(serverLevel.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK));
                commandBlockEntity.setAutomatic(this.automatic);
            }
            boolean hasNeighborSignal = level.hasNeighborSignal(pos);
            this.setPoweredAndUpdate(level, pos, commandBlockEntity, hasNeighborSignal);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONDITIONAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    private static void executeChain(ServerLevel level, BlockPos blockPos, Direction direction) {
        BlockPos.MutableBlockPos pos = blockPos.mutable();
        GameRules gameRules = level.getGameRules();
        int maxIterations = gameRules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH);
        while (maxIterations-- > 0) {
            CommandBlockEntity commandBlock;
            BlockEntity blockEntity;
            pos.move(direction);
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (!state.is(Blocks.CHAIN_COMMAND_BLOCK) || !((blockEntity = level.getBlockEntity(pos)) instanceof CommandBlockEntity) || (commandBlock = (CommandBlockEntity)blockEntity).getMode() != CommandBlockEntity.Mode.SEQUENCE) break;
            if (commandBlock.isPowered() || commandBlock.isAutomatic()) {
                BaseCommandBlock baseCommandBlock = commandBlock.getCommandBlock();
                if (commandBlock.markConditionMet()) {
                    if (!baseCommandBlock.performCommand(level)) break;
                    level.updateNeighbourForOutputSignal(pos, block);
                } else if (commandBlock.isConditional()) {
                    baseCommandBlock.setSuccessCount(0);
                }
            }
            direction = state.getValue(FACING);
        }
        if (maxIterations <= 0) {
            int limit = Math.max(gameRules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH), 0);
            LOGGER.warn("Command Block chain tried to execute more than {} steps!", (Object)limit);
        }
    }
}

