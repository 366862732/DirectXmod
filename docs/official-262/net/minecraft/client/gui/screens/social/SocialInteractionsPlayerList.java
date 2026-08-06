/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  com.google.common.collect.Lists
 *  com.mojang.authlib.GameProfile
 *  it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SocialInteractionsPlayerList
extends ContainerObjectSelectionList<PlayerEntry> {
    private final SocialInteractionsScreen socialInteractionsScreen;
    private final List<PlayerEntry> players = Lists.newArrayList();
    private @Nullable String filter;

    public SocialInteractionsPlayerList(SocialInteractionsScreen socialInteractionsScreen, Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.socialInteractionsScreen = socialInteractionsScreen;
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void enableScissor(GuiGraphicsExtractor graphics) {
        graphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
    }

    public void updatePlayerList(Collection<UUID> playersToAdd, double scrollAmount, boolean addOfflineEntries) {
        HashMap<UUID, PlayerEntry> newEntries = new HashMap<UUID, PlayerEntry>();
        this.addOnlinePlayers(playersToAdd, newEntries);
        if (addOfflineEntries) {
            this.addSeenPlayers(newEntries);
        }
        this.updatePlayersFromChatLog(newEntries, addOfflineEntries);
        this.updateFiltersAndScroll(newEntries.values(), scrollAmount);
    }

    private void addOnlinePlayers(Collection<UUID> playersToAdd, Map<UUID, PlayerEntry> output) {
        ClientPacketListener connection = this.minecraft.player.connection;
        for (UUID id : playersToAdd) {
            PlayerInfo playerInfo = connection.getPlayerInfo(id);
            if (playerInfo == null) continue;
            PlayerEntry player = this.makePlayerEntry(id, playerInfo);
            output.put(id, player);
        }
    }

    private void addSeenPlayers(Map<UUID, PlayerEntry> newEntries) {
        Map<UUID, PlayerInfo> seenPlayers = this.minecraft.player.connection.getSeenPlayers();
        for (Map.Entry<UUID, PlayerInfo> entry : seenPlayers.entrySet()) {
            newEntries.computeIfAbsent(entry.getKey(), uuid -> {
                PlayerEntry player = this.makePlayerEntry((UUID)uuid, (PlayerInfo)entry.getValue());
                player.setRemoved(true);
                return player;
            });
        }
    }

    private PlayerEntry makePlayerEntry(UUID id, PlayerInfo playerInfo) {
        return new PlayerEntry(this.minecraft, this.socialInteractionsScreen, id, playerInfo.getProfile().name(), playerInfo::getSkin, playerInfo.hasVerifiableChat());
    }

    private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> entries, boolean addOfflineEntries) {
        Map<UUID, GameProfile> gameProfiles = SocialInteractionsPlayerList.collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog());
        gameProfiles.forEach((id, gameProfile) -> {
            PlayerEntry entry;
            if (addOfflineEntries) {
                entry = entries.computeIfAbsent((UUID)id, uuid -> {
                    PlayerEntry player = new PlayerEntry(this.minecraft, this.socialInteractionsScreen, gameProfile.id(), gameProfile.name(), this.minecraft.getSkinManager().createLookup((GameProfile)gameProfile, true), true);
                    player.setRemoved(true);
                    return player;
                });
            } else {
                entry = (PlayerEntry)entries.get(id);
                if (entry == null) {
                    return;
                }
            }
            entry.setHasRecentMessages(true);
        });
    }

    private static Map<UUID, GameProfile> collectProfilesFromChatLog(ChatLog chatLog) {
        Object2ObjectLinkedOpenHashMap gameProfiles = new Object2ObjectLinkedOpenHashMap();
        for (int id = chatLog.end(); id >= chatLog.start(); --id) {
            LoggedChatMessage.Player message;
            LoggedChatEvent event = chatLog.lookup(id);
            if (!(event instanceof LoggedChatMessage.Player) || !(message = (LoggedChatMessage.Player)event).message().hasSignature()) continue;
            gameProfiles.put(message.profileId(), message.profile());
        }
        return gameProfiles;
    }

    private void sortPlayerEntries() {
        this.players.sort(Comparator.comparing(e -> {
            if (this.minecraft.isLocalPlayer(e.getPlayerId())) {
                return 0;
            }
            if (this.minecraft.getReportingContext().hasDraftReportFor(e.getPlayerId())) {
                return 1;
            }
            if (e.getPlayerId().version() == 2) {
                return 4;
            }
            if (e.hasRecentMessages()) {
                return 2;
            }
            return 3;
        }).thenComparing(e -> {
            int firstCodepoint;
            if (!e.getPlayerName().isBlank() && ((firstCodepoint = e.getPlayerName().codePointAt(0)) == 95 || firstCodepoint >= 97 && firstCodepoint <= 122 || firstCodepoint >= 65 && firstCodepoint <= 90 || firstCodepoint >= 48 && firstCodepoint <= 57)) {
                return 0;
            }
            return 1;
        }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
    }

    private void updateFiltersAndScroll(Collection<PlayerEntry> newEntries, double scrollAmount) {
        this.players.clear();
        this.players.addAll(newEntries);
        this.sortPlayerEntries();
        this.updateFilteredPlayers();
        this.replaceEntries(this.players);
        this.setScrollAmount(scrollAmount);
    }

    private void updateFilteredPlayers() {
        if (this.filter != null) {
            this.players.removeIf(p -> !p.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
            this.replaceEntries(this.players);
        }
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    public void addPlayer(PlayerInfo player, SocialInteractionsScreen.Page page) {
        UUID playerId = player.getProfile().id();
        for (PlayerEntry playerEntry : this.players) {
            if (!playerEntry.getPlayerId().equals(playerId)) continue;
            playerEntry.setRemoved(false);
            return;
        }
        if ((page == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(playerId)) && (Strings.isNullOrEmpty((String)this.filter) || player.getProfile().name().toLowerCase(Locale.ROOT).contains(this.filter))) {
            PlayerEntry playerEntry;
            boolean chatReportable = player.hasVerifiableChat();
            playerEntry = new PlayerEntry(this.minecraft, this.socialInteractionsScreen, player.getProfile().id(), player.getProfile().name(), player::getSkin, chatReportable);
            this.addEntry(playerEntry);
            this.players.add(playerEntry);
        }
    }

    public void removePlayer(UUID id) {
        for (PlayerEntry playerEntry : this.players) {
            if (!playerEntry.getPlayerId().equals(id)) continue;
            playerEntry.setRemoved(true);
            return;
        }
    }

    public void refreshHasDraftReport() {
        this.players.forEach(playerEntry -> playerEntry.refreshHasDraftReport(this.minecraft.getReportingContext()));
    }
}

