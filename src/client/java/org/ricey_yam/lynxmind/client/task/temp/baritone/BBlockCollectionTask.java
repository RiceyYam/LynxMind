package org.ricey_yam.lynxmind.client.task.temp.baritone;

import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.RotationUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerBaritoneTaskStop;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.temp.ui.UClickSlotTask;
import org.ricey_yam.lynxmind.client.task.temp.ui.UTask;
import org.ricey_yam.lynxmind.client.utils.game_ext.block.BlockUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.ClientUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.TransformUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.PlayerUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ContainerHelper;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemTagHelper;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.InventoryHotBarSlot;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlotType;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.SlotHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class BBlockCollectionTask extends BTask {
    private List<String> targetBlockIDList = new ArrayList<>();
    /// 当前目标方块位置
    private BlockPos currentTargetBlockPos;
    private List<BlockPos> blackList = new ArrayList<>();
    /// 发现的掉落物位置
    private ItemEntity currentLootTarget;
    /// 当前玩家世界
    private World currentWorld;
    /// 状态
    private CollectingState collectingState;
    /// 需要的物品列表
    private List<ItemStackLite> neededItem;
    /// 正在挖掘的方块ID
    private String miningBlockName;
    /// 正在挖掘的方块位置
    private BlockPos miningBlockPos;
    /// 重新寻找次数
    private int re_pathTicks;

    private String bestToolID;
    private boolean toolInHotbar;
    private List<UTask> switchingToolUTasks = new ArrayList<>();
    private UTask performingSwitchUTask;

    public enum CollectingState {
        MOVING_TO_LOOT,
        MOVING_TO_BLOCK,
        SWITCHING_TOOL,
        MINING_BLOCK,
    }

    public BBlockCollectionTask(List<ItemStackLite> neededItem, Action linkedAction) {
        super();
        this.weight = 1;
        this.taskType = BTaskType.BLOCK_COLLECTION;
        this.currentWorld = baritone.getPlayerContext().world();
        this.neededItem = neededItem;
        this.currentTaskState = TaskState.IDLE;
        this.collectingState = CollectingState.MOVING_TO_BLOCK;
        this.linkedAction = linkedAction;
    }

    @Override
    public void start() {
        if (neededItem.isEmpty() || currentWorld == null) {
            stop(currentWorld == null ? "玩家处于未知世界" : "需要收集的物品为空...");
            return;
        }

        nextBlock();
        blackList.clear();
        currentTaskState = TaskState.IDLE;
        transitionToMovingToBlock();
        updateBlockTargetList();
    }
    @Override
    public void tick() {
        try{
            var player = ClientUtils.getPlayer();

            /// 先检查任务状态是否正常
            if (currentTaskState != TaskState.IDLE) {
                stop("任务状态不对！");
                return;
            }

            /// 如果没有收集的东西 就完成任务
            if(neededItem.isEmpty()) {
                LynxMindClient.sendModMessage("收集任务已完成！");
                stop("收集任务已完成！");
                return;
            }

            /// 检查目标是否正常
            if(currentTargetBlockPos == null){
                nextBlock();
                return;
            }

            switch(collectingState) {
                /// 寻路到方块
                case MOVING_TO_BLOCK -> movingToBlockTick();

                /// 移动到掉落物
                case MOVING_TO_LOOT -> movingToLootTick();

                /// 切换到合适的工具
                case SWITCHING_TOOL -> switchingToToolTick();

                /// 挖掘方块
                case MINING_BLOCK -> miningBlockTick();
            }
        }
        catch (Exception e){
            System.out.println("执行收集任务时，出现错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void stop(String stopReason) {
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
        cancelMining();

        neededItem.clear();
        currentTargetBlockPos = null;
        miningBlockPos = null;
        miningBlockName = "";
        collectingState = CollectingState.MOVING_TO_BLOCK;
        currentTaskState = TaskState.FINISHED;

        /// 发送任务停止事件给AI
        sendBTaskStopMessage(stopReason);

        System.out.println("收集任务已停止：" + stopReason);
    }
    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
        cancelMining();
        ContainerHelper.closeContainer();
    }

    private void transitionToMovingToLoot(){
        collectingState = CollectingState.MOVING_TO_LOOT;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
    }
    private void transitionToMovingToBlock(){
        collectingState = CollectingState.MOVING_TO_BLOCK;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
    }
    private void transitionToSwitchingTool(){

    }
    private void transitionToMiningBlock(){
        collectingState = CollectingState.MINING_BLOCK;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
        switchToBestTool();
    }

    private void movingToLootTick(){
        if(currentLootTarget != null && currentLootTarget.getStack().getCount() > 0){
            var newGoal = new GoalBlock(currentLootTarget.getBlockPos());
            baritone.getCustomGoalProcess().setGoalAndPath(newGoal);
        }
        else{
            transitionToMovingToBlock();
        }
    }
    private void movingToBlockTick(){
        var player = getPlayer();
        /// 寻找是否有需要的物品的掉落物
        var nearestLootEntity = EntityUtils.findNearestEntity(player,ItemEntity.class,15);
        if(nearestLootEntity != null && nearestLootEntity.getStack().getCount() > 0) {
            var distanceToLoot = nearestLootEntity.distanceTo(player);
            var distanceToTargetBlock = TransformUtils.getDistance(player.getBlockPos(),currentTargetBlockPos);
            if(distanceToLoot < distanceToTargetBlock && nearestLootEntity.isOnGround()){
                cancelMining();
                var targetStack = nearestLootEntity.getStack();
                var isNeededItem = false;
                for (int i = neededItem.size() - 1; i >= 0; i--) {
                    var nItem = neededItem.get(i);
                    if (ItemTagHelper.isFuzzyMatch(nItem.getItem_name(),ItemUtils.getItemID(targetStack)) && nItem.getCount() >= 0) {
                        isNeededItem = true;
                        break;
                    }
                }
                if(isNeededItem) {
                    currentLootTarget = nearestLootEntity;
                    transitionToMovingToLoot();
                    return;
                }
                else {
                    currentLootTarget = null;
                }
            }
        }

        cancelMining();
        if (isPlayerReachBlock(currentTargetBlockPos) || isPlayerNearBlock(currentTargetBlockPos)) {
            transitionToMiningBlock();
        }
        else if(!baritone.getCustomGoalProcess().isActive()){
            if(re_pathTicks <= 100){
                baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(currentTargetBlockPos,2));
                re_pathTicks++;
            }
            else{
                blackList.add(currentTargetBlockPos);
                re_pathTicks = 0;
                currentTargetBlockPos = null;
            }
        }
    }
    private void switchingToToolTick(){
        if(holdingBestTool(currentTargetBlockPos)){
            transitionToMiningBlock();
            return;
        }
        cancelMining();
        if(ContainerHelper.isContainerOpen(InventoryScreen.class)) {
            if(switchingToolUTasks != null && !switchingToolUTasks.isEmpty()){
                switchingToolUTasks.removeIf(uTask -> uTask.getCurrentTaskState() != TaskState.IDLE);
                /// 若当前无操作，进行下一步操作
                if(performingSwitchUTask == null){
                    performingSwitchUTask = switchingToolUTasks.get(0);
                    LynxMindEndTickEventManager.registerTask(performingSwitchUTask);
                }
                /// 若当前有操作，判断操作结果，来决定是否下一步
                else if(performingSwitchUTask.getResult() != UTask.UTaskResult.NONE){
                    switch (performingSwitchUTask.getResult()) {
                        /// 成功：继续下一个操作
                        case SUCCESS -> performingSwitchUTask = null;
                    }
                }
            }
            else{
                transitionToMiningBlock();
            }
            /// 若没有操作 说明挖掘工具已移动完成
        }
        else {
            ContainerHelper.openContainer(InventoryScreen.class);
        }
    }
    private void miningBlockTick(){
        /// 判断方块状态是否正常
        var targetBlock = BlockUtils.getTargetBlock(currentTargetBlockPos);
        if(targetBlock == null || targetBlock.getDefaultState().isAir()) {
            transitionToMovingToBlock();
            currentTargetBlockPos = null;
            return;
        }

        if(baritone.getCustomGoalProcess().isActive()){
            baritone.getCustomGoalProcess().setGoal(null);
            baritone.getPathingBehavior().cancelEverything();
        }

        if (!isPlayerNearBlock(miningBlockPos)) {
            transitionToMovingToBlock();
        }

        if (currentTargetBlockPos != null) {
            mine(currentTargetBlockPos);
        }
    }

    /// 更新收集任务
    public void onItemCollected(String itemName,int count){
        if(neededItem.isEmpty()) return;
        for (int i = neededItem.size() - 1; i >= 0; i--) {
            var item = neededItem.get(i);
            if(item == null) continue;
            if (ItemTagHelper.isFuzzyMatch(item.getItem_name(),itemName) && item.getCount() >= 0) {
                item.setCount(item.getCount() - count);
                if (item.getCount() <= 0) {
                    neededItem.remove(i);
                }
                return;
            }
        }
        updateBlockTargetList();
    }

    /// 寻找下一个方块的位置
    private void nextBlock() {
        cancelMining();
        if(targetBlockIDList.isEmpty()){
            updateBlockTargetList();
        }
        currentTargetBlockPos = BlockUtils.findNearestBlock(baritone.getPlayerContext().player(), targetBlockIDList, 50,blackList);
    }

    private boolean isPlayerNearBlock(BlockPos pos) {
        if (pos == null) return false;
        var player = getPlayer();
        double distSq = player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return distSq <= 14;
    }

    private boolean isPlayerReachBlock(BlockPos pos) {
        if (pos == null) return false;
        double maxReach = baritone.getPlayerContext().playerController().getBlockReachDistance();
        return RotationUtils.reachable(baritone.getPlayerContext(), pos, maxReach, false).isPresent();
    }

    private boolean isBlockGone(BlockPos pos) {
        if (pos == null) return true;
        var state = BlockUtils.getBlockState(pos);
        return state == null || state.isAir();
    }

    private void updateBlockTargetList(){
        targetBlockIDList.clear();
        for(var item : neededItem){
            if(item == null) continue;
            var itemName = item.getItem_name();
            var matchedBlockList = ItemTagHelper.getTagList(itemName);
            if (matchedBlockList != null && !matchedBlockList.isEmpty()) {
                targetBlockIDList.addAll(matchedBlockList);
            }
        }
    }

    /// 很遗憾....Baritone原生的Mine有许多BUG 所以我自己写了个安全的挖掘方法 至少不会卡住
    private void mine(BlockPos targetPos) {
        try{
            if(ContainerHelper.isContainerOpen()){
                var client = MinecraftClient.getInstance();
                client.execute(this::cancelMining);
                return;
            }

            var options = ClientUtils.getOptions();
            if(options == null) {
                return;
            }

            var player = getPlayer();
            var targetBlock = BlockUtils.getTargetBlock(targetPos);
            if (targetBlock == null || baritone == null || player == null) {
                cancelMining();
                return;
            }

            if(!isBlockGone(targetPos)){
                if(!PlayerUtils.isLookingAt(targetPos)) {
                    var targetRotation = PlayerUtils.calcLookRotationFromVec3d(player,targetPos);
                    baritone.getLookBehavior().updateTarget(targetRotation,true);
                }

                options.attackKey.setPressed(true);

                /// 根据当前选中的方块切换到合适工具
                var holdItemStack = PlayerUtils.getHoldingItemStack();
                var selectedBlock = PlayerUtils.getSelectedBlock();
                if(selectedBlock != null){
                    miningBlockName = PlayerUtils.getSelectedBlockID();
                    miningBlockPos = PlayerUtils.getSelectedBlockPos();
                    var selectedBlockState = selectedBlock.getDefaultState();
                    if(selectedBlockState != null && !holdingBestTool(miningBlockPos) && options.attackKey.isPressed()){
                        switchToBestTool(miningBlockPos);
                    }
                }
            }
            else {
                currentTargetBlockPos = null;
                this.collectingState = CollectingState.MOVING_TO_BLOCK;
                cancelMining();
            }
        }
        catch (Exception e) {
            System.out.println("Baritone挖掘方块出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cancelMining(){
        var options = ClientUtils.getOptions();
        if(options == null) {
            return;
        }
        miningBlockPos = null;
        miningBlockName = null;
        options.attackKey.setPressed(false);
    }

    private boolean hasSuitableTool(BlockPos targetPos){
        bestToolID = PlayerUtils.getBestToolIDInInventory(targetPos);
        return bestToolID != null && !bestToolID.isEmpty() && !bestToolID.contains("air");
    }

    /// 是否握着最佳武器
    private boolean holdingBestTool(BlockPos targetPos){
        if(!hasSuitableTool(targetPos)) return true;
        var holdingItemStack = PlayerUtils.getHoldingItemStack();
        return ItemUtils.getItemID(holdingItemStack).equals(bestToolID);
    }

    private void switchToBestTool(BlockPos targetPos) {
        if(targetPos == null) {
            return;
        }
        if(baritone.getCustomGoalProcess().isActive()) return;
        if(bestToolID == null || bestToolID.isEmpty()) bestToolID = PlayerUtils.getBestToolIDInInventory(targetPos);
        var toolSlot = SlotHelper.getLSlotByItemID(bestToolID, ComplexContainerType.PLAYER_INFO);
        if(toolSlot == null) {
            bestToolID = null;
            return;
        }
        toolInHotbar = toolSlot.getSlotType() == LSlotType.INVENTORY_HOTBAR;
        if(toolInHotbar){
            ContainerHelper.closeContainer();
            SlotHelper.switchToHotbarItem(bestToolID);
        }
        else{
            this.collectingState = CollectingState.SWITCHING_TOOL;
            switchingToolUTasks.clear();
            var toolQuickLSlotID = SlotHelper.getQuickHotbarLSlotIDForTool(bestToolID);
            var u1 = new UClickSlotTask(toolSlot,0, SlotActionType.PICKUP);
            var u2 =  new UClickSlotTask(new InventoryHotBarSlot(toolQuickLSlotID,ComplexContainerType.PLAYER_INFO),0, SlotActionType.PICKUP);
            var u3 = new UClickSlotTask(toolSlot,0, SlotActionType.PICKUP);
            switchingToolUTasks.add(u1);
            switchingToolUTasks.add(u2);
            switchingToolUTasks.add(u3);
        }
    }
    private void switchToBestTool(){
        switchToBestTool(currentTargetBlockPos);
    }
}