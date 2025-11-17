package org.ricey_yam.lynxmind.client.utils.game_ext.interaction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Constructor;

/// 容器工具类
public class ContainerHelper {

    /// 当前容器是否开启
    public static boolean isContainerOpen() {
        var client = MinecraftClient.getInstance();
        return client != null && client.player != null && client.currentScreen != null;
    }
    public static boolean isContainerOpen(Class<? extends Screen> targetMenuClass) {
        var client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return targetMenuClass.isInstance(client.currentScreen);
    }

    /// 关闭当前打开的容器
    public static void closeContainer() {

        var client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        var screen = MinecraftClient.getInstance().currentScreen;
        if (screen != null && !(screen instanceof GameMenuScreen) && !(screen instanceof GameOptionsScreen) && !(screen instanceof ChatScreen)) {
            client.player.closeHandledScreen();
        }
    }

    /// 打开容器（无法打开有交互方块的UI（工作台/熔炉等））
    public static void openContainer(Class<? extends Screen> targetMenuClass) {
        var client = MinecraftClient.getInstance();
        var player = client.player;

        if (player != null) {
            /// 如果当前有打开的容器，先关闭
            if (isContainerOpen()) {
                closeContainer();
            }

            try {
                Constructor<? extends Screen> constructor = targetMenuClass.getConstructor(PlayerEntity.class);
                Screen newScreen = constructor.newInstance(player);
                client.setScreen(newScreen);

            }
            catch (Exception e) {
                System.out.println("打开容器时出现错误：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
