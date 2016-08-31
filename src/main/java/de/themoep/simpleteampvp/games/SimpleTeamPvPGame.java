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
import org.bukkit.event.entity.PlayerDeathEvent;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;


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

    private Objective pointObjective = null;
    private String objectiveDisplay = "";
    private GameState state;
    private boolean useKits = false;
    private boolean showScore = false;
    private boolean showScoreExp = false;
    private ItemStack pointItem = null;
    private int winScore = -1;
    private int duration = -1;
    private GameTimer timer = null;
    private Material pointBlock = Material.AIR;
    private Set<LocationInfo> pointBlockSet = new HashSet<LocationInfo>();
    private int regenId = -1;
    private BukkitTask teleportTask;
    private BukkitTask fwTask;

    public SimpleTeamPvPGame(SimpleTeamPvP plugin, String name) {
        this.plugin = plugin;
        this.name = name.toLowerCase();

        ConfigurationSection locSec = plugin.getConfig().getConfigurationSection("game." + name.toLowerCase() + ".pointitemchest");
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

        //pointItem = plugin.getConfig().getItemStack("game." + this.name + ".pointitem", null);
        duration = plugin.getConfig().getInt("game." + this.name + ".duration", 0);
        winScore = plugin.getConfig().getInt("game." + this.name + ".winscore", -1);
        try {
            pointBlock = Material.matchMaterial(plugin.getConfig().getString("game." + this.name + ".pointblock"));
        } catch (IllegalArgumentException e) {
            pointBlock = Material.AIR;
            plugin.getLogger().log(Level.WARNING, plugin.getConfig().getString("game." + this.name + ".pointblock", "null") + "game." + this.name + ".pointblock is not a valid Material name");
        }
        if(pointItem == null) {
            plugin.getLogger().log(Level.WARNING, "No point item configured!");
            pointItem = new ItemStack(pointBlock != Material.AIR ? pointBlock : Material.SLIME_BALL, 1);
            ItemMeta meta = pointItem.getItemMeta();
            meta.setDisplayName("Point Item");
            pointItem.setItemMeta(meta);
        }
        state = GameState.INITIATED;
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("teamPoints");
        if(pointObjective != null) {
            pointObjective.unregister();
        }
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("teamPoints", "dummy");
        setObjectiveDisplay("Points (%winscore%)");
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
        plugin.getLogger().log(Level.INFO, "plugin.getTeamMap().size(): " + plugin.getTeamMap().size());
        double perfectSize = totalPlayers / plugin.getTeamMap().size();

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
                    if(team.getSize() <= perfectSize)
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
                while (team.getSize() > perfectSize) {
                    String name = teamMates.get(teamMates.size() - 1);
                    Player player = plugin.getServer().getPlayer(name);
                    if (player == null)
                        continue;

                    team.removePlayer(player);
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
                if(team.getSize() <= perfectSize)
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
                    if(useKits) {
                        plugin.getKitGui().show(player);
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
                            player.teleport(player.getWorld().getSpawnLocation());
                            player.setBedSpawnLocation(player.getWorld().getSpawnLocation(), true);
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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getEntity().hasPermission("simpleteampvp.bypass"))
            return;

        TeamInfo team = plugin.getTeam(event.getEntity());
        if(team != null) {
            event.getEntity().setBedSpawnLocation(team.getSpawn().getLocation().add(0,1,0), true);
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
                        plugin.getKitGui().show(player);
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
