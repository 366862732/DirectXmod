/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.common.io.Files
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Lifecycle
 *  org.apache.commons.io.function.IORunnable
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.FileUtil;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.FileNameDateFormatter;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.minecraft.world.level.validation.PathAllowList;
import org.apache.commons.io.function.IORunnable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LevelStorageSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String TAG_DATA = "Data";
    private static final PathMatcher NO_SYMLINKS_ALLOWED = path -> false;
    public static final String ALLOWED_SYMLINKS_CONFIG_NAME = "allowed_symlinks.txt";
    private static final int DISK_SPACE_WARNING_THRESHOLD = 0x4000000;
    private static final Component LOAD_FOLDER_ACCESS_MESSAGE = Component.translatable("selectWorld.load_folder_access");
    private final Path baseDir;
    private final Path backupDir;
    private final DataFixer fixerUpper;
    private final DirectoryValidator worldDirValidator;

    public LevelStorageSource(Path baseDir, Path backupDir, DirectoryValidator worldDirValidator, DataFixer fixerUpper) {
        this.fixerUpper = fixerUpper;
        try {
            FileUtil.createDirectoriesSafe(baseDir);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.baseDir = baseDir;
        this.backupDir = backupDir;
        this.worldDirValidator = worldDirValidator;
    }

    public static DirectoryValidator parseValidator(Path configPath) {
        if (Files.exists(configPath, new LinkOption[0])) {
            DirectoryValidator directoryValidator;
            block9: {
                BufferedReader reader = Files.newBufferedReader(configPath);
                try {
                    directoryValidator = new DirectoryValidator(PathAllowList.readPlain(reader));
                    if (reader == null) break block9;
                }
                catch (Throwable throwable) {
                    try {
                        if (reader != null) {
                            try {
                                reader.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (Exception e) {
                        LOGGER.error("Failed to parse {}, disallowing all symbolic links", (Object)ALLOWED_SYMLINKS_CONFIG_NAME, (Object)e);
                    }
                }
                reader.close();
            }
            return directoryValidator;
        }
        return new DirectoryValidator(NO_SYMLINKS_ALLOWED);
    }

    public static LevelStorageSource createDefault(Path path) {
        DirectoryValidator validator = LevelStorageSource.parseValidator(path.resolve(ALLOWED_SYMLINKS_CONFIG_NAME));
        return new LevelStorageSource(path, path.resolve("../backups"), validator, DataFixers.getDataFixer());
    }

    public static WorldDataConfiguration readDataConfig(Dynamic<?> levelData) {
        return WorldDataConfiguration.CODEC.parse(levelData).resultOrPartial(arg_0 -> ((Logger)LOGGER).error(arg_0)).orElse(WorldDataConfiguration.DEFAULT);
    }

    public static WorldLoader.PackConfig getPackConfig(Dynamic<?> levelDataTag, PackRepository packRepository, boolean safeMode) {
        return new WorldLoader.PackConfig(packRepository, LevelStorageSource.readDataConfig(levelDataTag), safeMode, false);
    }

    public static LevelDataAndDimensions getLevelDataAndDimensions(LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, WorldDataConfiguration dataConfiguration, Registry<LevelStem> datapackDimensions, HolderLookup.Provider registryAccess) {
        if (DataFixers.getFileFixer().requiresFileFixing(NbtUtils.getDataVersion(levelDataTag))) {
            throw new IllegalStateException("Cannot get level data without file fixing first");
        }
        Dynamic<?> dataTag = RegistryOps.injectRegistryContext(levelDataTag, registryAccess);
        WorldGenSettings worldGenSettings = (WorldGenSettings)LevelStorageSource.readExistingSavedData(worldAccess, registryAccess, WorldGenSettings.TYPE).mapOrElse(Function.identity(), error -> {
            LOGGER.error("Unable to read or access the world gen settings file! Falling back to the default settings with a random world seed. {}", (Object)error.message());
            return new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), new WorldDimensions(datapackDimensions));
        });
        LevelSettings settings = LevelSettings.parse(dataTag, dataConfiguration);
        WorldDimensions.Complete dimensions = worldGenSettings.dimensions().bake(datapackDimensions);
        Lifecycle lifecycle = dimensions.lifecycle().add(registryAccess.allRegistriesLifecycle());
        PrimaryLevelData worldData = PrimaryLevelData.parse(dataTag, settings, dimensions.specialWorldProperty(), lifecycle);
        return LevelDataAndDimensions.create(worldData, worldGenSettings, dimensions);
    }

    public static <T extends SavedData> DataResult<T> readExistingSavedData(LevelStorageAccess access, HolderLookup.Provider registryAccess, SavedDataType<T> savedDataType) {
        CompoundTag fileContents;
        Path dataLocation = savedDataType.id().withSuffix(".dat").resolveAgainst(access.getLevelPath(LevelResource.DATA));
        try {
            fileContents = NbtIo.readCompressed(dataLocation, NbtAccounter.unlimitedHeap());
        }
        catch (IOException e) {
            return DataResult.error(e::getMessage);
        }
        return savedDataType.codec().parse(RegistryOps.create(NbtOps.INSTANCE, registryAccess), (Object)fileContents.getCompoundOrEmpty("data"));
    }

    public static void writeGameRules(WorldData worldData, Path worldFolder, GameRules gameRules) throws IOException {
        Codec<GameRules> codec = GameRules.codec(worldData.enabledFeatures());
        LevelStorageSource.writeSavedData(worldFolder, NbtOps.INSTANCE, GameRuleMap.TYPE, codec, gameRules);
    }

    public static void writeWorldGenSettings(RegistryAccess registryAccess, Path worldFolder, WorldGenSettings worldGenSettings) throws IOException {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        LevelStorageSource.writeSavedData(worldFolder, ops, WorldGenSettings.TYPE, WorldGenSettings.CODEC, worldGenSettings);
    }

    private static <T> void writeSavedData(Path worldFolder, DynamicOps<Tag> ops, SavedDataType<?> type, Codec<T> codec, T data) throws IOException {
        Tag encoded = (Tag)codec.encodeStart(ops, data).getOrThrow();
        CompoundTag fullTag = new CompoundTag();
        fullTag.put("data", encoded);
        NbtUtils.addCurrentDataVersion(fullTag);
        Path path = type.id().withSuffix(".dat").resolveAgainst(worldFolder.resolve("data"));
        FileUtil.createDirectoriesSafe(path.getParent());
        NbtIo.writeCompressed(fullTag, path);
    }

    public String getName() {
        return "Anvil";
    }

    public LevelCandidates findLevelCandidates() throws LevelStorageException {
        LevelCandidates levelCandidates;
        block13: {
            if (!Files.exists(this.baseDir, new LinkOption[0])) {
                try {
                    Files.createDirectory(this.baseDir, new FileAttribute[0]);
                }
                catch (IOException e) {
                    throw new LevelStorageException(LOAD_FOLDER_ACCESS_MESSAGE, e);
                }
            } else if (!Files.isDirectory(this.baseDir, new LinkOption[0])) {
                throw new LevelStorageException(LOAD_FOLDER_ACCESS_MESSAGE);
            }
            Stream<Path> paths = Files.list(this.baseDir);
            try {
                List<LevelDirectory> candidates = paths.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).map(LevelDirectory::new).filter(directory -> Files.isRegularFile(directory.dataFile(), new LinkOption[0]) || Files.isRegularFile(directory.oldDataFile(), new LinkOption[0])).toList();
                levelCandidates = new LevelCandidates(candidates);
                if (paths == null) break block13;
            }
            catch (Throwable throwable) {
                try {
                    if (paths != null) {
                        try {
                            paths.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    throw new LevelStorageException(LOAD_FOLDER_ACCESS_MESSAGE);
                }
            }
            paths.close();
        }
        return levelCandidates;
    }

    public CompletableFuture<List<LevelSummary>> loadLevelSummaries(LevelCandidates candidates) {
        ArrayList<CompletableFuture<@Nullable LevelSummary>> futures = new ArrayList<CompletableFuture<LevelSummary>>(candidates.levels.size());
        for (LevelDirectory level : candidates.levels) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                boolean locked;
                try {
                    locked = DirectoryLock.isLocked(level.path());
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to read {} lock", (Object)level.path(), (Object)e);
                    return null;
                }
                try {
                    return this.readLevelSummary(level, locked);
                }
                catch (OutOfMemoryError e) {
                    MemoryReserve.release();
                    String detailedMessage = "Ran out of memory trying to read summary of world folder \"" + level.directoryName() + "\"";
                    LOGGER.error(LogUtils.FATAL_MARKER, detailedMessage);
                    OutOfMemoryError detailedException = new OutOfMemoryError("Ran out of memory reading level data");
                    detailedException.initCause(e);
                    CrashReport crashReport = CrashReport.forThrowable(detailedException, detailedMessage);
                    CrashReportCategory worldDetails = crashReport.addCategory("World details");
                    worldDetails.setDetail("Folder Name", level.directoryName());
                    try {
                        long size = Files.size(level.dataFile());
                        worldDetails.setDetail("level.dat size", size);
                    }
                    catch (IOException ex) {
                        worldDetails.setDetailError("level.dat size", ex);
                    }
                    throw new ReportedException(crashReport);
                }
            }, Util.backgroundExecutor().forName("loadLevelSummaries")));
        }
        return Util.sequenceFailFastAndCancel(futures).thenApply(levels -> levels.stream().filter(Objects::nonNull).sorted().toList());
    }

    private int getStorageVersion() {
        return 19133;
    }

    private static CompoundTag readLevelDataTagRaw(Path dataFile) throws IOException {
        return NbtIo.readCompressed(dataFile, NbtAccounter.uncompressedQuota());
    }

    private LevelSummary readLevelSummary(LevelDirectory level, boolean locked) {
        Path dataFile = level.dataFile();
        if (Files.exists(dataFile, new LinkOption[0])) {
            try {
                List<ForbiddenSymlinkInfo> issues;
                if (Files.isSymbolicLink(dataFile) && !(issues = this.worldDirValidator.validateSymlink(dataFile)).isEmpty()) {
                    LOGGER.warn("{}", (Object)ContentValidationException.getMessage(dataFile, issues));
                    return new LevelSummary.SymlinkLevelSummary(level.directoryName(), level.iconFile());
                }
                Tag result = LevelStorageSource.readLightweightData(dataFile);
                if (result instanceof CompoundTag) {
                    CompoundTag root = (CompoundTag)result;
                    CompoundTag tag = root.getCompoundOrEmpty(TAG_DATA);
                    int dataVersion = NbtUtils.getDataVersion(tag);
                    Dynamic updated = DataFixTypes.LEVEL_SUMMARY.updateToCurrentVersion(this.fixerUpper, new Dynamic((DynamicOps)NbtOps.INSTANCE, (Object)tag), dataVersion);
                    return this.makeLevelSummary(updated, level, locked, dataVersion);
                }
                LOGGER.warn("Invalid root tag in {}", (Object)dataFile);
            }
            catch (Exception e) {
                LOGGER.error("Exception reading {}", (Object)dataFile, (Object)e);
            }
        }
        return new LevelSummary.CorruptedLevelSummary(level.directoryName(), level.iconFile(), LevelStorageSource.getFileModificationTime(level));
    }

    private static long getFileModificationTime(LevelDirectory level) {
        Instant timeStamp = LevelStorageSource.getFileModificationTime(level.dataFile());
        if (timeStamp == null) {
            timeStamp = LevelStorageSource.getFileModificationTime(level.oldDataFile());
        }
        return timeStamp == null ? -1L : timeStamp.toEpochMilli();
    }

    private static @Nullable Instant getFileModificationTime(Path path) {
        try {
            return Files.getLastModifiedTime(path, new LinkOption[0]).toInstant();
        }
        catch (IOException iOException) {
            return null;
        }
    }

    private LevelSummary makeLevelSummary(Dynamic<?> dataTag, LevelDirectory levelDirectory, boolean locked, int dataVersion) {
        LevelVersion levelVersion = LevelVersion.parse(dataTag);
        int levelDataVersion = levelVersion.levelDataVersion();
        if (levelDataVersion == 19132 || levelDataVersion == 19133) {
            boolean requiresManualConversion = levelDataVersion != this.getStorageVersion();
            boolean requiresFileFixing = DataFixers.getFileFixer().requiresFileFixing(dataVersion);
            Path icon = levelDirectory.iconFile();
            WorldDataConfiguration dataConfiguration = LevelStorageSource.readDataConfig(dataTag);
            LevelSettings settings = LevelSettings.parse(dataTag, dataConfiguration);
            FeatureFlagSet enabledFeatureFlags = LevelStorageSource.parseFeatureFlagsFromSummary(dataTag);
            boolean experimental = FeatureFlags.isExperimental(enabledFeatureFlags);
            return new LevelSummary(settings, levelVersion, levelDirectory.directoryName(), requiresManualConversion, requiresFileFixing, locked, experimental, icon);
        }
        throw new NbtFormatException("Unknown data version: " + Integer.toHexString(levelDataVersion));
    }

    private static FeatureFlagSet parseFeatureFlagsFromSummary(Dynamic<?> tag) {
        Set<Identifier> enabledFlags = tag.get("enabled_features").asStream().flatMap(entry -> entry.asString().result().map(Identifier::tryParse).stream()).collect(Collectors.toSet());
        return FeatureFlags.REGISTRY.fromNames(enabledFlags, unknownId -> {});
    }

    private static @Nullable Tag readLightweightData(Path dataFile) throws IOException {
        SkipFields parser = new SkipFields(new FieldSelector(TAG_DATA, CompoundTag.TYPE, "Player"), new FieldSelector(TAG_DATA, CompoundTag.TYPE, "WorldGenSettings"));
        NbtIo.parseCompressed(dataFile, (StreamTagVisitor)parser, NbtAccounter.uncompressedQuota());
        return parser.getResult();
    }

    public boolean isNewLevelIdAcceptable(String levelId) {
        try {
            Path fullPath = this.getLevelPath(levelId);
            Files.createDirectory(fullPath, new FileAttribute[0]);
            Files.deleteIfExists(fullPath);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public boolean levelExists(String levelId) {
        try {
            return Files.isDirectory(this.getLevelPath(levelId), new LinkOption[0]);
        }
        catch (InvalidPathException e) {
            return false;
        }
    }

    public Path getLevelPath(String levelId) {
        return this.baseDir.resolve(levelId);
    }

    public Path getBaseDir() {
        return this.baseDir;
    }

    public Path getBackupPath() {
        return this.backupDir;
    }

    public LevelStorageAccess validateAndCreateAccess(String levelId) throws IOException, ContentValidationException {
        Path levelPath = this.getLevelPath(levelId);
        List<ForbiddenSymlinkInfo> validationResults = this.worldDirValidator.validateDirectory(levelPath, true);
        if (!validationResults.isEmpty()) {
            throw new ContentValidationException(levelPath, validationResults);
        }
        return new LevelStorageAccess(this, levelId, levelPath);
    }

    public LevelStorageAccess createAccess(String levelId) throws IOException {
        Path levelPath = this.getLevelPath(levelId);
        return new LevelStorageAccess(this, levelId, levelPath);
    }

    public DirectoryValidator getWorldDirValidator() {
        return this.worldDirValidator;
    }

    public class LevelStorageAccess
    implements AutoCloseable {
        private DirectoryLock lock;
        private final LevelDirectory levelDirectory;
        private final String levelId;
        private final Map<LevelResource, Path> resources;
        final /* synthetic */ LevelStorageSource this$0;

        private LevelStorageAccess(LevelStorageSource this$0, String levelId, Path path) throws IOException {
            LevelStorageSource levelStorageSource = this$0;
            Objects.requireNonNull(levelStorageSource);
            this.this$0 = levelStorageSource;
            this.resources = Maps.newHashMap();
            this.levelId = levelId;
            this.levelDirectory = new LevelDirectory(path);
            this.createLock();
        }

        private void createLock() throws IOException {
            this.lock = DirectoryLock.create(this.levelDirectory.path);
        }

        public void releaseTemporarilyAndRun(IORunnable runnable) throws IOException {
            this.close();
            try {
                runnable.run();
            }
            finally {
                this.createLock();
            }
        }

        public long estimateDiskSpace() {
            try {
                return Files.getFileStore(this.levelDirectory.path).getUsableSpace();
            }
            catch (Exception ignored) {
                return Long.MAX_VALUE;
            }
        }

        public boolean checkForLowDiskSpace() {
            return this.estimateDiskSpace() < 0x4000000L;
        }

        public void safeClose() {
            try {
                this.close();
            }
            catch (IOException e) {
                LOGGER.warn("Failed to unlock access to level {}", (Object)this.getLevelId(), (Object)e);
            }
        }

        public LevelStorageSource parent() {
            return this.this$0;
        }

        public LevelDirectory getLevelDirectory() {
            return this.levelDirectory;
        }

        public String getLevelId() {
            return this.levelId;
        }

        public Path getLevelPath(LevelResource resource) {
            return this.resources.computeIfAbsent(resource, this.levelDirectory::resourcePath);
        }

        public Path getDimensionPath(ResourceKey<Level> name) {
            return DimensionType.getStorageFolder(name, this.levelDirectory.path());
        }

        private void checkLock() {
            if (!this.lock.isValid()) {
                throw new IllegalStateException("Lock is no longer valid");
            }
        }

        public PlayerDataStorage createPlayerStorage() {
            this.checkLock();
            return new PlayerDataStorage(this, this.this$0.fixerUpper);
        }

        public void collectIssues(boolean useFallback) throws IOException {
            this.checkLock();
            Dynamic<?> unfixedDataTag = this.getUnfixedDataTag(useFallback);
            int dataVersion = NbtUtils.getDataVersion(unfixedDataTag);
            Dynamic<?> fixedDataTag = DataFixTypes.LEVEL.updateToCurrentVersion(this.this$0.fixerUpper, unfixedDataTag, dataVersion);
            this.this$0.makeLevelSummary(fixedDataTag, this.levelDirectory, false, dataVersion);
        }

        public LevelSummary fixAndGetSummary() throws IOException {
            this.checkLock();
            return this.fixAndGetSummaryFromTag(this.getUnfixedDataTag(false));
        }

        public LevelSummary fixAndGetSummaryFromTag(Dynamic<?> dataTag) {
            this.checkLock();
            int dataVersion = NbtUtils.getDataVersion(dataTag);
            Dynamic<?> dataTagFixed = DataFixTypes.LEVEL_SUMMARY.updateToCurrentVersion(this.this$0.fixerUpper, dataTag, dataVersion);
            return this.this$0.makeLevelSummary(dataTagFixed, this.levelDirectory, false, dataVersion);
        }

        public Dynamic<?> getUnfixedDataTagWithFallback() throws IOException {
            Dynamic<?> unfixedDataTag;
            try {
                unfixedDataTag = this.getUnfixedDataTag(false);
            }
            catch (IOException | NbtException | ReportedNbtException e) {
                LOGGER.warn("Failed to load world data from {}", (Object)this.levelDirectory.dataFile(), (Object)e);
                LOGGER.info("Attempting to use fallback {}", (Object)this.levelDirectory.oldDataFile());
                unfixedDataTag = this.getUnfixedDataTag(true);
                this.restoreLevelDataFromOld();
            }
            return unfixedDataTag;
        }

        public Dynamic<?> getUnfixedDataTag(boolean useFallback) throws IOException {
            this.checkLock();
            Path dataFile = this.getDataFile(useFallback);
            CompoundTag root = LevelStorageSource.readLevelDataTagRaw(dataFile);
            return new Dynamic((DynamicOps)NbtOps.INSTANCE, (Object)root.getCompoundOrEmpty(LevelStorageSource.TAG_DATA));
        }

        private Path getDataFile(boolean useFallback) {
            return useFallback ? this.levelDirectory.oldDataFile() : this.levelDirectory.dataFile();
        }

        public void saveDataTag(WorldData levelData) {
            this.saveDataTag(levelData, null);
        }

        public void saveDataTag(WorldData levelData, @Nullable UUID singleplayerUUID) {
            CompoundTag dataTag = levelData.createTag(singleplayerUUID);
            CompoundTag root = new CompoundTag();
            root.put(LevelStorageSource.TAG_DATA, dataTag);
            this.saveLevelData(root);
        }

        public void saveLevelData(Dynamic<?> tag) {
            Tag genericTag = (Tag)tag.convert((DynamicOps)NbtOps.INSTANCE).getValue();
            CompoundTag root = new CompoundTag();
            root.put(LevelStorageSource.TAG_DATA, genericTag);
            this.saveLevelData(root);
        }

        private void saveLevelData(CompoundTag root) {
            Path worldDir = this.levelDirectory.path();
            try {
                Path dataFile = Files.createTempFile(worldDir, "level", ".dat", new FileAttribute[0]);
                NbtIo.writeCompressed(root, dataFile);
                Path oldDataFile = this.levelDirectory.oldDataFile();
                Path currentFile = this.levelDirectory.dataFile();
                Util.safeReplaceFile(currentFile, dataFile, oldDataFile);
            }
            catch (Exception e) {
                LOGGER.error("Failed to save level {}", (Object)worldDir, (Object)e);
            }
        }

        public Optional<Path> getIconFile() {
            if (!this.lock.isValid()) {
                return Optional.empty();
            }
            return Optional.of(this.levelDirectory.iconFile());
        }

        public void deleteLevel() throws IOException {
            this.checkLock();
            final Path lockPath = this.levelDirectory.lockFile();
            LOGGER.info("Deleting level {}", (Object)this.levelId);
            for (int attempt = 1; attempt <= 5; ++attempt) {
                LOGGER.info("Attempt {}...", (Object)attempt);
                try {
                    Files.walkFileTree(this.levelDirectory.path(), (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
                        final /* synthetic */ LevelStorageAccess this$1;
                        {
                            LevelStorageAccess levelStorageAccess = this$1;
                            Objects.requireNonNull(levelStorageAccess);
                            this.this$1 = levelStorageAccess;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (!file.equals(lockPath)) {
                                LOGGER.debug("Deleting {}", (Object)file);
                                Files.deleteIfExists(file);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                            if (exc != null) {
                                throw exc;
                            }
                            if (dir.equals(this.this$1.levelDirectory.path())) {
                                this.this$1.lock.close();
                                Files.deleteIfExists(lockPath);
                            }
                            Files.deleteIfExists(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    break;
                }
                catch (IOException e) {
                    if (attempt < 5) {
                        LOGGER.warn("Failed to delete {}", (Object)this.levelDirectory.path(), (Object)e);
                        try {
                            Thread.sleep(500L);
                        }
                        catch (InterruptedException interruptedException) {}
                        continue;
                    }
                    throw e;
                }
            }
        }

        public void renameLevel(String newName) throws IOException {
            this.modifyLevelDataWithoutDatafix(tag -> tag.putString("LevelName", newName.trim()));
        }

        public void renameAndDropPlayer(String newName) throws IOException {
            this.modifyLevelDataWithoutDatafix(tag -> {
                tag.putString("LevelName", newName.trim());
                tag.remove("singleplayer_uuid");
            });
        }

        private void modifyLevelDataWithoutDatafix(Consumer<CompoundTag> updater) throws IOException {
            this.checkLock();
            CompoundTag root = LevelStorageSource.readLevelDataTagRaw(this.levelDirectory.dataFile());
            updater.accept(root.getCompoundOrEmpty(LevelStorageSource.TAG_DATA));
            this.saveLevelData(root);
        }

        public long makeWorldBackup() throws IOException {
            this.checkLock();
            String zipFilePrefix = FileNameDateFormatter.FORMATTER.format(ZonedDateTime.now()) + "_" + this.levelId;
            Path root = this.this$0.getBackupPath();
            try {
                FileUtil.createDirectoriesSafe(root);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            Path zipFilePath = root.resolve(FileUtil.findAvailableName(root, zipFilePrefix, ".zip"));
            try (final ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFilePath, new OpenOption[0])));){
                final Path rootPath = Paths.get(this.levelId, new String[0]);
                Files.walkFileTree(this.levelDirectory.path(), (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
                    final /* synthetic */ LevelStorageAccess this$1;
                    {
                        LevelStorageAccess levelStorageAccess = this$1;
                        Objects.requireNonNull(levelStorageAccess);
                        this.this$1 = levelStorageAccess;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        if (path.endsWith("session.lock")) {
                            return FileVisitResult.CONTINUE;
                        }
                        String entryPath = rootPath.resolve(this.this$1.levelDirectory.path().relativize(path)).toString().replace('\\', '/');
                        ZipEntry entry = new ZipEntry(entryPath);
                        stream.putNextEntry(entry);
                        com.google.common.io.Files.asByteSource((File)path.toFile()).copyTo((OutputStream)stream);
                        stream.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            return Files.size(zipFilePath);
        }

        public boolean hasWorldData() {
            return Files.exists(this.levelDirectory.dataFile(), new LinkOption[0]) || Files.exists(this.levelDirectory.oldDataFile(), new LinkOption[0]);
        }

        @Override
        public void close() throws IOException {
            this.lock.close();
        }

        public boolean restoreLevelDataFromOld() {
            return Util.safeReplaceOrMoveFile(this.levelDirectory.dataFile(), this.levelDirectory.oldDataFile(), this.levelDirectory.corruptedDataFile(ZonedDateTime.now()), true);
        }

        public @Nullable Instant getFileModificationTime(boolean fallback) {
            return LevelStorageSource.getFileModificationTime(this.getDataFile(fallback));
        }
    }

    public record LevelCandidates(List<LevelDirectory> levels) implements Iterable<LevelDirectory>
    {
        public boolean isEmpty() {
            return this.levels.isEmpty();
        }

        @Override
        public Iterator<LevelDirectory> iterator() {
            return this.levels.iterator();
        }
    }

    public record LevelDirectory(Path path) {
        public String directoryName() {
            return this.path.getFileName().toString();
        }

        public Path dataFile() {
            return this.resourcePath(LevelResource.LEVEL_DATA_FILE);
        }

        public Path oldDataFile() {
            return this.resourcePath(LevelResource.OLD_LEVEL_DATA_FILE);
        }

        public Path corruptedDataFile(ZonedDateTime time) {
            return this.path.resolve(LevelResource.LEVEL_DATA_FILE.id() + "_corrupted_" + time.format(FileNameDateFormatter.FORMATTER));
        }

        public Path rawDataFile(ZonedDateTime time) {
            return this.path.resolve(LevelResource.LEVEL_DATA_FILE.id() + "_raw_" + time.format(FileNameDateFormatter.FORMATTER));
        }

        public Path iconFile() {
            return this.resourcePath(LevelResource.ICON_FILE);
        }

        public Path lockFile() {
            return this.resourcePath(LevelResource.LOCK_FILE);
        }

        public Path resourcePath(LevelResource resource) {
            return this.path.resolve(resource.id());
        }
    }
}

