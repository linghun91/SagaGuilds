package cn.i7mc.sagaguild.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * GUI工具类
 * 提供无缝GUI操作方法，避免GUI跳转时的鼠标移动问题
 * 遵循统一方法原则，所有GUI操作统一通过此工具类处理
 */
public class GUIUtils {
    
    /**
     * 直接切换GUI，避免鼠标移动到游戏画面中央
     * 使用Paper API的openInventory方法直接打开新GUI，无需关闭当前GUI
     * 
     * @param player 玩家对象
     * @param newInventory 新的inventory对象
     */
    public static void switchGUI(Player player, Inventory newInventory) {
        if (player == null || newInventory == null) {
            return;
        }
        
        // 直接打开新GUI，Paper API会自动处理当前GUI的关闭
        player.openInventory(newInventory);
    }
    
    /**
     * 动态更新GUI内容，避免重新打开GUI
     * 使用Inventory.setContents方法直接更新GUI内容
     * 
     * @param inventory 要更新的inventory对象
     * @param newContents 新的物品数组
     */
    public static void updateGUIContent(Inventory inventory, ItemStack[] newContents) {
        if (inventory == null || newContents == null) {
            return;
        }
        
        // 直接更新inventory内容，无需关闭重开
        inventory.setContents(newContents);
    }
    
    /**
     * 更新GUI中的单个槽位
     * 使用Inventory.setItem方法直接更新特定槽位
     * 
     * @param inventory 要更新的inventory对象
     * @param slot 槽位索引
     * @param item 新的物品
     */
    public static void updateGUISlot(Inventory inventory, int slot, ItemStack item) {
        if (inventory == null) {
            return;
        }
        
        // 直接更新特定槽位，无需关闭重开
        inventory.setItem(slot, item);
    }
    
    /**
     * 安全关闭GUI
     * 只有在必要时才关闭GUI，避免不必要的关闭操作
     * 
     * @param player 玩家对象
     */
    public static void closeGUI(Player player) {
        if (player == null) {
            return;
        }
        
        // 检查玩家是否确实打开了GUI
        if (player.getOpenInventory() != null && 
            player.getOpenInventory().getTopInventory() != player.getInventory()) {
            player.closeInventory();
        }
    }
    
    /**
     * 检查玩家是否打开了自定义GUI
     * 
     * @param player 玩家对象
     * @return 是否打开了自定义GUI
     */
    public static boolean hasCustomGUIOpen(Player player) {
        if (player == null) {
            return false;
        }
        
        return player.getOpenInventory() != null && 
               player.getOpenInventory().getTopInventory() != player.getInventory();
    }
}