package de.themoep.simpleteampvp;

import de.themoep.servertags.bukkit.ServerInfo;
import de.themoep.servertags.bukkit.ServerTags;
import de.themoep.simpleteampvp.commands.GameSubCommand;
import de.themoep.simpleteampvp.commands.KitSubCommand;
import de.themoep.simpleteampvp.commands.PluginCommandExecutor;
import de.themoep.simpleteampvp.commands.AdminSubCommand;
import de.themoep.simpleteampvp.commands.TeamSubCommand;
import de.themoep.simpleteampvp.games.GameState;
import de.themoep.simpleteampvp.games.SimpleTeamPvPGame;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

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

public class SimpleTeamPvP extends JavaPlugin {
    
    public static final String BYPASS_PERM = "simpleteampvp.bypass";
    private Map<String, KitInfo> kitMap;
    private SimpleTeamPvPGame game = null;
    private KitGui kitGui;
    private Map<String, SimpleTeamPvPGame> gameMap;
    private ServerTags serverTags = null;
    private boolean useMultiLineApi = false;
    
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("ServerTags")) {
            serverTags = (ServerTags) getServer().getPluginManager().getPlugin("ServerTags");
        }
        saveDefaultConfig();
        loadConfig();
        PluginCommandExecutor comEx = new PluginCommandExecutor(this);
        comEx.register(new AdminSubCommand(this));
        comEx.register(new GameSubCommand(this));
        comEx.register(new KitSubCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }
    
    public void onDisable() {
        for (SimpleTeamPvPGame game : gameMap.values()) {
            game.destroy();
        }
    }
    
    private void loadConfig() {
        useMultiLineApi = getServer().getPluginManager().isPluginEnabled("MultiLineAPI");
        kitGui = new KitGui(this);
        kitMap = new LinkedHashMap<>();
        reloadConfig();
        if (getConfig().isConfigurationSection("kits")) {
            for (String kitName : getConfig().getConfigurationSection("kits").getKeys(false)) {
                ConfigurationSection kitSection = getConfig().getConfigurationSection("kits." + kitName);
                if (kitSection != null) {
                    KitInfo kitInfo = new KitInfo(kitSection);
                    addKit(kitInfo);
                }
            }
        }
        getLogger().log(Level.INFO, "Loaded " + kitMap.size() + " kits from the config!");
    
        gameMap = new HashMap<>();
        ConfigurationSection games = getConfig().getConfigurationSection("games");
        if (games != null) {
            for (String gameName : games.getKeys(false)) {
                String type = games.getString(gameName + ".type", null);
                if (type == null) {
                    type = gameName;
                    getLogger().log(Level.WARNING, "No type set for " + gameName + "! Using name as type...");
                }
                try {
                    Class<?> gameClass = Class.forName(getClass().getPackage().getName() + ".games." + type + "Game");
                    if(SimpleTeamPvPGame.class.isAssignableFrom(gameClass)) {
                        SimpleTeamPvPGame game = (SimpleTeamPvPGame) gameClass.getConstructor(SimpleTeamPvP.class, String.class).newInstance(this, gameName);
                        registerGame(game);
                    }
                } catch (ClassNotFoundException e) {
                    getLogger().log(Level.WARNING, "No game type with the name " + gameName + " found!");
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    getLogger().log(Level.SEVERE, "Error while creating game " + gameName + " of type " + type + "!", e);
                }
            }
        }
    }
    
    /**
     * Writes everything to the config
     */
    public void toConfig() {
        for (KitInfo kitInfo : kitMap.values()) {
            toConfig(kitInfo, false);
        }
        saveConfig();
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
        getConfig().set("teams." + teamInfo.getName(), teamInfo.serialize());
        getLogger().log(Level.INFO, "Saved team " + teamInfo.getName() + " to config!");
        if (save)
            saveConfig();
    }
    
    /**
     * Write the kit info to the config
     * @param kitInfo The info to write
     */
    public void toConfig(KitInfo kitInfo) {
        toConfig(kitInfo, true);
    }
    
    /**
     * Write the kit info to the config
     * @param kitInfo The info to write
     * @param save    Whether or not we should write the config to disk
     */
    public void toConfig(KitInfo kitInfo, boolean save) {
        getConfig().set("kits." + kitInfo.getName(), kitInfo.serialize());
        getLogger().log(Level.INFO, "Saved kit " + kitInfo.getName() + " to config!");
        if (save)
            saveConfig();
    }
    
    public boolean reload() {
        loadConfig();
        return true;
    }
    
    /**
     * Add a new kit if a kit with that name doesn't already exist
     * @param kitInfo The info of the kit to add
     * @return Whether or not the kit was added.
     */
    public boolean addKit(KitInfo kitInfo) {
        if (kitMap.containsKey(kitInfo.getName().toLowerCase()))
            return false;
        kitMap.put(kitInfo.getName().toLowerCase(), kitInfo);
        kitGui.generate();
        return true;
    }
    
    /**
     * Get a kit by its name
     * @param kitName The name of the kit
     * @return The KitInfo or <tt>null</tt> if not found
     */
    public KitInfo getKit(String kitName) {
        return kitMap.get(kitName.toLowerCase());
    }
    
    /**
     * Get a kit by its icon
     * @param icon Item used as an icon
     * @return The KitInfo or <tt>null</tt> if not found
     */
    public KitInfo getKit(ItemStack icon) {
        if (icon == null || icon.getType() == Material.AIR)
            return null;
        List<KitInfo> possibleKits = new ArrayList<KitInfo>();
        for (KitInfo kitInfo : kitMap.values()) {
            if (kitInfo.getIcon() != null && icon.getType() == kitInfo.getIcon().getType()) {
                possibleKits.add(kitInfo);
            }
        }
        
        if (possibleKits.size() == 1)
            return possibleKits.get(0);
        
        if (possibleKits.size() > 1)
            for (KitInfo kitInfo : possibleKits)
                if (icon.isSimilar(kitInfo.getIcon()))
                    return kitInfo;
        
        return null;
    }
    
    public Map<String, KitInfo> getKitMap() {
        return kitMap;
    }
    
    public boolean removeKit(KitInfo kitInfo) {
        if (!kitMap.containsKey(kitInfo.getName()))
            return false;
        String kitName = kitInfo.getName();
        getConfig().set("kits." + kitName, null);
        getLogger().log(Level.INFO, "Removed kit " + kitName);
        kitMap.remove(kitName.toLowerCase());
        return true;
    }
    
    /**
     * Register a game with the plugin
     * @param game The game's object
     */
    public void registerGame(SimpleTeamPvPGame game) {
        gameMap.put(game.getName().toLowerCase(), game);
        game.loadConfig();
    }
    
    /**
     * Create a new game of a specific type (and stops a previous game if it existed)
     * @param name The name of the game
     * @return True if the game was successfully created, false if not
     */
    public boolean newGame(String name) {
        if (game != null && game.getState() != GameState.DESTROYED) {
            game.stop();
            game = null;
        }
        if (gameMap.containsKey(name.toLowerCase())) {
            game = gameMap.get(name.toLowerCase()).clone();
            return true;
        }
        return false;
    }
    
    /**
     * Get the currently running game
     * @return The game
     */
    public SimpleTeamPvPGame getGame() {
        return game;
    }
    
    public SimpleTeamPvPGame getGame(String name) {
        return gameMap.get(name.toLowerCase());
    }
    
    public KitGui getKitGui() {
        return kitGui;
    }
    
    public boolean setPointItem(String gameName, LocationInfo loc) {
        SimpleTeamPvPGame targetGame = gameMap.get(gameName.toLowerCase());
        if (targetGame != null) {
            Block block = loc.getLocation().getBlock();
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
                    if (game != null && gameName.equalsIgnoreCase(game.getName())) {
                        game.getConfig().setPointItem(item.clone());
                    }
                    targetGame.getConfig().setPointItem(item.clone());
                    getConfig().set("game." + gameName.toLowerCase() + ".pointitemchest", loc.serialize());
                    saveConfig();
                    return true;
                } else {
                    getLogger().log(Level.INFO, "No items in chest found!");
                }
            } else {
                getLogger().log(Level.INFO, "No chest at " + loc);
            }
        } else {
            getLogger().log(Level.INFO, "No game " + gameName + " found!");
        }
        return false;
    }
    
    public boolean setPointItem(String gameName, ItemStack item) {
        SimpleTeamPvPGame targetGame = gameMap.get(gameName.toLowerCase());
        if (targetGame != null) {
            if (game != null && gameName.equalsIgnoreCase(game.getName())) {
                game.getConfig().setPointItem(item.clone());
            }
            targetGame.getConfig().setPointItem(item.clone());
            getConfig().set("game." + gameName.toLowerCase() + ".pointitem", item.clone());
            saveConfig();
            return true;
        }
        return false;
    }
    
    public boolean setPointBlock(String gameName, Material material) {
        SimpleTeamPvPGame targetGame = gameMap.get(gameName.toLowerCase());
        if (targetGame != null) {
            if (game != null && gameName.equalsIgnoreCase(game.getName())) {
                game.getConfig().setPointBlock(material);
            }
            targetGame.getConfig().setPointBlock(material);
            getConfig().set("game." + gameName.toLowerCase() + ".pointblock", material.toString());
            saveConfig();
            return true;
        }
        return false;
    }
    
    public boolean setRandom(String gameName, Material material) {
        SimpleTeamPvPGame targetGame = gameMap.get(gameName.toLowerCase());
        if (targetGame != null) {
            if (game != null && gameName.equalsIgnoreCase(game.getName())) {
                game.getConfig().setPointBlock(material);
            }
            targetGame.getConfig().setPointBlock(material);
            getConfig().set("game." + gameName.toLowerCase() + ".pointblock", material.toString());
            saveConfig();
            return true;
        }
        return false;
    }
    
    public boolean setDuration(String gameName, int duration) {
        SimpleTeamPvPGame targetGame = gameMap.get(gameName.toLowerCase());
        if (targetGame != null) {
            if (game != null && gameName.equalsIgnoreCase(game.getName())) {
                game.setDuration(duration);
            }
            targetGame.setDuration(duration);
            getConfig().set("game." + gameName.toLowerCase() + ".duration", duration);
            saveConfig();
            return true;
        }
        return false;
    }
    
    public boolean setWinScore(String gameName, int score) {
        SimpleTeamPvPGame targetGame = gameMap.get(gameName.toLowerCase());
        if (targetGame != null) {
            if (game != null && gameName.equalsIgnoreCase(game.getName())) {
                game.getConfig().setWinScore(score);
            }
            targetGame.getConfig().setWinScore(score);
            getConfig().set("game." + gameName.toLowerCase() + ".winscore", score);
            saveConfig();
            return true;
        }
        return false;
    }
    
    public void broadcast(TeamInfo team, String msg) {
        for (Player p : getServer().getOnlinePlayers()) {
            if (team.containsPlayer(p)) {
                p.sendMessage(msg);
            }
        }
    }
    
    public void applyKit(KitInfo kit, Player player) {
        PlayerInventory playerInv = player.getInventory();
        playerInv.clear();
        
        List<ItemStack> armor = Arrays.asList(
                kit.getHelmet(),
                kit.getChest(),
                kit.getLegs(),
                kit.getBoots()
        );
        
        TeamInfo team = game.getTeam(player);
        
        for (int i = 0; i < armor.size(); i++) {
            ItemStack item = armor.get(i);
            if (item != null) {
                item = item.clone();
                if (team != null && team.getColor() != null && item.getItemMeta() instanceof LeatherArmorMeta) {
                    LeatherArmorMeta leatherMeta = (LeatherArmorMeta) item.getItemMeta();
                    if (leatherMeta.getColor().equals(Color.fromRGB(10511680))) {
                        leatherMeta.setColor(Utils.convertColor(team.getColor()));
                        item.setItemMeta(leatherMeta);
                    }
                }
                armor.set(i, item);
            }
        }
        
        playerInv.setHelmet(armor.get(0));
        playerInv.setChestplate(armor.get(1));
        playerInv.setLeggings(armor.get(2));
        playerInv.setBoots(armor.get(3));
        
        for (int i = 0; i < kit.getItems().size() && i < 36; i++) {
            playerInv.setItem(i, kit.getItems().get(i));
        }
        player.updateInventory();
        final UUID playerId = player.getUniqueId();
        getServer().getScheduler().runTaskLater(this, () -> {
            Player player1 = getServer().getPlayer(playerId);
            if (player1 != null && player1.isOnline()) {
                player1.setHealth(player1.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }
        }, 1L);
    }
    
    public Map<String, SimpleTeamPvPGame> getGameMap() {
        return gameMap;
    }
    
    public ServerTags getServerTags() {
        return serverTags;
    }
    
    public String addServerTag(String name) {
        String taggedName = addServerTag(getServer().getPlayer(name));
        return taggedName != null ? taggedName : name;
    }
    
    public String addServerTag(Player player) {
        if (player != null) {
            String name = player.getName();
            name = ChatColor.WHITE + name + ChatColor.GRAY;
            if (getServerTags() != null) {
                ServerInfo server = getServerTags().getPlayerServer(player);
                if (server != null && !server.getTag().isEmpty()) {
                    name += " (" + server.getTag() + ")";
                }
            }
            return name;
        }
        return null;
    }
    
    public boolean useMultiLineApi() {
        return useMultiLineApi;
    }
    
}
