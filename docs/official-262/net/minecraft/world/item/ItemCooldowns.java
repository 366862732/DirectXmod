/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 */
package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;

public class ItemCooldowns {
    private final Map<Identifier, CooldownInstance> cooldowns = Maps.newHashMap();
    private int tickCount;

    public boolean isOnCooldown(ItemStack item) {
        return this.getCooldownPercent(item, 0.0f) > 0.0f;
    }

    public float getCooldownPercent(ItemStack item, float a) {
        Identifier group = this.getCooldownGroup(item);
        CooldownInstance cooldown = this.cooldowns.get(group);
        if (cooldown != null) {
            float duration = cooldown.endTime - cooldown.startTime;
            float remaining = (float)cooldown.endTime - ((float)this.tickCount + a);
            return Mth.clamp(remaining / duration, 0.0f, 1.0f);
        }
        return 0.0f;
    }

    public void tick() {
        ++this.tickCount;
        if (!this.cooldowns.isEmpty()) {
            Iterator<Map.Entry<Identifier, CooldownInstance>> iterator = this.cooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Identifier, CooldownInstance> entry = iterator.next();
                if (entry.getValue().endTime > this.tickCount) continue;
                iterator.remove();
                this.onCooldownEnded(entry.getKey());
            }
        }
    }

    public Identifier getCooldownGroup(ItemStack item) {
        UseCooldown useCooldown = item.get(DataComponents.USE_COOLDOWN);
        Identifier defaultItemGroup = BuiltInRegistries.ITEM.getKey(item.getItem());
        if (useCooldown == null) {
            return defaultItemGroup;
        }
        return useCooldown.cooldownGroup().orElse(defaultItemGroup);
    }

    public void addCooldown(ItemStack item, int time) {
        this.addCooldown(this.getCooldownGroup(item), time);
    }

    public void addCooldown(Identifier cooldownGroup, int time) {
        this.cooldowns.put(cooldownGroup, new CooldownInstance(this.tickCount, this.tickCount + time));
        this.onCooldownStarted(cooldownGroup, time);
    }

    public void removeCooldown(Identifier cooldownGroup) {
        this.cooldowns.remove(cooldownGroup);
        this.onCooldownEnded(cooldownGroup);
    }

    protected void onCooldownStarted(Identifier cooldownGroup, int duration) {
    }

    protected void onCooldownEnded(Identifier cooldownGroup) {
    }

    private record CooldownInstance(int startTime, int endTime) {
    }
}

