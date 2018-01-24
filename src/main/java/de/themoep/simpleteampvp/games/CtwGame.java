package de.themoep.simpleteampvp.games;

import de.themoep.simpleteampvp.KitInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import net.blitzcube.mlapi.MultiLineAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

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

    Set<Integer> artificialWool = new HashSet<Integer>();

    public CtwGame(SimpleTeamPvP plugin) {
        super(plugin, "ctw");
        showScore(true);
        useKits(true);
    }

    @Override
    public boolean start() {
        for (TeamInfo team : plugin.getTeamMap().values()) {
            team.getScoreboardTeam().setAllowFriendlyFire(false);
            team.getScoreboardTeam().setCanSeeFriendlyInvisibles(true);
            team.getScoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }
        plugin.getServer().getScoreboardManager().getMainScoreboard().clearSlot(DisplaySlot.PLAYER_LIST);

        if (!plugin.useMultiLineApi()) {
            carriedObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("carriedPoints");
            if (carriedObjective != null) {
                try {
                    carriedObjective.unregister();
                } catch (IllegalStateException e) {
                    // wat
                }
            }
            carriedObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("carriedPoints", "dummy");
            carriedObjective.setDisplayName("Wolle");
            carriedObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
        return super.start();
    }

    @Override
    public void stop() {
        super.stop();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            resetCarried(player);
        }
        if (carriedObjective != null) {
            try {
                carriedObjective.unregister();
            } catch (IllegalStateException e) {
                // wat
            }
            carriedObjective = null;
        }
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

        if (!event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM)) {
            artificialWool.add(event.getBlock().getLocation().hashCode());
        }

        if (team != woolTeam && team.regionContains(event.getBlock().getLocation())) {
            incrementScore(team);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Du hast einen Wolleblock für dein Team hinzugefügt!");
        }

        updateCarried(event.getPlayer());

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
                && !event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM)
                && !artificialWool.contains(event.getBlock().getLocation().hashCode())
                ) {
            event.getPlayer().sendMessage(ChatColor.RED + "Du kannst die Wolle deines eigenen Teams nicht abbauen!");
            event.setCancelled(true);
            return;
        }

        for (TeamInfo t : plugin.getTeamMap().values()) {
            if (t.regionContains(event.getBlock().getLocation())) {
                if (woolTeam != t) {
                    decrementScore(t);
                }
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
    public void onWoolBlockExplode(BlockExplodeEvent event) {
        handleBlocks(event.blockList());
    }

    @EventHandler
    public void onWoolBlockExplode(EntityExplodeEvent event) {
        handleBlocks(event.blockList());
    }

    private void handleBlocks(List<Block> blocklist) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        TeamInfo informTeam = null;

        for (Block block : blocklist) {
            TeamInfo woolTeam = plugin.getTeam(block);
            if (woolTeam == null) {
                continue;
            }

            for (TeamInfo t : plugin.getTeamMap().values()) {
                if (t.regionContains(block.getLocation())) {
                    decrementScore(t);
                    informTeam = t;
                    break;
                }
            }
        }

        if (informTeam != null) {
            plugin.broadcast(informTeam, ChatColor.DARK_RED + "ACHTUNG: "
                    + ChatColor.RED + " Eure Wollkammer ist in die Luft geflogen!");
        }
    }

    @EventHandler
    public void onWoolPistonPush(BlockPistonExtendEvent event) {
        handlePistonEvent(event);
    }
    @EventHandler
    public void onWoolPistonPush(BlockPistonRetractEvent event) {
        handlePistonEvent(event);
    }

    private void handlePistonEvent(BlockPistonEvent event) {
        if (getState() != GameState.RUNNING) {
            return;
        }

        for (TeamInfo t : plugin.getTeamMap().values()) {
            if (t.regionContains(event.getBlock().getLocation()) || t.regionContains(event.getBlock().getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onWoolDye(CraftItemEvent event) {
        if (plugin.getTeam(event.getRecipe().getResult()) != null) {
            event.setResult(Event.Result.DENY);
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

        updateCarried(event.getPlayer());
    }
    
    @EventHandler
    public void onWoolBlockPickup(EntityPickupItemEvent event) {
        if (getState() != GameState.RUNNING || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        TeamInfo woolTeam = plugin.getTeam(event.getItem().getItemStack());
        if (woolTeam == null) {
            return;
        }
        
        updateCarried((Player) event.getEntity());
    }
    
    @EventHandler
    public void onWoolBlockPrepareCraft(PrepareItemCraftEvent event) {
        if (getState() != GameState.RUNNING || event.getInventory().getResult() == null) {
            return;
        }
        
        TeamInfo woolTeam = plugin.getTeam(event.getInventory().getResult());
        if (woolTeam == null) {
            return;
        }
        event.getInventory().setResult(null);
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onWoolBlockCraft(CraftItemEvent event) {
        if (getState() != GameState.RUNNING || event.getCurrentItem() == null) {
            return;
        }
        
        TeamInfo woolTeam = plugin.getTeam(event.getCurrentItem());
        if (woolTeam == null) {
            return;
        }
        event.setCancelled(true);
        event.setCurrentItem(null);
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
                if (item != null && !kit.isArmor(item)) {
                    event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), item);
                }
            }
            event.getEntity().getInventory().clear();
        }
    }

    private void resetCarried(Player player) {
        if (plugin.useMultiLineApi()) {
            MultiLineAPI.clearLines(tagController, player);
        } else if (carriedObjective != null) {
            carriedObjective.getScore(player.getName()).setScore(0);
        }
    }

    private void updateCarried(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            TeamInfo team = plugin.getTeam(player);
            if (team == null) {
                return;
            }

            // Create two sets that contain the materials to check the inventory for
            Set<Material> teamMaterials = new HashSet<>();
            Set<Byte> teamData = new HashSet<>();
            for (TeamInfo t : plugin.getTeamMap().values()) {
                teamMaterials.add(t.getBlockMaterial());
                teamData.add(t.getBlockData());
            }

            int amount = 0;
            for (ItemStack item : player.getInventory()) {
                if (item != null && teamMaterials.contains(item.getType()) && teamData.contains(item.getData().getData())) {
                    amount += item.getAmount();
                }
            }

            if (plugin.useMultiLineApi()) {
                if (MultiLineAPI.getLineCount(tagController, player) < 1) {
                    MultiLineAPI.addLine(tagController, player);
                }
                MultiLineAPI.getLine(tagController, player, 0).setText("Trägt Wolle");
            } else if (carriedObjective != null) {
                Score score = carriedObjective.getScore(player.getName());
                score.setScore(amount);
            }
        });
    }

    @Override
    public SimpleTeamPvPGame clone() {
        return new CtwGame(plugin);
    }
}
