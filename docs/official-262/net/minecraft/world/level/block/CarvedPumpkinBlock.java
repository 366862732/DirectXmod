/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class CarvedPumpkinBlock
extends HorizontalDirectionalBlock {
    public static final MapCodec<CarvedPumpkinBlock> CODEC = CarvedPumpkinBlock.simpleCodec(CarvedPumpkinBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private @Nullable BlockPattern snowGolemBase;
    private @Nullable BlockPattern snowGolemFull;
    private @Nullable BlockPattern ironGolemBase;
    private @Nullable BlockPattern ironGolemFull;
    private @Nullable BlockPattern copperGolemBase;
    private @Nullable BlockPattern copperGolemFull;
    private static final Predicate<BlockState> PUMPKINS_PREDICATE = input -> input.is(Blocks.CARVED_PUMPKIN) || input.is(Blocks.JACK_O_LANTERN);

    public MapCodec<? extends CarvedPumpkinBlock> codec() {
        return CODEC;
    }

    protected CarvedPumpkinBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        this.trySpawnGolem(level, pos);
    }

    public boolean canSpawnGolem(LevelReader level, BlockPos topPos) {
        return this.getOrCreateSnowGolemBase().find(level, topPos) != null || this.getOrCreateIronGolemBase().find(level, topPos) != null || this.getOrCreateCopperGolemBase().find(level, topPos) != null;
    }

    private void trySpawnGolem(Level level, BlockPos topPos) {
        CopperGolem copperGolem;
        IronGolem ironGolem;
        SnowGolem snowGolem;
        BlockPattern.BlockPatternMatch snowGolemMatch = this.getOrCreateSnowGolemFull().find(level, topPos);
        if (snowGolemMatch != null && (snowGolem = EntityTypes.SNOW_GOLEM.create(level, EntitySpawnReason.TRIGGERED)) != null) {
            CarvedPumpkinBlock.spawnGolemInWorld(level, snowGolemMatch, snowGolem, snowGolemMatch.getBlock(0, 2, 0).getPos());
            return;
        }
        BlockPattern.BlockPatternMatch ironGolemMatch = this.getOrCreateIronGolemFull().find(level, topPos);
        if (ironGolemMatch != null && (ironGolem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.TRIGGERED)) != null) {
            ironGolem.setPlayerCreated(true);
            CarvedPumpkinBlock.spawnGolemInWorld(level, ironGolemMatch, ironGolem, ironGolemMatch.getBlock(1, 2, 0).getPos());
            return;
        }
        BlockPattern.BlockPatternMatch copperGolemMatch = this.getOrCreateCopperGolemFull().find(level, topPos);
        if (copperGolemMatch != null && (copperGolem = EntityTypes.COPPER_GOLEM.create(level, EntitySpawnReason.TRIGGERED)) != null) {
            CarvedPumpkinBlock.spawnGolemInWorld(level, copperGolemMatch, copperGolem, copperGolemMatch.getBlock(0, 0, 0).getPos());
            this.replaceCopperBlockWithChest(level, copperGolemMatch);
            copperGolem.spawn(this.getWeatherStateFromPattern(copperGolemMatch));
        }
    }

    private WeatheringCopper.WeatherState getWeatherStateFromPattern(BlockPattern.BlockPatternMatch copperGolemMatch) {
        BlockState state = copperGolemMatch.getBlock(0, 1, 0).getState();
        Block block = state.getBlock();
        if (block instanceof WeatheringCopper) {
            WeatheringCopper copper = (WeatheringCopper)((Object)block);
            return (WeatheringCopper.WeatherState)copper.getAge();
        }
        return (WeatheringCopper.WeatherState)Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get((Object)state.getBlock())).filter(weatheringCopper -> weatheringCopper instanceof WeatheringCopper).map(weatheringCopper -> (WeatheringCopper)((Object)weatheringCopper)).orElse((WeatheringCopper)((Object)Blocks.COPPER_BLOCK.weathering().unaffected())).getAge();
    }

    private static void spawnGolemInWorld(Level level, BlockPattern.BlockPatternMatch match, Entity golem, BlockPos spawnPos) {
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        golem.snapTo((double)spawnPos.getX() + 0.5, (double)spawnPos.getY() + 0.05, (double)spawnPos.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(golem);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }

    public static void clearPatternBlocks(Level level, BlockPattern.BlockPatternMatch match) {
        for (int x = 0; x < match.getWidth(); ++x) {
            for (int y = 0; y < match.getHeight(); ++y) {
                BlockInWorld block = match.getBlock(x, y, 0);
                level.setBlock(block.getPos(), Blocks.AIR.defaultBlockState(), 2);
                level.levelEvent(2001, block.getPos(), Block.getId(block.getState()));
            }
        }
    }

    public static void updatePatternBlocks(Level level, BlockPattern.BlockPatternMatch match) {
        for (int x = 0; x < match.getWidth(); ++x) {
            for (int y = 0; y < match.getHeight(); ++y) {
                BlockInWorld block = match.getBlock(x, y, 0);
                level.updateNeighborsAt(block.getPos(), Blocks.AIR);
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private BlockPattern getOrCreateSnowGolemBase() {
        if (this.snowGolemBase == null) {
            this.snowGolemBase = BlockPatternBuilder.start().aisle(" ", "#", "#").where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }
        return this.snowGolemBase;
    }

    private BlockPattern getOrCreateSnowGolemFull() {
        if (this.snowGolemFull == null) {
            this.snowGolemFull = BlockPatternBuilder.start().aisle("^", "#", "#").where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK))).build();
        }
        return this.snowGolemFull;
    }

    private BlockPattern getOrCreateIronGolemBase() {
        if (this.ironGolemBase == null) {
            this.ironGolemBase = BlockPatternBuilder.start().aisle("~ ~", "###", "~#~").where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir)).build();
        }
        return this.ironGolemBase;
    }

    private BlockPattern getOrCreateIronGolemFull() {
        if (this.ironGolemFull == null) {
            this.ironGolemFull = BlockPatternBuilder.start().aisle("~^~", "###", "~#~").where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK))).where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir)).build();
        }
        return this.ironGolemFull;
    }

    private BlockPattern getOrCreateCopperGolemBase() {
        if (this.copperGolemBase == null) {
            this.copperGolemBase = BlockPatternBuilder.start().aisle(" ", "#").where('#', BlockInWorld.hasState(block -> block.is(BlockTags.COPPER))).build();
        }
        return this.copperGolemBase;
    }

    private BlockPattern getOrCreateCopperGolemFull() {
        if (this.copperGolemFull == null) {
            this.copperGolemFull = BlockPatternBuilder.start().aisle("^", "#").where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE)).where('#', BlockInWorld.hasState(block -> block.is(BlockTags.COPPER))).build();
        }
        return this.copperGolemFull;
    }

    public void replaceCopperBlockWithChest(Level level, BlockPattern.BlockPatternMatch match) {
        BlockInWorld copperBlock = match.getBlock(0, 1, 0);
        BlockInWorld pumpkinBlock = match.getBlock(0, 0, 0);
        Direction facing = pumpkinBlock.getState().getValue(FACING);
        BlockState blockState = CopperChestBlock.getFromCopperBlock(copperBlock.getState().getBlock(), facing, level, copperBlock.getPos());
        level.setBlock(copperBlock.getPos(), blockState, 2);
    }
}

