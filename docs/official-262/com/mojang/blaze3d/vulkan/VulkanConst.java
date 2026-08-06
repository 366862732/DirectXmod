/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.vulkan;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.systems.GpuSurface;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class VulkanConst {
    public static int textureUsageToVk(@GpuTexture.Usage int usage, GpuFormat format) {
        int vkUsage = 0;
        if ((usage & 8) != 0) {
            if (format.hasColorAspect()) {
                vkUsage |= 0x10;
            }
            if (format.hasDepthAspect()) {
                vkUsage |= 0x20;
            }
        }
        if ((usage & 4) != 0) {
            vkUsage |= 4;
        }
        if ((usage & 1) != 0) {
            vkUsage |= 2;
        }
        if ((usage & 2) != 0) {
            vkUsage |= 1;
        }
        return vkUsage;
    }

    public static int bufferUsageToVk(@GpuBuffer.Usage int usage) {
        int result = 0;
        if ((usage & 8) != 0) {
            result |= 2;
        }
        if ((usage & 0x10) != 0) {
            result |= 1;
        }
        if ((usage & 0x20) != 0) {
            result |= 0x80;
        }
        if ((usage & 0x40) != 0) {
            result |= 0x40;
        }
        if ((usage & 0x80) != 0) {
            result |= 0x10;
        }
        if ((usage & 0x100) != 0) {
            result |= 4;
        }
        if ((usage & 0x200) != 0) {
            result |= 0x100;
        }
        return result;
    }

    public static int formatAspectMask(GpuFormat format) {
        int aspectMask = 0;
        if (format.hasColorAspect()) {
            aspectMask |= 1;
        }
        if (format.hasDepthAspect()) {
            aspectMask |= 2;
        }
        if (format.hasStencilAspect()) {
            aspectMask |= 4;
        }
        return aspectMask;
    }

    public static int toVk(AddressMode addressMode) {
        return switch (addressMode) {
            default -> throw new MatchException(null, null);
            case AddressMode.REPEAT -> 0;
            case AddressMode.CLAMP_TO_EDGE -> 2;
        };
    }

    public static int toVk(FilterMode filter) {
        return switch (filter) {
            default -> throw new MatchException(null, null);
            case FilterMode.NEAREST -> 0;
            case FilterMode.LINEAR -> 1;
        };
    }

    public static int toVk(GpuFormat format) {
        return switch (format) {
            default -> throw new MatchException(null, null);
            case GpuFormat.R8_UNORM -> 9;
            case GpuFormat.R8_SNORM -> 10;
            case GpuFormat.RG8_UNORM -> 16;
            case GpuFormat.RG8_SNORM -> 17;
            case GpuFormat.RGB8_UNORM -> 23;
            case GpuFormat.RGB8_SNORM -> 24;
            case GpuFormat.RGBA8_UNORM -> 37;
            case GpuFormat.RGBA8_SNORM -> 38;
            case GpuFormat.R16_UNORM -> 70;
            case GpuFormat.R16_SNORM -> 71;
            case GpuFormat.RG16_UNORM -> 77;
            case GpuFormat.RG16_SNORM -> 78;
            case GpuFormat.RGB16_UNORM -> 84;
            case GpuFormat.RGB16_SNORM -> 85;
            case GpuFormat.RGBA16_UNORM -> 91;
            case GpuFormat.RGBA16_SNORM -> 92;
            case GpuFormat.R8_UINT -> 13;
            case GpuFormat.R8_SINT -> 14;
            case GpuFormat.RG8_UINT -> 20;
            case GpuFormat.RG8_SINT -> 21;
            case GpuFormat.RGB8_UINT -> 27;
            case GpuFormat.RGB8_SINT -> 28;
            case GpuFormat.RGBA8_UINT -> 41;
            case GpuFormat.RGBA8_SINT -> 42;
            case GpuFormat.R16_UINT -> 74;
            case GpuFormat.R16_SINT -> 75;
            case GpuFormat.RG16_UINT -> 81;
            case GpuFormat.RG16_SINT -> 82;
            case GpuFormat.RGB16_UINT -> 88;
            case GpuFormat.RGB16_SINT -> 89;
            case GpuFormat.RGBA16_UINT -> 95;
            case GpuFormat.RGBA16_SINT -> 96;
            case GpuFormat.R32_UINT -> 98;
            case GpuFormat.R32_SINT -> 99;
            case GpuFormat.RG32_UINT -> 101;
            case GpuFormat.RG32_SINT -> 102;
            case GpuFormat.RGB32_UINT -> 104;
            case GpuFormat.RGB32_SINT -> 105;
            case GpuFormat.RGBA32_UINT -> 107;
            case GpuFormat.RGBA32_SINT -> 108;
            case GpuFormat.R16_FLOAT -> 76;
            case GpuFormat.RG16_FLOAT -> 83;
            case GpuFormat.RGB16_FLOAT -> 90;
            case GpuFormat.RGBA16_FLOAT -> 97;
            case GpuFormat.R32_FLOAT -> 100;
            case GpuFormat.RG32_FLOAT -> 103;
            case GpuFormat.RGB32_FLOAT -> 106;
            case GpuFormat.RGBA32_FLOAT -> 109;
            case GpuFormat.RGB10A2_UNORM -> 64;
            case GpuFormat.RGB10A2_UINT -> 68;
            case GpuFormat.RG11B10_FLOAT -> 122;
            case GpuFormat.D32_FLOAT -> 126;
            case GpuFormat.D32_FLOAT_S8_UINT -> 130;
            case GpuFormat.D24_UNORM_S8_UINT -> 129;
            case GpuFormat.D16_UNORM -> 124;
            case GpuFormat.S8_UINT -> 127;
        };
    }

    public static int toVk(BlendFactor factor) {
        return switch (factor) {
            default -> throw new MatchException(null, null);
            case BlendFactor.CONSTANT_ALPHA -> 12;
            case BlendFactor.CONSTANT_COLOR -> 10;
            case BlendFactor.ONE_MINUS_CONSTANT_ALPHA -> 13;
            case BlendFactor.ONE_MINUS_CONSTANT_COLOR -> 11;
            case BlendFactor.DST_ALPHA -> 8;
            case BlendFactor.DST_COLOR -> 4;
            case BlendFactor.ONE -> 1;
            case BlendFactor.ONE_MINUS_DST_ALPHA -> 9;
            case BlendFactor.ONE_MINUS_DST_COLOR -> 5;
            case BlendFactor.ONE_MINUS_SRC_ALPHA -> 7;
            case BlendFactor.ONE_MINUS_SRC_COLOR -> 3;
            case BlendFactor.SRC_ALPHA -> 6;
            case BlendFactor.SRC_ALPHA_SATURATE -> 14;
            case BlendFactor.SRC_COLOR -> 2;
            case BlendFactor.ZERO -> 0;
        };
    }

    public static int toVk(BlendOp blendOp) {
        return switch (blendOp) {
            default -> throw new MatchException(null, null);
            case BlendOp.ADD -> 0;
            case BlendOp.SUBTRACT -> 1;
            case BlendOp.REVERSE_SUBTRACT -> 2;
            case BlendOp.MIN -> 3;
            case BlendOp.MAX -> 4;
        };
    }

    public static int toVk(CompareOp op) {
        return switch (op) {
            default -> throw new MatchException(null, null);
            case CompareOp.ALWAYS_PASS -> 7;
            case CompareOp.LESS_THAN -> 1;
            case CompareOp.LESS_THAN_OR_EQUAL -> 3;
            case CompareOp.EQUAL -> 2;
            case CompareOp.NOT_EQUAL -> 5;
            case CompareOp.GREATER_THAN_OR_EQUAL -> 6;
            case CompareOp.GREATER_THAN -> 4;
            case CompareOp.NEVER_PASS -> 0;
        };
    }

    public static int toVk(PolygonMode polygonMode) {
        return switch (polygonMode) {
            default -> throw new MatchException(null, null);
            case PolygonMode.FILL -> 0;
            case PolygonMode.WIREFRAME -> 1;
        };
    }

    public static int toVk(PrimitiveTopology primitiveTopology) {
        return switch (primitiveTopology) {
            default -> throw new MatchException(null, null);
            case PrimitiveTopology.LINES -> 3;
            case PrimitiveTopology.DEBUG_LINES -> 1;
            case PrimitiveTopology.DEBUG_LINE_STRIP -> 2;
            case PrimitiveTopology.POINTS -> 0;
            case PrimitiveTopology.TRIANGLES -> 3;
            case PrimitiveTopology.TRIANGLE_STRIP -> 4;
            case PrimitiveTopology.TRIANGLE_FAN -> 5;
            case PrimitiveTopology.QUADS -> 3;
        };
    }

    public static int toVk(ColorTargetState colorTargetState) {
        int result = 0;
        if (colorTargetState.writeAlpha()) {
            result |= 8;
        }
        if (colorTargetState.writeRed()) {
            result |= 1;
        }
        if (colorTargetState.writeGreen()) {
            result |= 2;
        }
        if (colorTargetState.writeBlue()) {
            result |= 4;
        }
        return result;
    }

    public static int toVk(GpuSurface.PresentMode mode) {
        return switch (mode) {
            default -> throw new MatchException(null, null);
            case GpuSurface.PresentMode.IMMEDIATE -> 0;
            case GpuSurface.PresentMode.MAILBOX -> 1;
            case GpuSurface.PresentMode.FIFO -> 2;
            case GpuSurface.PresentMode.FIFO_RELAXED -> 3;
        };
    }
}

