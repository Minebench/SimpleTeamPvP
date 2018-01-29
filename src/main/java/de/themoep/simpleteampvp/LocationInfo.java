package de.themoep.simpleteampvp;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.NumberConversions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bukkit Plugins
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
@Data
public class LocationInfo implements Cloneable {
    
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public LocationInfo(String worldName, double x, double y, double z) {
        this(worldName, x, y, z, 0F, 0F);
    }

    public LocationInfo(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldName = worldName;
    }

    public LocationInfo(Location location) {
        this(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public LocationInfo(ConfigurationSection config) {
        this(config.getString("world"), config.getDouble("x"), config.getDouble("y"), config.getDouble("z"), (float) config.getDouble("yaw"), (float) config.getDouble("pitch"));
    }

    public int getBlockX() {
        return NumberConversions.floor(x);
    }

    public int getBlockY() {
        return NumberConversions.floor(y);
    }

    public int getBlockZ() {
        return NumberConversions.floor(z);
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(getWorldName());
        return world == null ? null : new Location(world, getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("world", this.worldName);
        data.put("x", this.x);
        data.put("y", this.y);
        data.put("z", this.z);
        data.put("yaw", this.yaw);
        data.put("pitch", this.pitch);
        return data;
    }

    public static LocationInfo deserialize(Map<String, Object> args) {
        World world = Bukkit.getWorld((String)args.get("world"));
        if(world == null) {
            throw new IllegalArgumentException("unknown world");
        } else {
            return new LocationInfo(world.getName(), NumberConversions.toDouble(args.get("x")), NumberConversions.toDouble(args.get("y")), NumberConversions.toDouble(args.get("z")), NumberConversions.toFloat(args.get("yaw")), NumberConversions.toFloat(args.get("pitch")));
        }
    }

    /**
     * Check whether or not a block is at that location
     */
    public boolean contains(Block block) {
        return (int) x == block.getX() && (int) y == block.getY() && (int) z == block.getZ() && worldName.equalsIgnoreCase(block.getWorld().getName());
    }
}
