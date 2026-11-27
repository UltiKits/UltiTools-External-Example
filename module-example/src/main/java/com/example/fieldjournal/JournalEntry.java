package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.data.BaseDataEntity;
import com.ultikits.ultitools.annotations.Column;
import com.ultikits.ultitools.annotations.Table;

@Table("field_journal_entries")
public class JournalEntry extends BaseDataEntity<String> {
    @Column(value = "entry_count", type = "INTEGER")
    private int entryCount;

    @Column("site")
    private String site;

    @Column("note")
    private String note = "";

    public int entryCount() {
        return entryCount;
    }

    public String site() {
        return site;
    }

    public String note() {
        return note;
    }

    void record(String site) {
        this.site = site;
        entryCount++;
    }

    void replaceNote(String note) {
        this.note = note;
    }
}
