/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BeaconScreen
extends AbstractContainerScreen<BeaconMenu> {
    private static final Identifier BEACON_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/beacon.png");
    private static final Identifier BUTTON_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_disabled");
    private static final Identifier BUTTON_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_selected");
    private static final Identifier BUTTON_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/beacon/button_highlighted");
    private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("container/beacon/button");
    private static final Identifier CONFIRM_SPRITE = Identifier.withDefaultNamespace("container/beacon/confirm");
    private static final Identifier CANCEL_SPRITE = Identifier.withDefaultNamespace("container/beacon/cancel");
    private static final Component PRIMARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.primary");
    private static final Component SECONDARY_EFFECT_LABEL = Component.translatable("block.minecraft.beacon.secondary");
    private final List<BeaconButton> beaconButtons = Lists.newArrayList();
    private @Nullable Holder<MobEffect> primary;
    private @Nullable Holder<MobEffect> secondary;

    public BeaconScreen(final BeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 219);
        menu.addSlotListener(new ContainerListener(){
            final /* synthetic */ BeaconScreen this$0;
            {
                BeaconScreen beaconScreen = this$0;
                Objects.requireNonNull(beaconScreen);
                this.this$0 = beaconScreen;
            }

            @Override
            public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
            }

            @Override
            public void dataChanged(AbstractContainerMenu container, int id, int value) {
                this.this$0.primary = menu.getPrimaryEffect();
                this.this$0.secondary = menu.getSecondaryEffect();
            }
        });
    }

    private <T extends AbstractWidget> void addBeaconButton(T beaconButton) {
        this.addRenderableWidget(beaconButton);
        this.beaconButtons.add((BeaconButton)((Object)beaconButton));
    }

    @Override
    protected void init() {
        BeaconPowerButton beaconPowerButton;
        Holder<MobEffect> effect;
        int c;
        int totalWidth;
        int count;
        int tier;
        super.init();
        this.beaconButtons.clear();
        for (tier = 0; tier <= 2; ++tier) {
            count = BeaconBlockEntity.BEACON_EFFECTS.get(tier).size();
            totalWidth = count * 22 + (count - 1) * 2;
            for (c = 0; c < count; ++c) {
                effect = BeaconBlockEntity.BEACON_EFFECTS.get(tier).get(c);
                beaconPowerButton = new BeaconPowerButton(this, this.leftPos + 76 + c * 24 - totalWidth / 2, this.topPos + 22 + tier * 25, effect, true, tier);
                beaconPowerButton.active = false;
                this.addBeaconButton(beaconPowerButton);
            }
        }
        tier = 3;
        count = BeaconBlockEntity.BEACON_EFFECTS.get(3).size() + 1;
        totalWidth = count * 22 + (count - 1) * 2;
        for (c = 0; c < count - 1; ++c) {
            effect = BeaconBlockEntity.BEACON_EFFECTS.get(3).get(c);
            beaconPowerButton = new BeaconPowerButton(this, this.leftPos + 167 + c * 24 - totalWidth / 2, this.topPos + 47, effect, false, 3);
            beaconPowerButton.active = false;
            this.addBeaconButton(beaconPowerButton);
        }
        Holder<MobEffect> dummyEffect = BeaconBlockEntity.BEACON_EFFECTS.get(0).get(0);
        BeaconUpgradePowerButton beaconPowerButton2 = new BeaconUpgradePowerButton(this, this.leftPos + 167 + (count - 1) * 24 - totalWidth / 2, this.topPos + 47, dummyEffect);
        beaconPowerButton2.visible = false;
        this.addBeaconButton(beaconPowerButton2);
        this.addBeaconButton(new BeaconConfirmButton(this, this.leftPos + 164, this.topPos + 107));
        this.addBeaconButton(new BeaconCancelButton(this, this.leftPos + 190, this.topPos + 107));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.updateButtons();
    }

    private void updateButtons() {
        int levels = ((BeaconMenu)this.menu).getLevels();
        this.beaconButtons.forEach(b -> b.updateStatus(levels));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.centeredText(this.font, PRIMARY_EFFECT_LABEL, 62, 10, -2039584);
        graphics.centeredText(this.font, SECONDARY_EFFECT_LABEL, 169, 10, -2039584);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BEACON_LOCATION, xo, yo, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
        graphics.item(new ItemStack(Items.NETHERITE_INGOT), xo + 20, yo + 109);
        graphics.item(new ItemStack(Items.EMERALD), xo + 41, yo + 109);
        graphics.item(new ItemStack(Items.DIAMOND), xo + 41 + 22, yo + 109);
        graphics.item(new ItemStack(Items.GOLD_INGOT), xo + 42 + 44, yo + 109);
        graphics.item(new ItemStack(Items.IRON_INGOT), xo + 42 + 66, yo + 109);
    }

    @Environment(value=EnvType.CLIENT)
    private static interface BeaconButton {
        public void updateStatus(int var1);
    }

    @Environment(value=EnvType.CLIENT)
    private class BeaconPowerButton
    extends BeaconScreenButton {
        private final boolean isPrimary;
        protected final int tier;
        private Holder<MobEffect> effect;
        private Identifier sprite;
        final /* synthetic */ BeaconScreen this$0;

        public BeaconPowerButton(BeaconScreen beaconScreen, int x, int y, Holder<MobEffect> effect, boolean isPrimary, int tier) {
            BeaconScreen beaconScreen2 = beaconScreen;
            Objects.requireNonNull(beaconScreen2);
            this.this$0 = beaconScreen2;
            super(x, y);
            this.isPrimary = isPrimary;
            this.tier = tier;
            this.setEffect(effect);
        }

        protected void setEffect(Holder<MobEffect> effect) {
            this.effect = effect;
            this.sprite = Hud.getMobEffectSprite(effect);
            this.setTooltip(Tooltip.create(this.createEffectDescription(effect), null));
        }

        protected MutableComponent createEffectDescription(Holder<MobEffect> effect) {
            return Component.translatable(effect.value().getDescriptionId());
        }

        @Override
        public void onPress(InputWithModifiers input) {
            if (this.isSelected()) {
                return;
            }
            if (this.isPrimary) {
                this.this$0.primary = this.effect;
                if (!Objects.equals(this.this$0.secondary, this.effect)) {
                    this.this$0.secondary = null;
                }
            } else {
                this.this$0.secondary = this.effect;
            }
            this.this$0.updateButtons();
        }

        @Override
        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX() + 2, this.getY() + 2, 18, 18);
        }

        @Override
        public void updateStatus(int levels) {
            this.active = this.tier < levels;
            this.setSelected(this.effect.equals(this.isPrimary ? this.this$0.primary : this.this$0.secondary));
        }

        @Override
        protected MutableComponent createNarrationMessage() {
            return this.createEffectDescription(this.effect);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class BeaconUpgradePowerButton
    extends BeaconPowerButton {
        final /* synthetic */ BeaconScreen this$0;

        public BeaconUpgradePowerButton(BeaconScreen beaconScreen, int x, int y, Holder<MobEffect> effect) {
            BeaconScreen beaconScreen2 = beaconScreen;
            Objects.requireNonNull(beaconScreen2);
            this.this$0 = beaconScreen2;
            super(beaconScreen, x, y, effect, false, 3);
        }

        @Override
        protected MutableComponent createEffectDescription(Holder<MobEffect> effect) {
            return Component.translatable(effect.value().getDescriptionId()).append(" II");
        }

        @Override
        public void updateStatus(int levels) {
            if (this.this$0.primary != null) {
                this.visible = true;
                this.setEffect(this.this$0.primary);
                super.updateStatus(levels);
            } else {
                this.visible = false;
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class BeaconConfirmButton
    extends BeaconSpriteScreenButton {
        final /* synthetic */ BeaconScreen this$0;

        public BeaconConfirmButton(BeaconScreen beaconScreen, int x, int y) {
            BeaconScreen beaconScreen2 = beaconScreen;
            Objects.requireNonNull(beaconScreen2);
            this.this$0 = beaconScreen2;
            super(x, y, CONFIRM_SPRITE, CommonComponents.GUI_DONE);
        }

        @Override
        public void onPress(InputWithModifiers input) {
            this.this$0.minecraft.getConnection().send(new ServerboundSetBeaconPacket(Optional.ofNullable(this.this$0.primary), Optional.ofNullable(this.this$0.secondary)));
            ((BeaconScreen)this.this$0).minecraft.player.closeContainer();
        }

        @Override
        public void updateStatus(int levels) {
            this.active = ((BeaconMenu)this.this$0.menu).hasPayment() && this.this$0.primary != null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class BeaconCancelButton
    extends BeaconSpriteScreenButton {
        final /* synthetic */ BeaconScreen this$0;

        public BeaconCancelButton(BeaconScreen beaconScreen, int x, int y) {
            BeaconScreen beaconScreen2 = beaconScreen;
            Objects.requireNonNull(beaconScreen2);
            this.this$0 = beaconScreen2;
            super(x, y, CANCEL_SPRITE, CommonComponents.GUI_CANCEL);
        }

        @Override
        public void onPress(InputWithModifiers input) {
            ((BeaconScreen)this.this$0).minecraft.player.closeContainer();
        }

        @Override
        public void updateStatus(int levels) {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static abstract class BeaconSpriteScreenButton
    extends BeaconScreenButton {
        private final Identifier sprite;

        protected BeaconSpriteScreenButton(int x, int y, Identifier sprite, Component label) {
            super(x, y, label);
            this.setTooltip(Tooltip.create(label));
            this.sprite = sprite;
        }

        @Override
        protected void extractIcon(GuiGraphicsExtractor graphics) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX() + 2, this.getY() + 2, 18, 18);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static abstract class BeaconScreenButton
    extends AbstractButton
    implements BeaconButton {
        private boolean selected;

        protected BeaconScreenButton(int x, int y) {
            super(x, y, 22, 22, CommonComponents.EMPTY);
        }

        protected BeaconScreenButton(int x, int y, Component component) {
            super(x, y, 22, 22, component);
        }

        @Override
        public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier sprite = !this.active ? BUTTON_DISABLED_SPRITE : (this.selected ? BUTTON_SELECTED_SPRITE : (this.isHoveredOrFocused() ? BUTTON_HIGHLIGHTED_SPRITE : BUTTON_SPRITE));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height);
            this.extractIcon(graphics);
        }

        protected abstract void extractIcon(GuiGraphicsExtractor var1);

        public boolean isSelected() {
            return this.selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}

