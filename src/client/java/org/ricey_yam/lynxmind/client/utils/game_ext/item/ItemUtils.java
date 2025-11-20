package org.ricey_yam.lynxmind.client.utils.game_ext.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemUtils {
    /// 获取物品ID
    public static String getItemID(ItemStack itemStack) {
        Item item = itemStack.getItem();
        Identifier itemIdentifier = Registries.ITEM.getId(item);
        return itemIdentifier.toString();
    }

}
