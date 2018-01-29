package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.RegionInfo;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

@Data
public class GameConfig {

    private final ConfigurationSection config;

    /* --- Settings --- */
    @GameConfigSetting(key = "use-kits")
    private boolean usingKits = false;

    @GameConfigSetting(key = "show-score")
    private boolean showScore = false;

    @GameConfigSetting(key = "score-in-exp-bar")
    private boolean showScoreExp = false;

    @GameConfigSetting(key = "filter.break")
    private boolean filterBreak = false;

    @GameConfigSetting(key = "filter.place")
    private boolean filterPlace = false;

    @GameConfigSetting(key = "filter.drops")
    private boolean filterDrops = false;

    @GameConfigSetting(key = "filter.crafting")
    private boolean filterCrafting = false;

    @GameConfigSetting(key = "stop-build")
    private boolean stopBuild = false;

    @GameConfigSetting(key = "stop-interact")
    private boolean stopInteract = false;

    @GameConfigSetting(key = "stop-container-access")
    private boolean stopContainerAccess = false;

    @GameConfigSetting(key = "stop-armor-change")
    private boolean stopArmorChange = false;

    @GameConfigSetting(key = "kill-streak.name")
    private boolean killStreakDisplayName = false;

    @GameConfigSetting(key = "kill-streak.tab")
    private boolean killStreakDisplayTab = false;

    @GameConfigSetting(key = "respawn-resistance")
    private int respawnResistance = 5;

    @GameConfigSetting(key = "objective-display")
    private String objectiveDisplay = "Points (%winscore%)";

    @GameConfigSetting(key = "pointitem")
    private ItemStack pointItem = null;

    @GameConfigSetting(key = "filter.whitelist")
    private Set<String> itemWhitelist = new HashSet<>();

    @GameConfigSetting(key = "death-drops")
    private List<ItemStack> deathDrops = new ArrayList<>();

    @GameConfigSetting(key = "winscore")
    private int winScore = -1;

    @GameConfigSetting(key = "duration")
    private int duration = -1;

    @GameConfigSetting(key = "pointBlock")
    private Material pointBlock = Material.AIR;

    @GameConfigSetting(key = "pointitemchest")
    private LocationInfo pointItemChestLocation = null;
    
    @GameConfigSetting(key = "random")
    private RegionInfo randomRegion = null;

    public ConfigurationSection getConfig() {
        return config;
    }
}
