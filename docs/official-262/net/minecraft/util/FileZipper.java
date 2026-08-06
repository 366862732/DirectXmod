/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class FileZipper
implements Closeable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path outputFile;
    private final Path tempFile;
    private final FileSystem fs;

    public FileZipper(Path outputFile) {
        this.outputFile = outputFile;
        this.tempFile = outputFile.resolveSibling(outputFile.getFileName().toString() + "_tmp");
        try {
            this.fs = Util.ZIP_FILE_SYSTEM_PROVIDER.newFileSystem(this.tempFile, (Map<String, ?>)ImmutableMap.of((Object)"create", (Object)"true"));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void add(Path destinationRelativePath, String content) {
        try {
            Path root = this.fs.getPath(File.separator, new String[0]);
            Path path = root.resolve(destinationRelativePath.toString());
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void add(Path destinationRelativePath, File file) {
        try {
            Path root = this.fs.getPath(File.separator, new String[0]);
            Path path = root.resolve(destinationRelativePath.toString());
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            Files.copy(file.toPath(), path, new CopyOption[0]);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void add(Path path) {
        try {
            Path root = this.fs.getPath(File.separator, new String[0]);
            if (Files.isRegularFile(path, new LinkOption[0])) {
                Path targetFile = root.resolve(path.getParent().relativize(path).toString());
                Files.copy(targetFile, path, new CopyOption[0]);
                return;
            }
            try (Stream<Path> sourceFiles = Files.find(path, Integer.MAX_VALUE, (p, a) -> a.isRegularFile(), new FileVisitOption[0]);){
                for (Path sourceFile : sourceFiles.collect(Collectors.toList())) {
                    Path targetFile = root.resolve(path.relativize(sourceFile).toString());
                    Files.createDirectories(targetFile.getParent(), new FileAttribute[0]);
                    Files.copy(sourceFile, targetFile, new CopyOption[0]);
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.fs.close();
            Files.move(this.tempFile, this.outputFile, new CopyOption[0]);
            LOGGER.info("Compressed to {}", (Object)this.outputFile);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

