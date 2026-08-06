/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.dispatch;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;

@Environment(value=EnvType.CLIENT)
public interface BlockStateModel {
    public void collectParts(RandomSource var1, List<BlockStateModelPart> var2);

    public Material.Baked particleMaterial();

    public @BakedQuad.MaterialFlags int materialFlags();

    default public boolean hasMaterialFlag(@BakedQuad.MaterialFlags int flag) {
        return (this.materialFlags() & flag) != 0;
    }

    @Environment(value=EnvType.CLIENT)
    public static class SimpleCachedUnbakedRoot
    implements UnbakedRoot {
        private final Unbaked contents;
        private final ModelBaker.SharedOperationKey<BlockStateModel> bakingKey = new ModelBaker.SharedOperationKey<BlockStateModel>(this){
            final /* synthetic */ SimpleCachedUnbakedRoot this$0;
            {
                SimpleCachedUnbakedRoot simpleCachedUnbakedRoot = this$0;
                Objects.requireNonNull(simpleCachedUnbakedRoot);
                this.this$0 = simpleCachedUnbakedRoot;
            }

            @Override
            public BlockStateModel compute(ModelBaker modelBakery) {
                return this.this$0.contents.bake(modelBakery);
            }
        };

        public SimpleCachedUnbakedRoot(Unbaked contents) {
            this.contents = contents;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.contents.resolveDependencies(resolver);
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker modelBakery) {
            return modelBakery.compute(this.bakingKey);
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            return this;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Unbaked
    extends ResolvableModel {
        public static final Codec<Weighted<Variant>> ELEMENT_CODEC = RecordCodecBuilder.create(i -> i.group((App)Variant.MAP_CODEC.forGetter(Weighted::value), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", (Object)1).forGetter(Weighted::weight)).apply((Applicative)i, Weighted::new));
        public static final Codec<WeightedVariants.Unbaked> HARDCODED_WEIGHTED_CODEC = ExtraCodecs.nonEmptyList(ELEMENT_CODEC.listOf()).flatComapMap(w -> new WeightedVariants.Unbaked(WeightedList.of(Lists.transform((List)w, e -> e.map(SingleVariant.Unbaked::new)))), unbaked -> {
            List<Weighted<Unbaked>> entries = unbaked.entries().unwrap();
            ArrayList<Weighted<Variant>> result = new ArrayList<Weighted<Variant>>(entries.size());
            for (Weighted<Unbaked> entry : entries) {
                Unbaked patt0$temp = entry.value();
                if (patt0$temp instanceof SingleVariant.Unbaked) {
                    SingleVariant.Unbaked singleVariant = (SingleVariant.Unbaked)patt0$temp;
                    result.add(new Weighted<Variant>(singleVariant.variant(), entry.weight()));
                    continue;
                }
                return DataResult.error(() -> "Only single variants are supported");
            }
            return DataResult.success(result);
        });
        public static final Codec<Unbaked> CODEC = Codec.either(HARDCODED_WEIGHTED_CODEC, SingleVariant.Unbaked.CODEC).flatComapMap(v -> (Unbaked)v.map(l -> l, r -> r), o -> {
            Unbaked unbaked = o;
            Objects.requireNonNull(unbaked);
            Unbaked selector0$temp = unbaked;
            int index$1 = 0;
            return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{SingleVariant.Unbaked.class, WeightedVariants.Unbaked.class}, (Unbaked)selector0$temp, index$1)) {
                case 0 -> {
                    SingleVariant.Unbaked single = (SingleVariant.Unbaked)selector0$temp;
                    yield DataResult.success((Object)Either.right((Object)single));
                }
                case 1 -> {
                    WeightedVariants.Unbaked multiple = (WeightedVariants.Unbaked)selector0$temp;
                    yield DataResult.success((Object)Either.left((Object)multiple));
                }
                default -> DataResult.error(() -> "Only a single variant or a list of variants are supported");
            };
        });

        public BlockStateModel bake(ModelBaker var1);

        default public UnbakedRoot asRoot() {
            return new SimpleCachedUnbakedRoot(this);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface UnbakedRoot
    extends ResolvableModel {
        public BlockStateModel bake(BlockState var1, ModelBaker var2);

        public Object visualEqualityGroup(BlockState var1);
    }
}

