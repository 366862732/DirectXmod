/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public enum TrialSpawnerState implements StringRepresentable
{
    INACTIVE("inactive", 0, ParticleEmission.NONE, -1.0, false),
    WAITING_FOR_PLAYERS("waiting_for_players", 4, ParticleEmission.SMALL_FLAMES, 200.0, true),
    ACTIVE("active", 8, ParticleEmission.FLAMES_AND_SMOKE, 1000.0, true),
    WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, ParticleEmission.SMALL_FLAMES, -1.0, false),
    EJECTING_REWARD("ejecting_reward", 8, ParticleEmission.SMALL_FLAMES, -1.0, false),
    COOLDOWN("cooldown", 0, ParticleEmission.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

    private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0f;
    private static final int TIME_BETWEEN_EACH_EJECTION;
    private final String name;
    private final int lightLevel;
    private final double spinningMobSpeed;
    private final ParticleEmission particleEmission;
    private final boolean isCapableOfSpawning;

    private TrialSpawnerState(String name, int lightLevel, ParticleEmission particleEmission, double spinningMobSpeed, boolean isCapableOfSpawning) {
        this.name = name;
        this.lightLevel = lightLevel;
        this.particleEmission = particleEmission;
        this.spinningMobSpeed = spinningMobSpeed;
        this.isCapableOfSpawning = isCapableOfSpawning;
    }

    TrialSpawnerState tickAndGetNext(BlockPos spawnerPos, TrialSpawner trialSpawner, ServerLevel serverLevel) {
        TrialSpawnerStateData data = trialSpawner.getStateData();
        TrialSpawnerConfig config = trialSpawner.activeConfig();
        RandomSource random = serverLevel.getRandom();
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (data.getOrCreateDisplayEntity(trialSpawner, serverLevel, WAITING_FOR_PLAYERS) == null) {
                    yield this;
                }
                yield WAITING_FOR_PLAYERS;
            }
            case 1 -> {
                if (!trialSpawner.canSpawnInLevel(serverLevel)) {
                    data.resetStatistics();
                    yield this;
                }
                if (!data.hasMobToSpawn(trialSpawner, random)) {
                    yield INACTIVE;
                }
                data.tryDetectPlayers(serverLevel, spawnerPos, trialSpawner);
                if (data.detectedPlayers.isEmpty()) {
                    yield this;
                }
                yield ACTIVE;
            }
            case 2 -> {
                if (!trialSpawner.canSpawnInLevel(serverLevel)) {
                    data.resetStatistics();
                    yield WAITING_FOR_PLAYERS;
                }
                if (!data.hasMobToSpawn(trialSpawner, random)) {
                    yield INACTIVE;
                }
                int additionalPlayers = data.countAdditionalPlayers(spawnerPos);
                data.tryDetectPlayers(serverLevel, spawnerPos, trialSpawner);
                if (trialSpawner.isOminous()) {
                    this.spawnOminousOminousItemSpawner(serverLevel, spawnerPos, trialSpawner);
                }
                if (data.hasFinishedSpawningAllMobs(config, additionalPlayers)) {
                    if (data.haveAllCurrentMobsDied()) {
                        data.cooldownEndsAt = serverLevel.getGameTime() + (long)trialSpawner.getTargetCooldownLength();
                        data.totalMobsSpawned = 0;
                        data.nextMobSpawnsAt = 0L;
                        yield WAITING_FOR_REWARD_EJECTION;
                    }
                } else if (data.isReadyToSpawnNextMob(serverLevel, config, additionalPlayers)) {
                    trialSpawner.spawnMob(serverLevel, spawnerPos).ifPresent(entityId -> {
                        data.currentMobs.add((UUID)entityId);
                        ++data.totalMobsSpawned;
                        data.nextMobSpawnsAt = serverLevel.getGameTime() + (long)config.ticksBetweenSpawn();
                        config.spawnPotentialsDefinition().getRandom(random).ifPresent(entry -> {
                            data.nextSpawnData = Optional.of(entry);
                            trialSpawner.markUpdated();
                        });
                    });
                }
                yield this;
            }
            case 3 -> {
                if (data.isReadyToOpenShutter(serverLevel, 40.0f, trialSpawner.getTargetCooldownLength())) {
                    serverLevel.playSound(null, spawnerPos, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS);
                    yield EJECTING_REWARD;
                }
                yield this;
            }
            case 4 -> {
                if (!data.isReadyToEjectItems(serverLevel, TIME_BETWEEN_EACH_EJECTION, trialSpawner.getTargetCooldownLength())) {
                    yield this;
                }
                if (data.detectedPlayers.isEmpty()) {
                    serverLevel.playSound(null, spawnerPos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS);
                    data.ejectingLootTable = Optional.empty();
                    yield COOLDOWN;
                }
                if (data.ejectingLootTable.isEmpty()) {
                    data.ejectingLootTable = config.lootTablesToEject().getRandom(random);
                }
                data.ejectingLootTable.ifPresent(lootTable -> trialSpawner.ejectReward(serverLevel, spawnerPos, (ResourceKey<LootTable>)lootTable));
                data.detectedPlayers.remove(data.detectedPlayers.iterator().next());
                yield this;
            }
            case 5 -> {
                data.tryDetectPlayers(serverLevel, spawnerPos, trialSpawner);
                if (!data.detectedPlayers.isEmpty()) {
                    data.totalMobsSpawned = 0;
                    data.nextMobSpawnsAt = 0L;
                    yield ACTIVE;
                }
                if (data.isCooldownFinished(serverLevel)) {
                    trialSpawner.removeOminous(serverLevel, spawnerPos);
                    data.reset();
                    yield WAITING_FOR_PLAYERS;
                }
                yield this;
            }
        };
    }

    private void spawnOminousOminousItemSpawner(ServerLevel level, BlockPos trialSpawnerPos, TrialSpawner trialSpawner) {
        TrialSpawnerConfig config;
        TrialSpawnerStateData data = trialSpawner.getStateData();
        ItemStack itemToDispense = data.getDispensingItems(level, config = trialSpawner.activeConfig(), trialSpawnerPos).getRandom(level.getRandom()).orElse(ItemStack.EMPTY);
        if (itemToDispense.isEmpty()) {
            return;
        }
        if (this.timeToSpawnItemSpawner(level, data)) {
            TrialSpawnerState.calculatePositionToSpawnSpawner(level, trialSpawnerPos, trialSpawner, data).ifPresent(pos -> {
                OminousItemSpawner itemSpawner = OminousItemSpawner.create(level, itemToDispense);
                itemSpawner.snapTo((Vec3)pos);
                level.addFreshEntity(itemSpawner);
                float pitch = (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2f + 1.0f;
                level.playSound(null, BlockPos.containing(pos), SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, SoundSource.BLOCKS, 1.0f, pitch);
                data.cooldownEndsAt = level.getGameTime() + trialSpawner.ominousConfig().ticksBetweenItemSpawners();
            });
        }
    }

    private static Optional<Vec3> calculatePositionToSpawnSpawner(ServerLevel level, BlockPos trialSpawnerPos, TrialSpawner trialSpawner, TrialSpawnerStateData data) {
        List<Player> nearbyPlayers = data.detectedPlayers.stream().map(level::getPlayerByUUID).filter(Objects::nonNull).filter(player -> !player.isCreative() && !player.isSpectator() && player.isAlive() && player.distanceToSqr(Vec3.atCenterOf(trialSpawnerPos)) <= (double)Mth.square(trialSpawner.getRequiredPlayerRange())).toList();
        if (nearbyPlayers.isEmpty()) {
            return Optional.empty();
        }
        Entity entity = TrialSpawnerState.selectEntityToSpawnItemAbove(nearbyPlayers, data.currentMobs, trialSpawner, trialSpawnerPos, level);
        if (entity == null) {
            return Optional.empty();
        }
        return TrialSpawnerState.calculatePositionAbove(entity, level);
    }

    private static Optional<Vec3> calculatePositionAbove(Entity entityToSpawnItemAbove, ServerLevel level) {
        Vec3 trySpawnPos;
        Vec3 entityPos = entityToSpawnItemAbove.position();
        BlockHitResult hitResult = level.clip(new ClipContext(entityPos, trySpawnPos = entityPos.relative(Direction.UP, entityToSpawnItemAbove.getBbHeight() + 2.0f + (float)level.getRandom().nextInt(4)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 down = Vec3.atCenterOf(hitResult.getBlockPos()).relative(Direction.DOWN, 1.0);
        BlockPos blockPosDown = BlockPos.containing(down);
        if (!level.getBlockState(blockPosDown).getCollisionShape(level, blockPosDown).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(down);
    }

    private static @Nullable Entity selectEntityToSpawnItemAbove(List<Player> nearbyPlayers, Set<UUID> mobIds, TrialSpawner trialSpawner, BlockPos spawnerPos, ServerLevel level) {
        List<Entity> eligibleEntities;
        Stream<Entity> nearbyMobs = mobIds.stream().map(level::getEntity).filter(Objects::nonNull).filter(target -> target.isAlive() && target.distanceToSqr(Vec3.atCenterOf(spawnerPos)) <= (double)Mth.square(trialSpawner.getRequiredPlayerRange()));
        RandomSource random = level.getRandom();
        List<Entity> list = eligibleEntities = random.nextBoolean() ? nearbyMobs.toList() : nearbyPlayers;
        if (eligibleEntities.isEmpty()) {
            return null;
        }
        if (eligibleEntities.size() == 1) {
            return eligibleEntities.getFirst();
        }
        return Util.getRandom(eligibleEntities, random);
    }

    private boolean timeToSpawnItemSpawner(ServerLevel serverLevel, TrialSpawnerStateData data) {
        return serverLevel.getGameTime() >= data.cooldownEndsAt;
    }

    public int lightLevel() {
        return this.lightLevel;
    }

    public double spinningMobSpeed() {
        return this.spinningMobSpeed;
    }

    public boolean hasSpinningMob() {
        return this.spinningMobSpeed >= 0.0;
    }

    public boolean isCapableOfSpawning() {
        return this.isCapableOfSpawning;
    }

    public void emitParticles(Level level, BlockPos blockPos, boolean isOminous) {
        this.particleEmission.emit(level, level.getRandom(), blockPos, isOminous);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        TIME_BETWEEN_EACH_EJECTION = Mth.floor(30.0f);
    }

    private static interface ParticleEmission {
        public static final ParticleEmission NONE = (level, random, pos, isOminous) -> {};
        public static final ParticleEmission SMALL_FLAMES = (level, random, pos, isOminous) -> {
            if (random.nextInt(2) == 0) {
                Vec3 vec = Vec3.atCenterOf(pos).offsetRandom(random, 0.9f);
                ParticleEmission.addParticle(isOminous ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME, vec, level);
            }
        };
        public static final ParticleEmission FLAMES_AND_SMOKE = (level, random, pos, isOminous) -> {
            Vec3 vec = Vec3.atCenterOf(pos).offsetRandom(random, 1.0f);
            ParticleEmission.addParticle(ParticleTypes.SMOKE, vec, level);
            ParticleEmission.addParticle(isOminous ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, vec, level);
        };
        public static final ParticleEmission SMOKE_INSIDE_AND_TOP_FACE = (level, random, pos, isOminous) -> {
            Vec3 vec = Vec3.atCenterOf(pos).offsetRandom(random, 0.9f);
            if (random.nextInt(3) == 0) {
                ParticleEmission.addParticle(ParticleTypes.SMOKE, vec, level);
            }
            if (level.getGameTime() % 20L == 0L) {
                Vec3 topFaceVec = Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0);
                int smokeCount = level.getRandom().nextInt(4) + 20;
                for (int i = 0; i < smokeCount; ++i) {
                    ParticleEmission.addParticle(ParticleTypes.SMOKE, topFaceVec, level);
                }
            }
        };

        private static void addParticle(SimpleParticleType smoke, Vec3 vec, Level level) {
            level.addParticle(smoke, vec.x(), vec.y(), vec.z(), 0.0, 0.0, 0.0);
        }

        public void emit(Level var1, RandomSource var2, BlockPos var3, boolean var4);
    }

    private static class LightLevel {
        private static final int UNLIT = 0;
        private static final int HALF_LIT = 4;
        private static final int LIT = 8;

        private LightLevel() {
        }
    }

    private static class SpinningMob {
        private static final double NONE = -1.0;
        private static final double SLOW = 200.0;
        private static final double FAST = 1000.0;

        private SpinningMob() {
        }
    }
}

