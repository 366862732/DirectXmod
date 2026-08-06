/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.profiling.jfr.parse;

import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.ChunkIdentification;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.FpsStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.IoSummary;
import net.minecraft.util.profiling.jfr.stats.PacketIdentification;
import net.minecraft.util.profiling.jfr.stats.StructureGenStat;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;
import org.jspecify.annotations.Nullable;

public class JfrStatsParser {
    private Instant recordingStarted = Instant.EPOCH;
    private Instant recordingEnded = Instant.EPOCH;
    private final List<ChunkGenStat> chunkGenStats = new ArrayList<ChunkGenStat>();
    private final List<StructureGenStat> structureGenStats = new ArrayList<StructureGenStat>();
    private final List<CpuLoadStat> cpuLoadStat = new ArrayList<CpuLoadStat>();
    private final Map<PacketIdentification, MutableCountAndSize> receivedPackets = new HashMap<PacketIdentification, MutableCountAndSize>();
    private final Map<PacketIdentification, MutableCountAndSize> sentPackets = new HashMap<PacketIdentification, MutableCountAndSize>();
    private final Map<ChunkIdentification, MutableCountAndSize> readChunks = new HashMap<ChunkIdentification, MutableCountAndSize>();
    private final Map<ChunkIdentification, MutableCountAndSize> writtenChunks = new HashMap<ChunkIdentification, MutableCountAndSize>();
    private final List<FileIOStat> fileWrites = new ArrayList<FileIOStat>();
    private final List<FileIOStat> fileReads = new ArrayList<FileIOStat>();
    private int garbageCollections;
    private Duration gcTotalDuration = Duration.ZERO;
    private final List<GcHeapStat> gcHeapStats = new ArrayList<GcHeapStat>();
    private final List<ThreadAllocationStat> threadAllocationStats = new ArrayList<ThreadAllocationStat>();
    private final List<FpsStat> fps = new ArrayList<FpsStat>();
    private final List<TickTimeStat> serverTickTimes = new ArrayList<TickTimeStat>();
    private @Nullable Duration worldCreationDuration = null;

    private JfrStatsParser(Stream<RecordedEvent> events) {
        this.capture(events);
    }

