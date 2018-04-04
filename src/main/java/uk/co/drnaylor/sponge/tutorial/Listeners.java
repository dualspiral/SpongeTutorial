package uk.co.drnaylor.sponge.tutorial;

/*
 * Listener objects in Sponge do not need a special interface. Just listen to events
 * using the @Listener annotation.
 */

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// See https://docs.spongepowered.org/stable/en/plugin/event/index.html for more
// info about listeners
public class Listeners {

    private final Tutorial plugin;

    public Listeners(Tutorial plugin) {
        this.plugin = plugin;
    }

    /*
     * There are three client connection events. Auth runs async,
     * Login and Join run sync. Join is being used here because we're
     * running a task in it.
     *
     * Order is set to POST because we don't want to set off the task
     * if it is cancelled beforehand
     */
    @Listener(order = Order.POST)
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();

        // The method signature here could have been written as
        //
        // public void onPlayerJoin(ClientConnectionEvent.Join event, @Getter("getTargetEntity") Player player) {
        //
        // Then, the first line (above) would not have been needed

        // The player may have logged out again by the time the task we're about
        // to create fires. Don't store Entity or World references, store UUIDs.
        final UUID uuid = player.getUniqueId();

        // This is a scheduled task. We'll run this sync, but you can add
        // .async() to the chain for an async chain.
        //
        // DO NOT USE GAME OBJECTS OFF THE MAIN THREAD.
        Task.builder()
                // Runs in the time in seconds specified in the config. Catches negative values and treats them as zeros
                .delay(Math.max(this.plugin.getConfig().getCountdown(), 0), TimeUnit.SECONDS)
                .name("Tutorial Plugin Join task") // Friendly name
                .execute(task -> { // This lambda takes a Task object and returns nothing (Callable<Task>)
                    Optional<Player> taskPlayer = Sponge.getServer().getPlayer(uuid);
                    if (taskPlayer.isPresent()) { // can also use ifPresent(p -> ...);
                        // Send them a message
                        taskPlayer.get().sendMessage(Text.of(TextColors.BLUE, TextStyles.ITALIC, "Hello!"));
                    }
                }).submit(this.plugin);
    }

    /*
     * Every event has a cause, or series of causes, that caused the event to fire.
     * Sponge has a cause tracking system that tracks all of the causes leading up to an
     * event which can be queried by these events. In most cases, you only care about an
     * event when there is a certain cause.
     *
     * See https://docs.spongepowered.org/stable/en/plugin/event/causes.html about what
     * causes are in more detail.
     *
     * This event uses an event filter. In this case, we want all chat events
     * that were directly caused by a player. This means if that a plugin causes a chat
     * event (and not on behalf of a player), this will not fire.
     *
     * This is the same as getting the Player from the root like so:
     *
     *  Object root = event.getCause().root();
     *  if (!(root instanceof Player)) {
     *      return;
     *  }
     *
     *  Player player = (Player) root;
     *
     * Root means the last item in the cause, or the most direct cause. There are other
     * filters, see https://docs.spongepowered.org/stable/en/plugin/event/filters.html
     */
    @Listener
    public void onPlayerChat(MessageChannelEvent.Chat event, @Root Player player) {
        // We might want to block the word "Hello". We can do that by getting the message
        // and then blocking the word
        Text message = event.getMessage(); // could use "@Getter("getMessage") Text message" in the event sig
        if (message.toPlain().toLowerCase().contains("hello")) {
            // block it - you might want a permission check though!
            event.setCancelled(true);

            // send a message to the player telling them it's a banned word
            player.sendMessage(Text.of(TextColors.RED, "Hello is a banned word on this server"));
        }
    }

}
