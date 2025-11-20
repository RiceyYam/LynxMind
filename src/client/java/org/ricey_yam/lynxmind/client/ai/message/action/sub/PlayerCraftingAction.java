package org.ricey_yam.lynxmind.client.ai.message.action.sub;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.baritone.BCraftingTask;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PlayerCraftingAction extends Action {
    private List<ItemStackLite> to_craft;

    @Expose(deserialize = false)
    private List<ItemStackLite> craft_failed = new ArrayList<>();

    @Expose(deserialize = false)
    private List<ItemStackLite> craft_success = new ArrayList<>();

    public PlayerCraftingAction(List<ItemStackLite> to_craft) {
        this.to_craft = to_craft;
    }
    @Override
    public boolean invoke() {
        try {
            BaritoneManager.stopPathingRelatedTasks("由于制作物品自带寻路，需取消普通寻路Task");
            if (to_craft == null || to_craft.isEmpty()) {
                LynxMindClient.sendModMessage("§c错误: 物品ID列表不能为空！");
                return false;
            }
            var baritoneCraftingTask = new BCraftingTask(this.to_craft,this);
            LynxMindEndTickEventManager.registerTask(baritoneCraftingTask);
            return super.invoke();
        }
        catch (Exception e) {
            LynxMindClient.sendModMessage("§c执行制作物品任务时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
