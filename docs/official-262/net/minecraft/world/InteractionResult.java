/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public sealed interface InteractionResult {
    public static final Success SUCCESS = new Success(SwingSource.CLIENT, ItemContext.DEFAULT);
    public static final Success SUCCESS_SERVER = new Success(SwingSource.SERVER, ItemContext.DEFAULT);
    public static final Success CONSUME = new Success(SwingSource.NONE, ItemContext.DEFAULT);
    public static final Fail FAIL = new Fail();
    public static final Pass PASS = new Pass();
    public static final TryEmptyHandInteraction TRY_WITH_EMPTY_HAND = new TryEmptyHandInteraction();

    default public boolean consumesAction() {
        return false;
    }

    public record Success(SwingSource swingSource, ItemContext itemContext) implements InteractionResult
    {
        @Override
        public boolean consumesAction() {
            return true;
        }

        public Success heldItemTransformedTo(ItemStack itemStack) {
            return new Success(this.swingSource, new ItemContext(true, itemStack));
        }

        public Success withoutItem() {
            return new Success(this.swingSource, ItemContext.NONE);
        }

        public boolean wasItemInteraction() {
            return this.itemContext.wasItemInteraction;
        }

        public @Nullable ItemStack heldItemTransformedTo() {
            return this.itemContext.heldItemTransformedTo;
        }
    }

    public static enum SwingSource {
        NONE,
        CLIENT,
        SERVER;

    }

    public record ItemContext(boolean wasItemInteraction, @Nullable ItemStack heldItemTransformedTo) {
        public static final ItemContext NONE = new ItemContext(false, null);
        public static final ItemContext DEFAULT = new ItemContext(true, null);
    }

    public record Fail() implements InteractionResult
    {
    }

    public record Pass() implements InteractionResult
    {
    }

    public record TryEmptyHandInteraction() implements InteractionResult
    {
    }
}

