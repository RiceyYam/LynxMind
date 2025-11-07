package org.ricey_yam.lynxmind.ai;

import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.service.ArkService;
import org.ricey_yam.lynxmind.config.ArkServiceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArkServiceManager {
    private static final String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    private static ArkService arkService = null;

    /// 存储聊天消息 包含用户
    private static final List<ChatMessage> chatMessages = new ArrayList<>();

    public static void init(){
        chatMessages.clear();
        arkService = ArkService.builder()
                .connectionPool(new ConnectionPool(5, 1, TimeUnit.SECONDS))
                .dispatcher(new Dispatcher())
                .apiKey(ArkServiceConfig.getInstance().getApi_key())
                .build();
    }
    public static String sendAndReceiveMessage(String message){
        var chatMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(message).build();
        chatMessages.add(chatMessage);
        var awa = "";

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(ArkServiceConfig.getInstance().getModel())
                .messages(chatMessages)
                .build();
        arkService.createChatCompletion(chatCompletionRequest).getChoices().forEach(choice -> {
            System.out.println("awa");
        });
        return null;
    }
}
