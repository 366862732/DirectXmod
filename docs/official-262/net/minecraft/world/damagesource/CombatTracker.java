/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.damagesource;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.CommonLinks;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraft.world.damagesource.FallLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class CombatTracker {
    public static final int RESET_DAMAGE_STATUS_TIME = 100;
    public static final int RESET_COMBAT_STATUS_TIME = 300;
    private static final Style INTENTIONAL_GAME_DESIGN_STYLE = Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(CommonLinks.INTENTIONAL_GAME_DESIGN_BUG)).withHoverEvent(new HoverEvent.ShowText(Component.literal("MCPE-28723")));
    private final List<CombatEntry> entries = Lists.newArrayList();
    private final LivingEntity mob;
    private int lastDamageTime;
    private int combatStartTime;
    private int combatEndTime;
    private boolean inCombat;
    private boolean takingDamage;

    public CombatTracker(LivingEntity mob) {
        this.mob = mob;
    }

    public void recordDamage(DamageSource source, float damage) {
        this.recheckStatus();
        FallLocation fallLocation = FallLocation.getCurrentFallLocation(this.mob);
        CombatEntry entry = new CombatEntry(source, damage, fallLocation, (float)this.mob.fallDistance);
        this.entries.add(entry);
        this.lastDamageTime = this.mob.tickCount;
        this.takingDamage = true;
        if (!this.inCombat && this.mob.isAlive() && CombatTracker.shouldEnterCombat(source)) {
            this.inCombat = true;
            this.combatEndTime = this.combatStartTime = this.mob.tickCount;
            this.mob.onEnterCombat();
        }
    }

    private static boolean shouldEnterCombat(DamageSource source) {
        return source.getEntity() instanceof LivingEntity;
    }

    private Component getMessageForAssistedFall(Entity attackerEntity, Component attackerName, String messageWithItem, String messageWithoutItem) {
        ItemStack attackerItem;
        if (attackerEntity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)attackerEntity;
            v0 = livingEntity.getMainHandItem();
        } else {
            v0 = attackerItem = ItemStack.EMPTY;
        }
        if (!attackerItem.isEmpty() && attackerItem.has(DataComponents.CUSTOM_NAME)) {
            return Component.translatable(messageWithItem, this.mob.getDisplayName(), attackerName, attackerItem.getDisplayName());
        }
        return Component.translatable(messageWithoutItem, this.mob.getDisplayName(), attackerName);
    }

    private Component getFallMessage(CombatEntry knockOffEntry, @Nullable Entity killingEntity) {
        DamageSource knockOffSource = knockOffEntry.source();
        if (knockOffSource.is(DamageTypeTags.IS_FALL) || knockOffSource.is(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL)) {
            FallLocation fallLocation = Objects.requireNonNullElse(knockOffEntry.fallLocation(), FallLocation.GENERIC);
            return Component.translatable(fallLocation.languageKey(), this.mob.getDisplayName());
        }
        Component killerName = CombatTracker.getDisplayName(killingEntity);
        Entity attackerEntity = knockOffSource.getEntity();
        Component attackerName = CombatTracker.getDisplayName(attackerEntity);
        if (attackerName != null && !attackerName.equals(killerName)) {
            return this.getMessageForAssistedFall(attackerEntity, attackerName, "death.fell.assist.item", "death.fell.assist");
        }
        if (killerName != null) {
            return this.getMessageForAssistedFall(killingEntity, killerName, "death.fell.finish.item", "death.fell.finish");
        }
        return Component.translatable("death.fell.killer", this.mob.getDisplayName());
    }

    private static @Nullable Component getDisplayName(@Nullable Entity entity) {
        return entity == null ? null : entity.getDisplayName();
    }

    public Component getDeathMessage() {
        if (this.entries.isEmpty()) {
            return Component.translatable("death.attack.generic", this.mob.getDisplayName());
        }
        CombatEntry killingBlow = this.entries.get(this.entries.size() - 1);
        DamageSource killingSource = killingBlow.source();
        CombatEntry knockOffEntry = this.getMostSignificantFall();
        DeathMessageType messageType = killingSource.type().deathMessageType();
        if (messageType == DeathMessageType.FALL_VARIANTS && knockOffEntry != null) {
            return this.getFallMessage(knockOffEntry, killingSource.getEntity());
        }
        if (messageType == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
            String deathMsg = "death.attack." + killingSource.getMsgId();
            MutableComponent link = ComponentUtils.wrapInSquareBrackets(Component.translatable(deathMsg + ".link")).withStyle(INTENTIONAL_GAME_DESIGN_STYLE);
            return Component.translatable(deathMsg + ".message", this.mob.getDisplayName(), link);
        }
        return killingSource.getLocalizedDeathMessage(this.mob);
    }

    private @Nullable CombatEntry getMostSignificantFall() {
        CombatEntry result = null;
        CombatEntry alternative = null;
        float altDamage = 0.0f;
        float bestFall = 0.0f;
        for (int i = 0; i < this.entries.size(); ++i) {
            float fallDistance;
            CombatEntry entry = this.entries.get(i);
            CombatEntry previous = i > 0 ? this.entries.get(i - 1) : null;
            DamageSource source = entry.source();
            boolean isFakeFall = source.is(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL);
            float f = fallDistance = isFakeFall ? Float.MAX_VALUE : entry.fallDistance();
            if ((source.is(DamageTypeTags.IS_FALL) || isFakeFall) && fallDistance > 0.0f && (result == null || fallDistance > bestFall)) {
                result = i > 0 ? previous : entry;
                bestFall = fallDistance;
            }
            if (entry.fallLocation() == null || alternative != null && !(entry.damage() > altDamage)) continue;
            alternative = entry;
            altDamage = entry.damage();
        }
        if (bestFall > 5.0f && result != null) {
            return result;
        }
        if (altDamage > 5.0f && alternative != null) {
            return alternative;
        }
        return null;
    }

    public int getCombatDuration() {
        if (this.inCombat) {
            return this.mob.tickCount - this.combatStartTime;
        }
        return this.combatEndTime - this.combatStartTime;
    }

    public void recheckStatus() {
        int reset;
        int n = reset = this.inCombat ? 300 : 100;
        if (this.takingDamage && (!this.mob.isAlive() || this.mob.tickCount - this.lastDamageTime > reset)) {
            boolean wasInCombat = this.inCombat;
            this.takingDamage = false;
            this.inCombat = false;
            this.combatEndTime = this.mob.tickCount;
            if (wasInCombat) {
                this.mob.onLeaveCombat();
            }
            this.entries.clear();
        }
    }
}

