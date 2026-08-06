/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server;

import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class ChainedJsonException
extends IOException {
    private final List<Entry> entries = Lists.newArrayList();
    private final String message;

    public ChainedJsonException(String message) {
        this.entries.add(new Entry());
        this.message = message;
    }

    public ChainedJsonException(String message, Throwable cause) {
        super(cause);
        this.entries.add(new Entry());
        this.message = message;
    }

    public void prependJsonKey(String key) {
        this.entries.get(0).addJsonKey(key);
    }

    public void setFilenameAndFlush(String filename) {
        this.entries.get((int)0).filename = filename;
        this.entries.add(0, new Entry());
    }

    @Override
    public String getMessage() {
        return "Invalid " + String.valueOf(this.entries.get(this.entries.size() - 1)) + ": " + this.message;
    }

    public static ChainedJsonException forException(Exception e) {
        if (e instanceof ChainedJsonException) {
            ChainedJsonException chainedJsonException = (ChainedJsonException)e;
            return chainedJsonException;
        }
        String message = e.getMessage();
        if (e instanceof FileNotFoundException) {
            message = "File not found";
        }
        return new ChainedJsonException(message, e);
    }

    public static class Entry {
        private @Nullable String filename;
        private final List<String> jsonKeys = Lists.newArrayList();

        private Entry() {
        }

        private void addJsonKey(String name) {
            this.jsonKeys.add(0, name);
        }

        public @Nullable String getFilename() {
            return this.filename;
        }

        public String getJsonKeys() {
            return StringUtils.join(this.jsonKeys, (String)"->");
        }

        public String toString() {
            if (this.filename != null) {
                if (this.jsonKeys.isEmpty()) {
                    return this.filename;
                }
                return this.filename + " " + this.getJsonKeys();
            }
            if (this.jsonKeys.isEmpty()) {
                return "(Unknown file)";
            }
            return "(Unknown file) " + this.getJsonKeys();
        }
    }
}

