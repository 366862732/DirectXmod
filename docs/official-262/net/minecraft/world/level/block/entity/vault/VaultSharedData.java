/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
 */
package net.minecraft.world.level.block.entity.vault;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;

public class VaultSharedData {
    public static final String TAG_NAME = "shared_data";
    public static final Codec<VaultSharedData> CODEC = RecordCodecBuilder.create(i -> i.group((App)ItemStack.lenientOptionalFieldOf("display_item").forGetter(vault -> vault.displayItem), (App)UUIDUtil.CODEC_LINKED_SET.lenientOptionalFieldOf("connected_players", Set.of()).forGetter(vault -> vault.connectedPlayers), (App)Codec.DOUBLE.lenientOptionalFieldOf("connected_particles_range", (Object)VaultConfig.DEFAULT.deactivationRange()).forGetter(vault -> vault.connectedParticlesRange)).apply((Applicative)i, VaultSharedData::new));
    private ItemStack displayItem = ItemStack.EMPTY;
    private Set<UUID> connectedPlayers = new ObjectLinkedOpenHashSet();
    private double connectedParticlesRange = VaultConfig.DEFAULT.deactivationRange();
    boolean isDirty;

    public VaultSharedData(ItemStack displayItem, Set<UUID> connectedPlayers, double connectedParticlesRange) {
        this.displayItem = displayItem;
        this.connectedPlayers.addAll(connectedPlayers);
        this.connectedParticlesRange = connectedParticlesRange;
    }

    public VaultSharedData() {
    }

    public ItemStack getDisplayItem() {
        return this.displayItem;
    }

    public boolean hasDisplayItem() {
        return !this.displayItem.isEmpty();
    }

    public void setDisplayItem(ItemStack stack) {
        if (ItemStack.matches(this.displayItem, stack)) {
            return;
        }
        this.displayItem = stack.copy();
        this.markDirty();
    }

    boolean hasConnectedPlayers() {
        return !this.connectedPlayers.isEmpty();
    }

    Set<UUID> getConnectedPlayers() {
        return this.connectedPlayers;
    }

    double connectedParticlesRange() {
        return this.connectedParticlesRange;
    }

    void updateConnectedPlayersWithinRange(ServerLevel serverLevel, BlockPos pos, VaultServerData serverData, VaultConfig config, double limit) {
        Set currentConnectedPlayers = config.playerDetector().detect(serverLevel, config.entitySelector(), pos, limit, false).stream().filter(uuid -> !serverData.getRewardedPlayers().contains(uuid)).collect(Collectors.toSet());
        if (!this.connectedPlayers.equals(currentConnectedPlayers)) {
            this.connectedPlayers = currentConnectedPlayers;
            this.markDirty();
        }
    }

    private void markDirty() {
        this.isDirty = true;
    }

    void set(VaultSharedData from) {
        this.displayItem = from.displayItem;
        this.connectedPlayers = from.connectedPlayers;
        this.connectedParticlesRange = from.connectedParticlesRange;
    }
}

