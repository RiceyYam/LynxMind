package org.ricey_yam.lynxmind.client.task.non_temp.lynx.sub;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.LivingEntity;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.LTask;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.LTaskType;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BEntityCollectionTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BKillauraTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BMurderTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BTaskType;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class LAutoStrikeBackTask extends LTask {
    private int strikeBoxSize;
    private int checkTickDelay;
    private List<UUID> addedToKillauraBTaskUUIDs = new ArrayList<>();
    private List<UUID> additionalStrikeBackTarget = new ArrayList<>();
    public LAutoStrikeBackTask(int strikeBoxSize,int checkTickDelay) {
        setTaskType(LTaskType.AUTO_STRIKE_BACK);
        additionalStrikeBackTarget.clear();
        this.strikeBoxSize = strikeBoxSize;
        this.checkTickDelay = checkTickDelay;
    }
    @Override
    public void start() {
        this.currentTaskState = TaskState.IDLE;
        tickTimer = 0;
    }

    @Override
    public void tick(){
        tickTimer++;
        if(tickTimer >= checkTickDelay){
            tickTimer = 0;
            if(strikeBoxSize <= 0) return;
            var entities = EntityUtils.scanAllEntity(getPlayer(),LivingEntity.class,strikeBoxSize, e -> EntityUtils.isHostileToPlayer(e) || additionalStrikeBackTarget.contains(e.getUuid()));
            if(entities != null && !entities.isEmpty()){
                enableKillaura(entities);
            }
            else if(!hasMustKillTarget()){
                disableKillaura();
            }
        }
    }

    @Override
    public void stop(String cancelReason) {
        this.currentTaskState = TaskState.FINISHED;
        checkTickDelay = 9999;
        tickTimer = 0;
        strikeBoxSize = 0;
        additionalStrikeBackTarget.clear();
        LynxMindEndTickEventManager.unregisterTask(BTaskType.KILLAURA,"自动还击已停止!");
        System.out.println("自动还击已停止!" + cancelReason);
    }

    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        tickTimer = 0;
    }

    private boolean hasMustKillTarget(){
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.KILLAURA)){
            var killauraBTask = LynxMindEndTickEventManager.getTask(BTaskType.KILLAURA);
            if(!(killauraBTask instanceof BKillauraTask kbt) || kbt.getCurrentTaskState() == TaskState.PAUSED) return false;
            kbt.setStrikeBackTargetList(new ArrayList<>());
            return !kbt.getMustKillTargetList().isEmpty();
        }
        else return false;
    }

    private void enableKillaura(List<LivingEntity> entities){
        var kbt = getKillauraBTask();
        if(kbt != null){
            addedToKillauraBTaskUUIDs.clear();
            for(var entity : entities){
                addedToKillauraBTaskUUIDs.add(entity.getUuid());
            }
            addedToKillauraBTaskUUIDs.addAll(additionalStrikeBackTarget);

            kbt.getStrikeBackTargetList().addAll(addedToKillauraBTaskUUIDs);
            kbt.setWeight(2);
        }
    }

    private void disableKillaura(){
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.KILLAURA) && !hasOtherRelatedKillauraTask()){
            var kbt = getKillauraBTask();
            if (kbt != null && kbt.getCurrentTaskState() == TaskState.IDLE) {
                kbt.getStrikeBackTargetList().removeAll(addedToKillauraBTaskUUIDs);
                kbt.setWeight(0);
            }
        }
    }

    private boolean hasOtherRelatedKillauraTask(){
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.ENTITY_COLLECTION)){
            var eCT = (BEntityCollectionTask) LynxMindEndTickEventManager.getTask(BTaskType.ENTITY_COLLECTION);
            return eCT != null && eCT.getCurrentTaskState() == TaskState.IDLE;
        }
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.MURDER)){
            var mCT = (BMurderTask) LynxMindEndTickEventManager.getTask(BTaskType.MURDER);
            return mCT != null && mCT.getCurrentTaskState() == TaskState.IDLE;
        }
        return false;
    }

    public static BKillauraTask getKillauraBTask(){
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.KILLAURA)){
            var killauraBTask = LynxMindEndTickEventManager.getTask(BTaskType.KILLAURA);
            if(!(killauraBTask instanceof BKillauraTask kbt)) return null;
            return kbt;
        }
        else{
            var registered = new BKillauraTask(3);
            LynxMindEndTickEventManager.registerTask(registered);
            return registered;
        }
    }
}
