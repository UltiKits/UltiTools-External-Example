package com.example.ultitoolsext.commands;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.data.VisitorRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ultikits.ultitools.UltiTools;
import com.ultikits.ultitools.api.ExternalPluginAdapter;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.interfaces.DataOperator;
import com.ultikits.ultitools.manager.CommandManager;
import com.ultikits.ultitools.manager.DependenceManagers;
import com.ultikits.ultitools.manager.ListenerManager;
import com.ultikits.ultitools.scheduling.Scheduler;
import com.ultikits.ultitools.scheduling.TaskContext;
import com.ultikits.ultitools.scheduling.internal.FrameworkScheduling;
import com.ultikits.ultitools.text.LocaleResolver;
import com.ultikits.ultitools.text.MessageKey;
import com.ultikits.ultitools.text.Messages;
import com.ultikits.ultitools.text.internal.TextEngine;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockito.MockedStatic;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class GreetCommandRegistrationTest {
    @TempDir Path directory;

    @Test
    void aRootFirstConnectedAfterInitialEventWaitsForAnotherEvent() throws Exception {
        try (ExternalCommands harness = new ExternalCommands(directory)) {
            harness.commands.unregisterAllExternal(harness.adapter.getPluginName());
            harness.deliverCommands();
            harness.commands.registerAllExternal(harness.adapter);
            var console = harness.console();
            assertThrows(CommandSyntaxException.class, () -> harness.execute(console, "ultiext hello"));
            console.assertNoMoreSaid();
            harness.deliverCommands();
            harness.execute(console, "ultiext hello");
            console.assertSaid("Hello Console! This message is from an external plugin using UltiTools-API.");
        }
    }

    @Test
    void deliversScannedCommandAndAliasOnCommandsEventAndUnbindsBoth() throws Exception {
        try (ExternalCommands harness = new ExternalCommands(directory)) {
            var console = harness.console();
            assertThrows(CommandSyntaxException.class, () -> harness.execute(console, "ultiext info"));
            harness.deliverCommands();
            assertThat(harness.aliases).containsExactly("uext");
            assertThat(harness.description).isEqualTo("External plugin example command");
            harness.execute(console, "ultiext info");
            console.assertSaid("UltiTools External Example v1.0.0 - verifying @Service injection works!");
            harness.execute(console, "uext hello");
            console.assertSaid("Hello Console! This message is from an external plugin using UltiTools-API.");

            harness.deliverCommands();
            harness.execute(console, "uext hello");
            console.assertSaid("Hello Console! This message is from an external plugin using UltiTools-API.");

            var consoleSource = ExternalCommands.source(console);
            assertThat(harness.dispatcher.getRoot().getChild("ultiext").canUse(consoleSource)).isTrue();
            harness.commands.unregisterAllExternal(harness.adapter.getPluginName());
            for (String label : List.of("ultiext", "uext")) {
                assertThat(harness.dispatcher.getRoot().getChild(label).canUse(consoleSource)).isFalse();
                assertThrows(CommandSyntaxException.class, () -> harness.execute(console, label + " hello"));
            }
            console.assertNoMoreSaid();
            harness.deliverCommands();
            assertThat(harness.dispatcher.getRoot().getChildren()).isEmpty();
        }
    }
}

/** 只替代 Paper 的事件投递和 registrar；实际命令树由框架编译与执行。 */
final class ExternalCommands implements AutoCloseable {
    interface PluginLifecycle extends LifecycleEventManager<Plugin> { }
    interface CommandsEvent extends ReloadableRegistrarEvent<Commands> { }

    final ServerMock server = MockBukkit.mock();
    final MockedStatic<UltiTools> frameworkSingleton = mockStatic(UltiTools.class);
    final SimpleContainer context = new SimpleContainer();
    final UltiToolsExtExample plugin = mock(UltiToolsExtExample.class);
    final UltiTools framework = mock(UltiTools.class);
    final Scheduler scheduler = mock(Scheduler.class);
    final ListenerManager listeners = new ListenerManager();
    final CommandManager commands = new CommandManager();
    final TextEngine texts;
    final ExternalPluginAdapter adapter;
    final DataOperator<VisitorRecord> data;
    private LifecycleEventHandler<ReloadableRegistrarEvent<Commands>> callback;
    CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    List<String> aliases;
    String description;

