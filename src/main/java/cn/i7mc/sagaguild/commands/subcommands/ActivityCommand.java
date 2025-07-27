package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.commands.subcommands.activity.*;
import cn.i7mc.sagaguild.utils.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 公会活动主命令
 */
public class ActivityCommand implements SubCommand {
    private final SagaGuild plugin;
    private final Map<String, SubCommand> subCommands;

    public ActivityCommand(SagaGuild plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();

        // 注册子命令
        registerSubCommand(new ActivityListCommand(plugin));
        registerSubCommand(new ActivityInfoCommand(plugin));
        registerSubCommand(new ActivityCreateCommand(plugin));
        registerSubCommand(new ActivityJoinCommand(plugin));
        registerSubCommand(new ActivityLeaveCommand(plugin));
        registerSubCommand(new ActivityCancelCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }

    @Override
    public String getName() {
        return "activity";
    }

    @Override
    public String getDescription() {
        return "管理公会活动";
    }

    @Override
    public String getSyntax() {
        return "/guild activity <list|info|create|join|leave|cancel> [参数...]";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"activities", "act", "event", "events"};
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length < 1) {
            // 显示帮助信息
            showHelp(player);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            PlayerUtil.sendMessage(player, Component.text("未知的子命令！使用 /guild activity 查看帮助。", NamedTextColor.RED));
            return false;
        }

        // 创建新的参数数组，去掉第一个参数（子命令名）
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        
        // 执行子命令
        return subCommand.execute(player, subArgs);
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 提供子命令补全
            for (String subCommandName : subCommands.keySet()) {
                if (subCommandName.startsWith(args[0].toLowerCase())) {
                    SubCommand subCommand = subCommands.get(subCommandName);
                    if (!completions.contains(subCommand.getName())) {
                        completions.add(subCommand.getName());
                    }
                }
            }
        } else if (args.length > 1) {
            // 提供子命令参数补全
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null) {
                // 创建新的参数数组，去掉第一个参数
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);

                completions = subCommand.tabComplete(player, subArgs);
            }
        }

        return completions;
    }

    /**
     * 显示帮助信息
     * @param player 玩家
     */
    private void showHelp(Player player) {
        PlayerUtil.sendMessage(player, Component.text("=== 公会活动命令 ===", NamedTextColor.GOLD));

        // 使用HashSet避免重复显示相同的命令对象
        Set<SubCommand> uniqueCommands = new HashSet<>(subCommands.values());
        for (SubCommand subCommand : uniqueCommands) {
            PlayerUtil.sendMessage(player,
                Component.text("/guild activity " + subCommand.getName(), NamedTextColor.YELLOW)
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(subCommand.getDescription(), NamedTextColor.WHITE))
            );
        }
    }
}
