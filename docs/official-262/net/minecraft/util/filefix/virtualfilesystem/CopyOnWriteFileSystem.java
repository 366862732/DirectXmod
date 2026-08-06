/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.file.PathUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util.filefix.virtualfilesystem;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.lang.runtime.SwitchBootstraps;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Util;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFSPath;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFSProvider;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileStore;
import net.minecraft.util.filefix.virtualfilesystem.DirectoryNode;
import net.minecraft.util.filefix.virtualfilesystem.FileMove;
import net.minecraft.util.filefix.virtualfilesystem.FileNode;
import net.minecraft.util.filefix.virtualfilesystem.Node;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSCreationException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSSymlinkException;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;

public class CopyOnWriteFileSystem
extends FileSystem {
    private static final Set<String> FILE_ATTRIBUTE_VIEWS = Set.of("basic");
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CopyOnWriteFileStore store;
    private final CopyOnWriteFSProvider provider;
    private final Path baseDirectory;
    private final PathMatcher skippedPaths;
    private final Path tmpDirectory;
    private final CopyOnWriteFSPath rootPath;
    private final AtomicInteger tmpFileIndex = new AtomicInteger();
    private DirectoryNode fileTree;

    private CopyOnWriteFileSystem(String name, Path baseDirectory, Path tmpDirectory, PathMatcher skippedPaths) throws IOException {
        this.baseDirectory = baseDirectory;
        this.tmpDirectory = tmpDirectory;
        this.skippedPaths = skippedPaths;
        this.provider = new CopyOnWriteFSProvider(this);
        this.store = new CopyOnWriteFileStore(name, this);
        this.rootPath = this.getPath("/", new String[0]);
        this.fileTree = this.buildFileTreeFrom(baseDirectory);
    }

    public static CopyOnWriteFileSystem create(String name, Path baseDirectory, Path tmpDirectory, PathMatcher skippedPaths) throws IOException {
        if (Files.exists(tmpDirectory, new LinkOption[0])) {
            throw new CowFSCreationException("Temporary directory already exists: " + String.valueOf(tmpDirectory));
        }
        CopyOnWriteFileSystem fileSystem = new CopyOnWriteFileSystem(name, baseDirectory, tmpDirectory, skippedPaths);
        Files.createDirectory(tmpDirectory, new FileAttribute[0]);
        return fileSystem;
    }

    private DirectoryNode buildFileTreeFrom(final Path baseDirectory) throws IOException {
        final DirectoryNode fileTree = new DirectoryNode(this.rootPath);
        Files.walkFileTree(baseDirectory, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
            final /* synthetic */ CopyOnWriteFileSystem this$0;
            {
                CopyOnWriteFileSystem copyOnWriteFileSystem = this$0;
                Objects.requireNonNull(copyOnWriteFileSystem);
                this.this$0 = copyOnWriteFileSystem;
            }

            @Override
            public FileVisitResult visitFile(Path realPath, BasicFileAttributes attrs) throws IOException {
                1.checkAttributes(realPath, attrs);
                if (this.this$0.skippedPaths.matches(realPath)) {
                    return FileVisitResult.CONTINUE;
                }
                CopyOnWriteFSPath cowPath = this.toCowPath(realPath);
                DirectoryNode parentNode = fileTree.directoryByPath(Objects.requireNonNull(cowPath.getParent()));
                parentNode.addChild(new FileNode(cowPath, realPath, false));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path realPath, BasicFileAttributes attrs) throws IOException {
                1.checkAttributes(realPath, attrs);
                if (this.this$0.skippedPaths.matches(realPath)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (realPath.equals(baseDirectory)) {
                    return FileVisitResult.CONTINUE;
                }
                CopyOnWriteFSPath cowPath = this.toCowPath(realPath);
                DirectoryNode parentNode = fileTree.directoryByPath(Objects.requireNonNull(cowPath.getParent()));
                parentNode.addChild(new DirectoryNode(cowPath));
                return FileVisitResult.CONTINUE;
            }

            private static void checkAttributes(Path realPath, BasicFileAttributes attrs) throws CowFSCreationException {
                if (!attrs.isRegularFile() && !attrs.isDirectory()) {
                    throw new CowFSSymlinkException("Cannot build copy-on-write file system when symlink is present: " + String.valueOf(realPath));
                }
                if (!Files.isWritable(realPath)) {
                    throw new CowFSCreationException("Cannot build copy-on-write file system, missing write access for file: " + String.valueOf(realPath));
                }
            }

            private CopyOnWriteFSPath toCowPath(Path realPath) {
                return fileTree.path().resolve(baseDirectory.relativize(realPath).toString());
            }
        });
        return fileTree;
    }

    @VisibleForTesting
    protected void resetFileTreeToBaseFolderContent() throws IOException {
        this.fileTree = this.buildFileTreeFrom(this.baseDirectory);
    }

    @Override
    public CopyOnWriteFSProvider provider() {
        return this.provider;
    }

    @Override
    public void close() throws IOException {
        if (Files.exists(this.tmpDirectory, new LinkOption[0])) {
            PathUtils.deleteDirectory((Path)this.tmpDirectory);
        }
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return this.backingFileSystem().getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return List.of(this.rootPath());
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return List.of(this.store);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return FILE_ATTRIBUTE_VIEWS;
    }

    @Override
    public CopyOnWriteFSPath getPath(String first, String ... more) {
        return CopyOnWriteFSPath.of(this, first, more);
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

    public CopyOnWriteFileStore store() {
        return this.store;
    }

    public CopyOnWriteFSPath rootPath() {
        return this.rootPath;
    }

    DirectoryNode fileTree() {
        return this.fileTree;
    }

    public Path baseDirectory() {
        return this.baseDirectory;
    }

    public Path tmpDirectory() {
        return this.tmpDirectory;
    }

    Path createTemporaryFilePath() {
        return this.tmpDirectory.resolve("tmp_" + this.tmpFileIndex.incrementAndGet());
    }

    public FileSystem backingFileSystem() {
        return this.tmpDirectory.getFileSystem();
    }

    public Moves collectMoveOperations(Path outPath) {
        Moves result = new Moves(new ArrayList<Path>(), new ArrayList<FileMove>(), new ArrayList<FileMove>());
        this.collectMoveOperations(outPath, this.fileTree, result);
        return result;
    }

    private void collectMoveOperations(Path outPath, DirectoryNode folder, Moves result) {
        for (Node childNode : folder.children()) {
            Node node;
            Path target = outPath.resolve(Objects.requireNonNull(childNode.name()));
            Objects.requireNonNull(childNode);
            int n = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{FileNode.class, DirectoryNode.class}, (Node)node, n)) {
                default: {
                    throw new MatchException(null, null);
                }
                case 0: {
                    FileNode fileNode = (FileNode)node;
                    FileMove move = new FileMove(fileNode.storagePath(), target);
                    if (fileNode.isCopy) {
                        result.copiedFiles.add(move);
                        break;
                    }
                    result.preexistingFiles.add(move);
                    break;
                }
                case 1: {
                    DirectoryNode directoryNode = (DirectoryNode)node;
                    result.directories.add(target);
                    this.collectMoveOperations(target, directoryNode, result);
                }
            }
        }
    }

    public static void createDirectories(List<Path> directories) throws IOException {
        for (Path directory : directories) {
            Files.createDirectory(directory, new FileAttribute[0]);
        }
    }

    public static void hardLinkFiles(List<FileMove> moves) throws IOException {
        for (FileMove move : moves) {
            if (Files.exists(move.to(), new LinkOption[0])) continue;
            if (Files.isRegularFile(move.from(), new LinkOption[0])) {
                Files.createLink(move.to(), move.from());
                continue;
            }
            throw new IllegalStateException("Not a regular file: " + String.valueOf(move.from()));
        }
    }

    public static void moveFiles(List<FileMove> moves) throws IOException {
        for (FileMove move : moves) {
            Files.move(move.from(), move.to(), new CopyOption[0]);
        }
    }

    public static void moveFilesWithRetry(List<FileMove> moves, CopyOption ... options) throws IOException {
        for (FileMove move : moves) {
            if (!Files.exists(move.from(), new LinkOption[0]) && Files.exists(move.to(), new LinkOption[0])) continue;
            if (Files.isRegularFile(move.from(), new LinkOption[0])) {
                Files.move(move.from(), move.to(), options);
                continue;
            }
            throw new IOException("Not a regular file: " + String.valueOf(move.from()));
        }
    }

    public static List<FileMove> tryRevertMoves(List<FileMove> moves, CopyOption ... options) {
        ArrayList<FileMove> failedMoves = new ArrayList<FileMove>();
        for (FileMove move : moves) {
            if (!Files.exists(move.to(), new LinkOption[0]) && Files.exists(move.from(), new LinkOption[0])) continue;
            if (Files.isRegularFile(move.to(), new LinkOption[0])) {
                boolean success = Util.safeMoveFile(move.to(), move.from(), options);
                if (success) {
                    LOGGER.info("Reverted move from {} to {}", (Object)move.from(), (Object)move.to());
                    continue;
                }
                LOGGER.error("Failed to revert move from {} to {}", (Object)move.from(), (Object)move.to());
                failedMoves.add(move);
                continue;
            }
            LOGGER.error("Skipping reverting move from {} to {} as it's not a file", (Object)move.from(), (Object)move.to());
            failedMoves.add(move);
        }
        if (failedMoves.isEmpty()) {
            LOGGER.info("Successfully reverted back to previous world state");
        } else {
            LOGGER.error("Completed reverting with errors");
        }
        return failedMoves;
    }

    public record Moves(List<Path> directories, List<FileMove> copiedFiles, List<FileMove> preexistingFiles) {
    }
}

