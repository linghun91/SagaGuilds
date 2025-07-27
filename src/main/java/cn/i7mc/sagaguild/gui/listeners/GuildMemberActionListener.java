package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.gui.holders.GuildMemberActionHolder;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 公会成员操作监听器
 * 处理公会成员操作GUI的点击事件
 */
public class GuildMemberActionListener implements Listener {
    private final SagaGuild plugin;

    public GuildMemberActionListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会成员操作GUI
        if (!(event.getInventory().getHolder() instanceof GuildMemberActionHolder)) {
            return;
        }

        // 取消事件
        event.setCancelled(true);

        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuildMemberActionHolder holder = (GuildMemberActionHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        GuildMember targetMember = holder.getMember();

        // 检查玩家是否有权限管理公会成员
        GuildMember playerMember = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (playerMember == null || !playerMember.isElder()) {
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
            case 11: // 提升职位
                if (playerMember.isAdmin() && targetMember.getRole() != GuildMember.Role.OWNER) {
                    boolean success = plugin.getGuildManager().promoteMember(player, targetMember.getPlayerUuid());
                    if (success) {
                        // 获取更新后的成员信息
                        GuildMember updatedMember = plugin.getGuildManager().getMemberByUuid(targetMember.getPlayerUuid());
                        player.sendMessage(plugin.getConfigManager().getMessage("members.player-promoted",
                                "player", targetMember.getPlayerName(),
                                "role", updatedMember.getRole().getDisplayName()));
                        
                        // 重新打开成员操作界面，显示更新后的信息
                        plugin.getGuiManager().openGuildMemberActionGUI(player, guild, updatedMember, holder.getPage());
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("guild.action-failed"));
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission-manage-member"));
                }
                break;
            case 13: // 降级职位
                if (playerMember.isAdmin() && targetMember.getRole() != GuildMember.Role.MEMBER && targetMember.getRole() != GuildMember.Role.OWNER) {
                    boolean success = plugin.getGuildManager().demoteMember(player, targetMember.getPlayerUuid());
                    if (success) {
                        // 获取更新后的成员信息
                        GuildMember updatedMember = plugin.getGuildManager().getMemberByUuid(targetMember.getPlayerUuid());
                        player.sendMessage(plugin.getConfigManager().getMessage("members.player-demoted",
                                "player", targetMember.getPlayerName(),
                                "role", updatedMember.getRole().getDisplayName()));
                        
                        // 重新打开成员操作界面，显示更新后的信息
                        plugin.getGuiManager().openGuildMemberActionGUI(player, guild, updatedMember, holder.getPage());
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("guild.action-failed"));
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission-manage-member"));
                }
                break;
            case 15: // 踢出公会
                if (playerMember.canKick(targetMember.getRole())) {
                    boolean success = plugin.getGuildManager().kickMember(player, targetMember.getPlayerUuid());
                    if (success) {
                        player.sendMessage(plugin.getConfigManager().getMessage("members.player-kicked",
                                "player", targetMember.getPlayerName()));
                        // 踢出成功后返回成员列表
                        plugin.getGuiManager().openGuildMemberGUI(player, guild, holder.getPage());
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("guild.action-failed"));
                    }
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission-manage-member"));
                }
                break;
            case 31: // 转让会长
                if (playerMember.isOwner()) {
                    // 关闭GUI并启动确认流程
                    GUIUtils.closeGUI(player);
                    player.sendMessage("§c§l警告: 你确定要将公会会长转让给 §f" + targetMember.getPlayerName() + " §c§l吗？");
                    player.sendMessage("§c此操作将使你失去公会的完全控制权！");
                    player.sendMessage("§e如果确认转让，请在30秒内输入: §fconfirm §e或 §f确认");
                    player.sendMessage("§7输入其他任何内容或等待超时将取消转让。");
                    
                    // 设置确认输入状态
                    Object[] confirmData = new Object[]{targetMember, holder.getPage()};
                    plugin.getPlayerInputListener().setPlayerInputState(player, 
                        cn.i7mc.sagaguild.listeners.PlayerInputListener.InputType.OWNERSHIP_TRANSFER_CONFIRM, 
                        guild, confirmData);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.owner-only"));
                }
                break;
            case 27: // 返回
                plugin.getGuiManager().openGuildMemberGUI(player, guild, holder.getPage());
                break;
        }
    }
}
