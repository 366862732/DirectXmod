/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.quickplay;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class QuickPlay {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component ERROR_TITLE = Component.translatable("quickplay.error.title");
    private static final Component INVALID_IDENTIFIER = Component.translatable("quickplay.error.invalid_identifier");
    private static final Component REALM_CONNECT = Component.translatable("quickplay.error.realm_connect");
    private static final Component REALM_PERMISSION = Component.translatable("quickplay.error.realm_permission");
    private static final Component TO_TITLE = Component.translatable("gui.toTitle");
    private static final Component TO_WORLD_LIST = Component.translatable("gui.toWorld");
    private static final Component TO_REALMS_LIST = Component.translatable("gui.toRealms");

    public static void connect(Minecraft minecraft, GameConfig.QuickPlayVariant quickPlayVariant, RealmsClient realmsClient) {
        if (!quickPlayVariant.isEnabled()) {
            LOGGER.error("Quick play disabled");
            minecraft.gui.setScreen(new TitleScreen());
            return;
        }
        GameConfig.QuickPlayVariant quickPlayVariant2 = quickPlayVariant;
        Objects.requireNonNull(quickPlayVariant2);
        GameConfig.QuickPlayVariant quickPlayVariant3 = quickPlayVariant2;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{GameConfig.QuickPlayMultiplayerData.class, GameConfig.QuickPlayRealmsData.class, GameConfig.QuickPlaySinglePlayerData.class, GameConfig.QuickPlayDisabled.class}, (GameConfig.QuickPlayVariant)quickPlayVariant3, n)) {
            default: {
                throw new MatchException(null, null);
            }
            case 0: {
                GameConfig.QuickPlayMultiplayerData multiplayerData = (GameConfig.QuickPlayMultiplayerData)quickPlayVariant3;
                QuickPlay.joinMultiplayerWorld(minecraft, multiplayerData.serverAddress());
                break;
            }
            case 1: {
                GameConfig.QuickPlayRealmsData realmsData = (GameConfig.QuickPlayRealmsData)quickPlayVariant3;
                QuickPlay.joinRealmsWorld(minecraft, realmsClient, realmsData.realmId());
                break;
            }
            case 2: {
                GameConfig.QuickPlaySinglePlayerData singlePlayerData = (GameConfig.QuickPlaySinglePlayerData)quickPlayVariant3;
                String worldId = singlePlayerData.worldId();
                if (StringUtil.isBlank(worldId)) {
                    worldId = QuickPlay.getLatestSingleplayerWorld(minecraft.getLevelSource());
                }
                QuickPlay.joinSingleplayerWorld(minecraft, worldId);
                break;
            }
            case 3: {
                GameConfig.QuickPlayDisabled disabled = (GameConfig.QuickPlayDisabled)quickPlayVariant3;
                LOGGER.error("Quick play disabled");
                minecraft.gui.setScreen(new TitleScreen());
            }
        }
    }

    private static @Nullable String getLatestSingleplayerWorld(LevelStorageSource levelSource) {
        try {
            List<LevelSummary> levels = levelSource.loadLevelSummaries(levelSource.findLevelCandidates()).get();
            if (levels.isEmpty()) {
                LOGGER.warn("no latest singleplayer world found");
                return null;
            }
            return levels.getFirst().getLevelId();
        }
        catch (InterruptedException | ExecutionException e) {
            LOGGER.error("failed to load singleplayer world summaries", (Throwable)e);
            return null;
        }
    }

    private static void joinSingleplayerWorld(Minecraft minecraft, @Nullable String identifier) {
        if (StringUtil.isBlank(identifier) || !minecraft.getLevelSource().levelExists(identifier)) {
            SelectWorldScreen parent = new SelectWorldScreen(new TitleScreen());
            minecraft.gui.setScreen(new DisconnectedScreen((Screen)parent, ERROR_TITLE, INVALID_IDENTIFIER, TO_WORLD_LIST));
            return;
        }
        minecraft.createWorldOpenFlows().openWorld(identifier, () -> minecraft.gui.setScreen(new TitleScreen()));
    }

    private static void joinMultiplayerWorld(Minecraft minecraft, String serverAddressString) {
        ServerList servers = new ServerList(minecraft);
        servers.load();
        ServerData serverData = servers.get(serverAddressString);
        if (serverData == null) {
            serverData = new ServerData(I18n.get("selectServer.defaultName", new Object[0]), serverAddressString, ServerData.Type.OTHER);
            servers.add(serverData, true);
            servers.save();
        }
        ServerAddress serverAddress = ServerAddress.parseString(serverAddressString);
        ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), minecraft, serverAddress, serverData, true, null);
    }

    private static void joinRealmsWorld(Minecraft minecraft, RealmsClient realmsClient, String identifier) {
        RealmsServerList realmsServerList;
        long realmId;
        try {
            realmId = Long.parseLong(identifier);
            realmsServerList = realmsClient.listRealms();
        }
        catch (NumberFormatException e) {
            RealmsMainScreen parent = new RealmsMainScreen(new TitleScreen());
            minecraft.gui.setScreen(new DisconnectedScreen((Screen)parent, ERROR_TITLE, INVALID_IDENTIFIER, TO_REALMS_LIST));
            return;
        }
        catch (RealmsServiceException e) {
            TitleScreen parent = new TitleScreen();
            minecraft.gui.setScreen(new DisconnectedScreen((Screen)parent, ERROR_TITLE, REALM_CONNECT, TO_TITLE));
            return;
        }
        RealmsServer server = realmsServerList.servers().stream().filter(realmsServer -> realmsServer.id == realmId).findFirst().orElse(null);
        if (server == null) {
            RealmsMainScreen parent = new RealmsMainScreen(new TitleScreen());
            minecraft.gui.setScreen(new DisconnectedScreen((Screen)parent, ERROR_TITLE, REALM_PERMISSION, TO_REALMS_LIST));
            return;
        }
        TitleScreen titleScreen = new TitleScreen();
        minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(titleScreen, new GetServerDetailsTask(titleScreen, server)));
    }
}

