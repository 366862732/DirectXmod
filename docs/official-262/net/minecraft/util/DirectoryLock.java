/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.util.FileUtil;

public class DirectoryLock
implements AutoCloseable {
    public static final String LOCK_FILE = "session.lock";
    private final FileChannel lockFile;
    private final FileLock lock;
    private static final ByteBuffer DUMMY;

    public static DirectoryLock create(Path dir) throws IOException {
        Path lockPath = dir.resolve(LOCK_FILE);
        FileUtil.createDirectoriesSafe(dir);
        FileChannel lockFile = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            lockFile.write(DUMMY.duplicate());
            lockFile.force(true);
            FileLock lock = lockFile.tryLock();
            if (lock == null) {
                throw LockException.alreadyLocked(lockPath);
            }
            return new DirectoryLock(lockFile, lock);
        }
        catch (IOException e) {
            try {
                lockFile.close();
            }
            catch (IOException nested) {
                e.addSuppressed(nested);
            }
            throw e;
        }
    }

    private DirectoryLock(FileChannel lockFile, FileLock lock) {
        this.lockFile = lockFile;
        this.lock = lock;
    }

    @Override
    public void close() throws IOException {
        try {
            if (this.lock.isValid()) {
                this.lock.release();
            }
        }
        finally {
            if (this.lockFile.isOpen()) {
                this.lockFile.close();
            }
        }
    }

    public boolean isValid() {
        return this.lock.isValid();
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static boolean isLocked(Path dir) throws IOException {
        Path lockPath = dir.resolve(LOCK_FILE);
        try (FileChannel lockFile = FileChannel.open(lockPath, StandardOpenOption.WRITE);){
            boolean bl;
            block15: {
                FileLock maybeLock = lockFile.tryLock();
                try {
                    boolean bl2 = bl = maybeLock == null;
                    if (maybeLock == null) break block15;
                }
                catch (Throwable throwable) {
                    if (maybeLock != null) {
                        try {
                            maybeLock.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                maybeLock.close();
            }
            return bl;
        }
        catch (AccessDeniedException e) {
            return true;
        }
        catch (NoSuchFileException e) {
            return false;
        }
    }

    static {
        byte[] chars = "\u2603".getBytes(StandardCharsets.UTF_8);
        DUMMY = ByteBuffer.allocateDirect(chars.length);
        DUMMY.put(chars);
        DUMMY.flip();
    }

    public static class LockException
    extends IOException {
        private LockException(Path path, String message) {
            super(String.valueOf(path.toAbsolutePath()) + ": " + message);
        }

        public static LockException alreadyLocked(Path path) {
            return new LockException(path, "already locked (possibly by other Minecraft instance?)");
        }
    }
}

