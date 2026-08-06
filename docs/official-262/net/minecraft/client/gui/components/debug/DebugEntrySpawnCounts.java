/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntrySpawnCounts
implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        ServerLevel level;
        ServerLevel serverLevel;
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        ServerLevel serverLevel2 = serverLevel = serverOrClientLevel instanceof ServerLevel ? (level = (ServerLevel)serverOrClientLevel) : null;
        if (entity == null || serverLevel == null) {
            return;
        }
        ServerChunkCache chunkSource = serverLevel.getChunkSource();
        NaturalSpawner.SpawnState lastSpawnState = chunkSource.getLastSpawnState();
        if (lastSpawnState != null) {
            Object2IntMap<MobCategory> mobCategoryCounts = lastSpawnState.getMobCategoryCounts();
            int chunkCount = lastSpawnState.getSpawnableChunkCount();
            displayer.addLine("SC: " + chunkCount + ", " + Stream.of(MobCategory.values()).map(c -> c.getDebugAbbreviation() + ": " + mobCategoryCounts.getInt(c)).collect(Collectors.joining(", ")));
        }
    }
}

