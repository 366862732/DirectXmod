/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.block;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class CopperChestBlock
extends ChestBlock {
    public static final MapCodec<CopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getState), (App)BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound), (App)BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound), CopperChestBlock.propertiesCodec()).apply((Applicative)i, CopperChestBlock::new));
    private static final Supplier<Map<Block, Block>> COPPER_TO_COPPER_CHEST_MAPPING = Suppliers.memoize(() -> {
        ImmutableMap.Builder result = ImmutableMap.builder();
        WeatheringCopperCollection.zipApply(Blocks.COPPER_BLOCK, Blocks.COPPER_CHEST, (arg_0, arg_1) -> ((ImmutableMap.Builder)result).put(arg_0, arg_1));
        return result.buildOrThrow();
    });
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<? extends CopperChestBlock> codec() {
        return CODEC;
    }

    public CopperChestBlock(WeatheringCopper.WeatherState weatherState, SoundEvent openSound, SoundEvent closeSound, BlockBehaviour.Properties properties) {
        super(() -> BlockEntityTypes.CHEST, openSound, closeSound, properties);
        this.weatherState = weatherState;
    }

    public static SoundEvent getHingeSound(WeatheringCopper.WeatherState state, boolean open) {
        return switch (state) {
            case WeatheringCopper.WeatherState.WEATHERED -> {
                if (open) {
                    yield SoundEvents.COPPER_CHEST_WEATHERED_OPEN;
                }
                yield SoundEvents.COPPER_CHEST_WEATHERED_CLOSE;
            }
            case WeatheringCopper.WeatherState.OXIDIZED -> {
                if (open) {
                    yield SoundEvents.COPPER_CHEST_OXIDIZED_OPEN;
                }
                yield SoundEvents.COPPER_CHEST_OXIDIZED_CLOSE;
            }
            default -> open ? SoundEvents.COPPER_CHEST_OPEN : SoundEvents.COPPER_CHEST_CLOSE;
        };
    }

    @Override
    public boolean chestCanConnectTo(BlockState blockState) {
        return blockState.is(BlockTags.COPPER_CHESTS) && blockState.hasProperty(ChestBlock.TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return CopperChestBlock.getLeastOxidizedChestOfConnectedBlocks(state, context.getLevel(), context.getClickedPos());
    }

    private static BlockState getLeastOxidizedChestOfConnectedBlocks(BlockState state, Level level, BlockPos pos) {
        Block block;
        BlockState connectedState = level.getBlockState(pos.relative(CopperChestBlock.getConnectedDirection(state)));
        if (!state.getValue(ChestBlock.TYPE).equals(ChestType.SINGLE) && (block = state.getBlock()) instanceof CopperChestBlock) {
            CopperChestBlock copperChestBlock = (CopperChestBlock)block;
            block = connectedState.getBlock();
            if (block instanceof CopperChestBlock) {
                CopperChestBlock connectedCopperChestBlock = (CopperChestBlock)block;
                BlockState updatedBlockState = state;
                BlockState connectedPredictedBlockState = connectedState;
                if (copperChestBlock.isWaxed() != connectedCopperChestBlock.isWaxed()) {
                    updatedBlockState = CopperChestBlock.unwaxBlock(copperChestBlock, state).orElse(updatedBlockState);
                    connectedPredictedBlockState = CopperChestBlock.unwaxBlock(connectedCopperChestBlock, connectedState).orElse(connectedPredictedBlockState);
                }
                Block leastOxidizedBlock = copperChestBlock.weatherState.ordinal() <= connectedCopperChestBlock.weatherState.ordinal() ? updatedBlockState.getBlock() : connectedPredictedBlockState.getBlock();
                return leastOxidizedBlock.withPropertiesOf(updatedBlockState);
            }
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        ChestType chestType;
        BlockState blockState = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        if (this.chestCanConnectTo(neighbourState) && !(chestType = blockState.getValue(ChestBlock.TYPE)).equals(ChestType.SINGLE) && CopperChestBlock.getConnectedDirection(blockState) == directionToNeighbour) {
            return neighbourState.getBlock().withPropertiesOf(blockState);
        }
        return blockState;
    }

    private static Optional<BlockState> unwaxBlock(CopperChestBlock copperChestBlock, BlockState state) {
        if (!copperChestBlock.isWaxed()) {
            return Optional.of(state);
        }
        return Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get((Object)state.getBlock())).map(b -> b.withPropertiesOf(state));
    }

    public WeatheringCopper.WeatherState getState() {
        return this.weatherState;
    }

    public static BlockState getFromCopperBlock(Block copperBlock, Direction facing, Level level, BlockPos pos) {
        CopperChestBlock block = (CopperChestBlock)COPPER_TO_COPPER_CHEST_MAPPING.get().getOrDefault(copperBlock, Blocks.COPPER_CHEST.weathering().unaffected());
        ChestType chestType = block.getChestType(level, pos, facing);
        BlockState state = (BlockState)((BlockState)block.defaultBlockState().setValue(FACING, facing)).setValue(TYPE, chestType);
        return CopperChestBlock.getLeastOxidizedChestOfConnectedBlocks(state, level, pos);
    }

    public boolean isWaxed() {
        return true;
    }

    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.is(BlockTags.COPPER_CHESTS);
    }
}

