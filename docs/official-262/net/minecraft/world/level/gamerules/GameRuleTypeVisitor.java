/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.gamerules;

import net.minecraft.world.level.gamerules.GameRule;

public interface GameRuleTypeVisitor {
    default public <T> void visit(GameRule<T> gameRule) {
    }

    default public void visitBoolean(GameRule<Boolean> gameRule) {
    }

    default public void visitInteger(GameRule<Integer> gameRule) {
    }
}

