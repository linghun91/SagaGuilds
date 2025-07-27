package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.gui.holders.GuildChatSettingsHolder;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

/**
 * 公会聊天设置监听器
 * 处理公会聊天设置GUI的点击事件
 */
public class GuildChatSettingsListener implements Listener {
    private final SagaGuild plugin;
    
    public GuildChatSettingsListener(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会聊天设置GUI
        if (!(event.getInventory().getHolder() instanceof GuildChatSettingsHolder)) {
            return;
        }
        
        // 取消事件
        event.setCancelled(true);
        
        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        GuildChatSettingsHolder holder = (GuildChatSettingsHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        
        // 检查玩家是否有权限管理聊天设置
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
            case 10: // 公会聊天开关
                toggleGuildChat(player, guild);
                refreshChatSettingsGUI(player, guild, event.getInventory());
                break;
            case 12: // 联盟聊天开关
                toggleAllyChat(player, guild);
                refreshChatSettingsGUI(player, guild, event.getInventory());
                break;
            case 14: // 聊天过滤开关
                toggleChatFilter(player, guild);
                refreshChatSettingsGUI(player, guild, event.getInventory());
                break;
            case 16: // 自动进入公会聊天
                toggleAutoGuildChat(player, guild);
                refreshChatSettingsGUI(player, guild, event.getInventory());
                break;
            case 28: // 聊天格式设置
                handleChatFormatSetting(player, guild);
                break;
            case 30: // 聊天前缀设置
                handleChatPrefixSetting(player, guild);
                break;
            case 40: // 重置聊天设置
                resetChatSettings(player, guild);
                refreshChatSettingsGUI(player, guild, event.getInventory());
                break;
            case 49: // 返回
                GUIUtils.closeGUI(player);
                plugin.getGuiManager().openGuildSettingsGUI(player, guild);
                break;
        }
    }
    
    /**
     * 切换公会聊天功能
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleGuildChat(Player player, Guild guild) {
        ConfigurationSection chatConfig = plugin.getConfig().getConfigurationSection("chat.guild");
        boolean currentSetting = chatConfig != null ? chatConfig.getBoolean(guild.getName() + ".enabled", true) : true;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("chat.guild." + guild.getName() + ".enabled", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "启用" : "禁用";
        player.sendMessage(plugin.getConfigManager().getMessage("chat.guild-chat-toggled", 
            "status", status));
    }
    
    /**
     * 切换联盟聊天功能
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleAllyChat(Player player, Guild guild) {
        ConfigurationSection chatConfig = plugin.getConfig().getConfigurationSection("chat.ally");
        boolean currentSetting = chatConfig != null ? chatConfig.getBoolean(guild.getName() + ".enabled", true) : true;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("chat.ally." + guild.getName() + ".enabled", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "启用" : "禁用";
        player.sendMessage(plugin.getConfigManager().getMessage("chat.ally-chat-toggled", 
            "status", status));
    }
    
    /**
     * 切换聊天过滤功能
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleChatFilter(Player player, Guild guild) {
        ConfigurationSection chatConfig = plugin.getConfig().getConfigurationSection("chat.filter");
        boolean currentSetting = chatConfig != null ? chatConfig.getBoolean(guild.getName() + ".enabled", false) : false;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("chat.filter." + guild.getName() + ".enabled", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "启用" : "禁用";
        player.sendMessage(plugin.getConfigManager().getMessage("chat.filter-toggled", 
            "status", status));
    }
    
    /**
     * 切换自动进入公会聊天
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleAutoGuildChat(Player player, Guild guild) {
        ConfigurationSection chatConfig = plugin.getConfig().getConfigurationSection("chat.auto");
        boolean currentSetting = chatConfig != null ? chatConfig.getBoolean(guild.getName() + ".enabled", false) : false;
        
        // 切换设置
        boolean newSetting = !currentSetting;
        plugin.getConfig().set("chat.auto." + guild.getName() + ".enabled", newSetting);
        plugin.saveConfig();
        
        String status = newSetting ? "启用" : "禁用";
        player.sendMessage(plugin.getConfigManager().getMessage("chat.auto-guild-chat-toggled", 
            "status", status));
    }
    
    /**
     * 处理聊天格式设置
     * @param player 玩家
     * @param guild 公会
     */
    private void handleChatFormatSetting(Player player, Guild guild) {
        GUIUtils.closeGUI(player);
        player.sendMessage(plugin.getConfigManager().getMessage("chat.enter-format"));
        player.sendMessage("§7输入 §ecancel §7或 §e取消 §7来取消操作");
        
        // 设置输入状态为聊天格式设置
        plugin.getPlayerInputListener().setPlayerInputState(player, 
            cn.i7mc.sagaguild.listeners.PlayerInputListener.InputType.CHAT_FORMAT, guild);
    }
    
