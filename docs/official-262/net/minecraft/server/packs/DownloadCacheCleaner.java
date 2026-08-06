/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;

public class DownloadCacheCleaner {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void vacuumCacheDir(Path cacheDir, int maxFiles) {
        try {
            List<PathAndTime> filesAndDates = DownloadCacheCleaner.listFilesWithModificationTimes(cacheDir);
            int toRemove = filesAndDates.size() - maxFiles;
            if (toRemove <= 0) {
                return;
            }
            filesAndDates.sort(PathAndTime.NEWEST_FIRST);
            List<PathAndPriority> filesWithDirOrder = DownloadCacheCleaner.prioritizeFilesInDirs(filesAndDates);
            Collections.reverse(filesWithDirOrder);
            filesWithDirOrder.sort(PathAndPriority.HIGHEST_PRIORITY_FIRST);
            HashSet<Path> emptyDirectoryCandidates = new HashSet<Path>();
            for (int i = 0; i < toRemove; ++i) {
                PathAndPriority entry = filesWithDirOrder.get(i);
                Path pathToRemove = entry.path;
                try {
                    Files.delete(pathToRemove);
                    if (entry.removalPriority != 0) continue;
                    emptyDirectoryCandidates.add(pathToRemove.getParent());
                    continue;
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to delete cache file {}", (Object)pathToRemove, (Object)e);
                }
            }
            emptyDirectoryCandidates.remove(cacheDir);
            for (Path dir : emptyDirectoryCandidates) {
                try {
                    Files.delete(dir);
                }
                catch (DirectoryNotEmptyException pathToRemove) {
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to delete empty(?) cache directory {}", (Object)dir, (Object)e);
                }
            }
        }
        catch (IOException | UncheckedIOException e) {
            LOGGER.error("Failed to vacuum cache dir {}", (Object)cacheDir, (Object)e);
        }
    }

    private static List<PathAndTime> listFilesWithModificationTimes(final Path cacheDir) throws IOException {
        try {
            final ArrayList<PathAndTime> unsortedFiles = new ArrayList<PathAndTime>();
            Files.walkFileTree(cacheDir, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && !file.getParent().equals(cacheDir)) {
                        FileTime fileTime = attrs.lastModifiedTime();
                        unsortedFiles.add(new PathAndTime(file, fileTime));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return unsortedFiles;
        }
        catch (NoSuchFileException e) {
            return List.of();
        }
    }

    private static List<PathAndPriority> prioritizeFilesInDirs(List<PathAndTime> filesAndDates) {
        ArrayList<PathAndPriority> result = new ArrayList<PathAndPriority>();
        Object2IntOpenHashMap parentCounts = new Object2IntOpenHashMap();
        for (PathAndTime entry : filesAndDates) {
            int removalPriority = parentCounts.addTo((Object)entry.path.getParent(), 1);
            result.add(new PathAndPriority(entry.path, removalPriority));
        }
        return result;
    }

    private record PathAndTime(Path path, FileTime modifiedTime) {
        public static final Comparator<PathAndTime> NEWEST_FIRST = Comparator.comparing(PathAndTime::modifiedTime).reversed();
    }

    private record PathAndPriority(Path path, int removalPriority) {
        public static final Comparator<PathAndPriority> HIGHEST_PRIORITY_FIRST = Comparator.comparing(PathAndPriority::removalPriority).reversed();
    }
}

