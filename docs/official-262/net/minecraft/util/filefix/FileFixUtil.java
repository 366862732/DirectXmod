/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.filefix;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;

public class FileFixUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String NAMESPACE_PATTERN = "([a-z0-9_.-]+)";

    public static void moveFile(Path baseDirectory, String from, String to) throws IOException {
        Path fromAbsolute = baseDirectory.resolve(from);
        if (!Files.exists(fromAbsolute, new LinkOption[0])) {
            return;
        }
        Path toAbsolute = baseDirectory.resolve(to);
        if (Files.exists(toAbsolute, new LinkOption[0])) {
            LOGGER.warn("Target already exists, skipping move from {} to {}", (Object)from, (Object)to);
            return;
        }
        FileUtil.createDirectoriesSafe(toAbsolute.getParent());
        Files.move(fromAbsolute, toAbsolute, StandardCopyOption.COPY_ATTRIBUTES);
    }

    public static void deleteFileOrEmptyDirectory(Path baseDirectory, String file) throws IOException {
        Path toDelete = baseDirectory.resolve(file);
        if (!Files.exists(toDelete, new LinkOption[0])) {
            return;
        }
        if (Files.isDirectory(toDelete, new LinkOption[0])) {
            try {
                List<Path> files;
                try (Stream<Path> paths = Files.list(toDelete);){
                    files = paths.toList();
                    if (files.size() == 1 && files.getFirst().getFileName().toString().equals(".DS_Store")) {
                        LOGGER.debug("Attempting to delete DS_Store at '{}'", (Object)toDelete);
                        if (!Files.deleteIfExists(files.getFirst())) {
                            LOGGER.warn("Failed to delete file '{}' at '{}'", (Object)files.getFirst(), (Object)toDelete);
                        }
                    }
                }
                paths = Files.list(toDelete);
                try {
                    files = paths.toList();
                }
                finally {
                    if (paths != null) {
                        paths.close();
                    }
                }
                if (!files.isEmpty()) {
                    LOGGER.warn("Failed to delete directory '{}', as it's not empty. Content: {}", (Object)toDelete, files);
                    return;
                }
            }
            catch (IOException e) {
                LOGGER.warn("Failed to delete directory '{}' because {}", (Object)toDelete, (Object)e.toString());
                return;
            }
        }
        Files.deleteIfExists(toDelete);
    }
}

