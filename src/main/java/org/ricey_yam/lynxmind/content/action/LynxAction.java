package org.ricey_yam.lynxmind.content.action;

import lombok.Getter;
import lombok.Setter;
import org.ricey_yam.lynxmind.baritone.BaritoneManager;

@Getter
@Setter
public class LynxAction {
    private LynxActionType type;
    public boolean invoke(){
        return true;
    }

    protected void stopAllActions() {
        var baritone = BaritoneManager.getClientBaritone();
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
    }
}
