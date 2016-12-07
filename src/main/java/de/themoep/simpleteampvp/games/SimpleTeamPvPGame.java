package de.themoep.simpleteampvp.games;

import de.themoep.servertags.bukkit.ServerInfo;
import de.themoep.servertags.bukkit.ServerTags;
import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import de.themoep.simpleteampvp.Utils;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 *
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */
public abstract class SimpleTeamPvPGame implements Listener {

    protected final SimpleTeamPvP plugin;
    private final String name;
    private GameState state;
    private GameTimer timer = null;
    private int regenId = -1;
    private BukkitTask teleportTask;
    private BukkitTask fwTask;
    private Objective pointObjective = null;

    /* --- Settings --- */
    private boolean useKits;
    private boolean showScore = false;
    private boolean showScoreExp = false;
    private boolean filterDrops;
    private boolean stopBuild;
    private boolean stopInteract;
    private String objectiveDisplay = "";
    private ItemStack pointItem = null;
    private Set<String> drops = new HashSet<>();
    private Set<ItemStack> deathDrops = new HashSet<>();
    private int winScore = -1;
    private int duration = -1;
    private Material pointBlock = Material.AIR;
    private Set<LocationInfo> pointBlockSet = new HashSet<LocationInfo>();

    public SimpleTeamPvPGame(SimpleTeamPvP plugin, String name) {
        this.plugin = plugin;
        this.name = name.toLowerCase();

        plugin.getLogger().log(Level.INFO, "Initializing " + name + " game");

        ConfigurationSection game = plugin.getConfig().getConfigurationSection("game." + getName());
        if (game == null) {
            game = plugin.getConfig().createSection("game." + getName());
        }

        pointItem = game.getItemStack("pointitem");
        if (pointItem != null) {
            plugin.getLogger().log(Level.INFO, "Point item is " + pointItem);
        }
        ConfigurationSection locSec = game.getConfigurationSection("pointitemchest");
        if(locSec != null) {
            LocationInfo locInfo = new LocationInfo(locSec);
            Location loc = locInfo.getLocation();
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
                        pointItem = item;
                        plugin.getLogger().log(Level.INFO, "Point item is " + pointItem.getType());
                    }
                }
            } else {
                plugin.getLogger().log(Level.WARNING, "Could not get location for LocationInfo " + locInfo + ". Maybe the world doesn't exist anymore?");
            }
        }

        duration = game.getInt("duration", 0);
        plugin.getLogger().log(Level.INFO, "Duration: " + duration);
        winScore = game.getInt("winscore", -1);
        plugin.getLogger().log(Level.INFO, "Winscore: " + winScore);
        useKits = game.getBoolean("use-kits", false);
        plugin.getLogger().log(Level.INFO, "Use kits: " + useKits);
        showScore = game.getBoolean("show-score", false);
        plugin.getLogger().log(Level.INFO, "Show score: " + showScore);
        showScoreExp = game.getBoolean("score-in-exp-bar", false);
        plugin.getLogger().log(Level.INFO, "Show score in exp bar: " + showScoreExp);
        stopBuild = game.getBoolean("stop-build", false);
        plugin.getLogger().log(Level.INFO, "Stop build: " + filterDrops);
        stopInteract = game.getBoolean("stop-interact", false);
        plugin.getLogger().log(Level.INFO, "Stop interact: " + filterDrops);
        filterDrops = game.getBoolean("custom-death-drops", false);
        plugin.getLogger().log(Level.INFO, "Custom death drops: " + filterDrops);
        objectiveDisplay = ChatColor.translateAlternateColorCodes('&', game.getString("objective-display", "Points (%winscore%)"));
        plugin.getLogger().log(Level.INFO, "Objective display: " + objectiveDisplay);

        try {
            pointBlock = Material.matchMaterial(game.getString("pointblock"));
            plugin.getLogger().log(Level.INFO, "Point block: " + pointBlock);
        } catch (IllegalArgumentException e) {
            pointBlock = Material.AIR;
            plugin.getLogger().log(Level.WARNING, game.getString("pointblock", "null") + " is not a valid Material name for the point block");
        }
        if(pointItem == null) {
            plugin.getLogger().log(Level.WARNING, "No point item configured!");
            pointItem = new ItemStack(pointBlock != Material.AIR ? pointBlock : Material.SLIME_BALL, 1);
            ItemMeta meta = pointItem.getItemMeta();
            meta.setDisplayName("Point Item");
            pointItem.setItemMeta(meta);
        }

        plugin.getLogger().log(Level.INFO, "Loading Drops:");
        for (String drop : game.getStringList("drops")) {
            drops.add(drop.toUpperCase());
            plugin.getLogger().log(Level.INFO, "Added " + drop);
        }

        for (String deathDrop : game.getStringList("death-drops")) {
            plugin.getLogger().log(Level.INFO, "Loading DeathDrops...");
            try {
                int amount = 1;
                String[] partsA = deathDrop.split(" ");
                if (partsA.length > 1) {
                    amount = Integer.parseInt(partsA[1]);
                }
                String[] partsB = partsA[0].split(":");
                short damage = 0;
                if (partsB.length > 1) {
                    damage = Short.parseShort(partsB[1]);
                }
                deathDrops.add(new ItemStack(Material.valueOf(partsB[0].toUpperCase()), amount, damage));
                plugin.getLogger().log(Level.INFO, "Added " + deathDrop);
            } catch (NumberFormatException e) {
                plugin.getLogger().log(Level.WARNING, deathDrop + " does contain an invalid number?");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, deathDrop + " does not contain a valid Bukkit Material name?");
            }
        }

        state = GameState.INITIATED;
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("teamPoints");
        if(pointObjective != null) {
            try {
                pointObjective.unregister();
            } catch (IllegalStateException e) {
                // wat
            }
        }
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("teamPoints", "dummy");
        setObjectiveDisplay(objectiveDisplay);
    }

    /**
     * Join players into this game
     * @return <tt>true</tt> if game is in GameState.INITIATED and players can be joined
     */
    public boolean join() {
        if(getState() != GameState.INITIATED)
            return false;

        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spieler auf Plattformen werden den Teams hinzugefügt...");

        state = GameState.JOINING;
        for(Player player : plugin.getServer().getOnlinePlayers()) {
            if(player.hasPermission("simpleteampvp.bypass") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
                continue;

            if(plugin.getTeam(player) != null)
                continue;

            Block block = player.getLocation().getBlock();
            while(block.getType() == Material.AIR && block.getLocation().getBlockY() > 0) {
                block = player.getLocation().getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());
            }
            TeamInfo team = plugin.getTeam(block);
            if(team != null) {
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
        if(getState() != GameState.JOINING)
            return false;

        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Ausbalancieren und Auffüllen der Teams gestartet...");

        List<Player> playersToJoin = new ArrayList<Player>();
        for(Player player : plugin.getServer().getOnlinePlayers()) {
            if(player.hasPermission("simpleteampvp.bypass") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            if(plugin.getTeam(player) != null)
                continue;
            playersToJoin.add(player);
        }
        plugin.getLogger().log(Level.INFO, "Players to join: " + playersToJoin.size());

        int totalPlayers = playersToJoin.size();
        for(TeamInfo team : plugin.getTeamMap().values()) {
            totalPlayers += team.getSize();
        }
        plugin.getLogger().log(Level.INFO, "Number of teams: " + plugin.getTeamMap().size());
        double perfectSize = (double) totalPlayers / (double) plugin.getTeamMap().size();

        plugin.getLogger().log(Level.INFO, "perfectSize: " + perfectSize);

        if(plugin.getServer().getPluginManager().getPlugin("ServerTags") != null) {
            ServerTags serverTags = (ServerTags) plugin.getServer().getPluginManager().getPlugin("ServerTags");
            // Team name -> Tag
            Map<String, String> teamTags = new HashMap<String, String>();

            for(TeamInfo team : plugin.getTeamMap().values()) {

                Map<String, Integer> tags = new HashMap<String, Integer>();
                for(String playerName : team.getScoreboardTeam().getEntries()) {
                    Player player = plugin.getServer().getPlayer(playerName);
                    if(player == null)
                        continue;

                    String tag = "no server";
                    ServerInfo serverInfo = serverTags.getPlayerServer(player);
                    if(serverInfo != null) {
                        tag = serverInfo.getTag();
                    }
                    if(!tags.containsKey(tag)) {
                        tags.put(tag, 0);
                    }
                    tags.put(tag, tags.get(tag) + 1);
                }

                String teamTag = "no server";
                int tagCount = 0;
                for(Map.Entry<String, Integer> entry : tags.entrySet()) {
                    if(entry.getValue() > tagCount) {
                        teamTag = entry.getKey();
                    }
                }

                teamTags.put(team.getName(), teamTag);
            }

            for(TeamInfo team : plugin.getTeamMap().values()) {
                // Filter out players that come from another server than the majority of the team
                // and remove them as long as the team is larger than the perfect size
                for (String playerName : team.getScoreboardTeam().getEntries()){
                    if(team.getSize() <= perfectSize + 0.5)
                        break;

                    Player player = plugin.getServer().getPlayer(playerName);
                    if(player == null)
                        continue;

                    String tag = "no server";
                    ServerInfo serverInfo = serverTags.getPlayerServer(player);
                    if(serverInfo != null) {
                        tag = serverInfo.getTag();
                    }

                    if(tag.equals(teamTags.get(team.getName())))
                        continue;

                    plugin.getLogger().log(Level.INFO, "[ST] Removed " + player.getName() + " from " + team.getName());

                    team.removePlayer(player);
                    playersToJoin.add(player);
                    break;
                }

                // Team still larger than the perfect size? Remove last joined player
                List<String> teamMates = new ArrayList<String>(team.getScoreboardTeam().getEntries());
                while (team.getSize() > perfectSize + 0.5) {
                    String name = teamMates.get(teamMates.size() - 1);
                    Player player = plugin.getServer().getPlayer(name);
                    if (player == null)
                        continue;

                    team.removePlayer(player);
                    plugin.getLogger().log(Level.INFO, "[ST] Removed " + player.getName() + " from " + team.getName());
                    teamMates.remove(name);
                    playersToJoin.add(player);
                }
            }

            // Add rest of players to teams from their server
            Iterator<Player> playerIterator = playersToJoin.iterator();
            while(playerIterator.hasNext()) {
                Player player = playerIterator.next();
                ServerInfo serverInfo = serverTags.getPlayerServer(player);
                if(serverInfo != null && teamTags.containsValue(serverInfo.getTag())) {
                    for(TeamInfo team : plugin.getTeamMap().values()) {
                        if(team.getSize() < perfectSize && teamTags.containsKey(team.getName()) && teamTags.get(team.getName()).equals(serverInfo.getTag())) {
                            team.addPlayer(player);
                            plugin.getLogger().log(Level.INFO, "[ST] Added " + player.getName() + " to " + team.getName());
                            playerIterator.remove();
                            break;
                        }
                    }
                }
            }
        }
        plugin.getLogger().log(Level.INFO, "Players to join after servertags: " + playersToJoin.size());

        // Remove players from teams that have more than the perfect size
        for(TeamInfo team : plugin.getTeamMap().values()) {
            for (String playerName : team.getScoreboardTeam().getEntries()){
                if(team.getSize() <= perfectSize + 0.5)
                    break;

                Player player = plugin.getServer().getPlayer(playerName);
                if(player == null)
                    continue;

                plugin.getLogger().log(Level.INFO, "Removed " + player.getName() + " from " + team.getName());

                team.removePlayer(player);
                playersToJoin.add(player);
                break;
            }
        }

        Iterator<Player> playerIterator = playersToJoin.iterator();
        while(playerIterator.hasNext()) {
            Player player = playerIterator.next();
            for(TeamInfo team : plugin.getTeamMap().values()) {
                if(team.getSize() < perfectSize) {
                    team.addPlayer(player);
                    plugin.getLogger().log(Level.INFO, "Added " + player.getName() + " to " + team.getName());
                    playerIterator.remove();
                    break;
                }
            }
        }

        List<TeamInfo> teams = new ArrayList<TeamInfo>(plugin.getTeamMap().values());
        Random random = new Random();
        for(Player player : playersToJoin) {
            TeamInfo team = teams.get(random.nextInt(teams.size()));
            while (team.getSize() == 0) {
                team = teams.get(random.nextInt(teams.size()));
            }
            team.addPlayer(player);
            plugin.getLogger().log(Level.INFO, "Added " + player.getName() + " to " + team.getName() + " by random!");
        }
        plugin.getLogger().log(Level.INFO, "All players joined! (" + playersToJoin.size() + ")");

        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Teams ausbalanciert und aufgefüllt!");

        state = GameState.WAITING;
        return true;
    }

    /**
     * Starts the game and teleports all players into the arena
     * @return <tt>true</tt> if the game is in GameState.WAITING after players got joined
     */
    public boolean start() {
        if(getState() != GameState.WAITING)
            return false;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for(TeamInfo team : plugin.getTeamMap().values()) {
            Set<String> teamPlayers = team.getScoreboardTeam().getEntries();
            if(plugin.getServer().getPluginManager().isPluginEnabled("ServerTags")) {
                teamPlayers = new LinkedHashSet<String>();
                ServerTags serverTags = (ServerTags) plugin.getServer().getPluginManager().getPlugin("ServerTags");
                for(String name : team.getScoreboardTeam().getEntries()) {
                    Player player = plugin.getServer().getPlayer(name);
                    name = ChatColor.WHITE + name + ChatColor.GRAY;
                    if(player != null) {
                        ServerInfo server = serverTags.getPlayerServer(player);
                        if(server != null && !server.getTag().isEmpty()) {
                            name += " (" + server.getTag() + ")";
                        }
                    }
                    teamPlayers.add(name);
                }
            }

            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + team.getScoreboardTeam().getPrefix() + team.getScoreboardTeam().getDisplayName() + team.getScoreboardTeam().getSuffix() + ChatColor.GREEN + ":");
            plugin.getServer().broadcastMessage(ChatColor.WHITE + StringUtils.join(teamPlayers, ", "));
            Location spawnLocation = team.getSpawn() != null ? team.getSpawn().getLocation() :
                    team.getPoint() != null ? team.getPoint().getLocation() :
                            plugin.getServer().getWorlds().get(0).getSpawnLocation();
            spawnLocation = spawnLocation.add(0, 1, 0);
            for(String name : team.getScoreboardTeam().getEntries()) {
                Player player = plugin.getServer().getPlayer(name);
                if(player != null) {
                    player.getInventory().clear();
                    player.getInventory().setHelmet(null);
                    player.getInventory().setChestplate(null);
                    player.getInventory().setLeggings(null);
                    player.getInventory().setBoots(null);
                    player.setLevel(0);
                    player.setExp(0);
                    player.setHealth(player.getMaxHealth());
                    player.updateInventory();
                    if(useKits) {
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
            Score score = pointObjective.getScore(team.getColor() + team.getName());
            score.setScore(0);
        }
        state = GameState.RUNNING;
        if(showScore) {
            pointObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spiel gestartet!");

        if(duration > 0) {
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
        if(getState() == GameState.ENDED)
            return;

        if(timer != null) {
            timer.destroy();
        }

        state = GameState.ENDED;

        int maxScore = 0;

        final List<TeamInfo> winTeams = new ArrayList<TeamInfo>();
        for(TeamInfo team : plugin.getTeamMap().values()) {
            if(team.getScore() > maxScore) {
                maxScore = team.getScore();
                winTeams.clear();
            }
            if(team.getScore() >= maxScore) {
                winTeams.add(team);
            }
            Set<String> teamPlayers = team.getScoreboardTeam().getEntries();
            if(plugin.getServer().getPluginManager().isPluginEnabled("ServerTags")) {
                teamPlayers = new LinkedHashSet<String>();
                ServerTags serverTags = (ServerTags) plugin.getServer().getPluginManager().getPlugin("ServerTags");
                for(String name : team.getScoreboardTeam().getEntries()) {
                    Player player = plugin.getServer().getPlayer(name);
                    name = ChatColor.WHITE + name + ChatColor.GRAY;
                    if(player != null) {
                        ServerInfo server = serverTags.getPlayerServer(player);
                        if(server != null && !server.getTag().isEmpty()) {
                            name += " (" + server.getTag() + ")";
                        }
                    }
                    teamPlayers.add(name);
                }
            }
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + team.getScoreboardTeam().getPrefix() + team.getScoreboardTeam().getDisplayName() + team.getScoreboardTeam().getSuffix() + ChatColor.GREEN + (winScore > 0 ? " - Score: " + ChatColor.RED + team.getScore() : ":"));
            plugin.getServer().broadcastMessage(ChatColor.WHITE + StringUtils.join(teamPlayers, ", "));

        }
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spiel beendet!");

        List<String> winTeamNames = new ArrayList<String>();
        for(TeamInfo team : winTeams) {
            winTeamNames.add(team.getColor() + team.getName());
        }

        plugin.getServer().broadcastMessage("");
        if(winTeamNames.size() == 1) {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + winTeamNames.get(0) + ChatColor.GREEN + " hat das Spiel mit " + ChatColor.YELLOW + (maxScore == 1 ? "einem Punkt" : maxScore + " Punkten") + ChatColor.GREEN + " gewonnen!");
        } else if(winTeamNames.size() > 1){
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Teams " + StringUtils.join(winTeamNames, ChatColor.GREEN + ", ") + ChatColor.GREEN + " haben das Spiel mit " + ChatColor.YELLOW + (maxScore == 1 ? "einem Punkt" : maxScore + " Punkten") + ChatColor.GREEN + " gewonnen!");
        } else {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Kein Team hat das Spiel gewonnen!");
        }
        plugin.getServer().broadcastMessage("");

        fwTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            public void run() {
                for(TeamInfo team : winTeams) {
                    Color color = Utils.convertColor(team.getColor());
                    FireworkMeta fwm = null;
                    if(team.getPoint() != null) {
                        Firework fw = (Firework) team.getPoint().getLocation().getWorld().spawnEntity(team.getPoint().getLocation(), EntityType.FIREWORK);
                        fwm = fw.getFireworkMeta();
                        FireworkEffect effect = FireworkEffect.builder().flicker(true).withColor(color).with(FireworkEffect.Type.BALL).trail(true).build();
                        fwm.addEffect(effect);
                        fwm.setPower(0);
                        fw.setFireworkMeta(fwm);
                    }
                    for(String entry : team.getScoreboardTeam().getEntries()) {
                        Player player = plugin.getServer().getPlayer(entry);
                        if(player != null && player.isOnline()) {
                            Firework fw = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
                            if(fwm == null) {
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

        teleportTask = plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            public void run() {
                for(TeamInfo team : plugin.getTeamMap().values()) {
                    for(String name : team.getScoreboardTeam().getEntries()) {
                        Player player = plugin.getServer().getPlayer(name);
                        if(player != null) {
                            team.getScoreboardTeam().removeEntry(name);
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
                    }
                }
                plugin.getServer().broadcastMessage(ChatColor.GREEN + "Spieler zurück zum Spawn geportet!");

                HandlerList.unregisterAll(plugin.getGame());

                destroy();

                state = GameState.DESTROYED;
            }
        }, 20 * 10);
    }


    protected void destroy() {
        teleportTask.cancel();
        fwTask.cancel();
        regenPointBlocks();
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
        useKits = use;
    }

    /**
     * Get whether or not this game uses kits
     * @return Whether or not this game uses kits
     */
    protected boolean isUsingKits() {
        return useKits;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if (stopBuild) {
            event.setCancelled(true);
        }

        if(!event.isCancelled() && filterDrops) {
            List<ItemStack> drops = event.getBlock().getDrops().stream().filter(this::isDrop).collect(Collectors.toList());
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
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if (stopBuild) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if (stopInteract) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if(filterDrops && !isDrop(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        Player player = event.getEntity();

        if(player.hasPermission("simpleteampvp.bypass"))
            return;

        TeamInfo team = plugin.getTeam(player);
        if(team == null)
            return;

        player.setBedSpawnLocation(team.getSpawn().getLocation().add(0,1,0), true);

        for (ItemStack drop : getDeathDrops(team, player)) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }

        if(!event.getKeepLevel() && showScoreExp) {
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }

        if (filterDrops) {
            if (event.getKeepInventory()) {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (isDrop(item)) {
                        player.getInventory().setItem(i, null);
                        player.getLocation().getWorld().dropItem(player.getLocation(), item);
                    }
                }
            } else {
                Iterator<ItemStack> dropIterator = event.getDrops().iterator();
                while (dropIterator.hasNext()) {
                    ItemStack drop = dropIterator.next();
                    if (!isDrop(drop)) {
                        dropIterator.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        TeamInfo team = plugin.getTeam(event.getPlayer());
        if(team != null) {
            if(isUsingKits()) {
                final Player player = event.getPlayer();
                // We need to wait a tick after respawning to show a chest gui
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                    public void run() {
                        if (plugin.getKitMap().size() > 1) {
                            plugin.getKitGui().show(player);
                        } else {
                            plugin.applyKit(plugin.getKitMap().values().iterator().next(), player);
                        }
                    }
                }, 1L);
            }
            if(showScoreExp) {
                event.getPlayer().setLevel(team.getScore());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void logPointBlockBreak(BlockBreakEvent event) {
        if(state != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission("simpleteampvp.bypass"))
            return;

        if(event.getBlock().getType() == getPointBlock()) {
            addToPointBlockSet(event.getBlock().getLocation());
        }
    }

    public abstract SimpleTeamPvPGame clone();

    public int incrementScore(TeamInfo team) {
        return incrementScore(team, 1);
    }

    public int incrementScore(TeamInfo team, int i) {
        return setScore(team, team.getScore() + i);
    }

    public int decrementScore(TeamInfo team) {
        return decrementScore(team, 1);
    }

    public int decrementScore(TeamInfo team, int i) {
        return setScore(team, team.getScore() - i);
    }

    private int setScore(TeamInfo team, int i) {
        team.setScore(i);
        if(showScoreExp) {
            for(String entry : team.getScoreboardTeam().getEntries()) {
                Player player = plugin.getServer().getPlayer(entry);
                if(player != null && player.isOnline()) {
                    player.setLevel(i);
                }
            }
        }
        Score score = pointObjective.getScore(team.getColor() + team.getName());
        score.setScore(i);

        if(winScore > 0 && team.getScore() >= winScore) {
            stop();
        }

        return team.getScore();
    }

    public void showScore(boolean showScore) {
        this.showScore = showScore;
    }

    public void showScoreExp(boolean showScoreExp) {
        this.showScoreExp = showScoreExp;
    }

    public ItemStack getPointItem() {
        return pointItem;
    }

    public void setPointItem(ItemStack pointItem) {
        this.pointItem = pointItem;
    }

    public Set<String> getDrops() {
        return drops;
    }

    public boolean isDrop(ItemStack item) {
        return item.getData() == null || item.getData().getData() == 0
                ? getDrops().contains(item.getType().toString())
                : getDrops().contains(item.getType().toString() + ":" + item.getData().getData());
    }

    public Set<ItemStack> getDeathDrops() {
        return deathDrops;
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

    public Objective getPointObjective() {
        return pointObjective;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        if(timer != null) {
            timer.setTime(duration * 60);
        } else if(duration > 0 && state == GameState.RUNNING) {
            timer = new GameTimer(this);
            timer.start();
        }
    }

    public int getWinScore() {
        return winScore;
    }

    public void setWinScore(int score) {
        this.winScore = score;
    }

    public void setObjectiveDisplay(String format) {
        this.objectiveDisplay = format;
        setTimerDisplay(duration);
    }

    public void setTimerDisplay(int seconds) {
        pointObjective.setDisplayName(
                objectiveDisplay
                .replace("%winscore%", winScore > 0 ? Integer.toString(winScore) : "")
                .replace("%time%", seconds >= 0 ? Utils.formatTime(seconds, TimeUnit.SECONDS) : "")
        );
    }

    public String getName() {
        return name;
    }

    public void setPointBlock(Material pointBlock) {
        this.pointBlock = pointBlock;
    }

    public Material getPointBlock() {
        return pointBlock;
    }

    public Set<LocationInfo> getPointBlockSet() {
        return pointBlockSet;
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
        regenId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                if(fLocs.size() > 0) {
                    Iterator<LocationInfo> locIt = fLocs.iterator();
                    if(locIt.hasNext()) {
                        Location loc = locIt.next().getLocation();
                        Block block = loc.getBlock();
                        if(block.getType() == Material.AIR)
                            block.setType(pointBlock);
                        locIt.remove();
                        return;
                    }
                }
                plugin.getServer().getScheduler().cancelTask(regenId);
                regenId = -1;
            }
        }, 1L, 1L);
        return fLocs.size();
    }

    public String toString() {
        return "Game{name=" + name + ",state=" + state + ",winScore=" + winScore + ",duration=" + duration + ",useKits=" + useKits + ",showScore=" + showScore + ",showScoreExp=" + showScoreExp + ",pointBlock=" + pointBlock + ",pointItem=" + pointItem + "}";
    }
}
