/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.exceptions.AuthenticationUnavailableException
 *  com.mojang.authlib.yggdrasil.ProfileResult
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.lang3.Validate
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.network;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerLoginPacketListenerImpl
implements ServerLoginPacketListener,
TickablePacketListener {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private final byte[] challenge;
    private final MinecraftServer server;
    private final Connection connection;
    private final ServerActivityMonitor serverActivityMonitor;
    private volatile State state = State.HELLO;
    private int tick;
    private @Nullable String requestedUsername;
    private @Nullable GameProfile authenticatedProfile;
    private final String serverId = "";
    private final boolean transferred;

    public ServerLoginPacketListenerImpl(MinecraftServer minecraftserver, Connection connection, boolean transferred) {
        this.server = minecraftserver;
        this.connection = connection;
        this.serverActivityMonitor = this.server.getServerActivityMonitor();
        this.challenge = Ints.toByteArray((int)RandomSource.create().nextInt());
        this.transferred = transferred;
    }

    @Override
    public void tick() {
        if (this.state == State.VERIFYING) {
            this.verifyLoginAndFinishConnectionSetup(Objects.requireNonNull(this.authenticatedProfile));
        }
        if (this.state == State.WAITING_FOR_DUPE_DISCONNECT && !this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile))) {
            this.finishLoginAndWaitForClient(this.authenticatedProfile);
        }
        if (this.tick++ == 600) {
            this.disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
        }
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public void disconnect(Component component) {
        try {
            LOGGER.info("Disconnecting {}: {}", (Object)this.getUserName(), (Object)component.getString());
            this.connection.send(new ClientboundLoginDisconnectPacket(component));
            this.connection.disconnect(component);
        }
        catch (Exception e) {
            LOGGER.error("Error whilst disconnecting player", (Throwable)e);
        }
    }

    private boolean isPlayerAlreadyInWorld(GameProfile gameProfile) {
        return this.server.getPlayerList().getPlayer(gameProfile.id()) != null;
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        LOGGER.info("{} lost connection: {}", (Object)this.getUserName(), (Object)details.reason().getString());
    }

    public String getUserName() {
        String loggableAddress = this.connection.getLoggableAddress(this.server.logIPs());
        if (this.requestedUsername != null) {
            return this.requestedUsername + " (" + loggableAddress + ")";
        }
        return loggableAddress;
    }

    @Override
    public void handleHello(ServerboundHelloPacket packet) {
        Validate.validState((this.state == State.HELLO ? 1 : 0) != 0, (String)"Unexpected hello packet", (Object[])new Object[0]);
        Validate.validState((boolean)StringUtil.isValidPlayerName(packet.name()), (String)"Invalid characters in username", (Object[])new Object[0]);
        this.requestedUsername = packet.name();
        GameProfile singleplayerProfile = this.server.getSingleplayerProfile();
        if (singleplayerProfile != null && this.requestedUsername.equalsIgnoreCase(singleplayerProfile.name())) {
            this.startClientVerification(singleplayerProfile);
            return;
        }
        if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
            this.state = State.KEY;
            this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge, true));
        } else {
            this.startClientVerification(UUIDUtil.createOfflineProfile(this.requestedUsername));
        }
    }

    private void startClientVerification(GameProfile profile) {
        this.authenticatedProfile = profile;
        this.state = State.VERIFYING;
    }

    private void verifyLoginAndFinishConnectionSetup(GameProfile profile) {
        PlayerList playerList = this.server.getPlayerList();
        Component error = playerList.canPlayerLogin(this.connection.getRemoteAddress(), new NameAndId(profile));
        if (error != null) {
            this.disconnect(error);
        } else if (this.connection.getIntendedProfileId() != null && !profile.id().equals(this.connection.getIntendedProfileId())) {
            this.disconnect(CommonComponents.CONNECT_FAILED);
        } else {
            boolean waitForDisconnection;
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
                this.connection.send(new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()), PacketSendListener.thenRun(() -> this.connection.setupCompression(this.server.getCompressionThreshold(), true)));
            }
            if (waitForDisconnection = playerList.disconnectAllPlayersWithProfile(profile.id())) {
                this.state = State.WAITING_FOR_DUPE_DISCONNECT;
            } else {
                this.finishLoginAndWaitForClient(profile);
            }
        }
    }

    private void finishLoginAndWaitForClient(GameProfile gameProfile) {
        this.state = State.PROTOCOL_SWITCHING;
        this.connection.send(new ClientboundLoginFinishedPacket(gameProfile, this.server.getConnection().getSessionId()));
    }

    @Override
    public void handleKey(ServerboundKeyPacket packet) {
        String digest;
        Validate.validState((this.state == State.KEY ? 1 : 0) != 0, (String)"Unexpected key packet", (Object[])new Object[0]);
        try {
            PrivateKey serverPrivateKey = this.server.getKeyPair().getPrivate();
            if (!packet.isChallengeValid(this.challenge, serverPrivateKey)) {
                throw new IllegalStateException("Protocol error");
            }
            SecretKey secretKey = packet.getSecretKey(serverPrivateKey);
            digest = new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey)).toString(16);
            this.state = State.AUTHENTICATING;
            Cipher decryptCipher = Crypt.getCipher(2, secretKey);
            Cipher encryptCipher = Crypt.getCipher(1, secretKey);
            this.connection.setEncryptionKey(decryptCipher, encryptCipher);
        }
        catch (CryptException e) {
            throw new IllegalStateException("Protocol error", e);
        }
        Thread thread = new Thread(this, "User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()){
            final /* synthetic */ ServerLoginPacketListenerImpl this$0;
            {
                ServerLoginPacketListenerImpl serverLoginPacketListenerImpl = this$0;
                Objects.requireNonNull(serverLoginPacketListenerImpl);
                this.this$0 = serverLoginPacketListenerImpl;
                super(name);
            }

            @Override
            public void run() {
                String name = Objects.requireNonNull(this.this$0.requestedUsername, "Player name not initialized");
                try {
                    ProfileResult result = this.this$0.server.services().sessionService().hasJoinedServer(name, digest, this.getAddress());
                    if (result != null) {
                        GameProfile profile = result.profile();
                        LOGGER.info("UUID of player {} is {}", (Object)profile.name(), (Object)profile.id());
                        this.this$0.serverActivityMonitor.reportLoginActivity();
                        this.this$0.startClientVerification(profile);
                    } else if (this.this$0.server.isSingleplayer()) {
                        LOGGER.warn("Failed to verify username but will let them in anyway!");
                        this.this$0.startClientVerification(UUIDUtil.createOfflineProfile(name));
                    } else {
                        this.this$0.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                        LOGGER.error("Username '{}' tried to join with an invalid session", (Object)name);
                    }
                }
                catch (AuthenticationUnavailableException ignored) {
                    if (this.this$0.server.isSingleplayer()) {
                        LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        this.this$0.startClientVerification(UUIDUtil.createOfflineProfile(name));
                    }
                    this.this$0.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                    LOGGER.error("Couldn't verify username because servers are unavailable");
                }
            }

            private @Nullable InetAddress getAddress() {
                InetAddress inetAddress;
                SocketAddress remoteAddress = this.this$0.connection.getRemoteAddress();
                if (this.this$0.server.getPreventProxyConnections() && remoteAddress instanceof InetSocketAddress) {
                    InetSocketAddress inetSocketAddress = (InetSocketAddress)remoteAddress;
                    inetAddress = inetSocketAddress.getAddress();
                } else {
                    inetAddress = null;
                }
                return inetAddress;
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket packet) {
        this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
    }

    @Override
    public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket packet) {
        Validate.validState((this.state == State.PROTOCOL_SWITCHING ? 1 : 0) != 0, (String)"Unexpected login acknowledgement packet", (Object[])new Object[0]);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(Objects.requireNonNull(this.authenticatedProfile), this.transferred);
        ServerConfigurationPacketListenerImpl configPacketListener = new ServerConfigurationPacketListenerImpl(this.server, this.connection, cookie);
        this.connection.setupInboundProtocol(ConfigurationProtocols.SERVERBOUND, configPacketListener);
        configPacketListener.startConfiguration();
        this.state = State.ACCEPTED;
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReport report, CrashReportCategory connectionDetails) {
        connectionDetails.setDetail("Login phase", () -> this.state.toString());
    }

    @Override
    public void handleCookieResponse(ServerboundCookieResponsePacket packet) {
        this.disconnect(ServerCommonPacketListenerImpl.DISCONNECT_UNEXPECTED_QUERY);
    }

    private static enum State {
        HELLO,
        KEY,
        AUTHENTICATING,
        NEGOTIATING,
        VERIFYING,
        WAITING_FOR_DUPE_DISCONNECT,
        PROTOCOL_SWITCHING,
        ACCEPTED;

    }
}

