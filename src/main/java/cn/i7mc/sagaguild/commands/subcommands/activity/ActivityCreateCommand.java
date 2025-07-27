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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 创建公会活动命令
 */
public class ActivityCreateCommand implements SubCommand {
    private final SagaGuild plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ActivityCreateCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "创建公会活动";
    }

    @Override
    public String getSyntax() {
        return "/guild activity create <名称> <类型> <开始时间> <结束时间> <地点> <最大参与人数> [描述]";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"new", "add"};
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // 检查参数数量（名称、类型、开始日期、开始时间、结束日期、结束时间、地点、最大人数）
        if (args.length < 8) {
            PlayerUtil.sendMessage(player, Component.text("用法: " + getSyntax(), NamedTextColor.RED));
            PlayerUtil.sendMessage(player, Component.text("活动类型: MEETING(会议), DUNGEON(副本), PVP(PVP), RESOURCE_GATHERING(资源收集), BUILDING(建筑), CUSTOM(自定义)", NamedTextColor.GRAY));
            PlayerUtil.sendMessage(player, Component.text("时间格式: yyyy-MM-dd HH:mm，例如: 2023-05-20 18:30", NamedTextColor.GRAY));
            return false;
        }

        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            PlayerUtil.sendMessage(player, Component.text("你不在任何公会中！", NamedTextColor.RED));
            return false;
        }

        // 检查权限
        GuildMember member = plugin.getGuildManager().getGuildMember(guild.getId(), player.getUniqueId());
        if (member == null || (!member.isAdmin() && !player.hasPermission("guild.admin"))) {
            PlayerUtil.sendMessage(player, Component.text("你没有权限创建公会活动！", NamedTextColor.RED));
            return false;
        }

        // 解析参数
        String name = args[0];

        // 解析活动类型
        GuildActivity.Type type;
        try {
            type = GuildActivity.Type.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            PlayerUtil.sendMessage(player, Component.text("无效的活动类型！可用类型: MEETING(会议), DUNGEON(副本), PVP(PVP), RESOURCE_GATHERING(资源收集), BUILDING(建筑), CUSTOM(自定义)", NamedTextColor.RED));
            return false;
        }

        // 解析开始时间
        Date startTime;
        try {
            startTime = dateFormat.parse(args[2] + " " + args[3]);
        } catch (ParseException e) {
            PlayerUtil.sendMessage(player, Component.text("无效的开始时间格式！正确格式: yyyy-MM-dd HH:mm，例如: 2023-05-20 18:30", NamedTextColor.RED));
            return false;
        }

        // 检查开始时间是否在当前时间之后
        if (startTime.before(new Date())) {
            PlayerUtil.sendMessage(player, Component.text("开始时间必须在当前时间之后！", NamedTextColor.RED));
            return false;
        }

        // 解析结束时间
        Date endTime;
        try {
            endTime = dateFormat.parse(args[4] + " " + args[5]);
        } catch (ParseException e) {
            PlayerUtil.sendMessage(player, Component.text("无效的结束时间格式！正确格式: yyyy-MM-dd HH:mm，例如: 2023-05-20 20:30", NamedTextColor.RED));
            return false;
        }

        // 检查结束时间是否在开始时间之后
        if (endTime.before(startTime)) {
            PlayerUtil.sendMessage(player, Component.text("结束时间必须在开始时间之后！", NamedTextColor.RED));
            return false;
        }

        // 解析地点
        String location = args[6];

        // 解析最大参与人数
        int maxParticipants;
        try {
            maxParticipants = Integer.parseInt(args[7]);
            if (maxParticipants < 0) {
                PlayerUtil.sendMessage(player, Component.text("最大参与人数必须大于等于0！0表示不限制人数", NamedTextColor.RED));
                return false;
            }
        } catch (NumberFormatException e) {
            PlayerUtil.sendMessage(player, Component.text("无效的最大参与人数！必须是一个整数", NamedTextColor.RED));
            return false;
        }

        // 解析描述
        StringBuilder descriptionBuilder = new StringBuilder();
        if (args.length > 8) {
            for (int i = 8; i < args.length; i++) {
                descriptionBuilder.append(args[i]).append(" ");
            }
        }
        String description = descriptionBuilder.toString().trim();

        // 创建活动
        GuildActivity activity = plugin.getActivityManager().createActivity(
                guild.getId(), name, description, type, player.getUniqueId(),
                startTime, endTime, location, maxParticipants
        );

        if (activity == null) {
            PlayerUtil.sendMessage(player, Component.text("创建活动失败！", NamedTextColor.RED));
            return false;
        }

        // 发送成功消息
        PlayerUtil.sendMessage(player, Component.text("成功创建活动 ", NamedTextColor.GREEN)
                .append(Component.text(name, NamedTextColor.YELLOW))
                .append(Component.text("！", NamedTextColor.GREEN)));

        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 活动名称，不提供补全
            return completions;
        } else if (args.length == 2) {
            // 活动类型
            for (GuildActivity.Type type : GuildActivity.Type.values()) {
                completions.add(type.name());
            }
        } else if (args.length == 3) {
            // 开始日期，提供当前日期
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            completions.add(dateFormat.format(new Date()));
        } else if (args.length == 4) {
            // 开始时间，提供几个常用时间
            completions.add("08:00");
            completions.add("12:00");
            completions.add("18:00");
            completions.add("20:00");
        } else if (args.length == 5) {
            // 结束日期，提供当前日期
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            completions.add(dateFormat.format(new Date()));
        } else if (args.length == 6) {
            // 结束时间，提供几个常用时间
            completions.add("10:00");
            completions.add("14:00");
            completions.add("20:00");
            completions.add("22:00");
        } else if (args.length == 7) {
            // 地点，提供几个常用地点
            completions.add("主城");
            completions.add("公会基地");
            completions.add("副本入口");
            completions.add("竞技场");
        } else if (args.length == 8) {
            // 最大参与人数，提供几个常用数量
            completions.add("0");
            completions.add("5");
            completions.add("10");
            completions.add("20");
        }

        return completions;
    }
}
