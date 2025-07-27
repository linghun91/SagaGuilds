package cn.i7mc.sagaguild.data;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.MemberDAO;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 数据库管理器
 * 负责数据库连接和表结构管理
 */
public class DatabaseManager {
    private final SagaGuild plugin;
    private Connection connection;
    private MemberDAO memberDAO;

    public DatabaseManager(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据库
     */
    public void initialize() {
        // 创建数据库目录
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // 获取数据库路径
        FileConfiguration config = plugin.getConfig();
        String dbPath = config.getString("database.path", "plugins/SagaGuild/database.db");
        File dbFile = new File(dbPath);

        // 确保数据库目录存在
        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
        }

        // 连接数据库
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // 创建表结构
            createTables();

            // 设置连接属性
            connection.setAutoCommit(true);
            
            // 初始化 DAO
            memberDAO = new MemberDAO(plugin);

            // 只在首次连接时输出成功信息
            plugin.getLogger().info("数据库连接成功！");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("数据库连接失败: " + e.getMessage());
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() {
        try {
            // 创建公会表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS guilds (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "tag TEXT NOT NULL UNIQUE," +
                "description TEXT," +
                "announcement TEXT," +
                "owner_uuid TEXT NOT NULL," +
                "level INTEGER DEFAULT 1," +
                "experience INTEGER DEFAULT 0," +
                "is_public INTEGER DEFAULT 1," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // 创建成员表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "player_name TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "UNIQUE(guild_id, player_uuid)" +
                ")"
            );

            // 创建领地表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS lands (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild_id INTEGER NOT NULL," +
                "world TEXT NOT NULL," +
                "chunk_x INTEGER NOT NULL," +
                "chunk_z INTEGER NOT NULL," +
                "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "UNIQUE(world, chunk_x, chunk_z)" +
                ")"
            );

            // 创建银行表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS banks (" +
                "guild_id INTEGER PRIMARY KEY," +
                "balance REAL DEFAULT 0," +
                "capacity REAL DEFAULT 10000," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                ")"
            );

            // 创建联盟表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS alliances (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild1_id INTEGER NOT NULL," +
                "guild2_id INTEGER NOT NULL," +
                "formed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (guild1_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "FOREIGN KEY (guild2_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "UNIQUE(guild1_id, guild2_id)" +
                ")"
            );

            // 创建战争表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS wars (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "attacker_id INTEGER NOT NULL," +
                "defender_id INTEGER NOT NULL," +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "end_time TIMESTAMP," +
                "winner_id INTEGER," +
                "status TEXT DEFAULT 'PENDING'," +
                "FOREIGN KEY (attacker_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "FOREIGN KEY (defender_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "FOREIGN KEY (winner_id) REFERENCES guilds(id) ON DELETE SET NULL" +
                ")"
            );

