package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.AllianceDAO;
import cn.i7mc.sagaguild.data.models.Alliance;
import cn.i7mc.sagaguild.data.models.AllianceRequest;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联盟管理器
 * 负责公会联盟的创建和管理
 */
public class AllianceManager {
    private final SagaGuild plugin;
    private final AllianceDAO allianceDAO;

    // 缓存联盟数据
    private final Map<Integer, List<Integer>> guildAlliances;

    // 缓存联盟请求数据
    private final Map<Integer, List<AllianceRequest>> receivedRequests;
    private final Map<Integer, List<AllianceRequest>> sentRequests;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public AllianceManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.allianceDAO = new AllianceDAO(plugin);
        this.guildAlliances = new HashMap<>();
        this.receivedRequests = new HashMap<>();
        this.sentRequests = new HashMap<>();

        // 加载所有联盟数据到缓存
        loadAlliances();

        // 加载所有联盟请求数据到缓存
        loadAllianceRequests();
    }

    /**
     * 从数据库加载所有联盟数据到缓存
     */
    private void loadAlliances() {
        // 清空缓存
        guildAlliances.clear();

        // 加载所有联盟
        List<Alliance> alliances = allianceDAO.getAllAlliances();
        for (Alliance alliance : alliances) {
            // 为第一个公会添加联盟
            List<Integer> guild1Allies = guildAlliances.computeIfAbsent(alliance.getGuild1Id(), k -> new ArrayList<>());
            guild1Allies.add(alliance.getGuild2Id());

            // 为第二个公会添加联盟
            List<Integer> guild2Allies = guildAlliances.computeIfAbsent(alliance.getGuild2Id(), k -> new ArrayList<>());
            guild2Allies.add(alliance.getGuild1Id());
        }

        plugin.getLogger().info("已加载 " + alliances.size() + " 个联盟数据！");
    }

    /**
     * 重新加载所有联盟数据
     */
    public void reloadAlliances() {
        loadAlliances();
    }

    /**
     * 从数据库加载所有联盟请求数据到缓存
     */
    private void loadAllianceRequests() {
        // 清空缓存
        receivedRequests.clear();
        sentRequests.clear();

        // 加载所有公会的联盟请求
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            int guildId = guild.getId();

            // 加载收到的请求
            List<AllianceRequest> received = allianceDAO.getReceivedAllianceRequests(guildId);
            if (!received.isEmpty()) {
                receivedRequests.put(guildId, received);
            }

            // 加载发送的请求
            List<AllianceRequest> sent = allianceDAO.getSentAllianceRequests(guildId);
            if (!sent.isEmpty()) {
                sentRequests.put(guildId, sent);
            }
        }


    }

    /**
     * 重新加载所有联盟请求数据
     */
    public void reloadAllianceRequests() {
        loadAllianceRequests();
    }

    /**
     * 获取公会收到的联盟请求
     * @param guildId 公会ID
     * @return 联盟请求列表
     */
    public List<AllianceRequest> getReceivedRequests(int guildId) {
        return receivedRequests.getOrDefault(guildId, new ArrayList<>());
    }

    /**
     * 获取公会发送的联盟请求
     * @param guildId 公会ID
     * @return 联盟请求列表
     */
    public List<AllianceRequest> getSentRequests(int guildId) {
        return sentRequests.getOrDefault(guildId, new ArrayList<>());
    }

    /**
     * 创建联盟
     * @param player 发起联盟的玩家
     * @param targetGuildId 目标公会ID
     * @return 是否成功
     */
    public boolean createAlliance(Player player, int targetGuildId) {
        // 获取玩家所在公会
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isOwner()) {
            return false;
        }

        int guildId = member.getGuildId();

        // 检查目标公会是否存在
        Guild targetGuild = plugin.getGuildManager().getGuildById(targetGuildId);
        if (targetGuild == null) {
            return false;
        }

        // 检查是否已经是联盟
        if (areGuildsAllied(guildId, targetGuildId)) {
            return false;
        }

        // 创建联盟
        Alliance alliance = new Alliance(guildId, targetGuildId);
        int allianceId = allianceDAO.createAlliance(alliance);

        if (allianceId == -1) {
            return false;
        }

        // 更新缓存
        List<Integer> guildAllies = guildAlliances.computeIfAbsent(guildId, k -> new ArrayList<>());
        guildAllies.add(targetGuildId);

        List<Integer> targetGuildAllies = guildAlliances.computeIfAbsent(targetGuildId, k -> new ArrayList<>());
        targetGuildAllies.add(guildId);

        // 通知双方公会成员
        notifyGuildMembers(guildId, targetGuildId, true);

        return true;
    }

    /**
     * 解除联盟
     * @param player 发起解除的玩家
     * @param targetGuildId 目标公会ID
     * @return 是否成功
     */
    public boolean breakAlliance(Player player, int targetGuildId) {
        // 获取玩家所在公会
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isOwner()) {
            return false;
        }

        int guildId = member.getGuildId();

        // 检查是否存在联盟
        if (!areGuildsAllied(guildId, targetGuildId)) {
            return false;
        }

        // 解除联盟
        boolean success = allianceDAO.deleteAllianceBetweenGuilds(guildId, targetGuildId);

        if (!success) {
            return false;
        }

        // 更新缓存
        List<Integer> guildAllies = guildAlliances.get(guildId);
        if (guildAllies != null) {
            guildAllies.remove(Integer.valueOf(targetGuildId));
        }

        List<Integer> targetGuildAllies = guildAlliances.get(targetGuildId);
        if (targetGuildAllies != null) {
            targetGuildAllies.remove(Integer.valueOf(guildId));
        }

        // 通知双方公会成员
        notifyGuildMembers(guildId, targetGuildId, false);

        return true;
    }

    /**
     * 获取公会的所有联盟
     * @param guildId 公会ID
     * @return 联盟公会ID列表
     */
    public List<Integer> getGuildAlliances(int guildId) {
        return guildAlliances.getOrDefault(guildId, new ArrayList<>());
    }

    /**
     * 检查两个公会是否已结盟
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 是否已结盟
     */
    public boolean areGuildsAllied(int guild1Id, int guild2Id) {
        List<Integer> allies = guildAlliances.get(guild1Id);
        return allies != null && allies.contains(guild2Id);
    }

    /**
     * 通知公会成员联盟状态变化
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @param isCreated 是创建还是解除
     */
    private void notifyGuildMembers(int guild1Id, int guild2Id, boolean isCreated) {
        Guild guild1 = plugin.getGuildManager().getGuildById(guild1Id);
        Guild guild2 = plugin.getGuildManager().getGuildById(guild2Id);

        if (guild1 == null || guild2 == null) {
            return;
        }

        // 获取消息
        String messageKey = isCreated ? "alliance.created" : "alliance.broken";

        // 通知第一个公会成员
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(guild1Id)) {
            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                String message = plugin.getConfigManager().getMessage(messageKey,
                        "guild", guild2.getName());
                player.sendMessage(message);
            }
        }

        // 通知第二个公会成员
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(guild2Id)) {
            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                String message = plugin.getConfigManager().getMessage(messageKey,
                        "guild", guild1.getName());
                player.sendMessage(message);
            }
        }
    }

    /**
     * 发送联盟请求
     * @param requesterId 请求方公会ID
     * @param targetId 目标公会ID
     * @return 是否成功
     */
    public boolean sendAllianceRequest(int requesterId, int targetId) {
        // 检查公会是否存在
        Guild requesterGuild = plugin.getGuildManager().getGuildById(requesterId);
        Guild targetGuild = plugin.getGuildManager().getGuildById(targetId);
        if (requesterGuild == null || targetGuild == null) {
            return false;
        }

        // 检查是否已经是联盟
        if (areGuildsAllied(requesterId, targetId)) {
            return false;
        }

        // 检查是否已经发送过请求
        AllianceRequest existingRequest = allianceDAO.getAllianceRequestBetweenGuilds(requesterId, targetId);
        if (existingRequest != null) {
            return false;
        }

        // 检查是否有反向请求（目标公会向请求公会发送的请求）
        AllianceRequest reverseRequest = allianceDAO.getAllianceRequestBetweenGuilds(targetId, requesterId);
        if (reverseRequest != null) {
            return false;
        }

        // 检查是否存在任何状态的请求记录（包括已接受、已拒绝等）
        boolean hasExistingRequest = allianceDAO.existsAnyAllianceRequest(requesterId, targetId);
        boolean hasReverseRequest = allianceDAO.existsAnyAllianceRequest(targetId, requesterId);

        if (hasExistingRequest || hasReverseRequest) {
            // 清理所有相关的请求记录（包括反向请求）
            boolean cleaned = allianceDAO.cleanupAllRelatedAllianceRequests(requesterId, targetId);
            if (!cleaned) {
                return false;
            }
        }

        // 创建联盟请求
        AllianceRequest request = new AllianceRequest(requesterId, targetId);
        int requestId = allianceDAO.createAllianceRequest(request);

        if (requestId == -1) {
            return false;
        }

        // 更新缓存
        request.setId(requestId);
        List<AllianceRequest> sentList = sentRequests.computeIfAbsent(requesterId, k -> new ArrayList<>());
        sentList.add(request);

        List<AllianceRequest> receivedList = receivedRequests.computeIfAbsent(targetId, k -> new ArrayList<>());
        receivedList.add(request);

        // 通知目标公会在线成员
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(targetId)) {
            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                String message = plugin.getConfigManager().getMessage("alliance.request-received",
                        "guild", requesterGuild.getName());
                player.sendMessage(message);
            }
        }

        return true;
    }

    /**
     * 接受联盟请求
     * @param targetId 接受方公会ID
     * @param requesterId 请求方公会ID
     * @return 是否成功
     */
    public boolean acceptAllianceRequest(int targetId, int requesterId) {
        // 获取请求
        AllianceRequest request = allianceDAO.getAllianceRequestBetweenGuilds(requesterId, targetId);
        if (request == null) {
            return false;
        }

        // 更新请求状态
        boolean success = allianceDAO.updateAllianceRequestStatus(request.getId(), AllianceRequest.Status.ACCEPTED);
        if (!success) {
            return false;
        }

        // 创建联盟
        Alliance alliance = new Alliance(requesterId, targetId);
        int allianceId = allianceDAO.createAlliance(alliance);

        if (allianceId == -1) {
            return false;
        }

        // 更新缓存
        List<Integer> requesterAllies = guildAlliances.computeIfAbsent(requesterId, k -> new ArrayList<>());
        requesterAllies.add(targetId);

        List<Integer> targetAllies = guildAlliances.computeIfAbsent(targetId, k -> new ArrayList<>());
        targetAllies.add(requesterId);

        // 从请求缓存中移除
        List<AllianceRequest> receivedList = receivedRequests.get(targetId);
        if (receivedList != null) {
            receivedList.removeIf(r -> r.getId() == request.getId());
        }

        List<AllianceRequest> sentList = sentRequests.get(requesterId);
        if (sentList != null) {
            sentList.removeIf(r -> r.getId() == request.getId());
        }

        // 通知双方公会成员
        notifyGuildMembers(requesterId, targetId, true);

        return true;
    }

    /**
     * 拒绝联盟请求
     * @param targetId 拒绝方公会ID
     * @param requesterId 请求方公会ID
     * @return 是否成功
     */
    public boolean rejectAllianceRequest(int targetId, int requesterId) {
        // 获取请求
        AllianceRequest request = allianceDAO.getAllianceRequestBetweenGuilds(requesterId, targetId);
        if (request == null) {
            return false;
        }

        // 更新请求状态
        boolean success = allianceDAO.updateAllianceRequestStatus(request.getId(), AllianceRequest.Status.REJECTED);
        if (!success) {
            return false;
        }

        // 从请求缓存中移除
        List<AllianceRequest> receivedList = receivedRequests.get(targetId);
        if (receivedList != null) {
            receivedList.removeIf(r -> r.getId() == request.getId());
        }

        List<AllianceRequest> sentList = sentRequests.get(requesterId);
        if (sentList != null) {
            sentList.removeIf(r -> r.getId() == request.getId());
        }

        // 通知请求方公会
        Guild requesterGuild = plugin.getGuildManager().getGuildById(requesterId);
        Guild targetGuild = plugin.getGuildManager().getGuildById(targetId);

        if (requesterGuild != null && targetGuild != null) {
            String message = plugin.getConfigManager().getMessage("alliance.alliance-rejected",
                    "guild", targetGuild.getName());

            for (GuildMember member : plugin.getGuildManager().getGuildMembers(requesterId)) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }

        return true;
    }

    /**
     * 获取公会的所有联盟公会
     * @param guildId 公会ID
     * @return 联盟公会列表
     */
    public List<Guild> getAlliedGuilds(int guildId) {
        List<Guild> allies = new ArrayList<>();
        List<Integer> allyIds = guildAlliances.get(guildId);
        
        if (allyIds != null) {
            for (Integer allyId : allyIds) {
                Guild guild = plugin.getGuildManager().getGuildById(allyId);
                if (guild != null) {
                    allies.add(guild);
                }
            }
        }
        
        return allies;
    }

    /**
     * 清理公会的所有联盟关系
     * @param guildId 公会ID
     */
    public void cleanupGuildAlliances(int guildId) {
        try {
            // 删除数据库中的所有联盟关系
            allianceDAO.deleteAllGuildAlliances(guildId);

            // 更新缓存
            guildAlliances.remove(guildId);

            // 从其他公会的联盟列表中移除该公会
            for (Map.Entry<Integer, List<Integer>> entry : guildAlliances.entrySet()) {
                List<Integer> allies = entry.getValue();
                allies.remove(Integer.valueOf(guildId));
            }

            // 清理所有相关的联盟请求
            allianceDAO.cleanupAllRelatedRequests(guildId);

        } catch (Exception e) {
            plugin.getLogger().warning("清理公会联盟关系时发生错误: " + e.getMessage());
        }
    }
}
