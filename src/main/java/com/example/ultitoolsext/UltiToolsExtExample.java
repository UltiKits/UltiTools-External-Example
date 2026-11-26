package com.example.ultitoolsext;

import com.example.ultitoolsext.data.VisitorRecord;
import com.ultikits.ultitools.api.UltiToolsAPI;
import com.ultikits.ultitools.interfaces.DataOperator;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class UltiToolsExtExample extends JavaPlugin {

    @Getter
    private DataOperator<VisitorRecord> visitorData;

    @Override
    public void onEnable() {
        // Connect to UltiTools — scans package for @Service, @CmdExecutor, @EventListener
        UltiToolsAPI.connect(this);

        // Get a DataOperator scoped to this plugin's data folder
        visitorData = UltiToolsAPI.getDataOperator(this, VisitorRecord.class);
        getLogger().info("DataOperator type: " + visitorData.getClass().getSimpleName());

        getLogger().info("UltiTools External Example enabled!");
    }

    @Override
    public void onDisable() {
        try {
            UltiToolsAPI.disconnect(this);
        } finally {
            visitorData = null;
        }
        getLogger().info("UltiTools External Example disabled!");
    }
}
