package com.example.ultitoolsext.commands;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCatalogueTest {
    private static final Pattern KEY_CALL = Pattern.compile("\\bmessages\\s*\\.\\s*key\\s*\\(");
    private static final Pattern KEY_ARGUMENT = Pattern.compile("\\s*\"([a-z0-9_.-]+)\"\\s*\\)");
    private static final Pattern PLACEHOLDER = Pattern.compile("(?<!\\\\)<([a-z][a-z0-9_]*)(?=[:>])");

    @Test
    void cataloguesDefineTheSameKeysAndPlaceholders() throws Exception {
        var english = catalogue("en");
        var chinese = catalogue("zh");
        assertThat(english).isNotEmpty();
        assertThat(chinese).containsOnlyKeys(english.keySet());
        for (var entry : english.entrySet()) {
            assertThat(placeholders(chinese.get(entry.getKey())))
                    .as("占位符：%s", entry.getKey())
                    .isEqualTo(placeholders(entry.getValue()));
        }
    }

    @Test
    void bothCataloguesCoverEveryRelativeKeyInMainSources() throws Exception {
        Set<String> used = new TreeSet<>();
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                var call = KEY_CALL.matcher(source);
                while (call.find()) {
                    var argument = KEY_ARGUMENT.matcher(source).region(call.end(), source.length());
                    assertThat(argument.lookingAt()).as("%s 中的消息键须是字符串字面量", file).isTrue();
                    used.add(argument.group(1));
                }
            }
        }
        assertThat(used).isNotEmpty();
        assertThat(catalogue("en")).containsOnlyKeys(used);
        assertThat(catalogue("zh")).containsOnlyKeys(used);
    }

    private static Map<String, String> catalogue(String language) throws Exception {
        Path resource = ExternalCommands.pluginClasses().resolve("messages/" + language + ".yml");
        assertThat(resource).as("随插件打包的消息文件").isRegularFile();
        var yaml = new YamlConfiguration();
        try (var reader = Files.newBufferedReader(resource, StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }
        return yaml.getValues(true).entrySet().stream()
                .filter(entry -> !yaml.isConfigurationSection(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    assertThat(entry.getValue()).as("消息 %s", entry.getKey()).isInstanceOf(String.class);
                    return (String) entry.getValue();
                }));
    }

    private static Set<String> placeholders(String template) {
        return PLACEHOLDER.matcher(template).results()
                .map(match -> match.group(1)).collect(Collectors.toSet());
    }
}
