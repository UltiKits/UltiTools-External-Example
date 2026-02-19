package com.example.ultitoolsext.listeners;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.example.ultitoolsext.services.GreetService;
import com.ultikits.ultitools.annotations.Autowired;
import com.ultikits.ultitools.annotations.EventListener;
import com.ultikits.ultitools.entities.WhereCondition;
import com.ultikits.ultitools.interfaces.DataOperator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

@EventListener
public class JoinListener implements Listener {

    @Autowired
    private GreetService greetService;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        event.getPlayer().sendMessage(greetService.greet(name));

        // Auto-record visit on join
        DataOperator<VisitorRecord> data = UltiToolsExtExample.getInstance().getVisitorData();
        if (data != null) {
            List<VisitorRecord> existing = data.getAll(
                    WhereCondition.builder().column("player_name").value(name).build()
            );
            if (!existing.isEmpty()) {
                VisitorRecord record = existing.get(0);
                record.incrementVisit();
                try {
                    data.update(record);
                } catch (IllegalAccessException e) {
                    event.getPlayer().sendMessage("[ExtExample] Failed to update visit: " + e.getMessage());
                }
            } else {
                data.insert(new VisitorRecord(name));
            }
        }
    }
}
