/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.WorldVersion;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public abstract class DataGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final PackOutput vanillaPackOutput;
    protected final Set<String> allProviderIds = new HashSet<String>();
    protected final Map<String, DataProvider> providersToRun = new LinkedHashMap<String, DataProvider>();

    public DataGenerator(Path output) {
        this.vanillaPackOutput = new PackOutput(output);
    }

    public abstract void run() throws IOException;

    public PackGenerator getVanillaPack(boolean toRun) {
        return new PackGenerator(this, toRun, "vanilla", this.vanillaPackOutput);
    }

    public PackGenerator getBuiltinDatapack(boolean toRun, String packId) {
        Path packOutputDir = this.vanillaPackOutput.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft").resolve("datapacks").resolve(packId);
        return new PackGenerator(this, toRun, packId, new PackOutput(packOutputDir));
    }

    static {
        Bootstrap.bootStrap();
    }

    public class PackGenerator {
        private final boolean toRun;
        private final String providerPrefix;
        private final PackOutput output;
        final /* synthetic */ DataGenerator this$0;

        private PackGenerator(DataGenerator this$0, boolean toRun, String providerPrefix, PackOutput output) {
            DataGenerator dataGenerator = this$0;
            Objects.requireNonNull(dataGenerator);
            this.this$0 = dataGenerator;
            this.toRun = toRun;
            this.providerPrefix = providerPrefix;
            this.output = output;
        }

        public <T extends DataProvider> T addProvider(DataProvider.Factory<T> factory) {
            T provider = factory.create(this.output);
            String providerId = this.providerPrefix + "/" + provider.getName();
            if (!this.this$0.allProviderIds.add(providerId)) {
                throw new IllegalStateException("Duplicate provider: " + providerId);
            }
            if (this.toRun) {
                this.this$0.providersToRun.put(providerId, (DataProvider)provider);
            }
            return provider;
        }
    }

    public static class Uncached
    extends DataGenerator {
        public Uncached(Path output) {
            super(output);
        }

        @Override
        public void run() throws IOException {
            Stopwatch totalTime = Stopwatch.createStarted();
            Stopwatch stopwatch = Stopwatch.createUnstarted();
            this.providersToRun.forEach((providerId, provider) -> {
                LOGGER.info("Starting uncached provider: {}", providerId);
                stopwatch.start();
                provider.run(CachedOutput.NO_CACHE).join();
                stopwatch.stop();
                LOGGER.info("{} finished after {} ms", providerId, (Object)stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
            });
            LOGGER.info("All providers took: {} ms", (Object)totalTime.elapsed(TimeUnit.MILLISECONDS));
        }
    }

    public static class Cached
    extends DataGenerator {
        private final Path rootOutputFolder;
        private final WorldVersion version;
        private final boolean alwaysGenerate;

        public Cached(Path output, WorldVersion version, boolean alwaysGenerate) {
            super(output);
            this.rootOutputFolder = output;
            this.alwaysGenerate = alwaysGenerate;
            this.version = version;
        }

        @Override
        public void run() throws IOException {
            HashCache cache = new HashCache(this.rootOutputFolder, this.allProviderIds, this.version);
            Stopwatch totalTime = Stopwatch.createStarted();
            Stopwatch stopwatch = Stopwatch.createUnstarted();
            this.providersToRun.forEach((providerId, provider) -> {
                if (!this.alwaysGenerate && !cache.shouldRunInThisVersion((String)providerId)) {
                    LOGGER.debug("Generator {} already run for version {}", providerId, (Object)this.version.name());
                    return;
                }
                LOGGER.info("Starting provider: {}", providerId);
                stopwatch.start();
                cache.applyUpdate(cache.generateUpdate((String)providerId, provider::run).join());
                stopwatch.stop();
                LOGGER.info("{} finished after {} ms", providerId, (Object)stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
            });
            LOGGER.info("All providers took: {} ms", (Object)totalTime.elapsed(TimeUnit.MILLISECONDS));
            cache.purgeStaleAndWrite();
        }
    }
}

