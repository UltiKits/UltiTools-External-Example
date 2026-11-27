package com.example.fieldjournal;

import com.ultikits.ultitools.annotations.Scheduled;
import com.ultikits.ultitools.annotations.Service;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

@Service
public class JournalReminder {
    private final Messages messages;

    public JournalReminder(Messages messages) {
        this.messages = messages;
    }

    @Scheduled(config = JournalConfig.class, periodKey = JournalConfig.INTERVAL, delayKey = JournalConfig.INTERVAL)
    public void remind() {
        remind(Bukkit.getOnlinePlayers(), Bukkit.getLogger());
    }

    void remind(Iterable<? extends Player> players, Logger logger) {
        for (Player player : players) {
            if (player.hasPermission(JournalCommand.PERMISSION)) {
                messages.sendActionBar(player, Text.of(messages.key("journal.reminder")));
            }
        }
        logger.info("Field Journal reminder tick.");
    }
}
