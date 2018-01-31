package de.themoep.simpleteampvp.commands;

import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.Utils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
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
public class PluginCommandExecutor implements CommandExecutor, TabCompleter {
    private final SimpleTeamPvP plugin;
    
    private Map<String, Map<String, SubCommand>> subCommands = new HashMap<String, Map<String, SubCommand>>();
    private String header;
    
    public PluginCommandExecutor(SimpleTeamPvP plugin) {
        this.plugin = plugin;
        header = ChatColor.GRAY + plugin.getDescription().getAuthors().get(0) + "'s " +
                ChatColor.RED + plugin.getName() +
                ChatColor.GRAY + " v" + plugin.getDescription().getVersion();
        plugin.getCommand(plugin.getName().toLowerCase()).setExecutor(this);
        plugin.getCommand(plugin.getName().toLowerCase()).setTabCompleter(this);
    }
    
    public void register(SubCommand sub) {
        if (!subCommands.containsKey(sub.getCommand())) {
            subCommands.put(sub.getCommand(), new LinkedHashMap<String, SubCommand>());
        }
        if (subCommands.get(sub.getCommand()).containsKey(sub.getPath())) {
            throw new IllegalArgumentException("A sub command with the path '" + sub.getPath() + "' is already defined for command '" + sub.getCommand() + "'!");
        }
        subCommands.get(sub.getCommand()).put(sub.getPath(), sub);
        try {
            plugin.getServer().getPluginManager().addPermission(sub.getPermission());
        } catch (IllegalArgumentException ignore) {
            // Permission was already defined correctly in the plugin.yml
        }
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            List<String> helpText = new ArrayList<String>();
            helpText.add(header);
            if (subCommands.containsKey(cmd.getName())) {
                for (SubCommand sub : subCommands.get(cmd.getName()).values()) {
                    if (!sender.hasPermission(sub.getPermission())) {
                        continue;
                    }
                    helpText.add(ChatColor.YELLOW + sub.getUsage(label));
                    helpText.add(ChatColor.WHITE + " " + sub.getHelp());
                }
            } else {
                helpText.add("No sub commands found.");
            }
            sender.sendMessage(helpText.toArray(new String[helpText.size()]));
            return true;
        }
        
        SubCommand sub = null;
        
        int pathPartCount = args.length;
        if (subCommands.containsKey(cmd.getName())) {
            String path = Utils.join(args, " ", 0, pathPartCount).toLowerCase();
            while (!subCommands.get(cmd.getName()).containsKey(path) && pathPartCount > 0) {
                pathPartCount--;
                path = Utils.join(args, " ", 0, pathPartCount).toLowerCase();
            }
            sub = subCommands.get(cmd.getName()).get(path);
        }
        
        if (sub == null) {
            if (subCommands.containsKey(cmd.getName())) {
                Set<String> subCmdsStr = subCommands.get(cmd.getName()).keySet();
                sender.sendMessage("Usage: /" + label + " " + Arrays.toString(subCmdsStr.toArray(new String[subCmdsStr.size()])));
                return true;
            } else {
                return false;
            }
        }
        
        if (!sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(plugin.getCommand(sub.getCommand()).getPermissionMessage().replace("<permission>", sub.getPermission().getName()));
            return true;
        }
        
