package org.ricey_yam.lynxmind.client.ai.message.event.player.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEventType;
import org.ricey_yam.lynxmind.client.utils.game_ext.block.BlockLite;
import org.ricey_yam.lynxmind.client.utils.game_ext.block.BlockUtils;

import java.util.List;

@Getter
@Setter
public class PlayerScanBlockEvent extends PlayerEvent {
    private int radius;
    private List<String> scanning_id;
    private List<BlockLite> nearby_blocks;
    public PlayerScanBlockEvent(int radius, List<String> scanning_id) {
        setType(PlayerEventType.EVENT_PLAYER_SCAN_BLOCK);
        this.radius = radius;
        this.scanning_id = scanning_id;
        this.nearby_blocks = BlockUtils.scanAllBlocks(getPlayer(),scanning_id,radius);
    }
}
