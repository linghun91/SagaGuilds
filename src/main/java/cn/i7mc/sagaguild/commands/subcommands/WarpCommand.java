package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 传送到公会传送点命令
 * 所有公会成员都可以使用
 */
public class WarpCommand implements SubCommand {
    private final SagaGuild plugin;
    
    public WarpCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "warp";
    }
    
    @Override
    public String getDescription() {
        return "传送到公会传送点";
    }
    
    @Override
    public String getSyntax() {
        return "/guild warp";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"home", "spawn"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }
        
        // 执行传送
        plugin.getWarpManager().teleportToWarp(player, guild);
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        return new ArrayList<>();
    }
}