package cn.i7mc.sagaguild.gui.holders;

import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 公会成员操作持有者
 * 用于标识公会成员操作GUI
 */
public class GuildMemberActionHolder implements InventoryHolder {
    private final Guild guild;
    private final GuildMember member;
    private final int page;
    
    /**
     * 构造函数
     * @param guild 公会对象
     * @param member 成员对象
     * @param page 当前页码
     */
    public GuildMemberActionHolder(Guild guild, GuildMember member, int page) {
        this.guild = guild;
        this.member = member;
        this.page = page;
    }
    
    /**
     * 获取公会
     * @return 公会对象
     */
    public Guild getGuild() {
        return guild;
    }
    
    /**
     * 获取成员
     * @return 成员对象
     */
    public GuildMember getMember() {
        return member;
    }
    
    /**
     * 获取页码
     * @return 当前页码
     */
    public int getPage() {
        return page;
    }
    
    @Override
    public Inventory getInventory() {
        return null; // 由Bukkit管理
    }
}
