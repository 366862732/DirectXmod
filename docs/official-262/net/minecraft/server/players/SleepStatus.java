/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.players;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class SleepStatus {
    private int activePlayers;
    private int sleepingPlayers;

    public boolean areEnoughSleeping(int sleepPercentageNeeded) {
        return this.sleepingPlayers >= this.sleepersNeeded(sleepPercentageNeeded);
    }

    public boolean areEnoughDeepSleeping(int sleepPercentageNeeded, List<ServerPlayer> players) {
        int deepSleepers = (int)players.stream().filter(Player::isSleepingLongEnough).count();
        return deepSleepers >= this.sleepersNeeded(sleepPercentageNeeded);
    }

    public int sleepersNeeded(int sleepPercentageNeeded) {
        return Math.max(1, Mth.ceil((float)(this.activePlayers * sleepPercentageNeeded) / 100.0f));
    }

    public void removeAllSleepers() {
        this.sleepingPlayers = 0;
    }

    public int amountSleeping() {
        return this.sleepingPlayers;
    }

    public boolean update(List<ServerPlayer> players) {
        int oldActivePlayers = this.activePlayers;
        int oldSleepingPlayers = this.sleepingPlayers;
        this.activePlayers = 0;
        this.sleepingPlayers = 0;
        for (ServerPlayer player : players) {
            if (player.isSpectator()) continue;
            ++this.activePlayers;
            if (!player.isSleeping()) continue;
            ++this.sleepingPlayers;
        }
        return !(oldSleepingPlayers <= 0 && this.sleepingPlayers <= 0 || oldActivePlayers == this.activePlayers && oldSleepingPlayers == this.sleepingPlayers);
    }
}

