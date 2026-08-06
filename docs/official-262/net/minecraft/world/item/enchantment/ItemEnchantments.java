/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

public class ItemEnchantments
implements TooltipProvider {
    public static final ItemEnchantments EMPTY = new ItemEnchantments((Object2IntOpenHashMap<Holder<Enchantment>>)new Object2IntOpenHashMap());
    private static final Codec<Integer> LEVEL_CODEC = Codec.intRange((int)1, (int)255);
    public static final Codec<ItemEnchantments> CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC).xmap(map -> new ItemEnchantments((Object2IntOpenHashMap<Holder<Enchantment>>)new Object2IntOpenHashMap(map)), enchantments -> enchantments.enchantments);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT), c -> c.enchantments, ItemEnchantments::new);
    private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;

    private ItemEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> enchantments) {
        this.enchantments = enchantments;
        for (Object2IntMap.Entry entry : enchantments.object2IntEntrySet()) {
            int level = entry.getIntValue();
            if (level >= 0 && level <= 255) continue;
            throw new IllegalArgumentException("Enchantment " + String.valueOf(entry.getKey()) + " has invalid level " + level);
        }
    }

    public int getLevel(Holder<Enchantment> enchantment) {
        return this.enchantments.getInt(enchantment);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        HolderLookup.Provider registries = context.registries();
        HolderSet<Enchantment> order = ItemEnchantments.getTagOrEmpty(registries, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);
        for (Holder holder : order) {
            int level = this.enchantments.getInt((Object)holder);
            if (level <= 0) continue;
            consumer.accept(Enchantment.getFullname(holder, level));
        }
        for (Object2IntMap.Entry entry : this.enchantments.object2IntEntrySet()) {
            Holder enchantment = (Holder)entry.getKey();
            if (order.contains(enchantment)) continue;
            consumer.accept(Enchantment.getFullname((Holder)entry.getKey(), entry.getIntValue()));
        }
    }

    private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider registries, ResourceKey<Registry<T>> registry, TagKey<T> tag) {
        Optional<HolderSet.Named<T>> maybeOrder;
        if (registries != null && (maybeOrder = registries.lookupOrThrow(registry).get(tag)).isPresent()) {
            return maybeOrder.get();
        }
        return HolderSet.empty();
    }

    public Set<Holder<Enchantment>> keySet() {
        return Collections.unmodifiableSet(this.enchantments.keySet());
    }

    public Set<Object2IntMap.Entry<Holder<Enchantment>>> entrySet() {
        return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
    }

    public int size() {
        return this.enchantments.size();
    }

    public boolean isEmpty() {
        return this.enchantments.isEmpty();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ItemEnchantments) {
            ItemEnchantments that = (ItemEnchantments)obj;
            return this.enchantments.equals(that.enchantments);
        }
        return false;
    }

    public int hashCode() {
        return this.enchantments.hashCode();
    }

    public String toString() {
        return "ItemEnchantments{enchantments=" + String.valueOf(this.enchantments) + "}";
    }

    public static class Mutable {
        private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();

        public Mutable(ItemEnchantments enchantments) {
            this.enchantments.putAll(enchantments.enchantments);
        }

        public void set(Holder<Enchantment> enchantment, int level) {
            if (level <= 0) {
                this.enchantments.removeInt(enchantment);
            } else {
                this.enchantments.put(enchantment, Math.min(level, 255));
            }
        }

        public void upgrade(Holder<Enchantment> enchantment, int level) {
            if (level > 0) {
                this.enchantments.merge(enchantment, Math.min(level, 255), Integer::max);
            }
        }

        public void removeIf(Predicate<Holder<Enchantment>> predicate) {
            this.enchantments.keySet().removeIf(predicate);
        }

        public int getLevel(Holder<Enchantment> enchantment) {
            return this.enchantments.getOrDefault(enchantment, 0);
        }

        public Set<Holder<Enchantment>> keySet() {
            return this.enchantments.keySet();
        }

        public ItemEnchantments toImmutable() {
            return new ItemEnchantments(this.enchantments);
        }
    }
}

