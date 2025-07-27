package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildWar;
import cn.i7mc.sagaguild.gui.holders.GuildRelationHolder;
import cn.i7mc.sagaguild.utils.ItemUtil;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 公会关系设置监听器
 * 处理公会关系设置GUI的点击事件
 */
public class GuildRelationListener implements Listener {
    private final SagaGuild plugin;

    public GuildRelationListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会关系设置GUI
        if (!(event.getInventory().getHolder() instanceof GuildRelationHolder)) {
            return;
        }

        // 取消事件
        event.setCancelled(true);

        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuildRelationHolder holder = (GuildRelationHolder) event.getInventory().getHolder();
        Guild targetGuild = holder.getTargetGuild();

        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 获取玩家所在公会
        Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (playerGuild == null) {
            GUIUtils.closeGUI(player);
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
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
        switch (event.getSlot()) {
            case 11: // 申请结盟
                handleAllianceRequest(player, playerGuild, targetGuild);
                break;
            case 13: // 宣战
                handleWarDeclaration(player, playerGuild, targetGuild);
                break;
            case 15: // 解除结盟
                handleBreakAlliance(player, playerGuild, targetGuild);
                break;
            case 17: // 停战
                handleCeasefire(player, playerGuild, targetGuild);
                break;
            case 49: // 返回
                plugin.getGuiManager().openGuildListGUI(player, 1);
                break;
        }
    }

    /**
     * 处理结盟申请
     * @param player 玩家
     * @param playerGuild 玩家公会
     * @param targetGuild 目标公会
     */
    private void handleAllianceRequest(Player player, Guild playerGuild, Guild targetGuild) {
        // 检查是否已经是联盟
        if (plugin.getAllianceManager().areGuildsAllied(playerGuild.getId(), targetGuild.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.already-allied"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 检查是否处于战争状态
        GuildWar war = plugin.getWarManager().getActiveWarBetweenGuilds(playerGuild.getId(), targetGuild.getId());
        if (war != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("war.cannot-ally-during-war"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 发送结盟申请
        boolean success = plugin.getAllianceManager().sendAllianceRequest(playerGuild.getId(), targetGuild.getId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.request-sent", "guild", targetGuild.getName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.alliance-request-failed"));
        }

        GUIUtils.closeGUI(player);
    }

    /**
     * 处理宣战
     * @param player 玩家
     * @param playerGuild 玩家公会
     * @param targetGuild 目标公会
     */
    private void handleWarDeclaration(Player player, Guild playerGuild, Guild targetGuild) {
        // 检查是否已经处于战争状态
        if (plugin.getWarManager().areGuildsAtWar(playerGuild.getId(), targetGuild.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.already-at-war"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 检查是否是联盟
        if (plugin.getAllianceManager().areGuildsAllied(playerGuild.getId(), targetGuild.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.cannot-declare-war-on-ally"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 宣战
        boolean success = plugin.getWarManager().inviteToWar(player, targetGuild.getName());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.war-declared", "guild", targetGuild.getName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("war.war-declaration-failed"));
        }

        GUIUtils.closeGUI(player);
    }

    /**
     * 处理解除结盟
     * @param player 玩家
     * @param playerGuild 玩家公会
     * @param targetGuild 目标公会
     */
    private void handleBreakAlliance(Player player, Guild playerGuild, Guild targetGuild) {
        // 检查是否是联盟
        if (!plugin.getAllianceManager().areGuildsAllied(playerGuild.getId(), targetGuild.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.not-allied"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 解除结盟
        boolean success = plugin.getAllianceManager().breakAlliance(player, targetGuild.getId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.broken", "guild", targetGuild.getName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("alliance.break-failed"));
        }

        GUIUtils.closeGUI(player);
    }

    /**
     * 处理停战
     * @param player 玩家
     * @param playerGuild 玩家公会
     * @param targetGuild 目标公会
     */
    private void handleCeasefire(Player player, Guild playerGuild, Guild targetGuild) {
        // 检查是否处于战争状态
        if (!plugin.getWarManager().areGuildsAtWar(playerGuild.getId(), targetGuild.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-at-war"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 停战
        boolean success = plugin.getWarManager().requestCeasefire(player, targetGuild.getId());
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-requested", "guild", targetGuild.getName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.ceasefire-request-failed"));
        }

        GUIUtils.closeGUI(player);
    }
}