            // 创建任务表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild_id INTEGER NOT NULL," +
                "type TEXT NOT NULL," +
                "description TEXT NOT NULL," +
                "target INTEGER NOT NULL," +
                "progress INTEGER DEFAULT 0," +
                "reward_exp INTEGER NOT NULL," +
                "reward_money REAL NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "expires_at TIMESTAMP," +
                "completed_at TIMESTAMP," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                ")"
            );

            // 创建活动表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS activities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "type TEXT NOT NULL," +
                "creator_uuid TEXT NOT NULL," +
                "start_time TIMESTAMP NOT NULL," +
                "end_time TIMESTAMP NOT NULL," +
                "location TEXT NOT NULL," +
                "max_participants INTEGER DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status TEXT DEFAULT 'PLANNED'," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                ")"
            );

            // 创建活动参与者表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS activity_participants (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "activity_id INTEGER NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "player_name TEXT NOT NULL," +
                "status TEXT DEFAULT 'REGISTERED'," +
                "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE," +
                "UNIQUE(activity_id, player_uuid)" +
                ")"
            );

            // 创建联盟请求表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS alliance_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "requester_id INTEGER NOT NULL," +
                "target_id INTEGER NOT NULL," +
                "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status TEXT DEFAULT 'PENDING'," +
                "FOREIGN KEY (requester_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "FOREIGN KEY (target_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "UNIQUE(requester_id, target_id)" +
                ")"
            );

            // 创建停战请求表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS ceasefire_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "requester_id INTEGER NOT NULL," +
                "target_id INTEGER NOT NULL," +
                "war_id INTEGER NOT NULL," +
                "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status TEXT DEFAULT 'PENDING'," +
                "FOREIGN KEY (requester_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "FOREIGN KEY (target_id) REFERENCES guilds(id) ON DELETE CASCADE," +
                "FOREIGN KEY (war_id) REFERENCES wars(id) ON DELETE CASCADE," +
                "UNIQUE(requester_id, target_id, war_id)" +
                ")"
            );

            // 创建战争击杀记录表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS war_kills (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "war_id INTEGER NOT NULL," +
                "killer_uuid TEXT NOT NULL," +
                "victim_uuid TEXT NOT NULL," +
                "kill_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (war_id) REFERENCES wars(id) ON DELETE CASCADE" +
                ")"
            );

            // 创建公会加入请求表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS join_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid TEXT NOT NULL," +
                "player_name TEXT NOT NULL," +
                "guild_id INTEGER NOT NULL," +
                "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "status TEXT DEFAULT 'PENDING'," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                // 移除唯一约束，允许玩家对同一公会发送多个请求
                // ", UNIQUE(player_uuid, guild_id)" +
                ")"
            );
            
            // 创建公会传送点表
            executeUpdate(
                "CREATE TABLE IF NOT EXISTS guild_warps (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "guild_id INTEGER NOT NULL UNIQUE," +
                "world TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "yaw REAL NOT NULL," +
                "pitch REAL NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "creator_uuid TEXT NOT NULL," +
                "FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE" +
                ")"
            );

            // 数据库迁移：添加tag_color字段
            migrateDatabase();

        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据库表失败: " + e.getMessage());
        }
    }

    /**
     * 数据库迁移
     */
    private void migrateDatabase() {
        try {
            // 检查是否需要添加tag_color字段
            boolean hasTagColor = false;
            try (var rs = connection.getMetaData().getColumns(null, null, "guilds", "tag_color")) {
                if (rs.next()) {
                    hasTagColor = true;
                }
            }
            
            if (!hasTagColor) {
                plugin.getLogger().info("添加tag_color字段到guilds表...");
                executeUpdate("ALTER TABLE guilds ADD COLUMN tag_color TEXT DEFAULT '7'");
                plugin.getLogger().info("tag_color字段添加成功！");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("数据库迁移失败: " + e.getMessage());
        }
    }

    /**
     * 执行更新操作
     * @param sql SQL语句
     * @throws SQLException SQL异常
     */
    private void executeUpdate(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    /**
     * 获取数据库连接
     * 注意：此方法返回的是一个持久连接，调用者不应该关闭它
     * @return 数据库连接
     */
    public Connection getConnection() {
        try {
            // 检查连接是否有效
            if (connection == null || connection.isClosed() || !isConnectionValid()) {
                // 重新连接数据库
                reconnect();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("检查数据库连接失败: " + e.getMessage());
            // 尝试重新连接
            reconnect();
        }

        // 添加额外的连接有效性检查
        try {
            if (connection != null && !connection.isClosed() && connection.isValid(1)) {
                return connection;
            } else {
                plugin.getLogger().warning("数据库连接无效，尝试重新连接");
                reconnect();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("验证数据库连接失败: " + e.getMessage());
            reconnect();
        }

        return connection;
    }

    /**
     * 检查数据库连接是否有效
     * @return 连接是否有效
     */
    private boolean isConnectionValid() {
        try {
            // 使用isValid方法检查连接是否有效，超时时间为1秒
            return connection != null && connection.isValid(1);
        } catch (SQLException e) {
            plugin.getLogger().severe("检查数据库连接有效性失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 重新连接数据库
     */
    private void reconnect() {
        try {
            // 关闭旧连接
            if (connection != null) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    // 忽略关闭错误
                }
            }

            // 获取数据库路径
            FileConfiguration config = plugin.getConfig();
            String dbPath = config.getString("database.path", "plugins/SagaGuild/database.db");
            File dbFile = new File(dbPath);

            // 创建新连接
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());


        } catch (SQLException e) {
            plugin.getLogger().severe("数据库重新连接失败: " + e.getMessage());
        }
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();


                }
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取成员 DAO
     * @return MemberDAO 实例
     */
    public MemberDAO getMemberDAO() {
        return memberDAO;
    }
}
