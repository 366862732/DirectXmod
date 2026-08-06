/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GlConst {
    public static final int GL_READ_FRAMEBUFFER = 36008;
    public static final int GL_DRAW_FRAMEBUFFER = 36009;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;
    public static final int GL_NONE = 0;
    public static final int GL_LINES = 1;
    public static final int GL_LINE_STRIP = 3;
    public static final int GL_TRIANGLE_STRIP = 5;
    public static final int GL_TRIANGLE_FAN = 6;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_POINTS = 0;
    public static final int GL_WRITE_ONLY = 35001;
    public static final int GL_READ_ONLY = 35000;
    public static final int GL_READ_WRITE = 35002;
    public static final int GL_MAP_READ_BIT = 1;
    public static final int GL_MAP_WRITE_BIT = 2;
    public static final int GL_EQUAL = 514;
    public static final int GL_LEQUAL = 515;
    public static final int GL_LESS = 513;
    public static final int GL_GREATER = 516;
    public static final int GL_GEQUAL = 518;
    public static final int GL_ALWAYS = 519;
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_TEXTURE_MIN_FILTER = 10241;
    public static final int GL_TEXTURE_WRAP_S = 10242;
    public static final int GL_TEXTURE_WRAP_T = 10243;
    public static final int GL_NEAREST = 9728;
    public static final int GL_LINEAR = 9729;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 9986;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
    public static final int GL_CLAMP_TO_EDGE = 33071;
    public static final int GL_REPEAT = 10497;
    public static final int GL_FRONT = 1028;
    public static final int GL_FRONT_AND_BACK = 1032;
    public static final int GL_LINE = 6913;
    public static final int GL_FILL = 6914;
    public static final int GL_BYTE = 5120;
    public static final int GL_UNSIGNED_BYTE = 5121;
    public static final int GL_SHORT = 5122;
    public static final int GL_UNSIGNED_SHORT = 5123;
    public static final int GL_INT = 5124;
    public static final int GL_UNSIGNED_INT = 5125;
    public static final int GL_FLOAT = 5126;
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 768;
    public static final int GL_ONE_MINUS_SRC_COLOR = 769;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_DST_ALPHA = 772;
    public static final int GL_ONE_MINUS_DST_ALPHA = 773;
    public static final int GL_DST_COLOR = 774;
    public static final int GL_ONE_MINUS_DST_COLOR = 775;
    public static final int GL_REPLACE = 7681;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_RGBA8 = 32856;
    public static final int GL_PROXY_TEXTURE_2D = 32868;
    public static final int GL_RGBA = 6408;
    public static final int GL_TEXTURE_WIDTH = 4096;
    public static final int GL_BGR = 32992;
    public static final int GL_FUNC_ADD = 32774;
    public static final int GL_MIN = 32775;
    public static final int GL_MAX = 32776;
    public static final int GL_FUNC_SUBTRACT = 32778;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 32779;
    public static final int GL_DEPTH_COMPONENT24 = 33190;
    public static final int GL_STATIC_DRAW = 35044;
    public static final int GL_DYNAMIC_DRAW = 35048;
    public static final int GL_STREAM_DRAW = 35040;
    public static final int GL_STATIC_READ = 35045;
    public static final int GL_DYNAMIC_READ = 35049;
    public static final int GL_STREAM_READ = 35041;
    public static final int GL_STATIC_COPY = 35046;
    public static final int GL_DYNAMIC_COPY = 35050;
    public static final int GL_STREAM_COPY = 35042;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 37143;
    public static final int GL_TIMEOUT_EXPIRED = 37147;
    public static final int GL_WAIT_FAILED = 37149;
    public static final int GL_UNPACK_SWAP_BYTES = 3312;
    public static final int GL_UNPACK_LSB_FIRST = 3313;
    public static final int GL_UNPACK_ROW_LENGTH = 3314;
    public static final int GL_UNPACK_SKIP_ROWS = 3315;
    public static final int GL_UNPACK_SKIP_PIXELS = 3316;
    public static final int GL_UNPACK_ALIGNMENT = 3317;
    public static final int GL_PACK_ALIGNMENT = 3333;
    public static final int GL_PACK_ROW_LENGTH = 3330;
    public static final int GL_MAX_TEXTURE_SIZE = 3379;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int[] CUBEMAP_TARGETS = new int[]{34069, 34070, 34071, 34072, 34073, 34074};
    public static final int GL_DEPTH_COMPONENT = 6402;
    public static final int GL_DEPTH_COMPONENT32 = 33191;
    public static final int GL_FRAMEBUFFER = 36160;
    public static final int GL_RENDERBUFFER = 36161;
    public static final int GL_COLOR_ATTACHMENT0 = 36064;
    public static final int GL_DEPTH_ATTACHMENT = 36096;
    public static final int GL_FRAMEBUFFER_COMPLETE = 36053;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 36054;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 36055;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 36059;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 36060;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 36061;
    public static final int GL_LINK_STATUS = 35714;
    public static final int GL_COMPILE_STATUS = 35713;
    public static final int GL_VERTEX_SHADER = 35633;
    public static final int GL_FRAGMENT_SHADER = 35632;
    public static final int GL_TEXTURE0 = 33984;
    public static final int GL_TEXTURE1 = 33985;
    public static final int GL_TEXTURE2 = 33986;
    public static final int GL_TEXTURE_COMPARE_MODE = 34892;
    public static final int GL_ARRAY_BUFFER = 34962;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
    public static final int GL_PIXEL_PACK_BUFFER = 35051;
    public static final int GL_COPY_READ_BUFFER = 36662;
    public static final int GL_COPY_WRITE_BUFFER = 36663;
    public static final int GL_PIXEL_UNPACK_BUFFER = 35052;
    public static final int GL_UNIFORM_BUFFER = 35345;
    public static final int GL_RGB = 6407;
    public static final int GL_RG = 33319;
    public static final int GL_R8 = 33321;
    public static final int GL_RED = 6403;
    public static final int GL_OUT_OF_MEMORY = 1285;

    public static int toGl(CompareOp compareOp) {
        return switch (compareOp) {
            default -> throw new MatchException(null, null);
            case CompareOp.ALWAYS_PASS -> 519;
            case CompareOp.LESS_THAN -> 513;
            case CompareOp.LESS_THAN_OR_EQUAL -> 515;
            case CompareOp.EQUAL -> 514;
            case CompareOp.NOT_EQUAL -> 517;
            case CompareOp.GREATER_THAN_OR_EQUAL -> 518;
            case CompareOp.GREATER_THAN -> 516;
            case CompareOp.NEVER_PASS -> 512;
        };
    }

    public static int toGl(PolygonMode polygonMode) {
        return switch (polygonMode) {
            case PolygonMode.WIREFRAME -> 6913;
            default -> 6914;
        };
    }

    public static int toGl(BlendFactor blendFactor) {
        return switch (blendFactor) {
            default -> throw new MatchException(null, null);
            case BlendFactor.CONSTANT_ALPHA -> 32771;
            case BlendFactor.CONSTANT_COLOR -> 32769;
            case BlendFactor.DST_ALPHA -> 772;
            case BlendFactor.DST_COLOR -> 774;
            case BlendFactor.ONE -> 1;
            case BlendFactor.ONE_MINUS_CONSTANT_ALPHA -> 32772;
            case BlendFactor.ONE_MINUS_CONSTANT_COLOR -> 32770;
            case BlendFactor.ONE_MINUS_DST_ALPHA -> 773;
            case BlendFactor.ONE_MINUS_DST_COLOR -> 775;
            case BlendFactor.ONE_MINUS_SRC_ALPHA -> 771;
            case BlendFactor.ONE_MINUS_SRC_COLOR -> 769;
            case BlendFactor.SRC_ALPHA -> 770;
            case BlendFactor.SRC_ALPHA_SATURATE -> 776;
            case BlendFactor.SRC_COLOR -> 768;
            case BlendFactor.ZERO -> 0;
        };
    }

    public static int toGl(BlendOp blendOp) {
        return switch (blendOp) {
            default -> throw new MatchException(null, null);
            case BlendOp.ADD -> 32774;
            case BlendOp.SUBTRACT -> 32778;
            case BlendOp.REVERSE_SUBTRACT -> 32779;
            case BlendOp.MIN -> 32775;
            case BlendOp.MAX -> 32776;
        };
    }

    public static int toGl(PrimitiveTopology primitiveTopology) {
        return switch (primitiveTopology) {
            default -> throw new MatchException(null, null);
            case PrimitiveTopology.LINES -> 4;
            case PrimitiveTopology.DEBUG_LINES -> 1;
            case PrimitiveTopology.DEBUG_LINE_STRIP -> 3;
            case PrimitiveTopology.POINTS -> 0;
            case PrimitiveTopology.TRIANGLES -> 4;
            case PrimitiveTopology.TRIANGLE_STRIP -> 5;
            case PrimitiveTopology.TRIANGLE_FAN -> 6;
            case PrimitiveTopology.QUADS -> 4;
        };
    }

    public static int toGl(IndexType indexType) {
        return switch (indexType) {
            default -> throw new MatchException(null, null);
            case IndexType.SHORT -> 5123;
            case IndexType.INT -> 5125;
        };
    }

    public static int toGl(AddressMode addressMode) {
        return switch (addressMode) {
            default -> throw new MatchException(null, null);
            case AddressMode.REPEAT -> 10497;
            case AddressMode.CLAMP_TO_EDGE -> 33071;
        };
    }

    public static int glFormatChannelCount(int glExternalID) {
        if (glExternalID == 36249 || glExternalID == 6408) {
            return 4;
        }
        if (glExternalID == 36248 || glExternalID == 6407) {
            return 3;
        }
        if (glExternalID == 33320 || glExternalID == 33319) {
            return 2;
        }
        if (glExternalID == 36244 || glExternalID == 6403) {
            return 1;
        }
        return 0;
    }

    public static boolean isGlFormatInteger(int glExternalID) {
        return glExternalID == 36249 || glExternalID == 36248 || glExternalID == 33320 || glExternalID == 36244;
    }

    public static boolean isFormatNormalized(GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case GpuFormat.R8_UNORM, GpuFormat.R8_SNORM, GpuFormat.R16_UNORM, GpuFormat.R16_SNORM, GpuFormat.RG8_UNORM, GpuFormat.RG8_SNORM, GpuFormat.RG16_UNORM, GpuFormat.RG16_SNORM, GpuFormat.RGB8_UNORM, GpuFormat.RGB8_SNORM, GpuFormat.RGB16_UNORM, GpuFormat.RGB16_SNORM, GpuFormat.RGBA8_UNORM, GpuFormat.RGBA8_SNORM, GpuFormat.RGBA16_UNORM, GpuFormat.RGB10A2_UNORM, GpuFormat.D16_UNORM -> true;
            default -> false;
        };
    }

    public static int toGlInternalId(GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case GpuFormat.R8_UNORM -> 33321;
            case GpuFormat.R8_SNORM -> 36756;
            case GpuFormat.RG8_UNORM -> 33323;
            case GpuFormat.RG8_SNORM -> 36757;
            case GpuFormat.RGBA8_UNORM -> 32856;
            case GpuFormat.RGBA8_SNORM -> 36759;
            case GpuFormat.R16_UNORM -> 33322;
            case GpuFormat.R16_SNORM -> 36760;
            case GpuFormat.RG16_UNORM -> 33324;
            case GpuFormat.RG16_SNORM -> 36761;
            case GpuFormat.RGBA16_UNORM -> 32859;
            case GpuFormat.RGBA16_SNORM -> 36763;
            case GpuFormat.R8_UINT -> 33330;
            case GpuFormat.R8_SINT -> 33329;
            case GpuFormat.RG8_UINT -> 33336;
            case GpuFormat.RG8_SINT -> 33335;
            case GpuFormat.RGBA8_UINT -> 36220;
            case GpuFormat.RGBA8_SINT -> 36238;
            case GpuFormat.R16_UINT -> 33332;
            case GpuFormat.R16_SINT -> 33331;
            case GpuFormat.RG16_UINT -> 33338;
            case GpuFormat.RG16_SINT -> 33337;
            case GpuFormat.RGBA16_UINT -> 36214;
            case GpuFormat.RGBA16_SINT -> 36232;
            case GpuFormat.R32_UINT -> 33334;
            case GpuFormat.R32_SINT -> 33333;
            case GpuFormat.RG32_UINT -> 33340;
            case GpuFormat.RG32_SINT -> 33339;
            case GpuFormat.RGB32_UINT -> 36209;
            case GpuFormat.RGB32_SINT -> 36227;
            case GpuFormat.RGBA32_UINT -> 36208;
            case GpuFormat.RGBA32_SINT -> 36226;
            case GpuFormat.R16_FLOAT -> 33325;
            case GpuFormat.RG16_FLOAT -> 33327;
            case GpuFormat.RGBA16_FLOAT -> 34842;
            case GpuFormat.R32_FLOAT -> 33326;
            case GpuFormat.RG32_FLOAT -> 33328;
            case GpuFormat.RGBA32_FLOAT -> 34836;
            case GpuFormat.RGB10A2_UNORM -> 32857;
            case GpuFormat.RGB10A2_UINT -> 36975;
            case GpuFormat.RG11B10_FLOAT -> 35898;
            case GpuFormat.D32_FLOAT -> 36012;
            case GpuFormat.D32_FLOAT_S8_UINT -> 36013;
            case GpuFormat.D24_UNORM_S8_UINT -> 35056;
            case GpuFormat.D16_UNORM -> 33189;
            case GpuFormat.S8_UINT -> 36168;
            default -> 0;
        };
    }

    public static int toGlExternalId(GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case GpuFormat.R8_UNORM, GpuFormat.R8_SNORM, GpuFormat.R16_UNORM, GpuFormat.R16_SNORM, GpuFormat.R16_FLOAT, GpuFormat.R32_FLOAT -> 6403;
            case GpuFormat.R8_UINT, GpuFormat.R8_SINT, GpuFormat.R16_UINT, GpuFormat.R16_SINT, GpuFormat.R32_UINT, GpuFormat.R32_SINT -> 36244;
            case GpuFormat.RG8_UNORM, GpuFormat.RG8_SNORM, GpuFormat.RG16_UNORM, GpuFormat.RG16_SNORM, GpuFormat.RG16_FLOAT, GpuFormat.RG32_FLOAT -> 33319;
            case GpuFormat.RG8_UINT, GpuFormat.RG8_SINT, GpuFormat.RG16_UINT, GpuFormat.RG16_SINT, GpuFormat.RG32_UINT, GpuFormat.RG32_SINT -> 33320;
            case GpuFormat.RGB8_UNORM, GpuFormat.RGB8_SNORM, GpuFormat.RGB16_UNORM, GpuFormat.RGB16_SNORM, GpuFormat.RG11B10_FLOAT, GpuFormat.RGB16_FLOAT, GpuFormat.RGB32_FLOAT -> 6407;
            case GpuFormat.RGB32_UINT, GpuFormat.RGB32_SINT, GpuFormat.RGB8_UINT, GpuFormat.RGB8_SINT, GpuFormat.RGB16_UINT, GpuFormat.RGB16_SINT -> 36248;
            case GpuFormat.RGBA8_UNORM, GpuFormat.RGBA8_SNORM, GpuFormat.RGBA16_UNORM, GpuFormat.RGB10A2_UNORM, GpuFormat.RGBA16_SNORM, GpuFormat.RGBA16_FLOAT, GpuFormat.RGBA32_FLOAT -> 6408;
            case GpuFormat.RGBA8_UINT, GpuFormat.RGBA8_SINT, GpuFormat.RGBA16_UINT, GpuFormat.RGBA16_SINT, GpuFormat.RGBA32_UINT, GpuFormat.RGBA32_SINT, GpuFormat.RGB10A2_UINT -> 36249;
            case GpuFormat.D16_UNORM, GpuFormat.D32_FLOAT -> 6402;
            case GpuFormat.D32_FLOAT_S8_UINT, GpuFormat.D24_UNORM_S8_UINT -> 34041;
            case GpuFormat.S8_UINT -> 6401;
            default -> 0;
        };
    }

    public static int toGlType(GpuFormat gpuFormat) {
        return switch (gpuFormat) {
            case GpuFormat.R8_UNORM, GpuFormat.RG8_UNORM, GpuFormat.RGB8_UNORM, GpuFormat.RGBA8_UNORM, GpuFormat.R8_UINT, GpuFormat.RG8_UINT, GpuFormat.RGBA8_UINT, GpuFormat.S8_UINT, GpuFormat.RGB8_UINT -> 5121;
            case GpuFormat.R8_SNORM, GpuFormat.RG8_SNORM, GpuFormat.RGB8_SNORM, GpuFormat.RGBA8_SNORM, GpuFormat.R8_SINT, GpuFormat.RG8_SINT, GpuFormat.RGBA8_SINT, GpuFormat.RGB8_SINT -> 5120;
            case GpuFormat.R16_UNORM, GpuFormat.RG16_UNORM, GpuFormat.RGB16_UNORM, GpuFormat.RGBA16_UNORM, GpuFormat.D16_UNORM, GpuFormat.R16_UINT, GpuFormat.RG16_UINT, GpuFormat.RGBA16_UINT, GpuFormat.RGB16_UINT -> 5123;
            case GpuFormat.R16_SNORM, GpuFormat.RG16_SNORM, GpuFormat.RGB16_SNORM, GpuFormat.RGBA16_SNORM, GpuFormat.R16_SINT, GpuFormat.RG16_SINT, GpuFormat.RGBA16_SINT, GpuFormat.RGB16_SINT -> 5122;
            case GpuFormat.R32_UINT, GpuFormat.RG32_UINT, GpuFormat.RGB32_UINT, GpuFormat.RGBA32_UINT -> 5125;
            case GpuFormat.R32_SINT, GpuFormat.RG32_SINT, GpuFormat.RGB32_SINT, GpuFormat.RGBA32_SINT -> 5124;
            case GpuFormat.R32_FLOAT, GpuFormat.RG32_FLOAT, GpuFormat.RGBA32_FLOAT, GpuFormat.D32_FLOAT, GpuFormat.RGB32_FLOAT -> 5126;
            case GpuFormat.R16_FLOAT, GpuFormat.RG16_FLOAT, GpuFormat.RGBA16_FLOAT, GpuFormat.RGB16_FLOAT -> 5131;
            case GpuFormat.RGB10A2_UNORM, GpuFormat.RGB10A2_UINT -> 33640;
            case GpuFormat.RG11B10_FLOAT -> 35899;
            case GpuFormat.D32_FLOAT_S8_UINT -> 36269;
            case GpuFormat.D24_UNORM_S8_UINT -> 34042;
            default -> 0;
        };
    }

    public static int toGl(ShaderType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case ShaderType.VERTEX -> 35633;
            case ShaderType.FRAGMENT -> 35632;
        };
    }

    public static int bufferUsageToGlFlag(@GpuBuffer.Usage int usage) {
        int result = 0;
        if ((usage & 1) != 0) {
            result |= 0x41;
        }
        if ((usage & 2) != 0) {
            result |= 0x42;
        }
        if ((usage & 8) != 0) {
            result |= 0x100;
        }
        if ((usage & 4) != 0) {
            result |= 0x200;
        }
        return result;
    }

    public static int bufferUsageToGlEnum(@GpuBuffer.Usage int usage) {
        boolean clientStorage;
        boolean bl = clientStorage = (usage & 4) != 0;
        if ((usage & 2) != 0) {
            if (clientStorage) {
                return 35040;
            }
            return 35044;
        }
        if ((usage & 1) != 0) {
            if (clientStorage) {
                return 35041;
            }
            return 35045;
        }
        return 35044;
    }
}

