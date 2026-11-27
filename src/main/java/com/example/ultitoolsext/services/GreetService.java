package com.example.ultitoolsext.services;

import com.ultikits.ultitools.annotations.Autowired;
import com.ultikits.ultitools.annotations.Service;
import com.ultikits.ultitools.text.Arg;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Service
public class GreetService {

    private final Messages messages;

    @Autowired
    public GreetService(Messages messages) {
        this.messages = messages;
    }

    public Text greet(CommandSender recipient) {
        Arg name = recipient instanceof Player
                ? Arg.string("name", recipient.getName())
                : Arg.text("name", Text.of(messages.key("greet.console")));
        return Text.of(messages.key("greet.hello"), name);
    }

    public Text info() {
        return Text.of(messages.key("greet.info"));
    }
}
