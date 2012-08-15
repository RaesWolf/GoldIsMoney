package com.flobi.GoldIsMoney;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

public class GiMUtility {
	public static File dataFolder;
	public static GoldIsMoney plugin;
    public static YamlConfiguration config;
    
	public static Map<String, PlayerAccount> loadMapStringPlayerAccount(
			String string) {
		return null;
	}

	public static Map<String, BankAccount> loadMapStringBankAccount(
			String string) {
		return null;
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
	}
}
