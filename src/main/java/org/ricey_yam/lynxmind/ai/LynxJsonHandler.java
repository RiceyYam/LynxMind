package org.ricey_yam.lynxmind.ai;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.ricey_yam.lynxmind.content.action.LynxAction;
import org.ricey_yam.lynxmind.content.action.LynxMoveAction;
import org.ricey_yam.lynxmind.content.event.ai.LynxAIControlEvent;
import org.ricey_yam.lynxmind.content.event.player.LynxPlayerHeartBeatEvent;
import org.ricey_yam.lynxmind.utils.JsonExt;

import java.util.ArrayList;
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
        if(result instanceof LynxAIControlEvent lynxAIEvent){
            var jsonObj = gson.fromJson(json,JsonObject.class);
            var jsonExt = JsonExt.of(jsonObj);
            var jsonActions = jsonExt.getAsElementList("actions");
            List<LynxAction> actions = new ArrayList<>();
            if(jsonActions != null && !jsonActions.isEmpty()){
                for (int i = 0; i < jsonActions.size(); i++) {
                    var jsonActionExt = JsonExt.of(jsonActions).getAsExt(i);
                    var actionJson = jsonActionExt.toJsonObject().getAsString();
                    if(actionJson != null){
                        actions.add(deserialize(actionJson));
                    }
                }
            }
            lynxAIEvent.setActions(actions);
        }

        return result;
    }

    /// 序列化JSON
    public static <T> String serialize(Class<T> tClass){
        if(tClass == null) return "";
        return gson.toJson(tClass);
    }


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
                case "PLAYER" ->{
                    switch (eventType){
                        case "STATUS_HEARTBEAT" -> {
                            return new TypeToken<LynxPlayerHeartBeatEvent>(){};
                        }
                    }
                }
                case "AI" ->{
                    switch (eventType){
                        case "CONTROL" -> {
                            return new TypeToken<LynxAIControlEvent>(){};
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
                case "MOVE" -> {
                    return new TypeToken<LynxMoveAction>(){};
                }
                //todo more action
            }
        }

        return null;
    }
}
