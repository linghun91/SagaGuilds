package cn.i7mc.sagaguild.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家输入监听器
 * 处理GUI触发的文本输入功能
 */
public class PlayerInputListener implements Listener {
    private final SagaGuild plugin;
    
    // 输入状态映射表
    private final Map<UUID, InputState> playerInputStates = new ConcurrentHashMap<>();
    
    /**
     * 输入状态枚举
     */
    public enum InputType {
        GUILD_DESCRIPTION,
        GUILD_ANNOUNCEMENT,
        OWNERSHIP_TRANSFER_CONFIRM,
        CHAT_FORMAT,
        CHAT_PREFIX
    }
    
    /**
     * 输入状态类
     */
    public static class InputState {
        private final InputType type;
        private final Guild guild;
        private final long timestamp;
        private final Object extraData; // 额外数据，如目标成员
        
        public InputState(InputType type, Guild guild) {
            this.type = type;
            this.guild = guild;
            this.timestamp = System.currentTimeMillis();
            this.extraData = null;
        }
        
        public InputState(InputType type, Guild guild, Object extraData) {
            this.type = type;
            this.guild = guild;
            this.timestamp = System.currentTimeMillis();
            this.extraData = extraData;
        }
        
        public InputType getType() { return type; }
        public Guild getGuild() { return guild; }
        public long getTimestamp() { return timestamp; }
        public Object getExtraData() { return extraData; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000; // 30秒过期
        }
    }
    
