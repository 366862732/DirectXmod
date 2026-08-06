/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Mannequin
extends Avatar {
    protected static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.RESOLVABLE_PROFILE);
    private static final EntityDataAccessor<Boolean> DATA_IMMOVABLE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<Component>> DATA_DESCRIPTION = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.OPTIONAL_COMPONENT);
    private static final byte ALL_LAYERS = (byte)Arrays.stream(PlayerModelPart.values()).mapToInt(PlayerModelPart::getMask).reduce(0, (a, b) -> a | b);
    private static final Set<Pose> VALID_POSES = Set.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING);
    public static final Codec<Pose> POSE_CODEC = Pose.CODEC.validate(pose -> VALID_POSES.contains(pose) ? DataResult.success((Object)pose) : DataResult.error(() -> "Invalid pose: " + pose.getSerializedName()));
    private static final Codec<Byte> LAYERS_CODEC = PlayerModelPart.CODEC.listOf().xmap(list -> (byte)list.stream().mapToInt(PlayerModelPart::getMask).reduce(ALL_LAYERS, (a, b) -> a & ~b), mask -> Arrays.stream(PlayerModelPart.values()).filter(part -> (mask & part.getMask()) == 0).toList());
    public static final ResolvableProfile DEFAULT_PROFILE = ResolvableProfile.Static.EMPTY;
    private static final Component DEFAULT_DESCRIPTION = Component.translatable("entity.minecraft.mannequin.label");
    protected static EntityType.EntityFactory<Mannequin> constructor = Mannequin::new;
    private static final String PROFILE_FIELD = "profile";
    private static final String HIDDEN_LAYERS_FIELD = "hidden_layers";
    private static final String MAIN_HAND_FIELD = "main_hand";
    private static final String POSE_FIELD = "pose";
    private static final String IMMOVABLE_FIELD = "immovable";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String HIDE_DESCRIPTION_FIELD = "hide_description";
    private Component description = DEFAULT_DESCRIPTION;
    private boolean hideDescription = false;

    public Mannequin(EntityType<Mannequin> type, Level level) {
        super((EntityType<? extends LivingEntity>)type, level);
        this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, ALL_LAYERS);
    }

    protected Mannequin(Level level) {
        this(EntityTypes.MANNEQUIN, level);
    }

    public static @Nullable Mannequin create(EntityType<Mannequin> type, Level level) {
        return constructor.create(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PROFILE, DEFAULT_PROFILE);
        entityData.define(DATA_IMMOVABLE, false);
        entityData.define(DATA_DESCRIPTION, Optional.of(DEFAULT_DESCRIPTION));
    }

    @Override
    public ResolvableProfile getProfile() {
        return this.entityData.get(DATA_PROFILE);
    }

    private void setProfile(ResolvableProfile profile) {
        this.entityData.set(DATA_PROFILE, profile);
    }

    private boolean getImmovable() {
        return this.entityData.get(DATA_IMMOVABLE);
    }

    private void setImmovable(boolean immovable) {
        this.entityData.set(DATA_IMMOVABLE, immovable);
    }

    protected @Nullable Component getDescription() {
        return this.entityData.get(DATA_DESCRIPTION).orElse(null);
    }

    private void setDescription(Component description) {
        this.description = description;
        this.updateDescription();
    }

    private void setHideDescription(boolean hideDescription) {
        this.hideDescription = hideDescription;
        this.updateDescription();
    }

    private void updateDescription() {
        this.entityData.set(DATA_DESCRIPTION, this.hideDescription ? Optional.empty() : Optional.of(this.description));
    }

    @Override
    protected boolean isImmobile() {
        return this.getImmovable() || super.isImmobile();
    }

    @Override
    public boolean isEffectiveAi() {
        return !this.getImmovable() && super.isEffectiveAi();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store(PROFILE_FIELD, ResolvableProfile.CODEC, this.getProfile());
        output.store(HIDDEN_LAYERS_FIELD, LAYERS_CODEC, (Byte)this.entityData.get(DATA_PLAYER_MODE_CUSTOMISATION));
        output.store(MAIN_HAND_FIELD, HumanoidArm.CODEC, this.getMainArm());
        output.store(POSE_FIELD, POSE_CODEC, this.getPose());
        output.putBoolean(IMMOVABLE_FIELD, this.getImmovable());
        Component description = this.getDescription();
        if (description != null) {
            if (!description.equals(DEFAULT_DESCRIPTION)) {
                output.store(DESCRIPTION_FIELD, ComponentSerialization.CODEC, description);
            }
        } else {
            output.putBoolean(HIDE_DESCRIPTION_FIELD, true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read(PROFILE_FIELD, ResolvableProfile.CODEC).ifPresent(this::setProfile);
        this.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, input.read(HIDDEN_LAYERS_FIELD, LAYERS_CODEC).orElse(ALL_LAYERS));
        this.setMainArm(input.read(MAIN_HAND_FIELD, HumanoidArm.CODEC).orElse(DEFAULT_MAIN_HAND));
        this.setPose(input.read(POSE_FIELD, POSE_CODEC).orElse(Pose.STANDING));
        this.setImmovable(input.getBooleanOr(IMMOVABLE_FIELD, false));
        this.setHideDescription(input.getBooleanOr(HIDE_DESCRIPTION_FIELD, false));
        this.setDescription(input.read(DESCRIPTION_FIELD, ComponentSerialization.CODEC).orElse(DEFAULT_DESCRIPTION));
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.PROFILE) {
            return Mannequin.castComponentValue(type, this.getProfile());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.PROFILE);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.PROFILE) {
            this.setProfile(Mannequin.castComponentValue(DataComponents.PROFILE, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
    }
}

