/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyBonusCount
extends LootItemConditionalFunction {
    private static final Map<Identifier, FormulaType> FORMULAS = Stream.of(BinomialWithBonusCount.TYPE, OreDrops.TYPE, UniformBonusCount.TYPE).collect(Collectors.toMap(FormulaType::id, Function.identity()));
    private static final Codec<FormulaType> FORMULA_TYPE_CODEC = Identifier.CODEC.comapFlatMap(location -> {
        FormulaType type = FORMULAS.get(location);
        if (type != null) {
            return DataResult.success((Object)type);
        }
        return DataResult.error(() -> "No formula type with id: '" + String.valueOf(location) + "'");
    }, FormulaType::id);
    private static final MapCodec<Formula> FORMULA_CODEC = ExtraCodecs.dispatchOptionalValue("formula", "parameters", FORMULA_TYPE_CODEC, Formula::getType, FormulaType::codec);
    public static final MapCodec<ApplyBonusCount> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> ApplyBonusCount.commonFields(i).and(i.group((App)Enchantment.CODEC.fieldOf("enchantment").forGetter(f -> f.enchantment), (App)FORMULA_CODEC.forGetter(f -> f.formula))).apply((Applicative)i, ApplyBonusCount::new));
    private final Holder<Enchantment> enchantment;
    private final Formula formula;

    private ApplyBonusCount(List<LootItemCondition> predicates, Holder<Enchantment> enchantment, Formula formula) {
        super(predicates);
        this.enchantment = enchantment;
        this.formula = formula;
    }

    public MapCodec<ApplyBonusCount> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.TOOL);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, tool);
            int newCount = this.formula.calculateNewCount(context.getRandom(), itemStack.getCount(), level);
            itemStack.setCount(newCount);
        }
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(Holder<Enchantment> enchantment, float probability, int extraRounds) {
        return ApplyBonusCount.simpleBuilder(conditions -> new ApplyBonusCount((List<LootItemCondition>)conditions, enchantment, new BinomialWithBonusCount(extraRounds, probability)));
    }

    public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Holder<Enchantment> enchantment) {
        return ApplyBonusCount.simpleBuilder(conditions -> new ApplyBonusCount((List<LootItemCondition>)conditions, enchantment, OreDrops.INSTANCE));
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> enchantment) {
        return ApplyBonusCount.simpleBuilder(conditions -> new ApplyBonusCount((List<LootItemCondition>)conditions, enchantment, new UniformBonusCount(1)));
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> enchantment, int bonusMultiplier) {
        return ApplyBonusCount.simpleBuilder(conditions -> new ApplyBonusCount((List<LootItemCondition>)conditions, enchantment, new UniformBonusCount(bonusMultiplier)));
    }

    private static interface Formula {
        public int calculateNewCount(RandomSource var1, int var2, int var3);

        public FormulaType getType();
    }

    private record UniformBonusCount(int bonusMultiplier) implements Formula
    {
        public static final Codec<UniformBonusCount> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.fieldOf("bonusMultiplier").forGetter(UniformBonusCount::bonusMultiplier)).apply((Applicative)i, UniformBonusCount::new));
        public static final FormulaType TYPE = new FormulaType(Identifier.withDefaultNamespace("uniform_bonus_count"), CODEC);

        @Override
        public int calculateNewCount(RandomSource random, int count, int level) {
            return count + random.nextInt(this.bonusMultiplier * level + 1);
        }

        @Override
        public FormulaType getType() {
            return TYPE;
        }
    }

    private record OreDrops() implements Formula
    {
        public static final OreDrops INSTANCE = new OreDrops();
        public static final Codec<OreDrops> CODEC = MapCodec.unitCodec((Object)INSTANCE);
        public static final FormulaType TYPE = new FormulaType(Identifier.withDefaultNamespace("ore_drops"), CODEC);

        @Override
        public int calculateNewCount(RandomSource random, int count, int level) {
            if (level > 0) {
                int bonus = random.nextInt(level + 2) - 1;
                if (bonus < 0) {
                    bonus = 0;
                }
                return count * (bonus + 1);
            }
            return count;
        }

        @Override
        public FormulaType getType() {
            return TYPE;
        }
    }

    private record BinomialWithBonusCount(int extraRounds, float probability) implements Formula
    {
        private static final Codec<BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.fieldOf("extra").forGetter(BinomialWithBonusCount::extraRounds), (App)Codec.FLOAT.fieldOf("probability").forGetter(BinomialWithBonusCount::probability)).apply((Applicative)i, BinomialWithBonusCount::new));
        public static final FormulaType TYPE = new FormulaType(Identifier.withDefaultNamespace("binomial_with_bonus_count"), CODEC);

        @Override
        public int calculateNewCount(RandomSource random, int count, int level) {
            for (int i = 0; i < level + this.extraRounds; ++i) {
                if (!(random.nextFloat() < this.probability)) continue;
                ++count;
            }
            return count;
        }

        @Override
        public FormulaType getType() {
            return TYPE;
        }
    }

    private record FormulaType(Identifier id, Codec<? extends Formula> codec) {
    }
}

