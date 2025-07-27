# SagaGuild 插件开发文档

## 项目概述
SagaGuild 是一个为 Paper 1.20.1 服务器开发的全功能公会插件，提供公会创建、成员管理、领地系统、公会等级、公会战等功能。插件具备完整的GUI管理界面，包括领地权限设置、聊天系统配置等高级功能，并完全集成经济系统，支持各项操作的费用管理。

- **包名**: cn.i7mc
- **作者**: Saga
- **Java版本**: 17
- **API**: Paper 1.20.1 (https://jd.papermc.io/paper/1.20.1)
- **数据存储**: SQLite (使用Paper内置驱动)

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── cn/
│   │       └── i7mc/
│   │           └── sagaguild/
│   │               ├── SagaGuild.java                     # 主类
│   │               ├── commands/                          # 命令系统
│   │               │   ├── CommandManager.java            # 命令管理器
│   │               │   ├── GuildCommand.java              # 主命令处理
│   │               │   ├── SubCommand.java                # 子命令接口
│   │               │   └── subcommands/                   # 子命令
│   │               │       ├── ActivityCommand.java       # 活动命令
│   │               │       ├── AdminCommand.java          # 管理员命令
│   │               │       ├── AllyCommand.java           # 联盟命令
│   │               │       ├── BankCommand.java           # 银行命令
│   │               │       ├── ChatCommand.java           # 聊天命令
│   │               │       ├── ClaimCommand.java          # 领地声明命令
│   │               │       ├── CreateCommand.java         # 创建公会命令
│   │               │       ├── DelWarpCommand.java        # 删除公会传送点命令
│   │               │       ├── DisbandCommand.java        # 解散公会命令
│   │               │       ├── HelpCommand.java           # 帮助命令
│   │               │       ├── InfoCommand.java           # 公会信息命令
│   │               │       ├── InviteAcceptCommand.java   # 接受邀请命令
│   │               │       ├── InviteCommand.java         # 邀请玩家命令
│   │               │       ├── InviteRejectCommand.java   # 拒绝邀请命令
│   │               │       ├── JoinCommand.java           # 加入公会命令
│   │               │       ├── LeaveCommand.java          # 离开公会命令
│   │               │       ├── LevelCommand.java          # 等级命令
│   │               │       ├── ListCommand.java           # 公会列表命令
│   │               │       ├── ManagerCommand.java        # 公会管理命令
│   │               │       ├── RelationCommand.java       # 公会关系命令
│   │               │       ├── SetRoleCommand.java        # 设置成员职位命令
│   │               │       ├── SetWarpCommand.java        # 设置公会传送点命令
│   │               │       ├── TagColorCommand.java       # 设置公会标签颜色命令
│   │               │       ├── TaskCommand.java           # 任务命令
│   │               │       ├── UnclaimCommand.java        # 取消领地声明命令
│   │               │       ├── WarCommand.java            # 公会战命令
│   │               │       ├── WarpCommand.java           # 传送到公会传送点命令
│   │               │       └── activity/                  # 活动子命令
│   │               │           ├── ActivityCancelCommand.java  # 取消活动命令
│   │               │           ├── ActivityCreateCommand.java  # 创建活动命令
│   │               │           ├── ActivityInfoCommand.java    # 活动信息命令
│   │               │           ├── ActivityJoinCommand.java    # 加入活动命令
│   │               │           ├── ActivityLeaveCommand.java   # 离开活动命令
│   │               │           └── ActivityListCommand.java    # 活动列表命令
│   │               ├── config/                            # 配置系统
│   │               │   └── ConfigManager.java             # 配置管理器
│   │               ├── data/                              # 数据管理
│   │               │   ├── DatabaseManager.java           # 数据库管理器
│   │               │   ├── dao/                           # 数据访问对象
│   │               │   │   ├── ActivityDAO.java           # 活动数据访问
│   │               │   │   ├── AllianceDAO.java           # 联盟数据访问
│   │               │   │   ├── BankDAO.java               # 银行数据访问
│   │               │   │   ├── GuildDAO.java              # 公会数据访问
│   │               │   │   ├── JoinRequestDAO.java        # 加入请求数据访问
│   │               │   │   ├── LandDAO.java               # 领地数据访问
│   │               │   │   ├── MemberDAO.java             # 成员数据访问
│   │               │   │   ├── ParticipantDAO.java        # 参与者数据访问
│   │               │   │   ├── TaskDAO.java               # 任务数据访问
│   │               │   │   ├── WarDAO.java                # 公会战数据访问
│   │               │   │   └── WarpDAO.java               # 公会传送点数据访问
│   │               │   └── models/                        # 数据模型
│   │               │       ├── ActivityParticipant.java   # 活动参与者模型
│   │               │       ├── Alliance.java              # 联盟模型
│   │               │       ├── AllianceRequest.java       # 联盟请求模型
│   │               │       ├── CeasefireRequest.java      # 停战请求模型
│   │               │       ├── Guild.java                 # 公会模型
│   │               │       ├── GuildActivity.java         # 公会活动模型
│   │               │       ├── GuildLand.java             # 公会领地模型
│   │               │       ├── GuildMember.java           # 公会成员模型
│   │               │       ├── GuildTask.java             # 公会任务模型
│   │               │       ├── GuildWar.java              # 公会战模型
│   │               │       ├── GuildWarp.java             # 公会传送点模型
│   │               │       └── JoinRequest.java           # 加入请求模型
│   │               ├── gui/                               # GUI系统
│   │               │   ├── GUIManager.java                # GUI管理器
│   │               │   ├── holders/                       # 物品栏持有者
│   │               │   │   ├── GuildChatSettingsHolder.java # 公会聊天设置持有者
│   │               │   │   ├── GuildLandSettingsHolder.java # 公会领地设置持有者
│   │               │   │   ├── GuildListHolder.java       # 公会列表持有者
│   │               │   │   ├── GuildManageHolder.java     # 公会管理持有者
│   │               │   │   ├── GuildMemberActionHolder.java # 公会成员操作持有者
│   │               │   │   ├── GuildMemberHolder.java     # 公会成员管理持有者
│   │               │   │   ├── GuildRelationHolder.java   # 公会关系持有者
│   │               │   │   ├── GuildRelationManageHolder.java # 公会关系管理持有者
│   │               │   │   ├── GuildSettingsHolder.java   # 公会设置持有者
│   │               │   │   └── JoinRequestHolder.java     # 加入请求持有者
│   │               │   └── listeners/                     # GUI监听器
│   │               │       ├── GuildChatSettingsListener.java # 公会聊天设置监听器
│   │               │       ├── GuildLandSettingsListener.java # 公会领地设置监听器
│   │               │       ├── GuildListListener.java     # 公会列表监听器
│   │               │       ├── GuildManageListener.java   # 公会管理监听器
│   │               │       ├── GuildMemberActionListener.java # 公会成员操作监听器
│   │               │       ├── GuildMemberListener.java   # 公会成员管理监听器
│   │               │       ├── GuildRelationListener.java # 公会关系监听器
│   │               │       ├── GuildRelationManageListener.java # 公会关系管理监听器
│   │               │       ├── GuildSettingsListener.java # 公会设置监听器
│   │               │       └── JoinRequestListener.java   # 加入请求监听器
│   │               ├── listeners/                         # 事件监听器
│   │               │   ├── ActivityListener.java          # 活动事件监听器
│   │               │   ├── ChatListener.java              # 聊天事件监听器
│   │               │   ├── ExperienceListener.java        # 经验事件监听器
│   │               │   ├── LandListener.java              # 领地事件监听器
│   │               │   ├── PlayerInputListener.java       # 玩家输入监听器
│   │               │   ├── PlayerListener.java            # 玩家事件监听器
│   │               │   ├── TaskListener.java              # 任务事件监听器
│   │               │   └── WarListener.java               # 公会战事件监听器
│   │               ├── managers/                          # 功能管理器
│   │               │   ├── ActivityManager.java           # 活动管理器
│   │               │   ├── AllianceManager.java           # 联盟管理器
│   │               │   ├── BankManager.java               # 银行管理器
│   │               │   ├── ChatManager.java               # 聊天管理器
│   │               │   ├── EconomyManager.java            # 经济系统管理器
│   │               │   ├── GuildManager.java              # 公会管理器
│   │               │   ├── LandManager.java               # 领地管理器
│   │               │   ├── MemberManager.java             # 成员管理器
│   │               │   ├── RankingManager.java            # 排行榜管理器
│   │               │   ├── TaskManager.java               # 任务管理器
│   │               │   ├── WarManager.java                # 公会战管理器
│   │               │   └── WarpManager.java               # 公会传送点管理器
│   │               ├── placeholders/                      # PlaceholderAPI扩展
│   │               │   └── SagaGuildPlaceholders.java     # 占位符扩展类
│   │               └── utils/                             # 工具类
│   │                   ├── GUIUtils.java                  # GUI工具类
│   │                   ├── InventoryUtil.java             # 物品栏工具类
│   │                   ├── ItemBuilder.java               # 物品构建器
│   │                   ├── ItemUtil.java                  # 物品工具类（兼容性处理）
│   │                   ├── MessageUtil.java               # 消息工具类
│   │                   ├── PlayerUtil.java                # 玩家工具类（兼容性处理）
│   │                   └── TeamUtil.java                  # 团队工具类（兼容性处理）
│   └── resources/                                         # 资源文件
│       ├── config.yml                                     # 配置文件
│       ├── messages.yml                                   # 消息文件
│       └── plugin.yml                                     # 插件描述文件
```

## 模块详细说明

### 1. 核心模块 (SagaGuild.java)

主类负责插件的初始化、加载和卸载，管理所有模块的生命周期。

**API URL**: [JavaPlugin](https://jd.papermc.io/paper/1.20.1/org/bukkit/plugin/java/JavaPlugin.html)

```java
public class SagaGuild extends JavaPlugin {
    // 单例模式
    private static SagaGuild instance;

    // 各管理器实例
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GuildManager guildManager;
    private CommandManager commandManager;
    private GUIManager guiManager;
    // 其他管理器...

    @Override
    public void onEnable() {
        // 初始化单例
        instance = this;

        // 初始化配置
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 初始化管理器
        initializeManagers();

        // 注册命令
        commandManager = new CommandManager(this);
        commandManager.registerCommands();

        // 注册监听器
        registerListeners();

        // 初始化GUI系统
        guiManager = new GUIManager(this);
    }

    @Override
    public void onDisable() {
        // 保存数据
        if (databaseManager != null) {
            databaseManager.close();
        }

        // 卸载资源
        // ...
    }

    // 获取单例实例
    public static SagaGuild getInstance() {
        return instance;
    }

    // 获取各管理器的方法
    // ...
}
```

### 2. 数据管理模块 (data/)

#### 2.1 数据库管理器 (DatabaseManager.java)

负责数据库连接和表结构管理。

**API URL**: [SQLite JDBC](https://github.com/xerial/sqlite-jdbc)

```java
public class DatabaseManager {
    private final SagaGuild plugin;
    private Connection connection;

    public DatabaseManager(SagaGuild plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // 创建数据库连接
        // 创建表结构
        createTables();
    }

    private void createTables() {
        // 创建公会表
        // 创建成员表
        // 创建领地表
        // 创建其他必要的表
    }

    public Connection getConnection() {
        // 返回数据库连接
        return connection;
    }

    public void close() {
        // 关闭数据库连接
    }
}
```

#### 2.2 数据模型 (models/)

定义数据实体类，表示数据库中的记录。

```java
public class Guild {
    private int id;
    private String name;
    private String tag;
    private String description;
    private String announcement;
    private UUID ownerId;
    private int level;
    private int experience;
    private boolean isPublic;
    private Date createdAt;

    // 构造函数、getter和setter
}

public class GuildMember {
    private int id;
    private int guildId;
    private UUID playerId;
    private String role; // OWNER, ADMIN, MEMBER, etc.
    private Date joinedAt;

    // 构造函数、getter和setter
}

public class Alliance {
    private int id;
    private int guild1Id;
    private int guild2Id;
    private Date formedAt;

    // 构造函数、getter和setter
}
```

#### 2.3 数据访问对象 (dao/)

提供数据库操作的抽象层，处理CRUD操作。

```java
public class GuildDAO {
    private final DatabaseManager databaseManager;

    public GuildDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void createGuild(Guild guild) {
        // 插入公会记录
    }

    public Guild getGuildById(int id) {
        // 根据ID查询公会
        return null;
    }

    public Guild getGuildByName(String name) {
        // 根据名称查询公会
        return null;
    }

    public List<Guild> getAllGuilds() {
        // 获取所有公会
        return null;
    }

    public void updateGuild(Guild guild) {
        // 更新公会信息
    }

    public void deleteGuild(int id) {
        // 删除公会
    }
}

public class AllianceDAO {
    private final SagaGuild plugin;

    public AllianceDAO(SagaGuild plugin) {
        this.plugin = plugin;
    }

    public int createAlliance(Alliance alliance) {
        // 创建联盟
        return -1;
    }

    public boolean deleteAlliance(int id) {
        // 删除联盟
        return false;
    }

    public Alliance getAllianceById(int id) {
        // 根据ID获取联盟
        return null;
    }

    public List<Alliance> getGuildAlliances(int guildId) {
        // 获取公会的所有联盟
        return null;
    }

    public boolean areGuildsAllied(int guild1Id, int guild2Id) {
        // 检查两个公会是否已结盟
        return false;
    }
}
```

### 3. 公会管理模块 (managers/GuildManager.java)

负责公会的创建、解散和管理。

**API URL**: [Player](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/Player.html)

```java
public class GuildManager {
    private final SagaGuild plugin;
    private final GuildDAO guildDAO;
    private final MemberDAO memberDAO;

    // 缓存公会数据
    private final Map<Integer, Guild> guildsById;
    private final Map<String, Guild> guildsByName;
    private final Map<UUID, Integer> playerGuildMap;

    public GuildManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.guildDAO = new GuildDAO(plugin.getDatabaseManager());
        this.memberDAO = new MemberDAO(plugin.getDatabaseManager());

        this.guildsById = new HashMap<>();
        this.guildsByName = new HashMap<>();
        this.playerGuildMap = new HashMap<>();

        // 加载所有公会数据到缓存
        loadGuilds();
    }

    private void loadGuilds() {
        // 从数据库加载公会数据到缓存
    }

    public Guild createGuild(Player owner, String name, String tag) {
        // 创建新公会
        return null;
    }

    public boolean disbandGuild(Player player, int guildId) {
        // 解散公会
        return false;
    }

    public Guild getGuildById(int id) {
        // 获取公会
        return guildsById.get(id);
    }

    public Guild getGuildByName(String name) {
        // 获取公会
        return guildsByName.get(name.toLowerCase());
    }

    public Guild getPlayerGuild(UUID playerId) {
        // 获取玩家所在的公会
        Integer guildId = playerGuildMap.get(playerId);
        return guildId != null ? guildsById.get(guildId) : null;
    }

    // 其他公会管理方法
}
```

### 4. 命令系统 (commands/)

#### 4.1 命令管理器 (CommandManager.java)

负责注册和管理所有命令。

**API URL**: [CommandExecutor](https://jd.papermc.io/paper/1.20.1/org/bukkit/command/CommandExecutor.html)

```java
public class CommandManager {
    private final SagaGuild plugin;
    private final Map<String, SubCommand> subCommands;

