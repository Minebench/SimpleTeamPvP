package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.RegionInfo;
import de.themoep.simpleteampvp.config.SimpleConfig;
import de.themoep.simpleteampvp.TeamInfo;
import de.themoep.simpleteampvp.config.SimpleConfigSetting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * SimpleTeamPvP
 * Copyright (C) 2017 Max Lee (https://github.com/Phoenix616/)
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
@ToString
@EqualsAndHashCode(callSuper = false)
public class GameConfig extends SimpleConfig {

    /* --- Settings --- */
    @SimpleConfigSetting(key = "use-kits")
    private boolean usingKits = false;

    @SimpleConfigSetting(key = "show-score")
    private boolean showScore = false;

    @SimpleConfigSetting(key = "score-in-exp-bar")
    private boolean showScoreExp = false;

    @SimpleConfigSetting(key = "filter.break")
    private boolean filterBreak = false;

    @SimpleConfigSetting(key = "filter.place")
    private boolean filterPlace = false;

    @SimpleConfigSetting(key = "filter.drops")
    private boolean filterDrops = false;

    @SimpleConfigSetting(key = "filter.crafting")
    private boolean filterCrafting = false;

    @SimpleConfigSetting(key = "stop-build")
    private boolean stopBuild = false;

    @SimpleConfigSetting(key = "stop-interact")
    private boolean stopInteract = false;

    @SimpleConfigSetting(key = "stop-container-access")
    private boolean stopContainerAccess = false;

    @SimpleConfigSetting(key = "stop-armor-change")
    private boolean stopArmorChange = false;

    @SimpleConfigSetting(key = "kill-streak.name")
    private boolean killStreakDisplayName = false;

    @SimpleConfigSetting(key = "kill-streak.tab")
    private boolean killStreakDisplayTab = false;

    @SimpleConfigSetting(key = "respawn-resistance")
    private int respawnResistance = 5;

    @SimpleConfigSetting(key = "objective-display")
    private String objectiveDisplay = "Points (%winscore%)";

    @SimpleConfigSetting(key = "pointitem")
    private ItemStack pointItem = null;

    @SimpleConfigSetting(key = "filter.whitelist")
    private Set<String> itemWhitelist = new HashSet<>();

    @SimpleConfigSetting(key = "death-drops")
    private List<ItemStack> deathDrops = new ArrayList<>();

    @SimpleConfigSetting(key = "winscore")
    private int winScore = -1;

    @SimpleConfigSetting(key = "duration")
    private int duration = -1;

    @SimpleConfigSetting(key = "pointBlock")
    private Material pointBlock = Material.AIR;

    @SimpleConfigSetting(key = "pointitemchest")
    private LocationInfo pointItemChestLocation = null;
    
    @SimpleConfigSetting(key = "random")
    private RegionInfo randomRegion = new RegionInfo(null, null);
    
    @SimpleConfigSetting(key = "teams")
    private Map<String, TeamInfo> teams = new HashMap<>();
    
    public GameConfig(ConfigurationSection config) {
        super(config);
    }
}
