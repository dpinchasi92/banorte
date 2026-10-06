package com.cbarkinozer.onlinebankingrestapi.app.gen.util;

import java.security.SecureRandom;

public class ClabeUtil {

    public static final int CLABE_LENGTH = 18;
    public static final String BANORTE_BANK_CODE = "072";

    private static final int PLAZA_CODE_LENGTH = 3;
    private static final int ACCOUNT_NUMBER_LENGTH = 11;
    private static final int[] WEIGHTS = {3, 7, 1};
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClabeUtil() {
    }

    public static String generateClabe() {
        String plazaCode = randomDigits(PLAZA_CODE_LENGTH);
        String accountNumber = randomDigits(ACCOUNT_NUMBER_LENGTH);
        String clabeWithoutCheckDigit = BANORTE_BANK_CODE + plazaCode + accountNumber;
        return clabeWithoutCheckDigit + calculateCheckDigit(clabeWithoutCheckDigit);
    }

    public static int calculateCheckDigit(String clabeWithoutCheckDigit) {
        if (clabeWithoutCheckDigit == null
                || clabeWithoutCheckDigit.length() != CLABE_LENGTH - 1
                || !isNumeric(clabeWithoutCheckDigit)) {
            throw new IllegalArgumentException("CLABE check digit requires exactly 17 numeric digits");
        }

        int sum = 0;
        for (int i = 0; i < clabeWithoutCheckDigit.length(); i++) {
            int digit = clabeWithoutCheckDigit.charAt(i) - '0';
            sum += (digit * WEIGHTS[i % WEIGHTS.length]) % 10;
        }
        return (10 - (sum % 10)) % 10;
    }

    public static boolean isValidClabe(String clabe) {
        if (clabe == null || clabe.length() != CLABE_LENGTH || !isNumeric(clabe)) {
            return false;
        }
        int expectedCheckDigit = calculateCheckDigit(clabe.substring(0, CLABE_LENGTH - 1));
        return clabe.charAt(CLABE_LENGTH - 1) - '0' == expectedCheckDigit;
    }

    private static String randomDigits(int count) {
        StringBuilder digits = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        return digits.toString();
    }

    private static boolean isNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
