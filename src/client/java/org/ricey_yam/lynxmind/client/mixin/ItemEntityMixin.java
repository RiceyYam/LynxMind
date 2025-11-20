package org.ricey_yam.lynxmind.client.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerPickupItemEvent;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.baritone.BCollectionTask;
import org.ricey_yam.lynxmind.client.task.baritone.BTaskType;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Shadow
    private int pickupDelay;
    @Inject(method = "onPlayerCollision",at = @At("HEAD"),cancellable = true)
    private void onPlayerCollision(PlayerEntity player, CallbackInfo ci){
        var itemEntity = (ItemEntity)(Object)this;
        if (!itemEntity.getWorld().isClient) {
            ItemStack itemStack = itemEntity.getStack();
            Item item = itemStack.getItem();
            int i = itemStack.getCount();
            var name = ItemUtils.getItemID(itemStack);
            if (pickupDelay == 0 && (itemEntity.getOwner() == null || itemEntity.getOwner().equals(player.getUuid())) && player.getInventory().insertStack(itemStack)) {
                player.sendPickup(itemEntity, i);

                /// 发送捡起物品状态
                if(AIServiceManager.isTaskActive() && AIServiceManager.isServiceActive){
                    var playerPickupItemEvent = new PlayerPickupItemEvent(name,i);
                    var serialized = LynxJsonHandler.serialize(playerPickupItemEvent);
                    Objects.requireNonNull(AIServiceManager.sendAndReceiveReplyAsync(serialized)).whenComplete((reply, throwable) -> ChatManager.handleAIReply(reply));
                }

                /// 更新Baritone收集任务状态
                if(LynxMindEndTickEventManager.isTaskActive(BTaskType.COLLECTION)){
                    var bCT = (BCollectionTask) LynxMindEndTickEventManager.getTask(BTaskType.COLLECTION);
                    if(bCT != null) bCT.onItemCollected(name,i);
                }

                if (itemStack.isEmpty()) {
                    itemEntity.discard();
                    itemStack.setCount(i);
                }

                player.increaseStat(Stats.PICKED_UP.getOrCreateStat(item), i);
                player.triggerItemPickedUpByEntityCriteria(itemEntity);
            }
        }
        ci.cancel();
    }
}
