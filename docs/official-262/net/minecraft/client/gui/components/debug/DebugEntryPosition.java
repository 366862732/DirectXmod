/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongSets
 *  it.unimi.dsi.fastutil.longs.LongSets$EmptySet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryPosition
implements DebugScreenEntry {
    public static final Identifier GROUP = Identifier.withDefaultNamespace("position");

    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        LongSets.EmptySet emptySet;
        String faceString;
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity == null) {
            return;
        }
        BlockPos feetPos = minecraft.getCameraEntity().blockPosition();
        ChunkPos chunkPos = ChunkPos.containing(feetPos);
        Direction direction = entity.getDirection();
        switch (direction) {
            case NORTH: {
                String string = "Towards negative Z";
                break;
            }
            case SOUTH: {
                String string = "Towards positive Z";
                break;
            }
            case WEST: {
                String string = "Towards negative X";
                break;
            }
            case EAST: {
                String string = "Towards positive X";
                break;
            }
            default: {
                String string = faceString = "Invalid";
            }
        }
        if (serverOrClientLevel instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)serverOrClientLevel;
            emptySet = serverLevel.getForceLoadedChunks();
        } else {
            emptySet = LongSets.EMPTY_SET;
        }
        LongSets.EmptySet chunks = emptySet;
        displayer.addToGroup(GROUP, List.of(String.format(Locale.ROOT, "XYZ: %.3f / %.5f / %.3f", minecraft.getCameraEntity().getX(), minecraft.getCameraEntity().getY(), minecraft.getCameraEntity().getZ()), String.format(Locale.ROOT, "Block: %d %d %d", feetPos.getX(), feetPos.getY(), feetPos.getZ()), String.format(Locale.ROOT, "Chunk: %d %d %d [%d %d in r.%d.%d.mca]", chunkPos.x(), SectionPos.blockToSectionCoord(feetPos.getY()), chunkPos.z(), chunkPos.getRegionLocalX(), chunkPos.getRegionLocalZ(), chunkPos.getRegionX(), chunkPos.getRegionZ()), String.format(Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", direction, faceString, Float.valueOf(Mth.wrapDegrees(entity.getYRot())), Float.valueOf(Mth.wrapDegrees(entity.getXRot()))), String.valueOf(minecraft.level.dimension().identifier()) + " FC: " + chunks.size()));
    }
}

