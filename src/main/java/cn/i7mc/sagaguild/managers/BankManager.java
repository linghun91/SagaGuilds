package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.BankDAO;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * 银行管理器
 * 负责公会银行的资金管理
 */
public class BankManager {
    private final SagaGuild plugin;
    private final BankDAO bankDAO;

    public BankManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.bankDAO = new BankDAO(plugin);
    }

    /**
     * 获取公会银行余额
     * @param guildId 公会ID
     * @return 银行余额
     */
    public double getBalance(int guildId) {
        return bankDAO.getBalance(guildId);
    }

    /**
     * 获取公会银行容量
     * @param guildId 公会ID
     * @return 银行容量
     */
    public double getCapacity(int guildId) {
        return bankDAO.getCapacity(guildId);
    }

    /**
     * 计算公会银行容量
     * @param guildId 公会ID
     * @return 银行容量
     */
    public double calculateCapacity(int guildId) {
        FileConfiguration config = plugin.getConfig();
        int initialCapacity = config.getInt("bank.initial-capacity", 10000);
        int capacityIncrease = config.getInt("bank.capacity-increase", 5000);

        // 根据公会等级增加容量
        int level = plugin.getGuildManager().getGuildLevel(guildId);
        return initialCapacity + level * capacityIncrease;
    }

    /**
     * 更新公会银行容量
     * @param guildId 公会ID
     */
    public void updateCapacity(int guildId) {
        double capacity = calculateCapacity(guildId);
        bankDAO.setCapacity(guildId, capacity);
    }

    /**
     * 存款
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean deposit(Player player, double amount) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查金额是否有效
        if (amount <= 0) {
            player.sendMessage("§c存款金额必须大于0！");
            return false;
        }

        // 检查玩家是否有足够的金钱
        EconomyManager economyManager = plugin.getEconomyManager();
        if (!economyManager.isEnabled()) {
            player.sendMessage(plugin.getConfigManager().getMessage("bank.economy-disabled"));
            return false;
        }
        
        if (!economyManager.hasBalance(player, amount)) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-enough-money",
                    "amount", economyManager.format(amount)));
            return false;
        }

        // 检查银行是否已满
        double balance = getBalance(guild.getId());
        double capacity = getCapacity(guild.getId());

        if (balance + amount > capacity) {
            player.sendMessage(plugin.getConfigManager().getMessage("bank.capacity-reached",
                    "capacity", String.valueOf((int) capacity)));
            return false;
        }

        // 扣除玩家金钱
        if (!economyManager.withdrawPlayer(player, amount)) {
            player.sendMessage(plugin.getConfigManager().getMessage("bank.deposit-failed"));
            return false;
        }

        // 存款
        boolean success = bankDAO.deposit(guild.getId(), amount);
        if (!success) {
            // 返还玩家金钱
            economyManager.depositPlayer(player, amount);

            player.sendMessage("§c存款失败，请稍后再试！");
            return false;
        }

        // 发送成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("bank.deposited",
                "amount", String.valueOf((int) amount)));

        return true;
    }

    /**
     * 直接存款到公会银行（用于系统奖励）
     * @param guildId 公会ID
     * @param amount 金额
     * @return 是否成功
     */
    public boolean deposit(int guildId, double amount) {
        // 检查金额是否有效
        if (amount <= 0) {
            return false;
        }

        // 检查银行是否已满
        double balance = getBalance(guildId);
        double capacity = getCapacity(guildId);

        if (balance + amount > capacity) {
            return false;
        }

        // 存款
        return bankDAO.deposit(guildId, amount);
    }

    /**
     * 取款
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean withdraw(Player player, double amount) {
        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return false;
        }

        // 检查玩家是否有权限取款
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isElder()) {
            player.sendMessage(plugin.getConfigManager().getMessage("members.not-admin"));
            return false;
        }

        // 检查金额是否有效
        if (amount <= 0) {
            player.sendMessage("§c取款金额必须大于0！");
            return false;
        }

        // 检查银行是否有足够的金钱
        double balance = getBalance(guild.getId());

        if (balance < amount) {
            player.sendMessage(plugin.getConfigManager().getMessage("bank.not-enough-funds"));
            return false;
        }

        // 取款
        boolean success = bankDAO.withdraw(guild.getId(), amount);
        if (!success) {
            player.sendMessage("§c取款失败，请稍后再试！");
            return false;
        }

        // 给予玩家金钱
        EconomyManager economyManager = plugin.getEconomyManager();
        if (!economyManager.isEnabled()) {
            // 如果经济系统不可用，返还金钱到银行
            bankDAO.deposit(guild.getId(), amount);
            player.sendMessage(plugin.getConfigManager().getMessage("bank.economy-disabled"));
            return false;
        }
        
        if (!economyManager.depositPlayer(player, amount)) {
            // 如果给予失败，返还金钱到银行
            bankDAO.deposit(guild.getId(), amount);
            player.sendMessage(plugin.getConfigManager().getMessage("bank.withdraw-failed"));
            return false;
        }

        // 发送成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("bank.withdrawn",
                "amount", String.valueOf((int) amount)));

        return true;
    }
}
