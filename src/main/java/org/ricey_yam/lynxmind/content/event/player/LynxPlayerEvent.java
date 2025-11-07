package org.ricey_yam.lynxmind.content.event.player;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.content.event.LynxEvent;

@Getter
@Setter
public class LynxPlayerEvent extends LynxEvent {
    protected LynxPlayerEventType type = LynxPlayerEventType.NONE;
}
