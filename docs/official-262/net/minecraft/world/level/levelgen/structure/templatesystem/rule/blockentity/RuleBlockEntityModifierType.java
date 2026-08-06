/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendStatic;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Clear;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Passthrough;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;

public interface RuleBlockEntityModifierType<P extends RuleBlockEntityModifier> {
    public static final RuleBlockEntityModifierType<Clear> CLEAR = RuleBlockEntityModifierType.register("clear", Clear.CODEC);
    public static final RuleBlockEntityModifierType<Passthrough> PASSTHROUGH = RuleBlockEntityModifierType.register("passthrough", Passthrough.CODEC);
    public static final RuleBlockEntityModifierType<AppendStatic> APPEND_STATIC = RuleBlockEntityModifierType.register("append_static", AppendStatic.CODEC);
    public static final RuleBlockEntityModifierType<AppendLoot> APPEND_LOOT = RuleBlockEntityModifierType.register("append_loot", AppendLoot.CODEC);

    public MapCodec<P> codec();

    private static <P extends RuleBlockEntityModifier> RuleBlockEntityModifierType<P> register(String id, MapCodec<P> codec) {
        return Registry.register(BuiltInRegistries.RULE_BLOCK_ENTITY_MODIFIER, id, () -> codec);
    }
}

