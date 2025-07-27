package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildWar;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 公会战命令
 * 管理公会战的发起、接受和查看
 */
public class WarCommand implements SubCommand {
    private final SagaGuild plugin;
    
    public WarCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "war";
    }
    
    @Override
    public String getDescription() {
        return "管理公会战";
    }
    
    @Override
    public String getSyntax() {
        return "/guild war <declare/accept/reject/surrender/status/history> [公会名]";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"w", "battle"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 检查参数
        if (args.length == 0) {
            player.sendMessage("§c用法: " + getSyntax());
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "declare":
            case "challenge":
                // 发起公会战
                if (args.length < 2) {
                    player.sendMessage("§c用法: /guild war declare <公会名>");
                    return true;
                }
                
                return plugin.getWarManager().inviteToWar(player, args[1]);
                
            case "accept":
                // 接受公会战
                if (args.length < 2) {
                    player.sendMessage("§c用法: /guild war accept <公会名>");
                    return true;
                }
                
                return plugin.getWarManager().acceptWarInvitation(player, args[1]);
                
            case "reject":
                // 拒绝公会战
                if (args.length < 2) {
                    player.sendMessage("§c用法: /guild war reject <公会名>");
                    return true;
                }
                
                return plugin.getWarManager().rejectWarInvitation(player, args[1]);
                
            case "surrender":
                // 投降
                return plugin.getWarManager().surrender(player);
                
            case "status":
                // 查看当前战争状态
                showWarStatus(player);
                break;
                
            case "history":
                // 查看战争历史
                showWarHistory(player);
                break;
                
            default:
                player.sendMessage("§c未知的子命令！用法: " + getSyntax());
                break;
        }
        
        return true;
    }
    
    /**
     * 显示当前战争状态
     * @param player 玩家
     */
    private void showWarStatus(Player player) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return;
        }
        
        // 获取当前战争
        GuildWar war = plugin.getWarManager().getActiveWar(guild.getId());
        if (war == null) {
            player.sendMessage("§c你的公会当前没有进行中的战争！");
            return;
        }
        
        // 获取对方公会
        int opponentId = war.getOpponentId(guild.getId());
        Guild opponent = plugin.getGuildManager().getGuildById(opponentId);
        if (opponent == null) {
            player.sendMessage("§c无法获取对方公会信息！");
            return;
        }
        
        // 显示战争状态
        player.sendMessage("§8§m-----§r §c公会战状态 §8§m-----");
        player.sendMessage("§7对方公会: §f" + opponent.getName());
        
        String statusText;
        switch (war.getStatus()) {
            case PENDING:
                statusText = "§e等待中";
                break;
            case PREPARING:
                statusText = "§6准备中";
                break;
            case ONGOING:
                statusText = "§c进行中";
                break;
            case FINISHED:
                statusText = "§a已结束";
                break;
            default:
                statusText = "§7未知";
                break;
        }
        
        player.sendMessage("§7状态: " + statusText);
        
        // 格式化时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        player.sendMessage("§7开始时间: §f" + sdf.format(war.getStartTime()));
        
        if (war.getStatus() == GuildWar.Status.ONGOING) {
            // TODO: 显示战争统计信息
        }
        
        player.sendMessage("§8§m-----------------------");
    }
    
    /**
     * 显示战争历史
     * @param player 玩家
     */
    private void showWarHistory(Player player) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return;
        }
        
        // 获取战争历史
        List<GuildWar> warHistory = plugin.getWarManager().getWarHistory(guild.getId());
        if (warHistory.isEmpty()) {
            player.sendMessage("§c你的公会没有战争历史！");
            return;
        }
        
        // 显示战争历史
        player.sendMessage("§8§m-----§r §c公会战历史 §8§m-----");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        for (int i = 0; i < Math.min(5, warHistory.size()); i++) {
            GuildWar war = warHistory.get(i);
            
            // 获取对方公会
            int opponentId = war.getOpponentId(guild.getId());
            Guild opponent = plugin.getGuildManager().getGuildById(opponentId);
            if (opponent == null) {
                continue;
            }
            
            // 显示战争结果
            String result;
            if (war.getStatus() != GuildWar.Status.FINISHED) {
                result = "§e进行中";
            } else if (war.getWinnerId() == null) {
                result = "§6平局";
            } else if (war.getWinnerId() == guild.getId()) {
                result = "§a胜利";
            } else {
                result = "§c失败";
            }
            
            player.sendMessage("§7" + sdf.format(war.getStartTime()) + " vs " + opponent.getName() + ": " + result);
        }
        
        if (warHistory.size() > 5) {
            player.sendMessage("§7... 共 " + warHistory.size() + " 条记录");
        }
        
        player.sendMessage("§8§m-----------------------");
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList("declare", "accept", "reject", "surrender", "status", "history");
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(arg)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("declare") || subCommand.equals("accept") || subCommand.equals("reject")) {
                String arg = args[1].toLowerCase();
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.getName().toLowerCase().startsWith(arg)) {
                        completions.add(guild.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
