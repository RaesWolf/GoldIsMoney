package com.flobi.GoldIsMoney2;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class GiMListener implements Listener {
	private GoldIsMoney plugin;
	
	public GiMListener() {
		plugin = GiMUtility.plugin;
	}
	
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
    	syncPlayerInventory(event.getPlayer().getName());
    }

    @EventHandler
	public void playerChangedWorldEvent(PlayerChangedWorldEvent event) {
    	syncPlayerInventory(event.getPlayer().getName());
    }

    @EventHandler
	public void inventoryClickEvent(InventoryClickEvent event) {
    	syncPlayerInventory(event.getWhoClicked().getName());
    }

    @EventHandler
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
    	syncPlayerInventory(event.getPlayer().getName());
    }

    @EventHandler
	public void playerDropItemEvent(PlayerDropItemEvent event) {
    	syncPlayerInventory(event.getPlayer().getName());
    }

    @EventHandler
	public void playerTeleportEvent(PlayerTeleportEvent event) {
    	syncPlayerInventory(event.getPlayer().getName());
    }

    @EventHandler
	public void playerDeathEvent(PlayerDeathEvent event) {
    	syncPlayerInventory(event.getEntity().getName());
    }
    
    private void syncPlayerInventory(final String playerName) {
    	plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
  		   public void run() {
  		    	if (GoldIsMoney.hasAccount(playerName)) {
  		    		GoldIsMoney.getPlayerAccount(playerName).syncInventory();
  		    	} else {
  		    		GoldIsMoney.createPlayerAccount(playerName);
  		    	}
 		   }
 		}, 2L);
    }
}
