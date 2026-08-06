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
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBookCoverFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetBookCoverFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetBookCoverFunction.commonFields(i).and(i.group((App)Filterable.codec(Codec.string((int)0, (int)32)).optionalFieldOf("title").forGetter(f -> f.title), (App)Codec.STRING.optionalFieldOf("author").forGetter(f -> f.author), (App)ExtraCodecs.intRange(0, 3).optionalFieldOf("generation").forGetter(f -> f.generation))).apply((Applicative)i, SetBookCoverFunction::new));
    private final Optional<String> author;
    private final Optional<Filterable<String>> title;
    private final Optional<Integer> generation;

    public SetBookCoverFunction(List<LootItemCondition> predicates, Optional<Filterable<String>> title, Optional<String> author, Optional<Integer> generation) {
        super(predicates);
        this.author = author;
        this.title = title;
        this.generation = generation;
    }

    @Override
    protected ItemStack run(ItemStack itemStack, LootContext context) {
        itemStack.update(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY, this::apply);
        return itemStack;
    }

    private WrittenBookContent apply(WrittenBookContent original) {
        return new WrittenBookContent(this.title.orElseGet(original::title), this.author.orElseGet(original::author), this.generation.orElseGet(original::generation), original.pages(), original.resolved());
    }

    public MapCodec<SetBookCoverFunction> codec() {
        return MAP_CODEC;
    }
}

