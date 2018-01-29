package de.themoep.simpleteampvp.commands;

import com.google.common.collect.ImmutableSet;
import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.games.SimpleTeamPvPGame;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
public class GameSubCommand extends SubCommand {
    public GameSubCommand(SimpleTeamPvP plugin) {
        super(plugin, plugin.getName().toLowerCase(), "game",
                "[new <type>|start|stop|join|balance|regen|<type> [setpointitem|setpointblock|setduration|setwinscore]]",
                "Start and manage games"
        );
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (plugin.getGame() != null && plugin.getGame().getConfig().getPointBlock() == null) {
            sender.sendMessage(ChatColor.RED + "WARNING: No point item set!");
        }
        if (args.length == 0) {
            return false;
        }
        
        if ("new".equalsIgnoreCase(args[0])) {
            if (args.length > 1) {
                if (plugin.newGame(args[1])) {
                    sender.sendMessage(ChatColor.GREEN + "Created new game " + ChatColor.YELLOW + args[1].toLowerCase());
                } else {
                    sender.sendMessage(ChatColor.RED + "There is no game type " + ChatColor.YELLOW + args[1].toLowerCase());
                }
            } else {
                sender.sendMessage("Usage: /" + getCommand() + " game new <type>");
            }
            
        } else if ("start".equalsIgnoreCase(args[0])) {
            if (plugin.getGame() != null) {
                if (plugin.getGame().start()) {
                    sender.sendMessage(ChatColor.GREEN + "Started game " + ChatColor.YELLOW + plugin.getGame().getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "The game " + ChatColor.YELLOW + plugin.getGame().getName() + ChatColor.RED + " cannot be started! Current state: " + plugin.getGame().getState().toString());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You have to create a new game before you start it!");
            }
            
        } else if ("stop".equalsIgnoreCase(args[0])) {
            if (plugin.getGame() != null) {
                plugin.getGame().stop();
                sender.sendMessage(ChatColor.GREEN + "Stopped game " + ChatColor.YELLOW + plugin.getGame().getName());
            } else {
                sender.sendMessage(ChatColor.RED + "There currently is no game running!");
            }
            
        } else if ("join".equalsIgnoreCase(args[0])) {
            if (plugin.getGame() != null) {
                if (plugin.getGame().join()) {
                    sender.sendMessage(ChatColor.GREEN + "Joined players into teams for game " + ChatColor.YELLOW + plugin.getGame().getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "You can't join players to game " + ChatColor.YELLOW + plugin.getGame().getName() + ChatColor.RED + "! Wrong gamestate (" + plugin.getGame().getState().toString() + ")");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You have to create a new game before you can join players to it!");
            }
            
        } else if ("balance".equalsIgnoreCase(args[0])) {
            if (plugin.getGame() != null) {
                if (plugin.getGame().balance()) {
                    sender.sendMessage(ChatColor.GREEN + "Balancing teams for game " + ChatColor.YELLOW + plugin.getGame().getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "You can't balance teams for game " + ChatColor.YELLOW + plugin.getGame().getName() + ChatColor.RED + "! Wrong gamestate (" + plugin.getGame().getState().toString() + ")");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You have to create a new game and add some players before you can balance teams!");
            }
            
        } else if ("regen".equalsIgnoreCase(args[0])) {
            if (plugin.getGame() != null) {
                int regened = plugin.getGame().regenPointBlocks();
                sender.sendMessage(ChatColor.YELLOW + Integer.toString(regened) + ChatColor.GREEN + " point blocks regened!");
            } else {
                sender.sendMessage(ChatColor.RED + "No game found!");
            }
            
        } else if ("setpointitem".equalsIgnoreCase(args[0])) {
            if (sender instanceof Player) {
                if (args.length > 1) {
                    Player player = (Player) sender;
                    ItemStack item = player.getItemInHand();
                    if (item != null && item.getType() != Material.AIR) {
                        if (plugin.setPointItem(args[1], item)) {
                            sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + item.getType() + ChatColor.GREEN + " as the point item of " + args[1] + "!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[1] + ChatColor.RED + " found!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please hold the item to set as the point item in your hand!");
                    }
                } else {
                    sender.sendMessage("Usage: /" + getCommand() + " game setpointitem <game>");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Only a player can set an item!");
            }
            
        } else if ("setpointitemchest".equalsIgnoreCase(args[0])) {
            if (args.length > 1) {
                LocationInfo chestLoc = null;
                if (args.length == 3) {
                    if (sender instanceof Player) {
                        Block targetBlock = ((Player) sender).getTargetBlock(ImmutableSet.of(Material.AIR), 10);
                        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                            sender.sendMessage(ChatColor.RED + "Please look at a block to set as the point item chest!");
                        } else {
                            chestLoc = new LocationInfo(targetBlock.getLocation());
                        }
                    } else {
                        sender.sendMessage("To run this command from the console use \"" + getCommand() + " game setpointitem <game> <world> <x> <y> <z> [<pitch> <yaw>]\"");
                        return true;
                    }
                } else if (args.length > 5) {
                    if (plugin.getServer().getWorld(args[2]) == null) {
                        sender.sendMessage(ChatColor.RED + "The world " + args[2] + " does not exist!");
                        return true;
                    }
                    String errorArg = "";
                    try {
                        errorArg = args[3];
                        double x = Double.parseDouble(args[3]);
                        errorArg = args[4];
                        double y = Double.parseDouble(args[4]);
                        errorArg = args[5];
                        double z = Double.parseDouble(args[5]);
                        chestLoc = new LocationInfo(args[2], x, y, z, 0, 0);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "The input " + errorArg + " is not a valid number!");
                        return true;
                    }
                }
                if (chestLoc != null) {
                    if (plugin.setPointItem(args[1], chestLoc)) {
                        sender.sendMessage(ChatColor.GREEN + "Set point location to " + ChatColor.WHITE + chestLoc);
                        
                    } else {
                        sender.sendMessage(ChatColor.RED + "Could not set point item chest location!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Could not set point item chest location");
                }
            } else {
                sender.sendMessage("Usage: /" + getCommand() + " game setpointitem <game> [<world> <x> <y> <z> [<pitch> <yaw>]]");
            }
            
        } else if ("setpointblock".equalsIgnoreCase(args[0])) {
            if (args.length == 3) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Block block = player.getTargetBlock(ImmutableSet.of(Material.AIR), 10);
                    if (block != null && block.getType() != Material.AIR) {
                        if (plugin.setPointBlock(args[1], block.getType())) {
                            sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + block.getType() + ChatColor.GREEN + " as the point block of " + args[1] + "!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[1] + ChatColor.RED + " found!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Please look at the block to set!");
                    }
                    
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage on the console: /" + getCommand() + " game setpointblock <game> <material>");
                }
            } else if (args.length > 2) {
                try {
                    Material material = Material.matchMaterial(args[2]);
                    if (plugin.setPointBlock(args[1], material)) {
                        sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + material + ChatColor.GREEN + " as the point block of " + args[1] + "!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[1] + ChatColor.RED + " found!");
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.YELLOW + args[2] + ChatColor.RED + " is not a valid material name!");
                }
            } else {
                sender.sendMessage("Usage: /" + getCommand() + " game setpointblock <game> [<material>]");
            }
            
        } else if ("setwinscore".equalsIgnoreCase(args[0])) {
            if (args.length > 2) {
                try {
                    int duration = Integer.parseInt(args[2]);
                    if (plugin.setWinScore(args[1], duration)) {
                        sender.sendMessage(ChatColor.GREEN + "Set win score of " + args[1] + " to " + ChatColor.YELLOW + duration + ChatColor.GREEN + " points!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[1] + ChatColor.RED + " found!");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.YELLOW + args[0] + ChatColor.RED + " is not a valid number!");
                }
            } else {
                sender.sendMessage("Usage: /" + getCommand() + " game setwinscore <game> <points>");
            }
            
            
        } else if ("setduration".equalsIgnoreCase(args[0])) {
            if (args.length > 2) {
                try {
                    int duration = Integer.parseInt(args[2]);
                    if (plugin.setDuration(args[1], duration)) {
                        sender.sendMessage(ChatColor.GREEN + "Set duration of " + args[1] + " to " + ChatColor.YELLOW + duration + ChatColor.GREEN + " minutes!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[1] + ChatColor.RED + " found!");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.YELLOW + args[0] + ChatColor.RED + " is not a valid number!");
                }
            } else {
                sender.sendMessage("Usage: /" + getCommand() + " game setduration <game> <minutes>");
            }
            
        } else if ("setrandom".equalsIgnoreCase(args[0])) {
            if (args.length > 2) {
                SimpleTeamPvPGame game = plugin.getGameMap().get(args[1].toLowerCase());
                if (game == null) {
                    sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[1] + ChatColor.RED + " found!");
                    return true;
                }
                String type = args[2].toLowerCase();
                LocationInfo loc = null;
                if (args.length == 3) {
                    if (sender instanceof Player) {
                        loc = new LocationInfo(((Player) sender).getLocation());
                    } else {
                        sender.sendMessage("To run this command from the console use \"" + getCommand() + " team set <name> <loctype> <world> <x> <y> <z> [<pitch> <yaw>]\"");
                        return true;
                    }
                } else if (args.length > 6) {
                    if (plugin.getServer().getWorld(args[3]) == null) {
                        sender.sendMessage(ChatColor.RED + "The world " + args[3] + " does not exist!");
                        return true;
                    }
                    String errorArg = "";
                    try {
                        errorArg = args[4];
                        double x = Double.parseDouble(args[4]);
                        errorArg = args[5];
                        double y = Double.parseDouble(args[5]);
                        errorArg = args[6];
                        double z = Double.parseDouble(args[6]);
                        float pitch = 0;
                        float yaw = 0;
                        if (args.length > 8) {
                            errorArg = args[7];
                            pitch = Float.parseFloat(args[7]);
                            errorArg = args[8];
                            yaw = Float.parseFloat(args[8]);
                        }
                        loc = new LocationInfo(args[3], x, y, z, pitch, yaw);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "The input " + errorArg + " is not a valid number!");
                        return true;
                    }
                }
                if (loc != null) {
                    if (game.setRandom(type, loc)) {
                        sender.sendMessage(ChatColor.GREEN + "Set random " + type + " location for game " + ChatColor.WHITE + game.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + loc);
                    } else {
                        sender.sendMessage(ChatColor.RED + type + " is not a valid location setting. Valid ones are pos1 or pos2!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Could not set random " + type + " location for game " + ChatColor.WHITE + game.getName());
                }
            } else {
                sender.sendMessage("Usage: /" + getCommand() + " game setrandom <game> pos1|pos2 [<world> <x> <y> <z>]");
            }
        } else {
            return false;
        }
        return true;
    }
}
