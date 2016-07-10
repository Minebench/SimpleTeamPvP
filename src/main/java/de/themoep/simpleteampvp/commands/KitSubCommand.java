package de.themoep.simpleteampvp.commands;

import de.themoep.simpleteampvp.KitInfo;
import de.themoep.simpleteampvp.SimpleTeamPvP;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class KitSubCommand extends SubCommand {
    public KitSubCommand(SimpleTeamPvP plugin) {
        super(plugin, plugin.getName().toLowerCase(), "admin",
                "[create|remove|list|info|seticon|items]",
                "Create and edit kits"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length > 0) {

            if("gui".equalsIgnoreCase(args[0])) {
                if(sender instanceof Player) {
                    plugin.getKitGui().show((Player) sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "This command cannot be run by the console!");
                }

            } else if("create".equalsIgnoreCase(args[0])) {
                if(args.length > 1) {
                    String name = args[1];
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
                    sender.sendMessage("Usage: /" + getCommand() + " kit create <name...>");
                }

            } else if("remove".equalsIgnoreCase(args[0])) {
                if(args.length > 1) {
                    String name = args[1];
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
                    sender.sendMessage("Usage: /" + getCommand() + " kit remove <name...>");
                }

            } else if("list".equalsIgnoreCase(args[0])) {
                sender.sendMessage(ChatColor.GREEN + "Available kits:");
                for(KitInfo kit : plugin.getKitMap().values()) {
                    sender.sendMessage(kit.getName());
                }

            } else if("info".equalsIgnoreCase(args[0])) {
                if(args.length > 1) {
                    String name = args[1];
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
                    sender.sendMessage("Usage: /" + getCommand() + " kit info <name...>");
                }

            } else if("seticon".equalsIgnoreCase(args[0])) {
                if(sender instanceof Player) {
                    if(args.length > 1) {
                        String name = args[1];
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
                        sender.sendMessage("Usage: /" + getCommand() + " kit info <name...>");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command cannot be run by the console!");
                }

            } else if("items".equalsIgnoreCase(args[0])) {
                if(sender instanceof Player) {
                    if(args.length > 1) {
                        String name = args[2];
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
                            if("copyarmor".equalsIgnoreCase(args[1])) {
                                kit.setHelmet(player.getInventory().getHelmet());
                                kit.setChest(player.getInventory().getChestplate());
                                kit.setLegs(player.getInventory().getLeggings());
                                kit.setBoots(player.getInventory().getBoots());
                                sender.sendMessage(ChatColor.GREEN + "Copied your armor to the kit!");
                                plugin.toConfig(kit);

                            } else if("head".equalsIgnoreCase(args[1])) {
                                kit.setHelmet(player.getItemInHand());
                                sender.sendMessage(ChatColor.GREEN + "Set head to " + ChatColor.WHITE + (kit.getHelmet() == null ? "none" : kit.getHelmet().getType().toString().toLowerCase()));
                                plugin.toConfig(kit);

                            } else if("chest".equalsIgnoreCase(args[1])) {
                                kit.setChest(player.getItemInHand());
                                sender.sendMessage(ChatColor.GREEN + "Set chest to " + ChatColor.WHITE + (kit.getChest() == null ? "none" : kit.getChest().getType().toString().toLowerCase()));
                                plugin.toConfig(kit);

                            } else if("legs".equalsIgnoreCase(args[1])) {
                                kit.setLegs(player.getItemInHand());
                                sender.sendMessage(ChatColor.GREEN + "Set legs to " + ChatColor.WHITE + (kit.getLegs() == null ? "none" : kit.getLegs().getType().toString().toLowerCase()));
                                plugin.toConfig(kit);

                            } else if("feet".equalsIgnoreCase(args[1])) {
                                kit.setBoots(player.getItemInHand());
                                sender.sendMessage(ChatColor.GREEN + "Set feet to " + ChatColor.WHITE + (kit.getBoots() == null ? "none" : kit.getBoots().getType().toString().toLowerCase()));
                                plugin.toConfig(kit);

                            } else if("copytoolbar".equalsIgnoreCase(args[1])) {
                                for(int i = 0; i < 9; i++) {
                                    kit.setItem(i, player.getInventory().getItem(i));
                                }
                                sender.sendMessage(ChatColor.GREEN + "Copied your toolbar to the kit!");
                                plugin.toConfig(kit);

                            } else if("copyinv".equalsIgnoreCase(args[1])) {
                                kit.setHelmet(player.getInventory().getHelmet());
                                kit.setChest(player.getInventory().getChestplate());
                                kit.setLegs(player.getInventory().getLeggings());
                                kit.setBoots(player.getInventory().getBoots());
                                for(int i = 0; i < 36; i++) {
                                    kit.setItem(i, player.getInventory().getItem(i));
                                }
                                sender.sendMessage(ChatColor.GREEN + "Copied your entire inventory to the kit!");
                                plugin.toConfig(kit);

                            } else if("add".equalsIgnoreCase(args[1])) {
                                if(player.getItemInHand() != null) {
                                    kit.addItem(player.getItemInHand());
                                    sender.sendMessage(ChatColor.GREEN + "Added " + ChatColor.WHITE + player.getItemInHand().getType().toString().toLowerCase() + ChatColor.GREEN + " to the kit!");
                                    plugin.toConfig(kit);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Please hold an item in your hand to add to the kit!");
                                }

                            } else if("remove".equalsIgnoreCase(args[1])) {
                                if(player.getItemInHand() != null) {
                                    kit.getItems().remove(player.getItemInHand());
                                    sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + player.getItemInHand().getType().toString().toLowerCase() + ChatColor.GREEN + " from the kit!");
                                    plugin.toConfig(kit);
                                } else {
                                    sender.sendMessage(ChatColor.RED + "Please hold an item in your hand to remove from the kit!");
                                }

                            } else {
                                sender.sendMessage("Usage: /" + getCommand() + " kit items [copyarmor|head|chest|legs|feet|copytoolbar|copyinv|add|remove] <name...>");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "No kit with the name " + ChatColor.WHITE + name + ChatColor.RED + " exists!");
                        }

                    } else {
                        sender.sendMessage("Usage: /" + getCommand() + " kit items [copyarmor|head|chest|legs|feet|copytoolbar|copyinv|add|remove] <name...>");
                    }

                } else {
                    sender.sendMessage(ChatColor.RED + "This command cannot be run by the console!");
                }

            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
