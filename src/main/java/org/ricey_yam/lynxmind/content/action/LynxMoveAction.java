package org.ricey_yam.lynxmind.content.action;

import lombok.Getter;
import lombok.Setter;
import baritone.api.pathing.goals.GoalBlock;
import org.ricey_yam.lynxmind.baritone.BaritoneManager;

@Getter
@Setter
public class LynxMoveAction extends LynxAction {
    private int x;
    private int y;
    private int z;

    @Override
    public boolean invoke() {
        /// 先停下其他动作
        stopAllActions();

        /// 开始寻路
        var baritone = BaritoneManager.getClientBaritone();
        var goal = new GoalBlock(x, y, z);
        baritone.getCustomGoalProcess().setGoalAndPath(goal);

        return super.invoke();
    }
}
