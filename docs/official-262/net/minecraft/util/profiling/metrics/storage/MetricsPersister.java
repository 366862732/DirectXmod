/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.IOUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.profiling.metrics.storage;

import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.storage.RecordedDeviation;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class MetricsPersister {
    public static final Path PROFILING_RESULTS_DIR = Paths.get("debug/profiling", new String[0]);
    public static final String METRICS_DIR_NAME = "metrics";
    public static final String DEVIATIONS_DIR_NAME = "deviations";
    public static final String PROFILING_RESULT_FILENAME = "profiling.txt";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String rootFolderName;

    public MetricsPersister(String rootFolderName) {
        this.rootFolderName = rootFolderName;
    }

    public Path saveReports(Set<MetricSampler> samplers, Map<MetricSampler, List<RecordedDeviation>> deviationsBySampler, ProfileResults profilerResults) {
        try {
            Files.createDirectories(PROFILING_RESULTS_DIR, new FileAttribute[0]);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            Path tempDir = Files.createTempDirectory("minecraft-profiling", new FileAttribute[0]);
            tempDir.toFile().deleteOnExit();
            Files.createDirectories(PROFILING_RESULTS_DIR, new FileAttribute[0]);
            Path workingDir = tempDir.resolve(this.rootFolderName);
            Path metricsDir = workingDir.resolve(METRICS_DIR_NAME);
            this.saveMetrics(samplers, metricsDir);
            if (!deviationsBySampler.isEmpty()) {
                this.saveDeviations(deviationsBySampler, workingDir.resolve(DEVIATIONS_DIR_NAME));
            }
            this.saveProfilingTaskExecutionResult(profilerResults, workingDir);
            return tempDir;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveMetrics(Set<MetricSampler> samplers, Path dir) {
        if (samplers.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one sampler to persist");
        }
        Map<MetricCategory, List<MetricSampler>> samplersByCategory = samplers.stream().collect(Collectors.groupingBy(MetricSampler::getCategory));
        samplersByCategory.forEach((category, samplersInCategory) -> this.saveCategory((MetricCategory)((Object)category), (List<MetricSampler>)samplersInCategory, dir));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void saveCategory(MetricCategory category, List<MetricSampler> samplers, Path dir) {
        Path file = dir.resolve(Util.sanitizeName(category.getDescription(), Identifier::validPathChar) + ".csv");
        BufferedWriter writer = null;
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, new OpenOption[0]);
            CsvOutput.Builder csvBuilder = CsvOutput.builder();
            csvBuilder.addColumn("@tick");
            for (MetricSampler sampler : samplers) {
                csvBuilder.addColumn(sampler.getName());
            }
            CsvOutput csvOutput = csvBuilder.build(writer);
            List results = samplers.stream().map(MetricSampler::result).collect(Collectors.toList());
            int firstTick = results.stream().mapToInt(MetricSampler.SamplerResult::getFirstTick).summaryStatistics().getMin();
            int lastTick = results.stream().mapToInt(MetricSampler.SamplerResult::getLastTick).summaryStatistics().getMax();
            for (int tick = firstTick; tick <= lastTick; ++tick) {
                int finalTick = tick;
                Stream<String> valuesStream = results.stream().map(it -> String.valueOf(it.valueAtTick(finalTick)));
                Object[] row = Stream.concat(Stream.of(String.valueOf(tick)), valuesStream).toArray(String[]::new);
                csvOutput.writeRow(row);
            }
            LOGGER.info("Flushed metrics to {}", (Object)file);
            IOUtils.closeQuietly((Writer)writer);
        }
        catch (Exception e) {
            LOGGER.error("Could not save profiler results to {}", (Object)file, (Object)e);
        }
        finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private void saveDeviations(Map<MetricSampler, List<RecordedDeviation>> deviationsBySampler, Path directory) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK).withZone(ZoneId.systemDefault());
        deviationsBySampler.forEach((sampler, deviations) -> deviations.forEach(deviation -> {
            String timestamp = formatter.format(deviation.timestamp);
            Path deviationLogFile = directory.resolve(Util.sanitizeName(sampler.getName(), Identifier::validPathChar)).resolve(String.format(Locale.ROOT, "%d@%s.txt", deviation.tick, timestamp));
            deviation.profilerResultAtTick.saveResults(deviationLogFile);
        }));
    }

    private void saveProfilingTaskExecutionResult(ProfileResults results, Path directory) {
        results.saveResults(directory.resolve(PROFILING_RESULT_FILENAME));
    }
}

