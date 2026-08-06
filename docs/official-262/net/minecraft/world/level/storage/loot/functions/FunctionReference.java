/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference
extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<FunctionReference> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> FunctionReference.commonFields(i).and((App)ResourceKey.codec(Registries.ITEM_MODIFIER).fieldOf("name").forGetter(f -> f.name)).apply((Applicative)i, FunctionReference::new));
    private final ResourceKey<LootItemFunction> name;

    private FunctionReference(List<LootItemCondition> predicates, ResourceKey<LootItemFunction> name) {
        super(predicates);
        this.name = name;
    }

    public MapCodec<FunctionReference> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validateReference(context, this.name);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        LootItemFunction function = context.getResolver().get(this.name).map(Holder::value).orElse(null);
        if (function == null) {
            LOGGER.warn("Unknown function: {}", (Object)this.name.identifier());
            return itemStack;
        }
        LootContext.VisitedEntry<LootItemFunction> breadcrumb = LootContext.createVisitedEntry(function);
        if (context.pushVisitedElement(breadcrumb)) {
            try {
                ItemStack itemStack2 = (ItemStack)function.apply(itemStack, context);
                return itemStack2;
            }
            finally {
                context.popVisitedElement(breadcrumb);
            }
        }
        LOGGER.warn("Detected infinite loop in loot tables");
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> functionReference(ResourceKey<LootItemFunction> name) {
        return FunctionReference.simpleBuilder(conditions -> new FunctionReference((List<LootItemCondition>)conditions, name));
    }
}

