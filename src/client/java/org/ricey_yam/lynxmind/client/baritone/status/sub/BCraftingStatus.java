package org.ricey_yam.lynxmind.client.baritone.status.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.baritone.status.BStatus;
import org.ricey_yam.lynxmind.client.baritone.status.BStatusType;

import java.util.List;

@Getter
@Setter
public class BCraftingStatus extends BStatus {
    private List<ItemStackLite> to_craft;
    private List<ItemStackLite> craft_failed;
    private List<ItemStackLite> craft_success;
    public BCraftingStatus(List<ItemStackLite> to_craft, List<ItemStackLite> craft_failed, List<ItemStackLite> craft_success) {
        this.type =  BStatusType.BSTATUS_CRAFTING;
        this.to_craft = to_craft;
        this.craft_failed = craft_failed;
        this.craft_success = craft_success;
    }
}
