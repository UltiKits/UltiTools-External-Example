package com.example.ultitoolsext.commands;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.example.ultitoolsext.services.GreetService;
import com.ultikits.ultitools.abstracts.AbstractCommandExecutor;
import com.ultikits.ultitools.annotations.Autowired;
import com.ultikits.ultitools.annotations.command.CmdExecutor;
import com.ultikits.ultitools.annotations.command.CmdMapping;
import com.ultikits.ultitools.annotations.command.CmdParam;
import com.ultikits.ultitools.annotations.command.CmdSender;
import com.ultikits.ultitools.annotations.command.CmdTarget;
import com.ultikits.ultitools.entities.WhereCondition;
import com.ultikits.ultitools.interfaces.DataOperator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CmdTarget(CmdTarget.CmdTargetType.BOTH)
@CmdExecutor(
        permission = "ultiext.greet",
        description = "External plugin example command",
        alias = {"ultiext", "uext"}
)
public class GreetCommand extends AbstractCommandExecutor {

    @Autowired
    private GreetService greetService;

    @CmdMapping(format = "hello")
    public void hello(@CmdSender CommandSender sender) {
        String name = sender instanceof Player ? sender.getName() : "Console";
        sender.sendMessage(greetService.greet(name));
    }

    @CmdMapping(format = "info")
    public void info(@CmdSender CommandSender sender) {
        sender.sendMessage(greetService.info());
    }

    // --- Data Storage Tests ---

    @CmdMapping(format = "visit <name>")
    public void visit(@CmdSender CommandSender sender, @CmdParam("name") String playerName) {
        DataOperator<VisitorRecord> data = UltiToolsExtExample.getInstance().getVisitorData();
        if (data == null) {
            sender.sendMessage("[ERROR] DataOperator not initialized!");
            return;
        }

        // Check if visitor exists
        List<VisitorRecord> existing = data.getAll(
                WhereCondition.builder().column("player_name").value(playerName).build()
        );

        if (!existing.isEmpty()) {
            VisitorRecord record = existing.get(0);
            record.incrementVisit();
            try {
                data.update(record);
            } catch (IllegalAccessException e) {
                sender.sendMessage("[ERROR] Update failed: " + e.getMessage());
                return;
            }
            sender.sendMessage("[DATA] Updated " + playerName + " — visit #" + record.getVisitCount());
        } else {
            VisitorRecord record = new VisitorRecord(playerName);
            data.insert(record);
            sender.sendMessage("[DATA] Created record for " + playerName + " — first visit!");
        }
    }

    @CmdMapping(format = "visitors")
    public void visitors(@CmdSender CommandSender sender) {
        DataOperator<VisitorRecord> data = UltiToolsExtExample.getInstance().getVisitorData();
        if (data == null) {
            sender.sendMessage("[ERROR] DataOperator not initialized!");
            return;
        }

        List<VisitorRecord> all = data.getAll();
        if (all.isEmpty()) {
            sender.sendMessage("[DATA] No visitor records.");
            return;
        }
        sender.sendMessage("[DATA] === Visitor Records (" + all.size() + ") ===");
        for (VisitorRecord r : all) {
            sender.sendMessage("  " + r.getPlayerName() + " — " + r.getVisitCount() + " visits");
        }
    }

    @CmdMapping(format = "delvisitor <name>")
    public void delVisitor(@CmdSender CommandSender sender, @CmdParam("name") String playerName) {
        DataOperator<VisitorRecord> data = UltiToolsExtExample.getInstance().getVisitorData();
        if (data == null) {
            sender.sendMessage("[ERROR] DataOperator not initialized!");
            return;
        }

        data.del(WhereCondition.builder().column("player_name").value(playerName).build());
        sender.sendMessage("[DATA] Deleted visitor record for " + playerName);
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        sender.sendMessage("=== UltiTools External Example ===");
        sender.sendMessage("/ultiext hello           - Greet (tests @Service injection)");
        sender.sendMessage("/ultiext info            - Plugin info");
        sender.sendMessage("/ultiext visit <name>    - Record a visit (tests DataOperator insert/update)");
        sender.sendMessage("/ultiext visitors        - List all visitors (tests DataOperator getAll)");
        sender.sendMessage("/ultiext delvisitor <n>  - Delete a visitor (tests DataOperator del)");
        sender.sendMessage("==============================");
    }
}
