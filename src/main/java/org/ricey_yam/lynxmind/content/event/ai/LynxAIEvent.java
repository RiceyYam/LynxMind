package org.ricey_yam.lynxmind.content.event.ai;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.content.event.LynxEvent;


@Getter
@Setter
public class LynxAIEvent extends LynxEvent {
    protected LynxAIEventType type = LynxAIEventType.NONE;
}
