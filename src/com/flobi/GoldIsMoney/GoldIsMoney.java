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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
	private static String balanceMessage = "You have: %g";
	private static String balanceMessagePermsFail = "&6You cannot use item currency at this time.";
	private static Map<Long, String> currencyFamily = new TreeMap<Long, String>();
	private static Map<Long, String> currencyFamilyReverse = new TreeMap<Long, String>(Collections.reverseOrder());
	
	private static boolean allowCreative = false;

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
            @EventHandler
            public void playerJoin(PlayerJoinEvent event) {
            	loadOfflineBalance(event.getPlayer());
            }
            @EventHandler
            public void playerQuit(PlayerQuitEvent event) {
            	saveOfflineBalance(event.getPlayer().getName());
            }
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

        // Load up the Plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
        getLogger().info("GoldIsMoney has been enabled!");

        getCommand("goldismoney").setExecutor(this);
        getCommand("balance").setExecutor(this);
        getCommand("money").setExecutor(this);
	}

    public void onDisable(){ 
    	getLogger().info("GoldIsMoney has been disabled.");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (command.getName().equalsIgnoreCase("goldismoney")) {
        	if (args.length < 1 || args[0].equalsIgnoreCase("reload")) return false;
        } else if (command.getName().equalsIgnoreCase("money") || command.getName().equalsIgnoreCase("balance")) {
        	if (sender instanceof Player) {
        		Player player = (Player) sender;
            	if (!hasPermission(player.getName())) {
                	player.sendMessage(ChatColor.translateAlternateColorCodes('&', balanceMessagePermsFail.replaceAll("%g", format(getBalance(player.getName())))));
            		return false;
            	}
            	player.sendMessage(ChatColor.translateAlternateColorCodes('&', balanceMessage.replaceAll("%g", format(getBalance(player.getName())))));
            	return true;
        	}
        }
        return false;
    }

    // Access
    public static long getBalance(String playerName) {
    	if (!hasPermission(playerName)) return 0;
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
    	if (!hasPermission(playerName)) return false;
    	return getBalance(playerName) >= amount;
    }
    
    private static void setBalance(String playerName, long amount) {
    	if (!hasPermission(playerName)) return;
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
    	if (!hasPermission(playerName)) return;
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
    	if (!hasPermission(playerName)) return false;
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
    	if (!hasPermission(playerName)) return;
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
		int stackMax;
		long itemWorth;
		String itemKey;
		
		items = inventory.getContents();
		if (difference < 0) {
			difference = Math.abs(difference);
			
			player.sendMessage("Withdrawing " + difference);
			
			for (Entry<Long, String> pair: currencyFamily.entrySet()) {
				if (difference <= 0) break;

				itemWorth = pair.getKey();
				itemKey = pair.getValue();
				
				for (ItemStack item : items) {
    				if (difference <= 0) break;

    				if (isItem(itemKey, item)) {
						stackCount = item.getAmount();
	    				if (stackCount * itemWorth > difference) {
	    					// This stack is more than enough to cover our debt:
	    					int removedItems = (int) Math.ceil((double) difference / (double) itemWorth);
	    					if (stackCount - removedItems <= 0) {
		    					item.setAmount(-1);
		    					player.sendMessage("Removed " + itemKey + " stack (overflow) of " + stackCount + " worth " + (stackCount * itemWorth));
	    					} else {
		    					item.setAmount(stackCount - removedItems);
		    					player.sendMessage("Removed " + removedItems + " worth " + (removedItems * itemWorth) + " from " + itemKey + " stack of " + stackCount);
	    					}
	    					difference -= removedItems * itemWorth;
	    					break;
	    				}
    					// Owe this or more than this, take the stack.
	    				item.setAmount(-1);
	    				player.sendMessage("Removed " + itemKey + " stack of " + stackCount + " worth " + (stackCount * itemWorth));
    					difference -= stackCount * itemWorth;
					}
				}
			}
			
			// Just in case we overdrew, actually a high probability, give back change.
			difference = 0 - difference;
			player.sendMessage("Change is " + difference);
    	}
		
		
		for (Entry<Long, String> pair: currencyFamilyReverse.entrySet()) {
			itemWorth = pair.getKey();
			if (difference < itemWorth) continue;

			itemKey = pair.getValue();
			stackMax = maxStackSize(itemKey);

			for (ItemStack item : items) {
				if (difference < itemWorth) break;

				if (item != null && isItem(itemKey, item) && item.getAmount() < stackMax) {
					stackCount = item.getAmount();
					if (stackCount < 0) stackCount = 0;
					item.setAmount((int) Math.min(stackMax, stackCount + Math.floor((double) difference / (double) itemWorth)));
					difference -= (item.getAmount() - stackCount) * itemWorth;
				}
			}
		}
		
		// Remove any empty stacks.
		for (ItemStack item : items) {
			if (item != null && item.getAmount() <= 0) inventory.remove(item);
		}
		
		for (Entry<Long, String> pair: currencyFamilyReverse.entrySet()) {
			itemWorth = pair.getKey();
			if (difference < itemWorth) continue;

			itemKey = pair.getValue();
			stackMax = maxStackSize(itemKey);

			// Refresh inventory contents.
			items = inventory.getContents();
			for (ItemStack item : items) {
				if (difference < itemWorth) break;

				if (item == null) {
					stackCount = (int) Math.min(stackMax, Math.floor((double) difference / (double) itemWorth));
					inventory.addItem(getItemStack(itemKey, stackCount));
					difference -= stackCount * itemWorth;
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
    	if (!hasPermission(playerName)) return;
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
			balance += getStackValue(item);
		}
    	
    	return balance;
    }
    
	private static long getStackValue(ItemStack item) {
		if (item == null) return 0;
		for (Entry<Long, String> pair: currencyFamily.entrySet()) {
			if (isItem(pair.getValue(), item)) {
				return item.getAmount() * pair.getKey();
			}
		}
		return 0;
	}
	
	private static boolean isItem(String itemKey, ItemStack item) {
		if (item == null) return false;
		return itemKey.equalsIgnoreCase(Integer.toString(item.getTypeId())) || itemKey.equalsIgnoreCase(item.getTypeId() + ";" + item.getDurability());
	}

	private static ItemStack getItemStack(String itemKey, int quantity) {
		int typeId = 0;
		short damage = 0;
		String[] keyParts = itemKey.split(";");
		typeId = Integer.parseInt(keyParts[0]);
		if (keyParts.length > 1) {
			damage = Short.parseShort(keyParts[1]);
		}
		ItemStack item = new ItemStack(typeId, quantity, damage);
		return item;
	}

	private static int maxStackSize(String itemKey) {
		int typeId;
		String[] keyParts = itemKey.split(";");
		typeId = Integer.parseInt(keyParts[0]);
		ItemStack item = new ItemStack(typeId);
		return item.getMaxStackSize();
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
	  	    	plugin.getLogger().severe("Can't save file, " + filename);
	    		return;
	  	    }
			finally {
				output.close();
			}
  	    }  
  	    catch(IOException ex){
  	    	plugin.getLogger().severe("Can't save file, " + filename);
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
		String currencyFamiliyName = ""; 
		
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
		allowCreative = config.getBoolean("allow-creative");
		currencyFamiliyName = config.getString("currency-family");

		// Update file in resource folder.
		FileConfiguration cleanConfig = new YamlConfiguration();
		Map<String, Object> configValues = config.getDefaults().getValues(false);
		for (Map.Entry<String, Object> configEntry : configValues.entrySet()) {
			cleanConfig.set(configEntry.getKey(), config.get(configEntry.getKey()));
		}

		try {
			cleanConfig.save(configFile);
		} catch(IOException ex) {
			plugin.getLogger().severe("Cannot save config.yml");
		}
		
		// Process currency family:
		currencyFamily.clear();
		if (currencyFamiliyName.equalsIgnoreCase("emerald")) {
			plugin.getLogger().info("Using emeralds for money.");
			currencyFamily.put(1L, "388");
			currencyFamily.put(9L, "133");
		} else if (currencyFamiliyName.equalsIgnoreCase("diamond")) {
			plugin.getLogger().info("Using diamonds for money.");
			currencyFamily.put(1L, "264");
			currencyFamily.put(9L, "57");
		} else if (currencyFamiliyName.equalsIgnoreCase("iron")) {
			plugin.getLogger().info("Using iron for money.");
			currencyFamily.put(1L, "265");
			currencyFamily.put(9L, "42");
		} else if (currencyFamiliyName.equalsIgnoreCase("lapis")) {
			plugin.getLogger().info("Using lapis lazuli for money.");
			currencyFamily.put(1L, "351;4");
			currencyFamily.put(9L, "16");
		} else if (currencyFamiliyName.equalsIgnoreCase("snow")) {
			plugin.getLogger().info("Using snow for money.");
			currencyFamily.put(1L, "332");
			currencyFamily.put(4L, "80");
		} else if (currencyFamiliyName.equalsIgnoreCase("clay")) {
			plugin.getLogger().info("Using clay for money.");
			currencyFamily.put(1L, "337");
			currencyFamily.put(4L, "82");
		} else if (currencyFamiliyName.equalsIgnoreCase("custom")) {
			boolean hasBaseValue = false;
			
			ConfigurationSection customCurrency = config.getConfigurationSection("custom-currency");
			Set<String> customCurrencyBlocks = customCurrency.getKeys(false);
			
			if (customCurrencyBlocks != null) {
				for (String customCurrencyBlock : customCurrencyBlocks) {
					long blockWorth = customCurrency.getLong(customCurrencyBlock);
					if (!isValidItemIdFormat(customCurrencyBlock)) {
						plugin.getLogger().warning("Ignoring invalid custom deomination, '" + customCurrencyBlock + "': " + blockWorth + ".");
					} else if (currencyFamily.containsKey(blockWorth)) {
						plugin.getLogger().warning("Ignoring duplicate custom valuation, '" + customCurrencyBlock + "': " + blockWorth + ".");
					} else {
						currencyFamily.put(blockWorth, customCurrencyBlock);
						if (blockWorth == 1L) hasBaseValue = true;
					}
				}
				
				if (currencyFamily.size() == 0) {
					plugin.getLogger().warning("No valid items in custom currency.  Using gold for money.");
				} else if (!hasBaseValue) {
					currencyFamily.clear();
					plugin.getLogger().warning("Custom currency requires base value.  Using gold for money.");
				} else {
					plugin.getLogger().info("Using custom item list for money.");
				}
			}
		} else if (currencyFamiliyName.equalsIgnoreCase("gold")) {
			plugin.getLogger().info("Using gold for money.");
		} else {
			plugin.getLogger().warning("Unknown currency family, '" + currencyFamiliyName + "'.  Using gold for money.");
		}
		
		if (currencyFamily.isEmpty()) {
			currencyFamily.put(1L, "371");
			currencyFamily.put(9L, "266");
			currencyFamily.put(81L, "41");
		}
		currencyFamilyReverse.clear();
		currencyFamilyReverse.putAll(currencyFamily);
    }
    
    private static boolean isValidItemIdFormat(String itemId) {
    	if (itemId.contains(";")) {
    		String[] itemIdParts = itemId.split(";");
    		if (itemIdParts.length != 2) return false;
    		try {
    			Integer.parseInt(itemIdParts[0]);
    			Integer.parseInt(itemIdParts[1]);
    			return true;
    		} catch (NumberFormatException e) {
				return false;
			}
    	} else {
    		try {
    			Integer.parseInt(itemId);
    			return true;
    		} catch (NumberFormatException e) {
				return false;
			}
    	}
    }

    private static boolean hasPermission(String playerName) {
    	Player player = server.getPlayer(playerName);
    	if (player == null) return OfflineBalance.containsKey(playerName);
    	if (player.getGameMode() == GameMode.CREATIVE && !allowCreative) return false;
    	return player.hasPermission("goldismoney.use");
    	
    }
}