package de.themoep.simpleteampvp;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * SimpleTeamPvP
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
@Getter
@Setter
public class TeamInfo {
    private ChatColor color = null;
    private Team scoreboardTeam;
    private Material blockMaterial = Material.AIR;
    private byte blockData = 0;
    
    private LocationInfo spawn = null;
    
    private LocationInfo point = null;
    
    private RegionInfo region = null;
    private RegionInfo joinRegion = null;

    /**
     * Create a new TeamInfo object
     * @param name The name of the Team
     */
    public TeamInfo(String name) {
        try {
            scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(name);
        } catch(IllegalArgumentException e) {
            scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name);
        }
    }

    /**
     * Create a new TeamInfo object
     * @param scoreboardTeam The Team object registered within the server's main scoreboard
     */
    public TeamInfo(Team scoreboardTeam) {
        this.scoreboardTeam = scoreboardTeam;
    }

    /**
     * Create a new TeamInfo object
     * @param scoreboardTeam The Team object registered within the server's main scoreboard
     * @param spawn The respawn point of this team
     */
    public TeamInfo(Team scoreboardTeam, LocationInfo spawn, LocationInfo point) {
        this(scoreboardTeam);
        this.spawn = spawn;
    }
    /**
     * Create a new TeamInfo object from a configuration section
     * @param config The configuration section which holds the info for this team
     */
    public TeamInfo(ConfigurationSection config) throws IllegalArgumentException {
        this(config.getName());
        setColor(config.getString("color", ""));
        setBlock(config.getString("block", ""));

        String displayName = config.getString("displayname", "");
        if(!displayName.isEmpty()) {
            scoreboardTeam.setDisplayName(displayName);
        }
        ConfigurationSection spawnSection = config.getConfigurationSection("spawn");
        if(spawnSection != null) {
            spawn = new LocationInfo(spawnSection);
        }
        ConfigurationSection pointSection = config.getConfigurationSection("point");
        if(pointSection != null) {
            point = new LocationInfo(pointSection);
        }
        ConfigurationSection pos1Section = config.getConfigurationSection("pos1");
        ConfigurationSection pos2Section = config.getConfigurationSection("pos2");
        if(pos1Section != null && pos2Section != null) {
            region = new RegionInfo(new LocationInfo(pos1Section), new LocationInfo(pos2Section));
        }
        ConfigurationSection joinPos1Section = config.getConfigurationSection("join-pos1");
        ConfigurationSection joinPos2Section = config.getConfigurationSection("join-pos2");
        if(joinPos1Section != null && joinPos2Section != null) {
            joinRegion = new RegionInfo(new LocationInfo(joinPos1Section), new LocationInfo(joinPos2Section));
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("displayname", this.scoreboardTeam.getDisplayName());
        data.put("color", this.color == null ? null : this.color.getName());
        data.put("block", this.blockMaterial + ":" + this.blockData);
        data.put("spawn", this.spawn == null ? null : this.spawn.serialize());
        data.put("point", this.point == null ? null : this.point.serialize());
        data.put("pos1", this.region == null ? null : this.region.getPos1().serialize());
        data.put("pos2", this.region == null ? null : this.region.getPos2().serialize());
        data.put("join-pos1", this.joinRegion == null ? null : this.joinRegion.getPos1().serialize());
        data.put("join-pos2", this.joinRegion == null ? null : this.joinRegion.getPos2().serialize());
        return data;
    }

    public boolean inTeam(Player player) {
        return scoreboardTeam.hasEntry(player.getName());
    }

    public void addPlayer(Player player) {
        player.sendMessage(ChatColor.GREEN + "Du wurdest Team " + color + getName() + ChatColor.GREEN + " hinzugefügt!");
        scoreboardTeam.addEntry(player.getName());
    }

    public boolean removePlayer(Player player) {
        return scoreboardTeam.removeEntry(player.getName());
    }

    public String getName() {
        return scoreboardTeam.getName();
    }

    public int getSize() {
        return scoreboardTeam.getSize();
    }

    public boolean setColor(String colorStr) {
        if(colorStr.isEmpty())
            return false;
        try {
            color = ChatColor.valueOf(colorStr.toUpperCase());
        } catch(IllegalArgumentException e) {
            if(colorStr.length() == 1) {
                color = ChatColor.getByChar(colorStr.toLowerCase().charAt(0));
            } else if(colorStr.length() == 2 && (colorStr.charAt(0) == ChatColor.COLOR_CHAR || colorStr.charAt(0) == '&' || colorStr.charAt(0) == '#')) {
                color = ChatColor.getByChar(colorStr.toLowerCase().charAt(1));
            }
        }
        if(color == null) {
            List<String> colorList = new ArrayList<String>();
            for(ChatColor color : ChatColor.values()) {
                colorList.add(color.getName());
            }
            Bukkit.getLogger().log(Level.WARNING, "[SimpleTeamPvP] " + colorStr + " is not a valid color name! (Available are " + colorList.stream().collect(Collectors.joining(", ")) + ")");
            return false;
        }
        scoreboardTeam.setPrefix("" + color);
        scoreboardTeam.setSuffix("" + ChatColor.RESET);
        return true;

    }

    public boolean setBlock(String blockStr) {
        if(blockStr.indexOf(':') > -1 && blockStr.length() > blockStr.indexOf(':') + 1) {
            String matStr = blockStr.toUpperCase().substring(0, blockStr.indexOf(':'));
            try {
                blockMaterial = Material.valueOf(matStr);
            } catch(IllegalArgumentException e) {
                Bukkit.getLogger().log(Level.WARNING, "[SimpleTeamPvP] " + matStr + " is not a valid block material name!");
                return false;
            }
            String dataStr = blockStr.substring(blockStr.indexOf(':') + 1);
            try {
                blockData = Byte.parseByte(dataStr);
            } catch(NumberFormatException e) {
                Bukkit.getLogger().log(Level.WARNING, "[SimpleTeamPvP] " + dataStr + " is not valid block data byte!");
                return false;
            }
        } else {
            try {
                blockMaterial = Material.valueOf(blockStr.toUpperCase());
            } catch(IllegalArgumentException e) {
                Bukkit.getLogger().log(Level.WARNING, "[SimpleTeamPvP] " + blockStr + " is not a valid block material name!");
                return false;
            }
        }
        return true;
    }

    public void setBlock(Block block) {
        blockMaterial = block.getType();
        blockData = block.getData();
    }

    public boolean regionContains(Location loc) {
        return region != null && region.contains(loc);
    }
    
    public boolean joinRegionContains(Location loc) {
        return joinRegion != null && joinRegion.contains(loc);
    }

    public boolean containsPlayer(Player p) {
        return scoreboardTeam.hasEntry(p.getName());
    }
}
