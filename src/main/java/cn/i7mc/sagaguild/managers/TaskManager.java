package cn.i7mc.sagaguild.managers;

import cn.i7mc.sagaguild.SagaGuild;
import cn.i7mc.sagaguild.data.dao.TaskDAO;
import cn.i7mc.sagaguild.data.models.Guild;
import cn.i7mc.sagaguild.data.models.GuildMember;
import cn.i7mc.sagaguild.data.models.GuildTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 任务管理器
 * 负责公会任务的创建、完成和奖励
 */
public class TaskManager {
    private final SagaGuild plugin;
    private final TaskDAO taskDAO;

    // 缓存公会任务
    private final Map<Integer, List<GuildTask>> guildTasks;

    // 任务检查任务
    private BukkitTask taskCheckTask;

    // 随机任务生成器
    private final Random random;

    // 任务类型对应的实体和方块
    private final Map<GuildTask.Type, List<String>> taskTargets;

    public TaskManager(SagaGuild plugin) {
        this.plugin = plugin;
        this.taskDAO = new TaskDAO(plugin);

        this.guildTasks = new HashMap<>();
        this.random = new Random();
        this.taskTargets = new HashMap<>();

        // 初始化任务目标
        initTaskTargets();

        // 加载所有公会的活跃任务
        loadActiveTasks();

        // 启动任务检查任务
        startTaskCheckTask();
    }

    /**
     * 初始化任务目标
     */
    private void initTaskTargets() {
        // 击杀怪物任务目标
        List<String> mobTargets = new ArrayList<>();
        mobTargets.add(EntityType.ZOMBIE.name());
        mobTargets.add(EntityType.SKELETON.name());
        mobTargets.add(EntityType.CREEPER.name());
        mobTargets.add(EntityType.SPIDER.name());
        mobTargets.add(EntityType.ENDERMAN.name());
        taskTargets.put(GuildTask.Type.KILL_MOBS, mobTargets);

        // 破坏方块任务目标
        List<String> breakTargets = new ArrayList<>();
        breakTargets.add(Material.STONE.name());
        breakTargets.add(Material.COBBLESTONE.name());
        breakTargets.add(Material.DIRT.name());
        breakTargets.add(Material.SAND.name());
        breakTargets.add(Material.GRAVEL.name());
        breakTargets.add(Material.OAK_LOG.name());
        taskTargets.put(GuildTask.Type.BREAK_BLOCKS, breakTargets);

        // 放置方块任务目标
        List<String> placeTargets = new ArrayList<>();
        placeTargets.add(Material.STONE.name());
        placeTargets.add(Material.COBBLESTONE.name());
        placeTargets.add(Material.DIRT.name());
        placeTargets.add(Material.SAND.name());
        placeTargets.add(Material.OAK_PLANKS.name());
        taskTargets.put(GuildTask.Type.PLACE_BLOCKS, placeTargets);

        // 钓鱼任务目标
        List<String> fishTargets = new ArrayList<>();
        fishTargets.add("ANY");
        taskTargets.put(GuildTask.Type.FISH, fishTargets);

        // 合成物品任务目标
        List<String> craftTargets = new ArrayList<>();
        craftTargets.add(Material.IRON_INGOT.name());
        craftTargets.add(Material.GOLD_INGOT.name());
        craftTargets.add(Material.DIAMOND.name());
        craftTargets.add(Material.EMERALD.name());
        craftTargets.add(Material.BREAD.name());
        taskTargets.put(GuildTask.Type.CRAFT, craftTargets);
    }

    /**
     * 从数据库加载所有公会的活跃任务
     */
    private void loadActiveTasks() {
        // 清空缓存
        guildTasks.clear();

        // 加载所有公会的活跃任务
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            List<GuildTask> tasks = taskDAO.getActiveGuildTasks(guild.getId());
            guildTasks.put(guild.getId(), tasks);
        }

