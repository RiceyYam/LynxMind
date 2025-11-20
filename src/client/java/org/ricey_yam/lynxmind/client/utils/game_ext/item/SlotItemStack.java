package org.ricey_yam.lynxmind.client.utils.game_ext.item;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlot;

@Getter
@Setter
public class SlotItemStack {
    private ItemStackLite item_stack;
    private LSlot l_slot;
    public SlotItemStack(LSlot l_slot, ItemStack itemStack) {
        this.l_slot = l_slot;
        this.item_stack = new ItemStackLite(itemStack.copy());
    }
    public SlotItemStack(LSlot l_slot, ItemStackLite itemStackLite) {
        this.l_slot = l_slot;
        this.item_stack = itemStackLite;
    }
}
