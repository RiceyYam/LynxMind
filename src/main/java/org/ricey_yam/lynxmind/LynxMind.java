package org.ricey_yam.lynxmind;

import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import org.ricey_yam.lynxmind.ai.ArkServiceManager;
import org.ricey_yam.lynxmind.config.ConfigManager;

public class LynxMind implements ModInitializer {
    @Getter
    private final static String modID = "LynxMind";

    @Override
    public void onInitialize() {

        /// 加载全部配置文件
        ConfigManager.init();

        /// 加载模型调用模块
        ArkServiceManager.init();


    }
}
