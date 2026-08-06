/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 */
package net.minecraft.advancements.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Optional;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentPredicate(Optional<HolderSet<Enchantment>> enchantments, MinMaxBounds.Ints level) {
    public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("enchantments").forGetter(EnchantmentPredicate::enchantments), (App)MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", (Object)MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)).apply((Applicative)i, EnchantmentPredicate::new));

    public EnchantmentPredicate(Holder<Enchantment> enchantment, MinMaxBounds.Ints level) {
        this(Optional.of(HolderSet.direct(enchantment)), level);
    }

    public EnchantmentPredicate(HolderSet<Enchantment> enchantments, MinMaxBounds.Ints level) {
        this(Optional.of(enchantments), level);
    }

    public boolean containedIn(ItemEnchantments itemEnchantments) {
        if (this.enchantments.isPresent()) {
            for (Holder holder : this.enchantments.get()) {
                if (!this.matchesEnchantment(itemEnchantments, holder)) continue;
                return true;
            }
            return false;
        }
        if (this.level != MinMaxBounds.Ints.ANY) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantments.entrySet()) {
                if (!this.level.matches(entry.getIntValue())) continue;
                return true;
            }
            return false;
        }
        return !itemEnchantments.isEmpty();
    }

    private boolean matchesEnchantment(ItemEnchantments itemEnchantments, Holder<Enchantment> enchantment) {
        int level = itemEnchantments.getLevel(enchantment);
        if (level == 0) {
            return false;
        }
        if (this.level == MinMaxBounds.Ints.ANY) {
            return true;
        }
        return this.level.matches(level);
    }
}

