package org.ricey_yam.lynxmind.client.event;

import net.minecraft.entity.LivingEntity;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerPickupItemEvent;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BBlockCollectionTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BEntityCollectionTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BMurderTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BTaskType;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;

import java.util.Objects;

public class LynxMindMixinEvent {
    public static void onPlayerPickupItem(String itemID,int count){
        /// 发送捡起物品状态
        if(AIServiceManager.isTaskActive() && AIServiceManager.isServiceActive){
            var playerPickupItemEvent = new PlayerPickupItemEvent(itemID,count);
            var serialized = LynxJsonHandler.serialize(playerPickupItemEvent);
            Objects.requireNonNull(AIServiceManager.sendAndReceiveReplyAsync(serialized)).whenComplete((reply, throwable) -> ChatManager.handleAIReply(reply));
        }

        /// 更新Baritone收集任务状态
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.BLOCK_COLLECTION)){
            var bCT = (BBlockCollectionTask) LynxMindEndTickEventManager.getTask(BTaskType.BLOCK_COLLECTION);
            if(bCT != null) bCT.onItemCollected(itemID,count);
        }
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.ENTITY_COLLECTION)){
            var eCT = (BEntityCollectionTask) LynxMindEndTickEventManager.getTask(BTaskType.ENTITY_COLLECTION);
            if(eCT != null && eCT.getKillingQuotas() != null && !eCT.getKillingQuotas().isEmpty()) {
                for (int i = 0; i < eCT.getKillingQuotas().size(); i++) {
                    var quota = eCT.getKillingQuotas().get(i);
                    var items = quota.getNeeded_item();
                    for (int j = 0; j < items.size(); j++) {
                        var item = items.get(j);
                        if(item.getItem_name().equals(itemID)){
                            item.setCount(item.getCount() - count);
                            if(item.getCount() <= 0) {
                                items.remove(j);
                                if(items.isEmpty()){
                                    eCT.getKillingQuotas().remove(i);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void onPlayerKillEntity(LivingEntity killedEntity){
        var killedEntityID = EntityUtils.getEntityID(killedEntity);
        var killedEntityUUID = killedEntity.getUuid();

        /// 更新BTask状态
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.ENTITY_COLLECTION)){
            var eCT = (BEntityCollectionTask) LynxMindEndTickEventManager.getTask(BTaskType.ENTITY_COLLECTION);
            if(eCT != null && eCT.getKillingQuotas() != null && !eCT.getKillingQuotas().isEmpty()) {
                eCT.transitionToFindingLoot();
            }
        }
        if(LynxMindEndTickEventManager.isTaskActive(BTaskType.MURDER)){
            var mCT = (BMurderTask) LynxMindEndTickEventManager.getTask(BTaskType.MURDER);
            if(mCT != null && mCT.getMurderTargetUUIDs() != null && !mCT.getMurderTargetUUIDs().isEmpty()) {
                mCT.getMurderTargetUUIDs().remove(killedEntityUUID);
            }
        }

    }
}
