package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

import java.util.Iterator;
import java.util.logging.Level;

/**
 * Bukkit Plugins
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
public class XmasGame extends SimpleTeamPvPGame {
    public XmasGame(SimpleTeamPvP plugin) {
        super(plugin, "xmas");

        useKits(true);
        showScore(true);
        setObjectiveDisplay(ChatColor.GREEN + "%time% " + ChatColor.WHITE + "- " + ChatColor.RED +"%winscore%");
        for(TeamInfo team : plugin.getTeamMap().values()) {
            team.getScoreboardTeam().setAllowFriendlyFire(false);
            team.getScoreboardTeam().setCanSeeFriendlyInvisibles(true);
            team.getScoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getWhoClicked().hasPermission("simpleteampvp.bypass"))
            return;

        if(event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getWhoClicked().hasPermission("simpleteampvp.bypass"))
            return;

        for(int i : event.getRawSlots()) {
            if(i <= 8 && i >= 5) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if(!event.getItemDrop().getItemStack().isSimilar(getPointItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void checkForPointItemOnPlayerDeath(PlayerDeathEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        Player player = event.getEntity();

        if(player.hasPermission("simpleteampvp.bypass"))
            return;

        if(plugin.getTeam(player) == null)
            return;

        if(!event.getKeepLevel()) {
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }

        if(event.getKeepInventory()) {
            for(int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if(item.isSimilar(getPointItem())) {
                    player.getInventory().setItem(i, null);
                    player.getLocation().getWorld().dropItem(player.getLocation(), item);
                }
            }
        } else {
            Iterator<ItemStack> dropIterator = event.getDrops().iterator();
            while(dropIterator.hasNext()) {
                ItemStack drop = dropIterator.next();
                if(!drop.isSimilar(getPointItem())) {
                    dropIterator.remove();
                }
            }
        }
    }


    @EventHandler
    public void onPointClick(PlayerInteractEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        if(getPointItem() == null)
            return;

        Player player = event.getPlayer();

        if(player.hasPermission("simpleteampvp.bypass"))
            return;

        TeamInfo team = plugin.getTeam(player);

        if(team == null)
            return;

        if(team.getPoint() == null || !team.getPoint().contains(event.getClickedBlock()))
            return;

        event.setCancelled(true);

        if(!player.getInventory().containsAtLeast(getPointItem(), 1)) {
            player.sendMessage(ChatColor.RED + "Du hast keine Geschenke in deinem Inventar?");
            return;
        }

        int amount = 0;
        for(int i = 0; i < player.getInventory().getSize() && i <= 39; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if(item != null && item.isSimilar(getPointItem())) {
                amount += item.getAmount();
                player.getInventory().setItem(i, null);
            }
        }
        if(amount > 0) {
            player.updateInventory();
            player.sendMessage(ChatColor.YELLOW + Integer.toString(amount) + ChatColor.GREEN + " Geschenk" + (amount == 1 ? "" : "e") + " für dein Team hinzugefügt!");
            incrementScore(team, amount);
        } else {
            player.sendMessage(ChatColor.RED + "Du hast keine Geschenke in deinem Inventar?");
        }
    }

    @EventHandler
    public void onPointBlockBreak(BlockBreakEvent event) {
        TeamInfo team = plugin.getTeam(event.getPlayer());
        if(team == null)
            return;

        if(event.getBlock().getType() == getPointBlock() ||
                (getPointBlock() == Material.GLOWING_REDSTONE_ORE && event.getBlock().getType() == Material.REDSTONE_ORE) ||
                (getPointBlock() == Material.REDSTONE_ORE && event.getBlock().getType() == Material.GLOWING_REDSTONE_ORE)
                ) {
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            addToPointBlockSet(event.getBlock().getLocation());
            if(getPointItem() != null) {
                event.getPlayer().getInventory().addItem(getPointItem());
            } else {
                plugin.getLogger().log(Level.WARNING, "Point item is null!");
            }
        }
    }

    @Override
    public SimpleTeamPvPGame clone() {
        return new XmasGame(plugin);
    }
}
