package com.example.ultitoolsext.commands;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.example.ultitoolsext.services.GreetService;
import com.ultikits.ultitools.abstracts.command.BaseCommandExecutor;
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

/**
 * Example command for a plain Bukkit {@code JavaPlugin} that uses UltiTools through the External
 * Plugin API rather than extending {@code UltiToolsPlugin}.
 * <p>
 * How it is wired: {@code UltiToolsAPI.connect(this)} in {@link UltiToolsExtExample#onEnable()}
 * scans this plugin's package into its own IoC container, which creates this class as a bean and
 * injects {@link GreetService} through {@code @Autowired}. The framework's
 * {@code CommandManager#registerAllExternal} then registers every bean that is a Bukkit
 * {@code CommandExecutor} carrying {@code @CmdExecutor}, under that annotation's aliases
 * ({@code /ultiext}, {@code /uext}). When a player or the console runs the command, Bukkit calls
 * {@link BaseCommandExecutor#onCommand}, which picks the {@code @CmdMapping} whose format matches
 * the arguments, checks {@code @CmdTarget} and the {@code ultiext.greet} permission, binds
 * {@code @CmdSender}/{@code @CmdParam} parameters, and runs the method on the next server tick.
 * <p>
 * A command class extends {@link BaseCommandExecutor} and implements
 * {@link #handleHelp(CommandSender)}. The older {@code AbstractCommandExecutor} base class was
 * removed in UltiTools-API 6.3.0; while this example still extended it, none of the
 * {@code /ultiext} sub-commands registered (UltiKits/UltiTools-External-Example#4).
 * <p>
 * 通过外部插件 API（而非继承 {@code UltiToolsPlugin}）使用 UltiTools 的普通 Bukkit 插件的示例指令。
 * {@code UltiToolsAPI.connect(this)} 扫描本插件的包并将本类创建为 Bean，
 * {@code CommandManager#registerAllExternal} 再把带有 {@code @CmdExecutor} 的指令注册到 Bukkit。
 * 指令类需继承 {@link BaseCommandExecutor} 并实现 {@link #handleHelp(CommandSender)}；
 * 旧的 {@code AbstractCommandExecutor} 已在 UltiTools-API 6.3.0 中移除。
 */
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
@CmdExecutor(
        permission = "ultiext.greet",
        description = "External plugin example command",
        alias = {"ultiext", "uext"}
)
public class GreetCommand extends BaseCommandExecutor {

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
