package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.JoinRequest;
import cn.i7mc.sagaguild.gui.holders.JoinRequestHolder;
import cn.i7mc.sagaguild.utils.ItemUtil;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 公会加入请求GUI监听器
 */
public class JoinRequestListener implements Listener {
    private final SagaGuild plugin;

    public JoinRequestListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof JoinRequestHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        JoinRequestHolder holder = (JoinRequestHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        int page = holder.getPage();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 检查玩家是否有权限管理公会
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder() || member.getGuildId() != guild.getId()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            GUIUtils.closeGUI(player);
            return;
        }

        // 处理点击事件
        if (event.getSlot() < 45) {
            // 点击请求物品
            handleRequestItemClick(player, clickedItem, event.getClick());
        } else if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            // 点击返回按钮
            plugin.getGuiManager().openGuildManageGUI(player, guild);
        }
    }

    /**
     * 处理请求物品点击
     * @param player 玩家
     * @param item 物品
     * @param clickType 点击类型
     */
    private void handleRequestItemClick(Player player, ItemStack item, ClickType clickType) {
        // 获取请求ID
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            plugin.getLogger().warning("物品没有Lore");
            return;
        }

        // 从物品Lore中提取请求ID
        List<String> lore = ItemUtil.getLore(meta);
        int requestId = -1;

        for (String line : lore) {
            if (line.contains("ID:")) {
                try {
                    // 提取ID部分，去除所有颜色代码和非数字字符
                    String idStr = line.replaceAll("§[0-9a-fk-or]", "").replaceAll("[^0-9]", "");
                    requestId = Integer.parseInt(idStr);
                    break;
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("解析请求ID失败: " + line);
                }
            }
        }

        if (requestId == -1) {
            plugin.getLogger().warning("未找到有效的请求ID");
            return;
        }

        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            plugin.getLogger().warning("玩家不在任何公会中");
            player.sendMessage("§c你不在任何公会中，无法处理请求");
            GUIUtils.closeGUI(player);
            return;
        }

        // 根据点击类型处理请求
        if (clickType == ClickType.LEFT) {
            // 左键点击 - 接受请求
            boolean success = plugin.getGuildManager().acceptJoinRequest(player, requestId);

            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.join-request-accepted"));
                // 刷新GUI
                GUIUtils.closeGUI(player);
                plugin.getGuiManager().openGuildManageGUI(player, guild);
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.join-request-accept-failed"));
            }
        } else if (clickType == ClickType.RIGHT) {
            // 右键点击 - 拒绝请求
            boolean success = plugin.getGuildManager().rejectJoinRequest(player, requestId);

            if (success) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.join-request-rejected"));
                // 刷新GUI
                GUIUtils.closeGUI(player);
                plugin.getGuiManager().openGuildManageGUI(player, guild);
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.join-request-reject-failed"));
            }
        }
    }
}
