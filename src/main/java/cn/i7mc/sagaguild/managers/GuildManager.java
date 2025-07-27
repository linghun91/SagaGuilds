package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.GuildDAO;
import cn.i7mc.sagaguild.data.dao.JoinRequestDAO;
import cn.i7mc.sagaguild.data.dao.MemberDAO;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.JoinRequest;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 公会管理器
 * 负责公会的创建、解散和管理
 */
public class GuildManager {
    private final SagaGuild plugin;
    private final GuildDAO guildDAO;
    private final MemberDAO memberDAO;
    private JoinRequestDAO joinRequestDAO;

    // 缓存公会数据
    private final Map<Integer, Guild> guildsById;
    private final Map<String, Guild> guildsByName;
    private final Map<String, Guild> guildsByTag;
    private final Map<UUID, Integer> playerGuildMap;

    public GuildManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.guildDAO = new GuildDAO(plugin);
        this.memberDAO = new MemberDAO(plugin);
        this.joinRequestDAO = plugin.getJoinRequestDAO();

        this.guildsById = new HashMap<>();
        this.guildsByName = new HashMap<>();
        this.guildsByTag = new HashMap<>();
        this.playerGuildMap = new HashMap<>();

        // 加载所有公会数据到缓存
        loadGuilds();
    }

    /**
     * 从数据库加载所有公会数据到缓存
     */
    private void loadGuilds() {
        // 清空缓存
        guildsById.clear();
        guildsByName.clear();
        guildsByTag.clear();
        playerGuildMap.clear();

        // 加载所有公会
        List<Guild> guilds = guildDAO.getAllGuilds();
        for (Guild guild : guilds) {
            guildsById.put(guild.getId(), guild);
            guildsByName.put(guild.getName().toLowerCase(), guild);
            guildsByTag.put(guild.getTag().toLowerCase(), guild);

            // 加载公会成员
            List<GuildMember> members = memberDAO.getGuildMembers(guild.getId());
            for (GuildMember member : members) {
                playerGuildMap.put(member.getPlayerUuid(), guild.getId());
            }
        }

        plugin.getLogger().info("已加载 " + guilds.size() + " 个公会数据！");
    }

    /**
     * 重新加载所有公会数据
     */
    public void reloadGuilds() {
        loadGuilds();
    }

    /**
     * 创建新公会
     * @param owner 会长
     * @param name 公会名称
     * @param tag 公会标签
     * @param description 公会描述
     * @return 创建结果，成功返回公会对象，失败返回null
     */
    public Guild createGuild(Player owner, String name, String tag, String description) {
        // 检查玩家是否已经在公会中
        if (getPlayerGuild(owner.getUniqueId()) != null) {
            return null;
        }

        // 检查公会名称和标签是否已存在
        if (guildsByName.containsKey(name.toLowerCase()) || guildsByTag.containsKey(tag.toLowerCase())) {
            return null;
        }

        // 检查名称和标签长度
        FileConfiguration config = plugin.getConfig();
        int minNameLength = config.getInt("guild.min-name-length", 3);
        int maxNameLength = config.getInt("guild.max-name-length", 16);
        int minTagLength = config.getInt("guild.min-tag-length", 2);
        int maxTagLength = config.getInt("guild.max-tag-length", 5);

        if (name.length() < minNameLength || name.length() > maxNameLength) {
            return null;
        }

        if (tag.length() < minTagLength || tag.length() > maxTagLength) {
            return null;
        }

        // 创建公会
        Guild guild = new Guild(name, tag, description, owner.getUniqueId());
        int guildId = guildDAO.createGuild(guild);

        if (guildId == -1) {
            return null;
        }

        // 添加会长为成员
        GuildMember member = new GuildMember(guildId, owner.getUniqueId(), owner.getName(), GuildMember.Role.OWNER);
        int memberId = memberDAO.addMember(member);

        if (memberId == -1) {
            // 创建成员失败，删除公会
            guildDAO.deleteGuild(guildId);
            return null;
        }

        // 更新缓存
        guildsById.put(guildId, guild);
        guildsByName.put(name.toLowerCase(), guild);
        guildsByTag.put(tag.toLowerCase(), guild);
        playerGuildMap.put(owner.getUniqueId(), guildId);

        return guild;
    }

    /**
     * 解散公会
     * @param player 执行解散的玩家
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean disbandGuild(Player player, int guildId) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        // 检查是否是会长
        if (!guild.getOwnerUuid().equals(player.getUniqueId())) {
            return false;
        }

        return performGuildDisband(guildId);
    }

    /**
     * 强制解散公会（管理员权限）
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean forceDisband(int guildId) {
        return performGuildDisband(guildId);
    }

    /**
     * 执行公会解散的核心逻辑
     * @param guildId 公会ID
     * @return 是否成功
     */
    private boolean performGuildDisband(int guildId) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        // 获取所有成员（在删除前获取，用于通知）
        List<GuildMember> members = memberDAO.getGuildMembers(guildId);

        // 清理公会相关的所有数据
        cleanupGuildData(guildId);

        // 删除公会
        boolean success = guildDAO.deleteGuild(guildId);
        if (!success) {
            return false;
        }

        // 删除所有成员
        memberDAO.deleteAllGuildMembers(guildId);

        // 更新缓存
        guildsById.remove(guildId);
        guildsByName.remove(guild.getName().toLowerCase());
        guildsByTag.remove(guild.getTag().toLowerCase());

        for (GuildMember member : members) {
            playerGuildMap.remove(member.getPlayerUuid());
        }

        // 通知所有在线成员公会已解散
        notifyMembersGuildDisbanded(members, guild.getName());

        return true;
    }

    /**
     * 清理公会相关的所有数据
     * @param guildId 公会ID
     */
    private void cleanupGuildData(int guildId) {
        try {
            // 清理公会领地
            if (plugin.getLandManager() != null) {
                plugin.getLandManager().unclaimAllGuildLands(guildId);
            }

            // 清理公会联盟
            if (plugin.getAllianceManager() != null) {
                plugin.getAllianceManager().cleanupGuildAlliances(guildId);
            }

            // TODO: 清理公会战争、活动、任务等其他数据
            // 这些功能的清理方法将在后续实现

        } catch (Exception e) {
            plugin.getLogger().warning("清理公会数据时发生错误: " + e.getMessage());
        }
    }

    /**
     * 通知所有在线成员公会已解散
     * @param members 成员列表
     * @param guildName 公会名称
     */
    private void notifyMembersGuildDisbanded(List<GuildMember> members, String guildName) {
        for (GuildMember member : members) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getPlayerUuid());
            if (offlinePlayer.isOnline()) {
                Player onlinePlayer = (Player) offlinePlayer;
                onlinePlayer.sendMessage(plugin.getConfigManager().getMessage("guild.disbanded", "guild", guildName));
            }
        }
    }

    /**
     * 邀请玩家加入公会
     * @param inviter 邀请者
     * @param target 被邀请者
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean invitePlayer(Player inviter, Player target, int guildId) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        // 检查邀请者是否有权限
        GuildMember inviterMember = getMemberByUuid(inviter.getUniqueId());
        if (inviterMember == null || !inviterMember.isElder()) {
            return false;
        }

        // 检查被邀请者是否已经在公会中
        if (getPlayerGuild(target.getUniqueId()) != null) {
            return false;
        }

        // 检查公会成员是否已满
        int guildLevel = guild.getLevel();
        int maxMembers = getMaxMembersByLevel(guildLevel);
        int currentMembers = memberDAO.getGuildMemberCount(guildId);

        if (currentMembers >= maxMembers) {
            // 发送成员已满消息
            inviter.sendMessage(plugin.getConfigManager().getMessage("guild.members-full",
                    "level", String.valueOf(guildLevel),
                    "max", String.valueOf(maxMembers)));
            return false;
        }

        // 邀请过期机制已在InviteCommand类中实现，默认1分钟后过期

        return true;
    }

    /**
     * 申请加入公会
     * @param player 玩家
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean requestJoinGuild(Player player, int guildId) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        // 检查玩家是否已经在公会中
        if (getPlayerGuild(player.getUniqueId()) != null) {
            return false;
        }

        // 检查公会成员是否已满
        int guildLevel = guild.getLevel();
        int maxMembers = getMaxMembersByLevel(guildLevel);
        int currentMembers = memberDAO.getGuildMemberCount(guildId);

        if (currentMembers >= maxMembers) {
            // 发送成员已满消息
            player.sendMessage(plugin.getConfigManager().getMessage("guild.members-full",
                    "level", String.valueOf(guildLevel),
                    "max", String.valueOf(maxMembers)));
            return false;
        }

        // 创建加入请求
        JoinRequest request = new JoinRequest(player.getUniqueId(), player.getName(), guildId);
        int requestId = joinRequestDAO.createJoinRequest(request);

        if (requestId == -1) {
            return false;
        }

        // 通知玩家
        player.sendMessage(plugin.getConfigManager().getMessage("guild.join-requested",
                "guild", guild.getName()));

        // 通知公会管理员
        List<GuildMember> admins = getGuildAdmins(guildId);
        for (GuildMember admin : admins) {
            Player adminPlayer = Bukkit.getPlayer(admin.getPlayerUuid());
            if (adminPlayer != null && adminPlayer.isOnline()) {
                adminPlayer.sendMessage(plugin.getConfigManager().getMessage("guild.join-request-received",
                        "player", player.getName()));
            }
        }

        return true;
    }

    /**
     * 接受加入请求
     * @param admin 管理员
     * @param requestId 请求ID
     * @return 是否成功
     */
    public boolean acceptJoinRequest(Player admin, int requestId) {
        // 获取请求
        JoinRequest request = joinRequestDAO.getJoinRequestById(requestId);
        if (request == null) {
            return false;
        }

        if (request.getStatus() != JoinRequest.Status.PENDING) {
            return false;
        }

        // 检查管理员是否有权限
        Guild guild = getGuildById(request.getGuildId());
        GuildMember adminMember = getMemberByUuid(admin.getUniqueId());

        if (guild == null) {
            return false;
        }

        if (adminMember == null) {
            return false;
        }

        if (!adminMember.isElder()) {
            return false;
        }

        if (adminMember.getGuildId() != request.getGuildId()) {
            return false;
        }

        // 检查公会成员是否已满
        int guildLevel = guild.getLevel();
        int maxMembers = getMaxMembersByLevel(guildLevel);
        int currentMembers = memberDAO.getGuildMemberCount(guild.getId());

        if (currentMembers >= maxMembers) {
            // 发送成员已满消息
            admin.sendMessage(plugin.getConfigManager().getMessage("guild.members-full",
                    "level", String.valueOf(guildLevel),
                    "max", String.valueOf(maxMembers)));
            return false;
        }

        // 更新请求状态
        request.setStatus(JoinRequest.Status.ACCEPTED);
        boolean updated = joinRequestDAO.updateJoinRequestStatus(requestId, JoinRequest.Status.ACCEPTED);
        if (!updated) {
            return false;
        }

        // 添加成员
        GuildMember member = new GuildMember(guild.getId(), request.getPlayerUuid(), request.getPlayerName(), GuildMember.Role.MEMBER);
        int memberId = memberDAO.addMember(member);

        if (memberId == -1) {
            return false;
        }

        // 更新缓存
        playerGuildMap.put(request.getPlayerUuid(), guild.getId());

        // 删除该玩家的所有其他请求
        joinRequestDAO.deletePlayerJoinRequests(request.getPlayerUuid());

        // 通知玩家
        Player player = Bukkit.getPlayer(request.getPlayerUuid());
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.join-accepted",
                    "guild", guild.getName()));
        }

        // 通知公会成员
        broadcastToGuild(guild.getId(),
                plugin.getConfigManager().getMessage("guild.member-joined",
                        "player", request.getPlayerName()),
                request.getPlayerUuid());

        return true;
    }

    /**
     * 拒绝加入请求
     * @param admin 管理员
     * @param requestId 请求ID
     * @return 是否成功
     */
    public boolean rejectJoinRequest(Player admin, int requestId) {
        // 获取请求
        JoinRequest request = joinRequestDAO.getJoinRequestById(requestId);
        if (request == null) {
            return false;
        }

        if (request.getStatus() != JoinRequest.Status.PENDING) {
            return false;
        }

        // 检查管理员是否有权限
        Guild guild = getGuildById(request.getGuildId());
        GuildMember adminMember = getMemberByUuid(admin.getUniqueId());

        if (guild == null) {
            return false;
        }

        if (adminMember == null) {
            return false;
        }

        if (!adminMember.isElder()) {
            return false;
        }

        if (adminMember.getGuildId() != request.getGuildId()) {
            return false;
        }

        // 更新请求状态
        request.setStatus(JoinRequest.Status.REJECTED);
        boolean updated = joinRequestDAO.updateJoinRequestStatus(requestId, JoinRequest.Status.REJECTED);
        if (!updated) {
            return false;
        }

        // 通知玩家
        Player player = Bukkit.getPlayer(request.getPlayerUuid());
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.join-rejected",
                    "guild", guild.getName()));
        }

        return true;
    }

    /**
     * 获取公会的加入请求
     * @param guildId 公会ID
     * @return 请求列表
     */
    public List<JoinRequest> getGuildJoinRequests(int guildId) {
        return joinRequestDAO.getGuildJoinRequests(guildId);
    }

    /**
     * 获取公会的管理员成员（会长、副会长、长老）
     * @param guildId 公会ID
     * @return 管理员成员列表
     */
    public List<GuildMember> getGuildAdmins(int guildId) {
        List<GuildMember> members = memberDAO.getGuildMembers(guildId);
        List<GuildMember> admins = new ArrayList<>();

        for (GuildMember member : members) {
            if (member.isElder()) {
                admins.add(member);
            }
        }

        return admins;
    }

    /**
     * 玩家加入公会
     * @param player 玩家
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean joinGuild(Player player, int guildId) {
        return joinGuild(player, guildId, false);
    }

    /**
     * 玩家加入公会（支持邀请）
     * @param player 玩家
     * @param guildId 公会ID
     * @param fromInvitation 是否通过邀请加入
     * @return 是否成功
     */
    public boolean joinGuild(Player player, int guildId, boolean fromInvitation) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        // 检查玩家是否已经在公会中
        if (getPlayerGuild(player.getUniqueId()) != null) {
            return false;
        }

        // 检查公会是否公开（如果是通过邀请加入，则跳过此检查）
        if (!guild.isPublic() && !fromInvitation) {
            // 检查是否有邀请
            player.sendMessage(plugin.getConfigManager().getMessage("guild.need-invitation"));
            return false;
        }

        // 检查公会成员是否已满
        int guildLevel = guild.getLevel();
        int maxMembers = getMaxMembersByLevel(guildLevel);
        int currentMembers = memberDAO.getGuildMemberCount(guildId);

        if (currentMembers >= maxMembers) {
            // 发送成员已满消息
            player.sendMessage(plugin.getConfigManager().getMessage("guild.members-full",
                    "level", String.valueOf(guildLevel),
                    "max", String.valueOf(maxMembers)));
            return false;
        }

        // 添加成员
        GuildMember member = new GuildMember(guildId, player.getUniqueId(), player.getName(), GuildMember.Role.MEMBER);
        int memberId = memberDAO.addMember(member);

        if (memberId == -1) {
            return false;
        }

        // 更新缓存
        playerGuildMap.put(player.getUniqueId(), guildId);

        return true;
    }

    /**
     * 玩家离开公会
     * @param player 玩家
     * @return 是否成功
     */
    public boolean leaveGuild(Player player) {
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return false;
        }

        // 检查是否是会长
        if (guild.getOwnerUuid().equals(player.getUniqueId())) {
            return false;
        }

        // 获取成员
        GuildMember member = getMemberByUuid(player.getUniqueId());
        if (member == null) {
            return false;
        }

        // 删除成员
        boolean success = memberDAO.deleteMember(member.getId());
        if (!success) {
            return false;
        }

        // 更新缓存
        playerGuildMap.remove(player.getUniqueId());

        return true;
    }

    /**
     * 踢出公会成员
     * @param kicker 执行踢出的玩家
     * @param targetUuid 被踢出的玩家UUID
     * @return 是否成功
     */
    public boolean kickMember(Player kicker, UUID targetUuid) {
        Guild guild = getPlayerGuild(kicker.getUniqueId());
        if (guild == null) {
            return false;
        }

        // 获取执行者和目标成员
        GuildMember kickerMember = getMemberByUuid(kicker.getUniqueId());
        GuildMember targetMember = getMemberByUuid(targetUuid);

        if (kickerMember == null || targetMember == null) {
            return false;
        }

        // 检查是否有权限踢出
        if (!kickerMember.canKick(targetMember.getRole())) {
            return false;
        }

        // 删除成员
        boolean success = memberDAO.deleteMember(targetMember.getId());
        if (!success) {
            return false;
        }

        // 更新缓存
        playerGuildMap.remove(targetUuid);

        // 通知被踢出的玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        if (target.isOnline()) {
            String message = plugin.getConfigManager().getMessage("members.kicked",
                    "guild", guild.getName());
            ((Player) target).sendMessage(message);
        }

        return true;
    }

    /**
     * 提升成员职位
     * @param promoter 执行提升的玩家
     * @param targetUuid 被提升的玩家UUID
     * @return 是否成功
     */
    public boolean promoteMember(Player promoter, UUID targetUuid) {
        Guild guild = getPlayerGuild(promoter.getUniqueId());
        if (guild == null) {
            return false;
        }

        // 获取执行者和目标成员
        GuildMember promoterMember = getMemberByUuid(promoter.getUniqueId());
        GuildMember targetMember = getMemberByUuid(targetUuid);

        if (promoterMember == null || targetMember == null) {
            return false;
        }

        // 确定新角色
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole;

        switch (currentRole) {
            case MEMBER:
                newRole = GuildMember.Role.ELDER;
                break;
            case ELDER:
                newRole = GuildMember.Role.ADMIN;
                break;
            default:
                return false;
        }

        // 检查是否有权限提升
        if (!promoterMember.canPromote(newRole)) {
            return false;
        }

        // 更新成员角色
        targetMember.setRole(newRole);
        boolean success = memberDAO.updateMember(targetMember);
        if (!success) {
            return false;
        }

        // 通知被提升的玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        if (target.isOnline()) {
            String message = plugin.getConfigManager().getMessage("members.promoted",
                    "role", newRole.getDisplayName());
            ((Player) target).sendMessage(message);
        }

        return true;
    }

    /**
     * 降级成员职位
     * @param demoter 执行降级的玩家
     * @param targetUuid 被降级的玩家UUID
     * @return 是否成功
     */
    public boolean demoteMember(Player demoter, UUID targetUuid) {
        Guild guild = getPlayerGuild(demoter.getUniqueId());
        if (guild == null) {
            return false;
        }

        // 获取执行者和目标成员
        GuildMember demoterMember = getMemberByUuid(demoter.getUniqueId());
        GuildMember targetMember = getMemberByUuid(targetUuid);

        if (demoterMember == null || targetMember == null) {
            return false;
        }

        // 确定新角色
        GuildMember.Role currentRole = targetMember.getRole();
        GuildMember.Role newRole;

        switch (currentRole) {
            case ADMIN:
                newRole = GuildMember.Role.ELDER;
                break;
            case ELDER:
                newRole = GuildMember.Role.MEMBER;
                break;
            default:
                return false;
        }

        // 检查是否有权限降级
        if (!demoterMember.canDemote(currentRole)) {
            return false;
        }

        // 更新成员角色
        targetMember.setRole(newRole);
        boolean success = memberDAO.updateMember(targetMember);
        if (!success) {
            return false;
        }

        // 通知被降级的玩家
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        if (target.isOnline()) {
            String message = plugin.getConfigManager().getMessage("members.demoted",
                    "role", newRole.getDisplayName());
            ((Player) target).sendMessage(message);
        }

        return true;
    }

    /**
     * 转让公会会长
     * @param currentOwner 当前会长
     * @param newOwnerUuid 新会长UUID
     * @return 是否成功
     */
    public boolean transferOwnership(Player currentOwner, UUID newOwnerUuid) {
        Guild guild = getPlayerGuild(currentOwner.getUniqueId());
        if (guild == null) {
            return false;
        }

        // 检查是否是会长
        if (!guild.getOwnerUuid().equals(currentOwner.getUniqueId())) {
            return false;
        }

        // 获取新会长成员
        GuildMember newOwnerMember = getMemberByUuid(newOwnerUuid);
        if (newOwnerMember == null || newOwnerMember.getGuildId() != guild.getId()) {
            return false;
        }

        // 更新公会会长
        guild.setOwnerUuid(newOwnerUuid);
        boolean success = guildDAO.updateGuild(guild);
        if (!success) {
            return false;
        }

        // 更新成员角色
        GuildMember currentOwnerMember = getMemberByUuid(currentOwner.getUniqueId());
        currentOwnerMember.setRole(GuildMember.Role.ADMIN);
        memberDAO.updateMember(currentOwnerMember);

        newOwnerMember.setRole(GuildMember.Role.OWNER);
        memberDAO.updateMember(newOwnerMember);

        // 通知新会长
        OfflinePlayer newOwner = Bukkit.getOfflinePlayer(newOwnerUuid);
        if (newOwner.isOnline()) {
            String message = plugin.getConfigManager().getMessage("members.promoted",
                    "role", GuildMember.Role.OWNER.getDisplayName());
            ((Player) newOwner).sendMessage(message);
        }

        return true;
    }

    /**
     * 根据ID获取公会
     * @param id 公会ID
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildById(int id) {
        return guildsById.get(id);
    }

    /**
     * 根据名称获取公会
     * @param name 公会名称
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildByName(String name) {
        return guildsByName.get(name.toLowerCase());
    }

    /**
     * 根据标签获取公会
     * @param tag 公会标签
     * @return 公会对象，不存在返回null
     */
    public Guild getGuildByTag(String tag) {
        return guildsByTag.get(tag.toLowerCase());
    }

    /**
     * 获取玩家所在的公会
     * @param playerUuid 玩家UUID
     * @return 公会对象，不存在返回null
     */
    public Guild getPlayerGuild(UUID playerUuid) {
        Integer guildId = playerGuildMap.get(playerUuid);
        return guildId != null ? guildsById.get(guildId) : null;
    }

    /**
     * 获取玩家的公会成员信息
     * @param playerUuid 玩家UUID
     * @return 成员对象，不存在返回null
     */
    public GuildMember getMemberByUuid(UUID playerUuid) {
        Integer guildId = playerGuildMap.get(playerUuid);
        if (guildId == null) {
            return null;
        }

        return memberDAO.getMemberByGuildAndUuid(guildId, playerUuid);
    }

    /**
     * 获取所有公会
     * @return 公会列表
     */
    public List<Guild> getAllGuilds() {
        return guildDAO.getAllGuilds();
    }

    /**
     * 获取公会成员列表
     * @param guildId 公会ID
     * @return 成员列表
     */
    public List<GuildMember> getGuildMembers(int guildId) {
        return memberDAO.getGuildMembers(guildId);
    }

    /**
     * 获取公会成员数量
     * @param guildId 公会ID
     * @return 成员数量
     */
    public int getGuildMemberCount(int guildId) {
        return memberDAO.getGuildMemberCount(guildId);
    }

    /**
     * 增加公会经验
     * @param guildId 公会ID
     * @param amount 经验数量
     * @return 是否升级
     */
    public boolean addGuildExperience(int guildId, int amount) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        // 增加经验
        boolean levelUp = guild.addExperience(amount);

        // 更新数据库
        guildDAO.updateGuild(guild);

        return levelUp;
    }
    
    /**
     * 设置公会经验值（管理员专用）
     * @param guildId 公会ID
     * @param experience 新的经验值
     * @return 是否成功
     */
    public boolean setGuildExperience(int guildId, int experience) {
        Guild guild = getGuildById(guildId);
        if (guild == null) {
            return false;
        }
        
        // 设置经验值
        guild.setExperience(experience);
        
        // 根据新经验值重新计算等级
        calculateGuildLevel(guild);
        
        // 更新数据库
        boolean success = guildDAO.updateGuild(guild);
        
        if (success) {
            // 更新缓存
            guildsById.put(guild.getId(), guild);
            guildsByName.put(guild.getName().toLowerCase(), guild);
        }
        
        return success;
    }
    
    /**
     * 根据经验值计算并设置公会等级
     * @param guild 公会对象
     */
    private void calculateGuildLevel(Guild guild) {
        int experience = guild.getExperience();
        int level = 1;
        
        // 从配置文件获取最大等级
        int maxLevel = plugin.getConfig().getInt("levels.max-level", 10);
        
        // 根据经验值计算等级
        for (int i = 1; i <= maxLevel; i++) {
            int requiredExp = getRequiredExperienceForLevel(i);
            if (experience >= requiredExp) {
                level = i;
            } else {
                break;
            }
        }
        
        guild.setLevel(level);
    }
    
    /**
     * 获取指定等级所需的经验值
     * @param level 等级
     * @return 所需经验值
     */
    private int getRequiredExperienceForLevel(int level) {
        // 使用配置文件中的等级系统设置
        if (level <= 1) {
            return 0;
        }
        
        FileConfiguration config = plugin.getConfig();
        int baseExp = config.getInt("levels.base-exp", 1000);
        int expIncrease = config.getInt("levels.exp-increase", 500);
        int maxLevel = config.getInt("levels.max-level", 10);
        
        // 超过最大等级返回最大值
        if (level > maxLevel) {
            return Integer.MAX_VALUE;
        }
        
        // 计算累积经验需求
        // 1级: 0, 2级: base-exp, 3级: base-exp + exp-increase, 4级: base-exp + exp-increase * 2, ...
        return baseExp + (expIncrease * (level - 2));
    }

    /**
     * 获取公会等级
     * @param guildId 公会ID
     * @return 公会等级
     */
    public int getGuildLevel(int guildId) {
        Guild guild = getGuildById(guildId);
        return guild != null ? guild.getLevel() : 0;
    }

    /**
     * 获取公会经验
     * @param guildId 公会ID
     * @return 公会经验
     */
    public int getGuildExperience(int guildId) {
        Guild guild = getGuildById(guildId);
        return guild != null ? guild.getExperience() : 0;
    }

    /**
     * 获取公会下一级所需经验
     * @param guildId 公会ID
     * @return 下一级所需经验
     */
    public int getGuildNextLevelExperience(int guildId) {
        Guild guild = getGuildById(guildId);
        return guild != null ? guild.getNextLevelExperience() : 0;
    }

    /**
     * 获取指定等级公会的最大成员数量
     * @param level 公会等级
     * @return 最大成员数量
     */
    public int getMaxMembersByLevel(int level) {
        // 获取配置
        FileConfiguration config = plugin.getConfig();

        // 尝试从配置中获取指定等级的最大成员数量
        int maxMembers = config.getInt("levels.max-members-per-level." + level, -1);

        // 如果配置中没有指定该等级的最大成员数量，使用默认值
        if (maxMembers == -1) {
            // 默认值：10 + (等级 - 1) * 10
            maxMembers = 10 + (level - 1) * 10;

            // 确保不超过最大值100
            if (maxMembers > 100) {
                maxMembers = 100;
            }
        }

        return maxMembers;
    }

    /**
     * 更新公会信息
     * @param guild 公会对象
     * @return 是否成功
     */
    public boolean updateGuild(Guild guild) {
        // 更新数据库
        boolean success = guildDAO.updateGuild(guild);

        if (success) {
            // 更新缓存
            guildsById.put(guild.getId(), guild);
            guildsByName.put(guild.getName().toLowerCase(), guild);
            guildsByTag.put(guild.getTag().toLowerCase(), guild);
        }

        return success;
    }

    /**
     * 向公会所有在线成员广播消息
     * @param guildId 公会ID
     * @param message 消息
     * @param excludeUuid 排除的玩家UUID（可为null）
     */
    public void broadcastToGuild(int guildId, String message, UUID excludeUuid) {
        // 获取公会成员
        List<GuildMember> members = getGuildMembers(guildId);

        // 向所有在线成员发送消息
        for (GuildMember member : members) {
            // 排除指定玩家
            if (excludeUuid != null && member.getPlayerUuid().equals(excludeUuid)) {
                continue;
            }

            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * 获取指定公会中的成员信息
     * @param guildId 公会ID
     * @param playerUuid 玩家UUID
     * @return 成员对象，不存在返回null
     */
    public GuildMember getGuildMember(int guildId, UUID playerUuid) {
        return memberDAO.getMemberByGuildAndUuid(guildId, playerUuid);
    }
}
