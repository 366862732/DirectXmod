/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RangeSelectItemModel
implements ItemModel {
    private static final int LINEAR_SEARCH_THRESHOLD = 16;
    private final RangeSelectItemModelProperty property;
    private final float scale;
    private final float[] thresholds;
    private final ItemModel[] models;
    private final ItemModel fallback;

    private RangeSelectItemModel(RangeSelectItemModelProperty property, float scale, float[] thresholds, ItemModel[] models, ItemModel fallback) {
        this.property = property;
        this.thresholds = thresholds;
        this.models = models;
        this.fallback = fallback;
        this.scale = scale;
    }

    private static int lastIndexLessOrEqual(float[] haystack, float needle) {
        if (haystack.length < 16) {
            for (int i = 0; i < haystack.length; ++i) {
                if (!(haystack[i] > needle)) continue;
                return i - 1;
            }
            return haystack.length - 1;
        }
        int index = Arrays.binarySearch(haystack, needle);
        if (index < 0) {
            int insertionPoint = ~index;
            return insertionPoint - 1;
        }
        return index;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        int index;
        output.appendModelIdentityElement(this);
        float value = this.property.get(item, level, owner, seed) * this.scale;
        ItemModel selectedModel = Float.isNaN(value) ? this.fallback : ((index = RangeSelectItemModel.lastIndexLessOrEqual(this.thresholds, value)) == -1 ? this.fallback : this.models[index]);
        selectedModel.update(output, item, resolver, displayContext, level, owner, seed);
    }

    @Environment(value=EnvType.CLIENT)
    public record Entry(float threshold, ItemModel.Unbaked model) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.FLOAT.fieldOf("threshold").forGetter(Entry::threshold), (App)ItemModels.CODEC.fieldOf("model").forGetter(Entry::model)).apply((Applicative)i, Entry::new));
        public static final Comparator<Entry> BY_THRESHOLD = Comparator.comparingDouble(Entry::threshold);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Optional<Transformation> transformation, RangeSelectItemModelProperty property, float scale, List<Entry> entries, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation), (App)RangeSelectItemModelProperties.MAP_CODEC.forGetter(Unbaked::property), (App)Codec.FLOAT.optionalFieldOf("scale", (Object)Float.valueOf(1.0f)).forGetter(Unbaked::scale), (App)Entry.CODEC.listOf().fieldOf("entries").forGetter(Unbaked::entries), (App)ItemModels.CODEC.optionalFieldOf("fallback").forGetter(Unbaked::fallback)).apply((Applicative)i, Unbaked::new));

        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            float[] thresholds = new float[this.entries.size()];
            ItemModel[] models = new ItemModel[this.entries.size()];
            ArrayList<Entry> mutableEntries = new ArrayList<Entry>(this.entries);
            mutableEntries.sort(Entry.BY_THRESHOLD);
            for (int i = 0; i < mutableEntries.size(); ++i) {
                Entry entry = (Entry)mutableEntries.get(i);
                thresholds[i] = entry.threshold;
                models[i] = entry.model.bake(context, childTransform);
            }
            ItemModel bakedFallback = this.fallback.map(m -> m.bake(context, childTransform)).orElseGet(() -> context.missingItemModel(childTransform));
            return new RangeSelectItemModel(this.property, this.scale, thresholds, models, bakedFallback);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.fallback.ifPresent(m -> m.resolveDependencies(resolver));
            this.entries.forEach(entry -> entry.model.resolveDependencies(resolver));
        }
    }
}

