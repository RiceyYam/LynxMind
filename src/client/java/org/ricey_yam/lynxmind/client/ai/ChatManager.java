package org.ricey_yam.lynxmind.client.ai;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.config.AIServiceConfig;
import org.ricey_yam.lynxmind.client.ai.message.event.ai.AIEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerCreateTaskEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerRemoveTaskEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerStatusHeartBeatEvent;
import org.ricey_yam.lynxmind.client.utils.format.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ChatManager {
    private static final int wrongReplyFormatLimit = 3;
    private static int wrongReplyFormatTriedCount = 0;
    /// 处理收到的AI信息
    public static void handleAIReply(String reply){
        /// 格式正确
        if(StringUtils.isJson(reply)){
            wrongReplyFormatTriedCount = 0;
            var deserialized = LynxJsonHandler.deserialize(reply);
            if(deserialized != null){
                if(deserialized instanceof AIEvent aiEvent){
                    aiEvent.onReceive();
                }
            }
        }
        /// 格式错误
        else if(reply != null && !reply.isEmpty()){
            if(wrongReplyFormatTriedCount >= wrongReplyFormatLimit){
                /// AI连续多次返回错误格式 为防止消耗过多Token 将自动关闭当前对话
                LynxMindClient.sendModMessage("AI连续" + wrongReplyFormatLimit + "次出现返回格式错误.已自动关闭当前AI服务，请重新开始!\n Is " + AIServiceConfig.getInstance().getModel() + " stupid? :(");
                System.out.println("怎么会有这么蠢的AI？规则告诉它好几次 它就不看（ ");
                AIServiceManager.closeServiceAsync();
                return;
            }
            System.out.println("错误的AI回复格式：" + reply);
            wrongReplyFormatTriedCount++;
            sendHelpMessageToAIAndReceiveReply().whenComplete((replyAsync,throwable) -> handleAIReply(replyAsync));
        }
    }

    /// 发送开始信息给AI
    public static CompletableFuture<String> sendStartMessageToAIAndReceiveReply(){
        System.out.println("正在尝试开启服务，并介绍游戏规则给AI");
        return AIServiceManager.sendAndReceiveReplyAsync(UserMessageStorage.startMessage);
    }

    /// 发送帮助信息给AI
    public static CompletableFuture<String> sendHelpMessageToAIAndReceiveReply(){
        System.out.println("正在重新介绍游戏规则给AI...");
        return AIServiceManager.sendAndReceiveReplyAsync(UserMessageStorage.helpMessage);
    }

    /// 给AI布置新任务
    public static CompletableFuture<String> sendTaskMessageToAIAndReceiveReply(){
        var playerCreateTaskEvent = new PlayerCreateTaskEvent(AIServiceManager.getCurrentTask());
        var serialized = LynxJsonHandler.serialize(playerCreateTaskEvent);
        return AIServiceManager.sendAndReceiveReplyAsync(serialized);
    }

    /// 让AI忘记任务
    public static CompletableFuture<String> sendRemovingTaskMessageToAIAndReceiveReply(){
        var playerRemoveTaskEvent = new PlayerRemoveTaskEvent();
        var serialized = LynxJsonHandler.serialize(playerRemoveTaskEvent);
        return AIServiceManager.sendAndReceiveReplyAsync(serialized);
    }

    /// 发送状态信息给AI
    public static CompletableFuture<String> sendStatusJsonToAIAndReceiveReply(){
        var baritone = BaritoneManager.getClientBaritone();
        var playerStatusHeartBeatEvent = new PlayerStatusHeartBeatEvent();
        var serialized = LynxJsonHandler.serialize(playerStatusHeartBeatEvent);
        return AIServiceManager.sendAndReceiveReplyAsync(serialized);
    }

}
@SuppressWarnings("CallToPrintStackTrace")
class UserMessageStorage {
    private static final String schemasMDPath = "docs/schemas.md";
    private static String rule = "";

    public static final String startMessage = "请忘记我们之前全部对话，现在来玩个游戏，请认真阅读规则，并严格遵守：\n" + getSchemasMD();
    public static final String helpMessage = "输出格式不对，你只能输出纯Json！不准出现除Json以外的内容，否则你的消息会反序列化失败！请阅读规则后重新回复： \n" + getSchemasMD();

    /// 获取规则
    public static String getSchemasMD(){
        if(rule.isEmpty()) {
            var identifier = new Identifier(LynxMindClient.getModID(), schemasMDPath);
            var resourceManager = MinecraftClient.getInstance().getResourceManager();
            try {
                var resource = resourceManager.getResource(identifier);
                if (resource.isPresent()) {
                    InputStream inputStream = resource.get().getInputStream();
                    rule = new String(inputStream.readAllBytes(),StandardCharsets.UTF_8);
                    System.out.println(rule);
                    return rule;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rule;
    }
}