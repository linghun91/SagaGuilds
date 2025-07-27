package cn.i7mc.sagaguild.gui.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildWar;
import cn.i7mc.sagaguild.gui.holders.GuildListHolder;
import cn.i7mc.sagaguild.utils.ItemUtil;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 公会列表监听器
 * 处理公会列表GUI的点击事件
 */
public class GuildListListener implements Listener {
    private final SagaGuild plugin;

    public GuildListListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 物品栏点击事件
     * @param event 事件对象
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 检查是否是公会列表GUI
        if (!(event.getInventory().getHolder() instanceof GuildListHolder)) {
            return;
        }

        // 取消事件
        event.setCancelled(true);

        // 检查是否是玩家
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuildListHolder holder = (GuildListHolder) event.getInventory().getHolder();
        int page = holder.getPage();

        // 获取点击的物品
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 处理点击事件
        if (event.getSlot() < 45) {
            // 点击公会物品
            if (event.getClick() == ClickType.RIGHT) {
                // 右键点击 - 根据玩家状态处理
                Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
                if (playerGuild == null) {
                    // 没有公会的玩家 - 申请加入公会
                    handleGuildJoinClick(player, clickedItem);
                } else {
                    // 有公会的玩家 - 进入关系设置
                    handleGuildRelationClick(player, clickedItem);
                }
            } else {
                // 左键点击 - 查看公会信息
                handleGuildItemClick(player, clickedItem);
            }
        } else if (event.getSlot() == 45 && clickedItem.getType() == Material.ARROW) {
            // 点击上一页按钮
            plugin.getGuiManager().openGuildListGUI(player, page - 1);
        } else if (event.getSlot() == 53 && clickedItem.getType() == Material.ARROW) {
            // 点击下一页按钮
            plugin.getGuiManager().openGuildListGUI(player, page + 1);
        } else if (event.getSlot() == 49 && clickedItem.getType() == Material.BARRIER) {
            // 点击返回按钮
            GUIUtils.closeGUI(player);
        }
    }

    /**
     * 处理公会物品点击
     * @param player 玩家
     * @param item 物品
     */
    private void handleGuildItemClick(Player player, ItemStack item) {
        // 获取公会名称
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // 使用ItemUtil获取显示名称
        String displayName = ItemUtil.getDisplayName(meta);
        if (displayName.isEmpty()) {
            return;
        }

        // 从组件中提取公会名称
        // 格式通常是: "§b公会名称 §8[§7标签§8]"
        if (!displayName.contains("§8[")) {
            return;
        }

        String guildName = displayName.substring(displayName.indexOf("§b") + 2, displayName.indexOf(" §8["));

        // 获取公会
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        if (guild == null) {
            return;
        }

        // 显示公会信息并提供选项
        GUIUtils.closeGUI(player);

        // 检查玩家是否已经在公会中
        Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (playerGuild != null) {
            // 如果玩家已经在公会中
            player.performCommand("guild info " + guild.getName());

            // 检查是否是自己的公会
            if (playerGuild.getId() == guild.getId()) {
                // 如果是自己的公会，不显示额外选项
                return;
            }

            // 检查玩家是否有权限管理公会关系
            GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
            if (member != null && (member.isOwner() || member.isAdmin())) {
                // 检查当前关系状态
                boolean isAllied = plugin.getAllianceManager().areGuildsAllied(playerGuild.getId(), guild.getId());

                // 检查是否处于战争状态
                boolean isAtWar = false;
                GuildWar war = plugin.getWarManager().getActiveWar(playerGuild.getId());
                if (war != null && (war.getAttackerId() == guild.getId() || war.getDefenderId() == guild.getId())) {
                    isAtWar = true;
                }

                // 发送关系管理提示
                net.kyori.adventure.text.Component relationMessage = net.kyori.adventure.text.Component.text("点击 ")
                    .append(net.kyori.adventure.text.Component.text("[管理关系]")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guild relation " + guild.getName()))
                        .hoverEvent(net.kyori.adventure.text.Component.text("管理与 " + guild.getName() + " 的关系")))
                    .append(net.kyori.adventure.text.Component.text(" 或右键点击公会图标进入关系设置"));

                player.sendMessage(relationMessage);

                // 显示当前关系状态
                if (isAllied) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("当前状态: ")
                        .append(net.kyori.adventure.text.Component.text("已结盟")
                            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)));
                } else if (isAtWar) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("当前状态: ")
                        .append(net.kyori.adventure.text.Component.text("交战中")
                            .color(net.kyori.adventure.text.format.NamedTextColor.RED)));
                } else {
                    player.sendMessage(net.kyori.adventure.text.Component.text("当前状态: ")
                        .append(net.kyori.adventure.text.Component.text("中立")
                            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
                }
            }
        } else {
            // 如果玩家不在公会中，显示信息并提供加入选项
            player.performCommand("guild info " + guild.getName());

            // 检查公会是否公开
            if (guild.isPublic()) {
                // 发送加入提示
                net.kyori.adventure.text.Component joinMessage = net.kyori.adventure.text.Component.text("点击 ")
                    .append(net.kyori.adventure.text.Component.text("[申请加入公会]")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/guild join " + guild.getName()))
                        .hoverEvent(net.kyori.adventure.text.Component.text("点击申请加入 " + guild.getName() + " 公会")))
                    .append(net.kyori.adventure.text.Component.text(" 申请加入该公会"));

                player.sendMessage(joinMessage);
            } else {
                // 如果公会不是公开的，提示需要邀请
                player.sendMessage(plugin.getConfigManager().getMessage("guild.need-invitation"));
            }
        }
    }

    /**
     * 处理公会加入点击
     * @param player 玩家
     * @param item 物品
     */
    private void handleGuildJoinClick(Player player, ItemStack item) {
        // 获取公会名称
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // 使用ItemUtil获取显示名称
        String displayName = ItemUtil.getDisplayName(meta);
        if (displayName.isEmpty()) {
            return;
        }

        // 从组件中提取公会名称
        // 格式通常是: "§b公会名称 §8[§7标签§8]"
        if (!displayName.contains("§8[")) {
            return;
        }

        String guildName = displayName.substring(displayName.indexOf("§b") + 2, displayName.indexOf(" §8["));

        // 获取公会
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        if (guild == null) {
            return;
        }

        // 关闭物品栏
        GUIUtils.closeGUI(player);

        // 检查玩家是否已经在公会中
        if (plugin.getGuildManager().getPlayerGuild(player.getUniqueId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.already-in-guild"));
            return;
        }

        // 检查公会是否公开
        if (!guild.isPublic()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.need-invitation"));
            return;
        }

        // 发送加入申请
        boolean success = plugin.getGuildManager().requestJoinGuild(player, guild.getId());
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.join-request-failed"));
        }
        // 注意：成功消息已经在GuildManager.requestJoinGuild方法中发送，这里不需要重复发送
    }

    /**
     * 处理公会关系点击
     * @param player 玩家
     * @param item 物品
     */
    private void handleGuildRelationClick(Player player, ItemStack item) {
        // 获取公会名称
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // 使用ItemUtil获取显示名称
        String displayName = ItemUtil.getDisplayName(meta);
        if (displayName.isEmpty()) {
            return;
        }

        // 从组件中提取公会名称
        // 格式通常是: "§b公会名称 §8[§7标签§8]"
        if (!displayName.contains("§8[")) {
            return;
        }

        String guildName = displayName.substring(displayName.indexOf("§b") + 2, displayName.indexOf(" §8["));

        // 获取公会
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        if (guild == null) {
            return;
        }

        // 获取玩家所在公会
        Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (playerGuild == null) {
            GUIUtils.closeGUI(player);
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return;
        }

        // 检查是否是自己的公会
        if (playerGuild.getId() == guild.getId()) {
            GUIUtils.closeGUI(player);
            player.sendMessage(plugin.getConfigManager().getMessage("guild.cannot-set-relation-self"));
            return;
        }

        // 检查玩家是否有权限管理公会关系
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || (!member.isOwner() && !member.isAdmin())) {
            GUIUtils.closeGUI(player);
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 打开公会关系设置GUI
        GUIUtils.closeGUI(player);
        plugin.getGuiManager().openGuildRelationGUI(player, guild);
    }
}
