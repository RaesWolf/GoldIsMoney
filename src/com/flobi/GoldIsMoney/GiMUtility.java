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
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class GiMUtility {
	public static File dataFolder;
	public static GoldIsMoney plugin;
    public static YamlConfiguration config;

	@SuppressWarnings({ "finally", "unchecked" })
	public static Map<String, PlayerAccount> loadMapStringPlayerAccount(String filename) {
    	File saveFile = new File(dataFolder, filename);
    	Map<String, PlayerAccount> importedObjects = new HashMap<String, PlayerAccount>();
    	try {
			//use buffering
			if (saveFile.exists()) {
				InputStream file = new FileInputStream(saveFile.getAbsolutePath());
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream (buffer);
				importedObjects = (Map<String, PlayerAccount>) input.readObject();
				input.close();
			}
  	    }  
		finally {
  	    	return importedObjects;
		}
	}

	@SuppressWarnings({ "finally", "unchecked" })
	public static Map<String, BankAccount> loadMapStringBankAccount(String filename) {
    	File saveFile = new File(dataFolder, filename);
    	Map<String, BankAccount> importedObjects = new HashMap<String, BankAccount>();
    	try {
			//use buffering
			if (saveFile.exists()) {
				InputStream file = new FileInputStream(saveFile.getAbsolutePath());
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream (buffer);
				importedObjects = (Map<String, BankAccount>) input.readObject();
				input.close();
			}
  	    }  
		finally {
  	    	return importedObjects;
		}
	}

	@SuppressWarnings({ "unchecked", "finally" })
	public static Map<String, Long> loadMapStringLong(String filename) {
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
	public static void saveObject(Object arraylist, String filename) {
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

	
	public static void setPlugin(GoldIsMoney goldIsMoney) {
		plugin = goldIsMoney;
	}

	public static void setDataFolder(File dataFolder) {
		GiMUtility.dataFolder = dataFolder;
		if (!dataFolder.isDirectory()) dataFolder.mkdir(); 
		
	}

	public static void loadConfig() {
		File configFile = null;
		InputStream defConfigStream = null;
		YamlConfiguration defConfig = null;
		
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
	    String currencyFamiliyName = config.getString("currency-family");
	    
	    GiMMoney.currencyFamily.clear();
		if (currencyFamiliyName.equalsIgnoreCase("emerald")) {
			plugin.getLogger().info("Using emeralds for money.");
			GiMMoney.currencyFamily.put(1L, "388");
			GiMMoney.currencyFamily.put(9L, "133");
		} else if (currencyFamiliyName.equalsIgnoreCase("diamond")) {
			plugin.getLogger().info("Using diamonds for money.");
			GiMMoney.currencyFamily.put(1L, "264");
			GiMMoney.currencyFamily.put(9L, "57");
		} else if (currencyFamiliyName.equalsIgnoreCase("iron")) {
			plugin.getLogger().info("Using iron for money.");
			GiMMoney.currencyFamily.put(1L, "265");
			GiMMoney.currencyFamily.put(9L, "42");
		} else if (currencyFamiliyName.equalsIgnoreCase("lapis")) {
			plugin.getLogger().info("Using lapis lazuli for money.");
			GiMMoney.currencyFamily.put(1L, "351;4");
			GiMMoney.currencyFamily.put(9L, "16");
		} else if (currencyFamiliyName.equalsIgnoreCase("snow")) {
			plugin.getLogger().info("Using snow for money.");
			GiMMoney.currencyFamily.put(1L, "332");
			GiMMoney.currencyFamily.put(4L, "80");
		} else if (currencyFamiliyName.equalsIgnoreCase("clay")) {
			plugin.getLogger().info("Using clay for money.");
			GiMMoney.currencyFamily.put(1L, "337");
			GiMMoney.currencyFamily.put(4L, "82");
		} else if (currencyFamiliyName.equalsIgnoreCase("custom")) {
			boolean hasBaseValue = false;
			
			ConfigurationSection customCurrency = config.getConfigurationSection("custom-currency");
			Set<String> customCurrencyBlocks = customCurrency.getKeys(false);
			
			if (customCurrencyBlocks != null) {
				for (String customCurrencyBlock : customCurrencyBlocks) {
					long blockWorth = customCurrency.getLong(customCurrencyBlock);
					if (!isValidItemIdFormat(customCurrencyBlock)) {
						plugin.getLogger().warning("Ignoring invalid custom deomination, '" + customCurrencyBlock + "': " + blockWorth + ".");
					} else if (GiMMoney.currencyFamily.containsKey(blockWorth)) {
						plugin.getLogger().warning("Ignoring duplicate custom valuation, '" + customCurrencyBlock + "': " + blockWorth + ".");
					} else {
						GiMMoney.currencyFamily.put(blockWorth, customCurrencyBlock);
						if (blockWorth == 1L) hasBaseValue = true;
					}
				}
				
				if (GiMMoney.currencyFamily.size() == 0) {
					plugin.getLogger().warning("No valid items in custom currency.  Using gold for money.");
				} else if (!hasBaseValue) {
					GiMMoney.currencyFamily.clear();
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
		
		if (GiMMoney.currencyFamily.isEmpty()) {
			GiMMoney.currencyFamily.put(1L, "371");
			GiMMoney.currencyFamily.put(9L, "266");
			GiMMoney.currencyFamily.put(81L, "41");
		}
		GiMMoney.currencyFamilyReverse.clear();
		GiMMoney.currencyFamilyReverse.putAll(GiMMoney.currencyFamily);
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

	public static void savePlayerAccountFile() {
		GiMUtility.saveObject(GoldIsMoney.playerAccounts, "playerAccounts.ser");
	}

	public static void saveBankAccountFile() {
		GiMUtility.saveObject(GoldIsMoney.bankAccounts, "bankAccounts.ser");
	}
}
