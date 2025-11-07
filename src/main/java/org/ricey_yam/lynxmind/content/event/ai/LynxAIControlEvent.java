package org.ricey_yam.lynxmind.content.event.ai;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.content.action.LynxAction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LynxAIControlEvent {
    protected List<LynxAction> actions = new ArrayList<>();
    protected String plans = "";
}
