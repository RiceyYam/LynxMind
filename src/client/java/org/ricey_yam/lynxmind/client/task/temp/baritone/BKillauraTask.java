package org.ricey_yam.lynxmind.client.task.temp.baritone;

import baritone.api.pathing.goals.GoalNear;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.temp.ui.UClickSlotTask;
import org.ricey_yam.lynxmind.client.task.temp.ui.UTask;
import org.ricey_yam.lynxmind.client.utils.game_ext.ClientUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.EntityUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.entity.PlayerUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ComplexContainerType;
import org.ricey_yam.lynxmind.client.utils.game_ext.interaction.ContainerHelper;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemUtils;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.InventoryHotBarSlot;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlot;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.LSlotType;
import org.ricey_yam.lynxmind.client.utils.game_ext.slot.SlotHelper;

import java.util.*;

@Getter
@Setter
public class BKillauraTask extends BTask{
    public enum KillauraState {
        ATTACKING,
        PATHING_TO_TARGET,
        SWITCHING_TO_WEAPON
    }
    private KillauraState killauraState;

    /// 攻击范围
    private float attackRange;
    /// 反击对象
    private List<UUID> strikeBackTargetList = new ArrayList<>();
    /// 必杀榜
    private List<UUID> mustKillTargetList = new ArrayList<>();
    /// 当前攻击目标
    private LivingEntity currentTarget;

    private float hitTickTimer;
    /// 攻击间隔(取决于当前武器)
    private float hitTickDelay;

    /// 最佳武器格子
    private LSlot bestWeaponLSlot;
    /// 最佳武器物品ID
    private String bestWeaponID;
    /// 武器是否在快捷栏
    private boolean weaponInHotbar;

    /// 将武器转移到快捷栏的UTask
    private List<UTask> switchingToWeaponUTasks = new ArrayList<>();
    private UTask performingUTask;

    public BKillauraTask(float attackRange,List<UUID> mustKillTargetList){
        this(attackRange);
        this.mustKillTargetList = mustKillTargetList;
    }
    public BKillauraTask(float attackRange){
        super();
        this.weight = 2;
        this.hitTickDelay = 5;
        strikeBackTargetList.clear();
        killauraState = KillauraState.PATHING_TO_TARGET;
        setTaskType(BTaskType.KILLAURA);
        this.attackRange = attackRange;
    }
    @Override
    public void start() {
        this.currentTaskState = TaskState.IDLE;
    }
    @Override
    public void tick() {
        tickTimer++;
        hitTickTimer++;
        switch(killauraState){
            /// 寻路到目标附近
            case PATHING_TO_TARGET -> pathingToTargetTick();

            /// 开始攻击
            case ATTACKING -> attackingTick();

            /// 切换到武器
            case SWITCHING_TO_WEAPON -> switchingToWeaponTick();
        }
    }
    @Override
    public void stop(String cancelReason) {
        this.currentTaskState = TaskState.FINISHED;
        strikeBackTargetList.clear();
        mustKillTargetList.clear();
        attackRange = 0;
    }
    @Override
    public void pause() {
        this.currentTaskState = TaskState.PAUSED;
        baritone.getCustomGoalProcess().setGoal(null);
        baritone.getPathingBehavior().cancelEverything();
    }

