# LynxMind

LynxMind 是一个基于 Fabric 框架开发的 Minecraft 模组，专注于提供智能化的游戏辅助功能，提升玩家的游戏体验。


## 项目信息

- **模组ID**：lynx_mind
- **版本**：0,1-beta
- **支持的 Minecraft 版本**：1.20.4（定义于 gradle.properties）

## 功能特点

- **AI 服务集成**：通过 `AIServiceManager` 实现与外部 AI 服务的交互，支持消息发送与异步回复处理
- **配置管理系统**：
  - 由 `ConfigManager` 负责配置文件的加载、保存，支持 YAML 格式，自动创建父目录
  - `AIServiceConfig` 专门管理 AI 服务相关配置（如 API 地址、令牌等），提供 `save()` 方法快速保存配置
- **数据生成支持**：实现 `LynxMindDataGenerator` 集成 Fabric 数据生成系统，用于处理模组数据生成逻辑
- **模块化入口**：基于 Fabric 入口点机制，分别实现客户端（`LynxMindClient`）与服务端（`LynxMind`）初始化逻辑


## 依赖项

- **核心依赖**：
  - Minecraft: ${minecraft_version}
  - Yarn Mappings: ${yarn_mappings}
  - Fabric Loader: >=${loader_version}
  - Fabric API: ${fabric_version}
- **第三方库**：
  - Baritone API（fabric 适配版本，通过本地 libs 目录引入）
  - SnakeYAML 2.2（配置文件处理）
  - Jackson Databind 2.15.3（JSON 处理）
  - Nether Pathfinder 1.4.1
  - Lombok 1.18.42（注解处理，需启用注解处理器）


## 构建与运行

### 构建项目
```bash
./gradlew build
```
构建完成后，模组 JAR 文件位于 `build/libs` 目录下
