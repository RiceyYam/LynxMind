package org.ricey_yam.lynxmind.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class BaritoneManager {

    public static ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    public static IBaritone getClientBaritone() {
        if (getPlayer() == null) {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        }
        return BaritoneAPI.getProvider().getBaritoneForPlayer(getPlayer());
    }
}
