/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FolderRepositorySource
implements RepositorySource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    private final Path folder;
    private final PackType packType;
    private final PackSource packSource;
    private final DirectoryValidator validator;

    public FolderRepositorySource(Path folder, PackType packType, PackSource packSource, DirectoryValidator validator) {
        this.folder = folder;
        this.packType = packType;
        this.packSource = packSource;
        this.validator = validator;
    }

    private static String nameFromPath(Path content) {
        return content.getFileName().toString();
    }

    @Override
    public void loadPacks(Consumer<Pack> result) {
        try {
            FileUtil.createDirectoriesSafe(this.folder);
            FolderRepositorySource.discoverPacks(this.folder, this.validator, (content, resources) -> {
                PackLocationInfo locationInfo = this.createDiscoveredFilePackInfo((Path)content);
                Pack pack = Pack.readMetaAndCreate(locationInfo, resources, this.packType, DISCOVERED_PACK_SELECTION_CONFIG);
                if (pack != null) {
                    result.accept(pack);
                }
            });
        }
        catch (IOException e) {
            LOGGER.warn("Failed to list packs in {}", (Object)this.folder, (Object)e);
        }
    }

    private PackLocationInfo createDiscoveredFilePackInfo(Path content) {
        String name = FolderRepositorySource.nameFromPath(content);
        return new PackLocationInfo("file/" + name, Component.literal(name), this.packSource, Optional.empty());
    }

    public static void discoverPacks(Path folder, DirectoryValidator validator, BiConsumer<Path, Pack.ResourcesSupplier> result) throws IOException {
        FolderPackDetector detector = new FolderPackDetector(validator);
        try (DirectoryStream<Path> contents = Files.newDirectoryStream(folder);){
            for (Path content : contents) {
                try {
                    ArrayList<ForbiddenSymlinkInfo> validationIssues = new ArrayList<ForbiddenSymlinkInfo>();
                    Pack.ResourcesSupplier resources = (Pack.ResourcesSupplier)detector.detectPackResources(content, validationIssues);
                    if (!validationIssues.isEmpty()) {
                        LOGGER.warn("Ignoring potential pack entry: {}", (Object)ContentValidationException.getMessage(content, validationIssues));
                        continue;
                    }
                    if (resources != null) {
                        result.accept(content, resources);
                        continue;
                    }
                    LOGGER.info("Found non-pack entry '{}', ignoring", (Object)content);
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to read properties of '{}', ignoring", (Object)content, (Object)e);
                }
            }
        }
    }

    private static class FolderPackDetector
    extends PackDetector<Pack.ResourcesSupplier> {
        protected FolderPackDetector(DirectoryValidator validator) {
            super(validator);
        }

        @Override
        protected @Nullable Pack.ResourcesSupplier createZipPack(Path content) {
            FileSystem fileSystem = content.getFileSystem();
            if (fileSystem == FileSystems.getDefault() || fileSystem instanceof LinkFileSystem) {
                return new FilePackResources.FileResourcesSupplier(content);
            }
            LOGGER.info("Can't open pack archive at {}", (Object)content);
            return null;
        }

        @Override
        protected Pack.ResourcesSupplier createDirectoryPack(Path content) {
            return new PathPackResources.PathResourcesSupplier(content);
        }
    }
}

