/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record MovementPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles speed, MinMaxBounds.Doubles horizontalSpeed, MinMaxBounds.Doubles verticalSpeed, MinMaxBounds.Doubles fallDistance) implements EntitySubPredicate
{
    public static final Codec<MovementPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("x", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::x), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("y", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::y), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("z", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::z), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("speed", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::speed), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("horizontal_speed", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::horizontalSpeed), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("vertical_speed", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::verticalSpeed), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("fall_distance", (Object)MinMaxBounds.Doubles.ANY).forGetter(MovementPredicate::fallDistance)).apply((Applicative)i, MovementPredicate::new));

    public static MovementPredicate speed(MinMaxBounds.Doubles bounds) {
        return new MovementPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, bounds, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
    }

    public static MovementPredicate horizontalSpeed(MinMaxBounds.Doubles bounds) {
        return new MovementPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, bounds, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
    }

    public static MovementPredicate verticalSpeed(MinMaxBounds.Doubles bounds) {
        return new MovementPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, bounds, MinMaxBounds.Doubles.ANY);
    }

    public static MovementPredicate fallDistance(MinMaxBounds.Doubles bounds) {
        return new MovementPredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, bounds);
    }

    public boolean matches(double x, double y, double z, double fallDistance) {
        if (!(this.x.matches(x) && this.y.matches(y) && this.z.matches(z))) {
            return false;
        }
        double speedSqr = Mth.lengthSquared(x, y, z);
        if (!this.speed.matchesSqr(speedSqr)) {
            return false;
        }
        double horizontalSpeedSqr = Mth.lengthSquared(x, z);
        if (!this.horizontalSpeed.matchesSqr(horizontalSpeedSqr)) {
            return false;
        }
        double verticalSpeed = Math.abs(y);
        if (!this.verticalSpeed.matches(verticalSpeed)) {
            return false;
        }
        return this.fallDistance.matches(fallDistance);
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        Vec3 velocity = entity.getKnownMovement().scale(20.0);
        return this.matches(velocity.x, velocity.y, velocity.z, entity.fallDistance);
    }
}

