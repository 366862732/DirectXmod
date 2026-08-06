/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Display
extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int NO_BRIGHTNESS_OVERRIDE = -1;
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POS_ROT_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3fc> DATA_TRANSLATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3fc> DATA_SCALE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionfc> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Quaternionfc> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Byte> DATA_BILLBOARD_RENDER_CONSTRAINTS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_BRIGHTNESS_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_RADIUS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_STRENGTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final IntSet RENDER_STATE_IDS = IntSet.of((int[])new int[]{DATA_TRANSLATION_ID.id(), DATA_SCALE_ID.id(), DATA_LEFT_ROTATION_ID.id(), DATA_RIGHT_ROTATION_ID.id(), DATA_BILLBOARD_RENDER_CONSTRAINTS_ID.id(), DATA_BRIGHTNESS_OVERRIDE_ID.id(), DATA_SHADOW_RADIUS_ID.id(), DATA_SHADOW_STRENGTH_ID.id()});
    private static final int INITIAL_TRANSFORMATION_INTERPOLATION_DURATION = 0;
    private static final int INITIAL_TRANSFORMATION_START_INTERPOLATION = 0;
    private static final int INITIAL_POS_ROT_INTERPOLATION_DURATION = 0;
    private static final float INITIAL_SHADOW_RADIUS = 0.0f;
    private static final float INITIAL_SHADOW_STRENGTH = 1.0f;
    private static final float INITIAL_VIEW_RANGE = 1.0f;
    private static final float INITIAL_WIDTH = 0.0f;
    private static final float INITIAL_HEIGHT = 0.0f;
    private static final int NO_GLOW_COLOR_OVERRIDE = -1;
    public static final String TAG_POS_ROT_INTERPOLATION_DURATION = "teleport_duration";
    public static final String TAG_TRANSFORMATION_INTERPOLATION_DURATION = "interpolation_duration";
    public static final String TAG_TRANSFORMATION_START_INTERPOLATION = "start_interpolation";
    public static final String TAG_TRANSFORMATION = "transformation";
    public static final String TAG_BILLBOARD = "billboard";
    public static final String TAG_BRIGHTNESS = "brightness";
    public static final String TAG_VIEW_RANGE = "view_range";
    public static final String TAG_SHADOW_RADIUS = "shadow_radius";
    public static final String TAG_SHADOW_STRENGTH = "shadow_strength";
    public static final String TAG_WIDTH = "width";
    public static final String TAG_HEIGHT = "height";
    public static final String TAG_GLOW_COLOR_OVERRIDE = "glow_color_override";
    private long interpolationStartClientTick = Integer.MIN_VALUE;
    private int interpolationDuration;
    private float lastProgress;
    private AABB cullingBoundingBox;
    private boolean noCulling = true;
    protected boolean updateRenderState;
    private boolean updateStartTick;
    private boolean updateInterpolationDuration;
    private @Nullable RenderState renderState;
    private final InterpolationHandler interpolation = new InterpolationHandler((Entity)this, 0);

    public Display(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.cullingBoundingBox = this.getBoundingBox();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_HEIGHT_ID.equals(accessor) || DATA_WIDTH_ID.equals(accessor)) {
            this.updateCulling();
        }
        if (DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID.equals(accessor)) {
            this.updateStartTick = true;
        }
        if (DATA_POS_ROT_INTERPOLATION_DURATION_ID.equals(accessor)) {
            this.interpolation.setInterpolationLength(this.getPosRotInterpolationDuration());
        }
        if (DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID.equals(accessor)) {
            this.updateInterpolationDuration = true;
        }
        if (RENDER_STATE_IDS.contains(accessor.id())) {
            this.updateRenderState = true;
        }
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    private static Transformation createTransformation(SynchedEntityData entityData) {
        Vector3fc translation = entityData.get(DATA_TRANSLATION_ID);
        Quaternionfc leftRotation = entityData.get(DATA_LEFT_ROTATION_ID);
        Vector3fc scale = entityData.get(DATA_SCALE_ID);
        Quaternionfc rightRotation = entityData.get(DATA_RIGHT_ROTATION_ID);
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }

    @Override
    public void tick() {
        Entity vehicle = this.getVehicle();
        if (vehicle != null && vehicle.isRemoved()) {
            this.stopRiding();
        }
        if (this.level().isClientSide()) {
            if (this.updateStartTick) {
                this.updateStartTick = false;
                int interpolationStartDelta = this.getTransformationInterpolationDelay();
                this.interpolationStartClientTick = this.tickCount + interpolationStartDelta;
            }
            if (this.updateInterpolationDuration) {
                this.updateInterpolationDuration = false;
                this.interpolationDuration = this.getTransformationInterpolationDuration();
            }
            if (this.updateRenderState) {
                this.updateRenderState = false;
                boolean shouldInterpolate = this.interpolationDuration != 0;
                this.renderState = shouldInterpolate && this.renderState != null ? this.createInterpolatedRenderState(this.renderState, this.lastProgress) : this.createFreshRenderState();
                this.updateRenderSubState(shouldInterpolate, this.lastProgress);
            }
            this.interpolation.interpolate();
        }
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    protected abstract void updateRenderSubState(boolean var1, float var2);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0);
        entityData.define(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
        entityData.define(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 0);
        entityData.define(DATA_TRANSLATION_ID, new Vector3f());
        entityData.define(DATA_SCALE_ID, new Vector3f(1.0f, 1.0f, 1.0f));
        entityData.define(DATA_RIGHT_ROTATION_ID, new Quaternionf());
        entityData.define(DATA_LEFT_ROTATION_ID, new Quaternionf());
        entityData.define(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, BillboardConstraints.FIXED.getId());
        entityData.define(DATA_BRIGHTNESS_OVERRIDE_ID, -1);
        entityData.define(DATA_VIEW_RANGE_ID, Float.valueOf(1.0f));
        entityData.define(DATA_SHADOW_RADIUS_ID, Float.valueOf(0.0f));
        entityData.define(DATA_SHADOW_STRENGTH_ID, Float.valueOf(1.0f));
        entityData.define(DATA_WIDTH_ID, Float.valueOf(0.0f));
        entityData.define(DATA_HEIGHT_ID, Float.valueOf(0.0f));
        entityData.define(DATA_GLOW_COLOR_OVERRIDE_ID, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setTransformation(input.read(TAG_TRANSFORMATION, Transformation.EXTENDED_CODEC).orElse(Transformation.IDENTITY));
        this.setTransformationInterpolationDuration(input.getIntOr(TAG_TRANSFORMATION_INTERPOLATION_DURATION, 0));
        this.setTransformationInterpolationDelay(input.getIntOr(TAG_TRANSFORMATION_START_INTERPOLATION, 0));
        int teleportDuration = input.getIntOr(TAG_POS_ROT_INTERPOLATION_DURATION, 0);
        this.setPosRotInterpolationDuration(Mth.clamp(teleportDuration, 0, 59));
        this.setBillboardConstraints(input.read(TAG_BILLBOARD, BillboardConstraints.CODEC).orElse(BillboardConstraints.FIXED));
        this.setViewRange(input.getFloatOr(TAG_VIEW_RANGE, 1.0f));
        this.setShadowRadius(input.getFloatOr(TAG_SHADOW_RADIUS, 0.0f));
        this.setShadowStrength(input.getFloatOr(TAG_SHADOW_STRENGTH, 1.0f));
        this.setWidth(input.getFloatOr(TAG_WIDTH, 0.0f));
        this.setHeight(input.getFloatOr(TAG_HEIGHT, 0.0f));
        this.setGlowColorOverride(input.getIntOr(TAG_GLOW_COLOR_OVERRIDE, -1));
        this.setBrightnessOverride(input.read(TAG_BRIGHTNESS, Brightness.CODEC).orElse(null));
    }

    private void setTransformation(Transformation transformation) {
        this.entityData.set(DATA_TRANSLATION_ID, transformation.translation());
        this.entityData.set(DATA_LEFT_ROTATION_ID, transformation.leftRotation());
        this.entityData.set(DATA_SCALE_ID, transformation.scale());
        this.entityData.set(DATA_RIGHT_ROTATION_ID, transformation.rightRotation());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store(TAG_TRANSFORMATION, Transformation.EXTENDED_CODEC, Display.createTransformation(this.entityData));
        output.store(TAG_BILLBOARD, BillboardConstraints.CODEC, this.getBillboardConstraints());
        output.putInt(TAG_TRANSFORMATION_INTERPOLATION_DURATION, this.getTransformationInterpolationDuration());
        output.putInt(TAG_POS_ROT_INTERPOLATION_DURATION, this.getPosRotInterpolationDuration());
        output.putFloat(TAG_VIEW_RANGE, this.getViewRange());
        output.putFloat(TAG_SHADOW_RADIUS, this.getShadowRadius());
        output.putFloat(TAG_SHADOW_STRENGTH, this.getShadowStrength());
        output.putFloat(TAG_WIDTH, this.getWidth());
        output.putFloat(TAG_HEIGHT, this.getHeight());
        output.putInt(TAG_GLOW_COLOR_OVERRIDE, this.getGlowColorOverride());
        output.storeNullable(TAG_BRIGHTNESS, Brightness.CODEC, this.getBrightnessOverride());
    }

    public AABB getBoundingBoxForCulling() {
        return this.cullingBoundingBox;
    }

    public boolean affectedByCulling() {
        return !this.noCulling;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    public @Nullable RenderState renderState() {
        return this.renderState;
    }

    private void setTransformationInterpolationDuration(int duration) {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, duration);
    }

    private int getTransformationInterpolationDuration() {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID);
    }

    private void setTransformationInterpolationDelay(int ticks) {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, ticks, true);
    }

    private int getTransformationInterpolationDelay() {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID);
    }

    private void setPosRotInterpolationDuration(int duration) {
        this.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, duration);
    }

    private int getPosRotInterpolationDuration() {
        return this.entityData.get(DATA_POS_ROT_INTERPOLATION_DURATION_ID);
    }

    private void setBillboardConstraints(BillboardConstraints constraints) {
        this.entityData.set(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, constraints.getId());
    }

    private BillboardConstraints getBillboardConstraints() {
        return BillboardConstraints.BY_ID.apply(this.entityData.get(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID).byteValue());
    }

    private void setBrightnessOverride(@Nullable Brightness brightness) {
        this.entityData.set(DATA_BRIGHTNESS_OVERRIDE_ID, brightness != null ? brightness.pack() : -1);
    }

    private @Nullable Brightness getBrightnessOverride() {
        int value = this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
        return value != -1 ? Brightness.unpack(value) : null;
    }

    private int getPackedBrightnessOverride() {
        return this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
    }

    private void setViewRange(float range) {
        this.entityData.set(DATA_VIEW_RANGE_ID, Float.valueOf(range));
    }

    private float getViewRange() {
        return this.entityData.get(DATA_VIEW_RANGE_ID).floatValue();
    }

    private void setShadowRadius(float size) {
        this.entityData.set(DATA_SHADOW_RADIUS_ID, Float.valueOf(size));
    }

    private float getShadowRadius() {
        return this.entityData.get(DATA_SHADOW_RADIUS_ID).floatValue();
    }

    private void setShadowStrength(float strength) {
        this.entityData.set(DATA_SHADOW_STRENGTH_ID, Float.valueOf(strength));
    }

    private float getShadowStrength() {
        return this.entityData.get(DATA_SHADOW_STRENGTH_ID).floatValue();
    }

    private void setWidth(float width) {
        this.entityData.set(DATA_WIDTH_ID, Float.valueOf(width));
    }

    private float getWidth() {
        return this.entityData.get(DATA_WIDTH_ID).floatValue();
    }

    private void setHeight(float width) {
        this.entityData.set(DATA_HEIGHT_ID, Float.valueOf(width));
    }

    private int getGlowColorOverride() {
        return this.entityData.get(DATA_GLOW_COLOR_OVERRIDE_ID);
    }

    private void setGlowColorOverride(int value) {
        this.entityData.set(DATA_GLOW_COLOR_OVERRIDE_ID, value);
    }

    public float calculateInterpolationProgress(float partialTickTime) {
        float result;
        int duration = this.interpolationDuration;
        if (duration <= 0) {
            return 1.0f;
        }
        float ticksSinceUpdate = (long)this.tickCount - this.interpolationStartClientTick;
        float partialTicksSinceLastUpdate = ticksSinceUpdate + partialTickTime;
        this.lastProgress = result = Mth.clamp(Mth.inverseLerp(partialTicksSinceLastUpdate, 0.0f, duration), 0.0f, 1.0f);
        return result;
    }

    private float getHeight() {
        return this.entityData.get(DATA_HEIGHT_ID).floatValue();
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        this.updateCulling();
    }

    private void updateCulling() {
        float width = this.getWidth();
        float height = this.getHeight();
        this.noCulling = width == 0.0f || height == 0.0f;
        float w = width / 2.0f;
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        this.cullingBoundingBox = new AABB(x - (double)w, y, z - (double)w, x + (double)w, y + (double)height, z + (double)w);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < Mth.square((double)this.getViewRange() * 64.0 * Display.getViewScale());
    }

    @Override
    public int getTeamColor() {
        int glowColorOverride = this.getGlowColorOverride();
        return glowColorOverride != -1 ? glowColorOverride : super.getTeamColor();
    }

    private RenderState createFreshRenderState() {
        return new RenderState(GenericInterpolator.constant(Display.createTransformation(this.entityData)), this.getBillboardConstraints(), this.getPackedBrightnessOverride(), FloatInterpolator.constant(this.getShadowRadius()), FloatInterpolator.constant(this.getShadowStrength()), this.getGlowColorOverride());
    }

    private RenderState createInterpolatedRenderState(RenderState previousState, float progress) {
        Transformation currentTransform = previousState.transformation.get(progress);
        float currentShadowRadius = previousState.shadowRadius.get(progress);
        float currentShadowStrength = previousState.shadowStrength.get(progress);
        return new RenderState(new TransformationInterpolator(currentTransform, Display.createTransformation(this.entityData)), this.getBillboardConstraints(), this.getPackedBrightnessOverride(), new LinearFloatInterpolator(currentShadowRadius, this.getShadowRadius()), new LinearFloatInterpolator(currentShadowStrength, this.getShadowStrength()), this.getGlowColorOverride());
    }

    public record RenderState(GenericInterpolator<Transformation> transformation, BillboardConstraints billboardConstraints, int brightnessOverride, FloatInterpolator shadowRadius, FloatInterpolator shadowStrength, int glowColorOverride) {
    }

    public static enum BillboardConstraints implements StringRepresentable
    {
        FIXED(0, "fixed"),
        VERTICAL(1, "vertical"),
        HORIZONTAL(2, "horizontal"),
        CENTER(3, "center");

        public static final Codec<BillboardConstraints> CODEC;
        public static final IntFunction<BillboardConstraints> BY_ID;
        private final byte id;
        private final String name;

        private BillboardConstraints(byte id, String name) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        private byte getId() {
            return this.id;
        }

        static {
            CODEC = StringRepresentable.fromEnum(BillboardConstraints::values);
            BY_ID = ByIdMap.continuous(BillboardConstraints::getId, BillboardConstraints.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        }
    }

    @FunctionalInterface
    public static interface GenericInterpolator<T> {
        public static <T> GenericInterpolator<T> constant(T value) {
            return progress -> value;
        }

        public T get(float var1);
    }

    @FunctionalInterface
    public static interface FloatInterpolator {
        public static FloatInterpolator constant(float value) {
            return progress -> value;
        }

        public float get(float var1);
    }

    private record TransformationInterpolator(Transformation previous, Transformation current) implements GenericInterpolator<Transformation>
    {
        @Override
        public Transformation get(float progress) {
            if ((double)progress >= 1.0) {
                return this.current;
            }
            return this.previous.slerp(this.current, progress);
        }
    }

    private record LinearFloatInterpolator(float previous, float current) implements FloatInterpolator
    {
        @Override
        public float get(float progress) {
            return Mth.lerp(progress, this.previous, this.current);
        }
    }

    private record ColorInterpolator(int previous, int current) implements IntInterpolator
    {
        @Override
        public int get(float progress) {
            return ARGB.srgbLerp(progress, this.previous, this.current);
        }
    }

    private record LinearIntInterpolator(int previous, int current) implements IntInterpolator
    {
        @Override
        public int get(float progress) {
            return Mth.lerpInt(progress, this.previous, this.current);
        }
    }

    @FunctionalInterface
    public static interface IntInterpolator {
        public static IntInterpolator constant(int value) {
            return progress -> value;
        }

        public int get(float var1);
    }

    public static class TextDisplay
    extends Display {
        public static final String TAG_TEXT = "text";
        private static final String TAG_LINE_WIDTH = "line_width";
        private static final String TAG_TEXT_OPACITY = "text_opacity";
        private static final String TAG_BACKGROUND_COLOR = "background";
        private static final String TAG_SHADOW = "shadow";
        private static final String TAG_SEE_THROUGH = "see_through";
        private static final String TAG_USE_DEFAULT_BACKGROUND = "default_background";
        private static final String TAG_ALIGNMENT = "alignment";
        public static final byte FLAG_SHADOW = 1;
        public static final byte FLAG_SEE_THROUGH = 2;
        public static final byte FLAG_USE_DEFAULT_BACKGROUND = 4;
        public static final byte FLAG_ALIGN_LEFT = 8;
        public static final byte FLAG_ALIGN_RIGHT = 16;
        private static final byte INITIAL_TEXT_OPACITY = -1;
        public static final int INITIAL_BACKGROUND = 0x40000000;
        private static final int INITIAL_LINE_WIDTH = 200;
        private static final EntityDataAccessor<Component> DATA_TEXT_ID = SynchedEntityData.defineId(TextDisplay.class, EntityDataSerializers.COMPONENT);
        private static final EntityDataAccessor<Integer> DATA_LINE_WIDTH_ID = SynchedEntityData.defineId(TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> DATA_BACKGROUND_COLOR_ID = SynchedEntityData.defineId(TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Byte> DATA_TEXT_OPACITY_ID = SynchedEntityData.defineId(TextDisplay.class, EntityDataSerializers.BYTE);
        private static final EntityDataAccessor<Byte> DATA_STYLE_FLAGS_ID = SynchedEntityData.defineId(TextDisplay.class, EntityDataSerializers.BYTE);
        private static final IntSet TEXT_RENDER_STATE_IDS = IntSet.of((int[])new int[]{DATA_TEXT_ID.id(), DATA_LINE_WIDTH_ID.id(), DATA_BACKGROUND_COLOR_ID.id(), DATA_TEXT_OPACITY_ID.id(), DATA_STYLE_FLAGS_ID.id()});
        private @Nullable CachedInfo clientDisplayCache;
        private @Nullable TextRenderState textRenderState;

        public TextDisplay(EntityType<?> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder entityData) {
            super.defineSynchedData(entityData);
            entityData.define(DATA_TEXT_ID, Component.empty());
            entityData.define(DATA_LINE_WIDTH_ID, 200);
            entityData.define(DATA_BACKGROUND_COLOR_ID, 0x40000000);
            entityData.define(DATA_TEXT_OPACITY_ID, (byte)-1);
            entityData.define(DATA_STYLE_FLAGS_ID, (byte)0);
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
            super.onSyncedDataUpdated(accessor);
            if (TEXT_RENDER_STATE_IDS.contains(accessor.id())) {
                this.updateRenderState = true;
            }
        }

        private Component getText() {
            return this.entityData.get(DATA_TEXT_ID);
        }

        private void setText(Component text) {
            this.entityData.set(DATA_TEXT_ID, text);
        }

        private int getLineWidth() {
            return this.entityData.get(DATA_LINE_WIDTH_ID);
        }

        private void setLineWidth(int width) {
            this.entityData.set(DATA_LINE_WIDTH_ID, width);
        }

        private byte getTextOpacity() {
            return this.entityData.get(DATA_TEXT_OPACITY_ID);
        }

        private void setTextOpacity(byte opacity) {
            this.entityData.set(DATA_TEXT_OPACITY_ID, opacity);
        }

        private int getBackgroundColor() {
            return this.entityData.get(DATA_BACKGROUND_COLOR_ID);
        }

        private void setBackgroundColor(int color) {
            this.entityData.set(DATA_BACKGROUND_COLOR_ID, color);
        }

        private byte getFlags() {
            return this.entityData.get(DATA_STYLE_FLAGS_ID);
        }

        private void setFlags(byte flags) {
            this.entityData.set(DATA_STYLE_FLAGS_ID, flags);
        }

        private static byte loadFlag(byte flags, ValueInput input, String id, byte mask) {
            if (input.getBooleanOr(id, false)) {
                return (byte)(flags | mask);
            }
            return flags;
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
            super.readAdditionalSaveData(input);
            this.setLineWidth(input.getIntOr(TAG_LINE_WIDTH, 200));
            this.setTextOpacity(input.getByteOr(TAG_TEXT_OPACITY, (byte)-1));
            this.setBackgroundColor(input.getIntOr(TAG_BACKGROUND_COLOR, 0x40000000));
            byte flags = TextDisplay.loadFlag((byte)0, input, TAG_SHADOW, (byte)1);
            flags = TextDisplay.loadFlag(flags, input, TAG_SEE_THROUGH, (byte)2);
            flags = TextDisplay.loadFlag(flags, input, TAG_USE_DEFAULT_BACKGROUND, (byte)4);
            Optional<Align> alignment = input.read(TAG_ALIGNMENT, Align.CODEC);
            if (alignment.isPresent()) {
                flags = switch (alignment.get().ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 0 -> flags;
                    case 1 -> (byte)(flags | 8);
                    case 2 -> (byte)(flags | 0x10);
                };
            }
            this.setFlags(flags);
            Optional<Component> text = input.read(TAG_TEXT, ComponentSerialization.CODEC);
            if (text.isPresent()) {
                try {
                    Level level = this.level();
                    if (level instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel)level;
                        CommandSourceStack context = this.createCommandSourceStackForNameResolution(serverLevel).withPermission(LevelBasedPermissionSet.GAMEMASTER);
                        MutableComponent resolvedText = ComponentUtils.resolve(ResolutionContext.create(context), text.get());
                        this.setText(resolvedText);
                    } else {
                        this.setText(Component.empty());
                    }
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to parse display entity text {}", text, (Object)e);
                }
            }
        }

        private static void storeFlag(byte flags, ValueOutput output, String id, byte mask) {
            output.putBoolean(id, (flags & mask) != 0);
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
            super.addAdditionalSaveData(output);
            output.store(TAG_TEXT, ComponentSerialization.CODEC, this.getText());
            output.putInt(TAG_LINE_WIDTH, this.getLineWidth());
            output.putInt(TAG_BACKGROUND_COLOR, this.getBackgroundColor());
            output.putByte(TAG_TEXT_OPACITY, this.getTextOpacity());
            byte flags = this.getFlags();
            TextDisplay.storeFlag(flags, output, TAG_SHADOW, (byte)1);
            TextDisplay.storeFlag(flags, output, TAG_SEE_THROUGH, (byte)2);
            TextDisplay.storeFlag(flags, output, TAG_USE_DEFAULT_BACKGROUND, (byte)4);
            output.store(TAG_ALIGNMENT, Align.CODEC, TextDisplay.getAlign(flags));
        }

        @Override
        protected void updateRenderSubState(boolean shouldInterpolate, float progress) {
            this.textRenderState = shouldInterpolate && this.textRenderState != null ? this.createInterpolatedTextRenderState(this.textRenderState, progress) : this.createFreshTextRenderState();
            this.clientDisplayCache = null;
        }

        public @Nullable TextRenderState textRenderState() {
            return this.textRenderState;
        }

        private TextRenderState createFreshTextRenderState() {
            return new TextRenderState(this.getText(), this.getLineWidth(), IntInterpolator.constant(this.getTextOpacity()), IntInterpolator.constant(this.getBackgroundColor()), this.getFlags());
        }

        private TextRenderState createInterpolatedTextRenderState(TextRenderState previous, float progress) {
            int currentBackground = previous.backgroundColor.get(progress);
            int currentOpacity = previous.textOpacity.get(progress);
            return new TextRenderState(this.getText(), this.getLineWidth(), new LinearIntInterpolator(currentOpacity, this.getTextOpacity()), new ColorInterpolator(currentBackground, this.getBackgroundColor()), this.getFlags());
        }

        public CachedInfo cacheDisplay(LineSplitter splitter) {
            if (this.clientDisplayCache == null) {
                this.clientDisplayCache = this.textRenderState != null ? splitter.split(this.textRenderState.text(), this.textRenderState.lineWidth()) : new CachedInfo(List.of(), 0);
            }
            return this.clientDisplayCache;
        }

        public static Align getAlign(byte flags) {
            if ((flags & 8) != 0) {
                return Align.LEFT;
            }
            if ((flags & 0x10) != 0) {
                return Align.RIGHT;
            }
            return Align.CENTER;
        }

        public static enum Align implements StringRepresentable
        {
            CENTER("center"),
            LEFT("left"),
            RIGHT("right");

            public static final Codec<Align> CODEC;
            private final String name;

            private Align(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }

            static {
                CODEC = StringRepresentable.fromEnum(Align::values);
            }
        }

        public record TextRenderState(Component text, int lineWidth, IntInterpolator textOpacity, IntInterpolator backgroundColor, byte flags) {
        }

        public record CachedInfo(List<CachedLine> lines, int width) {
        }

        @FunctionalInterface
        public static interface LineSplitter {
            public CachedInfo split(Component var1, int var2);
        }

        public record CachedLine(FormattedCharSequence contents, int width) {
        }
    }

    public static class BlockDisplay
    extends Display {
        public static final String TAG_BLOCK_STATE = "block_state";
        private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(BlockDisplay.class, EntityDataSerializers.BLOCK_STATE);
        private @Nullable BlockRenderState blockRenderState;

        public BlockDisplay(EntityType<?> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder entityData) {
            super.defineSynchedData(entityData);
            entityData.define(DATA_BLOCK_STATE_ID, Blocks.AIR.defaultBlockState());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
            super.onSyncedDataUpdated(accessor);
            if (accessor.equals(DATA_BLOCK_STATE_ID)) {
                this.updateRenderState = true;
            }
        }

        private BlockState getBlockState() {
            return this.entityData.get(DATA_BLOCK_STATE_ID);
        }

        private void setBlockState(BlockState blockState) {
            this.entityData.set(DATA_BLOCK_STATE_ID, blockState);
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
            super.readAdditionalSaveData(input);
            this.setBlockState(input.read(TAG_BLOCK_STATE, BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState()));
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
            super.addAdditionalSaveData(output);
            output.store(TAG_BLOCK_STATE, BlockState.CODEC, this.getBlockState());
        }

        public @Nullable BlockRenderState blockRenderState() {
            return this.blockRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean shouldInterpolate, float progress) {
            this.blockRenderState = new BlockRenderState(this.getBlockState());
        }

        public record BlockRenderState(BlockState blockState) {
        }
    }

    public static class ItemDisplay
    extends Display {
        private static final String TAG_ITEM = "item";
        private static final String TAG_ITEM_DISPLAY = "item_display";
        private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(ItemDisplay.class, EntityDataSerializers.ITEM_STACK);
        private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(ItemDisplay.class, EntityDataSerializers.BYTE);
        private final SlotAccess slot = SlotAccess.of(this::getItemStack, this::setItemStack);
        private @Nullable ItemRenderState itemRenderState;

        public ItemDisplay(EntityType<?> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder entityData) {
            super.defineSynchedData(entityData);
            entityData.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
            entityData.define(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.NONE.getId());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
            super.onSyncedDataUpdated(accessor);
            if (DATA_ITEM_STACK_ID.equals(accessor) || DATA_ITEM_DISPLAY_ID.equals(accessor)) {
                this.updateRenderState = true;
            }
        }

        private ItemStack getItemStack() {
            return this.entityData.get(DATA_ITEM_STACK_ID);
        }

        private void setItemStack(ItemStack item) {
            this.entityData.set(DATA_ITEM_STACK_ID, item);
        }

        private void setItemTransform(ItemDisplayContext transform) {
            this.entityData.set(DATA_ITEM_DISPLAY_ID, transform.getId());
        }

        private ItemDisplayContext getItemTransform() {
            return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID).byteValue());
        }

        @Override
        protected void readAdditionalSaveData(ValueInput input) {
            super.readAdditionalSaveData(input);
            this.setItemStack(input.read(TAG_ITEM, ItemStack.CODEC).orElse(ItemStack.EMPTY));
            this.setItemTransform(input.read(TAG_ITEM_DISPLAY, ItemDisplayContext.CODEC).orElse(ItemDisplayContext.NONE));
        }

        @Override
        protected void addAdditionalSaveData(ValueOutput output) {
            super.addAdditionalSaveData(output);
            ItemStack itemStack = this.getItemStack();
            if (!itemStack.isEmpty()) {
                output.store(TAG_ITEM, ItemStack.CODEC, itemStack);
            }
            output.store(TAG_ITEM_DISPLAY, ItemDisplayContext.CODEC, this.getItemTransform());
        }

        @Override
        public @Nullable SlotAccess getSlot(int slot) {
            if (slot == 0) {
                return this.slot;
            }
            return null;
        }

        public @Nullable ItemRenderState itemRenderState() {
            return this.itemRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean shouldInterpolate, float progress) {
            this.itemRenderState = new ItemRenderState(this.getItemStack(), this.getItemTransform());
        }

        public record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform) {
        }
    }
}

