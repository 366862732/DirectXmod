/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryChunkGeneration
implements DebugScreenEntry {
    private static final Identifier GROUP = Identifier.withDefaultNamespace("chunk_generation");
    private final List<String> result = new ArrayList<String>();
    private @Nullable BlockPos lastPos = null;

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
        BlockPos feetPos = entity.blockPosition();
        if (!feetPos.equals(this.lastPos)) {
            this.update(serverChunk, feetPos, serverLevel);
        }
        displayer.addToGroup(GROUP, this.result);
    }

    private void update(@Nullable LevelChunk serverChunk, BlockPos feetPos, ServerLevel serverLevel) {
        this.result.clear();
        this.lastPos = feetPos;
        ServerChunkCache chunkSource = serverLevel.getChunkSource();
        ChunkGenerator generator = chunkSource.getGenerator();
        RandomState randomState = chunkSource.randomState();
        generator.addDebugScreenInfo(this.result, randomState, feetPos);
        Climate.Sampler sampler = randomState.sampler();
        BiomeSource biomeSource = generator.getBiomeSource();
        biomeSource.addDebugInfo(this.result, feetPos, sampler);
        if (serverChunk != null && serverChunk.isOldNoiseGeneration()) {
            this.result.add("Blending: Old");
        }
    }
}

