package de.themoep.simpleteampvp;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

/**
 * SimpleTeamPvP
 * Copyright (C) 2015 Max Lee (https://github.com/Phoenix616/)
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
public class KitInfo {
    private String name;

    private ItemStack icon = null;
    private ItemStack generatedIcon = null;

    private ItemStack helmet = null;
    private ItemStack chest = null;
    private ItemStack legs = null;
    private ItemStack boots = null;

    private List<ItemStack> items = new ArrayList<ItemStack>();

    public KitInfo(String name) {
        this.name = name;
    }

    public KitInfo(String name, Player player) {
        this(name);
        helmet = player.getInventory().getHelmet();
        chest = player.getInventory().getChestplate();
        legs = player.getInventory().getLeggings();
        boots = player.getInventory().getBoots();
        for(int i = 0; i < 36; i++) {
            items.add(player.getInventory().getItem(i));
        }
    }

    public KitInfo(ConfigurationSection config) {
        this(config.getName());
        icon = config.getItemStack("icon");
        helmet = config.getItemStack("helmet");
        chest = config.getItemStack("chest");
        legs = config.getItemStack("legs");
        boots = config.getItemStack("boots");
        try {
            items = (List<ItemStack>) config.get("items", new ArrayList<ItemStack>());
        } catch(ClassCastException e) {
            Bukkit.getLogger().log(Level.WARNING, "[SimpleTeamPvP] Could not load items for kit " + name);
        }
    }

    public ItemStack getHelmet() {
        return helmet;
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChest() {
        return chest;
    }

    public void setChest(ItemStack chest) {
        this.chest = chest;
    }

    public ItemStack getLegs() {
        return legs;
    }

    public void setLegs(ItemStack legs) {
        this.legs = legs;
    }

    public ItemStack getBoots() {
        return boots;
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public List<ItemStack> addItem(ItemStack item) {
        for(int i = 0; i < items.size(); i++) {
            if(items.get(i) == null) {
                items.set(i, item);
                return items;
            }
        }
        items.add(item);
        return items;
    }

    public List<ItemStack> setItem(int i, ItemStack item) {
        while(items.size() <= i) {
            items.add(null);
        }
        items.set(i, item);
        return items;
    }

    public ItemStack getIcon() {
        if(generatedIcon == null)
            generateIcon();
        return generatedIcon;
    }

    private ItemStack generateIcon() {
        ItemStack i = icon;
        if(i == null)
            i = helmet;
        if(i == null)
            i = helmet;
        if(i == null)
            i = chest;
        if(i == null)
            i = boots;
        if(i == null && items.size() > 0)
            i = items.get(0);
        if(i == null) {
            i = new ItemStack(Material.LEATHER_HELMET);
            LeatherArmorMeta meta = (LeatherArmorMeta) i.getItemMeta();
            int r = (name.hashCode() & 0xFF0000) >> 16;
            int g = (name.hashCode() & 0x00FF00) >> 8;
            int b = name.hashCode() & 0x0000FF;
            meta.setColor(Color.fromRGB(r, g, b));
            i.setItemMeta(meta);
        }
        ItemMeta meta = i.getItemMeta();
        if(!meta.hasDisplayName()) {
            meta.setDisplayName(ChatColor.YELLOW + ChatColor.translateAlternateColorCodes('&', name));
            i.setItemMeta(meta);
        }
        generatedIcon = i;
        return generatedIcon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
        generateIcon();
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("icon", this.icon);
        data.put("helmet", this.helmet);
        data.put("chest", this.chest);
        data.put("legs", this.legs);
        data.put("boots", this.boots);
        data.put("items", this.items);
        return data;
    }
}
