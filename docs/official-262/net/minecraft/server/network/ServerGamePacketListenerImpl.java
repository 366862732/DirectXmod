/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.primitives.Floats
 *  com.mojang.authlib.GameProfile
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.ParseResults
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMaps
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.HashedStack;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.LastSeenMessagesValidator;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.FetchProfileCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.FutureChain;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.StringUtil;
import net.minecraft.util.TickThrottler;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerGamePacketListenerImpl
extends ServerCommonPacketListenerImpl
implements ServerGamePacketListener,
ServerPlayerConnection,
TickablePacketListener,
GameProtocols.Context {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_BLOCK_UPDATES_TO_ACK = -1;
    private static final int TRACKED_MESSAGE_DISCONNECT_THRESHOLD = 4096;
    private static final int MAXIMUM_FLYING_TICKS = 80;
    private static final int ATTACK_INDICATOR_TOLERANCE_TICKS = 5;
    public static final int CLIENT_LOADED_TIMEOUT_TIME = 60;
    private static final Component CHAT_VALIDATION_FAILED = Component.translatable("multiplayer.disconnect.chat_validation_failed");
    private static final Component INVALID_COMMAND_SIGNATURE = Component.translatable("chat.disabled.invalid_command_signature").withStyle(ChatFormatting.RED);
    private static final int MAX_COMMAND_SUGGESTIONS = 1000;
    public ServerPlayer player;
    public final PlayerChunkSender chunkSender;
    private int tickCount;
    private int ackBlockChangesUpTo = -1;
    private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1480);
    private final TickThrottler chatSpamThrottler;
    private final TickThrottler commandSpamThrottler;
    private double firstGoodX;
    private double firstGoodY;
    private double firstGoodZ;
    private double lastGoodX;
    private double lastGoodY;
    private double lastGoodZ;
    private @Nullable Entity lastVehicle;
    private double vehicleFirstGoodX;
    private double vehicleFirstGoodY;
    private double vehicleFirstGoodZ;
    private double vehicleLastGoodX;
    private double vehicleLastGoodY;
    private double vehicleLastGoodZ;
    private @Nullable Vec3 awaitingPositionFromClient;
    private int awaitingTeleport;
    private int awaitingTeleportTime;
    private boolean clientIsFloating;
    private int aboveGroundTickCount;
    private boolean clientVehicleIsFloating;
    private int aboveGroundVehicleTickCount;
    private int receivedMovePacketCount;
    private int knownMovePacketCount;
    private boolean receivedMovementThisTick;
    private @Nullable RemoteChatSession chatSession;
    private SignedMessageChain.Decoder signedMessageDecoder;
    private final LastSeenMessagesValidator lastSeenMessages = new LastSeenMessagesValidator(20);
    private int nextChatIndex;
    private final MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
    private final FutureChain chatMessageChain;
    private boolean waitingForSwitchToConfig;
    private boolean waitingForRespawn;
    private int clientLoadedTimeoutTimer;

    public ServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, cookie);
        this.commandSpamThrottler = new TickThrottler(20, 20 * server.getCommandSpamThresholdSeconds());
        this.chatSpamThrottler = new TickThrottler(20, 20 * server.getChatSpamThresholdSeconds());
        this.restartClientLoadTimerAfterRespawn();
        this.chunkSender = new PlayerChunkSender(connection.isMemoryConnection());
        this.player = player;
        player.connection = this;
        player.getTextFilter().join();
        this.signedMessageDecoder = SignedMessageChain.Decoder.unsigned(player.getUUID(), server::enforceSecureProfile);
        this.chatMessageChain = new FutureChain(server);
    }

    @Override
    public void tick() {
        if (this.ackBlockChangesUpTo > -1) {
            this.send(new ClientboundBlockChangedAckPacket(this.ackBlockChangesUpTo));
            this.ackBlockChangesUpTo = -1;
        }
        if (!this.server.isPaused() && this.tickPlayer()) {
            return;
        }
        this.keepConnectionAlive();
        this.chatSpamThrottler.tick();
        this.commandSpamThrottler.tick();
        this.dropSpamThrottler.tick();
        if (this.player.getLastActionTime() > 0L && this.server.playerIdleTimeout() > 0 && Util.getMillis() - this.player.getLastActionTime() > TimeUnit.MINUTES.toMillis(this.server.playerIdleTimeout()) && !this.player.wonGame) {
            this.disconnect(Component.translatable("multiplayer.disconnect.idling"));
        }
    }

    private boolean tickPlayer() {
        this.resetPosition();
        this.player.xo = this.player.getX();
        this.player.yo = this.player.getY();
        this.player.zo = this.player.getZ();
        this.player.doTick();
        this.player.absSnapTo(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
        ++this.tickCount;
        this.knownMovePacketCount = this.receivedMovePacketCount;
        if (this.clientIsFloating && !this.player.isSleeping() && !this.player.isPassenger() && !this.player.isDeadOrDying()) {
            if (++this.aboveGroundTickCount > this.getMaximumFlyingTicks(this.player)) {
                LOGGER.warn("{} was kicked for floating too long!", (Object)this.player.getPlainTextName());
                this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
                return true;
            }
        } else {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }
        this.lastVehicle = this.player.getRootVehicle();
        if (this.lastVehicle == this.player || this.lastVehicle.getControllingPassenger() != this.player) {
            this.lastVehicle = null;
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
        } else {
            this.vehicleFirstGoodX = this.lastVehicle.getX();
            this.vehicleFirstGoodY = this.lastVehicle.getY();
            this.vehicleFirstGoodZ = this.lastVehicle.getZ();
            this.vehicleLastGoodX = this.lastVehicle.getX();
            this.vehicleLastGoodY = this.lastVehicle.getY();
            this.vehicleLastGoodZ = this.lastVehicle.getZ();
            if (this.clientVehicleIsFloating && this.lastVehicle.getControllingPassenger() == this.player) {
                if (++this.aboveGroundVehicleTickCount > this.getMaximumFlyingTicks(this.lastVehicle)) {
                    LOGGER.warn("{} was kicked for floating a vehicle too long!", (Object)this.player.getPlainTextName());
                    this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
                    return true;
                }
            } else {
                this.clientVehicleIsFloating = false;
                this.aboveGroundVehicleTickCount = 0;
            }
        }
        return false;
    }

    private int getMaximumFlyingTicks(Entity entity) {
        double gravity = entity.getGravity();
        if (gravity < (double)1.0E-5f) {
            return Integer.MAX_VALUE;
        }
        double gravityModifier = 0.08 / gravity;
        return Mth.ceil(80.0 * Math.max(gravityModifier, 1.0));
    }

    public void resetFlyingTicks() {
        this.aboveGroundTickCount = 0;
        this.aboveGroundVehicleTickCount = 0;
    }

    public void resetPosition() {
        this.firstGoodX = this.player.getX();
        this.firstGoodY = this.player.getY();
        this.firstGoodZ = this.player.getZ();
        this.lastGoodX = this.player.getX();
        this.lastGoodY = this.player.getY();
        this.lastGoodZ = this.player.getZ();
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected() && !this.waitingForSwitchToConfig;
    }

    @Override
    public boolean shouldHandleMessage(Packet<?> packet) {
        if (super.shouldHandleMessage(packet)) {
            return true;
        }
        return this.waitingForSwitchToConfig && this.connection.isConnected() && packet instanceof ServerboundConfigurationAcknowledgedPacket;
    }

    @Override
    protected GameProfile playerProfile() {
        return this.player.getGameProfile();
    }

    private <T, R> CompletableFuture<R> filterTextPacket(T message, BiFunction<TextFilter, T, CompletableFuture<R>> action) {
        return action.apply(this.player.getTextFilter(), (TextFilter)message).thenApply(result -> {
            if (!this.isAcceptingMessages()) {
                LOGGER.debug("Ignoring packet due to disconnection");
                throw new CancellationException("disconnected");
            }
            return result;
        });
    }

    private CompletableFuture<FilteredText> filterTextPacket(String message) {
        return this.filterTextPacket(message, TextFilter::processStreamMessage);
    }

    private CompletableFuture<List<FilteredText>> filterTextPacket(List<String> message) {
        return this.filterTextPacket(message, TextFilter::processMessageBundle);
    }

    @Override
    public void handlePlayerInput(ServerboundPlayerInputPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.setLastClientInput(packet.input());
        if (this.hasClientLoaded()) {
            this.player.resetLastActionTime();
            this.player.setShiftKeyDown(packet.input().shift());
        }
    }

    private static boolean containsInvalidValues(double x, double y, double z, float yRot, float xRot) {
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || !Floats.isFinite((float)xRot) || !Floats.isFinite((float)yRot);
    }

    private static double clampHorizontal(double value) {
        return Mth.clamp(value, -3.0E7, 3.0E7);
    }

    private static double clampVertical(double value) {
        return Mth.clamp(value, -2.0E7, 2.0E7);
    }

    @Override
    public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (ServerGamePacketListenerImpl.containsInvalidValues(packet.position().x(), packet.position().y(), packet.position().z(), packet.yRot(), packet.xRot())) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
            return;
        }
        if (this.updateAwaitingTeleport() || !this.hasClientLoaded()) {
            return;
        }
        Entity vehicle = this.player.getRootVehicle();
        if (vehicle != this.player && vehicle.getControllingPassenger() == this.player && vehicle == this.lastVehicle) {
            LivingEntity livingVehicle;
            ServerLevel level = this.player.level();
            double oldX = vehicle.getX();
            double oldY = vehicle.getY();
            double oldZ = vehicle.getZ();
            double targetX = ServerGamePacketListenerImpl.clampHorizontal(packet.position().x());
            double targetY = ServerGamePacketListenerImpl.clampVertical(packet.position().y());
            double targetZ = ServerGamePacketListenerImpl.clampHorizontal(packet.position().z());
            float targetYRot = Mth.wrapDegrees(packet.yRot());
            float targetXRot = Mth.wrapDegrees(packet.xRot());
            double xDist = targetX - this.vehicleFirstGoodX;
            double yDist = targetY - this.vehicleFirstGoodY;
            double zDist = targetZ - this.vehicleFirstGoodZ;
            double movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
            double expectedDist = vehicle.getDeltaMovement().lengthSqr();
            if (movedDist - expectedDist > 100.0 && !this.isSingleplayerOwner()) {
                LOGGER.warn("{} (vehicle of {}) moved too quickly! {},{},{}", new Object[]{vehicle.getPlainTextName(), this.player.getPlainTextName(), xDist, yDist, zDist});
                this.send(ClientboundMoveVehiclePacket.fromEntity(vehicle));
                return;
            }
            AABB oldAABB = vehicle.getBoundingBox();
            xDist = targetX - this.vehicleLastGoodX;
            yDist = targetY - this.vehicleLastGoodY;
            zDist = targetZ - this.vehicleLastGoodZ;
            boolean vehicleRestsOnSomething = vehicle.verticalCollisionBelow;
            if (vehicle instanceof LivingEntity && (livingVehicle = (LivingEntity)vehicle).onClimbable()) {
                livingVehicle.resetFallDistance();
            }
            vehicle.move(MoverType.PLAYER, new Vec3(xDist, yDist, zDist));
            double oyDist = yDist;
            xDist = targetX - vehicle.getX();
            yDist = targetY - vehicle.getY();
            if (yDist > -0.5 || yDist < 0.5) {
                yDist = 0.0;
            }
            zDist = targetZ - vehicle.getZ();
            movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
            boolean fail = false;
            if (movedDist > 0.0625) {
                fail = true;
                LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", new Object[]{vehicle.getPlainTextName(), this.player.getPlainTextName(), Math.sqrt(movedDist)});
            }
            if (fail && level.noCollision(vehicle, oldAABB) || this.isEntityCollidingWithAnythingNew(level, vehicle, oldAABB, targetX, targetY, targetZ)) {
                vehicle.absSnapTo(oldX, oldY, oldZ, targetYRot, targetXRot);
                this.send(ClientboundMoveVehiclePacket.fromEntity(vehicle));
                vehicle.removeLatestMovementRecording();
                return;
            }
            vehicle.absSnapTo(targetX, targetY, targetZ, targetYRot, targetXRot);
            this.player.level().getChunkSource().move(this.player);
            Vec3 clientDeltaMovement = new Vec3(vehicle.getX() - oldX, vehicle.getY() - oldY, vehicle.getZ() - oldZ);
            this.handlePlayerKnownMovement(clientDeltaMovement);
            vehicle.setOnGroundWithMovement(packet.onGround(), clientDeltaMovement);
            vehicle.doCheckFallDamage(clientDeltaMovement.x, clientDeltaMovement.y, clientDeltaMovement.z, packet.onGround());
            this.player.checkMovementStatistics(clientDeltaMovement.x, clientDeltaMovement.y, clientDeltaMovement.z);
            this.clientVehicleIsFloating = oyDist >= -0.03125 && !vehicleRestsOnSomething && !this.server.allowFlight() && !vehicle.isFlyingVehicle() && !vehicle.isNoGravity() && this.noBlocksAround(vehicle);
            this.vehicleLastGoodX = vehicle.getX();
            this.vehicleLastGoodY = vehicle.getY();
            this.vehicleLastGoodZ = vehicle.getZ();
        }
    }

    private boolean noBlocksAround(Entity entity) {
        return entity.level().getBlockStates(entity.getBoundingBox().inflate(0.0625).expandTowards(0.0, -0.55, 0.0)).allMatch(BlockBehaviour.BlockStateBase::isAir);
    }

    @Override
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (packet.getId() == this.awaitingTeleport) {
            if (this.awaitingPositionFromClient == null) {
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
                return;
            }
            this.player.absSnapTo(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            this.lastGoodX = this.awaitingPositionFromClient.x;
            this.lastGoodY = this.awaitingPositionFromClient.y;
            this.lastGoodZ = this.awaitingPositionFromClient.z;
            this.player.hasChangedDimension();
            this.awaitingPositionFromClient = null;
        }
    }

    @Override
    public void handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.markClientLoaded();
    }

    @Override
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        RecipeManager.ServerDisplayInfo entry = this.server.getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (entry != null) {
            this.player.getRecipeBook().removeHighlight(entry.parent().id());
        }
    }

    @Override
    public void handleBundleItemSelectedPacket(ServerboundSelectBundleItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.containerMenu.setSelectedBundleItemIndex(packet.slotId(), packet.selectedItemIndex());
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.getRecipeBook().setBookSetting(packet.getBookType(), packet.isOpen(), packet.isFiltering());
    }

    @Override
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (packet.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            Identifier id = Objects.requireNonNull(packet.getTab());
            AdvancementHolder advancement = this.server.getAdvancements().get(id);
            if (advancement != null) {
                this.player.getAdvancements().setSelectedTab(advancement);
            }
        }
    }

    @Override
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        StringReader command = new StringReader(packet.getCommand());
        if (command.canRead() && command.peek() == '/') {
            command.skip();
        }
        ParseResults parse = this.server.getCommands().getDispatcher().parse(command, (Object)this.player.createCommandSourceStack());
        this.server.getCommands().getDispatcher().getCompletionSuggestions(parse).thenAccept(results -> {
            Suggestions suggestions = results.getList().size() <= 1000 ? results : new Suggestions(results.getRange(), results.getList().subList(0, 1000));
            this.send(new ClientboundCommandSuggestionsPacket(packet.getId(), suggestions));
        });
    }

    @Override
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
            return;
        }
        BaseCommandBlock commandBlock = null;
        CommandBlockEntity autoCommandBlock = null;
        BlockPos blockPos = packet.getPos();
        BlockEntity blockEntity = this.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof CommandBlockEntity) {
            CommandBlockEntity commandBlockEntity;
            autoCommandBlock = commandBlockEntity = (CommandBlockEntity)blockEntity;
            commandBlock = autoCommandBlock.getCommandBlock();
        }
        String command = packet.getCommand();
        boolean trackOutput = packet.isTrackOutput();
        if (commandBlock != null) {
            CommandBlockEntity.Mode oldMode = autoCommandBlock.getMode();
            BlockState currentBlockState = this.player.level().getBlockState(blockPos);
            Direction direction = currentBlockState.getValue(CommandBlock.FACING);
            BlockState baseBlockState = switch (packet.getMode()) {
                case CommandBlockEntity.Mode.SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
                case CommandBlockEntity.Mode.AUTO -> Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
                default -> Blocks.COMMAND_BLOCK.defaultBlockState();
            };
            BlockState blockState = (BlockState)((BlockState)baseBlockState.setValue(CommandBlock.FACING, direction)).setValue(CommandBlock.CONDITIONAL, packet.isConditional());
            if (blockState != currentBlockState) {
                this.player.level().setBlock(blockPos, blockState, 2);
                blockEntity.setBlockState(blockState);
                this.player.level().getChunkAt(blockPos).setBlockEntity(blockEntity);
            }
            commandBlock.setCommand(command);
            commandBlock.setTrackOutput(trackOutput);
            if (!trackOutput) {
                commandBlock.setLastOutput(null);
            }
            autoCommandBlock.setAutomatic(packet.isAutomatic());
            if (oldMode != packet.getMode()) {
                autoCommandBlock.onModeSwitch();
            }
            if (this.player.level().isCommandBlockEnabled()) {
                commandBlock.onUpdated(this.player.level());
            }
            if (!StringUtil.isNullOrEmpty(command)) {
                this.player.sendSystemMessage(Component.translatable(this.player.level().isCommandBlockEnabled() ? "advMode.setCommand.success" : "advMode.setCommand.disabled", command));
            }
        }
    }

    @Override
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
            return;
        }
        BaseCommandBlock commandBlock = packet.getCommandBlock(this.player.level());
        if (commandBlock != null) {
            boolean commandBlockEnabled;
            String command = packet.getCommand();
            commandBlock.setCommand(command);
            commandBlock.setTrackOutput(packet.isTrackOutput());
            if (!packet.isTrackOutput()) {
                commandBlock.setLastOutput(null);
            }
            if (commandBlockEnabled = this.player.level().isCommandBlockEnabled()) {
                commandBlock.onUpdated(this.player.level());
            }
            if (!StringUtil.isNullOrEmpty(command)) {
                this.player.sendSystemMessage(Component.translatable(commandBlockEnabled ? "advMode.setCommand.success" : "advMode.setCommand.disabled", command));
            }
        }
    }

    @Override
    public void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket packet) {
        boolean includeData;
        ServerLevel level = this.player.level();
        PacketUtils.ensureRunningOnSameThread(packet, this, level);
        BlockPos pos = packet.pos();
        if (!this.player.isWithinBlockInteractionRange(pos, 1.0)) {
            return;
        }
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState blockState = level.getBlockState(pos);
        ItemStack itemStack = blockState.getCloneItemStack(level, pos, includeData = this.player.hasInfiniteMaterials() && packet.includeData());
        if (itemStack.isEmpty()) {
            return;
        }
        if (includeData) {
            ServerGamePacketListenerImpl.addBlockDataToItem(blockState, level, pos, itemStack);
        }
        this.tryPickItem(itemStack);
    }

    private static void addBlockDataToItem(BlockState blockState, ServerLevel level, BlockPos pos, ItemStack itemStack) {
        BlockEntity blockEntity;
        BlockEntity blockEntity2 = blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        if (blockEntity != null) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER);){
                TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                blockEntity.saveCustomOnly(output);
                blockEntity.removeComponentsFromTag(output);
                BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), output);
                itemStack.applyComponents(blockEntity.collectComponents());
            }
        }
    }

    @Override
    public void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket packet) {
        ServerLevel level = this.player.level();
        PacketUtils.ensureRunningOnSameThread(packet, this, level);
        Entity entity = level.getEntityOrPart(packet.id());
        if (entity == null || !this.player.isWithinEntityInteractionRange(entity, 3.0)) {
            return;
        }
        ItemStack itemStack = entity.getPickResult();
        if (itemStack != null && !itemStack.isEmpty()) {
            this.tryPickItem(itemStack);
        }
        if (packet.includeData() && this.player.canUseGameMasterBlocks() && entity instanceof Avatar) {
            Avatar avatar = (Avatar)entity;
            FetchProfileCommand.printForAvatar(this.player.createCommandSourceStack(), avatar);
        }
    }

    private void tryPickItem(ItemStack itemStack) {
        if (!itemStack.isItemEnabled(this.player.level().enabledFeatures())) {
            return;
        }
        Inventory inventory = this.player.getInventory();
        int slotWithExistingItem = inventory.findSlotMatchingItem(itemStack);
        if (slotWithExistingItem != -1) {
            if (Inventory.isHotbarSlot(slotWithExistingItem)) {
                inventory.setSelectedSlot(slotWithExistingItem);
            } else {
                inventory.pickSlot(slotWithExistingItem);
            }
        } else if (this.player.hasInfiniteMaterials()) {
            inventory.addAndPickItem(itemStack);
        }
        this.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
        this.player.inventoryMenu.broadcastChanges();
    }

    @Override
    public void handleRenameItem(ServerboundRenameItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        AbstractContainerMenu abstractContainerMenu = this.player.containerMenu;
        if (abstractContainerMenu instanceof AnvilMenu) {
            AnvilMenu menu = (AnvilMenu)abstractContainerMenu;
            if (!menu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", (Object)this.player, (Object)menu);
                return;
            }
            menu.setItemName(packet.getName());
        }
    }

    @Override
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        AbstractContainerMenu abstractContainerMenu = this.player.containerMenu;
        if (abstractContainerMenu instanceof BeaconMenu) {
            BeaconMenu menu = (BeaconMenu)abstractContainerMenu;
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", (Object)this.player, (Object)this.player.containerMenu);
                return;
            }
            if (!menu.updateEffects(packet.primary(), packet.secondary())) {
                this.disconnect(Component.translatable("multiplayer.disconnect.generic"));
                LOGGER.warn("Player {} tried to set invalid beacon effects", (Object)this.player);
            }
        }
    }

    @Override
    public void handleSetGameRule(ServerboundSetGameRulePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            LOGGER.warn("Player {} tried to set game rule values without required permissions", (Object)this.player.getGameProfile().name());
            return;
        }
        GameRules gameRules = this.player.level().getGameRules();
        for (ServerboundSetGameRulePacket.Entry entry : packet.entries()) {
            GameRule<?> rule = BuiltInRegistries.GAME_RULE.getValue(entry.gameRuleKey());
            if (rule != null) {
                this.setGameRuleValue(gameRules, rule, entry.value());
                continue;
            }
            LOGGER.warn("Received request to set unknown game rule: {}", entry.gameRuleKey());
        }
    }

    private <T> void setGameRuleValue(GameRules gameRules, GameRule<T> rule, String value) {
        rule.deserialize(value).result().ifPresent(parsedValue -> {
            gameRules.set(rule, parsedValue, this.server);
            this.broadcastGameRuleChangeToOperators(rule, parsedValue);
        });
    }

    private <T> void broadcastGameRuleChangeToOperators(GameRule<T> rule, T value) {
        MutableComponent message = Component.translatable("commands.gamerule.set", rule.id(), rule.serialize(value));
        PlayerList playerList = this.server.getPlayerList();
        playerList.getPlayers().stream().filter(op -> playerList.isOp(op.nameAndId())).forEach(op -> op.sendSystemMessage(message));
    }

    @Override
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            return;
        }
        BlockPos blockPos = packet.getPos();
        BlockState state = this.player.level().getBlockState(blockPos);
        BlockEntity blockEntity = this.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof StructureBlockEntity) {
            StructureBlockEntity structure = (StructureBlockEntity)blockEntity;
            structure.setMode(packet.getMode());
            structure.setStructureName(packet.getName());
            structure.setStructurePos(packet.getOffset());
            structure.setStructureSize(packet.getSize());
            structure.setMirror(packet.getMirror());
            structure.setRotation(packet.getRotation());
            structure.setMetaData(packet.getData());
            structure.setIgnoreEntities(packet.isIgnoreEntities());
            structure.setStrict(packet.isStrict());
            structure.setShowAir(packet.isShowAir());
            structure.setShowBoundingBox(packet.isShowBoundingBox());
            structure.setIntegrity(packet.getIntegrity());
            structure.setSeed(packet.getSeed());
            if (structure.hasStructureName()) {
                String actualStructureName = structure.getStructureName();
                if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                    if (structure.saveStructure()) {
                        this.player.sendSystemMessage(Component.translatable("structure_block.save_success", actualStructureName));
                    } else {
                        this.player.sendSystemMessage(Component.translatable("structure_block.save_failure", actualStructureName));
                    }
                } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                    if (!structure.isStructureLoadable()) {
                        this.player.sendSystemMessage(Component.translatable("structure_block.load_not_found", actualStructureName));
                    } else if (structure.placeStructureIfSameSize(this.player.level())) {
                        this.player.sendSystemMessage(Component.translatable("structure_block.load_success", actualStructureName));
                    } else {
                        this.player.sendSystemMessage(Component.translatable("structure_block.load_prepare", actualStructureName));
                    }
                } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                    if (structure.detectSize()) {
                        this.player.sendSystemMessage(Component.translatable("structure_block.size_success", actualStructureName));
                    } else {
                        this.player.sendSystemMessage(Component.translatable("structure_block.size_failure"));
                    }
                }
            } else {
                this.player.sendSystemMessage(Component.translatable("structure_block.invalid_structure_name", packet.getName()));
            }
            structure.setChanged();
            this.player.level().sendBlockUpdated(blockPos, state, state, 3);
        }
    }

    @Override
    public void handleSetTestBlock(ServerboundSetTestBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            return;
        }
        BlockPos blockPos = packet.position();
        BlockState initialState = this.player.level().getBlockState(blockPos);
        BlockEntity blockEntity = this.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof TestBlockEntity) {
            TestBlockEntity testBlock = (TestBlockEntity)blockEntity;
            testBlock.setMode(packet.mode());
            testBlock.setMessage(packet.message());
            testBlock.setChanged();
            this.player.level().sendBlockUpdated(blockPos, initialState, testBlock.getBlockState(), 3);
        }
    }

    @Override
    public void handleTestInstanceBlockAction(ServerboundTestInstanceBlockActionPacket packet) {
        BlockEntity blockEntity;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        BlockPos pos = packet.pos();
        if (!this.player.canUseGameMasterBlocks() || !((blockEntity = this.player.level().getBlockEntity(pos)) instanceof TestInstanceBlockEntity)) {
            return;
        }
        TestInstanceBlockEntity blockEntity2 = (TestInstanceBlockEntity)blockEntity;
        if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.QUERY || packet.action() == ServerboundTestInstanceBlockActionPacket.Action.INIT) {
            HolderLookup.RegistryLookup registry = this.player.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
            Optional test = packet.data().test().flatMap(((Registry)registry)::get);
            Component status = test.isPresent() ? ((GameTestInstance)((Holder.Reference)test.get()).value()).describe() : Component.translatable("test_instance.description.no_test").withStyle(ChatFormatting.RED);
            Optional<Object> size = packet.action() == ServerboundTestInstanceBlockActionPacket.Action.QUERY ? packet.data().test().flatMap(testKey -> TestInstanceBlockEntity.getStructureSize(this.player.level(), testKey)) : Optional.empty();
            this.connection.send(new ClientboundTestInstanceBlockStatus(status, size));
        } else {
            blockEntity2.set(packet.data());
            if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.RESET) {
                blockEntity2.resetTest(this.player::sendSystemMessage);
            } else if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.SAVE) {
                blockEntity2.saveTest(this.player::sendSystemMessage);
            } else if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.EXPORT) {
                blockEntity2.exportTest(this.player::sendSystemMessage);
            } else if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.RUN) {
                blockEntity2.runTest(this.player::sendSystemMessage);
            }
            BlockState state = this.player.level().getBlockState(pos);
            this.player.level().sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), state, 3);
        }
    }

    @Override
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            return;
        }
        BlockPos blockPos = packet.getPos();
        BlockState state = this.player.level().getBlockState(blockPos);
        BlockEntity blockEntity = this.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof JigsawBlockEntity) {
            JigsawBlockEntity jigsaw = (JigsawBlockEntity)blockEntity;
            jigsaw.setName(packet.getName());
            jigsaw.setTarget(packet.getTarget());
            jigsaw.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, packet.getPool()));
            jigsaw.setFinalState(packet.getFinalState());
            jigsaw.setJoint(packet.getJoint());
            jigsaw.setPlacementPriority(packet.getPlacementPriority());
            jigsaw.setSelectionPriority(packet.getSelectionPriority());
            jigsaw.setChanged();
            this.player.level().sendBlockUpdated(blockPos, state, state, 3);
        }
    }

    @Override
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            return;
        }
        BlockPos blockPos = packet.getPos();
        BlockEntity blockEntity = this.player.level().getBlockEntity(blockPos);
        if (blockEntity instanceof JigsawBlockEntity) {
            JigsawBlockEntity jigsaw = (JigsawBlockEntity)blockEntity;
            jigsaw.generate(this.player.level(), packet.levels(), packet.keepJigsaws());
        }
    }

    @Override
    public void handleSelectTrade(ServerboundSelectTradePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        int selection = packet.getItem();
        AbstractContainerMenu abstractContainerMenu = this.player.containerMenu;
        if (abstractContainerMenu instanceof MerchantMenu) {
            MerchantMenu menu = (MerchantMenu)abstractContainerMenu;
            if (!menu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", (Object)this.player, (Object)menu);
                return;
            }
            menu.setSelectionHint(selection);
            menu.tryMoveItems(selection);
        }
    }

    @Override
    public void handleEditBook(ServerboundEditBookPacket packet) {
        int slot = packet.slot();
        if (!Inventory.isHotbarSlot(slot) && slot != 40) {
            return;
        }
        ArrayList contents = Lists.newArrayList();
        Optional<String> title = packet.title();
        title.ifPresent(contents::add);
        contents.addAll(packet.pages());
        Consumer<List> handler = title.isPresent() ? filteredContents -> this.signBook((FilteredText)filteredContents.get(0), filteredContents.subList(1, filteredContents.size()), slot) : filteredContents -> this.updateBookContents((List<FilteredText>)filteredContents, slot);
        this.filterTextPacket(contents).thenAcceptAsync(handler, (Executor)this.server);
    }

    private void updateBookContents(List<FilteredText> contents, int slot) {
        ItemStack carried = this.player.getInventory().getItem(slot);
        if (!carried.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            return;
        }
        List<Filterable<String>> pages = contents.stream().map(this::filterableFromOutgoing).toList();
        carried.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
    }

    private void signBook(FilteredText title, List<FilteredText> contents, int slot) {
        ItemStack carried = this.player.getInventory().getItem(slot);
        if (!carried.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            return;
        }
        ItemStack writtenBook = carried.transmuteCopy(Items.WRITTEN_BOOK);
        writtenBook.remove(DataComponents.WRITABLE_BOOK_CONTENT);
        List<Filterable<Component>> pages = contents.stream().map(page -> this.filterableFromOutgoing((FilteredText)page).map(Component::literal)).toList();
        writtenBook.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(this.filterableFromOutgoing(title), this.player.getPlainTextName(), 0, pages, true));
        this.player.getInventory().setItem(slot, writtenBook);
    }

    private Filterable<String> filterableFromOutgoing(FilteredText text) {
        if (this.player.isTextFilteringEnabled()) {
            return Filterable.passThrough(text.filteredOrEmpty());
        }
        return Filterable.from(text);
    }

    @Override
    public void handleEntityTagQuery(ServerboundEntityTagQueryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        Entity entity = this.player.level().getEntity(packet.getEntityId());
        if (entity != null) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER);){
                TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                entity.saveWithoutId(output);
                CompoundTag result = output.buildResult();
                this.send(new ClientboundTagQueryPacket(packet.getTransactionId(), result));
            }
        }
    }

    @Override
    public void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket packet) {
        CrafterMenu crafterMenu;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.isSpectator() || packet.containerId() != this.player.containerMenu.containerId) {
            return;
        }
        Object object = this.player.containerMenu;
        if (object instanceof CrafterMenu && (object = (crafterMenu = (CrafterMenu)object).getContainer()) instanceof CrafterBlockEntity) {
            CrafterBlockEntity crafterBlockEntity = (CrafterBlockEntity)object;
            crafterBlockEntity.setSlotState(packet.slotId(), packet.newState());
        }
    }

    @Override
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        BlockEntity blockEntity = this.player.level().getBlockEntity(packet.getPos());
        CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata(this.player.registryAccess()) : null;
        this.send(new ClientboundTagQueryPacket(packet.getTransactionId(), tag));
    }

    @Override
    public void handleMovePlayer(ServerboundMovePlayerPacket packet) {
        boolean movedUpwards;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (ServerGamePacketListenerImpl.containsInvalidValues(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), packet.getYRot(0.0f), packet.getXRot(0.0f))) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
            return;
        }
        ServerLevel level = this.player.level();
        if (this.player.wonGame) {
            return;
        }
        if (this.tickCount == 0) {
            this.resetPosition();
        }
        if (!this.hasClientLoaded()) {
            return;
        }
        float targetYRot = Mth.wrapDegrees(packet.getYRot(this.player.getYRot()));
        float targetXRot = Mth.wrapDegrees(packet.getXRot(this.player.getXRot()));
        if (this.updateAwaitingTeleport()) {
            this.player.absSnapRotationTo(targetYRot, targetXRot);
            return;
        }
        double targetX = ServerGamePacketListenerImpl.clampHorizontal(packet.getX(this.player.getX()));
        double targetY = ServerGamePacketListenerImpl.clampVertical(packet.getY(this.player.getY()));
        double targetZ = ServerGamePacketListenerImpl.clampHorizontal(packet.getZ(this.player.getZ()));
        if (this.player.isPassenger()) {
            this.player.absSnapTo(this.player.getX(), this.player.getY(), this.player.getZ(), targetYRot, targetXRot);
            this.player.level().getChunkSource().move(this.player);
            return;
        }
        double startX = this.player.getX();
        double startY = this.player.getY();
        double startZ = this.player.getZ();
        double xDist = targetX - this.firstGoodX;
        double yDist = targetY - this.firstGoodY;
        double zDist = targetZ - this.firstGoodZ;
        double expectedDist = this.player.getDeltaMovement().lengthSqr();
        double movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
        if (this.player.isSleeping()) {
            if (movedDist > 1.0) {
                this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), targetYRot, targetXRot);
            }
            return;
        }
        boolean isFallFlying = this.player.isFallFlying();
        if (level.tickRateManager().runsNormally()) {
            ++this.receivedMovePacketCount;
            int deltaPackets = this.receivedMovePacketCount - this.knownMovePacketCount;
            if (deltaPackets > 5) {
                LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", (Object)this.player.getPlainTextName(), (Object)deltaPackets);
                deltaPackets = 1;
            }
            if (this.shouldCheckPlayerMovement(isFallFlying)) {
                float metersPerTick;
                float f = metersPerTick = isFallFlying ? 300.0f : 100.0f;
                if (movedDist - expectedDist > (double)(metersPerTick * (float)deltaPackets)) {
                    LOGGER.warn("{} moved too quickly! {},{},{}", new Object[]{this.player.getPlainTextName(), xDist, yDist, zDist});
                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                    return;
                }
            }
        }
        AABB oldAABB = this.player.getBoundingBox();
        xDist = targetX - this.lastGoodX;
        yDist = targetY - this.lastGoodY;
        zDist = targetZ - this.lastGoodZ;
        boolean bl = movedUpwards = yDist > 0.0;
        if (this.player.onGround() && !packet.isOnGround() && movedUpwards) {
            this.player.jumpFromGround();
        }
        boolean playerStandsOnSomething = this.player.verticalCollisionBelow;
        this.player.move(MoverType.PLAYER, new Vec3(xDist, yDist, zDist));
        double oyDist = yDist;
        xDist = targetX - this.player.getX();
        yDist = targetY - this.player.getY();
        if (yDist > -0.5 || yDist < 0.5) {
            yDist = 0.0;
        }
        zDist = targetZ - this.player.getZ();
        movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
        boolean fail = false;
        if (!(this.player.isChangingDimension() || !(movedDist > 0.0625) || this.player.isSleeping() || this.player.isCreative() || this.player.isSpectator() || this.player.isInPostImpulseGraceTime())) {
            fail = true;
            LOGGER.warn("{} moved wrongly!", (Object)this.player.getPlainTextName());
        }
        if (!this.player.noPhysics && !this.player.isSleeping() && (fail && level.noCollision(this.player, oldAABB) || this.isEntityCollidingWithAnythingNew(level, this.player, oldAABB, targetX, targetY, targetZ))) {
            this.teleport(startX, startY, startZ, targetYRot, targetXRot);
            this.player.doCheckFallDamage(this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ, packet.isOnGround());
            this.player.removeLatestMovementRecording();
            return;
        }
        this.player.absSnapTo(targetX, targetY, targetZ, targetYRot, targetXRot);
        boolean isAutoSpinAttack = this.player.isAutoSpinAttack();
        this.clientIsFloating = oyDist >= -0.03125 && !playerStandsOnSomething && !this.player.isSpectator() && !this.server.allowFlight() && !this.player.getAbilities().mayfly && !this.player.hasEffect(MobEffects.LEVITATION) && !isFallFlying && !isAutoSpinAttack && this.noBlocksAround(this.player);
        this.player.level().getChunkSource().move(this.player);
        Vec3 clientDeltaMovement = new Vec3(this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ);
        this.player.setOnGroundWithMovement(packet.isOnGround(), packet.horizontalCollision(), clientDeltaMovement);
        this.player.doCheckFallDamage(clientDeltaMovement.x, clientDeltaMovement.y, clientDeltaMovement.z, packet.isOnGround());
        this.handlePlayerKnownMovement(clientDeltaMovement);
        if (movedUpwards) {
            this.player.resetFallDistance();
        }
        if (packet.isOnGround() || this.player.hasLandedInLiquid() || this.player.onClimbable() || this.player.isSpectator() || isFallFlying || isAutoSpinAttack) {
            this.player.tryResetCurrentImpulseContext();
        }
        this.player.checkMovementStatistics(this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ);
        this.lastGoodX = this.player.getX();
        this.lastGoodY = this.player.getY();
        this.lastGoodZ = this.player.getZ();
    }

    private boolean shouldCheckPlayerMovement(boolean isFallFlying) {
        if (this.isSingleplayerOwner()) {
            return false;
        }
        if (this.player.isChangingDimension()) {
            return false;
        }
        GameRules gameRules = this.player.level().getGameRules();
        if (!gameRules.get(GameRules.PLAYER_MOVEMENT_CHECK).booleanValue()) {
            return false;
        }
        return !isFallFlying || gameRules.get(GameRules.ELYTRA_MOVEMENT_CHECK) != false;
    }

    private boolean updateAwaitingTeleport() {
        if (this.awaitingPositionFromClient != null) {
            if (this.tickCount - this.awaitingTeleportTime > 20) {
                this.awaitingTeleportTime = this.tickCount;
                this.teleport(this.awaitingPositionFromClient.x, this.awaitingPositionFromClient.y, this.awaitingPositionFromClient.z, this.player.getYRot(), this.player.getXRot());
            }
            return true;
        }
        this.awaitingTeleportTime = this.tickCount;
        return false;
    }

    private boolean isEntityCollidingWithAnythingNew(LevelReader level, Entity entity, AABB oldAABB, double newX, double newY, double newZ) {
        AABB newAABB = entity.getBoundingBox().move(newX - entity.getX(), newY - entity.getY(), newZ - entity.getZ());
        Iterable<VoxelShape> newCollisions = level.getPreMoveCollisions(entity, newAABB.deflate(1.0E-5f), oldAABB.getBottomCenter());
        VoxelShape oldShape = Shapes.create(oldAABB.deflate(1.0E-5f));
        for (VoxelShape shape : newCollisions) {
            if (Shapes.joinIsNotEmpty(shape, oldShape, BooleanOp.AND)) continue;
            return true;
        }
        return false;
    }

    public void teleport(double x, double y, double z, float yRot, float xRot) {
        this.teleport(new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yRot, xRot), Collections.emptySet());
    }

    public void teleport(PositionMoveRotation destination, Set<Relative> relatives) {
        this.awaitingTeleportTime = this.tickCount;
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }
        this.player.teleportSetPosition(destination, relatives);
        this.awaitingPositionFromClient = this.player.position();
        this.send(ClientboundPlayerPositionPacket.of(this.awaitingTeleport, destination, relatives));
    }

    @Override
    public void handlePlayerAction(ServerboundPlayerActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded()) {
            return;
        }
        BlockPos pos = packet.getPos();
        this.player.resetLastActionTime();
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        switch (action) {
            case STAB: {
                if (this.player.isSpectator()) {
                    return;
                }
                ItemStack itemInHand = this.player.getItemInHand(InteractionHand.MAIN_HAND);
                if (this.player.cannotAttackWithItem(itemInHand, 5)) {
                    return;
                }
                PiercingWeapon piercingWeapon = itemInHand.get(DataComponents.PIERCING_WEAPON);
                if (piercingWeapon != null) {
                    piercingWeapon.attack(this.player, EquipmentSlot.MAINHAND);
                }
                return;
            }
            case SWAP_ITEM_WITH_OFFHAND: {
                if (!this.player.isSpectator()) {
                    ItemStack swap = this.player.getItemInHand(InteractionHand.OFF_HAND);
                    this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
                    this.player.setItemInHand(InteractionHand.MAIN_HAND, swap);
                    this.player.stopUsingItem();
                }
                return;
            }
            case DROP_ITEM: {
                if (!this.player.isSpectator()) {
                    this.player.drop(false);
                }
                return;
            }
            case DROP_ALL_ITEMS: {
                if (!this.player.isSpectator()) {
                    this.player.drop(true);
                }
                return;
            }
            case RELEASE_USE_ITEM: {
                this.player.releaseUsingItem();
                return;
            }
            case START_DESTROY_BLOCK: 
            case ABORT_DESTROY_BLOCK: 
            case STOP_DESTROY_BLOCK: {
                this.player.gameMode.handleBlockBreakAction(pos, action, packet.getDirection(), this.player.level().getMaxY(), packet.getSequence());
                this.ackBlockChangesUpTo(packet.getSequence());
                return;
            }
        }
        throw new IllegalArgumentException("Invalid player action");
    }

    private static boolean wasBlockPlacementAttempt(ServerPlayer player, ItemStack itemStack) {
        BucketItem bucket;
        if (itemStack.isEmpty()) {
            return false;
        }
        Item item = itemStack.getItem();
        return (item instanceof BlockItem || item instanceof BucketItem && (bucket = (BucketItem)item).getContent() != Fluids.EMPTY) && !player.getCooldowns().isOnCooldown(itemStack);
    }

    @Override
    public void handleUseItemOn(ServerboundUseItemOnPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded()) {
            return;
        }
        this.ackBlockChangesUpTo(packet.getSequence());
        ServerLevel level = this.player.level();
        InteractionHand hand = packet.getHand();
        ItemStack itemStack = this.player.getItemInHand(hand);
        if (!itemStack.isItemEnabled(level.enabledFeatures())) {
            return;
        }
        BlockHitResult blockHit = packet.getHitResult();
        Vec3 location = blockHit.getLocation();
        BlockPos pos = blockHit.getBlockPos();
        if (!this.player.isWithinBlockInteractionRange(pos, 1.0)) {
            return;
        }
        Vec3 distance = location.subtract(Vec3.atCenterOf(pos));
        double limit = 1.0000001;
        if (!(Math.abs(distance.x()) < 1.0000001 && Math.abs(distance.y()) < 1.0000001 && Math.abs(distance.z()) < 1.0000001)) {
            LOGGER.warn("Rejecting UseItemOnPacket from {}: Location {} too far away from hit block {}.", new Object[]{this.player.getGameProfile().name(), location, pos});
            return;
        }
        Direction direction = blockHit.getDirection();
        this.player.resetLastActionTime();
        int maxY = level.getMaxY();
        int minY = level.getMinY();
        if (pos.getY() > maxY) {
            this.player.sendBuildLimitMessage(true, maxY);
            return;
        }
        if (pos.getY() < minY) {
            this.player.sendBuildLimitMessage(false, minY);
            return;
        }
        if (this.server.isUnderSpawnProtection(level, pos, this.player)) {
            this.player.sendSpawnProtectionMessage(pos);
        } else if (this.awaitingPositionFromClient == null && level.mayInteract(this.player, pos)) {
            InteractionResult.Success success;
            InteractionResult interactionResult = this.player.gameMode.useItemOn(this.player, level, itemStack, hand, blockHit);
            if (interactionResult.consumesAction()) {
                CriteriaTriggers.ANY_BLOCK_USE.trigger(this.player, blockHit.getBlockPos(), itemStack);
            }
            if (direction == Direction.UP && !interactionResult.consumesAction() && pos.getY() >= maxY && ServerGamePacketListenerImpl.wasBlockPlacementAttempt(this.player, itemStack)) {
                this.player.sendBuildLimitMessage(true, maxY);
            } else if (interactionResult instanceof InteractionResult.Success && (success = (InteractionResult.Success)interactionResult).swingSource() == InteractionResult.SwingSource.SERVER) {
                this.player.swing(hand, true);
            }
            if (!interactionResult.consumesAction() && ServerGamePacketListenerImpl.wasBlockPlacementAttempt(this.player, itemStack)) {
                if (direction == Direction.UP && pos.getY() >= maxY) {
                    this.player.sendBuildLimitMessage(true, maxY);
                } else if (direction == Direction.DOWN && pos.getY() <= minY) {
                    this.player.sendBuildLimitMessage(false, minY);
                }
            } else if (interactionResult instanceof InteractionResult.Success && (success = (InteractionResult.Success)interactionResult).swingSource() == InteractionResult.SwingSource.SERVER) {
                this.player.swing(hand, true);
            }
        } else {
            this.player.sendBuildLimitMessage(true, maxY);
        }
        this.send(new ClientboundBlockUpdatePacket(level, pos));
        this.send(new ClientboundBlockUpdatePacket(level, pos.relative(direction)));
    }

    @Override
    public void handleUseItem(ServerboundUseItemPacket packet) {
        InteractionResult.Success success;
        InteractionResult useResult;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded()) {
            return;
        }
        this.ackBlockChangesUpTo(packet.getSequence());
        ServerLevel level = this.player.level();
        InteractionHand hand = packet.getHand();
        ItemStack itemStack = this.player.getItemInHand(hand);
        this.player.resetLastActionTime();
        if (itemStack.isEmpty() || !itemStack.isItemEnabled(level.enabledFeatures())) {
            return;
        }
        float targetYRot = Mth.wrapDegrees(packet.getYRot());
        float targetXRot = Mth.wrapDegrees(packet.getXRot());
        if (targetXRot != this.player.getXRot() || targetYRot != this.player.getYRot()) {
            this.player.absSnapRotationTo(targetYRot, targetXRot);
        }
        if ((useResult = this.player.gameMode.useItem(this.player, level, itemStack, hand)) instanceof InteractionResult.Success && (success = (InteractionResult.Success)useResult).swingSource() == InteractionResult.SwingSource.SERVER) {
            this.player.swing(hand, true);
        }
    }

    @Override
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.isSpectator()) {
            for (ServerLevel level : this.server.getAllLevels()) {
                Entity entity = packet.getEntity(level);
                if (entity == null) continue;
                this.player.teleportTo(level, entity.getX(), entity.getY(), entity.getZ(), Set.of(), entity.getYRot(), entity.getXRot(), true);
                return;
            }
        }
    }

    @Override
    public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        Entity vehicle = this.player.getControlledVehicle();
        if (vehicle instanceof AbstractBoat) {
            AbstractBoat boat = (AbstractBoat)vehicle;
            boat.setPaddleState(packet.getLeft(), packet.getRight());
        }
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        LOGGER.info("{} lost connection: {}", (Object)this.player.getPlainTextName(), (Object)details.reason().getString());
        this.removePlayerFromWorld();
        super.onDisconnect(details);
    }

    private void removePlayerFromWorld() {
        this.chatMessageChain.close();
        this.server.invalidateStatus();
        this.server.getPlayerList().broadcastSystemMessage(Component.translatable("multiplayer.player.left", this.player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
        this.player.disconnect();
        this.server.getPlayerList().remove(this.player);
        this.player.getTextFilter().leave();
    }

    public void ackBlockChangesUpTo(int packetSequenceNr) {
        if (packetSequenceNr < 0) {
            throw new IllegalArgumentException("Expected packet sequence nr >= 0");
        }
        this.ackBlockChangesUpTo = Math.max(packetSequenceNr, this.ackBlockChangesUpTo);
    }

    @Override
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (packet.getSlot() < 0 || packet.getSlot() >= Inventory.getSelectionSize()) {
            LOGGER.warn("{} tried to set an invalid carried item", (Object)this.player.getPlainTextName());
            return;
        }
        if (this.player.getInventory().getSelectedSlot() != packet.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            this.player.stopUsingItem();
        }
        this.player.getInventory().setSelectedSlot(packet.getSlot());
        this.player.resetLastActionTime();
    }

    @Override
    public void handleChat(ServerboundChatPacket packet) {
        Optional<LastSeenMessages> unpackedLastSeen = this.unpackAndApplyLastSeen(packet.lastSeenMessages());
        if (unpackedLastSeen.isEmpty()) {
            return;
        }
        this.tryHandleChat(packet.message(), false, () -> {
            PlayerChatMessage signedMessage;
            try {
                signedMessage = this.getSignedMessage(packet, (LastSeenMessages)unpackedLastSeen.get());
            }
            catch (SignedMessageChain.DecodeException e) {
                this.handleMessageDecodeFailure(e);
                return;
            }
            CompletableFuture<FilteredText> filteredFuture = this.filterTextPacket(signedMessage.signedContent());
            Component decorated = this.server.getChatDecorator().decorate(this.player, signedMessage.decoratedContent());
            this.chatMessageChain.append(filteredFuture, filtered -> {
                PlayerChatMessage filteredMessage = signedMessage.withUnsignedContent(decorated).filter(filtered.mask());
                this.broadcastChatMessage(filteredMessage);
            });
        });
    }

    @Override
    public void handleChatCommand(ServerboundChatCommandPacket packet) {
        this.tryHandleChat(packet.command(), true, () -> {
            this.performUnsignedChatCommand(packet.command());
            this.detectCommandRateSpam();
        });
    }

    private void performUnsignedChatCommand(String command) {
        ParseResults<CommandSourceStack> parsed = this.parseCommand(command);
        if (this.server.enforceSecureProfile() && SignableCommand.hasSignableArguments(parsed)) {
            LOGGER.error("Received unsigned command packet from {}, but the command requires signable arguments: {}", (Object)this.player.getGameProfile().name(), (Object)command);
            this.player.sendSystemMessage(INVALID_COMMAND_SIGNATURE);
            return;
        }
        this.server.getCommands().performCommand(parsed, command);
    }

    @Override
    public void handleSignedChatCommand(ServerboundChatCommandSignedPacket packet) {
        Optional<LastSeenMessages> unpackedLastSeen = this.unpackAndApplyLastSeen(packet.lastSeenMessages());
        if (unpackedLastSeen.isEmpty()) {
            return;
        }
        this.tryHandleChat(packet.command(), true, () -> {
            this.performSignedChatCommand(packet, (LastSeenMessages)unpackedLastSeen.get());
            this.detectCommandRateSpam();
        });
    }

    private void performSignedChatCommand(ServerboundChatCommandSignedPacket packet, LastSeenMessages lastSeenMessages) {
        Map<String, PlayerChatMessage> signedArguments;
        ParseResults<CommandSourceStack> command = this.parseCommand(packet.command());
        try {
            signedArguments = this.collectSignedArguments(packet, SignableCommand.of(command), lastSeenMessages);
        }
        catch (SignedMessageChain.DecodeException e) {
            this.handleMessageDecodeFailure(e);
            return;
        }
        CommandSigningContext.SignedArguments signingContext = new CommandSigningContext.SignedArguments(signedArguments);
        command = Commands.mapSource(command, source -> source.withSigningContext(signingContext, this.chatMessageChain));
        this.server.getCommands().performCommand(command, packet.command());
    }

    private void handleMessageDecodeFailure(SignedMessageChain.DecodeException e) {
        LOGGER.warn("Failed to update secure chat state for {}: '{}'", (Object)this.player.getGameProfile().name(), (Object)e.getComponent().getString());
        this.player.sendSystemMessage(e.getComponent().copy().withStyle(ChatFormatting.RED));
    }

    private <S> Map<String, PlayerChatMessage> collectSignedArguments(ServerboundChatCommandSignedPacket packet, SignableCommand<S> command, LastSeenMessages lastSeenMessages) throws SignedMessageChain.DecodeException {
        List<ArgumentSignatures.Entry> argumentSignatures = packet.argumentSignatures().entries();
        List<SignableCommand.Argument<S>> parsedArguments = command.arguments();
        if (argumentSignatures.isEmpty()) {
            return this.collectUnsignedArguments(parsedArguments);
        }
        Object2ObjectOpenHashMap signedArguments = new Object2ObjectOpenHashMap();
        for (ArgumentSignatures.Entry entry : argumentSignatures) {
            SignableCommand.Argument<S> expectedArgument = command.getArgument(entry.name());
            if (expectedArgument == null) {
                this.signedMessageDecoder.setChainBroken();
                throw ServerGamePacketListenerImpl.createSignedArgumentMismatchException(packet.command(), argumentSignatures, parsedArguments);
            }
            SignedMessageBody body = new SignedMessageBody(expectedArgument.value(), packet.timeStamp(), packet.salt(), lastSeenMessages);
            signedArguments.put(expectedArgument.name(), this.signedMessageDecoder.unpack(entry.signature(), body));
        }
        for (SignableCommand.Argument argument : parsedArguments) {
            if (signedArguments.containsKey(argument.name())) continue;
            throw ServerGamePacketListenerImpl.createSignedArgumentMismatchException(packet.command(), argumentSignatures, parsedArguments);
        }
        return signedArguments;
    }

    private <S> Map<String, PlayerChatMessage> collectUnsignedArguments(List<SignableCommand.Argument<S>> parsedArguments) throws SignedMessageChain.DecodeException {
        HashMap<String, PlayerChatMessage> arguments = new HashMap<String, PlayerChatMessage>();
        for (SignableCommand.Argument<S> parsedArgument : parsedArguments) {
            SignedMessageBody body = SignedMessageBody.unsigned(parsedArgument.value());
            arguments.put(parsedArgument.name(), this.signedMessageDecoder.unpack(null, body));
        }
        return arguments;
    }

    private static <S> SignedMessageChain.DecodeException createSignedArgumentMismatchException(String command, List<ArgumentSignatures.Entry> clientArguments, List<SignableCommand.Argument<S>> expectedArguments) {
        String clientNames = clientArguments.stream().map(ArgumentSignatures.Entry::name).collect(Collectors.joining(", "));
        String expectedNames = expectedArguments.stream().map(SignableCommand.Argument::name).collect(Collectors.joining(", "));
        LOGGER.error("Signed command mismatch between server and client ('{}'): got [{}] from client, but expected [{}]", new Object[]{command, clientNames, expectedNames});
        return new SignedMessageChain.DecodeException(INVALID_COMMAND_SIGNATURE);
    }

    private ParseResults<CommandSourceStack> parseCommand(String command) {
        CommandDispatcher<CommandSourceStack> commands = this.server.getCommands().getDispatcher();
        return commands.parse(command, (Object)this.player.createCommandSourceStack());
    }

    private void tryHandleChat(String message, boolean isCommand, Runnable chatHandler) {
        if (ServerGamePacketListenerImpl.isChatMessageIllegal(message)) {
            this.disconnect(Component.translatable("multiplayer.disconnect.illegal_characters"));
            return;
        }
        if (!isCommand && this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            this.send(new ClientboundSystemChatPacket(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED), false));
            return;
        }
        this.player.resetLastActionTime();
        this.server.execute(chatHandler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Optional<LastSeenMessages> unpackAndApplyLastSeen(LastSeenMessages.Update update) {
        LastSeenMessagesValidator lastSeenMessagesValidator = this.lastSeenMessages;
        synchronized (lastSeenMessagesValidator) {
            try {
                LastSeenMessages result = this.lastSeenMessages.applyUpdate(update);
                return Optional.of(result);
            }
            catch (LastSeenMessagesValidator.ValidationException e) {
                LOGGER.error("Failed to validate message acknowledgements from {}: {}", (Object)this.player.getPlainTextName(), (Object)e.getMessage());
                this.disconnect(CHAT_VALIDATION_FAILED);
                return Optional.empty();
            }
        }
    }

    private static boolean isChatMessageIllegal(String message) {
        for (int i = 0; i < message.length(); ++i) {
            if (StringUtil.isAllowedChatCharacter(message.charAt(i))) continue;
            return true;
        }
        return false;
    }

    private PlayerChatMessage getSignedMessage(ServerboundChatPacket packet, LastSeenMessages lastSeenMessages) throws SignedMessageChain.DecodeException {
        SignedMessageBody body = new SignedMessageBody(packet.message(), packet.timeStamp(), packet.salt(), lastSeenMessages);
        return this.signedMessageDecoder.unpack(packet.signature(), body);
    }

    private void broadcastChatMessage(PlayerChatMessage message) {
        this.server.getPlayerList().broadcastChatMessage(message, this.player, ChatType.bind(ChatType.CHAT, this.player));
        this.detectChatRateSpam();
    }

    private void detectRateSpam(TickThrottler throttler) {
        throttler.increment();
        if (!(throttler.isUnderThreshold() || this.server.getPlayerList().isOp(this.player.nameAndId()) || this.server.isSingleplayerOwner(this.player.nameAndId()))) {
            this.disconnect(Component.translatable("disconnect.spam"));
        }
    }

    private void detectCommandRateSpam() {
        this.detectRateSpam(this.commandSpamThrottler);
    }

    private void detectChatRateSpam() {
        this.detectRateSpam(this.chatSpamThrottler);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void handleChatAck(ServerboundChatAckPacket packet) {
        LastSeenMessagesValidator lastSeenMessagesValidator = this.lastSeenMessages;
        synchronized (lastSeenMessagesValidator) {
            try {
                this.lastSeenMessages.applyOffset(packet.offset());
            }
            catch (LastSeenMessagesValidator.ValidationException e) {
                LOGGER.error("Failed to validate message acknowledgement offset from {}: {}", (Object)this.player.getPlainTextName(), (Object)e.getMessage());
                this.disconnect(CHAT_VALIDATION_FAILED);
            }
        }
    }

    @Override
    public void handleAnimate(ServerboundSwingPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        this.player.swing(packet.getHand());
    }

    @Override
    public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded()) {
            return;
        }
        this.player.resetLastActionTime();
        switch (packet.getAction()) {
            case START_SPRINTING: {
                this.player.setSprinting(true);
                break;
            }
            case STOP_SPRINTING: {
                this.player.setSprinting(false);
                break;
            }
            case STOP_SLEEPING: {
                if (!this.player.isSleeping()) break;
                this.player.stopSleepInBed(false, true);
                this.awaitingPositionFromClient = this.player.position();
                break;
            }
            case START_RIDING_JUMP: {
                Entity entity = this.player.getControlledVehicle();
                if (!(entity instanceof PlayerRideableJumping)) break;
                PlayerRideableJumping vehicle = (PlayerRideableJumping)((Object)entity);
                int data = packet.getData();
                if (!vehicle.canJump() || data <= 0) break;
                vehicle.handleStartJump(data);
                break;
            }
            case STOP_RIDING_JUMP: {
                Entity entity = this.player.getControlledVehicle();
                if (!(entity instanceof PlayerRideableJumping)) break;
                PlayerRideableJumping vehicle = (PlayerRideableJumping)((Object)entity);
                vehicle.handleStopJump();
                break;
            }
            case OPEN_INVENTORY: {
                Entity entity = this.player.getVehicle();
                if (!(entity instanceof HasCustomInventoryScreen)) break;
                HasCustomInventoryScreen vehicleWithInventory = (HasCustomInventoryScreen)((Object)entity);
                vehicleWithInventory.openCustomInventoryScreen(this.player);
                break;
            }
            case START_FALL_FLYING: {
                if (this.player.tryToStartFallFlying()) break;
                this.player.stopFallFlying();
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid client command!");
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendPlayerChatMessage(PlayerChatMessage message, ChatType.Bound chatType) {
        int trackedCount;
        this.send(new ClientboundPlayerChatPacket(this.nextChatIndex++, message.link().sender(), message.link().index(), message.signature(), message.signedBody().pack(this.messageSignatureCache), message.unsignedContent(), message.filterMask(), chatType));
        MessageSignature signature = message.signature();
        if (signature == null) {
            return;
        }
        this.messageSignatureCache.push(message.signedBody(), message.signature());
        LastSeenMessagesValidator lastSeenMessagesValidator = this.lastSeenMessages;
        synchronized (lastSeenMessagesValidator) {
            this.lastSeenMessages.addPending(signature);
            trackedCount = this.lastSeenMessages.trackedMessagesCount();
        }
        if (trackedCount > 4096) {
            this.disconnect(Component.translatable("multiplayer.disconnect.too_many_pending_chats"));
        }
    }

    public void sendDisguisedChatMessage(Component content, ChatType.Bound chatType) {
        this.send(new ClientboundDisguisedChatPacket(content, chatType));
    }

    public SocketAddress getRemoteAddress() {
        return this.connection.getRemoteAddress();
    }

    public void switchToConfig() {
        this.waitingForSwitchToConfig = true;
        this.removePlayerFromWorld();
        this.send(ClientboundStartConfigurationPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
    }

    @Override
    public void handlePingRequest(ServerboundPingRequestPacket packet) {
        this.connection.send(new ClientboundPongResponsePacket(packet.getTime()));
    }

    @Override
    public void handleAttack(ServerboundAttackPacket packet) {
        AbstractArrow abstractArrow;
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded() || this.player.isSpectator()) {
            return;
        }
        ServerLevel level = this.player.level();
        Entity target = level.getEntityOrPart(packet.entityId());
        this.player.resetLastActionTime();
        if (target == null || !level.getWorldBorder().isWithinBounds(target.blockPosition())) {
            return;
        }
        AABB targetBounds = target.getBoundingBox();
        ItemStack mainHandItem = this.player.getMainHandItem();
        if (!this.player.isWithinAttackRange(mainHandItem, targetBounds, 3.0)) {
            return;
        }
        if (mainHandItem.has(DataComponents.PIERCING_WEAPON)) {
            return;
        }
        if (target instanceof ItemEntity || target instanceof ExperienceOrb || target == this.player || target instanceof AbstractArrow && !(abstractArrow = (AbstractArrow)target).isAttackable()) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_entity_attacked"));
            LOGGER.warn("Player {} tried to attack an invalid entity", (Object)this.player.getPlainTextName());
            return;
        }
        if (!mainHandItem.isItemEnabled(level.enabledFeatures())) {
            return;
        }
        if (this.player.cannotAttackWithItem(mainHandItem, 5)) {
            return;
        }
        this.player.attack(target);
    }

    @Override
    public void handleInteract(ServerboundInteractPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded()) {
            return;
        }
        ServerLevel level = this.player.level();
        Entity target = level.getEntityOrPart(packet.entityId());
        this.player.resetLastActionTime();
        this.player.setShiftKeyDown(packet.usingSecondaryAction());
        if (target == null || !level.getWorldBorder().isWithinBounds(target.blockPosition())) {
            return;
        }
        AABB targetBounds = target.getBoundingBox();
        if (!this.player.isWithinEntityInteractionRange(targetBounds, 3.0)) {
            return;
        }
        InteractionHand hand = packet.hand();
        Vec3 location = packet.location();
        ItemStack tool = this.player.getItemInHand(hand);
        if (!tool.isItemEnabled(level.enabledFeatures())) {
            return;
        }
        ItemStack usedItemStack = tool.copy();
        InteractionResult result = this.player.interactOn(target, hand, location);
        if (result instanceof InteractionResult.Success) {
            InteractionResult.Success success = (InteractionResult.Success)result;
            ItemStack awardedForStack = success.wasItemInteraction() ? usedItemStack : ItemStack.EMPTY;
            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(this.player, awardedForStack, target);
            if (success.swingSource() == InteractionResult.SwingSource.SERVER) {
                this.player.swing(hand, true);
            }
        }
    }

    @Override
    public void handleSpectatorAction(ServerboundSpectatorActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.hasClientLoaded() || !this.player.isSpectator()) {
            return;
        }
        this.player.resetLastActionTime();
        if (!packet.spectateEntityId().isPresent()) {
            return;
        }
        ServerLevel level = this.player.level();
        Entity target = level.getEntityOrPart(packet.spectateEntityId().getAsInt());
        if (target == null || !level.getWorldBorder().isWithinBounds(target.blockPosition())) {
            return;
        }
        if (!this.player.isWithinEntityInteractionRange(target.getBoundingBox(), 3.0)) {
            return;
        }
        if (!target.isPickable()) {
            return;
        }
        this.player.setCamera(target);
    }

    @Override
    public void handleClientCommand(ServerboundClientCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        ServerboundClientCommandPacket.Action action = packet.getAction();
        switch (action) {
            case PERFORM_RESPAWN: {
                if (this.player.wonGame) {
                    this.player.wonGame = false;
                    this.player = this.server.getPlayerList().respawn(this.player, true, Entity.RemovalReason.CHANGED_DIMENSION);
                    this.resetPosition();
                    this.restartClientLoadTimerAfterRespawn();
                    CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
                    break;
                }
                if (this.player.getHealth() > 0.0f) {
                    return;
                }
                this.player = this.server.getPlayerList().respawn(this.player, false, Entity.RemovalReason.KILLED);
                this.resetPosition();
                this.restartClientLoadTimerAfterRespawn();
                if (!this.server.isHardcore()) break;
                this.player.setGameMode(GameType.SPECTATOR);
                break;
            }
            case REQUEST_STATS: {
                this.player.getStats().sendStats(this.player);
                break;
            }
            case REQUEST_GAMERULE_VALUES: {
                this.sendGameRuleValues();
            }
        }
    }

    private void sendGameRuleValues() {
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            LOGGER.warn("Player {} tried to request game rule values without required permissions", (Object)this.player.getGameProfile().name());
            return;
        }
        GameRules gameRules = this.player.level().getGameRules();
        HashMap values = new HashMap();
        gameRules.availableRules().forEach(rule -> ServerGamePacketListenerImpl.addGameRuleValue(gameRules, values, rule));
        this.send(new ClientboundGameRuleValuesPacket(values));
    }

    private static <T> void addGameRuleValue(GameRules gameRules, Map<ResourceKey<GameRule<?>>, String> values, GameRule<T> rule) {
        BuiltInRegistries.GAME_RULE.getResourceKey(rule).ifPresent(key -> values.put((ResourceKey<GameRule<?>>)key, rule.serialize(gameRules.get(rule))));
    }

    @Override
    public void handleContainerClose(ServerboundContainerClosePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.doCloseContainer();
    }

    @Override
    public void handleContainerClick(ServerboundContainerClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId != packet.containerId()) {
            return;
        }
        if (this.player.isSpectator() || this.player.isDeadOrDying()) {
            this.player.containerMenu.sendAllDataToRemote();
            return;
        }
        if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", (Object)this.player, (Object)this.player.containerMenu);
            return;
        }
        short slotIndex = packet.slotNum();
        if (!this.player.containerMenu.isValidSlotIndex(slotIndex)) {
            LOGGER.debug("Player {} clicked invalid slot index: {}, available slots: {}", new Object[]{this.player.getPlainTextName(), (int)slotIndex, this.player.containerMenu.slots.size()});
            return;
        }
        boolean fullResyncNeeded = packet.stateId() != this.player.containerMenu.getStateId();
        this.player.containerMenu.suppressRemoteUpdates();
        this.player.containerMenu.clicked(slotIndex, packet.buttonNum(), packet.containerInput(), this.player);
        for (Int2ObjectMap.Entry e : Int2ObjectMaps.fastIterable(packet.changedSlots())) {
            this.player.containerMenu.setRemoteSlotUnsafe(e.getIntKey(), (HashedStack)e.getValue());
        }
        this.player.containerMenu.setRemoteCarried(packet.carriedItem());
        this.player.containerMenu.resumeRemoteUpdates();
        if (fullResyncNeeded) {
            this.player.containerMenu.broadcastFullState();
        } else {
            this.player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        if (this.player.isSpectator() || this.player.containerMenu.containerId != packet.containerId()) {
            return;
        }
        if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", (Object)this.player, (Object)this.player.containerMenu);
            return;
        }
        RecipeManager.ServerDisplayInfo displayInfo = this.server.getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (displayInfo == null) {
            return;
        }
        RecipeHolder<?> recipe = displayInfo.parent();
        if (!this.player.getRecipeBook().contains(recipe.id())) {
            return;
        }
        AbstractContainerMenu abstractContainerMenu = this.player.containerMenu;
        if (abstractContainerMenu instanceof RecipeBookMenu) {
            RecipeBookMenu recipeBookMenu = (RecipeBookMenu)abstractContainerMenu;
            if (recipe.value().placementInfo().isImpossibleToPlace()) {
                LOGGER.debug("Player {} tried to place impossible recipe {}", (Object)this.player, (Object)recipe.id().identifier());
                return;
            }
            RecipeBookMenu.PostPlaceAction postPlaceAction = recipeBookMenu.handlePlacement(packet.useMaxItems(), this.player.isCreative(), recipe, this.player.level(), this.player.getInventory());
            if (postPlaceAction == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
                this.send(new ClientboundPlaceGhostRecipePacket(this.player.containerMenu.containerId, displayInfo.display().display()));
            }
        }
    }

    @Override
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId != packet.containerId() || this.player.isSpectator()) {
            return;
        }
        if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", (Object)this.player, (Object)this.player.containerMenu);
            return;
        }
        boolean clickAccepted = this.player.containerMenu.clickMenuButton(this.player, packet.buttonId());
        if (clickAccepted) {
            this.player.containerMenu.broadcastChanges();
        }
    }

    @Override
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.hasInfiniteMaterials()) {
            boolean validData;
            boolean drop = packet.slotNum() < 0;
            ItemStack itemStack = packet.itemStack();
            if (!itemStack.isItemEnabled(this.player.level().enabledFeatures())) {
                return;
            }
            boolean validSlot = packet.slotNum() >= 1 && packet.slotNum() <= 45;
            boolean bl = validData = itemStack.isEmpty() || itemStack.getCount() <= itemStack.getMaxStackSize();
            if (validSlot && validData) {
                this.player.inventoryMenu.getSlot(packet.slotNum()).setByPlayer(itemStack);
                this.player.inventoryMenu.setRemoteSlot(packet.slotNum(), itemStack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (drop && validData) {
                if (this.dropSpamThrottler.isUnderThreshold()) {
                    this.dropSpamThrottler.increment();
                    this.player.drop(itemStack, true);
                } else {
                    LOGGER.warn("Player {} was dropping items too fast in creative mode, ignoring.", (Object)this.player.getPlainTextName());
                }
            }
        }
    }

    @Override
    public void handleSignUpdate(ServerboundSignUpdatePacket packet) {
        List<String> lines = Stream.of(packet.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());
        this.filterTextPacket(lines).thenAcceptAsync(filteredLines -> this.updateSignText(packet, (List<FilteredText>)filteredLines), (Executor)this.server);
    }

    private void updateSignText(ServerboundSignUpdatePacket packet, List<FilteredText> lines) {
        this.player.resetLastActionTime();
        ServerLevel level = this.player.level();
        BlockPos pos = packet.getPos();
        if (level.hasChunkAt(pos)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof SignBlockEntity)) {
                return;
            }
            SignBlockEntity sign = (SignBlockEntity)blockEntity;
            sign.updateSignText(this.player, packet.isFrontText(), lines);
        }
    }

    @Override
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.getAbilities().flying = packet.isFlying() && this.player.getAbilities().mayfly;
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        boolean wasHatShown = this.player.isModelPartShown(PlayerModelPart.HAT);
        this.player.updateOptions(packet.information());
        if (this.player.isModelPartShown(PlayerModelPart.HAT) != wasHatShown) {
            this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT, this.player));
        }
    }

    @Override
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !this.isSingleplayerOwner()) {
            LOGGER.warn("Player {} tried to change difficulty to {} without required permissions", (Object)this.player.getGameProfile().name(), (Object)packet.difficulty().getDisplayName());
            return;
        }
        this.server.setDifficulty(packet.difficulty(), false);
    }

    @Override
    public void handleChangeGameMode(ServerboundChangeGameModePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!GameModeCommand.PERMISSION_CHECK.check(this.player.permissions())) {
            LOGGER.warn("Player {} tried to change game mode to {} without required permissions", (Object)this.player.getGameProfile().name(), (Object)packet.mode().getShortDisplayName().getString());
            return;
        }
        GameModeCommand.setGameMode(this.player, packet.mode());
    }

    @Override
    public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !this.isSingleplayerOwner()) {
            return;
        }
        this.server.setDifficultyLocked(packet.isLocked());
    }

    @Override
    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        RemoteChatSession.Data newChatSession = packet.chatSession();
        ProfilePublicKey.Data oldProfileKey = this.chatSession != null ? this.chatSession.profilePublicKey().data() : null;
        ProfilePublicKey.Data newProfileKey = newChatSession.profilePublicKey();
        if (Objects.equals(oldProfileKey, newProfileKey)) {
            return;
        }
        if (oldProfileKey != null && newProfileKey.expiresAt().isBefore(oldProfileKey.expiresAt())) {
            this.disconnect(ProfilePublicKey.EXPIRED_PROFILE_PUBLIC_KEY);
            return;
        }
        try {
            SignatureValidator profileKeySignatureValidator = this.server.services().profileKeySignatureValidator();
            if (profileKeySignatureValidator == null) {
                LOGGER.warn("Ignoring chat session from {} due to missing Services public key", (Object)this.player.getGameProfile().name());
                return;
            }
            this.resetPlayerChatState(newChatSession.validate(this.player.getGameProfile(), profileKeySignatureValidator));
        }
        catch (ProfilePublicKey.ValidationException e) {
            LOGGER.error("Failed to validate profile key: {}", (Object)e.getMessage());
            this.disconnect(e.getComponent());
        }
    }

    @Override
    public void handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket packet) {
        if (!this.waitingForSwitchToConfig) {
            throw new IllegalStateException("Client acknowledged config, but none was requested");
        }
        this.connection.setupInboundProtocol(ConfigurationProtocols.SERVERBOUND, new ServerConfigurationPacketListenerImpl(this.server, this.connection, this.createCookie(this.player.clientInformation())));
    }

    @Override
    public void handleChunkBatchReceived(ServerboundChunkBatchReceivedPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.chunkSender.onChunkBatchReceivedByClient(packet.desiredChunksPerTick());
    }

    @Override
    public void handleDebugSubscriptionRequest(ServerboundDebugSubscriptionRequestPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.requestDebugSubscriptions(packet.subscriptions());
    }

    private void resetPlayerChatState(RemoteChatSession chatSession) {
        this.chatSession = chatSession;
        this.signedMessageDecoder = chatSession.createMessageDecoder(this.player.getUUID());
        this.chatMessageChain.append(() -> {
            this.player.setChatSession(chatSession);
            this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT), List.of(this.player)));
        });
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {
    }

    @Override
    public void handleClientTickEnd(ServerboundClientTickEndPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.receivedMovementThisTick) {
            this.player.setKnownMovement(Vec3.ZERO);
        }
        this.receivedMovementThisTick = false;
    }

    private void handlePlayerKnownMovement(Vec3 movement) {
        if (movement.lengthSqr() > (double)1.0E-5f) {
            this.player.resetLastActionTime();
        }
        this.player.setKnownMovement(movement);
        this.receivedMovementThisTick = true;
    }

    @Override
    public boolean hasInfiniteMaterials() {
        return this.player.hasInfiniteMaterials();
    }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    public boolean hasClientLoaded() {
        return !this.waitingForRespawn && this.clientLoadedTimeoutTimer <= 0;
    }

    public void tickClientLoadTimeout() {
        if (this.clientLoadedTimeoutTimer > 0) {
            --this.clientLoadedTimeoutTimer;
        }
    }

    private void markClientLoaded() {
        this.clientLoadedTimeoutTimer = 0;
    }

    public void markClientUnloadedAfterDeath() {
        this.waitingForRespawn = true;
    }

    private void restartClientLoadTimerAfterRespawn() {
        this.waitingForRespawn = false;
        this.clientLoadedTimeoutTimer = 60;
    }
}

