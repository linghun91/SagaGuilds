package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 设置公会传送点命令
 * 副会长及以上权限可以设置
 */
public class SetWarpCommand implements SubCommand {
    private final SagaGuild plugin;
    
    public SetWarpCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "setwarp";
    }
    
    @Override
    public String getDescription() {
        return "设置公会传送点";
    }
    
    @Override
    public String getSyntax() {
        return "/guild setwarp";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"sw", "setspawn"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }
        
        // 检查权限（副会长及以上）
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return true;
        }
        
        // 检查世界是否允许设置传送点
        String worldName = player.getWorld().getName();
        List<String> disabledWorlds = plugin.getConfig().getStringList("warp.disabled-worlds");
        if (disabledWorlds.contains(worldName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.world-disabled"));
            return true;
        }
        
        // 设置传送点
        plugin.getWarpManager().setWarp(player, guild);
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return new ArrayList<>();
    }
}