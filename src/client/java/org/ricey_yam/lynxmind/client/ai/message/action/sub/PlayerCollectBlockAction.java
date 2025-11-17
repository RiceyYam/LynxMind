package org.ricey_yam.lynxmind.client.ai.message.action.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.ai.message.game_info.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.baritone.BCollectionTask;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;

import java.util.List;

@Getter
@Setter
public class PlayerCollectBlockAction extends Action {
    private List<ItemStackLite> needed_blocks;
    public PlayerCollectBlockAction(List<ItemStackLite> needed_blocks) {
        this.needed_blocks = needed_blocks;
    }
    @Override
    public boolean invoke() {
        try {
            BaritoneManager.stopPathingRelatedTasks("由于收集方块自带寻路，需取消普通寻路Task");
            if (needed_blocks == null || needed_blocks.isEmpty()) {
                LynxMindClient.sendModMessage("§c错误: 方块ID列表不能为空！");
                return false;
            }
            var baritoneCollectionTask = new BCollectionTask(this.needed_blocks,this);
            LynxMindEndTickEventManager.registerTask(baritoneCollectionTask);
            return super.invoke();
        }
        catch (Exception e) {
            LynxMindClient.sendModMessage("§c执行收集方块任务时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
