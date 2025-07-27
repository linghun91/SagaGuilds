package cn.i7mc.sagaguild.data.dao;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Alliance;
import cn.i7mc.sagaguild.data.models.AllianceRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 联盟数据访问对象
 * 处理联盟数据的CRUD操作
 */
public class AllianceDAO {
    private final SagaGuild plugin;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public AllianceDAO(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 创建联盟
     * @param alliance 联盟对象
     * @return 创建的联盟ID，失败返回-1
     */
    public int createAlliance(Alliance alliance) {
        String sql = "INSERT INTO alliances (guild1_id, guild2_id) VALUES (?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, alliance.getGuild1Id());
            stmt.setInt(2, alliance.getGuild2Id());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("创建联盟失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 删除联盟
     * @param id 联盟ID
     * @return 是否成功
     */
    public boolean deleteAlliance(int id) {
        String sql = "DELETE FROM alliances WHERE id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除联盟失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除两个公会之间的联盟
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 是否成功
     */
    public boolean deleteAllianceBetweenGuilds(int guild1Id, int guild2Id) {
        String sql = "DELETE FROM alliances WHERE (guild1_id = ? AND guild2_id = ?) OR (guild1_id = ? AND guild2_id = ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guild1Id);
            stmt.setInt(2, guild2Id);
            stmt.setInt(3, guild2Id);
            stmt.setInt(4, guild1Id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除公会间联盟失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据ID获取联盟
     * @param id 联盟ID
     * @return 联盟对象，不存在返回null
     */
    public Alliance getAllianceById(int id) {
        String sql = "SELECT * FROM alliances WHERE id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAllianceFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取联盟失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取两个公会之间的联盟
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 联盟对象，不存在返回null
     */
    public Alliance getAllianceBetweenGuilds(int guild1Id, int guild2Id) {
        String sql = "SELECT * FROM alliances WHERE (guild1_id = ? AND guild2_id = ?) OR (guild1_id = ? AND guild2_id = ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guild1Id);
            stmt.setInt(2, guild2Id);
            stmt.setInt(3, guild2Id);
            stmt.setInt(4, guild1Id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAllianceFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会间联盟失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取公会的所有联盟
     * @param guildId 公会ID
     * @return 联盟列表
     */
    public List<Alliance> getGuildAlliances(int guildId) {
        String sql = "SELECT * FROM alliances WHERE guild1_id = ? OR guild2_id = ?";
        List<Alliance> alliances = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setInt(2, guildId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    alliances.add(extractAllianceFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会联盟列表失败: " + e.getMessage());
        }

        return alliances;
    }

    /**
     * 获取所有联盟
     * @return 联盟列表
     */
    public List<Alliance> getAllAlliances() {
        String sql = "SELECT * FROM alliances";
        List<Alliance> alliances = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                alliances.add(extractAllianceFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取所有联盟失败: " + e.getMessage());
        }

        return alliances;
    }

    /**
     * 检查两个公会是否已结盟
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 是否已结盟
     */
    public boolean areGuildsAllied(int guild1Id, int guild2Id) {
        return getAllianceBetweenGuilds(guild1Id, guild2Id) != null;
    }

    /**
     * 从ResultSet中提取联盟对象
     * @param rs ResultSet
     * @return 联盟对象
     * @throws SQLException SQL异常
     */
    private Alliance extractAllianceFromResultSet(ResultSet rs) throws SQLException {
        return new Alliance(
                rs.getInt("id"),
                rs.getInt("guild1_id"),
                rs.getInt("guild2_id"),
                rs.getTimestamp("formed_at")
        );
    }

    /**
     * 创建联盟请求
     * @param request 联盟请求对象
     * @return 创建的请求ID，失败返回-1
     */
    public int createAllianceRequest(AllianceRequest request) {
        String sql = "INSERT INTO alliance_requests (requester_id, target_id, status) VALUES (?, ?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, request.getRequesterId());
            stmt.setInt(2, request.getTargetId());
            stmt.setString(3, request.getStatus().name());

            // 添加调试日志
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("尝试创建联盟请求: 请求方ID=" + request.getRequesterId() +
                                       ", 目标方ID=" + request.getTargetId() +
                                       ", 状态=" + request.getStatus().name());
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                plugin.getLogger().warning("创建联盟请求失败: 没有行被插入");
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int requestId = generatedKeys.getInt(1);
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("联盟请求创建成功: ID=" + requestId);
                    }
                    return requestId;
                } else {
                    plugin.getLogger().warning("创建联盟请求失败: 无法获取生成的ID");
                    return -1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("创建联盟请求失败: " + e.getMessage());
            // 添加更详细的错误信息
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                plugin.getLogger().severe("可能原因: 已经存在相同的请求 (requester_id=" +
                                         request.getRequesterId() + ", target_id=" +
                                         request.getTargetId() + ")");
            }
            return -1;
        }
    }

    /**
     * 更新联盟请求状态
     * @param requestId 请求ID
     * @param status 新状态
     * @return 是否成功
     */
    public boolean updateAllianceRequestStatus(int requestId, AllianceRequest.Status status) {
        String sql = "UPDATE alliance_requests SET status = ? WHERE id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, requestId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新联盟请求状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取公会收到的联盟请求
     * @param guildId 公会ID
     * @return 联盟请求列表
     */
    public List<AllianceRequest> getReceivedAllianceRequests(int guildId) {
        String sql = "SELECT * FROM alliance_requests WHERE target_id = ? AND status = ?";
        List<AllianceRequest> requests = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setString(2, AllianceRequest.Status.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(extractAllianceRequestFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取联盟请求失败: " + e.getMessage());
        }

        return requests;
    }

    /**
     * 获取公会发送的联盟请求
     * @param guildId 公会ID
     * @return 联盟请求列表
     */
    public List<AllianceRequest> getSentAllianceRequests(int guildId) {
        String sql = "SELECT * FROM alliance_requests WHERE requester_id = ? AND status = ?";
        List<AllianceRequest> requests = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setString(2, AllianceRequest.Status.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(extractAllianceRequestFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取联盟请求失败: " + e.getMessage());
        }

        return requests;
    }

    /**
     * 获取两个公会之间的联盟请求
     * @param requesterId 请求方公会ID
     * @param targetId 目标公会ID
     * @return 联盟请求对象，不存在返回null
     */
    public AllianceRequest getAllianceRequestBetweenGuilds(int requesterId, int targetId) {
        String sql = "SELECT * FROM alliance_requests WHERE requester_id = ? AND target_id = ? AND status = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requesterId);
            stmt.setInt(2, targetId);
            stmt.setString(3, AllianceRequest.Status.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractAllianceRequestFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取联盟请求失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 检查是否存在任何状态的联盟请求（包括已接受、已拒绝等）
     * @param requesterId 请求方公会ID
     * @param targetId 目标公会ID
     * @return 是否存在请求
     */
    public boolean existsAnyAllianceRequest(int requesterId, int targetId) {
        String sql = "SELECT COUNT(*) FROM alliance_requests WHERE requester_id = ? AND target_id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requesterId);
            stmt.setInt(2, targetId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("检查联盟请求失败: " + e.getMessage());
        }

        return false;
    }

    /**
     * 清理过期的联盟请求
     * @param requesterId 请求方公会ID
     * @param targetId 目标公会ID
     * @return 是否成功
     */
    public boolean cleanupAllianceRequests(int requesterId, int targetId) {
        String sql = "DELETE FROM alliance_requests WHERE requester_id = ? AND target_id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requesterId);
            stmt.setInt(2, targetId);

            int affectedRows = stmt.executeUpdate();
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("清理联盟请求: 删除了 " + affectedRows + " 条记录 (请求方ID=" +
                                       requesterId + ", 目标方ID=" + targetId + ")");
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("清理联盟请求失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 清理所有相关的联盟请求（包括反向请求）
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 是否成功
     */
    public boolean cleanupAllRelatedAllianceRequests(int guild1Id, int guild2Id) {
        String sql = "DELETE FROM alliance_requests WHERE (requester_id = ? AND target_id = ?) OR (requester_id = ? AND target_id = ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guild1Id);
            stmt.setInt(2, guild2Id);
            stmt.setInt(3, guild2Id);
            stmt.setInt(4, guild1Id);

            int affectedRows = stmt.executeUpdate();
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("清理所有相关联盟请求: 删除了 " + affectedRows + " 条记录 (公会ID=" +
                                       guild1Id + ", 公会ID=" + guild2Id + ")");
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("清理所有相关联盟请求失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除公会的所有联盟关系
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean deleteAllGuildAlliances(int guildId) {
        String sql = "DELETE FROM alliances WHERE guild1_id = ? OR guild2_id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setInt(2, guildId);

            int affectedRows = stmt.executeUpdate();
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("删除公会所有联盟关系: 删除了 " + affectedRows + " 条记录 (公会ID=" + guildId + ")");
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除公会所有联盟关系失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 清理公会的所有相关联盟请求
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean cleanupAllRelatedRequests(int guildId) {
        String sql = "DELETE FROM alliance_requests WHERE requester_id = ? OR target_id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setInt(2, guildId);

            int affectedRows = stmt.executeUpdate();
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("清理公会所有联盟请求: 删除了 " + affectedRows + " 条记录 (公会ID=" + guildId + ")");
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("清理公会所有联盟请求失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从ResultSet中提取联盟请求对象
     * @param rs ResultSet
     * @return 联盟请求对象
     * @throws SQLException SQL异常
     */
    private AllianceRequest extractAllianceRequestFromResultSet(ResultSet rs) throws SQLException {
        return new AllianceRequest(
                rs.getInt("id"),
                rs.getInt("requester_id"),
                rs.getInt("target_id"),
                rs.getTimestamp("requested_at"),
                AllianceRequest.Status.valueOf(rs.getString("status"))
        );
    }
}
