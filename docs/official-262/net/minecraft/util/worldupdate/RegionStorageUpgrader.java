/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.worldupdate;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.worldupdate.FileToUpgrade;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RecreatingSimpleRegionStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionStorageUpgrader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NEW_DIRECTORY_PREFIX = "new_";
    private static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
    private final DataFixer dataFixer;
    private final UpgradeProgress upgradeProgress;
    private final String type;
    private final String folderName;
    protected @Nullable CompletableFuture<Void> previousWriteFuture;
    protected final DataFixTypes dataFixType;
    protected final int defaultVersion;
    private final boolean recreateRegionFiles;
    private @Nullable ResourceKey<Level> dimensionKey;
    private @Nullable SimpleRegionStorage storage;
    private @Nullable List<FileToUpgrade> files;
    private final int startIndex;
    private final @Nullable CompoundTag dataFixContextTag;
    private final Int2ObjectMap<TagModifier> tagModifiers;

    protected RegionStorageUpgrader(DataFixer dataFixer, DataFixTypes dataFixType, String type, String folderName, int defaultVersion, boolean recreateRegionFiles, UpgradeProgress upgradeProgress, int startIndex, @Nullable CompoundTag dataFixContextTag, Int2ObjectMap<TagModifier> tagModifiers) {
        this.dataFixer = dataFixer;
        this.dataFixType = dataFixType;
        this.type = type;
        this.folderName = folderName;
        this.recreateRegionFiles = recreateRegionFiles;
        this.defaultVersion = defaultVersion;
        this.upgradeProgress = upgradeProgress;
        this.startIndex = startIndex;
        this.dataFixContextTag = dataFixContextTag;
        this.tagModifiers = tagModifiers;
    }

    public void init(ResourceKey<Level> dimensionKey, LevelStorageSource.LevelStorageAccess levelStorage) {
        RegionStorageInfo info = new RegionStorageInfo(levelStorage.getLevelId(), dimensionKey, this.type);
        Path regionFolder = levelStorage.getDimensionPath(dimensionKey).resolve(this.folderName);
        this.dimensionKey = dimensionKey;
        this.storage = this.createStorage(info, regionFolder);
        this.files = this.getFilesToProcess(info, regionFolder);
    }

    public void upgrade() {
        if (this.dimensionKey == null || this.storage == null || this.files == null) {
            throw new IllegalStateException("RegionStorageUpgrader has not been initialized");
        }
        if (this.files.isEmpty()) {
            return;
        }
        float totalSize = this.upgradeProgress.getTotalFileFixStats().totalOperations();
        this.upgradeProgress.setStatus(UpgradeProgress.Status.UPGRADING);
        ListIterator<FileToUpgrade> iterator = this.files.listIterator();
        while (!this.upgradeProgress.isCanceled()) {
            boolean worked = false;
            float totalProgress = 0.0f;
            if (iterator.hasNext()) {
                FileToUpgrade fileToUpgrade = iterator.next();
                boolean converted = true;
                for (ChunkPos chunkPos : fileToUpgrade.chunksToUpgrade()) {
                    converted = converted && this.processOnePosition(this.storage, chunkPos);
                    worked = true;
                }
                if (this.recreateRegionFiles) {
                    if (converted) {
                        this.onFileFinished(fileToUpgrade.file());
                    } else {
                        LOGGER.error("Failed to convert region file {}", (Object)fileToUpgrade.file().getPath());
                    }
                }
            }
            int nextIndex = iterator.nextIndex();
            float currentDimensionProgress = (float)nextIndex / totalSize;
            float currentTotalProgress = (float)(this.startIndex + nextIndex) / totalSize;
            this.upgradeProgress.setDimensionProgress(this.dimensionKey, currentDimensionProgress);
            this.upgradeProgress.setTotalProgress(totalProgress += currentTotalProgress);
            if (worked) continue;
            break;
        }
        this.upgradeProgress.setStatus(UpgradeProgress.Status.FINISHED);
        try {
            this.storage.close();
        }
        catch (Exception e) {
            LOGGER.error("Error upgrading chunk", (Throwable)e);
        }
    }

    protected final SimpleRegionStorage createStorage(RegionStorageInfo info, Path regionFolder) {
        return this.recreateRegionFiles ? new RecreatingSimpleRegionStorage(info.withTypeSuffix("source"), regionFolder, info.withTypeSuffix("target"), RegionStorageUpgrader.resolveRecreateDirectory(regionFolder), this.dataFixer, true, this.dataFixType) : new SimpleRegionStorage(info, regionFolder, this.dataFixer, true, this.dataFixType);
    }

    private List<FileToUpgrade> getFilesToProcess(RegionStorageInfo info, Path regionFolder) {
        List<FileToUpgrade> filesToUpgrade = RegionStorageUpgrader.getAllChunkPositions(info, regionFolder);
        this.upgradeProgress.addTotalFileFixOperations(filesToUpgrade.size());
        this.upgradeProgress.addTotalChunks(filesToUpgrade.stream().mapToInt(fileToUpgrade -> fileToUpgrade.chunksToUpgrade().size()).sum());
        return filesToUpgrade;
    }

    public int fileAmount() {
        if (this.files == null) {
            return 0;
        }
        return this.files.size();
    }

    private static List<FileToUpgrade> getAllChunkPositions(RegionStorageInfo info, Path regionFolder) {
        File[] files = regionFolder.toFile().listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) {
            return List.of();
        }
        ArrayList regionFileChunks = Lists.newArrayList();
        for (File regionFile : files) {
            Matcher regex = REGEX.matcher(regionFile.getName());
            if (!regex.matches()) continue;
            int xOffset = Integer.parseInt(regex.group(1)) << 5;
            int zOffset = Integer.parseInt(regex.group(2)) << 5;
            ArrayList chunkPositions = Lists.newArrayList();
            try (RegionFile regionSource = new RegionFile(info, regionFile.toPath(), regionFolder, true);){
                for (int x = 0; x < 32; ++x) {
                    for (int z = 0; z < 32; ++z) {
                        ChunkPos pos = new ChunkPos(x + xOffset, z + zOffset);
                        if (!regionSource.doesChunkExist(pos)) continue;
                        chunkPositions.add(pos);
                    }
                }
                if (chunkPositions.isEmpty()) continue;
                regionFileChunks.add(new FileToUpgrade(regionSource, chunkPositions));
            }
            catch (Throwable t) {
                LOGGER.error("Failed to read chunks from region file {}", (Object)regionFile.toPath(), (Object)t);
            }
        }
        return regionFileChunks;
    }

    private boolean processOnePosition(SimpleRegionStorage storage, ChunkPos pos) {
        boolean converted = false;
        try {
            converted = this.tryProcessOnePosition(storage, pos);
        }
        catch (CompletionException | ReportedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                LOGGER.error("Error upgrading chunk {}", (Object)pos, (Object)cause);
            }
            throw e;
        }
        if (converted) {
            this.upgradeProgress.incrementConverted();
        } else {
            this.upgradeProgress.incrementSkipped();
        }
        return converted;
    }

    protected boolean tryProcessOnePosition(SimpleRegionStorage storage, ChunkPos pos) {
        CompoundTag chunkTag = storage.read(pos).join().orElse(null);
        if (chunkTag != null) {
            int version = NbtUtils.getDataVersion(chunkTag);
            int latestVersion = SharedConstants.getCurrentVersion().dataVersion().version();
            boolean changed = false;
            for (Int2ObjectMap.Entry tagFixer : this.tagModifiers.int2ObjectEntrySet()) {
                int neededVersion = tagFixer.getIntKey();
                chunkTag = this.upgradeTag(storage, chunkTag, neededVersion);
                changed |= ((TagModifier)tagFixer.getValue()).modifyTagAfterFix(pos, chunkTag);
            }
            CompoundTag upgradedTag = this.upgradeTag(storage, chunkTag, latestVersion);
            if ((changed |= version < latestVersion) || this.recreateRegionFiles) {
                if (this.previousWriteFuture != null) {
                    this.previousWriteFuture.join();
                }
                this.previousWriteFuture = storage.write(pos, upgradedTag);
                return true;
            }
        }
        return false;
    }

    protected CompoundTag upgradeTag(SimpleRegionStorage storage, CompoundTag chunkTag, int targetVersion) {
        return storage.upgradeChunkTag(chunkTag, this.defaultVersion, this.dataFixContextTag, targetVersion);
    }

    private void onFileFinished(RegionFile regionFile) {
        if (!this.recreateRegionFiles) {
            return;
        }
        if (this.previousWriteFuture != null) {
            this.previousWriteFuture.join();
        }
        Path filePath = regionFile.getPath();
        Path directoryPath = filePath.getParent();
        Path newFilePath = RegionStorageUpgrader.resolveRecreateDirectory(directoryPath).resolve(filePath.getFileName().toString());
        try {
            if (newFilePath.toFile().exists()) {
                Files.delete(filePath);
                Files.move(newFilePath, filePath, new CopyOption[0]);
            } else {
                LOGGER.error("Failed to replace an old region file. New file {} does not exist.", (Object)newFilePath);
            }
        }
        catch (IOException e) {
            LOGGER.error("Failed to replace an old region file", (Throwable)e);
        }
    }

    protected static Path resolveRecreateDirectory(Path directoryPath) {
        return directoryPath.resolveSibling(NEW_DIRECTORY_PREFIX + directoryPath.getFileName().toString());
    }

    @FunctionalInterface
    public static interface TagModifier {
        public boolean modifyTagAfterFix(ChunkPos var1, CompoundTag var2);
    }

    public static class Builder {
        private final DataFixer dataFixer;
        private @Nullable DataFixTypes dataFixType;
        private @Nullable String type;
        private @Nullable String folderName;
        private int defaultVersion = -1;
        private boolean recreateRegionFiles;
        private UpgradeProgress upgradeProgress = new UpgradeProgress.Noop();
        private @Nullable CompoundTag dataFixContextTag;
        private Int2ObjectAVLTreeMap<TagModifier> tagModifiers = new Int2ObjectAVLTreeMap();

        public Builder(DataFixer dataFixer) {
            this.dataFixer = dataFixer;
        }

        public Builder setDataFixType(DataFixTypes dataFixType) {
            this.dataFixType = dataFixType;
            return this;
        }

        public Builder setTypeAndFolderName(String name) {
            return this.setType(name).setFolderName(name);
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setFolderName(String folderName) {
            this.folderName = folderName;
            return this;
        }

        public Builder setDefaultVersion(int defaultVersion) {
            this.defaultVersion = defaultVersion;
            return this;
        }

        public Builder setRecreateRegionFiles(boolean recreateRegionFiles) {
            this.recreateRegionFiles = recreateRegionFiles;
            return this;
        }

        public Builder trackProgress(UpgradeProgress upgradeProgress) {
            this.upgradeProgress = upgradeProgress;
            return this;
        }

        public Builder setDataFixContextTag(@Nullable CompoundTag dataFixContextTag) {
            this.dataFixContextTag = dataFixContextTag;
            return this;
        }

        public Builder addTagModifier(int version, TagModifier tagModifier) {
            if (this.tagModifiers.containsKey(version)) {
                throw new IllegalStateException("Can't add two fixers for the same data version");
            }
            this.tagModifiers.put(version, (Object)tagModifier);
            return this;
        }

        private Builder setTagModifiers(Int2ObjectAVLTreeMap<TagModifier> tagModifiers) {
            this.tagModifiers = tagModifiers;
            return this;
        }

        public Builder copy() {
            return new Builder(this.dataFixer).setDataFixType(this.dataFixType).setType(this.type).setFolderName(this.folderName).setDefaultVersion(this.defaultVersion).setRecreateRegionFiles(this.recreateRegionFiles).trackProgress(this.upgradeProgress).setDataFixContextTag(this.dataFixContextTag).setTagModifiers((Int2ObjectAVLTreeMap<TagModifier>)this.tagModifiers.clone());
        }

        public RegionStorageUpgrader build(int previousCopiesFileAmounts) {
            return new RegionStorageUpgrader(this.dataFixer, Objects.requireNonNull(this.dataFixType), Objects.requireNonNull(this.type), Objects.requireNonNull(this.folderName), this.defaultVersion, this.recreateRegionFiles, this.upgradeProgress, previousCopiesFileAmounts, this.dataFixContextTag, (Int2ObjectMap<TagModifier>)this.tagModifiers);
        }
    }
}

