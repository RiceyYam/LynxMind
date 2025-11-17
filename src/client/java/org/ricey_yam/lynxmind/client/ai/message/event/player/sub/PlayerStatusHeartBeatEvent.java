package org.ricey_yam.lynxmind.client.ai.message.event.player.sub;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.player.PlayerEntity;
import org.ricey_yam.lynxmind.client.ai.message.game_info.ui.SlotItemStack;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.PlayerEventType;
import org.ricey_yam.lynxmind.client.baritone.status.BStatus;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.TransformUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlotType;

import java.util.List;

@Getter
@Setter
public class PlayerStatusHeartBeatEvent extends PlayerEvent {

    private float health;
    private float maxHealth;

    private int hunger;
    private int maxHunger = 20;
    private float saturationLevel;

    private int posX;
    private int posY;
    private int posZ;

    private float yaw;
    private float pitch;

    private List<SlotItemStack> inventory_hotbar;
    private List<SlotItemStack> inventory_inner;
    private List<SlotItemStack> inventory_equipment;

    private BStatus current_baritone_task;
    public PlayerStatusHeartBeatEvent(PlayerEntity player) {
        setType(PlayerEventType.EVENT_PLAYER_STATUS_HEARTBEAT);

        this.health = player.getHealth();
        this.maxHealth = player.getMaxHealth();

        this.hunger = player.getHungerManager().getFoodLevel();
        this.saturationLevel = player.getHungerManager().getSaturationLevel();

        this.posX = player.getBlockX();
        this.posY = player.getBlockY();
        this.posZ = player.getBlockZ();

        this.yaw = TransformUtils.normalizeYaw180(player.getHeadYaw());
        this.pitch = player.getPitch();

        this.inventory_hotbar = ItemUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_HOTBAR, ComplexContainerType.PLAYER_INFO);
        this.inventory_inner =  ItemUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_INNER,ComplexContainerType.PLAYER_INFO);
        this.inventory_equipment = ItemUtils.getClientPlayerInventoryItems(LSlotType.INVENTORY_EQUIPMENT,ComplexContainerType.PLAYER_INFO);

        current_baritone_task = BaritoneManager.getCurrentBStatus();
    }
}
