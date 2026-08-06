/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.objects.ReferenceArraySet
 *  it.unimi.dsi.fastutil.objects.ReferenceSet
 */
package net.minecraft.server.permissions;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;

public class PermissionSetUnion
implements PermissionSet {
    private final ReferenceSet<PermissionSet> permissions = new ReferenceArraySet();

    PermissionSetUnion(PermissionSet first, PermissionSet second) {
        this.permissions.add((Object)first);
        this.permissions.add((Object)second);
        this.ensureNoUnionsWithinUnions();
    }

    private PermissionSetUnion(ReferenceSet<PermissionSet> oldPermissions, PermissionSet other) {
        this.permissions.addAll(oldPermissions);
        this.permissions.add((Object)other);
        this.ensureNoUnionsWithinUnions();
    }

    private PermissionSetUnion(ReferenceSet<PermissionSet> oldPermissions, ReferenceSet<PermissionSet> other) {
        this.permissions.addAll(oldPermissions);
        this.permissions.addAll(other);
        this.ensureNoUnionsWithinUnions();
    }

    @Override
    public boolean hasPermission(Permission permission) {
        for (PermissionSet set : this.permissions) {
            if (!set.hasPermission(permission)) continue;
            return true;
        }
        return false;
    }

    @Override
    public PermissionSet union(PermissionSet other) {
        if (other instanceof PermissionSetUnion) {
            PermissionSetUnion otherUnion = (PermissionSetUnion)other;
            return new PermissionSetUnion(this.permissions, otherUnion.permissions);
        }
        return new PermissionSetUnion(this.permissions, other);
    }

    @VisibleForTesting
    public ReferenceSet<PermissionSet> getPermissions() {
        return new ReferenceArraySet(this.permissions);
    }

    private void ensureNoUnionsWithinUnions() {
        for (PermissionSet set : this.permissions) {
            if (!(set instanceof PermissionSetUnion)) continue;
            throw new IllegalArgumentException("Cannot have PermissionSetUnion within another PermissionSetUnion");
        }
    }
}

