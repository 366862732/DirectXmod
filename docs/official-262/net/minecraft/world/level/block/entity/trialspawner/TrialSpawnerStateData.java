/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity.trialspawner;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerStateData {
    private static final String TAG_SPAWN_DATA = "spawn_data";
    private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
    private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
    private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
    final Set<UUID> detectedPlayers = new HashSet<UUID>();
    final Set<UUID> currentMobs = new HashSet<UUID>();
    long cooldownEndsAt;
    long nextMobSpawnsAt;
    int totalMobsSpawned;
    Optional<SpawnData> nextSpawnData = Optional.empty();
    Optional<ResourceKey<LootTable>> ejectingLootTable = Optional.empty();
    private @Nullable Entity displayEntity;
    private @Nullable WeightedList<ItemStack> dispensing;
    double spin;
    double oSpin;

    public Packed pack() {
        return new Packed(Set.copyOf(this.detectedPlayers), Set.copyOf(this.currentMobs), this.cooldownEndsAt, this.nextMobSpawnsAt, this.totalMobsSpawned, this.nextSpawnData, this.ejectingLootTable);
    }

    public void apply(Packed packed) {
        this.detectedPlayers.clear();
        this.detectedPlayers.addAll(packed.detectedPlayers);
        this.currentMobs.clear();
        this.currentMobs.addAll(packed.currentMobs);
        this.cooldownEndsAt = packed.cooldownEndsAt;
        this.nextMobSpawnsAt = packed.nextMobSpawnsAt;
        this.totalMobsSpawned = packed.totalMobsSpawned;
        this.nextSpawnData = packed.nextSpawnData;
        this.ejectingLootTable = packed.ejectingLootTable;
    }

    public void reset() {
        this.currentMobs.clear();
        this.nextSpawnData = Optional.empty();
        this.resetStatistics();
    }

    public void resetStatistics() {
        this.detectedPlayers.clear();
        this.totalMobsSpawned = 0;
        this.nextMobSpawnsAt = 0L;
        this.cooldownEndsAt = 0L;
    }

    public boolean hasMobToSpawn(TrialSpawner trialSpawner, RandomSource random) {
        boolean hasNextMobToSpawn = this.getOrCreateNextSpawnData(trialSpawner, random).getEntityToSpawn().getString("id").isPresent();
        return hasNextMobToSpawn || !trialSpawner.activeConfig().spawnPotentialsDefinition().isEmpty();
    }

    public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig config, int additionalPlayers) {
        return this.totalMobsSpawned >= config.calculateTargetTotalMobs(additionalPlayers);
    }

    public boolean haveAllCurrentMobsDied() {
        return this.currentMobs.isEmpty();
    }

    public boolean isReadyToSpawnNextMob(ServerLevel serverLevel, TrialSpawnerConfig config, int additionalPlayers) {
        return serverLevel.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < config.calculateTargetSimultaneousMobs(additionalPlayers);
    }

    public int countAdditionalPlayers(BlockPos pos) {
        if (this.detectedPlayers.isEmpty()) {
            Util.logAndPauseIfInIde("Trial Spawner at " + String.valueOf(pos) + " has no detected players");
        }
        return Math.max(0, this.detectedPlayers.size() - 1);
    }

    public void tryDetectPlayers(ServerLevel level, BlockPos pos, TrialSpawner trialSpawner) {
        List<UUID> foundPlayers;
        boolean becameOminous;
        boolean isThrottled;
        boolean bl = isThrottled = (pos.asLong() + level.getGameTime()) % 20L != 0L;
        if (isThrottled) {
            return;
        }
        if (trialSpawner.getState().equals(TrialSpawnerState.COOLDOWN) && trialSpawner.isOminous()) {
            return;
        }
        List<UUID> inLineOfSightPlayers = trialSpawner.getPlayerDetector().detect(level, trialSpawner.getEntitySelector(), pos, trialSpawner.getRequiredPlayerRange(), true);
        if (trialSpawner.isOminous() || inLineOfSightPlayers.isEmpty()) {
            becameOminous = false;
        } else {
            Optional<Pair<Player, Holder<MobEffect>>> playerWithOminousEffect = TrialSpawnerStateData.findPlayerWithOminousEffect(level, inLineOfSightPlayers);
            playerWithOminousEffect.ifPresent(playerAndEffect -> {
                Player player = (Player)playerAndEffect.getFirst();
                if (playerAndEffect.getSecond() == MobEffects.BAD_OMEN) {
                    TrialSpawnerStateData.transformBadOmenIntoTrialOmen(player);
                }
                level.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
                trialSpawner.applyOminous(level, pos);
            });
            becameOminous = playerWithOminousEffect.isPresent();
        }
        if (trialSpawner.getState().equals(TrialSpawnerState.COOLDOWN) && !becameOminous) {
            return;
        }
        boolean isSearchingForFirstPlayer = trialSpawner.getStateData().detectedPlayers.isEmpty();
        List<UUID> list = foundPlayers = isSearchingForFirstPlayer ? inLineOfSightPlayers : trialSpawner.getPlayerDetector().detect(level, trialSpawner.getEntitySelector(), pos, trialSpawner.getRequiredPlayerRange(), false);
        if (this.detectedPlayers.addAll(foundPlayers)) {
            this.nextMobSpawnsAt = Math.max(level.getGameTime() + 40L, this.nextMobSpawnsAt);
            if (!becameOminous) {
                int event = trialSpawner.isOminous() ? 3019 : 3013;
                level.levelEvent(event, pos, this.detectedPlayers.size());
            }
        }
    }

    private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel level, List<UUID> inLineOfSightPlayers) {
        Player playerWithBadOmen = null;
        for (UUID playerUuid : inLineOfSightPlayers) {
            Player player2 = level.getPlayerByUUID(playerUuid);
            if (player2 == null) continue;
            Holder<MobEffect> trialOmen = MobEffects.TRIAL_OMEN;
            if (player2.hasEffect(trialOmen)) {
                return Optional.of(Pair.of((Object)player2, trialOmen));
            }
            if (!player2.hasEffect(MobEffects.BAD_OMEN)) continue;
            playerWithBadOmen = player2;
        }
        return Optional.ofNullable(playerWithBadOmen).map(player -> Pair.of((Object)player, MobEffects.BAD_OMEN));
    }

    public void resetAfterBecomingOminous(TrialSpawner trialSpawner, ServerLevel level) {
        this.currentMobs.stream().map(level::getEntity).forEach(entity -> {
            if (entity == null) {
                return;
            }
            level.levelEvent(3012, entity.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
            if (entity instanceof Mob) {
                Mob mob = (Mob)entity;
                mob.dropPreservedEquipment(level);
            }
            entity.remove(Entity.RemovalReason.DISCARDED);
        });
        if (!trialSpawner.ominousConfig().spawnPotentialsDefinition().isEmpty()) {
            this.nextSpawnData = Optional.empty();
        }
        this.totalMobsSpawned = 0;
        this.currentMobs.clear();
        this.nextMobSpawnsAt = level.getGameTime() + (long)trialSpawner.ominousConfig().ticksBetweenSpawn();
        trialSpawner.markUpdated();
        this.cooldownEndsAt = level.getGameTime() + trialSpawner.ominousConfig().ticksBetweenItemSpawners();
    }

    private static void transformBadOmenIntoTrialOmen(Player player) {
        MobEffectInstance badOmen = player.getEffect(MobEffects.BAD_OMEN);
        if (badOmen == null) {
            return;
        }
        int amplifier = badOmen.getAmplifier() + 1;
        int duration = 18000 * amplifier;
        player.removeEffect(MobEffects.BAD_OMEN);
        player.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, duration, 0));
    }

    public boolean isReadyToOpenShutter(ServerLevel serverLevel, float delayBeforeOpen, int targetCooldownLength) {
        long cooldownStartedAt = this.cooldownEndsAt - (long)targetCooldownLength;
        return (float)serverLevel.getGameTime() >= (float)cooldownStartedAt + delayBeforeOpen;
    }

    public boolean isReadyToEjectItems(ServerLevel serverLevel, float timeBetweenEjections, int targetCooldownLength) {
        long cooldownStartedAt = this.cooldownEndsAt - (long)targetCooldownLength;
        return (float)(serverLevel.getGameTime() - cooldownStartedAt) % timeBetweenEjections == 0.0f;
    }

    public boolean isCooldownFinished(ServerLevel serverLevel) {
        return serverLevel.getGameTime() >= this.cooldownEndsAt;
    }

    protected SpawnData getOrCreateNextSpawnData(TrialSpawner trialSpawner, RandomSource random) {
        if (this.nextSpawnData.isPresent()) {
            return this.nextSpawnData.get();
        }
        WeightedList<SpawnData> spawnPotentials = trialSpawner.activeConfig().spawnPotentialsDefinition();
        Optional<SpawnData> selected = spawnPotentials.isEmpty() ? this.nextSpawnData : spawnPotentials.getRandom(random);
        this.nextSpawnData = Optional.of(selected.orElseGet(SpawnData::new));
        trialSpawner.markUpdated();
        return this.nextSpawnData.get();
    }

    public @Nullable Entity getOrCreateDisplayEntity(TrialSpawner trialSpawner, Level level, TrialSpawnerState state) {
        CompoundTag entityToSpawn;
        if (!state.hasSpinningMob()) {
            return null;
        }
        if (this.displayEntity == null && (entityToSpawn = this.getOrCreateNextSpawnData(trialSpawner, level.getRandom()).getEntityToSpawn()).getString("id").isPresent()) {
            this.displayEntity = EntityType.loadEntityRecursive(entityToSpawn, level, new EntitySpawnRequest(EntitySpawnReason.TRIAL_SPAWNER, true), BaseSpawner.SET_DISPLAY_ENTITY_ID);
        }
        return this.displayEntity;
    }

    public CompoundTag getUpdateTag(TrialSpawnerState state) {
        CompoundTag tag = new CompoundTag();
        if (state == TrialSpawnerState.ACTIVE) {
            tag.putLong(TAG_NEXT_MOB_SPAWNS_AT, this.nextMobSpawnsAt);
        }
        this.nextSpawnData.ifPresent(spawnData -> tag.store(TAG_SPAWN_DATA, SpawnData.CODEC, spawnData));
        return tag;
    }

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }

    public WeightedList<ItemStack> getDispensingItems(ServerLevel level, TrialSpawnerConfig config, BlockPos pos) {
        long simplePositionalSeed;
        LootParams params;
        if (this.dispensing != null) {
            return this.dispensing;
        }
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(config.itemsToDropWhenOminous());
        ObjectArrayList<ItemStack> lootDrops = lootTable.getRandomItems(params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY), simplePositionalSeed = TrialSpawnerStateData.lowResolutionPosition(level, pos));
        if (lootDrops.isEmpty()) {
            return WeightedList.of();
        }
        WeightedList.Builder<ItemStack> builder = WeightedList.builder();
        for (ItemStack drop : lootDrops) {
            builder.add(drop.copyWithCount(1), drop.getCount());
        }
        this.dispensing = builder.build();
        return this.dispensing;
    }

    private static long lowResolutionPosition(ServerLevel level, BlockPos pos) {
        BlockPos lowResolutionPosition = new BlockPos(Mth.floor((float)pos.getX() / 30.0f), Mth.floor((float)pos.getY() / 20.0f), Mth.floor((float)pos.getZ() / 30.0f));
        return level.getSeed() + lowResolutionPosition.asLong();
    }

    public record Packed(Set<UUID> detectedPlayers, Set<UUID> currentMobs, long cooldownEndsAt, long nextMobSpawnsAt, int totalMobsSpawned, Optional<SpawnData> nextSpawnData, Optional<ResourceKey<LootTable>> ejectingLootTable) {
        public static final MapCodec<Packed> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Set.of()).forGetter(Packed::detectedPlayers), (App)UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Set.of()).forGetter(Packed::currentMobs), (App)Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", (Object)0L).forGetter(Packed::cooldownEndsAt), (App)Codec.LONG.lenientOptionalFieldOf(TrialSpawnerStateData.TAG_NEXT_MOB_SPAWNS_AT, (Object)0L).forGetter(Packed::nextMobSpawnsAt), (App)Codec.intRange((int)0, (int)Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", (Object)0).forGetter(Packed::totalMobsSpawned), (App)SpawnData.CODEC.lenientOptionalFieldOf(TrialSpawnerStateData.TAG_SPAWN_DATA).forGetter(Packed::nextSpawnData), (App)LootTable.KEY_CODEC.lenientOptionalFieldOf("ejecting_loot_table").forGetter(Packed::ejectingLootTable)).apply((Applicative)i, Packed::new));
    }
}

