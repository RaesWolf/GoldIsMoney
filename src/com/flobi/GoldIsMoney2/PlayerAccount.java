package com.flobi.GoldIsMoney2;

import java.io.Serializable;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerAccount implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7200048650089450836L;
	private String playerName;
	private long balance = 0;
	private long inventoryBalance = 0;
	
	public PlayerAccount(String playerName) {
		this.playerName = playerName;
//		GiMUtility.plugin.getLogger().info("Created player account: " + playerName);
		GiMUtility.savePlayerAccountFile();
	}

	public long getBalance() {
		syncInventory();
		return balance;
	}
	
	public boolean has(long amount) {
		return amount <= balance;
	}
	
	public boolean deposit(long amount) {
		// Don't let it overflow.
		long oldBalance = balance; 
		balance += amount;
		if (balance < 0) {
			balance = oldBalance;
			return false;
		} else {
			syncInventory();
//			GiMUtility.plugin.getServer().getLogger().info("Deposit: New balance is " + GiMMoney.format(balance));
			GiMUtility.savePlayerAccountFile();
			return true;
		}
	}

	public boolean withdraw(long amount) {
		if (!has(amount)) return false;
		balance -= amount;
		syncInventory();
//		GiMUtility.plugin.getServer().getLogger().info("Withdraw: New balance is " + GiMMoney.format(balance));
		GiMUtility.savePlayerAccountFile();
		return true;
	}

	public void syncInventory() {
		
		// Get the player for this
		Player player = GiMUtility.plugin.getServer().getPlayer(playerName);
		
		// If the player's not online, don't sync.
		if (player == null) return;
		if (!player.isOnline()) return;
		
		// If the inventory changed since last sync, adjust main balance.
		long startInventoryBalance = getInventoryBalance(player.getInventory());
		balance += startInventoryBalance - inventoryBalance;

		setInventoryBalance(player, balance, startInventoryBalance);
		
		// Finally store the current balance for the next round.
		inventoryBalance = getInventoryBalance(player.getInventory());
		
		// If anything changed, save the player file.
		if (startInventoryBalance != inventoryBalance) GiMUtility.savePlayerAccountFile();
	}

    private static void setInventoryBalance(Player player, long newBalance, long startInventoryBalance) {
		PlayerInventory inventory = player.getInventory();
		
		if (newBalance == startInventoryBalance) return;
		
	//	player.sendMessage("Balance change from " + oldBalance + " to " + newBalance);
		
		long difference = newBalance - startInventoryBalance;
		ItemStack[] items;
		int stackCount;
		int stackMax;
		long itemWorth;
		String itemKey;
		
		items = inventory.getContents();
		if (difference < 0) {
			difference = Math.abs(difference);
			
	//		player.sendMessage("Withdrawing " + difference);
			
			for (Entry<Long, String> pair: GiMMoney.currencyFamily.entrySet()) {
				if (difference <= 0) break;
	
				itemWorth = pair.getKey();
				itemKey = pair.getValue();
				
				for (ItemStack item : items) {
					if (difference <= 0) break;
	
					if (GiMItems.isItem(itemKey, item)) {
						stackCount = item.getAmount();
	    				if (stackCount * itemWorth > difference) {
	    					// This stack is more than enough to cover our debt:
	    					int removedItems = (int) Math.ceil((double) difference / (double) itemWorth);
	    					if (stackCount - removedItems <= 0) {
		    					item.setAmount(-1);
	//	    					player.sendMessage("Removed " + itemKey + " stack (overflow) of " + stackCount + " worth " + (stackCount * itemWorth));
	    					} else {
		    					item.setAmount(stackCount - removedItems);
	//	    					player.sendMessage("Removed " + removedItems + " worth " + (removedItems * itemWorth) + " from " + itemKey + " stack of " + stackCount);
	//	    					player.sendMessage("Stack now contains: " + item.getAmount());
	    					}
	    					difference -= removedItems * itemWorth;
	    					break;
	    				}
						// Owe this or more than this, take the stack.
	    				item.setAmount(-1);
	//    				player.sendMessage("Removed " + itemKey + " stack of " + stackCount + " worth " + (stackCount * itemWorth));
						difference -= stackCount * itemWorth;
					}
				}
			}
			
			// Just in case we overdrew, actually a high probability, give back change.
			difference = 0 - difference;
	//		player.sendMessage("Change is " + difference);
		}
		
	//	player.sendMessage("Depositing " + difference);
		for (Entry<Long, String> pair: GiMMoney.currencyFamilyReverse.entrySet()) {
			itemWorth = pair.getKey();
			if (difference < itemWorth) continue;
	
			itemKey = pair.getValue();
			stackMax = GiMItems.maxStackSize(itemKey);
	
			for (ItemStack item : items) {
				if (difference < itemWorth) break;
	
				if (item != null && GiMItems.isItem(itemKey, item) && item.getAmount() < stackMax) {
					stackCount = item.getAmount();
					if (stackCount < 0) stackCount = 0;
					item.setAmount((int) Math.min(stackMax, stackCount + (int) Math.floor((double) difference / (double) itemWorth)));
					difference -= (item.getAmount() - stackCount) * itemWorth;
	//				player.sendMessage("Added " + (item.getAmount() - stackCount) + " worth " + ((item.getAmount() - stackCount) * itemWorth) + " to " + itemKey + " stack of " + stackCount);
				}
			}
		}
		
		// Remove any empty stacks.
		for (ItemStack item : items) {
			if (item != null && item.getAmount() <= 0) inventory.remove(item);
		}
		
		for (Entry<Long, String> pair: GiMMoney.currencyFamilyReverse.entrySet()) {
			itemWorth = pair.getKey();
			if (difference < itemWorth) continue;
	
			itemKey = pair.getValue();
			stackMax = GiMItems.maxStackSize(itemKey);
	
			// Refresh inventory contents.
			items = inventory.getContents();
			for (ItemStack item : items) {
				if (difference < itemWorth) break;
	
				if (item == null) {
					stackCount = (int) Math.min(stackMax, (int) Math.floor((double) difference / (double) itemWorth));
					inventory.addItem(GiMItems.getItemStack(itemKey, stackCount));
					difference -= stackCount * itemWorth;
	//				player.sendMessage("Added " + itemKey + " stack of " + stackCount + " worth " + (stackCount * itemWorth));
				}
			}
		}
		
	}

	
    private static long getInventoryBalance(PlayerInventory inventory) {
    	long balance = 0;
    	
		ItemStack[] items = inventory.getContents();


		for (ItemStack item : items) {
			balance += GiMMoney.getStackValue(item);
		}
    	
    	return balance;
    }

}
