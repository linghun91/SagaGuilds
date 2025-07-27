package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.LandDAO;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildLand;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 领地管理器
 * 负责公会领地的声明、保护和权限管理
 */
public class LandManager {
    private final SagaGuild plugin;
    private final LandDAO landDAO;

    // 缓存领地数据
    private final Map<String, GuildLand> landsByChunk;
    private final Map<Integer, Map<String, GuildLand>> landsByGuild;

    public LandManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.landDAO = new LandDAO(plugin);

        this.landsByChunk = new HashMap<>();
        this.landsByGuild = new HashMap<>();

        // 加载所有领地数据到缓存
        loadLands();
    }

    /**
     * 从数据库加载所有领地数据到缓存
     */
    private void loadLands() {
        // 清空缓存
        landsByChunk.clear();
        landsByGuild.clear();

        // 加载所有公会的领地
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            List<GuildLand> lands = landDAO.getGuildLands(guild.getId());
            Map<String, GuildLand> guildLands = new HashMap<>();

            for (GuildLand land : lands) {
                String chunkKey = land.getChunkKey();
                landsByChunk.put(chunkKey, land);
                guildLands.put(chunkKey, land);
            }

            landsByGuild.put(guild.getId(), guildLands);
        }

        plugin.getLogger().info("已加载 " + landsByChunk.size() + " 个领地数据！");
    }

    /**
     * 重新加载所有领地数据
     */
    public void reloadLands() {
        loadLands();
    }

    /**
     * 声明领地
     * @param player 玩家
     * @param chunk 区块
     * @return 是否成功
     */
    public boolean claimLand(Player player, Chunk chunk) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限声明领地
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 检查当前世界是否允许声明领地
        if (isWorldDisabled(chunk.getWorld().getName())) {
            player.sendMessage(plugin.getConfigManager().getMessage("land.world-disabled"));
            return false;
        }

        // 检查区块是否已被声明
        String chunkKey = getChunkKey(chunk);
        if (landsByChunk.containsKey(chunkKey)) {
            player.sendMessage(plugin.getConfigManager().getMessage("land.already-claimed"));
            return false;
        }

        // 检查公会领地数量是否已达上限
        FileConfiguration config = plugin.getConfig();
        int maxClaims = config.getInt("land.max-claims", 10);
        Map<String, GuildLand> guildLands = landsByGuild.getOrDefault(guild.getId(), new HashMap<>());

        if (guildLands.size() >= maxClaims) {
            player.sendMessage(plugin.getConfigManager().getMessage("land.max-claims-reached",
                    "max", String.valueOf(maxClaims)));
            return false;
        }

        // 检查声明费用
        double claimCost = config.getDouble("land.claim-cost", 500);
        if (claimCost > 0) {
            // 检查经济系统是否启用
            if (!plugin.getEconomyManager().isEnabled()) {
                player.sendMessage(plugin.getConfigManager().getMessage("bank.economy-disabled"));
                return false;
            }
            
            // 检查玩家余额
            if (!plugin.getEconomyManager().hasBalance(player, claimCost)) {
                player.sendMessage(plugin.getConfigManager().getMessage("guild.not-enough-money",
                    "amount", plugin.getEconomyManager().format(claimCost)));
                return false;
            }
            
            // 扣除费用
            if (!plugin.getEconomyManager().withdrawPlayer(player, claimCost)) {
                player.sendMessage("§c扣除费用失败，请稍后再试！");
                return false;
            }
        }

        // 声明领地
        GuildLand land = new GuildLand(guild.getId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        int landId = landDAO.claimLand(land);

        if (landId == -1) {
            // 如果声明失败，退还费用
            if (claimCost > 0 && plugin.getEconomyManager().isEnabled()) {
                plugin.getEconomyManager().depositPlayer(player, claimCost);
            }
            player.sendMessage("§c声明领地失败，请稍后再试！");
            return false;
        }

        // 更新缓存
        landsByChunk.put(chunkKey, land);
        guildLands.put(chunkKey, land);
        landsByGuild.put(guild.getId(), guildLands);

        // 发送成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("land.claimed"));

        return true;
    }

    /**
     * 取消领地声明
     * @param player 玩家
     * @param chunk 区块
     * @return 是否成功
     */
    public boolean unclaimLand(Player player, Chunk chunk) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限取消声明
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 检查区块是否已被声明
        String chunkKey = getChunkKey(chunk);
        GuildLand land = landsByChunk.get(chunkKey);

        if (land == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("land.not-claimed"));
            return false;
        }

        // 检查是否是自己公会的领地
        if (land.getGuildId() != guild.getId()) {
            player.sendMessage(plugin.getConfigManager().getMessage("land.not-your-claim"));
            return false;
        }

        // 取消声明
        boolean success = landDAO.unclaimLand(land.getId());
        if (!success) {
            player.sendMessage("§c取消领地声明失败，请稍后再试！");
            return false;
        }

        // 更新缓存
        landsByChunk.remove(chunkKey);
        Map<String, GuildLand> guildLands = landsByGuild.get(guild.getId());
        if (guildLands != null) {
            guildLands.remove(chunkKey);
        }

        // 发送成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("land.unclaimed"));

        return true;
    }

    /**
     * 取消公会所有领地声明
     * @param guildId 公会ID
     * @return 是否成功
     */
    public boolean unclaimAllGuildLands(int guildId) {
        // 获取公会所有领地
        Map<String, GuildLand> guildLands = landsByGuild.get(guildId);
        if (guildLands == null || guildLands.isEmpty()) {
            return true; // 没有领地，直接返回成功
        }

        // 删除数据库中的所有领地
        boolean success = landDAO.unclaimAllLands(guildId);
        if (!success) {
            return false;
        }

        // 更新缓存
        for (String chunkKey : guildLands.keySet()) {
            landsByChunk.remove(chunkKey);
        }
        landsByGuild.remove(guildId);

        return true;
    }

    /**
     * 检查区块是否被声明
     * @param chunk 区块
     * @return 是否被声明
     */
    public boolean isChunkClaimed(Chunk chunk) {
        return landsByChunk.containsKey(getChunkKey(chunk));
    }

    /**
     * 获取区块所属公会
     * @param chunk 区块
     * @return 公会ID，未声明返回-1
     */
    public int getChunkGuildId(Chunk chunk) {
        GuildLand land = landsByChunk.get(getChunkKey(chunk));
        return land != null ? land.getGuildId() : -1;
    }

    /**
     * 检查玩家是否有权限在区块中执行操作
     * @param player 玩家
     * @param chunk 区块
     * @return 是否有权限
     */
    public boolean hasPermission(Player player, Chunk chunk) {
        // 检查区块是否被声明
        GuildLand land = landsByChunk.get(getChunkKey(chunk));
        if (land == null) {
            return true; // 未声明的区块任何人都可以操作
        }

        // 检查玩家是否有管理员权限
        if (player.hasPermission("guild.admin")) {
            return true;
        }

        // 检查玩家是否在公会中
        Guild playerGuild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (playerGuild == null) {
            return false;
        }

        // 检查是否是自己公会的领地
        return playerGuild.getId() == land.getGuildId();
    }

    /**
     * 获取区块唯一标识
     * @param chunk 区块
     * @return 区块唯一标识
     */
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    /**
     * 检查指定世界是否禁止声明领地
     * @param worldName 世界名称
     * @return 是否禁止声明领地
     */
    private boolean isWorldDisabled(String worldName) {
        FileConfiguration config = plugin.getConfig();
        List<String> disabledWorlds = config.getStringList("land.disabled-worlds");
        return disabledWorlds.contains(worldName);
    }

    /**
     * 获取位置所在区块的唯一标识
     * @param location 位置
     * @return 区块唯一标识
     */
    private String getChunkKey(Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX() / 16 + ":" + location.getBlockZ() / 16;
    }

    /**
     * 获取公会所有领地
     * @param guildId 公会ID
     * @return 领地列表
     */
    public List<GuildLand> getGuildLands(int guildId) {
        return landDAO.getGuildLands(guildId);
    }

    /**
     * 获取公会领地数量
     * @param guildId 公会ID
     * @return 领地数量
     */
    public int getGuildLandCount(int guildId) {
        Map<String, GuildLand> guildLands = landsByGuild.get(guildId);
        return guildLands != null ? guildLands.size() : 0;
    }
}
