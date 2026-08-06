/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.access;

import java.nio.file.Path;

public class FileResourceType<T extends AutoCloseable> {
    private final AccessFactory<T> factory;

    public FileResourceType(AccessFactory<T> factory) {
        this.factory = factory;
    }

    public T create(Path path, int dataVersion) {
        return (T)((AutoCloseable)this.factory.create(path, dataVersion));
    }

    @FunctionalInterface
    public static interface AccessFactory<T> {
        public T create(Path var1, int var2);
    }
}

