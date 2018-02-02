package de.themoep.simpleteampvp.games;

import de.themoep.servertags.bukkit.ServerInfo;
import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import de.themoep.simpleteampvp.Utils;
import lombok.Getter;
import net.blitzcube.mlapi.MultiLineAPI;
import net.blitzcube.mlapi.tag.TagController;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;


/**
 * SimpleTeamPvP
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * <p>
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public abstract class SimpleTeamPvPGame implements Listener {
    
    protected final SimpleTeamPvP plugin;
    @Getter
    private final String name;
    @Getter
    private final GameConfig config;
    protected TagController tagController;
    private GameState state = GameState.CREATED;
    private GameTimer timer = null;
    private int regenId = -1;
    private BukkitTask teleportTask;
    private BukkitTask fwTask;
    private Objective pointObjective = null;
    private Objective killStreakObjectiveTab = null;
    private Objective killStreakObjectiveName = null;
    private List<String> highestKillStreakPlayers = new ArrayList<>();
    private int highestKillStreakScore = 0;
    
    private Objective playerKillsObjective = null;
    
    @Getter
    private Set<LocationInfo> pointBlockSet = new HashSet<>();
    private Map<String, Integer> teamScores = new HashMap<>();
    
    public SimpleTeamPvPGame(SimpleTeamPvP plugin, String name) {
        this.plugin = plugin;
        this.name = name.toLowerCase();
        
        plugin.getLogger().log(Level.INFO, "Initializing " + name + " game");
        
        ConfigurationSection game = plugin.getConfig().getConfigurationSection("games." + name);
        config = new GameConfig(game);
        
        if (plugin.useMultiLineApi()) {
            tagController = () -> 0;
            MultiLineAPI.register(tagController);
        }
    }
    
    
    public void loadConfig() {
        config.load();
        
        if (config.getPointItemChestLocation() != null) {
            Location loc = config.getPointItemChestLocation().getLocation();
            if (loc != null) {
                Block block = loc.getBlock();
                if (block.getState() instanceof Chest) {
                    Chest chest = (Chest) block.getState();
                    ItemStack item = null;
                    for (ItemStack i : chest.getBlockInventory().getContents()) {
                        if (i != null) {
                            item = i;
                            break;
                        }
                    }
                    if (item != null) {
                        config.setPointItem(item);
                        plugin.getLogger().log(Level.INFO, "Point item is " + config.getPointItem().getType());
                    }
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Could not get location for LocationInfo " + config.getPointItemChestLocation() + ". Maybe the world doesn't exist anymore?");
            }
        }
        
        if (config.getPointItem() == null) {
            plugin.getLogger().log(Level.WARNING, "No point item configured!");
            config.setPointItem(new ItemStack(config.getPointBlock() != Material.AIR ? config.getPointBlock() : Material.SLIME_BALL, 1));
            ItemMeta meta = config.getPointItem().getItemMeta();
            meta.setDisplayName("Point Item");
            config.getPointItem().setItemMeta(meta);
        }
        
        state = GameState.INITIATED;
    }
    
    /**
     * Join players into this game
     * @return <tt>true</tt> if game is in GameState.INITIATED and players can be joined
     */
    public boolean join() {
        if (getState() != GameState.INITIATED)
            return false;
        
        for (TeamInfo team : config.getTeams().values()) {
            team.init();
        }
        
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spieler werden den Teams hinzugefügt...");
        
        state = GameState.JOINING;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(SimpleTeamPvP.BYPASS_PERM) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            
            if (getTeam(player) != null)
                continue;
            
            TeamInfo team = getTeamByJoinLocation(player.getLocation());
            if (team != null) {
                team.addPlayer(player);
            }
        }
        return true;
    }
    
    /**
     * Balance the teams
     * @return <tt>true</tt> if game is in GameState.JOINING and players can be balanced
     */
    public boolean balance() {
        if (getState() != GameState.JOINING)
            return false;
        
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Ausbalancieren und Auffüllen der Teams gestartet...");
        
        Map<Player, String> beforeBalance = new HashMap<>();
        List<Player> playersToJoin = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission(SimpleTeamPvP.BYPASS_PERM) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            TeamInfo team = getTeam(player);
            if (team == null) {
                if (config.getRandomRegion() == null || config.getRandomRegion().contains(player.getLocation()))
                    playersToJoin.add(player);
                beforeBalance.put(player, "");
            } else {
                beforeBalance.put(player, team.getName());
            }
        }
        plugin.getLogger().log(Level.INFO, "Players to join: " + playersToJoin.size());
        
        int totalPlayers = playersToJoin.size();
        for (TeamInfo team : config.getTeams().values()) {
            totalPlayers += team.getSize();
        }
        plugin.getLogger().log(Level.INFO, "Number of teams: " + config.getTeams().size());
        double perfectSize = (double) totalPlayers / (double) config.getTeams().size();
        
        plugin.getLogger().log(Level.INFO, "perfectSize: " + perfectSize);
        
        if (plugin.getServerTags() != null) {
            // Team key -> Tag
            Map<String, String> teamTags = new HashMap<>();
            
            for (TeamInfo team : config.getTeams().values()) {
                
                Map<String, Integer> tags = new HashMap<>();
                for (String playerName : team.getScoreboardTeam().getEntries()) {
                    Player player = plugin.getServer().getPlayer(playerName);
                    if (player == null)
                        continue;
                    
                    String tag = "no server";
                    ServerInfo serverInfo = plugin.getServerTags().getPlayerServer(player);
                    if (serverInfo != null) {
                        tag = serverInfo.getTag();
                    }
                    if (!tags.containsKey(tag)) {
                        tags.put(tag, 0);
                    }
                    tags.put(tag, tags.get(tag) + 1);
                }
                
                String teamTag = "no server";
                int tagCount = 0;
                for (Map.Entry<String, Integer> entry : tags.entrySet()) {
                    if (entry.getValue() > tagCount) {
                        tagCount = entry.getValue();
                        teamTag = entry.getKey();
                    }
                }
                
                teamTags.put(team.getName(), teamTag);
            }
            
            for (TeamInfo team : config.getTeams().values()) {
                // Filter out players that come from another server than the majority of the team
                // and remove them as long as the team is larger than the perfect size
                for (String playerName : team.getScoreboardTeam().getEntries()) {
                    if (team.getSize() <= perfectSize + 0.5)
                        break;
                    
                    Player player = plugin.getServer().getPlayer(playerName);
                    if (player == null)
                        continue;
                    
                    String tag = "no server";
                    ServerInfo serverInfo = plugin.getServerTags().getPlayerServer(player);
                    if (serverInfo != null) {
                        tag = serverInfo.getTag();
                    }
                    
                    if (tag.equals(teamTags.get(team.getName())))
                        continue;
                    
                    plugin.getLogger().log(Level.INFO, "[ST] Removed " + player.getName() + " from " + team.getName() + " (Step 1)");
                    
                    team.removePlayer(player);
                    playersToJoin.add(player);
                }
                
                // Team still larger than the perfect size? Remove last joined player
                Deque<String> teamMates = new ArrayDeque<>(team.getScoreboardTeam().getEntries());
                while (team.getSize() > perfectSize + 0.5) {
                    String name = teamMates.peekLast();
                    Player player = plugin.getServer().getPlayer(name);
                    if (player == null)
                        continue;
                    
                    team.removePlayer(player);
                    plugin.getLogger().log(Level.INFO, "[ST] Removed " + player.getName() + " from " + team.getName() + " (Step 2)");
                    teamMates.pollLast();
                    playersToJoin.add(player);
                }
            }
            
            // Add rest of players to teams from their server
            Iterator<Player> playerIterator = playersToJoin.iterator();
            while (playerIterator.hasNext()) {
                Player player = playerIterator.next();
                ServerInfo serverInfo = plugin.getServerTags().getPlayerServer(player);
                if (serverInfo != null && teamTags.containsValue(serverInfo.getTag())) {
                    for (TeamInfo team : config.getTeams().values()) {
                        if (team.getSize() < perfectSize - 0.5 && teamTags.containsKey(team.getName()) && teamTags.get(team.getName()).equals(serverInfo.getTag())) {
                            team.addPlayer(player);
                            plugin.getLogger().log(Level.INFO, "[ST] Added " + player.getName() + " to " + team.getName());
                            playerIterator.remove();
                            break;
                        }
                    }
                }
            }
            plugin.getLogger().log(Level.INFO, "Players to join after servertags: " + playersToJoin.size());
        }
        
        // Remove players from teams that have more than the perfect size
        for (TeamInfo team : config.getTeams().values()) {
            for (String playerName : team.getScoreboardTeam().getEntries()) {
                if (team.getSize() <= perfectSize + 0.5)
                    break;
                
                Player player = plugin.getServer().getPlayer(playerName);
                if (player == null)
                    continue;
                
                plugin.getLogger().log(Level.INFO, "Removed " + player.getName() + " from " + team.getName());
                
                team.removePlayer(player);
                playersToJoin.add(player);
            }
        }
        
        Iterator<Player> playerIterator = playersToJoin.iterator();
        for (TeamInfo team : config.getTeams().values()) {
            while (playerIterator.hasNext()) {
                if (team.getSize() >= perfectSize - 0.5)
                    break;
                
                Player player = playerIterator.next();
                team.addPlayer(player);
                plugin.getLogger().log(Level.INFO, "Added " + player.getName() + " to " + team.getName());
                playerIterator.remove();
            }
        }
        
        if (playerIterator.hasNext()) {
            plugin.getLogger().log(Level.INFO, "Adding " + playersToJoin.size() + " remaining players to teams according to their player count:");
            
            List<TeamInfo> teams = new ArrayList<>(config.getTeams().values());
            teams.sort((t1, t2) -> Integer.compare(t2.getSize(), t1.getSize()));
            
            for (TeamInfo team : teams) {
                while (playerIterator.hasNext()) {
                    if (team.getSize() > perfectSize)
                        break;
                    
                    Player player = playerIterator.next();
                    team.addPlayer(player);
                    plugin.getLogger().log(Level.INFO, "Added remaining player " + player.getName() + " to " + team.getName());
                    playerIterator.remove();
                }
            }
        }
        
        if (playerIterator.hasNext()) {
            plugin.getLogger().log(Level.INFO, "Adding " + playersToJoin.size() + " remaining players to totally random teams:");
            Random r = new Random();
            List<TeamInfo> teams = new ArrayList<>(config.getTeams().values());
            while (playerIterator.hasNext()) {
                Player player = playerIterator.next();
                TeamInfo team = teams.get(r.nextInt(teams.size()));
                team.addPlayer(player);
                plugin.getLogger().log(Level.INFO, "Added player " + player.getName() + " to " + team.getName() + " by random");
                playerIterator.remove();
            }
        }
        plugin.getLogger().log(Level.INFO, "All players joined! (" + playersToJoin.size() + ")");
        
        for (Map.Entry<Player, String> entry : beforeBalance.entrySet()) {
            TeamInfo team = getTeam(entry.getKey());
            if (team != null && !team.getName().equals(entry.getValue())) {
                Player player = null;
                for (Iterator<String> it = team.getScoreboardTeam().getEntries().iterator(); player == null && it.hasNext();) {
                    player = plugin.getServer().getPlayer(it.next());
                }
                if (player != null && team.getJoinRegion().contains(player.getLocation())) {
                    entry.getKey().teleport(player);
                } else {
                    entry.getKey().teleport(team.getJoinRegion().calculateMiddle().getLocation());
                }
            }
        }
        
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Teams ausbalanciert und aufgefüllt!");
        
        state = GameState.WAITING;
        return true;
    }
    
    /**
     * Starts the game and teleports all players into the arena
     * @return <tt>true</tt> if the game is in GameState.WAITING after players got joined
     */
    public boolean start() {
        if (getState() != GameState.WAITING)
            return false;
        
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("teamPoints");
        if (pointObjective != null) {
            try {
                pointObjective.unregister();
            } catch (IllegalStateException e) {
                plugin.getLogger().log(Level.WARNING, "Could not unregister point objective?", e);
            }
        }
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("teamPoints", "dummy");
        setObjectiveDisplay(config.getObjectiveDisplay());
        
        playerKillsObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("playerKills");
        if (playerKillsObjective != null) {
            try {
                playerKillsObjective.unregister();
            } catch (IllegalStateException e) {
                plugin.getLogger().log(Level.WARNING, "Could not unregister player kills objective?", e);
            }
        }
        playerKillsObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("playerKills", "playerKillCount");
        
        if (config.isKillStreakDisplayTab()) {
            killStreakObjectiveTab = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("killStreakTab");
            if (killStreakObjectiveTab != null) {
                try {
                    killStreakObjectiveTab.unregister();
                } catch (IllegalStateException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not unregister kill streak tab objective?", e);
                }
            }
            killStreakObjectiveTab = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("killStreakTab", "playerKillCount");
            killStreakObjectiveTab.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
        
        if (config.isKillStreakDisplayName()) {
            killStreakObjectiveName = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("killStreakName");
            if (killStreakObjectiveName != null) {
                try {
                    killStreakObjectiveName.unregister();
                } catch (IllegalStateException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not unregister kill streak name objective?", e);
                }
            }
            killStreakObjectiveName = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("killStreakName", "playerKillCount");
            killStreakObjectiveName.setDisplayName("Kills");
            killStreakObjectiveName.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        for (TeamInfo team : config.getTeams().values()) {
            
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + team.getScoreboardTeam().getPrefix() + team.getScoreboardTeam().getDisplayName() + team.getScoreboardTeam().getSuffix() + ChatColor.GREEN + ":");
            showPlayerList(team);
            
            Location spawnLocation = team.getSpawn() != null ? team.getSpawn().getLocation() :
                    team.getPoint() != null ? team.getPoint().getLocation() :
                            plugin.getServer().getWorlds().get(0).getSpawnLocation();
            spawnLocation = spawnLocation.add(0, 1, 0);
            for (String name : team.getScoreboardTeam().getEntries()) {
                Player player = plugin.getServer().getPlayer(name);
                if (player != null) {
                    player.getInventory().clear();
                    player.getInventory().setHelmet(null);
                    player.getInventory().setChestplate(null);
                    player.getInventory().setLeggings(null);
                    player.getInventory().setBoots(null);
                    player.setLevel(0);
                    player.setExp(0);
                    player.setHealth(player.getMaxHealth());
                    player.updateInventory();
                    if (config.isUsingKits()) {
                        if (plugin.getKitMap().size() > 1) {
                            plugin.getKitGui().show(player);
                        } else {
                            plugin.applyKit(plugin.getKitMap().values().iterator().next(), player);
                        }
                    }
                    player.teleport(spawnLocation);
                    player.setBedSpawnLocation(spawnLocation, true);
                }
            }
            pointObjective.getScore(team.getColor() + team.getName()).setScore(0);
        }
        state = GameState.RUNNING;
        if (config.isShowScore()) {
            pointObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spiel gestartet!");
        
        if (config.getDuration() > 0) {
            timer = new GameTimer(this);
            timer.start();
        }
        
        return true;
    }
    
    /**
     * Stop the game:
     * - Teleports every player to the world spawn
     * - Clears inventory
     * - Sets respawn point to world spawn
     * - Prints a list of all players in each team
     * - Removes all players from the teams
     */
    public void stop() {
        if (getState() == GameState.ENDED)
            return;
        
        if (timer != null) {
            timer.destroy();
        }
        
        state = GameState.ENDED;
        
        int maxScore = 0;
        
        final List<TeamInfo> winTeams = new ArrayList<>();
        for (TeamInfo team : config.getTeams().values()) {
            if (!team.isInitialised()) {
                continue;
            }
            int teamScore = getScore(team);
            if (teamScore > maxScore) {
                maxScore = teamScore;
                winTeams.clear();
            }
            if (teamScore >= maxScore) {
                winTeams.add(team);
            }
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + team.getScoreboardTeam().getPrefix() + team.getScoreboardTeam().getDisplayName() + team.getScoreboardTeam().getSuffix() + ChatColor.GREEN + (config.getWinScore() > 0 ? " - Score: " + ChatColor.RED + teamScore : ":"));
            showPlayerList(team);
            
            for (String entry : team.getScoreboardTeam().getEntries()) {
                Player player = plugin.getServer().getPlayer(entry);
                if (player != null && player.isDead()) {
                    player.spigot().respawn();
                }
                calculateHighestKillStreak(entry);
            }
        }
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spiel beendet!");
        
        List<String> winTeamNames = new ArrayList<String>();
        for (TeamInfo team : winTeams) {
            winTeamNames.add(team.getColor() + team.getName());
        }
        
        plugin.getServer().broadcastMessage("");
        if (winTeamNames.size() == 1) {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + winTeamNames.get(0) + ChatColor.GREEN + " hat das Spiel mit " + ChatColor.YELLOW + (maxScore == 1 ? "einem Punkt" : maxScore + " Punkten") + ChatColor.GREEN + " gewonnen!");
        } else if (winTeamNames.size() > 1) {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Teams " + winTeamNames.stream().collect(Collectors.joining(ChatColor.GREEN + ", ")) + ChatColor.GREEN + " haben das Spiel mit " + ChatColor.YELLOW + (maxScore == 1 ? "einem Punkt" : maxScore + " Punkten") + ChatColor.GREEN + " gewonnen!");
        } else {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Kein Team hat das Spiel gewonnen!");
        }
        plugin.getServer().broadcastMessage("");
        
        if (playerKillsObjective != null) {
            List<String> killScoreWinners = new ArrayList<>();
            int amount = 0;
            for (String entry : playerKillsObjective.getScoreboard().getEntries()) {
                Score killScore = playerKillsObjective.getScore(entry);
                if (killScore.getScore() >= amount) {
                    if (killScore.getScore() > amount) {
                        killScoreWinners.clear();
                        amount = killScore.getScore();
                    }
                    killScoreWinners.add(plugin.addServerTag(entry));
                }
            }
            if (amount > 0 && killScoreWinners.size() > 0) {
                plugin.getServer().broadcastMessage(ChatColor.GREEN + "Meiste Kills ("
                        + ChatColor.YELLOW + amount
                        + ChatColor.GREEN + "): " + killScoreWinners.stream().collect(Collectors.joining(", ")));
            }
        }
        
        if (highestKillStreakScore > 0) {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Höchste Killstreak ("
                    + ChatColor.YELLOW + highestKillStreakScore
                    + ChatColor.GREEN + "): " + ChatColor.WHITE + highestKillStreakPlayers.stream().collect(Collectors.joining(", ")));
        }
        
        fwTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (TeamInfo team : winTeams) {
                Color color = Utils.convertColor(team.getColor());
                FireworkMeta fwm = null;
                if (team.getPoint() != null) {
                    Firework fw = (Firework) team.getPoint().getLocation().getWorld().spawnEntity(team.getPoint().getLocation(), EntityType.FIREWORK);
                    fwm = fw.getFireworkMeta();
                    FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(color).with(FireworkEffect.Type.BALL).trail(true).build();
                    fwm.addEffect(effect);
                    fwm.setPower(0);
                    fw.setFireworkMeta(fwm);
                }
                if (team.getScoreboardTeam() != null) {
                    for (String entry : team.getScoreboardTeam().getEntries()) {
                        Player player = plugin.getServer().getPlayer(entry);
                        if (player != null && player.isOnline()) {
                            Firework fw = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
                            if (fwm == null) {
                                fwm = fw.getFireworkMeta();
                                FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(color).with(FireworkEffect.Type.BALL).trail(true).build();
                                fwm.addEffect(effect);
                                fwm.setPower(0);
                            }
                            fw.setFireworkMeta(fwm);
                        }
                    }
                }
            }
        }, 0, 10);
        
        teleportTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (TeamInfo team : config.getTeams().values()) {
                if (team.getScoreboardTeam() != null) {
                    for (String name1 : team.getScoreboardTeam().getEntries()) {
                        Player player = plugin.getServer().getPlayer(name1);
                        if (player != null) {
                            plugin.getKitGui().close(player);
                            team.getScoreboardTeam().removeEntry(name1);
                            player.getInventory().clear();
                            player.getInventory().setHelmet(null);
                            player.getInventory().setChestplate(null);
                            player.getInventory().setLeggings(null);
                            player.getInventory().setBoots(null);
                            player.setLevel(0);
                            player.setExp(0);
                            player.setHealth(player.getMaxHealth());
                            player.updateInventory();
                            player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                            player.setBedSpawnLocation(plugin.getServer().getWorlds().get(0).getSpawnLocation(), true);
                        }
                        team.getScoreboardTeam().removeEntry(name1);
                    }
                }
            }
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spieler zurück zum Spawn geportet!");
            
            HandlerList.unregisterAll(plugin.getGame());
            
            destroy();
            
            state = GameState.DESTROYED;
        }, 20 * 10);
        
        regenPointBlocks();
    }
    
    private void showPlayerList(TeamInfo team) {
        Validate.notNull(team.getScoreboardTeam(), "Team not initialised yet!");
        Set<String> teamPlayers = team.getScoreboardTeam().getEntries();
        if (plugin.getServerTags() != null) {
            teamPlayers = new LinkedHashSet<>();
            for (String name : team.getScoreboardTeam().getEntries()) {
                teamPlayers.add(plugin.addServerTag(name));
            }
        }
        plugin.getServer().broadcastMessage(ChatColor.WHITE + teamPlayers.stream().collect(Collectors.joining(", ")));
    }
    
    
    public void destroy() {
        if (teleportTask != null) {
            teleportTask.cancel();
        }
        if (fwTask != null) {
            fwTask.cancel();
        }
        for (Objective o : new Objective[]{pointObjective, killStreakObjectiveTab, killStreakObjectiveName, playerKillsObjective}) {
            if (o != null) {
                try {
                    o.unregister();
                } catch (IllegalStateException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not unregister objective " + o.getName() + " ?", e);
                }
            }
        }
        for (TeamInfo team : config.getTeams().values()) {
            team.unregister();
        }
        HandlerList.unregisterAll(this);
        state = GameState.DESTROYED;
    }
    
    public GameState setState(GameState state) {
        GameState oldState = this.state;
        this.state = state;
        return oldState;
    }
    
    public GameState getState() {
        return state;
    }
    
    /**
     * Set whether or not this game should use kits
     * @param use
     */
    protected void useKits(boolean use) {
        config.setUsingKits(use);
    }
    
    /**
     * Get whether or not this game uses kits
     * @return Whether or not this game uses kits
     */
    protected boolean isUsingKits() {
        return config.isUsingKits();
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isStopBuild()) {
            event.setCancelled(true);
        }
        
        if (!event.isCancelled() && config.isFilterDrops()) {
            List<ItemStack> drops = event.getBlock().getDrops().stream().filter(this::isWhitelisted).collect(Collectors.toList());
            if (drops.size() != event.getBlock().getDrops().size()) {
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                for (ItemStack drop : drops) {
                    event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), drop);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isStopBuild() || config.isFilterBreak() && !isWhitelisted(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isStopBuild() || config.isFilterPlace() && !isWhitelisted(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (state != GameState.RUNNING && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player || event.getDamager() instanceof Projectile || event.getDamager() instanceof Firework)) {
            return;
        }
        
        if (event.getEntity() instanceof Creature || event.getEntity() instanceof Player) {
            return;
        }
        
        if (config.isStopInteract()) {
            if (event.getDamager() instanceof Player) {
                event.setCancelled(!event.getDamager().hasPermission(SimpleTeamPvP.BYPASS_PERM));
            } else {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getRemover().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isStopInteract() || config.isStopBuild()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isStopInteract()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if ((event.getAction() == Action.PHYSICAL) && event.getClickedBlock().getType() == Material.SOIL) {
            event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM));
        }
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (config.isStopInteract() && event.getClickedBlock().getType() == Material.BED_BLOCK) {
                event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM));
            } else if (config.isStopContainerAccess() && event.getClickedBlock().getState() instanceof InventoryHolder) {
                event.setCancelled(event.isCancelled() || !event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM));
            }
        }
    }
    
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isFilterDrops() && !isWhitelisted(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isStopArmorChange() && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
        }
        
        if (event.getClickedInventory() != event.getWhoClicked().getInventory() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (config.isFilterDrops() && !(isWhitelisted(event.getCurrentItem()) && isWhitelisted(event.getCursor()))) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onItemDrop(CraftItemEvent event) {
        if (event.getWhoClicked().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (config.isFilterCrafting() && !isWhitelisted(event.getRecipe().getResult())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (player.hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        TeamInfo team = getTeam(player);
        if (team == null)
            return;
        
        player.setBedSpawnLocation(team.getSpawn().getLocation().add(0, 1, 0), true);
        
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent lastDamageCause = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
            if (lastDamageCause.getDamager() instanceof Player && lastDamageCause.getDamager() != player) {
                for (ItemStack drop : getDeathDrops(team, player)) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }
        
        if (!event.getKeepLevel() && config.isShowScoreExp()) {
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }
        
        if (config.isFilterDrops()) {
            if (event.getKeepInventory()) {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (isWhitelisted(item)) {
                        player.getInventory().setItem(i, null);
                        player.getLocation().getWorld().dropItem(player.getLocation(), item);
                    }
                }
            } else {
                Iterator<ItemStack> dropIterator = event.getDrops().iterator();
                while (dropIterator.hasNext()) {
                    ItemStack drop = dropIterator.next();
                    if (!isWhitelisted(drop)) {
                        dropIterator.remove();
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        TeamInfo team = getTeam(event.getPlayer());
        if (team != null) {
            if (isUsingKits()) {
                final Player player = event.getPlayer();
                // We need to wait a tick after respawning to show a chest gui
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getKitMap().size() > 1) {
                        plugin.getKitGui().show(player);
                    } else {
                        plugin.applyKit(plugin.getKitMap().values().iterator().next(), player);
                    }
                }, 1L);
            }
            if (config.isShowScoreExp()) {
                event.getPlayer().setLevel(getScore(team));
            }
            if (config.getRespawnResistance() > 0) {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, config.getRespawnResistance() * 20, 5, true));
            }
            
            calculateHighestKillStreak(event.getPlayer());
            if (config.isKillStreakDisplayName() && killStreakObjectiveName != null) {
                killStreakObjectiveName.getScore(event.getPlayer().getName()).setScore(0);
            }
            if (config.isKillStreakDisplayTab() && killStreakObjectiveTab != null) {
                killStreakObjectiveTab.getScore(event.getPlayer().getName()).setScore(0);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void logPointBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;
        
        if (event.getBlock().getType() == config.getPointBlock()) {
            addToPointBlockSet(event.getBlock().getLocation());
        }
    }
    
    public abstract SimpleTeamPvPGame clone();
    
    private int getScore(TeamInfo team) {
        if (teamScores.containsKey(team.getName())) {
            return teamScores.get(team.getName());
        }
        return 0;
    }
    
    public int incrementScore(TeamInfo team) {
        return incrementScore(team, 1);
    }
    
    public int incrementScore(TeamInfo team, int i) {
        return setScore(team, getScore(team) + i);
    }
    
    
    public int decrementScore(TeamInfo team) {
        return decrementScore(team, 1);
    }
    
    public int decrementScore(TeamInfo team, int i) {
        return setScore(team, getScore(team) - i);
    }
    
    private int setScore(TeamInfo team, int i) {
        teamScores.put(team.getName(), i);
        if (config.isShowScoreExp()) {
            for (String entry : team.getScoreboardTeam().getEntries()) {
                Player player = plugin.getServer().getPlayer(entry);
                if (player != null && player.isOnline()) {
                    player.setLevel(i);
                }
            }
        }
        Score score = pointObjective.getScore(team.getColor() + team.getName());
        score.setScore(i);
        
        if (config.getWinScore() > 0 && i >= config.getWinScore()) {
            stop();
        }
        
        return i;
    }
    
    public boolean isWhitelisted(ItemStack item) {
        return item == null || isWhitelisted(item.getType(), item.getData().getData());
    }
    
    private boolean isWhitelisted(Block block) {
        return block == null || isWhitelisted(block.getType(), block.getState().getData().getData());
    }
    
    public boolean isWhitelisted(Material type, int data) {
        return type == Material.AIR
                || config.getItemWhitelist().contains(type.toString())
                || config.getItemWhitelist().contains(type.toString() + ":" + data);
    }
    
    public List<ItemStack> getDeathDrops() {
        return config.getDeathDrops();
    }
    
    public List<ItemStack> getDeathDrops(TeamInfo team, Player player) {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack dropTemplate : getDeathDrops()) {
            ItemStack drop = dropTemplate.clone();
            if (drop.hasItemMeta()) {
                ItemMeta meta = drop.getItemMeta();
                
                if (meta.hasDisplayName()) {
                    meta.setDisplayName(meta.getDisplayName()
                            .replace("%teamColor%", team.getColor().toString())
                            .replace("%teamName%", team.getName())
                            .replace("%playerName%", player.getName())
                    );
                }
                
                if (meta.hasLore()) {
                    List<String> lore = new ArrayList<>();
                    for (String loreText : meta.getLore()) {
                        lore.add(loreText
                                .replace("%teamColor%", team.getColor().toString())
                                .replace("%teamName%", team.getName())
                                .replace("%playerName%", player.getName())
                        );
                    }
                    meta.setLore(lore);
                }
                drop.setItemMeta(meta);
            }
            drops.add(drop);
        }
        return drops;
    }
    
    public void setDuration(int duration) {
        config.setDuration(duration);
        if (timer != null) {
            timer.setTime(duration * 60);
        } else if (duration > 0 && state == GameState.RUNNING) {
            timer = new GameTimer(this);
            timer.start();
        }
    }
    
    public void setObjectiveDisplay(String format) {
        config.setObjectiveDisplay(format);
        setTimerDisplay(config.getDuration());
    }
    
    public void setTimerDisplay(int seconds) {
        pointObjective.setDisplayName(
                config.getObjectiveDisplay()
                        .replace("%winscore%", config.getWinScore() > 0 ? Integer.toString(config.getWinScore()) : "")
                        .replace("%time%", seconds >= 0 ? Utils.formatTime(seconds, TimeUnit.SECONDS) : "")
        );
    }
    
    public void addToPointBlockSet(LocationInfo loc) {
        pointBlockSet.add(loc);
    }
    
    public void addToPointBlockSet(Location loc) {
        pointBlockSet.add(new LocationInfo(loc));
    }
    
    public int regenPointBlocks() {
        final List<LocationInfo> fLocs = new ArrayList<LocationInfo>(pointBlockSet);
        pointBlockSet.clear();
        regenId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (fLocs.size() > 0) {
                Iterator<LocationInfo> locIt = fLocs.iterator();
                if (locIt.hasNext()) {
                    Location loc = locIt.next().getLocation();
                    Block block = loc.getBlock();
                    if (block.getType() == Material.AIR)
                        block.setType(config.getPointBlock());
                    locIt.remove();
                    return;
                }
            }
            plugin.getServer().getScheduler().cancelTask(regenId);
            regenId = -1;
        }, 1L, 1L);
        return fLocs.size();
    }
    
    private void calculateHighestKillStreak(Player player) {
        calculateHighestKillStreak(player.getName());
    }
    
    private void calculateHighestKillStreak(String name) {
        Objective killStreakObjective = null;
        if (config.isKillStreakDisplayName() && killStreakObjectiveName != null) {
            killStreakObjective = killStreakObjectiveName;
        } else if (config.isKillStreakDisplayTab() && killStreakObjectiveTab != null) {
            killStreakObjective = killStreakObjectiveTab;
        }
        if (killStreakObjective != null) {
            Score score = killStreakObjective.getScore(name);
            if (score != null && score.getScore() >= highestKillStreakScore) {
                if (score.getScore() > highestKillStreakScore) {
                    highestKillStreakPlayers.clear();
                    highestKillStreakScore = score.getScore();
                }
                highestKillStreakPlayers.add(plugin.addServerTag(name));
            }
        }
    }
    
    public String toString() {
        return "Game{key=" + name + ",state=" + state + ",config=" + config + "}";
    }
    
    public boolean setRandom(String type, LocationInfo loc) {
        switch (type) {
            case "pos1":
                config.getRandomRegion().setPos1(loc);
                break;
            case "pos2":
                config.getRandomRegion().setPos2(loc);
                break;
            default:
                return false;
        }
        plugin.getConfig().set("game." + name.toLowerCase() + "random." + type, loc.serialize());
        plugin.saveConfig();
        return true;
    }
    
    public TeamInfo addTeam(TeamInfo teamInfo) {
        plugin.getLogger().log(Level.INFO, "Added team " + teamInfo.getName());
        return config.getTeams().put(teamInfo.getName().toLowerCase(), teamInfo);
    }
    
    public TeamInfo getTeam(String teamName) {
        return config.getTeams().get(teamName.toLowerCase());
    }
    
    /**
     * Get the team by a block or item
     * @return The team, null of none found
     * @param mat
     * @param data
     */
    public TeamInfo getTeam(Material mat, byte data) {
        for (TeamInfo team : config.getTeams().values()) {
            if (team.getBlockMaterial() == mat && team.getBlockData() == data) {
                return team;
            }
        }
        return null;
    }
    
    /**
     * Get a team by its region
     * @param location The location to get the team by
     * @return The team, null of none found
     */
    public TeamInfo getTeamByRegion(Location location) {
        for (TeamInfo team : config.getTeams().values()) {
            if (team.getRegion().contains(location)) {
                return team;
            }
        }
        return null;
    }
    
    /**
     * Get the team by a block
     * @return The team, null of none found
     * @param block
     */
    public TeamInfo getTeam(Block block) {
        return getTeam(block.getType(), block.getState().getData().getData());
    }
    
    /**
     * Get the team by an item
     * @return The team, null of none found
     * @param item
     */
    public TeamInfo getTeam(ItemStack item) {
        return getTeam(item.getType(), item.getData().getData());
    }
    
    /**
     * Get the team a player is in
     * @param player
     * @return The team the player is in or null if he doesn't have one
     */
    public TeamInfo getTeam(Player player) {
        for (TeamInfo team : config.getTeams().values()) {
            if (team.inTeam(player)) {
                return team;
            }
        }
        return null;
    }
    
    /**
     * Get a team by its join region
     * @param location The location to get the team by
     * @return The team, null of none found
     */
    public TeamInfo getTeamByJoinLocation(Location location) {
        for (TeamInfo team : config.getTeams().values()) {
            if (team.getJoinRegion().contains(location)) {
                return team;
            }
        }
        return null;
    }
    
    public TeamInfo removeTeam(TeamInfo teamInfo) {
        String teamName = teamInfo.getName();
        config.getConfig().set("teams." + teamName, null);
        for (String entry : teamInfo.getScoreboardTeam().getEntries()) {
            Player player = plugin.getServer().getPlayer(entry);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GOLD + "Your team has been removed!");
            }
        }
        teamInfo.getScoreboardTeam().unregister();
        plugin.getLogger().log(Level.INFO, "Removed team " + teamName);
        plugin.saveConfig();
        return config.getTeams().remove(teamName.toLowerCase());
    }
    
    public Map<String, TeamInfo> getTeamMap() {
        return config.getTeams();
    }
    
    /**
     * Write the team info to the config
     * @param teamInfo The info to write
     */
    public void toConfig(TeamInfo teamInfo) {
        toConfig(teamInfo, true);
    }
    
    /**
     * Write the team info to the config
     * @param teamInfo The info to write
     * @param save     Whether or not we should write the config to disk
     */
    public void toConfig(TeamInfo teamInfo, boolean save) {
        plugin.getConfig().set("games." + getName() + ".teams." + teamInfo.getName(), teamInfo.serialize());
        plugin.getLogger().log(Level.INFO, "Saved team " + teamInfo.getName() + " to config!");
        if (save)
            plugin.saveConfig();
    }
}
