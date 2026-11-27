package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.AbstractConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntity;
import com.ultikits.ultitools.annotations.ConfigEntry;
import com.ultikits.ultitools.annotations.config.NotEmpty;
import com.ultikits.ultitools.annotations.config.Size;

@ConfigEntity("config/journal.yml")
public class JournalConfig extends AbstractConfigEntity {
    static final String INTERVAL = "reminder-seconds";

    @NotEmpty
    @Size(max = 40)
    @ConfigEntry(path = "journal-name", comment = "{config.journal-name}")
    private String journalName = "Field Journal";

    @ConfigEntry(path = INTERVAL, comment = "{config.reminder-seconds}")
    private int reminderSeconds = 60;

    public JournalConfig(String path) {
        super(path);
    }

    public String journalName() {
        return journalName;
    }
}
