/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.permissions;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

public interface LevelBasedPermissionSet
extends PermissionSet {
    @Deprecated
    public static final LevelBasedPermissionSet ALL = LevelBasedPermissionSet.create(PermissionLevel.ALL);
    public static final LevelBasedPermissionSet MODERATOR = LevelBasedPermissionSet.create(PermissionLevel.MODERATORS);
    public static final LevelBasedPermissionSet GAMEMASTER = LevelBasedPermissionSet.create(PermissionLevel.GAMEMASTERS);
    public static final LevelBasedPermissionSet ADMIN = LevelBasedPermissionSet.create(PermissionLevel.ADMINS);
    public static final LevelBasedPermissionSet OWNER = LevelBasedPermissionSet.create(PermissionLevel.OWNERS);

    public PermissionLevel level();

    @Override
    default public boolean hasPermission(Permission permission) {
        if (permission instanceof Permission.HasCommandLevel) {
            Permission.HasCommandLevel levelCheck = (Permission.HasCommandLevel)permission;
            return this.level().isEqualOrHigherThan(levelCheck.level());
        }
        if (permission.equals(Permissions.COMMANDS_ENTITY_SELECTORS)) {
            return this.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
        }
        return false;
    }

    @Override
    default public PermissionSet union(PermissionSet other) {
        if (other instanceof LevelBasedPermissionSet) {
            LevelBasedPermissionSet otherSet = (LevelBasedPermissionSet)other;
            if (this.level().isEqualOrHigherThan(otherSet.level())) {
                return otherSet;
            }
            return this;
        }
        return PermissionSet.super.union(other);
    }

    public static LevelBasedPermissionSet forLevel(PermissionLevel level) {
        return switch (level) {
            default -> throw new MatchException(null, null);
            case PermissionLevel.ALL -> ALL;
            case PermissionLevel.MODERATORS -> MODERATOR;
            case PermissionLevel.GAMEMASTERS -> GAMEMASTER;
            case PermissionLevel.ADMINS -> ADMIN;
            case PermissionLevel.OWNERS -> OWNER;
        };
    }

    private static LevelBasedPermissionSet create(final PermissionLevel level) {
        return new LevelBasedPermissionSet(){

            @Override
            public PermissionLevel level() {
                return level;
            }

            public String toString() {
                return "permission level: " + level.name();
            }
        };
    }
}

