package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.SimpleTeamPvP;

/**
 * SimpleTeamPvP
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p/>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public class GameTimer {

    private final SimpleTeamPvPGame game;
    private final SimpleTeamPvP plugin;
    private int time = 0;
    private int taskId;

    public GameTimer(SimpleTeamPvPGame game) {
        this.game = game;
        this.plugin = game.plugin;
        time = game.getDuration() * 60;
    }

    public boolean start() {
        if(time <= 0)
            return false;
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                time--;
                if(time < 0)
                    time = 0;
                game.setTimerDisplay(time);
                if(time == 0) {
                    game.stop();
                }
            }
        }, 0L, 20L);
        return true;
    }

    public void destroy() {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }
}
