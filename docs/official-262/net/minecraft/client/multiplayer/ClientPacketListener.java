/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.google.common.hash.HashCode
 *  com.mojang.authlib.GameProfile
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.ClientClockManager;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.DebugQueryHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.NautilusInventoryScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import net.minecraft.client.gui.screens.options.HasDifficultyReaction;
import net.minecraft.client.gui.screens.options.InWorldGameRulesScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ChunkBatchSizeCalculator;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientDebugSubscriber;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientRecipeContainer;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.PingDebugMonitor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.BeeAggressiveSoundInstance;
import net.minecraft.client.resources.sounds.BeeFlyingSoundInstance;
import net.minecraft.client.resources.sounds.BeeSoundInstance;
import net.minecraft.client.resources.sounds.GuardianAttackSoundInstance;
import net.minecraft.client.resources.sounds.MinecartSoundInstance;
import net.minecraft.client.resources.sounds.SnifferSoundInstance;
import net.minecraft.client.waypoints.ClientWaypointManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.chat.SignedMessageLink;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundLowDiskSpaceWarningPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMountScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Crypt;
import net.minecraft.util.HashOps;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.NautilusInventoryMenu;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientPacketListener
extends ClientCommonPacketListenerImpl
implements ClientGamePacketListener,
TickablePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component UNSECURE_SERVER_TOAST_TITLE = Component.translatable("multiplayer.unsecureserver.toast.title");
    private static final Component UNSERURE_SERVER_TOAST = Component.translatable("multiplayer.unsecureserver.toast");
    private static final Component INVALID_PACKET = Component.translatable("multiplayer.disconnect.invalid_packet");
    private static final Component RECONFIGURE_SCREEN_MESSAGE = Component.translatable("connect.reconfiguring");
    private static final Component BAD_CHAT_INDEX = Component.translatable("multiplayer.disconnect.bad_chat_index");
    private static final Component COMMAND_SEND_CONFIRM_TITLE = Component.translatable("multiplayer.confirm_command.title");
    private static final Component BUTTON_RUN_COMMAND = Component.translatable("multiplayer.confirm_command.run_command");
    private static final Component BUTTON_SUGGEST_COMMAND = Component.translatable("multiplayer.confirm_command.suggest_command");
    private static final int PENDING_OFFSET_THRESHOLD = 64;
    public static final int TELEPORT_INTERPOLATION_THRESHOLD = 64;
    private static final Permission RESTRICTED_COMMAND = Permission.Atom.create("client/commands/restricted");
    private static final PermissionCheck RESTRICTED_COMMAND_CHECK = new PermissionCheck.Require(RESTRICTED_COMMAND);
    private static final PermissionSet ALLOW_RESTRICTED_COMMANDS = permission -> permission.equals(RESTRICTED_COMMAND);
    private static final ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> COMMAND_NODE_BUILDER = new ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider>(){

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> createLiteral(String id) {
            return LiteralArgumentBuilder.literal((String)id);
        }

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> createArgument(String id, ArgumentType<?> argumentType, @Nullable Identifier suggestionId) {
            RequiredArgumentBuilder builder = RequiredArgumentBuilder.argument((String)id, argumentType);
            if (suggestionId != null) {
                builder.suggests(SuggestionProviders.getProvider(suggestionId));
            }
            return builder;
        }

        @Override
        public ArgumentBuilder<ClientSuggestionProvider, ?> configure(ArgumentBuilder<ClientSuggestionProvider, ?> builder, boolean executable, boolean restricted) {
            if (executable) {
                builder.executes(c -> 0);
            }
            if (restricted) {
                builder.requires(Commands.hasPermission(RESTRICTED_COMMAND_CHECK));
            }
            return builder;
        }
    };
    private final GameProfile localGameProfile;
    private ClientLevel level;
    private ClientLevel.ClientLevelData levelData;
    private final Map<UUID, PlayerInfo> playerInfoMap = Maps.newHashMap();
    private final Set<PlayerInfo> listedPlayers = new ReferenceOpenHashSet();
    private final ClientAdvancements advancements;
    private final ClientSuggestionProvider suggestionsProvider;
    private final ClientSuggestionProvider restrictedSuggestionsProvider;
    private final DebugQueryHandler debugQueryHandler = new DebugQueryHandler(this);
    private int serverChunkRadius = 3;
    private int serverSimulationDistance = 3;
    private final RandomSource random = RandomSource.createThreadSafe();
    private CommandDispatcher<ClientSuggestionProvider> commands = new CommandDispatcher();
    private ClientRecipeContainer recipes = new ClientRecipeContainer(Map.of(), SelectableRecipe.SingleInputSet.empty());
    private Set<ResourceKey<Level>> levels;
    private final RegistryAccess.Frozen registryAccess;
    private final FeatureFlagSet enabledFeatures;
    private final PotionBrewing potionBrewing;
    private FuelValues fuelValues;
    private final HashedPatchMap.HashGenerator decoratedHashOpsGenerator;
    private OptionalInt removedPlayerVehicleId = OptionalInt.empty();
    private @Nullable LocalChatSession chatSession;
    private SignedMessageChain.Encoder signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
    private int nextChatIndex;
    private LastSeenMessagesTracker lastSeenMessages = new LastSeenMessagesTracker(20);
    private MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
    private @Nullable CompletableFuture<Optional<ProfileKeyPair>> keyPairFuture;
    private @Nullable ClientInformation remoteClientInformation;
    private final ChunkBatchSizeCalculator chunkBatchSizeCalculator = new ChunkBatchSizeCalculator();
    private final PingDebugMonitor pingDebugMonitor;
    private final ClientDebugSubscriber debugSubscriber;
    private @Nullable LevelLoadTracker levelLoadTracker;
    private boolean serverEnforcesSecureChat;
    private boolean onlineMode;
    private volatile boolean closed;
    private final Scoreboard scoreboard = new Scoreboard();
    private final ClientWaypointManager waypointManager = new ClientWaypointManager();
    private final ClientClockManager clockManager;
    private final SessionSearchTrees searchTrees = new SessionSearchTrees();
    private final List<WeakReference<CacheSlot<?, ?>>> cacheSlots = new ArrayList();
    private boolean clientLoaded;

    public ClientPacketListener(Minecraft minecraft, Connection connection, CommonListenerCookie cookie) {
        super(minecraft, connection, cookie);
        this.localGameProfile = cookie.localGameProfile();
        this.registryAccess = cookie.receivedRegistries();
        RegistryOps<HashCode> hashOps = this.registryAccess.createSerializationContext(HashOps.CRC32C_INSTANCE);
        this.decoratedHashOpsGenerator = component -> ((HashCode)component.encodeValue(hashOps).getOrThrow(msg -> new IllegalArgumentException("Failed to hash " + String.valueOf(component) + ": " + msg))).asInt();
        this.enabledFeatures = cookie.enabledFeatures();
        this.advancements = new ClientAdvancements(minecraft, this.telemetryManager);
        PermissionSet playerPermissions = permission -> {
            LocalPlayer player = minecraft.player;
            return player != null && player.permissions().hasPermission(permission);
        };
        this.suggestionsProvider = new ClientSuggestionProvider(this, minecraft, playerPermissions.union(ALLOW_RESTRICTED_COMMANDS));
        this.restrictedSuggestionsProvider = new ClientSuggestionProvider(this, minecraft, PermissionSet.NO_PERMISSIONS);
        this.pingDebugMonitor = new PingDebugMonitor(this, minecraft.getDebugOverlay().getPingLogger());
        this.debugSubscriber = new ClientDebugSubscriber(this, minecraft.getDebugOverlay());
        if (cookie.chatState() != null) {
            minecraft.gui.hud.getChat().restoreState(cookie.chatState());
        }
        this.potionBrewing = PotionBrewing.bootstrap(this.enabledFeatures);
        this.fuelValues = FuelValues.vanillaBurnTimes(cookie.receivedRegistries(), this.enabledFeatures);
        this.levelLoadTracker = cookie.levelLoadTracker();
        this.clockManager = new ClientClockManager();
    }

    public ClientSuggestionProvider getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    public void close() {
        this.closed = true;
        this.clearLevel();
        this.telemetryManager.onDisconnect();
    }

    public void clearLevel() {
        this.clearCacheSlots();
        this.level = null;
        this.levelLoadTracker = null;
    }

    private void clearCacheSlots() {
        for (WeakReference<CacheSlot<?, ?>> cacheSlot : this.cacheSlots) {
            CacheSlot slot = (CacheSlot)cacheSlot.get();
            if (slot == null) continue;
            slot.clear();
        }
        this.cacheSlots.clear();
    }

    public RecipeAccess recipes() {
        return this.recipes;
    }

    @Override
    public void handleLogin(ClientboundLoginPacket packet) {
        ClientLevel.ClientLevelData levelData;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gameMode = new MultiPlayerGameMode(this.minecraft, this);
        CommonPlayerSpawnInfo spawnInfo = packet.commonPlayerSpawnInfo();
        ArrayList levels = Lists.newArrayList(packet.levels());
        Collections.shuffle(levels);
        this.levels = Sets.newLinkedHashSet((Iterable)levels);
        ResourceKey<Level> dimension = spawnInfo.dimension();
        Holder<DimensionType> dimensionType = spawnInfo.dimensionType();
        this.serverChunkRadius = packet.chunkRadius();
        this.serverSimulationDistance = packet.simulationDistance();
        boolean isDebug = spawnInfo.isDebug();
        boolean isFlat = spawnInfo.isFlat();
        int seaLevel = spawnInfo.seaLevel();
        this.levelData = levelData = new ClientLevel.ClientLevelData(Difficulty.NORMAL, packet.hardcore(), isFlat);
        this.level = new ClientLevel(this, levelData, dimension, dimensionType, this.serverChunkRadius, this.serverSimulationDistance, this.minecraft.levelExtractor, isDebug, spawnInfo.seed(), seaLevel);
        this.minecraft.setLevel(this.level);
        if (this.minecraft.player == null) {
            this.minecraft.player = this.minecraft.gameMode.createPlayer(this.level, new StatsCounter(), new ClientRecipeBook());
            this.minecraft.player.setYRot(-180.0f);
            if (this.minecraft.getSingleplayerServer() != null) {
                this.minecraft.getSingleplayerServer().setUUID(this.minecraft.player.getUUID());
            }
        }
        this.setClientLoaded(false);
        this.debugSubscriber.clear();
        this.minecraft.levelExtractor.debugRenderer.refreshRendererList();
        this.minecraft.player.resetPos();
        this.minecraft.player.setId(packet.playerId());
        this.level.addEntity(this.minecraft.player);
        this.minecraft.player.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(this.minecraft.player);
        this.minecraft.setCameraEntity(this.minecraft.player);
        this.startWaitingForNewLevel(this.minecraft.player, this.level, LevelLoadingScreen.Reason.OTHER);
        this.minecraft.player.setReducedDebugInfo(packet.reducedDebugInfo());
        this.minecraft.player.setShowDeathScreen(packet.showDeathScreen());
        this.minecraft.player.setDoLimitedCrafting(packet.doLimitedCrafting());
        this.minecraft.player.setLastDeathLocation(spawnInfo.lastDeathLocation());
        this.minecraft.player.setPortalCooldown(spawnInfo.portalCooldown());
        this.minecraft.gameMode.setLocalMode(spawnInfo.gameType(), spawnInfo.previousGameType());
        this.minecraft.options.setServerRenderDistance(packet.chunkRadius());
        this.chatSession = null;
        this.signedMessageEncoder = SignedMessageChain.Encoder.UNSIGNED;
        this.nextChatIndex = 0;
        this.lastSeenMessages = new LastSeenMessagesTracker(20);
        this.messageSignatureCache = MessageSignatureCache.createDefault();
        this.onlineMode = packet.onlineMode();
        if (packet.onlineMode()) {
            this.prepareKeyPair();
        }
        this.telemetryManager.onPlayerInfoReceived(spawnInfo.gameType(), packet.hardcore());
        this.minecraft.quickPlayLog().log(this.minecraft);
        this.serverEnforcesSecureChat = packet.enforcesSecureChat();
        if (this.serverData != null && !this.seenInsecureChatWarning && !this.enforcesSecureChat()) {
            SystemToast toast = new SystemToast(SystemToast.SystemToastId.UNSECURE_SERVER_WARNING, UNSECURE_SERVER_TOAST_TITLE, UNSERURE_SERVER_TOAST);
            this.minecraft.gui.toastManager().addToast(toast);
            this.seenInsecureChatWarning = true;
        }
    }

    @Override
    public void handleAddEntity(ClientboundAddEntityPacket packet) {
        Player player;
        UUID uuid;
        PlayerInfo playerInfo;
        Entity entity;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == packet.getId()) {
            this.removedPlayerVehicleId = OptionalInt.empty();
        }
        if ((entity = this.createEntityFromPacket(packet)) != null) {
            entity.recreateFromPacket(packet);
            this.level.addEntity(entity);
            this.postAddEntitySoundInstance(entity);
        } else {
            LOGGER.warn("Skipping Entity with id {}", packet.getType());
        }
        if (entity instanceof Player && (playerInfo = this.playerInfoMap.get(uuid = (player = (Player)entity).getUUID())) != null) {
            this.seenPlayers.put(uuid, playerInfo);
        }
    }

    private @Nullable Entity createEntityFromPacket(ClientboundAddEntityPacket packet) {
        EntityType<?> type = packet.getType();
        if (type == EntityTypes.PLAYER) {
            PlayerInfo playerInfo = this.getPlayerInfo(packet.getUUID());
            if (playerInfo == null) {
                LOGGER.warn("Server attempted to add player prior to sending player info (Player id {})", (Object)packet.getUUID());
                return null;
            }
            return new RemotePlayer(this.level, playerInfo.getProfile());
        }
        return type.create((Level)this.level, EntitySpawnReason.LOAD);
    }

    private void postAddEntitySoundInstance(Entity entity) {
        if (entity instanceof AbstractMinecart) {
            AbstractMinecart minecart = (AbstractMinecart)entity;
            this.minecraft.getSoundManager().play(new MinecartSoundInstance(minecart));
        } else if (entity instanceof Bee) {
            Bee bee = (Bee)entity;
            boolean angry = bee.isAngry();
            BeeSoundInstance soundInstance = angry ? new BeeAggressiveSoundInstance(bee) : new BeeFlyingSoundInstance(bee);
            this.minecraft.getSoundManager().queueTickingSound(soundInstance);
        }
    }

    @Override
    public void handleSetEntityMotion(ClientboundSetEntityMotionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity == null) {
            return;
        }
        entity.lerpMotion(packet.movement());
    }

    @Override
    public void handleSetEntityData(ClientboundSetEntityDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity != null) {
            entity.getEntityData().assignValues(packet.packedItems());
        }
    }

    @Override
    public void handleEntityPositionSync(ClientboundEntityPositionSyncPacket packet) {
        boolean tooBigToInterpolate;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity == null) {
            return;
        }
        Vec3 pos = packet.values().position();
        entity.getPositionCodec().setBase(pos);
        if (entity.isLocalInstanceAuthoritative()) {
            return;
        }
        float yRot = packet.values().yRot();
        float xRot = packet.values().xRot();
        boolean bl = tooBigToInterpolate = entity.position().distanceToSqr(pos) > 4096.0;
        if (this.level.isTickingEntity(entity) && !tooBigToInterpolate) {
            entity.moveOrInterpolateTo(pos, yRot, xRot);
        } else {
            entity.snapTo(pos, yRot, xRot);
        }
        if (!entity.isInterpolating() && entity.hasIndirectPassenger(this.minecraft.player)) {
            entity.positionRider(this.minecraft.player);
            this.minecraft.player.setOldPosAndRot();
        }
        entity.setOnGround(packet.onGround());
    }

    @Override
    public void handleTeleportEntity(ClientboundTeleportEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity == null) {
            if (this.removedPlayerVehicleId.isPresent() && this.removedPlayerVehicleId.getAsInt() == packet.id()) {
                LOGGER.debug("Trying to teleport entity with id {}, that was formerly player vehicle, applying teleport to player instead", (Object)packet.id());
                ClientPacketListener.setValuesFromPositionPacket(packet.change(), packet.relatives(), this.minecraft.player, false);
                this.connection.send(new ServerboundMovePlayerPacket.PosRot(this.minecraft.player.getX(), this.minecraft.player.getY(), this.minecraft.player.getZ(), this.minecraft.player.getYRot(), this.minecraft.player.getXRot(), false, false));
            }
            return;
        }
        boolean hasRelative = packet.relatives().contains((Object)Relative.X) || packet.relatives().contains((Object)Relative.Y) || packet.relatives().contains((Object)Relative.Z);
        boolean interpolate = this.level.isTickingEntity(entity) || !entity.isLocalInstanceAuthoritative() || hasRelative;
        boolean wasInterpolated = ClientPacketListener.setValuesFromPositionPacket(packet.change(), packet.relatives(), entity, interpolate);
        entity.setOnGround(packet.onGround());
        if (!wasInterpolated && entity.hasIndirectPassenger(this.minecraft.player)) {
            entity.positionRider(this.minecraft.player);
            this.minecraft.player.setOldPosAndRot();
            if (entity.isLocalInstanceAuthoritative()) {
                this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
            }
        }
    }

    @Override
    public void handleTickingState(ClientboundTickingStatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.minecraft.level == null) {
            return;
        }
        TickRateManager manager = this.minecraft.level.tickRateManager();
        manager.setTickRate(packet.tickRate());
        manager.setFrozen(packet.isFrozen());
    }

    @Override
    public void handleTickingStep(ClientboundTickingStepPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.minecraft.level == null) {
            return;
        }
        TickRateManager manager = this.minecraft.level.tickRateManager();
        manager.setFrozenTicksToRun(packet.tickSteps());
    }

    @Override
    public void handleSetHeldSlot(ClientboundSetHeldSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (Inventory.isHotbarSlot(packet.slot())) {
            this.minecraft.player.getInventory().setSelectedSlot(packet.slot());
        }
    }

    @Override
    public void handleMoveEntity(ClientboundMoveEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity == null) {
            return;
        }
        if (entity.isLocalInstanceAuthoritative()) {
            VecDeltaCodec positionCodec = entity.getPositionCodec();
            Vec3 pos = positionCodec.decode(packet.getXa(), packet.getYa(), packet.getZa());
            positionCodec.setBase(pos);
            return;
        }
        if (packet.hasPosition()) {
            VecDeltaCodec positionCodec = entity.getPositionCodec();
            Vec3 pos = positionCodec.decode(packet.getXa(), packet.getYa(), packet.getZa());
            positionCodec.setBase(pos);
            if (packet.hasRotation()) {
                entity.moveOrInterpolateTo(pos, packet.getYRot(), packet.getXRot());
            } else {
                entity.moveOrInterpolateTo(pos);
            }
        } else if (packet.hasRotation()) {
            entity.moveOrInterpolateTo(packet.getYRot(), packet.getXRot());
        }
        entity.setOnGround(packet.isOnGround());
    }

    @Override
    public void handleMinecartAlongTrack(ClientboundMoveMinecartPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (!(entity instanceof AbstractMinecart)) {
            return;
        }
        AbstractMinecart minecart = (AbstractMinecart)entity;
        MinecartBehavior minecartBehavior = minecart.getBehavior();
        if (minecartBehavior instanceof NewMinecartBehavior) {
            NewMinecartBehavior newMinecartBehavior = (NewMinecartBehavior)minecartBehavior;
            newMinecartBehavior.lerpSteps.addAll(packet.lerpSteps());
        }
    }

    @Override
    public void handleRotateMob(ClientboundRotateHeadPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity == null) {
            return;
        }
        entity.lerpHeadTo(packet.getYHeadRot(), 3);
    }

    @Override
    public void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.getEntityIds().forEach(entityId -> {
            Entity entity = this.level.getEntity(entityId);
            if (entity == null) {
                return;
            }
            if (entity.hasIndirectPassenger(this.minecraft.player)) {
                LOGGER.debug("Remove entity {}:{} that has player as passenger", (Object)entity.typeHolder().getRegisteredName(), (Object)entityId);
                this.removedPlayerVehicleId = OptionalInt.of(entityId);
            }
            this.level.removeEntity(entityId, Entity.RemovalReason.DISCARDED);
            this.debugSubscriber.dropEntity(entity);
        });
    }

    @Override
    public void handleMovePlayer(ClientboundPlayerPositionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = this.minecraft.player;
        if (!player.isPassenger()) {
            ClientPacketListener.setValuesFromPositionPacket(packet.change(), packet.relatives(), player, false);
        }
        this.connection.send(new ServerboundAcceptTeleportationPacket(packet.id()));
        this.connection.send(new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), false, false));
        this.minecraft.level.getBlockStatePredictionHandler().onTeleport();
    }

    private static boolean setValuesFromPositionPacket(PositionMoveRotation change, Set<Relative> relatives, Entity entity, boolean interpolate) {
        boolean tooBigToInterpolate;
        PositionMoveRotation currentValues = PositionMoveRotation.of(entity);
        PositionMoveRotation newValues = PositionMoveRotation.calculateAbsolute(currentValues, change, relatives);
        boolean bl = tooBigToInterpolate = currentValues.position().distanceToSqr(newValues.position()) > 4096.0;
        if (interpolate && !tooBigToInterpolate) {
            entity.moveOrInterpolateTo(newValues.position(), newValues.yRot(), newValues.xRot());
            entity.setDeltaMovement(newValues.deltaMovement());
            return true;
        }
        entity.setPos(newValues.position());
        entity.setDeltaMovement(newValues.deltaMovement());
        entity.setYRot(newValues.yRot());
        entity.setXRot(newValues.xRot());
        PositionMoveRotation currentInterpolationValues = new PositionMoveRotation(entity.oldPosition(), Vec3.ZERO, entity.yRotO, entity.xRotO);
        PositionMoveRotation interpolationValues = PositionMoveRotation.calculateAbsolute(currentInterpolationValues, change, relatives);
        entity.setOldPosAndRot(interpolationValues.position(), interpolationValues.yRot(), interpolationValues.xRot());
        return false;
    }

    @Override
    public void handleRotatePlayer(ClientboundPlayerRotationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = this.minecraft.player;
        Set<Relative> relatives = Relative.rotation(packet.relativeY(), packet.relativeX());
        PositionMoveRotation currentValues = PositionMoveRotation.of(player);
        PositionMoveRotation newValues = PositionMoveRotation.calculateAbsolute(currentValues, currentValues.withRotation(packet.yRot(), packet.xRot()), relatives);
        player.setYRot(newValues.yRot());
        player.setXRot(newValues.xRot());
        player.setOldRot();
        this.connection.send(new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), false, false));
    }

    @Override
    public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.runUpdates((pos, state) -> this.level.setServerVerifiedBlockState((BlockPos)pos, (BlockState)state, 19));
    }

    @Override
    public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        int x = packet.getX();
        int z = packet.getZ();
        this.updateLevelChunk(x, z, packet.getChunkData());
        ClientboundLightUpdatePacketData lightData = packet.getLightData();
        this.level.queueLightUpdate(() -> {
            this.applyLightData(x, z, lightData, false);
            LevelChunk chunk = this.level.getChunkSource().getChunk(x, z, false);
            if (chunk != null) {
                this.enableChunkLight(chunk, x, z);
            }
        });
    }

    @Override
    public void handleChunksBiomes(ClientboundChunksBiomesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        for (ClientboundChunksBiomesPacket.ChunkBiomeData data : packet.chunkBiomeData()) {
            this.level.getChunkSource().replaceBiomes(data.pos().x(), data.pos().z(), data.getReadBuffer());
        }
        for (ClientboundChunksBiomesPacket.ChunkBiomeData data : packet.chunkBiomeData()) {
            this.level.onChunkLoaded(new ChunkPos(data.pos().x(), data.pos().z()));
        }
        for (ClientboundChunksBiomesPacket.ChunkBiomeData data : packet.chunkBiomeData()) {
            for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                for (int zOffset = -1; zOffset <= 1; ++zOffset) {
                    for (int y = this.level.getMinSectionY(); y <= this.level.getMaxSectionY(); ++y) {
                        this.minecraft.levelExtractor.setSectionDirty(data.pos().x() + xOffset, y, data.pos().z() + zOffset);
                    }
                }
            }
        }
    }

    private void updateLevelChunk(int x, int z, ClientboundLevelChunkPacketData chunkData) {
        this.level.getChunkSource().replaceWithPacketData(x, z, chunkData.getReadBuffer(), chunkData.getHeightmaps(), chunkData.getBlockEntitiesTagsConsumer(x, z));
    }

    private void enableChunkLight(LevelChunk chunk, int x, int z) {
        LevelLightEngine lightEngine = this.level.getChunkSource().getLightEngine();
        LevelChunkSection[] sections = chunk.getSections();
        ChunkPos chunkPos = chunk.getPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; ++sectionIndex) {
            LevelChunkSection section = sections[sectionIndex];
            int sectionY = this.level.getSectionYFromSectionIndex(sectionIndex);
            lightEngine.updateSectionStatus(SectionPos.of(chunkPos, sectionY), section.hasOnlyAir());
        }
        this.level.setSectionRangeDirty(x - 1, this.level.getMinSectionY(), z - 1, x + 1, this.level.getMaxSectionY(), z + 1);
    }

    @Override
    public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getChunkSource().drop(packet.pos());
        this.debugSubscriber.dropChunk(packet.pos());
        this.queueLightRemoval(packet);
    }

    private void queueLightRemoval(ClientboundForgetLevelChunkPacket packet) {
        ChunkPos chunkPos = packet.pos();
        this.level.queueLightUpdate(() -> {
            int sectionY;
            LevelLightEngine lightEngine = this.level.getLightEngine();
            lightEngine.setLightEnabled(chunkPos, false);
            for (sectionY = lightEngine.getMinLightSection(); sectionY < lightEngine.getMaxLightSection(); ++sectionY) {
                SectionPos sectionPos = SectionPos.of(chunkPos, sectionY);
                lightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, null);
                lightEngine.queueSectionData(LightLayer.SKY, sectionPos, null);
            }
            for (sectionY = this.level.getMinSectionY(); sectionY <= this.level.getMaxSectionY(); ++sectionY) {
                lightEngine.updateSectionStatus(SectionPos.of(chunkPos, sectionY), true);
            }
        });
    }

    @Override
    public void handleBlockUpdate(ClientboundBlockUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.setServerVerifiedBlockState(packet.getPos(), packet.getBlockState(), 19);
    }

    @Override
    public void handleConfigurationStart(ClientboundStartConfigurationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.chatListener().flushQueue();
        this.sendChatAcknowledgement();
        ChatComponent.State chatState = this.minecraft.gui.hud.getChat().storeState();
        this.minecraft.clearClientLevel(new ServerReconfigScreen(RECONFIGURE_SCREEN_MESSAGE, this.connection));
        this.connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, new ClientConfigurationPacketListenerImpl(this.minecraft, this.connection, new CommonListenerCookie(new LevelLoadTracker(), this.localGameProfile, this.telemetryManager, this.registryAccess, this.enabledFeatures, this.serverBrand, this.serverData, this.postDisconnectScreen, this.serverCookies, chatState, this.customReportDetails, this.serverLinks(), this.seenPlayers, this.seenInsecureChatWarning)));
        this.send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
    }

    @Override
    public void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity from = this.level.getEntity(packet.getItemId());
        LivingEntity to = (LivingEntity)this.level.getEntity(packet.getPlayerId());
        if (to == null) {
            to = this.minecraft.player;
        }
        if (from != null) {
            if (from instanceof ExperienceOrb) {
                this.level.playLocalSound(from.getX(), from.getY(), from.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1f, (this.random.nextFloat() - this.random.nextFloat()) * 0.35f + 0.9f, false);
            } else {
                this.level.playLocalSound(from.getX(), from.getY(), from.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, (this.random.nextFloat() - this.random.nextFloat()) * 1.4f + 2.0f, false);
            }
            EntityRenderState itemState = this.minecraft.getEntityRenderDispatcher().extractEntity(from, 1.0f);
            this.minecraft.particleEngine.add(new ItemPickupParticle(this.level, itemState, to, from.getDeltaMovement()));
            if (from instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity)from;
                ItemStack itemStack = itemEntity.getItem();
                if (!itemStack.isEmpty()) {
                    itemStack.shrink(packet.getAmount());
                }
                if (itemStack.isEmpty()) {
                    this.level.removeEntity(packet.getItemId(), Entity.RemovalReason.DISCARDED);
                }
            } else if (!(from instanceof ExperienceOrb)) {
                this.level.removeEntity(packet.getItemId(), Entity.RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void handleSystemChat(ClientboundSystemChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.overlay()) {
            this.minecraft.gui.chatListener().handleOverlay(packet.content());
        } else {
            this.minecraft.gui.chatListener().handleSystemMessage(packet.content(), true);
        }
    }

    @Override
    public void handlePlayerChat(ClientboundPlayerChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        int expectedChatIndex = this.nextChatIndex++;
        if (packet.globalIndex() != expectedChatIndex) {
            LOGGER.error("Missing or out-of-order chat message from server, expected index {} but got {}", (Object)expectedChatIndex, (Object)packet.globalIndex());
            this.connection.disconnect(BAD_CHAT_INDEX);
            return;
        }
        Optional<SignedMessageBody> body = packet.body().unpack(this.messageSignatureCache);
        if (body.isEmpty()) {
            LOGGER.error("Message from player with ID {} referenced unrecognized signature id", (Object)packet.sender());
            this.connection.disconnect(INVALID_PACKET);
            return;
        }
        this.messageSignatureCache.push(body.get(), packet.signature());
        UUID senderId = packet.sender();
        PlayerInfo sender = this.getPlayerInfo(senderId);
        if (sender == null) {
            LOGGER.error("Received player chat packet for unknown player with ID: {}", (Object)senderId);
            this.minecraft.gui.chatListener().handleChatMessageError(senderId, packet.signature(), packet.chatType());
            return;
        }
        RemoteChatSession chatSession = sender.getChatSession();
        SignedMessageLink link = chatSession != null ? new SignedMessageLink(packet.index(), senderId, chatSession.sessionId()) : SignedMessageLink.unsigned(senderId);
        PlayerChatMessage message = new PlayerChatMessage(link, packet.signature(), body.get(), packet.unsignedContent(), packet.filterMask());
        message = sender.getMessageValidator().updateAndValidate(message);
        if (message != null) {
            this.minecraft.gui.chatListener().handlePlayerChatMessage(message, sender.getProfile(), packet.chatType());
        } else {
            this.minecraft.gui.chatListener().handleChatMessageError(senderId, packet.signature(), packet.chatType());
        }
    }

    @Override
    public void handleDisguisedChat(ClientboundDisguisedChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.chatListener().handleDisguisedChatMessage(packet.message(), packet.chatType());
    }

    @Override
    public void handleDeleteChat(ClientboundDeleteChatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Optional<MessageSignature> signature = packet.messageSignature().unpack(this.messageSignatureCache);
        if (signature.isEmpty()) {
            this.connection.disconnect(INVALID_PACKET);
            return;
        }
        this.lastSeenMessages.ignorePending(signature.get());
        if (!this.minecraft.gui.chatListener().removeFromDelayedMessageQueue(signature.get())) {
            this.minecraft.gui.hud.getChat().deleteMessage(signature.get());
        }
    }

    @Override
    public void handleAnimate(ClientboundAnimatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getId());
        if (entity == null) {
            return;
        }
        if (packet.getAction() == 0) {
            LivingEntity mob = (LivingEntity)entity;
            mob.swing(InteractionHand.MAIN_HAND);
        } else if (packet.getAction() == 3) {
            LivingEntity mob = (LivingEntity)entity;
            mob.swing(InteractionHand.OFF_HAND);
        } else if (packet.getAction() == 2) {
            Player player = (Player)entity;
            player.stopSleepInBed(false, false);
        } else if (packet.getAction() == 4) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
        } else if (packet.getAction() == 5) {
            this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
        }
    }

    @Override
    public void handleHurtAnimation(ClientboundHurtAnimationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.id());
        if (entity == null) {
            return;
        }
        entity.animateHurt(packet.yaw());
    }

    @Override
    public void handleSetTime(ClientboundSetTimePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        long gameTime = packet.gameTime();
        this.level.setTimeFromServer(gameTime);
        this.telemetryManager.setTime(gameTime);
        this.clockManager.handleUpdates(gameTime, packet.clockUpdates());
        this.level.environmentAttributes().invalidateTickCache();
    }

    @Override
    public void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.setRespawnData(packet.respawnData());
    }

    @Override
    public void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity vehicle = this.level.getEntity(packet.getVehicle());
        if (vehicle == null) {
            LOGGER.warn("Received passengers for unknown entity");
            return;
        }
        boolean wasPlayerMounted = vehicle.hasIndirectPassenger(this.minecraft.player);
        vehicle.ejectPassengers();
        for (int id : packet.getPassengers()) {
            Entity passenger = this.level.getEntity(id);
            if (passenger == null) continue;
            passenger.startRiding(vehicle, true, false);
            if (passenger != this.minecraft.player) continue;
            this.removedPlayerVehicleId = OptionalInt.empty();
            if (wasPlayerMounted) continue;
            if (vehicle instanceof AbstractBoat) {
                this.minecraft.player.yRotO = vehicle.getYRot();
                this.minecraft.player.setYRot(vehicle.getYRot());
                this.minecraft.player.setYHeadRot(vehicle.getYRot());
            }
            MutableComponent message = Component.translatable("mount.onboard", this.minecraft.options.keyShift.getTranslatedKeyMessage());
            this.minecraft.gui.hud.setOverlayMessage(message, false);
            this.minecraft.getNarrator().saySystemNow(message);
        }
    }

    @Override
    public void handleEntityLinkPacket(ClientboundSetEntityLinkPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity sourceEntity = this.level.getEntity(packet.getSourceId());
        if (sourceEntity instanceof Leashable) {
            Leashable leashable = (Leashable)((Object)sourceEntity);
            leashable.setDelayedLeashHolderId(packet.getDestId());
        }
    }

    private static ItemStack findTotem(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack itemStack = player.getItemInHand(hand);
            if (!itemStack.has(DataComponents.DEATH_PROTECTION)) continue;
            return itemStack;
        }
        return new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    @Override
    public void handleEntityEvent(ClientboundEntityEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            switch (packet.getEventId()) {
                case 63: {
                    this.minecraft.getSoundManager().play(new SnifferSoundInstance((Sniffer)entity));
                    break;
                }
                case 21: {
                    this.minecraft.getSoundManager().play(new GuardianAttackSoundInstance((Guardian)entity));
                    break;
                }
                case 35: {
                    int tickLength = 40;
                    this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
                    this.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TOTEM_USE, entity.getSoundSource(), 1.0f, 1.0f, false);
                    if (entity != this.minecraft.player) break;
                    this.minecraft.gameRenderer.displayItemActivation(ClientPacketListener.findTotem(this.minecraft.player));
                    break;
                }
                default: {
                    entity.handleEntityEvent(packet.getEventId());
                }
            }
        }
    }

    @Override
    public void handleDamageEvent(ClientboundDamageEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.entityId());
        if (entity == null) {
            return;
        }
        entity.handleDamageEvent(packet.getSource(this.level));
    }

    @Override
    public void handleSetHealth(ClientboundSetHealthPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.player.hurtTo(packet.getHealth());
        this.minecraft.player.getFoodData().setFoodLevel(packet.getFood());
        this.minecraft.player.getFoodData().setSaturation(packet.getSaturation());
    }

    @Override
    public void handleSetExperience(ClientboundSetExperiencePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.player.setExperienceValues(packet.getExperienceProgress(), packet.getTotalExperience(), packet.getExperienceLevel());
    }

    @Override
    public void handleRespawn(ClientboundRespawnPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        CommonPlayerSpawnInfo spawnInfo = packet.commonPlayerSpawnInfo();
        ResourceKey<Level> dimensionKey = spawnInfo.dimension();
        Holder<DimensionType> dimensionType = spawnInfo.dimensionType();
        LocalPlayer oldPlayer = this.minecraft.player;
        ResourceKey<Level> oldDimensionKey = oldPlayer.level().dimension();
        boolean dimensionChanged = dimensionKey != oldDimensionKey;
        LevelLoadingScreen.Reason levelLoadingReason = this.determineLevelLoadingReason(oldPlayer.isDeadOrDying(), dimensionKey, oldDimensionKey);
        if (dimensionChanged) {
            ClientLevel.ClientLevelData levelData;
            Map<MapId, MapItemSavedData> mapData = this.level.getAllMapData();
            boolean isDebug = spawnInfo.isDebug();
            boolean isFlat = spawnInfo.isFlat();
            int seaLevel = spawnInfo.seaLevel();
            this.levelData = levelData = new ClientLevel.ClientLevelData(this.levelData.getDifficulty(), this.levelData.isHardcore(), isFlat);
            this.level = new ClientLevel(this, levelData, dimensionKey, dimensionType, this.serverChunkRadius, this.serverSimulationDistance, this.minecraft.levelExtractor, isDebug, spawnInfo.seed(), seaLevel);
            this.level.addMapData(mapData);
            this.minecraft.setLevel(this.level);
            this.debugSubscriber.dropLevel();
        }
        this.minecraft.setCameraEntity(null);
        if (oldPlayer.hasContainerOpen()) {
            oldPlayer.closeContainer();
        }
        LocalPlayer newPlayer = packet.shouldKeep((byte)2) ? this.minecraft.gameMode.createPlayer(this.level, oldPlayer.getStats(), oldPlayer.getRecipeBook(), oldPlayer.getLastSentInput(), oldPlayer.isSprinting()) : this.minecraft.gameMode.createPlayer(this.level, oldPlayer.getStats(), oldPlayer.getRecipeBook());
        this.setClientLoaded(false);
        this.startWaitingForNewLevel(newPlayer, this.level, levelLoadingReason);
        newPlayer.setId(oldPlayer.getId());
        this.minecraft.player = newPlayer;
        if (dimensionChanged) {
            this.minecraft.getMusicManager().stopPlaying();
        }
        this.minecraft.setCameraEntity(newPlayer);
        if (packet.shouldKeep((byte)2)) {
            List<SynchedEntityData.DataValue<?>> data = oldPlayer.getEntityData().getNonDefaultValues();
            if (data != null) {
                newPlayer.getEntityData().assignValues(data);
            }
            newPlayer.setDeltaMovement(oldPlayer.getDeltaMovement());
            newPlayer.setYRot(oldPlayer.getYRot());
            newPlayer.setXRot(oldPlayer.getXRot());
        } else {
            newPlayer.resetPos();
            newPlayer.setYRot(-180.0f);
        }
        if (packet.shouldKeep((byte)1)) {
            newPlayer.getAttributes().assignAllValues(oldPlayer.getAttributes());
        } else {
            newPlayer.getAttributes().assignBaseValues(oldPlayer.getAttributes());
        }
        this.level.addEntity(newPlayer);
        newPlayer.input = new KeyboardInput(this.minecraft.options);
        this.minecraft.gameMode.adjustPlayer(newPlayer);
        newPlayer.setReducedDebugInfo(oldPlayer.isReducedDebugInfo());
        newPlayer.setShowDeathScreen(oldPlayer.shouldShowDeathScreen());
        newPlayer.setLastDeathLocation(spawnInfo.lastDeathLocation());
        newPlayer.setPortalCooldown(spawnInfo.portalCooldown());
        newPlayer.portalEffectIntensity = oldPlayer.portalEffectIntensity;
        newPlayer.oPortalEffectIntensity = oldPlayer.oPortalEffectIntensity;
        if (this.minecraft.gui.screen() instanceof DeathScreen || this.minecraft.gui.screen() instanceof DeathScreen.TitleConfirmScreen) {
            this.minecraft.gui.setScreen(null);
        }
        this.minecraft.gameMode.setLocalMode(spawnInfo.gameType(), spawnInfo.previousGameType());
    }

    private LevelLoadingScreen.Reason determineLevelLoadingReason(boolean playerDied, ResourceKey<Level> dimensionKey, ResourceKey<Level> oldDimensionKey) {
        LevelLoadingScreen.Reason levelLoadingReason = LevelLoadingScreen.Reason.OTHER;
        if (!playerDied) {
            if (dimensionKey == Level.NETHER || oldDimensionKey == Level.NETHER) {
                levelLoadingReason = LevelLoadingScreen.Reason.NETHER_PORTAL;
            } else if (dimensionKey == Level.END || oldDimensionKey == Level.END) {
                levelLoadingReason = LevelLoadingScreen.Reason.END_PORTAL;
            }
        }
        return levelLoadingReason;
    }

    @Override
    public void handleExplosion(ClientboundExplodePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Vec3 center = packet.center();
        this.minecraft.level.playLocalSound(center.x(), center.y(), center.z(), packet.explosionSound().value(), SoundSource.BLOCKS, 4.0f, (1.0f + (this.minecraft.level.getRandom().nextFloat() - this.minecraft.level.getRandom().nextFloat()) * 0.2f) * 0.7f, false);
        this.minecraft.level.addParticle(packet.explosionParticle(), center.x(), center.y(), center.z(), 1.0, 0.0, 0.0);
        this.minecraft.level.trackExplosionEffects(center, packet.radius(), packet.blockCount(), packet.blockParticles());
        packet.playerKnockback().ifPresent(this.minecraft.player::addDeltaMovement);
    }

    @Override
    public void handleMountScreenOpen(ClientboundMountScreenOpenPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getEntityId());
        LocalPlayer player = this.minecraft.player;
        int inventoryColumns = packet.getInventoryColumns();
        SimpleContainer container = new SimpleContainer(AbstractMountInventoryMenu.getInventorySize(inventoryColumns));
        if (entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse)entity;
            HorseInventoryMenu menu = new HorseInventoryMenu(packet.getContainerId(), player.getInventory(), container, horse, inventoryColumns);
            player.containerMenu = menu;
            this.minecraft.gui.setScreen(new HorseInventoryScreen(menu, player.getInventory(), horse, inventoryColumns));
        } else if (entity instanceof AbstractNautilus) {
            AbstractNautilus nautilus = (AbstractNautilus)entity;
            NautilusInventoryMenu menu = new NautilusInventoryMenu(packet.getContainerId(), player.getInventory(), container, nautilus, inventoryColumns);
            player.containerMenu = menu;
            this.minecraft.gui.setScreen(new NautilusInventoryScreen(menu, player.getInventory(), nautilus, inventoryColumns));
        }
    }

    @Override
    public void handleOpenScreen(ClientboundOpenScreenPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        MenuScreens.create(packet.getType(), this.minecraft, packet.getContainerId(), packet.getTitle());
    }

    @Override
    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet) {
        CreativeModeInventoryScreen screen;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = this.minecraft.player;
        ItemStack itemStack = packet.getItem();
        int slot = packet.getSlot();
        this.minecraft.getTutorial().onGetItem(itemStack);
        Screen screen2 = this.minecraft.gui.screen();
        boolean creative = screen2 instanceof CreativeModeInventoryScreen ? !(screen = (CreativeModeInventoryScreen)screen2).isInventoryOpen() : false;
        if (packet.getContainerId() == 0) {
            ItemStack lastItemStack;
            if (InventoryMenu.isHotbarSlot(slot) && !itemStack.isEmpty() && ((lastItemStack = player.inventoryMenu.getSlot(slot).getItem()).isEmpty() || lastItemStack.getCount() < itemStack.getCount())) {
                itemStack.setPopTime(5);
            }
            player.inventoryMenu.setItem(slot, packet.getStateId(), itemStack);
        } else if (!(packet.getContainerId() != player.containerMenu.containerId || packet.getContainerId() == 0 && creative)) {
            player.containerMenu.setItem(slot, packet.getStateId(), itemStack);
        }
        if (this.minecraft.gui.screen() instanceof CreativeModeInventoryScreen) {
            player.inventoryMenu.setRemoteSlot(slot, itemStack);
            player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public void handleSetCursorItem(ClientboundSetCursorItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getTutorial().onGetItem(packet.contents());
        if (!(this.minecraft.gui.screen() instanceof CreativeModeInventoryScreen)) {
            this.minecraft.player.containerMenu.setCarried(packet.contents());
        }
    }

    @Override
    public void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getTutorial().onGetItem(packet.contents());
        this.minecraft.player.getInventory().setItem(packet.slot(), packet.contents());
    }

    @Override
    public void handleContainerContent(ClientboundContainerSetContentPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = this.minecraft.player;
        if (packet.containerId() == 0) {
            player.inventoryMenu.initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
        } else if (packet.containerId() == player.containerMenu.containerId) {
            player.containerMenu.initializeContents(packet.stateId(), packet.items(), packet.carriedItem());
        }
    }

    @Override
    public void handleOpenSignEditor(ClientboundOpenSignEditorPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        BlockPos pos = packet.getPos();
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        if (blockEntity instanceof SignBlockEntity) {
            SignBlockEntity sign = (SignBlockEntity)blockEntity;
            this.minecraft.player.openTextEdit(sign, packet.isFrontText());
        } else {
            LOGGER.warn("Ignoring openTextEdit on an invalid entity: {} at pos {}", (Object)this.level.getBlockEntity(pos), (Object)pos);
        }
    }

    @Override
    public void handleBlockEntityData(ClientboundBlockEntityDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        BlockPos pos = packet.getPos();
        this.minecraft.level.getBlockEntity(pos, packet.getType()).ifPresent(blockEntity -> {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER);){
                blockEntity.loadWithComponents(TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)this.registryAccess, packet.getTag()));
            }
            if (blockEntity instanceof CommandBlockEntity && this.minecraft.gui.screen() instanceof CommandBlockEditScreen) {
                ((CommandBlockEditScreen)this.minecraft.gui.screen()).updateGui();
            }
        });
    }

    @Override
    public void handleContainerSetData(ClientboundContainerSetDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = this.minecraft.player;
        if (player.containerMenu.containerId == packet.getContainerId()) {
            player.containerMenu.setData(packet.getId(), packet.getValue());
        }
    }

    @Override
    public void handleSetEquipment(ClientboundSetEquipmentPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getEntity());
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            packet.getSlots().forEach(e -> livingEntity.setItemSlot((EquipmentSlot)e.getFirst(), (ItemStack)e.getSecond()));
        }
    }

    @Override
    public void handleContainerClose(ClientboundContainerClosePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.player.clientSideCloseContainer();
    }

    @Override
    public void handleBlockEvent(ClientboundBlockEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.blockEvent(packet.getPos(), packet.getBlock(), packet.getB0(), packet.getB1());
    }

    @Override
    public void handleBlockDestruction(ClientboundBlockDestructionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.destroyBlockProgress(packet.getId(), packet.getPos(), packet.getProgress());
    }

    @Override
    public void handleGameEvent(ClientboundGameEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = Objects.requireNonNull(this.minecraft.player);
        ClientboundGameEventPacket.Type event = packet.getEvent();
        float paramFloat = packet.getParam();
        int param = Mth.floor(paramFloat + 0.5f);
        if (event == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE) {
            player.sendSystemMessage(Component.translatable("block.minecraft.spawn.not_valid"));
        } else if (event == ClientboundGameEventPacket.START_RAINING) {
            this.level.setRainLevel(0.0f);
        } else if (event == ClientboundGameEventPacket.STOP_RAINING) {
            this.level.setRainLevel(1.0f);
        } else if (event == ClientboundGameEventPacket.CHANGE_GAME_MODE) {
            this.minecraft.gameMode.setLocalMode(GameType.byId(param));
        } else if (event == ClientboundGameEventPacket.WIN_GAME) {
            this.minecraft.gui.setScreen(new WinScreen(true, () -> {
                player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                this.minecraft.gui.setScreen(null);
            }));
        } else if (event == ClientboundGameEventPacket.DEMO_EVENT) {
            Options options = this.minecraft.options;
            MutableComponent message = null;
            if (paramFloat == 0.0f) {
                this.openDemoIntroScreen(options);
            } else if (paramFloat == 101.0f) {
                message = Component.translatable("demo.help.movement", options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage());
            } else if (paramFloat == 102.0f) {
                message = Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage());
            } else if (paramFloat == 103.0f) {
                message = Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage());
            } else if (paramFloat == 104.0f) {
                message = Component.translatable("demo.day.6", options.keyScreenshot.getTranslatedKeyMessage());
            }
            if (message != null) {
                this.minecraft.gui.hud.getChat().addClientSystemMessage(message);
                this.minecraft.getNarrator().saySystemQueued(message);
            }
        } else if (event == ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND) {
            this.level.playSound((Entity)player, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18f, 0.45f);
        } else if (event == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            this.level.setRainLevel(paramFloat);
        } else if (event == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            this.level.setThunderLevel(paramFloat);
        } else if (event == ClientboundGameEventPacket.PUFFER_FISH_STING) {
            this.level.playSound((Entity)player, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_STING, SoundSource.NEUTRAL, 1.0f, 1.0f);
        } else if (event == ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT) {
            this.level.addParticle(ParticleTypes.ELDER_GUARDIAN, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
            if (param == 1) {
                this.level.playSound((Entity)player, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0f, 1.0f);
            }
        } else if (event == ClientboundGameEventPacket.IMMEDIATE_RESPAWN) {
            player.setShowDeathScreen(paramFloat == 0.0f);
        } else if (event == ClientboundGameEventPacket.LIMITED_CRAFTING) {
            player.setDoLimitedCrafting(paramFloat == 1.0f);
        } else if (event == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START && this.levelLoadTracker != null) {
            this.levelLoadTracker.loadingPacketsReceived();
        }
    }

    private void openDemoIntroScreen(Options options) {
        this.minecraft.gui.setScreen(new PopupScreen.Builder(null, Component.translatable("demo.help.title")).addMessage(CommonComponents.joinLines(Component.translatable("demo.help.movementShort", options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage()), Component.translatable("demo.help.movementMouse"), Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage()), Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage()))).addMessage(Component.translatable("demo.help.fullWrapped")).addButton(Component.translatable("demo.help.buy"), popupScreen -> ConfirmLinkScreen.confirmLinkNow(null, CommonLinks.BUY_MINECRAFT_JAVA)).addButton(Component.translatable("demo.help.later"), popupScreen -> {
            this.minecraft.mouseHandler.grabMouse();
            popupScreen.onClose();
        }).build());
    }

    private void startWaitingForNewLevel(LocalPlayer player, ClientLevel level, LevelLoadingScreen.Reason reason) {
        if (this.levelLoadTracker == null) {
            this.levelLoadTracker = new LevelLoadTracker();
        }
        this.levelLoadTracker.startClientLoad(player, level);
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof LevelLoadingScreen) {
            LevelLoadingScreen loadingScreen = (LevelLoadingScreen)screen;
            loadingScreen.update(this.levelLoadTracker, reason);
        } else {
            this.minecraft.gui.hud.getChat().preserveCurrentChatScreen();
            this.minecraft.setScreenAndShow(new LevelLoadingScreen(this.levelLoadTracker, reason));
        }
    }

    @Override
    public void handleMapItemData(ClientboundMapItemDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        MapId id = packet.mapId();
        MapItemSavedData data = this.minecraft.level.getMapData(id);
        if (data == null) {
            data = MapItemSavedData.createForClient(packet.scale(), packet.locked(), this.minecraft.level.dimension());
            this.minecraft.level.overrideMapData(id, data);
        }
        packet.applyToMap(data);
        this.minecraft.getMapTextureManager().update(id, data);
    }

    @Override
    public void handleLevelEvent(ClientboundLevelEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.isGlobalEvent()) {
            this.minecraft.level.globalLevelEvent(packet.getType(), packet.getPos(), packet.getData());
        } else {
            this.minecraft.level.levelEvent(packet.getType(), packet.getPos(), packet.getData());
        }
    }

    @Override
    public void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.advancements.update(packet);
    }

    @Override
    public void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Identifier id = packet.getTab();
        if (id == null) {
            this.advancements.setSelectedTab(null, false);
        } else {
            AdvancementHolder advancement = this.advancements.get(id);
            this.advancements.setSelectedTab(advancement, false);
        }
    }

    @Override
    public void handleCommands(ClientboundCommandsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.commands = new CommandDispatcher(packet.getRoot(CommandBuildContext.simple(this.registryAccess, this.enabledFeatures), COMMAND_NODE_BUILDER));
    }

    @Override
    public void handleStopSoundEvent(ClientboundStopSoundPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.getSoundManager().stop(packet.getName(), packet.getSource());
    }

    @Override
    public void handleCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.suggestionsProvider.completeCustomSuggestions(packet.id(), packet.toSuggestions());
    }

    @Override
    public void handleUpdateRecipes(ClientboundUpdateRecipesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.recipes = new ClientRecipeContainer(packet.itemSets(), packet.stonecutterRecipes());
    }

    @Override
    public void handleLookAt(ClientboundPlayerLookAtPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Vec3 pos = packet.getPosition(this.level);
        if (pos != null) {
            this.minecraft.player.lookAt(packet.getFromAnchor(), pos);
        }
    }

    @Override
    public void handleTagQueryPacket(ClientboundTagQueryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (!this.debugQueryHandler.handleResponse(packet.getTransactionId(), packet.getTag())) {
            LOGGER.debug("Got unhandled response to tag query {}", (Object)packet.getTransactionId());
        }
    }

    @Override
    public void handleAwardStats(ClientboundAwardStatsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        for (Object2IntMap.Entry entry : packet.stats().object2IntEntrySet()) {
            Stat stat = (Stat)entry.getKey();
            int amount = entry.getIntValue();
            this.minecraft.player.getStats().setValue(this.minecraft.player, stat, amount);
        }
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof StatsScreen) {
            StatsScreen statsScreen = (StatsScreen)screen;
            statsScreen.onStatsUpdated();
        }
    }

    @Override
    public void handleRecipeBookAdd(ClientboundRecipeBookAddPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientRecipeBook recipeBook = this.minecraft.player.getRecipeBook();
        if (packet.replace()) {
            recipeBook.clear();
        }
        for (ClientboundRecipeBookAddPacket.Entry entry : packet.entries()) {
            recipeBook.add(entry.contents());
            if (entry.highlight()) {
                recipeBook.addHighlight(entry.contents().id());
            }
            if (!entry.notification()) continue;
            RecipeToast.addOrUpdate(this.minecraft.gui.toastManager(), entry.contents().display());
        }
        this.refreshRecipeBook(recipeBook);
    }

    @Override
    public void handleRecipeBookRemove(ClientboundRecipeBookRemovePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientRecipeBook recipeBook = this.minecraft.player.getRecipeBook();
        for (RecipeDisplayId id : packet.recipes()) {
            recipeBook.remove(id);
        }
        this.refreshRecipeBook(recipeBook);
    }

    @Override
    public void handleRecipeBookSettings(ClientboundRecipeBookSettingsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientRecipeBook recipeBook = this.minecraft.player.getRecipeBook();
        recipeBook.setBookSettings(packet.bookSettings());
        this.refreshRecipeBook(recipeBook);
    }

    private void refreshRecipeBook(ClientRecipeBook recipeBook) {
        recipeBook.rebuildCollections();
        this.searchTrees.updateRecipes(recipeBook, this.level);
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof RecipeUpdateListener) {
            RecipeUpdateListener updateListener = (RecipeUpdateListener)((Object)screen);
            updateListener.recipesUpdated();
        }
    }

    @Override
    public void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getEntityId());
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity livingEntity = (LivingEntity)entity;
        Holder<MobEffect> effect = packet.getEffect();
        MobEffectInstance mobEffectInstance = new MobEffectInstance(effect, packet.getEffectDurationTicks(), packet.getEffectAmplifier(), packet.isEffectAmbient(), packet.isEffectVisible(), packet.effectShowsIcon(), null);
        if (!packet.shouldBlend()) {
            mobEffectInstance.skipBlending();
        }
        livingEntity.forceAddEffect(mobEffectInstance, null);
    }

    private <T> Registry.PendingTags<T> updateTags(ResourceKey<? extends Registry<? extends T>> registryKey, TagNetworkSerialization.NetworkPayload payload) {
        HolderLookup.RegistryLookup registry = this.registryAccess.lookupOrThrow((ResourceKey)registryKey);
        return registry.prepareTagReload(payload.resolve(registry));
    }

    @Override
    public void handleUpdateTags(ClientboundUpdateTagsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ArrayList pendingTags = new ArrayList(packet.getTags().size());
        packet.getTags().forEach((key, networkPayload) -> pendingTags.add(this.updateTags((ResourceKey)key, (TagNetworkSerialization.NetworkPayload)networkPayload)));
        if (!this.connection.isMemoryConnection()) {
            pendingTags.forEach(Registry.PendingTags::apply);
        }
        this.fuelValues = FuelValues.vanillaBurnTimes(this.registryAccess, this.enabledFeatures);
        List<ItemStack> searchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
        this.searchTrees.updateCreativeTags(searchItems);
    }

    @Override
    public void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket packet) {
    }

    @Override
    public void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket packet) {
    }

    @Override
    public void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity player = this.level.getEntity(packet.playerId());
        if (player == this.minecraft.player) {
            if (this.minecraft.player.shouldShowDeathScreen()) {
                this.minecraft.gui.setScreen(new DeathScreen(packet.message(), this.level.getLevelData().isHardcore(), this.minecraft.player));
            } else {
                this.minecraft.player.respawn();
            }
        }
    }

    @Override
    public void handleChangeDifficulty(ClientboundChangeDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.levelData.setDifficulty(packet.difficulty());
        this.levelData.setDifficultyLocked(packet.locked());
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof HasDifficultyReaction) {
            HasDifficultyReaction screen2 = (HasDifficultyReaction)((Object)screen);
            screen2.onDifficultyChanged();
        }
    }

    @Override
    public void handleSetCamera(ClientboundSetCameraPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity != null) {
            this.minecraft.setCameraEntity(entity);
        }
    }

    @Override
    public void handleInitializeBorder(ClientboundInitializeBorderPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        WorldBorder border = this.level.getWorldBorder();
        border.setCenter(packet.getNewCenterX(), packet.getNewCenterZ());
        long lerpTime = packet.getLerpTime();
        if (lerpTime > 0L) {
            border.lerpSizeBetween(packet.getOldSize(), packet.getNewSize(), lerpTime, this.level.getGameTime());
        } else {
            border.setSize(packet.getNewSize());
        }
        border.setAbsoluteMaxSize(packet.getNewAbsoluteMaxSize());
        border.setWarningBlocks(packet.getWarningBlocks());
        border.setWarningTime(packet.getWarningTime());
    }

    @Override
    public void handleSetBorderCenter(ClientboundSetBorderCenterPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setCenter(packet.getNewCenterX(), packet.getNewCenterZ());
    }

    @Override
    public void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().lerpSizeBetween(packet.getOldSize(), packet.getNewSize(), packet.getLerpTime(), this.level.getGameTime());
    }

    @Override
    public void handleSetBorderSize(ClientboundSetBorderSizePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setSize(packet.getSize());
    }

    @Override
    public void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setWarningBlocks(packet.getWarningBlocks());
    }

    @Override
    public void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getWorldBorder().setWarningTime(packet.getWarningDelay());
    }

    @Override
    public void handleTitlesClear(ClientboundClearTitlesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.clearTitles();
        if (packet.shouldResetTimes()) {
            this.minecraft.gui.hud.resetTitleTimes();
        }
    }

    @Override
    public void handleServerData(ClientboundServerDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.serverData == null) {
            return;
        }
        this.serverData.motd = packet.motd();
        packet.iconBytes().map(ServerData::validateIcon).ifPresent(this.serverData::setIconBytes);
        ServerList.saveSingleServer(this.serverData);
    }

    @Override
    public void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.suggestionsProvider.modifyCustomCompletions(packet.action(), packet.entries());
    }

    @Override
    public void setActionBarText(ClientboundSetActionBarTextPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.setOverlayMessage(packet.text(), false);
    }

    @Override
    public void setTitleText(ClientboundSetTitleTextPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.setTitle(packet.text());
    }

    @Override
    public void setSubtitleText(ClientboundSetSubtitleTextPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.setSubtitle(packet.text());
    }

    @Override
    public void setTitlesAnimation(ClientboundSetTitlesAnimationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.setTimes(packet.getFadeIn(), packet.getStay(), packet.getFadeOut());
    }

    @Override
    public void handleTabListCustomisation(ClientboundTabListPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.getTabList().setHeader(packet.header().getString().isEmpty() ? null : packet.header());
        this.minecraft.gui.hud.getTabList().setFooter(packet.footer().getString().isEmpty() ? null : packet.footer());
    }

    @Override
    public void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = packet.getEntity(this.level);
        if (entity instanceof LivingEntity) {
            LivingEntity entity2 = (LivingEntity)entity;
            entity2.removeEffectNoUpdate(packet.effect());
        }
    }

    @Override
    public void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        for (UUID profileId : packet.profileIds()) {
            this.minecraft.getPlayerSocialManager().removePlayer(profileId);
            PlayerInfo info = this.playerInfoMap.remove(profileId);
            if (info == null) continue;
            this.listedPlayers.remove(info);
        }
    }

    @Override
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
            PlayerInfo playerInfo = new PlayerInfo(Objects.requireNonNull(entry.profile()), this.enforcesSecureChat());
            if (this.playerInfoMap.putIfAbsent(entry.profileId(), playerInfo) != null) continue;
            this.minecraft.getPlayerSocialManager().addPlayer(playerInfo);
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
            PlayerInfo info = this.playerInfoMap.get(entry.profileId());
            if (info == null) {
                LOGGER.warn("Ignoring player info update for unknown player {} ({})", (Object)entry.profileId(), packet.actions());
                continue;
            }
            for (ClientboundPlayerInfoUpdatePacket.Action action : packet.actions()) {
                this.applyPlayerInfoUpdate(action, entry, info);
            }
        }
    }

    private void applyPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket.Action action, ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo info) {
        switch (action) {
            case INITIALIZE_CHAT: {
                this.initializeChatSession(entry, info);
                break;
            }
            case UPDATE_GAME_MODE: {
                if (info.getGameMode() != entry.gameMode() && this.minecraft.player != null && this.minecraft.player.getUUID().equals(entry.profileId())) {
                    this.minecraft.player.onGameModeChanged(entry.gameMode());
                }
                info.setGameMode(entry.gameMode());
                break;
            }
            case UPDATE_LISTED: {
                if (entry.listed()) {
                    this.listedPlayers.add(info);
                    break;
                }
                this.listedPlayers.remove(info);
                break;
            }
            case UPDATE_LATENCY: {
                info.setLatency(entry.latency());
                break;
            }
            case UPDATE_DISPLAY_NAME: {
                info.setTabListDisplayName(entry.displayName());
                break;
            }
            case UPDATE_HAT: {
                info.setShowHat(entry.showHat());
                break;
            }
            case UPDATE_LIST_ORDER: {
                info.setTabListOrder(entry.listOrder());
            }
        }
    }

    private void initializeChatSession(ClientboundPlayerInfoUpdatePacket.Entry entry, PlayerInfo info) {
        GameProfile profile = info.getProfile();
        SignatureValidator signatureValidator = this.minecraft.services().profileKeySignatureValidator();
        if (signatureValidator == null) {
            LOGGER.warn("Ignoring chat session from {} due to missing Services public key", (Object)profile.name());
            info.clearChatSession(this.enforcesSecureChat());
            return;
        }
        RemoteChatSession.Data chatSessionData = entry.chatSession();
        if (chatSessionData != null) {
            try {
                RemoteChatSession chatSession = chatSessionData.validate(profile, signatureValidator);
                info.setChatSession(chatSession);
            }
            catch (ProfilePublicKey.ValidationException e) {
                LOGGER.error("Failed to validate profile key for player: '{}'", (Object)profile.name(), (Object)e);
                info.clearChatSession(this.enforcesSecureChat());
            }
        } else {
            info.clearChatSession(this.enforcesSecureChat());
        }
    }

    private boolean enforcesSecureChat() {
        return this.minecraft.services().canValidateProfileKeys() && this.serverEnforcesSecureChat;
    }

    public boolean onlineMode() {
        return this.onlineMode;
    }

    @Override
    public void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        LocalPlayer player = this.minecraft.player;
        player.getAbilities().flying = packet.isFlying();
        player.getAbilities().instabuild = packet.canInstabuild();
        player.getAbilities().invulnerable = packet.isInvulnerable();
        player.getAbilities().mayfly = packet.canFly();
        player.getAbilities().setFlyingSpeed(packet.getFlyingSpeed());
        player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
    }

    @Override
    public void handleGameRuleValues(ClientboundGameRuleValuesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof InWorldGameRulesScreen) {
            InWorldGameRulesScreen inWorldGameRulesScreen = (InWorldGameRulesScreen)screen;
            inWorldGameRulesScreen.onGameRuleValuesUpdated(packet.values());
        }
    }

    @Override
    public void handleSoundEvent(ClientboundSoundPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.level.playSeededSound((Entity)this.minecraft.player, packet.getX(), packet.getY(), packet.getZ(), packet.getSound(), packet.getSource(), packet.getVolume(), packet.getPitch(), packet.getSeed());
    }

    @Override
    public void handleSoundEntityEvent(ClientboundSoundEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getId());
        if (entity == null) {
            return;
        }
        this.minecraft.level.playSeededSound(this.minecraft.player, entity, packet.getSound(), packet.getSource(), packet.getVolume(), packet.getPitch(), packet.getSeed());
    }

    @Override
    public void handleBossUpdate(ClientboundBossEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.gui.hud.getBossOverlay().update(packet);
    }

    @Override
    public void handleItemCooldown(ClientboundCooldownPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.duration() == 0) {
            this.minecraft.player.getCooldowns().removeCooldown(packet.cooldownGroup());
        } else {
            this.minecraft.player.getCooldowns().addCooldown(packet.cooldownGroup(), packet.duration());
        }
    }

    @Override
    public void handleMoveVehicle(ClientboundMoveVehiclePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity vehicle = this.minecraft.player.getRootVehicle();
        if (vehicle != this.minecraft.player && vehicle.isLocalInstanceAuthoritative()) {
            Vec3 currentTarget;
            Vec3 target = packet.position();
            if (target.distanceTo(currentTarget = vehicle.isInterpolating() ? vehicle.getInterpolation().position() : vehicle.position()) > (double)1.0E-5f) {
                if (vehicle.isInterpolating()) {
                    vehicle.getInterpolation().cancel();
                }
                vehicle.absSnapTo(target.x(), target.y(), target.z(), packet.yRot(), packet.xRot());
            }
            this.connection.send(ServerboundMoveVehiclePacket.fromEntity(vehicle));
        }
    }

    @Override
    public void handleOpenBook(ClientboundOpenBookPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ItemStack held = this.minecraft.player.getItemInHand(packet.getHand());
        BookViewScreen.BookAccess bookAccess = BookViewScreen.BookAccess.fromItem(held);
        if (bookAccess != null) {
            this.minecraft.gui.setScreen(new BookViewScreen(bookAccess));
        }
    }

    @Override
    public void handleCustomPayload(CustomPacketPayload payload) {
        this.handleUnknownCustomPayload(payload);
    }

    private void handleUnknownCustomPayload(CustomPacketPayload payload) {
        LOGGER.warn("Unknown custom packet payload: {}", (Object)payload.type().id());
    }

    @Override
    public void handleAddObjective(ClientboundSetObjectivePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String objectiveName = packet.getObjectiveName();
        if (packet.getMethod() == 0) {
            this.scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, packet.getDisplayName(), packet.getRenderType(), false, packet.getNumberFormat().orElse(null));
        } else {
            Objective objective = this.scoreboard.getObjective(objectiveName);
            if (objective != null) {
                if (packet.getMethod() == 1) {
                    this.scoreboard.removeObjective(objective);
                } else if (packet.getMethod() == 2) {
                    objective.setRenderType(packet.getRenderType());
                    objective.setDisplayName(packet.getDisplayName());
                    objective.setNumberFormat(packet.getNumberFormat().orElse(null));
                }
            }
        }
    }

    @Override
    public void handleSetScore(ClientboundSetScorePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String objectiveName = packet.objectiveName();
        ScoreHolder scoreHolder = ScoreHolder.forNameOnly(packet.owner());
        Objective objective = this.scoreboard.getObjective(objectiveName);
        if (objective != null) {
            ScoreAccess score = this.scoreboard.getOrCreatePlayerScore(scoreHolder, objective, true);
            score.set(packet.score());
            score.display(packet.display().orElse(null));
            score.numberFormatOverride(packet.numberFormat().orElse(null));
        } else {
            LOGGER.warn("Received packet for unknown scoreboard objective: {}", (Object)objectiveName);
        }
    }

    @Override
    public void handleResetScore(ClientboundResetScorePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String objectiveName = packet.objectiveName();
        ScoreHolder scoreHolder = ScoreHolder.forNameOnly(packet.owner());
        if (objectiveName == null) {
            this.scoreboard.resetAllPlayerScores(scoreHolder);
        } else {
            Objective objective = this.scoreboard.getObjective(objectiveName);
            if (objective != null) {
                this.scoreboard.resetSinglePlayerScore(scoreHolder, objective);
            } else {
                LOGGER.warn("Received packet for unknown scoreboard objective: {}", (Object)objectiveName);
            }
        }
    }

    @Override
    public void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        String objectiveName = packet.getObjectiveName();
        Objective objective = objectiveName == null ? null : this.scoreboard.getObjective(objectiveName);
        this.scoreboard.setDisplayObjective(packet.getSlot(), objective);
    }

    @Override
    public void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet) {
        PlayerTeam team;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        ClientboundSetPlayerTeamPacket.Action teamAction = packet.getTeamAction();
        if (teamAction == ClientboundSetPlayerTeamPacket.Action.ADD) {
            team = this.scoreboard.addPlayerTeam(packet.getName());
        } else {
            team = this.scoreboard.getPlayerTeam(packet.getName());
            if (team == null) {
                LOGGER.warn("Received packet for unknown team {}: team action: {}, player action: {}", new Object[]{packet.getName(), packet.getTeamAction(), packet.getPlayerAction()});
                return;
            }
        }
        Optional<ClientboundSetPlayerTeamPacket.Parameters> parameters = packet.getParameters();
        parameters.ifPresent(p -> {
            team.setDisplayName(p.displayName());
            team.setColor(p.color());
            team.unpackOptions(p.options());
            team.setNameTagVisibility(p.nameTagVisibility());
            team.setCollisionRule(p.collisionRule());
            team.setPlayerPrefix(p.playerPrefix());
            team.setPlayerSuffix(p.playerSuffix());
        });
        ClientboundSetPlayerTeamPacket.Action playerAction = packet.getPlayerAction();
        if (playerAction == ClientboundSetPlayerTeamPacket.Action.ADD) {
            for (String player : packet.getPlayers()) {
                this.scoreboard.addPlayerToTeam(player, team);
            }
        } else if (playerAction == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            for (String player : packet.getPlayers()) {
                this.scoreboard.removePlayerFromTeam(player, team);
            }
        }
        if (teamAction == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
            this.scoreboard.removePlayerTeam(team);
        }
    }

    @Override
    public void handleParticleEvent(ClientboundLevelParticlesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (packet.getCount() == 0) {
            double xa = packet.getMaxSpeed() * packet.getXDist();
            double ya = packet.getMaxSpeed() * packet.getYDist();
            double za = packet.getMaxSpeed() * packet.getZDist();
            try {
                this.level.addParticle(packet.getParticle(), packet.isOverrideLimiter(), packet.alwaysShow(), packet.getX(), packet.getY(), packet.getZ(), xa, ya, za);
            }
            catch (Throwable ignored) {
                LOGGER.warn("Could not spawn particle effect {}", (Object)packet.getParticle());
            }
        } else {
            for (int i = 0; i < packet.getCount(); ++i) {
                double xVarience = this.random.nextGaussian() * (double)packet.getXDist();
                double yVarience = this.random.nextGaussian() * (double)packet.getYDist();
                double zVarience = this.random.nextGaussian() * (double)packet.getZDist();
                double xa = this.random.nextGaussian() * (double)packet.getMaxSpeed();
                double ya = this.random.nextGaussian() * (double)packet.getMaxSpeed();
                double za = this.random.nextGaussian() * (double)packet.getMaxSpeed();
                try {
                    this.level.addParticle(packet.getParticle(), packet.isOverrideLimiter(), packet.alwaysShow(), packet.getX() + xVarience, packet.getY() + yVarience, packet.getZ() + zVarience, xa, ya, za);
                    continue;
                }
                catch (Throwable ignored) {
                    LOGGER.warn("Could not spawn particle effect {}", (Object)packet.getParticle());
                    return;
                }
            }
        }
    }

    @Override
    public void handleUpdateAttributes(ClientboundUpdateAttributesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getEntityId());
        if (entity == null) {
            return;
        }
        if (!(entity instanceof LivingEntity)) {
            throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + String.valueOf(entity) + ")");
        }
        LivingEntity livingEntity = (LivingEntity)entity;
        AttributeMap attributes = livingEntity.getAttributes();
        for (ClientboundUpdateAttributesPacket.AttributeSnapshot attribute : packet.getValues()) {
            AttributeInstance instance = attributes.getInstance(attribute.attribute());
            if (instance == null) {
                LOGGER.warn("Entity {} does not have attribute {}", (Object)entity, (Object)attribute.attribute().getRegisteredName());
                continue;
            }
            instance.setBaseValue(attribute.base());
            instance.removeModifiers();
            for (AttributeModifier modifier : attribute.modifiers()) {
                instance.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        AbstractContainerMenu containerMenu = this.minecraft.player.containerMenu;
        if (containerMenu.containerId != packet.containerId()) {
            return;
        }
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof RecipeUpdateListener) {
            RecipeUpdateListener listener = (RecipeUpdateListener)((Object)screen);
            listener.fillGhostRecipe(packet.recipeDisplay());
        }
    }

    @Override
    public void handleLightUpdatePacket(ClientboundLightUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        int x = packet.getX();
        int z = packet.getZ();
        ClientboundLightUpdatePacketData lightData = packet.getLightData();
        this.level.queueLightUpdate(() -> this.applyLightData(x, z, lightData, true));
    }

    private void applyLightData(int x, int z, ClientboundLightUpdatePacketData lightData, boolean scheduleRebuild) {
        LevelLightEngine lightEngine = this.level.getChunkSource().getLightEngine();
        BitSet skyYMask = lightData.getSkyYMask();
        BitSet emptySkyYMask = lightData.getEmptySkyYMask();
        Iterator<byte[]> skyUpdates = lightData.getSkyUpdates().iterator();
        this.readSectionList(x, z, lightEngine, LightLayer.SKY, skyYMask, emptySkyYMask, skyUpdates, scheduleRebuild);
        BitSet blockYMask = lightData.getBlockYMask();
        BitSet emptyBlockYMask = lightData.getEmptyBlockYMask();
        Iterator<byte[]> blockUpdates = lightData.getBlockUpdates().iterator();
        this.readSectionList(x, z, lightEngine, LightLayer.BLOCK, blockYMask, emptyBlockYMask, blockUpdates, scheduleRebuild);
        lightEngine.setLightEnabled(new ChunkPos(x, z), true);
    }

    @Override
    public void handleMerchantOffers(ClientboundMerchantOffersPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        AbstractContainerMenu menu = this.minecraft.player.containerMenu;
        if (packet.getContainerId() == menu.containerId && menu instanceof MerchantMenu) {
            MerchantMenu merchantMenu = (MerchantMenu)menu;
            merchantMenu.setOffers(packet.getOffers());
            merchantMenu.setXp(packet.getVillagerXp());
            merchantMenu.setMerchantLevel(packet.getVillagerLevel());
            merchantMenu.setShowProgressBar(packet.showProgress());
            merchantMenu.setCanRestock(packet.canRestock());
        }
    }

    @Override
    public void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.serverChunkRadius = packet.getRadius();
        this.minecraft.options.setServerRenderDistance(this.serverChunkRadius);
        this.level.getChunkSource().updateViewRadius(packet.getRadius());
    }

    @Override
    public void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.serverSimulationDistance = packet.simulationDistance();
        this.level.setServerSimulationDistance(this.serverSimulationDistance);
    }

    @Override
    public void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.getChunkSource().updateViewCenter(packet.getX(), packet.getZ());
    }

    @Override
    public void handleBlockChangedAck(ClientboundBlockChangedAckPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.level.handleBlockChangedAck(packet.sequence());
    }

    @Override
    public void handleBundlePacket(ClientboundBundlePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        for (Packet<ClientPacketListener> packet2 : packet.subPackets()) {
            packet2.handle(this);
        }
    }

    @Override
    public void handleProjectilePowerPacket(ClientboundProjectilePowerPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.getId());
        if (entity instanceof AbstractHurtingProjectile) {
            AbstractHurtingProjectile projectile = (AbstractHurtingProjectile)entity;
            projectile.accelerationPower = packet.getAccelerationPower();
        }
    }

    @Override
    public void handleChunkBatchStart(ClientboundChunkBatchStartPacket packet) {
        this.chunkBatchSizeCalculator.onBatchStart();
    }

    @Override
    public void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket packet) {
        this.chunkBatchSizeCalculator.onBatchFinished(packet.batchSize());
        this.send(new ServerboundChunkBatchReceivedPacket(this.chunkBatchSizeCalculator.getDesiredChunksPerTick()));
    }

    @Override
    public void handleDebugSample(ClientboundDebugSamplePacket packet) {
        this.minecraft.getDebugOverlay().logRemoteSample(packet.sample(), packet.debugSampleType());
    }

    @Override
    public void handlePongResponse(ClientboundPongResponsePacket packet) {
        this.pingDebugMonitor.onPongReceived(packet);
    }

    @Override
    public void handleTestInstanceBlockStatus(ClientboundTestInstanceBlockStatus packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Screen screen = this.minecraft.gui.screen();
        if (screen instanceof TestInstanceBlockEditScreen) {
            TestInstanceBlockEditScreen editScreen = (TestInstanceBlockEditScreen)screen;
            editScreen.setStatus(packet.status(), packet.size());
        }
    }

    @Override
    public void handleWaypoint(ClientboundTrackedWaypointPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        packet.apply(this.waypointManager);
    }

    @Override
    public void handleDebugChunkValue(ClientboundDebugChunkValuePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.debugSubscriber.updateChunk(this.level.getGameTime(), packet.chunkPos(), packet.update());
    }

    @Override
    public void handleDebugBlockValue(ClientboundDebugBlockValuePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.debugSubscriber.updateBlock(this.level.getGameTime(), packet.blockPos(), packet.update());
    }

    @Override
    public void handleDebugEntityValue(ClientboundDebugEntityValuePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        Entity entity = this.level.getEntity(packet.entityId());
        if (entity != null) {
            this.debugSubscriber.updateEntity(this.level.getGameTime(), entity, packet.update());
        }
    }

    @Override
    public void handleDebugEvent(ClientboundDebugEventPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.debugSubscriber.pushEvent(this.level.getGameTime(), packet.event());
    }

    @Override
    public void handleGameTestHighlightPos(ClientboundGameTestHighlightPosPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.minecraft.levelExtractor.gameTestBlockHighlightRenderer.highlightPos(packet.absolutePos(), packet.relativePos());
    }

    @Override
    public void handleLowDiskSpaceWarning(ClientboundLowDiskSpaceWarningPacket packet) {
        this.minecraft.sendLowDiskSpaceWarning();
    }

    private void readSectionList(int chunkX, int chunkZ, LevelLightEngine lightEngine, LightLayer layer, BitSet yMask, BitSet emptyYMask, Iterator<byte[]> updates, boolean scheduleRebuild) {
        for (int sectionIndex = 0; sectionIndex < lightEngine.getLightSectionCount(); ++sectionIndex) {
            int sectionY = lightEngine.getMinLightSection() + sectionIndex;
            boolean haveData = yMask.get(sectionIndex);
            boolean haveEmpty = emptyYMask.get(sectionIndex);
            if (!haveData && !haveEmpty) continue;
            lightEngine.queueSectionData(layer, SectionPos.of(chunkX, sectionY, chunkZ), haveData ? new DataLayer((byte[])updates.next().clone()) : new DataLayer());
            if (!scheduleRebuild) continue;
            this.level.setSectionDirtyWithNeighbors(chunkX, sectionY, chunkZ);
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected() && !this.closed;
    }

    public Collection<PlayerInfo> getListedOnlinePlayers() {
        return this.listedPlayers;
    }

    public Collection<PlayerInfo> getOnlinePlayers() {
        return this.playerInfoMap.values();
    }

    public Collection<UUID> getOnlinePlayerIds() {
        return this.playerInfoMap.keySet();
    }

    public @Nullable PlayerInfo getPlayerInfo(UUID player) {
        return this.playerInfoMap.get(player);
    }

    public @Nullable PlayerInfo getPlayerInfo(String player) {
        for (PlayerInfo playerInfo : this.playerInfoMap.values()) {
            if (!playerInfo.getProfile().name().equals(player)) continue;
            return playerInfo;
        }
        return null;
    }

    public Map<UUID, PlayerInfo> getSeenPlayers() {
        return this.seenPlayers;
    }

    public @Nullable PlayerInfo getPlayerInfoIgnoreCase(String player) {
        for (PlayerInfo playerInfo : this.playerInfoMap.values()) {
            if (!playerInfo.getProfile().name().equalsIgnoreCase(player)) continue;
            return playerInfo;
        }
        return null;
    }

    public GameProfile getLocalGameProfile() {
        return this.localGameProfile;
    }

    public ClientAdvancements getAdvancements() {
        return this.advancements;
    }

    public CommandDispatcher<ClientSuggestionProvider> getCommands() {
        return this.commands;
    }

    public ClientLevel getLevel() {
        return this.level;
    }

    public DebugQueryHandler getDebugQueryHandler() {
        return this.debugQueryHandler;
    }

    public Set<ResourceKey<Level>> levels() {
        return this.levels;
    }

    public RegistryAccess.Frozen registryAccess() {
        return this.registryAccess;
    }

    public void markMessageAsProcessed(MessageSignature signature, boolean wasShown) {
        if (this.lastSeenMessages.addPending(signature, wasShown) && this.lastSeenMessages.offset() > 64) {
            this.sendChatAcknowledgement();
        }
    }

    private void sendChatAcknowledgement() {
        int offset = this.lastSeenMessages.getAndClearOffset();
        if (offset > 0) {
            this.send(new ServerboundChatAckPacket(offset));
        }
    }

    public void sendChat(String content) {
        Instant timeStamp = Instant.now();
        long salt = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update lastSeenUpdate = this.lastSeenMessages.generateAndApplyUpdate();
        MessageSignature signature = this.signedMessageEncoder.pack(new SignedMessageBody(content, timeStamp, salt, lastSeenUpdate.lastSeen()));
        this.send(new ServerboundChatPacket(content, timeStamp, salt, signature, lastSeenUpdate.update()));
    }

    public void sendCommand(String command) {
        SignableCommand signableCommand = SignableCommand.of(this.commands.parse(command, (Object)this.suggestionsProvider));
        if (signableCommand.arguments().isEmpty()) {
            this.send(new ServerboundChatCommandPacket(command));
            return;
        }
        Instant timeStamp = Instant.now();
        long salt = Crypt.SaltSupplier.getLong();
        LastSeenMessagesTracker.Update lastSeenUpdate = this.lastSeenMessages.generateAndApplyUpdate();
        ArgumentSignatures argumentSignatures = ArgumentSignatures.signCommand(signableCommand, argument -> {
            SignedMessageBody signedBody = new SignedMessageBody(argument, timeStamp, salt, lastSeenUpdate.lastSeen());
            return this.signedMessageEncoder.pack(signedBody);
        });
        this.send(new ServerboundChatCommandSignedPacket(command, timeStamp, salt, argumentSignatures, lastSeenUpdate.update()));
    }

    public void sendUnattendedCommand(String command, @Nullable Screen screenAfterCommand) {
        switch (this.verifyCommand(command).ordinal()) {
            case 0: {
                this.send(new ServerboundChatCommandPacket(command));
                this.minecraft.gui.setScreen(screenAfterCommand);
                break;
            }
            case 1: {
                this.openCommandSendConfirmationWindow(command, "multiplayer.confirm_command.parse_errors", screenAfterCommand);
                break;
            }
            case 3: {
                this.openCommandSendConfirmationWindow(command, "multiplayer.confirm_command.permissions_required", screenAfterCommand);
                break;
            }
            case 2: {
                this.openSignedCommandSendConfirmationWindow(command, "multiplayer.confirm_command.signature_required", screenAfterCommand);
            }
        }
    }

    private CommandCheckResult verifyCommand(String command) {
        ParseResults parseWithCurrentPermissions = this.commands.parse(command, (Object)this.suggestionsProvider);
        if (!ClientPacketListener.isValidCommand(parseWithCurrentPermissions)) {
            return CommandCheckResult.PARSE_ERRORS;
        }
        if (SignableCommand.hasSignableArguments(parseWithCurrentPermissions)) {
            return CommandCheckResult.SIGNATURE_REQUIRED;
        }
        ParseResults parseWithoutPermissions = this.commands.parse(command, (Object)this.restrictedSuggestionsProvider);
        if (!ClientPacketListener.isValidCommand(parseWithoutPermissions)) {
            return CommandCheckResult.PERMISSIONS_REQUIRED;
        }
        return CommandCheckResult.NO_ISSUES;
    }

    private static boolean isValidCommand(ParseResults<?> parseResults) {
        return !parseResults.getReader().canRead() && parseResults.getExceptions().isEmpty() && parseResults.getContext().getLastChild().getCommand() != null;
    }

    private void openSendConfirmationWindow(String command, String messageKey, Component acceptButton, Runnable onAccept) {
        Screen currentScreen = this.minecraft.gui.screen();
        this.minecraft.gui.setScreen(new ConfirmScreen(result -> {
            if (result) {
                onAccept.run();
            } else {
                this.minecraft.gui.setScreen(currentScreen);
            }
        }, COMMAND_SEND_CONFIRM_TITLE, Component.translatable(messageKey, Component.literal(command).withStyle(ChatFormatting.YELLOW)), acceptButton, currentScreen != null ? CommonComponents.GUI_BACK : CommonComponents.GUI_CANCEL));
    }

    private void openCommandSendConfirmationWindow(String command, String messageKey, @Nullable Screen screenAfterCommand) {
        this.openSendConfirmationWindow(command, messageKey, BUTTON_RUN_COMMAND, () -> {
            this.send(new ServerboundChatCommandPacket(command));
            this.minecraft.gui.setScreen(screenAfterCommand);
        });
    }

    private void openSignedCommandSendConfirmationWindow(String command, String messageKey, @Nullable Screen screenAfterCommand) {
        boolean canOpenChatScreen;
        boolean bl = canOpenChatScreen = screenAfterCommand == null && this.minecraft.player != null && this.minecraft.player.chatAbilities().canSendCommands();
        if (canOpenChatScreen) {
            this.openSendConfirmationWindow(command, messageKey, BUTTON_SUGGEST_COMMAND, () -> this.minecraft.gui.openChatAndAddText(ChatComponent.ChatMethod.COMMAND, command));
        } else {
            this.openSendConfirmationWindow(command, messageKey, CommonComponents.GUI_COPY_TO_CLIPBOARD, () -> {
                this.minecraft.keyboardHandler.setClipboard("/" + command);
                this.minecraft.gui.setScreen(screenAfterCommand);
            });
        }
    }

    public void broadcastClientInformation(ClientInformation information) {
        if (!information.equals(this.remoteClientInformation)) {
            this.send(new ServerboundClientInformationPacket(information));
            this.remoteClientInformation = information;
        }
    }

    @Override
    public void tick() {
        if (this.chatSession != null && this.minecraft.getProfileKeyPairManager().shouldRefreshKeyPair()) {
            this.prepareKeyPair();
        }
        if (this.keyPairFuture != null && this.keyPairFuture.isDone()) {
            this.keyPairFuture.join().ifPresent(this::setKeyPair);
            this.keyPairFuture = null;
        }
        this.sendDeferredPackets();
        if (this.minecraft.getDebugOverlay().showNetworkCharts()) {
            this.pingDebugMonitor.tick();
        }
        if (this.level != null) {
            this.debugSubscriber.tick(this.level.getGameTime());
        }
        this.telemetryManager.tick();
        if (this.levelLoadTracker != null) {
            this.levelLoadTracker.tickClientLoad();
            if (this.levelLoadTracker.isLevelReady()) {
                this.notifyPlayerLoaded();
                this.levelLoadTracker = null;
            }
        }
    }

    private void notifyPlayerLoaded() {
        if (!this.hasClientLoaded()) {
            this.connection.send(new ServerboundPlayerLoadedPacket());
            this.setClientLoaded(true);
        }
    }

    public @Nullable Runnable getPlayerCompiledSectionCallback() {
        return this.levelLoadTracker != null ? this.levelLoadTracker.getPlayerCompiledSectionCallback() : null;
    }

    public void prepareKeyPair() {
        this.keyPairFuture = this.minecraft.getProfileKeyPairManager().prepareKeyPair();
    }

    private void setKeyPair(ProfileKeyPair keyPair) {
        if (!this.minecraft.isLocalPlayer(this.localGameProfile.id())) {
            return;
        }
        if (this.chatSession != null && this.chatSession.keyPair().equals(keyPair)) {
            return;
        }
        this.chatSession = LocalChatSession.create(keyPair);
        this.signedMessageEncoder = this.chatSession.createMessageEncoder(this.localGameProfile.id());
        this.send(new ServerboundChatSessionUpdatePacket(this.chatSession.asRemote().asData()));
    }

    @Override
    protected DialogConnectionAccess createDialogAccess() {
        return new ClientCommonPacketListenerImpl.CommonDialogAccess(this){
            final /* synthetic */ ClientPacketListener this$0;
            {
                ClientPacketListener clientPacketListener = this$0;
                Objects.requireNonNull(clientPacketListener);
                this.this$0 = clientPacketListener;
                super(this$0);
            }

            @Override
            public void runCommand(String command, @Nullable Screen activeScreen) {
                this.this$0.sendUnattendedCommand(command, activeScreen);
            }
        };
    }

    public @Nullable ServerData getServerData() {
        return this.serverData;
    }

    public FeatureFlagSet enabledFeatures() {
        return this.enabledFeatures;
    }

    public boolean isFeatureEnabled(FeatureFlagSet requiredFlags) {
        return requiredFlags.isSubsetOf(this.enabledFeatures());
    }

    public Scoreboard scoreboard() {
        return this.scoreboard;
    }

    public PotionBrewing potionBrewing() {
        return this.potionBrewing;
    }

    public FuelValues fuelValues() {
        return this.fuelValues;
    }

    public void updateSearchTrees() {
        this.searchTrees.rebuildAfterLanguageChange();
    }

    public SessionSearchTrees searchTrees() {
        return this.searchTrees;
    }

    public void registerForCleaning(CacheSlot<?, ?> slot) {
        this.cacheSlots.add(new WeakReference(slot));
    }

    public HashedPatchMap.HashGenerator decoratedHashOpsGenenerator() {
        return this.decoratedHashOpsGenerator;
    }

    public ClientWaypointManager getWaypointManager() {
        return this.waypointManager;
    }

    public DebugValueAccess createDebugValueAccess() {
        return this.debugSubscriber.createDebugValueAccess(this.level);
    }

    public boolean hasClientLoaded() {
        return this.clientLoaded;
    }

    private void setClientLoaded(boolean loaded) {
        this.clientLoaded = loaded;
    }

    public ClientClockManager clockManager() {
        return this.clockManager;
    }

    @Environment(value=EnvType.CLIENT)
    private static enum CommandCheckResult {
        NO_ISSUES,
        PARSE_ERRORS,
        SIGNATURE_REQUIRED,
        PERMISSIONS_REQUIRED;

    }
}

