package uk.co.drnaylor.sponge.tutorial;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

public class ExampleCommandExecutor implements CommandExecutor {

    private final Tutorial plugin;

    // Injections only work on the main class unless you use your own
    // Guice injector
    public ExampleCommandExecutor(Tutorial plugin) {
        this.plugin = plugin;
    }

    @Override
    @NonnullByDefault
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        // Unlike in Bukkit, the CommandContext stores pre-parsed arguments
        // These arguments are defined by the CommandSpec registered in the Tutorial class.
        // We defined a Player and a String. We know they are going to be there (they weren't optional)
        // so we just get them
        //
        // You don't need to do the parsing, we've done it!
        Player player = args.<Player>getOne("player").get();
        String message = args.<String>getOne("message").get();

        // Now, send the message to the player. We'll send it in green, and add something from the config
        // for this in yellow
        // We use a Text object for this, as this translates easily to the Minecraft JSON format
        player.sendMessage(Text.of(TextColors.YELLOW, this.plugin.getConfig().getName(), ": ", TextColors.GREEN, message));

        // The command worked!
        return CommandResult.success();
    }
}
