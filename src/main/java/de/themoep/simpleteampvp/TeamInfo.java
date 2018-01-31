package de.themoep.simpleteampvp;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private final String name;
    private String displayName;
    private ChatColor color = null;
    private Team scoreboardTeam = null;
    private Material blockMaterial = Material.AIR;
    private byte blockData = 0;
    
    private LocationInfo spawn = null;
    
    private LocationInfo point = null;
    
    private RegionInfo region = new RegionInfo(null, null);
    private RegionInfo joinRegion = new RegionInfo(null, null);

    /**
     * Create a new TeamInfo object
     * @param name The name of the Team
     */
    public TeamInfo(String name) {
        this.name = name;
    }

    /**
     * Create a new TeamInfo object
     * @param scoreboardTeam The Team object registered within the server's main scoreboard
     */
    public TeamInfo(Team scoreboardTeam) {
        this(scoreboardTeam.getName());
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

        displayName = config.getString("displayname", "");
        ConfigurationSection spawnSection = config.getConfigurationSection("spawn");
        if(spawnSection != null) {
            spawn = new LocationInfo(spawnSection);
        } else {
            Bukkit.getLogger().log(Level.WARNING, "Team " + config.getName() + " does not have a spawn location defined!");
        }
        ConfigurationSection pointSection = config.getConfigurationSection("point");
        if(pointSection != null) {
            point = new LocationInfo(pointSection);
        }
        ConfigurationSection pos1Section = config.getConfigurationSection("region.pos1");
        ConfigurationSection pos2Section = config.getConfigurationSection("region.pos2");
        if(pos1Section != null && pos2Section != null) {
            region = new RegionInfo(new LocationInfo(pos1Section), new LocationInfo(pos2Section));
        }
        ConfigurationSection joinPos1Section = config.getConfigurationSection("joinregion.pos1");
        ConfigurationSection joinPos2Section = config.getConfigurationSection("joinregion.pos2");
        if(joinPos1Section != null && joinPos2Section != null) {
            joinRegion = new RegionInfo(new LocationInfo(joinPos1Section), new LocationInfo(joinPos2Section));
        } else {
            Bukkit.getLogger().log(Level.WARNING, "Team " + config.getName() + " does not have a join region defined!");
        }
    }
    
    public void init() {
        try {
            scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(name);
        } catch(IllegalArgumentException e) {
            scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name);
        }
        scoreboardTeam.setDisplayName(displayName);
        scoreboardTeam.setColor(color);
        scoreboardTeam.setPrefix("" + color);
        scoreboardTeam.setSuffix("" + ChatColor.RESET);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("displayname", displayName.isEmpty() ? null : displayName);
        data.put("color", color == null ? null : color.name());
        data.put("block", blockMaterial + ":" + blockData);
        data.put("spawn", spawn == null ? null : spawn.serialize());
        data.put("point", point == null ? null : point.serialize());
        data.put("region", region.isValid() ? ImmutableMap.of(
                "pos1", region.getPos1().serialize(),
                "pos2", region.getPos2().serialize()
        ) : null);
        data.put("joinregion", joinRegion.isValid() ? ImmutableMap.of(
                "pos1", joinRegion.getPos1().serialize(),
                "pos2", joinRegion.getPos2().serialize()
        ) : null);
        return data;
    }

    public boolean inTeam(Player player) {
        return scoreboardTeam.hasEntry(player.getName());
    }

    public void addPlayer(Player player) {
        Validate.notNull(scoreboardTeam, "Team not initialised yet!");
        player.sendMessage(ChatColor.GREEN + "Du wurdest Team " + color + getName() + ChatColor.GREEN + " hinzugefügt!");
        scoreboardTeam.addEntry(player.getName());
    }

    public boolean removePlayer(Player player) {
        Validate.notNull(scoreboardTeam, "Team not initialised yet!");
        return scoreboardTeam.removeEntry(player.getName());
    }

    public int getSize() {
        Validate.notNull(scoreboardTeam, "Team not initialised yet!");
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
            List<String> colorList = new ArrayList<>();
            for(ChatColor color : ChatColor.values()) {
                colorList.add(color.name());
            }
            Bukkit.getLogger().log(Level.WARNING, "[SimpleTeamPvP] " + colorStr + " is not a valid color name! (Available are " + colorList.stream().collect(Collectors.joining(", ")) + ")");
            return false;
        }
        if (scoreboardTeam != null) {
            scoreboardTeam.setPrefix("" + color);
            scoreboardTeam.setSuffix("" + ChatColor.RESET);
        }
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

    public boolean containsPlayer(Player p) {
        Validate.notNull(scoreboardTeam, "Team not initialised yet!");
        return scoreboardTeam.hasEntry(p.getName());
    }
    
    public void unregister() {
        if (scoreboardTeam != null) {
            scoreboardTeam.unregister();
        }
    }
}
