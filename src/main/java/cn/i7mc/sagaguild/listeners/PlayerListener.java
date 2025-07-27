package cn.i7mc.sagaguild.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家事件监听器
 * 处理玩家相关的事件
 */
public class PlayerListener implements Listener {
    private final SagaGuild plugin;
    
    public PlayerListener(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 玩家加入服务器事件
     * @param event 事件对象
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 更新玩家名称
        updatePlayerName(player);
        
        // 检查公会公告
        checkGuildAnnouncement(player);
    }
    
    /**
     * 玩家离开服务器事件
     * @param event 事件对象
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 清理传送任务
        plugin.getWarpManager().cleanupPlayer(event.getPlayer().getUniqueId());
    }
    
    /**
     * 更新玩家名称
     * @param player 玩家
     */
    private void updatePlayerName(Player player) {
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member != null && !member.getPlayerName().equals(player.getName())) {
            member.setPlayerName(player.getName());
            // 异步更新数据库
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().getConnection();
                // TODO: 实现异步更新
            });
        }
    }
    
    /**
     * 检查公会公告
     * @param player 玩家
     */
    private void checkGuildAnnouncement(Player player) {
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild != null && !guild.getAnnouncement().isEmpty()) {
            player.sendMessage("§8§m-----§r §b公会公告 §8§m-----");
            player.sendMessage("§f" + guild.getAnnouncement());
            player.sendMessage("§8§m-----------------------");
        }
    }
}
