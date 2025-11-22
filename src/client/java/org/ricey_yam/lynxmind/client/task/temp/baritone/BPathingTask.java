package org.ricey_yam.lynxmind.client.task.temp.baritone;

import baritone.api.pathing.goals.GoalBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.BlockPos;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ContainerHelper;

@Getter
@Setter
public class BPathingTask extends BTask {

    /// 目标位置
    private int targetX;
    private int targetY;
    private int targetZ;

    /// 重试次数（无法到达某个点时重新规划路径）
    private int restartCount;
    public BPathingTask(float x, float y, float z, Action linked_action) {
        super();
        this.weight = 1;
        this.taskType = BTaskType.PATHING;
        this.targetX = Math.round(x);
        this.targetY = Math.round(y);
        this.targetZ = Math.round(z);
        this.currentTaskState = TaskState.IDLE;
        this.linkedAction = linked_action;
    }

    @Override
    public void start() {
        var goal = new GoalBlock(targetX, targetY, targetZ);
        baritone.getCustomGoalProcess().setGoal(goal);
        baritone.getCustomGoalProcess().path();
    }

    @Override
    public void tick() {
        if(baritone != null){
            if(baritone.getPathingBehavior().getGoal() == null && getPlayer().getBlockPos().isWithinDistance(new BlockPos(targetX,targetY,targetZ),1)){
                stop("已到达目的地！");
            }
            else if(baritone.getPathingBehavior().getGoal() == null && restartCount < 3){
                System.out.println("无法找到路径，正在重试，次数：" + restartCount);
                restartCount++;
            }
            else if(baritone.getPathingBehavior().getGoal() == null && restartCount >= 3){
                stop("无法到达目的地。");
            }
            else restartCount = 0;
        }
    }

    @Override
    public void stop(String stopReason) {
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();

        /// 发送任务停止事件给AI
        sendBTaskStopMessage(stopReason);

        currentTaskState = TaskState.FINISHED;

        System.out.println("寻路任务已停止：" + stopReason);
    }

    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
        ContainerHelper.closeContainer();
    }

}
