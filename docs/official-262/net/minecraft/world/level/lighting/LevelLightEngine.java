/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.LightEventListener;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.jspecify.annotations.Nullable;

public class LevelLightEngine
implements LightEventListener {
    public static final int LIGHT_SECTION_PADDING = 1;
    public static final LevelLightEngine EMPTY = new LevelLightEngine();
    protected final LevelHeightAccessor levelHeightAccessor;
    private final @Nullable LightEngine<?, ?> blockEngine;
    private final @Nullable LightEngine<?, ?> skyEngine;

    public LevelLightEngine(LightChunkGetter chunkSource, boolean hasBlockLight, boolean hasSkyLight) {
        this.levelHeightAccessor = chunkSource.getLevel();
        this.blockEngine = hasBlockLight ? new BlockLightEngine(chunkSource) : null;
        this.skyEngine = hasSkyLight ? new SkyLightEngine(chunkSource) : null;
    }

    private LevelLightEngine() {
        this.levelHeightAccessor = LevelHeightAccessor.create(0, 0);
        this.blockEngine = null;
        this.skyEngine = null;
    }

    @Override
    public void checkBlock(BlockPos pos) {
        if (this.blockEngine != null) {
            this.blockEngine.checkBlock(pos);
        }
        if (this.skyEngine != null) {
            this.skyEngine.checkBlock(pos);
        }
    }

    @Override
    public boolean hasLightWork() {
        if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
            return true;
        }
        return this.blockEngine != null && this.blockEngine.hasLightWork();
    }

    @Override
    public int runLightUpdates() {
        int count = 0;
        if (this.blockEngine != null) {
            count += this.blockEngine.runLightUpdates();
        }
        if (this.skyEngine != null) {
            count += this.skyEngine.runLightUpdates();
        }
        return count;
    }

    @Override
    public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {
        if (this.blockEngine != null) {
            this.blockEngine.updateSectionStatus(pos, sectionEmpty);
        }
        if (this.skyEngine != null) {
            this.skyEngine.updateSectionStatus(pos, sectionEmpty);
        }
    }

    @Override
    public void setLightEnabled(ChunkPos pos, boolean enable) {
        if (this.blockEngine != null) {
            this.blockEngine.setLightEnabled(pos, enable);
        }
        if (this.skyEngine != null) {
            this.skyEngine.setLightEnabled(pos, enable);
        }
    }

    @Override
    public void propagateLightSources(ChunkPos pos) {
        if (this.blockEngine != null) {
            this.blockEngine.propagateLightSources(pos);
        }
        if (this.skyEngine != null) {
            this.skyEngine.propagateLightSources(pos);
        }
    }

    public LayerLightEventListener getLayerListener(LightLayer layer) {
        if (layer == LightLayer.BLOCK) {
            if (this.blockEngine == null) {
                return LayerLightEventListener.DummyLightLayerEventListener.INSTANCE;
            }
            return this.blockEngine;
        }
        if (this.skyEngine == null) {
            return LayerLightEventListener.DummyLightLayerEventListener.INSTANCE;
        }
        return this.skyEngine;
    }

    public String getDebugData(LightLayer layer, SectionPos pos) {
        if (layer == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                return this.blockEngine.getDebugData(pos.asLong());
            }
        } else if (this.skyEngine != null) {
            return this.skyEngine.getDebugData(pos.asLong());
        }
        return "n/a";
    }

    public LayerLightSectionStorage.SectionType getDebugSectionType(LightLayer layer, SectionPos pos) {
        if (layer == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                return this.blockEngine.getDebugSectionType(pos.asLong());
            }
        } else if (this.skyEngine != null) {
            return this.skyEngine.getDebugSectionType(pos.asLong());
        }
        return LayerLightSectionStorage.SectionType.EMPTY;
    }

    public void queueSectionData(LightLayer layer, SectionPos pos, @Nullable DataLayer data) {
        if (layer == LightLayer.BLOCK) {
            if (this.blockEngine != null) {
                this.blockEngine.queueSectionData(pos.asLong(), data);
            }
        } else if (this.skyEngine != null) {
            this.skyEngine.queueSectionData(pos.asLong(), data);
        }
    }

    public void retainData(ChunkPos pos, boolean retain) {
        if (this.blockEngine != null) {
            this.blockEngine.retainData(pos, retain);
        }
        if (this.skyEngine != null) {
            this.skyEngine.retainData(pos, retain);
        }
    }

    public int getRawBrightness(BlockPos pos, int skyDampen) {
        int skyLight = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(pos) - skyDampen;
        int blockLight = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(pos);
        return Math.max(blockLight, skyLight);
    }

    public boolean lightOnInColumn(long sectionZeroNode) {
        return this.blockEngine == null || ((LayerLightSectionStorage)this.blockEngine.storage).lightOnInColumn(sectionZeroNode) && (this.skyEngine == null || ((LayerLightSectionStorage)this.skyEngine.storage).lightOnInColumn(sectionZeroNode));
    }

    public int getLightSectionCount() {
        return this.levelHeightAccessor.getSectionsCount() + 2;
    }

    public int getMinLightSection() {
        return this.levelHeightAccessor.getMinSectionY() - 1;
    }

    public int getMaxLightSection() {
        return this.getMinLightSection() + this.getLightSectionCount();
    }
}

