/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonNull
 *  com.google.gson.JsonObject
 *  com.google.gson.LongSerializationPolicy
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.util.profiling.jfr.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.LongSerializationPolicy;
import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.jfr.Percentiles;
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
import net.minecraft.util.profiling.jfr.stats.TimedStatSummary;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class JfrResultJsonSerializer {
    private static final String BYTES_PER_SECOND = "bytesPerSecond";
    private static final String COUNT = "count";
    private static final String DURATION_NANOS_TOTAL = "durationNanosTotal";
    private static final String TOTAL_BYTES = "totalBytes";
    private static final String COUNT_PER_SECOND = "countPerSecond";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.DEFAULT).create();

    private static void serializePacketId(PacketIdentification identifier, JsonObject output) {
        output.addProperty("protocolId", identifier.protocolId());
        output.addProperty("packetId", identifier.packetId());
    }

    private static void serializeChunkId(ChunkIdentification identifier, JsonObject output) {
        output.addProperty("level", identifier.level());
        output.addProperty("dimension", identifier.dimension());
        output.addProperty("x", (Number)identifier.x());
        output.addProperty("z", (Number)identifier.z());
    }

    public String format(JfrStatsResult jfrStats) {
        JsonObject root = new JsonObject();
        root.addProperty("startedEpoch", (Number)jfrStats.recordingStarted().toEpochMilli());
        root.addProperty("endedEpoch", (Number)jfrStats.recordingEnded().toEpochMilli());
        root.addProperty("durationMs", (Number)jfrStats.recordingDuration().toMillis());
        Duration worldCreationDuration = jfrStats.worldCreationDuration();
        if (worldCreationDuration != null) {
            root.addProperty("worldGenDurationMs", (Number)worldCreationDuration.toMillis());
        }
        root.add("heap", this.heap(jfrStats.heapSummary()));
        root.add("cpuPercent", this.cpu(jfrStats.cpuLoadStats()));
        root.add("network", this.network(jfrStats));
        root.add("fileIO", this.fileIO(jfrStats));
        root.add("fps", this.fps(jfrStats.fps()));
        root.add("serverTick", this.serverTicks(jfrStats.serverTickTimes()));
        root.add("threadAllocation", this.threadAllocations(jfrStats.threadAllocationSummary()));
        root.add("chunkGen", this.chunkGen(jfrStats.chunkGenSummary()));
        root.add("structureGen", this.structureGen(jfrStats.structureGenStats()));
        return this.gson.toJson((JsonElement)root);
    }

    private JsonElement heap(GcHeapStat.Summary heapSummary) {
        JsonObject json = new JsonObject();
        json.addProperty("allocationRateBytesPerSecond", (Number)heapSummary.allocationRateBytesPerSecond());
        json.addProperty("gcCount", (Number)heapSummary.totalGCs());
        json.addProperty("gcOverHeadPercent", (Number)Float.valueOf(heapSummary.gcOverHead()));
        json.addProperty("gcTotalDurationMs", (Number)heapSummary.gcTotalDuration().toMillis());
        return json;
    }

    private JsonElement structureGen(List<StructureGenStat> structureGenStats) {
        JsonObject root = new JsonObject();
        Optional<TimedStatSummary<StructureGenStat>> optionalSummary = TimedStatSummary.summary(structureGenStats);
        if (optionalSummary.isEmpty()) {
            return root;
        }
        TimedStatSummary<StructureGenStat> summary = optionalSummary.get();
        JsonArray structureJsonArray = new JsonArray();
        root.add("structure", (JsonElement)structureJsonArray);
        structureGenStats.stream().collect(Collectors.groupingBy(StructureGenStat::structureName)).forEach((structureName, timedStat) -> {
            Optional optionalStatSummary = TimedStatSummary.summary(timedStat);
            if (optionalStatSummary.isEmpty()) {
                return;
            }
            TimedStatSummary statSummary = optionalStatSummary.get();
            JsonObject structureJson = new JsonObject();
            structureJsonArray.add((JsonElement)structureJson);
            structureJson.addProperty("name", structureName);
            structureJson.addProperty(COUNT, (Number)statSummary.count());
            structureJson.addProperty(DURATION_NANOS_TOTAL, (Number)statSummary.totalDuration().toNanos());
            structureJson.addProperty("durationNanosAvg", (Number)(statSummary.totalDuration().toNanos() / (long)statSummary.count()));
            JsonObject percentiles = Util.make(new JsonObject(), self -> structureJson.add("durationNanosPercentiles", (JsonElement)self));
            statSummary.percentilesNanos().forEach((percentile, v) -> percentiles.addProperty("p" + percentile, (Number)v));
            Function<StructureGenStat, JsonElement> structureGenStatJsonGenerator = structureGen -> {
                JsonObject result = new JsonObject();
                result.addProperty("durationNanos", (Number)structureGen.duration().toNanos());
                result.addProperty("chunkPosX", (Number)structureGen.chunkPos().x());
                result.addProperty("chunkPosZ", (Number)structureGen.chunkPos().z());
                result.addProperty("structureName", structureGen.structureName());
                result.addProperty("level", structureGen.level());
                result.addProperty("success", Boolean.valueOf(structureGen.success()));
                return result;
            };
            root.add("fastest", structureGenStatJsonGenerator.apply((StructureGenStat)summary.fastest()));
            root.add("slowest", structureGenStatJsonGenerator.apply((StructureGenStat)summary.slowest()));
            root.add("secondSlowest", (JsonElement)(summary.secondSlowest() != null ? structureGenStatJsonGenerator.apply((StructureGenStat)summary.secondSlowest()) : JsonNull.INSTANCE));
        });
        return root;
    }

    private JsonElement chunkGen(List<Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>>> chunkGenSummary) {
        JsonObject json = new JsonObject();
        if (chunkGenSummary.isEmpty()) {
            return json;
        }
        json.addProperty(DURATION_NANOS_TOTAL, (Number)chunkGenSummary.stream().mapToDouble(it -> ((TimedStatSummary)it.getSecond()).totalDuration().toNanos()).sum());
        JsonArray chunkJsonArray = Util.make(new JsonArray(), self -> json.add("status", (JsonElement)self));
        for (Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>> summaryByStatus : chunkGenSummary) {
            TimedStatSummary chunkStat = (TimedStatSummary)summaryByStatus.getSecond();
            JsonObject chunkStatusJson = Util.make(new JsonObject(), arg_0 -> ((JsonArray)chunkJsonArray).add(arg_0));
            chunkStatusJson.addProperty("state", ((ChunkStatus)summaryByStatus.getFirst()).toString());
            chunkStatusJson.addProperty(COUNT, (Number)chunkStat.count());
            chunkStatusJson.addProperty(DURATION_NANOS_TOTAL, (Number)chunkStat.totalDuration().toNanos());
            chunkStatusJson.addProperty("durationNanosAvg", (Number)(chunkStat.totalDuration().toNanos() / (long)chunkStat.count()));
            JsonObject percentiles = Util.make(new JsonObject(), self -> chunkStatusJson.add("durationNanosPercentiles", (JsonElement)self));
            chunkStat.percentilesNanos().forEach((percentile, value) -> percentiles.addProperty("p" + percentile, (Number)value));
            Function<ChunkGenStat, JsonElement> chunkGenStatJsonGenerator = chunk -> {
                JsonObject chunkGenStatJson = new JsonObject();
                chunkGenStatJson.addProperty("durationNanos", (Number)chunk.duration().toNanos());
                chunkGenStatJson.addProperty("level", chunk.level());
                chunkGenStatJson.addProperty("chunkPosX", (Number)chunk.chunkPos().x());
                chunkGenStatJson.addProperty("chunkPosZ", (Number)chunk.chunkPos().z());
                chunkGenStatJson.addProperty("worldPosX", (Number)chunk.worldPos().x());
                chunkGenStatJson.addProperty("worldPosZ", (Number)chunk.worldPos().z());
                return chunkGenStatJson;
            };
            chunkStatusJson.add("fastest", chunkGenStatJsonGenerator.apply((ChunkGenStat)chunkStat.fastest()));
            chunkStatusJson.add("slowest", chunkGenStatJsonGenerator.apply((ChunkGenStat)chunkStat.slowest()));
            chunkStatusJson.add("secondSlowest", (JsonElement)(chunkStat.secondSlowest() != null ? chunkGenStatJsonGenerator.apply((ChunkGenStat)chunkStat.secondSlowest()) : JsonNull.INSTANCE));
        }
        return json;
    }

    private JsonElement threadAllocations(ThreadAllocationStat.Summary threadAllocationSummary) {
        JsonArray threads = new JsonArray();
        threadAllocationSummary.allocationsPerSecondByThread().forEach((threadName, bytesPerSecond) -> threads.add((JsonElement)Util.make(new JsonObject(), json -> {
            json.addProperty("thread", threadName);
            json.addProperty(BYTES_PER_SECOND, (Number)bytesPerSecond);
        })));
        return threads;
    }

    private JsonElement serverTicks(List<TickTimeStat> tickTimeStats) {
        if (tickTimeStats.isEmpty()) {
            return JsonNull.INSTANCE;
        }
        JsonObject json = new JsonObject();
        double[] tickTimesMs = tickTimeStats.stream().mapToDouble(tickTimeStat -> (double)tickTimeStat.currentAverage().toNanos() / 1000000.0).toArray();
        DoubleSummaryStatistics summary = DoubleStream.of(tickTimesMs).summaryStatistics();
        json.addProperty("minMs", (Number)summary.getMin());
        json.addProperty("averageMs", (Number)summary.getAverage());
        json.addProperty("maxMs", (Number)summary.getMax());
        Map<Integer, Double> percentiles = Percentiles.evaluate(tickTimesMs);
        percentiles.forEach((percentile, value) -> json.addProperty("p" + percentile, (Number)value));
        return json;
    }

    private JsonElement fps(List<FpsStat> fpsStats) {
        if (fpsStats.isEmpty()) {
            return JsonNull.INSTANCE;
        }
        JsonObject json = new JsonObject();
        int[] fps = fpsStats.stream().mapToInt(FpsStat::fps).toArray();
        IntSummaryStatistics summary = IntStream.of(fps).summaryStatistics();
        json.addProperty("minFPS", (Number)summary.getMin());
        json.addProperty("averageFPS", (Number)summary.getAverage());
        json.addProperty("maxFPS", (Number)summary.getMax());
        Map<Integer, Double> percentiles = Percentiles.evaluate(fps);
        percentiles.forEach((percentile, value) -> json.addProperty("p" + percentile, (Number)value));
        return json;
    }

    private JsonElement fileIO(JfrStatsResult jfrStats) {
        JsonObject json = new JsonObject();
        json.add("write", this.fileIoSummary(jfrStats.fileWrites()));
        json.add("read", this.fileIoSummary(jfrStats.fileReads()));
        json.add("chunksRead", this.ioSummary(jfrStats.readChunks(), JfrResultJsonSerializer::serializeChunkId));
        json.add("chunksWritten", this.ioSummary(jfrStats.writtenChunks(), JfrResultJsonSerializer::serializeChunkId));
        return json;
    }

    private JsonElement fileIoSummary(FileIOStat.Summary io) {
        JsonObject json = new JsonObject();
        json.addProperty(TOTAL_BYTES, (Number)io.totalBytes());
        json.addProperty(COUNT, (Number)io.counts());
        json.addProperty(BYTES_PER_SECOND, (Number)io.bytesPerSecond());
        json.addProperty(COUNT_PER_SECOND, (Number)io.countsPerSecond());
        JsonArray topContributors = new JsonArray();
        json.add("topContributors", (JsonElement)topContributors);
        io.topTenContributorsByTotalBytes().forEach(contributor -> {
            JsonObject contributorJson = new JsonObject();
            topContributors.add((JsonElement)contributorJson);
            contributorJson.addProperty("path", (String)contributor.getFirst());
            contributorJson.addProperty(TOTAL_BYTES, (Number)contributor.getSecond());
        });
        return json;
    }

    private JsonElement network(JfrStatsResult jfrStats) {
        JsonObject json = new JsonObject();
        json.add("sent", this.ioSummary(jfrStats.sentPacketsSummary(), JfrResultJsonSerializer::serializePacketId));
        json.add("received", this.ioSummary(jfrStats.receivedPacketsSummary(), JfrResultJsonSerializer::serializePacketId));
        return json;
    }

    private <T> JsonElement ioSummary(IoSummary<T> summary, BiConsumer<T, JsonObject> elementWriter) {
        JsonObject json = new JsonObject();
        json.addProperty(TOTAL_BYTES, (Number)summary.getTotalSize());
        json.addProperty(COUNT, (Number)summary.getTotalCount());
        json.addProperty(BYTES_PER_SECOND, (Number)summary.getSizePerSecond());
        json.addProperty(COUNT_PER_SECOND, (Number)summary.getCountsPerSecond());
        JsonArray topContributors = new JsonArray();
        json.add("topContributors", (JsonElement)topContributors);
        summary.largestSizeContributors().forEach(contributor -> {
            JsonObject contributorJson = new JsonObject();
            topContributors.add((JsonElement)contributorJson);
            Object identifier = contributor.getFirst();
            IoSummary.CountAndSize countAndSize = (IoSummary.CountAndSize)contributor.getSecond();
            elementWriter.accept(identifier, contributorJson);
            contributorJson.addProperty(TOTAL_BYTES, (Number)countAndSize.totalSize());
            contributorJson.addProperty(COUNT, (Number)countAndSize.totalCount());
            contributorJson.addProperty("averageSize", (Number)Float.valueOf(countAndSize.averageSize()));
        });
        return json;
    }

    private JsonElement cpu(List<CpuLoadStat> cpuStats) {
        JsonObject json = new JsonObject();
        BiFunction<List, ToDoubleFunction, JsonObject> transformer = (cpuLoadStats, extractor) -> {
            JsonObject jsonGroup = new JsonObject();
            DoubleSummaryStatistics stats = cpuLoadStats.stream().mapToDouble(extractor).summaryStatistics();
            jsonGroup.addProperty("min", (Number)stats.getMin());
            jsonGroup.addProperty("average", (Number)stats.getAverage());
            jsonGroup.addProperty("max", (Number)stats.getMax());
            return jsonGroup;
        };
        json.add("jvm", (JsonElement)transformer.apply(cpuStats, CpuLoadStat::jvm));
        json.add("userJvm", (JsonElement)transformer.apply(cpuStats, CpuLoadStat::userJvm));
        json.add("system", (JsonElement)transformer.apply(cpuStats, CpuLoadStat::system));
        return json;
    }
}

