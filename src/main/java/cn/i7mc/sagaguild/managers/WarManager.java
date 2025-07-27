package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.WarDAO;
import cn.i7mc.sagaguild.data.models.CeasefireRequest;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildWar;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 战争管理器
 * 负责公会战的挑战、接受和管理
 */
public class WarManager {
    private final SagaGuild plugin;
    private final WarDAO warDAO;

    // 缓存当前进行中的战争
    private final Map<Integer, GuildWar> activeWars;

    // 战争任务
    private final Map<Integer, BukkitTask> warTasks;

    // 战争邀请
    private final Map<Integer, Map<Integer, Long>> warInvitations;

    // 停战请求缓存
    private final Map<Integer, List<CeasefireRequest>> receivedCeasefireRequests;
    private final Map<Integer, List<CeasefireRequest>> sentCeasefireRequests;

    public WarManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.warDAO = new WarDAO(plugin);

        this.activeWars = new HashMap<>();
        this.warTasks = new HashMap<>();
        this.warInvitations = new HashMap<>();
        this.receivedCeasefireRequests = new HashMap<>();
        this.sentCeasefireRequests = new HashMap<>();

        // 加载所有进行中的战争
        loadActiveWars();

        // 加载所有停战请求
        loadCeasefireRequests();
    }

    /**
     * 从数据库加载所有进行中的战争
     */
    private void loadActiveWars() {
        // 清空缓存
        activeWars.clear();

        // 加载所有公会的进行中战争
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            GuildWar war = warDAO.getActiveWarByGuild(guild.getId());
            if (war != null && !activeWars.containsKey(war.getId())) {
                activeWars.put(war.getId(), war);

                // 如果战争正在进行中，启动战争任务
                if (war.getStatus() == GuildWar.Status.ONGOING) {
                    startWarTask(war);
                }
            }
        }

        plugin.getLogger().info("已加载 " + activeWars.size() + " 个进行中的公会战！");
    }

    /**
     * 发起公会战邀请
     * @param player 发起者
     * @param targetGuildName 目标公会名称
     * @return 是否成功
     */
    public boolean inviteToWar(Player player, String targetGuildName) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限发起公会战
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 检查目标公会是否存在
        Guild targetGuild = plugin.getGuildManager().getGuildByName(targetGuildName);
        if (targetGuild == null) {
            player.sendMessage("§c找不到名为 §7" + targetGuildName + " §c的公会！");
            return false;
        }

        // 检查是否是自己的公会
        if (guild.getId() == targetGuild.getId()) {
            player.sendMessage("§c你不能向自己的公会发起战争！");
            return false;
        }

        // 检查公会是否已经在战争中
        if (getActiveWar(guild.getId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("war.already-in-war"));
            return false;
        }

        // 检查目标公会是否已经在战争中
        if (getActiveWar(targetGuild.getId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("war.target-in-war"));
            return false;
        }

        // 检查公会成员数量是否足够
        FileConfiguration config = plugin.getConfig();
        int minParticipants = config.getInt("war.min-participants", 3);

        if (plugin.getGuildManager().getGuildMemberCount(guild.getId()) < minParticipants) {
            player.sendMessage(plugin.getConfigManager().getMessage("war.not-enough-members"));
            return false;
        }

        if (plugin.getGuildManager().getGuildMemberCount(targetGuild.getId()) < minParticipants) {
            player.sendMessage("§c目标公会没有足够的成员参与公会战！");
            return false;
        }

        // 发送邀请
        Map<Integer, Long> invites = warInvitations.computeIfAbsent(targetGuild.getId(), k -> new HashMap<>());
        invites.put(guild.getId(), System.currentTimeMillis() + 300000); // 5分钟有效期

        // 通知发起者
        player.sendMessage(plugin.getConfigManager().getMessage("war.declared",
                "guild", targetGuild.getName()));

        // 通知目标公会在线成员
        for (GuildMember targetMember : plugin.getGuildManager().getGuildMembers(targetGuild.getId())) {
            Player targetPlayer = Bukkit.getPlayer(targetMember.getPlayerUuid());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(plugin.getConfigManager().getMessage("war.received",
                        "guild", guild.getName()));
                targetPlayer.sendMessage("§a使用 §7/guild war accept " + guild.getName() + " §a接受挑战！");
            }
        }

        return true;
    }

    /**
     * 接受公会战邀请
     * @param player 接受者
     * @param targetGuildName 发起公会名称
     * @return 是否成功
     */
    public boolean acceptWarInvitation(Player player, String targetGuildName) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限接受公会战
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 检查发起公会是否存在
        Guild targetGuild = plugin.getGuildManager().getGuildByName(targetGuildName);
        if (targetGuild == null) {
            player.sendMessage("§c找不到名为 §7" + targetGuildName + " §c的公会！");
            return false;
        }

        // 检查是否有邀请
        Map<Integer, Long> invites = warInvitations.get(guild.getId());
        if (invites == null || !invites.containsKey(targetGuild.getId())) {
            player.sendMessage("§c没有来自该公会的战争邀请！");
            return false;
        }

        // 检查邀请是否过期
        long expireTime = invites.get(targetGuild.getId());
        if (System.currentTimeMillis() > expireTime) {
            invites.remove(targetGuild.getId());
            player.sendMessage("§c该战争邀请已过期！");
            return false;
        }

        // 检查公会是否已经在战争中
        if (getActiveWar(guild.getId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("war.already-in-war"));
            return false;
        }

        // 检查目标公会是否已经在战争中
        if (getActiveWar(targetGuild.getId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("war.target-in-war"));
            return false;
        }

        // 移除邀请
        invites.remove(targetGuild.getId());

        // 创建公会战
        GuildWar war = new GuildWar(targetGuild.getId(), guild.getId());
        war.setStatus(GuildWar.Status.PREPARING);

        int warId = warDAO.createWar(war);
        if (warId == -1) {
            player.sendMessage("§c创建公会战失败，请稍后再试！");
            return false;
        }

        // 缓存公会战
        activeWars.put(warId, war);

        // 启动准备阶段
        startPreparationPhase(war);

        // 通知双方公会成员
        notifyWarParticipants(war, "§c公会战准备阶段开始！战斗将在 §7" +
                plugin.getConfig().getInt("war.preparation-time", 5) + " §c分钟后开始！");

        return true;
    }

    /**
     * 启动准备阶段
     * @param war 公会战对象
     */
    private void startPreparationPhase(GuildWar war) {
        FileConfiguration config = plugin.getConfig();
        int preparationTime = config.getInt("war.preparation-time", 5);

        // 启动准备阶段任务
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 更新状态为进行中
            war.setStatus(GuildWar.Status.ONGOING);
            warDAO.updateWarStatus(war.getId(), GuildWar.Status.ONGOING);

            // 通知参与者
            notifyWarParticipants(war, "§c公会战开始了！");

            // 启动战争任务
            startWarTask(war);

        }, preparationTime * 60 * 20); // 转换为tick

        warTasks.put(war.getId(), task);
    }

    /**
     * 启动战争任务
     * @param war 公会战对象
     */
    private void startWarTask(GuildWar war) {
        FileConfiguration config = plugin.getConfig();
        int warDuration = config.getInt("war.duration", 30);

        // 启动战争结束任务
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 结束战争
            endWar(war);

        }, warDuration * 60 * 20); // 转换为tick

        warTasks.put(war.getId(), task);
    }

    /**
     * 结束战争
     * @param war 公会战对象
     */
    private void endWar(GuildWar war) {
        // 确定胜利者
        Integer winnerId = determineWinner(war);

        // 更新数据库
        warDAO.endWar(war.getId(), winnerId);

        // 更新缓存
        war.setStatus(GuildWar.Status.FINISHED);
        war.setEndTime(new Date());
        war.setWinnerId(winnerId);

        // 移除缓存
        activeWars.remove(war.getId());

        // 取消任务
        BukkitTask task = warTasks.remove(war.getId());
        if (task != null) {
            task.cancel();
        }

        // 清除战争排行榜缓存，确保占位符能够获取最新数据
        plugin.getRankingManager().clearCache("war");

        // 通知参与者
        if (winnerId != null) {
            Guild winner = plugin.getGuildManager().getGuildById(winnerId);
            if (winner != null) {
                notifyWarParticipants(war, plugin.getConfigManager().getMessage("war.ended",
                        "winner", winner.getName()));
            }
        } else {
            notifyWarParticipants(war, plugin.getConfigManager().getMessage("war.tied"));
        }

        // 奖励胜利者
        if (winnerId != null) {
            // TODO: 实现奖励机制
        }
    }

    /**
     * 确定胜利者
     * @param war 公会战对象
     * @return 胜利者公会ID，平局返回null
     */
    private Integer determineWinner(GuildWar war) {
        try {
            // 获取攻击方击杀数
            int attackerKills = warDAO.getWarKillsByGuild(war.getId(), war.getAttackerId());
            
            // 获取防守方击杀数
            int defenderKills = warDAO.getWarKillsByGuild(war.getId(), war.getDefenderId());
            
            plugin.getLogger().info("公会战 " + war.getId() + " 结算: 攻击方击杀=" + attackerKills + ", 防守方击杀=" + defenderKills);
            
            // 比较击杀数确定胜利者
            if (attackerKills > defenderKills) {
                return war.getAttackerId(); // 攻击方获胜
            } else if (defenderKills > attackerKills) {
                return war.getDefenderId(); // 防守方获胜
            } else {
                return null; // 平局
            }
        } catch (Exception e) {
            plugin.getLogger().severe("确定战争胜利者时发生错误: " + e.getMessage());
            return null; // 发生异常时视为平局
        }
    }

    /**
     * 记录击杀
     * @param killer 击杀者
     * @param victim 被击杀者
     */
    public void recordKill(Player killer, Player victim) {
        // 获取玩家所在公会
        Guild killerGuild = plugin.getGuildManager().getPlayerGuild(killer.getUniqueId());
        Guild victimGuild = plugin.getGuildManager().getPlayerGuild(victim.getUniqueId());

        if (killerGuild == null || victimGuild == null) {
            return;
        }

        // 检查是否在同一场战争中
        GuildWar war = getActiveWar(killerGuild.getId());
        if (war == null || !war.isParticipant(victimGuild.getId()) || war.getStatus() != GuildWar.Status.ONGOING) {
            return;
        }

        // 更新数据库中的击杀记录
        warDAO.recordKill(war.getId(), killer.getUniqueId(), victim.getUniqueId());
    }

    /**
     * 获取公会当前进行中的战争
     * @param guildId 公会ID
     * @return 公会战对象，不存在返回null
     */
    public GuildWar getActiveWar(int guildId) {
        for (GuildWar war : activeWars.values()) {
            if (war.isParticipant(guildId)) {
                return war;
            }
        }
        return null;
    }

    /**
     * 获取公会战争历史
     * @param guildId 公会ID
     * @return 公会战列表
     */
    public List<GuildWar> getWarHistory(int guildId) {
        return warDAO.getWarHistoryByGuild(guildId);
    }

    /**
     * 从数据库加载所有停战请求
     */
    private void loadCeasefireRequests() {
        // 清空缓存
        receivedCeasefireRequests.clear();
        sentCeasefireRequests.clear();

        // 加载所有公会的停战请求
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            int guildId = guild.getId();

            // 加载收到的请求
            List<CeasefireRequest> received = warDAO.getReceivedCeasefireRequests(guildId);
            if (!received.isEmpty()) {
                receivedCeasefireRequests.put(guildId, received);
            }

            // 加载发送的请求
            List<CeasefireRequest> sent = warDAO.getSentCeasefireRequests(guildId);
            if (!sent.isEmpty()) {
                sentCeasefireRequests.put(guildId, sent);
            }
        }


    }

    /**
     * 重新加载所有停战请求
     */
    public void reloadCeasefireRequests() {
        loadCeasefireRequests();
    }

    /**
     * 获取公会收到的停战请求
     * @param guildId 公会ID
     * @return 停战请求列表
     */
    public List<CeasefireRequest> getReceivedCeasefireRequests(int guildId) {
        return receivedCeasefireRequests.getOrDefault(guildId, new ArrayList<>());
    }

    /**
     * 获取公会发送的停战请求
     * @param guildId 公会ID
     * @return 停战请求列表
     */
    public List<CeasefireRequest> getSentCeasefireRequests(int guildId) {
        return sentCeasefireRequests.getOrDefault(guildId, new ArrayList<>());
    }

    /**
     * 检查两个公会是否处于战争状态
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 是否处于战争状态
     */
    public boolean areGuildsAtWar(int guild1Id, int guild2Id) {
        for (GuildWar war : activeWars.values()) {
            if ((war.getAttackerId() == guild1Id && war.getDefenderId() == guild2Id) ||
                (war.getAttackerId() == guild2Id && war.getDefenderId() == guild1Id)) {
                return war.getStatus() == GuildWar.Status.ONGOING;
            }
        }
        return false;
    }

    /**
     * 获取两个公会之间的战争
     * @param guild1Id 第一个公会ID
     * @param guild2Id 第二个公会ID
     * @return 战争对象，不存在返回null
     */
    public GuildWar getActiveWarBetweenGuilds(int guild1Id, int guild2Id) {
        for (GuildWar war : activeWars.values()) {
            if ((war.getAttackerId() == guild1Id && war.getDefenderId() == guild2Id) ||
                (war.getAttackerId() == guild2Id && war.getDefenderId() == guild1Id)) {
                return war;
            }
        }
        return null;
    }

    /**
     * 请求停战
     * @param player 请求者
     * @param targetGuildId 目标公会ID
     * @return 是否成功
     */
    public boolean requestCeasefire(Player player, int targetGuildId) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限请求停战
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        int guildId = guild.getId();

        // 检查是否处于战争状态
        GuildWar war = getActiveWarBetweenGuilds(guildId, targetGuildId);
        if (war == null || war.getStatus() != GuildWar.Status.ONGOING) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-at-war"));
            return false;
        }

        // 检查是否已经发送过请求
        for (CeasefireRequest request : warDAO.getSentCeasefireRequests(guildId)) {
            if (request.getTargetId() == targetGuildId && request.getWarId() == war.getId()) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-already-requested"));
                return false;
            }
        }

        // 创建停战请求
        CeasefireRequest request = new CeasefireRequest(guildId, targetGuildId, war.getId());
        int requestId = warDAO.createCeasefireRequest(request);

        if (requestId == -1) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-request-failed"));
            return false;
        }

        // 更新缓存
        request.setId(requestId);
        List<CeasefireRequest> sentRequests = sentCeasefireRequests.computeIfAbsent(guildId, k -> new ArrayList<>());
        sentRequests.add(request);

        List<CeasefireRequest> receivedRequests = receivedCeasefireRequests.computeIfAbsent(targetGuildId, k -> new ArrayList<>());
        receivedRequests.add(request);

        // 通知目标公会在线成员
        Guild targetGuild = plugin.getGuildManager().getGuildById(targetGuildId);
        if (targetGuild != null) {
            for (GuildMember targetMember : plugin.getGuildManager().getGuildMembers(targetGuildId)) {
                Player targetPlayer = Bukkit.getPlayer(targetMember.getPlayerUuid());
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-request-received",
                            "guild", guild.getName()));
                }
            }
        }

        return true;
    }

    /**
     * 接受停战请求
     * @param guildId 接受方公会ID
     * @param requesterId 请求方公会ID
     * @return 是否成功
     */
    public boolean acceptCeasefire(int guildId, int requesterId) {
        // 获取战争
        GuildWar war = getActiveWarBetweenGuilds(guildId, requesterId);
        if (war == null || war.getStatus() != GuildWar.Status.ONGOING) {
            return false;
        }

        // 获取请求
        CeasefireRequest request = warDAO.getCeasefireRequestBetweenGuilds(requesterId, guildId, war.getId());
        if (request == null) {
            return false;
        }

        // 更新请求状态
        boolean success = warDAO.updateCeasefireRequestStatus(request.getId(), CeasefireRequest.Status.ACCEPTED);
        if (!success) {
            return false;
        }

        // 结束战争
        endWar(war);

        // 更新缓存
        List<CeasefireRequest> receivedRequests = receivedCeasefireRequests.get(guildId);
        if (receivedRequests != null) {
            receivedRequests.removeIf(r -> r.getId() == request.getId());
        }

        List<CeasefireRequest> sentRequests = sentCeasefireRequests.get(requesterId);
        if (sentRequests != null) {
            sentRequests.removeIf(r -> r.getId() == request.getId());
        }

        // 通知双方公会
        Guild requesterGuild = plugin.getGuildManager().getGuildById(requesterId);
        Guild targetGuild = plugin.getGuildManager().getGuildById(guildId);

        if (requesterGuild != null && targetGuild != null) {
            String message = plugin.getConfigManager().getMessage("guild.ceasefire-accepted-broadcast",
                    "requester", requesterGuild.getName(),
                    "target", targetGuild.getName());

            for (GuildMember member : plugin.getGuildManager().getGuildMembers(requesterId)) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }

            for (GuildMember member : plugin.getGuildManager().getGuildMembers(guildId)) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }

        return true;
    }

    /**
     * 拒绝停战请求
     * @param guildId 拒绝方公会ID
     * @param requesterId 请求方公会ID
     * @return 是否成功
     */
    public boolean rejectCeasefire(int guildId, int requesterId) {
        // 获取战争
        GuildWar war = getActiveWarBetweenGuilds(guildId, requesterId);
        if (war == null) {
            return false;
        }

        // 获取请求
        CeasefireRequest request = warDAO.getCeasefireRequestBetweenGuilds(requesterId, guildId, war.getId());
        if (request == null) {
            return false;
        }

        // 更新请求状态
        boolean success = warDAO.updateCeasefireRequestStatus(request.getId(), CeasefireRequest.Status.REJECTED);
        if (!success) {
            return false;
        }

        // 更新缓存
        List<CeasefireRequest> receivedRequests = receivedCeasefireRequests.get(guildId);
        if (receivedRequests != null) {
            receivedRequests.removeIf(r -> r.getId() == request.getId());
        }

        List<CeasefireRequest> sentRequests = sentCeasefireRequests.get(requesterId);
        if (sentRequests != null) {
            sentRequests.removeIf(r -> r.getId() == request.getId());
        }

        // 通知请求方公会
        Guild requesterGuild = plugin.getGuildManager().getGuildById(requesterId);
        Guild targetGuild = plugin.getGuildManager().getGuildById(guildId);

        if (requesterGuild != null && targetGuild != null) {
            String message = plugin.getConfigManager().getMessage("guild.ceasefire-rejected-notification",
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
     * 检查公会是否在战争中
     * @param guildId 公会ID
     * @return 是否在战争中
     */
    public boolean isGuildInWar(int guildId) {
        for (GuildWar war : activeWars.values()) {
            if (war.getStatus() == GuildWar.Status.ONGOING &&
                (war.getAttackerId() == guildId || war.getDefenderId() == guildId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通知战争参与者
     * @param war 公会战对象
     * @param message 消息
     */
    private void notifyWarParticipants(GuildWar war, String message) {
        // 通知攻击方
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(war.getAttackerId())) {
            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }

        // 通知防守方
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(war.getDefenderId())) {
            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * 拒绝公会战邀请
     * @param player 拒绝者
     * @param targetGuildName 发起公会名称
     * @return 是否成功
     */
    public boolean rejectWarInvitation(Player player, String targetGuildName) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限拒绝公会战
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 检查发起公会是否存在
        Guild targetGuild = plugin.getGuildManager().getGuildByName(targetGuildName);
        if (targetGuild == null) {
            player.sendMessage("§c找不到名为 §7" + targetGuildName + " §c的公会！");
            return false;
        }

        // 检查是否有邀请
        Map<Integer, Long> invites = warInvitations.get(guild.getId());
        if (invites == null || !invites.containsKey(targetGuild.getId())) {
            player.sendMessage("§c没有来自该公会的战争邀请！");
            return false;
        }

        // 移除邀请
        invites.remove(targetGuild.getId());

        // 通知双方公会
        player.sendMessage("§a你已拒绝来自 §e" + targetGuild.getName() + " §a的战争邀请！");
        
        // 通知发起方公会
        String rejectMessage = "§c" + guild.getName() + " §e拒绝了你们的战争邀请！";
        for (GuildMember targetMember : plugin.getGuildManager().getGuildMembers(targetGuild.getId())) {
            Player targetPlayer = Bukkit.getPlayer(targetMember.getPlayerUuid());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(rejectMessage);
            }
        }

        return true;
    }

    /**
     * 投降
     * @param player 投降的玩家
     * @return 是否成功
     */
    public boolean surrender(Player player) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限投降
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 获取当前战争
        GuildWar war = getActiveWar(guild.getId());
        if (war == null) {
            player.sendMessage("§c你的公会当前没有进行中的战争！");
            return false;
        }

        // 只能在进行中的战争中投降
        if (war.getStatus() != GuildWar.Status.ONGOING) {
            player.sendMessage("§c只能在进行中的战争中投降！");
            return false;
        }

        // 获取对方公会
        int opponentId = war.getOpponentId(guild.getId());
        Guild opponent = plugin.getGuildManager().getGuildById(opponentId);
        if (opponent == null) {
            player.sendMessage("§c无法获取对方公会信息！");
            return false;
        }

        // 结束战争，对方获胜
        boolean success = warDAO.endWar(war.getId(), opponentId);
        if (!success) {
            player.sendMessage("§c投降失败！");
            return false;
        }

        // 更新缓存
        war.setStatus(GuildWar.Status.FINISHED);
        war.setWinnerId(opponentId);
        war.setEndTime(new Date());

        // 移除活跃战争
        activeWars.remove(war.getId());

        // 取消战争任务
        BukkitTask task = warTasks.get(war.getId());
        if (task != null) {
            task.cancel();
            warTasks.remove(war.getId());
        }

        // 通知双方公会
        String surrenderMessage = "§c" + guild.getName() + " §e选择投降！ §a" + opponent.getName() + " §e获得胜利！";
        notifyWarParticipants(war, surrenderMessage);

        // 给获胜方奖励（可选，这里仅作示例）
        plugin.getLogger().info("公会 " + guild.getName() + " 向 " + opponent.getName() + " 投降");

        return true;
    }
}
