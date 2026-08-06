/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.filefix.virtualfilesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileSystem;
import org.jspecify.annotations.Nullable;

public class CopyOnWriteFileStore
extends FileStore {
    private final String name;
    private final CopyOnWriteFileSystem fs;

    public CopyOnWriteFileStore(String name, CopyOnWriteFileSystem fs) {
        this.name = name;
        this.fs = fs;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String type() {
        return "copy-on-write";
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return Files.getFileStore(this.fs.tmpDirectory()).getTotalSpace();
    }

    @Override
    public long getUsableSpace() throws IOException {
        return Files.getFileStore(this.fs.tmpDirectory()).getUsableSpace();
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return Files.getFileStore(this.fs.tmpDirectory()).getUnallocatedSpace();
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return type == BasicFileAttributeView.class;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return "basic".equals(name);
    }

    @Override
    public <V extends FileStoreAttributeView> @Nullable V getFileStoreAttributeView(Class<V> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        throw new UnsupportedOperationException();
    }
}

