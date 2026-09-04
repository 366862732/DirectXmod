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

    private static Dx12Config instance;
    private final Properties props = new Properties();
    private final Path configPath;
    
    private int aaMode = 1; // default: FXAA

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

    private void load() {
        try {
            if (Files.exists(configPath)) {
                try (var is = Files.newInputStream(configPath)) {
                    props.load(is);
                }
                aaMode = parseInt(props.getProperty(KEY_AA_MODE, "1"), 1);
                LOGGER.info("[dx12-wm] Config loaded: aa_mode={}", aaMode);
            } else {
                LOGGER.info("[dx12-wm] No config file found, using defaults");
                setAaMode(aaMode); // save defaults
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
