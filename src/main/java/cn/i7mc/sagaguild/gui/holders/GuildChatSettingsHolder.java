package cn.i7mc.sagaguild.gui.holders;

import cn.i7mc.sagaguild.data.models.Guild;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 公会聊天设置持有者
 * 用于标识公会聊天设置GUI
 */
public class GuildChatSettingsHolder implements InventoryHolder {
    private final Guild guild;
    
    /**
     * 构造函数
     * @param guild 公会对象
     */
    public GuildChatSettingsHolder(Guild guild) {
        this.guild = guild;
    }
    
    /**
     * 获取公会
     * @return 公会对象
     */
    public Guild getGuild() {
        return guild;
    }
    
    @Override
    public Inventory getInventory() {
        return null; // 由Bukkit管理
    }
}