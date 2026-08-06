/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.worldselection;

import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.worldselection.AbstractGameRulesScreen;
import net.minecraft.world.level.gamerules.GameRules;

@Environment(value=EnvType.CLIENT)
public class WorldCreationGameRulesScreen
extends AbstractGameRulesScreen {
    public WorldCreationGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> exitCallback) {
        super(gameRules, exitCallback);
    }

    @Override
    protected void initContent() {
        this.ruleList = this.layout.addToContents(new AbstractGameRulesScreen.RuleList(this, this.gameRules));
    }

    @Override
    protected void onDone() {
        this.closeAndApplyChanges();
    }

    @Override
    public void onClose() {
        this.closeAndDiscardChanges();
    }
}

