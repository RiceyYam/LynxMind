package org.ricey_yam.lynxmind.client.ai.message.action.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BEntityCollectionTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BMurderTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PlayerMurderAction extends Action {
    private List<String> uuid_str_list;
    public PlayerMurderAction(List<String> uuid_str_list) {
        this.uuid_str_list = uuid_str_list;
    }

    @Override
    public boolean invoke() {
        BaritoneManager.stopPathingRelatedTasks("由于Killaura自带寻路，需取消普通寻路Task");
        var uuids = new ArrayList<UUID>();
        for (String uuid_str : uuid_str_list) {
            var uuid = UUID.fromString(uuid_str);
            uuids.add(uuid);
        }
        var newMurderTask = new BMurderTask(uuids,this);
        LynxMindEndTickEventManager.registerTask(newMurderTask);
        return super.invoke();
    }
}
