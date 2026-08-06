/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CompassAngleState
extends NeedleDirectionHelper {
    public static final MapCodec<CompassAngleState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.optionalFieldOf("wobble", (Object)true).forGetter(NeedleDirectionHelper::wobble), (App)CompassTarget.CODEC.fieldOf("target").forGetter(CompassAngleState::target)).apply((Applicative)i, CompassAngleState::new));
    private final NeedleDirectionHelper.Wobbler wobbler;
    private final NeedleDirectionHelper.Wobbler noTargetWobbler;
    private final CompassTarget compassTarget;
    private final RandomSource random = RandomSource.create();

    public CompassAngleState(boolean wobble, CompassTarget compassTarget) {
        super(wobble);
        this.wobbler = this.newWobbler(0.8f);
        this.noTargetWobbler = this.newWobbler(0.8f);
        this.compassTarget = compassTarget;
    }

    @Override
    protected float calculate(ItemStack itemStack, ClientLevel level, int seed, ItemOwner owner) {
        GlobalPos compassTargetPos = this.compassTarget.get(level, itemStack, owner);
        long gameTime = level.getGameTime();
        if (!CompassAngleState.isValidCompassTargetPos(owner, compassTargetPos)) {
            return this.getRandomlySpinningRotation(seed, gameTime);
        }
        return this.getRotationTowardsCompassTarget(owner, gameTime, compassTargetPos.pos());
    }

    private float getRandomlySpinningRotation(int seed, long gameTime) {
        if (this.noTargetWobbler.shouldUpdate(gameTime)) {
            this.noTargetWobbler.update(gameTime, this.random.nextFloat());
        }
        float targetRotation = this.noTargetWobbler.rotation() + (float)CompassAngleState.hash(seed) / 2.1474836E9f;
        return Mth.positiveModulo(targetRotation, 1.0f);
    }

    private float getRotationTowardsCompassTarget(ItemOwner owner, long gameTime, BlockPos compassTargetPos) {
        float targetRotation;
        Player player;
        float angleToTarget = (float)CompassAngleState.getAngleFromEntityToPos(owner, compassTargetPos);
        float ownerYRotation = CompassAngleState.getWrappedVisualRotationY(owner);
        LivingEntity entity = owner.asLivingEntity();
        if (entity instanceof Player && (player = (Player)entity).isLocalPlayer() && player.level().tickRateManager().runsNormally()) {
            if (this.wobbler.shouldUpdate(gameTime)) {
                this.wobbler.update(gameTime, 0.5f - (ownerYRotation - 0.25f));
            }
            targetRotation = angleToTarget + this.wobbler.rotation();
        } else {
            targetRotation = 0.5f - (ownerYRotation - 0.25f - angleToTarget);
        }
        return Mth.positiveModulo(targetRotation, 1.0f);
    }

    private static boolean isValidCompassTargetPos(ItemOwner owner, @Nullable GlobalPos positionToPointTo) {
        return positionToPointTo != null && positionToPointTo.dimension() == owner.level().dimension() && !(positionToPointTo.pos().distToCenterSqr(owner.position()) < (double)1.0E-5f);
    }

    private static double getAngleFromEntityToPos(ItemOwner owner, BlockPos position) {
        Vec3 target = Vec3.atCenterOf(position);
        Vec3 ownerPosition = owner.position();
        return Math.atan2(target.z() - ownerPosition.z(), target.x() - ownerPosition.x()) / 6.2831854820251465;
    }

    private static float getWrappedVisualRotationY(ItemOwner owner) {
        return Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0f, 1.0f);
    }

    private static int hash(int input) {
        return input * 1327217883;
    }

    protected CompassTarget target() {
        return this.compassTarget;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum CompassTarget implements StringRepresentable
    {
        NONE("none"){

            @Override
            public @Nullable GlobalPos get(ClientLevel level, ItemStack itemStack, @Nullable ItemOwner owner) {
                return null;
            }
        }
        ,
        LODESTONE("lodestone"){

            @Override
            public @Nullable GlobalPos get(ClientLevel level, ItemStack itemStack, @Nullable ItemOwner owner) {
                LodestoneTracker tracker = itemStack.get(DataComponents.LODESTONE_TRACKER);
                return tracker != null ? (GlobalPos)tracker.target().orElse(null) : null;
            }
        }
        ,
        SPAWN("spawn"){

            @Override
            public GlobalPos get(ClientLevel level, ItemStack itemStack, @Nullable ItemOwner owner) {
                return level.getRespawnData().globalPos();
            }
        }
        ,
        RECOVERY("recovery"){

            @Override
            public @Nullable GlobalPos get(ClientLevel level, ItemStack itemStack, @Nullable ItemOwner owner) {
                GlobalPos globalPos;
                LivingEntity entity;
                LivingEntity livingEntity = entity = owner == null ? null : owner.asLivingEntity();
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    globalPos = player.getLastDeathLocation().orElse(null);
                } else {
                    globalPos = null;
                }
                return globalPos;
            }
        };

        public static final Codec<CompassTarget> CODEC;
        private final String name;

        private CompassTarget(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract @Nullable GlobalPos get(ClientLevel var1, ItemStack var2, @Nullable ItemOwner var3);

        static {
            CODEC = StringRepresentable.fromEnum(CompassTarget::values);
        }
    }
}

