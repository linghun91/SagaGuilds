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
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 查看公会活动列表命令
 */
public class ActivityListCommand implements SubCommand {
    private final SagaGuild plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ActivityListCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "查看公会活动列表";
    }

    @Override
    public String getSyntax() {
        return "/guild activity list [页码]";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"ls", "show"};
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            PlayerUtil.sendMessage(player, Component.text("你不在任何公会中！", NamedTextColor.RED));
            return false;
        }

        // 获取公会活动列表
        List<GuildActivity> activities = plugin.getActivityManager().getGuildActivities(guild.getId());
        if (activities.isEmpty()) {
            PlayerUtil.sendMessage(player, Component.text("公会暂无活动！", NamedTextColor.YELLOW));
            return true;
        }

        // 解析页码
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                PlayerUtil.sendMessage(player, Component.text("无效的页码！", NamedTextColor.RED));
                return false;
            }
        }

        // 计算总页数
        int activitiesPerPage = 5;
        int totalPages = (int) Math.ceil((double) activities.size() / activitiesPerPage);
        if (page > totalPages) {
            page = totalPages;
        }

        // 计算当前页的活动范围
        int startIndex = (page - 1) * activitiesPerPage;
        int endIndex = Math.min(startIndex + activitiesPerPage, activities.size());

        // 发送活动列表
        PlayerUtil.sendMessage(player, Component.text("=== 公会活动列表 (第 " + page + "/" + totalPages + " 页) ===", NamedTextColor.GOLD));

        for (int i = startIndex; i < endIndex; i++) {
            GuildActivity activity = activities.get(i);

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

            // 获取参与者数量
            List<ActivityParticipant> participants = plugin.getActivityManager().getActivityParticipants(activity.getId());
            int participantCount = participants.size();
            String participantInfo = participantCount + (activity.getMaxParticipants() > 0 ? "/" + activity.getMaxParticipants() : "");

            // 发送活动信息
            PlayerUtil.sendMessage(player,
                Component.text((i + 1) + ". ", NamedTextColor.GRAY)
                    .append(statusComponent)
                    .append(Component.text(" ", NamedTextColor.WHITE))
                    .append(Component.text(activity.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(activity.getType().getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text(")", NamedTextColor.GRAY))
            );

            PlayerUtil.sendMessage(player,
                Component.text("   时间: ", NamedTextColor.GRAY)
                    .append(Component.text(dateFormat.format(activity.getStartTime()) + " 至 " + dateFormat.format(activity.getEndTime()), NamedTextColor.WHITE))
            );

            PlayerUtil.sendMessage(player,
                Component.text("   地点: ", NamedTextColor.GRAY)
                    .append(Component.text(activity.getLocation(), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("参与人数: ", NamedTextColor.GRAY))
                    .append(Component.text(participantInfo, NamedTextColor.WHITE))
            );

            if (!activity.getDescription().isEmpty()) {
                PlayerUtil.sendMessage(player,
                    Component.text("   描述: ", NamedTextColor.GRAY)
                        .append(Component.text(activity.getDescription(), NamedTextColor.WHITE))
                );
            }

            // 添加分隔线
            if (i < endIndex - 1) {
                PlayerUtil.sendMessage(player, Component.text("   ----------------------", NamedTextColor.DARK_GRAY));
            }
        }

        // 显示翻页提示
        if (totalPages > 1) {
            Component pageNavigation = Component.text("=== ", NamedTextColor.GOLD);

            if (page > 1) {
                pageNavigation = pageNavigation.append(
                    Component.text("[上一页]", NamedTextColor.YELLOW)
                        .hoverEvent(Component.text("点击查看上一页", NamedTextColor.GRAY))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guild activity list " + (page - 1)))
                );
            } else {
                pageNavigation = pageNavigation.append(Component.text("[上一页]", NamedTextColor.DARK_GRAY));
            }

            pageNavigation = pageNavigation.append(Component.text(" | ", NamedTextColor.GOLD));

            if (page < totalPages) {
                pageNavigation = pageNavigation.append(
                    Component.text("[下一页]", NamedTextColor.YELLOW)
                        .hoverEvent(Component.text("点击查看下一页", NamedTextColor.GRAY))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guild activity list " + (page + 1)))
                );
            } else {
                pageNavigation = pageNavigation.append(Component.text("[下一页]", NamedTextColor.DARK_GRAY));
            }

            pageNavigation = pageNavigation.append(Component.text(" ===", NamedTextColor.GOLD));

            PlayerUtil.sendMessage(player, pageNavigation);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 页码，提供1-5页
            completions.add("1");
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
        }

        return completions;
    }
}
