package de.themoep.simpleteampvp;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
public class KitGui implements Listener {
    private final SimpleTeamPvP plugin;
    private ItemStack[] items;

    private Set<UUID> invOpen = new HashSet<UUID>();

    public KitGui(SimpleTeamPvP plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void generate() {
        List<KitInfo> kits = new ArrayList<KitInfo>(plugin.getKitMap().values());
        List<ItemStack> itemList = new ArrayList<ItemStack>();
        for(int i = 0; i < 9; i++) {
            itemList.add(null);
        }
        if(kits.size() == 0) {
            for(int i = 0; i < 9; i++) {
                itemList.add(null);
            }
        } else if(kits.size() == 1) {
            itemList.addAll(Arrays.asList(
                    null, null, null, null, kits.get(0).getIcon(), null, null, null, null
            ));
        } else if(kits.size() == 2) {
            itemList.addAll(Arrays.asList(
                    null, null, null, kits.get(0).getIcon(), null, kits.get(1).getIcon(), null, null, null
            ));
        } else if(kits.size() == 3) {
            itemList.addAll(Arrays.asList(
                    null, null, kits.get(0).getIcon(), null, kits.get(1).getIcon(), null, kits.get(2).getIcon(), null, null
            ));
        } else {
            for(KitInfo kit : kits) {
                itemList.add(null);
                itemList.add(kit.getIcon());
                if(itemList.size() % 9 == 0) {
                    for(int i = 0; i < 9; i++) {
                        itemList.add(null);
                    }
                }
            }
            while(itemList.size() % 9 != 0) {
                itemList.add(null);
            }
        }

        for(int i = 0; i < 9; i++) {
            itemList.add(null);
        }

        if(itemList.size() % 9 != 0) {
            while(itemList.size() % 9 != 0) {
                itemList.add(null);
            }
        }

        items = itemList.toArray(new ItemStack[itemList.size()]);
    }

    public void show(Player player) {
        if(!player.isOnline())
            return;

        if(plugin.getKitMap().size() == 0) {
            player.sendMessage(ChatColor.RED + "No Kits defined!");
            return;
        }
        Inventory inv = player.getServer().createInventory(null, items.length, ChatColor.BLUE + "Wähle ein Kit:");
        inv.setContents(items);
        player.openInventory(inv);
        invOpen.add(player.getUniqueId());
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player))
            return;

        if(!invOpen.contains(event.getWhoClicked().getUniqueId()))
            return;

        if(event.getClickedInventory() != event.getWhoClicked().getOpenInventory().getTopInventory()) {
            if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if(item != null && item.getType() != Material.AIR) {
            KitInfo kit = plugin.getKit(item);
            close((Player) event.getWhoClicked());
            if(kit == null) {
                plugin.getLogger().log(Level.WARNING, "Could not find a kit for item " + item.getType() + " in gui of player " + event.getWhoClicked().getName());
                event.getWhoClicked().sendMessage(ChatColor.RED  + "Could not find a kit for item " + item.getType() + "!");
            } else {
                plugin.applyKit(kit, (Player) event.getWhoClicked());
                event.getWhoClicked().addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 5, true));
            }
        }
    }

    private void close(Player player) {
        if(invOpen.contains(player.getUniqueId())) {
            invOpen.remove(player.getUniqueId());
            Inventory oldInv = player.getOpenInventory().getTopInventory();
            if(oldInv.getType() == InventoryType.ANVIL) {
                oldInv.setItem(0, null);
                oldInv.setItem(1, null);
                oldInv.setItem(2, null);
            }
            oldInv.clear();
            if(player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onItemDrag(InventoryDragEvent event) {
        if(invOpen.contains(event.getWhoClicked().getUniqueId()) && event.getInventory() == event.getWhoClicked().getOpenInventory().getTopInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if(event.getPlayer() instanceof Player && invOpen.contains(event.getPlayer().getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    if(invOpen.contains(event.getPlayer().getUniqueId())) {
                        show((Player) event.getPlayer());
                    }
                }
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerLoggout(PlayerQuitEvent event) {
        invOpen.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        invOpen.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if(invOpen.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
