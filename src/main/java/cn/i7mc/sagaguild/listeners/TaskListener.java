package cn.i7mc.sagaguild.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildTask;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * 任务监听器
 * 处理任务相关的事件
 */
public class TaskListener implements Listener {
    private final SagaGuild plugin;
    
    public TaskListener(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 实体死亡事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // 检查是否是玩家击杀
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 更新任务进度
        plugin.getTaskManager().updateTaskProgress(
                guild.getId(),
                GuildTask.Type.KILL_MOBS,
                event.getEntityType().name(),
                1,
                player.getUniqueId()
        );
    }
    
    /**
     * 方块破坏事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 更新任务进度
        plugin.getTaskManager().updateTaskProgress(
                guild.getId(),
                GuildTask.Type.BREAK_BLOCKS,
                event.getBlock().getType().name(),
                1,
                player.getUniqueId()
        );
    }
    
    /**
     * 方块放置事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 更新任务进度
        plugin.getTaskManager().updateTaskProgress(
                guild.getId(),
                GuildTask.Type.PLACE_BLOCKS,
                event.getBlock().getType().name(),
                1,
                player.getUniqueId()
        );
    }
    
    /**
     * 钓鱼事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // 检查是否成功钓到鱼
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 更新任务进度
        plugin.getTaskManager().updateTaskProgress(
                guild.getId(),
                GuildTask.Type.FISH,
                "ANY",
                1,
                player.getUniqueId()
        );
    }
    
    /**
     * 物品合成事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }
        
        // 更新任务进度
        plugin.getTaskManager().updateTaskProgress(
                guild.getId(),
                GuildTask.Type.CRAFT,
                event.getRecipe().getResult().getType().name(),
                1,
                player.getUniqueId()
        );
    }
}
