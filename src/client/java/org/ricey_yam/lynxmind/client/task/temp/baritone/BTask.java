package org.ricey_yam.lynxmind.client.task.temp.baritone;

import baritone.api.IBaritone;
import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerBaritoneTaskStop;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.task.IAbsoluteTask;
import org.ricey_yam.lynxmind.client.task.temp.TempTask;

import java.util.Objects;

@Getter
@Setter
public abstract class BTask extends TempTask<BTaskType> implements IAbsoluteTask {
    /// 关联的Action（Action创建BTask）
    protected final IBaritone baritone;
    protected Action linkedAction;
    protected int weight;
    public BTask(){
        baritone = BaritoneManager.getClientBaritone();
    }

    /// 发送任务停止事件给AI
    protected void sendBTaskStopMessage(String stopReason){
        if(stopReason != null && !stopReason.isEmpty() && AIServiceManager.isServiceActive && AIServiceManager.isTaskActive() && linkedAction != null){
            var bTaskStopEvent = new PlayerBaritoneTaskStop(linkedAction,stopReason);
            var serialized = LynxJsonHandler.serialize(bTaskStopEvent);
            Objects.requireNonNull(AIServiceManager.sendAndReceiveReplyAsync(serialized)).whenComplete((reply, error) -> ChatManager.handleAIReply(reply));
        }
    }
}
