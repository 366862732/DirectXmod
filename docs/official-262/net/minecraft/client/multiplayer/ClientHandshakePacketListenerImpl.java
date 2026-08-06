/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.exceptions.AuthenticationException
 *  com.mojang.authlib.exceptions.AuthenticationUnavailableException
 *  com.mojang.authlib.exceptions.ForcedUsernameChangeException
 *  com.mojang.authlib.exceptions.InsufficientPrivilegesException
 *  com.mojang.authlib.exceptions.InvalidCredentialsException
 *  com.mojang.authlib.exceptions.UserBannedException
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.ForcedUsernameChangeException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Crypt;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientHandshakePacketListenerImpl
implements ClientLoginPacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final @Nullable ServerData serverData;
    private final @Nullable Screen parent;
    private final Consumer<Component> updateStatus;
    private final Connection connection;
    private final boolean newWorld;
    private final @Nullable Duration worldLoadDuration;
    private @Nullable String minigameName;
    private final LevelLoadTracker levelLoadTracker;
    private final Map<Identifier, byte[]> cookies;
    private final boolean wasTransferredTo;
    private final Map<UUID, PlayerInfo> seenPlayers;
    private final boolean seenInsecureChatWarning;
    private final AtomicReference<State> state = new AtomicReference<State>(State.CONNECTING);

    public ClientHandshakePacketListenerImpl(Connection connection, Minecraft minecraft, @Nullable ServerData serverData, @Nullable Screen parent, boolean newWorld, @Nullable Duration worldLoadDuration, Consumer<Component> updateStatus, LevelLoadTracker levelLoadTracker, @Nullable TransferState transferState) {
        this.connection = connection;
        this.minecraft = minecraft;
        this.serverData = serverData;
        this.parent = parent;
        this.updateStatus = updateStatus;
        this.newWorld = newWorld;
        this.worldLoadDuration = worldLoadDuration;
        this.levelLoadTracker = levelLoadTracker;
        this.cookies = transferState != null ? new HashMap<Identifier, byte[]>(transferState.cookies()) : new HashMap();
        this.seenPlayers = transferState != null ? transferState.seenPlayers() : Map.of();
        this.seenInsecureChatWarning = transferState != null ? transferState.seenInsecureChatWarning() : false;
        this.wasTransferredTo = transferState != null;
    }

    private void switchState(State toState) {
        State newState = this.state.updateAndGet(lastState -> {
            if (!toState.fromStates.contains(lastState)) {
                throw new IllegalStateException("Tried to switch to " + String.valueOf((Object)toState) + " from " + String.valueOf(lastState) + ", but expected one of " + String.valueOf(toState.fromStates));
            }
            return toState;
        });
        this.updateStatus.accept(newState.message);
    }

    @Override
    public void handleHello(ClientboundHelloPacket packet) {
        ServerboundKeyPacket setKeyPacket;
        Cipher encryptCipher;
        Cipher decryptCipher;
        String digest;
        this.switchState(State.AUTHORIZING);
        try {
            SecretKey secretKey = Crypt.generateSecretKey();
            PublicKey publicKey = packet.getPublicKey();
            digest = new BigInteger(Crypt.digestData(packet.getServerId(), publicKey, secretKey)).toString(16);
            decryptCipher = Crypt.getCipher(2, secretKey);
            encryptCipher = Crypt.getCipher(1, secretKey);
            byte[] challenge = packet.getChallenge();
            setKeyPacket = new ServerboundKeyPacket(secretKey, publicKey, challenge);
        }
        catch (Exception e) {
            throw new IllegalStateException("Protocol error", e);
        }
        if (packet.shouldAuthenticate()) {
            Util.ioPool().execute(() -> {
                Component error = this.authenticateServer(digest);
                if (error != null) {
                    if (this.serverData != null && this.serverData.isLan()) {
                        LOGGER.warn(error.getString());
                    } else {
                        this.connection.disconnect(error);
                        return;
                    }
                }
                this.setEncryption(setKeyPacket, decryptCipher, encryptCipher);
            });
        } else {
            this.setEncryption(setKeyPacket, decryptCipher, encryptCipher);
        }
    }

    private void setEncryption(ServerboundKeyPacket setKeyPacket, Cipher decryptCipher, Cipher encryptCipher) {
        this.switchState(State.ENCRYPTING);
        this.connection.send(setKeyPacket, PacketSendListener.thenRun(() -> this.connection.setEncryptionKey(decryptCipher, encryptCipher)));
    }

    private @Nullable Component authenticateServer(String digest) {
        try {
            this.minecraft.services().sessionService().joinServer(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getAccessToken(), digest);
        }
        catch (AuthenticationUnavailableException ignored) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
        }
        catch (InvalidCredentialsException ignored) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
        }
        catch (InsufficientPrivilegesException ignored) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
        }
        catch (ForcedUsernameChangeException | UserBannedException ignored) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
        }
        catch (AuthenticationException e) {
            return Component.translatable("disconnect.loginFailedInfo", e.getMessage());
        }
        return null;
    }

    @Override
    public void handleLoginFinished(ClientboundLoginFinishedPacket packet) {
        this.switchState(State.JOINING);
        GameProfile localGameProfile = packet.gameProfile();
        this.connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, new ClientConfigurationPacketListenerImpl(this.minecraft, this.connection, new CommonListenerCookie(this.levelLoadTracker, localGameProfile, this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration, this.minigameName, packet.sessionId()), ClientRegistryLayer.createRegistryAccess().compositeAccess(), FeatureFlags.DEFAULT_FLAGS, null, this.serverData, this.parent, this.cookies, null, Map.of(), ServerLinks.EMPTY, this.seenPlayers, false)));
        this.connection.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
        this.connection.send(new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrandRetriever.getClientModName())));
        this.connection.send(new ServerboundClientInformationPacket(this.minecraft.options.buildPlayerInformation()));
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        Component title;
        Component component = title = this.wasTransferredTo ? CommonComponents.TRANSFER_CONNECT_FAILED : CommonComponents.CONNECT_FAILED;
        if (this.serverData != null && this.serverData.isRealm()) {
            this.minecraft.gui.setScreen(new DisconnectedScreen(this.parent, title, details.reason(), CommonComponents.GUI_BACK));
        } else {
            this.minecraft.gui.setScreen(new DisconnectedScreen(this.parent, title, details));
        }
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    @Override
    public void handleDisconnect(ClientboundLoginDisconnectPacket packet) {
        this.connection.disconnect(packet.reason());
    }

    @Override
    public void handleCompression(ClientboundLoginCompressionPacket packet) {
        if (!this.connection.isMemoryConnection()) {
            this.connection.setupCompression(packet.getCompressionThreshold(), false);
        }
    }

    @Override
    public void handleCustomQuery(ClientboundCustomQueryPacket packet) {
        this.updateStatus.accept(Component.translatable("connect.negotiating"));
        this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), null));
    }

    public void setMinigameName(@Nullable String minigameName) {
        this.minigameName = minigameName;
    }

    @Override
    public void handleRequestCookie(ClientboundCookieRequestPacket packet) {
        this.connection.send(new ServerboundCookieResponsePacket(packet.key(), this.cookies.get(packet.key())));
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReport report, CrashReportCategory connectionDetails) {
        connectionDetails.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<unknown>");
        connectionDetails.setDetail("Login phase", () -> this.state.get().toString());
        connectionDetails.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
    }

    @Environment(value=EnvType.CLIENT)
    private static enum State {
        CONNECTING(Component.translatable("connect.connecting"), Set.of()),
        AUTHORIZING(Component.translatable("connect.authorizing"), Set.of(CONNECTING)),
        ENCRYPTING(Component.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
        JOINING(Component.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

        private final Component message;
        private final Set<State> fromStates;

        private State(Component message, Set<State> fromStates) {
            this.message = message;
            this.fromStates = fromStates;
        }
    }
}

