package org.ricey_yam.lynxmind.client.baritone.status.sub;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.BlockPos;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.baritone.status.BStatus;
import org.ricey_yam.lynxmind.client.baritone.status.BStatusType;

import java.util.List;

@Getter
@Setter
public class BFindingNeededBlocksStatus extends BStatus {
    private List<ItemStackLite> needed_blocks;
    public BFindingNeededBlocksStatus(BlockPos blockPos, List<ItemStackLite> needed_blocks) {
        this.type =  BStatusType.BSTATUS_FINDING_NEEDED_BLOCKS;
        this.needed_blocks = needed_blocks;
    }
}
