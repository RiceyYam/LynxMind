package org.ricey_yam.lynxmind.client.ai.message.event.ai.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.event.ai.AIEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.ai.AIEventType;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerScanEntityEvent;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class AIGetNearbyEntityEvent extends AIEvent {
    private int radius;
    private List<String> targetEntityID;
    private AIGetNearbyEntityEvent(int radius, List<String> targetEntityID) {
        setType(AIEventType.EVENT_AI_GET_NEARBY_ENTITIES);
        this.radius = radius;
        this.targetEntityID = targetEntityID;
    }

    @Override
    public void onReceive() {
        var scanEntityEvent = new PlayerScanEntityEvent(radius, targetEntityID);
        var serialized = LynxJsonHandler.serialize(scanEntityEvent);
        Objects.requireNonNull(AIServiceManager.sendAndReceiveReplyAsync(serialized)).whenComplete((reply, error) -> ChatManager.handleAIReply(reply));
    }
}