    public CommandManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();

        // 注册子命令
        registerSubCommands();
    }

    private void registerSubCommands() {
        // 注册所有子命令
        registerSubCommand(new CreateCommand(plugin));
        registerSubCommand(new InfoCommand(plugin));
        registerSubCommand(new HelpCommand(plugin));
        registerSubCommand(new ClaimCommand(plugin));
        registerSubCommand(new UnclaimCommand(plugin));
        registerSubCommand(new BankCommand(plugin));
        registerSubCommand(new ChatCommand(plugin));
        registerSubCommand(new LevelCommand(plugin));
        registerSubCommand(new TaskCommand(plugin));
        registerSubCommand(new WarCommand(plugin));
        registerSubCommand(new AllyCommand(plugin));
        registerSubCommand(new ListCommand(plugin));
        registerSubCommand(new JoinCommand(plugin));
        registerSubCommand(new ManagerCommand(plugin));
        registerSubCommand(new RelationCommand(plugin));
        registerSubCommand(new ActivityCommand(plugin));
        registerSubCommand(new AdminCommand(plugin));
        registerSubCommand(new DisbandCommand(plugin));
        registerSubCommand(new InviteCommand(plugin));
        registerSubCommand(new InviteAcceptCommand(plugin));
        registerSubCommand(new InviteRejectCommand(plugin));
        registerSubCommand(new LeaveCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }

    public void registerCommands() {
        // 注册主命令
        plugin.getCommand("guild").setExecutor(new GuildCommand(this));
    }

    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}
