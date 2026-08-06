/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.compress.archivers.tar.TarArchiveEntry
 *  org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
 */
package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.client.worldupload.RealmsUploadCanceledException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadTooLargeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@Environment(value=EnvType.CLIENT)
public class RealmsUploadWorldPacker {
    private static final long SIZE_LIMIT = 0x140000000L;
    private static final String WORLD_FOLDER_NAME = "world";
    private final BooleanSupplier isCanceled;
    private final Path directoryToPack;

    public static File pack(Path directoryToPack, BooleanSupplier isCanceled) throws IOException {
        return new RealmsUploadWorldPacker(directoryToPack, isCanceled).tarGzipArchive();
    }

    private RealmsUploadWorldPacker(Path directoryToPack, BooleanSupplier isCanceled) {
        this.isCanceled = isCanceled;
        this.directoryToPack = directoryToPack;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private File tarGzipArchive() throws IOException {
        try (TarArchiveOutputStream tar = null;){
            File file = File.createTempFile("realms-upload-file", ".tar.gz");
            tar = new TarArchiveOutputStream((OutputStream)new GZIPOutputStream(new FileOutputStream(file)));
            tar.setLongFileMode(3);
            this.addFileToTarGz(tar, this.directoryToPack, WORLD_FOLDER_NAME, true);
            if (this.isCanceled.getAsBoolean()) {
                throw new RealmsUploadCanceledException();
            }
            tar.finish();
            this.verifyBelowSizeLimit(file.length());
            File file2 = file;
            return file2;
        }
    }

    private void addFileToTarGz(TarArchiveOutputStream out, Path path, String base, boolean root) throws IOException {
        if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
        }
        this.verifyBelowSizeLimit(out.getBytesWritten());
        File file = path.toFile();
        String entryName = root ? base : base + file.getName();
        TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
        out.putArchiveEntry(entry);
        if (file.isFile()) {
            try (FileInputStream is = new FileInputStream(file);){
                ((InputStream)is).transferTo((OutputStream)out);
            }
            out.closeArchiveEntry();
        } else {
            out.closeArchiveEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    this.addFileToTarGz(out, child.toPath(), entryName + "/", false);
                }
            }
        }
    }

    private void verifyBelowSizeLimit(long sizeInByte) {
        if (sizeInByte > 0x140000000L) {
            throw new RealmsUploadTooLargeException(0x140000000L);
        }
    }
}

