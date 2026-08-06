/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SectionUpdateTracker {
    private final RotatingSectionStorage<SectionDirtyState> storage;

    public SectionUpdateTracker(LevelHeightAccessor levelHeightAccessor, int renderDistance) {
        this.storage = new RotatingSectionStorage<SectionDirtyState>(renderDistance, levelHeightAccessor.getMinSectionY(), levelHeightAccessor.getMaxSectionY(), (index, sectionNode) -> new SectionDirtyState(true, false, sectionNode));
    }

    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean playerChanged) {
        SectionDirtyState section = this.storage.getValue(sectionX, sectionY, sectionZ);
        if (section != null) {
            section.setDirty(playerChanged);
        }
    }

    public void repositionCamera(SectionPos cameraSectionPos) {
        this.storage.repositionCenter(cameraSectionPos);
    }

    public int size() {
        return this.storage.size();
    }

    public @Nullable SectionDirtyState getDirtyState(long sectionNode) {
        return this.storage.getValue(sectionNode);
    }

    public boolean hasAllNeighbors(ClientLevel level, long sectionNode) {
        return this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.WEST)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.NORTH)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.EAST)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, Direction.SOUTH)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, -1, 0, -1)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, -1, 0, 1)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, 1, 0, -1)) && this.doesChunkExistAt(level, SectionPos.offset(sectionNode, 1, 0, 1));
    }

    private boolean doesChunkExistAt(ClientLevel level, long sectionNode) {
        ChunkAccess chunk = level.getChunk(SectionPos.x(sectionNode), SectionPos.z(sectionNode), ChunkStatus.FULL, false);
        return chunk != null && level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(sectionNode));
    }

    @Environment(value=EnvType.CLIENT)
    public static class SectionDirtyState
    implements RotatingSectionStorage.Value {
        private boolean isDirty;
        private boolean isDirtyFromPlayer;
        private long sectionNode;

        private SectionDirtyState(boolean isDirty, boolean isDirtyFromPlayer, long sectionNode) {
            this.isDirty = isDirty;
            this.isDirtyFromPlayer = isDirtyFromPlayer;
        }

        public void setDirty(boolean fromPlayer) {
            boolean wasDirty = this.isDirty;
            this.isDirty = true;
            this.isDirtyFromPlayer = fromPlayer | (wasDirty && this.isDirtyFromPlayer);
        }

        public void setNotDirty() {
            this.isDirty = false;
            this.isDirtyFromPlayer = false;
        }

        @Override
        public void setSectionNode(long sectionNode) {
            if (this.sectionNode != sectionNode) {
                this.sectionNode = sectionNode;
                this.isDirty = true;
                this.isDirtyFromPlayer = false;
            }
        }

        @Override
        public long getSectionNode() {
            return this.sectionNode;
        }

        public boolean isDirty() {
            return this.isDirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.isDirtyFromPlayer;
        }
    }
}

