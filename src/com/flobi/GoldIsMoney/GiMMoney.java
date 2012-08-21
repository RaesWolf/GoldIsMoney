package com.flobi.GoldIsMoney;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.bukkit.inventory.ItemStack;

public class GiMMoney {

	// Currency Definitions:
	public static Map<Long, String> currencyFamily = new TreeMap<Long, String>();
	public static Map<Long, String> currencyFamilyReverse = new TreeMap<Long, String>(Collections.reverseOrder());

	public static long doubleToLong(double amount) {
    	long newLong = (long) Math.round(amount * Math.pow(10D, (double) GoldIsMoney.fractionalDigits()));
    	// We only deal with positive numbers here, so make sure it's positive.
    	if (newLong < 0) newLong = 9223372036854775807L;
    	return newLong;
    }
    public static double longToDouble(long amount) {
    	return ((double) amount) / Math.pow(10D, (double) GoldIsMoney.fractionalDigits());
    }
    public static String format(double amount) {
    	int fractionalDigits = GoldIsMoney.fractionalDigits();
    	String numberFormat = "#,###,##0";
    	if (fractionalDigits > 0) numberFormat += ".";
    	for (int t = 0; t < fractionalDigits; t++) numberFormat += "0";
    	NumberFormat formatter = new DecimalFormat(numberFormat);
    	numberFormat = formatter.format(amount);
    	if (Math.round(amount * Math.pow(10, fractionalDigits)) / Math.pow(10, fractionalDigits) == 1) {
    		return GiMUtility.config.getString("format-singular").replace("%n", numberFormat);
    	} else {
    		return GiMUtility.config.getString("format-plural").replace("%n", numberFormat);
    	}
    }
    public static String format(long amount) {
    	return format(longToDouble(amount));
    }

	public static long getStackValue(ItemStack item) {
		if (item == null) return 0;
		for (Entry<Long, String> pair: GiMMoney.currencyFamily.entrySet()) {
			if (GiMItems.isItem(pair.getValue(), item)) {
				return item.getAmount() * pair.getKey();
			}
		}
		return 0;
	}
}