```

#### 4.2 子命令接口 (SubCommand.java)

定义子命令的通用接口。

```java
public interface SubCommand {
    String getName();
    String getDescription();
    String getSyntax();
    String[] getAliases();
    boolean execute(Player player, String[] args);
    List<String> tabComplete(Player player, String[] args);
}
```

#### 4.3 子命令详细说明

插件提供了丰富的子命令来管理公会的各项功能：

##### 基础命令
- **CreateCommand** - 创建新公会：`/guild create <名称> <标签>`
- **InfoCommand** - 查看公会信息：`/guild info [公会名称]`
- **HelpCommand** - 显示帮助信息：`/guild help`
- **ListCommand** - 列出所有公会：`/guild list [页码]`

##### 成员管理命令
- **InviteCommand** - 邀请玩家加入公会：`/guild invite <玩家名>`
- **InviteAcceptCommand** - 接受公会邀请：`/guild inviteaccept`
- **InviteRejectCommand** - 拒绝公会邀请：`/guild invitereject`
- **JoinCommand** - 申请加入公会：`/guild join <公会名称>`
- **LeaveCommand** - 离开当前公会：`/guild leave`
- **DisbandCommand** - 解散公会（仅会长）：`/guild disband`

##### 公会管理命令
- **ManagerCommand** - 打开公会管理GUI：`/guild manager`
- **BankCommand** - 管理公会银行：`/guild bank [存入/取出] [金额]`
- **LevelCommand** - 查看或升级公会等级：`/guild level [upgrade]`
- **ChatCommand** - 切换公会聊天模式：`/guild chat`

##### 领地命令
- **ClaimCommand** - 声明当前区块为公会领地：`/guild claim`
- **UnclaimCommand** - 取消当前区块的领地声明：`/guild unclaim`

##### 公会关系命令
- **AllyCommand** - 管理联盟关系：`/guild ally <add/remove> <公会名>`
- **WarCommand** - 管理战争状态：`/guild war <declare/ceasefire> <公会名>`
- **RelationCommand** - 打开公会关系管理GUI：`/guild relation`

##### 传送点命令
- **SetWarpCommand** - 设置公会传送点：`/guild setwarp`
- **WarpCommand** - 传送到公会传送点：`/guild warp`
- **DelWarpCommand** - 删除公会传送点：`/guild delwarp`

##### 成员管理命令
- **SetRoleCommand** - 设置成员职位：`/guild setrole <玩家名> <职位>`
- **TagColorCommand** - 设置公会标签颜色：`/guild tagcolor <颜色>`

##### 活动与任务命令
- **ActivityCommand** - 管理公会活动：`/guild activity`
- **TaskCommand** - 查看和完成公会任务：`/guild task`

##### 管理员命令
- **AdminCommand** - 管理员专用命令（需要权限）：`/guild admin <子命令>`

### 5. GUI系统 (gui/)

#### 5.1 GUI管理器 (GUIManager.java)

负责创建和管理GUI界面。

**API URL**: [InventoryHolder](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/InventoryHolder.html)

```java
public class GUIManager {
    private final SagaGuild plugin;

