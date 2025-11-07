package org.ricey_yam.lynxmind.config;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import org.ricey_yam.lynxmind.LynxMind;

@Getter
@Setter
public class ArkServiceConfig {

    @Getter
    private static ArkServiceConfig instance;

    private static final String fileName = "ArkServiceConfig.yml";
    private static String configDir;

    private String api_key;
    private String model;

    /// 保存配置文件
    public static void save(){
        ConfigManager.saveConfig(configDir,fileName);
    }

    /// 加载配置文件
    public static void load(){
        configDir = FabricLoader.getInstance().getConfigDir().resolve(LynxMind.getModID()).resolve(fileName).toString();
        instance = ConfigManager.loadConfig(configDir, ArkServiceConfig.class);
    }
}