    public static JfrStatsResult parse(Path path) {
        JfrStatsResult jfrStatsResult;
        final RecordingFile recordingFile = new RecordingFile(path);
        try {
            Iterator<RecordedEvent> iterator = new Iterator<RecordedEvent>(){

                @Override
                public boolean hasNext() {
                    return recordingFile.hasMoreEvents();
                }

                @Override
                public RecordedEvent next() {
                    if (!this.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        return recordingFile.readEvent();
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            };
            Stream<RecordedEvent> events = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1297), false);
            jfrStatsResult = new JfrStatsParser(events).results();
        }
        catch (Throwable throwable) {
            try {
                try {
                    recordingFile.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        recordingFile.close();
        return jfrStatsResult;
    }

    private JfrStatsResult results() {
        Duration recordingDuration = Duration.between(this.recordingStarted, this.recordingEnded);
        return new JfrStatsResult(this.recordingStarted, this.recordingEnded, recordingDuration, this.worldCreationDuration, this.fps, this.serverTickTimes, this.cpuLoadStat, GcHeapStat.summary(recordingDuration, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections), ThreadAllocationStat.summary(this.threadAllocationStats), JfrStatsParser.collectIoStats(recordingDuration, this.receivedPackets), JfrStatsParser.collectIoStats(recordingDuration, this.sentPackets), JfrStatsParser.collectIoStats(recordingDuration, this.writtenChunks), JfrStatsParser.collectIoStats(recordingDuration, this.readChunks), FileIOStat.summary(recordingDuration, this.fileWrites), FileIOStat.summary(recordingDuration, this.fileReads), this.chunkGenStats, this.structureGenStats);
    }

    private void capture(Stream<RecordedEvent> events) {
        events.forEach(event -> {
            if (event.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
                this.recordingEnded = event.getEndTime();
            }
            if (event.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
                this.recordingStarted = event.getStartTime();
            }
            switch (event.getEventType().getName()) {
                case "minecraft.ChunkGeneration": {
                    this.chunkGenStats.add(ChunkGenStat.from(event));
                    break;
                }
                case "minecraft.StructureGeneration": {
                    this.structureGenStats.add(StructureGenStat.from(event));
                    break;
                }
                case "minecraft.LoadWorld": {
                    this.worldCreationDuration = event.getDuration();
                    break;
                }
                case "minecraft.ClientFps": {
                    this.fps.add(FpsStat.from(event, "fps"));
                    break;
                }
                case "minecraft.ServerTickTime": {
                    this.serverTickTimes.add(TickTimeStat.from(event));
                    break;
                }
                case "minecraft.PacketReceived": {
                    this.incrementPacket((RecordedEvent)event, event.getInt("bytes"), this.receivedPackets);
                    break;
                }
                case "minecraft.PacketSent": {
                    this.incrementPacket((RecordedEvent)event, event.getInt("bytes"), this.sentPackets);
                    break;
                }
                case "minecraft.ChunkRegionRead": {
                    this.incrementChunk((RecordedEvent)event, event.getInt("bytes"), this.readChunks);
                    break;
                }
                case "minecraft.ChunkRegionWrite": {
                    this.incrementChunk((RecordedEvent)event, event.getInt("bytes"), this.writtenChunks);
                    break;
                }
                case "jdk.ThreadAllocationStatistics": {
                    this.threadAllocationStats.add(ThreadAllocationStat.from(event));
                    break;
                }
                case "jdk.GCHeapSummary": {
                    this.gcHeapStats.add(GcHeapStat.from(event));
                    break;
                }
                case "jdk.CPULoad": {
                    this.cpuLoadStat.add(CpuLoadStat.from(event));
                    break;
                }
                case "jdk.FileWrite": {
                    this.appendFileIO((RecordedEvent)event, this.fileWrites, "bytesWritten");
                    break;
                }
                case "jdk.FileRead": {
                    this.appendFileIO((RecordedEvent)event, this.fileReads, "bytesRead");
                    break;
                }
                case "jdk.GarbageCollection": {
                    ++this.garbageCollections;
                    this.gcTotalDuration = this.gcTotalDuration.plus(event.getDuration());
                    break;
                }
            }
        });
    }

    private void incrementPacket(RecordedEvent event, int packetSize, Map<PacketIdentification, MutableCountAndSize> packets) {
        packets.computeIfAbsent(PacketIdentification.from(event), ignored -> new MutableCountAndSize()).increment(packetSize);
    }

    private void incrementChunk(RecordedEvent event, int chunkSize, Map<ChunkIdentification, MutableCountAndSize> packets) {
        packets.computeIfAbsent(ChunkIdentification.from(event), ignored -> new MutableCountAndSize()).increment(chunkSize);
    }

    private void appendFileIO(RecordedEvent event, List<FileIOStat> stats, String sizeField) {
        stats.add(new FileIOStat(event.getDuration(), event.getString("path"), event.getLong(sizeField)));
    }

    private static <T> IoSummary<T> collectIoStats(Duration recordingDuration, Map<T, MutableCountAndSize> packetStats) {
        List summaryStats = packetStats.entrySet().stream().map(e -> Pair.of(e.getKey(), (Object)((MutableCountAndSize)e.getValue()).toCountAndSize())).toList();
        return new IoSummary(recordingDuration, summaryStats);
    }

    public static final class MutableCountAndSize {
        private long count;
        private long totalSize;

        public void increment(int bytes) {
            this.totalSize += (long)bytes;
            ++this.count;
        }

        public IoSummary.CountAndSize toCountAndSize() {
            return new IoSummary.CountAndSize(this.count, this.totalSize);
        }
    }
}

