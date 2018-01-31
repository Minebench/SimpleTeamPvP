package de.themoep.simpleteampvp;

import de.themoep.simpleteampvp.games.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerLoginEvent;

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
public class PlayerListener implements Listener {

    private final SimpleTeamPvP plugin;

    public PlayerListener(SimpleTeamPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(PlayerLoginEvent event) {
        if(event.getResult() != PlayerLoginEvent.Result.ALLOWED)
            return;

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        if(plugin.getGame() == null || plugin.getGame().getState() != GameState.RUNNING || plugin.getGame().getTeam(event.getPlayer()) == null) {
            final Player player = event.getPlayer();
            // We need to wait a tick after login...
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                player.setBedSpawnLocation(player.getWorld().getSpawnLocation(), true);
                player.getInventory().clear();
                player.getInventory().setHelmet(null);
                player.getInventory().setChestplate(null);
                player.getInventory().setLeggings(null);
                player.getInventory().setBoots(null);
                player.setLevel(0);
                player.setExp(0);
                player.setHealth(20);
                player.updateInventory();
            });
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player && plugin.getGame() != null && plugin.getGame().getState() != GameState.DESTROYED) {
            event.setCancelled(plugin.getGame().getTeam((Player) event.getEntity()) == null);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if(event.getEntity() instanceof Player && plugin.getGame() != null && plugin.getGame().getState() != GameState.DESTROYED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPvP(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player) {
            if(plugin.getGame().getTeam((Player) event.getEntity()) == null && plugin.getGame() != null && plugin.getGame().getState() != GameState.DESTROYED) {
                if(event.getDamager() instanceof Player) {
                    event.setCancelled(!event.getDamager().hasPermission(SimpleTeamPvP.BYPASS_PERM));
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }
}
