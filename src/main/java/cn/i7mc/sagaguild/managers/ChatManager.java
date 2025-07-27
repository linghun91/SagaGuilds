package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.utils.TeamUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天管理器
 * 负责公会聊天和标签显示
 */
public class ChatManager {
    private final SagaGuild plugin;

    // 玩家聊天模式
    private final Map<UUID, ChatMode> playerChatModes;

    // 记分板
    private Scoreboard scoreboard;

    /**
     * 聊天模式枚举
     */
    public enum ChatMode {
        NORMAL,     // 普通聊天
        GUILD,      // 公会聊天
        ALLIANCE    // 联盟聊天
    }

    public ChatManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.playerChatModes = new HashMap<>();

        // 初始化记分板
        initScoreboard();
    }

    /**
     * 初始化记分板
     */
    private void initScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        // 加载所有公会
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            createTeam(guild);
        }
    }

    /**
     * 创建公会队伍
     * @param guild 公会对象
     */
    public void createTeam(Guild guild) {
        String teamName = "guild_" + guild.getId();
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // 使用兼容工具设置前缀
        TeamUtil.setPrefix(team, Component.text("§8[§b" + guild.getTag() + "§8] "));

        // 添加所有成员
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(guild.getId())) {
            Player player = Bukkit.getPlayer(member.getPlayerUuid());
            if (player != null && player.isOnline()) {
                team.addEntry(player.getName());
                player.setScoreboard(scoreboard);
            }
        }
    }

    /**
     * 删除公会队伍
     * @param guildId 公会ID
     */
    public void removeTeam(int guildId) {
        String teamName = "guild_" + guildId;
        Team team = scoreboard.getTeam(teamName);

        if (team != null) {
            team.unregister();
        }
    }

    /**
     * 添加玩家到公会队伍
     * @param player 玩家
     * @param guildId 公会ID
     */
    public void addPlayerToTeam(Player player, int guildId) {
        String teamName = "guild_" + guildId;
        Team team = scoreboard.getTeam(teamName);

        if (team != null) {
            team.addEntry(player.getName());
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * 从公会队伍中移除玩家
     * @param player 玩家
     */
    public void removePlayerFromTeam(Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }

        // 重置记分板
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * 获取玩家聊天模式
     * @param playerUuid 玩家UUID
     * @return 聊天模式
     */
    public ChatMode getPlayerChatMode(UUID playerUuid) {
        return playerChatModes.getOrDefault(playerUuid, ChatMode.NORMAL);
    }

    /**
     * 设置玩家聊天模式
     * @param playerUuid 玩家UUID
     * @param mode 聊天模式
     */
    public void setPlayerChatMode(UUID playerUuid, ChatMode mode) {
        playerChatModes.put(playerUuid, mode);
    }

    /**
     * 切换玩家聊天模式
     * @param player 玩家
     * @return 新的聊天模式
     */
    public ChatMode togglePlayerChatMode(Player player) {
        UUID playerUuid = player.getUniqueId();
        ChatMode currentMode = getPlayerChatMode(playerUuid);
        ChatMode newMode;

        // 切换模式
        switch (currentMode) {
            case NORMAL:
                newMode = ChatMode.GUILD;
                player.sendMessage(plugin.getConfigManager().getMessage("chat.guild-chat-enabled"));
                break;
            case GUILD:
                newMode = ChatMode.ALLIANCE;
                player.sendMessage(plugin.getConfigManager().getMessage("chat.ally-chat-enabled"));
                break;
            case ALLIANCE:
                newMode = ChatMode.NORMAL;
                player.sendMessage(plugin.getConfigManager().getMessage("chat.guild-chat-disabled"));
                break;
            default:
                newMode = ChatMode.NORMAL;
                break;
        }

        setPlayerChatMode(playerUuid, newMode);
        return newMode;
    }

    /**
     * 处理聊天消息
     * @param player 玩家
     * @param message 消息
     * @return 是否取消原始事件
     */
    public boolean handleChat(Player player, String message) {
        UUID playerUuid = player.getUniqueId();
        ChatMode mode = getPlayerChatMode(playerUuid);

        // 检查是否是公会或联盟聊天
        if (mode == ChatMode.NORMAL) {
            return false;
        }

        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerUuid);
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            setPlayerChatMode(playerUuid, ChatMode.NORMAL);
            return false;
        }

        // 获取聊天格式
        FileConfiguration config = plugin.getConfig();
        String format;

        if (mode == ChatMode.GUILD) {
            // 尝试从公会特定配置中获取格式，如果不存在则使用默认格式
            String guildFormat = config.getString("chat.format." + guild.getName() + ".value");
            if (guildFormat == null || guildFormat.contains("MemorySection")) {
                guildFormat = config.getString("chat.format");
            }
            // 额外检查，确保不是MemorySection对象的toString()
            if (guildFormat == null || guildFormat.contains("MemorySection")) {
                guildFormat = "&8[&b公会&8] &f{player}: &7{message}";
            }
            format = guildFormat;
            sendGuildMessage(guild.getId(), format, player.getName(), message);
        } else if (mode == ChatMode.ALLIANCE) {
            // 获取联盟聊天格式
            format = config.getString("chat.ally-format", "&8[&d联盟&8] &f{player}: &7{message}");
            sendAllianceMessage(guild.getId(), format, player.getName(), message);
        }

        return true;
    }

    /**
     * 发送公会消息
     * @param guildId 公会ID
     * @param format 消息格式
     * @param playerName 玩家名称
     * @param message 消息内容
     */
    private void sendGuildMessage(int guildId, String format, String playerName, String message) {
        // 替换占位符
        String formattedMessage = format
                .replace("&", "§")
                .replace("{player}", playerName)
                .replace("{message}", message);

        // 发送给所有公会成员
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(guildId)) {
            Player target = Bukkit.getPlayer(member.getPlayerUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * 发送联盟消息
     * @param guildId 公会ID
     * @param format 消息格式
     * @param playerName 玩家名称
     * @param message 消息内容
     */
    private void sendAllianceMessage(int guildId, String format, String playerName, String message) {
        // 替换占位符
        String formattedMessage = format
                .replace("&", "§")
                .replace("{player}", playerName)
                .replace("{message}", message);

        // 发送给所有公会成员
        for (GuildMember member : plugin.getGuildManager().getGuildMembers(guildId)) {
            Player target = Bukkit.getPlayer(member.getPlayerUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(formattedMessage);
            }
        }

        // 发送给联盟公会成员
        java.util.List<Integer> alliances = plugin.getAllianceManager().getGuildAlliances(guildId);
        if (alliances != null && !alliances.isEmpty()) {
            for (Integer allyGuildId : alliances) {
                // 获取联盟公会的所有成员
                for (GuildMember allyMember : plugin.getGuildManager().getGuildMembers(allyGuildId)) {
                    Player allyPlayer = Bukkit.getPlayer(allyMember.getPlayerUuid());
                    if (allyPlayer != null && allyPlayer.isOnline()) {
                        allyPlayer.sendMessage(formattedMessage);
                    }
                }
            }
        }
    }
}
