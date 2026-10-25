package com.xgdt.dx12.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple config manager for GL4DX12 mod using Java Properties.
 * Stores settings in {game_dir}/config/gl4dx12.properties
 */
public class Dx12Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("Dx12Config");
    private static final String FILE_NAME = "config/gl4dx12.properties";

    private static final String KEY_AA_MODE = "aa_mode";
    private static final String KEY_RENDER_SCALE = "render_scale";
    private static final String KEY_UPSCALE_MODE = "upscale_mode";
    private static final String KEY_UPSCALE_SHARPNESS = "upscale_sharpness";
    private static final String KEY_FRAME_GEN = "frame_gen";
    private static final String KEY_FRAME_GEN_STRENGTH = "frame_gen_strength";
    private static final String KEY_NGX_APP_ID = "ngx_app_id";
    private static final String KEY_NGX_PROJECT_ID = "ngx_project_id";

    private static Dx12Config instance;
    private final Properties props = new Properties();
    private final Path configPath;
    
    private int aaMode = 1; // default: FXAA
    /** 内部渲染分辨率百分比（50..100）。100 = 原生分辨率。 */
    private int renderScale = 100;
    /** 上采样滤镜：0 = 双线性，1 = CAS 锐化。 */
    private int upscaleMode = 0;
    /** CAS 锐化强度（0..100）。 */
    private int upscaleSharpness = 60;
    /** 帧生成开关：0 = 关闭，1 = 开启（每个真实帧后再显示一帧插值帧）。 */
    private int frameGen = 0;
    /** 插值帧权重（0..100）：越大越靠近当前真实帧。 */
    private int frameGenStrength = 50;
    /** NVIDIA NGX（DLSS）Application ID：NVIDIA 分配的十进制数字，留空则 DLSS 通道关闭。 */
    private String ngxAppId = "";
    /** NGX Project ID：没有 App ID 时的替代标识（同样留空则关闭）。 */
    private String ngxProjectId = "";

    private Dx12Config() {
        configPath = Path.of(System.getProperty("user.dir"), FILE_NAME);
        load();
    }

    public static Dx12Config getInstance() {
        if (instance == null) {
            instance = new Dx12Config();
        }
        return instance;
    }

    public int getAaMode() { return aaMode; }
    
    public void setAaMode(int mode) {
        this.aaMode = mode;
        props.setProperty(KEY_AA_MODE, String.valueOf(mode));
        save();
    }

    public int getRenderScale() { return renderScale; }

    public void setRenderScale(int scale) {
        this.renderScale = Math.max(50, Math.min(100, scale));
        props.setProperty(KEY_RENDER_SCALE, String.valueOf(this.renderScale));
        save();
    }

    public int getUpscaleMode() { return upscaleMode; }

    public void setUpscaleMode(int mode) {
        this.upscaleMode = mode == 1 ? 1 : 0;
        props.setProperty(KEY_UPSCALE_MODE, String.valueOf(this.upscaleMode));
        save();
    }

    public int getUpscaleSharpness() { return upscaleSharpness; }

    public void setUpscaleSharpness(int sharpness) {
        this.upscaleSharpness = Math.max(0, Math.min(100, sharpness));
        props.setProperty(KEY_UPSCALE_SHARPNESS, String.valueOf(this.upscaleSharpness));
        save();
    }

    public int getFrameGen() { return frameGen; }

    public void setFrameGen(int enabled) {
        this.frameGen = enabled == 1 ? 1 : 0;
        props.setProperty(KEY_FRAME_GEN, String.valueOf(this.frameGen));
        save();
    }

    public int getFrameGenStrength() { return frameGenStrength; }

    public void setFrameGenStrength(int strength) {
        this.frameGenStrength = Math.max(0, Math.min(100, strength));
        props.setProperty(KEY_FRAME_GEN_STRENGTH, String.valueOf(this.frameGenStrength));
        save();
    }

    public String getNgxAppId() { return ngxAppId; }

    public void setNgxAppId(String appId) {
        this.ngxAppId = appId == null ? "" : appId.trim();
        props.setProperty(KEY_NGX_APP_ID, this.ngxAppId);
        save();
    }

    public String getNgxProjectId() { return ngxProjectId; }

    public void setNgxProjectId(String projectId) {
        this.ngxProjectId = projectId == null ? "" : projectId.trim();
        props.setProperty(KEY_NGX_PROJECT_ID, this.ngxProjectId);
        save();
    }

    private void load() {
        try {
            if (Files.exists(configPath)) {
                try (var is = Files.newInputStream(configPath)) {
                    props.load(is);
                }
                aaMode = parseInt(props.getProperty(KEY_AA_MODE, "1"), 1);
                renderScale = parseInt(props.getProperty(KEY_RENDER_SCALE, "100"), 100);
                upscaleMode = parseInt(props.getProperty(KEY_UPSCALE_MODE, "0"), 0);
                upscaleSharpness = parseInt(props.getProperty(KEY_UPSCALE_SHARPNESS, "60"), 60);
                frameGen = parseInt(props.getProperty(KEY_FRAME_GEN, "0"), 0);
                frameGenStrength = parseInt(props.getProperty(KEY_FRAME_GEN_STRENGTH, "50"), 50);
                ngxAppId = props.getProperty(KEY_NGX_APP_ID, "").trim();
                ngxProjectId = props.getProperty(KEY_NGX_PROJECT_ID, "").trim();
                LOGGER.info("[dx12-wm] Config loaded: aa_mode={} render_scale={} upscale_mode={} frame_gen={} ngx={}",
                    aaMode, renderScale, upscaleMode, frameGen,
                    ngxAppId.isEmpty() && ngxProjectId.isEmpty() ? "off" : "configured");
            } else {
                LOGGER.info("[dx12-wm] No config file found, using defaults");
                props.setProperty(KEY_AA_MODE, String.valueOf(aaMode));
                props.setProperty(KEY_RENDER_SCALE, String.valueOf(renderScale));
                props.setProperty(KEY_UPSCALE_MODE, String.valueOf(upscaleMode));
                props.setProperty(KEY_UPSCALE_SHARPNESS, String.valueOf(upscaleSharpness));
                props.setProperty(KEY_FRAME_GEN, String.valueOf(frameGen));
                props.setProperty(KEY_FRAME_GEN_STRENGTH, String.valueOf(frameGenStrength));
                // 空值也写进文件，方便用户按名字找到并填写 App ID。
                props.setProperty(KEY_NGX_APP_ID, "");
                props.setProperty(KEY_NGX_PROJECT_ID, "");
                save(); // save defaults
            }
        } catch (Exception e) {
            LOGGER.warn("[dx12-wm] Failed to load config: {}", e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (var os = Files.newOutputStream(configPath)) {
                props.store(os, "GL4DX12 Mod Configuration");
            }
        } catch (Exception e) {
            LOGGER.warn("[dx12-wm] Failed to save config: {}", e.getMessage());
        }
    }

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return def; }
    }
}
