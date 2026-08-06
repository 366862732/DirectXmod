/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.notifications;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class EmptyNotificationService
implements NotificationService {
    @Override
    public void playerJoined(ServerPlayer player) {
    }

    @Override
    public void playerLeft(ServerPlayer player) {
    }

    @Override
    public void serverStarted() {
    }

    @Override
    public void serverShuttingDown() {
    }

    @Override
    public void serverSaveStarted() {
    }

    @Override
    public void serverSaveCompleted() {
    }

    @Override
    public void serverActivityOccured() {
    }

    @Override
    public void playerOped(ServerOpListEntry operator) {
    }

    @Override
    public void playerDeoped(ServerOpListEntry operator) {
    }

    @Override
    public void playerAddedToAllowlist(NameAndId player) {
    }

    @Override
    public void playerRemovedFromAllowlist(NameAndId player) {
    }

    @Override
    public void ipBanned(IpBanListEntry ban) {
    }

    @Override
    public void ipUnbanned(String ip) {
    }

    @Override
    public void playerBanned(UserBanListEntry ban) {
    }

    @Override
    public void playerUnbanned(NameAndId player) {
    }

    @Override
    public <T> void onGameRuleChanged(GameRule<T> gameRule, T value) {
    }

    @Override
    public void statusHeartbeat() {
    }
}

