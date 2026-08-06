/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.SummaryReporter;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionReadEvent;
import net.minecraft.util.profiling.jfr.event.ChunkRegionWriteEvent;
import net.minecraft.util.profiling.jfr.event.ClientFpsEvent;
import net.minecraft.util.profiling.jfr.event.NetworkSummaryEvent;
import net.minecraft.util.profiling.jfr.event.PacketReceivedEvent;
import net.minecraft.util.profiling.jfr.event.PacketSentEvent;
import net.minecraft.util.profiling.jfr.event.ServerTickTimeEvent;
import net.minecraft.util.profiling.jfr.event.StructureGenerationEvent;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class JfrProfiler
implements JvmProfiler {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String ROOT_CATEGORY = "Minecraft";
    public static final String WORLD_GEN_CATEGORY = "World Generation";
    public static final String TICK_CATEGORY = "Ticking";
    public static final String NETWORK_CATEGORY = "Network";
    public static final String STORAGE_CATEGORY = "Storage";
    private static final List<Class<? extends Event>> CUSTOM_EVENTS = List.of(ChunkGenerationEvent.class, ChunkRegionReadEvent.class, ChunkRegionWriteEvent.class, PacketReceivedEvent.class, PacketSentEvent.class, NetworkSummaryEvent.class, ServerTickTimeEvent.class, ClientFpsEvent.class, StructureGenerationEvent.class, WorldLoadFinishedEvent.class);
    private static final String FLIGHT_RECORDER_CONFIG = "/flightrecorder-config.jfc";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd-HHmmss").toFormatter(Locale.ROOT).withZone(ZoneId.systemDefault());
    private static final JfrProfiler INSTANCE = new JfrProfiler();
    private @Nullable Recording recording;
    private int currentFPS;
    private float currentAverageTickTimeServer;
    private final Map<String, NetworkSummaryEvent.SumAggregation> networkTrafficByAddress = new ConcurrentHashMap<String, NetworkSummaryEvent.SumAggregation>();
    private final Runnable periodicClientFps = () -> new ClientFpsEvent(this.currentFPS).commit();
    private final Runnable periodicServerTickTime = () -> new ServerTickTimeEvent(this.currentAverageTickTimeServer).commit();
    private final Runnable periodicNetworkSummary = () -> {
        Iterator<NetworkSummaryEvent.SumAggregation> iterator = this.networkTrafficByAddress.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().commitEvent();
            iterator.remove();
        }
    };

    private JfrProfiler() {
        CUSTOM_EVENTS.forEach(FlightRecorder::register);
        this.registerPeriodicEvents();
        FlightRecorder.addListener(new FlightRecorderListener(this){
            final /* synthetic */ JfrProfiler this$0;
            {
                JfrProfiler jfrProfiler = this$0;
                Objects.requireNonNull(jfrProfiler);
                this.this$0 = jfrProfiler;
            }

            @Override
            public void recordingStateChanged(Recording rec) {
                switch (rec.getState()) {
                    case STOPPED: {
                        this.this$0.registerPeriodicEvents();
                        break;
                    }
                }
            }
        });
    }

    private void registerPeriodicEvents() {
        JfrProfiler.addPeriodicEvent(ClientFpsEvent.class, this.periodicClientFps);
        JfrProfiler.addPeriodicEvent(ServerTickTimeEvent.class, this.periodicServerTickTime);
        JfrProfiler.addPeriodicEvent(NetworkSummaryEvent.class, this.periodicNetworkSummary);
    }

    private static void addPeriodicEvent(Class<? extends Event> eventClass, Runnable runnable) {
        FlightRecorder.removePeriodicEvent(runnable);
        FlightRecorder.addPeriodicEvent(eventClass, runnable);
    }

    public static JfrProfiler getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean start(Environment environment) {
        boolean bl;
        URL resource = JfrProfiler.class.getResource(FLIGHT_RECORDER_CONFIG);
        if (resource == null) {
            LOGGER.warn("Could not find default flight recorder config at {}", (Object)FLIGHT_RECORDER_CONFIG);
            return false;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
        try {
            bl = this.start(reader, environment);
        }
        catch (Throwable throwable) {
            try {
                try {
                    reader.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException e) {
                LOGGER.warn("Failed to start flight recorder using configuration at {}", (Object)resource, (Object)e);
                return false;
            }
        }
        reader.close();
        return bl;
    }

    @Override
    public Path stop() {
        if (this.recording == null) {
            throw new IllegalStateException("Not currently profiling");
        }
        this.networkTrafficByAddress.clear();
        Path report = this.recording.getDestination();
        this.recording.stop();
        return report;
    }

    @Override
    public boolean isRunning() {
        return this.recording != null;
    }

    @Override
    public boolean isAvailable() {
        return FlightRecorder.isAvailable();
    }

    private boolean start(Reader configurationFile, Environment environment) {
        if (this.isRunning()) {
            LOGGER.warn("Profiling already in progress");
            return false;
        }
        try {
            Configuration jfrConfig = Configuration.create(configurationFile);
            String startTimestamp = DATE_TIME_FORMATTER.format(Instant.now());
            this.recording = Util.make(new Recording(jfrConfig), self -> {
                CUSTOM_EVENTS.forEach(self::enable);
                self.setDumpOnExit(true);
                self.setToDisk(true);
                self.setName(String.format(Locale.ROOT, "%s-%s-%s", environment.getDescription(), SharedConstants.getCurrentVersion().name(), startTimestamp));
            });
            Path destination = Paths.get(String.format(Locale.ROOT, "debug/%s-%s.jfr", environment.getDescription(), startTimestamp), new String[0]);
            FileUtil.createDirectoriesSafe(destination.getParent());
            this.recording.setDestination(destination);
            this.recording.start();
            this.setupSummaryListener();
        }
        catch (IOException | ParseException exception) {
            LOGGER.warn("Failed to start jfr profiling", (Throwable)exception);
            return false;
        }
        LOGGER.info("Started flight recorder profiling id({}):name({}) - will dump to {} on exit or stop command", new Object[]{this.recording.getId(), this.recording.getName(), this.recording.getDestination()});
        return true;
    }

    private void setupSummaryListener() {
        FlightRecorder.addListener(new FlightRecorderListener(this){
            private final SummaryReporter summaryReporter;
            final /* synthetic */ JfrProfiler this$0;
            {
                JfrProfiler jfrProfiler = this$0;
                Objects.requireNonNull(jfrProfiler);
                this.this$0 = jfrProfiler;
                this.summaryReporter = new SummaryReporter(() -> {
                    this.this$0.recording = null;
                });
            }

            @Override
            public void recordingStateChanged(Recording rec) {
                if (rec != this.this$0.recording) {
                    return;
                }
                switch (rec.getState()) {
                    case STOPPED: {
                        this.summaryReporter.recordingStopped(rec.getDestination());
                        FlightRecorder.removeListener(this);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onClientTick(int fps) {
        if (ClientFpsEvent.TYPE.isEnabled()) {
            this.currentFPS = fps;
        }
    }

    @Override
    public void onServerTick(float currentAverageTickTime) {
        if (ServerTickTimeEvent.TYPE.isEnabled()) {
            this.currentAverageTickTimeServer = currentAverageTickTime;
        }
    }

    @Override
    public void onPacketReceived(ConnectionProtocol protocol, PacketType<?> packetId, SocketAddress remoteAddress, int readableBytes) {
        if (PacketReceivedEvent.TYPE.isEnabled()) {
            new PacketReceivedEvent(protocol.id(), packetId.flow().id(), packetId.id().toString(), remoteAddress, readableBytes).commit();
        }
        if (NetworkSummaryEvent.TYPE.isEnabled()) {
            this.networkStatFor(remoteAddress).trackReceivedPacket(readableBytes);
        }
    }

    @Override
    public void onPacketSent(ConnectionProtocol protocol, PacketType<?> packetId, SocketAddress remoteAddress, int writtenBytes) {
        if (PacketSentEvent.TYPE.isEnabled()) {
            new PacketSentEvent(protocol.id(), packetId.flow().id(), packetId.id().toString(), remoteAddress, writtenBytes).commit();
        }
        if (NetworkSummaryEvent.TYPE.isEnabled()) {
            this.networkStatFor(remoteAddress).trackSentPacket(writtenBytes);
        }
    }

    private NetworkSummaryEvent.SumAggregation networkStatFor(SocketAddress remoteAddress) {
        return this.networkTrafficByAddress.computeIfAbsent(remoteAddress.toString(), NetworkSummaryEvent.SumAggregation::new);
    }

    @Override
    public void onRegionFileRead(RegionStorageInfo info, ChunkPos pos, RegionFileVersion version, int readBytes) {
        if (ChunkRegionReadEvent.TYPE.isEnabled()) {
            new ChunkRegionReadEvent(info, pos, version, readBytes).commit();
        }
    }

    @Override
    public void onRegionFileWrite(RegionStorageInfo info, ChunkPos pos, RegionFileVersion version, int writtenBytes) {
        if (ChunkRegionWriteEvent.TYPE.isEnabled()) {
            new ChunkRegionWriteEvent(info, pos, version, writtenBytes).commit();
        }
    }

    @Override
    public @Nullable ProfiledDuration onWorldLoadedStarted() {
        if (!WorldLoadFinishedEvent.TYPE.isEnabled()) {
            return null;
        }
        WorldLoadFinishedEvent event = new WorldLoadFinishedEvent();
        event.begin();
        return ignored -> event.commit();
    }

    @Override
    public @Nullable ProfiledDuration onChunkGenerate(ChunkPos pos, ResourceKey<Level> dimension, String name) {
        if (!ChunkGenerationEvent.TYPE.isEnabled()) {
            return null;
        }
        ChunkGenerationEvent event = new ChunkGenerationEvent(pos, dimension, name);
        event.begin();
        return ignored -> event.commit();
    }

    @Override
    public @Nullable ProfiledDuration onStructureGenerate(ChunkPos sourceChunkPos, ResourceKey<Level> dimension, Holder<Structure> structure) {
        if (!StructureGenerationEvent.TYPE.isEnabled()) {
            return null;
        }
        StructureGenerationEvent event = new StructureGenerationEvent(sourceChunkPos, structure, dimension);
        event.begin();
        return success -> {
            event.success = success;
            event.commit();
        };
    }
}

