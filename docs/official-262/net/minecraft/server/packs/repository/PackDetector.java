/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.packs.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;

public abstract class PackDetector<T> {
    private final DirectoryValidator validator;

    protected PackDetector(DirectoryValidator validator) {
        this.validator = validator;
    }

    public @Nullable T detectPackResources(Path content, List<ForbiddenSymlinkInfo> issues) throws IOException {
        BasicFileAttributes attributes;
        Path targetContext = content;
        try {
            attributes = Files.readAttributes(content, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        catch (NoSuchFileException e) {
            return null;
        }
        if (attributes.isSymbolicLink()) {
            this.validator.validateSymlink(content, issues);
            if (!issues.isEmpty()) {
                return null;
            }
            targetContext = Files.readSymbolicLink(content);
            attributes = Files.readAttributes(targetContext, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        if (attributes.isDirectory()) {
            this.validator.validateKnownDirectory(targetContext, issues);
            if (!issues.isEmpty()) {
                return null;
            }
            if (!Files.isRegularFile(targetContext.resolve("pack.mcmeta"), new LinkOption[0])) {
                return null;
            }
            return this.createDirectoryPack(targetContext);
        }
        if (attributes.isRegularFile() && targetContext.getFileName().toString().endsWith(".zip")) {
            return this.createZipPack(targetContext);
        }
        return null;
    }

    protected abstract @Nullable T createZipPack(Path var1) throws IOException;

    protected abstract @Nullable T createDirectoryPack(Path var1) throws IOException;
}