    private void transitionToPathingToTarget(){
        this.killauraState = KillauraState.PATHING_TO_TARGET;
    }
    private void transitionToAttacking(){
        ContainerHelper.closeContainer();
        this.killauraState = KillauraState.ATTACKING;
    }
    private void transitionToSwitchingToWeapon(){
        this.killauraState = KillauraState.SWITCHING_TO_WEAPON;
        if(holdingBestWeapon()) {
            transitionToAttacking();
            return;
        }
        bestWeaponID = PlayerUtils.getBestWeaponIDInInventory();
        bestWeaponLSlot = SlotHelper.getLSlotByItemID(bestWeaponID,ComplexContainerType.PLAYER_INFO);
        if(bestWeaponLSlot != null) {
            weaponInHotbar = bestWeaponLSlot.getSlotType() == LSlotType.INVENTORY_HOTBAR;
            if(!weaponInHotbar){
                createClickSlotUTask();
            }
        }
    }
    private void pathingToTargetTick(){
        if(baritone == null || baritone.getLookBehavior() == null) return;
        /// 获取反击对象
        var nearbyEnemies = EntityUtils.scanAllEntity(getPlayer(), LivingEntity.class,50, e -> (strikeBackTargetList != null && strikeBackTargetList.contains(e.getUuid())) || (mustKillTargetList != null && mustKillTargetList.contains(e.getUuid())));
        if(nearbyEnemies == null) nearbyEnemies = new ArrayList<>();

        /// 没有反击对象 获取必杀目标
        if(!mustKillTargetList.isEmpty() && nearbyEnemies.isEmpty()){
            for (int i = 0; i < mustKillTargetList.size(); i++) {
                var uuid = mustKillTargetList.get(i);
                var entity = EntityUtils.getEntityByUUID(uuid);

                if (entity != null && entity.getHealth() > 0) {
                    nearbyEnemies.add(entity);
                }
                else {
                    mustKillTargetList.remove(i);
                    i--;
                }
            }
        }

        currentTarget = nearbyEnemies.stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(getPlayer())))
                .orElse(null);

        var goalProcess = baritone.getCustomGoalProcess();

        if(isNearbyTarget()){
            goalProcess.setGoal(null);
            baritone.getPathingBehavior().cancelEverything();
            this.killauraState = KillauraState.ATTACKING;
        }
        else if(!targetDied() && goalProcess != null){
            var targetGoal = new GoalNear(currentTarget.getBlockPos(), 2);
            goalProcess.setGoalAndPath(targetGoal);
        }
        else if(currentTarget == null) {
            //System.out.println("No nearby enemies found");
        }
    }
    private void attackingTick(){
        if(!isNearbyTarget()){
            transitionToPathingToTarget();
            return;
        }

        var goalProcess = baritone.getCustomGoalProcess();
        if(goalProcess != null && goalProcess.isActive()) {
            goalProcess.setGoal(null);
            baritone.getPathingBehavior().cancelEverything();
        }

        var options = ClientUtils.getOptions();
        if(options == null) return;

        if(!holdingBestWeapon()){
            transitionToSwitchingToWeapon();
            return;
        }
        if(!targetDied()){
            if(hitTickTimer >= hitTickDelay){
                ClientUtils.getController().attackEntity(getPlayer(), currentTarget);
                getPlayer().swingHand(Hand.MAIN_HAND);
                resetAttackCooldown(PlayerUtils.getHoldingItemStack());
            }
        }
        else transitionToPathingToTarget();
    }
    private void switchingToWeaponTick(){
        if(holdingBestWeapon()){
            transitionToAttacking();
            return;
        }
        if(bestWeaponLSlot == null) {
            return;
        }
        weaponInHotbar = bestWeaponLSlot.getSlotType() == LSlotType.INVENTORY_HOTBAR;
        if(!weaponInHotbar){
            if(ContainerHelper.isContainerOpen(InventoryScreen.class)) {
                if(switchingToWeaponUTasks != null && !switchingToWeaponUTasks.isEmpty()){
                    switchingToWeaponUTasks.removeIf(uTask -> uTask.getCurrentTaskState() != TaskState.IDLE);
                    if(performingUTask == null){
                        performingUTask = switchingToWeaponUTasks.get(0);
                        LynxMindEndTickEventManager.registerTask(performingUTask);
                    }
                    else if(performingUTask.getResult() != UTask.UTaskResult.NONE){
                        switch (performingUTask.getResult()) {
                            case SUCCESS -> performingUTask = null;
                            case FAILED -> createClickSlotUTask();
                        }
                    }
                }
                else{
                    createClickSlotUTask();
                }
            }
            else {
                createClickSlotUTask();
                ContainerHelper.openContainer(InventoryScreen.class);
            }
        }
        else{
            ContainerHelper.closeContainer();
            SlotHelper.switchToHotbarItem(bestWeaponID);
        }
    }

    /// 重置攻击间隔
    private void resetAttackCooldown(ItemStack holdingItem){
        var itemID = ItemUtils.getItemID(holdingItem);
        this.hitTickDelay = PlayerUtils.getAttackingCooldownTick(itemID);
        this.hitTickTimer = 0;
    }

    /// 背包有合适的武器
    private boolean hasSuitableWeapon(){
        bestWeaponID = PlayerUtils.getBestWeaponIDInInventory();
        return bestWeaponID != null && !bestWeaponID.isEmpty() && !bestWeaponID.contains("air");
    }

    /// 创建UTask来拿出武器
    private void createClickSlotUTask(){
        ContainerHelper.closeContainer();
        bestWeaponLSlot = SlotHelper.getLSlotByItemID(bestWeaponID, ComplexContainerType.PLAYER_INFO);
        performingUTask = null;
        switchingToWeaponUTasks.clear();
        var quickLSlotID = SlotHelper.getQuickHotbarLSlotIDForTool(bestWeaponID);
        var u1 = new UClickSlotTask(bestWeaponLSlot,0, SlotActionType.PICKUP);
        var u2 =  new UClickSlotTask(new InventoryHotBarSlot(quickLSlotID,ComplexContainerType.PLAYER_INFO),0, SlotActionType.PICKUP);
        var u3 = new UClickSlotTask(bestWeaponLSlot,0, SlotActionType.PICKUP);
        switchingToWeaponUTasks.add(u1);
        switchingToWeaponUTasks.add(u2);
        switchingToWeaponUTasks.add(u3);
    }

    /// 是否握着最佳武器
    private boolean holdingBestWeapon(){
        if(!hasSuitableWeapon()) return true;
        var holdingItemStack = PlayerUtils.getHoldingItemStack();
        return ItemUtils.getItemID(holdingItemStack).equals(bestWeaponID);
    }

    /// 是否在攻击范围内
    private boolean isNearbyTarget(){
        if(currentTarget == null) return false;
        return getPlayer().distanceTo(currentTarget) <= 3f;
    }

    private boolean targetDied(){
        return currentTarget == null || currentTarget.getHealth() <= 0;
    }
}
