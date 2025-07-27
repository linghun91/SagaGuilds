package cn.i7mc.sagaguild.listeners;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildWar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 公会战监听器
 * 处理公会战相关的事件
 */
public class WarListener implements Listener {
    private final SagaGuild plugin;

    public WarListener(SagaGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家死亡事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // 检查是否是玩家击杀
        if (killer == null) {
            return;
        }

        // 获取玩家所在公会
        Guild killerGuild = plugin.getGuildManager().getPlayerGuild(killer.getUniqueId());
        Guild victimGuild = plugin.getGuildManager().getPlayerGuild(victim.getUniqueId());

        // 检查是否都在公会中
        if (killerGuild == null || victimGuild == null) {
            return;
        }

        // 检查是否是同一个公会
        if (killerGuild.getId() == victimGuild.getId()) {
            return;
        }

        // 检查是否在公会战中
        GuildWar war = plugin.getWarManager().getActiveWar(killerGuild.getId());
        if (war == null || !war.isParticipant(victimGuild.getId()) || war.getStatus() != GuildWar.Status.ONGOING) {
            return;
        }

        // 记录击杀
        plugin.getWarManager().recordKill(killer, victim);

        // 增加公会战积分
        war.addKill(killer.getUniqueId());
        war.addDeath(victim.getUniqueId());

        // 发送击杀通知
        String killerName = killer.getName();
        String victimName = victim.getName();
        String killerGuildName = killerGuild.getName();
        String victimGuildName = victimGuild.getName();

        // 构建动作条消息
        net.kyori.adventure.text.Component actionBarMessage = net.kyori.adventure.text.Component.text("§c" + killerGuildName + "§f的 §b" + killerName + " §f击杀了 §c" + victimGuildName + "§f的 §b" + victimName + " §f(§a+1分§f)");

        // 向所有在线玩家发送动作条消息
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            onlinePlayer.sendActionBar(actionBarMessage);
        }

        // 向击杀者发送消息
        killer.sendMessage("§a你击杀了 §c" + victimGuildName + "§a的 §b" + victimName + " §a(§6+1分§a)");

        // 向被击杀者发送消息
        victim.sendMessage("§c你被 §a" + killerGuildName + "§c的 §b" + killerName + " §c击杀了");

        // 广播给双方公会成员
        plugin.getGuildManager().broadcastToGuild(killerGuild.getId(),
            "§a公会成员 §b" + killerName + " §a击杀了 §c" + victimGuildName + "§a的 §b" + victimName + " §a(§6+1分§a)",
            killer.getUniqueId());

        plugin.getGuildManager().broadcastToGuild(victimGuild.getId(),
            "§c公会成员 §b" + victimName + " §c被 §a" + killerGuildName + "§c的 §b" + killerName + " §c击杀了",
            victim.getUniqueId());
    }

    /**
     * 实体伤害事件
     * @param event 事件对象
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 检查是否是玩家之间的伤害
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // 获取玩家所在公会
        Guild victimGuild = plugin.getGuildManager().getPlayerGuild(victim.getUniqueId());
        Guild attackerGuild = plugin.getGuildManager().getPlayerGuild(attacker.getUniqueId());

        // 检查是否都在公会中
        if (victimGuild == null || attackerGuild == null) {
            return;
        }

        // 检查是否是同一个公会
        if (victimGuild.getId() == attackerGuild.getId()) {
            // 同一个公会的玩家不能互相伤害
            event.setCancelled(true);
            return;
        }

        // 检查是否是联盟公会
        if (plugin.getAllianceManager().areGuildsAllied(victimGuild.getId(), attackerGuild.getId())) {
            // 联盟公会的玩家不能互相伤害
            event.setCancelled(true);
            return;
        }

        // 其他情况允许PVP：
        // 1. 无任何关系的不同公会成员可以正常PVP
        // 2. 战争状态的公会成员可以PVP（击杀统计在onPlayerDeath中处理）
        // 不需要额外限制，让PVP正常进行
    }

    /**
     * 玩家加入服务器事件
     * @param event 事件对象
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 检查玩家是否在公会中
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            return;
        }

        // 检查公会是否在战争中
        GuildWar war = plugin.getWarManager().getActiveWar(guild.getId());
        if (war == null) {
            return;
        }

        // 通知玩家当前战争状态
        switch (war.getStatus()) {
            case PREPARING:
                player.sendMessage("§c你的公会正在准备与 §7" +
                        plugin.getGuildManager().getGuildById(war.getOpponentId(guild.getId())).getName() +
                        " §c的战争！");
                break;
            case ONGOING:
                player.sendMessage("§c你的公会正在与 §7" +
                        plugin.getGuildManager().getGuildById(war.getOpponentId(guild.getId())).getName() +
                        " §c进行战争！");
                break;
            default:
                break;
        }
    }
}
