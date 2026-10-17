package com.xgdt.dx12.shaderpack;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * 一个 HLSL 光影包（{@code .zip} 或目录）。
 *
 * <p>文件结构模仿 Iris，但着色器是 HLSL、pass 链用 JSON 描述：
 * <pre>
 * MyPack.zip
 * └── shaders/
 *     ├── shaders.properties     # Iris 风格元数据 + option.* 设置项
 *     ├── pipeline.json          # pass 链（输入/输出/入口）
 *     ├── composite/*.hlsl
 *     ├── final/*.hlsl
 *     └── common/*.hlsl
 * </pre>
 *
 * <p>{@code pipeline.json} 中的路径既可以写 {@code shaders/composite/x.hlsl}，
 * 也可以写相对 {@code shaders/} 的 {@code composite/x.hlsl}。
 */
@Environment(EnvType.CLIENT)
public final class ShaderPack implements Closeable {
    private final Path path;
    private final String id;
    private final String displayName;
    private final ZipFile zip;
    private final Map<String, String> properties;
    private final ShaderPackManifest manifest;

    private ShaderPack(Path path, String id, String displayName, ZipFile zip,
                       Map<String, String> properties, ShaderPackManifest manifest) {
        this.path = path;
        this.id = id;
        this.displayName = displayName;
        this.zip = zip;
        this.properties = properties;
        this.manifest = manifest;
    }

    public static ShaderPack open(Path path) throws IOException {
        boolean directory = Files.isDirectory(path);
        ZipFile zip = directory ? null : new ZipFile(path.toFile());
        try {
            String baseId = path.getFileName().toString();
            int dot = baseId.lastIndexOf('.');
            if (dot > 0) {
                baseId = baseId.substring(0, dot);
            }
            String propertiesText = readTextStatic(path, zip, "shaders.properties");
            Map<String, String> properties = parseProperties(propertiesText);
            String name = properties.getOrDefault("name", baseId).trim();
            if (name.isEmpty()) {
                name = baseId;
            }
            String manifestText = readTextStatic(path, zip, "pipeline.json");
            ShaderPackManifest manifest = ShaderPackManifest.parse(manifestText);
            // 必须保留声明顺序：uniform 里 option 值按 shaders.properties 的书写顺序
            // 填入 Options[]，Map.copyOf 的迭代顺序不保证稳定。
            Map<String, String> ordered = Collections.unmodifiableMap(
                new LinkedHashMap<>(properties));
            return new ShaderPack(path, baseId, name, zip, ordered, manifest);
        } catch (RuntimeException e) {
            if (zip != null) {
                zip.close();
            }
            throw new IOException("无法解析光影包 " + path.getFileName() + ": " + e.getMessage(), e);
        } catch (IOException e) {
            if (zip != null) {
                zip.close();
            }
            throw e;
        }
    }

    public Path path() {
        return this.path;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public String description() {
        return this.properties.getOrDefault("description", "");
    }

    public String author() {
        return this.properties.getOrDefault("author", "");
    }

    public String version() {
        return this.properties.getOrDefault("version", "");
    }

    public ShaderPackManifest manifest() {
        return this.manifest;
    }

    public Map<String, String> properties() {
        return this.properties;
    }

    /**
     * 从 {@code shaders.properties} 读取 {@code option.<NAME>} 设置项。
     * 支持 Iris 的 {@code option.<NAME>.min / .max / .comment} 元数据。
     */
    public List<Setting> settings() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.properties.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("option.")) {
                continue;
            }
            String rest = key.substring("option.".length());
            int dot = rest.indexOf('.');
            String name = dot < 0 ? rest : rest.substring(0, dot);
            if (name.isEmpty()) {
                continue;
            }
            values.putIfAbsent(name.toUpperCase(Locale.ROOT), entry.getValue());
        }
        List<Setting> result = new ArrayList<>();
        for (String name : values.keySet()) {
            String base = "option." + name;
            // properties 中大小写可能不一致，做一次不区分大小写的回退查找。
            String defaultValue = this.propertyIgnoreCase(base, "0").trim();
            boolean isBoolean = "true".equalsIgnoreCase(defaultValue)
                || "false".equalsIgnoreCase(defaultValue);
            float parsed = parseFloat(defaultValue, 0.0f);
            float min = parseFloat(this.propertyIgnoreCase(base + ".min", "0"), 0.0f);
            float max = parseFloat(this.propertyIgnoreCase(base + ".max", "1"), 1.0f);
            String comment = this.propertyIgnoreCase(base + ".comment", "");
            result.add(new Setting(name, "true".equalsIgnoreCase(defaultValue), isBoolean,
                parsed, min, max, comment));
        }
        return result;
    }

    private String propertyIgnoreCase(String key, String fallback) {
        String direct = this.properties.get(key);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : this.properties.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    /** 读取包内文件；找不到返回 {@code null}。 */
    public String readText(String relativePath) throws IOException {
        return readTextStatic(this.path, this.zip, relativePath);
    }

    private static String readTextStatic(Path path, ZipFile zip, String relativePath)
        throws IOException {
        if (zip == null) {
            Path base = Files.isDirectory(path) ? path : path.getParent();
            for (String candidate : candidates(relativePath)) {
                Path file = base.resolve(candidate);
                if (Files.isRegularFile(file)) {
                    return Files.readString(file, StandardCharsets.UTF_8);
                }
            }
            return null;
        }
        for (String candidate : candidates(relativePath)) {
            ZipEntry entry = zip.getEntry(candidate);
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        // 回退：忽略大小写与前导 "./" 前缀扫描一遍（zip 来自不同打包工具时很常见）。
        String normalized = normalize(relativePath);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (normalize(entry.getName()).equals(normalized)) {
                try (InputStream in = zip.getInputStream(entry)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private static List<String> candidates(String relativePath) {
        String normalized = normalize(relativePath);
        List<String> list = new ArrayList<>(4);
        list.add(normalized);
        if (normalized.startsWith("shaders/")) {
            list.add(normalized.substring("shaders/".length()));
        } else {
            list.add("shaders/" + normalized);
        }
        return list;
    }

    private static String normalize(String name) {
        String result = name.replace('\\', '/');
        while (result.startsWith("./")) {
            result = result.substring(2);
        }
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private static Map<String, String> parseProperties(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null) {
            return map;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
        }
        return map;
    }

    private static float parseFloat(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void close() {
        if (this.zip != null) {
            try {
                this.zip.close();
            } catch (IOException ignored) {
                // 关闭失败不影响已加载的 pass 链。
            }
        }
    }

    /** 一个可在 UI 中调节的数值设置项。 */
    public record Setting(String name, boolean value, boolean isBoolean,
                          float numericValue, float min, float max, String comment) {
        public float normalized() {
            if (this.max <= this.min) {
                return 0.0f;
            }
            return Math.max(0.0f, Math.min(1.0f,
                (this.numericValue - this.min) / (this.max - this.min)));
        }
    }
}
