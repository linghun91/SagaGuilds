package cn.i7mc.sagaguild.data.dao;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.DatabaseManager;
import cn.i7mc.sagaguild.data.models.CeasefireRequest;
import cn.i7mc.sagaguild.data.models.GuildWar;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 公会战数据访问对象
 * 提供公会战数据的CRUD操作
 */
public class WarDAO {
    private final SagaGuild plugin;
    private final DatabaseManager databaseManager;

    public WarDAO(SagaGuild plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 创建公会战
     * @param war 公会战对象
     * @return 创建的公会战ID，失败返回-1
     */
    public int createWar(GuildWar war) {
        String sql = "INSERT INTO wars (attacker_id, defender_id, start_time, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, war.getAttackerId());
            stmt.setInt(2, war.getDefenderId());
            stmt.setTimestamp(3, new Timestamp(war.getStartTime().getTime()));
            stmt.setString(4, war.getStatus().name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    war.setId(id);
                    return id;
                } else {
                    return -1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("创建公会战失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 更新公会战状态
     * @param warId 公会战ID
     * @param status 状态
     * @return 是否成功
     */
    public boolean updateWarStatus(int warId, GuildWar.Status status) {
        String sql = "UPDATE wars SET status = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, warId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新公会战状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 结束公会战
     * @param warId 公会战ID
     * @param winnerId 胜利方公会ID
     * @return 是否成功
     */
    public boolean endWar(int warId, Integer winnerId) {
        String sql = "UPDATE wars SET end_time = ?, winner_id = ?, status = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));

            if (winnerId != null) {
                stmt.setInt(2, winnerId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, GuildWar.Status.FINISHED.name());
            stmt.setInt(4, warId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("结束公会战失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据ID获取公会战
     * @param id 公会战ID
     * @return 公会战对象，不存在返回null
     */
    public GuildWar getWarById(int id) {
        String sql = "SELECT * FROM wars WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractWarFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会战失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取公会当前进行中的战争
     * @param guildId 公会ID
     * @return 公会战对象，不存在返回null
     */
    public GuildWar getActiveWarByGuild(int guildId) {
        String sql = "SELECT * FROM wars WHERE (attacker_id = ? OR defender_id = ?) AND status != ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setInt(2, guildId);
            stmt.setString(3, GuildWar.Status.FINISHED.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractWarFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会战失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 获取公会所有战争历史
     * @param guildId 公会ID
     * @return 公会战列表
     */
    public List<GuildWar> getWarHistoryByGuild(int guildId) {
        List<GuildWar> wars = new ArrayList<>();
        String sql = "SELECT * FROM wars WHERE attacker_id = ? OR defender_id = ? ORDER BY start_time DESC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setInt(2, guildId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    wars.add(extractWarFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会战历史失败: " + e.getMessage());
        }

        return wars;
    }

    /**
     * 记录击杀
     * @param warId 公会战ID
     * @param killerUuid 击杀者UUID
     * @param victimUuid 被击杀者UUID
     * @return 是否成功
     */
    public boolean recordKill(int warId, UUID killerUuid, UUID victimUuid) {
        String sql = "INSERT INTO war_kills (war_id, killer_uuid, victim_uuid, kill_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, warId);
            stmt.setString(2, killerUuid.toString());
            stmt.setString(3, victimUuid.toString());
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("记录击杀失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定公会在指定战争中的击杀数
     * @param warId 战争ID
     * @param guildId 公会ID
     * @return 击杀数
     */
    public int getWarKillsByGuild(int warId, int guildId) {
        String sql = "SELECT COUNT(*) FROM war_kills wk " +
                    "JOIN members m ON wk.killer_uuid = m.player_uuid " +
                    "WHERE wk.war_id = ? AND m.guild_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, warId);
            stmt.setInt(2, guildId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会战击杀统计失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 获取公会的战争胜利次数
     * @param guildId 公会ID
     * @return 胜利次数
     */
    public int getGuildWarWins(int guildId) {
        String sql = "SELECT COUNT(*) FROM wars WHERE winner_id = ? AND status = 'FINISHED'";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取公会战胜利次数失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 从结果集中提取公会战对象
     * @param rs 结果集
     * @return 公会战对象
     * @throws SQLException SQL异常
     */
    private GuildWar extractWarFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int attackerId = rs.getInt("attacker_id");
        int defenderId = rs.getInt("defender_id");
        Date startTime = new Date(rs.getTimestamp("start_time").getTime());

        Timestamp endTimeTimestamp = rs.getTimestamp("end_time");
        Date endTime = endTimeTimestamp != null ? new Date(endTimeTimestamp.getTime()) : null;

        Integer winnerId = rs.getInt("winner_id");
        if (rs.wasNull()) {
            winnerId = null;
        }

        GuildWar.Status status = GuildWar.Status.valueOf(rs.getString("status"));

        return new GuildWar(id, attackerId, defenderId, startTime, endTime, winnerId, status);
    }

    /**
     * 创建停战请求
     * @param request 停战请求对象
     * @return 创建的停战请求ID，失败返回-1
     */
    public int createCeasefireRequest(CeasefireRequest request) {
        String sql = "INSERT INTO ceasefire_requests (requester_id, target_id, war_id, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, request.getRequesterId());
            stmt.setInt(2, request.getTargetId());
            stmt.setInt(3, request.getWarId());
            stmt.setString(4, request.getStatus().name());

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
            plugin.getLogger().severe("创建停战请求失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 更新停战请求状态
     * @param requestId 请求ID
     * @param status 新状态
     * @return 是否成功
     */
    public boolean updateCeasefireRequestStatus(int requestId, CeasefireRequest.Status status) {
        String sql = "UPDATE ceasefire_requests SET status = ? WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, requestId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("更新停战请求状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取公会收到的停战请求
     * @param guildId 公会ID
     * @return 停战请求列表
     */
    public List<CeasefireRequest> getReceivedCeasefireRequests(int guildId) {
        String sql = "SELECT * FROM ceasefire_requests WHERE target_id = ? AND status = ?";
        List<CeasefireRequest> requests = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setString(2, CeasefireRequest.Status.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(extractCeasefireRequestFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取停战请求失败: " + e.getMessage());
        }

        return requests;
    }

    /**
     * 获取公会发送的停战请求
     * @param guildId 公会ID
     * @return 停战请求列表
     */
    public List<CeasefireRequest> getSentCeasefireRequests(int guildId) {
        String sql = "SELECT * FROM ceasefire_requests WHERE requester_id = ? AND status = ?";
        List<CeasefireRequest> requests = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, guildId);
            stmt.setString(2, CeasefireRequest.Status.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(extractCeasefireRequestFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取停战请求失败: " + e.getMessage());
        }

        return requests;
    }

    /**
     * 获取两个公会之间的停战请求
     * @param requesterId 请求方公会ID
     * @param targetId 目标公会ID
     * @param warId 战争ID
     * @return 停战请求对象，不存在返回null
     */
    public CeasefireRequest getCeasefireRequestBetweenGuilds(int requesterId, int targetId, int warId) {
        String sql = "SELECT * FROM ceasefire_requests WHERE requester_id = ? AND target_id = ? AND war_id = ? AND status = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requesterId);
            stmt.setInt(2, targetId);
            stmt.setInt(3, warId);
            stmt.setString(4, CeasefireRequest.Status.PENDING.name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractCeasefireRequestFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取停战请求失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 从结果集中提取停战请求对象
     * @param rs 结果集
     * @return 停战请求对象
     * @throws SQLException SQL异常
     */
    private CeasefireRequest extractCeasefireRequestFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int requesterId = rs.getInt("requester_id");
        int targetId = rs.getInt("target_id");
        int warId = rs.getInt("war_id");
        Date requestedAt = new Date(rs.getTimestamp("requested_at").getTime());
        CeasefireRequest.Status status = CeasefireRequest.Status.valueOf(rs.getString("status"));

        return new CeasefireRequest(id, requesterId, targetId, warId, requestedAt, status);
    }
}