    public GUIManager(SagaGuild plugin) {
        this.plugin = plugin;

        // 注册GUI监听器
        plugin.getServer().getPluginManager().registerEvents(new GuildListListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildManageListener(plugin), plugin);
        // 注册其他GUI监听器...
    }

    public void openGuildListGUI(Player player, int page) {
        // 创建并打开公会列表GUI
        Inventory inventory = Bukkit.createInventory(new GuildListHolder(page), 54, Component.text("公会列表 - 第 " + page + " 页"));

        // 填充物品
        fillGuildListItems(inventory, page);

        // 打开GUI
        player.openInventory(inventory);
    }

    public void openGuildManageGUI(Player player) {
        // 创建并打开公会管理GUI
    }

    // 其他GUI相关方法
}
```

#### 5.2 物品栏持有者 (holders/)

实现InventoryHolder接口，用于标识不同类型的GUI。

```java
public class GuildListHolder implements InventoryHolder {
    private final int page;

    public GuildListHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null; // 由Bukkit管理
    }
}
```


### 1. 公会加入申请系统

#### 功能描述
公会加入申请系统允许玩家申请加入公会，并由公会管理员审核，提供以下功能：
- 玩家可以在公会列表中申请加入公会，而不是直接加入
- 玩家可以同时申请多个公会，等待审核
- 当某个公会接受申请后，玩家会自动加入该公会，其他申请会被取消
- 公会管理员可以在公会管理界面中查看和处理加入申请
- 公会管理员可以接受或拒绝加入申请

#### 实现内容
1. 创建了数据库表结构
   - `join_requests` 表存储加入请求
2. 创建了模型类
   - `JoinRequest` 类表示加入请求
3. 实现了GUI界面
   - 创建了`JoinRequestHolder`和`JoinRequestListener`类处理加入申请
   - 在公会管理GUI中添加了加入申请管理入口
4. 修改了`GuildListListener`类，将直接加入改为发送申请
5. 在`GuildManager`中添加了处理加入请求的方法
   - 实现了`requestJoinGuild`方法
   - 实现了`acceptJoinRequest`方法
   - 实现了`rejectJoinRequest`方法
6. 在`GUIManager`中添加了`openJoinRequestGUI`方法
7. 优化和修复
   - 修复了重复消息问题，确保申请消息只发送一次
   - 改进了请求ID解析逻辑，增强对不同格式Lore文本的兼容性

### 2. 公会关系系统

#### 功能描述
公会关系系统允许公会之间建立联盟或宣战关系，提供以下功能：
- 公会可以向其他公会发送结盟请求
- 公会可以向其他公会宣战
- 联盟公会成员之间无法互相伤害
- 公会战期间的击杀会在动作条显示并记录积分
- 公会可以请求停战或解除联盟关系
- 公会信息中显示联盟关系和战争状态

#### 实现内容
1. 创建了数据库表结构
   - `alliance_requests` 表存储结盟请求
   - `ceasefire_requests` 表存储停战请求
   - `war_kills` 表记录战争击杀数据
2. 创建了模型类
   - `AllianceRequest` 类表示结盟请求
3. 实现了GUI界面
   - 创建了`GuildRelationHolder`和`GuildRelationListener`类处理关系设置
   - 创建了`GuildRelationManageHolder`和`GuildRelationManageListener`类处理关系管理
4. 修改了`WarListener`类，实现了联盟公会成员互不伤害和战争击杀通知
5. 添加了`RelationCommand`命令处理公会关系操作
6. 在`GUIManager`中添加了关系相关的GUI打开方法
7. 修改了`GuildListListener`类，根据玩家状态显示不同的提示信息

### 3. 公会管理GUI

#### 功能描述
公会管理GUI允许公会会长和管理员管理公会的各项设置和功能。这个GUI提供以下功能：
- 公会基本信息管理（名称、标签、描述、公告等）
- 公会权限设置（是否公开、加入要求等）
- 公会银行管理入口
- 公会领地管理入口
- 公会成员管理入口
- 公会活动管理入口
- 公会任务管理入口
- 公会联盟管理入口
- 公会加入申请管理入口

#### 实现内容
1. 创建了`GuildManageHolder`类，实现`InventoryHolder`接口
2. 实现了`GuildManageListener`类，处理GUI交互事件
3. 在`GUIManager`中完善了`openGuildManageGUI`方法
4. 添加了权限检查，确保只有公会会长和管理员可以访问

### 4. 公会成员管理GUI

#### 功能描述
公会成员管理GUI允许公会会长和管理员管理成员的权限、职位和状态。这个GUI提供以下功能：
- 显示所有公会成员列表（支持分页）
- 成员信息显示（名称、职位、加入时间等）
- 成员职位调整（提升/降级）
- 踢出成员
- 转让会长职位
- 成员权限管理

#### 实现内容
1. 创建了`GuildMemberHolder`类，实现`InventoryHolder`接口
2. 实现了`GuildMemberListener`类，处理GUI交互事件
3. 在`GUIManager`中添加了`openGuildMemberGUI`方法
4. 实现了分页功能，支持大量成员的显示
5. 添加了权限检查，确保操作符合权限要求

### 5. 公会设置GUI

#### 功能描述
公会设置GUI允许公会会长配置公会的各种设置。这个GUI提供以下功能：
- 公会公开性设置（公开/私有）
- 加入要求设置（自由加入/需要邀请/需要申请）
- 公会描述和公告设置
- 公会标签颜色设置
- 公会领地相关设置
- 公会聊天设置

#### 实现内容
1. 创建了`GuildSettingsHolder`类，实现`InventoryHolder`接口
2. 实现了`GuildSettingsListener`类，处理GUI交互事件
3. 在`GUIManager`中添加了`openGuildSettingsGUI`方法
4. 添加了权限检查，确保只有公会会长可以修改关键设置

### 6. 公会邀请系统

#### 功能描述
公会邀请系统允许公会管理层邀请玩家加入公会，提供了一个比申请系统更主动的成员招募方式：
- 公会会长和管理员可以向在线玩家发送邀请
- 被邀请的玩家会收到包含点击按钮的邀请消息
- 玩家可以通过点击消息中的按钮快速接受或拒绝邀请
- 邀请具有时效性，超时后自动失效（默认5分钟）
- 玩家同时只能有一个待处理的邀请
- 接受邀请后会自动加入公会

#### 实现内容
1. 邀请数据管理
   - 使用`ConcurrentHashMap`在内存中存储邀请信息
   - 实现了定时清理过期邀请的机制
   - `InviteInfo`内部类存储邀请详细信息

2. 命令实现
   - `InviteCommand` - 发送邀请命令
   - `InviteAcceptCommand` - 接受邀请命令
   - `InviteRejectCommand` - 拒绝邀请命令

3. 消息交互
   - 使用Adventure API的`ClickEvent`实现可点击消息
   - 邀请消息包含[接受]和[拒绝]按钮
   - 点击按钮自动执行相应命令

4. 权限控制
   - 只有公会会长和管理员可以发送邀请
   - 防止向已有公会的玩家发送邀请
   - 防止重复发送邀请

### 7. 公会管理员功能

#### 功能描述
公会管理员功能为服务器管理员提供了强大的公会管理工具，允许他们在必要时干预和管理所有公会：
- 强制解散违规公会
- 修改公会信息（名称、标签、等级等）
- 管理公会成员（踢出、转让会长等）
- 查看所有公会的详细信息
- 清理无效或过期的公会数据
- 重置公会银行或领地

#### 实现内容
1. AdminCommand命令实现
   - 需要特定权限（sagaguild.admin）
   - 支持多种管理子命令
   - 提供详细的操作日志

2. 支持的管理操作
   - `/guild admin disband <公会名>` - 强制解散公会
   - `/guild admin setowner <公会名> <玩家名>` - 转让会长
   - `/guild admin setlevel <公会名> <等级>` - 设置公会等级
   - `/guild admin info <公会名>` - 查看详细信息
   - `/guild admin list` - 列出所有公会

3. 安全措施
   - 所有操作都需要管理员权限验证
   - 重要操作会记录到日志
   - 支持操作确认机制防止误操作

4. 数据管理
   - 可以直接操作数据库
   - 支持批量操作
   - 提供数据备份和恢复功能

## 兼容性工具类

为了确保插件在不同服务端环境中的兼容性，我们开发了一系列工具类来处理API差异。

### 1. ItemUtil (物品工具类)

处理物品元数据的兼容性问题，包括设置和获取物品名称、描述等。

```java
public class ItemUtil {
    // 初始化方法，检测服务器支持的API
    private static void initialize() {
        // 尝试获取使用Component的displayName方法
        try {
            adventureDisplayNameMethod = ItemMeta.class.getMethod("displayName", Component.class);
            useAdventure = true;
        } catch (NoSuchMethodException e) {
            useAdventure = false;
            // 尝试获取传统的setDisplayName方法
            legacyDisplayNameMethod = ItemMeta.class.getMethod("setDisplayName", String.class);
        }
    }

