/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 *  org.lwjgl.glfw.Callbacks
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWErrorCallback
 *  org.lwjgl.glfw.GLFWErrorCallbackI
 *  org.lwjgl.glfw.GLFWImage
 *  org.lwjgl.glfw.GLFWImage$Buffer
 *  org.lwjgl.glfw.GLFWWindowCloseCallback
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.GLFWErrorCapture;
import com.mojang.blaze3d.GLFWErrorScope;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.platform.MessageBox;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.MonitorManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.systems.BackendCreationException;
import com.mojang.blaze3d.systems.GpuBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public final class Window
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int BASE_WIDTH = 320;
    public static final int BASE_HEIGHT = 240;
    private final GLFWErrorCallback defaultErrorCallback = GLFWErrorCallback.create(this::defaultErrorCallback);
    private final WindowEventHandler eventHandler;
    private final MonitorManager monitorManager;
    private final long handle;
    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;
    private Optional<VideoMode> preferredFullscreenVideoMode;
    private boolean fullscreen;
    private boolean actuallyFullscreen;
    private int x;
    private int y;
    private int width;
    private int height;
    private int framebufferWidth;
    private int framebufferHeight;
    private int guiScaledWidth;
    private int guiScaledHeight;
    private int guiScale;
    private String errorSection = "";
    private boolean dirty;
    private boolean vsync;
    private boolean iconified;
    private boolean focused = true;
    private boolean minimized;
    private boolean allowCursorChanges;
    private CursorType currentCursor = CursorType.DEFAULT;
    private final boolean exclusiveFullscreen;
    private final GpuBackend backend;

    public Window(WindowEventHandler eventHandler, DisplayData displayData, @Nullable String fullscreenVideoModeString, boolean exclusiveFullscreen, String title, MonitorManager monitorManager, GpuBackend backend) throws BackendCreationException {
        this.monitorManager = monitorManager;
        this.exclusiveFullscreen = exclusiveFullscreen;
        this.setBootErrorCallback();
        this.setErrorSection("Pre startup");
        this.eventHandler = eventHandler;
        Optional<VideoMode> optionsMode = VideoMode.read(fullscreenVideoModeString);
        this.preferredFullscreenVideoMode = optionsMode.isPresent() ? optionsMode : (displayData.fullscreenWidth().isPresent() && displayData.fullscreenHeight().isPresent() ? Optional.of(new VideoMode(displayData.fullscreenWidth().getAsInt(), displayData.fullscreenHeight().getAsInt(), 8, 8, 8, 60)) : Optional.empty());
        this.actuallyFullscreen = this.fullscreen = displayData.isFullscreen();
        Monitor initialMonitor = monitorManager.getMonitor(GLFW.glfwGetPrimaryMonitor());
        this.windowedWidth = this.width = Window.allowedWindowMinSize(displayData.width());
        this.windowedHeight = this.height = Window.allowedWindowMinSize(displayData.height());
        this.handle = this.createWindow(backend, this.width, this.height, title, this.fullscreen && initialMonitor != null ? initialMonitor.monitor() : 0L);
        this.backend = backend;
        if (initialMonitor != null) {
            VideoMode mode = initialMonitor.getPreferredVidMode(this.fullscreen ? this.preferredFullscreenVideoMode : Optional.empty());
            this.windowedX = this.x = initialMonitor.x() + mode.getWidth() / 2 - this.width / 2;
            this.windowedY = this.y = initialMonitor.y() + mode.getHeight() / 2 - this.height / 2;
        } else {
            int[] actualX = new int[1];
            int[] actualY = new int[1];
            GLFW.glfwGetWindowPos((long)this.handle, (int[])actualX, (int[])actualY);
            this.windowedX = this.x = actualX[0];
            this.windowedY = this.y = actualY[0];
        }
        this.setMode();
        this.refreshFramebufferSize();
        GLFW.glfwSetFramebufferSizeCallback((long)this.handle, this::onFramebufferResize);
        GLFW.glfwSetWindowPosCallback((long)this.handle, this::onMove);
        GLFW.glfwSetWindowSizeCallback((long)this.handle, this::onResize);
        GLFW.glfwSetWindowFocusCallback((long)this.handle, this::onFocus);
        GLFW.glfwSetCursorEnterCallback((long)this.handle, this::onEnter);
        GLFW.glfwSetWindowIconifyCallback((long)this.handle, this::onIconify);
    }

    public static long createGlfwWindow(int width, int height, String title, long monitor, GpuBackend backend) throws BackendCreationException {
        long windowHandle;
        GLFWErrorCapture glfwErrors = new GLFWErrorCapture();
        try (GLFWErrorScope gLFWErrorScope = new GLFWErrorScope(glfwErrors);){
            backend.setWindowHints();
            GLFW.glfwWindowHint((int)131076, (int)0);
            windowHandle = GLFW.glfwCreateWindow((int)width, (int)height, (CharSequence)title, (long)monitor, (long)0L);
            if (windowHandle == 0L) {
                backend.handleWindowCreationErrors(glfwErrors.firstError());
            }
        }
        for (GLFWErrorCapture.Error error : glfwErrors) {
            LOGGER.error("GLFW error collected during GL backend initialization: {}", (Object)error);
        }
        return windowHandle;
    }

    private long createWindow(GpuBackend backend, int width, int height, String title, long initialMonitor) throws BackendCreationException {
        return Window.createGlfwWindow(width, height, title, initialMonitor, backend);
    }

    public static String getPlatform() {
        int platform = GLX.getGlfwPlatform();
        return switch (platform) {
            case 0 -> "<error>";
            case 393217 -> "win32";
            case 393218 -> "cocoa";
            case 393219 -> "wayland";
            case 393220 -> "x11";
            case 393221 -> "null";
            default -> String.format(Locale.ROOT, "unknown (%08X)", platform);
        };
    }

    public int getRefreshRate() {
        RenderSystem.assertOnRenderThread();
        return GLX._getRefreshRate(this);
    }

    public boolean shouldClose() {
        return GLX._shouldClose(this);
    }

    public static void checkGlfwError(BiConsumer<Integer, String> errorConsumer) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            PointerBuffer errorDescription = stack.mallocPointer(1);
            int errorCode = GLFW.glfwGetError((PointerBuffer)errorDescription);
            if (errorCode != 0) {
                long errorDescriptionAddress = errorDescription.get();
                String errorMessage = errorDescriptionAddress == 0L ? "" : MemoryUtil.memUTF8((long)errorDescriptionAddress);
                errorConsumer.accept(errorCode, errorMessage);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setIcon(PackResources resources, IconSet iconSet) throws IOException {
        int platform = GLX.getGlfwPlatform();
        switch (platform) {
            case 393217: 
            case 393220: {
                List<IoSupplier<InputStream>> iconStreams = iconSet.getStandardIcons(resources);
                ArrayList<ByteBuffer> allocatedBuffers = new ArrayList<ByteBuffer>(iconStreams.size());
                try (MemoryStack stack = MemoryStack.stackPush();){
                    GLFWImage.Buffer icons = GLFWImage.malloc((int)iconStreams.size(), (MemoryStack)stack);
                    for (int i = 0; i < iconStreams.size(); ++i) {
                        try (NativeImage image = NativeImage.read(iconStreams.get(i).get());){
                            ByteBuffer pixels = MemoryUtil.memAlloc((int)(image.getWidth() * image.getHeight() * 4));
                            allocatedBuffers.add(pixels);
                            pixels.asIntBuffer().put(image.getPixelsABGR());
                            icons.position(i);
                            icons.width(image.getWidth());
                            icons.height(image.getHeight());
                            icons.pixels(pixels);
                            continue;
                        }
                    }
                    GLFW.glfwSetWindowIcon((long)this.handle, (GLFWImage.Buffer)((GLFWImage.Buffer)icons.position(0)));
                    break;
                }
                finally {
                    allocatedBuffers.forEach(MemoryUtil::memFree);
                }
            }
            case 393218: {
                MacosUtil.loadIcon(iconSet.getMacIcon(resources));
                break;
            }
            case 393219: 
            case 393221: {
                break;
            }
            default: {
                LOGGER.warn("Not setting icon for unrecognized platform: {}", (Object)platform);
            }
        }
    }

    public void setErrorSection(String string) {
        this.errorSection = string;
    }

    private void setBootErrorCallback() {
        GLFW.glfwSetErrorCallback(Window::bootCrash);
    }

    private static void bootCrash(int error, long description) {
        String message = "GLFW error " + error + ": " + MemoryUtil.memUTF8((long)description);
        MessageBox.error(message + ".\n\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).");
        throw new WindowInitFailed(message);
    }

    public void defaultErrorCallback(int errorCode, long description) {
        if (!RenderSystem.isOnRenderThread()) {
            LOGGER.error("Error callback from invalid thread ({})", (Object)Thread.currentThread().getName());
        }
        String errorString = MemoryUtil.memUTF8((long)description);
        LOGGER.error("########## GL ERROR ##########");
        LOGGER.error("@ {}", (Object)this.errorSection);
        LOGGER.error("{}: {}", (Object)errorCode, (Object)errorString);
    }

    public void setDefaultErrorCallback() {
        GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback((GLFWErrorCallbackI)this.defaultErrorCallback);
        if (previousCallback != null) {
            previousCallback.free();
        }
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        Callbacks.glfwFreeCallbacks((long)this.handle);
        GLFW.glfwSetErrorCallback(null);
        this.defaultErrorCallback.close();
        GLFW.glfwDestroyWindow((long)this.handle);
    }

    private void onMove(long handle, int x, int y) {
        this.x = x;
        this.y = y;
    }

    private boolean isSoftScreen() {
        return Util.getPlatform() == Util.OS.WINDOWS && this.fullscreen && !this.exclusiveFullscreen;
    }

    private void onFramebufferResize(long handle, int newWidth, int newHeight) {
        int newFrameBufferHeight;
        if (handle != this.handle) {
            return;
        }
        int oldWidth = this.getWidth();
        int oldHeight = this.getHeight();
        if (newWidth == 0 || newHeight == 0) {
            this.minimized = true;
            return;
        }
        int n = newFrameBufferHeight = this.isSoftScreen() ? newHeight - 1 : newHeight;
        if (newWidth == oldWidth && newFrameBufferHeight == oldHeight && !this.minimized) {
            return;
        }
        this.minimized = false;
        this.framebufferWidth = newWidth;
        this.framebufferHeight = newFrameBufferHeight;
        try {
            this.eventHandler.framebufferSizeChanged();
        }
        catch (Exception e) {
            CrashReport report = CrashReport.forThrowable(e, "Window resize");
            CrashReportCategory windowSizeDetails = report.addCategory("Window Dimensions");
            windowSizeDetails.setDetail("Old", oldWidth + "x" + oldHeight);
            windowSizeDetails.setDetail("New", newWidth + "x" + newHeight);
            throw new ReportedException(report);
        }
    }

    private void refreshFramebufferSize() {
        int[] outWidth = new int[1];
        int[] outHeight = new int[1];
        GLFW.glfwGetFramebufferSize((long)this.handle, (int[])outWidth, (int[])outHeight);
        outHeight[0] = this.isSoftScreen() ? outHeight[0] - 1 : outHeight[0];
        this.framebufferWidth = outWidth[0] > 0 ? outWidth[0] : 1;
        this.framebufferHeight = outHeight[0] > 0 ? outHeight[0] : 1;
    }

    private void onResize(long handle, int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
    }

    private void onFocus(long handle, boolean focused) {
        if (handle == this.handle) {
            this.focused = focused;
        }
    }

    private void onEnter(long handle, boolean entered) {
        if (entered) {
            this.eventHandler.cursorEntered();
        }
    }

    private void onIconify(long handle, boolean iconified) {
        this.iconified = iconified;
    }

    public void updateFullscreenIfChanged() {
        if (this.fullscreen != this.actuallyFullscreen) {
            this.actuallyFullscreen = this.fullscreen;
            try {
                this.setMode();
                this.eventHandler.framebufferSizeChanged();
            }
            catch (Exception e) {
                LOGGER.error("Couldn't toggle fullscreen", (Throwable)e);
            }
        }
    }

    public Optional<VideoMode> getPreferredFullscreenVideoMode() {
        return this.preferredFullscreenVideoMode;
    }

    public void setPreferredFullscreenVideoMode(Optional<VideoMode> preferredFullscreenVideoMode) {
        boolean changed = !preferredFullscreenVideoMode.equals(this.preferredFullscreenVideoMode);
        this.preferredFullscreenVideoMode = preferredFullscreenVideoMode;
        if (changed) {
            this.dirty = true;
        }
    }

    public void changeFullscreenVideoMode() {
        if (this.fullscreen && this.dirty) {
            this.dirty = false;
            this.setMode();
            this.eventHandler.framebufferSizeChanged();
        }
    }

    private void setMode() {
        boolean wasFullscreen;
        boolean bl = wasFullscreen = GLFW.glfwGetWindowMonitor((long)this.handle) != 0L;
        if (this.fullscreen) {
            Monitor monitor = this.monitorManager.findBestMonitor(this);
            if (monitor == null) {
                LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
                this.fullscreen = false;
            } else {
                if (MacosUtil.IS_MACOS) {
                    MacosUtil.exitNativeFullscreen(this);
                }
                VideoMode mode = monitor.getPreferredVidMode(this.preferredFullscreenVideoMode);
                if (!wasFullscreen) {
                    this.windowedX = this.x;
                    this.windowedY = this.y;
                    this.windowedWidth = Window.allowedWindowMinSize(this.width);
                    this.windowedHeight = Window.allowedWindowMinSize(this.height);
                }
                this.x = 0;
                this.y = 0;
                this.width = Window.allowedWindowMinSize(mode.getWidth());
                this.height = Window.allowedWindowMinSize(mode.getHeight());
                GLFW.glfwSetWindowMonitor((long)this.handle, (long)monitor.monitor(), (int)this.x, (int)this.y, (int)this.width, (int)this.height, (int)mode.getRefreshRate());
                if (MacosUtil.IS_MACOS) {
                    MacosUtil.clearResizableBit(this);
                }
            }
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = Window.allowedWindowMinSize(this.windowedWidth);
            this.height = Window.allowedWindowMinSize(this.windowedHeight);
            GLFW.glfwSetWindowMonitor((long)this.handle, (long)0L, (int)this.x, (int)this.y, (int)this.width, (int)this.height, (int)-1);
        }
    }

    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
    }

    public void setWindowed(int width, int height) {
        this.windowedWidth = Window.allowedWindowMinSize(width);
        this.windowedHeight = Window.allowedWindowMinSize(height);
        this.fullscreen = false;
        this.setMode();
    }

    public int calculateScale(int maxScale, boolean enforceUnicode) {
        int scale;
        for (scale = 1; scale != maxScale && scale < this.framebufferWidth && scale < this.framebufferHeight && this.framebufferWidth / (scale + 1) >= 320 && this.framebufferHeight / (scale + 1) >= 240; ++scale) {
        }
        if (enforceUnicode && scale % 2 != 0) {
            ++scale;
        }
        return scale;
    }

    public void setGuiScale(int guiScale) {
        this.guiScale = guiScale;
        double doubleGuiScale = guiScale;
        int width = (int)((double)this.framebufferWidth / doubleGuiScale);
        this.guiScaledWidth = (double)this.framebufferWidth / doubleGuiScale > (double)width ? width + 1 : width;
        int height = (int)((double)this.framebufferHeight / doubleGuiScale);
        this.guiScaledHeight = (double)this.framebufferHeight / doubleGuiScale > (double)height ? height + 1 : height;
    }

    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle((long)this.handle, (CharSequence)title);
    }

    public long handle() {
        return this.handle;
    }

    public boolean isFullscreen() {
        return this.fullscreen;
    }

    public boolean isIconified() {
        return this.iconified;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public int getWidth() {
        return this.framebufferWidth;
    }

    public int getHeight() {
        return this.framebufferHeight;
    }

    public void setWidth(int width) {
        this.framebufferWidth = width;
    }

    public void setHeight(int height) {
        this.framebufferHeight = height;
    }

    public int getScreenWidth() {
        return this.width;
    }

    public int getScreenHeight() {
        return this.height;
    }

    public int getGuiScaledWidth() {
        return this.guiScaledWidth;
    }

    public int getGuiScaledHeight() {
        return this.guiScaledHeight;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getGuiScale() {
        return this.guiScale;
    }

    public @Nullable Monitor findBestMonitor() {
        return this.monitorManager.findBestMonitor(this);
    }

    public void updateRawMouseInput(boolean value) {
        InputConstants.updateRawMouseInput(this, value);
    }

    public void setWindowCloseCallback(Runnable task) {
        GLFWWindowCloseCallback prev = GLFW.glfwSetWindowCloseCallback((long)this.handle, id -> task.run());
        if (prev != null) {
            prev.free();
        }
    }

    public boolean isMinimized() {
        return this.minimized;
    }

    public void setAllowCursorChanges(boolean value) {
        this.allowCursorChanges = value;
    }

    public void selectCursor(CursorType cursor) {
        CursorType effectiveCursor;
        CursorType cursorType = effectiveCursor = this.allowCursorChanges ? cursor : CursorType.DEFAULT;
        if (this.currentCursor != effectiveCursor) {
            this.currentCursor = effectiveCursor;
            effectiveCursor.select(this);
        }
    }

    public float getAppropriateLineWidth() {
        return Math.max(2.5f, (float)this.getWidth() / 1920.0f * 2.5f);
    }

    public GpuBackend backend() {
        return this.backend;
    }

    private static int allowedWindowMinSize(int size) {
        return Math.max(size, 1);
    }

    @Environment(value=EnvType.CLIENT)
    public static class WindowInitFailed
    extends SilentInitException {
        public WindowInitFailed(String message) {
            super(message);
        }
    }
}

