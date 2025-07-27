package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.gui.holders.GuildSettingsHolder;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 公会设置监听器
 * 处理公会设置GUI的点击事件
 */
public class GuildSettingsListener implements Listener {
    private final SagaGuild plugin;
    
    public GuildSettingsListener(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会设置GUI
        if (!(event.getInventory().getHolder() instanceof GuildSettingsHolder)) {
            return;
        }
        
        // 取消事件
        event.setCancelled(true);
        
        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        GuildSettingsHolder holder = (GuildSettingsHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        
        // 检查玩家是否是公会会长
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isOwner()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.owner-only"));
            GUIUtils.closeGUI(player);
            return;
        }
        
        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // 检查是否是颜色选择GUI
        if (event.getInventory().getSize() == 27 && 
            event.getView().getTitle().equals("选择标签颜色")) {
            handleColorSelection(player, guild, event);
            return;
        }
        
        // 处理点击事件
        switch (event.getSlot()) {
            case 10: // 公会公开性设置
                toggleGuildPublic(player, guild);
                // 刷新当前GUI而不关闭
                refreshGuildSettingsGUI(player, guild, event.getInventory());
                break;
            case 12: // 公会描述设置
                // 关闭GUI，让玩家输入新的描述
                GUIUtils.closeGUI(player);
                player.sendMessage(plugin.getConfigManager().getMessage("guild.enter-description"));
                player.sendMessage("§7输入 §ecancel §7或 §e取消 §7来取消操作");
                // 设置输入状态
                plugin.getPlayerInputListener().setPlayerInputState(player, 
                    cn.i7mc.sagaguild.listeners.PlayerInputListener.InputType.GUILD_DESCRIPTION, guild);
                break;
            case 14: // 公会公告设置
                // 关闭GUI，让玩家输入新的公告
                GUIUtils.closeGUI(player);
                player.sendMessage(plugin.getConfigManager().getMessage("guild.enter-announcement"));
                player.sendMessage("§7输入 §ecancel §7或 §e取消 §7来取消操作");
                // 设置输入状态
                plugin.getPlayerInputListener().setPlayerInputState(player, 
                    cn.i7mc.sagaguild.listeners.PlayerInputListener.InputType.GUILD_ANNOUNCEMENT, guild);
                break;
            case 16: // 公会标签颜色设置
                openTagColorSelectionGUI(player, guild);
                break;
            case 28: // 领地相关设置
                plugin.getGuiManager().openGuildLandSettingsGUI(player, guild);
                break;
            case 30: // 聊天设置
                plugin.getGuiManager().openGuildChatSettingsGUI(player, guild);
                break;
            case 40: // 解散公会
                handleGuildDisband(player, guild);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGuildManageGUI(player, guild);
                break;
        }
    }
    
