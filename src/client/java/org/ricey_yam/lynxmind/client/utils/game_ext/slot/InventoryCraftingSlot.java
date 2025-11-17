package org.ricey_yam.lynxmind.client.utils.game_ext.slot;

import net.minecraft.screen.slot.Slot;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;

/// LSlot ID Range: 0-4 (0:输入 1-4:输出)
public class InventoryCraftingSlot extends LSlot {
    public InventoryCraftingSlot(){
        super();
        this.slotType = LSlotType.INVENTORY_CRAFTING;
    }
    public InventoryCraftingSlot(int id, ComplexContainerType complexContainerType) {
        super(id,complexContainerType);
        this.slotType = LSlotType.INVENTORY_CRAFTING;
    }
    @Override
    public Slot toSlot() {
        return SlotHelper.getSlot(id);
    }

    @Override
    public LSlot toLSlot(Slot slot,ComplexContainerType complexContainerType) {
        this.complexContainerType = complexContainerType;
        this.id = slot.id;
        return this;
    }
}