/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class EndPortalBlock
extends BaseEntityBlock
implements Portal {
    public static final MapCodec<EndPortalBlock> CODEC = EndPortalBlock.simpleCodec(EndPortalBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 6.0, 12.0);

    public MapCodec<EndPortalBlock> codec() {
        return CODEC;
    }

    protected EndPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new TheEndPortalBlockEntity(worldPosition, blockState);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getShape(level, pos);
    }

    /*
     * Enabled aggressive block sorting
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!entity.canUsePortal(false)) return;
        if (!level.isClientSide() && level.dimension() == Level.END && entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            if (!player.seenCredits) {
                player.showEndCredits();
                return;
            }
        }
        entity.setAsInsidePortal(this, pos);
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        Set<Relative> relatives;
        float xRot;
        float yRot;
        LevelData.RespawnData respawnData = currentLevel.getRespawnData();
        ResourceKey<Level> currentDimension = currentLevel.dimension();
        boolean fromEnd = currentDimension == Level.END;
        ResourceKey<Level> newDimension = fromEnd ? respawnData.dimension() : Level.END;
        BlockPos spawnBlockPos = fromEnd ? respawnData.pos() : ServerLevel.END_SPAWN_POINT;
        ServerLevel newLevel = currentLevel.getServer().getLevel(newDimension);
        if (newLevel == null) {
            return null;
        }
        Vec3 spawnPos = Vec3.atBottomCenterOf(spawnBlockPos);
        if (!fromEnd) {
            EndPlatformFeature.createEndPlatform(newLevel, BlockPos.containing(spawnPos).below(), true);
            yRot = Direction.WEST.toYRot();
            xRot = 0.0f;
            relatives = Relative.union(Relative.DELTA, Set.of(Relative.X_ROT));
            if (entity instanceof ServerPlayer) {
                spawnPos = spawnPos.subtract(0.0, 1.0, 0.0);
            }
        } else {
            yRot = respawnData.yaw();
            xRot = respawnData.pitch();
            relatives = Relative.union(Relative.DELTA, Relative.ROTATION);
            if (entity instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entity;
                return serverPlayer.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            }
            spawnPos = Vec3.atBottomCenterOf(entity.adjustSpawnLocation(newLevel, spawnBlockPos));
        }
        return new TeleportTransition(newLevel, spawnPos, Vec3.ZERO, yRot, xRot, relatives, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = (double)pos.getX() + random.nextDouble();
        double y = (double)pos.getY() + 0.8;
        double z = (double)pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}

