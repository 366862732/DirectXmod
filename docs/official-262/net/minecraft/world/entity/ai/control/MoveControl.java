/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.Control;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoveControl<T extends Mob>
implements Control {
    public static final float MIN_SPEED = 5.0E-4f;
    public static final float MIN_SPEED_SQR = 2.5000003E-7f;
    protected static final int MAX_TURN = 90;
    protected final T mob;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    protected double speedModifier;
    protected float strafeForwards;
    protected float strafeRight;
    protected Operation operation = Operation.WAIT;

    public MoveControl(T mob) {
        this.mob = mob;
    }

    public boolean hasWanted() {
        return this.operation == Operation.MOVE_TO;
    }

    public double getSpeedModifier() {
        return this.speedModifier;
    }

    public void setWantedPosition(double x, double y, double z, double speedModifier) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.speedModifier = speedModifier;
        if (this.operation != Operation.JUMPING) {
            this.operation = Operation.MOVE_TO;
        }
    }

    public void strafe(float forwards, float right) {
        this.operation = Operation.STRAFE;
        this.strafeForwards = forwards;
        this.strafeRight = right;
        this.speedModifier = 0.25;
    }

    public void tick() {
        if (this.operation == Operation.STRAFE) {
            float dz;
            float speed = (float)((LivingEntity)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED);
            float speedModified = (float)this.speedModifier * speed;
            float xa = this.strafeForwards;
            float za = this.strafeRight;
            float dist = Mth.sqrt(xa * xa + za * za);
            if (dist < 1.0f) {
                dist = 1.0f;
            }
            dist = speedModified / dist;
            float sin = Mth.sin(((Entity)this.mob).getYRot() * ((float)Math.PI / 180));
            float cos = Mth.cos(((Entity)this.mob).getYRot() * ((float)Math.PI / 180));
            float dx = (xa *= dist) * cos - (za *= dist) * sin;
            if (!this.isWalkable(dx, dz = za * cos + xa * sin)) {
                this.strafeForwards = 1.0f;
                this.strafeRight = 0.0f;
            }
            ((Mob)this.mob).setSpeed(speedModified);
            ((Mob)this.mob).setZza(this.strafeForwards);
            ((Mob)this.mob).setXxa(this.strafeRight);
            this.operation = Operation.WAIT;
        } else if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT;
            double xd = this.wantedX - ((Entity)this.mob).getX();
            double zd = this.wantedZ - ((Entity)this.mob).getZ();
            double yd = this.wantedY - ((Entity)this.mob).getY();
            double dd = xd * xd + yd * yd + zd * zd;
            if (dd < 2.500000277905201E-7) {
                ((Mob)this.mob).setZza(0.0f);
                return;
            }
            float yRotD = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f;
            ((Entity)this.mob).setYRot(this.rotlerp(((Entity)this.mob).getYRot(), yRotD, 90.0f));
            ((Mob)this.mob).setSpeed((float)(this.speedModifier * ((LivingEntity)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED)));
            BlockPos pos = ((Entity)this.mob).blockPosition();
            BlockState blockState = ((Entity)this.mob).level().getBlockState(pos);
            VoxelShape shape = blockState.getCollisionShape(((Entity)this.mob).level(), pos);
            if (yd > (double)((LivingEntity)this.mob).maxUpStep() && xd * xd + zd * zd < (double)Math.max(1.0f, ((Entity)this.mob).getBbWidth()) || !shape.isEmpty() && ((Entity)this.mob).getY() < shape.max(Direction.Axis.Y) + (double)pos.getY() && !blockState.is(BlockTags.DOORS) && !blockState.is(BlockTags.FENCES)) {
                ((Mob)this.mob).getJumpControl().jump();
                this.operation = Operation.JUMPING;
            }
        } else if (this.operation == Operation.JUMPING) {
            ((Mob)this.mob).setSpeed((float)(this.speedModifier * ((LivingEntity)this.mob).getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (((Entity)this.mob).onGround() || ((Entity)this.mob).isInLiquid() && ((LivingEntity)this.mob).isAffectedByFluids()) {
                this.operation = Operation.WAIT;
            }
        } else {
            ((Mob)this.mob).setZza(0.0f);
        }
    }

    private boolean isWalkable(float dx, float dz) {
        NodeEvaluator nodeEvaluator;
        PathNavigation pathNavigation = ((Mob)this.mob).getNavigation();
        return pathNavigation == null || (nodeEvaluator = pathNavigation.getNodeEvaluator()) == null || nodeEvaluator.getPathType((Mob)this.mob, BlockPos.containing(((Entity)this.mob).getX() + (double)dx, ((Entity)this.mob).getBlockY(), ((Entity)this.mob).getZ() + (double)dz)) == PathType.WALKABLE;
    }

    protected float rotlerp(float a, float b, float max) {
        float result;
        float diff = Mth.wrapDegrees(b - a);
        if (diff > max) {
            diff = max;
        }
        if (diff < -max) {
            diff = -max;
        }
        if ((result = a + diff) < 0.0f) {
            result += 360.0f;
        } else if (result > 360.0f) {
            result -= 360.0f;
        }
        return result;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    public void setWait() {
        this.operation = Operation.WAIT;
    }

    protected static enum Operation {
        WAIT,
        MOVE_TO,
        STRAFE,
        JUMPING;

    }
}

