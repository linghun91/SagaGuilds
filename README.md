# SagaGuild 插件开发文档

## 项目概述
SagaGuild 是一个为 Paper 1.20.1 服务器开发的全功能公会插件，提供公会创建、成员管理、领地系统、公会等级、公会战等功能。

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
│   │               │       ├── CreateCommand.java         # 创建公会命令
│   │               │       ├── InfoCommand.java           # 公会信息命令
│   │               │       ├── HelpCommand.java           # 帮助命令
│   │               │       ├── ClaimCommand.java          # 领地声明命令
│   │               │       ├── UnclaimCommand.java        # 取消领地声明命令
│   │               │       ├── BankCommand.java           # 银行命令
│   │               │       ├── ChatCommand.java           # 聊天命令
│   │               │       ├── LevelCommand.java          # 等级命令
│   │               │       ├── TaskCommand.java           # 任务命令
│   │               │       ├── WarCommand.java            # 公会战命令
│   │               │       ├── AllyCommand.java           # 联盟命令
│   │               │       ├── ListCommand.java           # 公会列表命令
│   │               │       ├── JoinCommand.java           # 加入公会命令
│   │               │       ├── ManagerCommand.java        # 公会管理命令
│   │               │       ├── RelationCommand.java       # 公会关系命令
│   │               │       ├── ActivityCommand.java       # 活动命令
│   │               │       ├── AdminCommand.java          # 管理员命令
│   │               │       ├── DisbandCommand.java        # 解散公会命令
│   │               │       ├── InviteCommand.java         # 邀请玩家命令
│   │               │       ├── InviteAcceptCommand.java   # 接受邀请命令
│   │               │       ├── InviteRejectCommand.java   # 拒绝邀请命令
│   │               │       ├── LeaveCommand.java          # 离开公会命令
│   │               │       └── activity/                  # 活动子命令
│   │               │           ├── ActivityCreateCommand.java  # 创建活动命令
│   │               │           ├── ActivityInfoCommand.java    # 活动信息命令
│   │               │           ├── ActivityListCommand.java    # 活动列表命令
│   │               │           ├── ActivityJoinCommand.java    # 加入活动命令
│   │               │           ├── ActivityLeaveCommand.java   # 离开活动命令
│   │               │           └── ActivityCancelCommand.java  # 取消活动命令
│   │               ├── config/                            # 配置系统
│   │               │   └── ConfigManager.java             # 配置管理器
│   │               ├── data/                              # 数据管理
│   │               │   ├── DatabaseManager.java           # 数据库管理器
│   │               │   ├── dao/                           # 数据访问对象
│   │               │   │   ├── GuildDAO.java              # 公会数据访问
│   │               │   │   ├── MemberDAO.java             # 成员数据访问
│   │               │   │   ├── LandDAO.java               # 领地数据访问
│   │               │   │   ├── BankDAO.java               # 银行数据访问
│   │               │   │   ├── WarDAO.java                # 公会战数据访问
│   │               │   │   ├── TaskDAO.java               # 任务数据访问
│   │               │   │   ├── ActivityDAO.java           # 活动数据访问
│   │               │   │   ├── AllianceDAO.java           # 联盟数据访问
│   │               │   │   ├── JoinRequestDAO.java        # 加入请求数据访问
│   │               │   │   └── ParticipantDAO.java        # 参与者数据访问
│   │               │   └── models/                        # 数据模型
│   │               │       ├── Guild.java                 # 公会模型
│   │               │       ├── GuildMember.java           # 公会成员模型
│   │               │       ├── GuildLand.java             # 公会领地模型
│   │               │       ├── GuildWar.java              # 公会战模型
│   │               │       ├── GuildTask.java             # 公会任务模型
│   │               │       ├── GuildActivity.java         # 公会活动模型
│   │               │       ├── Alliance.java              # 联盟模型
│   │               │       ├── AllianceRequest.java       # 联盟请求模型
│   │               │       ├── CeasefireRequest.java      # 停战请求模型
│   │               │       ├── JoinRequest.java           # 加入请求模型
│   │               │       └── ActivityParticipant.java   # 活动参与者模型
│   │               ├── gui/                               # GUI系统
│   │               │   ├── GUIManager.java                # GUI管理器
│   │               │   ├── holders/                       # 物品栏持有者
│   │               │   │   ├── GuildListHolder.java       # 公会列表持有者
│   │               │   │   ├── GuildManageHolder.java     # 公会管理持有者
│   │               │   │   ├── GuildMemberHolder.java     # 公会成员管理持有者
│   │               │   │   ├── GuildMemberActionHolder.java # 公会成员操作持有者
│   │               │   │   ├── GuildRelationHolder.java   # 公会关系持有者
│   │               │   │   ├── GuildRelationManageHolder.java # 公会关系管理持有者
│   │               │   │   ├── JoinRequestHolder.java     # 加入请求持有者
│   │               │   │   └── GuildSettingsHolder.java   # 公会设置持有者
│   │               │   └── listeners/                     # GUI监听器
│   │               │       ├── GuildListListener.java     # 公会列表监听器
│   │               │       ├── GuildManageListener.java   # 公会管理监听器
│   │               │       ├── GuildMemberListener.java   # 公会成员管理监听器
│   │               │       ├── GuildMemberActionListener.java # 公会成员操作监听器
│   │               │       ├── GuildRelationListener.java # 公会关系监听器
│   │               │       ├── GuildRelationManageListener.java # 公会关系管理监听器
│   │               │       ├── JoinRequestListener.java    # 加入请求监听器
│   │               │       └── GuildSettingsListener.java # 公会设置监听器
│   │               ├── listeners/                         # 事件监听器
│   │               │   ├── PlayerListener.java            # 玩家事件监听器
│   │               │   ├── LandListener.java              # 领地事件监听器
│   │               │   ├── ExperienceListener.java        # 经验事件监听器
│   │               │   ├── ChatListener.java              # 聊天事件监听器
│   │               │   ├── WarListener.java               # 公会战事件监听器
│   │               │   ├── TaskListener.java              # 任务事件监听器
│   │               │   └── ActivityListener.java          # 活动事件监听器
│   │               ├── managers/                          # 功能管理器
│   │               │   ├── GuildManager.java              # 公会管理器
│   │               │   ├── MemberManager.java             # 成员管理器
│   │               │   ├── LandManager.java               # 领地管理器
│   │               │   ├── WarManager.java                # 公会战管理器
│   │               │   ├── BankManager.java               # 银行管理器
│   │               │   ├── TaskManager.java               # 任务管理器
│   │               │   ├── ChatManager.java               # 聊天管理器
│   │               │   ├── ActivityManager.java           # 活动管理器
│   │               │   └── AllianceManager.java           # 联盟管理器
│   │               └── utils/                             # 工具类
│   │                   ├── ItemBuilder.java               # 物品构建器
│   │                   ├── ItemUtil.java                  # 物品工具类（兼容性处理）
│   │                   ├── PlayerUtil.java                # 玩家工具类（兼容性处理）
│   │                   ├── TeamUtil.java                  # 团队工具类（兼容性处理）
│   │                   ├── InventoryUtil.java             # 物品栏工具类
│   │                   └── MessageUtil.java               # 消息工具
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
