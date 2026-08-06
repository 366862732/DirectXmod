/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util.eventlog;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EventLogDirectory {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COMPRESS_BUFFER_SIZE = 4096;
    private static final String COMPRESSED_EXTENSION = ".gz";
    private final Path root;
    private final String extension;

    private EventLogDirectory(Path root, String extension) {
        this.root = root;
        this.extension = extension;
    }

    public static EventLogDirectory open(Path root, String extension) throws IOException {
        Files.createDirectories(root, new FileAttribute[0]);
        return new EventLogDirectory(root, extension);
    }

    public FileList listFiles() throws IOException {
        try (Stream<Path> list = Files.list(this.root);){
            FileList fileList = new FileList(list.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).map(this::parseFile).filter(Objects::nonNull).toList());
            return fileList;
        }
    }

    private @Nullable File parseFile(Path path) {
        String fileName = path.getFileName().toString();
        int extensionIndex = fileName.indexOf(46);
        if (extensionIndex == -1) {
            return null;
        }
        FileId id = FileId.parse(fileName.substring(0, extensionIndex));
        if (id != null) {
            String extension = fileName.substring(extensionIndex);
            if (extension.equals(this.extension)) {
                return new RawFile(path, id);
            }
            if (extension.equals(this.extension + COMPRESSED_EXTENSION)) {
                return new CompressedFile(path, id);
            }
        }
        return null;
    }

    private static void tryCompress(Path raw, Path compressed) throws IOException {
        if (Files.exists(compressed, new LinkOption[0])) {
            throw new IOException("Compressed target file already exists: " + String.valueOf(compressed));
        }
        try (FileChannel channel = FileChannel.open(raw, StandardOpenOption.WRITE, StandardOpenOption.READ);){
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new IOException("Raw log file is already locked, cannot compress: " + String.valueOf(raw));
            }
            EventLogDirectory.writeCompressed(channel, compressed);
            channel.truncate(0L);
        }
        Files.delete(raw);
    }

    private static void writeCompressed(ReadableByteChannel channel, Path target) throws IOException {
        try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(target, new OpenOption[0]));){
            byte[] bytes = new byte[4096];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (channel.read(buffer) >= 0) {
                buffer.flip();
                ((OutputStream)output).write(bytes, 0, buffer.limit());
                buffer.clear();
            }
        }
    }

    public RawFile createNewFile(LocalDate date) throws IOException {
        FileId id;
        int index = 1;
        Set<FileId> files = this.listFiles().ids();
        while (files.contains(id = new FileId(date, index++))) {
        }
        RawFile file = new RawFile(this.root.resolve(id.toFileName(this.extension)), id);
        Files.createFile(file.path(), new FileAttribute[0]);
        return file;
    }

    public static class FileList
    implements Iterable<File> {
        private final List<File> files;

        private FileList(List<File> files) {
            this.files = new ArrayList<File>(files);
        }

        public FileList prune(LocalDate date, int expiryDays) {
            this.files.removeIf(file -> {
                FileId id = file.id();
                LocalDate expiresAt = id.date().plusDays(expiryDays);
                if (!date.isBefore(expiresAt)) {
                    try {
                        Files.delete(file.path());
                        return true;
                    }
                    catch (IOException e) {
                        LOGGER.warn("Failed to delete expired event log file: {}", (Object)file.path(), (Object)e);
                    }
                }
                return false;
            });
            return this;
        }

        public FileList compressAll() {
            ListIterator<File> iterator = this.files.listIterator();
            while (iterator.hasNext()) {
                File file = iterator.next();
                try {
                    iterator.set(file.compress());
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to compress event log file: {}", (Object)file.path(), (Object)e);
                }
            }
            return this;
        }

        @Override
        public Iterator<File> iterator() {
            return this.files.iterator();
        }

        public Stream<File> stream() {
            return this.files.stream();
        }

        public Set<FileId> ids() {
            return this.files.stream().map(File::id).collect(Collectors.toSet());
        }
    }

    public record FileId(LocalDate date, int index) {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

        public static @Nullable FileId parse(String name) {
            int separator = name.indexOf("-");
            if (separator == -1) {
                return null;
            }
            String date = name.substring(0, separator);
            String index = name.substring(separator + 1);
            try {
                return new FileId(LocalDate.parse(date, DATE_FORMATTER), Integer.parseInt(index));
            }
            catch (NumberFormatException | DateTimeParseException e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return DATE_FORMATTER.format(this.date) + "-" + this.index;
        }

        public String toFileName(String extension) {
            return String.valueOf(this) + extension;
        }
    }

    public record RawFile(Path path, FileId id) implements File
    {
        public FileChannel openChannel() throws IOException {
            return FileChannel.open(this.path, StandardOpenOption.WRITE, StandardOpenOption.READ);
        }

        @Override
        public @Nullable Reader openReader() throws IOException {
            return Files.exists(this.path, new LinkOption[0]) ? Files.newBufferedReader(this.path) : null;
        }

        @Override
        public CompressedFile compress() throws IOException {
            Path compressedPath = this.path.resolveSibling(this.path.getFileName().toString() + EventLogDirectory.COMPRESSED_EXTENSION);
            EventLogDirectory.tryCompress(this.path, compressedPath);
            return new CompressedFile(compressedPath, this.id);
        }
    }

    public record CompressedFile(Path path, FileId id) implements File
    {
        @Override
        public @Nullable Reader openReader() throws IOException {
            if (!Files.exists(this.path, new LinkOption[0])) {
                return null;
            }
            return new BufferedReader(new InputStreamReader((InputStream)new GZIPInputStream(Files.newInputStream(this.path, new OpenOption[0])), StandardCharsets.UTF_8));
        }

        @Override
        public CompressedFile compress() {
            return this;
        }
    }

    public static interface File {
        public Path path();

        public FileId id();

        public @Nullable Reader openReader() throws IOException;

        public CompressedFile compress() throws IOException;
    }
}

