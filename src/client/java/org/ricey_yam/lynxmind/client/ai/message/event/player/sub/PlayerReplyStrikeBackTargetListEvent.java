package org.ricey_yam.lynxmind.client.ai.message.event.player.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEventType;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PlayerReplyStrikeBackTargetListEvent extends PlayerEvent {
    private List<UUID> strike_back_target_list;
    public PlayerReplyStrikeBackTargetListEvent(List<UUID> strike_back_target_list) {
        setType(PlayerEventType.EVENT_PLAYER_REPLY_STRIKE_BACK_TARGET_LIST);
        this.strike_back_target_list = strike_back_target_list;
    }
}