    @SuppressWarnings("unchecked")
    ExternalCommands(Path directory) throws Exception {
        LocaleResolver locales = mock(LocaleResolver.class);
        when(locales.resolve(any())).thenReturn(Locale.ENGLISH);
        when(locales.fallback()).thenReturn(Locale.ENGLISH);
        texts = new TextEngine(locales, Logger.getLogger("external-example-test"), codeSource(UltiTools.class), directory);
        try {
            frameworkSingleton.when(UltiTools::getInstance).thenReturn(framework);
            when(framework.isEnabled()).thenReturn(true);
            when(framework.getServer()).thenReturn(server);
            when(framework.getPluginLoader()).thenReturn(MockBukkit.createMockPlugin().getPluginLoader());
            var dependencies = mock(DependenceManagers.class);
            var root = mock(SimpleContainer.class);
            when(framework.getDependenceManagers()).thenReturn(dependencies);
            when(dependencies.getContext()).thenReturn(root);
            var messages = texts.messagesFor(MessageKey.FRAMEWORK_NAMESPACE);
            when(root.getBean(Messages.class)).thenReturn(messages);

            var global = mock(TaskContext.class);
            when(scheduler.global()).thenReturn(global);
            when(global.isCurrentThread()).thenReturn(true);
            FrameworkScheduling.install(scheduler);

            var lifecycle = mock(PluginLifecycle.class);
            when(framework.getLifecycleManager()).thenReturn(lifecycle);
            doAnswer(call -> {
                callback = call.getArgument(1);
                return null;
            }).when(lifecycle).registerEventHandler(eq(LifecycleEvents.COMMANDS), any());

            try (var descriptor = UltiToolsExtExample.class.getResourceAsStream("/plugin.yml")) {
                var description = new PluginDescriptionFile(descriptor);
                when(plugin.getName()).thenReturn(description.getName());
                when(plugin.getDescription()).thenReturn(description);
            }
            when(plugin.getDataFolder()).thenReturn(directory.toFile());
            data = mock(DataOperator.class);
            when(plugin.getVisitorData()).thenReturn(data);
            adapter = new ExternalPluginAdapter(plugin);

            // Surefire 默认先看到测试目录；扫描插件 JAR 在生产环境看到的主代码。
            context.setClassLoader(new ClassLoader(adapter.getPluginClassLoader()) {
                @Override public URL getResource(String name) {
                    try {
                        URL url = new URL(codeSource(GreetCommand.class), name);
                        return new File(url.toURI()).exists() ? url : super.getResource(name);
                    } catch (java.net.MalformedURLException | URISyntaxException error) {
                        throw new IllegalStateException(error);
                    }
                }
            });
            context.registerType(UltiToolsExtExample.class, plugin);
            context.registerType(JavaPlugin.class, plugin);
            context.scanComponents(adapter.getScanPackage());
            context.refresh();
            adapter.setContext(context);
            commands.registerAllExternal(adapter);
        } catch (Exception | Error failure) {
            close();
            throw failure;
        }
    }

    @SuppressWarnings("unchecked")
    void deliverCommands() {
        dispatcher = new CommandDispatcher<>();
        Commands registrar = mock(Commands.class);
        when(registrar.register(any(LiteralCommandNode.class), anyString(), anyCollection())).thenAnswer(call -> {
            LiteralCommandNode<CommandSourceStack> root = call.getArgument(0);
            description = call.getArgument(1);
            aliases = List.copyOf(call.<Collection<String>>getArgument(2));
            dispatcher.getRoot().addChild(root);
            for (String alias : aliases) {
                var copy = new LiteralCommandNode<>(alias, root.getCommand(), root.getRequirement(),
                        root.getRedirect(), root.getRedirectModifier(), root.isFork());
                root.getChildren().forEach(copy::addChild);
                dispatcher.getRoot().addChild(copy);
            }
            var labels = new HashSet<>(aliases);
            labels.add(root.getName());
            return labels;
        });
        CommandsEvent event = mock(CommandsEvent.class);
        when(event.registrar()).thenReturn(registrar);
        assertThat(callback).as("命令引擎安装 Paper 生命周期回调").isNotNull();
        callback.run(event);
    }

    static CommandSourceStack source(CommandSender sender) {
        var source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        return source;
    }

    int execute(CommandSender sender, String input) throws CommandSyntaxException {
        return dispatcher.execute(input, source(sender));
    }

    ConsoleCommandSenderMock console() {
        return (ConsoleCommandSenderMock) server.getConsoleSender();
    }

    private static URL codeSource(Class<?> type) {
        return type.getProtectionDomain().getCodeSource().getLocation();
    }

    @Override public void close() {
        try {
            // 监听器由模拟的框架实例持有，不在 MockBukkit 的插件卸载列表中。
            HandlerList.unregisterAll(framework);
            commands.close();
            context.close();
        } finally {
            FrameworkScheduling.uninstall(scheduler);
            texts.close();
            frameworkSingleton.close();
            MockBukkit.unmock();
        }
    }
}
