package com.dx12;

import java.io.*;
import java.nio.file.*;

public class NativeUtils {
    
    public static void loadLibraryFromJar(String path) throws IOException {
        System.out.println("[NativeUtils] Loading library from: " + path);
        
        // ?????
        String fileName = Paths.get(path).getFileName().toString();
        System.out.println("[NativeUtils] File name: " + fileName);
        
        // ??????
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "gl4dx12_native");
        Files.createDirectories(tempDir);
        System.out.println("[NativeUtils] Temp directory: " + tempDir.toAbsolutePath());
        
        // ?? DLL
        Path tempFile = tempDir.resolve(fileName);
        System.out.println("[NativeUtils] Target temp file: " + tempFile.toAbsolutePath());
        
        try (InputStream is = NativeUtils.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[NativeUtils] ? Resource not found: " + path);
                System.err.println("[NativeUtils] Available resources:");
                try {
                    java.net.URL url = NativeUtils.class.getResource("/");
                    if (url != null) {
                        System.err.println("[NativeUtils]   Base path: " + url);
                    }
                } catch (Exception e) {
                    System.err.println("[NativeUtils]   Cannot list resources: " + e.getMessage());
                }
                throw new IOException("Native library not found in JAR: " + path);
            }
            System.out.println("[NativeUtils] ? Resource found, copying to temp file...");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[NativeUtils] ? File copied, size: " + tempFile.toFile().length() + " bytes");
        }
        
        // ??????
        if (!Files.exists(tempFile)) {
            System.err.println("[NativeUtils] ? Temp file does not exist after copy!");
            throw new IOException("Failed to extract DLL to: " + tempFile);
        }
        
        // ?? DLL
        System.out.println("[NativeUtils] Loading DLL from: " + tempFile.toAbsolutePath());
        try {
            System.load(tempFile.toAbsolutePath().toString());
            System.out.println("[NativeUtils] ? DLL loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[NativeUtils] ? UnsatisfiedLinkError: " + e.getMessage());
            System.err.println("[NativeUtils] This usually means the DLL is missing required dependencies or is corrupted");
            throw e;
        }
        
        // ?????
        tempFile.toFile().deleteOnExit();
        System.out.println("[NativeUtils] Cleanup: DLL will be deleted on JVM exit");
    }
}