package org.ricey_yam.lynxmind.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import org.ricey_yam.lynxmind.client.event.LynxMindMixinEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "onKilledOther",at = @At("HEAD"),cancellable = true)
    private void onKilledOther(ServerWorld world, LivingEntity other, CallbackInfoReturnable<Boolean> cir) {
        var player = (PlayerEntity) (Object) this;
        LynxMindMixinEvent.onPlayerKillEntity(other);
        player.incrementStat(Stats.KILLED.getOrCreateStat(other.getType()));
        cir.setReturnValue(true);
        cir.cancel();
    }
}
