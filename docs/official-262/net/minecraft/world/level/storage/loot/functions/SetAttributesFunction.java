/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetAttributesFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetAttributesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetAttributesFunction.commonFields(i).and(i.group((App)Modifier.CODEC.listOf().fieldOf("modifiers").forGetter(f -> f.modifiers), (App)Codec.BOOL.optionalFieldOf("replace", (Object)true).forGetter(f -> f.replace))).apply((Applicative)i, SetAttributesFunction::new));
    private final List<Modifier> modifiers;
    private final boolean replace;

    private SetAttributesFunction(List<LootItemCondition> predicates, List<Modifier> modifiers, boolean replace) {
        super(predicates);
        this.modifiers = List.copyOf(modifiers);
        this.replace = replace;
    }

    public MapCodec<SetAttributesFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "modifiers", this.modifiers);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        if (this.replace) {
            itemStack.set(DataComponents.ATTRIBUTE_MODIFIERS, this.updateModifiers(context, ItemAttributeModifiers.EMPTY));
        } else {
            itemStack.update(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY, itemModifiers -> this.updateModifiers(context, (ItemAttributeModifiers)itemModifiers));
        }
        return itemStack;
    }

    private ItemAttributeModifiers updateModifiers(LootContext context, ItemAttributeModifiers itemModifiers) {
        RandomSource random = context.getRandom();
        for (Modifier modifier : this.modifiers) {
            EquipmentSlotGroup slot = Util.getRandom(modifier.slots, random);
            itemModifiers = itemModifiers.withModifierAdded(modifier.attribute, new AttributeModifier(modifier.id, modifier.amount.getFloat(context), modifier.operation), slot);
        }
        return itemModifiers;
    }

    public static ModifierBuilder modifier(Identifier id, Holder<Attribute> attribute, AttributeModifier.Operation operation, NumberProvider amount) {
        return new ModifierBuilder(id, attribute, operation, amount);
    }

    public static Builder setAttributes() {
        return new Builder();
    }

    private record Modifier(Identifier id, Holder<Attribute> attribute, AttributeModifier.Operation operation, NumberProvider amount, List<EquipmentSlotGroup> slots) implements LootContextUser
    {
        private static final Codec<List<EquipmentSlotGroup>> SLOTS_CODEC = ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(EquipmentSlotGroup.CODEC));
        public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.fieldOf("id").forGetter(Modifier::id), (App)Attribute.CODEC.fieldOf("attribute").forGetter(Modifier::attribute), (App)AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(Modifier::operation), (App)NumberProviders.CODEC.fieldOf("amount").forGetter(Modifier::amount), (App)SLOTS_CODEC.fieldOf("slot").forGetter(Modifier::slots)).apply((Applicative)i, Modifier::new));

        @Override
        public void validate(ValidationContext context) {
            LootContextUser.super.validate(context);
            Validatable.validate(context, "amount", this.amount);
        }
    }

    public static class ModifierBuilder {
        private final Identifier id;
        private final Holder<Attribute> attribute;
        private final AttributeModifier.Operation operation;
        private final NumberProvider amount;
        private final Set<EquipmentSlotGroup> slots = EnumSet.noneOf(EquipmentSlotGroup.class);

        public ModifierBuilder(Identifier id, Holder<Attribute> attribute, AttributeModifier.Operation operation, NumberProvider amount) {
            this.id = id;
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
        }

        public ModifierBuilder forSlot(EquipmentSlotGroup slot) {
            this.slots.add(slot);
            return this;
        }

        public Modifier build() {
            return new Modifier(this.id, this.attribute, this.operation, this.amount, List.copyOf(this.slots));
        }
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final boolean replace;
        private final List<Modifier> modifiers = Lists.newArrayList();

        public Builder(boolean replace) {
            this.replace = replace;
        }

        public Builder() {
            this(false);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withModifier(ModifierBuilder modifier) {
            this.modifiers.add(modifier.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetAttributesFunction(this.getConditions(), this.modifiers, this.replace);
        }
    }
}

