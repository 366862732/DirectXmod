/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.vehicle.minecart;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartSpawner
extends AbstractMinecart {
    private final BaseSpawner spawner = new BaseSpawner(this){
        final /* synthetic */ MinecartSpawner this$0;
        {
            MinecartSpawner minecartSpawner = this$0;
            Objects.requireNonNull(minecartSpawner);
            this.this$0 = minecartSpawner;
        }

        @Override
        public void broadcastEvent(Level level, BlockPos pos, int id) {
            level.broadcastEntityEvent(this.this$0, (byte)id);
        }
    };
    private final Runnable ticker;

    public MinecartSpawner(EntityType<? extends MinecartSpawner> type, Level level) {
        super(type, level);
        this.ticker = this.createTicker(level);
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.MINECART);
    }

    private Runnable createTicker(Level level) {
        Runnable runnable;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            runnable = () -> this.spawner.serverTick(serverLevel, this.blockPosition());
        } else {
            runnable = () -> this.spawner.clientTick(level, this.blockPosition());
        }
        return runnable;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.SPAWNER.defaultBlockState();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.spawner.load(this.level(), this.blockPosition(), input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.spawner.save(output);
    }

    @Override
    public void handleEntityEvent(byte id) {
        this.spawner.onEventTriggered(this.level(), id);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticker.run();
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }
}

