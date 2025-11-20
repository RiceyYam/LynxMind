package org.ricey_yam.lynxmind.client.ai.message.event.ai.sub;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.config.AIServiceConfig;
import org.ricey_yam.lynxmind.client.ai.message.action.Action;
import org.ricey_yam.lynxmind.client.ai.message.event.ai.AIEvent;

@Getter
@Setter
public class AIControlEvent extends AIEvent {

    @Expose(deserialize = false)
    protected Action action = null;

    protected String plans = "";

    @Override
    public void onReceive(){
        if(!plans.isEmpty()) LynxMindClient.sendModMessage("[" + AIServiceConfig.getInstance().getModel() + "]" + plans);
        if(action != null) action.invoke();
    }
}
