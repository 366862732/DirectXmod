/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ListOperation;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetWritableBookPagesFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetWritableBookPagesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetWritableBookPagesFunction.commonFields(i).and(i.group((App)WritableBookContent.PAGES_CODEC.fieldOf("pages").forGetter(f -> f.pages), (App)ListOperation.codec(100).forGetter(f -> f.pageOperation))).apply((Applicative)i, SetWritableBookPagesFunction::new));
    private final List<Filterable<String>> pages;
    private final ListOperation pageOperation;

    protected SetWritableBookPagesFunction(List<LootItemCondition> predicates, List<Filterable<String>> pages, ListOperation pageOperation) {
        super(predicates);
        this.pages = pages;
        this.pageOperation = pageOperation;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.update(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY, this::apply);
        return itemStack;
    }

    public WritableBookContent apply(WritableBookContent original) {
        List<Filterable<String>> newPages = this.pageOperation.apply(original.pages(), this.pages, 100);
        return original.withReplacedPages((List)newPages);
    }

    public MapCodec<SetWritableBookPagesFunction> codec() {
        return MAP_CODEC;
    }
}

