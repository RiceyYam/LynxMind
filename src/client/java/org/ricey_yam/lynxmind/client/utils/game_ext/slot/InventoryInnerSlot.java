package org.ricey_yam.lynxmind.client.utils.game_ext.slot;

import net.minecraft.screen.slot.Slot;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;

/// LSlot ID Range: 0-26
///
/// Slot ID Range: 10-36(任意容器)
public class InventoryInnerSlot extends LSlot {
    public InventoryInnerSlot(){
        super();
        this.slotType = LSlotType.INVENTORY_INNER;
    }
    public InventoryInnerSlot(int id, ComplexContainerType complexContainerType) {
        super(id,complexContainerType);
        this.slotType = LSlotType.INVENTORY_INNER;
    }
    @Override
    public Slot toSlot() {
        return SlotHelper.getSlot(id + SlotHelper.getOffsetFromLSlotToSlot(complexContainerType));
    }

    @Override
    public LSlot toLSlot(Slot slot,ComplexContainerType complexContainerType) {
        this.complexContainerType = complexContainerType;
        this.id = slot.id - SlotHelper.getOffsetFromLSlotToSlot(complexContainerType);
        return this;
    }
}
