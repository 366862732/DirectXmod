/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.channel.ChannelFuture
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ConnectScreen
extends Screen {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long NARRATION_DELAY_MS = 2000L;
    public static final Component ABORT_CONNECTION = Component.translatable("connect.aborted");
    public static final Component UNKNOWN_HOST_MESSAGE = Component.translatable("disconnect.genericReason", Component.translatable("disconnect.unknownHost"));
    private volatile @Nullable Connection connection;
    private @Nullable ChannelFuture channelFuture;
    private volatile boolean aborted;
    private final Screen parent;
    private Component status = Component.translatable("connect.connecting");
    private long lastNarration = -1L;
    private final Component connectFailedTitle;

    private ConnectScreen(Screen parent, Component connectFailedTitle) {
        super(GameNarrator.NO_TITLE);
        this.parent = parent;
        this.connectFailedTitle = connectFailedTitle;
    }

    public static void startConnecting(Screen parent, Minecraft minecraft, ServerAddress hostAndPort, ServerData data, boolean isQuickPlay, @Nullable TransferState transferState) {
        if (minecraft.gui.screen() instanceof ConnectScreen) {
            LOGGER.error("Attempt to connect while already connecting");
            return;
        }
        Component connectFailedTitle = transferState != null ? CommonComponents.TRANSFER_CONNECT_FAILED : (isQuickPlay ? QuickPlay.ERROR_TITLE : CommonComponents.CONNECT_FAILED);
        ConnectScreen screen = new ConnectScreen(parent, connectFailedTitle);
        if (transferState != null) {
            screen.updateStatus(Component.translatable("connect.transferring"));
        }
        minecraft.disconnectWithProgressScreen(false);
        minecraft.prepareForMultiplayer();
        minecraft.updateReportEnvironment(ReportEnvironment.thirdParty(data.ip));
        minecraft.quickPlayLog().setWorldData(QuickPlayLog.Type.MULTIPLAYER, data.ip, data.name);
        minecraft.gui.setScreen(screen);
        screen.connect(minecraft, hostAndPort, data, transferState);
    }

    private void connect(final Minecraft minecraft, final ServerAddress hostAndPort, final ServerData server, final @Nullable TransferState transferState) {
        LOGGER.info("Connecting to {}, {}", (Object)hostAndPort.getHost(), (Object)hostAndPort.getPort());
        Thread thread = new Thread(this, "Server Connector #" + UNIQUE_THREAD_ID.incrementAndGet()){
            final /* synthetic */ ConnectScreen this$0;
            {
                ConnectScreen connectScreen = this$0;
                Objects.requireNonNull(connectScreen);
                this.this$0 = connectScreen;
                super(name);
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                InetSocketAddress address = null;
                try {
                    Connection pendingConnection;
                    if (this.this$0.aborted) {
                        return;
                    }
                    Optional<InetSocketAddress> resolvedAddress = ServerNameResolver.DEFAULT.resolveAddress(hostAndPort).map(ResolvedServerAddress::asInetSocketAddress);
                    if (this.this$0.aborted) {
                        return;
                    }
                    if (resolvedAddress.isEmpty()) {
                        minecraft.execute(() -> minecraft2.gui.setScreen(new DisconnectedScreen(this.this$0.parent, this.this$0.connectFailedTitle, UNKNOWN_HOST_MESSAGE)));
                        return;
                    }
                    address = resolvedAddress.get();
                    ConnectScreen connectScreen = this.this$0;
                    synchronized (connectScreen) {
                        if (this.this$0.aborted) {
                            return;
                        }
                        pendingConnection = new Connection(PacketFlow.CLIENTBOUND);
                        pendingConnection.setBandwidthLogger(minecraft.getDebugOverlay().getBandwidthLogger());
                        this.this$0.channelFuture = Connection.connect(address, EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()), pendingConnection);
                    }
                    this.this$0.channelFuture.syncUninterruptibly();
                    connectScreen = this.this$0;
                    synchronized (connectScreen) {
                        if (this.this$0.aborted) {
                            pendingConnection.disconnect(ABORT_CONNECTION);
                            return;
                        }
                        this.this$0.connection = pendingConnection;
                        minecraft.getDownloadedPackSource().configureForServerControl(pendingConnection, 1.convertPackStatus(server.getResourcePackStatus()));
                    }
                    this.this$0.connection.initiateServerboundPlayConnection(address.getHostName(), address.getPort(), LoginProtocols.SERVERBOUND, LoginProtocols.CLIENTBOUND, new ClientHandshakePacketListenerImpl(this.this$0.connection, minecraft, server, this.this$0.parent, false, null, this.this$0::updateStatus, new LevelLoadTracker(), transferState), transferState != null);
                    this.this$0.connection.send(new ServerboundHelloPacket(minecraft.getUser().getName(), minecraft.getUser().getProfileId()));
                }
                catch (Exception exception) {
                    Exception originalCause;
                    if (this.this$0.aborted) {
                        return;
                    }
                    Throwable throwable = exception.getCause();
                    Exception cause = throwable instanceof Exception ? (originalCause = (Exception)throwable) : exception;
                    LOGGER.error("Couldn't connect to server", (Throwable)exception);
                    String message = address == null ? cause.getMessage() : cause.getMessage().replaceAll(address.getHostName() + ":" + address.getPort(), "").replaceAll(address.toString(), "");
                    minecraft.execute(() -> minecraft2.gui.setScreen(new DisconnectedScreen(this.this$0.parent, this.this$0.connectFailedTitle, (Component)Component.translatable("disconnect.genericReason", message))));
                }
            }

            private static ServerPackManager.PackPromptStatus convertPackStatus(ServerData.ServerPackStatus resourcePackStatus) {
                return switch (resourcePackStatus) {
                    default -> throw new MatchException(null, null);
                    case ServerData.ServerPackStatus.ENABLED -> ServerPackManager.PackPromptStatus.ALLOWED;
                    case ServerData.ServerPackStatus.DISABLED -> ServerPackManager.PackPromptStatus.DECLINED;
                    case ServerData.ServerPackStatus.PROMPT -> ServerPackManager.PackPromptStatus.PENDING;
                };
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    private void updateStatus(Component status) {
        this.status = status;
    }

    @Override
    public void tick() {
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                this.connection.tick();
            } else {
                this.connection.handleDisconnection();
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            ConnectScreen connectScreen = this;
            synchronized (connectScreen) {
                this.aborted = true;
                if (this.channelFuture != null) {
                    this.channelFuture.cancel(true);
                    this.channelFuture = null;
                }
                if (this.connection != null) {
                    this.connection.disconnect(ABORT_CONNECTION);
                }
            }
            this.minecraft.gui.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        long current = Util.getMillis();
        if (current - this.lastNarration > 2000L) {
            this.lastNarration = current;
            this.minecraft.getNarrator().saySystemNow(Component.translatable("narrator.joining"));
        }
        graphics.centeredText(this.font, this.status, this.width / 2, this.height / 2 - 50, -1);
    }
}

