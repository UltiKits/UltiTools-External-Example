package com.example.fieldjournal;

import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.Scheduled;
import com.ultikits.ultitools.text.MessageKey;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JournalReminderTest {
    @Test
    void firstAndRepeatDelaysUseTheSameSettingInSeconds() throws Exception {
        Scheduled schedule = JournalReminder.class.getMethod("remind").getAnnotation(Scheduled.class);
        assertEquals(JournalConfig.class, schedule.config());
        var field = schedule.config().getDeclaredField("reminderSeconds");
        assertTrue(field.trySetAccessible());
        assertEquals(60, field.getInt(new JournalConfig("config/journal.yml")));
        String path = field.getAnnotation(ConfigEntry.class).path();
        assertEquals(path, schedule.delayKey());
        assertEquals(path, schedule.periodKey());
        assertEquals(0L, schedule.delay());
        assertEquals(-1L, schedule.period());
        assertFalse(schedule.async());
    }

    @Test
    void reminderSendsLocalizedActionBarOnlyToPlayersWhoCanUseTheJournal() {
        Messages messages = mock(Messages.class);
        Player allowed = mock(Player.class);
        Player denied = mock(Player.class);
        Logger logger = mock(Logger.class);
        MessageKey reminder = MessageKey.of("fieldjournal", "journal.reminder");
        when(messages.key("journal.reminder")).thenReturn(reminder);
        when(allowed.hasPermission("fieldjournal.use")).thenReturn(true);

        new JournalReminder(messages).remind(List.of(allowed, denied), logger);

        verify(messages).key("journal.reminder");
        verify(messages).sendActionBar(allowed, Text.of(reminder));
        verifyNoMoreInteractions(messages);
        verify(logger).info("Field Journal reminder tick.");
    }
}
