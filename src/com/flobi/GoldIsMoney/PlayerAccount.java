package com.flobi.GoldIsMoney;

public class PlayerAccount {
	private String playerName;
	private long balance = 0;
	private long inventoryBalance = 0;
	
	public PlayerAccount(String playerName) {
		this.playerName = playerName;
	}

	public long getBalance() {
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
			return true;
		}
	}

	public boolean withdraw(long amount) {
		if (!has(amount)) return false;
		balance -= amount;
		syncInventory();
		return true;
	}

	public void syncInventory() {
	}

	public void initialize() {
	}
}
