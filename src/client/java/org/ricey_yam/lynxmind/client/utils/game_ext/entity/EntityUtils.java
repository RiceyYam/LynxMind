package org.ricey_yam.lynxmind.client.utils.game_ext.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.ricey_yam.lynxmind.client.utils.game_ext.ClientUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityUtils {
    /// 判断一个生物是否与玩家敌对
    public static boolean isHostileToPlayer(Entity entity) {
        // 怪物类生物本身就是敌方
        if (entity instanceof Monster) {
            return true;
        }
        // 中立生物若以玩家为目标，则视为敌方
        else if (entity instanceof MobEntity mob) {
            return mob.getTarget() instanceof PlayerEntity;
        }
        return false;
    }

    public static <T extends Entity> List<T> scanAllEntity(LivingEntity entity, Class<T> targetEntityClass, int boxSize, Predicate<? super T> predicate){
        if(entity == null) return null;
        var world = entity.getEntityWorld();
        if(world == null) return null;
        var minPos = entity.getBlockPos().add(-boxSize, -boxSize, -boxSize);
        var maxPos = entity.getBlockPos().add(boxSize, boxSize, boxSize);
        var minVec3 = new Vec3d(minPos.getX(), minPos.getY(), minPos.getZ());
        var maxVec3 = new Vec3d(maxPos.getX(), maxPos.getY(), maxPos.getZ());
        return world.getEntitiesByClass(targetEntityClass,new Box(minVec3,maxVec3),predicate);
    }

    public static List<EntityLite> scanAllEntity(LivingEntity entity, List<String> id, int boxSize,Predicate<? super LivingEntity> predicate){
        var entityList = scanAllEntity(entity, LivingEntity.class, boxSize,predicate);
        var resultList = new ArrayList<EntityLite>();
        for(var e : entityList){
            if(e == null) continue;
            if(id != null && !id.isEmpty() && !id.contains(EntityUtils.getEntityID(e))) continue;
            resultList.add(new EntityLite(e));
        }
        return resultList;
    }

    public static <T extends Entity> T findNearestEntity(LivingEntity entity,Class<T> targetEntityClass,int boxSize,Predicate<? super T> predicate){
        var targets = scanAllEntity(entity,targetEntityClass,boxSize ,predicate);
        if(targets.isEmpty()) return null;
        var nearestEntity = targets.get(0);
        float minR = boxSize * 2;
        for(var target : targets){
            if(target.distanceTo(entity) < minR){
                minR = target.distanceTo(entity);
                nearestEntity = target;
            }
        }
        return nearestEntity;
    }
    public static <T extends Entity> T findNearestEntity(LivingEntity entity,Class<T> targetEntityClass,int boxSize){
        return findNearestEntity(entity,targetEntityClass,boxSize,e -> true);
    }

    public static String getEntityID(Entity entity){
        if(entity == null) return null;
        var i = Registries.ENTITY_TYPE.getId(entity.getType());
        return i.toString();
    }

    public static LivingEntity getEntityByUUID(UUID uuid){
        var world = ClientUtils.getWorld();
        if(world == null) return null;
        for (var entity : world.getEntitiesByClass(LivingEntity.class, Box.from(Vec3d.ZERO).expand(30000), e -> true)) {
            if (entity.getUuid().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }
}
