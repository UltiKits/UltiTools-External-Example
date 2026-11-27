package com.example.ultitoolsext.listeners;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.example.ultitoolsext.services.GreetService;
import com.ultikits.ultitools.annotations.Autowired;
import com.ultikits.ultitools.annotations.EventListener;
import com.ultikits.ultitools.entities.WhereCondition;
import com.ultikits.ultitools.interfaces.DataOperator;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

@EventListener
public class JoinListener implements Listener {

    private final UltiToolsExtExample plugin;
    private final GreetService greetService;
    private final Messages messages;

    @Autowired
    public JoinListener(UltiToolsExtExample plugin, GreetService greetService, Messages messages) {
        this.plugin = plugin;
        this.greetService = greetService;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        messages.send(player, greetService.greet(player));

        // Auto-record visit on join
        DataOperator<VisitorRecord> data = plugin.getVisitorData();
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
                    messages.send(player, messages.key("join.update_failed"),
                            Arg.string("error", String.valueOf(e.getMessage())));
                }
            } else {
                data.insert(new VisitorRecord(name));
            }
        }
    }
}
