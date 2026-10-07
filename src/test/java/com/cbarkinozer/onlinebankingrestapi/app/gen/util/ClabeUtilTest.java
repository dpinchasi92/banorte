package com.cbarkinozer.onlinebankingrestapi.app.gen.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClabeUtilTest {

    @Test
    void shouldCalculateCheckDigit() {
        assertEquals(9, ClabeUtil.calculateCheckDigit("03218000011835971"));
        assertEquals(1, ClabeUtil.calculateCheckDigit("07218000011835971"));
    }

    @Test
    void shouldValidateKnownClabe() {
        assertTrue(ClabeUtil.isValidClabe("032180000118359719"));
        assertTrue(ClabeUtil.isValidClabe("072180000118359711"));
    }

    @Test
    void shouldRejectWrongCheckDigit() {
        assertFalse(ClabeUtil.isValidClabe("032180000118359710"));
    }

    @Test
    void shouldRejectWrongLengthOrNonNumeric() {
        assertFalse(ClabeUtil.isValidClabe(null));
        assertFalse(ClabeUtil.isValidClabe(""));
        assertFalse(ClabeUtil.isValidClabe("03218000011835971"));
        assertFalse(ClabeUtil.isValidClabe("0321800001183597190"));
        assertFalse(ClabeUtil.isValidClabe("0321800001183597A9"));
        assertFalse(ClabeUtil.isValidClabe("1234567890123456"));
    }

    @Test
    void shouldThrowWhenCheckDigitInputIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ClabeUtil.calculateCheckDigit("123"));
        assertThrows(IllegalArgumentException.class, () -> ClabeUtil.calculateCheckDigit(null));
    }

    @RepeatedTest(20)
    void shouldGenerateValidBanorteClabe() {
        String clabe = ClabeUtil.generateClabe();

        assertEquals(ClabeUtil.CLABE_LENGTH, clabe.length());
        assertTrue(clabe.startsWith(ClabeUtil.BANORTE_BANK_CODE));
        assertTrue(ClabeUtil.isValidClabe(clabe));
    }
}
