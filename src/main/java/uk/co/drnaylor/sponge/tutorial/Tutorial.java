package uk.co.drnaylor.sponge.tutorial;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.inject.Inject;

/*
 * The @Plugin annotation indicates the class that is the entry point for
 * your plugin on the Sponge platform. It is a simple object that is created
 * by the Sponge system on startup before pre-init.
 *
 * In Bukkit, your main plugin object is specified in your plugin.yml file
 * and had to extend JavaPlugin. In Sponge, neither of these steps are
 * required
 *
 * Your plugin annotation MUST contain the following:
 *
 * * an "id", which must be all lowercase and unique to this project
 * * a name, which can be as descriptive as you like
 *
 * Your plugin annotation SHOULD contain the following:
 *
 * * a version, which is a string
 * * a description of your plugin
 *
 * Your plugin annotation CAN contain the following:
 *
 * * A list of authors
 * * A URL that users can go to for documentation on your plugin
 *
 * Your plugin annotation can also contain dependency information. You do
 * not need to include the SpongeAPI dependency here as this is implicit
 * when using the plugin annotation.
 *
 * Your plugin must either have a no-args constructor OR have a constructor
 * that is annotated with @Inject and only has injectable classes in there,
 * see https://docs.spongepowered.org/stable/en/plugin/injection.html for more
 * info. It is recommended that you perform field injection instead, which will
 * be discussed below.
 *
 * The @Inject annotations below can be either "javax.inject.Inject" or
 * "com.google.inject.Inject", it doesn't matter, they both work
 *
 * Event listeners that are in this main class are automatically registered. Event
 * listeners outside of this class will need to be registered separately.
 */
@Plugin(
        id = "tutorial",
        name = "Tutorial",
        version = "1.0",
        description = "Tutorial for Sponge on API 7",
        authors = {
                "dualspiral"
        }
)
public class Tutorial {

    /*
     * This is an injected field. Injected fields are provided by
     * Sponge when the plugin is constructed, in this case, the
     * plugin's logger is added here. This can be accessed after
     * the plugin's constructor has run, meaning that in reality,
     * it should be available during GamePreInitializationEvent
     *
     * Note that the logger is of type org.slf4j.Logger, be careful
     * to not select the standard Java logger here or this will return
     * the wrong object
     */
    @Inject
    private Logger logger;

    /*
     * This injects a pre-made HOCON configuration loader for the file
     * "config/<plugin-id>/<plugin-id>.conf".
     *
     * It is possible to create a YAML or JSON configuration if you prefer
     * however, but you'll need to do extra legwork
     *
     * If you set sharedRoot to "true", the configuration file will be located
     * at "config/<plugin-id>/<plugin-id>.conf". In general, it is recommended
     * that you set this to "false".
     *
     * Use the "load" method to get a "ConfigurationNode", use the "save" method
     * to save it
     */
    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configurationLoader;

    /*
     * This will get you the default config file location, that is,
     * "config/<plugin-id>/<plugin-id>.conf".
     */
    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path defaultConfigFileLocation;

    /*
     * This will get you the default directory, that is,
     * "config/<plugin-id>/". This allows you to more easily setup other
     * configuration files that you might want to generate, including YAML or
     * JSON files
     */
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path defaultConfigDirectory;

    // The following field and method can be used to generate a YML file instead of a
    // HOCON file on startup

    private YAMLConfigurationLoader yamlConfigurationLoader;

    /*
     * This method is picked up by the Guice injector and will create a YAML loader for you.
     * There are other options on the config loader builder that you can use.
     *
     * We recommend HOCON as a configuration file object
     */
    @Inject
    public void setupYAMLConfigLoader(@DefaultConfig(sharedRoot = false) Path defaultConfigDirectory) {
        this.yamlConfigurationLoader = YAMLConfigurationLoader.builder()
                .setPath(defaultConfigDirectory.resolve("config.yml"))
                .build();
    }

    /*
     * We'll be storing our sample config values here.
     */
    private SampleConfig config;

    /**
     * Gets the plugin logger
     *
     * @return The {@link Logger}
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Gets the configuration object
     *
     * @return The {@link SampleConfig}
     */
    public SampleConfig getConfig() {
        return this.config;
    }

    // Tasks object

    private final Tasks tasks = new Tasks(this);

    // SERVER INITIALISATION

    // See https://docs.spongepowered.org/stable/en/plugin/lifecycle.html

    /*
     * Before we continue, a note on how plugin enabling and disabling works.
     * There are three main phases to consider.
     *
     * INITIALISATION:
     *
     * These are the events:
     * * GamePreInitializationEvent
     * * GameInitializationEvent
     * * GamePostInitializationEvent
     * * GameLoadCompleteEvent
     *
     * These all happen only once in the server lifecycle. When starting out,
     * and in most cases beyond that, use GameInitializationEvent to create your
     * commands and register events, as well as any one time housekeeping you need
     * to do. If you need to interact with other plugins, do that during
     * GamePostInitializationEvent, and make sure any APIs you have are available
     * during post-init.
     *
     * During init, game worlds have not been loaded, the server has NOT been started.
     * This is Sponge/Forge initialisation, not Minecraft server initialisation. The
     * server object is not available at this stage.
     *
     * GAME RUNNING:
     *
     * These are the events:
     * * GameAboutToStartServerEvent - server exists, worlds do not
     * * GameStartingServerEvent - server exists and worlds exist
     * * GameStartedServerEvent - server has started and is now accepting clients
     *
     * In general, choose the earliest event that makes sense. NOTE THAT THESE
     * EVENTS CAN FIRE MORE THAN ONCE DURING A SESSION, for example, a client-server
     * stopping and starting without Minecraft closing.
     *
     * GAME STOPPING:
     *
     * These are the events:
     * * GameStoppingEvent - fired as the game server is beginning the server shutdown process
     *                       Worlds will be saved, plugins should still accept API requests
     * * GameStoppedEvent - fired when the server has stopped and the worlds are no more.
     *
     * Note that these may not fire if the server crashes or is closed using Ctrl+C
     */

