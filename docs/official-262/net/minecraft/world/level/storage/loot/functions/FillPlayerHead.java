/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FillPlayerHead
extends LootItemConditionalFunction {
    public static final MapCodec<FillPlayerHead> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> FillPlayerHead.commonFields(i).and((App)LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(f -> f.entityTarget)).apply((Applicative)i, FillPlayerHead::new));
    private final LootContext.EntityTarget entityTarget;

    public FillPlayerHead(List<LootItemCondition> predicates, LootContext.EntityTarget entityTarget) {
        super(predicates);
        this.entityTarget = entityTarget;
    }

    public MapCodec<FillPlayerHead> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.entityTarget.contextParam());
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        Entity entity;
        if (itemStack.is(Items.PLAYER_HEAD) && (entity = context.getOptionalParameter(this.entityTarget.contextParam())) instanceof Player) {
            Player dataDonor = (Player)entity;
            itemStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(dataDonor.getGameProfile()));
        }
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> fillPlayerHead(LootContext.EntityTarget entityTarget) {
        return FillPlayerHead.simpleBuilder(conditions -> new FillPlayerHead((List<LootItemCondition>)conditions, entityTarget));
    }
}

