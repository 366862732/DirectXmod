/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  com.mojang.logging.LogUtils
 *  io.netty.bootstrap.Bootstrap
 *  io.netty.channel.Channel
 *  io.netty.channel.ChannelException
 *  io.netty.channel.ChannelFuture
 *  io.netty.channel.ChannelFutureListener
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.ChannelHandlerContext
 *  io.netty.channel.ChannelInboundHandler
 *  io.netty.channel.ChannelInitializer
 *  io.netty.channel.ChannelOption
 *  io.netty.channel.ChannelOutboundHandler
 *  io.netty.channel.ChannelOutboundHandlerAdapter
 *  io.netty.channel.ChannelPipeline
 *  io.netty.channel.ChannelPromise
 *  io.netty.channel.SimpleChannelInboundHandler
 *  io.netty.channel.local.LocalChannel
 *  io.netty.channel.local.LocalServerChannel
 *  io.netty.handler.flow.FlowControlHandler
 *  io.netty.handler.timeout.ReadTimeoutHandler
 *  io.netty.handler.timeout.TimeoutException
 *  io.netty.util.concurrent.GenericFutureListener
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 *  org.slf4j.Marker
 *  org.slf4j.MarkerFactory
 */
package net.minecraft.network;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import net.minecraft.SharedConstants;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.CipherDecoder;
import net.minecraft.network.CipherEncoder;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.LocalFrameDecoder;
import net.minecraft.network.LocalFrameEncoder;
import net.minecraft.network.MonitoredLocalFrameDecoder;
import net.minecraft.network.PacketBundlePacker;
import net.minecraft.network.PacketBundleUnpacker;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.SkipPacketException;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.UnconfiguredPipelineHandler;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.Varint21LengthFieldPrepender;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Connection
extends SimpleChannelInboundHandler<Packet<?>> {
    private static final float AVERAGE_PACKETS_SMOOTHING = 0.75f;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Marker ROOT_MARKER = MarkerFactory.getMarker((String)"NETWORK");
    public static final Marker PACKET_MARKER = Util.make(MarkerFactory.getMarker((String)"NETWORK_PACKETS"), m -> m.add(ROOT_MARKER));
    public static final Marker PACKET_RECEIVED_MARKER = Util.make(MarkerFactory.getMarker((String)"PACKET_RECEIVED"), m -> m.add(PACKET_MARKER));
    public static final Marker PACKET_SENT_MARKER = Util.make(MarkerFactory.getMarker((String)"PACKET_SENT"), m -> m.add(PACKET_MARKER));
    private static final ProtocolInfo<ServerHandshakePacketListener> INITIAL_PROTOCOL = HandshakeProtocols.SERVERBOUND;
    private final PacketFlow receiving;
    private volatile boolean sendLoginDisconnect = true;
    private final Queue<Consumer<Connection>> pendingActions = Queues.newConcurrentLinkedQueue();
    private Channel channel;
    private SocketAddress address;
    private volatile @Nullable PacketListener disconnectListener;
    private volatile @Nullable PacketListener packetListener;
    private @Nullable DisconnectionDetails disconnectionDetails;
    private boolean disconnectionHandled;
    private int receivedPackets;
    private int sentPackets;
    private float averageReceivedPackets;
    private float averageSentPackets;
    private int tickCount;
    private boolean handlingFault;
    private volatile @Nullable DisconnectionDetails delayedDisconnect;
    private @Nullable BandwidthDebugMonitor bandwidthDebugMonitor;
    private @Nullable UUID intendedProfileId;

    public Connection(PacketFlow receiving) {
        this.receiving = receiving;
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
        this.address = this.channel.remoteAddress();
        if (this.delayedDisconnect != null) {
            this.disconnect(this.delayedDisconnect);
        }
    }

    public void channelInactive(ChannelHandlerContext ctx) {
        this.disconnect(Component.translatable("disconnect.endOfStream"));
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SkipPacketException) {
            LOGGER.debug("Skipping packet due to errors", cause.getCause());
            return;
        }
        boolean isFirstFault = !this.handlingFault;
        this.handlingFault = true;
        if (!this.channel.isOpen()) {
            return;
        }
        if (cause instanceof TimeoutException) {
            LOGGER.debug("Timeout", cause);
            this.disconnect(Component.translatable("disconnect.timeout"));
        } else {
            MutableComponent reason = Component.translatable("disconnect.genericReason", "Internal Exception: " + String.valueOf(cause));
            PacketListener listener = this.packetListener;
            DisconnectionDetails details = listener != null ? listener.createDisconnectionInfo(reason, cause) : new DisconnectionDetails(reason);
            if (isFirstFault) {
                LOGGER.debug("Failed to sent packet", cause);
                if (this.getSending() == PacketFlow.CLIENTBOUND) {
                    Record packet = this.sendLoginDisconnect ? new ClientboundLoginDisconnectPacket(reason) : new ClientboundDisconnectPacket(reason);
                    this.send((Packet<?>)((Object)packet), PacketSendListener.thenRun(() -> this.disconnect(details)));
                } else {
                    this.disconnect(details);
                }
                this.setReadOnly();
            } else {
                LOGGER.debug("Double fault", cause);
                this.disconnect(details);
            }
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> packet) {
        if (!this.channel.isOpen()) {
            return;
        }
        PacketListener packetListener = this.packetListener;
        if (packetListener == null) {
            throw new IllegalStateException("Received a packet before the packet listener was initialized");
        }
        if (packetListener.shouldHandleMessage(packet)) {
            try {
                Connection.genericsFtw(packet, packetListener);
            }
            catch (RunningOnDifferentThreadException runningOnDifferentThreadException) {
            }
            catch (RejectedExecutionException ignored) {
                this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
            }
            catch (ClassCastException exception) {
                LOGGER.error("Received {} that couldn't be processed", packet.getClass(), (Object)exception);
                this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
            }
            ++this.receivedPackets;
        }
    }

    private static <T extends PacketListener> void genericsFtw(Packet<T> packet, PacketListener listener) {
        packet.handle(listener);
    }

    private void validateListener(ProtocolInfo<?> protocol, PacketListener packetListener) {
        Objects.requireNonNull(packetListener, "packetListener");
        PacketFlow listenerFlow = packetListener.flow();
        if (listenerFlow != this.receiving) {
            throw new IllegalStateException("Trying to set listener for wrong side: connection is " + String.valueOf((Object)this.receiving) + ", but listener is " + String.valueOf((Object)listenerFlow));
        }
        ConnectionProtocol listenerProtocol = packetListener.protocol();
        if (protocol.id() != listenerProtocol) {
            throw new IllegalStateException("Listener protocol (" + String.valueOf((Object)listenerProtocol) + ") does not match requested one " + String.valueOf(protocol));
        }
    }

    private static void syncAfterConfigurationChange(ChannelFuture future) {
        try {
            future.syncUninterruptibly();
        }
        catch (Exception e) {
            if (e instanceof ClosedChannelException) {
                LOGGER.info("Connection closed during protocol change");
                return;
            }
            throw e;
        }
    }

    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T packetListener) {
        this.validateListener(protocol, packetListener);
        if (protocol.flow() != this.getReceiving()) {
            throw new IllegalStateException("Invalid inbound protocol: " + String.valueOf((Object)protocol.id()));
        }
        this.packetListener = packetListener;
        this.disconnectListener = null;
        UnconfiguredPipelineHandler.InboundConfigurationTask configMessage = UnconfiguredPipelineHandler.setupInboundProtocol(protocol);
        BundlerInfo bundlerInfo = protocol.bundlerInfo();
        if (bundlerInfo != null) {
            PacketBundlePacker newBundler = new PacketBundlePacker(bundlerInfo);
            configMessage = configMessage.andThen(ctx -> ctx.pipeline().addAfter("decoder", "bundler", (ChannelHandler)newBundler));
        }
        Connection.syncAfterConfigurationChange(this.channel.writeAndFlush((Object)configMessage));
    }

    public void setupOutboundProtocol(ProtocolInfo<?> protocol) {
        if (protocol.flow() != this.getSending()) {
            throw new IllegalStateException("Invalid outbound protocol: " + String.valueOf((Object)protocol.id()));
        }
        UnconfiguredPipelineHandler.OutboundConfigurationTask configMessage = UnconfiguredPipelineHandler.setupOutboundProtocol(protocol);
        BundlerInfo bundlerInfo = protocol.bundlerInfo();
        if (bundlerInfo != null) {
            PacketBundleUnpacker newUnbundler = new PacketBundleUnpacker(bundlerInfo);
            configMessage = configMessage.andThen(ctx -> ctx.pipeline().addAfter("encoder", "unbundler", (ChannelHandler)newUnbundler));
        }
        boolean isLoginProtocol = protocol.id() == ConnectionProtocol.LOGIN;
        Connection.syncAfterConfigurationChange(this.channel.writeAndFlush((Object)configMessage.andThen(ctx -> {
            this.sendLoginDisconnect = isLoginProtocol;
        })));
    }

    public void setListenerForServerboundHandshake(PacketListener packetListener) {
        if (this.packetListener != null) {
            throw new IllegalStateException("Listener already set");
        }
        if (this.receiving != PacketFlow.SERVERBOUND || packetListener.flow() != PacketFlow.SERVERBOUND || packetListener.protocol() != INITIAL_PROTOCOL.id()) {
            throw new IllegalStateException("Invalid initial listener");
        }
        this.packetListener = packetListener;
    }

    public void initiateServerboundStatusConnection(String hostName, int port, ClientStatusPacketListener listener) {
        this.initiateServerboundConnection(hostName, port, StatusProtocols.SERVERBOUND, StatusProtocols.CLIENTBOUND, listener, ClientIntent.STATUS);
    }

    public void initiateServerboundPlayConnection(String hostName, int port, ClientLoginPacketListener listener) {
        this.initiateServerboundConnection(hostName, port, LoginProtocols.SERVERBOUND, LoginProtocols.CLIENTBOUND, listener, ClientIntent.LOGIN);
    }

    public <S extends ServerboundPacketListener, C extends ClientboundPacketListener> void initiateServerboundPlayConnection(String hostName, int port, ProtocolInfo<S> outbound, ProtocolInfo<C> inbound, C listener, boolean transfer) {
        this.initiateServerboundConnection(hostName, port, outbound, inbound, listener, transfer ? ClientIntent.TRANSFER : ClientIntent.LOGIN);
    }

    private <S extends ServerboundPacketListener, C extends ClientboundPacketListener> void initiateServerboundConnection(String hostName, int port, ProtocolInfo<S> outbound, ProtocolInfo<C> inbound, C listener, ClientIntent intent) {
        if (outbound.id() != inbound.id()) {
            throw new IllegalStateException("Mismatched initial protocols");
        }
        this.disconnectListener = listener;
        this.runOnceConnected(connection -> {
            this.setupInboundProtocol(inbound, listener);
            connection.sendPacket(new ClientIntentionPacket(SharedConstants.getCurrentVersion().protocolVersion(), hostName, port, intent), null, true);
            this.setupOutboundProtocol(outbound);
        });
    }

    public void send(Packet<?> packet) {
        this.send(packet, null);
    }

    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {
        this.send(packet, listener, true);
    }

    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        if (this.isConnected()) {
            this.flushQueue();
            this.sendPacket(packet, listener, flush);
        } else {
            this.pendingActions.add(connection -> connection.sendPacket(packet, listener, flush));
        }
    }

    public void runOnceConnected(Consumer<Connection> action) {
        if (this.isConnected()) {
            this.flushQueue();
            action.accept(this);
        } else {
            this.pendingActions.add(action);
        }
    }

    private void sendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        ++this.sentPackets;
        if (this.channel.eventLoop().inEventLoop()) {
            this.doSendPacket(packet, listener, flush);
        } else {
            this.channel.eventLoop().execute(() -> this.doSendPacket(packet, listener, flush));
        }
    }

    private void doSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        if (listener != null) {
            ChannelFuture future = flush ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
            future.addListener((GenericFutureListener)listener);
        } else if (flush) {
            this.channel.writeAndFlush(packet, this.channel.voidPromise());
        } else {
            this.channel.write(packet, this.channel.voidPromise());
        }
    }

    public void flushChannel() {
        if (this.isConnected()) {
            this.flush();
        } else {
            this.pendingActions.add(Connection::flush);
        }
    }

    private void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        } else {
            this.channel.eventLoop().execute(() -> this.channel.flush());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void flushQueue() {
        if (this.channel == null || !this.channel.isOpen()) {
            return;
        }
        Queue<Consumer<Connection>> queue = this.pendingActions;
        synchronized (queue) {
            Consumer<Connection> pendingAction;
            while ((pendingAction = this.pendingActions.poll()) != null) {
                pendingAction.accept(this);
            }
        }
    }

    public void tick() {
        this.flushQueue();
        PacketListener packetListener = this.packetListener;
        if (packetListener instanceof TickablePacketListener) {
            TickablePacketListener tickable = (TickablePacketListener)packetListener;
            tickable.tick();
        }
        if (!this.isConnected() && !this.disconnectionHandled) {
            this.handleDisconnection();
        }
        if (this.channel != null) {
            this.channel.flush();
        }
        if (this.tickCount++ % 20 == 0) {
            this.tickSecond();
        }
        if (this.bandwidthDebugMonitor != null) {
            this.bandwidthDebugMonitor.tick();
        }
    }

    protected void tickSecond() {
        this.averageSentPackets = Mth.lerp(0.75f, this.sentPackets, this.averageSentPackets);
        this.averageReceivedPackets = Mth.lerp(0.75f, this.receivedPackets, this.averageReceivedPackets);
        this.sentPackets = 0;
        this.receivedPackets = 0;
    }

    public SocketAddress getRemoteAddress() {
        return this.address;
    }

    public String getLoggableAddress(boolean logIPs) {
        if (this.address == null) {
            return "local";
        }
        if (logIPs) {
            return this.address.toString();
        }
        return "IP hidden";
    }

    public void disconnect(Component reason) {
        this.disconnect(new DisconnectionDetails(reason));
    }

    public void disconnect(DisconnectionDetails details) {
        if (this.channel == null) {
            this.delayedDisconnect = details;
        }
        if (this.isConnected()) {
            this.channel.close().awaitUninterruptibly();
            this.disconnectionDetails = details;
        }
    }

    public boolean isMemoryConnection() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public PacketFlow getReceiving() {
        return this.receiving;
    }

    public PacketFlow getSending() {
        return this.receiving.getOpposite();
    }

    public static Connection connectToServer(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, @Nullable LocalSampleLogger bandwidthLogger) {
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        if (bandwidthLogger != null) {
            connection.setBandwidthLogger(bandwidthLogger);
        }
        ChannelFuture connect = Connection.connect(address, eventLoopGroupHolder, connection);
        connect.syncUninterruptibly();
        return connection;
    }

    public static ChannelFuture connect(InetSocketAddress address, EventLoopGroupHolder eventLoopGroupHolder, final Connection connection) {
        return ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(eventLoopGroupHolder.eventLoopGroup())).handler((ChannelHandler)new ChannelInitializer<Channel>(){

            protected void initChannel(Channel channel) {
                try {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, (Object)true);
                }
                catch (ChannelException channelException) {
                    // empty catch block
                }
                ChannelPipeline pipeline = channel.pipeline().addLast("timeout", (ChannelHandler)new ReadTimeoutHandler(30));
                Connection.configureSerialization(pipeline, PacketFlow.CLIENTBOUND, false, connection.bandwidthDebugMonitor);
                connection.configurePacketHandler(pipeline);
            }
        })).channel(eventLoopGroupHolder.channelCls())).connect(address.getAddress(), address.getPort());
    }

    private static String outboundHandlerName(boolean configureOutbound) {
        return configureOutbound ? "encoder" : "outbound_config";
    }

    private static String inboundHandlerName(boolean configureInbound) {
        return configureInbound ? "decoder" : "inbound_config";
    }

    public void configurePacketHandler(ChannelPipeline pipeline) {
        pipeline.addLast("hackfix", (ChannelHandler)new ChannelOutboundHandlerAdapter(this){
            {
                Objects.requireNonNull(this$0);
            }

            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                super.write(ctx, msg, promise);
            }
        }).addLast("packet_handler", (ChannelHandler)this);
    }

    public static void configureSerialization(ChannelPipeline pipeline, PacketFlow inboundDirection, boolean local, @Nullable BandwidthDebugMonitor monitor) {
        PacketFlow outboundDirection = inboundDirection.getOpposite();
        boolean configureInbound = inboundDirection == PacketFlow.SERVERBOUND;
        boolean configureOutbound = outboundDirection == PacketFlow.SERVERBOUND;
        pipeline.addLast("splitter", (ChannelHandler)Connection.createFrameDecoder(monitor, local)).addLast(new ChannelHandler[]{new FlowControlHandler()}).addLast(Connection.inboundHandlerName(configureInbound), configureInbound ? new PacketDecoder<ServerHandshakePacketListener>(INITIAL_PROTOCOL) : new UnconfiguredPipelineHandler.Inbound()).addLast("prepender", (ChannelHandler)Connection.createFrameEncoder(local)).addLast(Connection.outboundHandlerName(configureOutbound), configureOutbound ? new PacketEncoder<ServerHandshakePacketListener>(INITIAL_PROTOCOL) : new UnconfiguredPipelineHandler.Outbound());
    }

    private static ChannelOutboundHandler createFrameEncoder(boolean local) {
        return local ? new LocalFrameEncoder() : new Varint21LengthFieldPrepender();
    }

    private static ChannelInboundHandler createFrameDecoder(@Nullable BandwidthDebugMonitor monitor, boolean local) {
        if (!local) {
            return new Varint21FrameDecoder(monitor);
        }
        if (monitor != null) {
            return new MonitoredLocalFrameDecoder(monitor);
        }
        return new LocalFrameDecoder();
    }

    public static void configureInMemoryPipeline(ChannelPipeline pipeline, PacketFlow packetFlow) {
        Connection.configureSerialization(pipeline, packetFlow, true, null);
    }

    public static Connection connectToLocalServer(SocketAddress address) {
        final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(EventLoopGroupHolder.local().eventLoopGroup())).handler((ChannelHandler)new ChannelInitializer<Channel>(){

            protected void initChannel(Channel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                Connection.configureInMemoryPipeline(pipeline, PacketFlow.CLIENTBOUND);
                connection.configurePacketHandler(pipeline);
            }
        })).channel(EventLoopGroupHolder.local().channelCls())).connect(address).syncUninterruptibly();
        return connection;
    }

    public static Connection fromChannel(Channel channel, PacketFlow flow, @Nullable LocalSampleLogger bandwidthLogger) {
        Connection connection = new Connection(flow);
        if (bandwidthLogger != null) {
            connection.setBandwidthLogger(bandwidthLogger);
        }
        ChannelPipeline pipeline = channel.pipeline().addLast("timeout", (ChannelHandler)new ReadTimeoutHandler(30));
        Connection.configureSerialization(pipeline, flow, false, connection.bandwidthDebugMonitor);
        connection.configurePacketHandler(pipeline);
        EventLoopGroupHolder.local().eventLoopGroup().register(channel).syncUninterruptibly();
        return connection;
    }

    public void setEncryptionKey(Cipher decryptCipher, Cipher encryptCipher) {
        this.channel.pipeline().addBefore("splitter", "decrypt", (ChannelHandler)new CipherDecoder(decryptCipher));
        this.channel.pipeline().addBefore("prepender", "encrypt", (ChannelHandler)new CipherEncoder(encryptCipher));
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean isConnecting() {
        return this.channel == null;
    }

    public @Nullable PacketListener getPacketListener() {
        return this.packetListener;
    }

    public @Nullable DisconnectionDetails getDisconnectionDetails() {
        return this.disconnectionDetails;
    }

    public void setReadOnly() {
        if (this.channel != null) {
            this.channel.config().setAutoRead(false);
        }
    }

    public void setupCompression(int threshold, boolean validateDecompressed) {
        if (threshold >= 0) {
            ChannelHandler channelHandler = this.channel.pipeline().get("decompress");
            if (channelHandler instanceof CompressionDecoder) {
                CompressionDecoder compressionDecoder = (CompressionDecoder)channelHandler;
                compressionDecoder.setThreshold(threshold, validateDecompressed);
            } else {
                this.channel.pipeline().addAfter("splitter", "decompress", (ChannelHandler)new CompressionDecoder(threshold, validateDecompressed));
            }
            channelHandler = this.channel.pipeline().get("compress");
            if (channelHandler instanceof CompressionEncoder) {
                CompressionEncoder compressionEncoder = (CompressionEncoder)channelHandler;
                compressionEncoder.setThreshold(threshold);
            } else {
                this.channel.pipeline().addAfter("prepender", "compress", (ChannelHandler)new CompressionEncoder(threshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
                this.channel.pipeline().remove("decompress");
            }
            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
                this.channel.pipeline().remove("compress");
            }
        }
    }

    public void handleDisconnection() {
        PacketListener disconnectListener;
        if (this.channel == null || this.channel.isOpen()) {
            return;
        }
        if (this.disconnectionHandled) {
            LOGGER.warn("handleDisconnection() called twice");
            return;
        }
        this.disconnectionHandled = true;
        PacketListener packetListener = this.getPacketListener();
        PacketListener packetListener2 = disconnectListener = packetListener != null ? packetListener : this.disconnectListener;
        if (disconnectListener != null) {
            DisconnectionDetails details = Objects.requireNonNullElseGet(this.getDisconnectionDetails(), () -> new DisconnectionDetails(Component.translatable("multiplayer.disconnect.generic")));
            disconnectListener.onDisconnect(details);
        }
    }

    public float getAverageReceivedPackets() {
        return this.averageReceivedPackets;
    }

    public float getAverageSentPackets() {
        return this.averageSentPackets;
    }

    public void setBandwidthLogger(LocalSampleLogger bandwidthLogger) {
        this.bandwidthDebugMonitor = new BandwidthDebugMonitor(bandwidthLogger);
    }

    public void setIntendedProfileId(UUID profileId) {
        this.intendedProfileId = profileId;
    }

    public @Nullable UUID getIntendedProfileId() {
        return this.intendedProfileId;
    }
}

