/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BaseSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPAWNER_ENTITY_ID = -1;
    public static final EntityProcessor SET_DISPLAY_ENTITY_ID = e -> {
        e.setId(-1);
        return e;
    };
    public static final String SPAWN_DATA_TAG = "SpawnData";
    private static final int EVENT_SPAWN = 1;
    private static final int DEFAULT_SPAWN_DELAY = 20;
    private static final int DEFAULT_MIN_SPAWN_DELAY = 200;
    private static final int DEFAULT_MAX_SPAWN_DELAY = 800;
    private static final int DEFAULT_SPAWN_COUNT = 4;
    private static final int DEFAULT_MAX_NEARBY_ENTITIES = 6;
    private static final int DEFAULT_REQUIRED_PLAYER_RANGE = 16;
    private static final int DEFAULT_SPAWN_RANGE = 4;
    private int spawnDelay = 20;
    private WeightedList<SpawnData> spawnPotentials = WeightedList.of();
    private @Nullable SpawnData nextSpawnData;
    private double spin;
    private double oSpin;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    private @Nullable Entity displayEntity;
    private int maxNearbyEntities = 6;
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;

    public void setEntityId(EntityType<?> type, @Nullable Level level, RandomSource random, BlockPos pos) {
        this.getOrCreateNextSpawnData(level, random, pos).getEntityToSpawn().putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    private boolean isNearPlayer(Level level, BlockPos pos) {
        return level.hasNearbyAlivePlayer((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, this.requiredPlayerRange);
    }

    public void clientTick(Level level, BlockPos pos) {
        if (!this.isNearPlayer(level, pos)) {
            this.oSpin = this.spin;
        } else if (this.displayEntity != null) {
            RandomSource random = level.getRandom();
            double xP = (double)pos.getX() + random.nextDouble();
            double yP = (double)pos.getY() + random.nextDouble();
            double zP = (double)pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, xP, yP, zP, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, xP, yP, zP, 0.0, 0.0, 0.0);
            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            }
            this.oSpin = this.spin;
            this.spin = (this.spin + (double)(1000.0f / ((float)this.spawnDelay + 200.0f))) % 360.0;
        }
    }

    public void serverTick(ServerLevel level, BlockPos pos) {
        if (!this.isNearPlayer(level, pos) || !level.isSpawnerBlockEnabled()) {
            return;
        }
        if (this.spawnDelay == -1) {
            this.delay(level, pos);
        }
        if (this.spawnDelay > 0) {
            --this.spawnDelay;
            return;
        }
        boolean delay = false;
        RandomSource random = level.getRandom();
        SpawnData nextSpawnData = this.getOrCreateNextSpawnData(level, random, pos);
        for (int c = 0; c < this.spawnCount; ++c) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this::toString, LOGGER);){
                Mob mob;
                ValueInput input = TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)level.registryAccess(), nextSpawnData.getEntityToSpawn());
                Optional<EntityType<?>> entityType = EntityType.by(input);
                if (entityType.isEmpty()) {
                    this.delay(level, pos);
                    return;
                }
                Vec3 spawnPos = input.read("Pos", Vec3.CODEC).orElseGet(() -> new Vec3((double)pos.getX() + (random.nextDouble() - random.nextDouble()) * (double)this.spawnRange + 0.5, pos.getY() + random.nextInt(3) - 1, (double)pos.getZ() + (random.nextDouble() - random.nextDouble()) * (double)this.spawnRange + 0.5));
                if (!level.noCollision(entityType.get().getSpawnAABB(spawnPos.x, spawnPos.y, spawnPos.z))) continue;
                BlockPos spawnBlockPos = BlockPos.containing(spawnPos);
                if (nextSpawnData.getCustomSpawnRules().isPresent()) {
                    SpawnData.CustomSpawnRules customSpawnRules;
                    if (!entityType.get().getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL || !(customSpawnRules = nextSpawnData.getCustomSpawnRules().get()).isValidPosition(spawnBlockPos, level)) continue;
                } else if (!SpawnPlacements.checkSpawnRules(entityType.get(), level, EntitySpawnReason.SPAWNER, spawnBlockPos, level.getRandom())) continue;
                Entity entity = EntityType.loadEntityRecursive(input, (Level)level, EntitySpawnReason.SPAWNER, e -> {
                    e.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, e.getYRot(), e.getXRot());
                    return e;
                });
                if (entity == null) {
                    this.delay(level, pos);
                    return;
                }
                int nearBy = level.getEntities(EntityTypeTest.forExactClass(entity.getClass()), new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).inflate(this.spawnRange), EntitySelector.NO_SPECTATORS).size();
                if (nearBy >= this.maxNearbyEntities) {
                    this.delay(level, pos);
                    return;
                }
                entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0f, 0.0f);
                if (entity instanceof Mob) {
                    boolean hasNoConfiguration;
                    mob = (Mob)entity;
                    if (nextSpawnData.getCustomSpawnRules().isEmpty() && !mob.checkSpawnRules(level, EntitySpawnReason.SPAWNER) || !mob.checkSpawnObstruction(level)) continue;
                    boolean bl = hasNoConfiguration = nextSpawnData.getEntityToSpawn().size() == 1 && nextSpawnData.getEntityToSpawn().getString("id").isPresent();
                    if (hasNoConfiguration) {
                        ((Mob)entity).finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null);
                    }
                    nextSpawnData.getEquipment().ifPresent(mob::equip);
                }
                if (!level.tryAddFreshEntityWithPassengers(entity)) {
                    this.delay(level, pos);
                    return;
                }
                level.levelEvent(2004, pos, 0);
                level.gameEvent(entity, GameEvent.ENTITY_PLACE, spawnBlockPos);
                if (entity instanceof Mob) {
                    mob = (Mob)entity;
                    mob.spawnAnim();
                }
                delay = true;
                continue;
            }
        }
        if (delay) {
            this.delay(level, pos);
        }
    }

    private void delay(Level level, BlockPos pos) {
        RandomSource random = level.random;
        this.spawnDelay = this.maxSpawnDelay <= this.minSpawnDelay ? this.minSpawnDelay : this.minSpawnDelay + random.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        this.spawnPotentials.getRandom(random).ifPresent(entry -> this.setNextSpawnData(level, pos, (SpawnData)entry));
        this.broadcastEvent(level, pos, 1);
    }

    public void load(@Nullable Level level, BlockPos pos, ValueInput input) {
        this.spawnDelay = input.getShortOr("Delay", (short)20);
        input.read(SPAWN_DATA_TAG, SpawnData.CODEC).ifPresent(nextSpawnData -> this.setNextSpawnData(level, pos, (SpawnData)nextSpawnData));
        this.spawnPotentials = input.read("SpawnPotentials", SpawnData.LIST_CODEC).orElseGet(() -> WeightedList.of(this.nextSpawnData != null ? this.nextSpawnData : new SpawnData()));
        this.minSpawnDelay = input.getIntOr("MinSpawnDelay", 200);
        this.maxSpawnDelay = input.getIntOr("MaxSpawnDelay", 800);
        this.spawnCount = input.getIntOr("SpawnCount", 4);
        this.maxNearbyEntities = input.getIntOr("MaxNearbyEntities", 6);
        this.requiredPlayerRange = input.getIntOr("RequiredPlayerRange", 16);
        this.spawnRange = input.getIntOr("SpawnRange", 4);
        this.displayEntity = null;
    }

    public void save(ValueOutput output) {
        output.putShort("Delay", (short)this.spawnDelay);
        output.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        output.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        output.putShort("SpawnCount", (short)this.spawnCount);
        output.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        output.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        output.putShort("SpawnRange", (short)this.spawnRange);
        output.storeNullable(SPAWN_DATA_TAG, SpawnData.CODEC, this.nextSpawnData);
        output.store("SpawnPotentials", SpawnData.LIST_CODEC, this.spawnPotentials);
    }

    public @Nullable Entity getOrCreateDisplayEntity(Level level, BlockPos pos) {
        if (this.displayEntity == null) {
            CompoundTag entityToSpawn = this.getOrCreateNextSpawnData(level, level.getRandom(), pos).getEntityToSpawn();
            if (entityToSpawn.getString("id").isEmpty()) {
                return null;
            }
            this.displayEntity = EntityType.loadEntityRecursive(entityToSpawn, level, new EntitySpawnRequest(EntitySpawnReason.SPAWNER, true), SET_DISPLAY_ENTITY_ID);
            if (entityToSpawn.size() != 1 || this.displayEntity instanceof Mob) {
                // empty if block
            }
        }
        return this.displayEntity;
    }

    public boolean onEventTriggered(Level level, int id) {
        if (id == 1) {
            if (level.isClientSide()) {
                this.spawnDelay = this.minSpawnDelay;
            }
            return true;
        }
        return false;
    }

    protected void setNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData nextSpawnData) {
        this.nextSpawnData = nextSpawnData;
    }

    private SpawnData getOrCreateNextSpawnData(@Nullable Level level, RandomSource random, BlockPos pos) {
        if (this.nextSpawnData != null) {
            return this.nextSpawnData;
        }
        this.setNextSpawnData(level, pos, this.spawnPotentials.getRandom(random).orElseGet(SpawnData::new));
        return this.nextSpawnData;
    }

    public abstract void broadcastEvent(Level var1, BlockPos var2, int var3);

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }
}

