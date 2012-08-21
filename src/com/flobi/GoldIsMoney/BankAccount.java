package com.flobi.GoldIsMoney;

import java.io.Serializable;
import java.util.ArrayList;

public class BankAccount implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8193482747890132674L;
	private String ownerName; 
	private long balance;
	private static ArrayList<String> members = new ArrayList<String>();
	
	public BankAccount(String ownerName) {
		this.ownerName = ownerName;
		GiMUtility.saveBankAccountFile();
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
		GiMUtility.saveBankAccountFile();
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
			GiMUtility.saveBankAccountFile();
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
		GiMUtility.saveBankAccountFile();
		return isMember(playerName);
	}
	
	public boolean removeMember(String playerName) {
		if (!isMember(playerName)) return true;
		members.remove(playerName);
		GiMUtility.saveBankAccountFile();
		return !isMember(playerName);
	}

}
