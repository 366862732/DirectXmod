/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.entity;

import java.util.UUID;
import net.minecraft.world.level.entity.UniquelyIdentifyable;
import org.jspecify.annotations.Nullable;

public interface UUIDLookup<IdentifiedType extends UniquelyIdentifyable> {
    public @Nullable IdentifiedType lookup(UUID var1);
}

