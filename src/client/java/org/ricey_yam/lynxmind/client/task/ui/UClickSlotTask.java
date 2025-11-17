package org.ricey_yam.lynxmind.client.task.ui;

import net.minecraft.screen.slot.SlotActionType;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlot;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.SlotHelper;

public class UClickSlotTask extends UTask{
    public LSlot l_slot;
    protected boolean clicked;
    protected int button;
    protected SlotActionType slotActionType;
    public UClickSlotTask(LSlot l_slot,int button,SlotActionType slotActionType) {
        this.taskType = UTaskType.CLICK_SLOT;
        this.currentTaskState = TaskState.IDLE;
        this.l_slot = l_slot;
        this.button = button;
        this.slotActionType = slotActionType;
    }
    @Override
    public void start() {
        this.result = UTaskResult.NONE;
        clicked = click();
    }

    @Override
    public void tick() {
        if(result != UTaskResult.NONE) {
            return;
        }
        var slotId = l_slot.getSlotId();
        if(clicked){
            result = UTaskResult.SUCCESS;
            stop("点击格子成功");
        }
        else{
            clicked = click();
            if(!clicked) {
                result = UTaskResult.FAILED;
                stop("点击格子失败 2");
            }
        }
    }

    @Override
    public void stop(String cancelReason) {
        this.currentTaskState = TaskState.FINISHED;
        clicked = false;
        slotActionType = null;
        System.out.println("点击任务结束：" + cancelReason);
    }
    private boolean click(){
        return SlotHelper.clickContainerSlot(l_slot,button, slotActionType);
    }
}
