package com.flobi.GoldIsMoney2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;

public class GoldIsMoney extends JavaPlugin {
	
	// Accounts
	public static Map<String, PlayerAccount> playerAccounts = new HashMap<String, PlayerAccount>();
	public static Map<String, BankAccount> bankAccounts = new HashMap<String, BankAccount>();
	
	// Setup
	public void onEnable(){
		GiMUtility.setPlugin(this);
		GiMUtility.setDataFolder(getDataFolder());
		GiMUtility.loadConfig();
		
		getServer().getPluginManager().registerEvents(new GiMListener(), this);

		playerAccounts = GiMUtility.loadMapStringPlayerAccount("playerAccounts.ser");
		bankAccounts = GiMUtility.loadMapStringBankAccount("bankAccounts.ser");

        // Load up the Plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }

        new GiMCommand();

        getLogger().info("GoldIsMoney has been enabled!");
	}

    public void onDisable(){ 
    	GiMUtility.setPlugin(null);
		GiMUtility.setDataFolder(null);
		playerAccounts.clear();
		playerAccounts = null;
		bankAccounts.clear();
		bankAccounts = null;
    	getLogger().info("GoldIsMoney has been disabled.");
    }
    

    
    // Vault compatible methods
    public static double getBalance(String playerName) {
    	if (!hasAccount(playerName)) return 0D;
		return GiMMoney.longToDouble(getPlayerAccount(playerName).getBalance());
    }
    public static boolean has(String playerName, double amount) {
    	if (!hasAccount(playerName)) return false;
    	return getPlayerAccount(playerName).has(GiMMoney.doubleToLong(amount));
    }
    public static int fractionalDigits() {
    	return GiMUtility.config.getInt("fractional-digits");
    }
    public static boolean withdrawPlayer(String playerName, double amount) {
    	if (!has(playerName, amount)) return false;
    	return getPlayerAccount(playerName).withdraw(GiMMoney.doubleToLong(amount));
    }
    public static boolean depositPlayer(String playerName, double amount) {
    	if (!hasAccount(playerName)) return false;
    	return getPlayerAccount(playerName).deposit(GiMMoney.doubleToLong(amount));
    }
    public static String format(double amount) {
    	return GiMMoney.format(amount);
    }
    public static String currencyNameSingular() {
    	return GiMUtility.config.getString("name-singular");
    }
    public static String currencyNamePlural() {
    	return GiMUtility.config.getString("name-plural");
    }
    public static boolean hasAccount(String playerName) {
    	// NOTE: Do not call getPlayerAccount in here, it would cause an infinite loop.
    	if (GiMUtility.config.getBoolean("autocreate-accounts")) {
    		return createPlayerAccount(playerName);
    	}
    	return playerAccounts.containsKey(playerName);
    }
    public static boolean createPlayerAccount(String playerName) {
    	// Don't use hasAccount here, will cause infinite loop.
    	if (playerAccounts.containsKey(playerName)) return true;
    	if (!GiMUtility.config.getBoolean("allow-fake-players") && GiMUtility.plugin.getServer().getPlayer(playerName) == null) return false;
    	
    	playerAccounts.put(playerName, new PlayerAccount(playerName));
    	return hasAccount(playerName);
    }
    public static boolean hasBankSupport() {
    	return GiMUtility.config.getBoolean("allow-banks");
    }
    public static boolean createBank(String bankName, String playerName) {
    	// Don't use bankExists here, will cause infinite loop.
    	if (bankAccounts.containsKey(bankName)) return true;
    	bankAccounts.put(bankName, new BankAccount(bankName));
    	return bankAccounts.containsKey(bankName);
    }
    public static double bankBalance(String bankName) {
    	if (!bankExists(bankName)) return 0D;
		return GiMMoney.longToDouble(getBankAccount(bankName).getBalance());
    }
    public static boolean deleteBank(String bankName) {
    	if (!bankExists(bankName)) return true;
    	
    	getBankAccount(bankName).dispose();
    	bankAccounts.remove(bankName);
    	GiMUtility.saveBankAccountFile();
    	return !bankExists(bankName);
    }
    public static boolean bankHas(String bankName, double amount) {
    	if (!bankExists(bankName)) return false;
    	return getBankAccount(bankName).has(GiMMoney.doubleToLong(amount));
    }
    public static boolean bankWithdraw(String bankName, double amount) {
    	if (!bankHas(bankName, amount)) return false;
    	return getBankAccount(bankName).withdraw(GiMMoney.doubleToLong(amount));
    }
    public static boolean bankDeposit(String bankName, double amount) {
    	if (!bankExists(bankName)) return false;
    	return getBankAccount(bankName).deposit(GiMMoney.doubleToLong(amount));
    }
    public static boolean isBankOwner(String bankName, String playerName) {
    	if (!bankExists(bankName)) return false;
    	return getBankAccount(bankName).getOwner().equalsIgnoreCase(playerName);
    }
    public static boolean isBankMember(String bankName, String playerName) {
    	if (!bankExists(bankName)) return false;
    	return getBankAccount(bankName).isMember(playerName);
    }
    public static List<String> getBanks() {
    	List<String> bankList = new ArrayList<String>(); 
    	if (!hasBankSupport()) return bankList;
    	for (String bankName : bankAccounts.keySet()) {
    		bankList.add(bankName);
    	}
    	return bankList;
    }
    
    // Potential new Vault supported methods:
    public static boolean bankExists(String bankName) {
    	// NOTE: Do not call getBankAccount in here, it would cause an infinite loop.
    	if (!hasBankSupport()) return false;
    	if (GiMUtility.config.getBoolean("autocreate-accounts")) {
    		return createBank(bankName, GiMUtility.config.getString("autocreated-bank-owner"));
    	}
    	return bankAccounts.containsKey(bankName);
    }
    public static boolean addBankMember(String bankName, String playerName) {
    	if (!bankExists(bankName)) return false;
    	return getBankAccount(bankName).addMember(playerName);
    }
    public static boolean removeBankMember(String bankName, String playerName) {
    	if (!bankExists(bankName)) return false;
    	return getBankAccount(bankName).removeMember(playerName);
    }

    // Misc relevant util
    public static BankAccount getBankAccount(String bankName) {
    	if (!bankExists(bankName)) return null;
    	return bankAccounts.get(bankName);
    }
    public static PlayerAccount getPlayerAccount(String playerName) {
    	if (!hasAccount(playerName)) return null;
    	return playerAccounts.get(playerName);
    }
    
}