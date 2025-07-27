package cn.i7mc.sagaguild.gui;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.AllianceRequest;
import cn.i7mc.sagaguild.data.models.CeasefireRequest;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildWar;
import cn.i7mc.sagaguild.data.models.JoinRequest;
import cn.i7mc.sagaguild.gui.holders.GuildListHolder;
import cn.i7mc.sagaguild.gui.holders.GuildManageHolder;
import cn.i7mc.sagaguild.gui.holders.GuildMemberHolder;
import cn.i7mc.sagaguild.gui.holders.GuildSettingsHolder;
import cn.i7mc.sagaguild.gui.holders.GuildMemberActionHolder;
import cn.i7mc.sagaguild.gui.holders.GuildRelationHolder;
import cn.i7mc.sagaguild.gui.holders.GuildRelationManageHolder;
import cn.i7mc.sagaguild.gui.holders.JoinRequestHolder;
import cn.i7mc.sagaguild.gui.holders.GuildLandSettingsHolder;
import cn.i7mc.sagaguild.gui.holders.GuildChatSettingsHolder;
import cn.i7mc.sagaguild.gui.listeners.GuildListListener;
import cn.i7mc.sagaguild.gui.listeners.GuildManageListener;
import cn.i7mc.sagaguild.gui.listeners.GuildMemberListener;
import cn.i7mc.sagaguild.gui.listeners.GuildSettingsListener;
import cn.i7mc.sagaguild.gui.listeners.GuildMemberActionListener;
import cn.i7mc.sagaguild.gui.listeners.GuildRelationListener;
import cn.i7mc.sagaguild.gui.listeners.GuildRelationManageListener;
import cn.i7mc.sagaguild.gui.listeners.JoinRequestListener;
import cn.i7mc.sagaguild.gui.listeners.GuildLandSettingsListener;
import cn.i7mc.sagaguild.gui.listeners.GuildChatSettingsListener;
import cn.i7mc.sagaguild.utils.InventoryUtil;
import cn.i7mc.sagaguild.utils.ItemUtil;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI管理器
 * 负责创建和管理GUI界面
 */
public class GUIManager {
    private final SagaGuild plugin;

    public GUIManager(SagaGuild plugin) {
        this.plugin = plugin;

        // 注册GUI监听器
        plugin.getServer().getPluginManager().registerEvents(new GuildListListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildManageListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildMemberListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildSettingsListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildMemberActionListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildRelationListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildRelationManageListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new JoinRequestListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildLandSettingsListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildChatSettingsListener(plugin), plugin);
    }

