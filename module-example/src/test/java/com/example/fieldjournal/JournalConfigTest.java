package com.example.fieldjournal;

import com.google.gson.JsonObject;
import com.ultikits.ultitools.annotations.ConfigEntity;
import com.ultikits.ultitools.exceptions.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalConfigTest {
    private final JournalConfig config = new JournalConfig("config/journal.yml");

    @Test
    void declaresAFileWithTheScreenNameDefault() {
        assertEquals("config/journal.yml", JournalConfig.class.getAnnotation(ConfigEntity.class).value());
        assertEquals("Field Journal", config.journalName());
    }

    @Test
    void validationRejectsBlankAndOverlongNamesWithoutChangingTheRunningName() {
        for (String invalid : new String[]{"   ", "x".repeat(41)}) {
            ConfigurationException failure = assertThrows(ConfigurationException.class,
                    () -> config.validateProposedProperties(proposedName(invalid)));
            assertTrue(failure.getMessage().contains("journal-name"), failure.getMessage());
            assertEquals("Field Journal", config.journalName());
        }
        assertDoesNotThrow(() -> config.validateProposedProperties(proposedName("Ranger Log")));
    }

    private static JsonObject proposedName(String name) {
        JsonObject proposed = new JsonObject();
        proposed.addProperty("journal-name", name);
        return proposed;
    }
}
