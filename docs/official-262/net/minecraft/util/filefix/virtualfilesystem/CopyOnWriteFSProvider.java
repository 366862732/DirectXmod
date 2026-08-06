/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.filefix.virtualfilesystem;

import java.io.IOException;
import java.lang.runtime.SwitchBootstraps;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.util.DummyFileAttributes;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFSPath;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileSystem;
import net.minecraft.util.filefix.virtualfilesystem.DirectoryNode;
import net.minecraft.util.filefix.virtualfilesystem.FileNode;
import net.minecraft.util.filefix.virtualfilesystem.Node;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSDirectoryNotEmptyException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSFileAlreadyExistsException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSFileSystemException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSNoSuchFileException;
import org.jspecify.annotations.Nullable;

public class CopyOnWriteFSProvider
extends FileSystemProvider {
    public static final String SCHEME = "x-mc-copy-on-write";
    private static final BasicFileAttributeView DUMMY_DIRECTORY_VIEW = new BasicFileAttributeView(){

        @Override
        public String name() {
            return "basic";
        }

        @Override
        public BasicFileAttributes readAttributes() {
            return DummyFileAttributes.DIRECTORY;
        }

        @Override
        public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        }
    };
    private final CopyOnWriteFileSystem fs;

    public CopyOnWriteFSProvider(CopyOnWriteFileSystem fileSystem) {
        this.fs = fileSystem;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getPath(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?> ... attrs) throws IOException {
        return this.newChannel(path, options, attrs, Files::newByteChannel);
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?> ... attrs) throws IOException {
        return this.newChannel(path, options, attrs, FileChannel::open);
    }

    private synchronized <C> C newChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs, ChannelFactory<C> channelFactory) throws IOException {
        CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(path);
        if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
            throw new UnsupportedOperationException("DELETE_ON_CLOSE is not supported by CowFS");
        }
        Node node = this.fs.fileTree().byPathOrNull(cowPath);
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{FileNode.class, DirectoryNode.class}, (Node)node, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                FileNode fileNode = (FileNode)node;
                if (CopyOnWriteFSProvider.wantsWrite(options)) {
                    fileNode.ensureCopy();
                }
                yield channelFactory.newChannel(fileNode.storagePath(), options, attrs);
            }
            case 1 -> throw new CowFSFileSystemException(String.valueOf(cowPath) + ": not a regular file");
            case -1 -> {
                if (options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW)) {
                    DirectoryNode directoryNode = this.fs.fileTree().directoryByPath(Objects.requireNonNull(cowPath.getParent()));
                    Path tempFile = this.fs.createTemporaryFilePath();
                    C result = channelFactory.newChannel(tempFile, options, attrs);
                    FileNode child = new FileNode(cowPath, tempFile, true);
                    directoryNode.addChild(child);
                    yield result;
                }
                throw new CowFSNoSuchFileException(cowPath.toString());
            }
        };
    }

    private static boolean wantsWrite(Set<? extends OpenOption> options) {
        return options.contains(StandardOpenOption.WRITE) || !options.contains(StandardOpenOption.READ) && options.contains(StandardOpenOption.APPEND);
    }

    @Override
    public synchronized DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(dir);
        DirectoryNode directoryNode = this.fs.fileTree().directoryByPath(cowPath);
        final ArrayList<CopyOnWriteFSPath> result = new ArrayList<CopyOnWriteFSPath>();
        for (Node childNode : directoryNode.children()) {
            CopyOnWriteFSPath path = childNode.path();
            if (!filter.accept(path)) continue;
            result.add(path);
        }
        return new DirectoryStream<Path>(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public void close() {
            }

            @Override
            public Iterator<Path> iterator() {
                return result.iterator();
            }
        };
    }

    @Override
    public synchronized void createDirectory(Path dir, FileAttribute<?> ... attrs) throws IOException {
        String folderName;
        CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(dir);
        CopyOnWriteFSPath parent = cowPath.getParent();
        if (parent == null) {
            throw new CowFSFileAlreadyExistsException(cowPath.toString());
        }
        DirectoryNode parentFolder = this.fs.fileTree().directoryByPath(parent);
        if (parentFolder.getChild(folderName = Objects.requireNonNull(cowPath.getFileName()).toString()) != null) {
            throw new CowFSFileAlreadyExistsException(cowPath.toString());
        }
        parentFolder.addChild(new DirectoryNode(cowPath));
    }

    @Override
    public synchronized void delete(Path path) throws IOException {
        CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(path);
        Node node = this.fs.fileTree().byPath(cowPath);
        if (node.parent == null) {
            throw new CowFSFileSystemException("Can't remove root");
        }
        String name = Objects.requireNonNull(node.name());
        if (node instanceof DirectoryNode) {
            DirectoryNode directoryNode = (DirectoryNode)node;
            if (!directoryNode.children().isEmpty()) {
                throw new CowFSDirectoryNotEmptyException(cowPath.toString());
            }
        } else if (node instanceof FileNode) {
            FileNode fileNode = (FileNode)node;
            fileNode.deleteCopy();
        }
        node.parent.removeChild(name);
    }

    @Override
    public void copy(Path source, Path target, CopyOption ... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void move(Path source, Path target, CopyOption ... options) throws IOException {
        CopyOnWriteFSPath sourceCow = CopyOnWriteFSPath.asCow(source);
        CopyOnWriteFSPath targetCow = CopyOnWriteFSPath.asCow(target);
        if (sourceCow.isRoot()) {
            throw new CowFSFileSystemException(String.valueOf(sourceCow) + ": can't move root directory");
        }
        boolean replaceExisting = false;
        for (CopyOption option : options) {
            if (option.equals(StandardCopyOption.ATOMIC_MOVE)) {
                throw new AtomicMoveNotSupportedException(sourceCow.toString(), targetCow.toString(), "CowFs does not support atomic move");
            }
            if (!option.equals(StandardCopyOption.REPLACE_EXISTING)) continue;
            replaceExisting = true;
        }
        Node sourceNode = this.fs.fileTree().byPathOrNull(sourceCow);
        if (sourceNode == null) {
            throw new CowFSNoSuchFileException(sourceCow.toString());
        }
        CopyOnWriteFSPath parent = targetCow.toAbsolutePath().getParent();
        if (parent == null) {
            throw new CowFSFileAlreadyExistsException(targetCow.toString());
        }
        Node targetParentNode = this.fs.fileTree().byPathOrNull(parent);
        if (targetParentNode instanceof DirectoryNode) {
            DirectoryNode folderTarget = (DirectoryNode)targetParentNode;
            String newName = Objects.requireNonNull(targetCow.getFileName()).toString();
            Node oldChild = folderTarget.getChild(newName);
            if (oldChild != null) {
                if (oldChild.equals(sourceNode)) {
                    return;
                }
                if (replaceExisting) {
                    folderTarget.removeChild(newName);
                } else {
                    throw new CowFSFileAlreadyExistsException(targetCow.toString());
                }
            }
            Objects.requireNonNull(sourceNode.parent).removeChild(Objects.requireNonNull(sourceNode.name()));
            sourceNode.setPath(targetCow);
            folderTarget.addChild(sourceNode);
            return;
        }
        throw new CowFSNoSuchFileException(targetCow.toString());
    }

    @Override
    public boolean isSameFile(Path path, Path path2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isHidden(Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStore getFileStore(Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void checkAccess(Path path, AccessMode ... modes) throws IOException {
        Node node;
        CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(path);
        Node node2 = node = this.fs.fileTree().byPath(cowPath);
        Objects.requireNonNull(node2);
        Node node3 = node2;
        int n = 0;
        Path checkPath = switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DirectoryNode.class, FileNode.class}, (Node)node3, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> this.fs.tmpDirectory();
            case 1 -> {
                FileNode file = (FileNode)node3;
                yield file.storagePath();
            }
        };
        checkPath.getFileSystem().provider().checkAccess(checkPath, modes);
    }

    @Override
    public synchronized <V extends FileAttributeView> @Nullable V getFileAttributeView(Path path, Class<V> type, LinkOption ... options) {
        Node node;
        final CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(path);
        Node node2 = node = this.fs.fileTree().byPathOrNull(cowPath);
        int n = 0;
        return (V)(switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DirectoryNode.class, FileNode.class}, (Node)node2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (type == BasicFileAttributeView.class) {
                    yield DUMMY_DIRECTORY_VIEW;
                }
                yield null;
            }
            case 1 -> {
                FileNode file = (FileNode)node2;
                yield Files.getFileAttributeView(file.storagePath(), type, options);
            }
            case -1 -> type == BasicFileAttributeView.class ? new BasicFileAttributeView(){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public String name() {
                    return "basic";
                }

                @Override
                public BasicFileAttributes readAttributes() throws IOException {
                    throw new CowFSNoSuchFileException(cowPath.toString());
                }

                @Override
                public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
                    throw new CowFSNoSuchFileException(cowPath.toString());
                }
            } : null;
        });
    }

    @Override
    public synchronized <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption ... options) throws IOException {
        Node node;
        CopyOnWriteFSPath cowPath = CopyOnWriteFSPath.asCow(path);
        Node node2 = node = this.fs.fileTree().byPath(cowPath);
        Objects.requireNonNull(node2);
        Node node3 = node2;
        int n = 0;
        return (A)(switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{DirectoryNode.class, FileNode.class}, (Node)node3, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> DummyFileAttributes.DIRECTORY;
            case 1 -> {
                FileNode file = (FileNode)node3;
                yield Files.readAttributes(file.storagePath(), type, options);
            }
        });
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption ... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption ... options) {
        throw new UnsupportedOperationException();
    }

    public synchronized CopyOnWriteFSPath getRealPath(CopyOnWriteFSPath path) throws CowFSNoSuchFileException {
        return this.fs.fileTree().byPath((CopyOnWriteFSPath)path.toAbsolutePath()).path;
    }

    @FunctionalInterface
    private static interface ChannelFactory<C> {
        public C newChannel(Path var1, Set<? extends OpenOption> var2, FileAttribute<?> ... var3) throws IOException;
    }
}

