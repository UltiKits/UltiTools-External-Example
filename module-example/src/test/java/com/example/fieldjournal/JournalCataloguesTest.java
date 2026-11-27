package com.example.fieldjournal;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalCataloguesTest {
    @Test
    void bothCataloguesShipMatchingKeysAndParameterizedTranslations() throws Exception {
        YamlConfiguration english = load("en");
        YamlConfiguration chinese = load("zh");
        assertEquals(messageKeys(english), messageKeys(chinese));
        assertTrue(messageKeys(english).contains("journal.recorded"));
        assertEquals("Recorded forest (entries: 2).", renderRecorded(english));
        assertEquals("已记录森林（累计 2 条）。", renderRecorded(chinese));
    }

    @Test
    void playerNoteKeepsMiniMessageLikeInputAsLiteralText() throws Exception {
        String pattern = load("en").getString("journal.note-saved");
        var component = MiniMessage.miniMessage().deserialize(pattern, Placeholder.unparsed("note", "<red>Blue moss"));
        assertEquals("Saved field note: <red>Blue moss", PlainTextComponentSerializer.plainText().serialize(component));
    }

    private static YamlConfiguration load(String locale) throws Exception {
        var stream = JournalCataloguesTest.class.getResourceAsStream("/messages/" + locale + ".yml");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        }
    }

    private static Set<String> messageKeys(YamlConfiguration catalogue) {
        return catalogue.getKeys(true).stream().filter(catalogue::isString).collect(Collectors.toSet());
    }

    private static String renderRecorded(YamlConfiguration catalogue) {
        var rendered = MiniMessage.miniMessage().deserialize(catalogue.getString("journal.recorded"),
                Placeholder.unparsed("site", catalogue.getString("site.forest")), Placeholder.unparsed("count", "2"));
        return PlainTextComponentSerializer.plainText().serialize(rendered);
    }
}
