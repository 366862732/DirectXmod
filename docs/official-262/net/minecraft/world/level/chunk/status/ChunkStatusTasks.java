/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk.status;

import com.mojang.logging.LogUtils;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;

public class ChunkStatusTasks {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean isLighted(ChunkAccess chunk) {
        return chunk.getPersistedStatus().isOrAfter(ChunkStatus.LIGHT) && chunk.isLightCorrect();
    }

    public static CompletableFuture<ChunkAccess> passThrough(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> generateStructureStarts(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        if (level.getServer().getWorldGenSettings().options().generateStructures()) {
            context.generator().createStructures(level.registryAccess(), level.getChunkSource().getGeneratorState(), level.structureManager(), chunk, context.structureManager(), level.dimension());
        }
        level.onStructureStartsAvailable(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> loadStructureStarts(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk) {
        context.level().onStructureStartsAvailable(chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> generateStructureReferences(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        WorldGenRegion region = new WorldGenRegion(level, chunks, step, chunk);
        context.generator().createReferences(region, level.structureManager().forWorldGenRegion(region), chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> generateBiomes(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        WorldGenRegion region = new WorldGenRegion(level, chunks, step, chunk);
        return context.generator().createBiomes(level.getChunkSource().randomState(), Blender.of(region), level.structureManager().forWorldGenRegion(region), chunk);
    }

    public static CompletableFuture<ChunkAccess> generateNoise(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        WorldGenRegion region = new WorldGenRegion(level, chunks, step, chunk);
        return context.generator().fillFromNoise(Blender.of(region), level.getChunkSource().randomState(), level.structureManager().forWorldGenRegion(region), chunk).thenApply(generatedChunk -> {
            ProtoChunk protoChunk;
            BelowZeroRetrogen belowZeroRetrogen;
            if (generatedChunk instanceof ProtoChunk && (belowZeroRetrogen = (protoChunk = (ProtoChunk)generatedChunk).getBelowZeroRetrogen()) != null) {
                BelowZeroRetrogen.replaceOldBedrock(protoChunk);
                if (belowZeroRetrogen.hasBedrockHoles()) {
                    belowZeroRetrogen.applyBedrockMask(protoChunk);
                }
            }
            return generatedChunk;
        });
    }

    public static CompletableFuture<ChunkAccess> generateSurface(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        WorldGenRegion region = new WorldGenRegion(level, chunks, step, chunk);
        context.generator().buildSurface(region, level.structureManager().forWorldGenRegion(region), level.getChunkSource().randomState(), chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> generateCarvers(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        WorldGenRegion region = new WorldGenRegion(level, chunks, step, chunk);
        if (chunk instanceof ProtoChunk) {
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            Blender.addAroundOldChunksCarvingMaskFilter(region, protoChunk);
        }
        context.generator().applyCarvers(region, level.getSeed(), level.getChunkSource().randomState(), level.getBiomeManager(), level.structureManager().forWorldGenRegion(region), chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> generateFeatures(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ServerLevel level = context.level();
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
        WorldGenRegion region = new WorldGenRegion(level, chunks, step, chunk);
        if (!SharedConstants.DEBUG_DISABLE_FEATURES) {
            context.generator().applyBiomeDecoration(region, chunk, level.structureManager().forWorldGenRegion(region));
        }
        Blender.generateBorderTicks(region, chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> initializeLight(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ThreadedLevelLightEngine lightEngine = context.lightEngine();
        chunk.initializeLightSources();
        ((ProtoChunk)chunk).setLightEngine(lightEngine);
        boolean lighted = ChunkStatusTasks.isLighted(chunk);
        return lightEngine.initializeLight(chunk, lighted);
    }

    public static CompletableFuture<ChunkAccess> light(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        boolean lighted = ChunkStatusTasks.isLighted(chunk);
        return context.lightEngine().lightChunk(chunk, lighted);
    }

    public static CompletableFuture<ChunkAccess> generateSpawn(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        if (!chunk.isUpgrading()) {
            context.generator().spawnOriginalMobs(new WorldGenRegion(context.level(), chunks, step, chunk));
        }
        return CompletableFuture.completedFuture(chunk);
    }

    public static CompletableFuture<ChunkAccess> full(WorldGenContext context, ChunkStep step, StaticCache2D<GenerationChunkHolder> chunks, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        GenerationChunkHolder holder = chunks.get(pos.x(), pos.z());
        return CompletableFuture.supplyAsync(() -> {
            LevelChunk levelChunk;
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            ServerLevel level = context.level();
            if (protoChunk instanceof ImposterProtoChunk) {
                ImposterProtoChunk imposter = (ImposterProtoChunk)protoChunk;
                levelChunk = imposter.getWrapped();
            } else {
                levelChunk = new LevelChunk(level, protoChunk, lc -> {
                    try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(chunk.problemPath(), LOGGER);){
                        ChunkStatusTasks.postLoadProtoChunk(level, TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)level.registryAccess(), protoChunk.getEntities()));
                    }
                });
                holder.replaceProtoChunk(new ImposterProtoChunk(levelChunk, false));
            }
            levelChunk.setFullStatus(holder::getFullStatus);
            levelChunk.runPostLoad();
            levelChunk.setLoaded(true);
            levelChunk.registerAllBlockEntitiesAfterLevelLoad();
            levelChunk.registerTickContainerInLevel(level);
            levelChunk.setUnsavedListener(context.unsavedListener());
            return levelChunk;
        }, context.mainThreadExecutor());
    }

    private static void postLoadProtoChunk(ServerLevel level, ValueInput.ValueInputList entities) {
        if (!entities.isEmpty()) {
            level.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(entities, level, EntitySpawnReason.LOAD));
        }
    }
}

