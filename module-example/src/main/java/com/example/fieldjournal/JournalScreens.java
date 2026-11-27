package com.example.fieldjournal;

import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.Service;
import com.ultikits.ultitools.exceptions.DataAccessException;
import com.ultikits.ultitools.item.ItemSpec;
import com.ultikits.ultitools.scheduling.Scheduler;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import com.ultikits.ultitools.ui.Form;
import com.ultikits.ultitools.ui.Grid;
import com.ultikits.ultitools.ui.TextInput;
import com.ultikits.ultitools.ui.UiService;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

@Service
public class JournalScreens {
    private final UltiToolsPlugin module;
    private final JournalStore store;
    private final Messages messages;
    private final UiService ui;
    private final Scheduler scheduler;

    public JournalScreens(UltiToolsPlugin module, JournalStore store, Messages messages, UiService ui,
                          Scheduler scheduler) {
        this.module = module;
        this.store = store;
        this.messages = messages;
        this.ui = ui;
        this.scheduler = scheduler;
    }

    public void openNote(Player player) {
        TextInput input = TextInput.builder("note", Text.of(messages.key("form.note")))
                .maxLength(80)
                .build();
        Form form = Form.builder(title("form.title"))
                .input(input)
                .submit(Text.of(messages.key("form.submit")))
                .build();
        ui.show(player, form).result().thenAcceptAsync(result -> {
            if (result.isCompleted()) {
                saveNote(player, result.value().get(input).strip());
            }
        }, scheduler.entity(player));
    }

    public void openBook(Player player) {
        Grid book = Grid.builder(title("book.title"), 1)
                .set(0, 4, ItemSpec.builder(Key.key("minecraft:writable_book"))
                        .name(messages.key("book.read")).build(), click -> {
                            showStatus(click.viewer());
                            click.session().close();
                        })
                .build();
        ui.show(player, book);
    }

    public void showStatus(Player player) {
        JournalEntry entry = store.find(player.getUniqueId());
        if (entry == null) {
            messages.send(player, messages.key("journal.empty"));
            return;
        }
        messages.send(player, messages.key("journal.status"),
                Arg.number("count", entry.entryCount()),
                Arg.text("site", Text.of(messages.key("site." + entry.site()))));
        if (!entry.note().isEmpty()) {
            messages.send(player, messages.key("journal.note"), Arg.string("note", entry.note()));
        }
    }

    private Text title(String key) {
        // 在打开界面时读取当前配置，不缓存重载前的值。
        return Text.of(messages.key(key), Arg.string("journal", module.getConfig(JournalConfig.class).journalName()));
    }

    private void saveNote(Player player, String note) {
        if (note.isEmpty()) {
            messages.send(player, messages.key("journal.blank-note"));
            return;
        }
        try {
            if (store.saveNote(player.getUniqueId(), note)) {
                messages.send(player, messages.key("journal.note-saved"), Arg.string("note", note));
            } else {
                messages.send(player, messages.key("journal.empty"));
            }
        } catch (DataAccessException failure) {
            messages.send(player, messages.key("journal.save-failed"));
        }
    }
}
