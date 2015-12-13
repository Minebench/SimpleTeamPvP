package de.themoep.simpleteampvp;

import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class SimpleTeamPvPCommand implements CommandExecutor, TabCompleter {
    private final SimpleTeamPvP plugin;
    private final static String NAME = "simpleteampvp";

    public SimpleTeamPvPCommand(SimpleTeamPvP plugin) {
        this.plugin = plugin;
        plugin.getCommand(NAME).setExecutor(this);
        plugin.getCommand(NAME).setTabCompleter(this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0) {

            if("reload".equalsIgnoreCase(args[0])) {
                if(plugin.reload()) {
                    plugin.getLogger().log(Level.INFO, sender.getName() + " reloaded the config!");
                    sender.sendMessage(ChatColor.GREEN + plugin.getName() + " reloaded!");
                } else {
                    plugin.getLogger().log(Level.INFO, sender.getName() + " tried to reload the config but failed?");
                    sender.sendMessage(ChatColor.RED + "Could not reload " + plugin.getName() + "!");
                }

            } else if("save".equalsIgnoreCase(args[0])) {
                plugin.toConfig();
                plugin.getLogger().log(Level.INFO, sender.getName() + " wrote settings to disc!");
                sender.sendMessage(ChatColor.GREEN + "Settings of " + plugin.getName() + " wrote to disk!");

            } else if("regen".equalsIgnoreCase(args[0])) {
                if(plugin.getGame() != null) {
                    int regened = plugin.getGame().regenPointBlocks();
                    sender.sendMessage(ChatColor.YELLOW + Integer.toString(regened) + ChatColor.GREEN + " point blocks regened!");
                } else {
                    sender.sendMessage(ChatColor.RED + "No game found!");
                }

            } else if("game".equalsIgnoreCase(args[0])) {
                if(args.length > 1) {
                    if(plugin.getGame() != null && plugin.getGame().getPointBlock() == null) {
                        sender.sendMessage(ChatColor.RED + "WARNING: No point item set!");
                    }

                    if("new".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            if(plugin.newGame(args[2])) {
                                sender.sendMessage(ChatColor.GREEN + "Created new game " + ChatColor.YELLOW + args[2].toLowerCase());
                            } else {
                                sender.sendMessage(ChatColor.RED + "There is no game type " + ChatColor.YELLOW + args[2].toLowerCase());
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " game new <type>");
                        }

                    } else if("start".equalsIgnoreCase(args[1])) {
                        if(plugin.getGame() != null) {
                            if(plugin.getGame().start()) {
                                sender.sendMessage(ChatColor.GREEN + "Started game " + ChatColor.YELLOW + plugin.getGame().getName());
                            } else {
                                sender.sendMessage(ChatColor.RED + "The game " + ChatColor.YELLOW + plugin.getGame().getName() + ChatColor.RED + " cannot be started! Current state: " + plugin.getGame().getState().toString());
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You have to create a new game before you start it!");
                        }

                    } else if("stop".equalsIgnoreCase(args[1])) {
                        if(plugin.getGame() != null) {
                            plugin.getGame().stop();
                            sender.sendMessage(ChatColor.GREEN + "Stopped game " + ChatColor.YELLOW + plugin.getGame().getName());
                        } else {
                            sender.sendMessage(ChatColor.RED + "There currently is no game running!");
                        }

                    } else if("join".equalsIgnoreCase(args[1])) {
                        if(plugin.getGame() != null) {
                            if(plugin.getGame().join()) {
                                sender.sendMessage(ChatColor.GREEN + "Joined players into teams for game " + ChatColor.YELLOW + plugin.getGame().getName());
                            } else {
                                sender.sendMessage(ChatColor.RED + "You can't join players to game " + ChatColor.YELLOW + plugin.getGame().getName() + ChatColor.RED + "! Wrong gamestate (" + plugin.getGame().getState().toString() + ")");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You have to create a new game before you can join players to it!");
                        }

                    } else if("balance".equalsIgnoreCase(args[1])) {
                        if(plugin.getGame() != null) {
                            if(plugin.getGame().balance()) {
                                sender.sendMessage(ChatColor.GREEN + "Balancing teams for game " + ChatColor.YELLOW + plugin.getGame().getName());
                            } else {
                                sender.sendMessage(ChatColor.RED + "You can't balance teams for game " + ChatColor.YELLOW + plugin.getGame().getName() + ChatColor.RED + "! Wrong gamestate (" + plugin.getGame().getState().toString() + ")");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You have to create a new game and add some players before you can balance teams!");
                        }

                    } else if("setpointitem".equalsIgnoreCase(args[1])) {
                        if(sender instanceof Player) {
                            if(args.length > 2) {
                                Player player = (Player) sender;
                                ItemStack item = player.getItemInHand();
                                if(item != null && item.getType() != Material.AIR) {
                                    if(plugin.setPointItem(args[2], item)) {
                                        sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + item.getType() + ChatColor.GREEN + " as the point item of " + args[2] + "!");
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[2] + ChatColor.RED + " found!");
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Please hold the item to set as the point item in your hand!");
                                }
                            } else {
                                sender.sendMessage("Usage: /" + label + " game setpointitem <game>");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Only a player can set an item!");
                        }

                    } else if("setpointitemchest".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            LocationInfo chestLoc = null;
                            if(args.length == 3) {
                                if(sender instanceof Player) {
                                    Block targetBlock = ((Player) sender).getTargetBlock(ImmutableSet.of(Material.AIR), 10);
                                    if(targetBlock == null || targetBlock.getType() == Material.AIR) {
                                        sender.sendMessage(ChatColor.RED + "Please look at a block to set as the point item chest!");
                                    } else {
                                        chestLoc = new LocationInfo(targetBlock.getLocation());
                                    }
                                } else {
                                    sender.sendMessage("To run this command from the console use \"" + label + " game setpointitem <game> <world> <x> <y> <z> [<pitch> <yaw>]\"");
                                    return true;
                                }
                            } else if(args.length > 6) {
                                if(plugin.getServer().getWorld(args[3]) == null) {
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
                                    chestLoc = new LocationInfo(args[3], x, y, z, 0, 0);
                                } catch(NumberFormatException e) {
                                    sender.sendMessage(ChatColor.RED + "The input " + errorArg + " is not a valid number!");
                                    return true;
                                }
                            }
                            if(chestLoc != null) {
                                if(plugin.setPointItem(args[2], chestLoc)) {
                                    sender.sendMessage(ChatColor.GREEN + "Set point location to " + ChatColor.WHITE + chestLoc);

                                } else {
                                    sender.sendMessage(ChatColor.RED + "Could not set point item chest location!");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "Could not set point item chest location");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " game setpointitem <game> [<world> <x> <y> <z> [<pitch> <yaw>]]");
                        }

                    } else if("setpointblock".equalsIgnoreCase(args[1])) {
                            if(args.length == 3) {
                                if(sender instanceof Player) {
                                    Player player = (Player) sender;
                                    Block block = player.getTargetBlock(ImmutableSet.of(Material.AIR), 10);
                                    if(block != null && block.getType() != Material.AIR) {
                                        if(plugin.setPointBlock(args[2], block.getType())) {
                                            sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + block.getType() + ChatColor.GREEN + " as the point block of " + args[2] + "!");
                                        } else {
                                            sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[2] + ChatColor.RED + " found!");
                                        }
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "Please look at the block to set!");
                                    }

                                } else {
                                    sender.sendMessage(ChatColor.RED + "Usage on the console: /" + label + " game setpointblock <game> <material>");
                                }
                            } else if(args.length > 3) {
                                try {
                                    Material material = Material.matchMaterial(args[3]);
                                    if(plugin.setPointBlock(args[2], material)) {
                                        sender.sendMessage(ChatColor.GREEN + "Set " + ChatColor.YELLOW + material + ChatColor.GREEN + " as the point block of " + args[2] + "!");
                                    } else {
                                        sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[2] + ChatColor.RED + " found!");
                                    }
                                } catch(IllegalArgumentException e) {
                                    sender.sendMessage(ChatColor.YELLOW + args[3] + ChatColor.RED + " is not a valid material name!");
                                }
                            } else {
                                sender.sendMessage("Usage: /" + label + " game setpointblock <game> [<material>]");
                            }

                    } else if("setwinscore".equalsIgnoreCase(args[1])) {
                        if(args.length > 3) {
                            try {
                                int duration = Integer.parseInt(args[3]);
                                if(plugin.setWinScore(args[2], duration)) {
                                    sender.sendMessage(ChatColor.GREEN + "Set win score of " + args[2] + " to " + ChatColor.YELLOW + duration + ChatColor.GREEN + " points!");
                                } else {
                                    sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[2] + ChatColor.RED + " found!");
                                }
                            } catch(NumberFormatException e) {
                                sender.sendMessage(ChatColor.YELLOW + args[1] + ChatColor.RED + " is not a valid number!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " game setwinscore <game> <points>");
                        }


                    } else if("setduration".equalsIgnoreCase(args[1])) {
                        if(args.length > 3) {
                            try {
                                int duration = Integer.parseInt(args[3]);
                                if(plugin.setDuration(args[2], duration)) {
                                    sender.sendMessage(ChatColor.GREEN + "Set duration of " + args[2] + " to " + ChatColor.YELLOW + duration + ChatColor.GREEN + " minutes!");
                                } else {
                                    sender.sendMessage(ChatColor.RED + "No game with the name " + ChatColor.YELLOW + args[2] + ChatColor.RED + " found!");
                                }
                            } catch(NumberFormatException e) {
                                sender.sendMessage(ChatColor.YELLOW + args[1] + ChatColor.RED + " is not a valid number!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " game setduration <game> <minutes>");
                        }

                    } else {
                        sender.sendMessage("Usage: /" + label + " game [new|start|stop|join|balance|setpointitem|setpointblock|setduration|setwinscore]");
                    }
                } else {
                    sender.sendMessage("Usage: /" + label + " game [new|start|stop|join|balance|setpointitem|setpointblock|setduration|setwinscore]");
                }

            } else if("team".equalsIgnoreCase(args[0])) {
                if(args.length > 1) {

                    if("create".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            if(plugin.getTeam(args[2]) == null) {
                                TeamInfo teamInfo = new TeamInfo(args[2]);
                                if(args.length > 3) {
                                    String displayname = args[3];
                                    for(int i = 4; i > args.length; i++) {
                                        displayname += " " + args[i];
                                    }
                                    teamInfo.getScoreboardTeam().setDisplayName(displayname);
                                    sender.sendMessage(ChatColor.GREEN + "Added team " + ChatColor.WHITE + teamInfo.getName() + "/" + teamInfo.getScoreboardTeam().getDisplayName());
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "Added team " + ChatColor.WHITE + teamInfo.getName());
                                }
                                plugin.addTeam(teamInfo);
                                plugin.toConfig(teamInfo);
                            } else {
                                sender.sendMessage(ChatColor.RED + "A team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " already exists on this server's scoreboard!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team create <name> [<displayname...>]");
                        }

                    } else if("remove".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                sender.sendMessage(ChatColor.GREEN + "Removed team " + ChatColor.WHITE + teamInfo.getName());
                                plugin.removeTeam(teamInfo);
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team remove <name>");
                        }

                    } else if("info".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                sender.sendMessage(ChatColor.GREEN + "Team name: " + ChatColor.WHITE + teamInfo.getName());
                                sender.sendMessage(ChatColor.GREEN + "Display name: " + ChatColor.WHITE + teamInfo.getScoreboardTeam().getDisplayName());
                                if(teamInfo.getColor() != null) {
                                    sender.sendMessage(ChatColor.GREEN + "Color: " + teamInfo.getColor() + teamInfo.getColor().getName());
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "Color: " + ChatColor.RED + "none");
                                }
                                sender.sendMessage(ChatColor.GREEN + "Block: " + ChatColor.WHITE + teamInfo.getBlockMaterial() + ":" + teamInfo.getBlockData());
                                if(teamInfo.getSpawn() != null) {
                                    sender.sendMessage(new String[]{
                                            ChatColor.GREEN + "Spawn:",
                                            ChatColor.GREEN + " World: " + ChatColor.WHITE + teamInfo.getSpawn().getWorldName(),
                                            ChatColor.GREEN + " X: " + ChatColor.WHITE + teamInfo.getSpawn().getX(),
                                            ChatColor.GREEN + " Y: " + ChatColor.WHITE + teamInfo.getSpawn().getY(),
                                            ChatColor.GREEN + " Z: " + ChatColor.WHITE + teamInfo.getSpawn().getZ(),
                                            ChatColor.GREEN + " Pitch: " + ChatColor.WHITE + teamInfo.getSpawn().getPitch(),
                                            ChatColor.GREEN + " Yaw: " + ChatColor.WHITE + teamInfo.getSpawn().getYaw(),
                                    });
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "Spawn: " + ChatColor.RED + "none");
                                }
                                if(teamInfo.getPoint() != null) {
                                    sender.sendMessage(new String[]{
                                            ChatColor.GREEN + "Point:",
                                            ChatColor.GREEN + " World: " + ChatColor.WHITE + teamInfo.getPoint().getWorldName(),
                                            ChatColor.GREEN + " X: " + ChatColor.WHITE + teamInfo.getPoint().getX(),
                                            ChatColor.GREEN + " Y: " + ChatColor.WHITE + teamInfo.getPoint().getY(),
                                            ChatColor.GREEN + " Z: " + ChatColor.WHITE + teamInfo.getPoint().getZ(),
                                    });
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "Point: " + ChatColor.RED + "none");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team info <name>");
                        }

                    } else if("list".equalsIgnoreCase(args[1])) {
                        sender.sendMessage(ChatColor.GREEN + "Registered teams:");
                        if(plugin.getTeamMap().size() > 0) {
                            for(TeamInfo teamInfo : plugin.getTeamMap().values()) {
                                sender.sendMessage((teamInfo.getColor() != null ? teamInfo.getColor() : "") + teamInfo.getName() + "/" + teamInfo.getScoreboardTeam().getDisplayName());
                            }
                        } else {
                            sender.sendMessage(ChatColor.WHITE + " - none - ");
                        }

                    } else if("setdisplayname".equalsIgnoreCase(args[1])) {
                        if(args.length > 3) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                String displayname = args[3];
                                for(int i = 4; i > args.length; i++) {
                                    displayname += " " + args[i];
                                }
                                teamInfo.getScoreboardTeam().setDisplayName(displayname);
                                sender.sendMessage(ChatColor.GREEN + "Set displayname of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getScoreboardTeam().getDisplayName());
                                plugin.toConfig(teamInfo);
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team setdisplayname <name> <displayname...>");
                        }

                    } else if("setcolor".equalsIgnoreCase(args[1])) {
                        if(args.length > 3) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                if(teamInfo.setColor(args[3])) {
                                    sender.sendMessage(ChatColor.GREEN + "Set color of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + teamInfo.getColor() + teamInfo.getColor().getName());
                                    plugin.toConfig(teamInfo);
                                } else {
                                    List<String> colorList = new ArrayList<String>();
                                    for(ChatColor color : ChatColor.values()) {
                                        colorList.add(color.getName());
                                    }
                                    sender.sendMessage(ChatColor.YELLOW + args[3].toUpperCase() + ChatColor.RED + " is not valid color string! (Colors: " + StringUtils.join(colorList, ", ") + ")");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team setcolor <name> <color>");
                        }

                    } else if("setblock".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                if(args.length > 3) {
                                    if(teamInfo.setBlock(args[3])) {
                                        sender.sendMessage(ChatColor.GREEN + "Set block of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getBlockMaterial() + ":" + teamInfo.getBlockData());
                                        plugin.toConfig(teamInfo);
                                    } else {
                                        sender.sendMessage(ChatColor.YELLOW + args[3].toUpperCase() + ChatColor.RED + " is not valid block string! (material:data)");
                                    }
                                } else if(sender instanceof Player) {
                                    Block block = ((Player) sender).getTargetBlock(ImmutableSet.of(Material.AIR), 20);
                                    teamInfo.setBlock(block);
                                    sender.sendMessage(ChatColor.GREEN + "Set block of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getBlockMaterial() + ":" + teamInfo.getBlockData());
                                    plugin.toConfig(teamInfo);
                                } else {
                                    sender.sendMessage("Usage: /" + label + " team setblock <name> [<material:data>]");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team setblock <name> [<material:data>]");
                        }

                    } else if("setspawn".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                LocationInfo spawn = null;
                                if(args.length == 3) {
                                    if(sender instanceof Player) {
                                        spawn = new LocationInfo(((Player) sender).getLocation());
                                    } else {
                                        sender.sendMessage("To run this command from the console use \"" + label + " team setspawn <name> <world> <x> <y> <z> [<pitch> <yaw>]\"");
                                        return true;
                                    }
                                } else if(args.length > 6) {
                                    if(plugin.getServer().getWorld(args[3]) == null) {
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
                                        if(args.length > 8) {
                                            errorArg = args[7];
                                            pitch = Float.parseFloat(args[7]);
                                            errorArg = args[8];
                                            yaw = Float.parseFloat(args[8]);
                                        }
                                        spawn = new LocationInfo(args[3], x, y, z, pitch, yaw);
                                    } catch(NumberFormatException e) {
                                        sender.sendMessage(ChatColor.RED + "The input " + errorArg + " is not a valid number!");
                                        return true;
                                    }
                                }
                                if(spawn != null) {
                                    teamInfo.setSpawn(spawn);
                                    sender.sendMessage(ChatColor.GREEN + "Set spawn location of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getSpawn());
                                    plugin.toConfig(teamInfo);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Could not set spawn location for team " + ChatColor.WHITE + teamInfo.getName());
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team setspawn <name> [<world> <x> <y> <z> [<pitch> <yaw>]]");
                        }

                    } else if("setpoint".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            TeamInfo teamInfo = plugin.getTeam(args[2]);
                            if(teamInfo != null) {
                                LocationInfo point = null;
                                if(args.length == 3) {
                                    if(sender instanceof Player) {
                                        Block targetBlock = ((Player) sender).getTargetBlock(ImmutableSet.of(Material.AIR), 10);
                                        if(targetBlock == null || targetBlock.getType() == Material.AIR) {
                                            sender.sendMessage(ChatColor.RED + "Please look at a block to set as the point!");
                                        } else {
                                            point = new LocationInfo(targetBlock.getLocation());
                                        }
                                    } else {
                                        sender.sendMessage("To run this command from the console use \"" + label + " team setpoint <name> <world> <x> <y> <z> [<pitch> <yaw>]\"");
                                        return true;
                                    }
                                } else if(args.length > 6) {
                                    if(plugin.getServer().getWorld(args[3]) == null) {
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
                                        point = new LocationInfo(args[3], x, y, z, 0, 0);
                                    } catch(NumberFormatException e) {
                                        sender.sendMessage(ChatColor.RED + "The input " + errorArg + " is not a valid number!");
                                        return true;
                                    }
                                }
                                if(point != null) {
                                    teamInfo.setPoint(point);
                                    sender.sendMessage(ChatColor.GREEN + "Set point location of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getPoint());
                                    plugin.toConfig(teamInfo);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Could not set point location for team " + ChatColor.WHITE + teamInfo.getName());
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[2] + ChatColor.RED + " found!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " team setpoint <name> [<world> <x> <y> <z> [<pitch> <yaw>]]");
                        }

                    } else {
                        sender.sendMessage("Usage: /" + label + " team [create|remove|list|info|setdisplayname|setcolor|setblock|setspawn|setpoint]");
                    }
                } else {
                    sender.sendMessage("Usage: /" + label + " team [create|remove|list|info|setdisplayname|setcolor|setblock|setspawn|setpoint]");
                }

            } else if("kit".equalsIgnoreCase(args[0])) {
                if(args.length > 1) {

                    if("gui".equalsIgnoreCase(args[1])) {
                        if(sender instanceof Player) {
                            plugin.getKitGui().show((Player) sender);
                        } else {
                            sender.sendMessage(ChatColor.RED + "This command cannot be run by the console!");
                        }

                    } else if("create".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            String name = args[2];
                            for(int i = 3; i < args.length; i ++) {
                                name += " " + args[i];
                            }
                            if(name.toCharArray()[0] == '"' && name.toCharArray()[name.length() - 1] == '"') {
                                name = name.substring(1, name.length() - 2);
                            }
                            name = name.replace('_', ' ');
                            KitInfo kit = new KitInfo(name);
                            if(plugin.addKit(new KitInfo(name))) {
                                plugin.toConfig(kit);
                                sender.sendMessage(ChatColor.GREEN + "Added kit " + ChatColor.WHITE + name);
                            } else {
                                sender.sendMessage(ChatColor.RED + "A kit with the name " + ChatColor.WHITE + name + ChatColor.RED + " already exists!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " kit create <name...>");
                        }

                    } else if("remove".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            String name = args[2];
                            for(int i = 3; i < args.length; i ++) {
                                name += " " + args[i];
                            }
                            if(name.toCharArray()[0] == '"' && name.toCharArray()[name.length() - 1] == '"') {
                                name = name.substring(1, name.length() - 2);
                            }
                            name = name.replace('_', ' ');
                            if(plugin.removeKit(new KitInfo(name))) {
                                sender.sendMessage(ChatColor.GREEN + "Removed kit " + ChatColor.WHITE + name);
                            } else {
                                sender.sendMessage(ChatColor.RED + "No kit with the name " + ChatColor.WHITE + name + ChatColor.RED + " exists!");
                            }
                        } else {
                            sender.sendMessage("Usage: /" + label + " kit remove <name...>");
                        }

                    } else if("list".equalsIgnoreCase(args[1])) {
                        sender.sendMessage(ChatColor.GREEN + "Available kits:");
                        for(KitInfo kit : plugin.getKitMap().values()) {
                            sender.sendMessage(kit.getName());
                        }

                    } else if("info".equalsIgnoreCase(args[1])) {
                        if(args.length > 2) {
                            String name = args[2];
                            for(int i = 3; i < args.length; i ++) {
                                name += " " + args[i];
                            }
                            if(name.toCharArray()[0] == '"' && name.toCharArray()[name.length() - 1] == '"') {
                                name = name.substring(1, name.length() - 2);
                            }
                            name = name.replace('_', ' ');
                            KitInfo kit = plugin.getKit(name);
                            if(kit != null) {
                                List<String> itemMsg = new ArrayList<String>(Arrays.asList(
                                        ChatColor.GREEN + "Kit " + ChatColor.WHITE + kit.getName(),
                                        ChatColor.GREEN + "Icon: " + ChatColor.WHITE + (kit.getIcon() == kit.getHelmet() ? "none" : kit.getIcon().getType().toString().toLowerCase()),
                                        ChatColor.GREEN + "Head: " + ChatColor.WHITE + (kit.getHelmet() == null ? "none" : kit.getHelmet().getType().toString().toLowerCase()),
                                        ChatColor.GREEN + "Chest: " + ChatColor.WHITE + (kit.getChest() == null ? "none" : kit.getChest().getType().toString().toLowerCase()),
                                        ChatColor.GREEN + "Legs: " + ChatColor.WHITE + (kit.getLegs() == null ? "none" : kit.getLegs().getType().toString().toLowerCase()),
                                        ChatColor.GREEN + "Feet: " + ChatColor.WHITE + (kit.getBoots() == null ? "none" : kit.getBoots().getType().toString().toLowerCase()),
                                        ChatColor.GREEN + "Items:" + (kit.getItems().size() == 0 ? ChatColor.WHITE + "none" : "")
                                ));
                                for(ItemStack item : kit.getItems()) {
                                    itemMsg.add(ChatColor.WHITE + (item == null ? "none" : item.getType().toString().toLowerCase()));
                                }
                                sender.sendMessage(itemMsg.toArray(new String[itemMsg.size()]));

                            } else {
                                sender.sendMessage(ChatColor.RED + "No kit with the name " + ChatColor.WHITE + name + ChatColor.RED + " exists!");
                            }

                        } else {
                            sender.sendMessage("Usage: /" + label + " kit info <name...>");
                        }

                    } else if("seticon".equalsIgnoreCase(args[1])) {
                        if(sender instanceof Player) {
                            if(args.length > 2) {
                                String name = args[2];
                                for(int i = 3; i < args.length; i ++) {
                                    name += " " + args[i];
                                }
                                if(name.toCharArray()[0] == '"' && name.toCharArray()[name.length() - 1] == '"') {
                                    name = name.substring(1, name.length() - 2);
                                }
                                name = name.replace('_', ' ');
                                KitInfo kit = plugin.getKit(name);
                                if(kit != null) {
                                    kit.setIcon(((Player) sender).getItemInHand());
                                    plugin.getKitGui().generate();
                                    sender.sendMessage(ChatColor.GREEN + "Set icon to " + ChatColor.WHITE + (kit.getIcon() == kit.getHelmet() ? "none" : kit.getIcon().getType().toString().toLowerCase()));
                                    plugin.toConfig(kit);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "No kit with the name " + ChatColor.WHITE + name + ChatColor.RED + " exists!");
                                }

                            } else {
                                sender.sendMessage("Usage: /" + label + " kit info <name...>");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "This command cannot be run by the console!");
                        }

                    } else if("items".equalsIgnoreCase(args[1])) {
                        if(sender instanceof Player) {
                            if(args.length > 2) {
                                String name = args[3];
                                for(int i = 4; i < args.length; i ++) {
                                    name += " " + args[i];
                                }
                                if(name.toCharArray()[0] == '"' && name.toCharArray()[name.length() - 1] == '"') {
                                    name = name.substring(1, name.length() - 2);
                                }
                                name = name.replace('_', ' ');
                                KitInfo kit = plugin.getKit(name);
                                if(kit != null) {
                                    Player player = (Player) sender;
                                    if("copyarmor".equalsIgnoreCase(args[2])) {
                                        kit.setHelmet(player.getInventory().getHelmet());
                                        kit.setChest(player.getInventory().getChestplate());
                                        kit.setLegs(player.getInventory().getLeggings());
                                        kit.setBoots(player.getInventory().getBoots());
                                        sender.sendMessage(ChatColor.GREEN + "Copied your armor to the kit!");
                                        plugin.toConfig(kit);

                                    } else if("head".equalsIgnoreCase(args[2])) {
                                        kit.setHelmet(player.getItemInHand());
                                        sender.sendMessage(ChatColor.GREEN + "Set head to " + ChatColor.WHITE + (kit.getHelmet() == null ? "none" : kit.getHelmet().getType().toString().toLowerCase()));
                                        plugin.toConfig(kit);

                                    } else if("chest".equalsIgnoreCase(args[2])) {
                                        kit.setChest(player.getItemInHand());
                                        sender.sendMessage(ChatColor.GREEN + "Set chest to " + ChatColor.WHITE + (kit.getChest() == null ? "none" : kit.getChest().getType().toString().toLowerCase()));
                                        plugin.toConfig(kit);

                                    } else if("legs".equalsIgnoreCase(args[2])) {
                                        kit.setLegs(player.getItemInHand());
                                        sender.sendMessage(ChatColor.GREEN + "Set legs to " + ChatColor.WHITE + (kit.getLegs() == null ? "none" : kit.getLegs().getType().toString().toLowerCase()));
                                        plugin.toConfig(kit);

                                    } else if("feet".equalsIgnoreCase(args[2])) {
                                        kit.setBoots(player.getItemInHand());
                                        sender.sendMessage(ChatColor.GREEN + "Set feet to " + ChatColor.WHITE + (kit.getBoots() == null ? "none" : kit.getBoots().getType().toString().toLowerCase()));
                                        plugin.toConfig(kit);

                                    } else if("copytoolbar".equalsIgnoreCase(args[2])) {
                                        for(int i = 0; i < 9; i++) {
                                            kit.setItem(i, player.getInventory().getItem(i));
                                        }
                                        sender.sendMessage(ChatColor.GREEN + "Copied your toolbar to the kit!");
                                        plugin.toConfig(kit);

                                    } else if("copyinv".equalsIgnoreCase(args[2])) {
                                        kit.setHelmet(player.getInventory().getHelmet());
                                        kit.setChest(player.getInventory().getChestplate());
                                        kit.setLegs(player.getInventory().getLeggings());
                                        kit.setBoots(player.getInventory().getBoots());
                                        for(int i = 0; i < 36; i++) {
                                            kit.setItem(i, player.getInventory().getItem(i));
                                        }
                                        sender.sendMessage(ChatColor.GREEN + "Copied your entire inventory to the kit!");
                                        plugin.toConfig(kit);

                                    } else if("add".equalsIgnoreCase(args[2])) {
                                        if(player.getItemInHand() != null) {
                                            kit.addItem(player.getItemInHand());
                                            sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + player.getItemInHand().getType().toString().toLowerCase() + ChatColor.GREEN + " to the kit!");
                                            plugin.toConfig(kit);
                                        } else {
                                            sender.sendMessage(ChatColor.RED + "Please hold an item in your hand to add to the kit!");
                                        }

                                    } else if("remove".equalsIgnoreCase(args[2])) {
                                        if(player.getItemInHand() != null) {
                                            kit.getItems().remove(player.getItemInHand());
                                            sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + player.getItemInHand().getType().toString().toLowerCase() + ChatColor.GREEN + " from the kit!");
                                            plugin.toConfig(kit);
                                        } else {
                                            sender.sendMessage(ChatColor.RED + "Please hold an item in your hand to remove from the kit!");
                                        }

                                    } else {
                                        sender.sendMessage("Usage: /" + label + " kit items [copyarmor|head|chest|legs|feet|copytoolbar|copyinv|add|remove] <name...>");
                                    }
                                } else {
                                    sender.sendMessage(ChatColor.RED + "No kit with the name " + ChatColor.WHITE + name + ChatColor.RED + " exists!");
                                }

                            } else {
                                sender.sendMessage("Usage: /" + label + " kit items [copyarmor|head|chest|legs|feet|copytoolbar|copyinv|add|remove] <name...>");
                            }

                        } else {
                            sender.sendMessage(ChatColor.RED + "This command cannot be run by the console!");
                        }

                    } else {
                        sender.sendMessage("Usage: /" + label + " kit [create|remove|list|info|seticon|items]");
                    }
                } else {
                    sender.sendMessage("Usage: /" + label + " kit [create|remove|list|info|seticon|items]");
                }
            } else {
                sender.sendMessage("Usage: /" + label + " [reload|save|regen|game|team|kit]");
            }
        } else {
            sender.sendMessage("Usage: /" + label + " [reload|save|regen|game|team|kit]");
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> compareList = new ArrayList<String>();
        if(args.length == 1) {
            compareList.addAll(Arrays.asList("reload", "save", "regen", "game", "team", "kit"));
        } else if(args.length == 2) {
            if("game".equalsIgnoreCase(args[0])) {
                compareList.addAll(Arrays.asList("new", "join", "balance", "start", "stop", "setpointitem", "setpointitemchest", "setpointblock", "setduration", "setwinscore"));
            } else if("team".equalsIgnoreCase(args[0])) {
                compareList.addAll(Arrays.asList("create", "remove", "list", "info", "setdisplayname", "setcolor", "setblock", "setspawn", "setpoint"));
            } else if("kit".equalsIgnoreCase(args[0])) {
                compareList.addAll(Arrays.asList("gui", "create", "remove", "list", "info", "seticon", "items"));
            }
        } else if(args.length == 3) {
            if("team".equalsIgnoreCase(args[0]) && Arrays.asList("create", "remove", "info", "setdisplayname", "setcolor", "setblock", "setspawn", "setpoint").contains(args[1].toLowerCase())) {
                compareList.addAll(plugin.getTeamMap().keySet());
            } else if("kit".equalsIgnoreCase(args[0])) {
                if("items".equalsIgnoreCase(args[1])) {
                    compareList.addAll(Arrays.asList("copyarmor", "head", "chest", "legs", "feet", "copytoolbar", "copyinv", "add", "remove"));
                } else if("info".equalsIgnoreCase(args[1]) || "remove".equalsIgnoreCase(args[1]) || "seticon".equalsIgnoreCase(args[1])) {
                    compareList.addAll(plugin.getKitMap().keySet());
                }
            } else if("game".equalsIgnoreCase(args[0]) && Arrays.asList("new", "setpointitem", "setpointitemchest", "setpointblock", "setduration", "setwinscore").contains(args[1].toLowerCase())) {
                compareList.addAll(plugin.getGameMap().keySet());
            }
        } else if(args.length == 4) {
            if("team".equalsIgnoreCase(args[0]) && plugin.getTeam(args[2]) != null) {
                if("setspawn".equalsIgnoreCase(args[1]) || "setpoint".equalsIgnoreCase(args[1])) {
                    List<String> worldList = new ArrayList<String>();
                    for(World world : plugin.getServer().getWorlds()) {
                        worldList.add(world.getName());
                    }
                    compareList.addAll(worldList);
                }
                if("setcolor".equalsIgnoreCase(args[1])) {
                    List<String> colorList = new ArrayList<String>();
                    for(ChatColor color : ChatColor.values()) {
                        colorList.add(color.getName());
                    }
                    compareList.addAll(colorList);
                }
            } else if("kit".equalsIgnoreCase(args[0]) && "items".equalsIgnoreCase(args[1]) && Arrays.asList("copyarmor", "head", "chest", "legs", "feet", "copytoolbar", "copyinv", "add", "remove").contains(args[2].toLowerCase())) {
                compareList.addAll(plugin.getKitMap().keySet());
            }
        }
        List<String> returnList = new ArrayList<String>();
        for(String s : compareList) {
            if(s.startsWith(args[args.length - 1].toLowerCase())) {
                returnList.add(s);
            }
        }
        if(returnList.size() == 1) {
            returnList.set(0, returnList.get(0) + " ");
        }
        return returnList;
    }
}
