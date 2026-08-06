/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Function
 *  com.google.common.base.Ticker
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.common.escape.Escaper
 *  com.google.common.escape.Escapers
 *  com.google.common.util.concurrent.MoreExecutors
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.jtracy.TracyClient
 *  com.mojang.jtracy.Zone
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectLists
 *  it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ReferenceImmutableList
 *  it.unimi.dsi.fastutil.objects.ReferenceList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.google.common.base.Function;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.CharPredicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.TracingExecutor;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.util.TimeSource;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Util {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_MAX_THREADS = 255;
    private static final int DEFAULT_SAFE_FILE_OPERATION_RETRIES = 10;
    private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
    private static final TracingExecutor BACKGROUND_EXECUTOR = Util.makeExecutor("Main");
    private static final TracingExecutor IO_POOL = Util.makeIoExecutor("IO-Worker-", false);
    private static final TracingExecutor DOWNLOAD_POOL = Util.makeIoExecutor("Download-", true);
    private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
    public static final int LINEAR_LOOKUP_THRESHOLD = 8;
    private static final Set<String> ALLOWED_UNTRUSTED_LINK_PROTOCOLS = Set.of("http", "https");
    public static final long NANOS_PER_MILLI = 1000000L;
    private static TimeSource.NanoTimeSource timeSource = System::nanoTime;
    private static final TimeSource.NanoTimeSource INDIRECT_TIME_SOURCE = () -> timeSource.getAsLong();
    public static final Ticker TICKER = new Ticker(){

        public long read() {
            return timeSource.getAsLong();
        }
    };
    public static final UUID NIL_UUID = new UUID(0L, 0L);
    public static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = FileSystemProvider.installedProviders().stream().filter(p -> p.getScheme().equalsIgnoreCase("jar")).findFirst().orElseThrow(() -> new IllegalStateException("No jar file system provider found"));
    public static final Escaper CONTROL_CHARACTER_ESCAPER = Util.make(Escapers.builder(), escaper -> {
        HexFormat hexFormat = HexFormat.of().withUpperCase();
        for (char c = '\u0000'; c <= '\u00ff'; c = (char)(c + '\u0001')) {
            if (!Character.isISOControl(c)) continue;
            String replacement = switch (c) {
                case '\u0007' -> "\\a";
                case '\b' -> "\\b";
                case '\t' -> "\\t";
                case '\n' -> "\\n";
                case '\u000b' -> "\\v";
                case '\f' -> "\\f";
                case '\r' -> "\\r";
                default -> "\\x" + hexFormat.toHexDigits(c, 2);
            };
            escaper.addEscape(c, replacement);
        }
        escaper.addEscape('\\', "\\\\");
    }).build();
    private static Consumer<String> thePauser = msg -> {};

    public static <K, V> Collector<Map.Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static <T> Collector<T, ?, List<T>> toMutableList() {
        return Collectors.toCollection(Lists::newArrayList);
    }

    public static <T extends Comparable<T>> String getPropertyName(Property<T> key, Object value) {
        return key.getName((Comparable)value);
    }

    public static String makeDescriptionId(String prefix, @Nullable Identifier location) {
        if (location == null) {
            return prefix + ".unregistered_sadface";
        }
        return prefix + "." + location.getNamespace() + "." + location.getPath().replace('/', '.');
    }

    public static TimeSource.NanoTimeSource timeSource() {
        return INDIRECT_TIME_SOURCE;
    }

    public static void setTimeSource(TimeSource.NanoTimeSource timeSource) {
        Util.timeSource = timeSource;
    }

    public static void shutdownTimeSource() {
        long currentTime = timeSource.getAsLong();
        timeSource = TimeSource.constant(currentTime);
    }

    public static long getMillis() {
        return Util.getNanos() / 1000000L;
    }

    public static long getNanos() {
        return timeSource.getAsLong();
    }

    public static long getEpochMillis() {
        return Instant.now().toEpochMilli();
    }

    public static String getFilenameFormattedDateTime() {
        return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now());
    }

    private static TracingExecutor makeExecutor(final String name) {
        Object executor;
        int threads = Util.maxAllowedExecutorThreads();
        if (threads <= 0) {
            executor = MoreExecutors.newDirectExecutorService();
        } else {
            AtomicInteger workerCount = new AtomicInteger(1);
            executor = new ForkJoinPool(threads, pool -> {
                final String threadName = "Worker-" + name + "-" + workerCount.getAndIncrement();
                ForkJoinWorkerThread thread = new ForkJoinWorkerThread(pool){

                    @Override
                    protected void onStart() {
                        TracyClient.setThreadName((String)threadName, (int)name.hashCode());
                        super.onStart();
                    }

                    @Override
                    protected void onTermination(@Nullable Throwable exception) {
                        if (exception != null) {
                            LOGGER.warn("{} died", (Object)this.getName(), (Object)exception);
                        } else {
                            LOGGER.debug("{} shutdown", (Object)this.getName());
                        }
                        super.onTermination(exception);
                    }
                };
                thread.setName(threadName);
                return thread;
            }, Util::onThreadException, true);
        }
        return new TracingExecutor((ExecutorService)executor);
    }

    public static int maxAllowedExecutorThreads() {
        return Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, Util.getMaxThreads());
    }

    private static int getMaxThreads() {
        String maxThreadsString = System.getProperty(MAX_THREADS_SYSTEM_PROPERTY);
        if (maxThreadsString != null) {
            try {
                int maxThreads = Integer.parseInt(maxThreadsString);
                if (maxThreads >= 1 && maxThreads <= 255) {
                    return maxThreads;
                }
                LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", new Object[]{MAX_THREADS_SYSTEM_PROPERTY, maxThreadsString, 255});
            }
            catch (NumberFormatException e) {
                LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", new Object[]{MAX_THREADS_SYSTEM_PROPERTY, maxThreadsString, 255});
            }
        }
        return 255;
    }

    public static TracingExecutor backgroundExecutor() {
        return BACKGROUND_EXECUTOR;
    }

    public static TracingExecutor ioPool() {
        return IO_POOL;
    }

    public static TracingExecutor nonCriticalIoPool() {
        return DOWNLOAD_POOL;
    }

    public static void shutdownExecutors() {
        BACKGROUND_EXECUTOR.shutdownAndAwait(3L, TimeUnit.SECONDS);
        IO_POOL.shutdownAndAwait(3L, TimeUnit.SECONDS);
    }

    private static TracingExecutor makeIoExecutor(String prefix, boolean daemon) {
        AtomicInteger workerCount = new AtomicInteger(1);
        return new TracingExecutor(Executors.newCachedThreadPool(runnable -> {
            String name = prefix + workerCount.getAndIncrement();
            Thread thread = new Thread(runnable, name);
            TracyClient.setThreadName((String)name, (int)prefix.hashCode());
            thread.setDaemon(daemon);
            thread.setUncaughtExceptionHandler(Util::onThreadException);
            return thread;
        }));
    }

    public static void throwAsRuntime(Throwable throwable) {
        RuntimeException runtimeException;
        throw throwable instanceof RuntimeException ? (runtimeException = (RuntimeException)throwable) : new RuntimeException(throwable);
    }

    private static void onThreadException(Thread thread, Throwable throwable) {
        CrashReport report;
        Util.pauseInIde(throwable);
        if (throwable instanceof CompletionException) {
            throwable = throwable.getCause();
        }
        LOGGER.error("Caught exception in thread {}", (Object)thread, (Object)throwable);
        if (throwable instanceof ReportedException) {
            ReportedException reportedException = (ReportedException)throwable;
            report = reportedException.getReport();
        } else {
            report = CrashReport.forThrowable(throwable, "Exception on worker thread");
        }
        CrashReportCategory threadInfo = report.addCategory("ThreadInfo");
        threadInfo.setDetail("Name", thread.getName());
        BlockableEventLoop.relayDelayCrash(report);
    }

    public static @Nullable Type<?> fetchChoiceType(DSL.TypeReference reference, String name) {
        if (!SharedConstants.CHECK_DATA_FIXER_SCHEMA) {
            return null;
        }
        return Util.doFetchChoiceType(reference, name);
    }

    private static @Nullable Type<?> doFetchChoiceType(DSL.TypeReference reference, String name) {
        Type dataType;
        block2: {
            dataType = null;
            try {
                dataType = DataFixers.getDataFixer().getSchema(DataFixUtils.makeKey((int)SharedConstants.getCurrentVersion().dataVersion().version())).getChoiceType(reference, name);
            }
            catch (IllegalArgumentException e) {
                LOGGER.error("No data fixer registered for {}", (Object)name);
                if (!SharedConstants.IS_RUNNING_IN_IDE) break block2;
                throw e;
            }
        }
        return dataType;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void runNamed(Runnable runnable, String name) {
        block16: {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                Thread thread = Thread.currentThread();
                String oldName = thread.getName();
                thread.setName(name);
                try (Zone ignored = TracyClient.beginZone((String)name, (boolean)SharedConstants.IS_RUNNING_IN_IDE);){
                    runnable.run();
                    break block16;
                }
                finally {
                    thread.setName(oldName);
                }
            }
            try (Zone ignored = TracyClient.beginZone((String)name, (boolean)SharedConstants.IS_RUNNING_IN_IDE);){
                runnable.run();
            }
        }
    }

    public static <T> String getRegisteredName(Registry<T> registry, T entry) {
        Identifier key = registry.getKey(entry);
        if (key == null) {
            return "[unregistered]";
        }
        return key.toString();
    }

    public static <T> Predicate<T> allOf() {
        return context -> true;
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> condition) {
        return condition;
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> condition1, Predicate<? super T> condition2) {
        return context -> condition1.test(context) && condition2.test(context);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> condition1, Predicate<? super T> condition2, Predicate<? super T> condition3) {
        return context -> condition1.test(context) && condition2.test(context) && condition3.test(context);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> condition1, Predicate<? super T> condition2, Predicate<? super T> condition3, Predicate<? super T> condition4) {
        return context -> condition1.test(context) && condition2.test(context) && condition3.test(context) && condition4.test(context);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> condition1, Predicate<? super T> condition2, Predicate<? super T> condition3, Predicate<? super T> condition4, Predicate<? super T> condition5) {
        return context -> condition1.test(context) && condition2.test(context) && condition3.test(context) && condition4.test(context) && condition5.test(context);
    }

    @SafeVarargs
    public static <T> Predicate<T> allOf(Predicate<? super T> ... conditions) {
        return context -> {
            for (Predicate entry : conditions) {
                if (entry.test(context)) continue;
                return false;
            }
            return true;
        };
    }

    public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> conditions) {
        return switch (conditions.size()) {
            case 0 -> Util.allOf();
            case 1 -> Util.allOf(conditions.get(0));
            case 2 -> Util.allOf(conditions.get(0), conditions.get(1));
            case 3 -> Util.allOf(conditions.get(0), conditions.get(1), conditions.get(2));
            case 4 -> Util.allOf(conditions.get(0), conditions.get(1), conditions.get(2), conditions.get(3));
            case 5 -> Util.allOf(conditions.get(0), conditions.get(1), conditions.get(2), conditions.get(3), conditions.get(4));
            default -> {
                Predicate[] conditionsCopy = (Predicate[])conditions.toArray(Predicate[]::new);
                yield Util.allOf(conditionsCopy);
            }
        };
    }

    public static <T> Predicate<T> anyOf() {
        return context -> false;
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> condition1) {
        return condition1;
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> condition1, Predicate<? super T> condition2) {
        return context -> condition1.test(context) || condition2.test(context);
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> condition1, Predicate<? super T> condition2, Predicate<? super T> condition3) {
        return context -> condition1.test(context) || condition2.test(context) || condition3.test(context);
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> condition1, Predicate<? super T> condition2, Predicate<? super T> condition3, Predicate<? super T> condition4) {
        return context -> condition1.test(context) || condition2.test(context) || condition3.test(context) || condition4.test(context);
    }

    public static <T> Predicate<T> anyOf(Predicate<? super T> condition1, Predicate<? super T> condition2, Predicate<? super T> condition3, Predicate<? super T> condition4, Predicate<? super T> condition5) {
        return context -> condition1.test(context) || condition2.test(context) || condition3.test(context) || condition4.test(context) || condition5.test(context);
    }

    @SafeVarargs
    public static <T> Predicate<T> anyOf(Predicate<? super T> ... conditions) {
        return context -> {
            for (Predicate entry : conditions) {
                if (!entry.test(context)) continue;
                return true;
            }
            return false;
        };
    }

    public static <T> Predicate<T> anyOf(List<? extends Predicate<? super T>> conditions) {
        return switch (conditions.size()) {
            case 0 -> Util.anyOf();
            case 1 -> Util.anyOf(conditions.get(0));
            case 2 -> Util.anyOf(conditions.get(0), conditions.get(1));
            case 3 -> Util.anyOf(conditions.get(0), conditions.get(1), conditions.get(2));
            case 4 -> Util.anyOf(conditions.get(0), conditions.get(1), conditions.get(2), conditions.get(3));
            case 5 -> Util.anyOf(conditions.get(0), conditions.get(1), conditions.get(2), conditions.get(3), conditions.get(4));
            default -> {
                Predicate[] conditionsCopy = (Predicate[])conditions.toArray(Predicate[]::new);
                yield Util.anyOf(conditionsCopy);
            }
        };
    }

    public static <T> boolean isSymmetrical(int width, int height, List<T> ingredients) {
        if (width == 1) {
            return true;
        }
        int centerX = width / 2;
        for (int y = 0; y < height; ++y) {
            for (int leftX = 0; leftX < centerX; ++leftX) {
                T right;
                int rightX = width - 1 - leftX;
                T left = ingredients.get(leftX + y * width);
                if (left.equals(right = ingredients.get(rightX + y * width))) continue;
                return false;
            }
        }
        return true;
    }

    public static int growByHalf(int currentSize, int minimalNewSize) {
        return (int)Math.max(Math.min((long)currentSize + (long)(currentSize >> 1), 0x7FFFFFF7L), (long)minimalNewSize);
    }

    @SuppressForbidden(reason="Intentional use of default locale for user-visible date")
    public static DateTimeFormatter localizedDateFormatter(FormatStyle formatStyle) {
        return DateTimeFormatter.ofLocalizedDateTime(formatStyle);
    }

    public static ThreadInfo[] dumpThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.dumpAllThreads(threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported());
    }

    public static OS getPlatform() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return OS.WINDOWS;
        }
        if (osName.contains("mac")) {
            return OS.OSX;
        }
        if (osName.contains("solaris")) {
            return OS.SOLARIS;
        }
        if (osName.contains("sunos")) {
            return OS.SOLARIS;
        }
        if (osName.contains("linux")) {
            return OS.LINUX;
        }
        if (osName.contains("unix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    public static boolean isAarch64() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        return arch.equals("aarch64");
    }

    public static URI parseAndValidateUntrustedUri(String uri) throws URISyntaxException {
        URI parsedUri = new URI(uri);
        String scheme = parsedUri.getScheme();
        if (scheme == null) {
            throw new URISyntaxException(uri, "Missing protocol in URI: " + uri);
        }
        String protocol = scheme.toLowerCase(Locale.ROOT);
        if (!ALLOWED_UNTRUSTED_LINK_PROTOCOLS.contains(protocol)) {
            throw new URISyntaxException(uri, "Unsupported protocol in URI: " + uri);
        }
        return parsedUri;
    }

    public static <T> T findNextInIterable(Iterable<T> collection, @Nullable T current) {
        Iterator<T> iterator = collection.iterator();
        T first = iterator.next();
        if (current != null) {
            T property = first;
            while (true) {
                if (property == current) {
                    if (!iterator.hasNext()) break;
                    return iterator.next();
                }
                if (!iterator.hasNext()) continue;
                property = iterator.next();
            }
        }
        return first;
    }

    public static <T> T findPreviousInIterable(Iterable<T> collection, @Nullable T current) {
        Iterator<T> iterator = collection.iterator();
        T last = null;
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next == current) {
                if (last != null) break;
                last = (T)(iterator.hasNext() ? Iterators.getLast(iterator) : current);
                break;
            }
            last = next;
        }
        return last;
    }

    public static <T> T make(Supplier<T> factory) {
        return factory.get();
    }

    public static <T> T make(T t, Consumer<? super T> consumer) {
        consumer.accept(t);
        return t;
    }

    public static <K extends Enum<K>, V> Map<K, V> makeEnumMap(Class<K> keyType, java.util.function.Function<K, V> function) {
        EnumMap<Enum, V> map = new EnumMap<Enum, V>(keyType);
        for (Enum key : (Enum[])keyType.getEnumConstants()) {
            map.put(key, function.apply(key));
        }
        return map;
    }

    public static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> map, java.util.function.Function<? super V1, V2> valueMapper) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> valueMapper.apply((Object)e.getValue())));
    }

    public static <K, V1, V2> Map<K, V2> mapValuesLazy(Map<K, V1> map, Function<V1, V2> valueMapper) {
        return Maps.transformValues(map, valueMapper);
    }

    public static <T extends Enum<T>> Set<T> allOfEnumExcept(T value) {
        return EnumSet.complementOf(EnumSet.of(value));
    }

    public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> futures) {
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        if (futures.size() == 1) {
            return futures.getFirst().thenApply(ObjectLists::singleton);
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return all.thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> futures) {
        CompletableFuture failureFuture = new CompletableFuture();
        return Util.fallibleSequence(futures, failureFuture::completeExceptionally).applyToEither((CompletionStage)failureFuture, java.util.function.Function.identity());
    }

    public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<? extends CompletableFuture<? extends V>> futures) {
        CompletableFuture failureFuture = new CompletableFuture();
        return Util.fallibleSequence(futures, exception -> {
            if (failureFuture.completeExceptionally((Throwable)exception)) {
                for (CompletableFuture future : futures) {
                    future.cancel(true);
                }
            }
        }).applyToEither((CompletionStage)failureFuture, java.util.function.Function.identity());
    }

    private static <V> CompletableFuture<List<V>> fallibleSequence(List<? extends CompletableFuture<? extends V>> futures, Consumer<Throwable> failureHandler) {
        ObjectArrayList results = new ObjectArrayList();
        results.size(futures.size());
        CompletableFuture[] decoratedFutures = new CompletableFuture[futures.size()];
        for (int i = 0; i < futures.size(); ++i) {
            int index = i;
            decoratedFutures[i] = futures.get(i).whenComplete((result, exception) -> {
                if (exception != null) {
                    failureHandler.accept((Throwable)exception);
                } else {
                    results.set(index, result);
                }
            });
        }
        return CompletableFuture.allOf(decoratedFutures).thenApply(nothing -> results);
    }

    public static <T> Optional<T> ifElse(Optional<T> input, Consumer<T> onTrue, Runnable onFalse) {
        if (input.isPresent()) {
            onTrue.accept(input.get());
        } else {
            onFalse.run();
        }
        return input;
    }

    public static <T> Supplier<T> name(final Supplier<T> task, Supplier<String> nameGetter) {
        if (SharedConstants.DEBUG_NAMED_RUNNABLES) {
            final String name = nameGetter.get();
            return new Supplier<T>(){

                @Override
                public T get() {
                    return task.get();
                }

                public String toString() {
                    return name;
                }
            };
        }
        return task;
    }

    public static Runnable name(final Runnable task, Supplier<String> nameGetter) {
        if (SharedConstants.DEBUG_NAMED_RUNNABLES) {
            final String name = nameGetter.get();
            return new Runnable(){

                @Override
                public void run() {
                    task.run();
                }

                public String toString() {
                    return name;
                }
            };
        }
        return task;
    }

    public static void logAndPauseIfInIde(String message) {
        LOGGER.error(message);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            Util.doPause(message);
        }
    }

    public static void logAndPauseIfInIde(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            Util.doPause(message);
        }
    }

    public static <T extends Throwable> T pauseInIde(T t) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            LOGGER.error("Trying to throw a fatal exception, pausing in IDE", t);
            Util.doPause(t.getMessage());
        }
        return t;
    }

    public static void setPause(Consumer<String> pauseFunction) {
        thePauser = pauseFunction;
    }

    private static void doPause(String message) {
        boolean dontBotherWithPause;
        Instant preLog = Instant.now();
        LOGGER.warn("Did you remember to set a breakpoint here?");
        boolean bl = dontBotherWithPause = Duration.between(preLog, Instant.now()).toMillis() > 500L;
        if (!dontBotherWithPause) {
            thePauser.accept(message);
        }
    }

    public static String describeError(Throwable err) {
        if (err.getCause() != null) {
            return Util.describeError(err.getCause());
        }
        if (err.getMessage() != null) {
            return err.getMessage();
        }
        return err.toString();
    }

    public static <T> T getRandom(T[] array, RandomSource random) {
        return array[random.nextInt(array.length)];
    }

    public static int getRandom(int[] array, RandomSource random) {
        return array[random.nextInt(array.length)];
    }

    public static <T> T getRandom(List<T> list, RandomSource random) {
        return list.get(random.nextInt(list.size()));
    }

    public static <T> Optional<T> getRandomSafe(List<T> list, RandomSource random) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Util.getRandom(list, random));
    }

    private static BooleanSupplier createRenamer(final Path from, final Path to, final CopyOption ... options) {
        return new BooleanSupplier(){

            @Override
            public boolean getAsBoolean() {
                try {
                    Files.move(from, to, options);
                    return true;
                }
                catch (IOException e) {
                    LOGGER.error("Failed to rename", (Throwable)e);
                    return false;
                }
            }

            public String toString() {
                return "rename " + String.valueOf(from) + " to " + String.valueOf(to);
            }
        };
    }

    private static BooleanSupplier createDeleter(final Path target) {
        return new BooleanSupplier(){

            @Override
            public boolean getAsBoolean() {
                try {
                    Files.deleteIfExists(target);
                    return true;
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to delete", (Throwable)e);
                    return false;
                }
            }

            public String toString() {
                return "delete old " + String.valueOf(target);
            }
        };
    }

    private static BooleanSupplier createFileDeletedCheck(final Path target) {
        return new BooleanSupplier(){

            @Override
            public boolean getAsBoolean() {
                return !Files.exists(target, new LinkOption[0]);
            }

            public String toString() {
                return "verify that " + String.valueOf(target) + " is deleted";
            }
        };
    }

    private static BooleanSupplier createFileCreatedCheck(final Path target) {
        return new BooleanSupplier(){

            @Override
            public boolean getAsBoolean() {
                return Files.isRegularFile(target, new LinkOption[0]);
            }

            public String toString() {
                return "verify that " + String.valueOf(target) + " is present";
            }
        };
    }

    private static boolean executeInSequence(BooleanSupplier ... operations) {
        for (BooleanSupplier operation : operations) {
            if (operation.getAsBoolean()) continue;
            LOGGER.warn("Failed to execute {}", (Object)operation);
            return false;
        }
        return true;
    }

    private static boolean runWithRetries(int numberOfRetries, String description, BooleanSupplier ... operations) {
        for (int retry = 0; retry < numberOfRetries; ++retry) {
            if (Util.executeInSequence(operations)) {
                return true;
            }
            LOGGER.error("Failed to {}, retrying {}/{}", new Object[]{description, retry, numberOfRetries});
        }
        LOGGER.error("Failed to {}, aborting, progress might be lost", (Object)description);
        return false;
    }

    public static boolean safeMoveFile(Path fromPath, Path toPath, CopyOption ... options) {
        return Util.runWithRetries(10, "move from  " + String.valueOf(fromPath) + " to " + String.valueOf(toPath), Util.createRenamer(fromPath, toPath, options), Util.createFileCreatedCheck(toPath));
    }

    public static void safeReplaceFile(Path targetPath, Path newPath, Path backupPath) {
        Util.safeReplaceOrMoveFile(targetPath, newPath, backupPath, false);
    }

    public static boolean safeReplaceOrMoveFile(Path targetPath, Path newPath, Path backupPath, boolean noRollback) {
        if (Files.exists(targetPath, new LinkOption[0]) && !Util.runWithRetries(10, "create backup " + String.valueOf(backupPath), Util.createDeleter(backupPath), Util.createRenamer(targetPath, backupPath, new CopyOption[0]), Util.createFileCreatedCheck(backupPath))) {
            return false;
        }
        if (!Util.runWithRetries(10, "remove old " + String.valueOf(targetPath), Util.createDeleter(targetPath), Util.createFileDeletedCheck(targetPath))) {
            return false;
        }
        if (!Util.runWithRetries(10, "replace " + String.valueOf(targetPath) + " with " + String.valueOf(newPath), Util.createRenamer(newPath, targetPath, new CopyOption[0]), Util.createFileCreatedCheck(targetPath)) && !noRollback) {
            Util.runWithRetries(10, "restore " + String.valueOf(targetPath) + " from " + String.valueOf(backupPath), Util.createRenamer(backupPath, targetPath, new CopyOption[0]), Util.createFileCreatedCheck(targetPath));
            return false;
        }
        return true;
    }

    public static int offsetByCodepoints(String input, int pos, int offset) {
        int length = input.length();
        if (offset >= 0) {
            for (int i = 0; pos < length && i < offset; ++i) {
                if (!Character.isHighSurrogate(input.charAt(pos++)) || pos >= length || !Character.isLowSurrogate(input.charAt(pos))) continue;
                ++pos;
            }
        } else {
            for (int i = offset; pos > 0 && i < 0; ++i) {
                if (!Character.isLowSurrogate(input.charAt(--pos)) || pos <= 0 || !Character.isHighSurrogate(input.charAt(pos - 1))) continue;
                --pos;
            }
        }
        return pos;
    }

    public static Consumer<String> prefix(String prefix, Consumer<String> consumer) {
        return s -> consumer.accept(prefix + s);
    }

    public static DataResult<int[]> fixedSize(IntStream stream, int size) {
        int[] ints = stream.limit(size + 1).toArray();
        if (ints.length != size) {
            Supplier<String> message = () -> "Input is not a list of " + size + " ints";
            if (ints.length >= size) {
                return DataResult.error(message, (Object)Arrays.copyOf(ints, size));
            }
            return DataResult.error(message);
        }
        return DataResult.success((Object)ints);
    }

    public static DataResult<long[]> fixedSize(LongStream stream, int size) {
        long[] longs = stream.limit(size + 1).toArray();
        if (longs.length != size) {
            Supplier<String> message = () -> "Input is not a list of " + size + " longs";
            if (longs.length >= size) {
                return DataResult.error(message, (Object)Arrays.copyOf(longs, size));
            }
            return DataResult.error(message);
        }
        return DataResult.success((Object)longs);
    }

    public static <T> DataResult<List<T>> fixedSize(List<T> list, int size) {
        if (list.size() != size) {
            Supplier<String> message = () -> "Input is not a list of " + size + " elements";
            if (list.size() >= size) {
                return DataResult.error(message, list.subList(0, size));
            }
            return DataResult.error(message);
        }
        return DataResult.success(list);
    }

    public static void startTimerHackThread() {
        Thread timerThread = new Thread("Timer hack thread"){

            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(Integer.MAX_VALUE);
                    }
                }
                catch (InterruptedException e) {
                    LOGGER.warn("Timer hack thread interrupted, that really should not happen");
                    return;
                }
            }
        };
        timerThread.setDaemon(true);
        timerThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        timerThread.start();
    }

    public static void copyBetweenDirs(Path sourceDir, Path targetDir, Path sourcePath) throws IOException {
        Path relative = sourceDir.relativize(sourcePath);
        Path target = targetDir.resolve(relative);
        Files.copy(sourcePath, target, new CopyOption[0]);
    }

    public static String sanitizeName(String value, CharPredicate isAllowedChar) {
        return value.toLowerCase(Locale.ROOT).chars().mapToObj(c -> isAllowedChar.test((char)c) ? Character.toString((char)c) : "_").collect(Collectors.joining());
    }

    public static <K, V> SingleKeyCache<K, V> singleKeyCache(java.util.function.Function<K, V> computeValueFunction) {
        return new SingleKeyCache<K, V>(computeValueFunction);
    }

    public static <T, R> java.util.function.Function<T, R> memoize(final java.util.function.Function<T, R> function) {
        return new java.util.function.Function<T, R>(){
            private final Map<T, R> cache = new ConcurrentHashMap();

            @Override
            public R apply(T arg) {
                return this.cache.computeIfAbsent(arg, function);
            }

            public String toString() {
                return "memoize/1[function=" + String.valueOf(function) + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> memoize(final BiFunction<T, U, R> function) {
        return new BiFunction<T, U, R>(){
            private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap();

            @Override
            public R apply(T a, U b) {
                return this.cache.computeIfAbsent(Pair.of(a, b), args -> function.apply(args.getFirst(), args.getSecond()));
            }

            public String toString() {
                return "memoize/2[function=" + String.valueOf(function) + ", size=" + this.cache.size() + "]";
            }
        };
    }

    public static <T> List<T> toShuffledList(Stream<T> stream, RandomSource random) {
        ObjectArrayList result = (ObjectArrayList)stream.collect(ObjectArrayList.toList());
        Util.shuffle(result, random);
        return result;
    }

    public static IntArrayList toShuffledList(IntStream stream, RandomSource random) {
        int size;
        IntArrayList result = IntArrayList.wrap((int[])stream.toArray());
        for (int i = size = result.size(); i > 1; --i) {
            int swapTo = random.nextInt(i);
            result.set(i - 1, result.set(swapTo, result.getInt(i - 1)));
        }
        return result;
    }

    public static <T> List<T> shuffledCopy(T[] array, RandomSource random) {
        ObjectArrayList copy = new ObjectArrayList((Object[])array);
        Util.shuffle(copy, random);
        return copy;
    }

    public static <T> List<T> shuffledCopy(ObjectArrayList<T> list, RandomSource random) {
        ObjectArrayList copy = new ObjectArrayList(list);
        Util.shuffle(copy, random);
        return copy;
    }

    public static <T> void shuffle(List<T> list, RandomSource random) {
        int size;
        for (int i = size = list.size(); i > 1; --i) {
            int swapTo = random.nextInt(i);
            list.set(i - 1, list.set(swapTo, list.get(i - 1)));
        }
    }

    public static <T> CompletableFuture<T> blockUntilDone(java.util.function.Function<Executor, CompletableFuture<T>> task) {
        return Util.blockUntilDone(task, CompletableFuture::isDone);
    }

    public static <T> T blockUntilDone(java.util.function.Function<Executor, T> task, Predicate<T> completionCheck) {
        int remainingSize;
        LinkedBlockingQueue tasks = new LinkedBlockingQueue();
        T result = task.apply(tasks::add);
        while (!completionCheck.test(result)) {
            try {
                Runnable runnable = (Runnable)tasks.poll(100L, TimeUnit.MILLISECONDS);
                if (runnable == null) continue;
                runnable.run();
            }
            catch (InterruptedException e) {
                LOGGER.warn("Interrupted wait");
                break;
            }
        }
        if ((remainingSize = tasks.size()) > 0) {
            LOGGER.warn("Tasks left in queue: {}", (Object)remainingSize);
        }
        return result;
    }

    public static <T> ToIntFunction<T> createIndexLookup(List<T> values) {
        int size = values.size();
        if (size < 8) {
            return values::indexOf;
        }
        Object2IntOpenHashMap lookup = new Object2IntOpenHashMap(size);
        lookup.defaultReturnValue(-1);
        for (int i = 0; i < size; ++i) {
            lookup.put(values.get(i), i);
        }
        return lookup;
    }

    public static <T> ToIntFunction<T> createIndexIdentityLookup(List<T> values) {
        int size = values.size();
        if (size < 8) {
            ReferenceImmutableList referenceLookup = new ReferenceImmutableList(values);
            return arg_0 -> ((ReferenceList)referenceLookup).indexOf(arg_0);
        }
        Reference2IntOpenHashMap lookup = new Reference2IntOpenHashMap(size);
        lookup.defaultReturnValue(-1);
        for (int i = 0; i < size; ++i) {
            lookup.put(values.get(i), i);
        }
        return lookup;
    }

    public static <A, B> Typed<B> writeAndReadTypedOrThrow(Typed<A> typed, Type<B> newType, UnaryOperator<Dynamic<?>> function) {
        Dynamic dynamic = (Dynamic)typed.write().getOrThrow();
        return Util.readTypedOrThrow(newType, (Dynamic)function.apply(dynamic), true);
    }

    public static <T> Typed<T> readTypedOrThrow(Type<T> type, Dynamic<?> dynamic) {
        return Util.readTypedOrThrow(type, dynamic, false);
    }

    public static <T> Typed<T> readTypedOrThrow(Type<T> type, Dynamic<?> dynamic, boolean acceptPartial) {
        DataResult result = type.readTyped(dynamic).map(Pair::getFirst);
        try {
            if (acceptPartial) {
                return (Typed)result.getPartialOrThrow(IllegalStateException::new);
            }
            return (Typed)result.getOrThrow(IllegalStateException::new);
        }
        catch (IllegalStateException e) {
            CrashReport report = CrashReport.forThrowable(e, "Reading type");
            CrashReportCategory category = report.addCategory("Info");
            category.setDetail("Data", dynamic);
            category.setDetail("Type", type);
            throw new ReportedException(report);
        }
    }

    public static <T> List<T> copyAndAdd(List<T> list, T element) {
        return ImmutableList.builderWithExpectedSize((int)(list.size() + 1)).addAll(list).add(element).build();
    }

    public static <T> List<T> copyAndAdd(List<T> list, T ... elements) {
        return ImmutableList.builderWithExpectedSize((int)(list.size() + elements.length)).addAll(list).add((Object[])elements).build();
    }

    public static <T> List<T> copyAndAdd(T element, List<T> list) {
        return ImmutableList.builderWithExpectedSize((int)(list.size() + 1)).add(element).addAll(list).build();
    }

    public static <T> List<T> join(List<T> first, List<T> second) {
        ImmutableList.Builder builder = ImmutableList.builderWithExpectedSize((int)(first.size() + second.size()));
        builder.addAll(first);
        builder.addAll(second);
        return builder.build();
    }

    public static <T> List<T> join(List<T> ... lists) {
        int size = 0;
        for (List<T> list : lists) {
            size += list.size();
        }
        ImmutableList.Builder builder = ImmutableList.builderWithExpectedSize((int)size);
        for (List<T> list : lists) {
            builder.addAll(list);
        }
        return builder.build();
    }

    public static <K, V> Map<K, V> copyAndPut(Map<K, V> map, K key, V value) {
        return ImmutableMap.builderWithExpectedSize((int)(map.size() + 1)).putAll(map).put(key, value).buildKeepingLast();
    }

    public static enum OS {
        LINUX("linux"),
        SOLARIS("solaris"),
        WINDOWS("windows"){

            @Override
            protected String[] getOpenUriArguments(URI uri) {
                return new String[]{"rundll32", "url.dll,FileProtocolHandler", uri.toString()};
            }
        }
        ,
        OSX("mac"){

            @Override
            protected String[] getOpenUriArguments(URI uri) {
                return new String[]{"open", uri.toString()};
            }
        }
        ,
        UNKNOWN("unknown");

        private final String telemetryName;

        private OS(String telemetryName) {
            this.telemetryName = telemetryName;
        }

        public void openUri(URI uri) {
            try {
                Process process = Runtime.getRuntime().exec(this.getOpenUriArguments(uri));
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
            }
            catch (IOException e) {
                LOGGER.error("Couldn't open location '{}'", (Object)uri, (Object)e);
            }
        }

        public void openFile(File file) {
            this.openUri(file.toURI());
        }

        public void openPath(Path path) {
            this.openUri(path.toUri());
        }

        protected String[] getOpenUriArguments(URI uri) {
            String string = uri.toString();
            if ("file".equals(uri.getScheme())) {
                string = string.replace("file:", "file://");
            }
            return new String[]{"xdg-open", string};
        }

        public void openUri(String uri) {
            try {
                this.openUri(new URI(uri));
            }
            catch (IllegalArgumentException | URISyntaxException e) {
                LOGGER.error("Couldn't open uri '{}'", (Object)uri, (Object)e);
            }
        }

        public String telemetryName() {
            return this.telemetryName;
        }
    }
}

