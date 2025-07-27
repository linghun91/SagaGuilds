package cn.i7mc.sagaguild.data.dao;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.DatabaseManager;
import cn.i7mc.sagaguild.data.models.Guild;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 公会数据访问对象
 * 提供公会数据的CRUD操作
 */
public class GuildDAO {
    private final SagaGuild plugin;
    private final DatabaseManager databaseManager;

    public GuildDAO(SagaGuild plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 创建新公会
     * @param guild 公会对象
     * @return 创建的公会ID，失败返回-1
     */
    public int createGuild(Guild guild) {
        String sql = "INSERT INTO guilds (name, tag, description, announcement, owner_uuid, level, experience, is_public, tag_color) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, guild.getName());
            stmt.setString(2, guild.getTag());
            stmt.setString(3, guild.getDescription());
            stmt.setString(4, guild.getAnnouncement());
            stmt.setString(5, guild.getOwnerUuid().toString());
            stmt.setInt(6, guild.getLevel());
            stmt.setInt(7, guild.getExperience());
            stmt.setInt(8, guild.isPublic() ? 1 : 0);
            stmt.setString(9, guild.getTagColor());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    guild.setId(id);

                    // 创建公会银行
                    createGuildBank(id);

                    return id;
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("创建公会失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 创建公会银行
     * @param guildId 公会ID
     * @throws SQLException SQL异常
     */
    private void createGuildBank(int guildId) throws SQLException {
        String sql = "INSERT INTO banks (guild_id, balance, capacity) VALUES (?, 0, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setDouble(2, plugin.getConfig().getDouble("bank.initial-capacity", 10000));

            stmt.executeUpdate();
        }
    }

    /**
     * 根据ID获取公会
     * @param id 公会ID
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildById(int id) {
        String sql = "SELECT * FROM guilds WHERE id = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Connection conn = databaseManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return extractGuildFromResultSet(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会失败: " + e.getMessage());
        } finally {
            // 只关闭语句和结果集，不关闭连接
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭资源失败: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * 根据名称获取公会
     * @param name 公会名称
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildByName(String name) {
        String sql = "SELECT * FROM guilds WHERE name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractGuildFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 根据标签获取公会
     * @param tag 公会标签
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildByTag(String tag) {
        String sql = "SELECT * FROM guilds WHERE tag = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tag);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractGuildFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 根据会长UUID获取公会
     * @param ownerUuid 会长UUID
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildByOwner(UUID ownerUuid) {
        String sql = "SELECT * FROM guilds WHERE owner_uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ownerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractGuildFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取所有公会
     * @return 公会列表
     */
    public List<Guild> getAllGuilds() {
        List<Guild> guilds = new ArrayList<>();
        String sql = "SELECT * FROM guilds";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // 获取连接但不关闭它
            conn = databaseManager.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                guilds.add(extractGuildFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会列表失败: " + e.getMessage());
        } finally {
            // 只关闭语句和结果集，保持连接打开
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                // 不关闭连接: conn.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭资源失败: " + e.getMessage());
            }
        }

        return guilds;
    }

    /**
     * 更新公会信息
     * @param guild 公会对象
     * @return 是否成功
     */
    public boolean updateGuild(Guild guild) {
        String sql = "UPDATE guilds SET name = ?, tag = ?, description = ?, announcement = ?, " +
                     "owner_uuid = ?, level = ?, experience = ?, is_public = ?, tag_color = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            // 获取连接但不关闭它
            conn = databaseManager.getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setString(1, guild.getName());
            stmt.setString(2, guild.getTag());
            stmt.setString(3, guild.getDescription());
            stmt.setString(4, guild.getAnnouncement());
            stmt.setString(5, guild.getOwnerUuid().toString());
            stmt.setInt(6, guild.getLevel());
            stmt.setInt(7, guild.getExperience());
            stmt.setInt(8, guild.isPublic() ? 1 : 0);
            stmt.setString(9, guild.getTagColor());
            stmt.setInt(10, guild.getId());

            success = stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新公会失败: " + e.getMessage());
        } finally {
            // 只关闭语句，保持连接打开
            try {
                if (stmt != null) stmt.close();
                // 不关闭连接: conn.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭资源失败: " + e.getMessage());
            }
        }

        return success;
    }

    /**
     * 删除公会
     * @param id 公会ID
     * @return 是否成功
     */
    public boolean deleteGuild(int id) {
        String sql = "DELETE FROM guilds WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        boolean success = false;

        try {
            // 获取连接但不关闭它
            conn = databaseManager.getConnection();
            stmt = conn.prepareStatement(sql);

            stmt.setInt(1, id);

            success = stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除公会失败: " + e.getMessage());
        } finally {
            // 只关闭语句，保持连接打开
            try {
                if (stmt != null) stmt.close();
                // 不关闭连接: conn.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭资源失败: " + e.getMessage());
            }
        }

        return success;
    }

    /**
     * 从结果集中提取公会对象
     * @param rs 结果集
     * @return 公会对象
     * @throws SQLException SQL异常
     */
    private Guild extractGuildFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String tag = rs.getString("tag");
        String description = rs.getString("description");
        String announcement = rs.getString("announcement");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        int level = rs.getInt("level");
        int experience = rs.getInt("experience");
        boolean isPublic = rs.getInt("is_public") == 1;
        Date createdAt = new Date(rs.getTimestamp("created_at").getTime());
        String tagColor = rs.getString("tag_color");
        if (tagColor == null) {
            tagColor = "7"; // 默认灰色
        }

        return new Guild(id, name, tag, description, announcement, ownerUuid, level, experience, isPublic, createdAt, tagColor);
    }
}
