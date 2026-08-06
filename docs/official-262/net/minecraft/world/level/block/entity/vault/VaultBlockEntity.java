/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity.vault;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class VaultBlockEntity
extends BlockEntity {
    private final VaultServerData serverData = new VaultServerData();
    private final VaultSharedData sharedData = new VaultSharedData();
    private final VaultClientData clientData = new VaultClientData();
    private VaultConfig config = VaultConfig.DEFAULT;

    public VaultBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.VAULT, worldPosition, blockState);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return Util.make(new CompoundTag(), tag -> tag.store("shared_data", VaultSharedData.CODEC, registries.createSerializationContext(NbtOps.INSTANCE), this.sharedData));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("config", VaultConfig.CODEC, this.config);
        output.store("shared_data", VaultSharedData.CODEC, this.sharedData);
        output.store("server_data", VaultServerData.CODEC, this.serverData);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("server_data", VaultServerData.CODEC).ifPresent(this.serverData::set);
        this.config = input.read("config", VaultConfig.CODEC).orElse(VaultConfig.DEFAULT);
        input.read("shared_data", VaultSharedData.CODEC).ifPresent(this.sharedData::set);
    }

    public @Nullable VaultServerData getServerData() {
        return this.level == null || this.level.isClientSide() ? null : this.serverData;
    }

    public VaultSharedData getSharedData() {
        return this.sharedData;
    }

    public VaultClientData getClientData() {
        return this.clientData;
    }

    public VaultConfig getConfig() {
        return this.config;
    }

    @VisibleForTesting
    public void setConfig(VaultConfig config) {
        this.config = config;
    }

    public static final class Client {
        private static final int PARTICLE_TICK_RATE = 20;
        private static final float IDLE_PARTICLE_CHANCE = 0.5f;
        private static final float AMBIENT_SOUND_CHANCE = 0.02f;
        private static final int ACTIVATION_PARTICLE_COUNT = 20;
        private static final int DEACTIVATION_PARTICLE_COUNT = 20;

        public static void tick(Level clientLevel, BlockPos pos, BlockState blockState, VaultClientData clientData, VaultSharedData sharedData) {
            clientData.updateDisplayItemSpin();
            if (clientLevel.getGameTime() % 20L == 0L) {
                Client.emitConnectionParticlesForNearbyPlayers(clientLevel, pos, blockState, sharedData);
            }
            Client.emitIdleParticles(clientLevel, pos, sharedData, blockState.getValue(VaultBlock.OMINOUS) != false ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME);
            Client.playIdleSounds(clientLevel, pos, sharedData);
        }

        public static void emitActivationParticles(Level clientLevel, BlockPos pos, BlockState blockState, VaultSharedData sharedData, ParticleOptions flameParticle) {
            Client.emitConnectionParticlesForNearbyPlayers(clientLevel, pos, blockState, sharedData);
            RandomSource random = clientLevel.getRandom();
            for (int i = 0; i < 20; ++i) {
                Vec3 particlePos = Client.randomPosInsideCage(pos, random);
                clientLevel.addParticle(ParticleTypes.SMOKE, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
                clientLevel.addParticle(flameParticle, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
            }
        }

        public static void emitDeactivationParticles(Level clientLevel, BlockPos pos, ParticleOptions flameParticle) {
            RandomSource random = clientLevel.getRandom();
            for (int i = 0; i < 20; ++i) {
                Vec3 particlePos = Client.randomPosCenterOfCage(pos, random);
                Vec3 dir = new Vec3(random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
                clientLevel.addParticle(flameParticle, particlePos.x(), particlePos.y(), particlePos.z(), dir.x(), dir.y(), dir.z());
            }
        }

        private static void emitIdleParticles(Level clientLevel, BlockPos pos, VaultSharedData sharedData, ParticleOptions flameParticle) {
            RandomSource random = clientLevel.getRandom();
            if (random.nextFloat() <= 0.5f) {
                Vec3 particlePos = Client.randomPosInsideCage(pos, random);
                clientLevel.addParticle(ParticleTypes.SMOKE, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
                if (Client.shouldDisplayActiveEffects(sharedData)) {
                    clientLevel.addParticle(flameParticle, particlePos.x(), particlePos.y(), particlePos.z(), 0.0, 0.0, 0.0);
                }
            }
        }

        private static void emitConnectionParticlesForPlayer(Level level, Vec3 flyTowards, Player player) {
            RandomSource random = level.getRandom();
            Vec3 direction = flyTowards.vectorTo(player.position().add(0.0, player.getBbHeight() / 2.0f, 0.0));
            int particleCount = Mth.nextInt(random, 2, 5);
            for (int i = 0; i < particleCount; ++i) {
                Vec3 randomDirection = direction.offsetRandom(random, 1.0f);
                level.addParticle(ParticleTypes.VAULT_CONNECTION, flyTowards.x(), flyTowards.y(), flyTowards.z(), randomDirection.x(), randomDirection.y(), randomDirection.z());
            }
        }

        private static void emitConnectionParticlesForNearbyPlayers(Level level, BlockPos pos, BlockState blockState, VaultSharedData sharedData) {
            Set<UUID> connectedPlayers = sharedData.getConnectedPlayers();
            if (connectedPlayers.isEmpty()) {
                return;
            }
            Vec3 keyholePos = Client.keyholePos(pos, blockState.getValue(VaultBlock.FACING));
            for (UUID uuid : connectedPlayers) {
                Player player = level.getPlayerByUUID(uuid);
                if (player == null || !Client.isWithinConnectionRange(pos, sharedData, player)) continue;
                Client.emitConnectionParticlesForPlayer(level, keyholePos, player);
            }
        }

        private static boolean isWithinConnectionRange(BlockPos vaultPos, VaultSharedData sharedData, Player player) {
            return player.blockPosition().distSqr(vaultPos) <= Mth.square(sharedData.connectedParticlesRange());
        }

        private static void playIdleSounds(Level clientLevel, BlockPos pos, VaultSharedData sharedData) {
            if (!Client.shouldDisplayActiveEffects(sharedData)) {
                return;
            }
            RandomSource random = clientLevel.getRandom();
            if (random.nextFloat() <= 0.02f) {
                clientLevel.playLocalSound(pos, SoundEvents.VAULT_AMBIENT, SoundSource.BLOCKS, random.nextFloat() * 0.25f + 0.75f, random.nextFloat() + 0.5f, false);
            }
        }

        public static boolean shouldDisplayActiveEffects(VaultSharedData sharedData) {
            return sharedData.hasDisplayItem();
        }

        private static Vec3 randomPosCenterOfCage(BlockPos blockPos, RandomSource random) {
            return Vec3.atLowerCornerOf(blockPos).add(Mth.nextDouble(random, 0.4, 0.6), Mth.nextDouble(random, 0.4, 0.6), Mth.nextDouble(random, 0.4, 0.6));
        }

        private static Vec3 randomPosInsideCage(BlockPos blockPos, RandomSource random) {
            return Vec3.atLowerCornerOf(blockPos).add(Mth.nextDouble(random, 0.1, 0.9), Mth.nextDouble(random, 0.25, 0.75), Mth.nextDouble(random, 0.1, 0.9));
        }

        private static Vec3 keyholePos(BlockPos blockPos, Direction blockFacing) {
            return Vec3.atBottomCenterOf(blockPos).add((double)blockFacing.getStepX() * 0.5, 1.75, (double)blockFacing.getStepZ() * 0.5);
        }
    }

    public static final class Server {
        private static final int UNLOCKING_DELAY_TICKS = 14;
        private static final int DISPLAY_CYCLE_TICK_RATE = 20;
        private static final int INSERT_FAIL_SOUND_BUFFER_TICKS = 15;

        public static void tick(ServerLevel serverLevel, BlockPos pos, BlockState blockState, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData) {
            VaultState currentState = blockState.getValue(VaultBlock.STATE);
            if (Server.shouldCycleDisplayItem(serverLevel.getGameTime(), currentState)) {
                Server.cycleDisplayItemFromLootTable(serverLevel, currentState, config, sharedData, pos);
            }
            BlockState nextBlockState = blockState;
            if (serverLevel.getGameTime() >= serverData.stateUpdatingResumesAt() && blockState != (nextBlockState = (BlockState)nextBlockState.setValue(VaultBlock.STATE, currentState.tickAndGetNext(serverLevel, pos, config, serverData, sharedData)))) {
                Server.setVaultState(serverLevel, pos, blockState, nextBlockState, config, sharedData);
            }
            if (serverData.isDirty || sharedData.isDirty) {
                VaultBlockEntity.setChanged(serverLevel, pos, blockState);
                if (sharedData.isDirty) {
                    serverLevel.sendBlockUpdated(pos, blockState, nextBlockState, 2);
                }
                serverData.isDirty = false;
                sharedData.isDirty = false;
            }
        }

        public static void tryInsertKey(ServerLevel serverLevel, BlockPos pos, BlockState blockState, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData, Player player, ItemStack stackToInsert) {
            VaultState vaultState = blockState.getValue(VaultBlock.STATE);
            if (!Server.canEjectReward(config, vaultState)) {
                return;
            }
            if (!Server.isValidToInsert(config, stackToInsert)) {
                Server.playInsertFailSound(serverLevel, serverData, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL);
                return;
            }
            if (serverData.hasRewardedPlayer(player)) {
                Server.playInsertFailSound(serverLevel, serverData, pos, SoundEvents.VAULT_REJECT_REWARDED_PLAYER);
                return;
            }
            List<ItemStack> itemsToEject = Server.resolveItemsToEject(serverLevel, config, pos, player, stackToInsert);
            if (itemsToEject.isEmpty()) {
                return;
            }
            player.awardStat(Stats.ITEM_USED.get(stackToInsert.getItem()));
            stackToInsert.consume(config.keyItem().getCount(), player);
            Server.unlock(serverLevel, blockState, pos, config, serverData, sharedData, itemsToEject);
            serverData.addToRewardedPlayers(player);
            sharedData.updateConnectedPlayersWithinRange(serverLevel, pos, serverData, config, config.deactivationRange());
        }

        private static void setVaultState(ServerLevel serverLevel, BlockPos pos, BlockState currentBlockState, BlockState newBlockState, VaultConfig config, VaultSharedData sharedData) {
            VaultState currentVaultState = currentBlockState.getValue(VaultBlock.STATE);
            VaultState newVaultState = newBlockState.getValue(VaultBlock.STATE);
            serverLevel.setBlock(pos, newBlockState, 3);
            currentVaultState.onTransition(serverLevel, pos, newVaultState, config, sharedData, newBlockState.getValue(VaultBlock.OMINOUS));
        }

        static void cycleDisplayItemFromLootTable(ServerLevel serverLevel, VaultState vaultState, VaultConfig config, VaultSharedData sharedData, BlockPos pos) {
            if (!Server.canEjectReward(config, vaultState)) {
                sharedData.setDisplayItem(ItemStack.EMPTY);
                return;
            }
            ItemStack displayItem = Server.getRandomDisplayItemFromLootTable(serverLevel, pos, config.overrideLootTableToDisplay().orElse(config.lootTable()));
            sharedData.setDisplayItem(displayItem);
        }

        private static ItemStack getRandomDisplayItemFromLootTable(ServerLevel serverLevel, BlockPos pos, ResourceKey<LootTable> lootTableId) {
            LootParams params;
            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(lootTableId);
            ObjectArrayList<ItemStack> results = lootTable.getRandomItems(params = new LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).create(LootContextParamSets.VAULT), serverLevel.getRandom());
            if (results.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return Util.getRandom(results, serverLevel.getRandom());
        }

        private static void unlock(ServerLevel serverLevel, BlockState blockState, BlockPos pos, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData, List<ItemStack> itemsToEject) {
            serverData.setItemsToEject(itemsToEject);
            sharedData.setDisplayItem(serverData.getNextItemToEject());
            serverData.pauseStateUpdatingUntil(serverLevel.getGameTime() + 14L);
            Server.setVaultState(serverLevel, pos, blockState, (BlockState)blockState.setValue(VaultBlock.STATE, VaultState.UNLOCKING), config, sharedData);
        }

        private static List<ItemStack> resolveItemsToEject(ServerLevel serverLevel, VaultConfig config, BlockPos pos, Player player, ItemInstance insertedStack) {
            LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(config.lootTable());
            LootParams params = new LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.TOOL, insertedStack).create(LootContextParamSets.VAULT);
            return lootTable.getRandomItems(params);
        }

        private static boolean canEjectReward(VaultConfig config, VaultState vaultState) {
            return !config.keyItem().isEmpty() && vaultState != VaultState.INACTIVE;
        }

        private static boolean isValidToInsert(VaultConfig config, ItemStack stackToInsert) {
            return ItemStack.isSameItemSameComponents(stackToInsert, config.keyItem()) && stackToInsert.getCount() >= config.keyItem().getCount();
        }

        private static boolean shouldCycleDisplayItem(long gameTime, VaultState vaultState) {
            return gameTime % 20L == 0L && vaultState == VaultState.ACTIVE;
        }

        private static void playInsertFailSound(ServerLevel serverLevel, VaultServerData serverData, BlockPos pos, SoundEvent sound) {
            if (serverLevel.getGameTime() >= serverData.getLastInsertFailTimestamp() + 15L) {
                serverLevel.playSound(null, pos, sound, SoundSource.BLOCKS);
                serverData.setLastInsertFailTimestamp(serverLevel.getGameTime());
            }
        }
    }
}

