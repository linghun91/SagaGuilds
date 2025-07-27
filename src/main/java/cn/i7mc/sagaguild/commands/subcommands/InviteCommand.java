package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邀请玩家加入公会命令
 * 允许公会高层邀请玩家加入公会
 */
public class InviteCommand implements SubCommand {
    private final SagaGuild plugin;

    // 存储邀请信息，格式：<被邀请者UUID, <公会ID, 邀请者UUID, 邀请时间>>
    private static final Map<UUID, InviteInfo> invitations = new ConcurrentHashMap<>();

    // 邀请过期时间（毫秒）
    private static final long INVITE_EXPIRATION_TIME = 60 * 1000; // 1分钟

    // 定时任务ID
    private static int taskId = -1;

    /**
     * 邀请信息类
     */
    private static class InviteInfo {
        private final int guildId;
        private final UUID inviterUuid;
        private final long inviteTime;

        public InviteInfo(int guildId, UUID inviterUuid) {
            this.guildId = guildId;
            this.inviterUuid = inviterUuid;
            this.inviteTime = System.currentTimeMillis();
        }

        public int getGuildId() {
            return guildId;
        }

        public UUID getInviterUuid() {
            return inviterUuid;
        }

        public long getInviteTime() {
            return inviteTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - inviteTime > INVITE_EXPIRATION_TIME;
        }
    }

    public InviteCommand(SagaGuild plugin) {
        this.plugin = plugin;

        // 启动定时任务检查过期邀请
        startExpirationChecker();
    }

