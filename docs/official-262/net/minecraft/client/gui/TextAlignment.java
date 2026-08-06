/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

@Environment(value=EnvType.CLIENT)
public enum TextAlignment {
    LEFT{

        @Override
        public int calculateLeft(int anchor, int width) {
            return anchor;
        }

        @Override
        public int calculateLeft(int anchor, Font font, FormattedCharSequence text) {
            return anchor;
        }
    }
    ,
    CENTER{

        @Override
        public int calculateLeft(int anchor, int width) {
            return anchor - width / 2;
        }
    }
    ,
    RIGHT{

        @Override
        public int calculateLeft(int anchor, int width) {
            return anchor - width;
        }
    };


    public abstract int calculateLeft(int var1, int var2);

    public int calculateLeft(int anchor, Font font, FormattedCharSequence text) {
        return this.calculateLeft(anchor, font.width(text));
    }
}

