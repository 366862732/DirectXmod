/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public record TeamPredicate(String team) implements EntitySubPredicate
{
    public static final Codec<TeamPredicate> CODEC = Codec.STRING.xmap(TeamPredicate::new, TeamPredicate::team);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        PlayerTeam team = entity.getTeam();
        return team != null && this.team.equals(((Team)team).getName());
    }
}

