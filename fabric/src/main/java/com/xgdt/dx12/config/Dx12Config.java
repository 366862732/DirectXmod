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
    private static final String KEY_DLSS_SR = "dlss_sr";
    private static final String KEY_DLSS_QUALITY = "dlss_quality";
    private static final String KEY_DLSS_PRESET = "dlss_preset";
    private static final String KEY_DLSS_FG = "dlss_fg";
    private static final String KEY_DLSS_FG_FRAMES = "dlss_fg_frames";

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
    /** DLSS 超分开光：0 = 关闭，1 = 开启。 */
    private int dlssSr = 0;
    /** DLSS 质量档位：0=DLAA, 1=Quality, 2=Balanced, 3=Performance, 4=UltraPerformance, 5=Custom。 */
    private int dlssQuality = 1;
    /** DLSS 渲染预设：0=Default, 10=J, 11=K, 12=L, 13=M。 */
    private int dlssPreset = 11;
    /** DLSS 帧生成开关：0 = 关闭，1 = 开启。 */
    private int dlssFg = 0;
    /** 每个真实帧额外生成的帧数：1=2x, 2=3x, 3=4x。 */
    private int dlssFgFrames = 1;

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

    public int getDlssSr() { return dlssSr; }

    public void setDlssSr(int enabled) {
        this.dlssSr = enabled == 1 ? 1 : 0;
        props.setProperty(KEY_DLSS_SR, String.valueOf(this.dlssSr));
        save();
    }

    public int getDlssQuality() { return dlssQuality; }

    public void setDlssQuality(int quality) {
        this.dlssQuality = Math.max(0, Math.min(5, quality));
        props.setProperty(KEY_DLSS_QUALITY, String.valueOf(this.dlssQuality));
        save();
    }

    public int getDlssPreset() { return dlssPreset; }

    public void setDlssPreset(int preset) {
        // 只接受 NGX 当前支持的预设值；其他值回落到 K。
        if (preset != 0 && preset != 10 && preset != 11 && preset != 12 && preset != 13) {
            preset = 11;
        }
        this.dlssPreset = preset;
        props.setProperty(KEY_DLSS_PRESET, String.valueOf(this.dlssPreset));
        save();
    }

    public int getDlssFg() { return dlssFg; }

    public void setDlssFg(int enabled) {
        this.dlssFg = enabled == 1 ? 1 : 0;
        props.setProperty(KEY_DLSS_FG, String.valueOf(this.dlssFg));
        save();
    }

    public int getDlssFgFrames() { return dlssFgFrames; }

    public void setDlssFgFrames(int frames) {
        this.dlssFgFrames = Math.max(1, Math.min(3, frames));
        props.setProperty(KEY_DLSS_FG_FRAMES, String.valueOf(this.dlssFgFrames));
        save();
    }

    /**
     * 实际内部渲染比例。DLSS 超分开启时由质量档位决定（Quality 67% 等）；
     * Custom 档复用 {@code render_scale}。DLSS 关闭时与旧行为完全一致。
     */
    public int getEffectiveRenderScale() {
        if (this.dlssSr != 1) {
            return this.renderScale;
        }
        switch (this.dlssQuality) {
            case 0: return 100; // DLAA
            case 1: return 67;  // Quality
            case 2: return 58;  // Balanced
            case 3: return 50;  // Performance
            case 4: return 33;  // Ultra Performance
            default: return this.renderScale; // Custom
        }
    }

    /** DLSS-G 开启时自研插帧必须让位，避免两套帧生成同时 Present。 */
    public boolean isCustomFrameGenActive() {
        return this.frameGen == 1 && this.dlssFg != 1;
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
                dlssSr = parseInt(props.getProperty(KEY_DLSS_SR, "0"), 0) == 1 ? 1 : 0;
                dlssQuality = Math.max(0, Math.min(5,
                    parseInt(props.getProperty(KEY_DLSS_QUALITY, "1"), 1)));
                dlssPreset = parseInt(props.getProperty(KEY_DLSS_PRESET, "11"), 11);
                if (dlssPreset != 0 && dlssPreset != 10 && dlssPreset != 11
                    && dlssPreset != 12 && dlssPreset != 13) {
                    dlssPreset = 11;
                }
                dlssFg = parseInt(props.getProperty(KEY_DLSS_FG, "0"), 0) == 1 ? 1 : 0;
                dlssFgFrames = Math.max(1, Math.min(3,
                    parseInt(props.getProperty(KEY_DLSS_FG_FRAMES, "1"), 1)));
                LOGGER.info("[dx12-wm] Config loaded: aa_mode={} render_scale={} upscale_mode={} frame_gen={} ngx={} dlss_sr={} dlss_quality={} dlss_fg={}",
                    aaMode, renderScale, upscaleMode, frameGen,
                    ngxAppId.isEmpty() && ngxProjectId.isEmpty() ? "off" : "configured",
                    dlssSr, dlssQuality, dlssFg);
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
                props.setProperty(KEY_DLSS_SR, String.valueOf(dlssSr));
                props.setProperty(KEY_DLSS_QUALITY, String.valueOf(dlssQuality));
                props.setProperty(KEY_DLSS_PRESET, String.valueOf(dlssPreset));
                props.setProperty(KEY_DLSS_FG, String.valueOf(dlssFg));
                props.setProperty(KEY_DLSS_FG_FRAMES, String.valueOf(dlssFgFrames));
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
