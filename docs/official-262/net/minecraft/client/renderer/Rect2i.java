/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class Rect2i {
    private int xPos;
    private int yPos;
    private int width;
    private int height;

    public Rect2i(int x, int y, int width, int height) {
        this.xPos = x;
        this.yPos = y;
        this.width = width;
        this.height = height;
    }

    public Rect2i intersect(Rect2i other) {
        int x0 = this.xPos;
        int y0 = this.yPos;
        int x1 = this.xPos + this.width;
        int y1 = this.yPos + this.height;
        int x2 = other.getX();
        int y2 = other.getY();
        int x3 = x2 + other.getWidth();
        int y3 = y2 + other.getHeight();
        this.xPos = Math.max(x0, x2);
        this.yPos = Math.max(y0, y2);
        this.width = Math.max(0, Math.min(x1, x3) - this.xPos);
        this.height = Math.max(0, Math.min(y1, y3) - this.yPos);
        return this;
    }

    public int getX() {
        return this.xPos;
    }

    public int getY() {
        return this.yPos;
    }

    public void setX(int x) {
        this.xPos = x;
    }

    public void setY(int y) {
        this.yPos = y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setPosition(int x, int y) {
        this.xPos = x;
        this.yPos = y;
    }

    public boolean contains(int x, int y) {
        return x >= this.xPos && x <= this.xPos + this.width && y >= this.yPos && y <= this.yPos + this.height;
    }
}

