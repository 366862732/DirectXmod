/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.filefix.access;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;

@FunctionalInterface
public interface FileRelation {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final FileRelation ORIGIN = List::of;
    public static final FileRelation REGION = basePath -> List.of(basePath.resolve("region"));
    public static final FileRelation DATA = basePath -> List.of(basePath.resolve("data"));
    public static final FileRelation PLAYER_DATA = basePath -> List.of(basePath.resolve("players/data"));
    public static final FileRelation DIMENSIONS = FileRelation::discoverDimensions;
    public static final FileRelation DIMENSIONS_DATA = DIMENSIONS.resolve(DATA);
    public static final FileRelation GENERATED_NAMESPACES = ORIGIN.resolve(basePath -> FileRelation.directoriesInPath(basePath.resolve("generated")));
    public static final FileRelation OLD_OVERWORLD = ORIGIN;
    public static final FileRelation OLD_NETHER = ORIGIN.resolve(basePath -> List.of(basePath.resolve("DIM-1")));
    public static final FileRelation OLD_END = ORIGIN.resolve(basePath -> List.of(basePath.resolve("DIM1")));

    public List<Path> getPaths(Path var1);

    public static FileRelation forDataFileInDimension(String dimension, String fileName) {
        return basePath -> List.of(basePath.resolve("dimensions/minecraft/" + dimension + "/data/" + fileName));
    }

    default public FileRelation forFile(String fileName) {
        return this.resolve(basePath -> List.of(basePath.resolve(fileName)));
    }

    default public FileRelation resolve(FileRelation other) {
        return basePath -> this.getPaths(basePath).stream().flatMap(path -> other.getPaths((Path)path).stream()).toList();
    }

    default public FileRelation join(FileRelation ... relations) {
        return basePath -> {
            HashSet<Path> paths = new HashSet<Path>();
            for (FileRelation relation : relations) {
                paths.addAll(relation.getPaths(basePath));
            }
            return List.copyOf(paths);
        };
    }

    private static List<Path> discoverDimensions(Path basePath) {
        List<Path> list;
        block9: {
            Path dimensionsRoot = basePath.resolve("dimensions");
            if (!Files.exists(dimensionsRoot, new LinkOption[0])) {
                return FileRelation.getDefaultDimensions(basePath);
            }
            Stream<Path> namespacePaths = Files.list(dimensionsRoot);
            try {
                List discoveredDimensions = namespacePaths.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).flatMap(path -> FileRelation.directoriesInPath(path).stream()).toList();
                List<Path> list2 = list = discoveredDimensions.isEmpty() ? FileRelation.getDefaultDimensions(basePath) : discoveredDimensions;
                if (namespacePaths == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (namespacePaths != null) {
                        try {
                            namespacePaths.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to discover dimensions, assuming default: {}", (Object)e.toString());
                    return FileRelation.getDefaultDimensions(basePath);
                }
            }
            namespacePaths.close();
        }
        return list;
    }

    public static List<Path> directoriesInPath(Path path) {
        List<Path> list;
        block8: {
            Stream<Path> dimensionPaths = Files.list(path);
            try {
                list = dimensionPaths.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).toList();
                if (dimensionPaths == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (dimensionPaths != null) {
                        try {
                            dimensionPaths.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    return List.of();
                }
            }
            dimensionPaths.close();
        }
        return list;
    }

    private static List<Path> getDefaultDimensions(Path basePath) {
        return List.of(basePath.resolve("dimensions/minecraft/overworld"), basePath.resolve("dimensions/minecraft/the_nether"), basePath.resolve("dimensions/minecraft/the_end"));
    }
}

