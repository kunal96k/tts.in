package com.tts.enquiries.util;

public class MobileUtils {
    /**
     * Normalizes a mobile number to a clean 10-digit format for consistent checks and storage.
     * Removes all non-digit characters, and strips leading +91, 91, or 0.
     */
    public static String normalizeMobile(String mobile) {
        if (mobile == null) {
            return "";
        }
        // Remove all non-digits
        String clean = mobile.replaceAll("\\D", "");
        
        // If it starts with 91 (and is 12 digits), remove 91
        if (clean.length() == 12 && clean.startsWith("91")) {
            clean = clean.substring(2);
        }
        // If it starts with 0 (and is 11 digits), remove 0
        if (clean.length() == 11 && clean.startsWith("0")) {
            clean = clean.substring(1);
        }
        
        return clean;
    }
}
