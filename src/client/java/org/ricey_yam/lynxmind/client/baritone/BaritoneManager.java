package org.ricey_yam.lynxmind.client.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.ricey_yam.lynxmind.client.baritone.status.sub.*;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.baritone.BCollectionTask;
import org.ricey_yam.lynxmind.client.baritone.status.BStatus;
import org.ricey_yam.lynxmind.client.task.baritone.BCraftingTask;
import org.ricey_yam.lynxmind.client.task.baritone.BTaskType;

@Getter
@Setter
public class BaritoneManager {
    public static ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    public static IBaritone getClientBaritone() {
        if (getPlayer() == null) {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        }
        return BaritoneAPI.getProvider().getBaritoneForPlayer(getPlayer());
    }

    /// 获取当前Baritone状态
    public static BStatus getCurrentBStatus() {
        var baritone = getClientBaritone();

        /// 寻路
        if(isPathingTaskActive()){
            var pathingGoal = baritone.getPathingBehavior().getGoal();
            if(pathingGoal instanceof GoalBlock goalBlock){
                return new BPathingToGoalStatus(goalBlock);
            }
            else if(pathingGoal instanceof GoalXZ goalXZ){
                return new BPathingToGoalXZStatus(goalXZ.getX(),goalXZ.getZ());
            }
        }
        /// 收集
        if(isCollectionTaskActive()){
            var collectingTask = LynxMindEndTickEventManager.getTask(BTaskType.COLLECTION);
            if(collectingTask instanceof BCollectionTask bCT){
                if(bCT.getCurrentTargetBlockPos() != null){

                    var isNeededBlock = false;
                    for(var item : bCT.getNeededItem()){
                        if(item == null) continue;
                        if (item.getItem_name().equals(bCT.getMiningBlockName())) {
                            isNeededBlock = true;
                            break;
                        }
                    }
                    var miningNeededBlock = bCT.getCollectingState() == BCollectionTask.CollectingState.MINING_BLOCK && isNeededBlock;
                    var miningUnneededBlock = bCT.getCollectingState() == BCollectionTask.CollectingState.MINING_BLOCK && !isNeededBlock;
                    if(miningNeededBlock){
                        return new BMiningStatus(bCT.getMiningBlockName());
                    }
                    else if(bCT.getCollectingState() == BCollectionTask.CollectingState.MOVING_TO_BLOCK || miningUnneededBlock){
                        return new BFindingNeededBlocksStatus(bCT.getCurrentTargetBlockPos(),bCT.getNeededItem());
                    }
                }
            }
        }
        /// 制作
        if(isCraftingTaskActive()){
            var craftingTask = LynxMindEndTickEventManager.getTask(BTaskType.CRAFTING);
            if(craftingTask instanceof BCraftingTask bCT){
                return new BCraftingStatus(bCT.getTo_craft(),bCT.getCraft_failed(),bCT.getCraft_success());
            }
        }
        return new BStatus();
    }

    /// 停止所有BTask
    public static void stopAllTasks(String reason) {
        LynxMindEndTickEventManager.cleanAllTasks(reason);
    }

    /// 停止所有需要寻路的BTask
    public static void stopPathingRelatedTasks(String reason) {
        LynxMindEndTickEventManager.unregisterTask(BTaskType.PATHING,reason);
        LynxMindEndTickEventManager.unregisterTask(BTaskType.COLLECTION,reason);
        LynxMindEndTickEventManager.unregisterTask(BTaskType.CRAFTING,reason);
    }

    public static boolean isPathingTaskActive(){
        return LynxMindEndTickEventManager.isTaskActive(BTaskType.PATHING);
    }

    public static boolean isCollectionTaskActive(){
        return LynxMindEndTickEventManager.isTaskActive(BTaskType.COLLECTION);
    }

    public static boolean isCraftingTaskActive(){
        return LynxMindEndTickEventManager.isTaskActive(BTaskType.CRAFTING);
    }
}
