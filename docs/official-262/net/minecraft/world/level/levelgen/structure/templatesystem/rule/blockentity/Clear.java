/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import org.jspecify.annotations.Nullable;

public class Clear
implements RuleBlockEntityModifier {
    private static final Clear INSTANCE = new Clear();
    public static final MapCodec<Clear> CODEC = MapCodec.unit((Object)INSTANCE);

    @Override
    public CompoundTag apply(RandomSource random, @Nullable CompoundTag existingTag) {
        return new CompoundTag();
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.CLEAR;
    }
}

