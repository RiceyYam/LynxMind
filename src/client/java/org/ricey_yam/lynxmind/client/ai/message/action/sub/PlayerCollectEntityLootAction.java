package org.ricey_yam.lynxmind.client.ai.message.action.sub;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BEntityCollectionTask;

import java.util.List;

@Getter
@Setter
public class PlayerCollectEntityLootAction extends Action {
    private List<BEntityCollectionTask.EntityKillingQuota> killingQuotas;
    public PlayerCollectEntityLootAction(List<BEntityCollectionTask.EntityKillingQuota> killingQuotas) {
        this.killingQuotas = killingQuotas;
    }

    @Override
    public boolean invoke() {
        BaritoneManager.stopPathingRelatedTasks("由于Killaura自带寻路，需取消普通寻路Task");
        var newKFCTask = new BEntityCollectionTask(killingQuotas,this);
        LynxMindEndTickEventManager.registerTask(newKFCTask);
        return super.invoke();
    }
}
