/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Splitter
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs.linkfs;

import com.google.common.base.Splitter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.packs.linkfs.LinkFSFileStore;
import net.minecraft.server.packs.linkfs.LinkFSPath;
import net.minecraft.server.packs.linkfs.LinkFSProvider;
import net.minecraft.server.packs.linkfs.PathContents;
import org.jspecify.annotations.Nullable;

public class LinkFileSystem
extends FileSystem {
    private static final Set<String> VIEWS = Set.of("basic");
    public static final String PATH_SEPARATOR = "/";
    private static final Splitter PATH_SPLITTER = Splitter.on((char)'/');
    private final FileStore store;
    private final FileSystemProvider provider = new LinkFSProvider();
    private final LinkFSPath root;

    private LinkFileSystem(String name, DirectoryEntry rootEntry) {
        this.store = new LinkFSFileStore(name);
        this.root = LinkFileSystem.buildPath(rootEntry, this, "", null);
    }

    private static LinkFSPath buildPath(DirectoryEntry entry, LinkFileSystem fileSystem, String selfName, @Nullable LinkFSPath parent) {
        Object2ObjectOpenHashMap children = new Object2ObjectOpenHashMap();
        LinkFSPath result = new LinkFSPath(fileSystem, selfName, parent, new PathContents.DirectoryContents((Map<String, LinkFSPath>)children));
        entry.files.forEach((name, linkTarget) -> children.put(name, (Object)new LinkFSPath(fileSystem, (String)name, result, new PathContents.FileContents((Path)linkTarget))));
        entry.children.forEach((name, childEntry) -> children.put(name, (Object)LinkFileSystem.buildPath(childEntry, fileSystem, name, result)));
        children.trim();
        return result;
    }

    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return List.of(this.root);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return List.of(this.store);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return VIEWS;
    }

    @Override
    public Path getPath(String first, String ... more) {
        String joinedPath;
        Stream<String> path = Stream.of(first);
        if (more.length > 0) {
            path = Stream.concat(path, Stream.of(more));
        }
        if ((joinedPath = path.collect(Collectors.joining(PATH_SEPARATOR))).equals(PATH_SEPARATOR)) {
            return this.root;
        }
        if (joinedPath.startsWith(PATH_SEPARATOR)) {
            LinkFSPath result = this.root;
            for (String segment : PATH_SPLITTER.split((CharSequence)joinedPath.substring(1))) {
                if (segment.isEmpty()) {
                    throw new IllegalArgumentException("Empty paths not allowed");
                }
                result = result.resolveName(segment);
            }
            return result;
        }
        LinkFSPath result = null;
        for (String segment : PATH_SPLITTER.split((CharSequence)joinedPath)) {
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("Empty paths not allowed");
            }
            result = new LinkFSPath(this, segment, result, PathContents.RELATIVE);
        }
        if (result == null) {
            throw new IllegalArgumentException("Empty paths not allowed");
        }
        return result;
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }

    public FileStore store() {
        return this.store;
    }

    public LinkFSPath rootPath() {
        return this.root;
    }

    public static Builder builder() {
        return new Builder();
    }

    private record DirectoryEntry(Map<String, DirectoryEntry> children, Map<String, Path> files) {
        public DirectoryEntry() {
            this(new HashMap<String, DirectoryEntry>(), new HashMap<String, Path>());
        }
    }

    public static class Builder {
        private final DirectoryEntry root = new DirectoryEntry();

        public Builder put(List<String> path, String name, Path target) {
            DirectoryEntry currentEntry = this.root;
            for (String segment : path) {
                currentEntry = currentEntry.children.computeIfAbsent(segment, n -> new DirectoryEntry());
            }
            currentEntry.files.put(name, target);
            return this;
        }

        public Builder put(List<String> path, Path target) {
            if (path.isEmpty()) {
                throw new IllegalArgumentException("Path can't be empty");
            }
            int lastIndex = path.size() - 1;
            return this.put(path.subList(0, lastIndex), path.get(lastIndex), target);
        }

        public FileSystem build(String name) {
            return new LinkFileSystem(name, this.root);
        }
    }
}

