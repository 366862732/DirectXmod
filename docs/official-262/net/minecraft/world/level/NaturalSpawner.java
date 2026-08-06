/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.PotentialCalculator;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class NaturalSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MIN_SPAWN_DISTANCE = 24;
    public static final int SPAWN_DISTANCE_CHUNK = 8;
    public static final int SPAWN_DISTANCE_BLOCK = 128;
    public static final int INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK = Mth.floor(8.0f / Mth.SQRT_OF_TWO);
    private static final int MAGIC_NUMBER = (int)Math.pow(17.0, 2.0);
    private static final MobCategory[] SPAWNING_CATEGORIES = (MobCategory[])Stream.of(MobCategory.values()).filter(c -> c != MobCategory.MISC).toArray(MobCategory[]::new);

    private NaturalSpawner() {
    }

    public static SpawnState createState(int spawnableChunkCount, Iterable<Entity> entities, ChunkGetter chunkGetter, LocalMobCapCalculator localMobCapCalculator) {
        PotentialCalculator spawnPotential = new PotentialCalculator();
        Object2IntOpenHashMap mobCounts = new Object2IntOpenHashMap();
        for (Entity entity : entities) {
            MobCategory category;
            Mob mob;
            if (entity instanceof Mob && ((mob = (Mob)entity).isPersistenceRequired() || mob.requiresCustomPersistence()) || (category = entity.getType().getCategory()) == MobCategory.MISC) continue;
            BlockPos pos = entity.blockPosition();
            chunkGetter.query(ChunkPos.pack(pos), chunk -> {
                MobSpawnSettings.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(pos, chunk).getMobSettings().getMobSpawnCost(entity.getType());
                if (mobSpawnCost != null) {
                    spawnPotential.addCharge(entity.blockPosition(), mobSpawnCost.charge());
                }
                if (entity instanceof Mob) {
                    localMobCapCalculator.addMob(chunk.getPos(), category);
                }
                mobCounts.addTo((Object)category, 1);
            });
        }
        return new SpawnState(spawnableChunkCount, (Object2IntOpenHashMap<MobCategory>)mobCounts, spawnPotential, localMobCapCalculator);
    }

    private static Biome getRoughBiome(BlockPos pos, ChunkAccess chunk) {
        return chunk.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())).value();
    }

    public static List<MobCategory> getFilteredSpawningCategories(SpawnState state, boolean spawnEnemies, boolean spawnPersistent) {
        ArrayList<MobCategory> spawningCategories = new ArrayList<MobCategory>(SPAWNING_CATEGORIES.length);
        for (MobCategory mobCategory : SPAWNING_CATEGORIES) {
            if (!spawnEnemies && !mobCategory.isFriendly() || !spawnPersistent && mobCategory.isPersistent() || !state.canSpawnForCategoryGlobal(mobCategory)) continue;
            spawningCategories.add(mobCategory);
        }
        return spawningCategories;
    }

    public static void spawnForChunk(ServerLevel level, LevelChunk chunk, SpawnState state, List<MobCategory> spawningCategories) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("spawner");
        for (MobCategory mobCategory : spawningCategories) {
            if (!state.canSpawnForCategoryLocal(mobCategory, chunk.getPos())) continue;
            NaturalSpawner.spawnCategoryForChunk(mobCategory, level, chunk, state::canSpawn, state::afterSpawn);
        }
        profiler.pop();
    }

    public static void spawnCategoryForChunk(MobCategory mobCategory, ServerLevel level, LevelChunk chunk, SpawnPredicate extraTest, AfterSpawnCallback spawnCallback) {
        BlockPos start = NaturalSpawner.getRandomPosWithin(level, chunk);
        if (start.getY() < level.getMinY() + 1) {
            return;
        }
        NaturalSpawner.spawnCategoryForPosition(mobCategory, level, chunk, start, extraTest, spawnCallback);
    }

    @VisibleForDebug
    public static void spawnCategoryForPosition(MobCategory mobCategory, ServerLevel level, BlockPos start) {
        NaturalSpawner.spawnCategoryForPosition(mobCategory, level, level.getChunk(start), start, (type, chunk, pos) -> true, (mob, chunk) -> {});
    }

    public static void spawnCategoryForPosition(MobCategory mobCategory, ServerLevel level, ChunkAccess chunk, BlockPos start, SpawnPredicate extraTest, AfterSpawnCallback spawnCallback) {
        StructureManager structureManager = level.structureManager();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        int yStart = start.getY();
        BlockState state = chunk.getBlockState(start);
        if (state.isRedstoneConductor(chunk, start)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int clusterSize = 0;
        block0: for (int groupCount = 0; groupCount < 3; ++groupCount) {
            int x = start.getX();
            int z = start.getZ();
            int ss = 6;
            MobSpawnSettings.SpawnerData currentSpawnData = null;
            SpawnGroupData groupData = null;
            int max = Mth.ceil(level.random.nextFloat() * 4.0f);
            int groupSize = 0;
            for (int ll = 0; ll < max; ++ll) {
                double nearestPlayerDistanceSqr;
                pos.set(x += level.random.nextInt(6) - level.random.nextInt(6), yStart, z += level.random.nextInt(6) - level.random.nextInt(6));
                double xx = (double)x + 0.5;
                double zz = (double)z + 0.5;
                Player nearestPlayer = level.getNearestPlayer(xx, (double)yStart, zz, -1.0, false);
                if (nearestPlayer == null || !NaturalSpawner.isRightDistanceToPlayerAndSpawnPoint(level, chunk, pos, nearestPlayerDistanceSqr = nearestPlayer.distanceToSqr(xx, yStart, zz))) continue;
                if (currentSpawnData == null) {
                    Optional<MobSpawnSettings.SpawnerData> nextSpawnData = NaturalSpawner.getRandomSpawnMobAt(level, structureManager, generator, mobCategory, level.random, pos);
                    if (nextSpawnData.isEmpty()) continue block0;
                    currentSpawnData = nextSpawnData.get();
                    max = currentSpawnData.minCount() + level.random.nextInt(1 + currentSpawnData.maxCount() - currentSpawnData.minCount());
                }
                if (!NaturalSpawner.isValidSpawnPostitionForType(level, mobCategory, structureManager, generator, currentSpawnData, pos, nearestPlayerDistanceSqr) || !extraTest.test(currentSpawnData.type(), pos, chunk)) continue;
                Mob mob = NaturalSpawner.getMobForSpawn(level, currentSpawnData.type());
                if (mob == null) {
                    return;
                }
                mob.snapTo(xx, yStart, zz, level.random.nextFloat() * 360.0f, 0.0f);
                if (!NaturalSpawner.isValidPositionForMob(level, mob, nearestPlayerDistanceSqr)) continue;
                groupData = mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, groupData);
                ++groupSize;
                level.addFreshEntityWithPassengers(mob);
                spawnCallback.run(mob, chunk);
                if (++clusterSize >= mob.getMaxSpawnClusterSize()) {
                    return;
                }
                if (mob.isMaxGroupSizeReached(groupSize)) continue block0;
            }
        }
    }

    private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double nearestPlayerDistanceSqr) {
        if (nearestPlayerDistanceSqr <= 576.0) {
            return false;
        }
        LevelData.RespawnData respawnData = level.getRespawnData();
        if (respawnData.dimension() == level.dimension() && respawnData.pos().closerToCenterThan(new Vec3((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5), 24.0)) {
            return false;
        }
        ChunkPos chunkPos = ChunkPos.containing(pos);
        return Objects.equals(chunkPos, chunk.getPos()) || level.canSpawnEntitiesInChunk(chunkPos);
    }

    private static boolean isValidSpawnPostitionForType(ServerLevel level, MobCategory mobCategory, StructureManager structureManager, ChunkGenerator generator, MobSpawnSettings.SpawnerData currentSpawnData, BlockPos.MutableBlockPos pos, double nearestPlayerDistanceSqr) {
        EntityType<?> type = currentSpawnData.type();
        if (type.getCategory() == MobCategory.MISC) {
            return false;
        }
        if (!type.canSpawnFarFromPlayer() && nearestPlayerDistanceSqr > (double)(type.getCategory().getDespawnDistance() * type.getCategory().getDespawnDistance())) {
            return false;
        }
        if (!type.canSummon() || !NaturalSpawner.canSpawnMobAt(level, structureManager, generator, mobCategory, currentSpawnData, pos)) {
            return false;
        }
        if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) {
            return false;
        }
        if (!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, level.random)) {
            return false;
        }
        return level.noCollision(type.getSpawnAABB((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5));
    }

    private static @Nullable Mob getMobForSpawn(ServerLevel level, EntityType<?> type) {
        try {
            Object obj = type.create((Level)level, EntitySpawnReason.NATURAL);
            if (obj instanceof Mob) {
                Mob mob = (Mob)obj;
                return mob;
            }
            LOGGER.warn("Can't spawn entity of type: {}", (Object)BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to create mob", (Throwable)e);
        }
        return null;
    }

    private static boolean isValidPositionForMob(ServerLevel level, Mob mob, double nearestPlayerDistanceSqr) {
        if (nearestPlayerDistanceSqr > (double)(mob.getType().getCategory().getDespawnDistance() * mob.getType().getCategory().getDespawnDistance()) && mob.removeWhenFarAway(nearestPlayerDistanceSqr)) {
            return false;
        }
        return mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) && mob.checkSpawnObstruction(level);
    }

    private static Optional<MobSpawnSettings.SpawnerData> getRandomSpawnMobAt(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory mobCategory, RandomSource random, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        if (mobCategory == MobCategory.WATER_AMBIENT && biome.is(BiomeTags.REDUCED_WATER_AMBIENT_SPAWNS) && random.nextFloat() < 0.98f) {
            return Optional.empty();
        }
        return NaturalSpawner.mobsAt(level, structureManager, generator, mobCategory, pos, biome).getRandom(random);
    }

    private static boolean canSpawnMobAt(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory mobCategory, MobSpawnSettings.SpawnerData spawnerData, BlockPos pos) {
        return NaturalSpawner.mobsAt(level, structureManager, generator, mobCategory, pos, null).contains(spawnerData);
    }

    private static WeightedList<MobSpawnSettings.SpawnerData> mobsAt(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory mobCategory, BlockPos pos, @Nullable Holder<Biome> biome) {
        if (NaturalSpawner.isInNetherFortressBounds(pos, level, mobCategory, structureManager)) {
            return NetherFortressStructure.FORTRESS_ENEMIES;
        }
        return generator.getMobsAt(biome != null ? biome : level.getBiome(pos), structureManager, mobCategory, pos);
    }

    public static boolean isInNetherFortressBounds(BlockPos pos, ServerLevel level, MobCategory category, StructureManager structureManager) {
        if (category != MobCategory.MONSTER || !level.getBlockState(pos.below()).is(Blocks.NETHER_BRICKS)) {
            return false;
        }
        Structure fortress = structureManager.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(BuiltinStructures.FORTRESS);
        if (fortress == null) {
            return false;
        }
        return structureManager.getStructureAt(pos, fortress).isValid();
    }

    private static BlockPos getRandomPosWithin(Level level, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int x = pos.getMinBlockX() + level.random.nextInt(16);
        int z = pos.getMinBlockZ() + level.random.nextInt(16);
        int topEmptyY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        int y = Mth.randomBetweenInclusive(level.random, level.getMinY(), topEmptyY);
        return new BlockPos(x, y, z);
    }

    public static boolean isValidEmptySpawnBlock(BlockGetter level, BlockPos pos, BlockState blockState, FluidState fluidState, EntityType<?> type) {
        if (blockState.isCollisionShapeFullBlock(level, pos)) {
            return false;
        }
        if (blockState.isSignalSource()) {
            return false;
        }
        if (!fluidState.isEmpty()) {
            return false;
        }
        if (blockState.is(BlockTags.PREVENT_MOB_SPAWNING_INSIDE)) {
            return false;
        }
        return !type.isBlockDangerous(blockState);
    }

    public static void spawnMobsForChunkGeneration(ServerLevelAccessor level, Holder<Biome> biome, ChunkPos chunkPos, RandomSource random) {
        MobSpawnSettings mobSettings = biome.value().getMobSettings();
        WeightedList<MobSpawnSettings.SpawnerData> mobs = mobSettings.getMobs(MobCategory.CREATURE);
        if (mobs.isEmpty() || !level.getLevel().getGameRules().get(GameRules.SPAWN_MOBS).booleanValue()) {
            return;
        }
        int xo = chunkPos.getMinBlockX();
        int zo = chunkPos.getMinBlockZ();
        while (random.nextFloat() < mobSettings.getCreatureProbability()) {
            Optional<MobSpawnSettings.SpawnerData> nextSpawnerData = mobs.getRandom(random);
            if (nextSpawnerData.isEmpty()) continue;
            MobSpawnSettings.SpawnerData spawnerData = nextSpawnerData.get();
            int count = spawnerData.minCount() + random.nextInt(1 + spawnerData.maxCount() - spawnerData.minCount());
            SpawnGroupData groupSpawnData = null;
            int x = xo + random.nextInt(16);
            int z = zo + random.nextInt(16);
            int startX = x;
            int startZ = z;
            for (int i = 0; i < count; ++i) {
                boolean success = false;
                for (int attempts = 0; !success && attempts < 4; ++attempts) {
                    BlockPos pos = NaturalSpawner.getTopNonCollidingPos(level, spawnerData.type(), x, z);
                    if (spawnerData.type().canSummon() && SpawnPlacements.isSpawnPositionOk(spawnerData.type(), level, pos)) {
                        Mob mob;
                        Object entity;
                        float width = spawnerData.type().getWidth();
                        double fx = Mth.clamp((double)x, (double)xo + (double)width, (double)xo + 16.0 - (double)width);
                        double fz = Mth.clamp((double)z, (double)zo + (double)width, (double)zo + 16.0 - (double)width);
                        if (!level.noCollision(spawnerData.type().getSpawnAABB(fx, pos.getY(), fz)) || !SpawnPlacements.checkSpawnRules(spawnerData.type(), level, EntitySpawnReason.CHUNK_GENERATION, BlockPos.containing(fx, pos.getY(), fz), level.getRandom())) continue;
                        try {
                            entity = spawnerData.type().create((Level)level.getLevel(), EntitySpawnReason.NATURAL);
                        }
                        catch (Exception e) {
                            LOGGER.warn("Failed to create mob", (Throwable)e);
                            continue;
                        }
                        if (entity == null) continue;
                        ((Entity)entity).snapTo(fx, pos.getY(), fz, random.nextFloat() * 360.0f, 0.0f);
                        if (entity instanceof Mob && (mob = (Mob)entity).checkSpawnRules(level, EntitySpawnReason.CHUNK_GENERATION) && mob.checkSpawnObstruction(level)) {
                            groupSpawnData = mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.CHUNK_GENERATION, groupSpawnData);
                            level.addFreshEntityWithPassengers(mob);
                            success = true;
                        }
                    }
                    x += random.nextInt(5) - random.nextInt(5);
                    z += random.nextInt(5) - random.nextInt(5);
                    while (x < xo || x >= xo + 16 || z < zo || z >= zo + 16) {
                        x = startX + random.nextInt(5) - random.nextInt(5);
                        z = startZ + random.nextInt(5) - random.nextInt(5);
                    }
                }
            }
        }
    }

    private static BlockPos getTopNonCollidingPos(LevelReader level, EntityType<?> type, int x, int z) {
        int levelHeight = level.getHeight(SpawnPlacements.getHeightmapType(type), x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, levelHeight, z);
        if (level.dimensionType().hasCeiling()) {
            do {
                pos.move(Direction.DOWN);
            } while (!level.getBlockState(pos).isAir());
            do {
                pos.move(Direction.DOWN);
            } while (level.getBlockState(pos).isAir() && pos.getY() > level.getMinY());
        }
        return SpawnPlacements.getPlacementType(type).adjustSpawnPosition(level, pos.immutable());
    }

    @FunctionalInterface
    public static interface ChunkGetter {
        public void query(long var1, Consumer<LevelChunk> var3);
    }

    public static class SpawnState {
        private final int spawnableChunkCount;
        private final Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
        private final PotentialCalculator spawnPotential;
        private final Object2IntMap<MobCategory> unmodifiableMobCategoryCounts;
        private final LocalMobCapCalculator localMobCapCalculator;
        private @Nullable BlockPos lastCheckedPos;
        private @Nullable EntityType<?> lastCheckedType;
        private double lastCharge;

        private SpawnState(int spawnableChunkCount, Object2IntOpenHashMap<MobCategory> mobCategoryCounts, PotentialCalculator spawnPotential, LocalMobCapCalculator localMobCapCalculator) {
            this.spawnableChunkCount = spawnableChunkCount;
            this.mobCategoryCounts = mobCategoryCounts;
            this.spawnPotential = spawnPotential;
            this.localMobCapCalculator = localMobCapCalculator;
            this.unmodifiableMobCategoryCounts = Object2IntMaps.unmodifiable(mobCategoryCounts);
        }

        private boolean canSpawn(EntityType<?> type, BlockPos testPos, ChunkAccess chunk) {
            double charge;
            this.lastCheckedPos = testPos;
            this.lastCheckedType = type;
            MobSpawnSettings.MobSpawnCost mobSpawnCost = NaturalSpawner.getRoughBiome(testPos, chunk).getMobSettings().getMobSpawnCost(type);
            if (mobSpawnCost == null) {
                this.lastCharge = 0.0;
                return true;
            }
            this.lastCharge = charge = mobSpawnCost.charge();
            double energyChange = this.spawnPotential.getPotentialEnergyChange(testPos, charge);
            return energyChange <= mobSpawnCost.energyBudget();
        }

        private void afterSpawn(Mob mob, ChunkAccess chunk) {
            MobSpawnSettings.MobSpawnCost mobSpawnCost;
            EntityType<?> type = mob.getType();
            BlockPos pos = mob.blockPosition();
            double charge = pos.equals(this.lastCheckedPos) && type == this.lastCheckedType ? this.lastCharge : ((mobSpawnCost = NaturalSpawner.getRoughBiome(pos, chunk).getMobSettings().getMobSpawnCost(type)) != null ? mobSpawnCost.charge() : 0.0);
            this.spawnPotential.addCharge(pos, charge);
            MobCategory category = type.getCategory();
            this.mobCategoryCounts.addTo((Object)category, 1);
            this.localMobCapCalculator.addMob(ChunkPos.containing(pos), category);
        }

        public int getSpawnableChunkCount() {
            return this.spawnableChunkCount;
        }

        public Object2IntMap<MobCategory> getMobCategoryCounts() {
            return this.unmodifiableMobCategoryCounts;
        }

        private boolean canSpawnForCategoryGlobal(MobCategory mobCategory) {
            int maxMobCount = mobCategory.getMaxInstancesPerChunk() * this.spawnableChunkCount / MAGIC_NUMBER;
            return this.mobCategoryCounts.getInt((Object)mobCategory) < maxMobCount;
        }

        private boolean canSpawnForCategoryLocal(MobCategory mobCategory, ChunkPos chunkPos) {
            return this.localMobCapCalculator.canSpawn(mobCategory, chunkPos) || SharedConstants.DEBUG_IGNORE_LOCAL_MOB_CAP;
        }
    }

    @FunctionalInterface
    public static interface SpawnPredicate {
        public boolean test(EntityType<?> var1, BlockPos var2, ChunkAccess var3);
    }

    @FunctionalInterface
    public static interface AfterSpawnCallback {
        public void run(Mob var1, ChunkAccess var2);
    }
}

