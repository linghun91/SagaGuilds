package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.WarpDAO;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildWarp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 公会传送点管理器
 * 负责公会传送点的创建、删除和传送功能
 */
public class WarpManager {
    private final SagaGuild plugin;
    private final WarpDAO warpDAO;
    
    // 传送延迟（秒）
    private final int teleportDelay;
    
    // 正在传送的玩家
    private final Map<UUID, BukkitRunnable> pendingTeleports;
    
    // 最近的位置记录（用于检测移动）
    private final Map<UUID, Location> lastLocations;

    public WarpManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.warpDAO = new WarpDAO(plugin);
        this.teleportDelay = plugin.getConfig().getInt("warp.teleport-delay", 3);
        this.pendingTeleports = new HashMap<>();
        this.lastLocations = new HashMap<>();
    }

    /**
     * 设置公会传送点
     * @param player 玩家
     * @param guild 公会
     * @return 是否成功
     */
    public boolean setWarp(Player player, Guild guild) {
        // 创建传送点对象
        GuildWarp warp = new GuildWarp(guild.getId(), player.getLocation(), player.getUniqueId());
        
        // 保存到数据库
        boolean success = warpDAO.setWarp(warp);
        
        if (success) {
            // 发送成功消息
            player.sendMessage(plugin.getConfigManager().getMessage("warp.set-success"));
            
            // 通知公会成员
            plugin.getGuildManager().broadcastToGuild(guild.getId(),
                    plugin.getConfigManager().getMessage("warp.set-broadcast",
                            "player", player.getName()),
                    player.getUniqueId());
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.set-failed"));
        }
        
        return success;
    }

    /**
     * 获取公会传送点
     * @param guildId 公会ID
     * @return 传送点对象
     */
    public GuildWarp getWarp(int guildId) {
        return warpDAO.getWarp(guildId);
    }

    /**
     * 删除公会传送点
     * @param player 玩家
     * @param guild 公会
     * @return 是否成功
     */
    public boolean deleteWarp(Player player, Guild guild) {
        // 检查是否存在传送点
        GuildWarp warp = getWarp(guild.getId());
        if (warp == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.not-found"));
            return false;
        }
        
        // 删除传送点
        boolean success = warpDAO.deleteWarp(guild.getId());
        
        if (success) {
            // 发送成功消息
            player.sendMessage(plugin.getConfigManager().getMessage("warp.delete-success"));
            
            // 通知公会成员
            plugin.getGuildManager().broadcastToGuild(guild.getId(),
                    plugin.getConfigManager().getMessage("warp.delete-broadcast",
                            "player", player.getName()),
                    player.getUniqueId());
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.delete-failed"));
        }
        
        return success;
    }

    /**
     * 传送到公会传送点
     * @param player 玩家
     * @param guild 公会
     */
    public void teleportToWarp(Player player, Guild guild) {
        // 获取传送点
        GuildWarp warp = getWarp(guild.getId());
        if (warp == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.not-found"));
            return;
        }
        
        // 获取目标位置
        Location destination = warp.toLocation();
        if (destination == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.world-not-found"));
            return;
        }
        
        // 检查是否已在传送中
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("warp.already-teleporting"));
            return;
        }
        
        // 检查战斗状态（如果配置启用）
        if (plugin.getConfig().getBoolean("warp.no-teleport-in-combat", true)) {
            // TODO: 实现战斗检测
        }
        
        // 记录当前位置
        lastLocations.put(player.getUniqueId(), player.getLocation().clone());
        
        // 发送传送倒计时消息
        player.sendMessage(plugin.getConfigManager().getMessage("warp.teleport-countdown",
                "seconds", String.valueOf(teleportDelay)));
        
        // 创建传送任务
        BukkitRunnable teleportTask = new BukkitRunnable() {
            private int countdown = teleportDelay;
            
            @Override
            public void run() {
                // 检查玩家是否在线
                if (!player.isOnline()) {
                    cancelTeleport(player.getUniqueId());
                    return;
                }
                
                // 检查是否移动（如果配置启用）
                if (plugin.getConfig().getBoolean("warp.cancel-on-move", true)) {
                    Location lastLoc = lastLocations.get(player.getUniqueId());
                    if (lastLoc != null && hasMovedSignificantly(lastLoc, player.getLocation())) {
                        player.sendMessage(plugin.getConfigManager().getMessage("warp.teleport-cancelled-move"));
                        cancelTeleport(player.getUniqueId());
                        return;
                    }
                }
                
                countdown--;
                
                if (countdown > 0) {
                    // 继续倒计时
                    if (countdown <= 3) {
                        player.sendMessage(plugin.getConfigManager().getMessage("warp.teleport-in",
                                "seconds", String.valueOf(countdown)));
                    }
                } else {
                    // 执行传送
                    player.teleport(destination);
                    player.sendMessage(plugin.getConfigManager().getMessage("warp.teleport-success"));
                    
                    // 清理
                    pendingTeleports.remove(player.getUniqueId());
                    lastLocations.remove(player.getUniqueId());
                    cancel();
                }
            }
        };
        
        // 保存任务引用
        pendingTeleports.put(player.getUniqueId(), teleportTask);
        
        // 开始任务（1秒后开始，每秒执行一次）
        teleportTask.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * 取消传送
     * @param playerUuid 玩家UUID
     */
    public void cancelTeleport(UUID playerUuid) {
        BukkitRunnable task = pendingTeleports.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
        lastLocations.remove(playerUuid);
    }

    /**
     * 检查玩家是否移动了显著距离
     * @param from 起始位置
     * @param to 目标位置
     * @return 是否移动了显著距离
     */
    private boolean hasMovedSignificantly(Location from, Location to) {
        // 检查世界是否相同
        if (!from.getWorld().equals(to.getWorld())) {
            return true;
        }
        
        // 检查距离（超过0.5格算移动）
        double distance = from.distance(to);
        return distance > 0.5;
    }

    /**
     * 检查玩家是否有权限设置传送点
     * @param player 玩家
     * @param guild 公会
     * @return 是否有权限
     */
    public boolean canSetWarp(Player player, Guild guild) {
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        return member != null && member.isAdmin();
    }

    /**
     * 清理玩家的传送任务（玩家退出时调用）
     * @param playerUuid 玩家UUID
     */
    public void cleanupPlayer(UUID playerUuid) {
        cancelTeleport(playerUuid);
    }
}