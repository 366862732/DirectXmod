/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class Avatar
extends LivingEntity {
    public static final HumanoidArm DEFAULT_MAIN_HAND = HumanoidArm.RIGHT;
    public static final int DEFAULT_MODEL_CUSTOMIZATION = 0;
    public static final float DEFAULT_EYE_HEIGHT = 1.62f;
    public static final Vec3 DEFAULT_VEHICLE_ATTACHMENT = new Vec3(0.0, 0.6, 0.0);
    private static final float CROUCH_BB_HEIGHT = 1.5f;
    private static final float SWIMMING_BB_WIDTH = 0.6f;
    public static final float SWIMMING_BB_HEIGHT = 0.6f;
    protected static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT));
    protected static final Map<Pose, EntityDimensions> POSES = ImmutableMap.builder().put((Object)Pose.STANDING, (Object)STANDING_DIMENSIONS).put((Object)Pose.SLEEPING, (Object)SLEEPING_DIMENSIONS).put((Object)Pose.FALL_FLYING, (Object)EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f)).put((Object)Pose.SWIMMING, (Object)EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f)).put((Object)Pose.SPIN_ATTACK, (Object)EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f)).put((Object)Pose.CROUCHING, (Object)EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT))).put((Object)Pose.DYING, (Object)EntityDimensions.fixed(0.2f, 0.2f).withEyeHeight(1.62f)).build();
    protected static final EntityDataAccessor<HumanoidArm> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Avatar.class, EntityDataSerializers.HUMANOID_ARM);
    protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Avatar.class, EntityDataSerializers.BYTE);

    protected Avatar(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PLAYER_MAIN_HAND, DEFAULT_MAIN_HAND);
        entityData.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
    }

    @Override
    public HumanoidArm getMainArm() {
        return this.entityData.get(DATA_PLAYER_MAIN_HAND);
    }

    public void setMainArm(HumanoidArm mainArm) {
        this.entityData.set(DATA_PLAYER_MAIN_HAND, mainArm);
    }

    public boolean isModelPartShown(PlayerModelPart part) {
        return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & part.getMask()) == part.getMask();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return POSES.getOrDefault(pose, STANDING_DIMENSIONS);
    }

    public abstract ResolvableProfile getProfile();
}

