package com.flobi.GoldIsMoney2;

import org.bukkit.inventory.ItemStack;

public class GiMItems {
	public static boolean isItem(String itemKey, ItemStack item) {
		if (item == null) return false;
		return itemKey.equalsIgnoreCase(Integer.toString(item.getTypeId())) || itemKey.equalsIgnoreCase(item.getTypeId() + ";" + item.getDurability());
	}

	public static ItemStack getItemStack(String itemKey, int quantity) {
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

	public static int maxStackSize(String itemKey) {
		int typeId;
		String[] keyParts = itemKey.split(";");
		typeId = Integer.parseInt(keyParts[0]);
		ItemStack item = new ItemStack(typeId);
		return item.getMaxStackSize();
	}

}
