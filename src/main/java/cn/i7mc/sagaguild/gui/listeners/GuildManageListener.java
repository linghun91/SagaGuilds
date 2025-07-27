package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.gui.holders.GuildManageHolder;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 公会管理监听器
 * 处理公会管理GUI的点击事件
 */
public class GuildManageListener implements Listener {
    private final SagaGuild plugin;

    public GuildManageListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会管理GUI
        if (!(event.getInventory().getHolder() instanceof GuildManageHolder)) {
            return;
        }

        // 取消事件
        event.setCancelled(true);

        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuildManageHolder holder = (GuildManageHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();

        // 检查玩家是否有权限管理公会
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
            case 10: // 基本信息管理
                plugin.getGuiManager().openGuildSettingsGUI(player, guild);
                break;
            case 12: // 成员管理
                plugin.getGuiManager().openGuildMemberGUI(player, guild, 1);
                break;
            case 14: // 银行管理
                GUIUtils.closeGUI(player);
                player.performCommand("guild bank");
                break;
            case 16: // 领地管理
                GUIUtils.closeGUI(player);
                player.performCommand("guild claim");
                break;
            case 28: // 活动管理
                GUIUtils.closeGUI(player);
                player.performCommand("guild activity");
                break;
            case 30: // 任务管理
                GUIUtils.closeGUI(player);
                player.performCommand("guild task");
                break;
            case 32: // 公会关系管理
                Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (playerGuild != null) {
                    plugin.getGuiManager().openGuildRelationManageGUI(player, playerGuild, 1);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
                    GUIUtils.closeGUI(player);
                }
                break;
            case 33: // 加入申请管理
                Guild playerGuild2 = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (playerGuild2 != null) {
                    plugin.getGuiManager().openJoinRequestGUI(player, playerGuild2, 1);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
                    GUIUtils.closeGUI(player);
                }
                break;
            case 34: // 公会战管理
                GUIUtils.closeGUI(player);
                player.performCommand("guild war");
                break;
            case 49: // 返回
                GUIUtils.closeGUI(player);
                break;
        }
    }
}
