/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.layouts;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface LayoutSettings {
    public LayoutSettings padding(int var1);

    public LayoutSettings padding(int var1, int var2);

    public LayoutSettings padding(int var1, int var2, int var3, int var4);

    public LayoutSettings paddingLeft(int var1);

    public LayoutSettings paddingTop(int var1);

    public LayoutSettings paddingRight(int var1);

    public LayoutSettings paddingBottom(int var1);

    public LayoutSettings paddingHorizontal(int var1);

    public LayoutSettings paddingVertical(int var1);

    public LayoutSettings align(float var1, float var2);

    public LayoutSettings alignHorizontally(float var1);

    public LayoutSettings alignVertically(float var1);

    default public LayoutSettings alignHorizontallyLeft() {
        return this.alignHorizontally(0.0f);
    }

    default public LayoutSettings alignHorizontallyCenter() {
        return this.alignHorizontally(0.5f);
    }

    default public LayoutSettings alignHorizontallyRight() {
        return this.alignHorizontally(1.0f);
    }

    default public LayoutSettings alignVerticallyTop() {
        return this.alignVertically(0.0f);
    }

    default public LayoutSettings alignVerticallyMiddle() {
        return this.alignVertically(0.5f);
    }

    default public LayoutSettings alignVerticallyBottom() {
        return this.alignVertically(1.0f);
    }

    public LayoutSettings copy();

    public LayoutSettingsImpl getExposed();

    public static LayoutSettings defaults() {
        return new LayoutSettingsImpl();
    }

    @Environment(value=EnvType.CLIENT)
    public static class LayoutSettingsImpl
    implements LayoutSettings {
        public int paddingLeft;
        public int paddingTop;
        public int paddingRight;
        public int paddingBottom;
        public float xAlignment;
        public float yAlignment;

        public LayoutSettingsImpl() {
        }

        public LayoutSettingsImpl(LayoutSettingsImpl copy) {
            this.paddingLeft = copy.paddingLeft;
            this.paddingTop = copy.paddingTop;
            this.paddingRight = copy.paddingRight;
            this.paddingBottom = copy.paddingBottom;
            this.xAlignment = copy.xAlignment;
            this.yAlignment = copy.yAlignment;
        }

        @Override
        public LayoutSettingsImpl padding(int padding) {
            return this.padding(padding, padding);
        }

        @Override
        public LayoutSettingsImpl padding(int horizontal, int vertical) {
            return this.paddingHorizontal(horizontal).paddingVertical(vertical);
        }

        @Override
        public LayoutSettingsImpl padding(int left, int top, int right, int bottom) {
            return this.paddingLeft(left).paddingRight(right).paddingTop(top).paddingBottom(bottom);
        }

        @Override
        public LayoutSettingsImpl paddingLeft(int padding) {
            this.paddingLeft = padding;
            return this;
        }

        @Override
        public LayoutSettingsImpl paddingTop(int padding) {
            this.paddingTop = padding;
            return this;
        }

        @Override
        public LayoutSettingsImpl paddingRight(int padding) {
            this.paddingRight = padding;
            return this;
        }

        @Override
        public LayoutSettingsImpl paddingBottom(int padding) {
            this.paddingBottom = padding;
            return this;
        }

        @Override
        public LayoutSettingsImpl paddingHorizontal(int padding) {
            return this.paddingLeft(padding).paddingRight(padding);
        }

        @Override
        public LayoutSettingsImpl paddingVertical(int padding) {
            return this.paddingTop(padding).paddingBottom(padding);
        }

        @Override
        public LayoutSettingsImpl align(float xAlignment, float yAlignment) {
            this.xAlignment = xAlignment;
            this.yAlignment = yAlignment;
            return this;
        }

        @Override
        public LayoutSettingsImpl alignHorizontally(float xAlignment) {
            this.xAlignment = xAlignment;
            return this;
        }

        @Override
        public LayoutSettingsImpl alignVertically(float yAlignment) {
            this.yAlignment = yAlignment;
            return this;
        }

        @Override
        public LayoutSettingsImpl copy() {
            return new LayoutSettingsImpl(this);
        }

        @Override
        public LayoutSettingsImpl getExposed() {
            return this;
        }
    }
}

