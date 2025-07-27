package cn.i7mc.sagaguild.commands.subcommands.activity;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.ActivityParticipant;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildActivity;
import cn.i7mc.sagaguild.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 退出公会活动命令
 */
public class ActivityLeaveCommand implements SubCommand {
    private final SagaGuild plugin;

    public ActivityLeaveCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "退出公会活动";
    }

    @Override
    public String getSyntax() {
        return "/guild activity leave <活动ID>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"quit", "exit"};
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
            PlayerUtil.sendMessage(player, Component.text("你不能退出其他公会的活动！", NamedTextColor.RED));
            return false;
        }

        // 退出活动
        boolean success = plugin.getActivityManager().leaveActivity(activityId, player.getUniqueId());
        if (!success) {
            PlayerUtil.sendMessage(player, Component.text("退出活动失败！可能你没有参加该活动，或者活动已经开始。", NamedTextColor.RED));
            return false;
        }

        // 发送成功消息
        PlayerUtil.sendMessage(player, Component.text("成功退出活动 ", NamedTextColor.GREEN)
                .append(Component.text(activity.getName(), NamedTextColor.YELLOW))
                .append(Component.text("！", NamedTextColor.GREEN)));

        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 获取玩家参与的活动
            List<ActivityParticipant> participations = plugin.getActivityManager().getPlayerParticipations(player.getUniqueId());

            for (ActivityParticipant participation : participations) {
                // 获取活动
                GuildActivity activity = plugin.getActivityManager().getActivityById(participation.getActivityId());

                // 只显示计划中的活动
                if (activity != null && activity.getStatus() == GuildActivity.Status.PLANNED) {
                    completions.add(String.valueOf(activity.getId()));
                }
            }
        }

        return completions;
    }
}
