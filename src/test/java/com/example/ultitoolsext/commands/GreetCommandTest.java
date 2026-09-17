package com.example.ultitoolsext.commands;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.example.ultitoolsext.services.GreetService;
import com.ultikits.ultitools.abstracts.command.BaseCommandExecutor;
import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.entities.WhereCondition;
import com.ultikits.ultitools.interfaces.DataOperator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GreetCommand} against the UltiTools-API 6.3.0 command API.
 * <p>
 * Before 6.3.0 this command extended {@code AbstractCommandExecutor}, which 6.3.0 deleted, so
 * {@code UltiToolsAPI.connect(this)} could not register any {@code /ultiext} subcommand
 * (UltiKits/UltiTools-External-Example#4). These tests do not compile against that shape: they
 * use {@link BaseCommandExecutor#getMappings()} and dispatch through
 * {@link BaseCommandExecutor#onCommand}, the same entry point Bukkit calls once
 * {@code CommandManager#registerAllExternal} has registered the command.
 */
@DisplayName("GreetCommand on the UltiTools-API 6.3.0 command API")
class GreetCommandTest {

    private static final String HELLO_SUFFIX =
            "! This message is from an external plugin using UltiTools-API.";

    private ServerMock server;
    private GreetCommand command;
    private DataOperator<VisitorRecord> visitorData;
    private Object previousInstance;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        server = MockBukkit.mock();
        command = new GreetCommand();
        setField(command, "greetService", new GreetService());

        visitorData = mock(DataOperator.class);
        UltiToolsExtExample plugin = mock(UltiToolsExtExample.class);
        when(plugin.getVisitorData()).thenReturn(visitorData);
        previousInstance = swapPluginInstance(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        swapPluginInstance(previousInstance);
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Registration contract read by CommandManager#registerAllExternal")
    class Registration {

        @Test
        @DisplayName("is a Bukkit CommandExecutor, the bean type registerAllExternal collects")
        void isCommandExecutorBean() {
            assertThat(command).isInstanceOf(CommandExecutor.class);
            assertThat(command).isInstanceOf(BaseCommandExecutor.class);
        }

        @Test
        @DisplayName("carries @CmdExecutor with its aliases and permission, not manually registered")
        void carriesCmdExecutor() {
            CmdExecutor annotation = GreetCommand.class.getAnnotation(CmdExecutor.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.alias()).containsExactly("ultiext", "uext");
            assertThat(annotation.permission()).isEqualTo("ultiext.greet");
            assertThat(annotation.manualRegister()).isFalse();
        }

        @Test
        @DisplayName("maps every documented sub-command")
        void mapsEverySubCommand() {
            assertThat(command.getMappings().keySet()).containsExactlyInAnyOrder(
                    "hello", "info", "visit <name>", "visitors", "delvisitor <name>");
        }
    }

    @Nested
    @DisplayName("Dispatch through onCommand")
    class Dispatch {

        @Test
        @DisplayName("/ultiext hello from a permitted player greets that player by name")
        void helloFromPlayer() {
            PlayerMock player = server.addPlayer("Steve");
            player.setOp(true);

            boolean handled = dispatch(player, "hello");

            assertThat(handled).isTrue();
            assertThat(player.nextMessage()).isEqualTo("Hello Steve" + HELLO_SUFFIX);
            assertThat(player.nextMessage()).isNull();
        }

        @Test
        @DisplayName("/ultiext hello from the console greets \"Console\"")
        void helloFromConsole() {
            ConsoleCommandSenderMock console = (ConsoleCommandSenderMock) server.getConsoleSender();

            dispatch(console, "hello");

            assertThat(console.nextMessage()).isEqualTo("Hello Console" + HELLO_SUFFIX);
        }

        @Test
        @DisplayName("/ultiext info reports the example's info line")
        void info() {
            PlayerMock player = server.addPlayer("Steve");
            player.setOp(true);

            dispatch(player, "info");

            assertThat(player.nextMessage())
                    .isEqualTo("UltiTools External Example v1.0.0 - verifying @Service injection works!");
        }

        @Test
        @DisplayName("/ultiext visit <name> binds the <name> parameter and inserts a first visit")
        void visitCreatesRecord() {
            PlayerMock player = server.addPlayer("Steve");
            player.setOp(true);
            when(visitorData.getAll(any(WhereCondition.class))).thenReturn(Collections.<VisitorRecord>emptyList());

            dispatch(player, "visit", "Alex");

            ArgumentCaptor<VisitorRecord> inserted = ArgumentCaptor.forClass(VisitorRecord.class);
            verify(visitorData).insert(inserted.capture());
            assertThat(inserted.getValue().getPlayerName()).isEqualTo("Alex");
            assertThat(inserted.getValue().getVisitCount()).isEqualTo(1);
            assertThat(player.nextMessage()).isEqualTo("[DATA] Created record for Alex — first visit!");
        }
    }

    @Nested
    @DisplayName("Mapped methods")
    class MappedMethods {

        private PlayerMock player;

        @BeforeEach
        void addPlayer() {
            player = server.addPlayer("Steve");
        }

        @Test
        @DisplayName("visit filters on player_name and increments an existing record")
        void visitUpdatesExistingRecord() throws Exception {
            VisitorRecord existing = new VisitorRecord("Alex");
            when(visitorData.getAll(any(WhereCondition.class))).thenReturn(listOf(existing));

            command.visit(player, "Alex");

            ArgumentCaptor<WhereCondition> where = ArgumentCaptor.forClass(WhereCondition.class);
            verify(visitorData).getAll(where.capture());
            assertThat(where.getValue().getColumn()).isEqualTo("player_name");
            assertThat(where.getValue().getValue()).isEqualTo("Alex");
            verify(visitorData).update(existing);
            verify(visitorData, never()).insert(any(VisitorRecord.class));
            assertThat(existing.getVisitCount()).isEqualTo(2);
            assertThat(player.nextMessage()).isEqualTo("[DATA] Updated Alex — visit #2");
        }

        @Test
        @DisplayName("visit reports a failed update instead of a success line")
        void visitReportsFailedUpdate() throws Exception {
            VisitorRecord existing = new VisitorRecord("Alex");
            when(visitorData.getAll(any(WhereCondition.class))).thenReturn(listOf(existing));
            org.mockito.Mockito.doThrow(new IllegalAccessException("denied")).when(visitorData).update(existing);

            command.visit(player, "Alex");

            assertThat(player.nextMessage()).isEqualTo("[ERROR] Update failed: denied");
            assertThat(player.nextMessage()).isNull();
        }

        @Test
        @DisplayName("visitors reports an empty store")
        void visitorsEmpty() {
            when(visitorData.getAll()).thenReturn(Collections.<VisitorRecord>emptyList());

            command.visitors(player);

            assertThat(player.nextMessage()).isEqualTo("[DATA] No visitor records.");
        }

        @Test
        @DisplayName("visitors lists every record with its visit count")
        void visitorsLists() {
            VisitorRecord alex = new VisitorRecord("Alex");
            VisitorRecord sam = new VisitorRecord("Sam");
            sam.incrementVisit();
            when(visitorData.getAll()).thenReturn(Arrays.asList(alex, sam));

            command.visitors(player);

            assertThat(player.nextMessage()).isEqualTo("[DATA] === Visitor Records (2) ===");
            assertThat(player.nextMessage()).isEqualTo("  Alex — 1 visits");
            assertThat(player.nextMessage()).isEqualTo("  Sam — 2 visits");
        }

        @Test
        @DisplayName("delvisitor deletes by player_name and confirms")
        void delVisitor() {
            command.delVisitor(player, "Alex");

            ArgumentCaptor<WhereCondition> where = ArgumentCaptor.forClass(WhereCondition.class);
            verify(visitorData).del(where.capture());
            assertThat(where.getValue().getColumn()).isEqualTo("player_name");
            assertThat(where.getValue().getValue()).isEqualTo("Alex");
            assertThat(player.nextMessage()).isEqualTo("[DATA] Deleted visitor record for Alex");
        }

        @Test
        @DisplayName("data sub-commands report an uninitialised DataOperator")
        void dataOperatorMissing() throws Exception {
            UltiToolsExtExample withoutData = mock(UltiToolsExtExample.class);
            swapPluginInstance(withoutData);

            command.visit(player, "Alex");
            command.visitors(player);
            command.delVisitor(player, "Alex");

            for (int i = 0; i < 3; i++) {
                assertThat(player.nextMessage()).isEqualTo("[ERROR] DataOperator not initialized!");
            }
        }

        @Test
        @DisplayName("help lists every sub-command")
        void help() {
            command.handleHelp(player);

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = player.nextMessage()) != null) {
                lines.add(line);
            }
            assertThat(lines).anySatisfy(l -> assertThat(l).startsWith("/ultiext hello"));
            assertThat(lines).anySatisfy(l -> assertThat(l).startsWith("/ultiext info"));
            assertThat(lines).anySatisfy(l -> assertThat(l).startsWith("/ultiext visit <name>"));
            assertThat(lines).anySatisfy(l -> assertThat(l).startsWith("/ultiext visitors"));
            assertThat(lines).anySatisfy(l -> assertThat(l).startsWith("/ultiext delvisitor"));
        }
    }

    /**
     * Calls {@code onCommand} the way Bukkit does for {@code /ultiext <args>}, then runs the
     * scheduler tick on which BaseCommandExecutor executes synchronous command bodies.
     */
    private boolean dispatch(org.bukkit.command.CommandSender sender, String... args) {
        Command bukkitCommand = mock(Command.class);
        when(bukkitCommand.getName()).thenReturn("ultiext");
        boolean handled = command.onCommand(sender, bukkitCommand, "ultiext", args);
        server.getScheduler().performOneTick();
        return handled;
    }

    private static List<VisitorRecord> listOf(VisitorRecord record) {
        List<VisitorRecord> list = new ArrayList<>();
        list.add(record);
        return list;
    }

    private static Object swapPluginInstance(Object value) throws Exception {
        Field field = UltiToolsExtExample.class.getDeclaredField("instance");
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, value);
        return previous;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
