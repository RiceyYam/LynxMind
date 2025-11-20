package org.ricey_yam.lynxmind.client.utils.game_ext.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.LivingEntity;

@Getter
@Setter
public class EntityLite {
    private String id;
    private float maxHealth;
    private float currentHealth;
    private int x;
    private int y;
    private int z;
    public EntityLite(LivingEntity entity) {
        this.id = EntityUtils.getEntityID(entity);
        this.currentHealth = entity.getHealth();
        this.maxHealth = entity.getMaxHealth();
        this.x = entity.getBlockPos().getX();
        this.y = entity.getBlockPos().getY();
        this.z = entity.getBlockPos().getZ();
    }
    public EntityLite(String id, float maxHealth, float currentHealth, int x, int y, int z) {
        this.id = id;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