    /**
     * 启动邀请过期检查任务
     */
    private void startExpirationChecker() {
        // 每30秒检查一次过期邀请
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            checkExpiredInvitations();
        }, 20 * 30, 20 * 30).getTaskId(); // 30秒一次
    }

    /**
     * 停止邀请过期检查任务
     */
    public static void stopExpirationChecker() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * 检查并处理过期邀请
     */
    private static void checkExpiredInvitations() {
        if (invitations.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, InviteInfo>> iterator = invitations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, InviteInfo> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            InviteInfo invitation = entry.getValue();

            if (invitation.isExpired()) {
                // 通知玩家邀请已过期
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    SagaGuild plugin = SagaGuild.getInstance();
                    Guild guild = plugin.getGuildManager().getGuildById(invitation.getGuildId());
                    if (guild != null) {
                        player.sendMessage(plugin.getConfigManager().getMessage("members.invite-expired",
                                "guild", guild.getName()));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("members.invite-expired",
                                "guild", "未知公会"));
                    }
                }

                // 移除过期邀请
                iterator.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public String getDescription() {
        return "邀请玩家加入公会";
    }

    @Override
    public String getSyntax() {
        return "/guild invite <玩家名>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"inv", "i"};
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // 检查参数
        if (args.length < 1) {
            player.sendMessage("§c用法: " + getSyntax());
            return true;
        }

        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }

        // 检查玩家是否有权限邀请
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return true;
        }

        // 获取目标玩家
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§c找不到玩家 §7" + targetName + "§c！");
            return true;
        }

        // 检查目标玩家是否已经在公会中
        if (plugin.getGuildManager().getPlayerGuild(target.getUniqueId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.player-already-in-guild",
                    "player", target.getName()));
            return true;
        }

        // 发送邀请
        boolean success = plugin.getGuildManager().invitePlayer(player, target, guild.getId());
        if (!success) {
            player.sendMessage("§c邀请失败，请稍后再试！");
            return true;
        }

        // 存储邀请信息
        invitations.put(target.getUniqueId(), new InviteInfo(guild.getId(), player.getUniqueId()));

        // 发送邀请消息给邀请者
        player.sendMessage(plugin.getConfigManager().getMessage("members.invited",
                "player", target.getName()));

        // 发送带有点击按钮的邀请消息给被邀请者
        String messageText = plugin.getConfigManager().getMessage("members.invite-received",
                "guild", guild.getName()).replace("{prefix}", "");

        Component message = Component.text(messageText);

        Component acceptButton = Component.text("[同意加入]")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/guild inviteaccept"))
                .hoverEvent(Component.text("点击接受加入 " + guild.getName() + " 公会"));

        Component rejectButton = Component.text("[拒绝邀请]")
                .color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/guild invitereject"))
                .hoverEvent(Component.text("点击拒绝加入 " + guild.getName() + " 公会"));

        target.sendMessage(message.append(Component.newline())
                .append(acceptButton)
                .append(Component.text(" "))
                .append(rejectButton));

        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(input)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }

        return completions;
    }

    /**
     * 接受邀请
     * @param player 玩家
     * @return 是否成功
     */
    public static boolean acceptInvitation(Player player) {
        UUID playerUuid = player.getUniqueId();
        if (!invitations.containsKey(playerUuid)) {
            player.sendMessage(SagaGuild.getInstance().getConfigManager().getMessage("members.no-pending-invite"));
            return false;
        }

        InviteInfo invitation = invitations.get(playerUuid);

        // 检查邀请是否过期
        if (invitation.isExpired()) {
            invitations.remove(playerUuid);

            // 获取公会信息用于消息替换
            SagaGuild plugin = SagaGuild.getInstance();
            Guild guild = plugin.getGuildManager().getGuildById(invitation.getGuildId());
            if (guild != null) {
                player.sendMessage(plugin.getConfigManager().getMessage("members.invite-expired",
                        "guild", guild.getName()));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("members.invite-expired",
                        "guild", "未知公会"));
            }
            return false;
        }

        int guildId = invitation.getGuildId();
        UUID inviterUuid = invitation.getInviterUuid();

        // 从邀请列表中移除
        invitations.remove(playerUuid);

        // 获取公会和邀请者
        SagaGuild plugin = SagaGuild.getInstance();
        Guild guild = plugin.getGuildManager().getGuildById(guildId);
        if (guild == null) {
            player.sendMessage("§c邀请的公会不存在！");
            return false;
        }

        // 加入公会（通过邀请）
        boolean success = plugin.getGuildManager().joinGuild(player, guildId, true);
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.invite-accept-failed"));
            return false;
        }

        // 发送加入成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("members.invite-accepted",
                "guild", guild.getName()));

        // 通知公会成员
        plugin.getGuildManager().broadcastToGuild(guildId,
                plugin.getConfigManager().getMessage("guild.member-joined",
                        "player", player.getName()),
                player.getUniqueId());

        return true;
    }

    /**
     * 拒绝邀请
     * @param player 玩家
     * @return 是否成功
     */
    public static boolean rejectInvitation(Player player) {
        UUID playerUuid = player.getUniqueId();
        if (!invitations.containsKey(playerUuid)) {
            player.sendMessage(SagaGuild.getInstance().getConfigManager().getMessage("members.no-pending-invite"));
            return false;
        }

        InviteInfo invitation = invitations.get(playerUuid);

        // 检查邀请是否过期
        if (invitation.isExpired()) {
            invitations.remove(playerUuid);

            // 获取公会信息用于消息替换
            SagaGuild plugin = SagaGuild.getInstance();
            Guild guild = plugin.getGuildManager().getGuildById(invitation.getGuildId());
            if (guild != null) {
                player.sendMessage(plugin.getConfigManager().getMessage("members.invite-expired",
                        "guild", guild.getName()));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("members.invite-expired",
                        "guild", "未知公会"));
            }
            return false;
        }

        int guildId = invitation.getGuildId();
        UUID inviterUuid = invitation.getInviterUuid();

        // 从邀请列表中移除
        invitations.remove(playerUuid);

        // 获取公会和邀请者
        SagaGuild plugin = SagaGuild.getInstance();
        Guild guild = plugin.getGuildManager().getGuildById(guildId);
        if (guild == null) {
            player.sendMessage("§c邀请的公会不存在！");
            return false;
        }

        // 发送拒绝消息
        player.sendMessage(plugin.getConfigManager().getMessage("members.invite-rejected",
                "guild", guild.getName()));

        // 通知邀请者
        Player inviter = Bukkit.getPlayer(inviterUuid);
        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage(plugin.getConfigManager().getMessage("members.invite-player-rejected",
                    "player", player.getName()));
        }

        return true;
    }
}
