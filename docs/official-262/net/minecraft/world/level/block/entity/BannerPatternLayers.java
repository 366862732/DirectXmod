/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.slf4j.Logger;

public record BannerPatternLayers(List<Layer> layers) implements TooltipProvider
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BannerPatternLayers EMPTY = new BannerPatternLayers(List.of());
    public static final Codec<BannerPatternLayers> CODEC = Layer.CODEC.listOf().xmap(BannerPatternLayers::new, BannerPatternLayers::layers);
    public static final StreamCodec<RegistryFriendlyByteBuf, BannerPatternLayers> STREAM_CODEC = Layer.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BannerPatternLayers::new, BannerPatternLayers::layers);

    public BannerPatternLayers removeLast() {
        return new BannerPatternLayers(List.copyOf(this.layers.subList(0, this.layers.size() - 1)));
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        for (int i = 0; i < Math.min(this.layers().size(), 6); ++i) {
            consumer.accept(this.layers().get(i).description().withStyle(ChatFormatting.GRAY));
        }
    }

    public record Layer(Holder<BannerPattern> pattern, DyeColor color) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group((App)BannerPattern.CODEC.fieldOf("pattern").forGetter(Layer::pattern), (App)DyeColor.CODEC.fieldOf("color").forGetter(Layer::color)).apply((Applicative)i, Layer::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Layer> STREAM_CODEC = StreamCodec.composite(BannerPattern.STREAM_CODEC, Layer::pattern, DyeColor.STREAM_CODEC, Layer::color, Layer::new);

        public MutableComponent description() {
            String prefix = this.pattern.value().translationKey();
            return Component.translatable(prefix + "." + this.color.getName());
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<Layer> layers = ImmutableList.builder();

        @Deprecated
        public Builder addIfRegistered(HolderGetter<BannerPattern> patternGetter, ResourceKey<BannerPattern> patternKey, DyeColor color) {
            Optional<Holder.Reference<BannerPattern>> pattern = patternGetter.get(patternKey);
            if (pattern.isEmpty()) {
                LOGGER.warn("Unable to find banner pattern with id: '{}'", (Object)patternKey.identifier());
                return this;
            }
            return this.add((Holder<BannerPattern>)pattern.get(), color);
        }

        public Builder add(Holder<BannerPattern> pattern, DyeColor color) {
            return this.add(new Layer(pattern, color));
        }

        public Builder add(Layer layer) {
            this.layers.add((Object)layer);
            return this;
        }

        public Builder addAll(BannerPatternLayers layers) {
            this.layers.addAll(layers.layers);
            return this;
        }

        public BannerPatternLayers build() {
            return new BannerPatternLayers((List<Layer>)this.layers.build());
        }
    }
}

