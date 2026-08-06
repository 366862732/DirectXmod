/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.HasGamemasterPermissionReaction;
import net.minecraft.client.gui.screens.worldselection.AbstractGameRulesScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class InWorldGameRulesScreen
extends AbstractGameRulesScreen
implements HasGamemasterPermissionReaction {
    private static final Component PENDING_TEXT = Component.translatable("editGamerule.inGame.downloadingGamerules");
    private final GameRuleMap initialValues = GameRuleMap.of();
    private final List<GameRule<?>> serverProvidedRules = new ArrayList();
    private final ClientPacketListener connection;
    private final Screen lastScreen;
    private @Nullable LoadingDotsWidget loadingDotsWidget;
    private boolean receivedServerValues = false;

    public InWorldGameRulesScreen(ClientPacketListener connection, Consumer<Optional<GameRules>> exitCallback, Screen lastScreen) {
        super(new GameRules(connection.enabledFeatures()), exitCallback);
        this.connection = connection;
        this.lastScreen = lastScreen;
    }

    @Override
    protected void initContent() {
        this.loadingDotsWidget = new LoadingDotsWidget(this.font, PENDING_TEXT);
        this.layout.addToContents(this.loadingDotsWidget);
        this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_GAMERULE_VALUES));
    }

    @Override
    protected void onDone() {
        ArrayList<ServerboundSetGameRulePacket.Entry> changedEntries = new ArrayList<ServerboundSetGameRulePacket.Entry>();
        this.initialValues.keySet().forEach(rule -> this.collectChangedGameRule((GameRule)rule, (List<ServerboundSetGameRulePacket.Entry>)changedEntries));
        if (!changedEntries.isEmpty()) {
            this.connection.send(new ServerboundSetGameRulePacket(changedEntries));
        }
        this.closeAndApplyChanges();
    }

    private <T> void collectChangedGameRule(GameRule<T> rule, List<ServerboundSetGameRulePacket.Entry> entries) {
        if (this.hasGameRuleChanged(rule)) {
            Object currentValue = this.gameRules.get(rule);
            BuiltInRegistries.GAME_RULE.getResourceKey(rule).ifPresent(key -> entries.add(new ServerboundSetGameRulePacket.Entry((ResourceKey<GameRule<?>>)key, rule.serialize(currentValue))));
        }
    }

    @Override
    public void onClose() {
        if (this.hasPendingChanges()) {
            this.minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    this.closeAndDiscardChanges();
                } else {
                    this.minecraft.gui.setScreen(this);
                }
            }, Component.translatable("editGamerule.inGame.discardChanges.title"), (Component)Component.translatable("editGamerule.inGame.discardChanges.message")));
        } else {
            this.closeAndDiscardChanges();
        }
    }

    private boolean hasPendingChanges() {
        return this.initialValues.keySet().stream().anyMatch(this::hasGameRuleChanged);
    }

    private <T> boolean hasGameRuleChanged(GameRule<T> rule) {
        return !this.gameRules.get(rule).equals(this.initialValues.get(rule));
    }

    public void onGameRuleValuesUpdated(Map<ResourceKey<GameRule<?>>, String> values) {
        if (this.receivedServerValues) {
            return;
        }
        this.receivedServerValues = true;
        values.forEach((key, valueStr) -> {
            GameRule<?> rule = BuiltInRegistries.GAME_RULE.getValue((ResourceKey<GameRule<?>>)key);
            if (rule != null) {
                this.serverProvidedRules.add(rule);
                this.initializeGameRuleValue(rule, (String)valueStr);
            }
        });
        if (this.loadingDotsWidget != null) {
            this.removeWidget(this.loadingDotsWidget);
        }
        GameRules serverGameRules = new GameRules(this.serverProvidedRules);
        this.ruleList = this.layout.addToContents(new AbstractGameRulesScreen.RuleList(this, serverGameRules));
        this.addRenderableWidget(this.ruleList);
        this.repositionElements();
    }

    private <T> void initializeGameRuleValue(GameRule<T> rule, String valueStr) {
        rule.deserialize(valueStr).result().ifPresent(value -> {
            this.initialValues.set(rule, value);
            this.gameRules.set(rule, value, null);
        });
    }

    @Override
    public void onGamemasterPermissionChanged(boolean hasGamemasterPermission) {
        if (!hasGamemasterPermission) {
            this.minecraft.gui.setScreen(this.lastScreen);
            Screen screen = this.minecraft.gui.screen();
            if (screen instanceof HasGamemasterPermissionReaction) {
                HasGamemasterPermissionReaction screen2 = (HasGamemasterPermissionReaction)((Object)screen);
                screen2.onGamemasterPermissionChanged(hasGamemasterPermission);
            }
        }
    }
}

