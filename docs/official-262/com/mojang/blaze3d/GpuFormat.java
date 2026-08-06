/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public enum GpuFormat {
    R8_UNORM(ComponentType.UNORM_8, 1),
    R8_SNORM(ComponentType.SNORM_8, 1),
    RG8_UNORM(ComponentType.UNORM_8, 2),
    RG8_SNORM(ComponentType.SNORM_8, 2),
    RGB8_UNORM(ComponentType.UNORM_8, 3),
    RGB8_SNORM(ComponentType.SNORM_8, 3),
    RGBA8_UNORM(ComponentType.UNORM_8, 4),
    RGBA8_SNORM(ComponentType.SNORM_8, 4),
    R16_UNORM(ComponentType.UNORM_16, 1),
    R16_SNORM(ComponentType.SNORM_16, 1),
    RG16_UNORM(ComponentType.UNORM_16, 2),
    RG16_SNORM(ComponentType.SNORM_16, 2),
    RGB16_UNORM(ComponentType.UNORM_16, 3),
    RGB16_SNORM(ComponentType.SNORM_16, 3),
    RGBA16_UNORM(ComponentType.UNORM_16, 4),
    RGBA16_SNORM(ComponentType.SNORM_16, 4),
    R8_UINT(ComponentType.UINT_8, 1),
    R8_SINT(ComponentType.SINT_8, 1),
    RG8_UINT(ComponentType.UINT_8, 2),
    RG8_SINT(ComponentType.SINT_8, 2),
    RGB8_UINT(ComponentType.UINT_8, 3),
    RGB8_SINT(ComponentType.SINT_8, 3),
    RGBA8_UINT(ComponentType.UINT_8, 4),
    RGBA8_SINT(ComponentType.SINT_8, 4),
    R16_UINT(ComponentType.UINT_16, 1),
    R16_SINT(ComponentType.SINT_16, 1),
    RG16_UINT(ComponentType.UINT_16, 2),
    RG16_SINT(ComponentType.SINT_16, 2),
    RGB16_UINT(ComponentType.UINT_16, 3),
    RGB16_SINT(ComponentType.SINT_16, 3),
    RGBA16_UINT(ComponentType.UINT_16, 4),
    RGBA16_SINT(ComponentType.SINT_16, 4),
    R32_UINT(ComponentType.UINT_32, 1),
    R32_SINT(ComponentType.SINT_32, 1),
    RG32_UINT(ComponentType.UINT_32, 2),
    RG32_SINT(ComponentType.SINT_32, 2),
    RGB32_UINT(ComponentType.UINT_32, 3),
    RGB32_SINT(ComponentType.SINT_32, 3),
    RGBA32_UINT(ComponentType.UINT_32, 4),
    RGBA32_SINT(ComponentType.SINT_32, 4),
    R16_FLOAT(ComponentType.FLOAT_16, 1),
    RG16_FLOAT(ComponentType.FLOAT_16, 2),
    RGB16_FLOAT(ComponentType.FLOAT_16, 3),
    RGBA16_FLOAT(ComponentType.FLOAT_16, 4),
    R32_FLOAT(ComponentType.FLOAT_32, 1),
    RG32_FLOAT(ComponentType.FLOAT_32, 2),
    RGB32_FLOAT(ComponentType.FLOAT_32, 3),
    RGBA32_FLOAT(ComponentType.FLOAT_32, 4),
    RGB10A2_UNORM(ComponentType.OPAQUE_32, 1),
    RGB10A2_UINT(ComponentType.OPAQUE_32, 1),
    RG11B10_FLOAT(ComponentType.OPAQUE_32, 1),
    D32_FLOAT(ComponentType.OPAQUE_32, 1),
    D32_FLOAT_S8_UINT(ComponentType.OPAQUE_64, 1),
    D24_UNORM_S8_UINT(ComponentType.OPAQUE_32, 1),
    D16_UNORM(ComponentType.OPAQUE_16, 1),
    S8_UINT(ComponentType.OPAQUE_8, 1);

    private final ComponentType componentType;
    private final int componentCount;

    private GpuFormat(ComponentType componentType, int componentCount) {
        this.componentType = componentType;
        this.componentCount = componentCount;
    }

    public int blockSize() {
        return this.componentType.byteSize() * this.componentCount;
    }

    public int byteAlignment() {
        return this.componentType.byteSize();
    }

    public ComponentType componentType() {
        return this.componentType;
    }

    public int componentCount() {
        return this.componentCount;
    }

    public boolean hasColorAspect() {
        return !this.hasDepthAspect() && !this.hasStencilAspect();
    }

    public boolean hasDepthAspect() {
        return this == D32_FLOAT || this == D32_FLOAT_S8_UINT || this == D24_UNORM_S8_UINT || this == D16_UNORM;
    }

    public boolean hasStencilAspect() {
        return this == S8_UINT || this == D32_FLOAT_S8_UINT || this == D24_UNORM_S8_UINT;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum ComponentType {
        UNORM_8(1),
        SNORM_8(1),
        UINT_8(1),
        SINT_8(1),
        UNORM_16(2),
        SNORM_16(2),
        UINT_16(2),
        SINT_16(2),
        FLOAT_16(2),
        UINT_32(4),
        SINT_32(4),
        FLOAT_32(4),
        OPAQUE_8(1),
        OPAQUE_16(2),
        OPAQUE_32(4),
        OPAQUE_64(8);

        private final int byteSize;

        private ComponentType(int byteSize) {
            this.byteSize = byteSize;
        }

        public int byteSize() {
            return this.byteSize;
        }
    }
}

