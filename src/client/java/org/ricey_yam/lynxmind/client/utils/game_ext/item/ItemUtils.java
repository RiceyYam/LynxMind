package org.ricey_yam.lynxmind.client.utils.game_ext.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.ricey_yam.lynxmind.client.ai.message.game_info.ui.SlotItemStack;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlotType;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.SlotHelper;

import java.util.*;
import java.util.stream.Collectors;

public class ItemUtils {
    /// 获取物品ID
    public static String getItemName(ItemStack itemStack) {
        Item item = itemStack.getItem();
        Identifier itemIdentifier = Registries.ITEM.getId(item);
        return itemIdentifier.toString();
    }

    /// 获取玩家背包全部物品
    public static List<SlotItemStack> getClientPlayerInventoryItems(LSlotType part, ComplexContainerType complexContainerType){
        var result = new ArrayList<SlotItemStack>();
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            List<ItemStack> sectionOfInventory = new ArrayList<>();
            var inventory = player.getInventory();
            var start = 0;
            var end = 0;
            switch (part){
                case INVENTORY_INNER:
                    sectionOfInventory.addAll(inventory.main);
                    start = 9;
                    end = 36;
                    break;
                case INVENTORY_HOTBAR:
                    sectionOfInventory.addAll(inventory.main);
                    end = 9;
                    break;
                case INVENTORY_EQUIPMENT:
                    sectionOfInventory.addAll(inventory.armor);
                    sectionOfInventory.addAll(inventory.offHand);
                    end = 4;
                    break;
            }
            if(!sectionOfInventory.isEmpty()) {
                var j = 0;
                for (int i = 0; i < end - start; i++) {
                    var itemStack = sectionOfInventory.get(start + i);
                    var inventoryInnerLSlot = SlotHelper.getLSlotInstanceByType(i,complexContainerType,part);
                    var slotItemStack = new SlotItemStack(inventoryInnerLSlot, itemStack);
                    result.add(slotItemStack);
                }
            }
        }
        return result;
    }
    public static DefaultedList<ItemStack> getClientPlayerInventoryItems(){
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            return player.getInventory().main;
        }
        else return null;
    }

    public static boolean hasItem(String itemId){
        var items = getClientPlayerInventoryItems();
        if (items != null) {
            for(var item : items){
                if(ItemUtils.getItemName(item).equals(itemId)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 标签工具类
     */
    public static class ItemTagHelper {

        private static final List<String> COMMON_ITEM_TAGS = Arrays.asList(
                // 基础方块
                "minecraft:dirt",
                "minecraft:grass_block",
                "minecraft:stone",
                "minecraft:cobblestone",
                "minecraft:oak_planks",
                "minecraft:spruce_planks",
                // 矿石
                "minecraft:diamond_ore",
                "minecraft:deepslate_diamond_ore",
                "minecraft:iron_ore",
                "minecraft:deepslate_iron_ore",
                // 其他常用物品
                "minecraft:coal",
                "minecraft:redstone",
                "minecraft:lapis_lazuli",
                "minecraft:emerald",
                "minecraft:obsidian"
        );

        /**
         * 精确匹配：关键词是否等于物品标签的“物品名”部分（忽略大小写）
         * 例：dirt → minecraft:dirt（匹配）；dirt → minecraft:grass_block（不匹配）
         * @param keyword 输入关键词（如 dirt、Dirt）
         * @param fullTag 完整物品标签（如 minecraft:dirt）
         * @return true：匹配；false：不匹配
         */
        public static boolean isExactMatch(String keyword, String fullTag) {
            if (keyword == null || fullTag == null || keyword.trim().isEmpty()) {
                return false;
            }
            // 提取完整标签的“物品名”部分（如 minecraft:dirt → dirt）
            String itemName = fullTag.contains(":") ? fullTag.split(":")[1] : fullTag;
            // 忽略大小写匹配
            return itemName.equalsIgnoreCase(keyword.trim());
        }

        /**
         * 模糊匹配：关键词是否包含在物品标签中（忽略大小写）
         * 例：dir → minecraft:dirt（匹配）；dia → minecraft:diamond_ore（匹配）
         * @param keyword 输入关键词（如 dir、Dia）
         * @param fullTag 完整物品标签（如 minecraft:dirt）
         * @return true：匹配；false：不匹配
         */
        public static boolean isFuzzyMatch(String keyword, String fullTag) {
            if (keyword == null || fullTag == null || keyword.trim().isEmpty()) {
                return false;
            }
            if(keyword.equals(fullTag)) return true;
            String lowerKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            String lowerFullTag = fullTag.toLowerCase(Locale.ROOT);
            return lowerFullTag.contains(lowerKeyword);
        }

        /**
         * 根据关键词获取所有匹配的完整标签（默认精确匹配）
         * @param keyword 输入关键词（如 dirt）
         * @return 匹配的完整标签列表（如 [minecraft:dirt]）
         */
        public static List<String> getMatchedTags(String keyword) {
            return getMatchedTags(keyword, true);
        }

        /**
         * 根据关键词获取所有匹配的完整标签（可选择精确/模糊匹配）
         * @param keyword 输入关键词
         * @param isExact 是否精确匹配
         * @return 匹配的完整标签列表
         */
        public static List<String> getMatchedTags(String keyword, boolean isExact) {
            if (keyword == null || keyword.trim().isEmpty()) {
                return List.of();
            }
            return COMMON_ITEM_TAGS.stream()
                    .filter(tag -> isExact ? isExactMatch(keyword, tag) : isFuzzyMatch(keyword, tag))
                    .collect(Collectors.toList());
        }

        /**
         * 动态添加新的物品标签到库中
         * @param newTag 新的完整物品标签
         */
        public static void addItemTag(String newTag) {
            if (newTag != null && !newTag.trim().isEmpty() && !COMMON_ITEM_TAGS.contains(newTag)) {
                COMMON_ITEM_TAGS.add(newTag);
            }
        }
    }
}
