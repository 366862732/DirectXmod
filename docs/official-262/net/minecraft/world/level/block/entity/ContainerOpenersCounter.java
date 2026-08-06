/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
    private static final int CHECK_TICK_DELAY = 5;
    private int openCount;
    private double maxInteractionRange;

    protected abstract void onOpen(Level var1, BlockPos var2, BlockState var3);

    protected abstract void onClose(Level var1, BlockPos var2, BlockState var3);

    protected abstract void openerCountChanged(Level var1, BlockPos var2, BlockState var3, int var4, int var5);

    public abstract boolean isOwnContainer(Player var1);

    public void incrementOpeners(LivingEntity entity, Level level, BlockPos pos, BlockState blockState, double maxInteractionRange) {
        int previous;
        if ((previous = this.openCount++) == 0) {
            this.onOpen(level, pos, blockState);
            level.gameEvent((Entity)entity, GameEvent.CONTAINER_OPEN, pos);
            ContainerOpenersCounter.scheduleRecheck(level, pos, blockState);
        }
        this.openerCountChanged(level, pos, blockState, previous, this.openCount);
        this.maxInteractionRange = Math.max(maxInteractionRange, this.maxInteractionRange);
    }

    public void decrementOpeners(LivingEntity entity, Level level, BlockPos pos, BlockState blockState) {
        int previous = this.openCount--;
        if (this.openCount == 0) {
            this.onClose(level, pos, blockState);
            level.gameEvent((Entity)entity, GameEvent.CONTAINER_CLOSE, pos);
            this.maxInteractionRange = 0.0;
        }
        this.openerCountChanged(level, pos, blockState, previous, this.openCount);
    }

    public List<ContainerUser> getEntitiesWithContainerOpen(Level level, BlockPos pos) {
        double range = this.maxInteractionRange + 4.0;
        AABB searchBox = new AABB(pos).inflate(range);
        return level.getEntities((Entity)null, searchBox, entity -> this.hasContainerOpen((Entity)entity, pos)).stream().map(entity -> (ContainerUser)((Object)entity)).collect(Collectors.toList());
    }

    private boolean hasContainerOpen(Entity entity, BlockPos blockPos) {
        ContainerUser containerUser;
        if (entity instanceof ContainerUser && !(containerUser = (ContainerUser)((Object)entity)).getLivingEntity().isSpectator()) {
            return containerUser.hasContainerOpen(this, blockPos);
        }
        return false;
    }

    public void recheckOpeners(Level level, BlockPos pos, BlockState blockState) {
        List<ContainerUser> containerUsers = this.getEntitiesWithContainerOpen(level, pos);
        this.maxInteractionRange = 0.0;
        for (ContainerUser containerUser : containerUsers) {
            this.maxInteractionRange = Math.max(containerUser.getContainerInteractionRange(), this.maxInteractionRange);
        }
        int prevCount = this.openCount;
        int openCount = containerUsers.size();
        if (prevCount != openCount) {
            boolean wasOpen;
            boolean isOpen = openCount != 0;
            boolean bl = wasOpen = prevCount != 0;
            if (isOpen && !wasOpen) {
                this.onOpen(level, pos, blockState);
                level.gameEvent(null, GameEvent.CONTAINER_OPEN, pos);
            } else if (!isOpen) {
                this.onClose(level, pos, blockState);
                level.gameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
            }
            this.openCount = openCount;
        }
        this.openerCountChanged(level, pos, blockState, prevCount, openCount);
        if (openCount > 0) {
            ContainerOpenersCounter.scheduleRecheck(level, pos, blockState);
        }
    }

    public int getOpenerCount() {
        return this.openCount;
    }

    private static void scheduleRecheck(Level level, BlockPos blockPos, BlockState blockState) {
        level.scheduleTick(blockPos, blockState.getBlock(), 5);
    }
}

