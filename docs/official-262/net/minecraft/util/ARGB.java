/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package net.minecraft.util;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ARGB {
    private static final int LINEAR_CHANNEL_DEPTH = 1024;
    private static final short[] SRGB_TO_LINEAR = Util.make(new short[256], lookup -> {
        for (int i = 0; i < ((short[])lookup).length; ++i) {
            float channel = (float)i / 255.0f;
            lookup[i] = (short)Math.round(ARGB.computeSrgbToLinear(channel) * 1023.0f);
        }
    });
    private static final byte[] LINEAR_TO_SRGB = Util.make(new byte[1024], lookup -> {
        for (int i = 0; i < ((byte[])lookup).length; ++i) {
            float channel = (float)i / 1023.0f;
            lookup[i] = (byte)Math.round(ARGB.computeLinearToSrgb(channel) * 255.0f);
        }
    });

    private static float computeSrgbToLinear(float x) {
        if (x >= 0.04045f) {
            return (float)Math.pow(((double)x + 0.055) / 1.055, 2.4);
        }
        return x / 12.92f;
    }

    private static float computeLinearToSrgb(float x) {
        if (x >= 0.0031308f) {
            return (float)(1.055 * Math.pow(x, 0.4166666666666667) - 0.055);
        }
        return 12.92f * x;
    }

    public static float srgbToLinearChannel(int srgb) {
        return (float)SRGB_TO_LINEAR[srgb] / 1023.0f;
    }

    public static int linearToSrgbChannel(float linear) {
        return LINEAR_TO_SRGB[Mth.floor(linear * 1023.0f)] & 0xFF;
    }

    public static int meanLinear(int srgb1, int srgb2, int srgb3, int srgb4) {
        return ARGB.color((ARGB.alpha(srgb1) + ARGB.alpha(srgb2) + ARGB.alpha(srgb3) + ARGB.alpha(srgb4)) / 4, ARGB.linearChannelMean(ARGB.red(srgb1), ARGB.red(srgb2), ARGB.red(srgb3), ARGB.red(srgb4)), ARGB.linearChannelMean(ARGB.green(srgb1), ARGB.green(srgb2), ARGB.green(srgb3), ARGB.green(srgb4)), ARGB.linearChannelMean(ARGB.blue(srgb1), ARGB.blue(srgb2), ARGB.blue(srgb3), ARGB.blue(srgb4)));
    }

    private static int linearChannelMean(int c1, int c2, int c3, int c4) {
        int linear = (SRGB_TO_LINEAR[c1] + SRGB_TO_LINEAR[c2] + SRGB_TO_LINEAR[c3] + SRGB_TO_LINEAR[c4]) / 4;
        return LINEAR_TO_SRGB[linear] & 0xFF;
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    public static int color(int red, int green, int blue) {
        return ARGB.color(255, red, green, blue);
    }

    public static int color(Vec3 vec) {
        return ARGB.color(ARGB.as8BitChannel((float)vec.x()), ARGB.as8BitChannel((float)vec.y()), ARGB.as8BitChannel((float)vec.z()));
    }

    public static int multiply(int lhs, int rhs) {
        if (lhs == -1) {
            return rhs;
        }
        if (rhs == -1) {
            return lhs;
        }
        return ARGB.color(ARGB.alpha(lhs) * ARGB.alpha(rhs) / 255, ARGB.red(lhs) * ARGB.red(rhs) / 255, ARGB.green(lhs) * ARGB.green(rhs) / 255, ARGB.blue(lhs) * ARGB.blue(rhs) / 255);
    }

    public static int addRgb(int lhs, int rhs) {
        return ARGB.color(ARGB.alpha(lhs), Math.min(ARGB.red(lhs) + ARGB.red(rhs), 255), Math.min(ARGB.green(lhs) + ARGB.green(rhs), 255), Math.min(ARGB.blue(lhs) + ARGB.blue(rhs), 255));
    }

    public static int subtractRgb(int lhs, int rhs) {
        return ARGB.color(ARGB.alpha(lhs), Math.max(ARGB.red(lhs) - ARGB.red(rhs), 0), Math.max(ARGB.green(lhs) - ARGB.green(rhs), 0), Math.max(ARGB.blue(lhs) - ARGB.blue(rhs), 0));
    }

    public static int multiplyAlpha(int color, float alphaMultiplier) {
        if (color == 0 || alphaMultiplier <= 0.0f) {
            return 0;
        }
        if (alphaMultiplier >= 1.0f) {
            return color;
        }
        return ARGB.color(ARGB.alphaFloat(color) * alphaMultiplier, color);
    }

    public static int scaleRGB(int color, float scale) {
        return ARGB.scaleRGB(color, scale, scale, scale);
    }

    public static int scaleRGB(int color, float scaleR, float scaleG, float scaleB) {
        return ARGB.color(ARGB.alpha(color), Math.clamp((long)((int)((float)ARGB.red(color) * scaleR)), 0, 255), Math.clamp((long)((int)((float)ARGB.green(color) * scaleG)), 0, 255), Math.clamp((long)((int)((float)ARGB.blue(color) * scaleB)), 0, 255));
    }

    public static int scaleRGB(int color, int scale) {
        return ARGB.color(ARGB.alpha(color), Math.clamp((long)ARGB.red(color) * (long)scale / 255L, 0, 255), Math.clamp((long)ARGB.green(color) * (long)scale / 255L, 0, 255), Math.clamp((long)ARGB.blue(color) * (long)scale / 255L, 0, 255));
    }

    public static int greyscale(int color) {
        int greyscale = (int)((float)ARGB.red(color) * 0.3f + (float)ARGB.green(color) * 0.59f + (float)ARGB.blue(color) * 0.11f);
        return ARGB.color(ARGB.alpha(color), greyscale, greyscale, greyscale);
    }

    public static int alphaBlend(int destination, int source) {
        int destinationAlpha = ARGB.alpha(destination);
        int sourceAlpha = ARGB.alpha(source);
        if (sourceAlpha == 255) {
            return source;
        }
        if (sourceAlpha == 0) {
            return destination;
        }
        int alpha = sourceAlpha + destinationAlpha * (255 - sourceAlpha) / 255;
        return ARGB.color(alpha, ARGB.alphaBlendChannel(alpha, sourceAlpha, ARGB.red(destination), ARGB.red(source)), ARGB.alphaBlendChannel(alpha, sourceAlpha, ARGB.green(destination), ARGB.green(source)), ARGB.alphaBlendChannel(alpha, sourceAlpha, ARGB.blue(destination), ARGB.blue(source)));
    }

    private static int alphaBlendChannel(int resultAlpha, int sourceAlpha, int destination, int source) {
        return (source * sourceAlpha + destination * (resultAlpha - sourceAlpha)) / resultAlpha;
    }

    public static int srgbLerp(float alpha, int p0, int p1) {
        int a = Mth.lerpInt(alpha, ARGB.alpha(p0), ARGB.alpha(p1));
        int red = Mth.lerpInt(alpha, ARGB.red(p0), ARGB.red(p1));
        int green = Mth.lerpInt(alpha, ARGB.green(p0), ARGB.green(p1));
        int blue = Mth.lerpInt(alpha, ARGB.blue(p0), ARGB.blue(p1));
        return ARGB.color(a, red, green, blue);
    }

    public static int linearLerp(float alpha, int p0, int p1) {
        return ARGB.color(Mth.lerpInt(alpha, ARGB.alpha(p0), ARGB.alpha(p1)), LINEAR_TO_SRGB[Mth.lerpInt(alpha, SRGB_TO_LINEAR[ARGB.red(p0)], SRGB_TO_LINEAR[ARGB.red(p1)])] & 0xFF, LINEAR_TO_SRGB[Mth.lerpInt(alpha, SRGB_TO_LINEAR[ARGB.green(p0)], SRGB_TO_LINEAR[ARGB.green(p1)])] & 0xFF, LINEAR_TO_SRGB[Mth.lerpInt(alpha, SRGB_TO_LINEAR[ARGB.blue(p0)], SRGB_TO_LINEAR[ARGB.blue(p1)])] & 0xFF);
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static int transparent(int color) {
        return color & 0xFFFFFF;
    }

    public static int color(int alpha, int rgb) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    public static int color(float alpha, int rgb) {
        return ARGB.as8BitChannel(alpha) << 24 | rgb & 0xFFFFFF;
    }

    public static int white(float alpha) {
        return ARGB.as8BitChannel(alpha) << 24 | 0xFFFFFF;
    }

    public static int white(int alpha) {
        return alpha << 24 | 0xFFFFFF;
    }

    public static int black(float alpha) {
        return ARGB.as8BitChannel(alpha) << 24;
    }

    public static int black(int alpha) {
        return alpha << 24;
    }

    public static int gray(float brightness) {
        int channel = ARGB.as8BitChannel(brightness);
        return ARGB.color(channel, channel, channel);
    }

    public static int colorFromFloat(float alpha, float red, float green, float blue) {
        return ARGB.color(ARGB.as8BitChannel(alpha), ARGB.as8BitChannel(red), ARGB.as8BitChannel(green), ARGB.as8BitChannel(blue));
    }

    public static Vector3f vector3fFromRGB24(int color) {
        return new Vector3f(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color));
    }

    public static Vector4f vector4fFromARGB32(int color) {
        return new Vector4f(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
    }

    public static Vector4f setVector4fFromARGB32(Vector4f dest, int color) {
        return dest.set(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
    }

    public static int average(int lhs, int rhs) {
        return ARGB.color((ARGB.alpha(lhs) + ARGB.alpha(rhs)) / 2, (ARGB.red(lhs) + ARGB.red(rhs)) / 2, (ARGB.green(lhs) + ARGB.green(rhs)) / 2, (ARGB.blue(lhs) + ARGB.blue(rhs)) / 2);
    }

    public static int as8BitChannel(float value) {
        return Mth.floor(value * 255.0f);
    }

    public static float alphaFloat(int color) {
        return ARGB.from8BitChannel(ARGB.alpha(color));
    }

    public static float redFloat(int color) {
        return ARGB.from8BitChannel(ARGB.red(color));
    }

    public static float greenFloat(int color) {
        return ARGB.from8BitChannel(ARGB.green(color));
    }

    public static float blueFloat(int color) {
        return ARGB.from8BitChannel(ARGB.blue(color));
    }

    private static float from8BitChannel(int value) {
        return (float)value / 255.0f;
    }

    public static int toABGR(int color) {
        return color & 0xFF00FF00 | (color & 0xFF0000) >> 16 | (color & 0xFF) << 16;
    }

    public static int fromABGR(int color) {
        return ARGB.toABGR(color);
    }

    public static int setBrightness(int color, float brightness) {
        float hue;
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);
        int alpha = ARGB.alpha(color);
        int rgbMax = Math.max(Math.max(red, green), blue);
        int rgbMin = Math.min(Math.min(red, green), blue);
        float rgbConstantRange = rgbMax - rgbMin;
        float saturation = rgbMax != 0 ? rgbConstantRange / (float)rgbMax : 0.0f;
        if (saturation == 0.0f) {
            hue = 0.0f;
        } else {
            float constantRed = (float)(rgbMax - red) / rgbConstantRange;
            float constantGreen = (float)(rgbMax - green) / rgbConstantRange;
            float constantBlue = (float)(rgbMax - blue) / rgbConstantRange;
            hue = red == rgbMax ? constantBlue - constantGreen : (green == rgbMax ? 2.0f + constantRed - constantBlue : 4.0f + constantGreen - constantRed);
            if ((hue /= 6.0f) < 0.0f) {
                hue += 1.0f;
            }
        }
        if (saturation == 0.0f) {
            green = blue = Math.round(brightness * 255.0f);
            red = blue;
            return ARGB.color(alpha, red, green, blue);
        }
        float colorWheelSegment = (hue - (float)Math.floor(hue)) * 6.0f;
        float colorWheelOffset = colorWheelSegment - (float)Math.floor(colorWheelSegment);
        float primaryColor = brightness * (1.0f - saturation);
        float secondaryColor = brightness * (1.0f - saturation * colorWheelOffset);
        float tertiaryColor = brightness * (1.0f - saturation * (1.0f - colorWheelOffset));
        switch ((int)colorWheelSegment) {
            case 0: {
                red = Math.round(brightness * 255.0f);
                green = Math.round(tertiaryColor * 255.0f);
                blue = Math.round(primaryColor * 255.0f);
                break;
            }
            case 1: {
                red = Math.round(secondaryColor * 255.0f);
                green = Math.round(brightness * 255.0f);
                blue = Math.round(primaryColor * 255.0f);
                break;
            }
            case 2: {
                red = Math.round(primaryColor * 255.0f);
                green = Math.round(brightness * 255.0f);
                blue = Math.round(tertiaryColor * 255.0f);
                break;
            }
            case 3: {
                red = Math.round(primaryColor * 255.0f);
                green = Math.round(secondaryColor * 255.0f);
                blue = Math.round(brightness * 255.0f);
                break;
            }
            case 4: {
                red = Math.round(tertiaryColor * 255.0f);
                green = Math.round(primaryColor * 255.0f);
                blue = Math.round(brightness * 255.0f);
                break;
            }
            case 5: {
                red = Math.round(brightness * 255.0f);
                green = Math.round(primaryColor * 255.0f);
                blue = Math.round(secondaryColor * 255.0f);
            }
        }
        return ARGB.color(alpha, red, green, blue);
    }
}

