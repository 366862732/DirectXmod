/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.fish;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TropicalFish
extends AbstractSchoolingFish {
    public static final Variant DEFAULT_VARIANT = new Variant(Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE);
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
    public static final List<Variant> COMMON_VARIANTS = List.of(new Variant(Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY), new Variant(Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY), new Variant(Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE), new Variant(Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY), new Variant(Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY), new Variant(Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE), new Variant(Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE), new Variant(Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW), new Variant(Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED), new Variant(Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW), new Variant(Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY), new Variant(Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE), new Variant(Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK), new Variant(Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE), new Variant(Pattern.BETTY, DyeColor.RED, DyeColor.WHITE), new Variant(Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED), new Variant(Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE), new Variant(Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW), new Variant(Pattern.KOB, DyeColor.RED, DyeColor.WHITE), new Variant(Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE), new Variant(Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW), new Variant(Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW));
    private boolean isSchool = true;

    public TropicalFish(EntityType<? extends TropicalFish> type, Level level) {
        super((EntityType<? extends AbstractSchoolingFish>)type, level);
    }

    public static String getPredefinedName(int index) {
        return "entity.minecraft.tropical_fish.predefined." + index;
    }

    private static int packVariant(Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
        return pattern.getPackedId() & 0xFFFF | (baseColor.getId() & 0xFF) << 16 | (patternColor.getId() & 0xFF) << 24;
    }

    public static DyeColor getBaseColor(int packedVariant) {
        return DyeColor.byId(packedVariant >> 16 & 0xFF);
    }

    public static DyeColor getPatternColor(int packedVariant) {
        return DyeColor.byId(packedVariant >> 24 & 0xFF);
    }

    public static Pattern getPattern(int packedVariant) {
        return Pattern.byId(packedVariant & 0xFFFF);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_TYPE_VARIANT, DEFAULT_VARIANT.getPackedId());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Variant", Variant.CODEC, new Variant(this.getPackedVariant()));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Variant variant = input.read("Variant", Variant.CODEC).orElse(DEFAULT_VARIANT);
        this.setPackedVariant(variant.getPackedId());
    }

    private void setPackedVariant(int i) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, i);
    }

    @Override
    public boolean isMaxGroupSizeReached(int groupSize) {
        return !this.isSchool;
    }

    private int getPackedVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    public DyeColor getBaseColor() {
        return TropicalFish.getBaseColor(this.getPackedVariant());
    }

    public DyeColor getPatternColor() {
        return TropicalFish.getPatternColor(this.getPackedVariant());
    }

    public Pattern getPattern() {
        return TropicalFish.getPattern(this.getPackedVariant());
    }

    private void setPattern(Pattern pattern) {
        int base = this.getPackedVariant();
        DyeColor baseColor = TropicalFish.getBaseColor(base);
        DyeColor patternColor = TropicalFish.getPatternColor(base);
        this.setPackedVariant(TropicalFish.packVariant(pattern, baseColor, patternColor));
    }

    private void setBaseColor(DyeColor baseColor) {
        int base = this.getPackedVariant();
        Pattern pattern = TropicalFish.getPattern(base);
        DyeColor patternColor = TropicalFish.getPatternColor(base);
        this.setPackedVariant(TropicalFish.packVariant(pattern, baseColor, patternColor));
    }

    private void setPatternColor(DyeColor patternColor) {
        int base = this.getPackedVariant();
        Pattern pattern = TropicalFish.getPattern(base);
        DyeColor baseColor = TropicalFish.getBaseColor(base);
        this.setPackedVariant(TropicalFish.packVariant(pattern, baseColor, patternColor));
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.TROPICAL_FISH_PATTERN) {
            return TropicalFish.castComponentValue(type, this.getPattern());
        }
        if (type == DataComponents.TROPICAL_FISH_BASE_COLOR) {
            return TropicalFish.castComponentValue(type, this.getBaseColor());
        }
        if (type == DataComponents.TROPICAL_FISH_PATTERN_COLOR) {
            return TropicalFish.castComponentValue(type, this.getPatternColor());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.TROPICAL_FISH_PATTERN);
        this.applyImplicitComponentIfPresent(components, DataComponents.TROPICAL_FISH_BASE_COLOR);
        this.applyImplicitComponentIfPresent(components, DataComponents.TROPICAL_FISH_PATTERN_COLOR);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.TROPICAL_FISH_PATTERN) {
            this.setPattern(TropicalFish.castComponentValue(DataComponents.TROPICAL_FISH_PATTERN, value));
            return true;
        }
        if (type == DataComponents.TROPICAL_FISH_BASE_COLOR) {
            this.setBaseColor(TropicalFish.castComponentValue(DataComponents.TROPICAL_FISH_BASE_COLOR, value));
            return true;
        }
        if (type == DataComponents.TROPICAL_FISH_PATTERN_COLOR) {
            this.setPatternColor(TropicalFish.castComponentValue(DataComponents.TROPICAL_FISH_PATTERN_COLOR, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        super.saveToBucketTag(bucket);
        bucket.copyFrom(DataComponents.TROPICAL_FISH_PATTERN, this);
        bucket.copyFrom(DataComponents.TROPICAL_FISH_BASE_COLOR, this);
        bucket.copyFrom(DataComponents.TROPICAL_FISH_PATTERN_COLOR, this);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        Variant variant;
        groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        RandomSource random = level.getRandom();
        if (groupData instanceof TropicalFishGroupData) {
            TropicalFishGroupData tropicalFishGroupData = (TropicalFishGroupData)groupData;
            variant = tropicalFishGroupData.variant;
        } else if ((double)random.nextFloat() < 0.9) {
            variant = Util.getRandom(COMMON_VARIANTS, random);
            groupData = new TropicalFishGroupData(this, variant);
        } else {
            this.isSchool = false;
            Pattern[] patterns = Pattern.values();
            DyeColor[] colors = DyeColor.values();
            Pattern pattern = Util.getRandom(patterns, random);
            DyeColor baseColor = Util.getRandom(colors, random);
            DyeColor patternColor = Util.getRandom(colors, random);
            variant = new Variant(pattern, baseColor, patternColor);
        }
        this.setPackedVariant(variant.getPackedId());
        return groupData;
    }

    public static boolean checkTropicalFishSpawnRules(EntityType<TropicalFish> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getFluidState(pos.below()).is(FluidTags.WATER) && level.getBlockState(pos.above()).is(Blocks.WATER) && (level.getBiome(pos).is(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT) || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, spawnReason, pos, random));
    }

    public static enum Pattern implements StringRepresentable,
    TooltipProvider
    {
        KOB("kob", Base.SMALL, 0),
        SUNSTREAK("sunstreak", Base.SMALL, 1),
        SNOOPER("snooper", Base.SMALL, 2),
        DASHER("dasher", Base.SMALL, 3),
        BRINELY("brinely", Base.SMALL, 4),
        SPOTTY("spotty", Base.SMALL, 5),
        FLOPPER("flopper", Base.LARGE, 0),
        STRIPEY("stripey", Base.LARGE, 1),
        GLITTER("glitter", Base.LARGE, 2),
        BLOCKFISH("blockfish", Base.LARGE, 3),
        BETTY("betty", Base.LARGE, 4),
        CLAYFISH("clayfish", Base.LARGE, 5);

        public static final Codec<Pattern> CODEC;
        private static final IntFunction<Pattern> BY_ID;
        public static final StreamCodec<ByteBuf, Pattern> STREAM_CODEC;
        private final String name;
        private final Component displayName;
        private final Base base;
        private final int packedId;

        private Pattern(String name, Base base, int index) {
            this.name = name;
            this.base = base;
            this.packedId = base.id | index << 8;
            this.displayName = Component.translatable("entity.minecraft.tropical_fish.type." + this.name);
        }

        public static Pattern byId(int packedId) {
            return BY_ID.apply(packedId);
        }

        public Base base() {
            return this.base;
        }

        public int getPackedId() {
            return this.packedId;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component displayName() {
            return this.displayName;
        }

        @Override
        public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
            DyeColor baseColor = components.getOrDefault(DataComponents.TROPICAL_FISH_BASE_COLOR, DEFAULT_VARIANT.baseColor());
            DyeColor patternColor = components.getOrDefault(DataComponents.TROPICAL_FISH_PATTERN_COLOR, DEFAULT_VARIANT.patternColor());
            ChatFormatting[] styles = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
            int commonIndex = COMMON_VARIANTS.indexOf(new Variant(this, baseColor, patternColor));
            if (commonIndex != -1) {
                consumer.accept(Component.translatable(TropicalFish.getPredefinedName(commonIndex)).withStyle(styles));
                return;
            }
            consumer.accept(this.displayName.plainCopy().withStyle(styles));
            MutableComponent colorComponent = Component.translatable("color.minecraft." + baseColor.getName());
            if (baseColor != patternColor) {
                colorComponent.append(", ").append(Component.translatable("color.minecraft." + patternColor.getName()));
            }
            colorComponent.withStyle(styles);
            consumer.accept(colorComponent);
        }

        static {
            CODEC = StringRepresentable.fromEnum(Pattern::values);
            BY_ID = ByIdMap.sparse(Pattern::getPackedId, Pattern.values(), KOB);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Pattern::getPackedId);
        }
    }

    public record Variant(Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
        public static final Codec<Variant> CODEC = Codec.INT.xmap(Variant::new, Variant::getPackedId);

        public Variant(int packedId) {
            this(TropicalFish.getPattern(packedId), TropicalFish.getBaseColor(packedId), TropicalFish.getPatternColor(packedId));
        }

        public int getPackedId() {
            return TropicalFish.packVariant(this.pattern, this.baseColor, this.patternColor);
        }
    }

    private static class TropicalFishGroupData
    extends AbstractSchoolingFish.SchoolSpawnGroupData {
        private final Variant variant;

        private TropicalFishGroupData(TropicalFish leader, Variant variant) {
            super(leader);
            this.variant = variant;
        }
    }

    public static enum Base {
        SMALL(0),
        LARGE(1);

        private final int id;

        private Base(int id) {
            this.id = id;
        }
    }
}

