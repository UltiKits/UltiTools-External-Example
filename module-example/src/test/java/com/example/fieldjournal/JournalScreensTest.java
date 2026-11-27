package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.scheduling.Scheduler;
import com.ultikits.ultitools.scheduling.TaskContext;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.MessageKey;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import com.ultikits.ultitools.ui.CloseReason;
import com.ultikits.ultitools.ui.Form;
import com.ultikits.ultitools.ui.FormResponse;
import com.ultikits.ultitools.ui.Grid;
import com.ultikits.ultitools.ui.GridClick;
import com.ultikits.ultitools.ui.GridSession;
import com.ultikits.ultitools.ui.GridSlot;
import com.ultikits.ultitools.ui.TextInput;
import com.ultikits.ultitools.ui.UiResult;
import com.ultikits.ultitools.ui.UiService;
import com.ultikits.ultitools.ui.UiSession;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JournalScreensTest {
    private final UltiToolsPlugin module = mock(UltiToolsPlugin.class);
    private final JournalStore store = mock(JournalStore.class);
    private final Messages messages = mock(Messages.class);
    private final UiService ui = mock(UiService.class);
    private final Scheduler scheduler = mock(Scheduler.class);
    private final Player player = mock(Player.class);
    private final UUID id = UUID.randomUUID();
    private final JournalScreens screens = new JournalScreens(module, store, messages, ui, scheduler);
    private final Queue<Runnable> entityTasks = new ArrayDeque<>();
    private final CompletableFuture<UiResult<FormResponse>> result = new CompletableFuture<>();

    @BeforeEach
    void setUp() {
        when(messages.key(anyString())).thenAnswer(call -> key(call.getArgument(0)));
        when(module.getConfig(JournalConfig.class)).thenReturn(new JournalConfig("config/journal.yml"));
        when(player.getUniqueId()).thenReturn(id);
    }

    @Test
    void completedFormSavesLiteralInputOnTheEntityContext() {
        Form form = openNoteWithPendingResult();
        var input = (TextInput) form.inputs().getFirst();
        assertEquals("note", input.key());
        assertEquals(80, input.maxLength());
        assertEquals(Text.of(key("form.submit")), form.submit());
        when(store.saveNote(id, "<red>Blue moss")).thenReturn(true);

        result.complete(new UiResult.Completed<>(FormResponse.of(form, Map.of(input, " <red>Blue moss "))));

        verifyNoInteractions(store);
        entityTasks.remove().run();
        verify(store).saveNote(id, "<red>Blue moss");
        verify(messages).send(player, key("journal.note-saved"), Arg.string("note", "<red>Blue moss"));
    }

    @Test
    void cancelledFormDoesNotWrite() {
        openNoteWithPendingResult();
        result.complete(new UiResult.Closed<>(CloseReason.PLAYER));
        entityTasks.remove().run();
        verifyNoInteractions(store);
    }

    @Test
    void rejectsBlankFormBeforeCallingTheStore() {
        Form form = openNoteWithPendingResult();
        result.complete(new UiResult.Completed<>(FormResponse.of(form, Map.of(form.inputs().getFirst(), "   "))));
        entityTasks.remove().run();
        verifyNoInteractions(store);
        verify(messages).send(player, key("journal.blank-note"));
    }

    @Test
    void centreOfChestShowsTheClickingPlayersSavedEntryAndCloses() {
        JournalEntry entry = new JournalEntry();
        entry.record("forest");
        entry.replaceNote("Blue moss");
        when(store.find(id)).thenReturn(entry);
        screens.openBook(player);

        ArgumentCaptor<Grid> grid = ArgumentCaptor.forClass(Grid.class);
        verify(ui).show(eq(player), grid.capture());
        assertEquals(1, grid.getValue().rows());
        assertEquals(Text.of(key("book.title"), Arg.string("journal", "Field Journal")), grid.getValue().title());
        var item = grid.getValue().item(new GridSlot(0, 4)).orElseThrow();
        assertEquals(Key.key("minecraft:writable_book"), item.icon().itemType());
        assertEquals(Text.of(key("book.read")), item.icon().name());

        GridClick click = mock(GridClick.class);
        GridSession session = mock(GridSession.class);
        when(click.viewer()).thenReturn(player);
        when(click.session()).thenReturn(session);
        item.action().orElseThrow().onClick(click);

        verify(store).find(id);
        verify(messages).send(player, key("journal.status"),
                Arg.number("count", 1), Arg.text("site", Text.of(key("site.forest"))));
        verify(messages).send(player, key("journal.note"), Arg.string("note", "Blue moss"));
        verify(session).close();
    }

    @Test
    void openingAnotherBookUsesTheCurrentConfigurationName() {
        screens.openBook(player);
        JournalConfig reloaded = mock(JournalConfig.class);
        when(reloaded.journalName()).thenReturn("Ranger Log");
        when(module.getConfig(JournalConfig.class)).thenReturn(reloaded);
        screens.openBook(player);

        ArgumentCaptor<Grid> grids = ArgumentCaptor.forClass(Grid.class);
        verify(ui, times(2)).show(eq(player), grids.capture());
        assertEquals(List.of(Text.of(key("book.title"), Arg.string("journal", "Field Journal")),
                        Text.of(key("book.title"), Arg.string("journal", "Ranger Log"))),
                grids.getAllValues().stream().map(Grid::title).toList());
    }

    private Form openNoteWithPendingResult() {
        @SuppressWarnings("unchecked")
        UiSession<FormResponse> session = mock(UiSession.class);
        when(session.result()).thenReturn(result);
        when(ui.show(eq(player), any(Form.class))).thenReturn(session);
        TaskContext entity = mock(TaskContext.class);
        when(scheduler.entity(player)).thenReturn(entity);
        doAnswer(call -> {
            entityTasks.add(call.getArgument(0));
            return null;
        }).when(entity).execute(any(Runnable.class));

        screens.openNote(player);
        ArgumentCaptor<Form> form = ArgumentCaptor.forClass(Form.class);
        verify(ui).show(eq(player), form.capture());
        assertTrue(entityTasks.isEmpty());
        return form.getValue();
    }

    private static MessageKey key(String path) {
        return MessageKey.of("fieldjournal", path);
    }
}
