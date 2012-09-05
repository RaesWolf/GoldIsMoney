package com.flobi.GoldIsMoney2;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import org.bukkit.ChatColor;

public class GiMCommand implements CommandExecutor {
	private GoldIsMoney plugin;
	
    public GiMCommand() {
    	plugin = GiMUtility.plugin;
    	plugin.getCommand("goldismoney").setExecutor(this);
    	plugin.getCommand("balance").setExecutor(this);
    	plugin.getCommand("money").setExecutor(this);
	}

	@Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (command.getName().equalsIgnoreCase("goldismoney")) {
        	if (sender instanceof ConsoleCommandSender || sender.hasPermission("goldismoney.admin")) {
            	if (args.length < 1) return false;
    			if (args[0].equalsIgnoreCase("reload")) {
	        		GiMUtility.loadConfig();
	            	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', GiMUtility.config.getString("text-reloaded")));
    			} else if (args[0].equalsIgnoreCase("test")) {
		        	String testPlayerAccount = "GiMTestUser";
		        	String testBankAccount = "GiMTestUser";
		        	EconomyResponse receipt;

		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6--- GoldIsMoney Test Routine ---"));
    				Economy econ = null;
    		        RegisteredServiceProvider<Economy> rsp = GiMUtility.plugin.getServer().getServicesManager().getRegistration(Economy.class);
    		        if (rsp == null) {
    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Economy registration not found."));
    		        	return true;
    		        }
    		        econ = rsp.getProvider();
    		        if (econ == null) {
    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Economy provider not found."));
    		        	return true;
    		        }
    		        if (econ.getName().equals("GoldIsMoney")) {
    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEconomy provider is: " + econ.getName()));
    		        } else {
    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Economy provider is: " + econ.getName()));
    		        	return true;
    		        }
    				if (GiMUtility.config.getBoolean("allow-fake-players")) {
    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney fake accounts are enabled."));
    		        	if (econ.hasAccount(testPlayerAccount)) {
    		        		econ.withdrawPlayer(testPlayerAccount, econ.getBalance(testPlayerAccount));
    		        	} else {
    		        		econ.createPlayerAccount(testPlayerAccount);
    		        		if (!econ.hasAccount(testPlayerAccount)) {
    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney fake account creation malfunction."));
    		        		}
    		        	}
    		        	
    		        	double balance = econ.getBalance(testPlayerAccount);
    		        	receipt = econ.depositPlayer(testPlayerAccount, 12345);
    		        	if (receipt.transactionSuccess()) {
    		        		if (econ.getBalance(testPlayerAccount) == balance + 12345) {
    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney deposit success."));
    		        		} else {
    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney deposit fail: Incorrect new balance."));
    		        		}
    		        	} else {
		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4GoldIsMoney deposit failure:"));
		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &d" + receipt.errorMessage));
    		        	}
    		        	
    		        	balance = econ.getBalance(testPlayerAccount);
    		        	receipt = econ.withdrawPlayer(testPlayerAccount, 1234);
    		        	if (receipt.transactionSuccess()) {
    		        		if (econ.getBalance(testPlayerAccount) == balance - 1234) {
    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney withdraw success."));
    		        		} else {
    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney withdraw fail: Incorrect new balance."));
    		        		}
    		        	} else {
		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4GoldIsMoney withdraw failure:"));
		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &d" + receipt.errorMessage));
    		        	}
    		        	
    				} else {
    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney fake accounts are disabled."));
    				}

    				if (GiMUtility.config.getBoolean("allow-banks")) {
    					if (!econ.hasBankSupport()) {
        		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Vault incorrectly says banks are unsupported."));
    					} else {
	    		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney bank accounts are enabled."));

	    		        	if (GoldIsMoney.bankExists(testBankAccount)) {
	    		        		receipt = econ.bankBalance(testBankAccount);
	    		        		econ.bankDeposit(testBankAccount, receipt.balance);
	    		        	} else {
	    		        		econ.createBank(testBankAccount, testPlayerAccount);
	    		        		if (!GoldIsMoney.bankExists(testBankAccount)) {
	    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney bank account creation malfunction."));
	    		        		}
	    		        	}
	    		        	
	    		        	receipt = econ.bankBalance(testBankAccount);
	    		        	double balance = receipt.balance;
	    		        	receipt = econ.bankDeposit(testBankAccount, 12345);
	    		        	if (receipt.transactionSuccess()) {
		    		        	receipt = econ.bankBalance(testBankAccount);
	    		        		if (receipt.balance == balance + 12345) {
	    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney bank deposit success."));
	    		        		} else {
	    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney bank deposit fail: Incorrect new balance."));
	    		        		}
	    		        	} else {
			        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4GoldIsMoney bank deposit failure:"));
			        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &d" + receipt.errorMessage));
	    		        	}
	    		        	
	    		        	receipt = econ.bankBalance(testBankAccount);
	    		        	balance = receipt.balance;
	    		        	receipt = econ.bankWithdraw(testBankAccount, 1234);
	    		        	if (receipt.transactionSuccess()) {
	    		        		receipt = econ.bankBalance(testBankAccount);
	    		        		if (receipt.balance == balance - 1234) {
	    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney bank withdraw success."));
	    		        		} else {
	    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGoldIsMoney bank withdraw fail: Incorrect new balance."));
	    		        		}
	    		        	} else {
			        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4GoldIsMoney bank withdraw failure:"));
			        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &d" + receipt.errorMessage));
	    		        	}
	    		        	
	    		        	econ.deleteBank(testBankAccount);
	    		        	if (GoldIsMoney.bankExists(testBankAccount)) {
    		        			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney bank account removal malfunction."));
	    		        	}
    					}

    				} else {
    					if (econ.hasBankSupport()) {
        		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Vault incorrectly says banks are supported."));
    					} else {
        		        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6GoldIsMoney bank accounts are disabled."));
    					}
    				}
    			} else {
    				return false;
    			}
            	return true;
        	}
        	sender.sendMessage(ChatColor.translateAlternateColorCodes('&', GiMUtility.config.getString("text-admin-perms-fail")));
        	return true;
        } else if (command.getName().equalsIgnoreCase("money") || command.getName().equalsIgnoreCase("balance")) {
        	if (sender instanceof Player) {
        		Player player = (Player) sender;
            	player.sendMessage(ChatColor.translateAlternateColorCodes('&', GiMUtility.config.getString("text-balance").replaceAll("%g", GoldIsMoney.format(GoldIsMoney.getBalance(player.getName())))));
            	return true;
        	}
        }
        return false;
    }
}
