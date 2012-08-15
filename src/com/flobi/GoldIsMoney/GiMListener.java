package com.flobi.GoldIsMoney;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class GiMListener implements Listener {
	private GoldIsMoney plugin;
	
	public GiMListener() {
		plugin = GiMUtility.plugin;
	}
	
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
    	String playerName = event.getPlayer().getName();
    	if (GoldIsMoney.hasAccount(playerName)) {
    		GoldIsMoney.getPlayerAccount(playerName).syncInventory();
    	} else {
    		GoldIsMoney.getPlayerAccount(playerName).initialize();
    	}
    }

    @EventHandler
	public void inventoryClickEvent(InventoryClickEvent event) {
    	final String playerName = event.getWhoClicked().getName();
    	plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
 		   public void run() {
 		    	if (GoldIsMoney.hasAccount(playerName)) {
 		    		GoldIsMoney.getPlayerAccount(playerName).syncInventory();
 		    	} else {
 		    		GoldIsMoney.getPlayerAccount(playerName).initialize();
 		    	}
		   }
		}, 1L);
    }
}
