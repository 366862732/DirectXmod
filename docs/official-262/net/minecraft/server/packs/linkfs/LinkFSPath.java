/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs.linkfs;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.server.packs.linkfs.PathContents;
import net.minecraft.util.DummyFileAttributes;
import org.jspecify.annotations.Nullable;

class LinkFSPath
implements Path {
    private static final Comparator<LinkFSPath> PATH_COMPARATOR = Comparator.comparing(LinkFSPath::pathToString);
    private final String name;
    private final LinkFileSystem fileSystem;
    private final @Nullable LinkFSPath parent;
    private @Nullable List<String> pathToRoot;
    private @Nullable String pathString;
    private final PathContents pathContents;

    public LinkFSPath(LinkFileSystem fileSystem, String name, @Nullable LinkFSPath parent, PathContents pathContents) {
        this.fileSystem = fileSystem;
        this.name = name;
        this.parent = parent;
        this.pathContents = pathContents;
    }

    private LinkFSPath createRelativePath(@Nullable LinkFSPath parent, String name) {
        return new LinkFSPath(this.fileSystem, name, parent, PathContents.RELATIVE);
    }

    @Override
    public LinkFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return this.pathContents != PathContents.RELATIVE;
    }

    @Override
    public File toFile() {
        PathContents pathContents = this.pathContents;
        if (pathContents instanceof PathContents.FileContents) {
            PathContents.FileContents file = (PathContents.FileContents)pathContents;
            return file.contents().toFile();
        }
        throw new UnsupportedOperationException("Path " + this.pathToString() + " does not represent file");
    }

    @Override
    public @Nullable LinkFSPath getRoot() {
        if (this.isAbsolute()) {
            return this.fileSystem.rootPath();
        }
        return null;
    }

    @Override
    public LinkFSPath getFileName() {
        return this.createRelativePath(null, this.name);
    }

    @Override
    public @Nullable LinkFSPath getParent() {
        return this.parent;
    }

    @Override
    public int getNameCount() {
        return this.pathToRoot().size();
    }

    private List<String> pathToRoot() {
        if (this.name.isEmpty()) {
            return List.of();
        }
        if (this.pathToRoot == null) {
            ImmutableList.Builder result = ImmutableList.builder();
            if (this.parent != null) {
                result.addAll(this.parent.pathToRoot());
            }
            result.add((Object)this.name);
            this.pathToRoot = result.build();
        }
        return this.pathToRoot;
    }

    @Override
    public LinkFSPath getName(int index) {
        List<String> names = this.pathToRoot();
        if (index < 0 || index >= names.size()) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        return this.createRelativePath(null, names.get(index));
    }

    @Override
    public LinkFSPath subpath(int beginIndex, int endIndex) {
        List<String> names = this.pathToRoot();
        if (beginIndex < 0 || endIndex > names.size() || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }
        LinkFSPath current = null;
        for (int i = beginIndex; i < endIndex; ++i) {
            current = this.createRelativePath(current, names.get(i));
        }
        return current;
    }

    @Override
    public boolean startsWith(Path other) {
        if (other.isAbsolute() != this.isAbsolute()) {
            return false;
        }
        if (other instanceof LinkFSPath) {
            LinkFSPath otherLink = (LinkFSPath)other;
            if (otherLink.fileSystem != this.fileSystem) {
                return false;
            }
            List<String> thisNames = this.pathToRoot();
            List<String> otherNames = otherLink.pathToRoot();
            int otherSize = otherNames.size();
            if (otherSize > thisNames.size()) {
                return false;
            }
            for (int i = 0; i < otherSize; ++i) {
                if (otherNames.get(i).equals(thisNames.get(i))) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean endsWith(Path other) {
        if (other.isAbsolute() && !this.isAbsolute()) {
            return false;
        }
        if (other instanceof LinkFSPath) {
            LinkFSPath otherLink = (LinkFSPath)other;
            if (otherLink.fileSystem != this.fileSystem) {
                return false;
            }
            List<String> thisNames = this.pathToRoot();
            List<String> otherNames = otherLink.pathToRoot();
            int otherSize = otherNames.size();
            int delta = thisNames.size() - otherSize;
            if (delta < 0) {
                return false;
            }
            for (int i = otherSize - 1; i >= 0; --i) {
                if (otherNames.get(i).equals(thisNames.get(delta + i))) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public LinkFSPath normalize() {
        return this;
    }

    @Override
    public LinkFSPath resolve(Path other) {
        LinkFSPath otherLink = this.toLinkPath(other);
        if (other.isAbsolute()) {
            return otherLink;
        }
        return this.resolve(otherLink.pathToRoot());
    }

    private LinkFSPath resolve(List<String> names) {
        LinkFSPath current = this;
        for (String name : names) {
            current = current.resolveName(name);
        }
        return current;
    }

    LinkFSPath resolveName(String name) {
        if (LinkFSPath.isRelativeOrMissing(this.pathContents)) {
            return new LinkFSPath(this.fileSystem, name, this, this.pathContents);
        }
        PathContents pathContents = this.pathContents;
        if (pathContents instanceof PathContents.DirectoryContents) {
            PathContents.DirectoryContents directory = (PathContents.DirectoryContents)pathContents;
            LinkFSPath child = directory.children().get(name);
            return child != null ? child : new LinkFSPath(this.fileSystem, name, this, PathContents.MISSING);
        }
        if (this.pathContents instanceof PathContents.FileContents) {
            return new LinkFSPath(this.fileSystem, name, this, PathContents.MISSING);
        }
        throw new AssertionError((Object)"All content types should be already handled");
    }

    private static boolean isRelativeOrMissing(PathContents contents) {
        return contents == PathContents.MISSING || contents == PathContents.RELATIVE;
    }

    @Override
    public LinkFSPath relativize(Path other) {
        LinkFSPath otherLink = this.toLinkPath(other);
        if (this.isAbsolute() != otherLink.isAbsolute()) {
            throw new IllegalArgumentException("absolute mismatch");
        }
        List<String> thisNames = this.pathToRoot();
        List<String> otherNames = otherLink.pathToRoot();
        if (thisNames.size() >= otherNames.size()) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < thisNames.size(); ++i) {
            if (thisNames.get(i).equals(otherNames.get(i))) continue;
            throw new IllegalArgumentException();
        }
        return otherLink.subpath(thisNames.size(), otherNames.size());
    }

    @Override
    public URI toUri() {
        try {
            return new URI("x-mc-link", this.fileSystem.store().name(), this.pathToString(), null);
        }
        catch (URISyntaxException e) {
            throw new AssertionError("Failed to create URI", e);
        }
    }

    @Override
    public LinkFSPath toAbsolutePath() {
        if (this.isAbsolute()) {
            return this;
        }
        return this.fileSystem.rootPath().resolve(this);
    }

    @Override
    public LinkFSPath toRealPath(LinkOption ... options) {
        return this.toAbsolutePath();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier ... modifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Path other) {
        LinkFSPath otherPath = this.toLinkPath(other);
        return PATH_COMPARATOR.compare(this, otherPath);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof LinkFSPath) {
            LinkFSPath that = (LinkFSPath)other;
            if (this.fileSystem != that.fileSystem) {
                return false;
            }
            boolean hasRealContents = this.hasRealContents();
            if (hasRealContents != that.hasRealContents()) {
                return false;
            }
            if (hasRealContents) {
                return this.pathContents == that.pathContents;
            }
            return Objects.equals(this.parent, that.parent) && Objects.equals(this.name, that.name);
        }
        return false;
    }

    private boolean hasRealContents() {
        return !LinkFSPath.isRelativeOrMissing(this.pathContents);
    }

    @Override
    public int hashCode() {
        return this.hasRealContents() ? this.pathContents.hashCode() : this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.pathToString();
    }

    private String pathToString() {
        if (this.pathString == null) {
            StringBuilder builder = new StringBuilder();
            if (this.isAbsolute()) {
                builder.append("/");
            }
            Joiner.on((String)"/").appendTo(builder, this.pathToRoot());
            this.pathString = builder.toString();
        }
        return this.pathString;
    }

    private LinkFSPath toLinkPath(@Nullable Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (path instanceof LinkFSPath) {
            LinkFSPath p = (LinkFSPath)path;
            if (p.fileSystem == this.fileSystem) {
                return p;
            }
        }
        throw new ProviderMismatchException();
    }

    public boolean exists() {
        return this.hasRealContents();
    }

    public @Nullable Path getTargetPath() {
        Path path;
        PathContents pathContents = this.pathContents;
        if (pathContents instanceof PathContents.FileContents) {
            PathContents.FileContents file = (PathContents.FileContents)pathContents;
            path = file.contents();
        } else {
            path = null;
        }
        return path;
    }

    public @Nullable PathContents.DirectoryContents getDirectoryContents() {
        PathContents.DirectoryContents dir;
        PathContents pathContents = this.pathContents;
        return pathContents instanceof PathContents.DirectoryContents ? (dir = (PathContents.DirectoryContents)pathContents) : null;
    }

    public BasicFileAttributeView getBasicAttributeView() {
        return new BasicFileAttributeView(this){
            final /* synthetic */ LinkFSPath this$0;
            {
                LinkFSPath linkFSPath = this$0;
                Objects.requireNonNull(linkFSPath);
                this.this$0 = linkFSPath;
            }

            @Override
            public String name() {
                return "basic";
            }

            @Override
            public BasicFileAttributes readAttributes() throws IOException {
                return this.this$0.getBasicAttributes();
            }

            @Override
            public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
                throw new ReadOnlyFileSystemException();
            }
        };
    }

    public BasicFileAttributes getBasicAttributes() throws IOException {
        if (this.pathContents instanceof PathContents.DirectoryContents) {
            return DummyFileAttributes.DIRECTORY;
        }
        if (this.pathContents instanceof PathContents.FileContents) {
            return DummyFileAttributes.FILE;
        }
        throw new NoSuchFileException(this.pathToString());
    }
}

