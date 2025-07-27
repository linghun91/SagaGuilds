package cn.i7mc.sagaguild.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 经验监听器
 * 处理公会经验相关的事件
 */
public class ExperienceListener implements Listener {
    private final SagaGuild plugin;
    
    // 玩家冷却时间（毫秒）
    private static final long COOLDOWN_TIME = 60000; // 1分钟
    
    // 玩家冷却记录
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    
    public ExperienceListener(SagaGuild plugin) {
        this.plugin = plugin;
        this.playerCooldowns = new HashMap<>();
    }
    
    /**
     * 玩家破坏方块事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // 暂时禁用自动经验获取 - 仅保留管理员命令添加经验
        return;
        
        /*
        Player player = event.getPlayer();
        
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 检查冷却时间
        if (isOnCooldown(player.getUniqueId(), "block_break")) {
            return;
        }
        
        // 增加公会经验
        boolean levelUp = plugin.getGuildManager().addGuildExperience(guild.getId(), 1);
        
        // 设置冷却时间
        setCooldown(player.getUniqueId(), "block_break");
        
        // 如果升级，通知公会成员
        if (levelUp) {
            notifyGuildLevelUp(guild);
        }
        */
    }
    
    /**
     * 玩家放置方块事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // 暂时禁用自动经验获取 - 仅保留管理员命令添加经验
        return;
        
        /*
        Player player = event.getPlayer();
        
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 检查冷却时间
        if (isOnCooldown(player.getUniqueId(), "block_place")) {
            return;
        }
        
        // 增加公会经验
        boolean levelUp = plugin.getGuildManager().addGuildExperience(guild.getId(), 1);
        
        // 设置冷却时间
        setCooldown(player.getUniqueId(), "block_place");
        
        // 如果升级，通知公会成员
        if (levelUp) {
            notifyGuildLevelUp(guild);
        }
        */
    }
    
    /**
     * 玩家击杀实体事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // 暂时禁用自动经验获取 - 仅保留管理员命令添加经验
        return;
        
        /*
        // 检查是否是玩家击杀
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 检查冷却时间
        if (isOnCooldown(player.getUniqueId(), "entity_kill")) {
            return;
        }
        
        // 增加公会经验
        boolean levelUp = plugin.getGuildManager().addGuildExperience(guild.getId(), 2);
        
        // 设置冷却时间
        setCooldown(player.getUniqueId(), "entity_kill");
        
        // 如果升级，通知公会成员
        if (levelUp) {
            notifyGuildLevelUp(guild);
        }
        */
    }
    
    /**
     * 玩家钓鱼事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // 暂时禁用自动经验获取 - 仅保留管理员命令添加经验
        return;
        
        /*
        // 检查是否成功钓到鱼
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 检查冷却时间
        if (isOnCooldown(player.getUniqueId(), "fishing")) {
            return;
        }
        
        // 增加公会经验
        boolean levelUp = plugin.getGuildManager().addGuildExperience(guild.getId(), 2);
        
        // 设置冷却时间
        setCooldown(player.getUniqueId(), "fishing");
        
        // 如果升级，通知公会成员
        if (levelUp) {
            notifyGuildLevelUp(guild);
        }
        */
    }
    
    /**
     * 检查玩家是否在冷却中
     * @param playerUuid 玩家UUID
     * @param action 动作类型
     * @return 是否在冷却中
     */
    private boolean isOnCooldown(UUID playerUuid, String action) {
        Map<String, Long> cooldowns = playerCooldowns.get(playerUuid);
        if (cooldowns == null) {
            return false;
        }
        
        Long lastTime = cooldowns.get(action);
        if (lastTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastTime < COOLDOWN_TIME;
    }
    
    /**
     * 设置玩家冷却时间
     * @param playerUuid 玩家UUID
     * @param action 动作类型
     */
    private void setCooldown(UUID playerUuid, String action) {
        Map<String, Long> cooldowns = playerCooldowns.computeIfAbsent(playerUuid, k -> new HashMap<>());
        cooldowns.put(action, System.currentTimeMillis());
    }
    
    /**
     * 通知公会升级
     * @param guild 公会对象
     */
    private void notifyGuildLevelUp(Guild guild) {
        // 获取公会所有在线成员
        plugin.getGuildManager().getGuildMembers(guild.getId()).forEach(member -> {
            Player player = plugin.getServer().getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.level-up", 
                        "guild", guild.getName(), 
                        "level", String.valueOf(guild.getLevel())));
            }
        });
    }
}
