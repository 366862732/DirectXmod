/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SnifferEggBlock
extends Block {
    public static final MapCodec<SnifferEggBlock> CODEC = SnifferEggBlock.simpleCodec(SnifferEggBlock::new);
    public static final int MAX_HATCH_LEVEL = 2;
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    private static final int REGULAR_HATCH_TIME_TICKS = 24000;
    private static final int BOOSTED_HATCH_TIME_TICKS = 12000;
    private static final int RANDOM_HATCH_OFFSET_TICKS = 300;
    private static final VoxelShape SHAPE = Block.column(14.0, 12.0, 0.0, 16.0);

    public MapCodec<SnifferEggBlock> codec() {
        return CODEC;
    }

    public SnifferEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(HATCH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public int getHatchLevel(BlockState state) {
        return state.getValue(HATCH);
    }

    private boolean isReadyToHatch(BlockState state) {
        return this.getHatchLevel(state) == 2;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (!this.isReadyToHatch(state)) {
            level.playSound(null, position, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7f, 0.9f + random.nextFloat() * 0.2f);
            level.setBlock(position, (BlockState)state.setValue(HATCH, this.getHatchLevel(state) + 1), 2);
            return;
        }
        level.playSound(null, position, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 0.7f, 0.9f + random.nextFloat() * 0.2f);
        level.destroyBlock(position, false);
        Sniffer sniffer = EntityTypes.SNIFFER.create((Level)level, EntitySpawnReason.BREEDING);
        if (sniffer != null) {
            Vec3 spawnAt = Vec3.atCenterOf(position);
            sniffer.setBaby(true);
            sniffer.snapTo(spawnAt.x(), spawnAt.y(), spawnAt.z(), Mth.wrapDegrees(level.getRandom().nextFloat() * 360.0f), 0.0f);
            level.addFreshEntity(sniffer);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        boolean boosted = SnifferEggBlock.hatchBoost(level, pos);
        if (!level.isClientSide() && boosted) {
            level.levelEvent(3009, pos, 0);
        }
        int hatchTime = boosted ? 12000 : 24000;
        int progressionTickDelay = hatchTime / 3;
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(state));
        level.scheduleTick(pos, this, progressionTickDelay + level.getRandom().nextInt(300));
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    public static boolean hatchBoost(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.SNIFFER_EGG_HATCH_BOOST);
    }
}

