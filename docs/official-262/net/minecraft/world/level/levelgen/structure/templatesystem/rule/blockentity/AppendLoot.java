/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public class AppendLoot
implements RuleBlockEntityModifier {
    public static final MapCodec<AppendLoot> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(c -> c.lootTable)).apply((Applicative)i, AppendLoot::new));
    private final ResourceKey<LootTable> lootTable;

    public AppendLoot(ResourceKey<LootTable> lootTable) {
        this.lootTable = lootTable;
    }

    @Override
    public CompoundTag apply(RandomSource random, @Nullable CompoundTag existingTag) {
        CompoundTag result = existingTag == null ? new CompoundTag() : existingTag.copy();
        result.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
        result.putLong("LootTableSeed", random.nextLong());
        return result;
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_LOOT;
    }
}

