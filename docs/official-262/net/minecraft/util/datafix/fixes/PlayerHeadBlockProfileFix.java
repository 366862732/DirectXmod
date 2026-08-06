/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class PlayerHeadBlockProfileFix
extends NamedEntityFix {
    public PlayerHeadBlockProfileFix(Schema outputSchema) {
        super(outputSchema, false, "PlayerHeadBlockProfileFix", References.BLOCK_ENTITY, "minecraft:skull");
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fix);
    }

    private <T> Dynamic<T> fix(Dynamic<T> entity) {
        Optional extraType;
        Optional skullOwner = entity.get("SkullOwner").result();
        Optional profile = skullOwner.or(() -> PlayerHeadBlockProfileFix.lambda$fix$0(extraType = entity.get("ExtraType").result()));
        if (profile.isEmpty()) {
            return entity;
        }
        entity = entity.remove("SkullOwner").remove("ExtraType");
        entity = entity.set("profile", ItemStackComponentizationFix.fixProfile((Dynamic)profile.get()));
        return entity;
    }

    private static /* synthetic */ Optional lambda$fix$0(Optional extraType) {
        return extraType;
    }
}

