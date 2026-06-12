package com.dx12;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class NativeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeUtils.class);
    
    /**
     * �?JAR 资源中提�?native 库并加载
     * @param resourcePath JAR 中的资源路径（以 / 开头）
     * @param extractDir 提取目录
     */
    public static void loadLibraryFromJar(String resourcePath, File extractDir) throws IOException {
        if (!extractDir.exists()) {
            extractDir.mkdirs();
        }
        
        // 获取文件�?        String fileName = new File(resourcePath).getName();
        File extractedFile = new File(extractDir, fileName);
        
        // 提取文件
        try (InputStream is = NativeUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(is, extractedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // 加载�?        System.load(extractedFile.getAbsolutePath());
        LOGGER.info("Loaded native library: {}", extractedFile.getAbsolutePath());
        
        // 可选：删除临时文件
        // extractedFile.deleteOnExit();
    }
}
