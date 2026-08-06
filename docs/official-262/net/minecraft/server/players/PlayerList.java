/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.authlib.GameProfile
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.FileUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class PlayerList {
    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    public static final Component CHAT_FILTERED_FULL = Component.translatable("chat.filtered_full");
    public static final Component DUPLICATE_LOGIN_DISCONNECT_MESSAGE = Component.translatable("multiplayer.disconnect.duplicate_login");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z", Locale.ROOT);
    private final MinecraftServer server;
    private final List<ServerPlayer> players = Lists.newArrayList();
    private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
    private final UserBanList bans;
    private final IpBanList ipBans;
    private final ServerOpList ops;
    private final UserWhiteList whitelist;
    private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
    private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
    private final PlayerDataStorage playerIo;
    private final LayeredRegistryAccess<RegistryLayer> registries;
    private int viewDistance;
    private int simulationDistance;
    private boolean allowCommandsForAllPlayers;
    private int sendAllPlayerInfoIn;

    public PlayerList(MinecraftServer server, LayeredRegistryAccess<RegistryLayer> registries, PlayerDataStorage playerIo, NotificationService notificationService) {
        this.server = server;
        this.registries = registries;
        this.playerIo = playerIo;
        this.whitelist = new UserWhiteList(WHITELIST_FILE, notificationService);
        this.ops = new ServerOpList(OPLIST_FILE, notificationService);
        this.bans = new UserBanList(USERBANLIST_FILE, notificationService);
        this.ipBans = new IpBanList(IPBANLIST_FILE, notificationService);
    }

    public void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        NameAndId gameProfile = player.nameAndId();
        UserNameToIdResolver profileCache = this.server.services().nameToIdCache();
        Optional<NameAndId> oldProfile = profileCache.get(gameProfile.id());
        String oldName = oldProfile.map(NameAndId::name).orElse(gameProfile.name());
        profileCache.add(gameProfile);
        ServerLevel level = player.level();
        String address = connection.getLoggableAddress(this.server.logIPs());
        LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", new Object[]{player.getPlainTextName(), address, player.getId(), player.getX(), player.getY(), player.getZ()});
        LevelData levelData = level.getLevelData();
        ServerGamePacketListenerImpl playerConnection = new ServerGamePacketListenerImpl(this.server, connection, player, cookie);
        connection.setupInboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess()), playerConnection), playerConnection);
        playerConnection.suspendFlushing();
        GameRules gameRules = level.getGameRules();
        boolean immediateRespawn = gameRules.get(GameRules.IMMEDIATE_RESPAWN);
        boolean reducedDebugInfo = gameRules.get(GameRules.REDUCED_DEBUG_INFO);
        boolean doLimitedCrafting = gameRules.get(GameRules.LIMITED_CRAFTING);
        playerConnection.send(new ClientboundLoginPacket(player.getId(), levelData.isHardcore(), this.server.levelKeys(), this.getMaxPlayers(), this.getViewDistance(), this.getSimulationDistance(), reducedDebugInfo, !immediateRespawn, doLimitedCrafting, player.createCommonSpawnInfo(level), this.server.usesAuthentication(), this.server.enforceSecureProfile()));
        playerConnection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        playerConnection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        playerConnection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
        RecipeManager recipeManager = this.server.getRecipeManager();
        playerConnection.send(new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), recipeManager.getSynchronizedStonecutterRecipes()));
        this.sendPlayerPermissionLevel(player);
        player.getStats().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        this.updateEntireScoreboard(level.getScoreboard(), player);
        this.server.invalidateStatus();
        MutableComponent component = player.getGameProfile().name().equalsIgnoreCase(oldName) ? Component.translatable("multiplayer.player.joined", player.getDisplayName()) : Component.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), oldName);
        this.broadcastSystemMessage(component.withStyle(ChatFormatting.YELLOW), false);
        playerConnection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        ServerStatus status = this.server.getStatus();
        if (status != null && !cookie.transferred()) {
            player.sendServerStatus(status);
        }
        player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
        this.sendLevelInfo(player, level);
        level.addNewPlayer(player);
        this.server.getCustomBossEvents().onPlayerConnect(player);
        this.sendActivePlayerEffects(player);
        player.initInventoryMenu();
        this.server.notificationManager().playerJoined(player);
        playerConnection.resumeFlushing();
    }

    protected void updateEntireScoreboard(ServerScoreboard scoreboard, ServerPlayer player) {
        HashSet objectives = Sets.newHashSet();
        for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        }
        for (DisplaySlot slot : DisplaySlot.values()) {
            Objective objective = scoreboard.getDisplayObjective(slot);
            if (objective == null || objectives.contains(objective)) continue;
            List<Packet<?>> packets = scoreboard.getStartTrackingPackets(objective);
            for (Packet<?> packet : packets) {
                player.connection.send(packet);
            }
            objectives.add(objective);
        }
    }

    public void addWorldborderListener(final ServerLevel level) {
        level.getWorldBorder().addListener(new BorderChangeListener(){
            final /* synthetic */ PlayerList this$0;
            {
                PlayerList playerList = this$0;
                Objects.requireNonNull(playerList);
                this.this$0 = playerList;
            }

            @Override
            public void onSetSize(WorldBorder border, double newSize) {
                this.this$0.broadcastAll(new ClientboundSetBorderSizePacket(border), level.dimension());
            }

            @Override
            public void onLerpSize(WorldBorder border, double fromSize, double targetSize, long ticks, long gameTime) {
                this.this$0.broadcastAll(new ClientboundSetBorderLerpSizePacket(border), level.dimension());
            }

            @Override
            public void onSetCenter(WorldBorder border, double x, double z) {
                this.this$0.broadcastAll(new ClientboundSetBorderCenterPacket(border), level.dimension());
            }

            @Override
            public void onSetWarningTime(WorldBorder border, int time) {
                this.this$0.broadcastAll(new ClientboundSetBorderWarningDelayPacket(border), level.dimension());
            }

            @Override
            public void onSetWarningBlocks(WorldBorder border, int blocks) {
                this.this$0.broadcastAll(new ClientboundSetBorderWarningDistancePacket(border), level.dimension());
            }

            @Override
            public void onSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
            }

            @Override
            public void onSetSafeZone(WorldBorder border, double safeZone) {
            }
        });
    }

    public Optional<CompoundTag> loadPlayerData(NameAndId nameAndId) {
        UUID lastSingleplayerOwnerUUID = this.server.getWorldData().getSinglePlayerUUID();
        if (this.server.isSingleplayerOwner(nameAndId) && lastSingleplayerOwnerUUID != null) {
            LOGGER.debug("loading single player");
            return this.playerIo.load(new NameAndId(lastSingleplayerOwnerUUID, "<singleplayer owner>"));
        }
        return this.playerIo.load(nameAndId);
    }

    protected void save(ServerPlayer player) {
        PlayerAdvancements advancements;
        this.playerIo.save(player);
        ServerStatsCounter stats = this.stats.get(player.getUUID());
        if (stats != null) {
            stats.save();
        }
        if ((advancements = this.advancements.get(player.getUUID())) != null) {
            advancements.save();
        }
    }

    public void remove(ServerPlayer player) {
        Object vehicle;
        ServerLevel level = player.level();
        player.awardStat(Stats.LEAVE_GAME);
        this.save(player);
        if (player.isPassenger() && ((Entity)(vehicle = player.getRootVehicle())).hasExactlyOnePlayerPassenger()) {
            LOGGER.debug("Removing player mount");
            player.stopRiding();
            ((Entity)vehicle).getPassengersAndSelf().forEach(e -> e.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
        }
        player.unRide();
        for (ThrownEnderpearl enderpearl : player.getEnderPearls()) {
            enderpearl.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        }
        level.removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        player.getAdvancements().clearTriggers();
        this.players.remove(player);
        this.server.getCustomBossEvents().onPlayerDisconnect(player);
        UUID uuid = player.getUUID();
        ServerPlayer serverPlayer = this.playersByUUID.get(uuid);
        if (serverPlayer == player) {
            this.playersByUUID.remove(uuid);
            this.stats.remove(uuid);
            this.advancements.remove(uuid);
            this.server.notificationManager().playerLeft(player);
        }
        this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
    }

    public @Nullable Component canPlayerLogin(SocketAddress address, NameAndId nameAndId) {
        if (this.bans.isBanned(nameAndId)) {
            UserBanListEntry ban = (UserBanListEntry)this.bans.get(nameAndId);
            MutableComponent reason = Component.translatable("multiplayer.disconnect.banned.reason", ban.getReasonMessage());
            if (ban.getExpires() != null) {
                reason.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(ban.getExpires())));
            }
            return reason;
        }
        if (!this.isWhiteListed(nameAndId)) {
            return Component.translatable("multiplayer.disconnect.not_whitelisted");
        }
        if (this.ipBans.isBanned(address)) {
            IpBanListEntry ban = this.ipBans.get(address);
            MutableComponent reason = Component.translatable("multiplayer.disconnect.banned_ip.reason", ban.getReasonMessage());
            if (ban.getExpires() != null) {
                reason.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ban.getExpires())));
            }
            return reason;
        }
        if (this.players.size() >= this.getMaxPlayers() && !this.canBypassPlayerLimit(nameAndId)) {
            return Component.translatable("multiplayer.disconnect.server_full");
        }
        return null;
    }

    public boolean disconnectAllPlayersWithProfile(UUID playerId) {
        Set dupes = Sets.newIdentityHashSet();
        for (ServerPlayer player : this.players) {
            if (!player.getUUID().equals(playerId)) continue;
            dupes.add(player);
        }
        ServerPlayer serverPlayer = this.playersByUUID.get(playerId);
        if (serverPlayer != null) {
            dupes.add(serverPlayer);
        }
        for (ServerPlayer player : dupes) {
            player.connection.disconnect(DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
        }
        return !dupes.isEmpty();
    }

    public ServerPlayer respawn(ServerPlayer serverPlayer, boolean keepAllPlayerData, Entity.RemovalReason removalReason) {
        BlockPos respawnPosition;
        BlockState blockState;
        LevelData.RespawnData respawnData;
        ServerLevel respawnLevel;
        TeleportTransition respawnInfo = serverPlayer.findRespawnPositionAndUseSpawnBlock(!keepAllPlayerData, TeleportTransition.DO_NOTHING);
        this.players.remove(serverPlayer);
        serverPlayer.level().removePlayerImmediately(serverPlayer, removalReason);
        ServerLevel level = respawnInfo.newLevel();
        ServerPlayer player = new ServerPlayer(this.server, level, serverPlayer.getGameProfile(), serverPlayer.clientInformation());
        player.connection = serverPlayer.connection;
        player.restoreFrom(serverPlayer, keepAllPlayerData);
        player.setId(serverPlayer.getId());
        player.setMainArm(serverPlayer.getMainArm());
        if (!respawnInfo.missingRespawnBlock()) {
            player.copyRespawnPosition(serverPlayer);
        }
        for (String tag : serverPlayer.entityTags()) {
            player.addTag(tag);
        }
        Vec3 pos = respawnInfo.position();
        player.snapTo(pos.x, pos.y, pos.z, respawnInfo.yRot(), respawnInfo.xRot());
        if (respawnInfo.missingRespawnBlock()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0f));
        }
        byte dataToKeep = keepAllPlayerData ? (byte)1 : 0;
        ServerLevel playerLevel = player.level();
        LevelData levelData = playerLevel.getLevelData();
        player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(playerLevel), dataToKeep));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        player.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
        this.sendActivePlayerEffects(player);
        this.sendLevelInfo(player, level);
        this.sendPlayerPermissionLevel(player);
        level.addRespawnedPlayer(player);
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        player.initInventoryMenu();
        player.setHealth(player.getHealth());
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (!keepAllPlayerData && respawnConfig != null && (respawnLevel = this.server.getLevel((respawnData = respawnConfig.respawnData()).dimension())) != null && (blockState = respawnLevel.getBlockState(respawnPosition = respawnData.pos())).is(Blocks.RESPAWN_ANCHOR)) {
            player.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, respawnPosition.getX(), respawnPosition.getY(), respawnPosition.getZ(), 1.0f, 1.0f, level.getRandom().nextLong()));
        }
        return player;
    }

    public void sendActivePlayerEffects(ServerPlayer player) {
        this.sendActiveEffects(player, player.connection);
    }

    public void sendActiveEffects(LivingEntity livingEntity, ServerGamePacketListenerImpl connection) {
        for (MobEffectInstance effect : livingEntity.getActiveEffects()) {
            connection.send(new ClientboundUpdateMobEffectPacket(livingEntity.getId(), effect, false));
        }
    }

    public void sendPlayerPermissionLevel(ServerPlayer player) {
        LevelBasedPermissionSet permissions = this.server.getProfilePermissions(player.nameAndId());
        this.sendPlayerPermissionLevel(player, permissions);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
            this.sendAllPlayerInfoIn = 0;
        }
    }

    public void broadcastAll(Packet<?> packet) {
        for (ServerPlayer player : this.players) {
            player.connection.send(packet);
        }
    }

    public void broadcastAll(Packet<?> packet, ResourceKey<Level> dimension) {
        for (ServerPlayer player : this.players) {
            if (player.level().dimension() != dimension) continue;
            player.connection.send(packet);
        }
    }

    public void broadcastSystemToTeam(Player player, Component message) {
        PlayerTeam team = player.getTeam();
        if (team == null) {
            return;
        }
        Collection<String> teamPlayers = ((Team)team).getPlayers();
        for (String name : teamPlayers) {
            ServerPlayer teamPlayer = this.getPlayerByName(name);
            if (teamPlayer == null || teamPlayer == player) continue;
            teamPlayer.sendSystemMessage(message);
        }
    }

    public void broadcastSystemToAllExceptTeam(Player player, Component message) {
        PlayerTeam team = player.getTeam();
        if (team == null) {
            this.broadcastSystemMessage(message, false);
            return;
        }
        for (int i = 0; i < this.players.size(); ++i) {
            ServerPlayer targetPlayer = this.players.get(i);
            if (targetPlayer.getTeam() == team) continue;
            targetPlayer.sendSystemMessage(message);
        }
    }

    public String[] getPlayerNamesArray() {
        String[] names = new String[this.players.size()];
        for (int i = 0; i < this.players.size(); ++i) {
            names[i] = this.players.get(i).getGameProfile().name();
        }
        return names;
    }

    public UserBanList getBans() {
        return this.bans;
    }

    public IpBanList getIpBans() {
        return this.ipBans;
    }

    public void op(NameAndId nameAndId) {
        this.op(nameAndId, Optional.empty(), Optional.empty());
    }

    public void op(NameAndId nameAndId, Optional<LevelBasedPermissionSet> permissions, Optional<Boolean> canBypassPlayerLimit) {
        this.ops.add(new ServerOpListEntry(nameAndId, permissions.orElse(this.server.operatorUserPermissions()), canBypassPlayerLimit.orElse(this.ops.canBypassPlayerLimit(nameAndId))));
        ServerPlayer player = this.getPlayer(nameAndId.id());
        if (player != null) {
            this.sendPlayerPermissionLevel(player);
        }
    }

    public void deop(NameAndId nameAndId) {
        ServerPlayer player;
        if (this.ops.remove(nameAndId) && (player = this.getPlayer(nameAndId.id())) != null) {
            this.sendPlayerPermissionLevel(player);
        }
    }

    private void sendPlayerPermissionLevel(ServerPlayer player, LevelBasedPermissionSet permissions) {
        if (player.connection != null) {
            byte eventId = switch (permissions.level()) {
                default -> throw new MatchException(null, null);
                case PermissionLevel.ALL -> 24;
                case PermissionLevel.MODERATORS -> 25;
                case PermissionLevel.GAMEMASTERS -> 26;
                case PermissionLevel.ADMINS -> 27;
                case PermissionLevel.OWNERS -> 28;
            };
            player.connection.send(new ClientboundEntityEventPacket(player, eventId));
        }
        this.server.getCommands().sendCommands(player);
    }

    public boolean isWhiteListed(NameAndId nameAndId) {
        return !this.isUsingWhitelist() || this.ops.contains(nameAndId) || this.whitelist.contains(nameAndId);
    }

    public boolean isOp(NameAndId nameAndId) {
        if (this.ops.contains(nameAndId)) {
            return true;
        }
        if (this.server.isSingleplayerOwner(nameAndId)) {
            return this.server.getWorldData().isAllowCommands();
        }
        return this.allowCommandsForAllPlayers;
    }

    public @Nullable ServerPlayer getPlayerByName(String name) {
        int size = this.players.size();
        for (int i = 0; i < size; ++i) {
            ServerPlayer player = this.players.get(i);
            if (!player.getGameProfile().name().equalsIgnoreCase(name)) continue;
            return player;
        }
        return null;
    }

    public void broadcast(@Nullable Player except, double x, double y, double z, double range, ResourceKey<Level> dimension, Packet<?> packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            double zd;
            double yd;
            double xd;
            ServerPlayer player = this.players.get(i);
            if (player == except || player.level().dimension() != dimension || !((xd = x - player.getX()) * xd + (yd = y - player.getY()) * yd + (zd = z - player.getZ()) * zd < range * range)) continue;
            player.connection.send(packet);
        }
    }

    public void saveAll() {
        for (int i = 0; i < this.players.size(); ++i) {
            this.save(this.players.get(i));
        }
    }

    public UserWhiteList getWhiteList() {
        return this.whitelist;
    }

    public String[] getWhiteListNames() {
        return this.whitelist.getUserList();
    }

    public ServerOpList getOps() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getUserList();
    }

    public void reloadWhiteList() {
    }

    public void sendLevelInfo(ServerPlayer player, ServerLevel level) {
        WorldBorder worldBorder = level.getWorldBorder();
        player.connection.send(new ClientboundInitializeBorderPacket(worldBorder));
        player.connection.send(this.server.clockManager().createFullSyncPacket());
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        if (level.isRaining()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0f));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0f)));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0f)));
        }
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0f));
        this.server.tickRateManager().updateJoiningPlayer(player);
    }

    public void sendAllPlayerInfo(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        player.resetSentInfo();
        player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.server.getMaxPlayers();
    }

    public boolean isUsingWhitelist() {
        return this.server.isUsingWhitelist();
    }

    public List<ServerPlayer> getPlayersWithAddress(String ip) {
        ArrayList result = Lists.newArrayList();
        for (ServerPlayer player : this.players) {
            if (!player.getIpAddress().equals(ip)) continue;
            result.add(player);
        }
        return result;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public int getSimulationDistance() {
        return this.simulationDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public void setAllowCommandsForAllPlayers(boolean allowCommands) {
        this.allowCommandsForAllPlayers = allowCommands;
    }

    public void removeAll() {
        for (int i = 0; i < this.players.size(); ++i) {
            this.players.get((int)i).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
        }
    }

    public void broadcastSystemMessage(Component message, boolean overlay) {
        this.broadcastSystemMessage(message, player -> message, overlay);
    }

    public void broadcastSystemMessage(Component message, Function<ServerPlayer, Component> playerMessages, boolean overlay) {
        this.server.sendSystemMessage(message);
        for (ServerPlayer player : this.players) {
            Component playerMessage = playerMessages.apply(player);
            if (playerMessage == null) continue;
            player.sendSystemMessage(playerMessage, overlay);
        }
    }

    public void broadcastChatMessage(PlayerChatMessage message, CommandSourceStack sender, ChatType.Bound chatType) {
        this.broadcastChatMessage(message, sender::shouldFilterMessageTo, sender.getPlayer(), chatType);
    }

    public void broadcastChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound chatType) {
        this.broadcastChatMessage(message, sender::shouldFilterMessageTo, sender, chatType);
    }

    private void broadcastChatMessage(PlayerChatMessage message, Predicate<ServerPlayer> isFiltered, @Nullable ServerPlayer senderPlayer, ChatType.Bound chatType) {
        boolean trusted = this.verifyChatTrusted(message);
        this.server.logChatMessage(message.decoratedContent(), chatType, trusted ? null : "Not Secure");
        OutgoingChatMessage tracked = OutgoingChatMessage.create(message);
        boolean wasFullyFiltered = false;
        for (ServerPlayer player : this.players) {
            boolean filtered = isFiltered.test(player);
            player.sendChatMessage(tracked, filtered, chatType);
            wasFullyFiltered |= filtered && message.isFullyFiltered();
        }
        if (wasFullyFiltered && senderPlayer != null) {
            senderPlayer.sendSystemMessage(CHAT_FILTERED_FULL);
        }
    }

    private boolean verifyChatTrusted(PlayerChatMessage message) {
        return message.hasSignature() && !message.hasExpiredServer(Instant.now());
    }

    public ServerStatsCounter getPlayerStats(Player player) {
        GameProfile gameProfile = player.getGameProfile();
        return this.stats.computeIfAbsent(gameProfile.id(), id -> {
            Path targetFile = this.locateStatsFile(gameProfile);
            return new ServerStatsCounter(this.server, targetFile);
        });
    }

    private Path locateStatsFile(GameProfile gameProfile) {
        Path playerNameStatsPath;
        Path statFolder = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Path uuidStatsFile = statFolder.resolve(String.valueOf(gameProfile.id()) + ".json");
        if (Files.exists(uuidStatsFile, new LinkOption[0])) {
            return uuidStatsFile;
        }
        String playerNameStatsFile = gameProfile.name() + ".json";
        if (FileUtil.isValidPathSegment(playerNameStatsFile) && Files.isRegularFile(playerNameStatsPath = statFolder.resolve(playerNameStatsFile), new LinkOption[0])) {
            try {
                return Files.move(playerNameStatsPath, uuidStatsFile, new CopyOption[0]);
            }
            catch (IOException e) {
                LOGGER.warn("Failed to copy file {} to {}", (Object)playerNameStatsFile, (Object)uuidStatsFile);
                return playerNameStatsPath;
            }
        }
        return uuidStatsFile;
    }

    public PlayerAdvancements getPlayerAdvancements(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerAdvancements result = this.advancements.get(uuid);
        if (result == null) {
            Path uuidStatsFile = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(String.valueOf(uuid) + ".json");
            result = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), uuidStatsFile, player);
            this.advancements.put(uuid, result);
        }
        result.setPlayer(player);
        return result;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(viewDistance));
        for (ServerLevel level : this.server.getAllLevels()) {
            level.getChunkSource().setViewDistance(viewDistance);
        }
    }

    public void setSimulationDistance(int simulationDistance) {
        this.simulationDistance = simulationDistance;
        this.broadcastAll(new ClientboundSetSimulationDistancePacket(simulationDistance));
        for (ServerLevel level : this.server.getAllLevels()) {
            level.getChunkSource().setSimulationDistance(simulationDistance);
        }
    }

    public List<ServerPlayer> getPlayers() {
        return this.players;
    }

    public Map<UUID, ServerPlayer> getPlayersByUUID() {
        return this.playersByUUID;
    }

    public @Nullable ServerPlayer getPlayer(UUID uuid) {
        return this.playersByUUID.get(uuid);
    }

    public @Nullable ServerPlayer getPlayer(String playerName) {
        for (ServerPlayer player : this.players) {
            if (!player.getGameProfile().name().equalsIgnoreCase(playerName)) continue;
            return player;
        }
        return null;
    }

    public boolean canBypassPlayerLimit(NameAndId nameAndId) {
        return false;
    }

    public void reloadResources() {
        for (PlayerAdvancements advancements : this.advancements.values()) {
            advancements.reload(this.server.getAdvancements());
        }
        this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
        RecipeManager recipeManager = this.server.getRecipeManager();
        ClientboundUpdateRecipesPacket recipes = new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), recipeManager.getSynchronizedStonecutterRecipes());
        for (ServerPlayer player : this.players) {
            player.connection.send(recipes);
            player.getRecipeBook().sendInitialRecipeBook(player);
        }
    }

    public boolean isAllowCommandsForAllPlayers() {
        return this.allowCommandsForAllPlayers;
    }
}

