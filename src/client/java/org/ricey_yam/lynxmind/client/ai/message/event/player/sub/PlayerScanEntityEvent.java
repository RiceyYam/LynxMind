package org.ricey_yam.lynxmind.client.ai.message.event.player.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEventType;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityLite;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;

import java.util.List;

@Getter
@Setter
public class PlayerScanEntityEvent extends PlayerEvent {
    private int radius;
    private List<String> scanning_id;
    private List<EntityLite> nearby_entities;
    public PlayerScanEntityEvent(int radius, List<String> scanning_id) {
        setType(PlayerEventType.EVENT_PLAYER_SCAN_ENTITY);
        this.radius = radius;
        this.scanning_id = scanning_id;
        this.nearby_entities = EntityUtils.scanAllEntity(getPlayer(),scanning_id,radius,e -> true);
    }
}
