package org.ricey_yam.lynxmind.client.ai.message.event.player;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import org.ricey_yam.lynxmind.client.ai.message.event.Event;
import org.ricey_yam.lynxmind.client.utils.game_ext.ClientUtils;

@Getter
@Setter
public abstract class PlayerEvent extends Event {
    protected PlayerEventType type = PlayerEventType.NONE;
    protected PlayerEntity getPlayer() {
        return ClientUtils.getPlayer();
    }
}
