/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;

public final class TrialSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
    private static final int DEFAULT_TARGET_COOLDOWN_LENGTH = 36000;
    private static final int DEFAULT_PLAYER_SCAN_RANGE = 14;
    private static final int MAX_MOB_TRACKING_DISTANCE = 47;
    private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
    private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02f;
    private final TrialSpawnerStateData data = new TrialSpawnerStateData();
    private FullConfig config;
    private final StateAccessor stateAccessor;
    private PlayerDetector playerDetector;
    private final PlayerDetector.EntitySelector entitySelector;
    private boolean overridePeacefulAndMobSpawnRule;
    private boolean isOminous;

    public TrialSpawner(FullConfig config, StateAccessor stateAccessor, PlayerDetector playerDetector, PlayerDetector.EntitySelector entitySelector) {
        this.config = config;
        this.stateAccessor = stateAccessor;
        this.playerDetector = playerDetector;
        this.entitySelector = entitySelector;
    }

    public TrialSpawnerConfig activeConfig() {
        return this.isOminous ? this.config.ominous().value() : this.config.normal.value();
    }

    public TrialSpawnerConfig normalConfig() {
        return this.config.normal.value();
    }

    public TrialSpawnerConfig ominousConfig() {
        return this.config.ominous.value();
    }

    public void load(ValueInput input) {
        input.read(TrialSpawnerStateData.Packed.MAP_CODEC).ifPresent(this.data::apply);
        this.config = input.read(FullConfig.MAP_CODEC).orElse(FullConfig.DEFAULT);
    }

    public void store(ValueOutput output) {
        output.store(TrialSpawnerStateData.Packed.MAP_CODEC, this.data.pack());
        output.store(FullConfig.MAP_CODEC, this.config);
    }

    public void applyOminous(ServerLevel level, BlockPos spawnerPos) {
        level.setBlock(spawnerPos, (BlockState)level.getBlockState(spawnerPos).setValue(TrialSpawnerBlock.OMINOUS, true), 3);
        level.levelEvent(3020, spawnerPos, 1);
        this.isOminous = true;
        this.data.resetAfterBecomingOminous(this, level);
    }

    public void removeOminous(ServerLevel level, BlockPos spawnerPos) {
        level.setBlock(spawnerPos, (BlockState)level.getBlockState(spawnerPos).setValue(TrialSpawnerBlock.OMINOUS, false), 3);
        this.isOminous = false;
    }

    public boolean isOminous() {
        return this.isOminous;
    }

    public int getTargetCooldownLength() {
        return this.config.targetCooldownLength;
    }

    public int getRequiredPlayerRange() {
        return this.config.requiredPlayerRange;
    }

    public TrialSpawnerState getState() {
        return this.stateAccessor.getState();
    }

    public TrialSpawnerStateData getStateData() {
        return this.data;
    }

    public void setState(Level level, TrialSpawnerState state) {
        this.stateAccessor.setState(level, state);
    }

    public void markUpdated() {
        this.stateAccessor.markUpdated();
    }

    public PlayerDetector getPlayerDetector() {
        return this.playerDetector;
    }

    public PlayerDetector.EntitySelector getEntitySelector() {
        return this.entitySelector;
    }

    public boolean canSpawnInLevel(ServerLevel level) {
        if (!level.getGameRules().get(GameRules.SPAWNER_BLOCKS_WORK).booleanValue()) {
            return false;
        }
        if (this.overridePeacefulAndMobSpawnRule) {
            return true;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return level.getGameRules().get(GameRules.SPAWN_MOBS);
    }

    public Optional<UUID> spawnMob(ServerLevel level, BlockPos spawnerPos) {
        RandomSource random = level.getRandom();
        SpawnData nextSpawnData = this.data.getOrCreateNextSpawnData(this, level.getRandom());
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(() -> "spawner@" + String.valueOf(spawnerPos), LOGGER);){
            Object mob;
            SpawnData.CustomSpawnRules customSpawnRules;
            ValueInput input = TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)level.registryAccess(), nextSpawnData.entityToSpawn());
            Optional<EntityType<?>> entityType = EntityType.by(input);
            if (entityType.isEmpty()) {
                Optional<UUID> optional = Optional.empty();
                return optional;
            }
            Vec3 spawnPos = input.read("Pos", Vec3.CODEC).orElseGet(() -> {
                TrialSpawnerConfig activeConfig = this.activeConfig();
                return new Vec3((double)spawnerPos.getX() + (random.nextDouble() - random.nextDouble()) * (double)activeConfig.spawnRange() + 0.5, spawnerPos.getY() + random.nextInt(3) - 1, (double)spawnerPos.getZ() + (random.nextDouble() - random.nextDouble()) * (double)activeConfig.spawnRange() + 0.5);
            });
            if (!level.noCollision(entityType.get().getSpawnAABB(spawnPos.x, spawnPos.y, spawnPos.z))) {
                Optional<UUID> optional = Optional.empty();
                return optional;
            }
            if (!TrialSpawner.inLineOfSight(level, Vec3.atCenterOf(spawnerPos), spawnPos)) {
                Optional<UUID> optional = Optional.empty();
                return optional;
            }
            BlockPos spawnBlockPos = BlockPos.containing(spawnPos);
            if (!SpawnPlacements.checkSpawnRules(entityType.get(), level, EntitySpawnReason.TRIAL_SPAWNER, spawnBlockPos, level.getRandom())) {
                Optional<UUID> optional = Optional.empty();
                return optional;
            }
            if (nextSpawnData.getCustomSpawnRules().isPresent() && !(customSpawnRules = nextSpawnData.getCustomSpawnRules().get()).isValidPosition(spawnBlockPos, level)) {
                Optional<UUID> optional = Optional.empty();
                return optional;
            }
            Entity entity = EntityType.loadEntityRecursive(input, (Level)level, EntitySpawnReason.TRIAL_SPAWNER, e -> {
                e.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, random.nextFloat() * 360.0f, 0.0f);
                return e;
            });
            if (entity == null) {
                Optional<UUID> optional = Optional.empty();
                return optional;
            }
            if (entity instanceof Mob) {
                boolean hasNoConfiguration;
                mob = (Mob)entity;
                if (!((Mob)mob).checkSpawnObstruction(level)) {
                    Optional<UUID> optional = Optional.empty();
                    return optional;
                }
                boolean bl = hasNoConfiguration = nextSpawnData.getEntityToSpawn().size() == 1 && nextSpawnData.getEntityToSpawn().getString("id").isPresent();
                if (hasNoConfiguration) {
                    ((Mob)mob).finalizeSpawn(level, level.getCurrentDifficultyAt(((Entity)mob).blockPosition()), EntitySpawnReason.TRIAL_SPAWNER, null);
                }
                ((Mob)mob).setPersistenceRequired();
                nextSpawnData.getEquipment().ifPresent(((Mob)mob)::equip);
            }
            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                mob = Optional.empty();
                return mob;
            }
            FlameParticle flameParticle = this.isOminous ? FlameParticle.OMINOUS : FlameParticle.NORMAL;
            level.levelEvent(3011, spawnerPos, flameParticle.encode());
            level.levelEvent(3012, spawnBlockPos, flameParticle.encode());
            level.gameEvent(entity, GameEvent.ENTITY_PLACE, spawnBlockPos);
            Optional<UUID> optional = Optional.of(entity.getUUID());
            return optional;
        }
    }

    public void ejectReward(ServerLevel level, BlockPos pos, ResourceKey<LootTable> ejectingLootTable) {
        LootParams params;
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(ejectingLootTable);
        ObjectArrayList<ItemStack> lootDrops = lootTable.getRandomItems(params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY));
        if (!lootDrops.isEmpty()) {
            for (ItemStack item : lootDrops) {
                DefaultDispenseItemBehavior.spawnItem(level, item, 2, Direction.UP, Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2));
            }
            level.levelEvent(3014, pos, 0);
        }
    }

    public void tickClient(Level level, BlockPos spawnerPos, boolean isOminous) {
        RandomSource random;
        TrialSpawnerState currentState = this.getState();
        currentState.emitParticles(level, spawnerPos, isOminous);
        if (currentState.hasSpinningMob()) {
            double spawnDelay = Math.max(0L, this.data.nextMobSpawnsAt - level.getGameTime());
            this.data.oSpin = this.data.spin;
            this.data.spin = (this.data.spin + currentState.spinningMobSpeed() / (spawnDelay + 200.0)) % 360.0;
        }
        if (currentState.isCapableOfSpawning() && (random = level.getRandom()).nextFloat() <= 0.02f) {
            SoundEvent ambientSound = isOminous ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
            level.playLocalSound(spawnerPos, ambientSound, SoundSource.BLOCKS, random.nextFloat() * 0.25f + 0.75f, random.nextFloat() + 0.5f, false);
        }
    }

    public void tickServer(ServerLevel serverLevel, BlockPos spawnerPos, boolean isOminous) {
        TrialSpawnerState nextState;
        this.isOminous = isOminous;
        TrialSpawnerState currentState = this.getState();
        if (this.data.currentMobs.removeIf(id -> TrialSpawner.shouldMobBeUntracked(serverLevel, spawnerPos, id))) {
            this.data.nextMobSpawnsAt = serverLevel.getGameTime() + (long)this.activeConfig().ticksBetweenSpawn();
        }
        if ((nextState = currentState.tickAndGetNext(spawnerPos, this, serverLevel)) != currentState) {
            this.setState(serverLevel, nextState);
        }
    }

    private static boolean shouldMobBeUntracked(ServerLevel serverLevel, BlockPos spawnerPos, UUID id) {
        Entity entity = serverLevel.getEntity(id);
        return entity == null || !entity.isAlive() || !entity.level().dimension().equals(serverLevel.dimension()) || entity.blockPosition().distSqr(spawnerPos) > (double)MAX_MOB_TRACKING_DISTANCE_SQR;
    }

    private static boolean inLineOfSight(Level level, Vec3 origin, Vec3 dest) {
        BlockHitResult hitResult = level.clip(new ClipContext(dest, origin, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hitResult.getBlockPos().equals(BlockPos.containing(origin)) || hitResult.getType() == HitResult.Type.MISS;
    }

    public static void addSpawnParticles(Level level, BlockPos pos, RandomSource random, SimpleParticleType particleType) {
        for (int i = 0; i < 20; ++i) {
            double xP = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double yP = (double)pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double zP = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.SMOKE, xP, yP, zP, 0.0, 0.0, 0.0);
            level.addParticle(particleType, xP, yP, zP, 0.0, 0.0, 0.0);
        }
    }

    public static void addBecomeOminousParticles(Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 20; ++i) {
            double xP = (double)pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double yP = (double)pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double zP = (double)pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double xa = random.nextGaussian() * 0.02;
            double ya = random.nextGaussian() * 0.02;
            double za = random.nextGaussian() * 0.02;
            level.addParticle(ParticleTypes.TRIAL_OMEN, xP, yP, zP, xa, ya, za);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, xP, yP, zP, xa, ya, za);
        }
    }

    public static void addDetectPlayerParticles(Level level, BlockPos pos, RandomSource random, int data, ParticleOptions type) {
        for (int i = 0; i < 30 + Math.min(data, 10) * 5; ++i) {
            double spreadX = (double)(2.0f * random.nextFloat() - 1.0f) * 0.65;
            double spreadZ = (double)(2.0f * random.nextFloat() - 1.0f) * 0.65;
            double xP = (double)pos.getX() + 0.5 + spreadX;
            double yP = (double)pos.getY() + 0.1 + (double)random.nextFloat() * 0.8;
            double zP = (double)pos.getZ() + 0.5 + spreadZ;
            level.addParticle(type, xP, yP, zP, 0.0, 0.0, 0.0);
        }
    }

    public static void addEjectItemParticles(Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 20; ++i) {
            double xp = (double)pos.getX() + 0.4 + random.nextDouble() * 0.2;
            double yp = (double)pos.getY() + 0.4 + random.nextDouble() * 0.2;
            double zp = (double)pos.getZ() + 0.4 + random.nextDouble() * 0.2;
            double xa = random.nextGaussian() * 0.02;
            double ya = random.nextGaussian() * 0.02;
            double za = random.nextGaussian() * 0.02;
            level.addParticle(ParticleTypes.SMALL_FLAME, xp, yp, zp, xa, ya, za * 0.25);
            level.addParticle(ParticleTypes.SMOKE, xp, yp, zp, xa, ya, za);
        }
    }

    public void overrideEntityToSpawn(EntityType<?> type, Level level) {
        this.data.reset();
        this.config = this.config.overrideEntity(type);
        this.setState(level, TrialSpawnerState.INACTIVE);
    }

    @Deprecated(forRemoval=true)
    @VisibleForTesting
    public void setPlayerDetector(PlayerDetector playerDetector) {
        this.playerDetector = playerDetector;
    }

    @Deprecated(forRemoval=true)
    @VisibleForTesting
    public void overridePeacefulAndMobSpawnRule() {
        this.overridePeacefulAndMobSpawnRule = true;
    }

    public record FullConfig(Holder<TrialSpawnerConfig> normal, Holder<TrialSpawnerConfig> ominous, int targetCooldownLength, int requiredPlayerRange) {
        public static final MapCodec<FullConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)TrialSpawnerConfig.CODEC.optionalFieldOf("normal_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(FullConfig::normal), (App)TrialSpawnerConfig.CODEC.optionalFieldOf("ominous_config", Holder.direct(TrialSpawnerConfig.DEFAULT)).forGetter(FullConfig::ominous), (App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", (Object)36000).forGetter(FullConfig::targetCooldownLength), (App)Codec.intRange((int)1, (int)128).optionalFieldOf("required_player_range", (Object)14).forGetter(FullConfig::requiredPlayerRange)).apply((Applicative)i, FullConfig::new));
        public static final FullConfig DEFAULT = new FullConfig(Holder.direct(TrialSpawnerConfig.DEFAULT), Holder.direct(TrialSpawnerConfig.DEFAULT), 36000, 14);

        public FullConfig overrideEntity(EntityType<?> type) {
            return new FullConfig(Holder.direct(this.normal.value().withSpawning(type)), Holder.direct(this.ominous.value().withSpawning(type)), this.targetCooldownLength, this.requiredPlayerRange);
        }
    }

    public static interface StateAccessor {
        public void setState(Level var1, TrialSpawnerState var2);

        public TrialSpawnerState getState();

        public void markUpdated();
    }

    public static enum FlameParticle {
        NORMAL(ParticleTypes.FLAME),
        OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

        public final SimpleParticleType particleType;

        private FlameParticle(SimpleParticleType particleType) {
            this.particleType = particleType;
        }

        public static FlameParticle decode(int data) {
            FlameParticle[] values = FlameParticle.values();
            if (data > values.length || data < 0) {
                return NORMAL;
            }
            return values[data];
        }

        public int encode() {
            return this.ordinal();
        }
    }
}

