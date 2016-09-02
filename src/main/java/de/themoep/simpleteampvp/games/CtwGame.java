package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.KitInfo;
import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class CtwGame extends SimpleTeamPvPGame {

    private Objective carriedObjective;

    Set<LocationInfo> artificialWool = new HashSet<LocationInfo>();

    public CtwGame(SimpleTeamPvP plugin) {
        super(plugin, "ctw");
        showScore(true);
        useKits(true);
    }

    @Override
    public boolean start() {
        setObjectiveDisplay(ChatColor.WHITE + "Zeit: " + ChatColor.GREEN + "%time%" + ChatColor.WHITE);
        for (TeamInfo team : plugin.getTeamMap().values()) {
            team.getScoreboardTeam().setAllowFriendlyFire(false);
            team.getScoreboardTeam().setCanSeeFriendlyInvisibles(true);
            team.getScoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        plugin.getServer().getScoreboardManager().getMainScoreboard().clearSlot(DisplaySlot.PLAYER_LIST);

        carriedObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("carriedPoints");
        if(carriedObjective != null) {
            carriedObjective.unregister();
        }
        carriedObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("carriedPoints", "dummy");
        carriedObjective.setDisplayName("Wolle");
        carriedObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        return super.start();
    }

    @EventHandler
    public void onWoolBlockPlace(BlockPlaceEvent event) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        TeamInfo woolTeam = plugin.getTeam(event.getBlock());
        if (woolTeam == null) {
            return;
        }

        TeamInfo team = plugin.getTeam(event.getPlayer());
        if (team == null) {
            return;
        }

        if (team.equals(woolTeam) && !event.getPlayer().hasPermission("simpleteampvp.bypass")) {
            artificialWool.add(new LocationInfo(event.getBlock().getLocation()));
        }

        if (team.regionContains(event.getBlock().getLocation())) {
            incrementScore(team);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Du hast einen Wolleblock für dein Team hinzugefügt!");
        }

        decrementCarried(event.getPlayer(), 1);

    }

    @EventHandler(ignoreCancelled = true)
    public void onWoolBlockBreak(BlockBreakEvent event) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        TeamInfo woolTeam = plugin.getTeam(event.getBlock());
        if (woolTeam == null) {
            return;
        }

        TeamInfo team = plugin.getTeam(event.getPlayer());
        if (team == null) {
            return;
        }

        if (team.equals(woolTeam)
                && !event.getPlayer().hasPermission("simpleteampvp.bypass")
                && !artificialWool.contains(new LocationInfo(event.getBlock().getLocation()))
                ) {
            event.getPlayer().sendMessage(ChatColor.RED + "Du kannst die Wolle deines eigenen Teams nicht abbauen!");
            event.setCancelled(true);
            return;
        }

        for (TeamInfo t : plugin.getTeamMap().values()) {
            if (t.regionContains(event.getBlock().getLocation())) {
                decrementScore(t);
                if (!t.equals(team) && !t.equals(woolTeam)) {
                    plugin.broadcast(t, ChatColor.DARK_RED + "ACHTUNG: "
                            + ChatColor.YELLOW + event.getPlayer().getDisplayName()
                            + ChatColor.RED + " klaut gerade aus eurer Wollekammer!");
                }
                break;
            }
        }

    }

    @EventHandler
    public void onWoolBlockDrop(PlayerDropItemEvent event) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        TeamInfo woolTeam = plugin.getTeam(event.getItemDrop().getItemStack());
        if (woolTeam == null) {
            return;
        }

        TeamInfo team = plugin.getTeam(event.getPlayer());
        if (team == null) {
            return;
        }

        decrementCarried(event.getPlayer(), event.getItemDrop().getItemStack().getAmount());
    }

    @EventHandler
    public void onWoolBlockPickup(PlayerPickupItemEvent event) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        TeamInfo woolTeam = plugin.getTeam(event.getItem().getItemStack());
        if (woolTeam == null) {
            return;
        }

        TeamInfo team = plugin.getTeam(event.getPlayer());
        if (team == null) {
            return;
        }

        incrementCarried(event.getPlayer(), event.getItem().getItemStack().getAmount());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        TeamInfo team = plugin.getTeam(event.getEntity());
        if (team == null) {
            return;
        }

        resetCarried(event.getEntity());

        if (!event.getKeepInventory() && plugin.getKitMap().size() == 1) {
            KitInfo kit = plugin.getKitMap().values().iterator().next();
            event.setKeepInventory(true);
            for (ItemStack item : event.getEntity().getInventory().getContents()) {
                if (item != null &&
                        !item.isSimilar(kit.getHelmet()) &&
                        !item.isSimilar(kit.getChest()) &&
                        !item.isSimilar(kit.getLegs()) &&
                        !item.isSimilar(kit.getBoots())) {
                    event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), item);
                }
            }
        }
    }

    private void resetCarried(Player player) {
        carriedObjective.getScore(player.getName()).setScore(0);
    }

    private void decrementCarried(Player player, int amount) {
        Score score = carriedObjective.getScore(player.getName());
        score.setScore(score.getScore() - amount);
    }

    private void incrementCarried(Player player, int amount) {
        Score score = carriedObjective.getScore(player.getName());
        score.setScore(score.getScore() + amount);
    }

    @Override
    public SimpleTeamPvPGame clone() {
        return new CtwGame(plugin);
    }
}
