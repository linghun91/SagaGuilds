package cn.i7mc.sagaguild.commands.subcommands.activity;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.ActivityParticipant;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildActivity;
import cn.i7mc.sagaguild.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 查看公会活动详情命令
 */
public class ActivityInfoCommand implements SubCommand {
    private final SagaGuild plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ActivityInfoCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "查看公会活动详情";
    }

    @Override
    public String getSyntax() {
        return "/guild activity info <活动ID>";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"details", "view"};
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
            PlayerUtil.sendMessage(player, Component.text("你不能查看其他公会的活动！", NamedTextColor.RED));
            return false;
        }

        // 获取活动状态
        GuildActivity.Status status = activity.getCurrentStatus();
        Component statusComponent;

        switch (status) {
            case PLANNED:
                statusComponent = Component.text("[计划中]", NamedTextColor.YELLOW);
                break;
            case ONGOING:
                statusComponent = Component.text("[进行中]", NamedTextColor.GREEN);
                break;
            case COMPLETED:
                statusComponent = Component.text("[已完成]", NamedTextColor.GRAY);
                break;
            case CANCELLED:
                statusComponent = Component.text("[已取消]", NamedTextColor.RED);
                break;
            default:
                statusComponent = Component.text("[未知]", NamedTextColor.DARK_GRAY);
        }

        // 获取创建者名称
        String creatorName = "未知";
        Player creator = Bukkit.getPlayer(activity.getCreatorUuid());
        if (creator != null) {
            creatorName = creator.getName();
        } else {
            creatorName = Bukkit.getOfflinePlayer(activity.getCreatorUuid()).getName();
            if (creatorName == null) {
                creatorName = "未知";
            }
        }

        // 获取参与者列表
        List<ActivityParticipant> participants = plugin.getActivityManager().getActivityParticipants(activity.getId());

        // 发送活动详情
        PlayerUtil.sendMessage(player, Component.text("=== 活动详情 ===", NamedTextColor.GOLD));

        PlayerUtil.sendMessage(player,
            Component.text("ID: ", NamedTextColor.GRAY)
                .append(Component.text("#" + activity.getId(), NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("状态: ", NamedTextColor.GRAY))
                .append(statusComponent)
        );

        PlayerUtil.sendMessage(player,
            Component.text("名称: ", NamedTextColor.GRAY)
                .append(Component.text(activity.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("类型: ", NamedTextColor.GRAY))
                .append(Component.text(activity.getType().getDisplayName(), NamedTextColor.AQUA))
        );

        PlayerUtil.sendMessage(player,
            Component.text("开始时间: ", NamedTextColor.GRAY)
                .append(Component.text(dateFormat.format(activity.getStartTime()), NamedTextColor.WHITE))
        );

        PlayerUtil.sendMessage(player,
            Component.text("结束时间: ", NamedTextColor.GRAY)
                .append(Component.text(dateFormat.format(activity.getEndTime()), NamedTextColor.WHITE))
        );

        PlayerUtil.sendMessage(player,
            Component.text("地点: ", NamedTextColor.GRAY)
                .append(Component.text(activity.getLocation(), NamedTextColor.WHITE))
        );

        PlayerUtil.sendMessage(player,
            Component.text("创建者: ", NamedTextColor.GRAY)
                .append(Component.text(creatorName, NamedTextColor.WHITE))
        );

        PlayerUtil.sendMessage(player,
            Component.text("参与人数: ", NamedTextColor.GRAY)
                .append(Component.text(participants.size() + (activity.getMaxParticipants() > 0 ? "/" + activity.getMaxParticipants() : ""), NamedTextColor.WHITE))
        );

        if (!activity.getDescription().isEmpty()) {
            PlayerUtil.sendMessage(player,
                Component.text("描述: ", NamedTextColor.GRAY)
                    .append(Component.text(activity.getDescription(), NamedTextColor.WHITE))
            );
        }

        // 显示参与者列表
        if (!participants.isEmpty()) {
            PlayerUtil.sendMessage(player, Component.text("参与者列表:", NamedTextColor.GRAY));

            StringBuilder participantList = new StringBuilder();
            for (int i = 0; i < participants.size(); i++) {
                ActivityParticipant participant = participants.get(i);
                participantList.append(participant.getPlayerName());

                if (i < participants.size() - 1) {
                    participantList.append(", ");
                }

                // 每5个参与者换行
                if ((i + 1) % 5 == 0 && i < participants.size() - 1) {
                    PlayerUtil.sendMessage(player, Component.text("  " + participantList.toString(), NamedTextColor.WHITE));
                    participantList = new StringBuilder();
                }
            }

            if (participantList.length() > 0) {
                PlayerUtil.sendMessage(player, Component.text("  " + participantList.toString(), NamedTextColor.WHITE));
            }
        }

        // 显示操作按钮
        if (status == GuildActivity.Status.PLANNED) {
            // 检查玩家是否已参与
            boolean isParticipating = false;
            for (ActivityParticipant participant : participants) {
                if (participant.getPlayerUuid().equals(player.getUniqueId())) {
                    isParticipating = true;
                    break;
                }
            }

            Component actionButtons = Component.text("操作: ", NamedTextColor.GRAY);

            if (isParticipating) {
                actionButtons = actionButtons.append(
                    Component.text("[退出活动]", NamedTextColor.RED)
                        .hoverEvent(Component.text("点击退出活动", NamedTextColor.GRAY))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guild activity leave " + activity.getId()))
                );
            } else {
                // 检查是否还有名额
                if (activity.getMaxParticipants() == 0 || participants.size() < activity.getMaxParticipants()) {
                    actionButtons = actionButtons.append(
                        Component.text("[参加活动]", NamedTextColor.GREEN)
                            .hoverEvent(Component.text("点击参加活动", NamedTextColor.GRAY))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guild activity join " + activity.getId()))
                    );
                } else {
                    actionButtons = actionButtons.append(Component.text("[人数已满]", NamedTextColor.DARK_GRAY));
                }
            }

            PlayerUtil.sendMessage(player, actionButtons);
        }

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

                for (GuildActivity activity : activities) {
                    completions.add(String.valueOf(activity.getId()));
                }
            }
        }

        return completions;
    }
}
