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
package net.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class TagEntry
extends LootPoolSingletonContainer {
    public static final MapCodec<TagEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)TagKey.codec(Registries.ITEM).fieldOf("name").forGetter(e -> e.tag), (App)Codec.BOOL.fieldOf("expand").forGetter(e -> e.expand)).and(TagEntry.singletonFields(i)).apply((Applicative)i, TagEntry::new));
    private final TagKey<Item> tag;
    private final boolean expand;

    private TagEntry(TagKey<Item> tag, boolean expand, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.tag = tag;
        this.expand = expand;
    }

    public MapCodec<TagEntry> codec() {
        return MAP_CODEC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> output, LootContext context) {
        BuiltInRegistries.ITEM.getTagOrEmpty(this.tag).forEach(item -> output.accept(new ItemStack((Holder<Item>)item)));
    }

    private boolean expandTag(LootContext context, Consumer<LootPoolEntry> output) {
        if (this.canRun(context)) {
            for (final Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
                output.accept(new LootPoolSingletonContainer.EntryBase(this){
                    {
                        Objects.requireNonNull(this$0);
                        super(this$0);
                    }

                    @Override
                    public void createItemStack(Consumer<ItemStack> output, LootContext context) {
                        output.accept(new ItemStack(item));
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean expand(LootContext context, Consumer<LootPoolEntry> output) {
        if (this.expand) {
            return this.expandTag(context, output);
        }
        return super.expand(context, output);
    }

    public static LootPoolSingletonContainer.Builder<?> tagContents(TagKey<Item> tag) {
        return TagEntry.simpleBuilder((weight, quality, conditions, functions) -> new TagEntry(tag, false, weight, quality, conditions, functions));
    }

    public static LootPoolSingletonContainer.Builder<?> expandTag(TagKey<Item> tag) {
        return TagEntry.simpleBuilder((weight, quality, conditions, functions) -> new TagEntry(tag, true, weight, quality, conditions, functions));
    }
}

