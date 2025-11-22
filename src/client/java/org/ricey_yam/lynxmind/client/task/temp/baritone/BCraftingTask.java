package org.ricey_yam.lynxmind.client.task.temp.baritone;

import baritone.api.pathing.goals.GoalBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.ai.message.action.sub.PlayerCraftingAction;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerBaritoneTaskStop;
import org.ricey_yam.lynxmind.client.task.temp.ui.UClickSlotTask;
import org.ricey_yam.lynxmind.client.task.temp.ui.UTask;
import org.ricey_yam.lynxmind.client.utils.game_ext.block.BlockUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.SlotItemStack;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.utils.game_ext.*;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.PlayerUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ContainerHelper;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.RecipeHelper;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Getter
@Setter
public class BCraftingTask extends BTask {
    public enum CraftingState {
        FINDING_CRAFTING_WAY,
        CRAFTING_ITEM,
        PUTTING_CRAFTING_TABLE,
        PATHING_TO_CRAFTING_TABLE
    }
    /// 工作台位置
    private BlockPos craftingTablePos;
    /// 工作台掉落物实体
    private ItemEntity craftingTableLootEntity;
    /// 需要制作的物品列表
    private List<ItemStackLite> to_craft;
    /// 制作任务状态
    private CraftingState craftingState;
    /// 当前的制作任务
    private CraftingSingleSubTask currentCraftingSingleSubTask;
    /// 当前的放置工作台任务
    private PuttingCraftingTableSubTask currentPuttingCraftingTableSubTask;
    /// 制作失败的物品
    private List<ItemStackLite> craft_failed;
    /// 制作成功的物品
    private List<ItemStackLite> craft_success;

    public BCraftingTask(List<ItemStackLite> to_craft, Action linkedAction) {
        super();
        this.weight = 1;
        this.craftingState = CraftingState.FINDING_CRAFTING_WAY;
        this.taskType = BTaskType.CRAFTING;
        this.to_craft = new ArrayList<>(to_craft);
        this.craft_failed = new ArrayList<>();
        this.craft_success = new ArrayList<>();
        this.linkedAction = linkedAction;
    }

    @Override
    public void start() {
        this.currentTaskState = TaskState.IDLE;
        if(to_craft.isEmpty()) {
            stop("制作列表为空，停止BTask");
        }
    }

