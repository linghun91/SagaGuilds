package cn.i7mc.sagaguild.commands.subcommands.activity;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildActivity;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 取消公会活动命令
 */
public class ActivityCancelCommand implements SubCommand {
    private final SagaGuild plugin;

    public ActivityCancelCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cancel";
    }

    @Override
    public String getDescription() {
        return "取消公会活动";
    }

    @Override
    public String getSyntax() {
        return "/guild activity cancel <活动ID>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"delete", "remove"};
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // 检查参数数量
        if (args.length < 1) {
            PlayerUtil.sendMessage(player, Component.text("用法: " + getSyntax(), NamedTextColor.RED));
            return false;
        }

        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            PlayerUtil.sendMessage(player, Component.text("你不在任何公会中！", NamedTextColor.RED));
            return false;
        }

        // 解析活动ID
        int activityId;
        try {
            activityId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            PlayerUtil.sendMessage(player, Component.text("无效的活动ID！", NamedTextColor.RED));
            return false;
        }

        // 获取活动
        GuildActivity activity = plugin.getActivityManager().getActivityById(activityId);
        if (activity == null) {
            PlayerUtil.sendMessage(player, Component.text("找不到指定的活动！", NamedTextColor.RED));
            return false;
        }

        // 检查活动是否属于玩家所在公会
        if (activity.getGuildId() != guild.getId()) {
            PlayerUtil.sendMessage(player, Component.text("你不能取消其他公会的活动！", NamedTextColor.RED));
            return false;
        }

        // 检查权限
        boolean isCreator = activity.getCreatorUuid().equals(player.getUniqueId());
        boolean isOwner = guild.getOwnerUuid().equals(player.getUniqueId());
        boolean isAdmin = false;

        GuildMember member = plugin.getGuildManager().getGuildMember(guild.getId(), player.getUniqueId());
        if (member != null && member.isAdmin()) {
            isAdmin = true;
        }

        if (!isCreator && !isOwner && !isAdmin && !player.hasPermission("guild.admin")) {
            PlayerUtil.sendMessage(player, Component.text("你没有权限取消该活动！", NamedTextColor.RED));
            return false;
        }

        // 检查活动状态
        if (activity.getStatus() != GuildActivity.Status.PLANNED && activity.getStatus() != GuildActivity.Status.ONGOING) {
            PlayerUtil.sendMessage(player, Component.text("该活动已经结束或被取消，无法再次取消！", NamedTextColor.RED));
            return false;
        }

        // 取消活动
        boolean success = plugin.getActivityManager().cancelActivity(activityId);
        if (!success) {
            PlayerUtil.sendMessage(player, Component.text("取消活动失败！", NamedTextColor.RED));
            return false;
        }

        // 发送成功消息
        PlayerUtil.sendMessage(player, Component.text("成功取消活动 ", NamedTextColor.GREEN)
                .append(Component.text(activity.getName(), NamedTextColor.YELLOW))
                .append(Component.text("！", NamedTextColor.GREEN)));

        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 获取玩家所在公会
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                // 检查权限
                boolean isOwner = guild.getOwnerUuid().equals(player.getUniqueId());
                boolean isAdmin = false;

                GuildMember member = plugin.getGuildManager().getGuildMember(guild.getId(), player.getUniqueId());
                if (member != null && member.getRole() == GuildMember.Role.ADMIN) {
                    isAdmin = true;
                }

                // 获取公会活动列表
                List<GuildActivity> activities = plugin.getActivityManager().getGuildActivities(guild.getId());

                for (GuildActivity activity : activities) {
                    // 只显示计划中或进行中的活动，且玩家有权限取消
                    if ((activity.getStatus() == GuildActivity.Status.PLANNED || activity.getStatus() == GuildActivity.Status.ONGOING) &&
                        (isOwner || isAdmin || activity.getCreatorUuid().equals(player.getUniqueId()) || player.hasPermission("guild.admin"))) {
                        completions.add(String.valueOf(activity.getId()));
                    }
                }
            }
        }

        return completions;
    }
}
