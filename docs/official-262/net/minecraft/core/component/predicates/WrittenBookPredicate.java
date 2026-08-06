/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.core.component.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

public record WrittenBookPredicate(Optional<CollectionPredicate<Filterable<Component>, PagePredicate>> pages, Optional<String> author, Optional<String> title, MinMaxBounds.Ints generation, Optional<Boolean> resolved) implements SingleComponentItemPredicate<WrittenBookContent>
{
    public static final Codec<WrittenBookPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)CollectionPredicate.codec(PagePredicate.CODEC).optionalFieldOf("pages").forGetter(WrittenBookPredicate::pages), (App)Codec.STRING.optionalFieldOf("author").forGetter(WrittenBookPredicate::author), (App)Codec.STRING.optionalFieldOf("title").forGetter(WrittenBookPredicate::title), (App)MinMaxBounds.Ints.CODEC.optionalFieldOf("generation", (Object)MinMaxBounds.Ints.ANY).forGetter(WrittenBookPredicate::generation), (App)Codec.BOOL.optionalFieldOf("resolved").forGetter(WrittenBookPredicate::resolved)).apply((Applicative)i, WrittenBookPredicate::new));

    @Override
    public DataComponentType<WrittenBookContent> componentType() {
        return DataComponents.WRITTEN_BOOK_CONTENT;
    }

    @Override
    public boolean matches(WrittenBookContent value) {
        if (this.author.isPresent() && !this.author.get().equals(value.author())) {
            return false;
        }
        if (this.title.isPresent() && !this.title.get().equals(value.title().raw())) {
            return false;
        }
        if (!this.generation.matches(value.generation())) {
            return false;
        }
        if (this.resolved.isPresent() && this.resolved.get().booleanValue() != value.resolved()) {
            return false;
        }
        return !this.pages.isPresent() || this.pages.get().test(value.pages());
    }

    public record PagePredicate(Component contents) implements Predicate<Filterable<Component>>
    {
        public static final Codec<PagePredicate> CODEC = ComponentSerialization.CODEC.xmap(PagePredicate::new, PagePredicate::contents);

        @Override
        public boolean test(Filterable<Component> value) {
            return value.raw().equals(this.contents);
        }
    }
}

