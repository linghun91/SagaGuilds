package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 解散公会命令
 * 只有公会会长可以执行此命令
 */
public class DisbandCommand implements SubCommand {
    private final SagaGuild plugin;

    public DisbandCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "disband";
    }
    
    @Override
    public String getDescription() {
        return "解散公会";
    }
    
    @Override
    public String getSyntax() {
        return "/guild disband";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"解散"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 检查权限
        if (!player.hasPermission("guild.disband")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }
        
        // 检查是否是会长
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isOwner()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.owner-only"));
            return true;
        }
        
        // 执行解散
        boolean success = plugin.getGuildManager().disbandGuild(player, guild.getId());
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.action-failed"));
            return true;
        }
        
        // 发送解散消息
        player.sendMessage(plugin.getConfigManager().getMessage("guild.disbanded", "guild", guild.getName()));
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        // 没有特定的补全
        return new ArrayList<>();
    }
}
