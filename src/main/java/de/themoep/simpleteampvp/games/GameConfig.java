package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.LocationInfo;
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

public class GameConfig {

    private final ConfigurationSection config;

    /* --- Settings --- */
    @GameConfigSetting(key = "use-kits")
    boolean useKits = false;

    @GameConfigSetting(key = "show-score")
    boolean showScore = false;

    @GameConfigSetting(key = "score-in-exp-bar")
    boolean showScoreExp = false;

    @GameConfigSetting(key = "filter.break")
    boolean filterBreak = false;

    @GameConfigSetting(key = "filter.place")
    boolean filterPlace = false;

    @GameConfigSetting(key = "filter.drops")
    boolean filterDrops = false;

    @GameConfigSetting(key = "filter.crafting")
    boolean filterCrafting = false;

    @GameConfigSetting(key = "stop-build")
    boolean stopBuild = false;

    @GameConfigSetting(key = "stop-interact")
    boolean stopInteract = false;

    @GameConfigSetting(key = "stop-container-access")
    boolean stopContainerAccess = false;

    @GameConfigSetting(key = "stop-armor-change")
    boolean stopArmorChange = false;

    @GameConfigSetting(key = "kill-streak.name")
    boolean killStreakDisplayName = false;

    @GameConfigSetting(key = "kill-streak.tab")
    boolean killStreakDisplayTab = false;

    @GameConfigSetting(key = "respawn-resistance")
    int respawnResistance = 5;

    @GameConfigSetting(key = "objective-display")
    String objectiveDisplay = "Points (%winscore%)";

    @GameConfigSetting(key = "pointitem")
    ItemStack pointItem = null;

    @GameConfigSetting(key = "filter.whitelist")
    Set<String> itemWhitelist = new HashSet<>();

    @GameConfigSetting(key = "death-drops")
    List<ItemStack> deathDrops = new ArrayList<>();

    @GameConfigSetting(key = "winscore")
    int winScore = -1;

    @GameConfigSetting(key = "duration")
    int duration = -1;

    @GameConfigSetting(key = "pointBlock")
    Material pointBlock = Material.AIR;

    @GameConfigSetting(key = "pointitemchest")
    LocationInfo pointItemChestLocation = null;

    public GameConfig(ConfigurationSection config) {
        this.config = config;
    }

    public ConfigurationSection getConfig() {
        return config;
    }
}
