package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide owner of the D3D11 device used by the 1.21.11 mixins.
 *
 * <p>The vanilla renderer creates its {@code GlDevice} in
 * {@code RenderSystem.initRenderer}.  The D3D11 mixins replace that call and
 * route {@code flipFrame}/{@code updateVsync} here.  Keeping the state in one
 * class also makes the fullscreen/vsync ordering safe: {@code Window.setMode()}
 * can call {@code updateVsync} before the device exists.</p>
 */
public final class Dx11State {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    private static volatile @Nullable Dx11Device device;
    private static volatile @Nullable Boolean pendingVsync;

    private Dx11State() {
    }

    public static boolean isActive() {
        Dx11Device current = device;
        return current != null && !current.isClosed();
    }

    /**
     * Creates the D3D11 device and returns it as the {@code GpuDevice} that
     * {@code RenderSystem.DEVICE} must point at.
     */
    public static GpuDevice init(long windowHandle, @Nullable ShaderSource shaderSource) {
        Dx11Device current = device;
        if (current != null && !current.isClosed()) {
            return current;
        }
        Dx11Device created = Dx11Device.create(windowHandle, shaderSource);
        device = created;
        Boolean vsync = pendingVsync;
        if (vsync != null) {
            created.setVsync(vsync);
        }
        LOGGER.info("[dx11] Device ready: {}", created.getImplementationInformation());
        return created;
    }

    public static void flipFrame(long windowHandle) {
        Dx11Device current = device;
        if (current != null && !current.isClosed()) {
            current.flipFrame(windowHandle);
        }
    }

    public static void setVsync(boolean vsync) {
        pendingVsync = vsync;
        Dx11Device current = device;
        if (current != null && !current.isClosed()) {
            current.setVsync(vsync);
        }
    }

    /** Releases the device if it has not already been closed by Minecraft. */
    public static void close() {
        Dx11Device current = device;
        device = null;
        if (current != null) {
            current.close();
        }
    }
}
