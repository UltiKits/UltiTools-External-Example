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
import com.ultikits.ultitools.api.UltiToolsAPI;
import com.ultikits.ultitools.entities.WhereCondition;
import com.ultikits.ultitools.interfaces.DataOperator;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Example command for a plain Bukkit {@code JavaPlugin} that uses UltiTools through the External
 * Plugin API rather than extending {@code UltiToolsPlugin}.
 * <p>
 * How it is wired: {@code UltiToolsAPI.connect(this)} in {@link UltiToolsExtExample#onEnable()}
 * scans this plugin's package into its own IoC container, which creates this class as a bean and
 * injects {@link GreetService} and {@link Messages} through its {@code @Autowired} constructor.
 * 框架把 {@code @CmdMapping} 编译为 Brigadier 命令树，并在 Paper 的命令生命周期事件中
 * 注册主名称 {@code /ultiext} 和别名 {@code /uext}。命令树检查发送者权限、解析参数，
 * 然后调用相应的方法。
 */
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
@CmdExecutor(
        permission = "ultiext.greet",
        description = "External plugin example command",
        alias = {"ultiext", "uext"}
)
public class GreetCommand extends BaseCommandExecutor {

    private final UltiToolsExtExample plugin;
    private final GreetService greetService;
    private final Messages messages;

    @Autowired
    public GreetCommand(UltiToolsExtExample plugin, GreetService greetService, Messages messages) {
        this.plugin = plugin;
        this.greetService = greetService;
        this.messages = messages;
    }

    @CmdMapping(format = "hello")
    public void hello(@CmdSender CommandSender sender) {
        messages.send(sender, greetService.greet(sender));
    }

    @CmdMapping(format = "info")
    public void info(@CmdSender CommandSender sender) {
        messages.send(sender, greetService.info());
    }

    @CmdMapping(format = "reload")
    public void reload(@CmdSender CommandSender sender) {
        try {
            UltiToolsAPI.reloadMessages(plugin);
        } catch (IllegalStateException notConnected) {
            messages.send(sender, messages.key("reload.not_connected"));
            return;
        }
        messages.send(sender, messages.key("reload.success"));
    }

    // --- Data Storage Tests ---

    @CmdMapping(format = "visit <name>")
    public void visit(@CmdSender CommandSender sender, @CmdParam("name") String playerName) {
        DataOperator<VisitorRecord> data = plugin.getVisitorData();
        if (data == null) {
            messages.send(sender, messages.key("data.not_initialized"));
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
                messages.send(sender, messages.key("data.update_failed"),
                        Arg.string("error", String.valueOf(e.getMessage())));
                return;
            }
            messages.send(sender, messages.key("data.updated"),
                    Arg.string("name", playerName), Arg.number("count", record.getVisitCount()));
        } else {
            VisitorRecord record = new VisitorRecord(playerName);
            data.insert(record);
            messages.send(sender, messages.key("data.created"), Arg.string("name", playerName));
        }
    }

    @CmdMapping(format = "visitors")
    public void visitors(@CmdSender CommandSender sender) {
        DataOperator<VisitorRecord> data = plugin.getVisitorData();
        if (data == null) {
            messages.send(sender, messages.key("data.not_initialized"));
            return;
        }

        List<VisitorRecord> all = data.getAll();
        if (all.isEmpty()) {
            messages.send(sender, messages.key("data.empty"));
            return;
        }
        messages.send(sender, messages.key("data.header"), Arg.number("count", all.size()));
        for (VisitorRecord r : all) {
            messages.send(sender, messages.key("data.row"),
                    Arg.string("name", r.getPlayerName()), Arg.number("count", r.getVisitCount()));
        }
    }

    @CmdMapping(format = "delvisitor <name>")
    public void delVisitor(@CmdSender CommandSender sender, @CmdParam("name") String playerName) {
        DataOperator<VisitorRecord> data = plugin.getVisitorData();
        if (data == null) {
            messages.send(sender, messages.key("data.not_initialized"));
            return;
        }

        data.del(WhereCondition.builder().column("player_name").value(playerName).build());
        messages.send(sender, messages.key("data.deleted"), Arg.string("name", playerName));
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        messages.send(sender, messages.key("help.title"));
        messages.send(sender, messages.key("help.hello"));
        messages.send(sender, messages.key("help.info"));
        messages.send(sender, messages.key("help.visit"));
        messages.send(sender, messages.key("help.visitors"));
        messages.send(sender, messages.key("help.delvisitor"));
        messages.send(sender, messages.key("help.reload"));
        messages.send(sender, messages.key("help.footer"));
    }
}