        String[] subArgs = new String[]{};
        if (args.length > pathPartCount) {
            subArgs = Arrays.copyOfRange(args, pathPartCount, args.length);
        }
        if (!sub.execute(sender, subArgs)) {
            sender.sendMessage("Usage: " + sub.getUsage(label));
        }
        return true;
    }
    
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> compareList = new ArrayList<>();
        if (args.length == 1) {
            compareList.addAll(Arrays.asList("reload", "save", "game", "kit"));
        } else if (args.length == 2) {
            if ("game".equalsIgnoreCase(args[0])) {
                compareList.addAll(Arrays.asList("new", "join", "balance", "regen", "start", "stop", "teams", "setpointitem", "setpointitemchest", "setpointblock", "setduration", "setwinscore"));
            } else if ("team".equalsIgnoreCase(args[0])) {
                compareList.addAll(Arrays.asList("create", "remove", "list", "info", "setdisplayname", "setcolor", "setblock", "set"));
            } else if ("kit".equalsIgnoreCase(args[0])) {
                compareList.addAll(Arrays.asList("gui", "create", "remove", "list", "info", "seticon", "items"));
            }
        } else if (args.length == 3) {
            if ("team".equalsIgnoreCase(args[0]) && Arrays.asList("create", "remove", "info", "setdisplayname", "setcolor", "setblock", "set", "set").contains(args[1].toLowerCase())) {
                compareList.addAll(plugin.getGame().getTeamMap().keySet());
            } else if ("kit".equalsIgnoreCase(args[0])) {
                if ("items".equalsIgnoreCase(args[1])) {
                    compareList.addAll(Arrays.asList("copyarmor", "head", "chest", "legs", "feet", "copytoolbar", "copyinv", "add", "remove"));
                } else if ("info".equalsIgnoreCase(args[1]) || "remove".equalsIgnoreCase(args[1]) || "seticon".equalsIgnoreCase(args[1])) {
                    compareList.addAll(plugin.getKitMap().keySet());
                }
            } else if ("game".equalsIgnoreCase(args[0]) && Arrays.asList("teams", "new", "setpointitem", "setpointitemchest", "setpointblock", "setduration", "setwinscore").contains(args[1].toLowerCase())) {
                compareList.addAll(plugin.getGameMap().keySet());
            }
        } else if (args.length == 4) {
            if ("game".equalsIgnoreCase(args[0]) && plugin.getGame(args[2]) != null && "teams".equalsIgnoreCase(args[1])) {
                compareList.addAll(Arrays.asList("create", "remove", "list", "info", "setdisplayname", "setcolor", "setblock", "set"));
            } else if ("kit".equalsIgnoreCase(args[0]) && "items".equalsIgnoreCase(args[1]) && Arrays.asList("copyarmor", "head", "chest", "legs", "feet", "copytoolbar", "copyinv", "add", "remove").contains(args[2].toLowerCase())) {
                compareList.addAll(plugin.getKitMap().keySet());
            }
        } else if (args.length == 5) {
            if ("game".equalsIgnoreCase(args[0]) && plugin.getGame(args[2]) != null && "teams".equalsIgnoreCase(args[1])) {
                compareList.addAll(plugin.getGame(args[2]).getConfig().getTeams().keySet());
            }
        } else if (args.length == 6) {
            if ("game".equalsIgnoreCase(args[0]) && "teams".equalsIgnoreCase(args[1])) {
                if ("set".equalsIgnoreCase(args[3])) {
                    compareList.addAll(Arrays.asList("spawn", "point", "pos1", "pos2", "joinpos1", "joinpos2"));
                } else if ("setcolor".equalsIgnoreCase(args[3])) {
                    List<String> colorList = new ArrayList<>();
                    for (ChatColor color : ChatColor.values()) {
                        colorList.add(color.name().toLowerCase());
                    }
                    compareList.addAll(colorList);
                }
            }
        } else if (args.length == 7) {
            if ("game".equalsIgnoreCase(args[0]) && "teams".equalsIgnoreCase(args[1])) {
                if ("set".equalsIgnoreCase(args[1])) {
                    List<String> worldList = new ArrayList<String>();
                    for (World world : plugin.getServer().getWorlds()) {
                        worldList.add(world.getName());
                    }
                    compareList.addAll(worldList);
                }
            }
        }
        List<String> returnList = new ArrayList<String>();
        for (String s : compareList) {
            if (s.startsWith(args[args.length - 1].toLowerCase())) {
                returnList.add(s);
            }
        }
        if (returnList.size() == 1) {
            returnList.set(0, returnList.get(0) + " ");
        }
        return returnList;
    }
}
