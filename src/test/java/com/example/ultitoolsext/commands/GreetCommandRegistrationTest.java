package com.example.ultitoolsext.commands;

import com.example.ultitoolsext.UltiToolsExtExample;
import com.example.ultitoolsext.services.GreetService;
import com.ultikits.ultitools.UltiTools;
import com.ultikits.ultitools.api.ExternalPluginAdapter;
import com.ultikits.ultitools.context.SimpleContainer;
import com.ultikits.ultitools.manager.CommandManager;
import com.ultikits.ultitools.manager.PluginManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Runs {@link GreetCommand} through the framework's own external-plugin registration code rather
 * than asserting its annotations by hand (UltiKits/UltiTools-External-Example#4).
 * <p>
 * The test repeats the container steps {@code PluginManager#registerExternal} performs for
 * {@code UltiToolsAPI.connect(this)} (plugin class loader, {@code JavaPlugin} type, package scan,
 * {@code refresh()}), then calls the real package-private contract check
 * {@code PluginManager.validateCommandExecutorContracts} and the real
 * {@link CommandManager#registerAllExternal(ExternalPluginAdapter)} against a real Bukkit
 * {@link SimpleCommandMap}. Only {@code UltiTools.getInstance()} and {@code Bukkit}'s static
 * accessors are stubbed, because the framework plugin itself is not running.
 * <p>
 * On 6.3.0 a command class that is not a {@code BaseCommandExecutor} (and so not a Bukkit
 * {@link CommandExecutor}) is skipped by {@code registerAllExternal} without an error, which is the
 * symptom of issue #4: no {@code /ultiext} command exists afterwards.
 */
@DisplayName("GreetCommand through the framework's external registration")
class GreetCommandRegistrationTest {

    private static final String PLUGIN_NAME = "UltiToolsExtExample";

    @Test
    @DisplayName("scan + registerAllExternal register /ultiext and /uext; unregisterAllExternal removes them")
    @SuppressWarnings("deprecation")
    void registersAndUnregistersUltiext() throws Exception {
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getLogger(PLUGIN_NAME));
        SimpleCommandMap commandMap = new SimpleCommandMap(server, new HashMap<String, Command>());
        org.bukkit.plugin.SimplePluginManager pluginManager =
                new org.bukkit.plugin.SimplePluginManager(server, commandMap);

        UltiTools ultiTools = mock(UltiTools.class);
        when(ultiTools.getDescription()).thenReturn(
                new PluginDescriptionFile("UltiTools", "6.3.0", "com.ultikits.ultitools.UltiTools"));

        UltiToolsExtExample example = mock(UltiToolsExtExample.class);
        when(example.getName()).thenReturn(PLUGIN_NAME);
        when(example.getDescription()).thenReturn(new PluginDescriptionFile(
                PLUGIN_NAME, "1.0.0", UltiToolsExtExample.class.getName()));
        when(example.getDataFolder()).thenReturn(new File("target", PLUGIN_NAME));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<UltiTools> ulti = mockStatic(UltiTools.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getLogger).thenReturn(Logger.getLogger(PLUGIN_NAME));
            ulti.when(UltiTools::getInstance).thenReturn(ultiTools);

            ExternalPluginAdapter adapter = new ExternalPluginAdapter(example);
            SimpleContainer context = new SimpleContainer();
            context.setClassLoader(mainClassesFirst(adapter.getPluginClassLoader()));
            context.registerType(JavaPlugin.class, example);
            context.scanComponents(adapter.getScanPackage());
            context.refresh();
            adapter.setContext(context);

            Method contractCheck = PluginManager.class
                    .getDeclaredMethod("validateCommandExecutorContracts", SimpleContainer.class);
            contractCheck.setAccessible(true);
            contractCheck.invoke(null, context);

            CommandManager commandManager = new CommandManager();
            commandManager.registerAllExternal(adapter);

            Command registered = commandMap.getCommand("ultiext");
            assertThat(registered).isInstanceOf(PluginCommand.class);
            assertThat(commandMap.getCommand("uext")).isSameAs(registered);
            assertThat(registered.getPermission()).isEqualTo("ultiext.greet");
            CommandExecutor executor = ((PluginCommand) registered).getExecutor();
            assertThat(executor).isInstanceOf(GreetCommand.class);
            assertThat(readField(executor, "greetService")).isInstanceOf(GreetService.class);

            commandManager.unregisterAllExternal(PLUGIN_NAME);

            assertThat(commandMap.getCommand("ultiext")).isNull();
            assertThat(commandMap.getCommand("uext")).isNull();
        }
    }

    /**
     * On a server the plugin's class loader resolves the package to the plugin's own jar. Under
     * Surefire the application class loader resolves {@code com/example/ultitoolsext} to
     * {@code target/test-classes} first, which holds only test classes, so the scan would find no
     * component. Resolve resources against the directory {@link GreetCommand} was loaded from.
     */
    private static ClassLoader mainClassesFirst(ClassLoader parent) {
        final URL mainClasses = GreetCommand.class.getProtectionDomain().getCodeSource().getLocation();
        return new ClassLoader(parent) {
            @Override
            public URL getResource(String name) {
                try {
                    URL url = new URL(mainClasses, name);
                    return new File(url.toURI()).exists() ? url : super.getResource(name);
                } catch (Exception e) {
                    return super.getResource(name);
                }
            }
        };
    }

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
