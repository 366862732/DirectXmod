/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BeaconBlockEntity
extends BlockEntity
implements MenuProvider,
Nameable,
BeaconBeamOwner {
    private static final int MAX_LEVELS = 4;
    private static final int LEVELS_NEEDED_FOR_SECONDARY = 4;
    public static final List<List<Holder<MobEffect>>> BEACON_EFFECTS = List.of(List.of(MobEffects.SPEED, MobEffects.HASTE), List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST), List.of(MobEffects.STRENGTH), List.of(MobEffects.REGENERATION));
    private static final Set<Holder<MobEffect>> VALID_EFFECTS = BEACON_EFFECTS.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    public static final int DATA_LEVELS = 0;
    public static final int DATA_PRIMARY = 1;
    public static final int DATA_SECONDARY = 2;
    public static final int NUM_DATA_VALUES = 3;
    private static final int BLOCKS_CHECK_PER_TICK = 10;
    private static final Component DEFAULT_NAME = Component.translatable("container.beacon");
    private static final String TAG_PRIMARY = "primary_effect";
    private static final String TAG_SECONDARY = "secondary_effect";
    private List<BeaconBeamOwner.Section> beamSections = new ArrayList<BeaconBeamOwner.Section>();
    private List<BeaconBeamOwner.Section> checkingBeamSections = new ArrayList<BeaconBeamOwner.Section>();
    private int levels;
    private int lastCheckY;
    private @Nullable Holder<MobEffect> primaryPower;
    private @Nullable Holder<MobEffect> secondaryPower;
    private @Nullable Component name;
    private LockCode lockKey = LockCode.NO_LOCK;
    private final ContainerData dataAccess = new ContainerData(this){
        final /* synthetic */ BeaconBlockEntity this$0;
        {
            BeaconBlockEntity beaconBlockEntity = this$0;
            Objects.requireNonNull(beaconBlockEntity);
            this.this$0 = beaconBlockEntity;
        }

        @Override
        public int get(int dataId) {
            return switch (dataId) {
                case 0 -> this.this$0.levels;
                case 1 -> BeaconMenu.encodeEffect(this.this$0.primaryPower);
                case 2 -> BeaconMenu.encodeEffect(this.this$0.secondaryPower);
                default -> 0;
            };
        }

        @Override
        public void set(int dataId, int value) {
            switch (dataId) {
                case 0: {
                    this.this$0.levels = value;
                    break;
                }
                case 1: {
                    if (!this.this$0.level.isClientSide() && !this.this$0.beamSections.isEmpty()) {
                        BeaconBlockEntity.playSound(this.this$0.level, this.this$0.worldPosition, SoundEvents.BEACON_POWER_SELECT);
                    }
                    this.this$0.primaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(value));
                    break;
                }
                case 2: {
                    this.this$0.secondaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(value));
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    private static @Nullable Holder<MobEffect> filterEffect(@Nullable Holder<MobEffect> effect) {
        return VALID_EFFECTS.contains(effect) ? effect : null;
    }

    public BeaconBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.BEACON, worldPosition, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState selfState, BeaconBlockEntity entity) {
        BlockPos checkPos;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (entity.lastCheckY < y) {
            checkPos = pos;
            entity.checkingBeamSections = Lists.newArrayList();
            entity.lastCheckY = checkPos.getY() - 1;
        } else {
            checkPos = new BlockPos(x, entity.lastCheckY + 1, z);
        }
        BeaconBeamOwner.Section lastBeamSection = entity.checkingBeamSections.isEmpty() ? null : entity.checkingBeamSections.get(entity.checkingBeamSections.size() - 1);
        int lastSetBlock = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int i = 0; i < 10 && checkPos.getY() <= lastSetBlock; ++i) {
            block18: {
                BlockState state;
                block16: {
                    int color;
                    block17: {
                        state = level.getBlockState(checkPos);
                        Block block = state.getBlock();
                        if (!(block instanceof BeaconBeamBlock)) break block16;
                        BeaconBeamBlock beaconBeamBlock = (BeaconBeamBlock)((Object)block);
                        color = beaconBeamBlock.getColor().getTextureDiffuseColor();
                        if (entity.checkingBeamSections.size() > 1) break block17;
                        lastBeamSection = new BeaconBeamOwner.Section(color);
                        entity.checkingBeamSections.add(lastBeamSection);
                        break block18;
                    }
                    if (lastBeamSection == null) break block18;
                    if (color == lastBeamSection.getColor()) {
                        lastBeamSection.increaseHeight();
                    } else {
                        lastBeamSection = new BeaconBeamOwner.Section(ARGB.average(lastBeamSection.getColor(), color));
                        entity.checkingBeamSections.add(lastBeamSection);
                    }
                    break block18;
                }
                if (lastBeamSection != null && (state.getLightDampening() < 15 || state.is(Blocks.BEDROCK))) {
                    lastBeamSection.increaseHeight();
                } else {
                    entity.checkingBeamSections.clear();
                    entity.lastCheckY = lastSetBlock;
                    break;
                }
            }
            checkPos = checkPos.above();
            ++entity.lastCheckY;
        }
        int previousLevels = entity.levels;
        if (level.getGameTime() % 80L == 0L) {
            if (!entity.beamSections.isEmpty()) {
                entity.levels = BeaconBlockEntity.updateBase(level, x, y, z);
            }
            if (entity.levels > 0 && !entity.beamSections.isEmpty()) {
                BeaconBlockEntity.applyEffects(level, pos, entity.levels, entity.primaryPower, entity.secondaryPower);
                BeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_AMBIENT);
            }
        }
        if (entity.lastCheckY >= lastSetBlock) {
            entity.lastCheckY = level.getMinY() - 1;
            boolean wasActive = previousLevels > 0;
            entity.beamSections = entity.checkingBeamSections;
            if (!level.isClientSide()) {
                boolean isActive;
                boolean bl = isActive = entity.levels > 0;
                if (!wasActive && isActive) {
                    BeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
                    for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(x, y, z, x, y - 4, z).inflate(10.0, 5.0, 10.0))) {
                        CriteriaTriggers.CONSTRUCT_BEACON.trigger(player, entity.levels);
                    }
                } else if (wasActive && !isActive) {
                    BeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
        }
    }

    private static int updateBase(Level level, int x, int y, int z) {
        int ly;
        int levels = 0;
        int step = 1;
        while (step <= 4 && (ly = y - step) >= level.getMinY()) {
            boolean isOk = true;
            block1: for (int lx = x - step; lx <= x + step && isOk; ++lx) {
                for (int lz = z - step; lz <= z + step; ++lz) {
                    if (level.getBlockState(new BlockPos(lx, ly, lz)).is(BlockTags.BEACON_BASE_BLOCKS)) continue;
                    isOk = false;
                    continue block1;
                }
            }
            if (!isOk) break;
            levels = step++;
        }
        return levels;
    }

    @Override
    public void setRemoved() {
        BeaconBlockEntity.playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    public static boolean validateEffects(@Nullable Holder<MobEffect> primary, @Nullable Holder<MobEffect> secondary, int levels) {
        if (secondary != null && levels < 4) {
            return false;
        }
        int primaryLevel = BeaconBlockEntity.getRequiredLevelsFor(primary);
        int secondaryLevel = BeaconBlockEntity.getRequiredLevelsFor(secondary);
        if (primaryLevel > levels || secondaryLevel > levels) {
            return false;
        }
        if (primaryLevel >= 4) {
            return false;
        }
        return secondaryLevel == 0 || secondaryLevel >= 4 || primary.equals(secondary);
    }

    private static int getRequiredLevelsFor(@Nullable Holder<MobEffect> effect) {
        if (effect == null) {
            return 0;
        }
        for (int i = 0; i < BEACON_EFFECTS.size(); ++i) {
            List<Holder<MobEffect>> effectsForLevel = BEACON_EFFECTS.get(i);
            if (!effectsForLevel.contains(effect)) continue;
            return i + 1;
        }
        return Integer.MAX_VALUE;
    }

    private static void applyEffects(Level level, BlockPos worldPosition, int levels, @Nullable Holder<MobEffect> primaryPower, @Nullable Holder<MobEffect> secondaryPower) {
        if (level.isClientSide() || primaryPower == null) {
            return;
        }
        double range = levels * 10 + 10;
        int baseAmp = 0;
        if (levels >= 4 && Objects.equals(primaryPower, secondaryPower)) {
            baseAmp = 1;
        }
        int durationTicks = (9 + levels * 2) * 20;
        AABB bb = new AABB(worldPosition).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, bb);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(primaryPower, durationTicks, baseAmp, true, true));
        }
        if (levels >= 4 && !Objects.equals(primaryPower, secondaryPower) && secondaryPower != null) {
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(secondaryPower, durationTicks, 0, true, true));
            }
        }
    }

    public static void playSound(Level level, BlockPos worldPosition, SoundEvent event) {
        level.playSound(null, worldPosition, event, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public List<BeaconBeamOwner.Section> getBeamSections() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSections;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    private static void storeEffect(ValueOutput output, String field, @Nullable Holder<MobEffect> effect) {
        if (effect != null) {
            effect.unwrapKey().ifPresent(key -> output.putString(field, key.identifier().toString()));
        }
    }

    private static @Nullable Holder<MobEffect> loadEffect(ValueInput input, String field) {
        return input.read(field, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(VALID_EFFECTS::contains).orElse(null);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.primaryPower = BeaconBlockEntity.loadEffect(input, TAG_PRIMARY);
        this.secondaryPower = BeaconBlockEntity.loadEffect(input, TAG_SECONDARY);
        this.name = BeaconBlockEntity.parseCustomNameSafe(input, "CustomName");
        this.lockKey = LockCode.fromTag(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        BeaconBlockEntity.storeEffect(output, TAG_PRIMARY, this.primaryPower);
        BeaconBlockEntity.storeEffect(output, TAG_SECONDARY, this.secondaryPower);
        output.putInt("Levels", this.levels);
        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
        this.lockKey.addToTag(output);
    }

    public void setCustomName(@Nullable Component name) {
        this.name = name;
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.lockKey.canUnlock(player)) {
            return new BeaconMenu(containerId, inventory, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()));
        }
        BaseContainerBlockEntity.sendChestLockedNotifications(Vec3.atCenterOf(this.getBlockPos()), player, this.getDisplayName());
        return null;
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        if (this.name != null) {
            return this.name;
        }
        return DEFAULT_NAME;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.name = components.get(DataComponents.CUSTOM_NAME);
        this.lockKey = components.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.name);
        if (!this.lockKey.equals(LockCode.NO_LOCK)) {
            components.set(DataComponents.LOCK, this.lockKey);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("CustomName");
        output.discard("lock");
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.lastCheckY = level.getMinY() - 1;
    }
}

