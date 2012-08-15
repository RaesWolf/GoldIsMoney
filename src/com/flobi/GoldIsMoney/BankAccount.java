package com.flobi.GoldIsMoney;

import java.util.ArrayList;

public class BankAccount {
	private String ownerName; 
	private long balance;
	private static ArrayList<String> members = new ArrayList<String>();
	
	public BankAccount(String ownerName) {
		this.ownerName = ownerName;
	}

	public long getBalance() {
		return balance;
	}

	public void dispose() {
		GoldIsMoney.depositPlayer(ownerName, GiMMoney.longToDouble(balance));
		ownerName = null;
		balance = 0;
		members.clear();
		members = null;
	}

	public boolean has(long amount) {
		return amount <= balance;
	}

	public boolean withdraw(long amount) {
		if (!has(amount)) return false;
		balance -= amount;
		return true;
	}

	public boolean deposit(long amount) {
		// Don't let it overflow.
		long oldBalance = balance; 
		balance += amount;
		if (balance < 0) {
			balance = oldBalance;
			return false;
		} else {
			return true;
		}
	}
	
	public String getOwner() {
		return ownerName;
	}

	public boolean isMember(String playerName) {
		return members.contains(playerName);
	}
	
	public boolean addMember(String playerName) {
		members.add(playerName);
		return isMember(playerName);
	}
	
	public boolean removeMember(String playerName) {
		if (!isMember(playerName)) return true;
		members.remove(playerName);
		return !isMember(playerName);
	}
}
