package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设置成员职位命令
 * 仅会长可以修改成员职位
 */
public class SetRoleCommand implements SubCommand {
    private final SagaGuild plugin;
    
    // 允许的职位
    private static final List<String> ALLOWED_ROLES = Arrays.asList(
        "MEMBER", "ELDER", "ADMIN"
    );
    
    // 职位显示名称
    private static final String[] ROLE_DISPLAY_NAMES = {
        "成员", "长老", "副会长"
    };
    
    public SetRoleCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "setrole";
    }
    
    @Override
    public String getDescription() {
        return "设置成员职位";
    }
    
    @Override
    public String getSyntax() {
        return "/guild setrole <玩家> <职位>";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"role", "setrank"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 检查参数
        if (args.length < 2) {
            player.sendMessage("§c用法: " + getSyntax());
            return true;
        }
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }
        
        // 检查权限（仅会长）
        if (!guild.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-leader"));
            return true;
        }
        
        // 获取目标玩家
        String targetName = args[0];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        
        // 检查目标玩家是否在公会中
        GuildMember targetMember = plugin.getGuildManager().getMemberByUuid(targetPlayer.getUniqueId());
        if (targetMember == null || targetMember.getGuildId() != guild.getId()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.player-not-in-guild", 
                    "player", targetName));
            return true;
        }
        
        // 不能修改自己的职位
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.cannot-modify-self-role"));
            return true;
        }
        
        // 获取新职位
        String roleStr = args[1].toUpperCase();
        
        // 验证职位
        if (!ALLOWED_ROLES.contains(roleStr)) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.invalid-role"));
            showAvailableRoles(player);
            return true;
        }
        
        // 转换为职位枚举
        GuildMember.Role newRole;
        try {
            newRole = GuildMember.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.invalid-role"));
            showAvailableRoles(player);
            return true;
        }
        
        // 不能设置为会长
        if (newRole == GuildMember.Role.OWNER) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.cannot-set-owner"));
            return true;
        }
        
        // 获取当前职位
        GuildMember.Role currentRole = targetMember.getRole();
        
        // 检查是否已经是该职位
        if (currentRole == newRole) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.already-has-role", 
                    "player", targetName,
                    "role", newRole.getDisplayName()));
            return true;
        }
        
        // 更新职位
        targetMember.setRole(newRole);
        
        // 保存到数据库
        boolean success = plugin.getDatabaseManager().getMemberDAO().updateMember(targetMember);
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.role-update-failed"));
            return true;
        }
        
        // 获取职位名称索引
        int roleIndex = ALLOWED_ROLES.indexOf(roleStr);
        String roleDisplayName = ROLE_DISPLAY_NAMES[roleIndex];
        
        // 发送成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("guild.role-updated",
                "player", targetName,
                "role", roleDisplayName));
        
        // 通知目标玩家（如果在线）
        if (targetPlayer.isOnline()) {
            Player onlineTarget = (Player) targetPlayer;
            if (currentRole.ordinal() < newRole.ordinal()) {
                // 提升
                onlineTarget.sendMessage(plugin.getConfigManager().getMessage("guild.promoted",
                        "role", roleDisplayName));
            } else {
                // 降级
                onlineTarget.sendMessage(plugin.getConfigManager().getMessage("guild.demoted",
                        "role", roleDisplayName));
            }
        }
        
        // 通知公会成员
        plugin.getGuildManager().broadcastToGuild(guild.getId(),
                plugin.getConfigManager().getMessage("guild.member-role-changed",
                        "player", targetName,
                        "role", roleDisplayName,
                        "operator", player.getName()),
                player.getUniqueId());
        
        return true;
    }
    
    /**
     * 显示可用职位列表
     * @param player 玩家
     */
    private void showAvailableRoles(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("guild.available-roles-header"));
        
        for (int i = 0; i < ALLOWED_ROLES.size(); i++) {
            String role = ALLOWED_ROLES.get(i);
            String displayName = ROLE_DISPLAY_NAMES[i];
            player.sendMessage(plugin.getConfigManager().getMessage("guild.available-role-item",
                    "role", role,
                    "display", displayName));
        }
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 补全公会成员名称
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                List<GuildMember> members = plugin.getDatabaseManager().getMemberDAO().getGuildMembers(guild.getId());
                String input = args[0].toLowerCase();
                
                completions = members.stream()
                        .filter(m -> !m.isOwner()) // 排除会长
                        .map(GuildMember::getPlayerName)
                        .filter(name -> name.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 2) {
            // 补全职位
            String input = args[1].toLowerCase();
            for (String role : ALLOWED_ROLES) {
                if (role.toLowerCase().startsWith(input)) {
                    completions.add(role);
                }
            }
        }
        
        return completions;
    }
}