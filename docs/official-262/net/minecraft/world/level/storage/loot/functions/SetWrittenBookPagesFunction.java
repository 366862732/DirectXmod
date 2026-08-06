/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ListOperation;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetWrittenBookPagesFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetWrittenBookPagesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetWrittenBookPagesFunction.commonFields(i).and(i.group((App)WrittenBookContent.PAGES_CODEC.fieldOf("pages").forGetter(f -> f.pages), (App)ListOperation.UNLIMITED_CODEC.forGetter(f -> f.pageOperation))).apply((Applicative)i, SetWrittenBookPagesFunction::new));
    private final List<Filterable<Component>> pages;
    private final ListOperation pageOperation;

    protected SetWrittenBookPagesFunction(List<LootItemCondition> predicates, List<Filterable<Component>> pages, ListOperation pageOperation) {
        super(predicates);
        this.pages = pages;
        this.pageOperation = pageOperation;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.update(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY, this::apply);
        return itemStack;
    }

    @VisibleForTesting
    public WrittenBookContent apply(WrittenBookContent original) {
        List<Filterable<Component>> newPages = this.pageOperation.apply(original.pages(), this.pages);
        return original.withReplacedPages((List)newPages);
    }

    public MapCodec<SetWrittenBookPagesFunction> codec() {
        return MAP_CODEC;
    }
}