    /**
     * 打开公会列表GUI
     * @param player 玩家
     * @param page 页码
     */
    public void openGuildListGUI(Player player, int page) {
        try {
            // 获取所有公会
            List<Guild> guilds = plugin.getGuildManager().getAllGuilds();

            if (guilds == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.list-failed"));
                return;
            }

            // 计算总页数
            int totalGuilds = guilds.size();
            int guildsPerPage = 45; // 9x5
            int totalPages = (int) Math.ceil((double) totalGuilds / guildsPerPage);

            // 检查页码是否有效
            if (page < 1) {
                page = 1;
            } else if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }

            // 创建物品栏
            String title = plugin.getConfigManager().getMessage("gui.guild-list-title", "page", String.valueOf(page));
            Inventory inventory = InventoryUtil.createInventory(new GuildListHolder(page), 54, Component.text(title));

            // 填充公会物品
            int startIndex = (page - 1) * guildsPerPage;
            int endIndex = Math.min(startIndex + guildsPerPage, totalGuilds);

            for (int i = startIndex; i < endIndex; i++) {
                Guild guild = guilds.get(i);
                ItemStack item = createGuildItem(guild);
                inventory.setItem(i - startIndex, item);
            }

            // 添加导航按钮
            if (page > 1) {
                // 上一页按钮
                ItemStack prevButton = new ItemStack(Material.ARROW);
                ItemMeta meta = prevButton.getItemMeta();
                ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.previous-page")));
                prevButton.setItemMeta(meta);
                inventory.setItem(45, prevButton);
            }

            if (page < totalPages) {
                // 下一页按钮
                ItemStack nextButton = new ItemStack(Material.ARROW);
                ItemMeta meta = nextButton.getItemMeta();
                ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.next-page")));
                nextButton.setItemMeta(meta);
                inventory.setItem(53, nextButton);
            }

            // 返回按钮
            ItemStack backButton = new ItemStack(Material.BARRIER);
            ItemMeta meta = backButton.getItemMeta();
            ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
            backButton.setItemMeta(meta);
            inventory.setItem(49, backButton);

            // 打开GUI
            player.openInventory(inventory);
        } catch (Exception e) {
            // 记录错误
            plugin.getLogger().severe("打开公会列表GUI失败: " + e.getMessage());
            e.printStackTrace();

            // 通知玩家
            player.sendMessage(plugin.getConfigManager().getMessage("guild.list-failed"));
        }
    }

    /**
     * 打开公会管理GUI
     * @param player 玩家
     * @param guild 公会对象
     */
    public void openGuildManageGUI(Player player, Guild guild) {
        // 检查玩家是否有权限管理公会
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-manage-title", "name", guild.getName());
        Inventory inventory = InventoryUtil.createInventory(new GuildManageHolder(guild), 54, Component.text(title));

        // 基本信息管理
        ItemStack infoItem = new ItemStack(Material.NAME_TAG);
        ItemMeta infoMeta = infoItem.getItemMeta();
        ItemUtil.setDisplayName(infoMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-info-manage")));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("§7管理公会的基本信息"));
        infoLore.add(Component.text("§7包括名称、标签、描述等"));
        infoLore.add(Component.text(""));
        infoLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(infoMeta, infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(10, infoItem);

        // 成员管理
        ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta memberMeta = memberItem.getItemMeta();
        ItemUtil.setDisplayName(memberMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-member-manage")));
        List<Component> memberLore = new ArrayList<>();
        memberLore.add(Component.text("§7管理公会成员"));
        memberLore.add(Component.text("§7包括提升、降级、踢出等"));
        memberLore.add(Component.text(""));
        memberLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(memberMeta, memberLore);
        memberItem.setItemMeta(memberMeta);
        inventory.setItem(12, memberItem);

        // 银行管理
        ItemStack bankItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bankMeta = bankItem.getItemMeta();
        ItemUtil.setDisplayName(bankMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-bank-manage")));
        List<Component> bankLore = new ArrayList<>();
        bankLore.add(Component.text("§7管理公会银行"));
        bankLore.add(Component.text("§7包括存款、取款等"));
        bankLore.add(Component.text(""));
        bankLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(bankMeta, bankLore);
        bankItem.setItemMeta(bankMeta);
        inventory.setItem(14, bankItem);

        // 领地管理
        ItemStack landItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta landMeta = landItem.getItemMeta();
        ItemUtil.setDisplayName(landMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-land-manage")));
        List<Component> landLore = new ArrayList<>();
        landLore.add(Component.text("§7管理公会领地"));
        landLore.add(Component.text("§7包括声明、放弃等"));
        landLore.add(Component.text(""));
        landLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(landMeta, landLore);
        landItem.setItemMeta(landMeta);
        inventory.setItem(16, landItem);

        // 活动管理
        ItemStack activityItem = new ItemStack(Material.CLOCK);
        ItemMeta activityMeta = activityItem.getItemMeta();
        ItemUtil.setDisplayName(activityMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-activity-manage")));
        List<Component> activityLore = new ArrayList<>();
        activityLore.add(Component.text("§7管理公会活动"));
        activityLore.add(Component.text("§7包括创建、取消等"));
        activityLore.add(Component.text(""));
        activityLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(activityMeta, activityLore);
        activityItem.setItemMeta(activityMeta);
        inventory.setItem(28, activityItem);

        // 任务管理
        ItemStack taskItem = new ItemStack(Material.BOOK);
        ItemMeta taskMeta = taskItem.getItemMeta();
        ItemUtil.setDisplayName(taskMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-task-manage")));
        List<Component> taskLore = new ArrayList<>();
        taskLore.add(Component.text("§7管理公会任务"));
        taskLore.add(Component.text("§7包括接受、放弃等"));
        taskLore.add(Component.text(""));
        taskLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(taskMeta, taskLore);
        taskItem.setItemMeta(taskMeta);
        inventory.setItem(30, taskItem);

        // 公会关系管理
        ItemStack relationItem = new ItemStack(Material.SHIELD);
        ItemMeta relationMeta = relationItem.getItemMeta();
        ItemUtil.setDisplayName(relationMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-relation-manage")));
        List<Component> relationLore = new ArrayList<>();
        relationLore.add(Component.text("§7管理公会的外交关系"));
        relationLore.add(Component.text("§7包括联盟、战争等"));
        relationLore.add(Component.text(""));
        relationLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(relationMeta, relationLore);
        relationItem.setItemMeta(relationMeta);
        inventory.setItem(32, relationItem);

        // 加入申请管理
        ItemStack joinRequestItem = new ItemStack(Material.PAPER);
        ItemMeta joinRequestMeta = joinRequestItem.getItemMeta();
        ItemUtil.setDisplayName(joinRequestMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-join-request-manage")));
        List<Component> joinRequestLore = new ArrayList<>();
        joinRequestLore.add(Component.text("§7管理公会加入申请"));
        joinRequestLore.add(Component.text("§7审核玩家的加入请求"));
        joinRequestLore.add(Component.text(""));
        joinRequestLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(joinRequestMeta, joinRequestLore);
        joinRequestItem.setItemMeta(joinRequestMeta);
        inventory.setItem(33, joinRequestItem);

        // 公会战管理
        ItemStack warItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta warMeta = warItem.getItemMeta();
        ItemUtil.setDisplayName(warMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-war-manage")));
        List<Component> warLore = new ArrayList<>();
        warLore.add(Component.text("§7管理公会战"));
        warLore.add(Component.text("§7包括发起、接受等"));
        warLore.add(Component.text(""));
        warLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(warMeta, warLore);
        warItem.setItemMeta(warMeta);
        inventory.setItem(34, warItem);

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 打开公会成员管理GUI
     * @param player 玩家
     * @param guild 公会对象
     * @param page 页码
     */
    public void openGuildMemberGUI(Player player, Guild guild, int page) {
        // 检查玩家是否有权限管理公会成员
        GuildMember playerMember = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (playerMember == null || !playerMember.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 获取公会成员列表
        List<GuildMember> members = plugin.getGuildManager().getGuildMembers(guild.getId());

        // 计算总页数
        int totalMembers = members.size();
        int membersPerPage = 45; // 9x5
        int totalPages = (int) Math.ceil((double) totalMembers / membersPerPage);

        // 检查页码是否有效
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-member-title", "name", guild.getName(), "page", String.valueOf(page));
        Inventory inventory = InventoryUtil.createInventory(new GuildMemberHolder(guild, page), 54, Component.text(title));

        // 填充成员物品
        int startIndex = (page - 1) * membersPerPage;
        int endIndex = Math.min(startIndex + membersPerPage, totalMembers);

        for (int i = startIndex; i < endIndex; i++) {
            GuildMember member = members.get(i);
            ItemStack item = createMemberItem(member);
            inventory.setItem(i - startIndex, item);
        }

        // 添加导航按钮
        if (page > 1) {
            // 上一页按钮
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.previous-page")));
            prevButton.setItemMeta(meta);
            inventory.setItem(45, prevButton);
        }

        if (page < totalPages) {
            // 下一页按钮
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.next-page")));
            nextButton.setItemMeta(meta);
            inventory.setItem(53, nextButton);
        }

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(meta);
        inventory.setItem(49, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 打开加入请求管理GUI
     * @param player 玩家
     * @param guild 公会对象
     * @param page 页码
     */
    public void openJoinRequestGUI(Player player, Guild guild, int page) {
        // 检查玩家是否有权限管理公会
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder() || member.getGuildId() != guild.getId()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 获取公会的加入请求
        List<JoinRequest> requests = plugin.getGuildManager().getGuildJoinRequests(guild.getId());

        // 计算总页数
        int totalRequests = requests.size();
        int requestsPerPage = 45; // 9x5
        int totalPages = (int) Math.ceil((double) totalRequests / requestsPerPage);

        // 检查页码是否有效
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-join-request-title", "name", guild.getName(), "page", String.valueOf(page));
        Inventory inventory = InventoryUtil.createInventory(new JoinRequestHolder(guild, page), 54, Component.text(title));

        // 填充请求物品
        int startIndex = (page - 1) * requestsPerPage;
        int endIndex = Math.min(startIndex + requestsPerPage, totalRequests);

        for (int i = startIndex; i < endIndex; i++) {
            JoinRequest request = requests.get(i);
            ItemStack item = createJoinRequestItem(request);
            inventory.setItem(i - startIndex, item);
        }

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(meta);
        inventory.setItem(49, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 创建加入请求物品
     * @param request 请求对象
     * @return 物品堆
     */
    private ItemStack createJoinRequestItem(JoinRequest request) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // 设置玩家头颅
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(request.getPlayerUuid()));

        // 设置显示名称
        ItemUtil.setDisplayName(meta, Component.text("§e" + request.getPlayerName() + " §7的加入申请"));

        // 设置描述
        List<Component> lore = new ArrayList<>();

        // 添加申请时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lore.add(Component.text("§7申请时间: §f" + sdf.format(request.getRequestedAt())));

        // 添加操作提示
        lore.add(Component.text(""));
        lore.add(Component.text("§a左键接受申请"));
        lore.add(Component.text("§c右键拒绝申请"));

        // 添加请求ID（用于处理点击事件）
        lore.add(Component.text("§8ID: " + request.getId()));

        ItemUtil.setLore(meta, lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 打开公会设置GUI
     * @param player 玩家
     * @param guild 公会对象
     */
    public void openGuildSettingsGUI(Player player, Guild guild) {
        // 检查玩家是否是公会会长
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isOwner()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.owner-only"));
            return;
        }

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-settings-title", "name", guild.getName());
        Inventory inventory = InventoryUtil.createInventory(new GuildSettingsHolder(guild), 54, Component.text(title));

        // 公会公开性设置
        ItemStack publicItem = new ItemStack(guild.isPublic() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta publicMeta = publicItem.getItemMeta();
        ItemUtil.setDisplayName(publicMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-public-setting")));
        List<Component> publicLore = new ArrayList<>();
        publicLore.add(Component.text("§7当前状态: §f" + (guild.isPublic() ? "公开" : "私有")));
        publicLore.add(Component.text(""));
        publicLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(publicMeta, publicLore);
        publicItem.setItemMeta(publicMeta);
        inventory.setItem(10, publicItem);

        // 公会描述设置
        ItemStack descItem = new ItemStack(Material.PAPER);
        ItemMeta descMeta = descItem.getItemMeta();
        ItemUtil.setDisplayName(descMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-description-setting")));
        List<Component> descLore = new ArrayList<>();
        descLore.add(Component.text("§7当前描述: §f" + guild.getDescription()));
        descLore.add(Component.text(""));
        descLore.add(Component.text("§e点击修改"));
        ItemUtil.setLore(descMeta, descLore);
        descItem.setItemMeta(descMeta);
        inventory.setItem(12, descItem);

        // 公会公告设置
        ItemStack announcementItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta announcementMeta = announcementItem.getItemMeta();
        ItemUtil.setDisplayName(announcementMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-announcement-setting")));
        List<Component> announcementLore = new ArrayList<>();
        announcementLore.add(Component.text("§7当前公告: §f" + guild.getAnnouncement()));
        announcementLore.add(Component.text(""));
        announcementLore.add(Component.text("§e点击修改"));
        ItemUtil.setLore(announcementMeta, announcementLore);
        announcementItem.setItemMeta(announcementMeta);
        inventory.setItem(14, announcementItem);

        // 公会标签颜色设置
        ItemStack tagColorItem = new ItemStack(Material.CYAN_DYE);
        ItemMeta tagColorMeta = tagColorItem.getItemMeta();
        ItemUtil.setDisplayName(tagColorMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-tag-color-setting")));
        List<Component> tagColorLore = new ArrayList<>();
        tagColorLore.add(Component.text("§7当前颜色: §" + guild.getTagColor() + guild.getTag()));
        tagColorLore.add(Component.text(""));
        tagColorLore.add(Component.text("§e点击修改"));
        ItemUtil.setLore(tagColorMeta, tagColorLore);
        tagColorItem.setItemMeta(tagColorMeta);
        inventory.setItem(16, tagColorItem);

        // 领地相关设置
        ItemStack landSettingsItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta landSettingsMeta = landSettingsItem.getItemMeta();
        ItemUtil.setDisplayName(landSettingsMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-land-settings")));
        List<Component> landSettingsLore = new ArrayList<>();
        landSettingsLore.add(Component.text("§7管理公会领地设置"));
        landSettingsLore.add(Component.text(""));
        landSettingsLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(landSettingsMeta, landSettingsLore);
        landSettingsItem.setItemMeta(landSettingsMeta);
        inventory.setItem(28, landSettingsItem);

        // 聊天设置
        ItemStack chatSettingsItem = new ItemStack(Material.PAPER);
        ItemMeta chatSettingsMeta = chatSettingsItem.getItemMeta();
        ItemUtil.setDisplayName(chatSettingsMeta, Component.text(plugin.getConfigManager().getMessage("gui.guild-chat-settings")));
        List<Component> chatSettingsLore = new ArrayList<>();
        chatSettingsLore.add(Component.text("§7管理公会聊天设置"));
        chatSettingsLore.add(Component.text(""));
        chatSettingsLore.add(Component.text("§e点击管理"));
        ItemUtil.setLore(chatSettingsMeta, chatSettingsLore);
        chatSettingsItem.setItemMeta(chatSettingsMeta);
        inventory.setItem(30, chatSettingsItem);

        // 解散公会按钮（仅会长可见）
        ItemStack disbandItem = new ItemStack(Material.TNT);
        ItemMeta disbandMeta = disbandItem.getItemMeta();
        ItemUtil.setDisplayName(disbandMeta, Component.text("§c解散公会"));
        List<Component> disbandLore = new ArrayList<>();
        disbandLore.add(Component.text("§7解散当前公会"));
        disbandLore.add(Component.text("§7§l警告: 此操作不可逆！"));
        disbandLore.add(Component.text("§7将删除公会的所有数据"));
        disbandLore.add(Component.text("§7包括成员、领地、联盟等"));
        disbandLore.add(Component.text(""));
        disbandLore.add(Component.text("§c点击解散"));
        ItemUtil.setLore(disbandMeta, disbandLore);
        disbandItem.setItemMeta(disbandMeta);
        inventory.setItem(40, disbandItem);

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 创建公会物品
     * @param guild 公会对象
     * @return 物品堆
     */
    private ItemStack createGuildItem(Guild guild) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();

        // 设置显示名称
        ItemUtil.setDisplayName(meta, Component.text("§b" + guild.getName() + " §8[§7" + guild.getTag() + "§8]"));

        // 设置描述
        List<Component> lore = new ArrayList<>();

        // 添加描述
        if (!guild.getDescription().isEmpty()) {
            lore.add(Component.text("§7描述: §f" + guild.getDescription()));
        }

        // 添加会长
        String ownerName = Bukkit.getOfflinePlayer(guild.getOwnerUuid()).getName();
        if (ownerName != null) {
            lore.add(Component.text("§7会长: §f" + ownerName));
        }

        // 添加成员数量
        int memberCount = plugin.getGuildManager().getGuildMemberCount(guild.getId());
        int maxMembers = guild.getMaxMembers(plugin.getGuildManager());
        lore.add(Component.text("§7成员: §f" + memberCount + "/" + maxMembers));

        // 添加等级
        lore.add(Component.text("§7等级: §f" + guild.getLevel()));

        // 添加是否公开
        lore.add(Component.text("§7公开: §f" + (guild.isPublic() ? "是" : "否")));

        // 添加联盟关系
        List<Integer> alliances = plugin.getAllianceManager().getGuildAlliances(guild.getId());
        if (alliances != null && !alliances.isEmpty()) {
            StringBuilder allyNames = new StringBuilder();
            int count = 0;
            for (Integer allyId : alliances) {
                Guild allyGuild = plugin.getGuildManager().getGuildById(allyId);
                if (allyGuild != null) {
                    if (count > 0) {
                        allyNames.append(", ");
                    }
                    allyNames.append(allyGuild.getName());
                    count++;
                    if (count >= 3) {
                        allyNames.append("...");
                        break;
                    }
                }
            }
            lore.add(Component.text("§7联盟关系: §f" + allyNames.toString()));
        } else {
            lore.add(Component.text("§7联盟关系: §f无"));
        }

        // 添加战争状态
        GuildWar war = plugin.getWarManager().getActiveWar(guild.getId());
        if (war != null) {
            int opponentId = war.getAttackerId() == guild.getId() ? war.getDefenderId() : war.getAttackerId();
            Guild opponent = plugin.getGuildManager().getGuildById(opponentId);
            if (opponent != null) {
                lore.add(Component.text("§7战争状态: §c与 " + opponent.getName() + " 交战中"));
            } else {
                lore.add(Component.text("§7战争状态: §c交战中"));
            }
        } else {
            lore.add(Component.text("§7战争状态: §f无"));
        }



        // 添加空行
        lore.add(Component.text(""));

        if (war != null) {
            int opponentId = war.getOpponentId(guild.getId());
            Guild opponent = plugin.getGuildManager().getGuildById(opponentId);
            if (opponent != null) {
                String statusText;
                switch (war.getStatus()) {
                    case PENDING:
                        statusText = "§e等待中";
                        break;
                    case PREPARING:
                        statusText = "§6准备中";
                        break;
                    case ONGOING:
                        statusText = "§c进行中";
                        break;
                    default:
                        statusText = "§7未知";
                        break;
                }
                lore.add(Component.text("§c战争状态: " + statusText));
                lore.add(Component.text("§c对手: §7" + opponent.getName()));


            }
        } else {
            // 没有战争状态时显示"无"
            lore.add(Component.text("§c战争状态: §7无"));


        }

        // 添加联盟信息

        // 不需要再添加空行，因为战争状态部分已经添加了
        lore.add(Component.text("§a联盟关系:"));

        if (!alliances.isEmpty()) {
            int count = 0;
            for (int allyId : alliances) {
                Guild ally = plugin.getGuildManager().getGuildById(allyId);
                if (ally != null) {
                    lore.add(Component.text("§7- §f" + ally.getName()));
                    count++;

                    if (count >= 3) {
                        if (alliances.size() > 3) {
                            lore.add(Component.text("§7...以及其他 " + (alliances.size() - 3) + " 个公会"));
                        }
                        break;
                    }
                }
            }
        } else {
            // 没有联盟关系时显示"无"
            lore.add(Component.text("§7无"));


        }

        // 添加点击提示
        lore.add(Component.text(""));
        lore.add(Component.text("§e左键点击查看详细信息"));
        lore.add(Component.text("§a右键点击申请加入"));

        // 设置lore

        ItemUtil.setLore(meta, lore);
        item.setItemMeta(meta);



        return item;
    }

    /**
     * 创建公会关系管理专用物品
     * @param guild 公会对象
     * @return 物品堆
     */
    private ItemStack createRelationManageGuildItem(Guild guild) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();

        // 设置显示名称
        ItemUtil.setDisplayName(meta, Component.text("§b" + guild.getName() + " §8[§7" + guild.getTag() + "§8]"));

        // 设置描述
        List<Component> lore = new ArrayList<>();

        // 添加会长
        String ownerName = Bukkit.getOfflinePlayer(guild.getOwnerUuid()).getName();
        if (ownerName != null) {
            lore.add(Component.text("§7会长: §f" + ownerName));
        }

        // 添加成员数量
        int memberCount = plugin.getGuildManager().getGuildMemberCount(guild.getId());
        int maxMembers = guild.getMaxMembers(plugin.getGuildManager());
        lore.add(Component.text("§7成员: §f" + memberCount + "/" + maxMembers));

        // 添加等级
        lore.add(Component.text("§7等级: §f" + guild.getLevel()));

        // 添加是否公开
        lore.add(Component.text("§7公开: §f" + (guild.isPublic() ? "是" : "否")));

        // 添加联盟关系
        List<Integer> alliances = plugin.getAllianceManager().getGuildAlliances(guild.getId());
        if (alliances != null && !alliances.isEmpty()) {
            StringBuilder allyNames = new StringBuilder();
            int count = 0;
            for (Integer allyId : alliances) {
                Guild allyGuild = plugin.getGuildManager().getGuildById(allyId);
                if (allyGuild != null) {
                    if (count > 0) {
                        allyNames.append(", ");
                    }
                    allyNames.append(allyGuild.getName());
                    count++;
                    if (count >= 3) {
                        allyNames.append("...");
                        break;
                    }
                }
            }
            lore.add(Component.text("§7联盟关系: §f" + allyNames.toString()));
        } else {
            lore.add(Component.text("§7联盟关系: §f无"));
        }

        // 添加战争状态
        GuildWar war = plugin.getWarManager().getActiveWar(guild.getId());
        if (war != null) {
            int opponentId = war.getAttackerId() == guild.getId() ? war.getDefenderId() : war.getAttackerId();
            Guild opponent = plugin.getGuildManager().getGuildById(opponentId);
            if (opponent != null) {
                lore.add(Component.text("§7战争状态: §c与 " + opponent.getName() + " 交战中"));
            } else {
                lore.add(Component.text("§7战争状态: §c交战中"));
            }
        } else {
            lore.add(Component.text("§7战争状态: §f无"));
        }

        // 设置lore（不添加点击提示，因为这是关系管理GUI专用）
        ItemUtil.setLore(meta, lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 打开公会成员操作GUI
     * @param player 玩家
     * @param guild 公会对象
     * @param targetMember 目标成员
     */
    public void openGuildMemberActionGUI(Player player, Guild guild, GuildMember targetMember) {
        // 检查玩家是否有权限管理公会成员
        GuildMember playerMember = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (playerMember == null || !playerMember.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
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

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-member-action-title", "name", targetMember.getPlayerName());
        Inventory inventory = InventoryUtil.createInventory(new GuildMemberActionHolder(guild, targetMember, 1), 36, Component.text(title));

        // 成员信息
        ItemStack infoItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta infoMeta = (SkullMeta) infoItem.getItemMeta();
        infoMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetMember.getPlayerUuid()));
        ItemUtil.setDisplayName(infoMeta, Component.text("§b" + targetMember.getPlayerName()));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("§7职位: §f" + targetMember.getRole().getDisplayName()));
        infoLore.add(Component.text("§7加入时间: §f" + targetMember.getJoinedAt()));
        ItemUtil.setLore(infoMeta, infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);

        // 提升职位
        if (playerMember.isAdmin() && targetMember.getRole() != GuildMember.Role.OWNER) {
            ItemStack promoteItem = new ItemStack(Material.EMERALD);
            ItemMeta promoteMeta = promoteItem.getItemMeta();
            ItemUtil.setDisplayName(promoteMeta, Component.text("§a提升职位"));
            List<Component> promoteLore = new ArrayList<>();

            GuildMember.Role newRole;
            switch (targetMember.getRole()) {
                case MEMBER:
                    newRole = GuildMember.Role.ELDER;
                    break;
                case ELDER:
                    newRole = GuildMember.Role.ADMIN;
                    break;
                default:
                    newRole = null;
                    break;
            }

            if (newRole != null) {
                promoteLore.add(Component.text("§7将 §f" + targetMember.getPlayerName() + " §7提升为 §f" + newRole.getDisplayName()));
                promoteLore.add(Component.text(""));
                promoteLore.add(Component.text("§e点击提升"));
                ItemUtil.setLore(promoteMeta, promoteLore);
                promoteItem.setItemMeta(promoteMeta);
                inventory.setItem(11, promoteItem);
            }
        }

        // 降级职位
        if (playerMember.isAdmin() && targetMember.getRole() != GuildMember.Role.MEMBER && targetMember.getRole() != GuildMember.Role.OWNER) {
            ItemStack demoteItem = new ItemStack(Material.REDSTONE);
            ItemMeta demoteMeta = demoteItem.getItemMeta();
            ItemUtil.setDisplayName(demoteMeta, Component.text("§c降级职位"));
            List<Component> demoteLore = new ArrayList<>();

            GuildMember.Role newRole;
            switch (targetMember.getRole()) {
                case ADMIN:
                    newRole = GuildMember.Role.ELDER;
                    break;
                case ELDER:
                    newRole = GuildMember.Role.MEMBER;
                    break;
                default:
                    newRole = null;
                    break;
            }

            if (newRole != null) {
                demoteLore.add(Component.text("§7将 §f" + targetMember.getPlayerName() + " §7降级为 §f" + newRole.getDisplayName()));
                demoteLore.add(Component.text(""));
                demoteLore.add(Component.text("§e点击降级"));
                ItemUtil.setLore(demoteMeta, demoteLore);
                demoteItem.setItemMeta(demoteMeta);
                inventory.setItem(13, demoteItem);
            }
        }

        // 踢出公会
        if (playerMember.canKick(targetMember.getRole())) {
            ItemStack kickItem = new ItemStack(Material.BARRIER);
            ItemMeta kickMeta = kickItem.getItemMeta();
            ItemUtil.setDisplayName(kickMeta, Component.text("§c踢出公会"));
            List<Component> kickLore = new ArrayList<>();
            kickLore.add(Component.text("§7将 §f" + targetMember.getPlayerName() + " §7踢出公会"));
            kickLore.add(Component.text(""));
            kickLore.add(Component.text("§c点击踢出"));
            ItemUtil.setLore(kickMeta, kickLore);
            kickItem.setItemMeta(kickMeta);
            inventory.setItem(15, kickItem);
        }

        // 转让会长
        if (playerMember.isOwner()) {
            ItemStack transferItem = new ItemStack(Material.GOLDEN_HELMET);
            ItemMeta transferMeta = transferItem.getItemMeta();
            ItemUtil.setDisplayName(transferMeta, Component.text("§6转让会长"));
            List<Component> transferLore = new ArrayList<>();
            transferLore.add(Component.text("§7将会长职位转让给 §f" + targetMember.getPlayerName()));
            transferLore.add(Component.text(""));
            transferLore.add(Component.text("§6点击转让"));
            ItemUtil.setLore(transferMeta, transferLore);
            transferItem.setItemMeta(transferMeta);
            inventory.setItem(31, transferItem);
        }

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(27, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 打开公会成员操作GUI（带页码）
     * @param player 玩家
     * @param guild 公会对象
     * @param targetMember 目标成员
     * @param page 页码
     */
    public void openGuildMemberActionGUI(Player player, Guild guild, GuildMember targetMember, int page) {
        // 检查玩家是否有权限管理公会成员
        GuildMember playerMember = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (playerMember == null || !playerMember.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
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

        // 创建物品栏（带页码信息）
        String title = plugin.getConfigManager().getMessage("gui.guild-member-action-title", "name", targetMember.getPlayerName());
        Inventory inventory = InventoryUtil.createInventory(new GuildMemberActionHolder(guild, targetMember, page), 36, Component.text(title));

        // 成员信息
        ItemStack infoItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta infoMeta = (SkullMeta) infoItem.getItemMeta();
        infoMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetMember.getPlayerUuid()));
        ItemUtil.setDisplayName(infoMeta, Component.text("§b" + targetMember.getPlayerName()));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("§7职位: §f" + targetMember.getRole().getDisplayName()));
        infoLore.add(Component.text("§7加入时间: §f" + targetMember.getJoinedAt()));
        ItemUtil.setLore(infoMeta, infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);

        // 提升职位
        if (playerMember.isAdmin() && targetMember.getRole() != GuildMember.Role.OWNER) {
            ItemStack promoteItem = new ItemStack(Material.EMERALD);
            ItemMeta promoteMeta = promoteItem.getItemMeta();
            ItemUtil.setDisplayName(promoteMeta, Component.text("§a提升职位"));
            List<Component> promoteLore = new ArrayList<>();

            GuildMember.Role newRole;
            switch (targetMember.getRole()) {
                case MEMBER:
                    newRole = GuildMember.Role.ELDER;
                    break;
                case ELDER:
                    newRole = GuildMember.Role.ADMIN;
                    break;
                default:
                    newRole = null;
                    break;
            }

            if (newRole != null) {
                promoteLore.add(Component.text("§7将 §f" + targetMember.getPlayerName() + " §7提升为 §f" + newRole.getDisplayName()));
                promoteLore.add(Component.text(""));
                promoteLore.add(Component.text("§e点击提升"));
                ItemUtil.setLore(promoteMeta, promoteLore);
                promoteItem.setItemMeta(promoteMeta);
                inventory.setItem(11, promoteItem);
            }
        }

        // 降级职位
        if (playerMember.isAdmin() && targetMember.getRole() != GuildMember.Role.MEMBER && targetMember.getRole() != GuildMember.Role.OWNER) {
            ItemStack demoteItem = new ItemStack(Material.REDSTONE);
            ItemMeta demoteMeta = demoteItem.getItemMeta();
            ItemUtil.setDisplayName(demoteMeta, Component.text("§c降级职位"));
            List<Component> demoteLore = new ArrayList<>();

            GuildMember.Role newRole;
            switch (targetMember.getRole()) {
                case ADMIN:
                    newRole = GuildMember.Role.ELDER;
                    break;
                case ELDER:
                    newRole = GuildMember.Role.MEMBER;
                    break;
                default:
                    newRole = null;
                    break;
            }

            if (newRole != null) {
                demoteLore.add(Component.text("§7将 §f" + targetMember.getPlayerName() + " §7降级为 §f" + newRole.getDisplayName()));
                demoteLore.add(Component.text(""));
                demoteLore.add(Component.text("§e点击降级"));
                ItemUtil.setLore(demoteMeta, demoteLore);
                demoteItem.setItemMeta(demoteMeta);
                inventory.setItem(13, demoteItem);
            }
        }

        // 踢出公会
        if (playerMember.canKick(targetMember.getRole())) {
            ItemStack kickItem = new ItemStack(Material.BARRIER);
            ItemMeta kickMeta = kickItem.getItemMeta();
            ItemUtil.setDisplayName(kickMeta, Component.text("§c踢出公会"));
            List<Component> kickLore = new ArrayList<>();
            kickLore.add(Component.text("§7将 §f" + targetMember.getPlayerName() + " §7踢出公会"));
            kickLore.add(Component.text(""));
            kickLore.add(Component.text("§c点击踢出"));
            ItemUtil.setLore(kickMeta, kickLore);
            kickItem.setItemMeta(kickMeta);
            inventory.setItem(15, kickItem);
        }

        // 转让会长
        if (playerMember.isOwner()) {
            ItemStack transferItem = new ItemStack(Material.GOLDEN_HELMET);
            ItemMeta transferMeta = transferItem.getItemMeta();
            ItemUtil.setDisplayName(transferMeta, Component.text("§6转让会长"));
            List<Component> transferLore = new ArrayList<>();
            transferLore.add(Component.text("§7将会长职位转让给 §f" + targetMember.getPlayerName()));
            transferLore.add(Component.text(""));
            transferLore.add(Component.text("§6点击转让"));
            ItemUtil.setLore(transferMeta, transferLore);
            transferItem.setItemMeta(transferMeta);
            inventory.setItem(31, transferItem);
        }

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(27, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 检查是否有权限管理成员
     * @param manager 管理者
     * @param target 目标成员
     * @return 是否有权限
     */
    private boolean canManageMember(GuildMember manager, GuildMember target) {
        // 会长可以管理所有人
        if (manager.isOwner()) {
            return true;
        }

        // 副会长可以管理除会长外的所有人
        if (manager.isAdmin() && !target.isOwner()) {
            return true;
        }

        // 长老只能管理普通成员
        return manager.isElder() && target.getRole() == GuildMember.Role.MEMBER;
    }

    /**
     * 打开公会关系设置GUI
     * @param player 玩家
     * @param targetGuild 目标公会
     */
    public void openGuildRelationGUI(Player player, Guild targetGuild) {
        // 检查玩家是否在公会中
        Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (playerGuild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return;
        }

        // 检查玩家是否有权限管理公会关系
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || (!member.isOwner() && !member.isAdmin())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 检查是否是自己的公会
        if (playerGuild.getId() == targetGuild.getId()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.cannot-set-relation-self"));
            return;
        }

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-relation-title", "name", targetGuild.getName());
        Inventory inventory = InventoryUtil.createInventory(new GuildRelationHolder(targetGuild), 36, Component.text(title));

        // 检查当前关系状态
        boolean isAllied = plugin.getAllianceManager().areGuildsAllied(playerGuild.getId(), targetGuild.getId());
        // 检查是否处于战争状态
        boolean isAtWar = false;
        GuildWar war = plugin.getWarManager().getActiveWar(playerGuild.getId());
        if (war != null && (war.getAttackerId() == targetGuild.getId() || war.getDefenderId() == targetGuild.getId())) {
            isAtWar = true;
        }

        // 申请结盟按钮
        ItemStack allianceItem = new ItemStack(Material.EMERALD);
        ItemMeta allianceMeta = allianceItem.getItemMeta();
        ItemUtil.setDisplayName(allianceMeta, Component.text(plugin.getConfigManager().getMessage("gui.request-alliance")));
        List<Component> allianceLore = new ArrayList<>();
        allianceLore.add(Component.text("§7申请与该公会结成联盟"));
        allianceLore.add(Component.text(""));
        if (isAllied) {
            allianceLore.add(Component.text("§c已经是联盟关系"));
        } else if (isAtWar) {
            allianceLore.add(Component.text("§c战争期间无法结盟"));
        } else {
            allianceLore.add(Component.text("§e点击申请结盟"));
        }
        ItemUtil.setLore(allianceMeta, allianceLore);
        allianceItem.setItemMeta(allianceMeta);
        inventory.setItem(11, allianceItem);

        // 宣战按钮
        ItemStack warItem = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta warMeta = warItem.getItemMeta();
        ItemUtil.setDisplayName(warMeta, Component.text(plugin.getConfigManager().getMessage("gui.declare-war")));
        List<Component> warLore = new ArrayList<>();
        warLore.add(Component.text("§7向该公会宣战"));
        warLore.add(Component.text(""));
        if (isAtWar) {
            warLore.add(Component.text("§c已经处于战争状态"));
        } else if (isAllied) {
            warLore.add(Component.text("§c无法向联盟公会宣战"));
        } else {
            warLore.add(Component.text("§e点击宣战"));
        }
        ItemUtil.setLore(warMeta, warLore);
        warItem.setItemMeta(warMeta);
        inventory.setItem(13, warItem);

        // 解除结盟按钮
        ItemStack breakAllianceItem = new ItemStack(Material.REDSTONE);
        ItemMeta breakAllianceMeta = breakAllianceItem.getItemMeta();
        ItemUtil.setDisplayName(breakAllianceMeta, Component.text(plugin.getConfigManager().getMessage("gui.break-alliance")));
        List<Component> breakAllianceLore = new ArrayList<>();
        breakAllianceLore.add(Component.text("§7解除与该公会的联盟关系"));
        breakAllianceLore.add(Component.text(""));
        if (!isAllied) {
            breakAllianceLore.add(Component.text("§c没有联盟关系"));
        } else {
            breakAllianceLore.add(Component.text("§e点击解除结盟"));
        }
        ItemUtil.setLore(breakAllianceMeta, breakAllianceLore);
        breakAllianceItem.setItemMeta(breakAllianceMeta);
        inventory.setItem(15, breakAllianceItem);

        // 停战按钮
        ItemStack ceasefireItem = new ItemStack(Material.WHITE_BANNER);
        ItemMeta ceasefireMeta = ceasefireItem.getItemMeta();
        ItemUtil.setDisplayName(ceasefireMeta, Component.text(plugin.getConfigManager().getMessage("gui.request-ceasefire")));
        List<Component> ceasefireLore = new ArrayList<>();
        ceasefireLore.add(Component.text("§7请求与该公会停战"));
        ceasefireLore.add(Component.text(""));
        if (!isAtWar) {
            ceasefireLore.add(Component.text("§c没有处于战争状态"));
        } else {
            ceasefireLore.add(Component.text("§e点击请求停战"));
        }
        ItemUtil.setLore(ceasefireMeta, ceasefireLore);
        ceasefireItem.setItemMeta(ceasefireMeta);
        inventory.setItem(17, ceasefireItem);

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(31, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 打开公会关系管理GUI
     * @param player 玩家
     * @param guild 公会对象
     * @param page 页码
     */
    public void openGuildRelationManageGUI(Player player, Guild guild, int page) {
        // 检查玩家是否有权限管理公会关系
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || (!member.isOwner() && !member.isAdmin())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 获取所有关系请求
        List<ItemStack> requestItems = new ArrayList<>();

        // 获取收到的联盟请求
        List<AllianceRequest> allianceRequests = plugin.getAllianceManager().getReceivedRequests(guild.getId());

        // 获取收到的停战请求
        List<CeasefireRequest> ceasefireRequests = plugin.getWarManager().getReceivedCeasefireRequests(guild.getId());

        // 处理联盟请求
        for (AllianceRequest request : allianceRequests) {
            Guild requesterGuild = plugin.getGuildManager().getGuildById(request.getRequesterId());
            if (requesterGuild != null) {
                ItemStack item = createRelationManageGuildItem(requesterGuild);
                ItemMeta meta = item.getItemMeta();
                List<Component> lore = new ArrayList<>();
                for (String line : ItemUtil.getLore(meta)) {
                    lore.add(Component.text(line));
                }
                lore.add(Component.text(""));
                lore.add(Component.text("§e联盟申请"));
                lore.add(Component.text("§7该公会请求与你的公会结盟"));
                lore.add(Component.text(""));
                lore.add(Component.text("§a点击接受 §c右键拒绝"));
                ItemUtil.setLore(meta, lore);
                item.setItemMeta(meta);
                requestItems.add(item);
            }
        }

        // 处理停战请求
        for (CeasefireRequest request : ceasefireRequests) {
            Guild requesterGuild = plugin.getGuildManager().getGuildById(request.getRequesterId());
            if (requesterGuild != null) {
                ItemStack item = createRelationManageGuildItem(requesterGuild);
                ItemMeta meta = item.getItemMeta();
                List<Component> lore = new ArrayList<>();
                for (String line : ItemUtil.getLore(meta)) {
                    lore.add(Component.text(line));
                }
                lore.add(Component.text(""));
                lore.add(Component.text("§e停战申请"));
                lore.add(Component.text("§7该公会请求与你的公会停战"));
                lore.add(Component.text(""));
                lore.add(Component.text("§a点击接受 §c右键拒绝"));
                ItemUtil.setLore(meta, lore);
                item.setItemMeta(meta);
                requestItems.add(item);
            }
        }

        // 计算总页数
        int totalRequests = requestItems.size();
        int requestsPerPage = 45; // 9x5
        int totalPages = (int) Math.ceil((double) totalRequests / requestsPerPage);

        // 检查页码是否有效
        if (page < 1) {
            page = 1;
        } else if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }

        // 创建物品栏
        String title = plugin.getConfigManager().getMessage("gui.guild-relation-manage-title", "page", String.valueOf(page));
        Inventory inventory = InventoryUtil.createInventory(new GuildRelationManageHolder(guild, page), 54, Component.text(title));

        // 填充请求物品
        int startIndex = (page - 1) * requestsPerPage;
        int endIndex = Math.min(startIndex + requestsPerPage, totalRequests);

        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(i - startIndex, requestItems.get(i));
        }

        // 如果没有任何请求，显示提示信息
        if (totalRequests == 0) {
            ItemStack infoItem = new ItemStack(Material.PAPER);
            ItemMeta infoMeta = infoItem.getItemMeta();
            ItemUtil.setDisplayName(infoMeta, Component.text("§e暂无待处理的关系请求"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7当前没有任何联盟请求或停战请求"));
            lore.add(Component.text(""));
            lore.add(Component.text("§a你可以使用 §f/g ally add <公会名> §a主动结盟"));
            lore.add(Component.text("§a或使用 §f/g war declare <公会名> §a宣战"));
            ItemUtil.setLore(infoMeta, lore);
            infoItem.setItemMeta(infoMeta);
            inventory.setItem(22, infoItem); // 放在中央位置
        }

        // 添加导航按钮
        if (page > 1) {
            // 上一页按钮
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.previous-page")));
            prevButton.setItemMeta(meta);
            inventory.setItem(45, prevButton);
        }

        if (page < totalPages) {
            // 下一页按钮
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.next-page")));
            nextButton.setItemMeta(meta);
            inventory.setItem(53, nextButton);
        }

        // 返回按钮
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        ItemUtil.setDisplayName(meta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(meta);
        inventory.setItem(49, backButton);

        // 打开GUI
        player.openInventory(inventory);
    }

    /**
     * 创建成员物品
     * @param member 成员对象
     * @return 物品堆
     */
    private ItemStack createMemberItem(GuildMember member) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // 设置玩家头颅
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getPlayerUuid()));

        // 设置显示名称
        ItemUtil.setDisplayName(meta, Component.text("§b" + member.getPlayerName()));

        // 设置描述
        List<Component> lore = new ArrayList<>();

        // 添加职位
        lore.add(Component.text("§7职位: §f" + member.getRole().getDisplayName()));

        // 添加加入时间
        lore.add(Component.text("§7加入时间: §f" + member.getJoinedAt()));

        // 添加UUID（用于识别玩家，不显示给玩家看）
        lore.add(Component.text("§8UUID: " + member.getPlayerUuid().toString()));

        // 添加点击提示
        lore.add(Component.text(""));
        lore.add(Component.text("§e点击管理"));

        ItemUtil.setLore(meta, lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 打开公会领地设置GUI
     * @param player 玩家
     * @param guild 公会对象
     */
    public void openGuildLandSettingsGUI(Player player, Guild guild) {
        // 检查玩家权限
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 创建GUI
        String title = "领地设置 - " + guild.getName();
        Inventory inventory = InventoryUtil.createInventory(new GuildLandSettingsHolder(guild), 54, Component.text(title));

        // 获取当前配置
        boolean protectionEnabled = plugin.getConfig().getBoolean("land.protection." + guild.getName() + ".enabled", true);
        boolean pvpEnabled = plugin.getConfig().getBoolean("land.pvp." + guild.getName() + ".enabled", false);
        boolean guestAccess = plugin.getConfig().getBoolean("land.guest." + guild.getName() + ".access", true);
        boolean membersOnlyBuild = plugin.getConfig().getBoolean("land.build." + guild.getName() + ".members-only", true);

        // 领地保护设置 (位置10)
        ItemStack protectionItem = new ItemStack(protectionEnabled ? Material.SHIELD : Material.BARRIER);
        ItemMeta protectionMeta = protectionItem.getItemMeta();
        ItemUtil.setDisplayName(protectionMeta, Component.text("§6领地保护"));
        List<Component> protectionLore = new ArrayList<>();
        protectionLore.add(Component.text("§7当前状态: " + (protectionEnabled ? "§a启用" : "§c禁用")));
        protectionLore.add(Component.text("§7保护领地免受破坏"));
        protectionLore.add(Component.text(""));
        protectionLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(protectionMeta, protectionLore);
        protectionItem.setItemMeta(protectionMeta);
        inventory.setItem(10, protectionItem);

        // PVP设置 (位置12)
        ItemStack pvpItem = new ItemStack(pvpEnabled ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD);
        ItemMeta pvpMeta = pvpItem.getItemMeta();
        ItemUtil.setDisplayName(pvpMeta, Component.text("§cPVP设置"));
        List<Component> pvpLore = new ArrayList<>();
        pvpLore.add(Component.text("§7当前状态: " + (pvpEnabled ? "§a启用" : "§c禁用")));
        pvpLore.add(Component.text("§7领地内是否允许PVP"));
        pvpLore.add(Component.text(""));
        pvpLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(pvpMeta, pvpLore);
        pvpItem.setItemMeta(pvpMeta);
        inventory.setItem(12, pvpItem);

        // 访客权限设置 (位置14)
        ItemStack guestItem = new ItemStack(guestAccess ? Material.OAK_DOOR : Material.IRON_DOOR);
        ItemMeta guestMeta = guestItem.getItemMeta();
        ItemUtil.setDisplayName(guestMeta, Component.text("§b访客权限"));
        List<Component> guestLore = new ArrayList<>();
        guestLore.add(Component.text("§7当前状态: " + (guestAccess ? "§a允许" : "§c禁止")));
        guestLore.add(Component.text("§7非成员是否可进入领地"));
        guestLore.add(Component.text(""));
        guestLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(guestMeta, guestLore);
        guestItem.setItemMeta(guestMeta);
        inventory.setItem(14, guestItem);

        // 建筑权限设置 (位置16)
        ItemStack buildItem = new ItemStack(membersOnlyBuild ? Material.GOLDEN_PICKAXE : Material.WOODEN_PICKAXE);
        ItemMeta buildMeta = buildItem.getItemMeta();
        ItemUtil.setDisplayName(buildMeta, Component.text("§e建筑权限"));
        List<Component> buildLore = new ArrayList<>();
        buildLore.add(Component.text("§7当前状态: " + (membersOnlyBuild ? "§a仅成员" : "§c所有人")));
        buildLore.add(Component.text("§7领地内的建筑权限"));
        buildLore.add(Component.text(""));
        buildLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(buildMeta, buildLore);
        buildItem.setItemMeta(buildMeta);
        inventory.setItem(16, buildItem);

        // 重置设置按钮 (位置40)
        ItemStack resetItem = new ItemStack(Material.TNT);
        ItemMeta resetMeta = resetItem.getItemMeta();
        ItemUtil.setDisplayName(resetMeta, Component.text("§c重置设置"));
        List<Component> resetLore = new ArrayList<>();
        resetLore.add(Component.text("§7重置所有领地设置为默认值"));
        resetLore.add(Component.text("§7§l警告: 此操作不可逆！"));
        resetLore.add(Component.text(""));
        resetLore.add(Component.text("§c点击重置"));
        ItemUtil.setLore(resetMeta, resetLore);
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(40, resetItem);

        // 返回按钮 (位置49)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);

        player.openInventory(inventory);
    }

    /**
     * 打开公会聊天设置GUI
     * @param player 玩家
     * @param guild 公会对象
     */
    public void openGuildChatSettingsGUI(Player player, Guild guild) {
        // 检查玩家权限
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return;
        }

        // 创建GUI
        String title = "聊天设置 - " + guild.getName();
        Inventory inventory = InventoryUtil.createInventory(new GuildChatSettingsHolder(guild), 54, Component.text(title));

        // 获取当前配置
        boolean guildChatEnabled = plugin.getConfig().getBoolean("chat.guild." + guild.getName() + ".enabled", true);
        boolean allyChatEnabled = plugin.getConfig().getBoolean("chat.ally." + guild.getName() + ".enabled", true);
        boolean chatFilterEnabled = plugin.getConfig().getBoolean("chat.filter." + guild.getName() + ".enabled", false);
        boolean autoGuildChatEnabled = plugin.getConfig().getBoolean("chat.auto." + guild.getName() + ".enabled", false);

        // 公会聊天开关 (位置10)
        ItemStack guildChatItem = new ItemStack(guildChatEnabled ? Material.GREEN_DYE : Material.RED_DYE);
        ItemMeta guildChatMeta = guildChatItem.getItemMeta();
        ItemUtil.setDisplayName(guildChatMeta, Component.text("§a公会聊天"));
        List<Component> guildChatLore = new ArrayList<>();
        guildChatLore.add(Component.text("§7当前状态: " + (guildChatEnabled ? "§a启用" : "§c禁用")));
        guildChatLore.add(Component.text("§7公会内部聊天功能"));
        guildChatLore.add(Component.text(""));
        guildChatLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(guildChatMeta, guildChatLore);
        guildChatItem.setItemMeta(guildChatMeta);
        inventory.setItem(10, guildChatItem);

        // 联盟聊天开关 (位置12)
        ItemStack allyChatItem = new ItemStack(allyChatEnabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta allyChatMeta = allyChatItem.getItemMeta();
        ItemUtil.setDisplayName(allyChatMeta, Component.text("§d联盟聊天"));
        List<Component> allyChatLore = new ArrayList<>();
        allyChatLore.add(Component.text("§7当前状态: " + (allyChatEnabled ? "§a启用" : "§c禁用")));
        allyChatLore.add(Component.text("§7与联盟公会的聊天功能"));
        allyChatLore.add(Component.text(""));
        allyChatLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(allyChatMeta, allyChatLore);
        allyChatItem.setItemMeta(allyChatMeta);
        inventory.setItem(12, allyChatItem);

        // 聊天过滤开关 (位置14)
        ItemStack filterItem = new ItemStack(chatFilterEnabled ? Material.HOPPER : Material.BUCKET);
        ItemMeta filterMeta = filterItem.getItemMeta();
        ItemUtil.setDisplayName(filterMeta, Component.text("§6聊天过滤"));
        List<Component> filterLore = new ArrayList<>();
        filterLore.add(Component.text("§7当前状态: " + (chatFilterEnabled ? "§a启用" : "§c禁用")));
        filterLore.add(Component.text("§7过滤不当内容"));
        filterLore.add(Component.text(""));
        filterLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(filterMeta, filterLore);
        filterItem.setItemMeta(filterMeta);
        inventory.setItem(14, filterItem);

        // 自动进入公会聊天 (位置16)
        ItemStack autoItem = new ItemStack(autoGuildChatEnabled ? Material.REPEATER : Material.REDSTONE);
        ItemMeta autoMeta = autoItem.getItemMeta();
        ItemUtil.setDisplayName(autoMeta, Component.text("§b自动公会聊天"));
        List<Component> autoLore = new ArrayList<>();
        autoLore.add(Component.text("§7当前状态: " + (autoGuildChatEnabled ? "§a启用" : "§c禁用")));
        autoLore.add(Component.text("§7新成员自动进入公会聊天"));
        autoLore.add(Component.text(""));
        autoLore.add(Component.text("§e点击切换"));
        ItemUtil.setLore(autoMeta, autoLore);
        autoItem.setItemMeta(autoMeta);
        inventory.setItem(16, autoItem);

        // 聊天格式设置 (位置28)
        ItemStack formatItem = new ItemStack(Material.NAME_TAG);
        ItemMeta formatMeta = formatItem.getItemMeta();
        ItemUtil.setDisplayName(formatMeta, Component.text("§e聊天格式"));
        List<Component> formatLore = new ArrayList<>();
        String currentFormat = plugin.getConfig().getString("chat.format." + guild.getName() + ".value", "&8[&b{guild}&8] &f{player}: &7{message}");
        formatLore.add(Component.text("§7当前格式:"));
        formatLore.add(Component.text("§f" + currentFormat));
        formatLore.add(Component.text(""));
        formatLore.add(Component.text("§e点击修改"));
        ItemUtil.setLore(formatMeta, formatLore);
        formatItem.setItemMeta(formatMeta);
        inventory.setItem(28, formatItem);

        // 聊天前缀设置 (位置30)
        ItemStack prefixItem = new ItemStack(Material.OAK_SIGN);
        ItemMeta prefixMeta = prefixItem.getItemMeta();
        ItemUtil.setDisplayName(prefixMeta, Component.text("§6聊天前缀"));
        List<Component> prefixLore = new ArrayList<>();
        String currentPrefix = plugin.getConfig().getString("chat.prefix." + guild.getName() + ".value", "[" + guild.getName() + "]");
        prefixLore.add(Component.text("§7当前前缀: §f" + currentPrefix));
        prefixLore.add(Component.text("§7显示在聊天消息前"));
        prefixLore.add(Component.text(""));
        prefixLore.add(Component.text("§e点击修改"));
        ItemUtil.setLore(prefixMeta, prefixLore);
        prefixItem.setItemMeta(prefixMeta);
        inventory.setItem(30, prefixItem);

        // 重置设置按钮 (位置40)
        ItemStack resetItem = new ItemStack(Material.TNT);
        ItemMeta resetMeta = resetItem.getItemMeta();
        ItemUtil.setDisplayName(resetMeta, Component.text("§c重置设置"));
        List<Component> resetLore = new ArrayList<>();
        resetLore.add(Component.text("§7重置所有聊天设置为默认值"));
        resetLore.add(Component.text("§7§l警告: 此操作不可逆！"));
        resetLore.add(Component.text(""));
        resetLore.add(Component.text("§c点击重置"));
        ItemUtil.setLore(resetMeta, resetLore);
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(40, resetItem);

        // 返回按钮 (位置49)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        ItemUtil.setDisplayName(backMeta, Component.text(plugin.getConfigManager().getMessage("gui.back")));
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);

        player.openInventory(inventory);
    }
}
