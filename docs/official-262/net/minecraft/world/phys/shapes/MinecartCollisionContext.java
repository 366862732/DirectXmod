/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MinecartCollisionContext
extends EntityCollisionContext {
    private @Nullable BlockPos ingoreBelow;
    private @Nullable BlockPos slopeIgnore;

    protected MinecartCollisionContext(AbstractMinecart entity, boolean alwaysStandOnFluid) {
        super(entity, alwaysStandOnFluid, false);
        this.setupContext(entity);
    }

    private void setupContext(AbstractMinecart entity) {
        BlockPos currentRailPos = entity.getCurrentBlockPosOrRailBelow();
        BlockState currentState = entity.level().getBlockState(currentRailPos);
        boolean onRails = BaseRailBlock.isRail(currentState);
        if (onRails) {
            this.ingoreBelow = currentRailPos.below();
            RailShape shape = currentState.getValue(((BaseRailBlock)currentState.getBlock()).getShapeProperty());
            if (shape.isSlope()) {
                this.slopeIgnore = switch (shape) {
                    case RailShape.ASCENDING_EAST -> currentRailPos.east();
                    case RailShape.ASCENDING_WEST -> currentRailPos.west();
                    case RailShape.ASCENDING_NORTH -> currentRailPos.north();
                    case RailShape.ASCENDING_SOUTH -> currentRailPos.south();
                    default -> null;
                };
            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, CollisionGetter collisionGetter, BlockPos pos) {
        if (pos.equals(this.ingoreBelow) || pos.equals(this.slopeIgnore)) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, collisionGetter, pos);
    }
}

