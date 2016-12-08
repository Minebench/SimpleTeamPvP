package de.themoep.simpleteampvp.games;

import de.themoep.servertags.bukkit.ServerInfo;
import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import de.themoep.simpleteampvp.Utils;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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
    private Objective killStreakObjectiveTab = null;
    private Objective killStreakObjectiveName = null;
    private List<String> highestKillStreakPlayers = new ArrayList<>();
    private int highestKillStreakScore = 0;

    private Objective playerKillsObjective = null;

    /* --- Settings --- */
    @GameConfigSetting(key = "use-kits")
    private boolean useKits = false;

    @GameConfigSetting(key = "show-score")
    private boolean showScore = false;

    @GameConfigSetting(key = "score-in-exp-bar")
    private boolean showScoreExp = false;

    @GameConfigSetting(key = "filter.drops")
    private boolean filterDrops = false;

    @GameConfigSetting(key = "filter.crafting")
    private boolean filterCrafting = false;

    @GameConfigSetting(key = "stop-build")
    private boolean stopBuild = false;

    @GameConfigSetting(key = "stop-interact")
    private boolean stopInteract = false;

    @GameConfigSetting(key = "kill-streak.name")
    private boolean killStreakDisplayName = false;

    @GameConfigSetting(key = "kill-streak.tab")
    private boolean killStreakDisplayTab = false;

    @GameConfigSetting(key = "respawn-resistance")
    private int respawnResistance = 5;

    @GameConfigSetting(key = "objective-display")
    private String objectiveDisplay = "Points (%winscore%)";

    @GameConfigSetting(key = "pointitem")
    private ItemStack pointItem = null;

    @GameConfigSetting(key = "filter.whitelist")
    private Set<String> itemWhitelist = new HashSet<>();

    @GameConfigSetting(key = "death-drops")
    private Set<ItemStack> deathDrops = new HashSet<>();

    @GameConfigSetting(key = "winscore")
    private int winScore = -1;

    @GameConfigSetting(key = "duration")
    private int duration = -1;

    @GameConfigSetting(key = "pointBlock")
    private Material pointBlock = Material.AIR;

    @GameConfigSetting(key = "pointitemchest")
    private LocationInfo pointItemChestLocation = null;

    private Set<LocationInfo> pointBlockSet = new HashSet<LocationInfo>();

    public SimpleTeamPvPGame(SimpleTeamPvP plugin, String name) {
        this.plugin = plugin;
        this.name = name.toLowerCase();

        plugin.getLogger().log(Level.INFO, "Initializing " + name + " game");

        ConfigurationSection game = plugin.getConfig().getConfigurationSection("game." + getName());
        if (game == null) {
            game = plugin.getConfig().createSection("game." + getName());
        }

        for (Field field : FieldUtils.getAllFields(getClass())) {
            if (field.isAnnotationPresent(GameConfigSetting.class)) {
                GameConfigSetting config = field.getAnnotation(GameConfigSetting.class);
                Type type = field.getGenericType();
                String typeName = field.getType().getSimpleName();
                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType)type;
                    String rawTypeName = pType.getRawType().getTypeName();
                    String typeArgName = pType.getActualTypeArguments()[0].getTypeName();
                    typeName = rawTypeName.substring(rawTypeName.lastIndexOf('.') + 1) + "<" + typeArgName.substring(typeArgName.lastIndexOf('.') + 1) + ">";
                }
                Object value, defValue = null;
                try {
                    value = defValue = field.get(this);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }

                switch (typeName) {
                    case "LocationInfo":
                        ConfigurationSection locSec = game.getConfigurationSection(config.key());
                        if(locSec != null) {
                            value = new LocationInfo(locSec);
                        }
                        break;
                    case "Collection<ItemStack>":
                    case "List<ItemStack>":
                    case "Set<ItemStack>":
                        for (String string : game.getStringList(config.key())) {
                            try {
                                int amount = 1;
                                String[] partsA = string.split(" ");
                                if (partsA.length > 1) {
                                    amount = Integer.parseInt(partsA[1]);
                                }
                                String[] partsB = partsA[0].split(":");
                                short damage = 0;
                                if (partsB.length > 1) {
                                    damage = Short.parseShort(partsB[1]);
                                }
                                plugin.getLogger().log(Level.INFO, "Adding " + string);
                                ((Collection<ItemStack>) value).add(new ItemStack(Material.valueOf(partsB[0].toUpperCase()), amount, damage));
                            } catch (NumberFormatException e) {
                                plugin.getLogger().log(Level.WARNING, string + " does contain an invalid number?");
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().log(Level.WARNING, string + " does not contain a valid Bukkit Material key?");
                            }
                        }
                        break;
                    case "Collection<String>":
                    case "List<String>":
                    case "Set<String>":
                        for (String string : game.getStringList(config.key())) {
                            plugin.getLogger().log(Level.INFO, "Adding " + string);
                            ((Collection<String>) value).add(string);
                        }
                        break;
                    case "ItemStack":
                        value = game.getItemStack(config.key(), (ItemStack) value);
                        break;
                    case "Material":
                        try {
                            value = Material.matchMaterial(game.getString(config.key(), value.toString()));
                        } catch (IllegalArgumentException e) {
                            pointBlock = Material.AIR;
                            plugin.getLogger().log(Level.WARNING, game.getString(config.key(), "null") + " is not a valid Material name!");
                        }
                        break;
                    case "String":
                        value = ChatColor.translateAlternateColorCodes('&', game.getString(config.key(), (String) value));
                    default:
                         value = game.get(config.key(), value);
                }
                if (value != null) {
                    if (!(value instanceof Boolean) || !value.equals(defValue)) {
                        try {
                            plugin.getLogger().log(Level.INFO, config.key().replace('-', ' ') + ": " + value);
                            field.set(this, value);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().log(Level.WARNING, "Can't set " + typeName + " " + field.getName() + " to " + value.getClass().getSimpleName() + " loaded from " + config.key());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    plugin.getLogger().log(Level.WARNING, config.key() + "'s value is null?");
                }
            }
        }

        if(pointItemChestLocation != null) {
            Location loc = pointItemChestLocation.getLocation();
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
                plugin.getLogger().log(Level.WARNING, "Could not get location for LocationInfo " + pointItemChestLocation + ". Maybe the world doesn't exist anymore?");
            }
        }

        if(pointItem == null) {
            plugin.getLogger().log(Level.WARNING, "No point item configured!");
            pointItem = new ItemStack(pointBlock != Material.AIR ? pointBlock : Material.SLIME_BALL, 1);
            ItemMeta meta = pointItem.getItemMeta();
            meta.setDisplayName("Point Item");
            pointItem.setItemMeta(meta);
        }

        state = GameState.INITIATED;
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
            if(player.hasPermission(SimpleTeamPvP.BYPASS_PERM) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
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
            if(player.hasPermission(SimpleTeamPvP.BYPASS_PERM) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
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

        if(plugin.getServerTags() != null) {
            // Team key -> Tag
            Map<String, String> teamTags = new HashMap<>();

            for(TeamInfo team : plugin.getTeamMap().values()) {

                Map<String, Integer> tags = new HashMap<>();
                for(String playerName : team.getScoreboardTeam().getEntries()) {
                    Player player = plugin.getServer().getPlayer(playerName);
                    if(player == null)
                        continue;

                    String tag = "no server";
                    ServerInfo serverInfo = plugin.getServerTags().getPlayerServer(player);
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
                    ServerInfo serverInfo = plugin.getServerTags().getPlayerServer(player);
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
                ServerInfo serverInfo = plugin.getServerTags().getPlayerServer(player);
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

        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("teamPoints");
        if(pointObjective != null) {
            try {
                pointObjective.unregister();
            } catch (IllegalStateException e) {
                plugin.getLogger().log(Level.WARNING, "Could not unregister point objective?", e);
            }
        }
        pointObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("teamPoints", "dummy");
        setObjectiveDisplay(objectiveDisplay);

        playerKillsObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("playerKills");
        if(playerKillsObjective != null) {
            try {
                playerKillsObjective.unregister();
            } catch (IllegalStateException e) {
                plugin.getLogger().log(Level.WARNING, "Could not unregister player kills objective?", e);
            }
        }
        playerKillsObjective = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("playerKills", "playerKillCount");

        if (killStreakDisplayTab) {
            killStreakObjectiveTab = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("killStreakTab");
            if(killStreakObjectiveTab != null) {
                try {
                    killStreakObjectiveTab.unregister();
                } catch (IllegalStateException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not unregister kill streak tab objective?", e);
                }
            }
            killStreakObjectiveTab = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("killStreakTab", "playerKillCount");
            killStreakObjectiveTab.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        if (killStreakDisplayName) {
            killStreakObjectiveName = plugin.getServer().getScoreboardManager().getMainScoreboard().getObjective("killStreakName");
            if(killStreakObjectiveName != null) {
                try {
                    killStreakObjectiveName.unregister();
                } catch (IllegalStateException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not unregister kill streak name objective?", e);
                }
            }
            killStreakObjectiveName = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewObjective("killStreakName", "playerKillCount");
            killStreakObjectiveName.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for(TeamInfo team : plugin.getTeamMap().values()) {

            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + team.getScoreboardTeam().getPrefix() + team.getScoreboardTeam().getDisplayName() + team.getScoreboardTeam().getSuffix() + ChatColor.GREEN + ":");
            showPlayerList(team);

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
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Team " + team.getScoreboardTeam().getPrefix() + team.getScoreboardTeam().getDisplayName() + team.getScoreboardTeam().getSuffix() + ChatColor.GREEN + (winScore > 0 ? " - Score: " + ChatColor.RED + team.getScore() : ":"));
            showPlayerList(team);

            for (String entry : team.getScoreboardTeam().getEntries()) {
                calculateHighestKillStreak(entry);
            }
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

        if (playerKillsObjective != null) {
            List<String> killScoreWinners = new ArrayList<>();
            int amount = 0;
            for (String entry : playerKillsObjective.getScoreboard().getEntries()) {
                Score killScore = playerKillsObjective.getScore(entry);
                if (killScore.getScore() >= amount) {
                    if (killScore.getScore() > amount) {
                        killScoreWinners.clear();
                    }
                    killScoreWinners.add(plugin.addServerTag(entry));
                }
            }
            if (killScoreWinners.size() > 0) {
                plugin.getServer().broadcastMessage(ChatColor.GREEN + "Meiste Kills ("
                        + ChatColor.YELLOW + amount
                        + ChatColor.GREEN + "): " + StringUtils.join(killScoreWinners, ", "));
            }
        }

        if (highestKillStreakScore > 0) {
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Höchste Killstreak ("
                    + ChatColor.YELLOW + highestKillStreakScore
                    + ChatColor.GREEN + "): " + StringUtils.join(highestKillStreakPlayers, ", "));
        }

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
                            player.spigot().respawn();
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

    private void showPlayerList(TeamInfo team) {
        Set<String> teamPlayers = team.getScoreboardTeam().getEntries();
        if(plugin.getServerTags() != null) {
            teamPlayers = new LinkedHashSet<>();
            for(String name : team.getScoreboardTeam().getEntries()) {
                teamPlayers.add(plugin.addServerTag(name));
            }
        }
        plugin.getServer().broadcastMessage(ChatColor.WHITE + StringUtils.join(teamPlayers, ", "));
    }


    protected void destroy() {
        teleportTask.cancel();
        fwTask.cancel();
        regenPointBlocks();
        for (Objective o : new Objective[]{pointObjective, killStreakObjectiveTab, killStreakObjectiveName, playerKillsObjective}) {
            if(o != null) {
                try {
                    o.unregister();
                } catch (IllegalStateException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not unregister objective " + o.getName() + " ?", e);
                }
            }
        }
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

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        if (stopBuild) {
            event.setCancelled(true);
        }

        if(!event.isCancelled() && filterDrops) {
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
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        if (stopBuild) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageEntity(EntityDamageByEntityEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (!(event.getEntity() instanceof Creature || event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        if (stopInteract && !player.hasPermission(SimpleTeamPvP.BYPASS_PERM)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEntityEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        if (stopInteract) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        if(filterDrops && !isWhitelisted(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(CraftItemEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        if(event.getWhoClicked().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        if(filterCrafting && !isWhitelisted(event.getRecipe().getResult())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(getState() != GameState.RUNNING)
            return;

        Player player = event.getEntity();

        if(player.hasPermission(SimpleTeamPvP.BYPASS_PERM))
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
        if(getState() != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
            return;

        TeamInfo team = plugin.getTeam(event.getPlayer());
        if(team != null) {
            if(isUsingKits()) {
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
            if(showScoreExp) {
                event.getPlayer().setLevel(team.getScore());
            }
            if (respawnResistance > 0) {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, respawnResistance * 20, 5, true));
            }

            calculateHighestKillStreak(event.getPlayer());
            if (killStreakDisplayName && killStreakObjectiveName != null) {
                killStreakObjectiveName.getScore(event.getPlayer().getName()).setScore(0);
            }
            if (killStreakDisplayTab && killStreakObjectiveTab != null) {
                killStreakObjectiveTab.getScore(event.getPlayer().getName()).setScore(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void logPointBlockBreak(BlockBreakEvent event) {
        if(state != GameState.RUNNING)
            return;

        if(event.getPlayer().hasPermission(SimpleTeamPvP.BYPASS_PERM))
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

    public Set<String> getItemWhitelist() {
        return itemWhitelist;
    }

    public boolean isWhitelisted(ItemStack item) {
        return getItemWhitelist().contains(item.getType().toString()) || getItemWhitelist().contains(item.getType().toString() + ":" + item.getDurability());
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

    public int getRespawnResistance() {
        return respawnResistance;
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

    private void calculateHighestKillStreak(Player player) {
        calculateHighestKillStreak(player.getName());
    }

    private void calculateHighestKillStreak(String name) {
        Objective killStreakObjective = null;
        if (killStreakDisplayName && killStreakObjectiveName != null) {
            killStreakObjective = killStreakObjectiveName;
        } else if (killStreakDisplayTab && killStreakObjectiveTab != null) {
            killStreakObjective = killStreakObjectiveTab;
        }
        if (killStreakObjective != null) {
            Score score = killStreakObjective.getScore(name);
            if (score != null && score.getScore() >= highestKillStreakScore) {
                if (score.getScore() > highestKillStreakScore) {
                    highestKillStreakPlayers.clear();
                }
                highestKillStreakPlayers.add(plugin.addServerTag(name));
            }
        }
    }

    public String toString() {
        return "Game{key=" + name + ",state=" + state + ",winScore=" + winScore + ",duration=" + duration + ",useKits=" + useKits + ",showScore=" + showScore + ",showScoreExp=" + showScoreExp + ",pointBlock=" + pointBlock + ",pointItem=" + pointItem + "}";
    }
}
