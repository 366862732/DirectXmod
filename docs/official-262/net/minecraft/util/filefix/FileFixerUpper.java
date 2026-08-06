/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.datafixers.DataFixerBuilder
 *  com.mojang.datafixers.DataFixerBuilder$Result
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.apache.commons.io.file.PathUtils
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.filefix;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FileUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.filefix.AbortedFileFixException;
import net.minecraft.util.filefix.AtomicMoveNotSupportedFileFixException;
import net.minecraft.util.filefix.FailedCleanupFileFixException;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.FileFixException;
import net.minecraft.util.filefix.FileSystemCapabilities;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileSystem;
import net.minecraft.util.filefix.virtualfilesystem.FileMove;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FileFixerUpper {
    private static final int FILE_FIXER_INTRODUCTION_VERSION = 4772;
    private static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
    private static final String FILE_FIX_DIRECTORY_NAME = "filefix";
    private static final String NEW_WORLD_TEMP_NAME = "new_world";
    private static final String UPGRADE_IN_PROGRESS_NAME = "upgrade_in_progress.json";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DataFixerBuilder.Result dataFixer;
    private final List<FileFix> fileFixes;
    private final int latestFileFixerVersion;

    public FileFixerUpper(DataFixerBuilder.Result dataFixer, List<FileFix> fileFixes, int latestFileFixerVersion) {
        this.dataFixer = dataFixer;
        this.fileFixes = List.copyOf(fileFixes);
        this.latestFileFixerVersion = latestFileFixerVersion;
    }

    public static int worldVersionToFileFixerVersion(int levelDataVersion) {
        if (levelDataVersion < 4772) {
            return 0;
        }
        return levelDataVersion;
    }

    public boolean requiresFileFixing(int levelDataVersion) {
        return FileFixerUpper.worldVersionToFileFixerVersion(levelDataVersion) < this.latestFileFixerVersion;
    }

    public Dynamic<?> fix(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, UpgradeProgress upgradeProgress) throws FileFixException {
        return this.fix(worldAccess, levelDataTag, upgradeProgress, SharedConstants.getCurrentVersion().dataVersion().version());
    }

    @VisibleForTesting
    public Dynamic<?> fix(LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, UpgradeProgress upgradeProgress, int toVersion) throws FileFixException {
        int loadedVersion = NbtUtils.getDataVersion(levelDataTag);
        if (this.requiresFileFixing(loadedVersion)) {
            List<FileMove> moves;
            LOGGER.info("Starting upgrade for world \"{}\"", (Object)worldAccess.getLevelId());
            Path worldFolder = worldAccess.getLevelDirectory().path();
            Path fileFixDirectory = worldFolder.resolve(FILE_FIX_DIRECTORY_NAME);
            Path tempWorld = fileFixDirectory.resolve(NEW_WORLD_TEMP_NAME);
            try {
                moves = this.startOrContinueFileFixing(upgradeProgress, toVersion, worldFolder, tempWorld, fileFixDirectory, loadedVersion);
            }
            catch (IOException e) {
                throw new AbortedFileFixException(e);
            }
            try {
                FileFixerUpper.swapInFixedWorld(worldAccess, moves, fileFixDirectory, tempWorld);
            }
            catch (AbortedFileFixException e) {
                if (e.notRevertedMoves().isEmpty()) {
                    FileFixerUpper.cleanup(fileFixDirectory);
                }
                throw e;
            }
            try {
                levelDataTag = worldAccess.getUnfixedDataTag(false);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            loadedVersion = NbtUtils.getDataVersion(levelDataTag);
        }
        Dynamic<?> fixedLevelDataTag = DataFixTypes.LEVEL.updateToCurrentVersion(this.dataFixer.fixer(), levelDataTag, loadedVersion);
        return FileFixerUpper.addVersionsToLevelData(fixedLevelDataTag, toVersion);
    }

    private List<FileMove> startOrContinueFileFixing(UpgradeProgress upgradeProgress, int toVersion, Path worldFolder, Path tempWorld, Path fileFixDirectory, int loadedVersion) throws IOException {
        List<FileMove> moves;
        Path upgradeInProgressFile = fileFixDirectory.resolve(UPGRADE_IN_PROGRESS_NAME);
        if (Files.exists(upgradeInProgressFile, new LinkOption[0])) {
            LOGGER.warn("Found previously interrupted world upgrade, attempting to continue it");
            moves = FileFixerUpper.readMoves(worldFolder, tempWorld, upgradeInProgressFile);
        } else {
            if (Files.exists(fileFixDirectory, new LinkOption[0])) {
                FileFixerUpper.deleteDirectory(fileFixDirectory);
            }
            try {
                Files.createDirectory(fileFixDirectory, new FileAttribute[0]);
                moves = this.applyFileFixersOnCow(upgradeProgress, loadedVersion, toVersion, worldFolder, fileFixDirectory, tempWorld);
            }
            catch (Exception e) {
                FileFixerUpper.cleanup(fileFixDirectory);
                throw e;
            }
        }
        return moves;
    }

    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult visitFile(Path realPath, BasicFileAttributes attrs) {
                try {
                    Files.deleteIfExists(realPath);
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path realPath, @Nullable IOException e) {
                try {
                    Files.deleteIfExists(realPath);
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (Files.exists(directory, new LinkOption[0])) {
            PathUtils.deleteDirectory((Path)directory);
        }
    }

    private static void cleanup(Path fileFixDirectory) {
        try {
            FileFixerUpper.deleteDirectory(fileFixDirectory);
        }
        catch (Exception ex) {
            LOGGER.error("Failed to clean up", (Throwable)ex);
        }
    }

    private List<FileMove> applyFileFixersOnCow(UpgradeProgress upgradeProgress, int loadedVersion, int toVersion, Path worldFolder, Path fileFixDirectory, Path tempWorld) throws IOException {
        try (CopyOnWriteFileSystem fs = CopyOnWriteFileSystem.create(worldFolder.getFileName().toString(), worldFolder, fileFixDirectory.resolve("cow"), fileFixDirectory::equals);){
            this.applyFileFixers(upgradeProgress, loadedVersion, toVersion, fs.rootPath());
            CopyOnWriteFileSystem.Moves moves = fs.collectMoveOperations(tempWorld);
            Files.createDirectory(tempWorld, new FileAttribute[0]);
            CopyOnWriteFileSystem.createDirectories(moves.directories());
            CopyOnWriteFileSystem.moveFiles(moves.copiedFiles());
            List<FileMove> list = moves.preexistingFiles();
            return list;
        }
    }

    @VisibleForTesting
    public void applyFileFixers(UpgradeProgress upgradeProgress, int loadedVersion, int toVersion, Path basePath) throws IOException {
        List<FileFix> applicableFixers = this.getApplicableFixers(loadedVersion, toVersion);
        upgradeProgress.setType(UpgradeProgress.Type.FILES);
        this.countFiles(applicableFixers, upgradeProgress);
        upgradeProgress.setStatus(UpgradeProgress.Status.UPGRADING);
        upgradeProgress.setApplicableFixerAmount(applicableFixers.size());
        for (FileFix fileFix : applicableFixers) {
            upgradeProgress.incrementRunningFileFixer();
            fileFix.runFixOperations(basePath, upgradeProgress);
        }
        this.writeUpdatedLevelData(basePath, toVersion);
        Files.deleteIfExists(basePath.resolve("level.dat_old"));
        Files.deleteIfExists(basePath.resolve("session.lock"));
    }

    private List<FileFix> getApplicableFixers(int fromVersion, int toVersion) {
        int fileFixerFromVersion = FileFixerUpper.worldVersionToFileFixerVersion(fromVersion);
        return this.fileFixes.stream().filter(fileFix -> fileFix.getVersion() > fileFixerFromVersion && fileFix.getVersion() <= toVersion).toList();
    }

    private void countFiles(List<FileFix> applicableFixers, UpgradeProgress upgradeProgress) {
        upgradeProgress.setStatus(UpgradeProgress.Status.COUNTING);
        int totalFiles = 0;
        for (FileFix fileFix : applicableFixers) {
            totalFiles += fileFix.countFileOperations();
        }
        upgradeProgress.addTotalFileFixOperations(totalFiles);
    }

    private void writeUpdatedLevelData(Path worldFolder, int toVersion) throws IOException {
        Path levelDatPath = worldFolder.resolve(LevelResource.LEVEL_DATA_FILE.id());
        CompoundTag unfixedLevelDat = NbtIo.readCompressed(levelDatPath, NbtAccounter.defaultQuota());
        CompoundTag unfixedDataTag = unfixedLevelDat.getCompoundOrEmpty("Data");
        int dataVersion = NbtUtils.getDataVersion(unfixedDataTag);
        Object fixed = DataFixTypes.LEVEL.update(this.dataFixer.fixer(), new Dynamic((DynamicOps)NbtOps.INSTANCE, (Object)unfixedDataTag), dataVersion, toVersion);
        fixed = FileFixerUpper.addVersionsToLevelData(fixed, toVersion);
        Dynamic dynamic = fixed.emptyMap().set("Data", fixed);
        NbtIo.writeCompressed((CompoundTag)dynamic.convert((DynamicOps)NbtOps.INSTANCE).getValue(), levelDatPath);
    }

    private static Dynamic<?> addVersionsToLevelData(Dynamic<?> fixed, int toVersion) {
        fixed = NbtUtils.addDataVersion(fixed, toVersion);
        fixed = PrimaryLevelData.writeLastPlayed(fixed);
        fixed = PrimaryLevelData.writeVersionTag(fixed);
        return fixed;
    }

    @VisibleForTesting
    protected static void swapInFixedWorld(LevelStorageSource.LevelStorageAccess worldAccess, List<FileMove> moves, Path fileFixDirectory, Path tempWorld) throws FileFixException {
        Path oldWorldFolder;
        Path tempWorldTopLevel;
        FileSystemCapabilities fileSystemCapabilities;
        Path worldFolder = worldAccess.getLevelDirectory().path();
        Path savesDirectory = worldFolder.getParent();
        String worldName = worldFolder.getFileName().toString();
        try {
            fileSystemCapabilities = FileFixerUpper.detectFileSystemCapabilities(fileFixDirectory);
            tempWorldTopLevel = savesDirectory.resolve(FileUtil.findAvailableName(savesDirectory, worldName + " upgraded", ""));
            oldWorldFolder = savesDirectory.resolve(FileUtil.findAvailableName(savesDirectory, worldName + " OUTDATED", ""));
        }
        catch (Exception e) {
            throw new AbortedFileFixException(e);
        }
        if (!fileSystemCapabilities.atomicMove()) {
            throw new AtomicMoveNotSupportedFileFixException(fileSystemCapabilities);
        }
        CopyOption[] moveOptions = fileSystemCapabilities.getMoveOptions();
        LOGGER.info("File system capabilities: {}", (Object)fileSystemCapabilities);
        Path movesFile = fileFixDirectory.resolve(UPGRADE_IN_PROGRESS_NAME);
        try {
            if (fileSystemCapabilities.hardLinks()) {
                CopyOnWriteFileSystem.hardLinkFiles(moves);
            } else {
                FileFixerUpper.writeMoves(moves, worldFolder, tempWorld, movesFile);
            }
        }
        catch (Exception e) {
            throw new AbortedFileFixException(e, List.of(), fileSystemCapabilities);
        }
        LOGGER.info("Applying file structure changes for world \"{}\"", (Object)worldAccess.getLevelId());
        if (fileSystemCapabilities.hardLinks()) {
            try {
                LOGGER.info("Moving new hardlinked world to top level");
                Files.move(tempWorld, tempWorldTopLevel, moveOptions);
            }
            catch (Exception e) {
                LOGGER.error("Encountered error trying to move world folder:", (Throwable)e);
                throw new AbortedFileFixException(e, List.of(), fileSystemCapabilities);
            }
        }
        try {
            LOGGER.info("Moving files into new file structure");
            CopyOnWriteFileSystem.moveFilesWithRetry(moves, moveOptions);
            LOGGER.info("Moving new world to top level");
            Files.move(tempWorld, tempWorldTopLevel, moveOptions);
        }
        catch (Exception e) {
            LOGGER.error("Encountered error while trying to create new world folder:", (Throwable)e);
            List<FileMove> failedMoves = CopyOnWriteFileSystem.tryRevertMoves(moves, moveOptions);
            if (failedMoves.isEmpty()) {
                try {
                    Files.deleteIfExists(movesFile);
                }
                catch (IOException e2) {
                    LOGGER.warn("Failed to delete {}", (Object)movesFile, (Object)e);
                }
            }
            throw new AbortedFileFixException(e, failedMoves, fileSystemCapabilities);
        }
        LOGGER.info("Complete move");
        try {
            Files.deleteIfExists(movesFile);
        }
        catch (IOException e) {
            LOGGER.warn("Failed to delete {}", (Object)movesFile, (Object)e);
        }
        LOGGER.info("Start cleanup");
        try {
            Files.deleteIfExists(worldFolder.resolve("level.dat"));
            Files.deleteIfExists(worldFolder.resolve("level.dat_old"));
        }
        catch (Exception e) {
            LOGGER.warn("Failed to delete outdated level.dat files: ", (Throwable)e);
        }
        MutableBoolean succeeded = new MutableBoolean();
        try {
            worldAccess.releaseTemporarilyAndRun(() -> {
                LOGGER.info("Moving out old world folder");
                try {
                    Files.move(worldFolder, oldWorldFolder, moveOptions);
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to move outdated world folder out of the way; will try to delete instead: ", (Throwable)e);
                    try {
                        FileFixerUpper.deleteDirectory(worldFolder);
                    }
                    catch (Exception e2) {
                        LOGGER.warn("Failed to delete outdated world folder: ", (Throwable)e);
                        throw new FailedCleanupFileFixException(e, tempWorldTopLevel.getFileName().toString(), fileSystemCapabilities);
                    }
                }
                LOGGER.info("Moving in new world folder");
                try {
                    Files.move(tempWorldTopLevel, worldFolder, moveOptions);
                }
                catch (Exception e) {
                    LOGGER.warn("Failed to move in new world folder: ", (Throwable)e);
                    throw new FailedCleanupFileFixException(e, tempWorldTopLevel.getFileName().toString(), fileSystemCapabilities);
                }
                succeeded.setTrue();
            });
        }
        catch (IOException e) {
            Path newWorldFolder = succeeded.isTrue() ? worldFolder : tempWorldTopLevel;
            throw new FailedCleanupFileFixException(e, newWorldFolder.getFileName().toString(), fileSystemCapabilities);
        }
        LOGGER.info("Done applying file structure changes for world \"{}\". Cleaning up outdated data...", (Object)worldAccess.getLevelId());
        try {
            if (Files.exists(oldWorldFolder, new LinkOption[0])) {
                FileFixerUpper.deleteDirectory(oldWorldFolder);
            }
        }
        catch (Exception e) {
            LOGGER.warn("Failed to clean up old world folder", (Throwable)e);
        }
        LOGGER.info("Upgrade done for world \"{}\"", (Object)worldAccess.getLevelId());
    }

    private static void writeMoves(List<FileMove> moves, Path fromDirectory, Path toDirectory, Path filePath) throws IOException {
        Codec<UpgradeInProgress> codec = UpgradeInProgress.codec(fromDirectory, toDirectory);
        JsonElement json = (JsonElement)codec.encodeStart((DynamicOps)JsonOps.INSTANCE, (Object)new UpgradeInProgress(moves)).getOrThrow();
        Files.writeString(filePath, (CharSequence)GSON.toJson(json), StandardOpenOption.DSYNC, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static List<FileMove> readMoves(Path fromDirectory, Path toDirectory, Path filePath) throws IOException {
        JsonObject json = (JsonObject)GSON.fromJson(Files.readString(filePath), JsonObject.class);
        Codec<UpgradeInProgress> codec = UpgradeInProgress.codec(fromDirectory, toDirectory);
        return ((UpgradeInProgress)((Pair)codec.decode((DynamicOps)JsonOps.INSTANCE, (Object)json).getOrThrow()).getFirst()).moves;
    }

    public static FileSystemCapabilities detectFileSystemCapabilities(Path dir) throws IOException {
        return new FileSystemCapabilities(FileFixerUpper.supportsAtomicMove(dir), FileFixerUpper.supportsHardLinks(dir));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean supportsAtomicMove(Path dir) throws IOException {
        Path sourceFile = dir.resolve(UUID.randomUUID().toString());
        Path targetFile = dir.resolve(UUID.randomUUID().toString());
        try {
            Files.createFile(sourceFile, new FileAttribute[0]);
            try {
                Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
                boolean bl = true;
                return bl;
            }
            catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                boolean bl = false;
                Files.deleteIfExists(sourceFile);
                Files.deleteIfExists(targetFile);
                return bl;
            }
        }
        finally {
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(targetFile);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean supportsHardLinks(Path dir) throws IOException {
        Path sourceFile = dir.resolve(UUID.randomUUID().toString());
        Path targetFile = dir.resolve(UUID.randomUUID().toString());
        try {
            Files.createFile(sourceFile, new FileAttribute[0]);
            try {
                Files.createLink(targetFile, sourceFile);
                boolean bl = true;
                return bl;
            }
            catch (Exception exception) {
                boolean bl = false;
                Files.deleteIfExists(sourceFile);
                Files.deleteIfExists(targetFile);
                return bl;
            }
        }
        finally {
            Files.deleteIfExists(sourceFile);
            Files.deleteIfExists(targetFile);
        }
    }

    public record UpgradeInProgress(List<FileMove> moves) {
        public static Codec<UpgradeInProgress> codec(Path fromDirectory, Path toDirectory) {
            return RecordCodecBuilder.create(i -> i.group((App)FileMove.moveCodec(fromDirectory, toDirectory).listOf().fieldOf("moves").forGetter(UpgradeInProgress::moves)).apply((Applicative)i, UpgradeInProgress::new));
        }
    }

    public static class Builder {
        public final List<FileFix> fileFixes = new ArrayList<FileFix>();
        private final int currentVersion;
        private int latestFileFixerVersion;
        private final List<Schema> knownSchemas = new ArrayList<Schema>();

        public Builder(int currentVersion) {
            this.currentVersion = currentVersion;
        }

        public void addFixer(FileFix fileFix) {
            FileFix last;
            if (!this.knownSchemas.contains(fileFix.getSchema())) {
                throw new IllegalArgumentException("Tried to add file fixer with unknown schema. Add it through FileFixerUpper#addSchema instead");
            }
            int fileFixVersion = fileFix.getVersion();
            if (fileFix.getVersion() > this.currentVersion) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Tried to add too recent file fix for version: %s. The data version of the game is: %s", fileFixVersion, this.currentVersion));
            }
            if (!this.fileFixes.isEmpty() && (last = this.fileFixes.getLast()).getVersion() > fileFixVersion) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Tried to add too recent file fix for version: %s. The most recent file fix version is %s", fileFixVersion, last.getVersion()));
            }
            this.fileFixes.add(fileFix);
        }

        public Schema addSchema(DataFixerBuilder fixerUpper, int version, BiFunction<Integer, Schema, Schema> factory) {
            this.latestFileFixerVersion = Math.max(version, this.latestFileFixerVersion);
            Schema schema = fixerUpper.addSchema(version, factory);
            this.knownSchemas.add(schema);
            return schema;
        }

        public FileFixerUpper build(DataFixerBuilder.Result dataFixer) {
            return new FileFixerUpper(dataFixer, this.fileFixes, this.latestFileFixerVersion);
        }
    }
}

