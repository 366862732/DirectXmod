/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;

public class ContentValidationException
extends Exception {
    private final Path directory;
    private final List<ForbiddenSymlinkInfo> entries;

    public ContentValidationException(Path directory, List<ForbiddenSymlinkInfo> entries) {
        this.directory = directory;
        this.entries = entries;
    }

    @Override
    public String getMessage() {
        return ContentValidationException.getMessage(this.directory, this.entries);
    }

    public static String getMessage(Path directory, List<ForbiddenSymlinkInfo> entries) {
        return "Failed to validate '" + String.valueOf(directory) + "'. Found forbidden symlinks: " + entries.stream().map(e -> String.valueOf(e.link()) + "->" + String.valueOf(e.target())).collect(Collectors.joining(", "));
    }
}