    @Listener
    public void onServerInit(GameInitializationEvent event) {
        // Loading, saving and read configuration.
        // https://docs.spongepowered.org/stable/en/plugin/configuration/index.html
        try {
            // First, let's get our config object and load it.
            CommentedConfigurationNode configurationNode = this.configurationLoader.load();

            // We now use a function of Configurate to populate a config object which
            // will make using configurations easier for you.
            // Any nodes that don't exist will be left as the defaults in the config class
            // see SampleConfig for more details
            this.config = configurationNode.getValue(TypeToken.of(SampleConfig.class));

            // I use this to re-save a default config if the original doesn't exist. You might
            // want to consider saving if you change the config in the first place.
            if (!Files.exists(this.defaultConfigFileLocation)) {
                this.configurationLoader.save(
                        // Get the node from the config loader so the correct options are set
                        this.configurationLoader.createEmptyNode()
                                .setValue(TypeToken.of(SampleConfig.class), this.config));
            }
        } catch (IOException | ObjectMappingException e) {
            // If this errors, the rest of the plugin will run. You might want to write your
            // disabling routine, you might get some NPEs or other errors if you continue.
            e.printStackTrace();
        }

        // Creating commands
        // https://docs.spongepowered.org/stable/en/plugin/commands/index.html

        // Every command must be registered using the CommandManager
        Sponge.getCommandManager()
                // This plugin is registering the command
                .register(this,
                        // CommandSpec is our high level command builder to remove a lot of boilerplate code
                        // It can register child commands and do some fancy parsing. We'll just do the basics right now
                        CommandSpec.builder()
                                // Unlike in Bukkit, Sponge provides for custm argument parsing, saving you the
                                // trouble of boilerplate code. In this example, we want to send a player
                                // a message
                                .arguments(
                                        // The first argument is a player
                                        GenericArguments.player(Text.of("player")),
                                        // The rest of the arguments is a string message
                                        GenericArguments.remainingJoinedStrings(Text.of("message"))
                                )
                                // The executor can also be defined as a lambda
                                .executor(new ExampleCommandExecutor(this))
                                .build(),
                        "sendmessage");

        // Command /repeatbc <interval> <& encoded message>
        Sponge.getCommandManager()
                .register(this,
                        CommandSpec.builder()
                            .permission("tutorial.repeatbc")
                            .arguments(
                                    GenericArguments.integer(Text.of("seconds")),
                                    GenericArguments.text(Text.of("message"), TextSerializers.FORMATTING_CODE, true)
                            )
                            .executor((src, context) -> {
                                Text message = context.<Text>getOne("message").get();
                                this.tasks.createRecurringTaskThatBroadcastsAMessageEverySoOftenFiveTimes(
                                        message,
                                        context.<Integer>getOne("seconds").get()
                                );

                                src.sendMessage(Text.of(TextColors.GREEN, "Will broadcast the following 5 times:"));
                                src.sendMessage(Text.of(message));
                                return CommandResult.success();
                            })
                        .build(), "repeatbc");

        // Command /delayedbc <delay> <& encoded message>
        Sponge.getCommandManager()
                .register(this,
                        CommandSpec.builder()
                                .permission("tutorial.delayedbc")
                                .arguments(
                                        GenericArguments.integer(Text.of("seconds")),
                                        GenericArguments.text(Text.of("message"), TextSerializers.FORMATTING_CODE, true)
                                )
                                .executor((src, context) -> {
                                    Text message = context.<Text>getOne("message").get();
                                    int secs = context.<Integer>getOne("seconds").get();

                                    // Creates task
                                    this.tasks.sendADelayedMessage(message, secs);

                                    src.sendMessage(Text.of(TextColors.GREEN, "Will broadcast the following in ", secs, " seconds:"));
                                    src.sendMessage(Text.of(message));
                                    return CommandResult.success();
                                })
                                .build(), "delayedbc");

        // Command /healint [player]
        Sponge.getCommandManager()
                .register(this,
                        CommandSpec.builder()
                                .permission("tutorial.healint")
                                .arguments(
                                        // There is playerOrSource, but I find that will select yourself if you get an error, so
                                        // I go for optional + player here. Optional means "only parse if there is an argument to parse"
                                        // so "/heal" will select self, "/heal dualspiral" will select dualspiral, and "/heal idontexist"
                                        // will error.
                                        GenericArguments.optional(GenericArguments.player(Text.of("player")))
                                )
                                .executor((src, context) -> {
                                    Optional<Player> optionalPlayer = context.getOne("player");
                                    Player player;
                                    if (optionalPlayer.isPresent()) {
                                        player = optionalPlayer.get();
                                    } else if (src instanceof Player) {
                                        player = (Player) src;
                                    } else {
                                        throw new CommandException(Text.of(TextColors.RED, "This command requires a player!"));
                                    }

                                    // Creates task
                                    this.tasks.healPlayerEveryMinute(player.getUniqueId());

                                    src.sendMessage(Text.of(TextColors.GREEN, "Will heal " + player.getName() + " every minute until they log out:"));
                                    return CommandResult.success();
                                })
                                .build(), "healint");

        // Registering events is as easy as this. The first object in the method is the plugin object
        // (the one annotated with @Plugin), the second is your object containing listeners
        Sponge.getEventManager().registerListeners(this, new Listeners(this));
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        // Simple log message using the injected field
        this.logger.info("Server has started!");
    }
}