/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.entity.monster.warden;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WardenSpawnTracker {
    public static final Codec<WardenSpawnTracker> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "ticks_since_last_warning", 0).forGetter(o -> o.ticksSinceLastWarning), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "warning_level", 0).forGetter(o -> o.warningLevel), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "cooldown_ticks", 0).forGetter(o -> o.cooldownTicks)).apply((Applicative)i, WardenSpawnTracker::new));
    public static final int MAX_WARNING_LEVEL = 4;
    private static final double PLAYER_SEARCH_RADIUS = 16.0;
    private static final int WARNING_CHECK_DIAMETER = 48;
    private static final int DECREASE_WARNING_LEVEL_EVERY_INTERVAL = 12000;
    private static final int WARNING_LEVEL_INCREASE_COOLDOWN = 200;
    private int ticksSinceLastWarning;
    private int warningLevel;
    private int cooldownTicks;

    public WardenSpawnTracker(int ticksSinceLastWarning, int warningLevel, int cooldownTicks) {
        this.ticksSinceLastWarning = ticksSinceLastWarning;
        this.warningLevel = warningLevel;
        this.cooldownTicks = cooldownTicks;
    }

    public WardenSpawnTracker() {
        this(0, 0, 0);
    }

    public void tick() {
        if (this.ticksSinceLastWarning >= 12000) {
            this.decreaseWarningLevel();
            this.ticksSinceLastWarning = 0;
        } else {
            ++this.ticksSinceLastWarning;
        }
        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
        }
    }

    public void reset() {
        this.ticksSinceLastWarning = 0;
        this.warningLevel = 0;
        this.cooldownTicks = 0;
    }

    public static OptionalInt tryWarn(ServerLevel level, BlockPos pos, ServerPlayer triggerPlayer) {
        if (WardenSpawnTracker.hasNearbyWarden(level, pos)) {
            return OptionalInt.empty();
        }
        List<ServerPlayer> players = WardenSpawnTracker.getNearbyPlayers(level, pos);
        if (!players.contains(triggerPlayer)) {
            players.add(triggerPlayer);
        }
        if (players.stream().anyMatch(player -> player.getWardenSpawnTracker().map(WardenSpawnTracker::onCooldown).orElse(false))) {
            return OptionalInt.empty();
        }
        Optional<WardenSpawnTracker> highestWarningSpawnTracker = players.stream().flatMap(player -> player.getWardenSpawnTracker().stream()).max(Comparator.comparingInt(WardenSpawnTracker::getWarningLevel));
        if (highestWarningSpawnTracker.isPresent()) {
            WardenSpawnTracker spawnTracker = highestWarningSpawnTracker.get();
            spawnTracker.increaseWarningLevel();
            players.forEach(player -> player.getWardenSpawnTracker().ifPresent(otherSpawnTracker -> otherSpawnTracker.copyData(spawnTracker)));
            return OptionalInt.of(spawnTracker.warningLevel);
        }
        return OptionalInt.empty();
    }

    private boolean onCooldown() {
        return this.cooldownTicks > 0;
    }

    private static boolean hasNearbyWarden(ServerLevel level, BlockPos pos) {
        AABB areaToCheck = AABB.ofSize(Vec3.atCenterOf(pos), 48.0, 48.0, 48.0);
        return !level.getEntitiesOfClass(Warden.class, areaToCheck).isEmpty();
    }

    private static List<ServerPlayer> getNearbyPlayers(ServerLevel level, BlockPos pos) {
        Vec3 origin = Vec3.atCenterOf(pos);
        return level.getPlayers(player -> !player.isSpectator() && player.position().closerThan(origin, 16.0) && player.isAlive());
    }

    private void increaseWarningLevel() {
        if (!this.onCooldown()) {
            this.ticksSinceLastWarning = 0;
            this.cooldownTicks = 200;
            this.setWarningLevel(this.getWarningLevel() + 1);
        }
    }

    private void decreaseWarningLevel() {
        this.setWarningLevel(this.getWarningLevel() - 1);
    }

    public void setWarningLevel(int warningLevel) {
        this.warningLevel = Mth.clamp(warningLevel, 0, 4);
    }

    public int getWarningLevel() {
        return this.warningLevel;
    }

    private void copyData(WardenSpawnTracker copyFrom) {
        this.warningLevel = copyFrom.warningLevel;
        this.cooldownTicks = copyFrom.cooldownTicks;
        this.ticksSinceLastWarning = copyFrom.ticksSinceLastWarning;
    }
}

