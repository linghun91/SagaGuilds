package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Alliance;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildWar;
import cn.i7mc.sagaguild.gui.holders.GuildRelationManageHolder;
import cn.i7mc.sagaguild.utils.ItemUtil;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 公会关系管理监听器
 * 处理公会关系管理GUI的点击事件
 */
public class GuildRelationManageListener implements Listener {
    private final SagaGuild plugin;

    public GuildRelationManageListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会关系管理GUI
        if (!(event.getInventory().getHolder() instanceof GuildRelationManageHolder)) {
            return;
        }

        // 取消事件
        event.setCancelled(true);

        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuildRelationManageHolder holder = (GuildRelationManageHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        int page = holder.getPage();

        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 检查玩家权限
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || (!member.isOwner() && !member.isAdmin())) {
            GUIUtils.closeGUI(player);
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 处理点击事件
        if (event.getSlot() < 45) {
            // 点击关系项目
            handleRelationItemClick(player, guild, clickedItem, event);
        } else if (event.getSlot() == 45 && clickedItem.getType() == Material.ARROW) {
            // 点击上一页按钮
            plugin.getGuiManager().openGuildRelationManageGUI(player, guild, page - 1);
        } else if (event.getSlot() == 53 && clickedItem.getType() == Material.ARROW) {
            // 点击下一页按钮
            plugin.getGuiManager().openGuildRelationManageGUI(player, guild, page + 1);
        } else if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            // 点击返回按钮
            GUIUtils.closeGUI(player);
            plugin.getGuiManager().openGuildManageGUI(player, guild);
        }
    }

    /**
     * 处理关系项目点击
     * @param player 玩家
     * @param guild 玩家公会
     * @param item 物品
     * @param event 点击事件
     */
    private void handleRelationItemClick(Player player, Guild guild, ItemStack item, InventoryClickEvent event) {
        // 获取物品元数据
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // 获取物品描述
        List<String> lore = ItemUtil.getLore(meta);
        if (lore.isEmpty()) {
            return;
        }

        // 检查是否是联盟申请
        if (lore.stream().anyMatch(line -> line.contains("联盟申请"))) {
            handleAllianceRequestClick(player, guild, item, event);
        }
        // 检查是否是停战申请
        else if (lore.stream().anyMatch(line -> line.contains("停战申请"))) {
            handleCeasefireRequestClick(player, guild, item, event);
        }
    }

    /**
     * 处理联盟申请点击
     * @param player 玩家
     * @param guild 玩家公会
     * @param item 物品
     */
    private void handleAllianceRequestClick(Player player, Guild guild, ItemStack item, InventoryClickEvent event) {
        // 获取公会名称
        String displayName = ItemUtil.getDisplayName(item.getItemMeta());
        if (displayName.isEmpty()) {
            return;
        }

        // 从物品名称中提取公会名称
        // 格式通常是: "§b公会名称 §8[§7标签§8]"
        if (!displayName.contains("§8[")) {
            return;
        }

        String guildName = displayName.substring(displayName.indexOf("§b") + 2, displayName.indexOf(" §8["));

        // 获取公会
        Guild requestingGuild = plugin.getGuildManager().getGuildByName(guildName);
        if (requestingGuild == null) {
            return;
        }

        // 根据点击类型处理
        if (event.isLeftClick()) {
            // 左键：接受联盟申请
            boolean success = plugin.getAllianceManager().acceptAllianceRequest(guild.getId(), requestingGuild.getId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("alliance.request-accepted", "guild", requestingGuild.getName()));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("alliance.alliance-accept-failed"));
            }
        } else if (event.isRightClick()) {
            // 右键：拒绝联盟申请
            boolean success = plugin.getAllianceManager().rejectAllianceRequest(guild.getId(), requestingGuild.getId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("alliance.request-rejected", "guild", requestingGuild.getName()));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("alliance.alliance-reject-failed"));
            }
        } else {
            return;
        }

        // 刷新GUI
        GUIUtils.closeGUI(player);
        plugin.getGuiManager().openGuildRelationManageGUI(player, guild, 1);
    }

    /**
     * 处理停战申请点击
     * @param player 玩家
     * @param guild 玩家公会
     * @param item 物品
     */
    private void handleCeasefireRequestClick(Player player, Guild guild, ItemStack item, InventoryClickEvent event) {
        // 获取公会名称
        String displayName = ItemUtil.getDisplayName(item.getItemMeta());
        if (displayName.isEmpty()) {
            return;
        }

        // 从物品名称中提取公会名称
        // 格式通常是: "§b公会名称 §8[§7标签§8]"
        if (!displayName.contains("§8[")) {
            return;
        }

        String guildName = displayName.substring(displayName.indexOf("§b") + 2, displayName.indexOf(" §8["));

        // 获取公会
        Guild requestingGuild = plugin.getGuildManager().getGuildByName(guildName);
        if (requestingGuild == null) {
            return;
        }

        // 根据点击类型处理
        if (event.isLeftClick()) {
            // 左键：接受停战申请
            boolean success = plugin.getWarManager().acceptCeasefire(guild.getId(), requestingGuild.getId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-accepted", "guild", requestingGuild.getName()));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-accept-failed"));
            }
        } else if (event.isRightClick()) {
            // 右键：拒绝停战申请
            boolean success = plugin.getWarManager().rejectCeasefire(guild.getId(), requestingGuild.getId());
            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-rejected", "guild", requestingGuild.getName()));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-reject-failed"));
            }
        } else {
            return;
        }

        // 刷新GUI
        GUIUtils.closeGUI(player);
        plugin.getGuiManager().openGuildRelationManageGUI(player, guild, 1);
    }
}
