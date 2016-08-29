package de.themoep.simpleteampvp;

import de.themoep.simpleteampvp.games.GameState;
import org.bukkit.Material;
import org.bukkit.block.ContainerBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.InventoryHolder;

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

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if(plugin.getGame() == null || plugin.getGame().getState() != GameState.RUNNING || plugin.getTeam(event.getPlayer()) == null) {
            final Player player = event.getPlayer();
            // We need to wait a tick after login...
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    player.teleport(player.getWorld().getSpawnLocation());
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
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent event) {
        event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player && plugin.getGame() != null && plugin.getGame().getState() != GameState.DESTROYED) {
            event.setCancelled(plugin.getTeam((Player) event.getEntity()) == null);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            if(event.getClickedBlock().getState() instanceof InventoryHolder
                    || event.getClickedBlock().getType() == Material.BED_BLOCK
                    || event.getClickedBlock().getType() == Material.TRAP_DOOR
                    ) {
                event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
            }
        }
        if((event.getAction() == Action.PHYSICAL) && event.getClickedBlock().getType() == Material.SOIL) {
            event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPvP(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player) {
            if(plugin.getTeam((Player) event.getEntity()) == null && plugin.getGame() != null && plugin.getGame().getState() != GameState.DESTROYED) {
                if(event.getDamager() instanceof Player) {
                    event.setCancelled(!event.getDamager().hasPermission("simpleteampvp.bypass"));
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }
}
