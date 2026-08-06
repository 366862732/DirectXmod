/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.entity;

import org.jspecify.annotations.Nullable;

public interface EntityTypeTest<B, T extends B> {
    public static <B, T extends B> EntityTypeTest<B, T> forClass(final Class<T> cls) {
        return new EntityTypeTest<B, T>(){

            @Override
            public @Nullable T tryCast(B entity) {
                return cls.isInstance(entity) ? entity : null;
            }

            @Override
            public Class<? extends B> getBaseClass() {
                return cls;
            }
        };
    }

    public static <B, T extends B> EntityTypeTest<B, T> forExactClass(final Class<T> cls) {
        return new EntityTypeTest<B, T>(){

            @Override
            public @Nullable T tryCast(B entity) {
                return cls.equals(entity.getClass()) ? entity : null;
            }

            @Override
            public Class<? extends B> getBaseClass() {
                return cls;
            }
        };
    }

    public @Nullable T tryCast(B var1);

    public Class<? extends B> getBaseClass();
}

