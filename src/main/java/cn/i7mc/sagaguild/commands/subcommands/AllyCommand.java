package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 联盟命令
 * 处理公会联盟相关操作
 */
public class AllyCommand implements SubCommand {
    private final SagaGuild plugin;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public AllyCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ally";
    }

    @Override
    public String getDescription() {
        return "管理公会联盟";
    }

    @Override
    public String getSyntax() {
        return "/guild ally <add/remove/list> [公会名]";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"alliance"};
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // 检查玩家是否在公会中
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }

        // 检查是否有权限
        if (!member.isOwner()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-leader"));
            return true;
        }

        // 获取玩家所在公会
        int guildId = member.getGuildId();
        Guild guild = plugin.getGuildManager().getGuildById(guildId);
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }

        // 检查参数
        if (args.length < 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.invalid-command"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                return handleAddAlly(player, args, guildId);
            case "remove":
                return handleRemoveAlly(player, args, guildId);
            case "list":
                return handleListAllies(player, guildId);
            default:
                player.sendMessage(plugin.getConfigManager().getMessage("general.invalid-command"));
                return true;
        }
    }

    /**
     * 处理添加联盟命令
     * @param player 玩家
     * @param args 命令参数
     * @param guildId 玩家公会ID
     * @return 是否成功
     */
    private boolean handleAddAlly(Player player, String[] args, int guildId) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.invalid-command"));
            return true;
        }

        String targetGuildName = args[1];
        Guild targetGuild = plugin.getGuildManager().getGuildByName(targetGuildName);

        if (targetGuild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-found", "guild", targetGuildName));
            return true;
        }

        int targetGuildId = targetGuild.getId();

        // 检查是否是自己的公会
        if (guildId == targetGuildId) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.cannot-ally-self"));
            return true;
        }

        // 检查是否已经是联盟
        if (plugin.getAllianceManager().areGuildsAllied(guildId, targetGuildId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.already-allied", "guild", targetGuild.getName()));
            return true;
        }

        // 发送联盟申请
        boolean success = plugin.getAllianceManager().sendAllianceRequest(guildId, targetGuildId);

        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.request-sent", "guild", targetGuild.getName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.request-failed", "guild", targetGuild.getName()));
        }

        return true;
    }

    /**
     * 处理解除联盟命令
     * @param player 玩家
     * @param args 命令参数
     * @param guildId 玩家公会ID
     * @return 是否成功
     */
    private boolean handleRemoveAlly(Player player, String[] args, int guildId) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.invalid-command"));
            return true;
        }

        String targetGuildName = args[1];
        Guild targetGuild = plugin.getGuildManager().getGuildByName(targetGuildName);

        if (targetGuild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-found", "guild", targetGuildName));
            return true;
        }

        int targetGuildId = targetGuild.getId();

        // 检查是否是联盟
        if (!plugin.getAllianceManager().areGuildsAllied(guildId, targetGuildId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.not-allied", "guild", targetGuild.getName()));
            return true;
        }

        // 解除联盟
        boolean success = plugin.getAllianceManager().breakAlliance(player, targetGuildId);

        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.broken", "guild", targetGuild.getName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.break-failed", "guild", targetGuild.getName()));
        }

        return true;
    }

    /**
     * 处理列出联盟命令
     * @param player 玩家
     * @param guildId 玩家公会ID
     * @return 是否成功
     */
    private boolean handleListAllies(Player player, int guildId) {
        List<Integer> alliances = plugin.getAllianceManager().getGuildAlliances(guildId);

        if (alliances.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.no-allies"));
            return true;
        }

        player.sendMessage(plugin.getConfigManager().getMessage("alliance.list-header"));

        for (int allyGuildId : alliances) {
            Guild allyGuild = plugin.getGuildManager().getGuildById(allyGuildId);
            if (allyGuild != null) {
                player.sendMessage(plugin.getConfigManager().getMessage("alliance.list-item", 
                        "guild", allyGuild.getName(),
                        "tag", allyGuild.getTag()));
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("alliance.list-footer"));

        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("add");
            completions.add("remove");
            completions.add("list");
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add") || subCommand.equals("remove")) {
                // 获取玩家所在公会
                GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
                if (member != null) {
                    int guildId = member.getGuildId();
                    List<Guild> guilds = plugin.getGuildManager().getAllGuilds();
                    
                    // 过滤掉自己的公会和已经是联盟的公会
                    for (Guild guild : guilds) {
                        if (guild.getId() != guildId) {
                            boolean isAllied = plugin.getAllianceManager().areGuildsAllied(guildId, guild.getId());
                            
                            if ((subCommand.equals("add") && !isAllied) || 
                                (subCommand.equals("remove") && isAllied)) {
                                completions.add(guild.getName());
                            }
                        }
                    }
                }
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
