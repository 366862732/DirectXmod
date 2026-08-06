/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class BackendCreationException
extends Exception {
    private final Reason reason;
    private final List<String> missingCapabilities;

    public BackendCreationException(String message, Reason reason, List<String> missingCapabilities) {
        super(message);
        this.reason = reason;
        this.missingCapabilities = List.copyOf(missingCapabilities);
    }

    public BackendCreationException(String message, Reason reason) {
        this(message, reason, List.of());
    }

    public Reason getReason() {
        return this.reason;
    }

    public List<String> getMissingCapabilities() {
        return this.missingCapabilities;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Reason {
        GLFW_ERROR("glfw_error"),
        VULKAN_LOADER_MISSING("vulkan_loader_missing"),
        VULKAN_INSTANCE_CREATION_FAILED("vulkan_instance_creation_failed"),
        VULKAN_NO_DEVICE("vulkan_no_device"),
        VULKAN_DEVICE_VERSION_TOO_LOW("vulkan_device_version_too_low"),
        VULKAN_NO_GRAPHICS_QUEUE("vulkan_no_graphics_queue"),
        VULKAN_MISSING_EXTENSION("vulkan_missing_extension"),
        VULKAN_MISSING_FEATURE("vulkan_missing_feature"),
        OPENGL_MISSING("opengl_missing"),
        OTHER("other");

        private final String displayName;

        private Reason(String key) {
            this.displayName = key;
        }

        public String displayName() {
            return this.displayName;
        }
    }
}

