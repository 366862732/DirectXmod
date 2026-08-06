/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.syncher;

import net.minecraft.network.syncher.EntityDataSerializer;

public record EntityDataAccessor<T>(int id, EntityDataSerializer<T> serializer) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        EntityDataAccessor that = (EntityDataAccessor)o;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return "<entity data: " + this.id + ">";
    }
}

