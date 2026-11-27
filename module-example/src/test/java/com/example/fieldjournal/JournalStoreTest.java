package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.Table;
import com.ultikits.ultitools.interfaces.DataOperator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalStoreTest {
    private final UUID id = UUID.randomUUID();
    private final UltiToolsPlugin module = mock(UltiToolsPlugin.class);
    @SuppressWarnings("unchecked")
    private final DataOperator<JournalEntry> entries = mock(DataOperator.class);
    private final JournalStore store = new JournalStore(module);

    JournalStoreTest() {
        when(module.getDataOperator(JournalEntry.class)).thenReturn(entries);
    }

    @Test
    void firstRecordInsertsWithPlayerIdUnderTheDeclaredTable() {
        JournalEntry inserted = store.record(id, "forest");

        assertEquals("field_journal_entries", JournalEntry.class.getAnnotation(Table.class).value());
        assertEquals(id.toString(), inserted.getId());
        assertEquals(1, inserted.entryCount());
        assertEquals("forest", inserted.site());
        assertEquals("", inserted.note());
        verify(entries).getById(id.toString());
        verify(entries).insert(inserted);
        verify(entries, never()).updateCounted(any());
    }

    @Test
    void laterRecordUpdatesRatherThanInsertingAndKeepsTheNote() {
        JournalEntry row = storedEntry();
        when(entries.updateCounted(row)).thenReturn(1);

        assertSame(row, store.record(id, "river"));
        assertSame(row, store.find(id));
        assertEquals(2, row.entryCount());
        assertEquals("river", row.site());
        assertEquals("Blue moss", row.note());
        verify(entries).updateCounted(row);
        verify(entries, never()).insert(any());
    }

    @Test
    void noteUpdatesExistingRowOnlyAndDoesNotCountAsAnObservation() {
        assertFalse(store.saveNote(id, "Not recorded yet"));
        verify(entries, never()).updateCounted(any());
        JournalEntry row = storedEntry();
        when(entries.updateCounted(row)).thenReturn(1);

        assertTrue(store.saveNote(id, "Tracks by the water"));
        assertEquals(1, row.entryCount());
        assertEquals("Tracks by the water", row.note());
        verify(entries).updateCounted(row);
        verify(entries, never()).insert(any());
    }

    private JournalEntry storedEntry() {
        JournalEntry row = new JournalEntry();
        row.setId(id.toString());
        row.record("forest");
        row.replaceNote("Blue moss");
        when(entries.getById(id.toString())).thenReturn(row);
        return row;
    }
}
