/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record FishingRodCast() implements ConditionalItemModelProperty
{
    public static final MapCodec<FishingRodCast> MAP_CODEC = MapCodec.unit((Object)new FishingRodCast());

    @Override
    public boolean get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        if (owner instanceof Player) {
            Player player = (Player)owner;
            if (player.fishing != null) {
                HumanoidArm holdingArm = FishingHookRenderer.getHoldingArm(player);
                return owner.getItemHeldByArm(holdingArm) == itemStack;
            }
        }
        return false;
    }

    public MapCodec<FishingRodCast> type() {
        return MAP_CODEC;
    }
}

