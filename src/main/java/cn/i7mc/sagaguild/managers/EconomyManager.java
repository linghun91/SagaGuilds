package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * 经济管理器
 * 负责与Vault经济系统交互
 */
public class EconomyManager {
    private final SagaGuild plugin;
    private Economy economy = null;
    private boolean enabled = false;

    public EconomyManager(SagaGuild plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * 设置经济系统
     * @return 是否成功
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("未找到Vault插件，经济功能将被禁用！");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("未找到经济插件，经济功能将被禁用！");
            return false;
        }
        
        economy = rsp.getProvider();
        enabled = economy != null;
        
        if (enabled) {
            plugin.getLogger().info("成功连接到经济系统: " + economy.getName());
        } else {
            plugin.getLogger().warning("无法连接到经济系统！");
        }
        
        return enabled;
    }

    /**
     * 检查经济系统是否可用
     * @return 是否可用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取玩家余额
     * @param player 玩家
     * @return 余额
     */
    public double getBalance(OfflinePlayer player) {
        if (!enabled) {
            return 0;
        }
        
        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "获取玩家余额失败: " + player.getName(), e);
            return 0;
        }
    }

    /**
     * 检查玩家是否有足够的金钱
     * @param player 玩家
     * @param amount 金额
     * @return 是否有足够金钱
     */
    public boolean hasBalance(OfflinePlayer player, double amount) {
        if (!enabled) {
            return false;
        }
        
        try {
            return economy.has(player, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "检查玩家余额失败: " + player.getName(), e);
            return false;
        }
    }

    /**
     * 扣除玩家金钱
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean withdrawPlayer(OfflinePlayer player, double amount) {
        if (!enabled) {
            return false;
        }
        
        try {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (!response.transactionSuccess()) {
                plugin.getLogger().warning("扣除玩家金钱失败: " + player.getName() + ", 原因: " + response.errorMessage);
            }
            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "扣除玩家金钱异常: " + player.getName(), e);
            return false;
        }
    }

    /**
     * 给予玩家金钱
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (!enabled) {
            return false;
        }
        
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            if (!response.transactionSuccess()) {
                plugin.getLogger().warning("给予玩家金钱失败: " + player.getName() + ", 原因: " + response.errorMessage);
            }
            return response.transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "给予玩家金钱异常: " + player.getName(), e);
            return false;
        }
    }

    /**
     * 格式化金额显示
     * @param amount 金额
     * @return 格式化后的字符串
     */
    public String format(double amount) {
        if (!enabled) {
            return String.format("%.2f", amount);
        }
        
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }

    /**
     * 获取货币名称
     * @param plural 是否复数
     * @return 货币名称
     */
    public String getCurrencyName(boolean plural) {
        if (!enabled) {
            return plural ? "coins" : "coin";
        }
        
        try {
            return plural ? economy.currencyNamePlural() : economy.currencyNameSingular();
        } catch (Exception e) {
            return plural ? "coins" : "coin";
        }
    }
}