    /**
     * 处理聊天前缀设置
     * @param player 玩家
     * @param guild 公会
     */
    private void handleChatPrefixSetting(Player player, Guild guild) {
        GUIUtils.closeGUI(player);
        player.sendMessage(plugin.getConfigManager().getMessage("chat.enter-prefix"));
        player.sendMessage("§7输入 §ecancel §7或 §e取消 §7来取消操作");
        
        // 设置输入状态为聊天前缀设置
        plugin.getPlayerInputListener().setPlayerInputState(player, 
            cn.i7mc.sagaguild.listeners.PlayerInputListener.InputType.CHAT_PREFIX, guild);
    }
    
    /**
     * 重置聊天设置
     * @param player 玩家
     * @param guild 公会
     */
    private void resetChatSettings(Player player, Guild guild) {
        // 重置所有聊天相关配置
        plugin.getConfig().set("chat.guild." + guild.getName(), null);
        plugin.getConfig().set("chat.ally." + guild.getName(), null);
        plugin.getConfig().set("chat.filter." + guild.getName(), null);
        plugin.getConfig().set("chat.auto." + guild.getName(), null);
        plugin.getConfig().set("chat.format." + guild.getName() + ".value", null);
        plugin.getConfig().set("chat.prefix." + guild.getName() + ".value", null);
        plugin.saveConfig();
        
        player.sendMessage(plugin.getConfigManager().getMessage("chat.settings-reset"));
    }
    
    /**
     * 刷新聊天设置GUI（不关闭）
     * @param player 玩家
     * @param guild 公会
     * @param inventory 当前物品栏
     */
    private void refreshChatSettingsGUI(Player player, Guild guild, org.bukkit.inventory.Inventory inventory) {
        // 获取最新的配置值
        ConfigurationSection guildChatConfig = plugin.getConfig().getConfigurationSection("chat.guild");
        boolean guildChatEnabled = guildChatConfig != null ? guildChatConfig.getBoolean(guild.getName() + ".enabled", true) : true;
        
        ConfigurationSection allyChatConfig = plugin.getConfig().getConfigurationSection("chat.ally");
        boolean allyChatEnabled = allyChatConfig != null ? allyChatConfig.getBoolean(guild.getName() + ".enabled", true) : true;
        
        ConfigurationSection chatFilterConfig = plugin.getConfig().getConfigurationSection("chat.filter");
        boolean filterEnabled = chatFilterConfig != null ? chatFilterConfig.getBoolean(guild.getName() + ".enabled", false) : false;
        
        ConfigurationSection autoChatConfig = plugin.getConfig().getConfigurationSection("chat.auto");
        boolean autoGuildChatEnabled = autoChatConfig != null ? autoChatConfig.getBoolean(guild.getName() + ".enabled", false) : false;
        
        // 更新公会聊天开关物品 (位置10)
        updateToggleItem(inventory, 10, Material.DIAMOND, Material.COAL, guildChatEnabled, 
            "§a公会聊天", "§7当前状态: ", "§7成员之间的私密聊天", "§e点击切换");
        
        // 更新联盟聊天开关物品 (位置12)
        updateToggleItem(inventory, 12, Material.GOLD_INGOT, Material.IRON_INGOT, allyChatEnabled,
            "§6联盟聊天", "§7当前状态: ", "§7与联盟公会的交流", "§e点击切换");
        
        // 更新聊天过滤开关物品 (位置14)
        updateToggleItem(inventory, 14, Material.EMERALD, Material.BARRIER, filterEnabled,
            "§d聊天过滤", "§7当前状态: ", "§7过滤不当言论", "§e点击切换");
        
        // 更新自动进入公会聊天物品 (位置16)
        updateToggleItem(inventory, 16, Material.REPEATER, Material.REDSTONE, autoGuildChatEnabled,
            "§b自动公会聊天", "§7当前状态: ", "§7新成员自动进入公会聊天", "§e点击切换");
    }
    
    /**
     * 更新开关类型的物品
     */
    private void updateToggleItem(org.bukkit.inventory.Inventory inventory, int slot, 
            Material enabledMaterial, Material disabledMaterial, boolean enabled,
            String displayName, String statusPrefix, String description, String action) {
        ItemStack item = new ItemStack(enabled ? enabledMaterial : disabledMaterial);
        ItemMeta meta = item.getItemMeta();
        cn.i7mc.sagaguild.utils.ItemUtil.setDisplayName(meta, net.kyori.adventure.text.Component.text(displayName));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text(statusPrefix + (enabled ? "§a启用" : "§c禁用")));
        lore.add(net.kyori.adventure.text.Component.text("§7" + description));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text(action));
        cn.i7mc.sagaguild.utils.ItemUtil.setLore(meta, lore);
        
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
}