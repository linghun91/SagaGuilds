package cn.i7mc.sagaguild.commands.subcommands.activity;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildActivity;
import cn.i7mc.sagaguild.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 参加公会活动命令
 */
public class ActivityJoinCommand implements SubCommand {
    private final SagaGuild plugin;

    public ActivityJoinCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "参加公会活动";
    }

    @Override
    public String getSyntax() {
        return "/guild activity join <活动ID>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"participate", "attend"};
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
            PlayerUtil.sendMessage(player, Component.text("你不能参加其他公会的活动！", NamedTextColor.RED));
            return false;
        }

        // 检查活动状态
        if (activity.getStatus() != GuildActivity.Status.PLANNED) {
            PlayerUtil.sendMessage(player, Component.text("该活动已经开始、结束或被取消，无法参加！", NamedTextColor.RED));
            return false;
        }

        // 参加活动
        boolean success = plugin.getActivityManager().joinActivity(activityId, player.getUniqueId(), player.getName());
        if (!success) {
            PlayerUtil.sendMessage(player, Component.text("参加活动失败！可能你已经参加了该活动，或者活动人数已满。", NamedTextColor.RED));
            return false;
        }

        // 发送成功消息
        PlayerUtil.sendMessage(player, Component.text("成功参加活动 ", NamedTextColor.GREEN)
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
                // 获取公会活动列表
                List<GuildActivity> activities = plugin.getActivityManager().getGuildActivities(guild.getId());

                // 只显示计划中的活动
                for (GuildActivity activity : activities) {
                    if (activity.getStatus() == GuildActivity.Status.PLANNED) {
                        completions.add(String.valueOf(activity.getId()));
                    }
                }
            }
        }

        return completions;
    }
}
