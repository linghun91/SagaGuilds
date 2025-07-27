package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.gui.holders.GuildLandSettingsHolder;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 公会领地设置监听器
 * 处理公会领地设置GUI的点击事件
 */
public class GuildLandSettingsListener implements Listener {
    private final SagaGuild plugin;
    
    public GuildLandSettingsListener(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会领地设置GUI
        if (!(event.getInventory().getHolder() instanceof GuildLandSettingsHolder)) {
            return;
        }
        
        // 取消事件
        event.setCancelled(true);
        
        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        GuildLandSettingsHolder holder = (GuildLandSettingsHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        
        // 检查玩家是否有权限管理领地设置
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            GUIUtils.closeGUI(player);
            return;
        }
        
        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // 处理点击事件
        switch (event.getSlot()) {
            case 10: // 领地保护设置
                toggleLandProtection(player, guild);
                refreshLandSettingsGUI(player, guild, event.getInventory());
                break;
            case 12: // PVP设置
                togglePvpSetting(player, guild);
                refreshLandSettingsGUI(player, guild, event.getInventory());
                break;
            case 14: // 访客权限设置
                toggleGuestPermission(player, guild);
                refreshLandSettingsGUI(player, guild, event.getInventory());
                break;
            case 16: // 建筑权限设置
                toggleBuildPermission(player, guild);
                refreshLandSettingsGUI(player, guild, event.getInventory());
                break;
            case 40: // 重置所有设置
                resetLandSettings(player, guild);
                refreshLandSettingsGUI(player, guild, event.getInventory());
                break;
            case 49: // 返回
                GUIUtils.closeGUI(player);
                plugin.getGuiManager().openGuildSettingsGUI(player, guild);
                break;
        }
    }
    
    /**
     * 切换领地保护设置
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleLandProtection(Player player, Guild guild) {
        ConfigurationSection landConfig = plugin.getConfig().getConfigurationSection("land.protection");
        boolean currentSetting = landConfig != null ? landConfig.getBoolean(guild.getName() + ".enabled", true) : true;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("land.protection." + guild.getName() + ".enabled", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "启用" : "禁用";
        player.sendMessage(plugin.getConfigManager().getMessage("land.protection-toggled", 
            "status", status));
    }
    
    /**
     * 切换PVP设置
     * @param player 玩家
     * @param guild 公会
     */
    private void togglePvpSetting(Player player, Guild guild) {
        ConfigurationSection landConfig = plugin.getConfig().getConfigurationSection("land.pvp");
        boolean currentSetting = landConfig != null ? landConfig.getBoolean(guild.getName() + ".enabled", false) : false;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("land.pvp." + guild.getName() + ".enabled", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "启用" : "禁用";
        player.sendMessage(plugin.getConfigManager().getMessage("land.pvp-toggled", 
            "status", status));
    }
    
    /**
     * 切换访客权限设置
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleGuestPermission(Player player, Guild guild) {
        ConfigurationSection landConfig = plugin.getConfig().getConfigurationSection("land.guest");
        boolean currentSetting = landConfig != null ? landConfig.getBoolean(guild.getName() + ".access", true) : true;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("land.guest." + guild.getName() + ".access", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "允许" : "禁止";
        player.sendMessage(plugin.getConfigManager().getMessage("land.guest-access-toggled", 
            "status", status));
    }
    
    /**
     * 切换建筑权限设置
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleBuildPermission(Player player, Guild guild) {
        ConfigurationSection landConfig = plugin.getConfig().getConfigurationSection("land.build");
        boolean currentSetting = landConfig != null ? landConfig.getBoolean(guild.getName() + ".members-only", true) : true;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("land.build." + guild.getName() + ".members-only", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "仅成员" : "所有人";
        player.sendMessage(plugin.getConfigManager().getMessage("land.build-permission-toggled", 
            "status", status));
    }
    
    /**
     * 重置领地设置
     * @param player 玩家
     * @param guild 公会
     */
    private void resetLandSettings(Player player, Guild guild) {
        // 重置所有领地相关配置
        plugin.getConfig().set("land.protection." + guild.getName(), null);
        plugin.getConfig().set("land.pvp." + guild.getName(), null);
        plugin.getConfig().set("land.guest." + guild.getName(), null);
        plugin.getConfig().set("land.build." + guild.getName(), null);
        plugin.saveConfig();
        
        player.sendMessage(plugin.getConfigManager().getMessage("land.settings-reset"));
    }
    
    /**
     * 刷新领地设置GUI（不关闭）
     * @param player 玩家
     * @param guild 公会
     * @param inventory 当前物品栏
     */
    private void refreshLandSettingsGUI(Player player, Guild guild, org.bukkit.inventory.Inventory inventory) {
        // 重新打开GUI以刷新显示
        plugin.getGuiManager().openGuildLandSettingsGUI(player, guild);
    }
}