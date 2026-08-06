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
import java.util.Optional;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityFlagsPredicate(Optional<Boolean> isOnGround, Optional<Boolean> isOnFire, Optional<Boolean> isCrouching, Optional<Boolean> isSprinting, Optional<Boolean> isSwimming, Optional<Boolean> isFlying, Optional<Boolean> isBaby, Optional<Boolean> isInWater, Optional<Boolean> isFallFlying) implements EntitySubPredicate
{
    public static final Codec<EntityFlagsPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("is_on_ground").forGetter(EntityFlagsPredicate::isOnGround), (App)Codec.BOOL.optionalFieldOf("is_on_fire").forGetter(EntityFlagsPredicate::isOnFire), (App)Codec.BOOL.optionalFieldOf("is_sneaking").forGetter(EntityFlagsPredicate::isCrouching), (App)Codec.BOOL.optionalFieldOf("is_sprinting").forGetter(EntityFlagsPredicate::isSprinting), (App)Codec.BOOL.optionalFieldOf("is_swimming").forGetter(EntityFlagsPredicate::isSwimming), (App)Codec.BOOL.optionalFieldOf("is_flying").forGetter(EntityFlagsPredicate::isFlying), (App)Codec.BOOL.optionalFieldOf("is_baby").forGetter(EntityFlagsPredicate::isBaby), (App)Codec.BOOL.optionalFieldOf("is_in_water").forGetter(EntityFlagsPredicate::isInWater), (App)Codec.BOOL.optionalFieldOf("is_fall_flying").forGetter(EntityFlagsPredicate::isFallFlying)).apply((Applicative)i, EntityFlagsPredicate::new));

    /*
     * Unable to fully structure code
     */
    public boolean matches(Entity entity) {
        block11: {
            if (this.isOnGround.isPresent() && entity.onGround() != this.isOnGround.get().booleanValue()) {
                return false;
            }
            if (this.isOnFire.isPresent() && entity.isOnFire() != this.isOnFire.get().booleanValue()) {
                return false;
            }
            if (this.isCrouching.isPresent() && entity.isCrouching() != this.isCrouching.get().booleanValue()) {
                return false;
            }
            if (this.isSprinting.isPresent() && entity.isSprinting() != this.isSprinting.get().booleanValue()) {
                return false;
            }
            if (this.isSwimming.isPresent() && entity.isSwimming() != this.isSwimming.get().booleanValue()) {
                return false;
            }
            if (!this.isFlying.isPresent()) break block11;
            if (!(entity instanceof LivingEntity)) ** GOTO lbl-1000
            living = (LivingEntity)entity;
            if (living.isFallFlying()) ** GOTO lbl-1000
            if (living instanceof Player) {
                player = (Player)living;
                ** if (!player.getAbilities().flying) goto lbl-1000
            }
            ** GOTO lbl-1000
lbl-1000:
            // 2 sources

            {
                v0 = true;
                ** GOTO lbl22
            }
lbl-1000:
            // 3 sources

            {
                v0 = entityIsFlying = false;
            }
lbl22:
            // 2 sources

            if (entityIsFlying != this.isFlying.get()) {
                return false;
            }
        }
        if (this.isInWater.isPresent() && entity.isInWater() != this.isInWater.get().booleanValue()) {
            return false;
        }
        if (this.isFallFlying.isPresent() && entity instanceof LivingEntity && (living = (LivingEntity)entity).isFallFlying() != this.isFallFlying.get().booleanValue()) {
            return false;
        }
        return this.isBaby.isPresent() == false || entity instanceof LivingEntity == false || (living = (LivingEntity)entity).isBaby() == this.isBaby.get().booleanValue();
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return this.matches(entity);
    }

    public static class Builder {
        private Optional<Boolean> isOnGround = Optional.empty();
        private Optional<Boolean> isOnFire = Optional.empty();
        private Optional<Boolean> isCrouching = Optional.empty();
        private Optional<Boolean> isSprinting = Optional.empty();
        private Optional<Boolean> isSwimming = Optional.empty();
        private Optional<Boolean> isFlying = Optional.empty();
        private Optional<Boolean> isBaby = Optional.empty();
        private Optional<Boolean> isInWater = Optional.empty();
        private Optional<Boolean> isFallFlying = Optional.empty();

        public static Builder flags() {
            return new Builder();
        }

        public Builder setOnGround(Boolean onGround) {
            this.isOnGround = Optional.of(onGround);
            return this;
        }

        public Builder setOnFire(Boolean onFire) {
            this.isOnFire = Optional.of(onFire);
            return this;
        }

        public Builder setCrouching(Boolean crouching) {
            this.isCrouching = Optional.of(crouching);
            return this;
        }

        public Builder setSprinting(Boolean sprinting) {
            this.isSprinting = Optional.of(sprinting);
            return this;
        }

        public Builder setSwimming(Boolean swimming) {
            this.isSwimming = Optional.of(swimming);
            return this;
        }

        public Builder setIsFlying(Boolean flying) {
            this.isFlying = Optional.of(flying);
            return this;
        }

        public Builder setIsBaby(Boolean baby) {
            this.isBaby = Optional.of(baby);
            return this;
        }

        public Builder setIsInWater(Boolean inWater) {
            this.isInWater = Optional.of(inWater);
            return this;
        }

        public Builder setIsFallFlying(Boolean fallFlying) {
            this.isFallFlying = Optional.of(fallFlying);
            return this;
        }

        public EntityFlagsPredicate build() {
            return new EntityFlagsPredicate(this.isOnGround, this.isOnFire, this.isCrouching, this.isSprinting, this.isSwimming, this.isFlying, this.isBaby, this.isInWater, this.isFallFlying);
        }
    }
}

