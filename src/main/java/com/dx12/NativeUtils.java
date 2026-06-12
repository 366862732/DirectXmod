package com.dx12;

import java.io.*;
import java.nio.file.*;

public class NativeUtils {
    
    public static void loadLibraryFromJar(String path) throws IOException {
        String fileName = Paths.get(path).getFileName().toString();
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "gl4dx12_native");
        Files.createDirectories(tempDir);
        Path tempFile = tempDir.resolve(fileName);
        
        try (InputStream is = NativeUtils.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Native library not found in JAR: " + path);
            }
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        System.load(tempFile.toAbsolutePath().toString());
        tempFile.toFile().deleteOnExit();
    }
}