/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityFluidInteraction {
    private final Map<TagKey<Fluid>, Tracker> trackerByFluid = new Reference2ObjectArrayMap();

    public EntityFluidInteraction(Set<TagKey<Fluid>> fluids) {
        for (TagKey<Fluid> fluid : fluids) {
            this.trackerByFluid.put(fluid, new Tracker());
        }
    }

    public void update(Entity entity, boolean ignoreCurrent) {
        this.trackerByFluid.values().forEach(Tracker::reset);
        AABB box = entity.getFluidInteractionBox();
        if (box == null) {
            return;
        }
        int x0 = Mth.floor(box.minX);
        int y0 = Mth.floor(box.minY);
        int z0 = Mth.floor(box.minZ);
        int x1 = Mth.ceil(box.maxX) - 1;
        int y1 = Mth.ceil(box.maxY) - 1;
        int z1 = Mth.ceil(box.maxZ) - 1;
        if (!EntityFluidInteraction.hasFluidAndLoaded(entity.level(), x0 - 1, y0, z0 - 1, x1 + 1, y1, z1 + 1)) {
            return;
        }
        double entityY = entity.getBoundingBox().minY;
        int eyeBlockX = entity.getBlockX();
        double eyeY = entity.getEyeY();
        int eyeBlockZ = entity.getBlockZ();
        Fluid lastFluidType = null;
        Tracker tracker = null;
        Level level = entity.level();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; ++x) {
            for (int y = y0; y <= y1; ++y) {
                for (int z = z0; z <= z1; ++z) {
                    double fluidBottom;
                    double fluidTop;
                    mutablePos.set(x, y, z);
                    FluidState fluidState = level.getFluidState(mutablePos);
                    if (fluidState.isEmpty() || (fluidTop = (fluidBottom = (double)mutablePos.getY()) + (double)fluidState.getHeight(level, mutablePos)) < box.minY) continue;
                    Fluid fluidType = fluidState.getType();
                    if (fluidType != lastFluidType) {
                        lastFluidType = fluidType;
                        tracker = this.getTrackerFor(fluidType);
                    }
                    if (tracker == null) continue;
                    if (x == eyeBlockX && z == eyeBlockZ && eyeY >= fluidBottom && eyeY <= fluidTop) {
                        tracker.eyesInside = true;
                    }
                    tracker.height = Math.max(fluidTop - entityY, tracker.height);
                    if (ignoreCurrent) continue;
                    Vec3 flow = fluidState.getFlow(level, mutablePos);
                    if (tracker.height < 0.4) {
                        flow = flow.scale(tracker.height);
                    }
                    tracker.accumulateCurrent(flow);
                }
            }
        }
    }

    private static boolean hasFluidAndLoaded(Level level, int x0, int y0, int z0, int x1, int y1, int z1) {
        int sectionX0 = SectionPos.blockToSectionCoord(x0);
        int sectionY0 = SectionPos.blockToSectionCoord(y0);
        int sectionZ0 = SectionPos.blockToSectionCoord(z0);
        int sectionX1 = SectionPos.blockToSectionCoord(x1);
        int sectionY1 = SectionPos.blockToSectionCoord(y1);
        int sectionZ1 = SectionPos.blockToSectionCoord(z1);
        boolean hasFluid = false;
        for (int chunkZ = sectionZ0; chunkZ <= sectionZ1; ++chunkZ) {
            for (int chunkX = sectionX0; chunkX <= sectionX1; ++chunkX) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    return false;
                }
                LevelChunkSection[] sections = chunk.getSections();
                for (int sectionY = sectionY0; sectionY <= sectionY1; ++sectionY) {
                    int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
                    if (sectionIndex < 0 || sectionIndex >= sections.length) continue;
                    hasFluid |= sections[sectionIndex].hasFluid();
                }
            }
        }
        return hasFluid;
    }

    private @Nullable Tracker getTrackerFor(Fluid fluid) {
        for (Map.Entry<TagKey<Fluid>, Tracker> entry : this.trackerByFluid.entrySet()) {
            TagKey<Fluid> tag = entry.getKey();
            if (!fluid.is(tag)) continue;
            return entry.getValue();
        }
        return null;
    }

    public void applyCurrentTo(TagKey<Fluid> fluid, Entity entity, double scale) {
        Tracker tracker = this.trackerByFluid.get(fluid);
        if (tracker != null) {
            tracker.applyCurrentTo(entity, scale);
        }
    }

    public double getFluidHeight(TagKey<Fluid> fluid) {
        Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null ? tracker.height : 0.0;
    }

    public boolean isInFluid(TagKey<Fluid> fluid) {
        return this.getFluidHeight(fluid) > 0.0;
    }

    public boolean isEyeInFluid(TagKey<Fluid> fluid) {
        Tracker tracker = this.trackerByFluid.get(fluid);
        return tracker != null && tracker.eyesInside;
    }

    private static class Tracker {
        private double height;
        private boolean eyesInside;
        private Vec3 accumulatedCurrent = Vec3.ZERO;
        private int currentCount;

        private Tracker() {
        }

        public void reset() {
            this.height = 0.0;
            this.eyesInside = false;
            this.accumulatedCurrent = Vec3.ZERO;
            this.currentCount = 0;
        }

        public void accumulateCurrent(Vec3 flow) {
            this.accumulatedCurrent = this.accumulatedCurrent.add(flow);
            ++this.currentCount;
        }

        public void applyCurrentTo(Entity entity, double scale) {
            if (this.currentCount == 0 || this.accumulatedCurrent.lengthSqr() < (double)1.0E-5f) {
                return;
            }
            Vec3 impulse = !(entity instanceof Player) ? this.accumulatedCurrent.normalize() : this.accumulatedCurrent.scale(1.0 / (double)this.currentCount);
            Vec3 oldMovement = entity.getDeltaMovement();
            impulse = impulse.scale(scale);
            double min = 0.003;
            if (Math.abs(oldMovement.x) < 0.003 && Math.abs(oldMovement.z) < 0.003 && impulse.length() < 0.0045000000000000005) {
                impulse = impulse.normalize().scale(0.0045000000000000005);
            }
            entity.addDeltaMovement(impulse);
        }
    }
}

