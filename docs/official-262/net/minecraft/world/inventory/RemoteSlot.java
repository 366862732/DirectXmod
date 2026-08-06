/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.inventory;

import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface RemoteSlot {
    public static final RemoteSlot PLACEHOLDER = new RemoteSlot(){

        @Override
        public void receive(HashedStack incoming) {
        }

        @Override
        public void force(ItemStack outgoing) {
        }

        @Override
        public boolean matches(ItemStack local) {
            return true;
        }
    };

    public void force(ItemStack var1);

    public void receive(HashedStack var1);

    public boolean matches(ItemStack var1);

    public static class Synchronized
    implements RemoteSlot {
        private final HashedPatchMap.HashGenerator hasher;
        private @Nullable ItemStack remoteStack = null;
        private @Nullable HashedStack remoteHash = null;

        public Synchronized(HashedPatchMap.HashGenerator hasher) {
            this.hasher = hasher;
        }

        @Override
        public void force(ItemStack outgoing) {
            this.remoteStack = outgoing.copy();
            this.remoteHash = null;
        }

        @Override
        public void receive(HashedStack incoming) {
            this.remoteStack = null;
            this.remoteHash = incoming;
        }

        @Override
        public boolean matches(ItemStack local) {
            if (this.remoteStack != null) {
                return ItemStack.matches(this.remoteStack, local);
            }
            if (this.remoteHash != null && this.remoteHash.matches(local, this.hasher)) {
                this.remoteStack = local.copy();
                return true;
            }
            return false;
        }

        public void copyFrom(Synchronized other) {
            this.remoteStack = other.remoteStack;
            this.remoteHash = other.remoteHash;
        }
    }
}

