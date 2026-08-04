package com.urlshortener.unit;

import com.urlshortener.util.HashGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HashGenerator Tests")
class HashGeneratorTest {
    private HashGenerator hashGenerator;
    private static final int EXPECTED_LENGTH = 6;

    @BeforeEach
    void setUp() {
        hashGenerator = new HashGenerator(EXPECTED_LENGTH);
    }

    @Test
    @DisplayName("Should generate hash with correct length")
    void shouldGenerateHashWithCorrectLength() {
        String hash = hashGenerator.generate();
        assertEquals(EXPECTED_LENGTH, hash.length());
    }

    @Test
    @DisplayName("Should generate different hashes")
    void shouldGenerateDifferentHashes() {
        String hash1 = hashGenerator.generate();
        String hash2 = hashGenerator.generate();
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should only contain alphanumeric characters")
    void shouldOnlyContainAlphanumericCharacters() {
        String hash = hashGenerator.generate();
        assertTrue(hash.matches("^[a-zA-Z0-9]{6}$"));
    }
}
