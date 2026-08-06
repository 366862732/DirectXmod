/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.phys.Vec3;

public enum VaultState implements StringRepresentable
{
    INACTIVE("inactive", LightLevel.HALF_LIT){

        @Override
        protected void onEnter(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData, boolean isOminous) {
            sharedData.setDisplayItem(ItemStack.EMPTY);
            serverLevel.levelEvent(3016, pos, isOminous ? 1 : 0);
        }
    }
    ,
    ACTIVE("active", LightLevel.LIT){

        @Override
        protected void onEnter(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData, boolean isOminous) {
            if (!sharedData.hasDisplayItem()) {
                VaultBlockEntity.Server.cycleDisplayItemFromLootTable(serverLevel, this, config, sharedData, pos);
            }
            serverLevel.levelEvent(3015, pos, isOminous ? 1 : 0);
        }
    }
    ,
    UNLOCKING("unlocking", LightLevel.LIT){

        @Override
        protected void onEnter(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData, boolean isOminous) {
            serverLevel.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS);
        }
    }
    ,
    EJECTING("ejecting", LightLevel.LIT){

        @Override
        protected void onEnter(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData, boolean isOminous) {
            serverLevel.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS);
        }

        @Override
        protected void onExit(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData) {
            serverLevel.playSound(null, pos, SoundEvents.VAULT_CLOSE_SHUTTER, SoundSource.BLOCKS);
        }
    };

    private static final int UPDATE_CONNECTED_PLAYERS_TICK_RATE = 20;
    private static final int DELAY_BETWEEN_EJECTIONS_TICKS = 20;
    private static final int DELAY_AFTER_LAST_EJECTION_TICKS = 20;
    private static final int DELAY_BEFORE_FIRST_EJECTION_TICKS = 20;
    private final String stateName;
    private final LightLevel lightLevel;

    private VaultState(String stateName, LightLevel lightLevel) {
        this.stateName = stateName;
        this.lightLevel = lightLevel;
    }

    @Override
    public String getSerializedName() {
        return this.stateName;
    }

    public int lightLevel() {
        return this.lightLevel.value;
    }

    public VaultState tickAndGetNext(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> VaultState.updateStateForConnectedPlayers(serverLevel, pos, config, serverData, sharedData, config.activationRange());
            case 1 -> VaultState.updateStateForConnectedPlayers(serverLevel, pos, config, serverData, sharedData, config.deactivationRange());
            case 2 -> {
                serverData.pauseStateUpdatingUntil(serverLevel.getGameTime() + 20L);
                yield EJECTING;
            }
            case 3 -> {
                if (serverData.getItemsToEject().isEmpty()) {
                    serverData.markEjectionFinished();
                    yield VaultState.updateStateForConnectedPlayers(serverLevel, pos, config, serverData, sharedData, config.deactivationRange());
                }
                float ejectionSoundProgress = serverData.ejectionProgress();
                VaultState.ejectResultItem(serverLevel, pos, serverData.popNextItemToEject(), ejectionSoundProgress);
                sharedData.setDisplayItem(serverData.getNextItemToEject());
                boolean isLastEjection = serverData.getItemsToEject().isEmpty();
                int ejectionDelay = isLastEjection ? 20 : 20;
                serverData.pauseStateUpdatingUntil(serverLevel.getGameTime() + (long)ejectionDelay);
                yield EJECTING;
            }
        };
    }

    private static VaultState updateStateForConnectedPlayers(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData, double activationRange) {
        sharedData.updateConnectedPlayersWithinRange(serverLevel, pos, serverData, config, activationRange);
        serverData.pauseStateUpdatingUntil(serverLevel.getGameTime() + 20L);
        return sharedData.hasConnectedPlayers() ? ACTIVE : INACTIVE;
    }

    public void onTransition(ServerLevel serverLevel, BlockPos pos, VaultState to, VaultConfig config, VaultSharedData sharedData, boolean isOminous) {
        this.onExit(serverLevel, pos, config, sharedData);
        to.onEnter(serverLevel, pos, config, sharedData, isOminous);
    }

    protected void onEnter(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData, boolean isOminous) {
    }

    protected void onExit(ServerLevel serverLevel, BlockPos pos, VaultConfig config, VaultSharedData sharedData) {
    }

    private static void ejectResultItem(ServerLevel serverLevel, BlockPos pos, ItemStack itemToEject, float ejectionSoundProgress) {
        DefaultDispenseItemBehavior.spawnItem(serverLevel, itemToEject, 2, Direction.UP, Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2));
        serverLevel.levelEvent(3017, pos, 0);
        serverLevel.playSound(null, pos, SoundEvents.VAULT_EJECT_ITEM, SoundSource.BLOCKS, 1.0f, 0.8f + 0.4f * ejectionSoundProgress);
    }

    private static enum LightLevel {
        HALF_LIT(6),
        LIT(12);

        private final int value;

        private LightLevel(int value) {
            this.value = value;
        }
    }
}

