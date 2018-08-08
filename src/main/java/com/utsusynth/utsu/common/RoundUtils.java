package com.utsusynth.utsu.common;

import java.text.DecimalFormat;

/** Collection of functions to handle rounding numbers and printing rounded numbers. */
public class RoundUtils {
    private RoundUtils() {}

    /**
     * Rounds a double value to the nearest integer.
     */
    public static int round(double num) {
        return (int) Math.round(num);
    }

    /**
     * Returns a formatted string of a rounded double value.
     * 
     * @param number, the number to round.
     * @param roundFormat, format of rounded number, i.e. "#.##".
     * @return String of number rounded to the correct number of decimal places.
     */
    public static String roundDecimal(double number, String roundFormat) {
        int formatNumPlaces = roundFormat.length() - roundFormat.indexOf(".") - 1;
        String formatted = new DecimalFormat(roundFormat).format(number);
        if (formatted.contains(".")) {
            int numPlaces = formatted.length() - formatted.indexOf(".") - 1;
            for (int i = numPlaces; i < formatNumPlaces; i++) {
                formatted = formatted + "0";
            }
        } else {
            formatted = formatted + ".";
            for (int i = 0; i < formatNumPlaces; i++) {
                formatted = formatted + "0";
            }
        }
        return formatted;
    }


}
