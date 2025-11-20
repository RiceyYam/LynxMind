package org.ricey_yam.lynxmind.client.utils.game_ext.slot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.ricey_yam.lynxmind.client.utils.game_ext.ClientUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.PlayerUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;

/// 格子工具类
public class SlotHelper {
    /// 点击容器格子
    public static boolean clickContainerSlot(LSlot l_slot, int button, SlotActionType actionType) {
        var player = MinecraftClient.getInstance().player;
        if(player == null) return false;
        var syncId = player.currentScreenHandler.syncId;
        try{
            var slot = l_slot.toSlot();
            if(slot != null){
                ClientUtils.getController().clickSlot(syncId,slot.id,button,actionType,player);
                return true;
            }
            System.out.println("点击失败，未找到Slot！");
            return false;
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("点击容器物品时出现错误：" + e.getMessage());
            return false;
        }
    }

    /// 获取当前容器格子
    public static Slot getSlot(int slotId){
        var player = MinecraftClient.getInstance().player;
        if(player == null) return null;
        var screenHandler = player.currentScreenHandler;
        return screenHandler.getSlot(slotId);
    }

    /// 找到包含指定容器的LYNX SLOT
    public static LSlot getLSlotByItemID(String itemId,ComplexContainerType complexContainerType){
        var player = MinecraftClient.getInstance().player;
        if(player == null) return null;
        var screenHandler = player.currentScreenHandler;
        var playerItemsInner = PlayerUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_INNER,complexContainerType);
        var playerItemsHotBar = PlayerUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_HOTBAR,complexContainerType);
        playerItemsHotBar.addAll(playerItemsInner);
        for(var slotItem : playerItemsHotBar){
            if(slotItem.getItem_stack().getItem_name().equals(itemId)){
                return slotItem.getL_slot();
            }
        }
        return null;
    }

    /**
     * 获取某个槽位的物品
     * @param slotId 槽位 ID（从 0 开始）
     * @return 槽位中的 ItemStack，如果槽位不存在或无物品则返回空栈
     */
    public static ItemStack getSlotItem(int slotId) {
        if (slotId < 0) {
            return ItemStack.EMPTY;
        }
        var slot = getSlot(slotId);
        if (slot != null) {
            return slot.getStack().copy();
        }
        return ItemStack.EMPTY;
    }

    /**
     * 判断某个槽位是否为空
     */
    public static boolean isSlotEmpty(int slotId) {
        return getSlotItem(slotId).isEmpty();
    }

    public static LSlot getLSlotInstanceByType(int slotId, ComplexContainerType complexContainerType, LSlotType actionType){
        switch (actionType){
            case INVENTORY_HOTBAR -> {
                return new InventoryHotBarSlot(slotId,complexContainerType);
            }
            case INVENTORY_INNER ->  {
                return new InventoryInnerSlot(slotId,complexContainerType);
            }
            case INVENTORY_EQUIPMENT -> {
                return new InventoryEquipmentSlot(slotId,complexContainerType);
            }
        }
        return null;
    }

    /**
     * 切换到快捷栏中已有的指定物品
     * @param itemId 目标物品
     * @return 切换成功返回 true，物品不在快捷栏返回 false
     */
    public static boolean switchToHotbarItem(String itemId) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return false;
        for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
            var stack = player.getInventory().getStack(slotIndex);
            if (!stack.isEmpty() && ItemUtils.getItemID(stack).equals(itemId)) {
                player.getInventory().selectedSlot = slotIndex;
                return true;
            }
        }
        return false;
    }

    public static int getOffsetFromLSlotToSlot(ComplexContainerType complexContainerType){
        var result = 0;
        switch (complexContainerType){
            case PLAYER_INFO ->  result = 9;
            case CRAFTING_TABLE ->  result = 10;
            case CHEST ->  result = 27;
            case CHEST_BIG ->  result = 54;
            case FURNACE -> result = 3;
            case SMITHING_TABLE -> result = 4;
            case BREWING_STAND ->  result = 5;
        }
        return result;
    }

    public static int getQuickHotbarLSlotIDForTool(String toolId){
        if(toolId.contains("sword")) return 0;
        if(toolId.contains("pickaxe")) return 1;
        if(toolId.contains("axe")) return 2;
        if(toolId.contains("shovel")) return 3;
        if(toolId.contains("hoe") || toolId.contains("shears")) return 4;
        return 5;
    }
}