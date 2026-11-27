package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.command.BaseCommandExecutor;
import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.annotations.command.CmdMapping;
import com.ultikits.ultitools.annotations.command.CmdParam;
import com.ultikits.ultitools.annotations.command.CmdSender;
import com.ultikits.ultitools.annotations.command.CmdTarget;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

@CmdExecutor(alias = {"fieldjournal"}, permission = JournalCommand.PERMISSION, description = "command.description")
@CmdTarget(CmdTarget.CmdTargetType.PLAYER)
public class JournalCommand extends BaseCommandExecutor {
    static final String PERMISSION = "fieldjournal.use";
    private static final List<String> SITES = List.of("forest", "river", "mountain");

    private final JournalStore store;
    private final JournalScreens screens;
    private final Messages messages;

    public JournalCommand(JournalStore store, JournalScreens screens, Messages messages) {
        this.store = store;
        this.screens = screens;
        this.messages = messages;
    }

    @CmdMapping(format = "record <site>")
    public void record(@CmdSender Player player, @CmdParam(value = "site", suggest = "suggestSites") String site) {
        String normalized = site.toLowerCase(Locale.ROOT);
        if (!SITES.contains(normalized)) {
            messages.send(player, messages.key("command.invalid-site"));
            return;
        }
        JournalEntry entry = store.record(player.getUniqueId(), normalized);
        messages.send(player, messages.key("journal.recorded"),
                Arg.text("site", Text.of(messages.key("site." + entry.site()))),
                Arg.number("count", entry.entryCount()));
    }

    public List<String> suggestSites(String partial) {
        String prefix = partial.toLowerCase(Locale.ROOT);
        return SITES.stream().filter(site -> site.startsWith(prefix)).toList();
    }

    @CmdMapping(format = "status")
    public void status(@CmdSender Player player) {
        screens.showStatus(player);
    }

    @CmdMapping(format = "note")
    public void note(@CmdSender Player player) {
        screens.openNote(player);
    }

    @CmdMapping(format = "book")
    public void book(@CmdSender Player player) {
        screens.openBook(player);
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        messages.send(sender, messages.key("command.help"));
    }
}