    public PlayerInputListener(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 设置玩家输入状态
     * @param player 玩家
     * @param type 输入类型
     * @param guild 相关公会
     */
    public void setPlayerInputState(Player player, InputType type, Guild guild) {
        playerInputStates.put(player.getUniqueId(), new InputState(type, guild));
    }
    
    /**
     * 设置玩家输入状态（带额外数据）
     * @param player 玩家
     * @param type 输入类型
     * @param guild 相关公会
     * @param extraData 额外数据
     */
    public void setPlayerInputState(Player player, InputType type, Guild guild, Object extraData) {
        playerInputStates.put(player.getUniqueId(), new InputState(type, guild, extraData));
    }
    
    /**
     * 清除玩家输入状态
     * @param player 玩家
     */
    public void clearPlayerInputState(Player player) {
        playerInputStates.remove(player.getUniqueId());
    }
    
    /**
     * 监听玩家聊天事件处理输入
     * @param event 聊天事件
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    @SuppressWarnings("deprecation") // 为了兼容性使用已弃用API
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        InputState inputState = playerInputStates.get(playerId);
        if (inputState == null) {
            return; // 不是输入状态，让其他监听器处理
        }
        
        // 检查输入是否过期
        if (inputState.isExpired()) {
            clearPlayerInputState(player);
            player.sendMessage(plugin.getConfigManager().getMessage("general.input-timeout", "输入已超时，请重新操作"));
            return;
        }
        
        // 取消原始聊天事件
        event.setCancelled(true);
        
        String message = event.getMessage();
        
        // 检查是否是取消操作
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("取消")) {
            clearPlayerInputState(player);
            player.sendMessage(plugin.getConfigManager().getMessage("general.operation-cancelled", "操作已取消"));
            return;
        }
        
        // 根据输入类型处理
        try {
            switch (inputState.getType()) {
                case GUILD_DESCRIPTION:
                    handleGuildDescriptionInput(player, message, inputState.getGuild());
                    break;
                case GUILD_ANNOUNCEMENT:
                    handleGuildAnnouncementInput(player, message, inputState.getGuild());
                    break;
                case OWNERSHIP_TRANSFER_CONFIRM:
                    handleOwnershipTransferConfirm(player, message, inputState);
                    break;
                case CHAT_FORMAT:
                    handleChatFormatInput(player, message, inputState.getGuild());
                    break;
                case CHAT_PREFIX:
                    handleChatPrefixInput(player, message, inputState.getGuild());
                    break;
            }
        } finally {
            // 确保在任何情况下都清除输入状态
            clearPlayerInputState(player);
        }
    }
    
    /**
     * 处理公会描述输入
     * @param player 玩家
     * @param description 新描述
     * @param guild 公会
     */
    private void handleGuildDescriptionInput(Player player, String description, Guild guild) {
        // 验证描述长度
        int maxLength = plugin.getConfig().getInt("guild.max-description-length", 100);
        if (description.length() > maxLength) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.description-too-long", 
                "max", String.valueOf(maxLength)));
            return;
        }
        
        // 更新公会描述
        guild.setDescription(description);
        boolean success = plugin.getGuildManager().updateGuild(guild);
        
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.description-updated", 
                "description", description));
            // 重新打开设置界面
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getGuiManager().openGuildSettingsGUI(player, guild);
            });
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.update-failed"));
        }
    }
    
    /**
     * 处理公会公告输入
     * @param player 玩家
     * @param announcement 新公告
     * @param guild 公会
     */
    private void handleGuildAnnouncementInput(Player player, String announcement, Guild guild) {
        // 验证公告长度
        int maxLength = plugin.getConfig().getInt("guild.max-announcement-length", 200);
        if (announcement.length() > maxLength) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.announcement-too-long", 
                "max", String.valueOf(maxLength)));
            return;
        }
        
        // 更新公会公告
        guild.setAnnouncement(announcement);
        boolean success = plugin.getGuildManager().updateGuild(guild);
        
        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.announcement-updated", 
                "announcement", announcement));
            // 重新打开设置界面
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getGuiManager().openGuildSettingsGUI(player, guild);
            });
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.update-failed"));
        }
    }
    
    /**
     * 处理转让会长确认输入
     * @param player 玩家
     * @param message 输入消息
     * @param inputState 输入状态
     */
    private void handleOwnershipTransferConfirm(Player player, String message, InputState inputState) {
        if (!message.equalsIgnoreCase("confirm") && !message.equalsIgnoreCase("确认")) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.ownership-transfer-cancelled", 
                "会长转让已取消"));
            return;
        }
        
        // 检查转让会长权限
        if (!player.hasPermission("guild.transfer")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission", 
                "您没有权限执行此操作"));
            return;
        }
        
        // 获取目标成员和页码信息
        if (inputState.getExtraData() instanceof Object[]) {
            Object[] data = (Object[]) inputState.getExtraData();
            if (data.length >= 2 && data[0] instanceof cn.i7mc.sagaguild.data.models.GuildMember && data[1] instanceof Integer) {
                cn.i7mc.sagaguild.data.models.GuildMember targetMember = (cn.i7mc.sagaguild.data.models.GuildMember) data[0];
                int page = (Integer) data[1];
                
                boolean success = plugin.getGuildManager().transferOwnership(player, targetMember.getPlayerUuid());
                if (success) {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.ownership-transferred",
                            "player", targetMember.getPlayerName()));
                    // 转让成功后返回成员列表
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getGuiManager().openGuildMemberGUI(player, inputState.getGuild(), page);
                    });
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("guild.action-failed"));
                }
            }
        }
    }
    
    /**
     * 处理聊天格式输入
     * @param player 玩家
     * @param format 新格式
     * @param guild 公会
     */
    private void handleChatFormatInput(Player player, String format, Guild guild) {
        // 验证格式长度
        if (format.length() > 100) {
            player.sendMessage(plugin.getConfigManager().getMessage("chat.format-too-long", 
                "max", "100"));
            return;
        }
        
        // 更新聊天格式
        plugin.getConfig().set("chat.format." + guild.getName() + ".value", format);
        plugin.saveConfig();
        
        player.sendMessage(plugin.getConfigManager().getMessage("chat.format-updated", 
            "format", format));
        // 重新打开聊天设置界面
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getGuiManager().openGuildChatSettingsGUI(player, guild);
        });
    }
    
    /**
     * 处理聊天前缀输入
     * @param player 玩家
     * @param prefix 新前缀
     * @param guild 公会
     */
    private void handleChatPrefixInput(Player player, String prefix, Guild guild) {
        // 验证前缀长度
        if (prefix.length() > 20) {
            player.sendMessage(plugin.getConfigManager().getMessage("chat.prefix-too-long", 
                "max", "20"));
            return;
        }
        
        // 更新聊天前缀
        plugin.getConfig().set("chat.prefix." + guild.getName() + ".value", prefix);
        plugin.saveConfig();
        
        // 发送更新成功消息，直接使用字符串拼接确保显示正确的值
        String pluginPrefix = plugin.getConfigManager().getMessage("prefix");
        player.sendMessage(pluginPrefix + "§a聊天前缀已更新为: §7[" + prefix + "]");
        // 重新打开聊天设置界面
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getGuiManager().openGuildChatSettingsGUI(player, guild);
        });
    }
    
    /**
     * 玩家退出时清理输入状态
     * @param event 退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayerInputState(event.getPlayer());
    }
}