    // 设置物品显示名称
    public static void setDisplayName(ItemMeta meta, Component displayName) {
        // 根据服务器支持的API选择适当的方法
    }

    // 设置物品描述
    public static void setLore(ItemMeta meta, List<Component> lore) {
        // 根据服务器支持的API选择适当的方法
    }
}
```

### 2. PlayerUtil (玩家工具类)

处理玩家消息发送的兼容性问题，确保在所有服务端上都能正常显示消息。

```java
public class PlayerUtil {
    // 初始化方法，检测服务器支持的API
    private static void initialize() {
        // 尝试获取使用Component的sendMessage方法
        try {
            adventureSendMessageMethod = Player.class.getMethod("sendMessage", Component.class);
            useAdventure = true;
        } catch (NoSuchMethodException e) {
            useAdventure = false;
            // 尝试获取传统的sendMessage方法
            legacySendMessageMethod = Player.class.getMethod("sendMessage", String.class);
        }
    }

    // 发送消息给玩家
    public static void sendMessage(Player player, Component component) {
        // 根据服务器支持的API选择适当的方法
    }

    // 发送字符串消息给玩家
    public static void sendMessage(Player player, String message) {
        // 转换为Component后发送
    }
}
```

### 3. TeamUtil (团队工具类)

处理团队前缀设置的兼容性问题，用于公会标签显示。

```java
public class TeamUtil {
    // 初始化方法，检测服务器支持的API
    private static void initialize() {
        // 尝试获取Team.prefix(Component)方法
        try {
            Team.class.getMethod("prefix", Component.class);
            useAdventure = true;
        } catch (NoSuchMethodException e) {
            useAdventure = false;
            // 尝试获取传统的Team.setPrefix(String)方法
            legacyPrefixMethod = Team.class.getMethod("setPrefix", String.class);
        }
    }

