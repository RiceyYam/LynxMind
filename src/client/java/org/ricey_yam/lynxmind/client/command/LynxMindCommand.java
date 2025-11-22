package org.ricey_yam.lynxmind.client.command;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.ricey_yam.lynxmind.client.LynxMindClient;
import org.ricey_yam.lynxmind.client.ai.AIServiceManager;
import org.ricey_yam.lynxmind.client.ai.ChatManager;
import org.ricey_yam.lynxmind.client.ai.LynxJsonHandler;
import org.ricey_yam.lynxmind.client.ai.message.action.sub.*;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerScanBlockEvent;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerScanEntityEvent;
import org.ricey_yam.lynxmind.client.event.LynxMindEndTickEventManager;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.LTaskType;
import org.ricey_yam.lynxmind.client.task.non_temp.lynx.sub.LAutoStrikeBackTask;
import org.ricey_yam.lynxmind.client.task.temp.baritone.BEntityCollectionTask;
import org.ricey_yam.lynxmind.client.utils.game_ext.item.ItemStackLite;
import org.ricey_yam.lynxmind.client.baritone.BaritoneManager;
import org.ricey_yam.lynxmind.client.config.AIServiceConfig;
import org.ricey_yam.lynxmind.client.ai.message.event.player.sub.PlayerStatusHeartBeatEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LynxMindCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("lynx")

                .then(CommandManager.literal("start")
                        .executes(AIServiceExecutor::connectToAIService)
                )
                .then(CommandManager.literal("stop")
                        .executes(AIServiceExecutor::disconnect)
                )
                .then(CommandManager.literal("pause")
                        .executes(AIServiceExecutor::removeTaskForAI)
                )
                .then(CommandManager.literal("task")
                        .then(CommandManager.argument("你想做什么？", StringArgumentType.string())
                                .executes(AIServiceExecutor::setTaskForAI))
                )

                .then(CommandManager.literal("reload")
                        .executes(LynxMindCommand::reloadConfigByCommand)
                )

                .then(CommandManager.literal("run")
                        .then(CommandManager.literal("cancel")
                                .executes(BaritoneExecutor::stop)
                        )
                        .then(CommandManager.literal("path")
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(BaritoneExecutor::pathTo))
                        )
                        .then(CommandManager.literal("mine")
                                .then(CommandManager.argument("count", IntegerArgumentType.integer())
                                    .then(CommandManager.argument("item_id", StringArgumentType.greedyString())
                                        .executes(BaritoneExecutor::mine)
                                    )
                                )
                        )
                        .then(CommandManager.literal("craft")
                                .then(CommandManager.argument("count", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("item_id", StringArgumentType.greedyString())
                                                .executes(BaritoneExecutor::craft))
                                )
                        )
                        .then(CommandManager.literal("kfc")
                                .then(CommandManager.argument("kq_json", StringArgumentType.greedyString())
                                        .executes(BaritoneExecutor::killingForCollection))
                        )
                        .then(CommandManager.literal("murder")
                                .then(CommandManager.argument("uuids", StringArgumentType.greedyString())
                                        .executes(BaritoneExecutor::murder))
                        )
                )

                .then(CommandManager.literal("debug")
                        .then(CommandManager.literal("status")
                                .executes(DebugExecutor::getStatus)
                        )
                        .then(CommandManager.literal("scan_block")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer())
                                    .then(CommandManager.argument("block_id", StringArgumentType.greedyString())
                                            .executes(DebugExecutor::scanBlockNearby)
                                    )
                                )
                        )
                        .then(CommandManager.literal("scan_entity")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("entity_id", StringArgumentType.greedyString())
                                                .executes(DebugExecutor::scanEntityNearby)
                                        )
                                )
                        )
                        .then(CommandManager.literal("auto_killaura")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(DebugExecutor::autoKillaura)
                                )
                        )
                )
        );
    }
    static class AIServiceExecutor {
        private static int connectToAIService(CommandContext<ServerCommandSource> context) {
            if(!AIServiceManager.isServiceActive){
                AIServiceManager.openServiceAsync();
                LynxMindClient.sendModMessage("正在连接到AI服务......");
            }
            else{
                LynxMindClient.sendModMessage("当前AI服务处于开启状态，无需重复开启！");
            }
            return 1;
        }

        private static int disconnect(CommandContext<ServerCommandSource> context) {
            if(AIServiceManager.isServiceActive || !AIServiceManager.getChatMessages().isEmpty()){
                AIServiceManager.closeServiceAsync();
                LynxMindClient.sendModMessage("正在断开AI服务连接......");
            }
            else{
                LynxMindClient.sendModMessage("当前AI服务处于关闭状态，无需重复关闭！");
            }
            return 1;
        }

        private static int setTaskForAI(CommandContext<ServerCommandSource> context){
            /// 停止先前的任务
            AIServiceManager.stopTask("终止了先前任务创建的 BTask");

            var taskDesc = StringArgumentType.getString(context,"你想做什么？");
            AIServiceManager.setCurrentTask(taskDesc);

            if(AIServiceManager.isServiceActive && !AIServiceManager.getChatMessages().isEmpty()){
                /// 新任务
                LynxMindClient.sendModMessage("创建新任务：" + taskDesc);
                ChatManager.sendTaskMessageToAIAndReceiveReply().whenComplete((reply,throwable) ->{
                    if(throwable != null){
                        System.out.println("处理消息时异常！" + throwable.getMessage());
                    }
                    else{
                        ChatManager.handleAIReply(reply);
                    }
                });
            }
            else{
                LynxMindClient.sendModMessage("未连接到AI服务，请输入/lynx start");
            }

            return 1;
        }

        private static int removeTaskForAI(CommandContext<ServerCommandSource> context){
            BaritoneManager.stopAllTasks("玩家手动取消了当前任务");
            if(AIServiceManager.isServiceActive && !AIServiceManager.getChatMessages().isEmpty()){
                ChatManager.sendRemovingTaskMessageToAIAndReceiveReply();
                LynxMindClient.sendModMessage("已删除任务：" + AIServiceManager.getCurrentTask());
                AIServiceManager.setCurrentTask("");
            }
            else{
                LynxMindClient.sendModMessage("未连接到AI服务，请输入/lynx start");
            }

            return 1;
        }
    }
    static class BaritoneExecutor{
        private static int stop(CommandContext<ServerCommandSource> context){
            try{
                BaritoneManager.stopAllTasks("玩家手动取消了当前任务");
                return 1;
            }
            catch(Exception e){
                System.out.println("取消任务时出错：" + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        }
        private static int pathTo(CommandContext<ServerCommandSource> context){
            var pos = BlockPosArgumentType.getBlockPos(context,"pos");
            var x = pos.getX();
            var y = pos.getY();
            var z = pos.getZ();
            var newPathAction = new PlayerMoveAction(x,y,z);
            newPathAction.invoke();
            return 1;
        }
        private static int mine(CommandContext<ServerCommandSource> context){
            try {
                var count = IntegerArgumentType.getInteger(context,"count");
                var items = StringArgumentType.getString(context,"item_id");
                if (items == null || items.trim().isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: 请指定要挖掘的方块类型！");
                    return 0;
                }
                var item_id_list = Arrays.asList(items.trim().split("\\s+"));
                if (item_id_list.isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: 方块列表不能为空！");
                    return 0;
                }
                List<ItemStackLite> collectingPlan = new ArrayList<>();
                for(var i : item_id_list){
                    collectingPlan.add(new ItemStackLite(count,i));
                }
                var newMiningAction = new PlayerCollectBlockAction(collectingPlan);
                newMiningAction.invoke();
                return 1;
            }
            catch (Exception e) {
                LynxMindClient.sendModMessage("§c执行挖掘命令时出错: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        }
        private static int craft(CommandContext<ServerCommandSource> context){
            try {
                var count = IntegerArgumentType.getInteger(context,"count");
                var items = StringArgumentType.getString(context,"item_id");
                if (items == null || items.trim().isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: 请指定要制作的物品！");
                    return 0;
                }
                var item_id_list = Arrays.asList(items.trim().split("\\s+"));
                if (item_id_list.isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: 物品列表不能为空！");
                    return 0;
                }
                List<ItemStackLite> collectingPlan = new ArrayList<>();
                for(var i : item_id_list){
                    collectingPlan.add(new ItemStackLite(count,i));
                }
                var newCreateAction = new PlayerCraftingAction(collectingPlan);
                newCreateAction.invoke();
                return 1;
            }
            catch (Exception e) {
                LynxMindClient.sendModMessage("§c执行制作命令时出错: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        }
        private static int killingForCollection(CommandContext<ServerCommandSource> context){
            try {
                var gson = new Gson();
                var kqss = StringArgumentType.getString(context,"kq");
                if (kqss == null || kqss.trim().isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: 请指定JSON!");
                    return 0;
                }
                var kq_list = Arrays.asList(kqss.trim().split("\\s+"));
                if (kq_list.isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: JSON列表不能为空！");
                    return 0;
                }
                var kqs = new ArrayList<BEntityCollectionTask.EntityKillingQuota>();
                for (int i = 0; i < kq_list.size(); i++) {
                    var kq = kq_list.get(i);
                    var newKQ = gson.fromJson(kq, BEntityCollectionTask.EntityKillingQuota.class);
                    kqs.add(newKQ);
                }

                var newCollectLootAction = new PlayerCollectEntityLootAction(kqs);
                newCollectLootAction.invoke();
                return 1;
            }
            catch (Exception e) {
                LynxMindClient.sendModMessage("§c执行制作命令时出错: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        }
        private static int murder(CommandContext<ServerCommandSource> context){
            try {
                var uuids = StringArgumentType.getString(context,"uuids");
                if (uuids == null || uuids.trim().isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: 请指定要击杀的目标!");
                    return 0;
                }
                var uuid_list = Arrays.asList(uuids.trim().split("\\s+"));
                if (uuid_list.isEmpty()) {
                    LynxMindClient.sendModMessage("§c错误: UUID列表不能为空！");
                    return 0;
                }
                var newMurderAction = new PlayerMurderAction(uuid_list);
                newMurderAction.invoke();
                return 1;
            }
            catch (Exception e) {
                LynxMindClient.sendModMessage("§c执行制作命令时出错: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        }
    }

    static class DebugExecutor{
        private static int getStatus(CommandContext<ServerCommandSource> context){
            try{
                var playerStatusHeartBeatEvent = new PlayerStatusHeartBeatEvent();
                var serialized = LynxJsonHandler.serialize(playerStatusHeartBeatEvent);
                LynxMindClient.sendModMessage("当前玩家状态\n" + serialized);
            }
            catch(Exception e){
                System.out.println("查询玩家状态时遇到错误：" + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
        private static int scanBlockNearby(CommandContext<ServerCommandSource> context){
            try{
                var radius = IntegerArgumentType.getInteger(context,"radius");
                var blockIDs =  StringArgumentType.getString(context,"block_id");
                var block_id_list = Arrays.asList(blockIDs.trim().split("\\s+"));

                var playerScanBlockEvent = new PlayerScanBlockEvent(radius,block_id_list);
                var serialized = LynxJsonHandler.serialize(playerScanBlockEvent);
                LynxMindClient.sendModMessage("附近方块\n" + serialized);
            }
            catch(Exception e){
                System.out.println("扫描附近方块时遇到错误：" + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
        private static int scanEntityNearby(CommandContext<ServerCommandSource> context){
            try{
                var radius = IntegerArgumentType.getInteger(context,"radius");
                var entityIds =  StringArgumentType.getString(context,"entity_id");
                var entity_id_list = Arrays.asList(entityIds.trim().split("\\s+"));

                var playerScanEntityEvent = new PlayerScanEntityEvent(radius,entity_id_list);
                var serialized = LynxJsonHandler.serialize(playerScanEntityEvent);
                LynxMindClient.sendModMessage("附近实体\n" + serialized);
            }
            catch(Exception e){
                System.out.println("扫描附近实体时遇到错误：" + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
        private static int autoKillaura(CommandContext<ServerCommandSource> context){
            try{
                var enabled = BoolArgumentType.getBool(context,"enabled");
                if(enabled){
                    LynxMindEndTickEventManager.registerTask(new LAutoStrikeBackTask(5,10));
                    LynxMindClient.sendModMessage("自动杀戮光环已开启!");
                }
                else{
                    LynxMindEndTickEventManager.unregisterTask(LTaskType.AUTO_STRIKE_BACK,"COMMAND");
                    LynxMindClient.sendModMessage("自动杀戮光环已关闭!");
                }
            }
            catch(Exception e){
                System.out.println("自动杀戮光环开启失败: " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    private static int reloadConfigByCommand(CommandContext<ServerCommandSource> context){
        AIServiceConfig.load();
        LynxMindClient.sendModMessage("配置文件已重载！");
        return 1;
    }
}
