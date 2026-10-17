package com.xgdt.dx12.shaderpack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扫描 {@code <game_dir>/shaderpacks/}，管理当前启用的 HLSL 光影包。
 *
 * <p>支持 {@code .zip} 与已解压目录两种形式（与 Iris 一致）。启用状态与
 * {@code option.*} 设置值保存在 {@code config/gl4dx12-shaderpacks.properties}。
 */
@Environment(EnvType.CLIENT)
public final class ShaderPackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-shaderpack");
    private static final ShaderPackManager INSTANCE = new ShaderPackManager();

    private final Path directory;
    private final Path configPath;

    private final Properties config = new Properties();
    /** packId -> (optionName -> value)；未指定的选项使用 shaders.properties 中的默认值。 */
    private final Map<String, Map<String, Float>> overrides = new LinkedHashMap<>();

    private boolean enabled;
    private String activeId = "";
    private ShaderPack active;
    private String lastError = "";

    private ShaderPackManager() {
        this.directory = Path.of(System.getProperty("user.dir"), "shaderpacks");
        this.configPath = Path.of(System.getProperty("user.dir"),
            "config", "gl4dx12-shaderpacks.properties");
        loadConfig();
        if (this.enabled) {
            reload();
        }
    }

    public static ShaderPackManager getInstance() {
        return INSTANCE;
    }

    public Path directory() {
        return this.directory;
    }

    public boolean isEnabled() {
        return this.enabled && this.active != null;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
        if (enabled && this.active == null) {
            reload();
        }
    }

    public ShaderPack activePack() {
        return this.active;
    }

    public String activeId() {
        return this.activeId;
    }

    public String lastError() {
        return this.lastError;
    }

    /** 当前启用的包；未启用或加载失败时返回 {@code null}。 */
    public ShaderPack activeIfEnabled() {
        return this.enabled ? this.active : null;
    }

    public void select(String id) {
        this.activeId = id == null ? "" : id;
        this.enabled = this.activeId.isEmpty() ? false : this.enabled;
        saveConfig();
        reload();
    }

    public void clearSelection() {
        this.activeId = "";
        this.enabled = false;
        closeActive();
        saveConfig();
    }

    /** 重新打开当前包（热重载：文件修改后调用）。 */
    public void reload() {
        closeActive();
        this.lastError = "";
        if (this.activeId.isEmpty()) {
            return;
        }
        Path path = findById(this.activeId);
        if (path == null) {
            this.lastError = "光影包不存在: " + this.activeId;
            LOGGER.warn("[shaderpack] {}", this.lastError);
            return;
        }
        try {
            this.active = ShaderPack.open(path);
            LOGGER.info("[shaderpack] 已加载 {} ({} 个 pass) <- {}",
                this.active.displayName(), this.active.manifest().passes().size(),
                path.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            this.lastError = e.getMessage();
            LOGGER.error("[shaderpack] 加载失败: {}", path, e);
        }
    }

    private void closeActive() {
        if (this.active != null) {
            this.active.close();
            this.active = null;
        }
    }

    private Path findById(String id) {
        for (PackEntry entry : list()) {
            if (entry.id().equalsIgnoreCase(id)) {
                return entry.path();
            }
        }
        return null;
    }

    /** 扫描 shaderpacks 目录（按显示名排序）。 */
    public List<PackEntry> list() {
        List<PackEntry> result = new ArrayList<>();
        if (!Files.isDirectory(this.directory)) {
            return result;
        }
        try (Stream<Path> stream = Files.list(this.directory)) {
            for (Path path : stream.toList()) {
                String fileName = path.getFileName().toString();
                boolean zip = fileName.toLowerCase(Locale.ROOT).endsWith(".zip");
                if (!Files.isDirectory(path) && !zip) {
                    continue;
                }
                String id = zip ? fileName.substring(0, fileName.length() - 4) : fileName;
                String name = id;
                String description = "";
                int passCount = 0;
                boolean valid = true;
                try (ShaderPack pack = ShaderPack.open(path)) {
                    name = pack.displayName();
                    description = pack.description();
                    passCount = pack.manifest().passes().size();
                } catch (IOException | RuntimeException e) {
                    valid = false;
                    description = e.getMessage() == null ? "无效的光影包" : e.getMessage();
                }
                result.add(new PackEntry(id, name, description, path, zip, valid, passCount));
            }
        } catch (IOException e) {
            LOGGER.warn("[shaderpack] 无法扫描 {}: {}", this.directory, e.getMessage());
        }
        result.sort(Comparator.comparing(PackEntry::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    // ------------------------------------------------------------------
    // option.* 设置值
    // ------------------------------------------------------------------

    public float getSetting(String packId, String option, float fallback) {
        Map<String, Float> map = this.overrides.get(packId);
        if (map == null) {
            return fallback;
        }
        Float value = map.get(option.toUpperCase(Locale.ROOT));
        return value == null ? fallback : value;
    }

    public void setSetting(String packId, String option, float value) {
        this.overrides
            .computeIfAbsent(packId, ignored -> new LinkedHashMap<>())
            .put(option.toUpperCase(Locale.ROOT), value);
        saveConfig();
    }

    /** 当前包的所有设置项（默认值 + 用户覆盖值），按声明顺序。 */
    public List<ShaderPack.Setting> currentSettings() {
        ShaderPack pack = this.active;
        if (pack == null) {
            return List.of();
        }
        List<ShaderPack.Setting> result = new ArrayList<>();
        for (ShaderPack.Setting setting : pack.settings()) {
            float value = getSetting(pack.id(), setting.name(), setting.numericValue());
            result.add(new ShaderPack.Setting(setting.name(), value != 0.0f,
                setting.isBoolean(), value, setting.min(), setting.max(), setting.comment()));
        }
        return result;
    }

    /** 供 shader uniform 使用的设置值数组（最多 {@code max} 个，按声明顺序）。 */
    public float[] settingValues(int max) {
        List<ShaderPack.Setting> settings = currentSettings();
        float[] values = new float[max];
        for (int i = 0; i < max && i < settings.size(); i++) {
            values[i] = settings.get(i).numericValue();
        }
        return values;
    }

    // ------------------------------------------------------------------
    // 持久化
    // ------------------------------------------------------------------

    private void loadConfig() {
        if (!Files.isRegularFile(this.configPath)) {
            return;
        }
        try (InputStream in = Files.newInputStream(this.configPath)) {
            this.config.load(in);
        } catch (IOException e) {
            LOGGER.warn("[shaderpack] 读取配置失败: {}", e.getMessage());
        }
        this.enabled = Boolean.parseBoolean(this.config.getProperty("enabled", "false"));
        this.activeId = this.config.getProperty("active", "");
        String prefix = "option.";
        for (String key : this.config.stringPropertyNames()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            // option.<packId>.<OPTION>=<value>；packId 可能含 '.'，用最后一个 '.' 分隔。
            String rest = key.substring(prefix.length());
            int dot = rest.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            String packId = rest.substring(0, dot);
            String option = rest.substring(dot + 1).toUpperCase(Locale.ROOT);
            try {
                this.overrides
                    .computeIfAbsent(packId, ignored -> new LinkedHashMap<>())
                    .put(option, Float.parseFloat(this.config.getProperty(key)));
            } catch (NumberFormatException ignored) {
                // 忽略损坏的配置项。
            }
        }
    }

    private void saveConfig() {
        this.config.setProperty("enabled", Boolean.toString(this.enabled));
        this.config.setProperty("active", this.activeId);
        for (Map.Entry<String, Map<String, Float>> pack : this.overrides.entrySet()) {
            for (Map.Entry<String, Float> option : pack.getValue().entrySet()) {
                this.config.setProperty("option." + pack.getKey() + "." + option.getKey(),
                    Float.toString(option.getValue()));
            }
        }
        try {
            Files.createDirectories(this.configPath.getParent());
            try (OutputStream out = Files.newOutputStream(this.configPath)) {
                this.config.store(out, "gl4dx12 HLSL shaderpack configuration");
            }
        } catch (IOException e) {
            LOGGER.warn("[shaderpack] 保存配置失败: {}", e.getMessage());
        }
    }

    /** shaderpacks 目录中的一项。 */
    public record PackEntry(String id, String name, String description, Path path,
                            boolean zip, boolean valid, int passCount) {
    }
}
