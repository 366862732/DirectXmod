/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SelectItemModel<T>
implements ItemModel {
    private final SelectItemModelProperty<T> property;
    private final ModelSelector<T> models;

    public SelectItemModel(SelectItemModelProperty<T> property, ModelSelector<T> models) {
        this.property = property;
        this.models = models;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        T value = this.property.get(item, level, owner == null ? null : owner.asLivingEntity(), seed, displayContext);
        this.models.get(value, level).update(output, item, resolver, displayContext, level, owner, seed);
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface ModelSelector<T> {
        public ItemModel get(@Nullable T var1, @Nullable ClientLevel var2);
    }

    @Environment(value=EnvType.CLIENT)
    public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
        public static <T> Codec<SwitchCase<T>> codec(Codec<T> valueCodec) {
            return RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(valueCodec)).fieldOf("when").forGetter(SwitchCase::values), (App)ItemModels.CODEC.fieldOf("model").forGetter(SwitchCase::model)).apply((Applicative)i, SwitchCase::new));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record UnbakedSwitch<P extends SelectItemModelProperty<T>, T>(P property, List<SwitchCase<T>> cases) {
        public static final MapCodec<UnbakedSwitch<?, ?>> MAP_CODEC = SelectItemModelProperties.CODEC.dispatchMap("property", unbaked -> unbaked.property().type(), SelectItemModelProperty.Type::switchCodec);

        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation, ItemModel fallback) {
            Object2ObjectOpenHashMap bakedModels = new Object2ObjectOpenHashMap();
            for (SwitchCase<T> c : this.cases) {
                ItemModel.Unbaked caseModel = c.model;
                ItemModel bakedCaseModel = caseModel.bake(context, transformation);
                for (Object value : c.values) {
                    bakedModels.put(value, (Object)bakedCaseModel);
                }
            }
            bakedModels.defaultReturnValue((Object)fallback);
            return new SelectItemModel<T>(this.property, this.createModelGetter((Object2ObjectMap<T, ItemModel>)bakedModels, context.contextSwapper()));
        }

        private ModelSelector<T> createModelGetter(Object2ObjectMap<T, ItemModel> originalModels, @Nullable RegistryContextSwapper registrySwapper) {
            if (registrySwapper == null) {
                return (value, clientLevel) -> (ItemModel)originalModels.get(value);
            }
            ItemModel defaultModel = (ItemModel)originalModels.defaultReturnValue();
            CacheSlot<ClientLevel, Object2ObjectMap> remappedModelCache = new CacheSlot<ClientLevel, Object2ObjectMap>(clientLevel -> {
                Object2ObjectOpenHashMap remappedModels = new Object2ObjectOpenHashMap(originalModels.size());
                remappedModels.defaultReturnValue((Object)defaultModel);
                originalModels.forEach((arg_0, arg_1) -> this.lambda$createModelGetter$2(registrySwapper, clientLevel, (Object2ObjectMap)remappedModels, arg_0, arg_1));
                return remappedModels;
            });
            return (value, context) -> {
                if (context == null) {
                    return (ItemModel)originalModels.get(value);
                }
                if (value == null) {
                    return defaultModel;
                }
                return (ItemModel)((Object2ObjectMap)remappedModelCache.compute(context)).get(value);
            };
        }

        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (SwitchCase<T> c : this.cases) {
                c.model.resolveDependencies(resolver);
            }
        }

        private /* synthetic */ void lambda$createModelGetter$2(RegistryContextSwapper registrySwapper, ClientLevel clientLevel, Object2ObjectMap remappedModels, Object value, ItemModel model) {
            registrySwapper.swapTo(this.property.valueCodec(), value, clientLevel.registryAccess()).ifSuccess(remappedValue -> remappedModels.put(remappedValue, (Object)model));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Optional<Transformation> transformation, UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation), (App)UnbakedSwitch.MAP_CODEC.forGetter(Unbaked::unbakedSwitch), (App)ItemModels.CODEC.optionalFieldOf("fallback").forGetter(Unbaked::fallback)).apply((Applicative)i, Unbaked::new));

        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            ItemModel bakedFallback = this.fallback.map(m -> m.bake(context, childTransform)).orElseGet(() -> context.missingItemModel(childTransform));
            return this.unbakedSwitch.bake(context, childTransform, bakedFallback);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.unbakedSwitch.resolveDependencies(resolver);
            this.fallback.ifPresent(m -> m.resolveDependencies(resolver));
        }
    }
}

