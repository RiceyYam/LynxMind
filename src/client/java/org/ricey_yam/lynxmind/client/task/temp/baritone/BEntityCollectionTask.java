package org.ricey_yam.lynxmind.client.task.temp.baritone;

import baritone.api.pathing.goals.GoalBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.LTaskType;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.sub.LAutoStrikeBackTask;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class BEntityCollectionTask extends BTask{

    @Getter
    @Setter
    public static class EntityKillingQuota{
        private String entity_id;
        private List<ItemStackLite> needed_item;
        public EntityKillingQuota(String entity_id, List<ItemStackLite> needed_item) {
            this.entity_id = entity_id;
            this.needed_item = needed_item;
        }
    }

    public enum CollectionState{
        FINDING_LOOT,
        COLLECTING_LOOT,
        FINDING_AND_KILLING_ENTITY
    }

    private ItemEntity currentLootEntityTarget;

    private LAutoStrikeBackTask autoStrikeBackTask;
    private CollectionState collectionState;

    private List<EntityKillingQuota> killingQuotas;
    public BEntityCollectionTask(List<EntityKillingQuota> killingQuotas, Action linkedAction) {
        this.weight = 2;
        setTaskType(BTaskType.ENTITY_COLLECTION);
        this.killingQuotas = killingQuotas;

        /// 由于该BTask需要依赖自动杀戮光环LTask
        /// 所以必须保证autoStrikeBackTask存在
        if(LynxMindEndTickEventManager.isTaskActive(LTaskType.AUTO_STRIKE_BACK)){
            this.autoStrikeBackTask = (LAutoStrikeBackTask)LynxMindEndTickEventManager.getTask(LTaskType.AUTO_STRIKE_BACK);
        }
        else {
            this.autoStrikeBackTask = new LAutoStrikeBackTask(5,10);
            LynxMindEndTickEventManager.registerTask(this.autoStrikeBackTask);
        }
    }
    @Override
    public void start() {
        this.currentTaskState = TaskState.IDLE;
        transitionToCollectingLoot();
    }

    @Override
    public void tick() {
        if(killingQuotas.isEmpty()){
            stop("收集任务已完成!");
            return;
        }

        switch (collectionState){
            case FINDING_LOOT -> findingLootTick();
            case COLLECTING_LOOT -> collectingLootTick();
            case FINDING_AND_KILLING_ENTITY -> findingAndKillingEntityTick();
        }
    }

    @Override
    public void stop(String cancelReason) {
        this.currentTaskState = TaskState.FINISHED;
        baritone.getPathingBehavior().cancelEverything();
        baritone.getCustomGoalProcess().setGoal(null);
        Objects.requireNonNull(LAutoStrikeBackTask.getKillauraBTask()).getMustKillTargetList().clear();
        LAutoStrikeBackTask.getKillauraBTask().setWeight(0);
        sendBTaskStopMessage(cancelReason);
        System.out.println("击杀生物获取材料BTask已停止: " + cancelReason);
    }

    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        Objects.requireNonNull(LAutoStrikeBackTask.getKillauraBTask()).setWeight(0);
    }

    public void transitionToFindingLoot(){
        baritone.getPathingBehavior().cancelEverything();
        baritone.getCustomGoalProcess().setGoal(null);
        this.collectionState = CollectionState.FINDING_LOOT;
    }
    public void transitionToCollectingLoot(){
        this.collectionState = CollectionState.COLLECTING_LOOT;
    }
    public void transitionToFindingAndKillingEntity(){
        this.collectionState = CollectionState.FINDING_AND_KILLING_ENTITY;
    }

    private void findingLootTick(){
        /// 寻找是否有需要的物品的掉落物
        var nearestLootEntity = EntityUtils.findNearestEntity(getPlayer(), ItemEntity.class,50,e -> e.getStack().getCount() > 0 && isNeededLoot(ItemUtils.getItemID(e.getStack())));
        if(nearestLootEntity != null) {
            var targetStack = nearestLootEntity.getStack();
            var targetItemID = ItemUtils.getItemID(targetStack);
            var goalProcess = baritone.getCustomGoalProcess();
            if(goalProcess == null) return;
            currentLootEntityTarget = nearestLootEntity;
            transitionToCollectingLoot();
        }
        else {
            transitionToFindingAndKillingEntity();
        }
    }
    private void collectingLootTick(){
        var goalProcess = baritone.getCustomGoalProcess();
        if(goalProcess == null) return;

        if(!isLootDisappeared()){
            var newGoal = new GoalBlock(currentLootEntityTarget.getBlockPos());
            goalProcess.setGoalAndPath(newGoal);
        }
        else{
            transitionToFindingLoot();
        }
    }
    private void findingAndKillingEntityTick(){
        var killauraTask = LAutoStrikeBackTask.getKillauraBTask();
        if(killauraTask != null) {
            if (autoStrikeBackTask.getCurrentTaskState() == TaskState.IDLE) {
                var killingEntities = EntityUtils.scanAllEntity(getPlayer(), LivingEntity.class, 50, e -> isKillingEntity(EntityUtils.getEntityID(e)));
                var killingEntitiesUUIDs = new ArrayList<UUID>();
                for (var e : killingEntities) {
                    if (e == null) continue;
                    killingEntitiesUUIDs.add(e.getUuid());
                }
                if(!killingEntitiesUUIDs.isEmpty()){
                    killauraTask.setMustKillTargetList(killingEntitiesUUIDs);
                    killauraTask.setWeight(2);
                }
                else{
                    stop("附近没有可获取相应材料的生物!");
                }
            }
        }
    }
    private boolean isKillingEntity(String entityID){
        for(var q : killingQuotas){
            if(q == null) continue;
            var qI = q.getEntity_id();
            if(qI.equals(entityID)) return true;
        }
        return false;
    }
    private boolean isNeededLoot(String itemID){
        for(var quota : killingQuotas){
            var nItems = quota.getNeeded_item();
            for(var nItem : nItems){
                if(nItem.getItem_name().equals(itemID)) return true;
            }
        }
        return false;
    }
    private boolean isLootDisappeared(){
        return currentLootEntityTarget == null || currentLootEntityTarget.getStack().getCount() <= 0;
    }
}

