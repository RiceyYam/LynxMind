package org.ricey_yam.lynxmind.client.task.temp.baritone;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.sub.LAutoStrikeBackTask;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BMurderTask extends BTask{
    private BKillauraTask bKillauraTask;
    private List<UUID> murderTargetUUIDs;
    public BMurderTask(List<UUID> murderTargetUUIDs, Action linkedAction) {
        setTaskType(BTaskType.MURDER);
        this.murderTargetUUIDs = filterToEntityWhichCanMurder(murderTargetUUIDs);
        this.linkedAction = linkedAction;
        this.bKillauraTask = LAutoStrikeBackTask.getKillauraBTask();
    }
    @Override
    public void start() {
        this.currentTaskState = TaskState.IDLE;
    }

    @Override
    public void tick() {
        System.out.println("murder ticks");
        if(murderTargetUUIDs.isEmpty()){
            stop("谋杀任务已完成!");
            return;
        }
        if(bKillauraTask != null){
            System.out.println("murderTargetUUIDs: " + murderTargetUUIDs.get(0));
            bKillauraTask.setMustKillTargetList(murderTargetUUIDs);
            bKillauraTask.setWeight(2);
        }
        else{
            System.out.println("bKillauraTask is null");
        }
    }

    @Override
    public void stop(String cancelReason) {
        this.currentTaskState = TaskState.FINISHED;
        baritone.getPathingBehavior().cancelEverything();
        baritone.getCustomGoalProcess().setGoal(null);
        murderTargetUUIDs.clear();
        if(bKillauraTask != null){
            bKillauraTask.getMustKillTargetList().clear();
            bKillauraTask.setWeight(0);
        }
        sendBTaskStopMessage(cancelReason);
        System.out.println("谋杀BTask已停止: " + cancelReason);
    }

    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        bKillauraTask.setWeight(0);
    }
    
    private List<UUID> filterToEntityWhichCanMurder(List<UUID> murderTargetUUIDs){
        var result = new ArrayList<UUID>();
        for (int i = 0; i < murderTargetUUIDs.size(); i++) {
            var uuid = murderTargetUUIDs.get(i);
            if(EntityUtils.getEntityByUUID(uuid) != null) {
                result.add(uuid);
            }
            else{
                System.out.println("entity is null! removed this uuid!");
            }
        }
        return result;
    }
}
