package com.cipherlink.helper;

public class PhoneNumberUtil {

    /**
     * Normalizes a user-typed phone number into the canonical E.164-ish form
     * stored in the database (e.g. via Firebase auth: "+916380532255").
     * Strips spaces, dashes, parentheses and dots, but keeps a leading '+'.
     *
     * Without this, "+91 63805 32255" (as typed/formatted in a UI input)
     * will never exact-match "+916380532255" (as stored after Firebase OTP
     * verification), causing false "User not found on CipherLink" errors
     * even when the account exists.
     */
    public static String normalize(String rawPhoneNumber) {
        if (rawPhoneNumber == null) return null;
        String trimmed = rawPhoneNumber.trim();
        boolean hasPlus = trimmed.startsWith("+");
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        return hasPlus ? "+" + digitsOnly : digitsOnly;
    }
}
