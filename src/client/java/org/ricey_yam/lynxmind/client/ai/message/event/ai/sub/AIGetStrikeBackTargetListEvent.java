package org.ricey_yam.lynxmind.client.ai.message.event.ai.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.event.ai.AIEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerReplyStrikeBackTargetListEvent;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.LTaskType;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.sub.LAutoStrikeBackTask;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class AIGetStrikeBackTargetListEvent extends AIEvent {
    private String UUID_str;
    @Override
    public void onReceive() {
        var ACT_UUID = UUID.fromString(UUID_str);
        if(LynxMindEndTickEventManager.isTaskActive(LTaskType.AUTO_STRIKE_BACK)){
            var strikeBackTask = (LAutoStrikeBackTask) LynxMindEndTickEventManager.getTask(LTaskType.AUTO_STRIKE_BACK);
            if(strikeBackTask != null){
                var target = strikeBackTask.getAdditionalStrikeBackTarget();
                var replyEvent = new PlayerReplyStrikeBackTargetListEvent(target);
                var serialized = LynxJsonHandler.serialize(replyEvent);
                Objects.requireNonNull(AIServiceManager.sendAndReceiveReplyAsync(serialized)).whenComplete((reply, throwable) -> {
                    if(reply != null){
                        ChatManager.handleAIReply(reply);
                    }
                });
                LynxMindClient.sendModMessage("AI正在检索反击对象列表...");
            }
        }
    }
}