    // 设置团队前缀
    public static void setPrefix(Team team, Component prefix) {
        // 根据服务器支持的API选择适当的方法
    }
}
```

## API参考

### 核心API
- [JavaPlugin](https://jd.papermc.io/paper/1.20.1/org/bukkit/plugin/java/JavaPlugin.html)
- [CommandExecutor](https://jd.papermc.io/paper/1.20.1/org/bukkit/command/CommandExecutor.html)
- [TabCompleter](https://jd.papermc.io/paper/1.20.1/org/bukkit/command/TabCompleter.html)
- [Listener](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/Listener.html)

### 玩家相关API
- [Player](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/Player.html)
- [OfflinePlayer](https://jd.papermc.io/paper/1.20.1/org/bukkit/OfflinePlayer.html)

### GUI相关API
- [Inventory](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/Inventory.html)
- [InventoryHolder](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/InventoryHolder.html)
- [ItemStack](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/ItemStack.html)
- [ItemMeta](https://jd.papermc.io/paper/1.20.1/org/bukkit/inventory/meta/ItemMeta.html)

### 事件相关API
- [InventoryClickEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/inventory/InventoryClickEvent.html)
- [PlayerJoinEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/player/PlayerJoinEvent.html)
- [AsyncChatEvent](https://jd.papermc.io/paper/1.20.1/io/papermc/paper/event/player/AsyncChatEvent.html)

### 领地相关API
- [Chunk](https://jd.papermc.io/paper/1.20.1/org/bukkit/Chunk.html)
- [Location](https://jd.papermc.io/paper/1.20.1/org/bukkit/Location.html)
- [BlockBreakEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/block/BlockBreakEvent.html)
- [BlockPlaceEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/block/BlockPlaceEvent.html)
- [PlayerInteractEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/player/PlayerInteractEvent.html)

### 公会战相关API
- [EntityDamageByEntityEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/entity/EntityDamageByEntityEvent.html)
- [PlayerDeathEvent](https://jd.papermc.io/paper/1.20.1/org/bukkit/event/entity/PlayerDeathEvent.html)

### 调度器API
- [BukkitScheduler](https://jd.papermc.io/paper/1.20.1/org/bukkit/scheduler/BukkitScheduler.html)
- [BukkitTask](https://jd.papermc.io/paper/1.20.1/org/bukkit/scheduler/BukkitTask.html)

### 文本和消息API
- [Component](https://jd.advntr.dev/api/4.14.0/net/kyori/adventure/text/Component.html)
- [TextComponent](https://jd.advntr.dev/api/4.14.0/net/kyori/adventure/text/TextComponent.html)
- [Title](https://jd.papermc.io/paper/1.20.1/org/bukkit/entity/Player.html#sendTitle(net.kyori.adventure.title.Title))

### 交互事件API
- [ClickEvent](https://jd.advntr.dev/api/4.14.0/net/kyori/adventure/text/event/ClickEvent.html)
- [HoverEvent](https://jd.advntr.dev/api/4.14.0/net/kyori/adventure/text/event/HoverEvent.html)
- [ClickEvent.Action](https://jd.advntr.dev/api/4.14.0/net/kyori/adventure/text/event/ClickEvent.Action.html)

### 8. PlaceholderAPI 占位符系统

#### 功能描述
PlaceholderAPI 占位符系统为 SagaGuild 提供了与其他插件的集成能力，允许在聊天、记分板、菜单等地方显示公会信息。系统提供以下功能：
- 玩家公会信息占位符 - 显示玩家当前所在公会的各种信息
- 公会排行榜占位符 - 显示公会在各种排行榜中的排名和数据
- 支持离线玩家查询 - 即使玩家不在线也能获取其公会信息
- 优雅的错误处理 - 所有占位符在失败时返回空字符串，不会显示错误信息
- 高性能缓存机制 - 排行榜数据使用5分钟缓存，避免频繁数据库查询

#### 支持的占位符

##### 玩家公会信息占位符
- `%sg_currentguild_name%` - 玩家所在公会名称
- `%sg_currentguild_tag%` - 玩家所在公会标签
- `%sg_currentguild_description%` - 玩家所在公会描述
- `%sg_currentguild_level%` - 玩家所在公会等级
- `%sg_currentguild_role%` - 玩家在公会的职位（OWNER/ADMIN/MEMBER等）
- `%sg_currentguild_owner%` - 玩家所在公会会长名称
- `%sg_currentguild_currentmembers%` - 玩家所在公会当前人数
- `%sg_currentguild_maxmembers%` - 玩家所在公会最大人数
- `%sg_currentguild_money%` - 玩家所在公会银行余额
- `%sg_currentguild_warstats%` - 玩家所在公会战争状态（和平/战争中）
- `%sg_currentguild_allystats%` - 玩家所在公会联盟关系（无联盟/已结盟）

##### 公会排行榜占位符
排行榜占位符支持动态排名数字，格式为：`%sg_top[排名]_[类型]_[数据]%`

###### 银行资金排行榜
- `%sg_top1_money_gname%` - 银行资金第1名公会名称
- `%sg_top1_money_value%` - 银行资金第1名公会的资金值
- 支持 top1 到 top10 的任意排名

###### 成员数量排行榜
- `%sg_top3_members_gname%` - 成员数量第3名公会名称
- `%sg_top3_members_value%` - 成员数量第3名公会的人数
- 支持 top1 到 top10 的任意排名

###### 公会等级排行榜
- `%sg_top5_level_gname%` - 等级第5名公会名称
- `%sg_top5_level_value%` - 等级第5名公会的等级值
- 支持 top1 到 top10 的任意排名

###### 公会战争胜利次数排行榜
- `%sg_top1_war_gname%` - 等级第1名公会名称
- `%sg_top1_war_value%` - 等级第1名公会的战争胜利次数
- 支持 top1 到 top10 的任意排名

#### 实现内容
1. 创建了排行榜管理器
   - `RankingManager` 类管理所有排行榜数据
   - 实现了银行余额、成员数量、等级三种排行榜
   - 使用 ConcurrentHashMap 缓存排行榜数据
   - 缓存时间为5分钟，自动过期刷新

2. PlaceholderAPI 扩展实现
   - `SagaGuildPlaceholders` 类继承自 PlaceholderExpansion
   - 注册标识符为 `sg`（符合官方规范）
   - 统一处理所有占位符，使用标准格式
   - 使用正则表达式解析动态排行榜占位符

3. 与主插件集成
   - 在 plugin.yml 中添加了 PlaceholderAPI 软依赖
   - 在插件启动时自动检测并注册扩展
   - 在 messages.yml 中添加了可配置的状态文本

4. 性能优化
   - 排行榜数据使用缓存机制，减少数据库查询
   - 支持批量获取数据，提高查询效率
   - 使用流式处理和Lambda表达式优化代码

5. 兼容性处理
   - 修复了与现有管理器的集成问题
   - 确保在 PlaceholderAPI 不存在时插件仍能正常运行
   - 支持离线玩家数据查询

#### PlaceholderAPI 扩展修复（v1.0.9.21）

经过测试发现占位符功能未能正常工作，显示原始占位符文本而非实际数值。通过深入分析 PlaceholderAPI 官方文档和成功案例，识别并修复了以下关键问题：

##### 修复内容：
1. **扩展标识符规范化**：
   - 从过长的 `sagaguild` 改为简洁的 `sg`
   - 符合 PlaceholderAPI 官方最佳实践

2. **占位符格式标准化**：
   - 统一使用标准格式：`%sg_currentguild_name%`
   - 移除非标准的冒号分隔格式

3. **解析逻辑重构**：
   - 移除错误的前缀分离处理
   - 按照官方规范直接处理完整占位符名称
   - 优化排行榜占位符格式为 `%sg_top1_money_gname%`

4. **技术改进**：
   - 使用推荐的 `onRequest(OfflinePlayer, String)` 方法
   - 简化占位符解析逻辑，提高性能
   - 增强兼容性，支持不同版本的 PlaceholderAPI

修复后的占位符现在能够正确显示实际数值，完全符合 PlaceholderAPI 标准实现规范。

### 9. 领地设置GUI系统

#### 功能描述
领地设置GUI系统为公会管理员提供了直观的领地权限管理界面，允许长老及以上权限的成员配置公会领地的各项设置。该系统提供以下功能：
- 领地保护开关 - 控制是否保护领地免受破坏
- PVP设置开关 - 控制领地内是否允许PVP战斗
- 访客权限开关 - 控制非公会成员是否可进入领地
- 建筑权限开关 - 控制建筑权限的范围设置
- 一键重置功能 - 快速恢复所有设置为默认值

#### 实现内容
1. **GUI持有者** (`GuildLandSettingsHolder.java`)
   - 实现InventoryHolder接口，用于标识领地设置GUI
   - 存储关联的公会对象信息
   - 提供GUI识别和数据传递功能

2. **GUI监听器** (`GuildLandSettingsListener.java`)
   - 处理领地设置GUI中的所有点击事件
   - 实现设置开关的切换逻辑
   - 权限验证确保只有长老及以上可操作
   - 配置数据的保存和读取

3. **GUI界面设计**
   - 54格子的专业GUI界面
   - 直观的物品图标表示各项设置
   - 实时显示当前设置状态（启用/禁用）
   - 返回按钮提供便捷的导航体验

4. **权限控制**
   - 长老及以上权限方可访问
   - 每次操作都进行权限验证
   - 未授权访问时自动关闭界面并提示

#### 访问路径
公会设置GUI → 领地设置按钮 → 领地设置GUI

### 10. 聊天设置GUI系统

#### 功能描述
聊天设置GUI系统为公会提供了完整的聊天功能配置界面，支持公会管理员自定义各种聊天相关设置。该系统提供以下功能：
- 公会聊天开关 - 启用/禁用公会内部聊天功能
- 联盟聊天开关 - 启用/禁用与联盟公会的聊天功能
- 聊天过滤开关 - 启用/禁用不当内容过滤功能
- 自动公会聊天 - 新成员加入时自动进入公会聊天模式
- 聊天格式自定义 - 可完全自定义聊天消息的显示格式
- 聊天前缀自定义 - 可设置专属的公会聊天前缀
- 一键重置功能 - 快速恢复所有聊天设置为默认值

#### 实现内容
1. **GUI持有者** (`GuildChatSettingsHolder.java`)
   - 实现InventoryHolder接口，用于标识聊天设置GUI
   - 存储关联的公会对象信息
   - 提供GUI识别和数据传递功能

2. **GUI监听器** (`GuildChatSettingsListener.java`)
   - 处理聊天设置GUI中的所有交互事件
   - 实现各种聊天开关的切换逻辑
   - 集成PlayerInputListener处理文本输入
   - 配置数据的实时保存和刷新

3. **文本输入集成**
   - 与PlayerInputListener集成处理格式和前缀输入
   - 支持输入验证和长度限制
   - 输入取消机制和超时处理
   - 输入完成后自动返回设置界面

4. **配置管理**
   - 所有设置存储在config.yml中
   - 支持每个公会独立的配置项
   - 实时配置保存，立即生效
   - 重置功能清除所有自定义配置

#### 访问路径
公会设置GUI → 聊天设置按钮 → 聊天设置GUI

### 11. 经济系统集成增强

#### 领地声明费用系统
经济系统现已完全集成到领地声明功能中，提供以下增强：
- **费用检查** - 声明前自动检查玩家余额是否充足
- **费用扣除** - 根据config.yml中的`land.claim-cost`配置自动扣除费用
- **失败退还** - 当声明失败时自动退还已扣除的费用
- **优雅降级** - 当经济系统未启用时提供友好的错误提示

#### 公会创建费用系统
公会创建功能也已集成完整的费用检查机制：
- **创建费用** - 根据config.yml中的`guild.creation-cost`配置收取费用
- **余额验证** - 创建前检查玩家是否有足够资金
- **失败处理** - 创建失败时自动退还扣除的费用
- **错误处理** - 完善的费用验证和用户友好的错误提示

### 12. 联盟聊天功能完善

#### 功能增强
联盟聊天系统已得到全面完善，现在提供以下增强功能：
- **多联盟支持** - 消息会自动发送给所有联盟公会的成员
- **在线状态检查** - 只向在线成员发送消息，提高性能
- **联盟关系获取** - 自动获取当前公会的所有联盟关系
- **消息广播** - 确保所有相关成员都能收到联盟聊天消息

#### 实现细节
- 在ChatManager的`sendAllianceMessage`方法中实现
- 使用AllianceManager获取联盟公会列表
- 遍历所有联盟公会的在线成员发送消息
- 保持与公会内部聊天一致的消息格式

### 13. 经济系统管理器 (EconomyManager)

#### 功能描述
经济系统管理器提供与Vault经济系统的集成，为公会功能提供经济支持：
- 与主流经济插件兼容（如EssentialsX Economy、CMI等）
- 公会银行存取操作的经济后端支持
- 公会创建、升级等功能的费用扣除
- 公会活动和任务的奖励发放
- 优雅的经济插件检测和错误处理

#### 核心功能
- **经济系统检测** - 自动检测服务器上的经济插件
- **余额查询** - 获取玩家经济余额
- **资金转移** - 处理玩家与公会银行间的资金流转
- **费用扣除** - 处理各种公会功能的费用扣除
- **奖励发放** - 自动发放任务和活动奖励

#### 实现内容
1. **Vault集成**
   - 检测Vault插件是否存在
   - 自动连接到可用的经济服务提供者
   - 支持多种经济插件的统一API

2. **核心方法实现**
   ```java
   public boolean hasEnough(OfflinePlayer player, double amount)
   public boolean withdraw(OfflinePlayer player, double amount)  
   public boolean deposit(OfflinePlayer player, double amount)
   public double getBalance(OfflinePlayer player)
   ```

3. **错误处理**
   - 当Vault或经济插件缺失时优雅降级
   - 详细的错误日志记录
   - 操作失败时的用户友好提示

### 10. 公会传送点系统

#### 功能描述
公会传送点系统允许公会设置专属传送点，方便成员快速聚集：
- 公会可以设置一个传送点位置
- 成员可以快速传送到公会传送点
- 支持权限控制，防止滥用传送功能
- 传送点信息持久化存储
- 传送冷却时间和安全检查

#### 核心功能
- **传送点设置** - 副会长及以上权限可设置传送点
- **传送点传送** - 公会成员可传送到设置的传送点  
- **传送点删除** - 管理员可删除现有传送点
- **安全检查** - 传送前检查目标位置安全性
- **冷却控制** - 防止频繁传送的冷却机制

#### 实现内容
1. **数据模型** (`GuildWarp.java`)
   - 存储传送点的完整坐标信息（世界、坐标、朝向）
   - 记录创建时间和创建者信息
   - 与公会ID关联

2. **数据访问** (`WarpDAO.java`) 
   - 传送点数据的增删改查操作
   - 数据库表结构管理
   - 数据持久化保证

3. **业务逻辑** (`WarpManager.java`)
   - 传送点创建和验证逻辑
   - 传送安全性检查
   - 冷却时间管理

4. **命令实现**
   - `SetWarpCommand` - 设置传送点：`/guild setwarp`
   - `WarpCommand` - 传送到传送点：`/guild warp`  
   - `DelWarpCommand` - 删除传送点：`/guild delwarp`

5. **安全特性**
   - 传送前检查目标位置是否安全（非虚空、非岩浆等）
   - 权限验证确保只有授权成员可操作
   - 传送冷却防止滥用

## 最新功能更新 (v1.0.9.45)

### v1.0.9.45 版本稳定性增强

#### 版本更新
- **当前版本**: v1.0.9.45
- **构建系统**: Gradle 自动版本管理
- **依赖更新**: Adventure API 4.23.0
- **兼容性**: Paper 1.20.1-R0.1-SNAPSHOT

#### 技术改进

1. **构建系统优化**
   - 实现了自动版本增量系统
   - 构建过程自动更新版本号
   - 优化了Shadow JAR打包配置

2. **依赖管理增强**
   - 更新Adventure API到4.23.0版本
   - 改进了PlaceholderAPI集成方式
   - 优化了Vault依赖处理

3. **代码结构优化**
   - 完善了单例模式实现
   - 改进了管理器初始化顺序
   - 优化了监听器注册机制

### v1.0.9.37 重要Bug修复版本

#### 修复内容

1. **聊天系统修复**
   - ✅ 修复了公会聊天频道显示MemorySection而非实际消息内容的问题
   - ✅ 修复了聊天监听器在文本输入后不自动结束监听的问题
   - ✅ 修复了会长转让确认后聊天监听器状态未清除的问题

2. **GUI系统修复**
   - ✅ 修复了异步线程中打开GUI导致的IllegalStateException错误
   - ✅ 修复了成员管理GUI返回按钮无效的问题
   - ✅ 修复了GUI切换时鼠标自动移动到屏幕中央的问题

3. **命令系统修复**
   - ✅ 修复了/g ally命令参数索引错误导致的功能异常
   - ✅ 改进了命令参数处理逻辑，确保索引正确

4. **占位符系统修复**
   - ✅ 修复了公会战胜利次数占位符不更新的问题
   - ✅ 战争结束后排行榜数据会立即刷新

#### 技术改进

1. **线程安全增强**
   - 所有GUI操作现在都在主线程中执行
   - 使用Bukkit.getScheduler().runTask()确保线程安全

2. **状态管理优化**
   - 使用try-finally机制确保聊天监听器状态正确清除
   - 避免监听器状态残留导致的功能异常

3. **用户体验提升**
   - GUI刷新时不再关闭再打开，避免鼠标位置重置
   - 成员管理界面增加返回按钮，改善导航体验

## 历史功能更新 (v1.0.9.36)

### 新增功能

#### 1. 领地设置GUI系统
- ✅ 完整的领地权限管理界面
- ✅ 领地保护、PVP设置、访客权限、建筑权限开关
- ✅ 一键重置功能
- ✅ 长老及以上权限访问控制
- ✅ 实时配置保存和状态显示

#### 2. 聊天设置GUI系统  
- ✅ 完整的聊天功能配置界面
- ✅ 公会聊天、联盟聊天、聊天过滤等开关设置
- ✅ 自定义聊天格式和前缀功能
- ✅ 与PlayerInputListener集成的文本输入系统
- ✅ 配置重置和实时保存功能

#### 3. 经济系统集成增强
- ✅ 领地声明费用检查和自动扣除系统
- ✅ 公会创建费用验证和处理机制
- ✅ 操作失败时的自动费用退还功能
- ✅ 经济系统未启用时的优雅降级处理

#### 4. 联盟聊天功能完善
- ✅ 消息自动发送给所有联盟公会成员
- ✅ 多联盟支持和在线状态检查
- ✅ 完善的联盟关系获取和消息广播

### 技术改进

#### 代码结构优化
- ✅ 新增4个GUI相关类文件（2个Holder + 2个Listener）
- ✅ 完善的模块化设计和权限控制
- ✅ 统一的消息管理和错误处理

#### 配置系统增强
- ✅ messages.yml新增15+个配置项
- ✅ 支持每个公会独立的设置配置
- ✅ 实时配置保存和加载机制

#### 用户体验提升
- ✅ 直观的GUI界面设计和状态显示
- ✅ 完善的权限验证和操作反馈
- ✅ 用户友好的错误提示和操作指导

### 构建信息
- **版本**: v1.0.9.45
- **构建状态**: ✅ 成功
- **Java兼容性**: Java 17+ 
- **Paper版本**: 1.20.1
- **最新更新**: 版本稳定性增强
- **技术改进**: 构建系统、依赖管理、代码结构优化
