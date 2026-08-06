/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class CrossbowItem
extends ProjectileWeaponItem {
    private static final float MAX_CHARGE_DURATION = 1.25f;
    public static final int DEFAULT_RANGE = 8;
    private boolean startSoundPlayed = false;
    private boolean midLoadSoundPlayed = false;
    private static final float START_SOUND_PERCENT = 0.2f;
    private static final float MID_SOUND_PERCENT = 0.5f;
    private static final float ARROW_POWER = 3.15f;
    private static final float FIREWORK_POWER = 1.6f;
    public static final float MOB_ARROW_POWER = 1.6f;
    private static final ChargingSounds DEFAULT_SOUNDS = new ChargingSounds(Optional.of(SoundEvents.CROSSBOW_LOADING_START), Optional.of(SoundEvents.CROSSBOW_LOADING_MIDDLE), Optional.of(SoundEvents.CROSSBOW_LOADING_END));

    public CrossbowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return ARROW_OR_FIREWORK;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        ChargedProjectiles chargedProjectiles = itemStack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
            this.performShooting(level, player, hand, itemStack, CrossbowItem.getShootingPower(chargedProjectiles), 1.0f, null);
            return InteractionResult.CONSUME;
        }
        if (!player.getProjectile(itemStack).isEmpty()) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    private static float getShootingPower(ChargedProjectiles projectiles) {
        if (projectiles.contains(Items.FIREWORK_ROCKET)) {
            return 1.6f;
        }
        return 3.15f;
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
        return CrossbowItem.getPowerForTime(timeHeld, itemStack, entity) >= 1.0f && CrossbowItem.isCharged(itemStack);
    }

    private static boolean tryLoadProjectiles(LivingEntity shooter, ItemStack heldItem) {
        List<ItemStack> drawn = CrossbowItem.draw(heldItem, shooter.getProjectile(heldItem), shooter);
        if (!drawn.isEmpty()) {
            heldItem.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(drawn));
            return true;
        }
        return false;
    }

    public static boolean isCharged(ItemStack itemStack) {
        ChargedProjectiles projectiles = itemStack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        return !projectiles.isEmpty();
    }

    @Override
    protected void shootProjectile(LivingEntity livingEntity, Projectile projectileEntity, int index, float power, float uncertainty, float angle, @Nullable LivingEntity targetOverride) {
        Vector3f shotVector;
        if (targetOverride != null) {
            double xd = targetOverride.getX() - livingEntity.getX();
            double zd = targetOverride.getZ() - livingEntity.getZ();
            double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
            double yd = targetOverride.getY(0.3333333333333333) - projectileEntity.getY() + distanceToTarget * (double)0.2f;
            shotVector = CrossbowItem.getProjectileShotVector(livingEntity, new Vec3(xd, yd, zd), angle);
        } else {
            Vec3 upVector = livingEntity.getUpVector(1.0f);
            Quaternionf upQuaternion = new Quaternionf().setAngleAxis((double)(angle * ((float)Math.PI / 180)), upVector.x, upVector.y, upVector.z);
            Vec3 viewVec = livingEntity.getViewVector(1.0f);
            shotVector = viewVec.toVector3f().rotate((Quaternionfc)upQuaternion);
        }
        projectileEntity.shoot(shotVector.x(), shotVector.y(), shotVector.z(), power, uncertainty);
        float soundPitch = CrossbowItem.getShotPitch(livingEntity.getRandom(), index);
        livingEntity.level().playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CROSSBOW_SHOOT, livingEntity.getSoundSource(), 1.0f, soundPitch);
    }

    private static Vector3f getProjectileShotVector(LivingEntity body, Vec3 originalVector, float angle) {
        Vector3f viewVec = originalVector.toVector3f().normalize();
        Vector3f rightVectorPreRot = new Vector3f((Vector3fc)viewVec).cross((Vector3fc)new Vector3f(0.0f, 1.0f, 0.0f));
        if ((double)rightVectorPreRot.lengthSquared() <= 1.0E-7) {
            Vec3 up = body.getUpVector(1.0f);
            rightVectorPreRot = new Vector3f((Vector3fc)viewVec).cross((Vector3fc)up.toVector3f());
        }
        Vector3f viewVec3f = new Vector3f((Vector3fc)viewVec).rotateAxis(1.5707964f, rightVectorPreRot.x, rightVectorPreRot.y, rightVectorPreRot.z);
        return new Vector3f((Vector3fc)viewVec).rotateAxis(angle * ((float)Math.PI / 180), viewVec3f.x, viewVec3f.y, viewVec3f.z);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack heldItem, ItemStack projectile, boolean isCrit) {
        if (projectile.is(Items.FIREWORK_ROCKET)) {
            return new FireworkRocketEntity(level, projectile, shooter, shooter.getX(), shooter.getEyeY() - (double)0.15f, shooter.getZ(), true);
        }
        Projectile projectileEntity = super.createProjectile(level, shooter, heldItem, projectile, isCrit);
        if (projectileEntity instanceof AbstractArrow) {
            AbstractArrow arrow = (AbstractArrow)projectileEntity;
            arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        }
        return projectileEntity;
    }

    @Override
    protected int getDurabilityUse(ItemStack projectile) {
        return projectile.is(Items.FIREWORK_ROCKET) ? 3 : 1;
    }

    public void performShooting(Level level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, float power, float uncertainty, @Nullable LivingEntity targetOverride) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        ChargedProjectiles charged = weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (charged == null || charged.isEmpty()) {
            return;
        }
        this.shoot(serverLevel, shooter, hand, weapon, charged.itemCopies(), power, uncertainty, shooter instanceof Player, targetOverride);
        if (shooter instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)shooter;
            CriteriaTriggers.SHOT_CROSSBOW.trigger(player, weapon);
            player.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
        }
    }

    private static float getShotPitch(RandomSource random, int index) {
        if (index == 0) {
            return 1.0f;
        }
        return CrossbowItem.getRandomShotPitch((index & 1) == 1, random);
    }

    private static float getRandomShotPitch(boolean highPitch, RandomSource random) {
        float rangeDecider = highPitch ? 0.63f : 0.43f;
        return 1.0f / (random.nextFloat() * 0.5f + 1.8f) + rangeDecider;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int ticksRemaining) {
        if (!level.isClientSide()) {
            ChargingSounds sounds = CrossbowItem.getChargingSounds(itemStack);
            float tickPercent = (float)(itemStack.getUseDuration(entity) - ticksRemaining) / (float)CrossbowItem.getChargeDuration(itemStack, entity);
            if (tickPercent < 0.2f) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }
            if (tickPercent >= 0.2f && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                sounds.start().ifPresent(sound -> level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)sound.value(), SoundSource.PLAYERS, 0.5f, 1.0f));
            }
            if (tickPercent >= 0.5f && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                sounds.mid().ifPresent(sound -> level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)sound.value(), SoundSource.PLAYERS, 0.5f, 1.0f));
            }
            if (tickPercent >= 1.0f && !CrossbowItem.isCharged(itemStack) && CrossbowItem.tryLoadProjectiles(entity, itemStack)) {
                sounds.end().ifPresent(sound -> level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)sound.value(), entity.getSoundSource(), 1.0f, 1.0f / (level.getRandom().nextFloat() * 0.5f + 1.0f) + 0.2f));
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity user) {
        return 72000;
    }

    public static int getChargeDuration(ItemStack crossbow, LivingEntity user) {
        float duration = EnchantmentHelper.modifyCrossbowChargingTime(crossbow, user, 1.25f);
        return Mth.floor(duration * 20.0f);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.CROSSBOW;
    }

    private static ChargingSounds getChargingSounds(ItemStack itemStack) {
        return EnchantmentHelper.pickHighestLevel(itemStack, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
    }

    private static float getPowerForTime(int timeHeld, ItemStack itemStack, LivingEntity holder) {
        float pow = (float)timeHeld / (float)CrossbowItem.getChargeDuration(itemStack, holder);
        if (pow > 1.0f) {
            pow = 1.0f;
        }
        return pow;
    }

    @Override
    public boolean useOnRelease(ItemStack itemStack) {
        return itemStack.is(this);
    }

    @Override
    public int getDefaultProjectileRange() {
        return 8;
    }

    public record ChargingSounds(Optional<Holder<SoundEvent>> start, Optional<Holder<SoundEvent>> mid, Optional<Holder<SoundEvent>> end) {
        public static final Codec<ChargingSounds> CODEC = RecordCodecBuilder.create(i -> i.group((App)SoundEvent.CODEC.optionalFieldOf("start").forGetter(ChargingSounds::start), (App)SoundEvent.CODEC.optionalFieldOf("mid").forGetter(ChargingSounds::mid), (App)SoundEvent.CODEC.optionalFieldOf("end").forGetter(ChargingSounds::end)).apply((Applicative)i, ChargingSounds::new));
    }

    public static enum ChargeType implements StringRepresentable
    {
        NONE("none"),
        ARROW("arrow"),
        ROCKET("rocket");

        public static final Codec<ChargeType> CODEC;
        private final String name;

        private ChargeType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(ChargeType::values);
        }
    }
}

