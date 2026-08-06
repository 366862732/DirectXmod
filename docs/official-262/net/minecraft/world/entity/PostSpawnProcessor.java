/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.Objects;
import net.minecraft.world.entity.Entity;

public interface PostSpawnProcessor<T extends Entity> {
    public void apply(T var1);

    default public PostSpawnProcessor<T> andThen(PostSpawnProcessor<? super T> after) {
        Objects.requireNonNull(after);
        return t -> {
            this.apply(t);
            after.apply(t);
        };
    }

    public static <T extends Entity> PostSpawnProcessor<T> nop() {
        return entity -> {};
    }
}

