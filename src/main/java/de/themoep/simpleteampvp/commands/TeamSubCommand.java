package de.themoep.simpleteampvp.commands;

import com.google.common.collect.ImmutableSet;
import de.themoep.simpleteampvp.LocationInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import de.themoep.simpleteampvp.TeamInfo;
import de.themoep.simpleteampvp.games.SimpleTeamPvPGame;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@Getter
public class TeamSubCommand {
    private final String usage;
    private final String help;
    private final SimpleTeamPvP plugin;
    
    public TeamSubCommand(SimpleTeamPvP plugin) {
        this.plugin = plugin;
        usage = "create|remove|list|info|setdisplayname|setcolor|setblock|set [spawn|point|pos1|pos2]]";
        help = "Create and edit teams";
    }

    public boolean execute(String command, SimpleTeamPvPGame game, CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }

        if("create".equalsIgnoreCase(args[0])) {
            if(args.length > 1) {
                if(game.getTeam(args[1]) == null) {
                    TeamInfo teamInfo = new TeamInfo(args[1]);
                    if(args.length > 2) {
                        String displayname = args[2];
                        for(int i = 3; i > args.length; i++) {
                            displayname += " " + args[i];
                        }
                        teamInfo.getScoreboardTeam().setDisplayName(displayname);
                        sender.sendMessage(ChatColor.GREEN + "Added team " + ChatColor.WHITE + teamInfo.getName() + "/" + teamInfo.getScoreboardTeam().getDisplayName());
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Added team " + ChatColor.WHITE + teamInfo.getName());
                    }
                    game.addTeam(teamInfo);
                    game.toConfig(teamInfo);
                } else {
                    sender.sendMessage(ChatColor.RED + "A team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " already exists on this server's scoreboard!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team create <name> [<displayname...>]");
            }

        } else if("remove".equalsIgnoreCase(args[0])) {
            if(args.length > 1) {
                TeamInfo teamInfo = game.getTeam(args[1]);
                if(teamInfo != null) {
                    sender.sendMessage(ChatColor.GREEN + "Removed team " + ChatColor.WHITE + teamInfo.getName());
                    game.removeTeam(teamInfo);
                } else {
                    sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " found!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team remove <name>");
            }

        } else if("info".equalsIgnoreCase(args[0])) {
            if(args.length > 1) {
                TeamInfo teamInfo = game.getTeam(args[1]);
                if(teamInfo != null) {
                    sender.sendMessage(ChatColor.GREEN + "Team name: " + ChatColor.WHITE + teamInfo.getName());
                    for (Map.Entry<String, Object> entry : teamInfo.serialize().entrySet()) {
                        if (entry.getValue() instanceof LocationInfo) {
                            sender.sendMessage(ChatColor.GREEN + entry.getKey() + ":");
                            for (Map.Entry<String, Object> locEntry : ((LocationInfo) entry.getValue()).serialize().entrySet()) {
                                sender.sendMessage(ChatColor.GREEN + " " + locEntry.getKey() + ": " + ChatColor.WHITE + locEntry.getValue());
                            }
                        } else {
                            sender.sendMessage(ChatColor.GREEN + entry.getKey() + ": " + ChatColor.WHITE + entry.getValue());
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " found!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team info <name>");
            }

        } else if("list".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.GREEN + "Registered teams:");
            if(game.getTeamMap().size() > 0) {
                for(TeamInfo teamInfo : game.getTeamMap().values()) {
                    sender.sendMessage((teamInfo.getColor() != null ? teamInfo.getColor() : "") + teamInfo.getName() + "/" + teamInfo.getScoreboardTeam().getDisplayName());
                }
            } else {
                sender.sendMessage(ChatColor.WHITE + " - none - ");
            }

        } else if("setdisplayname".equalsIgnoreCase(args[0])) {
            if(args.length > 2) {
                TeamInfo teamInfo = game.getTeam(args[1]);
                if(teamInfo != null) {
                    String displayname = args[2];
                    for(int i = 3; i > args.length; i++) {
                        displayname += " " + args[i];
                    }
                    teamInfo.getScoreboardTeam().setDisplayName(displayname);
                    sender.sendMessage(ChatColor.GREEN + "Set displayname of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getScoreboardTeam().getDisplayName());
                    game.toConfig(teamInfo);
                } else {
                    sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " found!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team setdisplayname <name> <displayname...>");
            }

        } else if("setcolor".equalsIgnoreCase(args[0])) {
            if(args.length > 2) {
                TeamInfo teamInfo = game.getTeam(args[1]);
                if(teamInfo != null) {
                    if(teamInfo.setColor(args[2])) {
                        sender.sendMessage(ChatColor.GREEN + "Set color of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + teamInfo.getColor() + teamInfo.getColor().name());
                        game.toConfig(teamInfo);
                    } else {
                        List<String> colorList = new ArrayList<String>();
                        for(ChatColor color : ChatColor.values()) {
                            colorList.add(color.getName());
                        }
                        sender.sendMessage(ChatColor.YELLOW + args[2].toUpperCase() + ChatColor.RED + " is not valid color string! (Colors: " + colorList.stream().collect(Collectors.joining(", ")) + ")");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " found!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team setcolor <name> <color>");
            }

        } else if("setblock".equalsIgnoreCase(args[0])) {
            if(args.length > 1) {
                TeamInfo teamInfo = game.getTeam(args[1]);
                if(teamInfo != null) {
                    if(args.length > 2) {
                        if(teamInfo.setBlock(args[2])) {
                            sender.sendMessage(ChatColor.GREEN + "Set block of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getBlockMaterial() + ":" + teamInfo.getBlockData());
                            game.toConfig(teamInfo);
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + args[2].toUpperCase() + ChatColor.RED + " is not valid block string! (material:data)");
                        }
                    } else if(sender instanceof Player) {
                        Block block = ((Player) sender).getTargetBlock(ImmutableSet.of(Material.AIR), 20);
                        teamInfo.setBlock(block);
                        sender.sendMessage(ChatColor.GREEN + "Set block of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + teamInfo.getBlockMaterial() + ":" + teamInfo.getBlockData());
                        game.toConfig(teamInfo);
                    } else {
                        sender.sendMessage("Usage: /" + command + " team setblock <name> [<material:data>]");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " found!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team setblock <name> [<material:data>]");
            }

        } else if("set".equalsIgnoreCase(args[0])) {
            if(args.length > 2) {
                TeamInfo teamInfo = game.getTeam(args[1]);
                if(teamInfo != null) {
                    String type = args[2];
                    LocationInfo loc = null;
                    if(args.length == 3) {
                        if(sender instanceof Player) {
                            loc = new LocationInfo(((Player) sender).getLocation());
                        } else {
                            sender.sendMessage("To run this command from the console use \"" + command + " team set <name> <loctype> <world> <x> <y> <z> [<pitch> <yaw>]\"");
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
                            loc = new LocationInfo(args[3], x, y, z, pitch, yaw);
                        } catch(NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "The input " + errorArg + " is not a valid number!");
                            return true;
                        }
                    }
                    if(loc != null) {
                        if ("spawn".equalsIgnoreCase(type)) {
                            teamInfo.setSpawn(loc);
                        } else if ("point".equalsIgnoreCase(type)) {
                            teamInfo.setPoint(loc);
                        } else if ("pos1".equalsIgnoreCase(type)) {
                            if (teamInfo.getRegion().getPos2() != null && !teamInfo.getRegion().getPos2().getWorldName().equalsIgnoreCase(loc.getWorldName())) {
                                sender.sendMessage(ChatColor.RED + "Warning: Position 1 has to be in the same world as position 2! (Pos 2 world: " + teamInfo.getRegion().getPos2().getWorldName() + ")");
                            }
                            teamInfo.getRegion().setPos1(loc);
                        } else if ("pos2".equalsIgnoreCase(type)) {
                            if (teamInfo.getRegion().getPos1() != null && !teamInfo.getRegion().getPos1().getWorldName().equalsIgnoreCase(loc.getWorldName())) {
                                sender.sendMessage(ChatColor.RED + "Warning: Position 2 has to be in the same world as position 1! (Pos 1 world: " + teamInfo.getRegion().getPos1().getWorldName() + ")");
                            }
                            teamInfo.getRegion().setPos2(loc);
                        } else if ("joinpos1".equalsIgnoreCase(type)) {
                            if (teamInfo.getJoinRegion().getPos2() != null && !teamInfo.getJoinRegion().getPos2().getWorldName().equalsIgnoreCase(loc.getWorldName())) {
                                sender.sendMessage(ChatColor.RED + "Warning: Position 1 has to be in the same world as position 2! (Pos 2 world: " + teamInfo.getJoinRegion().getPos2().getWorldName() + ")");
                            }
                            teamInfo.getJoinRegion().setPos1(loc);
                        } else if ("joinpos2".equalsIgnoreCase(type)) {
                            if (teamInfo.getJoinRegion().getPos1() != null && !teamInfo.getJoinRegion().getPos1().getWorldName().equalsIgnoreCase(loc.getWorldName())) {
                                sender.sendMessage(ChatColor.RED + "Warning: Position 2 has to be in the same world as position 1! (Pos 1 world: " + teamInfo.getJoinRegion().getPos1().getWorldName() + ")");
                            }
                            teamInfo.getJoinRegion().setPos2(loc);
                        } else {
                            sender.sendMessage(ChatColor.RED + type + " is not a valid team location setting. Valid ones are spawn, point, pos1 or pos2!");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Set " + type + " location of team " + ChatColor.WHITE + teamInfo.getName() + ChatColor.GREEN + " to " + ChatColor.WHITE + loc);
                        game.toConfig(teamInfo);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Could not set " + type + " location for team " + ChatColor.WHITE + teamInfo.getName());
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "No team with the name " + ChatColor.WHITE + args[1] + ChatColor.RED + " found!");
                }
            } else {
                sender.sendMessage("Usage: /" + command + " team set <name> <loctype> [<world> <x> <y> <z> [<pitch> <yaw>]]");
            }

        } else {
            return false;
        }
        return true;
    }
}
