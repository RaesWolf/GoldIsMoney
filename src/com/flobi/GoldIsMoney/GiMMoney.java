package com.flobi.GoldIsMoney;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class GiMMoney {
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
    		numberFormat = GiMUtility.config.getString("format-singular").replace("%n", numberFormat);
    	} else {
    		numberFormat = GiMUtility.config.getString("format-plural").replace("%n", numberFormat);
    	}
    	return GiMUtility.config.getString("text-balance").replace("%g", numberFormat);
    }
    public static String format(long amount) {
    	return format(longToDouble(amount));
    }
}
