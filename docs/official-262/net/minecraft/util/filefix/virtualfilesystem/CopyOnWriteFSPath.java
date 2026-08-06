/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.filefix.virtualfilesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileSystem;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSIllegalArgumentException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSNoSuchFileException;
import org.jspecify.annotations.Nullable;

public class CopyOnWriteFSPath
implements Path {
    private final Path path;
    private final CopyOnWriteFileSystem fs;
    private final boolean isAbsolute;

    private CopyOnWriteFSPath(Path path, CopyOnWriteFileSystem fs, boolean isAbsolute) {
        this.path = path;
        this.fs = fs;
        this.isAbsolute = isAbsolute;
    }

    protected static CopyOnWriteFSPath of(CopyOnWriteFileSystem fs, String first, String ... more) {
        boolean isAbsolute = false;
        while (first.startsWith("/")) {
            isAbsolute = true;
            first = first.substring(1);
        }
        return new CopyOnWriteFSPath(fs.backingFileSystem().getPath(first, more), fs, isAbsolute);
    }

    @Override
    public CopyOnWriteFileSystem getFileSystem() {
        return this.fs;
    }

    @Override
    public boolean isAbsolute() {
        return this.isAbsolute;
    }

    @Override
    public @Nullable Path getRoot() {
        if (this.isAbsolute()) {
            return this.fs.rootPath();
        }
        return null;
    }

    @Override
    public @Nullable CopyOnWriteFSPath getFileName() {
        if (this.isRoot()) {
            return null;
        }
        return new CopyOnWriteFSPath(this.path.getFileName(), this.fs, false);
    }

    @Override
    public @Nullable CopyOnWriteFSPath getParent() {
        if (this.isRoot()) {
            return null;
        }
        Path parent = this.path.getParent();
        if (parent == null) {
            if (this.isAbsolute()) {
                return this.fs.rootPath();
            }
            return null;
        }
        return new CopyOnWriteFSPath(parent, this.fs, this.isAbsolute());
    }

    @Override
    public int getNameCount() {
        return this.path.toString().isEmpty() && this.isAbsolute ? 0 : this.path.getNameCount();
    }

    @Override
    public Path getName(int index) {
        if (this.isRoot()) {
            throw new IllegalArgumentException();
        }
        return new CopyOnWriteFSPath(this.path.getName(index), this.fs, false);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return new CopyOnWriteFSPath(this.path.subpath(beginIndex, endIndex), this.fs, false);
    }

    @Override
    public boolean startsWith(Path other) {
        CopyOnWriteFSPath otherCow = CopyOnWriteFSPath.asCow(other);
        if (this.isAbsolute() != otherCow.isAbsolute()) {
            return false;
        }
        return this.path.startsWith(otherCow.path);
    }

    @Override
    public boolean endsWith(Path other) {
        CopyOnWriteFSPath otherCow = CopyOnWriteFSPath.asCow(other);
        if (otherCow.isAbsolute()) {
            if (this.isAbsolute()) {
                return this.equals(otherCow);
            }
            return false;
        }
        return this.path.endsWith(otherCow.path);
    }

    @Override
    public CopyOnWriteFSPath normalize() {
        Path normalize = this.path.normalize();
        if (this.isAbsolute() && (normalize.startsWith(".") || normalize.startsWith(".."))) {
            return this.fs.rootPath();
        }
        return new CopyOnWriteFSPath(normalize, this.fs, this.isAbsolute());
    }

    @Override
    public CopyOnWriteFSPath resolve(Path other) {
        CopyOnWriteFSPath otherCow = CopyOnWriteFSPath.asCow(other);
        if (other.isAbsolute()) {
            return otherCow;
        }
        Path result = this.path.resolve(otherCow.path);
        return new CopyOnWriteFSPath(result, this.fs, this.isAbsolute());
    }

    @Override
    public CopyOnWriteFSPath resolve(String other) {
        return this.resolve(this.getFileSystem().getPath(other, new String[0]));
    }

    @Override
    public CopyOnWriteFSPath resolve(String first, String ... more) {
        CopyOnWriteFSPath result = this.resolve(first);
        for (String s : more) {
            result = result.resolve(s);
        }
        return result;
    }

    @Override
    public CopyOnWriteFSPath relativize(Path other) {
        CopyOnWriteFSPath otherCow = CopyOnWriteFSPath.asCow(other);
        if (this.isAbsolute() != otherCow.isAbsolute()) {
            throw new IllegalArgumentException("'other' is different type of Path");
        }
        Path result = this.path.relativize(otherCow.path);
        return new CopyOnWriteFSPath(result, this.fs, false);
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CopyOnWriteFSPath toAbsolutePath() {
        if (this.isAbsolute()) {
            return this;
        }
        return new CopyOnWriteFSPath(this.path, this.fs, true);
    }

    @Override
    public CopyOnWriteFSPath toRealPath(LinkOption ... options) throws CowFSNoSuchFileException {
        return this.fs.provider().getRealPath(this);
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier ... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Path other) {
        CopyOnWriteFSPath otherCow = CopyOnWriteFSPath.asCow(other);
        return this.toString().compareTo(otherCow.toString());
    }

    @Override
    public String toString() {
        return this.isAbsolute() ? "/" + String.valueOf(this.path) : this.path.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        CopyOnWriteFSPath paths = (CopyOnWriteFSPath)o;
        return this.isAbsolute == paths.isAbsolute && Objects.equals(this.path, paths.path) && Objects.equals(this.fs, paths.fs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.path, this.fs, this.isAbsolute);
    }

    protected boolean isRoot() {
        return this.getNameCount() == 0;
    }

    protected static CopyOnWriteFSPath asCow(Path other) {
        if (other instanceof CopyOnWriteFSPath) {
            CopyOnWriteFSPath copyOnWriteFSPath = (CopyOnWriteFSPath)other;
            return copyOnWriteFSPath;
        }
        throw new CowFSIllegalArgumentException("Other path is of mismatching file system");
    }
}

