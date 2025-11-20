package org.ricey_yam.lynxmind.client.utils.game_ext.item;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;

@Getter
@Setter
public class ItemStackLite {
    private int count;
    private String item_name;

    public ItemStackLite(ItemStack itemStack){
        this.count = itemStack.getCount();
        this.item_name = ItemUtils.getItemID(itemStack);
    }
    public ItemStackLite(int count, String item_name){
        this.count = count;
        this.item_name = item_name;
    }
    public ItemStackLite copy(){
        return new ItemStackLite(count,item_name);
    }
}
