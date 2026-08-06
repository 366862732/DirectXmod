/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DSL$TypeReference
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class MobEffectIdFix
extends DataFix {
    private static final Int2ObjectMap<String> ID_MAP = (Int2ObjectMap)Util.make(new Int2ObjectOpenHashMap(), m -> {
        m.put(1, (Object)"minecraft:speed");
        m.put(2, (Object)"minecraft:slowness");
        m.put(3, (Object)"minecraft:haste");
        m.put(4, (Object)"minecraft:mining_fatigue");
        m.put(5, (Object)"minecraft:strength");
        m.put(6, (Object)"minecraft:instant_health");
        m.put(7, (Object)"minecraft:instant_damage");
        m.put(8, (Object)"minecraft:jump_boost");
        m.put(9, (Object)"minecraft:nausea");
        m.put(10, (Object)"minecraft:regeneration");
        m.put(11, (Object)"minecraft:resistance");
        m.put(12, (Object)"minecraft:fire_resistance");
        m.put(13, (Object)"minecraft:water_breathing");
        m.put(14, (Object)"minecraft:invisibility");
        m.put(15, (Object)"minecraft:blindness");
        m.put(16, (Object)"minecraft:night_vision");
        m.put(17, (Object)"minecraft:hunger");
        m.put(18, (Object)"minecraft:weakness");
        m.put(19, (Object)"minecraft:poison");
        m.put(20, (Object)"minecraft:wither");
        m.put(21, (Object)"minecraft:health_boost");
        m.put(22, (Object)"minecraft:absorption");
        m.put(23, (Object)"minecraft:saturation");
        m.put(24, (Object)"minecraft:glowing");
        m.put(25, (Object)"minecraft:levitation");
        m.put(26, (Object)"minecraft:luck");
        m.put(27, (Object)"minecraft:unluck");
        m.put(28, (Object)"minecraft:slow_falling");
        m.put(29, (Object)"minecraft:conduit_power");
        m.put(30, (Object)"minecraft:dolphins_grace");
        m.put(31, (Object)"minecraft:bad_omen");
        m.put(32, (Object)"minecraft:hero_of_the_village");
        m.put(33, (Object)"minecraft:darkness");
    });
    private static final Set<String> MOB_EFFECT_INSTANCE_CARRIER_ITEMS = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");

    public MobEffectIdFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    private static <T> Optional<Dynamic<T>> getAndConvertMobEffectId(Dynamic<T> obj, String fieldName) {
        return obj.get(fieldName).asNumber().result().map(id -> (String)ID_MAP.get(id.intValue())).map(arg_0 -> obj.createString(arg_0));
    }

    private static <T> Dynamic<T> updateMobEffectIdField(Dynamic<T> input, String oldFieldName, Dynamic<T> output, String newFieldName) {
        Optional<Dynamic<T>> mappedId = MobEffectIdFix.getAndConvertMobEffectId(input, oldFieldName);
        return output.replaceField(oldFieldName, newFieldName, mappedId);
    }

    private static <T> Dynamic<T> updateMobEffectIdField(Dynamic<T> input, String oldFieldName, String newFieldName) {
        return MobEffectIdFix.updateMobEffectIdField(input, oldFieldName, input, newFieldName);
    }

    private static <T> Dynamic<T> updateMobEffectInstance(Dynamic<T> input) {
        input = MobEffectIdFix.updateMobEffectIdField(input, "Id", "id");
        input = input.renameField("Ambient", "ambient");
        input = input.renameField("Amplifier", "amplifier");
        input = input.renameField("Duration", "duration");
        input = input.renameField("ShowParticles", "show_particles");
        input = input.renameField("ShowIcon", "show_icon");
        Optional<Dynamic> hiddenEffect = input.get("HiddenEffect").result().map(MobEffectIdFix::updateMobEffectInstance);
        return input.replaceField("HiddenEffect", "hidden_effect", hiddenEffect);
    }

    private static <T> Dynamic<T> updateMobEffectInstanceList(Dynamic<T> input, String oldField, String newField) {
        Optional<Dynamic> newValue = input.get(oldField).asStreamOpt().result().map(effects -> input.createList(effects.map(MobEffectIdFix::updateMobEffectInstance)));
        return input.replaceField(oldField, newField, newValue);
    }

    private static <T> Dynamic<T> updateSuspiciousStewEntry(Dynamic<T> input, Dynamic<T> output) {
        output = MobEffectIdFix.updateMobEffectIdField(input, "EffectId", output, "id");
        Optional duration = input.get("EffectDuration").result();
        return output.replaceField("EffectDuration", "duration", duration);
    }

    private static <T> Dynamic<T> updateSuspiciousStewEntry(Dynamic<T> input) {
        return MobEffectIdFix.updateSuspiciousStewEntry(input, input);
    }

    private Typed<?> updateNamedChoice(Typed<?> input, DSL.TypeReference typeReference, String name, Function<Dynamic<?>, Dynamic<?>> function) {
        Type oldType = this.getInputSchema().getChoiceType(typeReference, name);
        Type newType = this.getOutputSchema().getChoiceType(typeReference, name);
        return input.updateTyped(DSL.namedChoice((String)name, (Type)oldType), newType, typedTag -> typedTag.update(DSL.remainderFinder(), function));
    }

    private TypeRewriteRule blockEntityFixer() {
        Type blockEntityType = this.getInputSchema().getType(References.BLOCK_ENTITY);
        return this.fixTypeEverywhereTyped("BlockEntityMobEffectIdFix", blockEntityType, input -> {
            input = this.updateNamedChoice((Typed<?>)input, References.BLOCK_ENTITY, "minecraft:beacon", tag -> {
                tag = MobEffectIdFix.updateMobEffectIdField(tag, "Primary", "primary_effect");
                return MobEffectIdFix.updateMobEffectIdField(tag, "Secondary", "secondary_effect");
            });
            return input;
        });
    }

    private static <T> Dynamic<T> fixMooshroomTag(Dynamic<T> entityTag) {
        Dynamic initialEntry = entityTag.emptyMap();
        Dynamic<T> entry = MobEffectIdFix.updateSuspiciousStewEntry(entityTag, initialEntry);
        if (!entry.equals((Object)initialEntry)) {
            entityTag = entityTag.set("stew_effects", entityTag.createList(Stream.of(entry)));
        }
        return entityTag.remove("EffectId").remove("EffectDuration");
    }

    private static <T> Dynamic<T> fixArrowTag(Dynamic<T> data) {
        return MobEffectIdFix.updateMobEffectInstanceList(data, "CustomPotionEffects", "custom_potion_effects");
    }

    private static <T> Dynamic<T> fixAreaEffectCloudTag(Dynamic<T> data) {
        return MobEffectIdFix.updateMobEffectInstanceList(data, "Effects", "effects");
    }

    private static Dynamic<?> updateLivingEntityTag(Dynamic<?> data) {
        return MobEffectIdFix.updateMobEffectInstanceList(data, "ActiveEffects", "active_effects");
    }

    private TypeRewriteRule entityFixer() {
        Type entityType = this.getInputSchema().getType(References.ENTITY);
        return this.fixTypeEverywhereTyped("EntityMobEffectIdFix", entityType, input -> {
            input = this.updateNamedChoice((Typed<?>)input, References.ENTITY, "minecraft:mooshroom", MobEffectIdFix::fixMooshroomTag);
            input = this.updateNamedChoice((Typed<?>)input, References.ENTITY, "minecraft:arrow", MobEffectIdFix::fixArrowTag);
            input = this.updateNamedChoice((Typed<?>)input, References.ENTITY, "minecraft:area_effect_cloud", MobEffectIdFix::fixAreaEffectCloudTag);
            input = input.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag);
            return input;
        });
    }

    private TypeRewriteRule playerFixer() {
        Type playerType = this.getInputSchema().getType(References.PLAYER);
        return this.fixTypeEverywhereTyped("PlayerMobEffectIdFix", playerType, input -> input.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag));
    }

    private static <T> Dynamic<T> fixSuspiciousStewTag(Dynamic<T> tag) {
        Optional<Dynamic> effectsList = tag.get("Effects").asStreamOpt().result().map(list -> tag.createList(list.map(MobEffectIdFix::updateSuspiciousStewEntry)));
        return tag.replaceField("Effects", "effects", effectsList);
    }

    private TypeRewriteRule itemStackFixer() {
        OpticFinder idF = DSL.fieldFinder((String)"id", (Type)DSL.named((String)References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder tagF = itemStackType.findField("tag");
        return this.fixTypeEverywhereTyped("ItemStackMobEffectIdFix", itemStackType, input -> {
            Optional idOpt = input.getOptional(idF);
            if (idOpt.isPresent()) {
                String id = (String)((Pair)idOpt.get()).getSecond();
                if (id.equals("minecraft:suspicious_stew")) {
                    return input.updateTyped(tagF, itemTag -> itemTag.update(DSL.remainderFinder(), MobEffectIdFix::fixSuspiciousStewTag));
                }
                if (MOB_EFFECT_INSTANCE_CARRIER_ITEMS.contains(id)) {
                    return input.updateTyped(tagF, itemTag -> itemTag.update(DSL.remainderFinder(), tag -> MobEffectIdFix.updateMobEffectInstanceList(tag, "CustomPotionEffects", "custom_potion_effects")));
                }
            }
            return input;
        });
    }

    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq((TypeRewriteRule)this.blockEntityFixer(), (TypeRewriteRule[])new TypeRewriteRule[]{this.entityFixer(), this.playerFixer(), this.itemStackFixer()});
    }
}

