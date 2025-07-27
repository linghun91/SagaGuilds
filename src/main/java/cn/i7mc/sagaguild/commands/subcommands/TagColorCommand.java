package cn.i7mc.sagaguild.commands.subcommands;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.commands.SubCommand;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 设置公会标签颜色命令
 * 允许副会长及以上权限设置公会标签颜色
 */
public class TagColorCommand implements SubCommand {
    private final SagaGuild plugin;
    
    // 允许的颜色代码
    private static final List<String> ALLOWED_COLORS = Arrays.asList(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "a", "b", "c", "d", "e", "f"
    );
    
    // 颜色名称映射
    private static final String[] COLOR_NAMES = {
        "黑色", "深蓝", "深绿", "深青", "深红", "深紫", "金色", "灰色",
        "深灰", "蓝色", "绿色", "青色", "红色", "粉色", "黄色", "白色"
    };
    
    public TagColorCommand(SagaGuild plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "tagcolor";
    }
    
    @Override
    public String getDescription() {
        return "设置公会标签颜色";
    }
    
    @Override
    public String getSyntax() {
        return "/guild tagcolor <颜色代码>";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"color", "tc"};
    }
    
    @Override
    public boolean execute(Player player, String[] args) {
        // 检查参数
        if (args.length < 1) {
            showColorOptions(player);
            return true;
        }
        
        // 获取玩家所在公会
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.not-in-guild"));
            return true;
        }
        
        // 检查权限（副会长及以上）
        GuildMember member = plugin.getGuildManager().getMemberByUuid(player.getUniqueId());
        if (member == null || !member.isAdmin()) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.no-permission"));
            return true;
        }
        
        // 获取颜色代码
        String colorCode = args[0].toLowerCase();
        
        // 验证颜色代码
        if (!ALLOWED_COLORS.contains(colorCode)) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.invalid-color"));
            showColorOptions(player);
            return true;
        }
        
        // 设置颜色
        guild.setTagColor(colorCode);
        
        // 更新数据库
        boolean success = plugin.getGuildManager().updateGuild(guild);
        if (!success) {
            player.sendMessage(plugin.getConfigManager().getMessage("guild.color-update-failed"));
            return true;
        }
        
        // 获取颜色名称
        int colorIndex = ALLOWED_COLORS.indexOf(colorCode);
        String colorName = COLOR_NAMES[colorIndex];
        
        // 发送成功消息
        player.sendMessage(plugin.getConfigManager().getMessage("guild.color-updated",
                "color", colorName));
        
        // 通知公会成员
        plugin.getGuildManager().broadcastToGuild(guild.getId(),
                plugin.getConfigManager().getMessage("guild.color-changed",
                        "player", player.getName(),
                        "color", colorName),
                player.getUniqueId());
        
        return true;
    }
    
    /**
     * 显示可用颜色选项
     * @param player 玩家
     */
    private void showColorOptions(Player player) {
        player.sendMessage(Component.text("§7=== 可用的公会标签颜色 ==="));
        
        for (int i = 0; i < ALLOWED_COLORS.size(); i++) {
            String code = ALLOWED_COLORS.get(i);
            String name = COLOR_NAMES[i];
            
            // 创建带颜色的示例文本
            Component colorExample = Component.text("§" + code + name + " [" + code + "]")
                    .decoration(TextDecoration.ITALIC, false);
            
            player.sendMessage(colorExample);
        }
        
        player.sendMessage(Component.text("§7使用: §f/guild tagcolor <颜色代码>"));
    }
    
    @Override
    public List<String> tabComplete(Player player, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String color : ALLOWED_COLORS) {
                if (color.startsWith(input)) {
                    completions.add(color);
                }
            }
        }
        
        return completions;
    }
}