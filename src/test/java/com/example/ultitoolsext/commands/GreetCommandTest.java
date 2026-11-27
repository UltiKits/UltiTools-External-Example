package com.example.ultitoolsext.commands;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ultikits.ultitools.api.UltiToolsAPI;
import com.ultikits.ultitools.entities.WhereCondition;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GreetCommandTest {
    private static final String HELLO_SUFFIX =
            "! This message is from an external plugin using UltiTools-API.";

    @TempDir Path directory;
    private ExternalCommands harness;
    private PlayerMock player;

    @BeforeEach
    void setUp() throws Exception {
        harness = new ExternalCommands(directory);
        harness.deliverCommands();
        player = harness.server.addPlayer("Steve");
        player.setOp(true);
    }

    @AfterEach
    void tearDown() {
        if (harness != null) harness.close();
    }

    @Test
    void permittedPlayerReceivesGreetingAndInfo() throws Exception {
        assertThat(run("hello")).isEqualTo(1);
        run("info");
        player.assertSaid("Hello Steve" + HELLO_SUFFIX);
        player.assertSaid("UltiTools External Example v1.0.0 - verifying @Service injection works!");
        player.assertNoMoreSaid();
    }

    @Test
    void sameCommandRendersForEachPlayersLocale() throws Exception {
        var chinese = harness.server.addPlayer("Alex");
        chinese.setOp(true);
        chinese.setLocale(Locale.SIMPLIFIED_CHINESE);
        player.setLocale(Locale.US);

        run("hello");
        harness.execute(chinese, "ultiext hello");

        player.assertSaid("Hello Steve" + HELLO_SUFFIX);
        chinese.assertSaid("你好，Alex！这条消息来自使用 UltiTools-API 的外部插件。");
        player.assertNoMoreSaid();
        chinese.assertNoMoreSaid();
    }

    @Test
    void reloadCallsApiBeforeConfirmingToSender() throws Exception {
        try (var api = mockStatic(UltiToolsAPI.class)) {
            api.when(() -> UltiToolsAPI.reloadMessages(harness.plugin)).thenAnswer(call -> {
                player.assertNoMoreSaid();
                return null;
            });

            run("reload");

            api.verify(() -> UltiToolsAPI.reloadMessages(harness.plugin));
            player.assertSaid("External messages reloaded.");
            player.assertNoMoreSaid();
        }
    }

    @Test
    void reloadOfDisconnectedPluginDoesNotConfirmSuccess() throws Exception {
        try (var api = mockStatic(UltiToolsAPI.class)) {
            api.when(() -> UltiToolsAPI.reloadMessages(harness.plugin)).thenCallRealMethod();

            run("reload");

            api.verify(() -> UltiToolsAPI.reloadMessages(harness.plugin));
            player.assertSaid("Could not reload messages: this plugin is not connected to UltiTools.");
            player.assertNoMoreSaid();
        }
    }

    @Test
    void rootPermissionHidesCommandAndPreventsWrites() {
        player.setOp(false);
        var source = ExternalCommands.source(player);
        for (String label : List.of("ultiext", "uext")) {
            assertThat(harness.dispatcher.getRoot().getChild(label).canUse(source)).isFalse();
            assertThrows(CommandSyntaxException.class, () -> harness.execute(player, label + " visit Alex"));
            assertThrows(CommandSyntaxException.class, () -> harness.execute(player, label + " reload"));
        }
        verifyNoInteractions(harness.data);
        player.assertNoMoreSaid();
    }

    @Test
    void missingNameReportsUsageWithoutWriting() throws Exception {
        run("visit");
        assertThat(PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage()))
                .isEqualTo("Correct Usage: /ultiext visit <name>");
        player.assertNoMoreSaid();
        verifyNoInteractions(harness.data);
    }

    @Test
    void visitBindsNameAndInsertsFirstVisit() throws Exception {
        when(harness.data.getAll(any(WhereCondition.class))).thenReturn(List.of());
        long before = System.currentTimeMillis();
        run("visit Alex");
        assertLookup("Alex");
        var inserted = ArgumentCaptor.forClass(VisitorRecord.class);
        verify(harness.data).insert(inserted.capture());
        assertThat(inserted.getValue().getPlayerName()).isEqualTo("Alex");
        assertThat(inserted.getValue().getVisitCount()).isEqualTo(1);
        assertThat(inserted.getValue().getLastVisit()).isBetween(before, System.currentTimeMillis());
        player.assertSaid("[DATA] Created record for Alex — first visit!");
    }

    @Test
    void visitUpdatesMatchingRecordInsteadOfInsertingAgain() throws Exception {
        var existing = new VisitorRecord("Alex");
        existing.setLastVisit(1L);
        when(harness.data.getAll(any(WhereCondition.class))).thenReturn(List.of(existing));
        run("visit Alex");
        assertLookup("Alex");
        verify(harness.data).update(existing);
        verify(harness.data, never()).insert(any(VisitorRecord.class));
        assertThat(existing.getVisitCount()).isEqualTo(2);
        assertThat(existing.getLastVisit()).isGreaterThan(1L);
        player.assertSaid("[DATA] Updated Alex — visit #2");
    }

    @Test
    void failedUpdateSendsOnlyError() throws Exception {
        var existing = new VisitorRecord("Alex");
        when(harness.data.getAll(any(WhereCondition.class))).thenReturn(List.of(existing));
        doThrow(new IllegalAccessException("denied")).when(harness.data).update(existing);
        run("visit Alex");
        player.assertSaid("[ERROR] Update failed: denied");
        player.assertNoMoreSaid();
    }

    @Test
    void listsEmptyAndNonEmptyStore() throws Exception {
        var sam = new VisitorRecord("Sam");
        sam.incrementVisit();
        var frequentVisitor = new VisitorRecord("Taylor");
        frequentVisitor.setVisitCount(1000);
        when(harness.data.getAll()).thenReturn(List.of(), List.of(new VisitorRecord("Alex"), sam), List.of(frequentVisitor));
        run("visitors");
        player.assertSaid("[DATA] No visitor records.");
        player.assertNoMoreSaid();
        run("visitors");
        player.assertSaid("[DATA] === Visitor Records (2) ===");
        player.assertSaid("  Alex — 1 visits");
        player.assertSaid("  Sam — 2 visits");
        player.assertNoMoreSaid();
        run("visitors");
        player.assertSaid("[DATA] === Visitor Records (1) ===");
        player.assertSaid("  Taylor — 1000 visits");
        player.assertNoMoreSaid();
    }

    @Test
    void deletesByNameAndConfirms() throws Exception {
        run("delvisitor Alex");
        var condition = ArgumentCaptor.forClass(WhereCondition.class);
        verify(harness.data).del(condition.capture());
        assertThat(condition.getValue().getColumn()).isEqualTo("player_name");
        assertThat(condition.getValue().getValue()).isEqualTo("Alex");
        player.assertSaid("[DATA] Deleted visitor record for Alex");
    }

    @Test
    void dataCommandsWithoutAnOperatorSendOnlyErrors() throws Exception {
        when(harness.plugin.getVisitorData()).thenReturn(null);
        for (String input : List.of("visit Alex", "visitors", "delvisitor Alex")) {
            run(input);
            player.assertSaid("[ERROR] DataOperator not initialized!");
        }
        player.assertNoMoreSaid();
        verifyNoInteractions(harness.data);
    }

    @Test
    void helpListsEverySubcommand() throws Exception {
        run("help");
        for (String line : List.of(
                "=== UltiTools External Example ===",
                "/ultiext hello           - Greet (tests @Service injection)",
                "/ultiext info            - Plugin info",
                "/ultiext visit <name>    - Record a visit (tests DataOperator insert/update)",
                "/ultiext visitors        - List all visitors (tests DataOperator getAll)",
                "/ultiext delvisitor <name> - Delete a visitor (tests DataOperator del)",
                "/ultiext reload          - Reload message overrides",
                "==============================")) {
            player.assertSaid(line);
        }
        player.assertNoMoreSaid();
    }

    @Test
    void autoRegisteredJoinListenerGreetsInsertsUpdatesAndUnregisters() throws Exception {
        harness.listeners.registerAllExternal(harness.adapter);
        var inserted = ArgumentCaptor.forClass(VisitorRecord.class);
        when(harness.data.getAll(any(WhereCondition.class))).thenReturn(List.of());
        join();
        player.assertSaid("Hello Steve" + HELLO_SUFFIX);
        verify(harness.data).insert(inserted.capture());
        VisitorRecord record = inserted.getValue();
        assertThat(record.getPlayerName()).isEqualTo("Steve");
        assertThat(record.getVisitCount()).isEqualTo(1);

        when(harness.data.getAll(any(WhereCondition.class))).thenReturn(List.of(record));
        join();
        player.assertSaid("Hello Steve" + HELLO_SUFFIX);
        verify(harness.data).update(record);
        assertThat(record.getVisitCount()).isEqualTo(2);

        harness.listeners.unregisterAllExternal(harness.adapter.getPluginName());
        join();
        player.assertNoMoreSaid();
        assertThat(record.getVisitCount()).isEqualTo(2);
    }

    @Test
    void joinListenerReportsUpdateFailure() throws Exception {
        harness.listeners.registerAllExternal(harness.adapter);
        var existing = new VisitorRecord("Steve");
        when(harness.data.getAll(any(WhereCondition.class))).thenReturn(List.of(existing));
        doThrow(new IllegalAccessException("denied")).when(harness.data).update(existing);
        join();
        player.assertSaid("Hello Steve" + HELLO_SUFFIX);
        player.assertSaid("[ExtExample] Failed to update visit: denied");
        player.assertNoMoreSaid();
    }

    @Test
    void enableConnectsBeforeResolvingDataAndDisableDisconnects() {
        var plugin = mock(UltiToolsExtExample.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("external-example-test"));
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).onDisable();
        when(plugin.getVisitorData()).thenCallRealMethod();
        try (var api = mockStatic(UltiToolsAPI.class)) {
            api.when(() -> UltiToolsAPI.getDataOperator(plugin, VisitorRecord.class)).thenAnswer(call -> {
                api.verify(() -> UltiToolsAPI.connect(plugin));
                return harness.data;
            });
            plugin.onEnable();
            assertThat(plugin.getVisitorData()).isSameAs(harness.data);
            plugin.onDisable();
            api.verify(() -> UltiToolsAPI.disconnect(plugin));
            assertThat(plugin.getVisitorData()).isNull();
        }
    }

    @Test
    void enablePropagatesConnectFailureAndDisableIsSafe() {
        var plugin = mock(UltiToolsExtExample.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("external-example-test"));
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).onDisable();
        when(plugin.getVisitorData()).thenCallRealMethod();
        var failure = new IllegalStateException("Failed to connect");
        try (var api = mockStatic(UltiToolsAPI.class)) {
            api.when(() -> UltiToolsAPI.connect(plugin)).thenThrow(failure);
            api.when(() -> UltiToolsAPI.disconnect(plugin)).thenCallRealMethod();

            assertThat(assertThrows(IllegalStateException.class, plugin::onEnable)).isSameAs(failure);
            api.verify(() -> UltiToolsAPI.connect(plugin));
            api.verifyNoMoreInteractions();
            assertThat(plugin.getVisitorData()).isNull();

            assertDoesNotThrow(plugin::onDisable);
            api.verify(() -> UltiToolsAPI.disconnect(plugin));
            assertThat(plugin.getVisitorData()).isNull();
        }
    }

    @Test
    void disableClearsCachedDataEvenIfDisconnectThrows() {
        var plugin = mock(UltiToolsExtExample.class);
        var logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).onDisable();
        when(plugin.getVisitorData()).thenCallRealMethod();
        var failure = new IllegalStateException("Failed to disconnect");
        try (var api = mockStatic(UltiToolsAPI.class)) {
            api.when(() -> UltiToolsAPI.getDataOperator(plugin, VisitorRecord.class)).thenReturn(harness.data);
            api.when(() -> UltiToolsAPI.disconnect(plugin)).thenThrow(failure);
            plugin.onEnable();
            assertThat(plugin.getVisitorData()).isSameAs(harness.data);

            assertThat(assertThrows(IllegalStateException.class, plugin::onDisable)).isSameAs(failure);
            assertThat(plugin.getVisitorData()).isNull();
            verify(logger, never()).info("UltiTools External Example disabled!");
        }
    }

    private int run(String input) throws CommandSyntaxException {
        return harness.execute(player, "ultiext " + input);
    }

    private void join() {
        harness.server.getPluginManager().callEvent(new PlayerJoinEvent(player, (String) null));
    }

    private void assertLookup(String expectedName) {
        var condition = ArgumentCaptor.forClass(WhereCondition.class);
        verify(harness.data).getAll(condition.capture());
        assertThat(condition.getValue().getColumn()).isEqualTo("player_name");
        assertThat(condition.getValue().getValue()).isEqualTo(expectedName);
    }
}
