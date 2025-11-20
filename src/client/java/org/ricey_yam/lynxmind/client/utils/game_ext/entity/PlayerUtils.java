package org.ricey_yam.lynxmind.client.utils.game_ext.entity;

import baritone.api.utils.Rotation;
import baritone.api.utils.VecUtils;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.SlotItemStack;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.utils.game_ext.block.BlockUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.ClientUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.TransformUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlotType;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.SlotHelper;

import java.util.ArrayList;
import java.util.List;

public class PlayerUtils {
    /// 是否玩家看向某个位置
    public static boolean isLookingAt(BlockPos pos) {
        var baritone = BaritoneManager.getClientBaritone();
        if (baritone == null) return false;

        var ctx = baritone.getPlayerContext();

        var player = ctx.player();

        Vec3d eyePosition = player.getEyePos();

        Vec3d targetCenter = VecUtils.getBlockPosCenter(pos);

        Rotation idealRotation = TransformUtils.getRotation(eyePosition, targetCenter);

        Rotation currentRotation = ctx.playerRotations();

        double yawDiff = Math.abs(TransformUtils.normalizeYaw180(idealRotation.getYaw() - currentRotation.getYaw()));
        double pitchDiff = Math.abs(idealRotation.getPitch() - currentRotation.getPitch());

        double totalDiff = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        return totalDiff < 4D;
    }
    /// 获取玩家选中的方块ID
    public static BlockHitResult getBlockHit(){
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return null;
        }
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        return blockHit;
    }
    public static String getSelectedBlockID() {
        var selectedBlock = getSelectedBlock();
        var id = BlockUtils.getBlockID(selectedBlock);
        return id;
    }
    public static BlockPos getSelectedBlockPos() {
        var blockHit = getBlockHit();
        if (blockHit != null) {
            return blockHit.getBlockPos();
        }
        else return null;
    }
    public static Block getSelectedBlock() {
        var client = MinecraftClient.getInstance();
        var blockHit = getBlockHit();
        if (client.world != null) {
            if (blockHit != null) {
                return client.world.getBlockState(blockHit.getBlockPos()).getBlock();
            }
        }
        return null;
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

    /// 计算旋转角度
    public static Rotation calcLookRotationFromVec3d(PlayerEntity player, BlockPos to) {
        var vec3dForm = player.getEyePos();
        var vec3dTo = VecUtils.getBlockPosCenter(to);
        return TransformUtils.getRotation(vec3dForm, vec3dTo);
    }

    /// 玩家是否有某个物品
    public static boolean hasItem(String itemId){
        var items = getClientPlayerInventoryItems();
        if (items != null) {
            for(var item : items){
                if(ItemUtils.getItemID(item).equals(itemId)){
                    return true;
                }
            }
        }
        return false;
    }

    /// 玩家手持物品信息
    public static ItemStack getHoldingItemStack(){
        var player = ClientUtils.getPlayer();
        if(player == null) return null;
        if(player.getMainHandStack().isEmpty()) return null;
        return player.getMainHandStack();
    }
    public static SlotItemStack getHoldingSlotItemStack(){
        var stack = getHoldingItemStack();
        if (stack != null) {
            var stackLite = new ItemStackLite(stack);
            var l_slot = SlotHelper.getLSlotByItemID(stackLite.getItem_name(),ComplexContainerType.PLAYER_INFO);
            return new SlotItemStack(l_slot, stackLite);
        }
        return null;
    }

    /// 根据要挖掘的方块获取最佳工具
    public static String getBestToolIDInInventory(BlockPos toBreak){
        if(toBreak == null) return null;
        var blockState = BlockUtils.getBlockState(toBreak);
        if(blockState == null) return null;
        var items = getClientPlayerInventoryItems();
        var maxMiningSpeed = -999f;
        ItemStack bestTool = null;
        if (items != null) {
            for(var item : items){
                if(item.isSuitableFor(blockState)){
                    var miningSpeed = item.getMiningSpeedMultiplier(blockState);
                    if(miningSpeed > maxMiningSpeed){
                        maxMiningSpeed = miningSpeed;
                        bestTool =  item;
                    }
                }
            }
            return bestTool != null ? ItemUtils.getItemID(bestTool) : null;
        }
        else return null;
    }

    /// 获取玩家背包最好的武器
    public static String getBestWeaponIDInInventory(){
        var items = getClientPlayerInventoryItems();
        var maxMiningDamage = -999f;
        ItemStack bestWeapon = null;
        if (items != null) {
            for(var item : items){
                if(item.getDamage() > maxMiningDamage) {
                    bestWeapon = item;
                    maxMiningDamage = item.getDamage();
                }
            }
            return bestWeapon != null ? ItemUtils.getItemID(bestWeapon) : null;
        }
        else return null;
    }
}
