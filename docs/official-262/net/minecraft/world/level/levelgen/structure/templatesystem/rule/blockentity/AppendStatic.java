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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import org.jspecify.annotations.Nullable;

public class AppendStatic
implements RuleBlockEntityModifier {
    public static final MapCodec<AppendStatic> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)CompoundTag.CODEC.fieldOf("data").forGetter(r -> r.tag)).apply((Applicative)i, AppendStatic::new));
    private final CompoundTag tag;

    public AppendStatic(CompoundTag tag) {
        this.tag = tag;
    }

    @Override
    public CompoundTag apply(RandomSource random, @Nullable CompoundTag existingTag) {
        return existingTag == null ? this.tag.copy() : existingTag.merge(this.tag);
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_STATIC;
    }
}

