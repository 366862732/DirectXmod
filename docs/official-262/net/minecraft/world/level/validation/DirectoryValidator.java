/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.validation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;

public class DirectoryValidator {
    private final PathMatcher symlinkTargetAllowList;

    public DirectoryValidator(PathMatcher symlinkTargetAllowList) {
        this.symlinkTargetAllowList = symlinkTargetAllowList;
    }

    public void validateSymlink(Path path, List<ForbiddenSymlinkInfo> issues) throws IOException {
        Path target = Files.readSymbolicLink(path);
        if (!this.symlinkTargetAllowList.matches(target)) {
            issues.add(new ForbiddenSymlinkInfo(path, target));
        }
    }

    public List<ForbiddenSymlinkInfo> validateSymlink(Path path) throws IOException {
        ArrayList<ForbiddenSymlinkInfo> result = new ArrayList<ForbiddenSymlinkInfo>();
        this.validateSymlink(path, result);
        return result;
    }

    public List<ForbiddenSymlinkInfo> validateDirectory(Path directory, boolean allowTopSymlink) throws IOException {
        BasicFileAttributes targetAttributes;
        ArrayList<ForbiddenSymlinkInfo> issues = new ArrayList<ForbiddenSymlinkInfo>();
        try {
            targetAttributes = Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }
        catch (NoSuchFileException e) {
            return issues;
        }
        if (targetAttributes.isRegularFile()) {
            throw new IOException("Path " + String.valueOf(directory) + " is not a directory");
        }
        if (targetAttributes.isSymbolicLink()) {
            if (allowTopSymlink) {
                directory = Files.readSymbolicLink(directory);
            } else {
                this.validateSymlink(directory, issues);
                return issues;
            }
        }
        this.validateKnownDirectory(directory, issues);
        return issues;
    }

    public void validateKnownDirectory(Path directory, final List<ForbiddenSymlinkInfo> issues) throws IOException {
        Files.walkFileTree(directory, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
            final /* synthetic */ DirectoryValidator this$0;
            {
                DirectoryValidator directoryValidator = this$0;
                Objects.requireNonNull(directoryValidator);
                this.this$0 = directoryValidator;
            }

            private void validateSymlink(Path path, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink()) {
                    this.this$0.validateSymlink(path, issues);
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                this.validateSymlink(dir, attrs);
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                this.validateSymlink(file, attrs);
                return super.visitFile(file, attrs);
            }
        });
    }
}

