/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.jspecify.annotations.Nullable;

public interface LootContextArg<R> {
    public static final Codec<LootContextArg<Object>> ENTITY_OR_BLOCK = LootContextArg.createArgCodec(builder -> builder.anyOf(LootContext.EntityTarget.values()).anyOf(LootContext.BlockEntityTarget.values()));

    public @Nullable R get(LootContext var1);

    public ContextKey<?> contextParam();

    public static <U> LootContextArg<U> cast(LootContextArg<? extends U> original) {
        return original;
    }

    public static <R> Codec<LootContextArg<R>> createArgCodec(UnaryOperator<ArgCodecBuilder<R>> consumer) {
        return ((ArgCodecBuilder)consumer.apply(new ArgCodecBuilder())).build();
    }

    public static final class ArgCodecBuilder<R> {
        private final ExtraCodecs.LateBoundIdMapper<String, LootContextArg<R>> sources = new ExtraCodecs.LateBoundIdMapper();

        private ArgCodecBuilder() {
        }

        public <T> ArgCodecBuilder<R> anyOf(T[] targets, Function<T, String> nameGetter, Function<T, ? extends LootContextArg<R>> argFactory) {
            for (T target : targets) {
                this.sources.put(nameGetter.apply(target), argFactory.apply(target));
            }
            return this;
        }

        public <T extends StringRepresentable> ArgCodecBuilder<R> anyOf(T[] targets, Function<T, ? extends LootContextArg<R>> argFactory) {
            return this.anyOf(targets, StringRepresentable::getSerializedName, argFactory);
        }

        public <T extends StringRepresentable & LootContextArg<? extends R>> ArgCodecBuilder<R> anyOf(T[] targets) {
            return this.anyOf((StringRepresentable[])targets, x$0 -> LootContextArg.cast((LootContextArg)((Object)x$0)));
        }

        public ArgCodecBuilder<R> anyEntity(Function<? super ContextKey<? extends Entity>, ? extends LootContextArg<R>> function) {
            return this.anyOf(LootContext.EntityTarget.values(), target -> (LootContextArg)function.apply(target.contextParam()));
        }

        public ArgCodecBuilder<R> anyBlockEntity(Function<? super ContextKey<? extends BlockEntity>, ? extends LootContextArg<R>> function) {
            return this.anyOf(LootContext.BlockEntityTarget.values(), target -> (LootContextArg)function.apply(target.contextParam()));
        }

        public ArgCodecBuilder<R> anyItemStack(Function<? super ContextKey<? extends ItemInstance>, ? extends LootContextArg<R>> function) {
            return this.anyOf(LootContext.ItemStackTarget.values(), target -> (LootContextArg)function.apply(target.contextParam()));
        }

        private Codec<LootContextArg<R>> build() {
            return this.sources.codec((Codec<String>)Codec.STRING);
        }
    }

    public static interface SimpleGetter<T>
    extends LootContextArg<T> {
        @Override
        public ContextKey<? extends T> contextParam();

        @Override
        default public @Nullable T get(LootContext context) {
            return context.getOptionalParameter(this.contextParam());
        }
    }

    public static interface Getter<T, R>
    extends LootContextArg<R> {
        public @Nullable R get(T var1);

        @Override
        public ContextKey<? extends T> contextParam();

        @Override
        default public @Nullable R get(LootContext context) {
            T value = context.getOptionalParameter(this.contextParam());
            return value != null ? (R)this.get(value) : null;
        }
    }
}