        plugin.getLogger().info("已加载 " + guildTasks.size() + " 个公会的任务数据！");
    }

    /**
     * 启动任务检查任务
     */
    private void startTaskCheckTask() {
        // 每小时检查一次任务状态
        taskCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTasks, 20 * 60, 20 * 60 * 60);
    }

    /**
     * 检查任务状态
     */
    private void checkTasks() {
        // 检查所有公会的任务
        for (Map.Entry<Integer, List<GuildTask>> entry : guildTasks.entrySet()) {
            int guildId = entry.getKey();
            List<GuildTask> tasks = entry.getValue();

            // 检查每个任务
            Iterator<GuildTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                GuildTask task = iterator.next();

                // 检查任务状态
                GuildTask.Status status = task.getStatus();
                if (status != GuildTask.Status.ACTIVE) {
                    // 移除非活跃任务
                    iterator.remove();
                }
            }

            // 移除自动生成任务的逻辑，避免创建默认任务
            // 公会可以通过命令手动创建任务
        }
    }

    /**
     * 生成随机任务
     * @param guildId 公会ID
     * @return 任务对象
     */
    private GuildTask generateRandomTask(int guildId) {
        // 随机选择任务类型
        GuildTask.Type type = GuildTask.Type.values()[random.nextInt(GuildTask.Type.values().length)];

        // 随机选择任务目标
        List<String> targets = taskTargets.get(type);
        String target = targets.get(random.nextInt(targets.size()));

        // 生成任务描述
        String description = generateTaskDescription(type, target);

        // 生成任务目标数量
        int targetAmount = random.nextInt(50) + 50; // 50-100

        // 生成任务奖励
        int rewardExp = random.nextInt(100) + 100; // 100-200
        double rewardMoney = random.nextInt(1000) + 1000; // 1000-2000

        // 生成任务过期时间（3天后）
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 3);
        Date expiresAt = calendar.getTime();

        // 创建任务对象
        return new GuildTask(guildId, type, description, targetAmount, rewardExp, rewardMoney, expiresAt);
    }

    /**
     * 生成任务描述
     * @param type 任务类型
     * @param target 任务目标
     * @return 任务描述
     */
    private String generateTaskDescription(GuildTask.Type type, String target) {
        switch (type) {
            case KILL_MOBS:
                return "击杀 " + formatTarget(target) + " 怪物";
            case BREAK_BLOCKS:
                return "破坏 " + formatTarget(target) + " 方块";
            case PLACE_BLOCKS:
                return "放置 " + formatTarget(target) + " 方块";
            case FISH:
                return "钓鱼";
            case CRAFT:
                return "合成 " + formatTarget(target) + " 物品";
            default:
                return "未知任务";
        }
    }

    /**
     * 格式化任务目标
     * @param target 任务目标
     * @return 格式化后的目标
     */
    private String formatTarget(String target) {
        if (target.equals("ANY")) {
            return "任意";
        }

        // TODO: 实现更好的格式化
        return target.toLowerCase().replace("_", " ");
    }

    /**
     * 更新任务进度
     * @param guildId 公会ID
     * @param type 任务类型
     * @param target 任务目标
     * @param amount 增加数量
     * @param playerUuid 贡献玩家的UUID
     */
    public void updateTaskProgress(int guildId, GuildTask.Type type, String target, int amount, UUID playerUuid) {
        // 获取公会任务
        List<GuildTask> tasks = guildTasks.getOrDefault(guildId, new ArrayList<>());

        // 更新匹配的任务
        for (GuildTask task : tasks) {
            if (task.getType() == type && task.getStatus() == GuildTask.Status.ACTIVE) {
                // 检查任务描述是否匹配目标
                if (task.getDescription().contains(formatTarget(target)) ||
                    (type == GuildTask.Type.FISH && task.getDescription().equals("钓鱼"))) {

                    // 增加进度
                    boolean completed = task.addProgress(amount);

                    // 更新数据库
                    taskDAO.updateTaskProgress(task.getId(), task.getProgress(), task.getCompletedAt());

                    // 如果任务完成，发放奖励
                    if (completed) {
                        giveTaskReward(task, playerUuid);
                    }
                }
            }
        }
    }

    /**
     * 发放任务奖励
     * @param task 任务对象
     * @param completerUuid 完成任务的玩家UUID
     */
    private void giveTaskReward(GuildTask task, UUID completerUuid) {
        // 增加公会经验
        plugin.getGuildManager().addGuildExperience(task.getGuildId(), task.getRewardExp());

        // 发放个人金币奖励给完成任务的玩家
        EconomyManager economyManager = plugin.getEconomyManager();
        if (economyManager.isEnabled()) {
            Player completer = Bukkit.getPlayer(completerUuid);
            if (completer != null) {
                economyManager.depositPlayer(completer, task.getRewardMoney());
                completer.sendMessage(plugin.getConfigManager().getMessage("task.reward-received",
                        "money", economyManager.format(task.getRewardMoney())));
            } else {
                // 如果玩家离线，尝试给离线玩家发放奖励
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(completerUuid);
                economyManager.depositPlayer(offlinePlayer, task.getRewardMoney());
            }
            
            // 记录日志
            plugin.getLogger().info("任务奖励发放: " + completerUuid + " 获得 " + task.getRewardMoney() + " 金币");
        } else {
            // 如果经济系统不可用，将金币存入公会银行
            plugin.getBankManager().deposit(task.getGuildId(), task.getRewardMoney());
        }

        // 通知公会成员
        Guild guild = plugin.getGuildManager().getGuildById(task.getGuildId());
        if (guild != null) {
            String completerName = Bukkit.getOfflinePlayer(completerUuid).getName();
            for (GuildMember member : plugin.getGuildManager().getGuildMembers(guild.getId())) {
                Player player = Bukkit.getPlayer(member.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("task.completed",
                            "task", task.getDescription(),
                            "player", completerName,
                            "exp", String.valueOf(task.getRewardExp()),
                            "money", economyManager.isEnabled() ? economyManager.format(task.getRewardMoney()) : String.valueOf(task.getRewardMoney())));
                }
            }
        }
    }

    /**
     * 获取公会活跃任务
     * @param guildId 公会ID
     * @return 任务列表
     */
    public List<GuildTask> getActiveTasks(int guildId) {
        return guildTasks.getOrDefault(guildId, new ArrayList<>());
    }

    /**
     * 获取公会已完成任务
     * @param guildId 公会ID
     * @return 任务列表
     */
    public List<GuildTask> getCompletedTasks(int guildId) {
        return taskDAO.getCompletedGuildTasks(guildId);
    }

    /**
     * 获取任务
     * @param taskId 任务ID
     * @return 任务对象，不存在返回null
     */
    public GuildTask getTask(int taskId) {
        return taskDAO.getTaskById(taskId);
    }
}
