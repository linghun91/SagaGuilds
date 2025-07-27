package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 管理员命令
 * 提供管理员专用的公会管理功能
 */
public class AdminCommand implements SubCommand {
    private final SagaGuild plugin;
    
    public AdminCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "admin";
    }
    
    @Override
    public String getDescription() {
        return "管理员专用命令";
    }
    
    @Override
    public String getSyntax() {
        return "/guild admin <disband|reload|info|exp> [参数...]";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"a", "administrator"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 检查管理员权限
        if (!player.hasPermission("guild.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        
        // 检查参数
        if (args.length == 0) {
            player.sendMessage("§c用法: " + getSyntax());
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "disband":
                return handleDisbandCommand(player, args);
            case "reload":
                return handleReloadCommand(player, args);
            case "info":
                return handleInfoCommand(player, args);
            case "exp":
                return handleExpCommand(player, args);
            default:
                player.sendMessage("§c无效的管理员命令！可用命令: disband, reload, info, exp");
                return true;
        }
    }
    
    /**
     * 处理强制解散公会命令
     * @param player 执行命令的管理员
     * @param args 命令参数
     * @return 是否成功执行
     */
    private boolean handleDisbandCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /guild admin disband <公会名>");
            return true;
        }
        
        String guildName = args[1];
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-found", "guild", guildName));
            return true;
        }
        
        // 强制解散公会（管理员权限）
        boolean success = plugin.getGuildManager().forceDisband(guild.getId());
        
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("admin.guild-disbanded", 
                    "guild", guild.getName(), "admin", player.getName()));
            plugin.getLogger().info("管理员 " + player.getName() + " 强制解散了公会: " + guild.getName());
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("admin.disband-failed"));
        }
        
        return true;
    }
    
    /**
     * 处理重载配置命令
     * @param player 执行命令的管理员
     * @param args 命令参数
     * @return 是否成功执行
     */
    private boolean handleReloadCommand(Player player, String[] args) {
        try {
            // 重载配置文件
            plugin.getConfigManager().reloadConfigs();
            
            // 重载公会数据
            plugin.getGuildManager().reloadGuilds();
            
            // 重载领地数据
            plugin.getLandManager().reloadLands();
            
            player.sendMessage(plugin.getConfigManager().getMessage("admin.reload-success"));
            plugin.getLogger().info("管理员 " + player.getName() + " 重载了插件配置");
        } catch (Exception e) {
            player.sendMessage(plugin.getConfigManager().getMessage("admin.reload-failed"));
            plugin.getLogger().severe("重载配置时发生错误: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理查看公会信息命令
     * @param player 执行命令的管理员
     * @param args 命令参数
     * @return 是否成功执行
     */
    private boolean handleInfoCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /guild admin info <公会名>");
            return true;
        }
        
        String guildName = args[1];
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-found", "guild", guildName));
            return true;
        }
        
        // 显示详细的公会信息（管理员视图）
        player.sendMessage("§8§m-----§r §b管理员视图 - " + guild.getName() + " §8[§7" + guild.getTag() + "§8] §8§m-----");
        player.sendMessage("§7公会ID: §f" + guild.getId());
        player.sendMessage("§7描述: §f" + guild.getDescription());
        player.sendMessage("§7会长: §f" + plugin.getServer().getOfflinePlayer(guild.getOwnerUuid()).getName());
        player.sendMessage("§7等级: §f" + guild.getLevel() + " §7(§f" + guild.getExperience() + "§7 经验)");
        player.sendMessage("§7创建时间: §f" + guild.getCreatedAt());
        player.sendMessage("§7是否公开: §f" + (guild.isPublic() ? "是" : "否"));
        
        // 显示成员数量
        int memberCount = plugin.getGuildManager().getGuildMemberCount(guild.getId());
        player.sendMessage("§7成员数量: §f" + memberCount);
        
        // 显示领地数量
        int landCount = plugin.getLandManager().getGuildLandCount(guild.getId());
        player.sendMessage("§7领地数量: §f" + landCount);
        
        player.sendMessage("§8§m-----------------------");
        
        return true;
    }
    
    /**
     * 处理公会经验管理命令
     * @param player 执行命令的管理员
     * @param args 命令参数
     * @return 是否成功执行
     */
    private boolean handleExpCommand(Player player, String[] args) {
        // 检查经验管理权限
        if (!player.hasPermission("guild.admin.exp")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        
        if (args.length < 4) {
            player.sendMessage("§c用法: /guild admin exp <add|set|take> <公会名> <经验值>");
            return true;
        }
        
        String operation = args[1].toLowerCase();
        String guildName = args[2];
        String expStr = args[3];
        
        // 验证操作类型
        if (!operation.equals("add") && !operation.equals("set") && !operation.equals("take")) {
            player.sendMessage("§c无效的操作类型！可用操作: add, set, take");
            return true;
        }
        
        // 验证经验值
        int experience;
        try {
            experience = Integer.parseInt(expStr);
            if (experience < 0) {
                player.sendMessage("§c经验值必须是非负整数！");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的经验值！请输入有效的数字。");
            return true;
        }
        
        // 查找公会
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-found", "guild", guildName));
            return true;
        }
        
        // 执行经验操作
        int oldExp = guild.getExperience();
        int newExp = oldExp;
        
        switch (operation) {
            case "add":
                newExp = oldExp + experience;
                break;
            case "set":
                newExp = experience;
                break;
            case "take":
                newExp = Math.max(0, oldExp - experience);  // 确保经验不会变成负数
                break;
        }
        
        // 使用GuildManager进行经验管理
        boolean success = plugin.getGuildManager().setGuildExperience(guild.getId(), newExp);
        
        if (success) {
            String operationText = "";
            switch (operation) {
                case "add":
                    operationText = "增加了 " + experience + " 点经验";
                    break;
                case "set":
                    operationText = "设置经验为 " + experience + " 点";
                    break;
                case "take":
                    operationText = "扣除了 " + Math.min(experience, oldExp) + " 点经验";
                    break;
            }
            
            player.sendMessage("§a成功为公会 §f" + guild.getName() + " §a" + operationText);
            player.sendMessage("§7原经验: §f" + oldExp + " §7→ §f" + newExp);
            
            // 记录管理员操作
            plugin.getLogger().info("管理员 " + player.getName() + " 对公会 " + guild.getName() + 
                    " 执行了经验操作: " + operation + " " + experience + " (原经验: " + oldExp + " → 新经验: " + newExp + ")");
        } else {
            player.sendMessage("§c操作失败！请稍后重试。");
        }
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 检查管理员权限
        if (!player.hasPermission("guild.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // 第一个参数：子命令补全
            String arg = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList("disband", "reload", "info", "exp");
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(arg)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // 第二个参数：根据子命令补全
            String subCommand = args[0].toLowerCase();
            if ("disband".equals(subCommand) || "info".equals(subCommand)) {
                // 补全公会名称
                String arg = args[1].toLowerCase();
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.getName().toLowerCase().startsWith(arg)) {
                        completions.add(guild.getName());
                    }
                }
            } else if ("exp".equals(subCommand)) {
                // exp命令的操作类型补全
                String arg = args[1].toLowerCase();
                List<String> operations = Arrays.asList("add", "set", "take");
                for (String operation : operations) {
                    if (operation.startsWith(arg)) {
                        completions.add(operation);
                    }
                }
            }
        } else if (args.length == 3) {
            // 第三个参数：根据子命令补全
            String subCommand = args[0].toLowerCase();
            if ("exp".equals(subCommand)) {
                // exp命令的公会名称补全
                String arg = args[2].toLowerCase();
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.getName().toLowerCase().startsWith(arg)) {
                        completions.add(guild.getName());
                    }
                }
            }
        } else if (args.length == 4) {
            // 第四个参数：根据子命令补全
            String subCommand = args[0].toLowerCase();
            if ("exp".equals(subCommand)) {
                // exp命令的经验值补全 - 提供一些示例值
                completions.addAll(Arrays.asList("100", "500", "1000", "5000"));
            }
        }
        
        return completions;
    }
}
