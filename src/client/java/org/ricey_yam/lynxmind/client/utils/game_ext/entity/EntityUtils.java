package org.ricey_yam.lynxmind.client.utils.game_ext.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtils {
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
            resultList.add(new EntityLite(entity));
        }
        return resultList;
    }

    public static <T extends Entity> T findNearestEntity(LivingEntity entity,Class<T> targetEntityClass,int boxSize){
        var targets = scanAllEntity(entity,targetEntityClass,boxSize ,e -> true);
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

    public static String getEntityID(Entity entity){
        if(entity == null) return null;
        var i = Registries.ENTITY_TYPE.getId(entity.getType());
        return i.toString();
    }
}
