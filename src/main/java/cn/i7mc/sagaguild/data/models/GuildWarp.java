package cn.i7mc.sagaguild.data.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Date;
import java.util.UUID;

/**
 * 公会传送点数据模型
 */
public class GuildWarp {
    private int id;
    private int guildId;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private Date createdAt;
    private UUID creatorUuid;

    /**
     * 创建一个新的公会传送点对象
     * @param id 传送点ID
     * @param guildId 公会ID
     * @param world 世界名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param yaw 偏航角
     * @param pitch 俯仰角
     * @param createdAt 创建时间
     * @param creatorUuid 创建者UUID
     */
    public GuildWarp(int id, int guildId, String world, double x, double y, double z, 
                     float yaw, float pitch, Date createdAt, UUID creatorUuid) {
        this.id = id;
        this.guildId = guildId;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = createdAt;
        this.creatorUuid = creatorUuid;
    }

    /**
     * 从Location创建传送点（用于新建传送点）
     * @param guildId 公会ID
     * @param location 位置
     * @param creatorUuid 创建者UUID
     */
    public GuildWarp(int guildId, Location location, UUID creatorUuid) {
        this.id = 0; // 未保存到数据库
        this.guildId = guildId;
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.createdAt = new Date();
        this.creatorUuid = creatorUuid;
    }

    /**
     * 转换为Bukkit Location对象
     * @return Location对象，如果世界不存在返回null
     */
    public Location toLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGuildId() {
        return guildId;
    }

    public void setGuildId(int guildId) {
        this.guildId = guildId;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public void setCreatorUuid(UUID creatorUuid) {
        this.creatorUuid = creatorUuid;
    }
}