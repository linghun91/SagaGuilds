package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildMember.Role;
import cn.i7mc.sagaguild.gui.holders.GuildMemberHolder;
import cn.i7mc.sagaguild.utils.GUIUtils;
import cn.i7mc.sagaguild.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

/**
 * 公会成员管理监听器
 * 处理公会成员管理GUI的点击事件
 */
public class GuildMemberListener implements Listener {
    private final SagaGuild plugin;

    public GuildMemberListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会成员管理GUI
        if (!(event.getInventory().getHolder() instanceof GuildMemberHolder)) {
            return;
        }

        // 取消事件
        event.setCancelled(true);

        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuildMemberHolder holder = (GuildMemberHolder) event.getInventory().getHolder();
        Guild guild = holder.getGuild();
        int page = holder.getPage();

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
        if (event.getSlot() < 45) {
            // 点击成员物品
            if (clickedItem.getType() == Material.PLAYER_HEAD) {
                handleMemberItemClick(player, clickedItem, guild, playerMember);
            }
        } else if (event.getSlot() == 45 && clickedItem.getType() == Material.ARROW) {
            // 点击上一页按钮
            plugin.getGuiManager().openGuildMemberGUI(player, guild, page - 1);
        } else if (event.getSlot() == 53 && clickedItem.getType() == Material.ARROW) {
            // 点击下一页按钮
            plugin.getGuiManager().openGuildMemberGUI(player, guild, page + 1);
        } else if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            // 点击返回按钮
            plugin.getGuiManager().openGuildManageGUI(player, guild);
        }
    }

    /**
     * 处理成员物品点击
     * @param player 玩家
     * @param item 物品
     * @param guild 公会
     * @param playerMember 玩家的成员信息
     */
    private void handleMemberItemClick(Player player, ItemStack item, Guild guild, GuildMember playerMember) {
        // 获取成员UUID
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta)) {
            return;
        }

        // 从物品lore中获取UUID
        List<String> lore = ItemUtil.getLore(meta);
        if (lore.isEmpty()) {
            return;
        }

        String uuidLine = null;
        for (String line : lore) {
            if (line.contains("UUID:")) {
                uuidLine = line;
                break;
            }
        }

        if (uuidLine == null) {
            return;
        }

        // 解析UUID
        // 从字符串中提取UUID格式的部分
        String uuidStr = null;
        // 使用正则表达式匹配UUID格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(uuidLine);
        if (matcher.find()) {
            uuidStr = matcher.group();
        } else {
            return;
        }

        UUID memberUuid;
        try {
            memberUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        // 获取成员信息
        GuildMember targetMember = plugin.getGuildManager().getMemberByUuid(memberUuid);
        if (targetMember == null) {
            return;
        }

        // 检查是否是自己
        if (playerMember.getPlayerUuid().equals(targetMember.getPlayerUuid())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.cannot-manage-self"));
            return;
        }

        // 检查是否有权限管理该成员
        if (!canManageMember(playerMember, targetMember)) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission-manage-member"));
            return;
        }

        // 打开成员管理菜单
        GUIUtils.closeGUI(player);
        plugin.getGuiManager().openGuildMemberActionGUI(player, guild, targetMember);
    }

    /**
     * 检查是否有权限管理指定成员
     * @param manager 管理者
     * @param target 目标成员
     * @return 是否有权限管理
     */
    private boolean canManageMember(GuildMember manager, GuildMember target) {
        // 获取角色
        Role managerRole = manager.getRole();
        Role targetRole = target.getRole();

        // 不能管理会长
        if (targetRole == Role.OWNER) {
            return false;
        }

        // 只有会长可以管理副会长
        if (targetRole == Role.ADMIN) {
            return managerRole == Role.OWNER;
        }

        // 会长和副会长可以管理长老
        if (targetRole == Role.ELDER) {
            return managerRole == Role.OWNER || managerRole == Role.ADMIN;
        }

        // 会长、副会长和长老可以管理普通成员
        return managerRole == Role.OWNER || managerRole == Role.ADMIN || managerRole == Role.ELDER;
    }
}
