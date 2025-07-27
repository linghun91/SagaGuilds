package cn.i7mc.sagaguild.placeholders;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.managers.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PlaceholderAPI 扩展类
 * 提供 SagaGuild 的占位符支持
 */
public class SagaGuildPlaceholders extends PlaceholderExpansion {
    private final SagaGuild plugin;
    private final GuildManager guildManager;
    private final MemberManager memberManager;
    private final BankManager bankManager;
    private final WarManager warManager;
    private final AllianceManager allianceManager;
    private final RankingManager rankingManager;
    
    // 排行榜占位符正则表达式
    private static final Pattern TOP_PATTERN = Pattern.compile("top(\\d+)_(\\w+)_(\\w+)(?:_(\\w+))?");
    
    public SagaGuildPlaceholders(SagaGuild plugin) {
        this.plugin = plugin;
        this.guildManager = plugin.getGuildManager();
        this.memberManager = plugin.getMemberManager();
        this.bankManager = plugin.getBankManager();
        this.warManager = plugin.getWarManager();
        this.allianceManager = plugin.getAllianceManager();
        this.rankingManager = plugin.getRankingManager();
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "sg";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        // 处理玩家公会信息占位符（以 currentguild_ 开头）
        if (params.startsWith("currentguild_")) {
            return handlePlayerPlaceholder(player, params);
        }
        
        // 处理排行榜占位符（以 top 开头）
        if (params.startsWith("top")) {
            return handleRankingPlaceholder(params);
        }
        
        return null;
    }
    
    /**
     * 处理玩家相关的占位符
     * @param player 玩家
     * @param placeholder 完整的占位符名称
     * @return 占位符值
     */
    private String handlePlayerPlaceholder(OfflinePlayer player, String placeholder) {
        if (player == null) {
            return "";
        }
        
        // 获取玩家所在的公会
        Guild guild = guildManager.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return "";
        }
        
        // 处理不同的占位符
        switch (placeholder.toLowerCase()) {
            case "currentguild_name":
                return guild.getName();
                
            case "currentguild_tag":
                return guild.getTag();
                
            case "currentguild_tag_color":
                return guild.getTagColor();
                
            case "currentguild_description":
                return guild.getDescription();
                
            case "currentguild_level":
                return String.valueOf(guild.getLevel());
                
            case "currentguild_role":
                // 使用 MemberDAO 获取成员信息
                GuildMember member = plugin.getDatabaseManager().getMemberDAO().getMemberByUuid(player.getUniqueId());
                if (member != null) {
                    return member.getRole().toString();
                }
                return "";
                
            case "currentguild_owner":
                OfflinePlayer owner = Bukkit.getOfflinePlayer(guild.getOwnerUuid());
                return owner.getName() != null ? owner.getName() : "";
                
            case "currentguild_currentmembers":
                // 使用 MemberDAO 获取成员列表
                int memberCount = plugin.getDatabaseManager().getMemberDAO().getGuildMembers(guild.getId()).size();
                return String.valueOf(memberCount);
                
            case "currentguild_maxmembers":
                int maxMembers = guildManager.getMaxMembersByLevel(guild.getLevel());
                return String.valueOf(maxMembers);
                
            case "currentguild_money":
                double balance = bankManager.getBalance(guild.getId());
                return String.format("%.2f", balance);
                
            case "currentguild_warstats":
                return getWarStatus(guild);
                
            case "currentguild_allystats":
                return getAllianceStatus(guild);
                
            default:
                return "";
        }
    }
    
    /**
     * 处理排行榜相关的占位符
     * @param placeholder 完整的占位符名称
     * @return 占位符值
     */
    private String handleRankingPlaceholder(String placeholder) {
        // 使用正则表达式匹配排行榜占位符格式
        Matcher matcher = TOP_PATTERN.matcher(placeholder);
        if (!matcher.matches()) {
            return "";
        }
        
        int rank = Integer.parseInt(matcher.group(1));
        String type = matcher.group(2);
        String valueType = matcher.group(3);
        String extra = matcher.group(4);
        
        // 获取排行榜条目
        RankingManager.RankingEntry entry = rankingManager.getRankingEntry(type, rank);
        if (entry == null) {
            return "";
        }
        
        // 根据请求的值类型返回相应数据
        if ("gname".equals(valueType)) {
            // 返回公会名称
            return entry.getGuild().getName();
        } else if ("value".equals(valueType)) {
            // 返回数值
            if ("money".equals(type)) {
                return String.format("%.2f", entry.getValue());
            } else {
                return String.valueOf((int) entry.getValue());
            }
        }
        
        return "";
    }
    
    /**
     * 获取公会战争状态
     * @param guild 公会
     * @return 战争状态文本
     */
    private String getWarStatus(Guild guild) {
        // 检查公会是否在战争中
        boolean inWar = warManager.isGuildInWar(guild.getId());
        
        if (inWar) {
            return plugin.getConfigManager().getMessage("placeholder.war_status.in_war", "战争中");
        } else {
            return plugin.getConfigManager().getMessage("placeholder.war_status.none", "和平");
        }
    }
    
    /**
     * 获取公会联盟状态
     * @param guild 公会
     * @return 联盟状态文本
     */
    private String getAllianceStatus(Guild guild) {
        // 获取公会的联盟
        List<Guild> allies = allianceManager.getAlliedGuilds(guild.getId());
        
        if (allies != null && !allies.isEmpty()) {
            // 返回第一个联盟的名称
            return plugin.getConfigManager().getMessage("placeholder.alliance_status.allied", 
                allies.get(0).getName());
        } else {
            return plugin.getConfigManager().getMessage("placeholder.alliance_status.none", "无联盟");
        }
    }
}