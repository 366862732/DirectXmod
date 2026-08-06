/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.multiplayer;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CacheSlot<C extends Cleaner<C>, D> {
    private final Function<C, D> operation;
    private @Nullable C context;
    private @Nullable D value;

    public CacheSlot(Function<C, D> operation) {
        this.operation = operation;
    }

    public D compute(C context) {
        if (context == this.context && this.value != null) {
            return this.value;
        }
        D newValue = this.operation.apply(context);
        this.value = newValue;
        this.context = context;
        context.registerForCleaning(this);
        return newValue;
    }

    public void clear() {
        this.value = null;
        this.context = null;
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface Cleaner<C extends Cleaner<C>> {
        public void registerForCleaning(CacheSlot<C, ?> var1);
    }
}

