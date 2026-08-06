/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;

public enum ConversionType {
    SINGLE(true){

        @Override
        public void convert(Mob from, Mob to, ConversionParams params) {
            Entity vehicle;
            Entity rootPassenger = from.getFirstPassenger();
            to.copyPosition(from);
            to.setDeltaMovement(from.getDeltaMovement());
            if (rootPassenger != null) {
                rootPassenger.stopRiding();
                rootPassenger.boardingCooldown = 0;
                for (Entity entity : to.getPassengers()) {
                    entity.stopRiding();
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
                rootPassenger.startRiding(to);
            }
            if ((vehicle = from.getVehicle()) != null) {
                from.stopRiding();
                to.startRiding(vehicle, false, false);
            }
            if (params.keepEquipment()) {
                for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                    ItemStack itemStack = from.getItemBySlot(slot);
                    if (itemStack.isEmpty()) continue;
                    to.setItemSlot(slot, itemStack.copyAndClear());
                    to.setDropChance(slot, from.getDropChances().byEquipment(slot));
                }
            }
            to.fallDistance = from.fallDistance;
            to.setSharedFlag(7, from.isFallFlying());
            to.lastHurtByPlayerMemoryTime = from.lastHurtByPlayerMemoryTime;
            to.hurtTime = from.hurtTime;
            to.yBodyRot = from.yBodyRot;
            to.setOnGround(from.onGround());
            from.getSleepingPos().ifPresent(to::setSleepingPos);
            Entity entity = from.getLeashHolder();
            if (entity != null) {
                to.setLeashedTo(entity, true);
            }
            ConversionType.convertCommon(from, to, params);
        }
    }
    ,
    SPLIT_ON_DEATH(false){

        @Override
        public void convert(Mob from, Mob to, ConversionParams params) {
            Entity leashHolder;
            Entity rootPassenger = from.getFirstPassenger();
            if (rootPassenger != null) {
                rootPassenger.stopRiding();
            }
            if ((leashHolder = from.getLeashHolder()) != null) {
                from.dropLeash();
            }
            ConversionType.convertCommon(from, to, params);
        }
    };

    private static final Set<DataComponentType<?>> COMPONENTS_TO_COPY;
    private final boolean discardAfterConversion;

    private ConversionType(boolean discardAfterConversion) {
        this.discardAfterConversion = discardAfterConversion;
    }

    public boolean shouldDiscardAfterConversion() {
        return this.discardAfterConversion;
    }

    public abstract void convert(Mob var1, Mob var2, ConversionParams var3);

    private static void convertCommon(Mob from, Mob to, ConversionParams params) {
        Zombie fromZombie;
        to.setAbsorptionAmount(from.getAbsorptionAmount());
        for (MobEffectInstance effect : from.getActiveEffects()) {
            to.addEffect(new MobEffectInstance(effect));
        }
        if (from.isBaby()) {
            to.setBaby(true);
        }
        if (from instanceof AgeableMob) {
            AgeableMob oldAgeable = (AgeableMob)from;
            if (to instanceof AgeableMob) {
                AgeableMob convertedAgeable = (AgeableMob)to;
                convertedAgeable.setAge(oldAgeable.getAge());
                convertedAgeable.forcedAge = oldAgeable.forcedAge;
                convertedAgeable.forcedAgeTimer = oldAgeable.forcedAgeTimer;
            }
        }
        Brain<? extends LivingEntity> oldBrain = from.getBrain();
        Brain<? extends LivingEntity> convertedBrain = to.getBrain();
        if (oldBrain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED) && oldBrain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
            convertedBrain.setMemory(MemoryModuleType.ANGRY_AT, oldBrain.getMemory(MemoryModuleType.ANGRY_AT));
        }
        if (params.preserveCanPickUpLoot()) {
            to.setCanPickUpLoot(from.canPickUpLoot());
        }
        to.setLeftHanded(from.isLeftHanded());
        to.setNoAi(from.isNoAi());
        if (from.isPersistenceRequired()) {
            to.setPersistenceRequired();
        }
        to.setCustomNameVisible(from.isCustomNameVisible());
        to.setSharedFlagOnFire(from.isOnFire());
        to.setInvulnerable(from.isInvulnerable());
        to.setNoGravity(from.isNoGravity());
        to.setPortalCooldown(from.getPortalCooldown());
        to.setSilent(from.isSilent());
        from.entityTags().forEach(to::addTag);
        for (DataComponentType<?> component : COMPONENTS_TO_COPY) {
            ConversionType.copyComponent(from, to, component);
        }
        if (params.team() != null) {
            Scoreboard scoreboard = to.level().getScoreboard();
            scoreboard.addPlayerToTeam(to.getStringUUID(), params.team());
            if (from.getTeam() != null && from.getTeam() == params.team()) {
                scoreboard.removePlayerFromTeam(from.getStringUUID(), from.getTeam());
            }
        }
        if (from instanceof Zombie && (fromZombie = (Zombie)from).canBreakDoors() && to instanceof Zombie) {
            Zombie toZombie = (Zombie)to;
            toZombie.setCanBreakDoors(true);
        }
    }

    private static <T> void copyComponent(Mob from, Mob to, DataComponentType<T> componentType) {
        T value = from.get(componentType);
        if (value != null) {
            to.setComponent(componentType, value);
        }
    }

    static {
        COMPONENTS_TO_COPY = Set.of(DataComponents.CUSTOM_NAME, DataComponents.CUSTOM_DATA);
    }
}

