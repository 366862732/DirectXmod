/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder
extends SavedData {
    public static final double MAX_SIZE = 5.9999968E7;
    public static final double MAX_CENTER_COORDINATE = 2.9999984E7;
    public static final Codec<WorldBorder> CODEC = Settings.CODEC.xmap(WorldBorder::new, Settings::new);
    public static final SavedDataType<WorldBorder> TYPE = new SavedDataType<WorldBorder>(Identifier.withDefaultNamespace("world_border"), WorldBorder::new, CODEC, DataFixTypes.SAVED_DATA_WORLD_BORDER);
    private final Settings settings;
    private boolean initialized;
    private final List<BorderChangeListener> listeners = Lists.newArrayList();
    private double damagePerBlock = 0.2;
    private double safeZone = 5.0;
    private int warningTime = 15;
    private int warningBlocks = 5;
    private double centerX;
    private double centerZ;
    private int absoluteMaxSize = 29999984;
    private BorderExtent extent = new StaticBorderExtent(this, 5.9999968E7);

    public WorldBorder() {
        this(Settings.DEFAULT);
    }

    public WorldBorder(Settings settings) {
        this.settings = settings;
    }

    public boolean isWithinBounds(BlockPos pos) {
        return this.isWithinBounds(pos.getX(), pos.getZ());
    }

    public boolean isWithinBounds(Vec3 pos) {
        return this.isWithinBounds(pos.x, pos.z);
    }

    public boolean isWithinBounds(ChunkPos pos) {
        return this.isWithinBounds(pos.getMinBlockX(), pos.getMinBlockZ()) && this.isWithinBounds(pos.getMaxBlockX(), pos.getMaxBlockZ());
    }

    public boolean isWithinBounds(AABB aabb) {
        return this.isWithinBounds(aabb.minX, aabb.minZ, aabb.maxX - (double)1.0E-5f, aabb.maxZ - (double)1.0E-5f);
    }

    private boolean isWithinBounds(double minX, double minZ, double maxX, double maxZ) {
        return this.isWithinBounds(minX, minZ) && this.isWithinBounds(maxX, maxZ);
    }

    public boolean isWithinBounds(double x, double z) {
        return this.isWithinBounds(x, z, 0.0);
    }

    public boolean isWithinBounds(double x, double z, double margin) {
        return x >= this.getMinX() - margin && x < this.getMaxX() + margin && z >= this.getMinZ() - margin && z < this.getMaxZ() + margin;
    }

    public BlockPos clampToBounds(BlockPos position) {
        return this.clampToBounds(position.getX(), position.getY(), position.getZ());
    }

    public BlockPos clampToBounds(Vec3 position) {
        return this.clampToBounds(position.x(), position.y(), position.z());
    }

    public BlockPos clampToBounds(double x, double y, double z) {
        return BlockPos.containing(this.clampVec3ToBound(x, y, z));
    }

    public Vec3 clampVec3ToBound(Vec3 position) {
        return this.clampVec3ToBound(position.x, position.y, position.z);
    }

    public Vec3 clampVec3ToBound(double x, double y, double z) {
        return new Vec3(Mth.clamp(x, this.getMinX(), this.getMaxX() - (double)1.0E-5f), y, Mth.clamp(z, this.getMinZ(), this.getMaxZ() - (double)1.0E-5f));
    }

    public double getDistanceToBorder(Entity entity) {
        return this.getDistanceToBorder(entity.getX(), entity.getZ());
    }

    public VoxelShape getCollisionShape() {
        return this.extent.getCollisionShape();
    }

    public double getDistanceToBorder(double x, double z) {
        double fromNorth = z - this.getMinZ();
        double fromSouth = this.getMaxZ() - z;
        double fromWest = x - this.getMinX();
        double fromEast = this.getMaxX() - x;
        double min = Math.min(fromWest, fromEast);
        min = Math.min(min, fromNorth);
        return Math.min(min, fromSouth);
    }

    public boolean isInsideCloseToBorder(Entity source, AABB boundingBox) {
        double bbMax = Math.max(Mth.absMax(boundingBox.getXsize(), boundingBox.getZsize()), 1.0);
        return this.getDistanceToBorder(source) < bbMax * 2.0 && this.isWithinBounds(source.getX(), source.getZ(), bbMax);
    }

    public BorderStatus getStatus() {
        return this.extent.getStatus();
    }

    public double getMinX() {
        return this.getMinX(0.0f);
    }

    public double getMinX(float deltaPartialTick) {
        return this.extent.getMinX(deltaPartialTick);
    }

    public double getMinZ() {
        return this.getMinZ(0.0f);
    }

    public double getMinZ(float deltaPartialTick) {
        return this.extent.getMinZ(deltaPartialTick);
    }

    public double getMaxX() {
        return this.getMaxX(0.0f);
    }

    public double getMaxX(float deltaPartialTick) {
        return this.extent.getMaxX(deltaPartialTick);
    }

    public double getMaxZ() {
        return this.getMaxZ(0.0f);
    }

    public double getMaxZ(float deltaPartialTick) {
        return this.extent.getMaxZ(deltaPartialTick);
    }

    public double getCenterX() {
        return this.centerX;
    }

    public double getCenterZ() {
        return this.centerZ;
    }

    public void setCenter(double x, double z) {
        this.centerX = x;
        this.centerZ = z;
        this.extent.onCenterChange();
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onSetCenter(this, x, z);
        }
    }

    public double getSize() {
        return this.extent.getSize();
    }

    public long getLerpTime() {
        return this.extent.getLerpTime();
    }

    public double getLerpTarget() {
        return this.extent.getLerpTarget();
    }

    public void setSize(double size) {
        this.extent = new StaticBorderExtent(this, size);
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onSetSize(this, size);
        }
    }

    public void lerpSizeBetween(double from, double to, long ticks, long gameTime) {
        this.extent = from == to ? new StaticBorderExtent(this, to) : new MovingBorderExtent(this, from, to, ticks, gameTime);
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onLerpSize(this, from, to, ticks, gameTime);
        }
    }

    protected List<BorderChangeListener> getListeners() {
        return Lists.newArrayList(this.listeners);
    }

    public void addListener(BorderChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(BorderChangeListener listener) {
        this.listeners.remove(listener);
    }

    public void setAbsoluteMaxSize(int absoluteMaxSize) {
        this.absoluteMaxSize = absoluteMaxSize;
        this.extent.onAbsoluteMaxSizeChange();
    }

    public int getAbsoluteMaxSize() {
        return this.absoluteMaxSize;
    }

    public double getSafeZone() {
        return this.safeZone;
    }

    public void setSafeZone(double safeZone) {
        this.safeZone = safeZone;
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onSetSafeZone(this, safeZone);
        }
    }

    public double getDamagePerBlock() {
        return this.damagePerBlock;
    }

    public void setDamagePerBlock(double damagePerBlock) {
        this.damagePerBlock = damagePerBlock;
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onSetDamagePerBlock(this, damagePerBlock);
        }
    }

    public double getLerpSpeed() {
        return this.extent.getLerpSpeed();
    }

    public int getWarningTime() {
        return this.warningTime;
    }

    public void setWarningTime(int warningTime) {
        this.warningTime = warningTime;
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onSetWarningTime(this, warningTime);
        }
    }

    public int getWarningBlocks() {
        return this.warningBlocks;
    }

    public void setWarningBlocks(int warningBlocks) {
        this.warningBlocks = warningBlocks;
        this.setDirty();
        for (BorderChangeListener listener : this.getListeners()) {
            listener.onSetWarningBlocks(this, warningBlocks);
        }
    }

    public void tick() {
        this.extent = this.extent.update();
    }

    public void applyInitialSettings(long gameTime) {
        if (!this.initialized) {
            this.setCenter(this.settings.centerX(), this.settings.centerZ());
            this.setDamagePerBlock(this.settings.damagePerBlock());
            this.setSafeZone(this.settings.safeZone());
            this.setWarningBlocks(this.settings.warningBlocks());
            this.setWarningTime(this.settings.warningTime());
            if (this.settings.lerpTime() > 0L) {
                this.lerpSizeBetween(this.settings.size(), this.settings.lerpTarget(), this.settings.lerpTime(), gameTime);
            } else {
                this.setSize(this.settings.size());
            }
            this.initialized = true;
        }
    }

    public record Settings(double centerX, double centerZ, double damagePerBlock, double safeZone, int warningBlocks, int warningTime, double size, long lerpTime, double lerpTarget) {
        public static final Settings DEFAULT = new Settings(0.0, 0.0, 0.2, 5.0, 5, 300, 5.9999968E7, 0L, 0.0);
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.doubleRange((double)-2.9999984E7, (double)2.9999984E7).fieldOf("center_x").forGetter(Settings::centerX), (App)Codec.doubleRange((double)-2.9999984E7, (double)2.9999984E7).fieldOf("center_z").forGetter(Settings::centerZ), (App)Codec.DOUBLE.fieldOf("damage_per_block").forGetter(Settings::damagePerBlock), (App)Codec.DOUBLE.fieldOf("safe_zone").forGetter(Settings::safeZone), (App)Codec.INT.fieldOf("warning_blocks").forGetter(Settings::warningBlocks), (App)Codec.INT.fieldOf("warning_time").forGetter(Settings::warningTime), (App)Codec.DOUBLE.fieldOf("size").forGetter(Settings::size), (App)Codec.LONG.fieldOf("lerp_time").forGetter(Settings::lerpTime), (App)Codec.DOUBLE.fieldOf("lerp_target").forGetter(Settings::lerpTarget)).apply((Applicative)i, Settings::new));

        public Settings(WorldBorder worldBorder) {
            this(worldBorder.centerX, worldBorder.centerZ, worldBorder.damagePerBlock, worldBorder.safeZone, worldBorder.warningBlocks, worldBorder.warningTime, worldBorder.extent.getSize(), worldBorder.extent.getLerpTime(), worldBorder.extent.getLerpTarget());
        }
    }

    private class StaticBorderExtent
    implements BorderExtent {
        private final double size;
        private double minX;
        private double minZ;
        private double maxX;
        private double maxZ;
        private VoxelShape shape;
        final /* synthetic */ WorldBorder this$0;

        public StaticBorderExtent(WorldBorder worldBorder, double size) {
            WorldBorder worldBorder2 = worldBorder;
            Objects.requireNonNull(worldBorder2);
            this.this$0 = worldBorder2;
            this.size = size;
            this.updateBox();
        }

        @Override
        public double getMinX(float deltaPartialTick) {
            return this.minX;
        }

        @Override
        public double getMaxX(float deltaPartialTick) {
            return this.maxX;
        }

        @Override
        public double getMinZ(float deltaPartialTick) {
            return this.minZ;
        }

        @Override
        public double getMaxZ(float deltaPartialTick) {
            return this.maxZ;
        }

        @Override
        public double getSize() {
            return this.size;
        }

        @Override
        public BorderStatus getStatus() {
            return BorderStatus.STATIONARY;
        }

        @Override
        public double getLerpSpeed() {
            return 0.0;
        }

        @Override
        public long getLerpTime() {
            return 0L;
        }

        @Override
        public double getLerpTarget() {
            return this.size;
        }

        private void updateBox() {
            this.minX = Mth.clamp(this.this$0.getCenterX() - this.size / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
            this.minZ = Mth.clamp(this.this$0.getCenterZ() - this.size / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
            this.maxX = Mth.clamp(this.this$0.getCenterX() + this.size / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
            this.maxZ = Mth.clamp(this.this$0.getCenterZ() + this.size / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
            this.shape = Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX(0.0f)), Double.NEGATIVE_INFINITY, Math.floor(this.getMinZ(0.0f)), Math.ceil(this.getMaxX(0.0f)), Double.POSITIVE_INFINITY, Math.ceil(this.getMaxZ(0.0f))), BooleanOp.ONLY_FIRST);
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
            this.updateBox();
        }

        @Override
        public void onCenterChange() {
            this.updateBox();
        }

        @Override
        public BorderExtent update() {
            return this;
        }

        @Override
        public VoxelShape getCollisionShape() {
            return this.shape;
        }
    }

    private static interface BorderExtent {
        public double getMinX(float var1);

        public double getMaxX(float var1);

        public double getMinZ(float var1);

        public double getMaxZ(float var1);

        public double getSize();

        public double getLerpSpeed();

        public long getLerpTime();

        public double getLerpTarget();

        public BorderStatus getStatus();

        public void onAbsoluteMaxSizeChange();

        public void onCenterChange();

        public BorderExtent update();

        public VoxelShape getCollisionShape();
    }

    private class MovingBorderExtent
    implements BorderExtent {
        private final double from;
        private final double to;
        private final long lerpEnd;
        private final long lerpBegin;
        private final double lerpDuration;
        private long lerpProgress;
        private double size;
        private double previousSize;
        final /* synthetic */ WorldBorder this$0;

        private MovingBorderExtent(WorldBorder worldBorder, double from, double to, long duration, long gameTime) {
            double size;
            WorldBorder worldBorder2 = worldBorder;
            Objects.requireNonNull(worldBorder2);
            this.this$0 = worldBorder2;
            this.from = from;
            this.to = to;
            this.lerpDuration = duration;
            this.lerpProgress = duration;
            this.lerpBegin = gameTime;
            this.lerpEnd = this.lerpBegin + duration;
            this.size = size = this.calculateSize();
            this.previousSize = size;
        }

        @Override
        public double getMinX(float deltaPartialTick) {
            return Mth.clamp(this.this$0.getCenterX() - Mth.lerp((double)deltaPartialTick, this.getPreviousSize(), this.getSize()) / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
        }

        @Override
        public double getMinZ(float deltaPartialTick) {
            return Mth.clamp(this.this$0.getCenterZ() - Mth.lerp((double)deltaPartialTick, this.getPreviousSize(), this.getSize()) / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
        }

        @Override
        public double getMaxX(float deltaPartialTick) {
            return Mth.clamp(this.this$0.getCenterX() + Mth.lerp((double)deltaPartialTick, this.getPreviousSize(), this.getSize()) / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
        }

        @Override
        public double getMaxZ(float deltaPartialTick) {
            return Mth.clamp(this.this$0.getCenterZ() + Mth.lerp((double)deltaPartialTick, this.getPreviousSize(), this.getSize()) / 2.0, (double)(-this.this$0.absoluteMaxSize), (double)this.this$0.absoluteMaxSize);
        }

        @Override
        public double getSize() {
            return this.size;
        }

        public double getPreviousSize() {
            return this.previousSize;
        }

        private double calculateSize() {
            double progress = (this.lerpDuration - (double)this.lerpProgress) / this.lerpDuration;
            return progress < 1.0 ? Mth.lerp(progress, this.from, this.to) : this.to;
        }

        @Override
        public double getLerpSpeed() {
            return Math.abs(this.from - this.to) / (double)(this.lerpEnd - this.lerpBegin);
        }

        @Override
        public long getLerpTime() {
            return this.lerpProgress;
        }

        @Override
        public double getLerpTarget() {
            return this.to;
        }

        @Override
        public BorderStatus getStatus() {
            return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
        }

        @Override
        public void onCenterChange() {
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
        }

        @Override
        public BorderExtent update() {
            --this.lerpProgress;
            this.previousSize = this.size;
            this.size = this.calculateSize();
            this.this$0.setDirty();
            if (this.lerpProgress <= 0L) {
                return new StaticBorderExtent(this.this$0, this.to);
            }
            return this;
        }

        @Override
        public VoxelShape getCollisionShape() {
            return Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX(0.0f)), Double.NEGATIVE_INFINITY, Math.floor(this.getMinZ(0.0f)), Math.ceil(this.getMaxX(0.0f)), Double.POSITIVE_INFINITY, Math.ceil(this.getMaxZ(0.0f))), BooleanOp.ONLY_FIRST);
        }
    }
}

