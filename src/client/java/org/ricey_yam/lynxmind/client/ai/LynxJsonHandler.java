package org.ricey_yam.lynxmind.client.ai;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.ricey_yam.lynxmind.client.ai.message.action.sub.*;
import org.ricey_yam.lynxmind.client.ai.message.event.ai.sub.*;
import org.ricey_yam.lynxmind.client.utils.format.JsonExt;

import java.util.Arrays;
import java.util.List;

public class LynxJsonHandler {
    private final static Gson gson = new Gson();

    /// 反序列化JSON
    public static <T> T deserialize(String json){
        var typeToken = getTypeToken(json);
        if(typeToken == null){
            return null;
        }

        T result = gson.fromJson(json,typeToken.getType());
        if(result == null) return null;

        /// Json的一些列表包含不同类型事件 要特殊处理
        result = handleSpecialJson(result,json);

        return result;
    }

    /// 序列化JSON
    public static <T> String serialize(T t){
        if(t == null) return "";
        return gson.toJson(t);
    }

    /// 获取类型
    public static TypeToken<?> getTypeToken(String json){
        var jsonObj = gson.fromJson(json,JsonObject.class);
        var jsonExt = JsonExt.of(jsonObj);
        var type = jsonExt.getAsString("type");
        List<String> typeSection;

        /// 未知Json 消息格式错误
        if(type == null || type.isEmpty() || type.equals("NONE")){
            return null;
        }

        /// 事件
        if(type.startsWith("EVENT")){
            typeSection = Arrays.asList(type.split("_",3));
            var eventFrom = typeSection.get(1);
            var eventType = typeSection.get(2);
            switch (eventFrom){
                case "AI" ->{
                    switch (eventType){
                        case "CONTROL" -> {
                            return new TypeToken<AIControlEvent>(){};
                        }
                        case "START" -> {
                            return new TypeToken<AIStartEvent>(){};
                        }
                        case "STOP" ->{
                            return new TypeToken<AIStopEvent>(){};
                        }
                        case "GET_STATUS" -> {
                            return new TypeToken<AIGetStatusEvent>(){};
                        }
                        case "GET_NEARBY_BLOCK" -> {
                            return new TypeToken<AIGetNearbyBlockEvent>(){};
                        }
                        case "GET_NEARBY_ENTITY" -> {
                            return new TypeToken<AIGetNearbyEntityEvent>(){};
                        }
                        case "ADD_STRIKE_BACK_TARGET" -> {
                            return new TypeToken<AIAddStrikeBackTargetEvent>(){};
                        }
                        case "REMOVE_STRIKE_BACK_TARGET" -> {
                            return new TypeToken<AIRemoveStrikeBackTargetEvent>(){};
                        }
                        case "GET_STRIKE_BACK_TARGET_LIST" -> {
                            return new TypeToken<AIGetStrikeBackTargetListEvent>(){};
                        }
                    }
                }

            }
        }

        /// 动作
        else if (type.startsWith("ACTION")) {
            typeSection = Arrays.asList(type.split("_",2));
            var actionMove =  typeSection.get(1);
            switch (actionMove){
                case "STOP_BARITONE" ->{
                    return new TypeToken<StopBaritoneAction>(){};
                }
                case "MOVE" -> {
                    return new TypeToken<PlayerMoveAction>(){};
                }
                case "COLLECT_BLOCK" ->{
                    return new TypeToken<PlayerCollectBlockAction>(){};
                }
                case "CRAFTING" ->{
                    return new TypeToken<PlayerCraftingAction>(){};
                }
                case "COLLECT_ENTITY_LOOT" -> {
                    return new TypeToken<PlayerCollectEntityLootAction>(){};
                }
                case "MURDER" -> {
                    return new TypeToken<PlayerMurderAction>(){};
                }
            }
        }

        return null;
    }

    /// 处理特殊的Json
    private static <T> T handleSpecialJson(T t,String specialJson){
        if(t instanceof AIControlEvent lynxAIEvent){
            var jsonObj = gson.fromJson(specialJson,JsonObject.class);
            var jsonExt = JsonExt.of(jsonObj);
            var jsonAction = jsonExt.getElement("action");
            var actionJson = gson.toJson(jsonAction);
            lynxAIEvent.setAction(deserialize(actionJson));
            return (T)lynxAIEvent;
        }
        return t;
    }
}
