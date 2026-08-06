/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.damagesource;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DamageSource {
    private final Holder<DamageType> type;
    private final @Nullable Entity causingEntity;
    private final @Nullable Entity directEntity;
    private final @Nullable Vec3 damageSourcePosition;

    public String toString() {
        return "DamageSource (" + this.type().msgId() + ")";
    }

    public float getFoodExhaustion() {
        return this.type().exhaustion();
    }

    public boolean isDirect() {
        return this.causingEntity == this.directEntity;
    }

    private DamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Vec3 damageSourcePosition) {
        this.type = type;
        this.causingEntity = causingEntity;
        this.directEntity = directEntity;
        this.damageSourcePosition = damageSourcePosition;
    }

    public DamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        this(type, directEntity, causingEntity, null);
    }

    public DamageSource(Holder<DamageType> type, Vec3 damageSourcePosition) {
        this(type, null, null, damageSourcePosition);
    }

    public DamageSource(Holder<DamageType> type, @Nullable Entity causingEntity) {
        this(type, causingEntity, causingEntity);
    }

    public DamageSource(Holder<DamageType> type) {
        this(type, null, null, null);
    }

    public @Nullable Entity getDirectEntity() {
        return this.directEntity;
    }

    public @Nullable Entity getEntity() {
        return this.causingEntity;
    }

    public @Nullable ItemStack getWeaponItem() {
        return this.directEntity != null ? this.directEntity.getWeaponItem() : null;
    }

    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String deathMsg = "death.attack." + this.type().msgId();
        if (this.causingEntity != null || this.directEntity != null) {
            ItemStack held;
            Component name = this.causingEntity == null ? this.directEntity.getDisplayName() : this.causingEntity.getDisplayName();
            Entity entity = this.causingEntity;
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                v0 = livingEntity.getMainHandItem();
            } else {
                v0 = held = ItemStack.EMPTY;
            }
            if (!held.isEmpty() && held.has(DataComponents.CUSTOM_NAME)) {
                return Component.translatable(deathMsg + ".item", victim.getDisplayName(), name, held.getDisplayName());
            }
            return Component.translatable(deathMsg, victim.getDisplayName(), name);
        }
        LivingEntity source = victim.getKillCredit();
        String playerMsg = deathMsg + ".player";
        if (source != null) {
            return Component.translatable(playerMsg, victim.getDisplayName(), source.getDisplayName());
        }
        return Component.translatable(deathMsg, victim.getDisplayName());
    }

    public String getMsgId() {
        return this.type().msgId();
    }

    public boolean scalesWithDifficulty() {
        return switch (this.type().scaling()) {
            default -> throw new MatchException(null, null);
            case DamageScaling.NEVER -> false;
            case DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER -> {
                if (this.causingEntity instanceof LivingEntity && !(this.causingEntity instanceof Player)) {
                    yield true;
                }
                yield false;
            }
            case DamageScaling.ALWAYS -> true;
        };
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean isCreativePlayer() {
        Entity entity = this.getEntity();
        if (!(entity instanceof Player)) return false;
        Player player = (Player)entity;
        if (!player.getAbilities().instabuild) return false;
        return true;
    }

    public @Nullable Vec3 getSourcePosition() {
        if (this.damageSourcePosition != null) {
            return this.damageSourcePosition;
        }
        if (this.directEntity != null) {
            return this.directEntity.position();
        }
        return null;
    }

    public @Nullable Vec3 sourcePositionRaw() {
        return this.damageSourcePosition;
    }

    public boolean is(TagKey<DamageType> tag) {
        return this.type.is(tag);
    }

    public boolean is(ResourceKey<DamageType> typeKey) {
        return this.type.is(typeKey);
    }

    public DamageType type() {
        return this.type.value();
    }

    public Holder<DamageType> typeHolder() {
        return this.type;
    }
}

