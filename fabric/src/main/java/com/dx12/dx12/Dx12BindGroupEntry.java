package com.dx12.dx12;

import com.mojang.blaze3d.GpuFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * 镜像官方 {@code com.mojang.blaze3d.vulkan.VulkanBindGroupLayout.Entry}。
 *
 * 记录着色器实际声明的绑定资源（按 addToBindGroup 顺序），rebind 阶段会把
 * SPIR-V 中对应资源的 binding decoration 按此顺序重写为 0..n-1：
 * UBO -> cbuffer b{index}（D3D12 CBV），sampled image -> t{index}（SRV），
 * texel buffer -> t{index}（SRV）。
 */
@Environment(EnvType.CLIENT)
public record Dx12BindGroupEntry(Type type, String name, @Nullable GpuFormat texelBufferFormat) {

    public enum Type {
        UNIFORM_BUFFER,
        SAMPLED_IMAGE,
        TEXEL_BUFFER
    }
}
