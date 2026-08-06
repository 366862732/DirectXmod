/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 */
package net.minecraft.world.level.lighting;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;

public final class BlockLightEngine
extends LightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    public BlockLightEngine(LightChunkGetter chunkSource) {
        this(chunkSource, new BlockLightSectionStorage(chunkSource));
    }

    @VisibleForTesting
    public BlockLightEngine(LightChunkGetter chunkSource, BlockLightSectionStorage storage) {
        super(chunkSource, storage);
    }

    @Override
    protected void checkNode(long blockNode) {
        int oldLevel;
        long sectionNode = SectionPos.blockToSection(blockNode);
        if (!((BlockLightSectionStorage)this.storage).storingLightForSection(sectionNode)) {
            return;
        }
        BlockState state = this.getState(this.mutablePos.set(blockNode));
        int lightEmission = this.getEmission(blockNode, state);
        if (lightEmission < (oldLevel = ((BlockLightSectionStorage)this.storage).getStoredLevel(blockNode))) {
            ((BlockLightSectionStorage)this.storage).setStoredLevel(blockNode, 0);
            this.enqueueDecrease(blockNode, LightEngine.QueueEntry.decreaseAllDirections(oldLevel));
        } else {
            this.enqueueDecrease(blockNode, PULL_LIGHT_IN_ENTRY);
        }
        if (lightEmission > 0) {
            this.enqueueIncrease(blockNode, LightEngine.QueueEntry.increaseLightFromEmission(lightEmission, BlockLightEngine.isEmptyShape(state)));
        }
    }

    @Override
    protected void propagateIncrease(long fromNode, long increaseData, int fromLevel) {
        BlockState fromState = null;
        for (Direction propagationDirection : PROPAGATION_DIRECTIONS) {
            int toLevel;
            int maxPossibleNewToLevel;
            long toNode;
            if (!LightEngine.QueueEntry.shouldPropagateInDirection(increaseData, propagationDirection) || !((BlockLightSectionStorage)this.storage).storingLightForSection(SectionPos.blockToSection(toNode = BlockPos.offset(fromNode, propagationDirection))) || (maxPossibleNewToLevel = fromLevel - 1) <= (toLevel = ((BlockLightSectionStorage)this.storage).getStoredLevel(toNode))) continue;
            this.mutablePos.set(toNode);
            BlockState toState = this.getState(this.mutablePos);
            int newToLevel = fromLevel - this.getOpacity(toState);
            if (newToLevel <= toLevel) continue;
            if (fromState == null) {
                BlockState blockState = fromState = LightEngine.QueueEntry.isFromEmptyShape(increaseData) ? Blocks.AIR.defaultBlockState() : this.getState(this.mutablePos.set(fromNode));
            }
            if (this.shapeOccludes(fromState, toState, propagationDirection)) continue;
            ((BlockLightSectionStorage)this.storage).setStoredLevel(toNode, newToLevel);
            if (newToLevel <= 1) continue;
            this.enqueueIncrease(toNode, LightEngine.QueueEntry.increaseSkipOneDirection(newToLevel, BlockLightEngine.isEmptyShape(toState), propagationDirection.getOpposite()));
        }
    }

    @Override
    protected void propagateDecrease(long fromNode, long decreaseData) {
        int oldFromLevel = LightEngine.QueueEntry.getFromLevel(decreaseData);
        for (Direction propagationDirection : PROPAGATION_DIRECTIONS) {
            int toLevel;
            long toNode;
            if (!LightEngine.QueueEntry.shouldPropagateInDirection(decreaseData, propagationDirection) || !((BlockLightSectionStorage)this.storage).storingLightForSection(SectionPos.blockToSection(toNode = BlockPos.offset(fromNode, propagationDirection))) || (toLevel = ((BlockLightSectionStorage)this.storage).getStoredLevel(toNode)) == 0) continue;
            if (toLevel <= oldFromLevel - 1) {
                BlockState toState = this.getState(this.mutablePos.set(toNode));
                int toEmission = this.getEmission(toNode, toState);
                ((BlockLightSectionStorage)this.storage).setStoredLevel(toNode, 0);
                if (toEmission < toLevel) {
                    this.enqueueDecrease(toNode, LightEngine.QueueEntry.decreaseSkipOneDirection(toLevel, propagationDirection.getOpposite()));
                }
                if (toEmission <= 0) continue;
                this.enqueueIncrease(toNode, LightEngine.QueueEntry.increaseLightFromEmission(toEmission, BlockLightEngine.isEmptyShape(toState)));
                continue;
            }
            this.enqueueIncrease(toNode, LightEngine.QueueEntry.increaseOnlyOneDirection(toLevel, false, propagationDirection.getOpposite()));
        }
    }

    private int getEmission(long blockNode, BlockState state) {
        int emission = state.getLightEmission();
        if (emission > 0 && ((BlockLightSectionStorage)this.storage).lightOnInSection(SectionPos.blockToSection(blockNode))) {
            return emission;
        }
        return 0;
    }

    @Override
    public void propagateLightSources(ChunkPos pos) {
        this.setLightEnabled(pos, true);
        LightChunk chunk = this.chunkSource.getChunkForLighting(pos.x(), pos.z());
        if (chunk != null) {
            chunk.findBlockLightSources((lightPos, state) -> {
                int lightEmission = state.getLightEmission();
                this.enqueueIncrease(lightPos.asLong(), LightEngine.QueueEntry.increaseLightFromEmission(lightEmission, BlockLightEngine.isEmptyShape(state)));
            });
        }
    }
}

