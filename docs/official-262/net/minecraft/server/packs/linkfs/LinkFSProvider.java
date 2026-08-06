/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs.linkfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.server.packs.linkfs.LinkFSPath;
import net.minecraft.server.packs.linkfs.PathContents;
import org.jspecify.annotations.Nullable;

class LinkFSProvider
extends FileSystemProvider {
    public static final String SCHEME = "x-mc-link";

    LinkFSProvider() {
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getPath(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?> ... attrs) throws IOException {
        if (options.contains(StandardOpenOption.CREATE_NEW) || options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.APPEND) || options.contains(StandardOpenOption.WRITE)) {
            throw new UnsupportedOperationException();
        }
        Path targetPath = LinkFSProvider.toLinkPath(path).toAbsolutePath().getTargetPath();
        if (targetPath == null) {
            throw new NoSuchFileException(path.toString());
        }
        return Files.newByteChannel(targetPath, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
        final PathContents.DirectoryContents directoryContents = LinkFSProvider.toLinkPath(dir).toAbsolutePath().getDirectoryContents();
        if (directoryContents == null) {
            throw new NotDirectoryException(dir.toString());
        }
        return new DirectoryStream<Path>(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public Iterator<Path> iterator() {
                return directoryContents.children().values().stream().filter(path -> {
                    try {
                        return filter.accept(path);
                    }
                    catch (IOException e) {
                        throw new DirectoryIteratorException(e);
                    }
                }).map(path -> path).iterator();
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?> ... attrs) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void delete(Path path) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption ... options) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void move(Path source, Path target, CopyOption ... options) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) {
        return path instanceof LinkFSPath && path2 instanceof LinkFSPath && path.equals(path2);
    }

    @Override
    public boolean isHidden(Path path) {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) {
        return LinkFSProvider.toLinkPath(path).getFileSystem().store();
    }

    @Override
    public void checkAccess(Path path, AccessMode ... modes) throws IOException {
        if (modes.length == 0 && !LinkFSProvider.toLinkPath(path).exists()) {
            throw new NoSuchFileException(path.toString());
        }
        block4: for (AccessMode mode : modes) {
            switch (mode) {
                case READ: {
                    if (LinkFSProvider.toLinkPath(path).exists()) continue block4;
                    throw new NoSuchFileException(path.toString());
                }
                case EXECUTE: 
                case WRITE: {
                    throw new AccessDeniedException(mode.toString());
                }
            }
        }
    }

    @Override
    public <V extends FileAttributeView> @Nullable V getFileAttributeView(Path path, Class<V> type, LinkOption ... options) {
        LinkFSPath linkPath = LinkFSProvider.toLinkPath(path);
        if (type == BasicFileAttributeView.class) {
            return (V)linkPath.getBasicAttributeView();
        }
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption ... options) throws IOException {
        LinkFSPath linkPath = LinkFSProvider.toLinkPath(path).toAbsolutePath();
        if (type == BasicFileAttributes.class) {
            return (A)linkPath.getBasicAttributes();
        }
        throw new UnsupportedOperationException("Attributes of type " + type.getName() + " not supported");
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption ... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption ... options) {
        throw new ReadOnlyFileSystemException();
    }

    private static LinkFSPath toLinkPath(@Nullable Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (path instanceof LinkFSPath) {
            LinkFSPath p = (LinkFSPath)path;
            return p;
        }
        throw new ProviderMismatchException();
    }
}

