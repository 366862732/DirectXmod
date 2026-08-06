/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.ArmorSlot;

public class NautilusInventoryMenu
extends AbstractMountInventoryMenu {
    private static final Identifier SADDLE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
    private static final Identifier ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/nautilus_armor_inventory");

    public NautilusInventoryMenu(int containerId, Inventory playerInventory, Container nautilusInventory, final AbstractNautilus nautilus, int inventoryColumns) {
        super(containerId, playerInventory, nautilusInventory, nautilus);
        Container saddleContainer = nautilus.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
        this.addSlot(new ArmorSlot(this, saddleContainer, nautilus, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE){
            {
                Objects.requireNonNull(this$0);
                super(inventory, owner, slot, slotIndex, x, y, emptyIcon);
            }

            @Override
            public boolean isActive() {
                return nautilus.canUseSlot(EquipmentSlot.SADDLE);
            }
        });
        Container armorContainer = nautilus.createEquipmentSlotContainer(EquipmentSlot.BODY);
        this.addSlot(new ArmorSlot(this, armorContainer, nautilus, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SLOT_SPRITE){
            {
                Objects.requireNonNull(this$0);
                super(inventory, owner, slot, slotIndex, x, y, emptyIcon);
            }

            @Override
            public boolean isActive() {
                return nautilus.canUseSlot(EquipmentSlot.BODY);
            }
        });
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    protected boolean hasInventoryChanged(Container container) {
        return ((AbstractNautilus)this.mount).hasInventoryChanged(container);
    }
}

