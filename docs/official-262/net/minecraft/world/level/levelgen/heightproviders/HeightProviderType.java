/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.heightproviders.BiasedToBottomHeight;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.heightproviders.VeryBiasedToBottomHeight;
import net.minecraft.world.level.levelgen.heightproviders.WeightedListHeight;

public interface HeightProviderType<P extends HeightProvider> {
    public static final HeightProviderType<ConstantHeight> CONSTANT = HeightProviderType.register("constant", ConstantHeight.CODEC);
    public static final HeightProviderType<UniformHeight> UNIFORM = HeightProviderType.register("uniform", UniformHeight.CODEC);
    public static final HeightProviderType<BiasedToBottomHeight> BIASED_TO_BOTTOM = HeightProviderType.register("biased_to_bottom", BiasedToBottomHeight.CODEC);
    public static final HeightProviderType<VeryBiasedToBottomHeight> VERY_BIASED_TO_BOTTOM = HeightProviderType.register("very_biased_to_bottom", VeryBiasedToBottomHeight.CODEC);
    public static final HeightProviderType<TrapezoidHeight> TRAPEZOID = HeightProviderType.register("trapezoid", TrapezoidHeight.CODEC);
    public static final HeightProviderType<WeightedListHeight> WEIGHTED_LIST = HeightProviderType.register("weighted_list", WeightedListHeight.CODEC);

    public MapCodec<P> codec();

    private static <P extends HeightProvider> HeightProviderType<P> register(String id, MapCodec<P> codec) {
        return Registry.register(BuiltInRegistries.HEIGHT_PROVIDER_TYPE, id, () -> codec);
    }
}

