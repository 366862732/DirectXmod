/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

public interface Portal {
    default public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 0;
    }

    public @Nullable TeleportTransition getPortalDestination(ServerLevel var1, Entity var2, BlockPos var3);

    default public Transition getLocalTransition() {
        return Transition.NONE;
    }

    public static enum Transition {
        CONFUSION,
        NONE;

    }
}

