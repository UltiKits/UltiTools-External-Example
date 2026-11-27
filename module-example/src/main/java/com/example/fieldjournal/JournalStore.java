package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.Service;
import com.ultikits.ultitools.interfaces.DataOperator;

import java.util.UUID;

@Service
public class JournalStore {
    private final UltiToolsPlugin module;

    public JournalStore(UltiToolsPlugin module) {
        this.module = module;
    }

    public JournalEntry find(UUID playerId) {
        return operator().getById(playerId.toString());
    }

    public JournalEntry record(UUID playerId, String site) {
        DataOperator<JournalEntry> entries = operator();
        String id = playerId.toString();
        JournalEntry entry = entries.getById(id);
        if (entry == null) {
            entry = new JournalEntry();
            entry.setId(id);
            entry.record(site);
            entries.insert(entry);
        } else {
            entry.record(site);
            if (entries.updateCounted(entry) != 1) {
                throw new IllegalStateException("Journal entry disappeared before it could be saved");
            }
        }
        return entry;
    }

    public boolean saveNote(UUID playerId, String note) {
        DataOperator<JournalEntry> entries = operator();
        JournalEntry entry = entries.getById(playerId.toString());
        if (entry == null) {
            return false;
        }
        entry.replaceNote(note);
        return entries.updateCounted(entry) == 1;
    }

    private DataOperator<JournalEntry> operator() {
        return module.getDataOperator(JournalEntry.class);
    }
}
