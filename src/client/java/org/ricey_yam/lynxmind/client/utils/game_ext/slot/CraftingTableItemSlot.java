package org.ricey_yam.lynxmind.client.utils.game_ext.slot;

import net.minecraft.screen.slot.Slot;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;


/// LSlot ID Range: 0-9 (0:输入 1-9:输出)
public class CraftingTableItemSlot extends LSlot{
    public CraftingTableItemSlot(){
        super();
        this.slotType = LSlotType.CRAFTING_TABLE;
    }

    public CraftingTableItemSlot(int id, ComplexContainerType complexContainerType) {
        super(id,complexContainerType);
        this.slotType = LSlotType.CRAFTING_TABLE;
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
