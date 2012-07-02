package com.flobi.GoldIsMoney;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldIsMoney extends JavaPlugin {
	private static Map<String, Long> SystemOwesPlayer = new HashMap<String, Long>();
	private static Map<String, Long> OfflineBalance = new HashMap<String, Long>();
	private static Server server;
	
	// Names:
	private static String nameSingular = "gnugus";
	private static String namePlural = "gnugi";
	private static String formatSingular = "%n gnugus";
	private static String formatPlural = "%n gnugi";

	private static File dataFolder;
	private static Plugin plugin;

	// Setup
	public void onEnable(){
		plugin = this;

		dataFolder = getDataFolder();
		if (!dataFolder.isDirectory()) dataFolder.mkdir(); 
		loadConfig();
		
		server = getServer();
        server.getPluginManager().registerEvents(new Listener() {
            @SuppressWarnings("unused")
			@EventHandler
            public void playerJoin(PlayerJoinEvent event) {
            	loadOfflineBalance(event.getPlayer());
            	getLogger().info(event.getPlayer().getName() + " has joined (GoldIsMoney) and has " + getBalance(event.getPlayer().getName()) + ".");
            }
            @SuppressWarnings("unused")
			@EventHandler
            public void playerQuit(PlayerQuitEvent event) {
            	saveOfflineBalance(event.getPlayer().getName());
            	getLogger().info(event.getPlayer().getName() + " has quit (GoldIsMoney) and has " + getBalance(event.getPlayer().getName()) + ".");
            }
            @SuppressWarnings("unused")
			@EventHandler
			public void inventoryClickEvent(InventoryClickEvent event) {
            	final String playerName = event.getWhoClicked().getName();
            	getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
         		   public void run() {
                       	offloadSystemIOU(playerName);
        		   }
        		}, 1L);
            }
        }, this);
        SystemOwesPlayer = loadMapStringLong("SystemOwesPlayer.ser");
        OfflineBalance = loadMapStringLong("OfflineBalance.ser");

        getLogger().info("GoldIsMoney has been enabled!");
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

    public static boolean has(String playerName, long amount) {
    	return getBalance(playerName) >= amount;
    }
    
    private static void setBalance(String playerName, long amount) {
    	amount = Math.max(0, amount);

    	Player player = getPlayer(playerName);
    	
    	if (player == null) {
        	if (OfflineBalance.containsKey(playerName)) {
        		OfflineBalance.put(playerName, amount);
        		saveObject(OfflineBalance, "OfflineBalance.ser");
        	}
        	return;
    	}
    	
    	setInventoryBalance(player, amount);
    	
    	return;
    }

    public static void withdrawPlayer(String playerName, long amount) {
    	if (amount <= 0) return;
    	long newBalance = getBalance(playerName) - amount;
    	setBalance(playerName, newBalance);
    }

    public static void depositPlayer(String playerName, long amount) {
    	if (amount <= 0) return;
    	long newBalance = getBalance(playerName) + amount;
    	setBalance(playerName, newBalance);
    }

    public static String format(long amount) {
    	if (amount == 1) {
	    	return formatSingular.replaceAll("%n", amount + "");
    	}
    	return formatPlural.replaceAll("%n", amount + "");
    }

    public static String currencyNameSingular() {
    	return nameSingular;
    }

    public static String currencyNamePlural() {
    	return namePlural;
    }
    
    public static boolean hasAccount(String playerName) {
    	return getPlayer(playerName) != null || OfflineBalance.containsKey(playerName);
    }
    
    // Utility
    private static Player getPlayer(String playerName) {
    	Player player = server.getPlayer(playerName);
    	if (player == null || !player.isOnline()) {
    		player = null;
    	}
    	return player;
    }

	private static void offloadSystemIOU(String playerName) {
		Player player = server.getPlayer(playerName);
    	if (SystemOwesPlayer.containsKey(playerName)) {
    		setInventoryBalance(player, getBalance(playerName));
    	}
    }
    
    private static void loadOfflineBalance(Player player) {
    	String playerName = player.getName();
    	if (OfflineBalance.containsKey(playerName)) {
    		setInventoryBalance(player, OfflineBalance.get(playerName));
    		OfflineBalance.remove(playerName);
    		saveObject(OfflineBalance, "OfflineBalance.ser");
    	}
    }
    private static void setInventoryBalance(Player player, long newBalance) {
    	String playerName = player.getName();
		PlayerInventory inventory = player.getInventory();
		
    	long oldBalance = getInventoryBalance(inventory);
    	
    	// New balance should include any system owed money.
    	if (SystemOwesPlayer.containsKey(playerName)) {
    		SystemOwesPlayer.remove(playerName);
    		saveObject(SystemOwesPlayer, "SystemOwesPlayer.ser");
    	}

    	if (newBalance == oldBalance) return;
    	
    	long difference = newBalance - oldBalance;
		ItemStack[] items;
		int stackCount;
		
		if (difference < 0) {
			// This should make the math easier:
			difference = 0 - difference;
			
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
			
			int nuggetMod = (int) (9 - (difference - nuggetCount) % 9) % 9;
			int ingotMod = (int) (9 - ((((difference + nuggetMod - 9) / 9) - ingotCount) % 9)) % 9;
			
			if (nuggetCount == 0) {
				// There are no nugget stacks, calculate the mod as change.
				change += nuggetMod;
				difference += nuggetMod;
				ingotMod += 8;
				ingotMod %= 9;
			} else {
				// Remove nuggets first:
	    		for (ItemStack item : items) {
	    			if (item != null && item.getTypeId() == 371) {
						stackCount = item.getAmount();
	    				
	    				if (stackCount > difference) {
	    					// This stack is more than enough to cover our debt:
	    					item.setAmount((int) (stackCount - difference));
	    					difference = 0;
	    					break;
	    				}
	    				if (stackCount == nuggetCount && nuggetMod > 0) {
	    					// Last stack standing, leave the nuggetMod.
	    					item.setAmount(nuggetMod);
	    					nuggetCount -= stackCount - nuggetMod;
	    					difference -= stackCount - nuggetMod;
	    					break;
	    				} else {
	    					// Owe this or more than this, take the stack.
	    					inventory.remove(item);
	    					nuggetCount -= stackCount;
	    					difference -= stackCount;
	    				}
	    				if (difference == 0) {
	    					break;
	    				}
	    			}
	    		}
			}
    		if (difference > 0) {
	    		// The difference is now in ingot measurement.
	    		difference /= 9;

				if (ingotCount == 0) {
					// There are no ingot stacks, calculate the mod as change.
					change += ingotMod * 9;
					difference += ingotMod;
				} else {
					// Remove ingots second:
		    		for (ItemStack item : items) {
		    			if (item != null && item.getTypeId() == 266) {
							stackCount = item.getAmount();
		    				
		    				if (stackCount > difference) {
		    					// This stack is more than enough to cover our debt:
		    					item.setAmount((int) (stackCount - difference));
		    					difference = 0;
		    					break;
		    				}
		    				if (stackCount == ingotCount && ingotMod > 0) {
		    					// Last stack standing, leave the ingotMod.
		    					item.setAmount(ingotMod);
		    					ingotCount -= stackCount - ingotMod;
		    					difference -= stackCount - ingotMod;
		    					break;
		    				} else {
		    					// Owe this or more than this, take the stack.
		    					inventory.remove(item);
		    					ingotCount -= stackCount;
		    					difference -= stackCount;
		    				}
		    				if (difference == 0) {
		    					break;
		    				}
		    			}
		    		}
				}
    		}
    		if (difference > 0) {
	    		// The difference is now in block measurement.
	    		difference /= 9;

				// Remove blocks last:
	    		for (ItemStack item : items) {
	    			if (item != null && item.getTypeId() == 41) {
						stackCount = item.getAmount();
	    				
	    				if (stackCount > difference) {
	    					// This stack is more than enough to cover our debt:
	    					item.setAmount((int) (stackCount - difference));
	    					difference = 0;
	    					break;
	    				}
    					// Owe this or more than this, take the stack.
    					inventory.remove(item);
    					difference -= stackCount;
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
	    				item.setAmount((int) Math.min(64, stackCount + Math.floor((double) difference / 81)));
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
        				item.setAmount((int) Math.min(64, stackCount + Math.floor((double) difference / 9)));
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
        				item.setAmount((int) Math.min(64, stackCount + difference));
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
        				stackCount = (int) Math.min(64, Math.floor((double) difference / 81));
        				inventory.addItem(new ItemStack(41, stackCount));
        				difference -= stackCount * 81;
        			}
        		}
        	}
    		// Gotta refresh this because there are spots that were null that aren't anymore.
			items = inventory.getContents();
    		if (difference > 8) {
    			// Fill as many ingots as possible.
        		for (ItemStack item : items) {
        			if (difference < 9) break;
        			if (item == null) {
        				// Got space for a stack of ingots.
        				stackCount = (int) Math.min(64, Math.floor((double) difference / 9));
        				inventory.addItem(new ItemStack(266, stackCount));
        				difference -= stackCount * 9;
        			}
        		}
        	}
    		// Gotta refresh this because there are spots that were null that aren't anymore.
			items = inventory.getContents();
    		if (difference > 0) {
    			// Fill as many nuggets as possible.
        		for (ItemStack item : items) {
        			if (difference < 0) break;
        			if (item == null) {
        				// Got space for a stack of nuggets.
        				stackCount = (int) Math.min(64, Math.floor((double) difference));
        				inventory.addItem(new ItemStack(371, stackCount));
        				difference -= stackCount;
        			}
        		}
        	}
    	}
		
		if (difference > 0) {
			// They didn't have enough space.
			SystemOwesPlayer.put(playerName, difference);
    		saveObject(SystemOwesPlayer, "SystemOwesPlayer.ser");
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
    		saveObject(SystemOwesPlayer, "SystemOwesPlayer.ser");
    	}
    	
    	OfflineBalance.put(playerName, balance);
		saveObject(OfflineBalance, "OfflineBalance.ser");
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
    
	private static void saveObject(Object arraylist, String filename) {
    	File saveFile = new File(dataFolder, filename);
    	
    	try {
			//use buffering
    		if (saveFile.exists()) saveFile.delete();
			OutputStream file = new FileOutputStream(saveFile.getAbsolutePath());
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(arraylist);
			}
	  	    catch(IOException ex){
	  	    	server.getConsoleSender().sendMessage("Can't save file, " + filename);
	    		return;
	  	    }
			finally {
				output.close();
			}
  	    }  
  	    catch(IOException ex){
  	    	server.getConsoleSender().sendMessage("Can't save file, " + filename);
    		return;
  	    }
	}
	@SuppressWarnings({ "unchecked", "finally" })
	private static Map<String, Long> loadMapStringLong(String filename) {
    	File saveFile = new File(dataFolder, filename);
    	Map<String, Long> importedObjects = new HashMap<String, Long>();
    	try {
			//use buffering
			if (saveFile.exists()) {
				InputStream file = new FileInputStream(saveFile.getAbsolutePath());
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream (buffer);
				importedObjects = (Map<String, Long>) input.readObject();
				input.close();
			}
  	    }  
		finally {
  	    	return importedObjects;
		}
	}
    /**
	 * Loads config.yml and language.yml configuration files.
	 */
    private static void loadConfig() {
		File configFile = null;
		InputStream defConfigStream = null;
		YamlConfiguration defConfig = null;
		YamlConfiguration config = null;
		
		defConfigStream = plugin.getResource("config.yml");
    	configFile = new File(dataFolder, "config.yml");
	    config = YamlConfiguration.loadConfiguration(configFile);
	 
		// Look for defaults in the jar
	    if (defConfigStream != null) {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        defConfigStream = null;
	    }
	    if (defConfig != null) {
	    	config.setDefaults(defConfig);
	    }
	    
		nameSingular = config.getString("name-singular");
		namePlural = config.getString("name-plural");
		formatSingular = config.getString("format-singular");
		formatPlural = config.getString("format-plural");

		// Update file in resource folder.
		FileConfiguration cleanConfig = new YamlConfiguration();
		Map<String, Object> configValues = config.getDefaults().getValues(true);
		for (Map.Entry<String, Object> configEntry : configValues.entrySet()) {
			cleanConfig.set(configEntry.getKey(), config.get(configEntry.getKey()));
		}

		try {
			cleanConfig.save(configFile);
		} catch(IOException ex) {
			server.getLogger().severe("Cannot save config.yml");
		}
    }
}