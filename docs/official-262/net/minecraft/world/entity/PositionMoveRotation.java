/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public record PositionMoveRotation(Vec3 position, Vec3 deltaMovement, float yRot, float xRot) {
    public static final StreamCodec<FriendlyByteBuf, PositionMoveRotation> STREAM_CODEC = StreamCodec.composite(Vec3.STREAM_CODEC, PositionMoveRotation::position, Vec3.STREAM_CODEC, PositionMoveRotation::deltaMovement, ByteBufCodecs.FLOAT, PositionMoveRotation::yRot, ByteBufCodecs.FLOAT, PositionMoveRotation::xRot, PositionMoveRotation::new);

    public static PositionMoveRotation of(Entity entity) {
        if (entity.isInterpolating()) {
            return new PositionMoveRotation(entity.getInterpolation().position(), entity.getKnownMovement(), entity.getInterpolation().yRot(), entity.getInterpolation().xRot());
        }
        return new PositionMoveRotation(entity.position(), entity.getKnownMovement(), entity.getYRot(), entity.getXRot());
    }

    public PositionMoveRotation withRotation(float yRot, float xRot) {
        return new PositionMoveRotation(this.position(), this.deltaMovement(), yRot, xRot);
    }

    public static PositionMoveRotation of(TeleportTransition transition) {
        return new PositionMoveRotation(transition.position(), transition.deltaMovement(), transition.yRot(), transition.xRot());
    }

    public static PositionMoveRotation calculateAbsolute(PositionMoveRotation source, PositionMoveRotation change, Set<Relative> relatives) {
        double offsetX = relatives.contains((Object)Relative.X) ? source.position.x : 0.0;
        double offsetY = relatives.contains((Object)Relative.Y) ? source.position.y : 0.0;
        double offsetZ = relatives.contains((Object)Relative.Z) ? source.position.z : 0.0;
        float offsetYRot = relatives.contains((Object)Relative.Y_ROT) ? source.yRot : 0.0f;
        float offsetXRot = relatives.contains((Object)Relative.X_ROT) ? source.xRot : 0.0f;
        Vec3 absolutePosition = new Vec3(offsetX + change.position.x, offsetY + change.position.y, offsetZ + change.position.z);
        float absoluteYRot = offsetYRot + change.yRot;
        float absoluteXRot = Mth.clamp(offsetXRot + change.xRot, -90.0f, 90.0f);
        Vec3 rotatedCurrentMovement = source.deltaMovement;
        if (relatives.contains((Object)Relative.ROTATE_DELTA)) {
            float diffYRot = source.yRot - absoluteYRot;
            float diffXRot = source.xRot - absoluteXRot;
            rotatedCurrentMovement = rotatedCurrentMovement.xRot((float)Math.toRadians(diffXRot));
            rotatedCurrentMovement = rotatedCurrentMovement.yRot((float)Math.toRadians(diffYRot));
        }
        Vec3 absoluteDeltaMovement = new Vec3(PositionMoveRotation.calculateDelta(rotatedCurrentMovement.x, change.deltaMovement.x, relatives, Relative.DELTA_X), PositionMoveRotation.calculateDelta(rotatedCurrentMovement.y, change.deltaMovement.y, relatives, Relative.DELTA_Y), PositionMoveRotation.calculateDelta(rotatedCurrentMovement.z, change.deltaMovement.z, relatives, Relative.DELTA_Z));
        return new PositionMoveRotation(absolutePosition, absoluteDeltaMovement, absoluteYRot, absoluteXRot);
    }

    private static double calculateDelta(double currentDelta, double deltaChange, Set<Relative> relatives, Relative relative) {
        return relatives.contains((Object)relative) ? currentDelta + deltaChange : deltaChange;
    }
}

