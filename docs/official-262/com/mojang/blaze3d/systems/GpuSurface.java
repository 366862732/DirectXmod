/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.systems.SurfaceException;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Collection;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GpuSurface
implements AutoCloseable {
    private final GpuSurfaceBackend backend;
    private boolean hasImageAcquired = false;
    private boolean hasBlittedTexture = false;
    private Optional<Configuration> currentConfiguration = Optional.empty();

    public GpuSurface(GpuSurfaceBackend backend) {
        this.backend = backend;
    }

    @Override
    public void close() {
        if (this.hasImageAcquired) {
            throw new IllegalStateException("Cannot close a surface while it is acquired");
        }
        this.backend.close();
    }

    public void configure(Configuration config) throws SurfaceException {
        if (this.hasImageAcquired) {
            throw new IllegalStateException("Cannot configure a surface while it is acquired");
        }
        if (!this.supportedPresentModes().contains((Object)config.presentMode())) {
            throw new SurfaceException("Surface does not support present mode " + String.valueOf((Object)config.presentMode()) + " (supported: " + String.valueOf(this.supportedPresentModes()) + ")");
        }
        this.backend.configure(config);
        this.currentConfiguration = Optional.of(config);
    }

    public Optional<Configuration> currentConfiguration() {
        return this.currentConfiguration;
    }

    public Collection<PresentMode> supportedPresentModes() {
        return this.backend.supportedPresentModes();
    }

    public boolean isSuboptimal() {
        return this.backend.isSuboptimal();
    }

    public boolean isAcquired() {
        return this.hasImageAcquired;
    }

    public void acquireNextTexture() throws SurfaceException {
        if (this.hasImageAcquired) {
            throw new IllegalStateException("Cannot acquire a surface while it is already acquired");
        }
        if (this.currentConfiguration.isEmpty()) {
            throw new IllegalStateException("Cannot acquire an unconfigured surface");
        }
        this.backend.acquireNextTexture();
        this.hasImageAcquired = true;
        this.hasBlittedTexture = false;
    }

    public void blitFromTexture(CommandEncoder commandEncoder, GpuTextureView textureView) {
        if (commandEncoder.isInRenderPass()) {
            throw new IllegalStateException("Close the existing render pass before presenting with a command encoder");
        }
        if (!textureView.texture().getFormat().hasColorAspect()) {
            throw new IllegalStateException("Cannot present a non-color texture!");
        }
        if ((textureView.texture().usage() & 2) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_SRC to presented to the screen");
        }
        if (textureView.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
        }
        if (!this.hasImageAcquired) {
            throw new IllegalStateException("Cannot present to an unacquired surface");
        }
        if (this.hasBlittedTexture) {
            throw new IllegalStateException("Already blitted to this frame!");
        }
        this.backend.blitFromTexture(commandEncoder.backend(), textureView);
        this.hasBlittedTexture = true;
    }

    public void present() {
        if (!this.hasImageAcquired) {
            throw new IllegalStateException("Cannot present to a surface if it isn't acquired");
        }
        if (!this.hasBlittedTexture) {
            throw new IllegalStateException("Must blit to surface before presenting!");
        }
        this.backend.present();
        this.hasImageAcquired = false;
    }

    @Environment(value=EnvType.CLIENT)
    public record Configuration(int width, int height, PresentMode presentMode) {
    }

    @Environment(value=EnvType.CLIENT)
    public static enum PresentMode {
        IMMEDIATE,
        MAILBOX,
        FIFO,
        FIFO_RELAXED;

        private static final PresentMode[] PRESENT_MODES_VSYNC;
        private static final PresentMode[] PRESENT_MODES_NO_VSYNC;

        public static PresentMode getSupportedVsyncMode(Collection<PresentMode> supportedModes, boolean vsync) {
            PresentMode[] preferred;
            for (PresentMode mode : preferred = vsync ? PRESENT_MODES_VSYNC : PRESENT_MODES_NO_VSYNC) {
                if (!supportedModes.contains((Object)mode)) continue;
                return mode;
            }
            throw new IllegalStateException("No supported presentation mode was found");
        }

        static {
            PRESENT_MODES_VSYNC = new PresentMode[]{FIFO_RELAXED, FIFO};
            PRESENT_MODES_NO_VSYNC = new PresentMode[]{IMMEDIATE, MAILBOX, FIFO};
        }
    }
}

