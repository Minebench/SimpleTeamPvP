package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;

/**
 * Copyright (C) 2016 Max Lee (https://github.com/Phoenix616/)
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
public class CookieWarsGame extends SimpleTeamPvPGame {
    public CookieWarsGame(SimpleTeamPvP plugin) {
        super(plugin, "cookie");
    }

    @Override
    public boolean start() {
        for(TeamInfo team : plugin.getTeamMap().values()) {
            team.getScoreboardTeam().setAllowFriendlyFire(false);
            team.getScoreboardTeam().setCanSeeFriendlyInvisibles(true);
            team.getScoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        plugin.getServer().getScoreboardManager().getMainScoreboard().clearSlot(DisplaySlot.BELOW_NAME);
        plugin.getServer().getScoreboardManager().getMainScoreboard().clearSlot(DisplaySlot.PLAYER_LIST);
        return super.start();
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
    public void onPointClick(PlayerInteractEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.SOIL) {
            event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
        }

        if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(event.getClickedBlock().getState() instanceof InventoryHolder
                    || event.getClickedBlock().getType() == Material.BED_BLOCK
                    ) {
                event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
            }
        }

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
            player.sendMessage(ChatColor.RED + "Du hast keine Cookies in deinem Inventar?");
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
            player.sendMessage(ChatColor.YELLOW + Integer.toString(amount) + ChatColor.GREEN + " Cookie" + (amount == 1 ? "" : "e") + " für dein Team hinzugefügt!");
            incrementScore(team, amount);
        } else {
            player.sendMessage(ChatColor.RED + "Du hast keine Cookies in deinem Inventar?");
        }
    }

    @EventHandler
    public void onIngredientBlockBreak(BlockBreakEvent event) {
        TeamInfo team = plugin.getTeam(event.getPlayer());
        if(team == null)
            return;

        if(getDrops().contains(event.getBlock().getType().toString())) {
            event.setCancelled(true);
            if (event.getBlock().getState().getData().getData() != 0) {
                event.getBlock().getState().getData().setData((byte) 0);
            }
            event.getBlock().getDrops().stream().filter(this::isDrop).forEach(drop -> {
                event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), drop);
            });
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission("simpleteampvp.bypass"));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        event.setCancelled(true);
    }

    @Override
    public SimpleTeamPvPGame clone() {
        return new CookieWarsGame(plugin);
    }
}
