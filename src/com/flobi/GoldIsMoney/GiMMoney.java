package com.flobi.GoldIsMoney;

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
    	return format(doubleToLong(amount));
    }
    public static String format(long amount) {
    	return "MONEY";
    }
}
