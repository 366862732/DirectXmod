/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetBannerPatternFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetBannerPatternFunction.commonFields(i).and(i.group((App)BannerPatternLayers.CODEC.fieldOf("patterns").forGetter(f -> f.patterns), (App)Codec.BOOL.fieldOf("append").forGetter(f -> f.append))).apply((Applicative)i, SetBannerPatternFunction::new));
    private final BannerPatternLayers patterns;
    private final boolean append;

    private SetBannerPatternFunction(List<LootItemCondition> predicates, BannerPatternLayers patterns, boolean append) {
        super(predicates);
        this.patterns = patterns;
        this.append = append;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        if (this.append) {
            itemStack.update(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY, this.patterns, (base, appended) -> new BannerPatternLayers.Builder().addAll((BannerPatternLayers)base).addAll((BannerPatternLayers)appended).build());
        } else {
            itemStack.set(DataComponents.BANNER_PATTERNS, this.patterns);
        }
        return itemStack;
    }

    public MapCodec<SetBannerPatternFunction> codec() {
        return MAP_CODEC;
    }

    public static Builder setBannerPattern(boolean append) {
        return new Builder(append);
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final BannerPatternLayers.Builder patterns = new BannerPatternLayers.Builder();
        private final boolean append;

        private Builder(boolean append) {
            this.append = append;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
        }

        public Builder addPattern(Holder<BannerPattern> pattern, DyeColor color) {
            this.patterns.add(pattern, color);
            return this;
        }
    }
}

