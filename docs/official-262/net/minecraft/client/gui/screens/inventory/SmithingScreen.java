/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.gui.screens.inventory;

import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.Equippable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class SmithingScreen
extends ItemCombinerScreen<SmithingMenu> {
    private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/smithing/error");
    private static final Identifier EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM = Identifier.withDefaultNamespace("container/slot/smithing_template_armor_trim");
    private static final Identifier EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE = Identifier.withDefaultNamespace("container/slot/smithing_template_netherite_upgrade");
    private static final Component MISSING_TEMPLATE_TOOLTIP = Component.translatable("container.upgrade.missing_template_tooltip");
    private static final Component ERROR_TOOLTIP = Component.translatable("container.upgrade.error_tooltip");
    private static final List<Identifier> EMPTY_SLOT_SMITHING_TEMPLATES = List.of(EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM, EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE);
    private static final int TITLE_LABEL_X = 44;
    private static final int TITLE_LABEL_Y = 15;
    private static final int ERROR_ICON_WIDTH = 28;
    private static final int ERROR_ICON_HEIGHT = 21;
    private static final int ERROR_ICON_X = 65;
    private static final int ERROR_ICON_Y = 46;
    private static final int TOOLTIP_WIDTH = 115;
    private static final int ARMOR_STAND_Y_ROT = 210;
    private static final int ARMOR_STAND_X_ROT = 25;
    private static final Vector3fc ARMOR_STAND_TRANSLATION = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Quaternionfc ARMOR_STAND_ANGLE = new Quaternionf().rotationXYZ(0.43633232f, 0.0f, (float)Math.PI);
    private static final int ARMOR_STAND_SCALE = 25;
    private static final int ARMOR_STAND_LEFT = 121;
    private static final int ARMOR_STAND_TOP = 20;
    private static final int ARMOR_STAND_RIGHT = 161;
    private static final int ARMOR_STAND_BOTTOM = 80;
    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground additionalIcon = new CyclingSlotBackground(2);
    private final ArmorStandRenderState armorStandPreview = new ArmorStandRenderState();

    public SmithingScreen(SmithingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, Identifier.withDefaultNamespace("textures/gui/container/smithing.png"));
        this.titleLabelX = 44;
        this.titleLabelY = 15;
        this.armorStandPreview.entityType = EntityTypes.ARMOR_STAND;
        this.armorStandPreview.showBasePlate = false;
        this.armorStandPreview.showArms = true;
        this.armorStandPreview.xRot = 25.0f;
        this.armorStandPreview.bodyRot = 210.0f;
    }

    @Override
    protected void subInit() {
        this.updateArmorStandPreview(((SmithingMenu)this.menu).getSlot(3).getItem());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        Optional<SmithingTemplateItem> template = this.getTemplateItem();
        this.templateIcon.tick(EMPTY_SLOT_SMITHING_TEMPLATES);
        this.baseIcon.tick(template.map(SmithingTemplateItem::getBaseSlotEmptyIcons).orElse(List.of()));
        this.additionalIcon.tick(template.map(SmithingTemplateItem::getAdditionalSlotEmptyIcons).orElse(List.of()));
    }

    private Optional<SmithingTemplateItem> getTemplateItem() {
        Item item;
        ItemStack templateSlotItem = ((SmithingMenu)this.menu).getSlot(0).getItem();
        if (!templateSlotItem.isEmpty() && (item = templateSlotItem.getItem()) instanceof SmithingTemplateItem) {
            SmithingTemplateItem templateItem = (SmithingTemplateItem)item;
            return Optional.of(templateItem);
        }
        return Optional.empty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractOnboardingTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        this.templateIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.baseIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.additionalIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        int x0 = this.leftPos + 121;
        int y0 = this.topPos + 20;
        int x1 = this.leftPos + 161;
        int y1 = this.topPos + 80;
        graphics.entity(this.armorStandPreview, 25.0f, ARMOR_STAND_TRANSLATION, ARMOR_STAND_ANGLE, null, x0, y0, x1, y1);
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack itemStack) {
        if (slotIndex == 3) {
            this.updateArmorStandPreview(itemStack);
        }
    }

    private void updateArmorStandPreview(ItemStack itemStack) {
        this.armorStandPreview.leftHandItemStack = ItemStack.EMPTY;
        this.armorStandPreview.leftHandItemState.clear();
        this.armorStandPreview.headEquipment = ItemStack.EMPTY;
        this.armorStandPreview.headItem.clear();
        this.armorStandPreview.chestEquipment = ItemStack.EMPTY;
        this.armorStandPreview.legsEquipment = ItemStack.EMPTY;
        this.armorStandPreview.feetEquipment = ItemStack.EMPTY;
        if (!itemStack.isEmpty()) {
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            EquipmentSlot slot = equippable != null ? equippable.slot() : null;
            ItemModelResolver itemModelResolver = this.minecraft.getItemModelResolver();
            EquipmentSlot equipmentSlot = slot;
            int n = 0;
            switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"HEAD", "CHEST", "LEGS", "FEET"}, (EquipmentSlot)equipmentSlot, n)) {
                case 0: {
                    if (HumanoidArmorLayer.shouldRender(itemStack, EquipmentSlot.HEAD)) {
                        this.armorStandPreview.headEquipment = itemStack.copy();
                        break;
                    }
                    itemModelResolver.updateForTopItem(this.armorStandPreview.headItem, itemStack, ItemDisplayContext.HEAD, null, null, 0);
                    break;
                }
                case 1: {
                    this.armorStandPreview.chestEquipment = itemStack.copy();
                    break;
                }
                case 2: {
                    this.armorStandPreview.legsEquipment = itemStack.copy();
                    break;
                }
                case 3: {
                    this.armorStandPreview.feetEquipment = itemStack.copy();
                    break;
                }
                default: {
                    this.armorStandPreview.leftHandItemStack = itemStack.copy();
                    itemModelResolver.updateForTopItem(this.armorStandPreview.leftHandItemState, itemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, null, null, 0);
                }
            }
        }
    }

    @Override
    protected void extractErrorIcon(GuiGraphicsExtractor graphics, int xo, int yo) {
        if (this.hasRecipeError()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, xo + 65, yo + 46, 28, 21);
        }
    }

    private void extractOnboardingTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Optional<Component> tooltip = Optional.empty();
        if (this.hasRecipeError() && this.isHovering(65, 46, 28, 21, mouseX, mouseY)) {
            tooltip = Optional.of(ERROR_TOOLTIP);
        }
        if (this.hoveredSlot != null) {
            ItemStack template = ((SmithingMenu)this.menu).getSlot(0).getItem();
            ItemStack hoveredStack = this.hoveredSlot.getItem();
            if (template.isEmpty()) {
                if (this.hoveredSlot.index == 0) {
                    tooltip = Optional.of(MISSING_TEMPLATE_TOOLTIP);
                }
            } else {
                Item item = template.getItem();
                if (item instanceof SmithingTemplateItem) {
                    SmithingTemplateItem templateItem = (SmithingTemplateItem)item;
                    if (hoveredStack.isEmpty()) {
                        if (this.hoveredSlot.index == 1) {
                            tooltip = Optional.of(templateItem.getBaseSlotDescription());
                        } else if (this.hoveredSlot.index == 2) {
                            tooltip = Optional.of(templateItem.getAdditionSlotDescription());
                        }
                    }
                }
            }
        }
        tooltip.ifPresent(component -> graphics.setTooltipForNextFrame(this.font, this.font.split((FormattedText)component, 115), mouseX, mouseY));
    }

    private boolean hasRecipeError() {
        return ((SmithingMenu)this.menu).hasRecipeError();
    }
}

