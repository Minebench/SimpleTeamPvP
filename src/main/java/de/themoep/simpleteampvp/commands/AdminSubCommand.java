package de.themoep.simpleteampvp.commands;

import de.themoep.simpleteampvp.SimpleTeamPvP;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;

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
public class AdminSubCommand extends SubCommand {
    public AdminSubCommand(SimpleTeamPvP plugin) {
        super(plugin, plugin.getName().toLowerCase(), "admin",
                "[reload|save]",
                "Administrate the plugin"
        );
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0])) {
                if (plugin.reload()) {
                    plugin.getLogger().log(Level.INFO, sender.getName() + " reloaded the config!");
                    sender.sendMessage(ChatColor.GREEN + plugin.getName() + " reloaded!");
                } else {
                    plugin.getLogger().log(Level.INFO, sender.getName() + " tried to reload the config but failed?");
                    sender.sendMessage(ChatColor.RED + "Could not reload " + plugin.getName() + "!");
                }
            } else if ("save".equalsIgnoreCase(args[0])) {
                plugin.toConfig();
                plugin.getLogger().log(Level.INFO, sender.getName() + " wrote settings to disc!");
                sender.sendMessage(ChatColor.GREEN + "Settings of " + plugin.getName() + " wrote to disk!");
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
