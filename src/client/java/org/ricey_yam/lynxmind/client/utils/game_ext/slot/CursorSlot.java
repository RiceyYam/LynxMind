package org.ricey_yam.lynxmind.client.utils.game_ext.slot;

import net.minecraft.screen.slot.Slot;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;

/// LSlot ID Range: -999
///
/// Slot ID Range: -999
public class CursorSlot extends LSlot {
    public static final int CURSOR_SLOT_INDEX = -999;
    public static final CursorSlot SLOT = new CursorSlot();

    public CursorSlot() {
        super(CURSOR_SLOT_INDEX,ComplexContainerType.NONE);
        this.slotType = LSlotType.CURSOR;
    }

    @Override
    public Slot toSlot() {
        return SlotHelper.getSlot(CURSOR_SLOT_INDEX);
    }

    @Override
    public LSlot toLSlot(Slot slot, ComplexContainerType complexContainerType) {
        return SLOT;
    }
}
