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
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.fixes.NamedEntityFix;
import net.minecraft.util.datafix.fixes.References;

public class EntityZombieVillagerTypeFix
extends NamedEntityFix {
    private static final int PROFESSION_MAX = 6;

    public EntityZombieVillagerTypeFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType, "EntityZombieVillagerTypeFix", References.ENTITY, "Zombie");
    }

    public Dynamic<?> fixTag(Dynamic<?> input) {
        if (input.get("IsVillager").asBoolean(false)) {
            if (input.get("ZombieType").result().isEmpty()) {
                int type = this.getVillagerProfession(input.get("VillagerProfession").asInt(-1));
                if (type == -1) {
                    type = this.getVillagerProfession(RandomSource.createThreadLocalInstance().nextInt(6));
                }
                input = input.set("ZombieType", input.createInt(type));
            }
            input = input.remove("IsVillager");
        }
        return input;
    }

    private int getVillagerProfession(int profession) {
        if (profession < 0 || profession >= 6) {
            return -1;
        }
        return profession;
    }

    @Override
    protected Typed<?> fix(Typed<?> entity) {
        return entity.update(DSL.remainderFinder(), this::fixTag);
    }
}

