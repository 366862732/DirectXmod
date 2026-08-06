/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.glfw.GLFW
 */
package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(value=EnvType.CLIENT)
public class TextInputManager {
    private final Window window;
    private boolean textInputEnabled;
    private boolean imeRequested;
    private volatile boolean imeStatusChanged = true;
    private boolean cachedIMEStatus;

    public TextInputManager(Window window) {
        this.window = window;
    }

    public void setTextInputArea(int x0, int y0, int x1, int y1) {
        int guiScale = this.window.getGuiScale();
        GLFW.glfwSetPreeditCursorRectangle((long)this.window.handle(), (int)(x0 * guiScale), (int)(y0 * guiScale), (int)((x1 - x0) * guiScale), (int)((y1 - y0) * guiScale));
    }

    public void notifyIMEChanged() {
        this.imeStatusChanged = true;
    }

    public void tick() {
        if (this.textInputEnabled) {
            this.tickDuringTextInput();
        } else {
            this.tickOutsideTextInput();
        }
    }

    private boolean getIMEStatus() {
        if (this.imeStatusChanged) {
            this.imeStatusChanged = false;
            this.cachedIMEStatus = GLFW.glfwGetInputMode((long)this.window.handle(), (int)208903) == 1;
        }
        return this.cachedIMEStatus;
    }

    private void tickOutsideTextInput() {
        if (this.window.isFocused() && this.getIMEStatus()) {
            this.setIMEInputMode(false);
        }
    }

    private void tickDuringTextInput() {
        this.imeRequested = this.getIMEStatus();
    }

    public void startTextInput() {
        this.textInputEnabled = true;
        if (this.imeRequested) {
            this.setIMEInputMode(true);
        }
    }

    public void stopTextInput() {
        this.textInputEnabled = false;
    }

    public void onTextInputFocusChange(boolean focused) {
        if (focused) {
            this.startTextInput();
        } else {
            this.stopTextInput();
        }
    }

    private void setIMEInputMode(boolean value) {
        GLFW.glfwSetInputMode((long)this.window.handle(), (int)208903, (int)GLX.glfwBool(value));
    }
}

