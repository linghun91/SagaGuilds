package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.dao.MemberDAO;
import cn.i7mc.sagaguild.data.dao.WarDAO;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 公会排行榜管理器
 * 负责管理和缓存各种公会排行榜数据
 */
public class RankingManager {
    private final SagaGuild plugin;
    private final GuildManager guildManager;
    private final MemberDAO memberDAO;
    private final BankManager bankManager;
    private final WarDAO warDAO;
    
    // 缓存排行榜数据
    private final Map<String, List<RankingEntry>> rankingCache;
    private final Map<String, Long> cacheTimestamps;
    
    // 缓存时间（毫秒）
    private static final long CACHE_DURATION = 300000; // 5分钟
    
    /**
     * 排行榜条目
     */
    public static class RankingEntry {
        private final Guild guild;
        private final double value;
        
        public RankingEntry(Guild guild, double value) {
            this.guild = guild;
            this.value = value;
        }
        
        public Guild getGuild() {
            return guild;
        }
        
        public double getValue() {
            return value;
        }
    }
    
    public RankingManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.guildManager = plugin.getGuildManager();
        this.memberDAO = plugin.getDatabaseManager().getMemberDAO();
        this.bankManager = plugin.getBankManager();
        this.warDAO = new WarDAO(plugin);
        this.rankingCache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取公会银行余额排行榜
     * @param limit 返回的最大数量
     * @return 排行榜列表
     */
    public List<RankingEntry> getTopGuildsByMoney(int limit) {
        String cacheKey = "money_" + limit;
        
        // 检查缓存是否有效
        if (isCacheValid(cacheKey)) {
            return rankingCache.get(cacheKey);
        }
        
        // 获取所有公会并按银行余额排序
        List<RankingEntry> ranking = guildManager.getAllGuilds().stream()
            .map(guild -> new RankingEntry(guild, bankManager.getBalance(guild.getId())))
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
        
        // 更新缓存
        updateCache(cacheKey, ranking);
        
        return ranking;
    }
    
    /**
     * 获取公会人数排行榜
     * @param limit 返回的最大数量
     * @return 排行榜列表
     */
    public List<RankingEntry> getTopGuildsByMembers(int limit) {
        String cacheKey = "members_" + limit;
        
        // 检查缓存是否有效
        if (isCacheValid(cacheKey)) {
            return rankingCache.get(cacheKey);
        }
        
        // 获取所有公会并按成员数量排序
        List<RankingEntry> ranking = guildManager.getAllGuilds().stream()
            .map(guild -> new RankingEntry(guild, memberDAO.getGuildMembers(guild.getId()).size()))
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
        
        // 更新缓存
        updateCache(cacheKey, ranking);
        
        return ranking;
    }
    
    /**
     * 获取公会等级排行榜
     * @param limit 返回的最大数量
     * @return 排行榜列表
     */
    public List<RankingEntry> getTopGuildsByLevel(int limit) {
        String cacheKey = "level_" + limit;
        
        // 检查缓存是否有效
        if (isCacheValid(cacheKey)) {
            return rankingCache.get(cacheKey);
        }
        
        // 获取所有公会并按等级排序，等级相同则按经验排序
        List<RankingEntry> ranking = guildManager.getAllGuilds().stream()
            .sorted((a, b) -> {
                int levelCompare = Integer.compare(b.getLevel(), a.getLevel());
                if (levelCompare != 0) {
                    return levelCompare;
                }
                return Integer.compare(b.getExperience(), a.getExperience());
            })
            .limit(limit)
            .map(guild -> new RankingEntry(guild, guild.getLevel()))
            .collect(Collectors.toList());
        
        // 更新缓存
        updateCache(cacheKey, ranking);
        
        return ranking;
    }
    
    /**
     * 获取公会战胜利次数排行榜
     * @param limit 返回的最大数量
     * @return 排行榜列表
     */
    public List<RankingEntry> getTopGuildsByWarWins(int limit) {
        String cacheKey = "war_" + limit;
        
        // 检查缓存是否有效
        if (isCacheValid(cacheKey)) {
            return rankingCache.get(cacheKey);
        }
        
        // 获取所有公会并按战争胜利次数排序
        List<RankingEntry> ranking = guildManager.getAllGuilds().stream()
            .map(guild -> new RankingEntry(guild, warDAO.getGuildWarWins(guild.getId())))
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
        
        // 更新缓存
        updateCache(cacheKey, ranking);
        
        return ranking;
    }
    
    /**
     * 清除所有缓存
     */
    public void clearCache() {
        rankingCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * 清除特定类型的缓存
     * @param type 缓存类型（money, members, level）
     */
    public void clearCache(String type) {
        rankingCache.entrySet().removeIf(entry -> entry.getKey().startsWith(type + "_"));
        cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().startsWith(type + "_"));
    }
    
    /**
     * 检查缓存是否有效
     * @param cacheKey 缓存键
     * @return 是否有效
     */
    private boolean isCacheValid(String cacheKey) {
        if (!rankingCache.containsKey(cacheKey)) {
            return false;
        }
        
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp == null) {
            return false;
        }
        
        return System.currentTimeMillis() - timestamp < CACHE_DURATION;
    }
    
    /**
     * 更新缓存
     * @param cacheKey 缓存键
     * @param data 数据
     */
    private void updateCache(String cacheKey, List<RankingEntry> data) {
        rankingCache.put(cacheKey, data);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * 获取特定排名的公会（用于占位符）
     * @param type 排行榜类型（money, members, level）
     * @param rank 排名（从1开始）
     * @return 排行榜条目，如果不存在则返回null
     */
    public RankingEntry getRankingEntry(String type, int rank) {
        if (rank < 1) {
            return null;
        }
        
        List<RankingEntry> ranking;
        switch (type.toLowerCase()) {
            case "money":
                ranking = getTopGuildsByMoney(rank);
                break;
            case "members":
                ranking = getTopGuildsByMembers(rank);
                break;
            case "level":
                ranking = getTopGuildsByLevel(rank);
                break;
            case "war":
                ranking = getTopGuildsByWarWins(rank);
                break;
            default:
                return null;
        }
        
        if (ranking.size() >= rank) {
            return ranking.get(rank - 1);
        }
        
        return null;
    }
}