package org.ricey_yam.lynxmind.client.utils.game_ext.slot;

import net.minecraft.screen.slot.Slot;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;

/// LSlot ID Range: 0-4 (0-3:护甲 4:副手)
public class InventoryEquipmentSlot extends LSlot {
    public InventoryEquipmentSlot(){
        super();
        this.slotType = LSlotType.INVENTORY_EQUIPMENT;
    }
    public InventoryEquipmentSlot(int id, ComplexContainerType complexContainerType) {
        super(id,complexContainerType);
        this.slotType = LSlotType.INVENTORY_EQUIPMENT;
    }
    @Override
    public Slot toSlot() {
        var actualId = id == 4 ? 45 : id + 5;
        return SlotHelper.getSlot(actualId);
    }

    @Override
    public LSlot toLSlot(Slot slot,ComplexContainerType complexContainerType) {
        this.complexContainerType = complexContainerType;
        var actualId = slot.id == 45 ? 4 : slot.id - 5;
        this.id = slot.id - 5;
        return this;
    }
}
