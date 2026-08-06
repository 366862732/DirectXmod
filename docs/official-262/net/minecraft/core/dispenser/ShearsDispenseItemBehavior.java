/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core.dispenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class ShearsDispenseItemBehavior
extends OptionalDispenseItemBehavior {
    @Override
    protected ItemStack execute(BlockSource source, ItemStack dispensed) {
        ServerLevel level = source.level();
        if (!level.isClientSide()) {
            BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
            this.setSuccess(ShearsDispenseItemBehavior.tryShearBeehive(level, dispensed, pos) || ShearsDispenseItemBehavior.tryShearEntity(level, pos, dispensed));
            if (this.isSuccess()) {
                dispensed.hurtAndBreak(1, level, null, item -> {});
            }
        }
        return dispensed;
    }

    private static boolean tryShearBeehive(ServerLevel level, ItemStack tool, BlockPos pos) {
        int honeyLevel;
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEEHIVES, s -> s.hasProperty(BeehiveBlock.HONEY_LEVEL) && s.getBlock() instanceof BeehiveBlock) && (honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL).intValue()) >= 5) {
            level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0f, 1.0f);
            BeehiveBlock.dropHoneycomb(level, tool, state, level.getBlockEntity(pos), null, pos);
            ((BeehiveBlock)state.getBlock()).releaseBeesAndResetHoneyLevel(level, state, pos, null, BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED);
            level.gameEvent(null, GameEvent.SHEAR, pos);
            return true;
        }
        return false;
    }

    private static boolean tryShearEntity(ServerLevel level, BlockPos pos, ItemStack tool) {
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(pos), EntitySelector.NO_SPECTATORS);
        for (Entity entity : entities) {
            Shearable shearable;
            if (entity.shearOffAllLeashConnections(null)) {
                return true;
            }
            if (!entity.isAlive() || !(entity instanceof Shearable) || !(shearable = (Shearable)((Object)entity)).readyForShearing()) continue;
            shearable.shear(level, SoundSource.BLOCKS, tool);
            level.gameEvent(null, GameEvent.SHEAR, pos);
            return true;
        }
        return false;
    }
}

