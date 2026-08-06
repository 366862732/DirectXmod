/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

public class PortalProcessor {
    private final Portal portal;
    private BlockPos entryPosition;
    private int portalTime;
    private boolean insidePortalThisTick;

    public PortalProcessor(Portal portal, BlockPos portalEntryPosition) {
        this.portal = portal;
        this.entryPosition = portalEntryPosition;
        this.insidePortalThisTick = true;
    }

    public boolean processPortalTeleportation(ServerLevel serverLevel, Entity entity, boolean allowedToTeleport) {
        if (this.insidePortalThisTick) {
            this.insidePortalThisTick = false;
            return allowedToTeleport && this.portalTime++ >= this.portal.getPortalTransitionTime(serverLevel, entity);
        }
        this.decayTick();
        return false;
    }

    public @Nullable TeleportTransition getPortalDestination(ServerLevel serverLevel, Entity entity) {
        return this.portal.getPortalDestination(serverLevel, entity, this.entryPosition);
    }

    public Portal.Transition getPortalLocalTransition() {
        return this.portal.getLocalTransition();
    }

    private void decayTick() {
        this.portalTime = Math.max(this.portalTime - 4, 0);
    }

    public boolean hasExpired() {
        return this.portalTime <= 0;
    }

    public BlockPos getEntryPosition() {
        return this.entryPosition;
    }

    public void updateEntryPosition(BlockPos entryPosition) {
        this.entryPosition = entryPosition;
    }

    public int getPortalTime() {
        return this.portalTime;
    }

    public boolean isInsidePortalThisTick() {
        return this.insidePortalThisTick;
    }

    public void setAsInsidePortalThisTick(boolean insidePortal) {
        this.insidePortalThisTick = insidePortal;
    }

    public boolean isSamePortal(Portal portal) {
        return this.portal == portal;
    }
}

