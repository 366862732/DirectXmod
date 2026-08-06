/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector2i
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.glfw.GLFWDropCallback
 *  org.slf4j.Logger
 */
package net.minecraft.client;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.InputType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class MouseHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final long DOUBLE_CLICK_THRESHOLD_MS = 250L;
    private final Minecraft minecraft;
    private boolean isLeftPressed;
    private boolean isMiddlePressed;
    private boolean isRightPressed;
    private double xpos;
    private double ypos;
    private @Nullable LastClick lastClick;
    protected @MouseButtonInfo.MouseButton int lastClickButton;
    private int fakeRightMouse;
    private @Nullable MouseButtonInfo activeButton = null;
    private boolean ignoreFirstMove = true;
    private double mousePressedTime;
    private final SmoothDouble smoothTurnX = new SmoothDouble();
    private final SmoothDouble smoothTurnY = new SmoothDouble();
    private double accumulatedDX;
    private double accumulatedDY;
    private final ScrollWheelHandler scrollWheelHandler;
    private double lastHandleMovementTime = Double.MIN_VALUE;
    private boolean mouseGrabbed;

    public MouseHandler(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.scrollWheelHandler = new ScrollWheelHandler();
    }

    private void onButton(long handle, MouseButtonInfo rawButtonInfo, @MouseButtonInfo.Action int action) {
        MouseButtonInfo buttonInfo;
        boolean pressed;
        block24: {
            Window window = this.minecraft.getWindow();
            if (handle != window.handle()) {
                return;
            }
            this.minecraft.getFramerateLimitTracker().onInputReceived();
            if (this.minecraft.gui.screen() != null) {
                this.minecraft.setLastInputType(InputType.MOUSE);
            }
            pressed = action == 1;
            buttonInfo = this.simulateRightClick(rawButtonInfo, pressed);
            if (pressed) {
                this.activeButton = buttonInfo;
                this.mousePressedTime = Blaze3D.getTime();
            } else if (this.activeButton != null) {
                this.activeButton = null;
            }
            if (this.minecraft.gui.overlay() == null) {
                if (pressed && this.minecraft.handleGlobalKeyPress(InputConstants.Type.MOUSE.getOrCreate(buttonInfo.button()), buttonInfo.hasControlDownWithQuirk())) {
                    return;
                }
                if (this.minecraft.gui.screen() == null) {
                    if (!this.mouseGrabbed && pressed) {
                        this.grabMouse();
                    }
                } else {
                    double xm = this.getScaledXPos(window);
                    double ym = this.getScaledYPos(window);
                    Screen screen = this.minecraft.gui.screen();
                    MouseButtonEvent event = new MouseButtonEvent(xm, ym, buttonInfo);
                    if (pressed) {
                        screen.afterMouseAction();
                        try {
                            boolean doubleClick;
                            long currentTime = Util.getMillis();
                            boolean bl = doubleClick = this.lastClick != null && currentTime - this.lastClick.time() < 250L && this.lastClick.screen() == screen && this.lastClickButton == event.button();
                            if (screen.mouseClicked(event, doubleClick)) {
                                this.lastClick = new LastClick(currentTime, screen);
                                this.lastClickButton = buttonInfo.button();
                                return;
                            }
                            break block24;
                        }
                        catch (Throwable t) {
                            CrashReport report = CrashReport.forThrowable(t, "mouseClicked event handler");
                            screen.fillCrashDetails(report);
                            CrashReportCategory mouseDetails = report.addCategory("Mouse");
                            this.fillMousePositionDetails(mouseDetails, window);
                            mouseDetails.setDetail("Button", event.button());
                            throw new ReportedException(report);
                        }
                    }
                    try {
                        if (screen.mouseReleased(event)) {
                            return;
                        }
                    }
                    catch (Throwable t) {
                        CrashReport report = CrashReport.forThrowable(t, "mouseReleased event handler");
                        screen.fillCrashDetails(report);
                        CrashReportCategory mouseDetails = report.addCategory("Mouse");
                        this.fillMousePositionDetails(mouseDetails, window);
                        mouseDetails.setDetail("Button", event.button());
                        throw new ReportedException(report);
                    }
                }
            }
        }
        if (this.minecraft.gui.screen() == null && this.minecraft.gui.overlay() == null) {
            if (buttonInfo.button() == 0) {
                this.isLeftPressed = pressed;
            } else if (buttonInfo.button() == 2) {
                this.isMiddlePressed = pressed;
            } else if (buttonInfo.button() == 1) {
                this.isRightPressed = pressed;
            }
            InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(buttonInfo.button());
            KeyMapping.set(mouseKey, pressed);
            if (pressed) {
                KeyMapping.click(mouseKey);
            }
        }
    }

    private MouseButtonInfo simulateRightClick(MouseButtonInfo info, boolean pressed) {
        if (InputQuirks.SIMULATE_RIGHT_CLICK_WITH_LONG_LEFT_CLICK && info.button() == 0) {
            if (pressed) {
                if ((info.modifiers() & 2) == 2) {
                    ++this.fakeRightMouse;
                    return new MouseButtonInfo(1, info.modifiers());
                }
            } else if (this.fakeRightMouse > 0) {
                --this.fakeRightMouse;
                return new MouseButtonInfo(1, info.modifiers());
            }
        }
        return info;
    }

    public void fillMousePositionDetails(CrashReportCategory category, Window window) {
        category.setDetail("Mouse location", () -> String.format(Locale.ROOT, "Scaled: (%f, %f). Absolute: (%f, %f)", MouseHandler.getScaledXPos(window, this.xpos), MouseHandler.getScaledYPos(window, this.ypos), this.xpos, this.ypos));
        category.setDetail("Screen size", () -> String.format(Locale.ROOT, "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d", window.getGuiScaledWidth(), window.getGuiScaledHeight(), window.getWidth(), window.getHeight(), window.getGuiScale()));
    }

    private void onScroll(long handle, double xoffset, double yoffset) {
        if (handle == this.minecraft.getWindow().handle()) {
            this.minecraft.getFramerateLimitTracker().onInputReceived();
            boolean discreteScroll = this.minecraft.options.discreteMouseScroll().get();
            double scrollSensitivity = this.minecraft.options.mouseWheelSensitivity().get();
            double scaledXOffset = (discreteScroll ? Math.signum(xoffset) : xoffset) * scrollSensitivity;
            double scaledYOffset = (discreteScroll ? Math.signum(yoffset) : yoffset) * scrollSensitivity;
            if (this.minecraft.gui.overlay() == null) {
                if (this.minecraft.gui.screen() != null) {
                    double xm = this.getScaledXPos(this.minecraft.getWindow());
                    double ym = this.getScaledYPos(this.minecraft.getWindow());
                    this.minecraft.gui.screen().mouseScrolled(xm, ym, scaledXOffset, scaledYOffset);
                    this.minecraft.gui.screen().afterMouseAction();
                } else if (this.minecraft.player != null) {
                    int wheel;
                    Vector2i wheelXY = this.scrollWheelHandler.onMouseScroll(scaledXOffset, scaledYOffset);
                    if (wheelXY.x == 0 && wheelXY.y == 0) {
                        return;
                    }
                    int n = wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
                    if (this.minecraft.player.isSpectator()) {
                        if (this.minecraft.gui.hud.getSpectatorGui().isMenuActive()) {
                            this.minecraft.gui.hud.getSpectatorGui().onMouseScrolled(-wheel);
                        } else {
                            float speed = Mth.clamp(this.minecraft.player.getAbilities().getFlyingSpeed() + (float)wheelXY.y * 0.005f, 0.0f, 0.2f);
                            this.minecraft.player.getAbilities().setFlyingSpeed(speed);
                        }
                    } else {
                        Inventory inventory = this.minecraft.player.getInventory();
                        inventory.setSelectedSlot(ScrollWheelHandler.getNextScrollWheelSelection(wheel, inventory.getSelectedSlot(), Inventory.getSelectionSize()));
                    }
                }
            }
        }
    }

    private void onDrop(long handle, List<Path> files, int failedCount) {
        this.minecraft.getFramerateLimitTracker().onInputReceived();
        if (this.minecraft.gui.screen() != null) {
            this.minecraft.gui.screen().onFilesDrop(files);
        }
        if (failedCount > 0) {
            SystemToast.onFileDropFailure(this.minecraft, failedCount);
        }
    }

    public void setup(Window window) {
        InputConstants.setupMouseCallbacks(window, (window1, xpos, ypos) -> this.minecraft.execute(() -> this.onMove(window1, xpos, ypos)), (window1, button, action, mods) -> {
            MouseButtonInfo buttonInfo = new MouseButtonInfo(button, mods);
            this.minecraft.execute(() -> this.onButton(window1, buttonInfo, action));
        }, (window1, xoffset, yoffset) -> this.minecraft.execute(() -> this.onScroll(window1, xoffset, yoffset)), (window1, count, namesPtr) -> {
            ArrayList<Path> names = new ArrayList<Path>(count);
            int failedCount = 0;
            for (int i = 0; i < count; ++i) {
                String name = GLFWDropCallback.getName((long)namesPtr, (int)i);
                try {
                    names.add(Paths.get(name, new String[0]));
                    continue;
                }
                catch (InvalidPathException e) {
                    ++failedCount;
                    LOGGER.error("Failed to parse path '{}'", (Object)name, (Object)e);
                }
            }
            if (!names.isEmpty()) {
                int finalFailedCount = failedCount;
                this.minecraft.execute(() -> this.onDrop(window1, names, finalFailedCount));
            }
        });
    }

    private void onMove(long handle, double xpos, double ypos) {
        if (handle != this.minecraft.getWindow().handle()) {
            return;
        }
        if (this.ignoreFirstMove) {
            this.xpos = xpos;
            this.ypos = ypos;
            this.ignoreFirstMove = false;
            return;
        }
        if (this.minecraft.isWindowActive()) {
            this.accumulatedDX += xpos - this.xpos;
            this.accumulatedDY += ypos - this.ypos;
        }
        this.xpos = xpos;
        this.ypos = ypos;
    }

    public void handleAccumulatedMovement() {
        double time = Blaze3D.getTime();
        double mousea = time - this.lastHandleMovementTime;
        this.lastHandleMovementTime = time;
        if (this.minecraft.isWindowActive()) {
            boolean mouseMoved;
            Screen screen = this.minecraft.gui.screen();
            boolean bl = mouseMoved = this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0;
            if (mouseMoved) {
                this.minecraft.getFramerateLimitTracker().onInputReceived();
            }
            if (screen != null && this.minecraft.gui.overlay() == null && mouseMoved) {
                Window window = this.minecraft.getWindow();
                double xm = this.getScaledXPos(window);
                double ym = this.getScaledYPos(window);
                try {
                    screen.mouseMoved(xm, ym);
                }
                catch (Throwable t) {
                    CrashReport report = CrashReport.forThrowable(t, "mouseMoved event handler");
                    screen.fillCrashDetails(report);
                    CrashReportCategory mouseDetails = report.addCategory("Mouse");
                    this.fillMousePositionDetails(mouseDetails, window);
                    throw new ReportedException(report);
                }
                if (this.activeButton != null && this.mousePressedTime > 0.0) {
                    double dx = MouseHandler.getScaledXPos(window, this.accumulatedDX);
                    double dy = MouseHandler.getScaledYPos(window, this.accumulatedDY);
                    try {
                        screen.mouseDragged(new MouseButtonEvent(xm, ym, this.activeButton), dx, dy);
                    }
                    catch (Throwable t) {
                        CrashReport report = CrashReport.forThrowable(t, "mouseDragged event handler");
                        screen.fillCrashDetails(report);
                        CrashReportCategory mouseDetails = report.addCategory("Mouse");
                        this.fillMousePositionDetails(mouseDetails, window);
                        throw new ReportedException(report);
                    }
                }
                screen.afterMouseMove();
            }
            if (this.isMouseGrabbed() && this.minecraft.player != null) {
                this.turnPlayer(mousea);
            }
        }
        this.accumulatedDX = 0.0;
        this.accumulatedDY = 0.0;
    }

    public static double getScaledXPos(Window window, double x) {
        return x * (double)window.getGuiScaledWidth() / (double)window.getScreenWidth();
    }

    public double getScaledXPos(Window window) {
        return MouseHandler.getScaledXPos(window, this.xpos);
    }

    public static double getScaledYPos(Window window, double y) {
        return y * (double)window.getGuiScaledHeight() / (double)window.getScreenHeight();
    }

    public double getScaledYPos(Window window) {
        return MouseHandler.getScaledYPos(window, this.ypos);
    }

    private void turnPlayer(double mousea) {
        double yo;
        double xo;
        double ss = this.minecraft.options.sensitivity().get() * (double)0.6f + (double)0.2f;
        double sensitivityMod = ss * ss * ss;
        double sens = sensitivityMod * 8.0;
        if (this.minecraft.options.smoothCamera) {
            double dx = this.smoothTurnX.getNewDeltaValue(this.accumulatedDX * sens, mousea * sens);
            double dy = this.smoothTurnY.getNewDeltaValue(this.accumulatedDY * sens, mousea * sens);
            xo = dx;
            yo = dy;
        } else if (this.minecraft.options.getCameraType().isFirstPerson() && this.minecraft.player.isScoping()) {
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
            xo = this.accumulatedDX * sensitivityMod;
            yo = this.accumulatedDY * sensitivityMod;
        } else {
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
            xo = this.accumulatedDX * sens;
            yo = this.accumulatedDY * sens;
        }
        this.minecraft.getTutorial().onMouse(xo, yo);
        if (this.minecraft.player != null) {
            this.minecraft.player.turn(this.minecraft.options.invertMouseX().get() != false ? -xo : xo, this.minecraft.options.invertMouseY().get() != false ? -yo : yo);
        }
    }

    public boolean isLeftPressed() {
        return this.isLeftPressed;
    }

    public boolean isMiddlePressed() {
        return this.isMiddlePressed;
    }

    public boolean isRightPressed() {
        return this.isRightPressed;
    }

    public double xpos() {
        return this.xpos;
    }

    public double ypos() {
        return this.ypos;
    }

    public void setIgnoreFirstMove() {
        this.ignoreFirstMove = true;
    }

    public boolean isMouseGrabbed() {
        return this.mouseGrabbed;
    }

    public void grabMouse() {
        if (!this.minecraft.isWindowActive()) {
            return;
        }
        if (this.mouseGrabbed) {
            return;
        }
        if (InputQuirks.RESTORE_KEY_STATE_AFTER_MOUSE_GRAB) {
            KeyMapping.setAll();
        }
        this.mouseGrabbed = true;
        this.xpos = this.minecraft.getWindow().getScreenWidth() / 2;
        this.ypos = this.minecraft.getWindow().getScreenHeight() / 2;
        InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212995, this.xpos, this.ypos);
        this.minecraft.gui.setScreen(null);
        this.minecraft.missTime = 10000;
        this.ignoreFirstMove = true;
    }

    public void releaseMouse() {
        if (!this.mouseGrabbed) {
            return;
        }
        this.mouseGrabbed = false;
        this.xpos = this.minecraft.getWindow().getScreenWidth() / 2;
        this.ypos = this.minecraft.getWindow().getScreenHeight() / 2;
        InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212993, this.xpos, this.ypos);
    }

    public void cursorEntered() {
        this.ignoreFirstMove = true;
    }

    public void drawDebugMouseInfo(Font font, GuiGraphicsExtractor graphics) {
        Window window = this.minecraft.getWindow();
        double x = this.getScaledXPos(window);
        double y = this.getScaledYPos(window) - 8.0;
        String text = String.format(Locale.ROOT, "%.0f,%.0f", x, y);
        graphics.text(font, text, (int)x, (int)y, -1);
    }

    @Environment(value=EnvType.CLIENT)
    private record LastClick(long time, Screen screen) {
    }
}

