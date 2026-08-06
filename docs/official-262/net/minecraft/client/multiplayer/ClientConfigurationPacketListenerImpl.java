/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.multiplayer.CodeOfConductScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientConfigurationPacketListenerImpl
extends ClientCommonPacketListenerImpl
implements ClientConfigurationPacketListener,
TickablePacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component DISCONNECTED_MESSAGE = Component.translatable("multiplayer.disconnect.code_of_conduct");
    private final LevelLoadTracker levelLoadTracker;
    private final GameProfile localGameProfile;
    private FeatureFlagSet enabledFeatures;
    private final RegistryAccess.Frozen receivedRegistries;
    private final RegistryDataCollector registryDataCollector = new RegistryDataCollector();
    private @Nullable KnownPacksManager knownPacks;
    protected @Nullable ChatComponent.State chatState;
    private boolean seenCodeOfConduct;

    public ClientConfigurationPacketListenerImpl(Minecraft minecraft, Connection connection, CommonListenerCookie cookie) {
        super(minecraft, connection, cookie);
        this.levelLoadTracker = cookie.levelLoadTracker();
        this.localGameProfile = cookie.localGameProfile();
        this.receivedRegistries = cookie.receivedRegistries();
        this.enabledFeatures = cookie.enabledFeatures();
        this.chatState = cookie.chatState();
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    @Override
    protected void handleCustomPayload(CustomPacketPayload payload) {
        this.handleUnknownCustomPayload(payload);
    }

    private void handleUnknownCustomPayload(CustomPacketPayload payload) {
        LOGGER.warn("Unknown custom packet payload: {}", (Object)payload.type().id());
    }

    @Override
    public void handleRegistryData(ClientboundRegistryDataPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.registryDataCollector.appendContents(packet.registry(), packet.entries());
    }

    @Override
    public void handleUpdateTags(ClientboundUpdateTagsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        this.registryDataCollector.appendTags(packet.getTags());
    }

    @Override
    public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket packet) {
        this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(packet.features());
    }

    @Override
    public void handleSelectKnownPacks(ClientboundSelectKnownPacks packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.knownPacks == null) {
            this.knownPacks = new KnownPacksManager();
        }
        List<KnownPack> selected = this.knownPacks.trySelectingPacks(packet.knownPacks());
        this.send(new ServerboundSelectKnownPacks(selected));
    }

    @Override
    public void handleResetChat(ClientboundResetChatPacket packet) {
        this.chatState = null;
    }

    private <T> T runWithResources(Function<ResourceProvider, T> operation) {
        if (this.knownPacks == null) {
            return operation.apply(ResourceProvider.EMPTY);
        }
        try (CloseableResourceManager manager = this.knownPacks.createResourceManager();){
            T t = operation.apply(manager);
            return t;
        }
    }

    @Override
    public void handleCodeOfConduct(ClientboundCodeOfConductPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        if (this.seenCodeOfConduct) {
            throw new IllegalStateException("Server sent duplicate Code of Conduct");
        }
        this.seenCodeOfConduct = true;
        String codeOfConduct = packet.codeOfConduct();
        if (this.serverData != null && this.serverData.hasAcceptedCodeOfConduct(codeOfConduct)) {
            this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
        } else {
            Screen lastScreen = this.minecraft.gui.screen();
            this.minecraft.gui.setScreen(new CodeOfConductScreen(this.serverData, lastScreen, codeOfConduct, accepted -> {
                if (accepted) {
                    this.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
                    this.minecraft.gui.setScreen(lastScreen);
                } else {
                    this.createDialogAccess().disconnect(DISCONNECTED_MESSAGE);
                }
            }));
        }
    }

    @Override
    public void handleConfigurationFinished(ClientboundFinishConfigurationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.minecraft.packetProcessor());
        RegistryAccess.Frozen registries = this.runWithResources(knownPacksProvider -> this.registryDataCollector.collectGameRegistries((ResourceProvider)knownPacksProvider, this.receivedRegistries, this.connection.isMemoryConnection()));
        IntegratedServer localServer = this.minecraft.getSingleplayerServer();
        if (localServer != null) {
            registries = ClientConfigurationPacketListenerImpl.filterRegistries(localServer.registryAccess(), registries.listRegistryKeys());
        }
        this.connection.setupInboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries)), new ClientPacketListener(this.minecraft, this.connection, new CommonListenerCookie(this.levelLoadTracker, this.localGameProfile, this.telemetryManager, registries, this.enabledFeatures, this.serverBrand, this.serverData, this.postDisconnectScreen, this.serverCookies, this.chatState, this.customReportDetails, this.serverLinks(), this.seenPlayers, this.seenInsecureChatWarning)));
        this.connection.send(ServerboundFinishConfigurationPacket.INSTANCE);
        this.connection.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries), new GameProtocols.Context(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public boolean hasInfiniteMaterials() {
                return true;
            }
        }));
    }

    private static RegistryAccess.Frozen filterRegistries(RegistryAccess.Frozen original, Stream<ResourceKey<? extends Registry<?>>> keysToInclude) {
        List<Registry> filteredRegistries = keysToInclude.map(original::lookupOrThrow).toList();
        return new RegistryAccess.ImmutableRegistryAccess(filteredRegistries).freeze();
    }

    @Override
    public void tick() {
        this.sendDeferredPackets();
    }

    @Override
    public void onDisconnect(DisconnectionDetails reason) {
        super.onDisconnect(reason);
        this.minecraft.clearDownloadedResourcePacks();
    }

    @Override
    protected DialogConnectionAccess createDialogAccess() {
        return new ClientCommonPacketListenerImpl.CommonDialogAccess(this){
            {
                Objects.requireNonNull(this$0);
                super(this$0);
            }

            @Override
            public void runCommand(String command, @Nullable Screen activeScreen) {
                LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", (Object)command);
            }
        };
    }
}

