package com.flobi.GoldIsMoney;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldIsMoney extends JavaPlugin {
	private static Map<String, Long> SystemOwesPlayer = new HashMap<String, Long>();
	private static Map<String, Long> OfflineBalance = new HashMap<String, Long>();
	private static Server server;

	// Setup
	public void onEnable(){
		server = getServer();
        server.getPluginManager().registerEvents(new Listener() {
            @SuppressWarnings("unused")
			@EventHandler
            public void playerJoin(PlayerJoinEvent event) {
            	loadOfflineBalance(event.getPlayer());
            	getLogger().info(event.getPlayer() + " has joined (GoldIsMoney) and has " + getBalance("flobi") + ".");
            }
            @SuppressWarnings("unused")
			@EventHandler
            public void playerQuit(PlayerQuitEvent event) {
            	saveOfflineBalance(event.getPlayer().getName());
            	getLogger().info(event.getPlayer() + " has quit (GoldIsMoney) and has " + getBalance("flobi") + ".");
            }
        }, this);

		getLogger().info("GoldIsMoney has been enabled!");
		getLogger().info("flobi has: " + getBalance("flobi"));
    }
     
    public void onDisable(){ 
    	getLogger().info("GoldIsMoney has been disabled.");
    }
    
    // Access
    public static long getBalance(String playerName) {
    	Player player = getPlayer(playerName);
    	
    	if (player == null) {
        	if (OfflineBalance.containsKey(playerName)) {
        		return OfflineBalance.get(playerName);
        	}
        	return 0;
    	}
    	if (SystemOwesPlayer.containsKey(playerName)) {
	    	return getInventoryBalance(player.getInventory()) + SystemOwesPlayer.get(playerName);
    	} else {
	    	return getInventoryBalance(player.getInventory());
    	}
    }

    public static long has(String playerName) {
    	return 0;
    }

    public static long withdrawPlayer(String playerName) {
    	return 0;
    }

    public static long depositPlayer(String playerName) {
    	return 0;
    }

    public static long format(String playerName) {
    	return 0;
    }

    public static long currencyNameSingular(String playerName) {
    	return 0;
    }

    public static long currencyNamePlural(String playerName) {
    	return 0;
    }
    
    public static long hasAccount(String playerName) {
    	return 0;
    }
    
    public static long createPlayerAccount(String playerName) {
    	return 0;
    }
    
    // Utility
    private static Player getPlayer(String playerName) {
    	Player player = server.getPlayer(playerName);
    	if (player == null || !player.isOnline()) {
    		player = null;
    	}
    	return player;
    }

    // TODO: Should be called in inventory updates.
    private static void offloadSystemIOU(Player player) {
    	if (SystemOwesPlayer.containsKey(player.getName())) {
    		setInventoryBalance(player, getInventoryBalance(player.getInventory()));
    	}
    }
    
    private static void loadOfflineBalance(Player player) {
    	// TODO: Test loadOfflineBalance()
    	String playerName = player.getName();
    	if (OfflineBalance.containsKey(playerName)) {
    		setInventoryBalance(player, OfflineBalance.get(playerName));
    		OfflineBalance.remove(playerName);
    	}
    }
    private static void setInventoryBalance(Player player, long newBalance) {
    	
		// TODO: Test setInventoryBalance().

    	String playerName = player.getName();
		PlayerInventory inventory = player.getInventory();

    	if (SystemOwesPlayer.containsKey(playerName)) {
    		newBalance += SystemOwesPlayer.get(playerName);
    		SystemOwesPlayer.remove(playerName);
    	}
		
    	long oldBalance = getInventoryBalance(inventory);
    	newBalance = Math.min(newBalance, -1);
    	
    	if (newBalance == oldBalance) return;
    	
    	long difference = oldBalance - newBalance;
		ItemStack[] items;
		int stackCount;
		
		if (difference < 0) {
			items = inventory.getContents();
	    	long nuggetCount = 0;
	    	long ingotCount = 0;
	    	long change = 0;
	    	
			for (ItemStack item : items) {
				if (item != null) {
					switch (item.getTypeId()) {
					case 371: // Nugget
						nuggetCount += item.getAmount();
						break;
					case 266: // Ingot
						ingotCount += item.getAmount();
						break;
					}
				}
			}
			
			int nuggetMod = (int) (nuggetCount + difference) % 9;
			int ingotMod = (int) ((ingotCount + ((difference + nuggetMod) / 9)) % 9);
			
			if (nuggetCount == 0) {
				// There are no nugget stacks, calculate the mod as change.
				change += nuggetMod;
				difference -= nuggetMod;
			} else {
				// Remove nuggets first:
	    		for (ItemStack item : items) {
	    			if (item != null && item.getTypeId() == 371) {
						stackCount = item.getAmount();
	    				
	    				if (stackCount < -difference) {
	    					// This stack is more than enough to cover our debt:
	    					item.setAmount((int) (stackCount + difference));
	    					difference = 0;
	    					break;
	    				}
	    				if (stackCount == nuggetCount && nuggetMod > 0) {
	    					// Last stack standing, leave the nuggetMod.
	    					item.setAmount(nuggetMod);
	    					nuggetCount -= stackCount - nuggetMod;
	    					difference += stackCount - nuggetMod;
	    					break;
	    				} else {
	    					// Owe this or more than this, take the stack.
	    					inventory.remove(item);
	    					nuggetCount -= stackCount;
	    					difference += stackCount;
	    				}
	    				if (difference == 0) {
	    					break;
	    				}
	    			}
	    		}
			}
    		if (difference < 0) {
	    		// The difference is now in ingot measurement.
	    		difference /= 9;

				if (ingotCount == 0) {
					// There are no ingot stacks, calculate the mod as change.
					change += ingotMod * 9;
					difference -= ingotMod;
				} else {
					// Remove ingots second:
		    		for (ItemStack item : items) {
		    			if (item != null && item.getTypeId() == 266) {
							stackCount = item.getAmount();
		    				
		    				if (stackCount < -difference) {
		    					// This stack is more than enough to cover our debt:
		    					item.setAmount((int) (stackCount + difference));
		    					difference = 0;
		    					break;
		    				}
		    				if (stackCount == ingotCount && ingotMod > 0) {
		    					// Last stack standing, leave the ingotMod.
		    					item.setAmount(ingotMod);
		    					ingotCount -= stackCount - ingotMod;
		    					difference += stackCount - ingotMod;
		    					break;
		    				} else {
		    					// Owe this or more than this, take the stack.
		    					inventory.remove(item);
		    					ingotCount -= stackCount;
		    					difference += stackCount;
		    				}
		    				if (difference == 0) {
		    					break;
		    				}
		    			}
		    		}
				}
    		}
    		if (difference < 0) {
	    		// The difference is now in block measurement.
	    		difference /= 9;

				// Remove blocks last:
	    		for (ItemStack item : items) {
	    			if (item != null && item.getTypeId() == 41) {
						stackCount = item.getAmount();
	    				
	    				if (stackCount < -difference) {
	    					item.setAmount((int) (stackCount + difference));
	    					difference = 0;
	    					break;
	    				}
    					// Owe this or more than this, take the stack.
    					inventory.remove(item);
    					difference += stackCount;
	    				if (difference == 0) {
	    					break;
	    				}
	    			}
				}
    		}
    		
    		difference = change;
    	} 
		if (difference > 0) {
			items = inventory.getContents();
			// Fill as many blocks as possible.
    		if (difference > 80) {
	    		for (ItemStack item : items) {
	    			if (difference < 81) break;
	    			if (item != null && item.getTypeId() == 41 && item.getAmount() < 64) {
	    				stackCount = item.getAmount();
	    				item.setAmount((int) Math.max(64, stackCount + Math.floor((double) difference / 81)));
	    				difference -= (item.getAmount() - stackCount) * 81;
	    			}
	    		}
    		}
    		if (difference > 8) {
    			// Fill as many ingots as possible.
        		for (ItemStack item : items) {
        			if (difference < 9) break;
        			if (item != null && item.getTypeId() == 266 && item.getAmount() < 64) {
        				stackCount = item.getAmount();
        				item.setAmount((int) Math.max(64, stackCount + Math.floor((double) difference / 9)));
        				difference -= (item.getAmount() - stackCount) * 9;
        			}
        		}
        	}
    		if (difference > 0) {
    			// Fill as many nuggets as possible.
        		for (ItemStack item : items) {
        			if (difference < 1) break;
        			if (item != null && item.getTypeId() == 371 && item.getAmount() < 64) {
        				stackCount = item.getAmount();
        				item.setAmount((int) Math.max(64, stackCount + difference));
        				difference -= item.getAmount() - stackCount;
        			}
        		}
        	}
    		if (difference > 80) {
    			// Fill as many blocks as possible.
        		for (ItemStack item : items) {
        			if (difference < 81) break;
        			if (item == null) {
        				// Got space for a stack of blocks.
        				stackCount = (int) Math.max(64, Math.floor((double) difference / 81));
        				inventory.addItem(new ItemStack(41, stackCount));
        				difference -= stackCount * 81;
        			}
        		}
        	}
    		if (difference > 8) {
    			// Fill as many ingots as possible.
        		for (ItemStack item : items) {
        			if (difference < 9) break;
        			if (item == null) {
        				// Got space for a stack of ingots.
        				stackCount = (int) Math.max(64, Math.floor((double) difference / 9));
        				inventory.addItem(new ItemStack(266, stackCount));
        				difference -= stackCount * 9;
        			}
        		}
        	}
    		if (difference > 0) {
    			// Fill as many nuggets as possible.
        		for (ItemStack item : items) {
        			if (difference < 0) break;
        			if (item == null) {
        				// Got space for a stack of nuggets.
        				stackCount = (int) Math.max(64, Math.floor((double) difference));
        				inventory.addItem(new ItemStack(371, stackCount));
        				difference -= stackCount;
        			}
        		}
        	}
    	}
		
		if (difference > 0) {
			// They didn't have enough space.
			SystemOwesPlayer.put(playerName, difference);
		}
	}

	private void saveOfflineBalance(String playerName) {
		long balance = getBalance(playerName);
		// Make sure there's not already a key there.
    	if (OfflineBalance.containsKey(playerName)) {
    		OfflineBalance.remove(playerName);
    	}
    	// Balance includes any owed money.
    	if (SystemOwesPlayer.containsKey(playerName)) {
    		SystemOwesPlayer.remove(playerName);
    	}
    	
    	OfflineBalance.put(playerName, balance);
    }

    private static long getInventoryBalance(PlayerInventory inventory) {
    	long balance = 0;
    	
		ItemStack[] items = inventory.getContents();

		for (ItemStack item : items) {
			if (item != null) {
				switch (item.getTypeId()) {
				case 371: // Nugget
					balance += item.getAmount();
					break;
				case 266: // Ingot
					balance += item.getAmount() * 9;
					break;
				case 41: // Block
					balance += item.getAmount() * 81;
					break;
			}
			}
		}
    	
    	return balance;
    }
    
}