    @Override
    public void tick(){
        tickTimer++;
        ClientUtils.getOptions().useKey.setPressed(false);
        var player = MinecraftClient.getInstance().player;
        if(player == null) return;
        if(to_craft.isEmpty()) {
            stop("完成制作！");
            return;
        }
        switch(this.craftingState) {
            /// 寻找制作方法
            case FINDING_CRAFTING_WAY -> findingCraftingWayTick();
            /// 寻找合适的工作台放置点
            case PUTTING_CRAFTING_TABLE -> puttingCraftingTableTick();
            /// 正在走向工作台
            case PATHING_TO_CRAFTING_TABLE -> pathingToCraftingTableTick();
            /// 制作阶段
            case CRAFTING_ITEM -> craftingItemTick();
        }
    }
    @Override
    public void stop(String stopReason) {
        currentTaskState = TaskState.FINISHED;
        currentCraftingSingleSubTask = null;
        craftingTablePos = null;
        to_craft.clear();
        ContainerHelper.closeContainer();
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();

        /// 发送任务停止事件给AI
        sendBTaskStopMessage(stopReason);

        System.out.println("制作任务已停止：" + stopReason);
    }
    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
        ContainerHelper.closeContainer();
    }

    @Override
    protected void sendBTaskStopMessage(String stopReason) {
        if(stopReason != null && !stopReason.isEmpty() && AIServiceManager.isServiceActive && AIServiceManager.isTaskActive() && linkedAction != null){
            if(linkedAction instanceof PlayerCraftingAction createAction){
                createAction.setCraft_failed(craft_failed);
                createAction.setCraft_success(craft_success);
            }
            var bTaskStopEvent = new PlayerBaritoneTaskStop(linkedAction,stopReason);
            var serialized = LynxJsonHandler.serialize(bTaskStopEvent);
            Objects.requireNonNull(AIServiceManager.sendAndReceiveReplyAsync(serialized)).whenComplete((reply, error) -> ChatManager.handleAIReply(reply));
        }
    }

    private void transitionToFindingCraftingWay(){
        this.craftingState = CraftingState.FINDING_CRAFTING_WAY;
    }
    private void transitionToPuttingCraftingTable(){
        this.craftingState = CraftingState.PUTTING_CRAFTING_TABLE;
    }
    private void transitionToPathingToCraftingTable(){
        craftingState = CraftingState.PATHING_TO_CRAFTING_TABLE;
    }
    private void transitionToCraftingItem(){
        this.craftingState = CraftingState.CRAFTING_ITEM;
    }

    private void findingCraftingWayTick(){
        /// 获取即将制作的物品 并制定计划 然后判定是否需要工作台
        currentCraftingSingleSubTask = new CraftingSingleSubTask(to_craft.get(0).getItem_name());
        /// 需要工作台，尝试寻找/制作工作台然后制作物品
        if(currentCraftingSingleSubTask.craftingTableNeeding()) {
            if(inSuitableCraftingUI()){
                transitionToCraftingItem();
                return;
            }
            /// 尝试寻找附近的工作台
            craftingTablePos = BlockUtils.findNearestBlock(getPlayer(), List.of("minecraft:crafting_table"),20);
            /// 没有工作台在20格内
            if(craftingTablePos == null){
                /// 如果有工作台掉落物目标，优先寻路捡起掉落物
                if(craftingTableLootEntity != null && craftingTableLootEntity.getStack().getCount() > 0){
                    var newGoal = new GoalBlock(craftingTableLootEntity.getBlockPos());
                    baritone.getCustomGoalProcess().setGoalAndPath(newGoal);
                    return;
                }
                else{
                    baritone.getCustomGoalProcess().setGoal(null);
                    baritone.getPathingBehavior().cancelEverything();
                }

                /// 寻找是否有需要的物品的掉落物
                var nearestLootEntity = EntityUtils.findNearestEntity(getPlayer(), ItemEntity.class,15);
                if(nearestLootEntity != null && nearestLootEntity.getStack().getCount() > 0) {
                    var targetStack = nearestLootEntity.getStack();
                    var isNeededItem = ItemUtils.getItemID(targetStack).equals("minecraft:crafting_table");
                    if(isNeededItem) {
                        craftingTableLootEntity = nearestLootEntity;
                        return;
                    }
                    else {
                        craftingTableLootEntity = null;
                    }
                }

                if(PlayerUtils.hasItem("minecraft:crafting_table")){
                    System.out.println("有工作台，正在放置工作台...");
                    transitionToPuttingCraftingTable();
                }
                else{
                    System.out.println("没有工作台，正在制作新工作台...");
                    to_craft.add(0,new ItemStackLite(1,"minecraft:crafting_table"));
                }
            }
            /// 找到工作台 寻路到那
            else{
                transitionToPathingToCraftingTable();
                var newGoal = new GoalBlock(craftingTablePos);
                baritone.getCustomGoalProcess().setGoalAndPath(newGoal);
            }
        }
        /// 无需工作台 打开背包制作物品
        else{
            this.craftingState = CraftingState.CRAFTING_ITEM;
            ContainerHelper.openContainer(InventoryScreen.class);
        }
    }
    private void puttingCraftingTableTick(){
        if(currentPuttingCraftingTableSubTask != null){
            if(currentPuttingCraftingTableSubTask.getState() == TaskState.IDLE){
                currentPuttingCraftingTableSubTask.puttingTick();
            }
        }
        else{
            System.out.println("当前子任务失效，重新创建。");
            currentPuttingCraftingTableSubTask = new PuttingCraftingTableSubTask(this);
        }
    }
    private void pathingToCraftingTableTick(){
        /// 没摸到工作台，继续等待
        if(!isCraftingTableInRange()){
            var goal = baritone.getPathingBehavior().getGoal();
            /// 若寻路任务意外丢失 重新创建
            if(goal == null){
                var newGoal = new GoalBlock(craftingTablePos);
                baritone.getCustomGoalProcess().setGoalAndPath(newGoal);
            }
        }
        /// 靠近工作台 尝试看向工作台
        else if(!PlayerUtils.isLookingAt(craftingTablePos)){
            baritone.getCustomGoalProcess().setGoal(null);
            baritone.getPathingBehavior().cancelEverything();
            var targetRotation = PlayerUtils.calcLookRotationFromVec3d(getPlayer(),craftingTablePos);
            baritone.getLookBehavior().updateTarget(targetRotation,true);
        }
        /// 看向工作台 点击工作台开始制作
        else{
            transitionToCraftingItem();
            if (!ContainerHelper.isContainerOpen()) {
                ContainerHelper.closeContainer();
            }
            ClientUtils.getOptions().useKey.setPressed(true);
        }
    }
    private void craftingItemTick(){
        if(currentCraftingSingleSubTask == null){
            System.out.println("当前子任务失效，重新创建。");
            transitionToFindingCraftingWay();
            return;
        }
        /// 判断是否在合成页面
        if(inSuitableCraftingUI()){
            /// 判断当前合成子任务状态
            if(currentCraftingSingleSubTask.getState() != TaskState.IDLE){
                if(currentCraftingSingleSubTask.getState() == TaskState.FAILED && currentCraftingSingleSubTask.crafting_item.equals("minecraft:crafting_table")){
                    stop("没有足够的木板制作工作台");
                    return;
                }
                if(currentCraftingSingleSubTask.getCrafting_item().equals("minecraft:crafting_table")){
                    transitionToPuttingCraftingTable();
                }
                else transitionToFindingCraftingWay();
                var currentCrafting = to_craft.get(0);
                if(currentCrafting.getCount() > 1){
                    currentCrafting.setCount(currentCrafting.getCount() - 1);
                }
                else to_craft.remove(0);
                currentCraftingSingleSubTask = null;

                ContainerHelper.closeContainer();
            }
            else {
                currentCraftingSingleSubTask.craftTick();
            }
        }
        else{
            System.out.println("不在制作界面!");
            ContainerHelper.closeContainer();
            if(currentCraftingSingleSubTask.craftingTableNeeding()) {
                transitionToFindingCraftingWay();
            }
            else{
                ContainerHelper.openContainer(InventoryScreen.class);
            }
        }
    }

    /// 是否能够到工作台
    private boolean isCraftingTableInRange(){
        var player = getPlayer();
        if(craftingTablePos == null) return false;
        return baritone.getPlayerContext().player().getPos().distanceTo(craftingTablePos.toCenterPos()) < 3D;
    }

    /// 是否处于合适的制作界面
    private boolean inSuitableCraftingUI(){
        if(currentCraftingSingleSubTask == null) {
            System.out.println("sub task is null!");
            return false;
        }
        if(currentCraftingSingleSubTask.craftingTableNeeding()) return ContainerHelper.isContainerOpen(CraftingScreen.class);
        else return ContainerHelper.isContainerOpen(InventoryScreen.class);
    }

    /// 更新制作结果
    private void updateCraftingResult(String itemId,boolean successful){
        var actualList = successful ? craft_success : craft_failed;
        for(var item : actualList){
            if(item.getItem_name().equals(itemId)){
                item.setCount(item.getCount() + 1);
                return;
            }
        }
        actualList.add(new ItemStackLite(1,itemId));
    }

    @Setter
    @Getter
    static class CraftingSingleSubTask {
        /// 子任务状态
        private TaskState state;
        /// 正在制作的物品ID
        private String crafting_item;
        /// 需要从背包取出的物品
        private List<SlotItemStack> takeout = new ArrayList<>();
        /// 需要放到工作台上的物品
        private List<SlotItemStack> placement = new ArrayList<>();
        private List<UTask> uTasks = new ArrayList<>();
        private UTask performingUTask;

        public CraftingSingleSubTask(String crafting_item) {
            this.state = TaskState.IDLE;
            this.crafting_item = crafting_item;
            var recipe = RecipeHelper.getRecipe(crafting_item);
            var playerItemsInner = PlayerUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_INNER,craftingTableNeeding() ? ComplexContainerType.CRAFTING_TABLE : ComplexContainerType.PLAYER_INFO);
            var playerItemsHotBar = PlayerUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_HOTBAR,craftingTableNeeding() ? ComplexContainerType.CRAFTING_TABLE : ComplexContainerType.PLAYER_INFO);
            playerItemsInner.addAll(playerItemsHotBar);
            var playerItems = playerItemsInner;
            if (recipe != null) {
                var ingredients = recipe.getIngredients();
                var targetSlotIndex = 0;

                /// 获取计划需取出的材料/取出位置/放置位置
                outer:
                for (int i = 0; i < ingredients.size(); i++) {
                    var ingredient = ingredients.get(i);
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    for (var matchTarget : ingredient.getMatchingStacks()) {
                        var matchedItemStackInInventory = getMatchedSlotItemStack(matchTarget, playerItems);
                        var hasMatchedTargetItemInInventory = matchedItemStackInInventory != null;
                        if (hasMatchedTargetItemInInventory) {
                            var itemStack = matchedItemStackInInventory.getItem_stack().copy();
                            itemStack.setCount(1);
                            var placingSlotItemStack = new SlotItemStack(craftingTableNeeding() ? new CraftingTableItemSlot(i + 1, ComplexContainerType.CRAFTING_TABLE) : new InventoryCraftingSlot(targetSlotIndex + 1,ComplexContainerType.PLAYER_INFO),itemStack);
                            takeout.add(matchedItemStackInInventory);
                            placement.add(placingSlotItemStack);
                            if (MinecraftClient.getInstance().world != null) {
                                matchedItemStackInInventory.getItem_stack().setCount(matchedItemStackInInventory.getItem_stack().getCount() - recipe.getResult(MinecraftClient.getInstance().world.getRegistryManager()).getCount());
                                if (matchedItemStackInInventory.getItem_stack().getCount() <= 0) {
                                    playerItems.remove(matchedItemStackInInventory);
                                }
                                targetSlotIndex++;
                                continue outer;
                            }
                        }
                    }
                    this.state = TaskState.FAILED;
                    System.out.println("材料不足！");
                    return;
                }

                /// 生成 -> 放置材料点击任务（UTask）
                if(takeout.size() != placement.size() || takeout.isEmpty()) {
                    uTasks = null;
                    this.state = TaskState.FAILED;
                    System.out.println("生成制作方案失败！");
                    return;
                }
                for (int i = 0; i < takeout.size(); i++) {
                    var TSlotItemStack = takeout.get(i);
                    var PSlotItemStack = placement.get(i);
                    var takeOutUTask = new UClickSlotTask(TSlotItemStack.getL_slot(),0, SlotActionType.PICKUP);
                    var placeUTask = new UClickSlotTask(PSlotItemStack.getL_slot(),1, SlotActionType.PICKUP);
                    var takeBackUTask = new UClickSlotTask(TSlotItemStack.getL_slot(),0, SlotActionType.PICKUP);
                    uTasks.add(takeOutUTask);
                    uTasks.add(placeUTask);
                    uTasks.add(takeBackUTask);
                }
                /// 生成 -> 拿出产物的点击任务 (UTask)
                var resultLSlot = craftingTableNeeding() ? new CraftingTableItemSlot(0,ComplexContainerType.CRAFTING_TABLE) : new InventoryCraftingSlot(0,ComplexContainerType.PLAYER_INFO);
                var takeOutResultItemUTask = new UClickSlotTask(resultLSlot,0,SlotActionType.QUICK_MOVE);
                uTasks.add(takeOutResultItemUTask);
            }
            else{
                this.state = TaskState.FAILED;
                System.out.println("无法找到该物品配方，跳过制作！");
            }
        }

        /// 交互逻辑
        public void craftTick(){
            if(state != TaskState.IDLE) return;

            if(uTasks != null && !uTasks.isEmpty()){
                uTasks.removeIf(uTask -> uTask.getCurrentTaskState() != TaskState.IDLE);
                /// 若当前无操作，进行下一步操作
                if(performingUTask == null){
                    performingUTask = uTasks.get(0);
                    LynxMindEndTickEventManager.registerTask(performingUTask);
                }
                /// 若当前有操作，判断操作结果，来决定是否下一步
                else if(performingUTask.getResult() != UTask.UTaskResult.NONE){
                    switch (performingUTask.getResult()) {
                        /// 失败：停止子任务
                        case FAILED -> this.state = TaskState.FAILED;
                        /// 成功：继续下一个操作
                        case SUCCESS -> performingUTask = null;
                    }
                }
            }
            /// 若没有操作 说明该物品制作完成
            else {
                this.state = TaskState.FINISHED;
            }
        }

        /// 是否需要工作台
        public boolean craftingTableNeeding(){
            return RecipeHelper.requiresCraftingTable(crafting_item);
        }

        /// 根据给出的物品，从玩家已获得的物品中输出匹配的物品
        private SlotItemStack getMatchedSlotItemStack(ItemStack targetItemStack, List<SlotItemStack> obtainedItem) {
            for(var item : obtainedItem) {
                if(item == null) continue;
                if(item.getItem_stack().getItem_name().equals(ItemUtils.getItemID(targetItemStack))) {
                    return item;
                }
            }
            return null;
        }
    }

    @Setter
    @Getter
    static class PuttingCraftingTableSubTask{
        private BCraftingTask parentTask;
        private TaskState state;
        private BlockPos cTPlacingPointPos;
        private int craftingTablePlacingPointSearchingRange;

        private List<UTask> moveCraftingTableUTask;
        private UTask performingUTask;

        private boolean isCraftingTableInHotbar = false;

        public PuttingCraftingTableSubTask(BCraftingTask parentTask){
            this.moveCraftingTableUTask = new ArrayList<>();
            this.performingUTask = null;
            this.state = TaskState.IDLE;
            this.parentTask = parentTask;
            this.craftingTablePlacingPointSearchingRange = 4;

            /// 是否有工作台，若没有则返回
            if(!PlayerUtils.hasItem("minecraft:crafting_table")){
                parentTask.setCraftingState(CraftingState.FINDING_CRAFTING_WAY);
                this.state = TaskState.FAILED;
                return;
            }

            /// 寻找工作台的格子
            var currentCraftingTableLSlot = SlotHelper.getLSlotByItemID("minecraft:crafting_table", ComplexContainerType.PLAYER_INFO);
            if(currentCraftingTableLSlot == null) {
                this.state = TaskState.FAILED;
                return;
            }
            /// 判断当前工作台是否处于快捷栏
            this.isCraftingTableInHotbar = currentCraftingTableLSlot.getSlotType() == LSlotType.INVENTORY_HOTBAR;
            /// 不是：创建打开背包把工作台移动到快捷栏的UTask
            if(!this.isCraftingTableInHotbar){
                var takeoutUTask = new UClickSlotTask(currentCraftingTableLSlot,0,SlotActionType.PICKUP);
                var putUTask = new UClickSlotTask(new InventoryHotBarSlot(8,ComplexContainerType.PLAYER_INFO),0,SlotActionType.PICKUP);
                var takeCursorItemBack = new UClickSlotTask(currentCraftingTableLSlot,0,SlotActionType.PICKUP);

                moveCraftingTableUTask.add(takeoutUTask);
                moveCraftingTableUTask.add(putUTask);
                moveCraftingTableUTask.add(takeCursorItemBack);
            }
        }

        public void puttingTick(){
            if(getParentTask().inSuitableCraftingUI()) {
                parentTask.setCraftingState(CraftingState.CRAFTING_ITEM);
                this.state = TaskState.FINISHED;
                return;
            }
            var craftingState = parentTask.getCraftingState();
            var craftingTablePos = parentTask.getCraftingTablePos();
            var player = ClientUtils.getPlayer();
            var baritone = parentTask.getBaritone();

            craftingTablePos = BlockUtils.findNearestBlock(player, List.of("minecraft:crafting_table"),20);
            if(craftingTablePos != null) {
                parentTask.setCraftingState(CraftingState.FINDING_CRAFTING_WAY);
                this.state = TaskState.FINISHED;
                return;
            }

            cTPlacingPointPos = BlockUtils.findCraftingTablePlacePoint(player,craftingTablePlacingPointSearchingRange);
            /// 打开背包拿取工作台
            if(cTPlacingPointPos != null) {
                /// 放置点太远，尝试寻路
                if(!isCTPlacingPointInRange()){
                    var newGoal = new GoalBlock(cTPlacingPointPos.getX(),cTPlacingPointPos.getY(),cTPlacingPointPos.getZ());
                    baritone.getCustomGoalProcess().setGoalAndPath(newGoal);
                }
                else{
                    baritone.getCustomGoalProcess().setGoal(null);
                    baritone.getPathingBehavior().cancelEverything();
                    /// 看向放置点
                    if(!PlayerUtils.isLookingAt(cTPlacingPointPos.down())){
                        System.out.println("正在看向放置点！");
                        var targetRotation = PlayerUtils.calcLookRotationFromVec3d(player,cTPlacingPointPos.down());
                        baritone.getLookBehavior().updateTarget(targetRotation,true);
                    }
                    else{
                        var currentCraftingTableLSlot = SlotHelper.getLSlotByItemID("minecraft:crafting_table", ComplexContainerType.PLAYER_INFO);
                        if(currentCraftingTableLSlot == null) {
                            System.out.println("CurrentCraftingTableLSlot is null!");
                            return;
                        }
                        this.isCraftingTableInHotbar = currentCraftingTableLSlot.getSlotType() == LSlotType.INVENTORY_HOTBAR;
                        /// 选中工作台然后放置
                        if(this.isCraftingTableInHotbar){
                            ContainerHelper.closeContainer();
                            if(SlotHelper.switchToHotbarItem("minecraft:crafting_table")){
                                ClientUtils.getOptions().useKey.setPressed(true);
                            }
                        }
                        /// 执行UTask拿出工作台
                        else{
                            if(ContainerHelper.isContainerOpen(InventoryScreen.class)) {
                                if(moveCraftingTableUTask != null && !moveCraftingTableUTask.isEmpty()){
                                    moveCraftingTableUTask.removeIf(uTask -> uTask.getCurrentTaskState() != TaskState.IDLE);
                                    /// 若当前无操作，进行下一步操作
                                    if(performingUTask == null){
                                        performingUTask = moveCraftingTableUTask.get(0);
                                        LynxMindEndTickEventManager.registerTask(performingUTask);
                                    }
                                    /// 若当前有操作，判断操作结果，来决定是否下一步
                                    else if(performingUTask.getResult() != UTask.UTaskResult.NONE){
                                        switch (performingUTask.getResult()) {
                                            /// 失败：停止子任务
                                            case FAILED -> this.state = TaskState.FAILED;
                                            /// 成功：继续下一个操作
                                            case SUCCESS -> performingUTask = null;
                                        }
                                    }
                                }
                                /// 若没有操作 说明工作台已移动完成
                            }
                            else {
                                System.out.println("尝试打开背包");
                                ContainerHelper.openContainer(InventoryScreen.class);
                            }
                        }
                    }
                }
            }
            else {
                if(craftingTablePlacingPointSearchingRange > 16){
                    parentTask.stop("该环境无法使用工作台。");
                }
                else{
                    craftingTablePlacingPointSearchingRange *= 2;
                    System.out.println("未找到放置工作台位置，正在扩大搜索范围：" + craftingTablePlacingPointSearchingRange);
                }
            }
        }
        private boolean isCTPlacingPointInRange(){
            var baritone = parentTask.getBaritone();
            var player = ClientUtils.getPlayer();
            if(cTPlacingPointPos == null) return false;
            return baritone.getPlayerContext().player().getPos().distanceTo(cTPlacingPointPos.toCenterPos()) < 4D;
        }
    }
}
