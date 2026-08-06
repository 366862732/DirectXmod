/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.dedicated;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.slf4j.Logger;

public class DedicatedPlayerList
extends PlayerList {
    private static final Logger LOGGER = LogUtils.getLogger();

    public DedicatedPlayerList(DedicatedServer server, LayeredRegistryAccess<RegistryLayer> registries, PlayerDataStorage playerDataStorage) {
        super(server, registries, playerDataStorage, server.notificationManager());
        this.setViewDistance(server.viewDistance());
        this.setSimulationDistance(server.simulationDistance());
        this.loadUserBanList();
        this.saveUserBanList();
        this.loadIpBanList();
        this.saveIpBanList();
        this.loadOps();
        this.loadWhiteList();
        this.saveOps();
        if (!this.getWhiteList().getFile().exists()) {
            this.saveWhiteList();
        }
    }

    @Override
    public void reloadWhiteList() {
        this.loadWhiteList();
    }

    private void saveIpBanList() {
        try {
            this.getIpBans().save();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to save ip banlist: ", (Throwable)e);
        }
    }

    private void saveUserBanList() {
        try {
            this.getBans().save();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to save user banlist: ", (Throwable)e);
        }
    }

    private void loadIpBanList() {
        try {
            this.getIpBans().load();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to load ip banlist: ", (Throwable)e);
        }
    }

    private void loadUserBanList() {
        try {
            this.getBans().load();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to load user banlist: ", (Throwable)e);
        }
    }

    private void loadOps() {
        try {
            this.getOps().load();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to load operators list: ", (Throwable)e);
        }
    }

    private void saveOps() {
        try {
            this.getOps().save();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to save operators list: ", (Throwable)e);
        }
    }

    private void loadWhiteList() {
        try {
            this.getWhiteList().load();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to load white-list: ", (Throwable)e);
        }
    }

    private void saveWhiteList() {
        try {
            this.getWhiteList().save();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to save white-list: ", (Throwable)e);
        }
    }

    @Override
    public boolean isWhiteListed(NameAndId nameAndId) {
        return !this.isUsingWhitelist() || this.isOp(nameAndId) || this.getWhiteList().isWhiteListed(nameAndId);
    }

    @Override
    public DedicatedServer getServer() {
        return (DedicatedServer)super.getServer();
    }

    @Override
    public boolean canBypassPlayerLimit(NameAndId nameAndId) {
        return this.getOps().canBypassPlayerLimit(nameAndId);
    }
}

