package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.UltiToolsModule;

@UltiToolsModule(scanBasePackages = "com.example.fieldjournal")
public class FieldJournal extends UltiToolsPlugin {

    @Override
    public boolean registerSelf() {
        return true;
    }
}
