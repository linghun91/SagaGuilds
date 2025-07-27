package cn.i7mc.sagaguild.data.dao;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.DatabaseManager;
import cn.i7mc.sagaguild.data.models.GuildWarp;

import java.sql.*;
import java.util.UUID;

/**
 * 公会传送点数据访问对象
 * 提供公会传送点的CRUD操作
 */
public class WarpDAO {
    private final SagaGuild plugin;
    private final DatabaseManager databaseManager;

    public WarpDAO(SagaGuild plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 创建或更新公会传送点
     * @param warp 传送点对象
     * @return 是否成功
     */
    public boolean setWarp(GuildWarp warp) {
        // 先检查是否已存在传送点
        GuildWarp existingWarp = getWarp(warp.getGuildId());
        
        if (existingWarp != null) {
            // 更新现有传送点
            return updateWarp(warp);
        } else {
            // 创建新传送点
            return createWarp(warp);
        }
    }
    
    /**
     * 创建新的传送点
     * @param warp 传送点对象
     * @return 是否成功
     */
    private boolean createWarp(GuildWarp warp) {
        String sql = "INSERT INTO guild_warps (guild_id, world, x, y, z, yaw, pitch, creator_uuid) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, warp.getGuildId());
            stmt.setString(2, warp.getWorld());
            stmt.setDouble(3, warp.getX());
            stmt.setDouble(4, warp.getY());
            stmt.setDouble(5, warp.getZ());
            stmt.setFloat(6, warp.getYaw());
            stmt.setFloat(7, warp.getPitch());
            stmt.setString(8, warp.getCreatorUuid().toString());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            // 获取生成的ID
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    warp.setId(generatedKeys.getInt(1));
                }
            }
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("创建公会传送点失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新传送点
     * @param warp 传送点对象
     * @return 是否成功
     */
    private boolean updateWarp(GuildWarp warp) {
        String sql = "UPDATE guild_warps SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, creator_uuid = ? " +
                     "WHERE guild_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, warp.getWorld());
            stmt.setDouble(2, warp.getX());
            stmt.setDouble(3, warp.getY());
            stmt.setDouble(4, warp.getZ());
            stmt.setFloat(5, warp.getYaw());
            stmt.setFloat(6, warp.getPitch());
            stmt.setString(7, warp.getCreatorUuid().toString());
            stmt.setInt(8, warp.getGuildId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新公会传送点失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取公会传送点
     * @param guildId 公会ID
     * @return 传送点对象，不存在返回null
     */
    public GuildWarp getWarp(int guildId) {
        String sql = "SELECT * FROM guild_warps WHERE guild_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractWarpFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会传送点失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 删除公会传送点
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean deleteWarp(int guildId) {
        String sql = "DELETE FROM guild_warps WHERE guild_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除公会传送点失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从结果集中提取传送点对象
     * @param rs 结果集
     * @return 传送点对象
     * @throws SQLException SQL异常
     */
    private GuildWarp extractWarpFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int guildId = rs.getInt("guild_id");
        String world = rs.getString("world");
        double x = rs.getDouble("x");
        double y = rs.getDouble("y");
        double z = rs.getDouble("z");
        float yaw = rs.getFloat("yaw");
        float pitch = rs.getFloat("pitch");
        Timestamp createdAt = rs.getTimestamp("created_at");
        UUID creatorUuid = UUID.fromString(rs.getString("creator_uuid"));

        return new GuildWarp(id, guildId, world, x, y, z, yaw, pitch, 
                            new java.util.Date(createdAt.getTime()), creatorUuid);
    }
}