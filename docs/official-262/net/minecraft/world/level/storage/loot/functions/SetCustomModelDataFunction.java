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
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.ListOperation;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetCustomModelDataFunction
extends LootItemConditionalFunction {
    private static final Codec<NumberProvider> COLOR_PROVIDER_CODEC = Codec.withAlternative(NumberProviders.CODEC, ExtraCodecs.RGB_COLOR_CODEC, ConstantValue::new);
    public static final MapCodec<SetCustomModelDataFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetCustomModelDataFunction.commonFields(i).and(i.group((App)ListOperation.StandAlone.codec(NumberProviders.CODEC, Integer.MAX_VALUE).optionalFieldOf("floats").forGetter(o -> o.floats), (App)ListOperation.StandAlone.codec(Codec.BOOL, Integer.MAX_VALUE).optionalFieldOf("flags").forGetter(o -> o.flags), (App)ListOperation.StandAlone.codec(Codec.STRING, Integer.MAX_VALUE).optionalFieldOf("strings").forGetter(o -> o.strings), (App)ListOperation.StandAlone.codec(COLOR_PROVIDER_CODEC, Integer.MAX_VALUE).optionalFieldOf("colors").forGetter(o -> o.colors))).apply((Applicative)i, SetCustomModelDataFunction::new));
    private final Optional<ListOperation.StandAlone<NumberProvider>> floats;
    private final Optional<ListOperation.StandAlone<Boolean>> flags;
    private final Optional<ListOperation.StandAlone<String>> strings;
    private final Optional<ListOperation.StandAlone<NumberProvider>> colors;

    public SetCustomModelDataFunction(List<LootItemCondition> predicates, Optional<ListOperation.StandAlone<NumberProvider>> floats, Optional<ListOperation.StandAlone<Boolean>> flags, Optional<ListOperation.StandAlone<String>> strings, Optional<ListOperation.StandAlone<NumberProvider>> colors) {
        super(predicates);
        this.floats = floats;
        this.flags = flags;
        this.strings = strings;
        this.colors = colors;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        this.floats.ifPresent(f -> Validatable.validate(context, "floats", f.value()));
        this.colors.ifPresent(c -> Validatable.validate(context, "colors", c.value()));
    }

    public MapCodec<SetCustomModelDataFunction> codec() {
        return MAP_CODEC;
    }

    private static <T> List<T> apply(Optional<ListOperation.StandAlone<T>> operation, List<T> current) {
        return operation.map(o -> o.apply(current)).orElse(current);
    }

    private static <T, E> List<E> apply(Optional<ListOperation.StandAlone<T>> operation, List<E> current, Function<T, E> mapper) {
        return operation.map(o -> {
            List transformedReplacement = o.value().stream().map(mapper).toList();
            return o.operation().apply(current, transformedReplacement);
        }).orElse(current);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        CustomModelData component = itemStack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(SetCustomModelDataFunction.apply(this.floats, component.floats(), provider -> Float.valueOf(provider.getFloat(context))), SetCustomModelDataFunction.apply(this.flags, component.flags()), SetCustomModelDataFunction.apply(this.strings, component.strings()), SetCustomModelDataFunction.apply(this.colors, component.colors(), provider -> provider.getInt(context))));
        return itemStack;
    }
}

