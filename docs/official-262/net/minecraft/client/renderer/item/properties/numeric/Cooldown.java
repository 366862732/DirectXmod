/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record Cooldown() implements RangeSelectItemModelProperty
{
    public static final MapCodec<Cooldown> MAP_CODEC = MapCodec.unit((Object)new Cooldown());

    @Override
    public float get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        float f;
        LivingEntity livingEntity;
        if (owner != null && (livingEntity = owner.asLivingEntity()) instanceof Player) {
            Player player = (Player)livingEntity;
            f = player.getCooldowns().getCooldownPercent(itemStack, 0.0f);
        } else {
            f = 0.0f;
        }
        return f;
    }

    public MapCodec<Cooldown> type() {
        return MAP_CODEC;
    }
}

