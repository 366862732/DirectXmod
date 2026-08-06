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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ToggleTooltips
extends LootItemConditionalFunction {
    public static final MapCodec<ToggleTooltips> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> ToggleTooltips.commonFields(i).and((App)Codec.unboundedMap(DataComponentType.CODEC, (Codec)Codec.BOOL).fieldOf("toggles").forGetter(e -> e.values)).apply((Applicative)i, ToggleTooltips::new));
    private final Map<DataComponentType<?>, Boolean> values;

    private ToggleTooltips(List<LootItemCondition> predicates, Map<DataComponentType<?>, Boolean> values) {
        super(predicates);
        this.values = values;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.update(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT, display -> {
            Iterator<Map.Entry<DataComponentType<?>, Boolean>> i$ = this.values.entrySet().iterator();
            while (i$.hasNext()) {
                boolean shown;
                Map.Entry<DataComponentType<?>, Boolean> entry;
                display = display.withHidden(entry.getKey(), !(shown = (entry = i$.next()).getValue().booleanValue()));
            }
            return display;
        });
        return itemStack;
    }

    public MapCodec<ToggleTooltips> codec() {
        return MAP_CODEC;
    }
}