    /**
     * 切换公会公开性
     * @param player 玩家
     * @param guild 公会
     */
    private void toggleGuildPublic(Player player, Guild guild) {
        // 切换公会公开性
        guild.setPublic(!guild.isPublic());

        // 更新数据库
        boolean success = plugin.getGuildManager().updateGuild(guild);

        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.public-toggled", "state", guild.isPublic() ? "公开" : "私有"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.update-failed"));
        }
    }

    /**
     * 处理公会解散
     * @param player 玩家
     * @param guild 公会
     */
    private void handleGuildDisband(Player player, Guild guild) {
        // 关闭GUI
        GUIUtils.closeGUI(player);

        // 发送确认消息
        player.sendMessage("§c§l警告: 你确定要解散公会 §f" + guild.getName() + " §c§l吗？");
        player.sendMessage("§c此操作将永久删除公会的所有数据，包括成员、领地、联盟等！");
        player.sendMessage("§e如果确认解散，请在30秒内输入: §f/guild disband");
        player.sendMessage("§7如果不想解散，请忽略此消息。");
    }
    
    /**
     * 刷新公会设置GUI（不关闭）
     * @param player 玩家
     * @param guild 公会
     * @param inventory 当前物品栏
     */
    private void refreshGuildSettingsGUI(Player player, Guild guild, org.bukkit.inventory.Inventory inventory) {
        // 更新公会公开性设置按钮
        org.bukkit.Material publicMaterial = guild.isPublic() ? org.bukkit.Material.LIME_DYE : org.bukkit.Material.GRAY_DYE;
        org.bukkit.inventory.ItemStack publicItem = new org.bukkit.inventory.ItemStack(publicMaterial);
        org.bukkit.inventory.meta.ItemMeta publicMeta = publicItem.getItemMeta();
        cn.i7mc.sagaguild.utils.ItemUtil.setDisplayName(publicMeta, net.kyori.adventure.text.Component.text(plugin.getConfigManager().getMessage("gui.guild-public-setting")));
        java.util.List<net.kyori.adventure.text.Component> publicLore = new java.util.ArrayList<>();
        publicLore.add(net.kyori.adventure.text.Component.text("§7当前状态: §f" + (guild.isPublic() ? "公开" : "私有")));
        publicLore.add(net.kyori.adventure.text.Component.text(""));
        publicLore.add(net.kyori.adventure.text.Component.text("§e点击切换"));
        cn.i7mc.sagaguild.utils.ItemUtil.setLore(publicMeta, publicLore);
        publicItem.setItemMeta(publicMeta);
        inventory.setItem(10, publicItem);
    }
    
    /**
     * 打开标签颜色选择GUI
     * @param player 玩家
     * @param guild 公会
     */
    private void openTagColorSelectionGUI(Player player, Guild guild) {
        // 创建临时的颜色选择界面
        org.bukkit.inventory.Inventory colorInventory = org.bukkit.Bukkit.createInventory(
            new cn.i7mc.sagaguild.gui.holders.GuildSettingsHolder(guild), 
            27, 
            net.kyori.adventure.text.Component.text("选择标签颜色")
        );
        
        // 添加所有可用颜色
        String[] colors = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        org.bukkit.Material[] colorMaterials = {
            Material.BLACK_DYE, Material.BLUE_DYE, Material.GREEN_DYE, Material.CYAN_DYE,
            Material.RED_DYE, Material.PURPLE_DYE, Material.ORANGE_DYE, Material.LIGHT_GRAY_DYE,
            Material.GRAY_DYE, Material.LIGHT_BLUE_DYE, Material.LIME_DYE, Material.LIGHT_BLUE_DYE,
            Material.RED_DYE, Material.MAGENTA_DYE, Material.YELLOW_DYE, Material.WHITE_DYE
        };
        
        String[] colorNames = {
            "黑色", "深蓝", "深绿", "青色", "红色", "紫色", "橙色", "浅灰",
            "深灰", "蓝色", "绿色", "浅蓝", "浅红", "品红", "黄色", "白色"
        };
        
        for (int i = 0; i < colors.length; i++) {
            org.bukkit.inventory.ItemStack colorItem = new org.bukkit.inventory.ItemStack(colorMaterials[i]);
            org.bukkit.inventory.meta.ItemMeta meta = colorItem.getItemMeta();
            
            String colorCode = "§" + colors[i];
            cn.i7mc.sagaguild.utils.ItemUtil.setDisplayName(meta, 
                net.kyori.adventure.text.Component.text(colorCode + colorNames[i] + " (&" + colors[i] + ")"));
            
            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("§7颜色代码: §f&" + colors[i]));
            lore.add(net.kyori.adventure.text.Component.text("§7预览: " + colorCode + guild.getTag()));
            lore.add(net.kyori.adventure.text.Component.text(""));
            if (guild.getTagColor().equals(colors[i])) {
                lore.add(net.kyori.adventure.text.Component.text("§a当前使用的颜色"));
            } else {
                lore.add(net.kyori.adventure.text.Component.text("§e点击选择该颜色"));
            }
            cn.i7mc.sagaguild.utils.ItemUtil.setLore(meta, lore);
            colorItem.setItemMeta(meta);
            
            colorInventory.setItem(i, colorItem);
        }
        
        // 返回按钮
        org.bukkit.inventory.ItemStack backButton = new org.bukkit.inventory.ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta backMeta = backButton.getItemMeta();
        cn.i7mc.sagaguild.utils.ItemUtil.setDisplayName(backMeta, 
            net.kyori.adventure.text.Component.text("§c返回设置"));
        backButton.setItemMeta(backMeta);
        colorInventory.setItem(26, backButton);
        
        player.openInventory(colorInventory);
    }
    
    /**
     * 处理颜色选择
     * @param player 玩家
     * @param guild 公会
     * @param event 点击事件
     */
    private void handleColorSelection(Player player, Guild guild, InventoryClickEvent event) {
        if (event.getSlot() == 26) {
            // 返回按钮
            plugin.getGuiManager().openGuildSettingsGUI(player, guild);
            return;
        }
        
        // 获取颜色数组
        String[] colors = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        
        if (event.getSlot() < colors.length) {
            String newColor = colors[event.getSlot()];
            
            // 检查是否是当前颜色
            if (guild.getTagColor().equals(newColor)) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.tag-color-already-set"));
                return;
            }
            
            // 使用TagColorCommand的逻辑来设置颜色
            guild.setTagColor(newColor);
            boolean success = plugin.getGuildManager().updateGuild(guild);
            
            if (success) {
                String colorName = getColorName(newColor);
                player.sendMessage(plugin.getConfigManager().getMessage("guild.tag-color-changed", "color", colorName));
                // 返回设置界面
                plugin.getGuiManager().openGuildSettingsGUI(player, guild);
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.tag-color-failed"));
            }
        }
    }
    
    /**
     * 获取颜色名称
     * @param colorCode 颜色代码
     * @return 颜色名称
     */
    private String getColorName(String colorCode) {
        String[] colorNames = {
            "黑色", "深蓝", "深绿", "青色", "红色", "紫色", "橙色", "浅灰",
            "深灰", "蓝色", "绿色", "浅蓝", "浅红", "品红", "黄色", "白色"
        };
        String[] colors = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
        
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].equals(colorCode)) {
                return colorNames[i];
            }
        }
        return "未知";
    }
}
