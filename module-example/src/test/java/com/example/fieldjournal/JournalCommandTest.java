package com.example.fieldjournal;

import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.annotations.command.CmdMapping;
import com.ultikits.ultitools.annotations.command.CmdParam;
import com.ultikits.ultitools.annotations.command.CmdTarget;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.MessageKey;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JournalCommandTest {
    private final JournalStore store = mock(JournalStore.class);
    private final JournalScreens screens = mock(JournalScreens.class);
    private final Messages messages = mock(Messages.class);
    private final Player player = mock(Player.class);
    private final JournalCommand command = new JournalCommand(store, screens, messages);

    @Test
    void declaresPlayerOnlyRootAndConnectsTheSiteArgumentToCompletion() throws Exception {
        CmdExecutor root = JournalCommand.class.getAnnotation(CmdExecutor.class);
        assertEquals(List.of("fieldjournal"), List.of(root.alias()));
        assertEquals("fieldjournal.use", root.permission());
        assertEquals(CmdTarget.CmdTargetType.PLAYER, JournalCommand.class.getAnnotation(CmdTarget.class).value());
        var record = JournalCommand.class.getMethod("record", Player.class, String.class);
        assertEquals("record <site>", record.getAnnotation(CmdMapping.class).format());
        CmdParam parameter = record.getParameters()[1].getAnnotation(CmdParam.class);
        assertEquals("site", parameter.value());
        var provider = JournalCommand.class.getMethod(parameter.suggest(), String.class);
        assertEquals(List.of("river"), provider.invoke(command, "RI"));
        assertEquals(List.of(), provider.invoke(command, "des"));
    }

    @Test
    void recordsNormalizedSiteAndSendsCountAndTranslatedSiteAsArguments() {
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(messages.key(anyString())).thenAnswer(call -> key(call.getArgument(0)));
        JournalEntry saved = new JournalEntry();
        saved.record("river");
        when(store.record(id, "river")).thenReturn(saved);

        command.record(player, "RIVER");

        verify(store).record(id, "river");
        verify(messages).send(player, key("journal.recorded"),
                Arg.text("site", Text.of(key("site.river"))), Arg.number("count", 1));
    }

    @Test
    void unknownSiteDoesNotReachTheStore() {
        when(messages.key("command.invalid-site")).thenReturn(key("command.invalid-site"));
        command.record(player, "desert");
        verify(messages).send(player, key("command.invalid-site"));
        verifyNoInteractions(store);
    }

    @Test
    void entryPointsDelegateToTheMatchingScreenOrStatus() {
        command.status(player);
        command.book(player);
        command.note(player);
        verify(screens).showStatus(player);
        verify(screens).openBook(player);
        verify(screens).openNote(player);
    }

    private static MessageKey key(String path) {
        return MessageKey.of("fieldjournal", path);
    }
}
