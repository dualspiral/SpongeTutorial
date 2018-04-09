package uk.co.drnaylor.sponge.tutorial;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/*
 * This class simply contains methods that kick tasks off in different ways.
 */
public class Tasks {

    private final Tutorial plugin;

    public Tasks(Tutorial plugin) {
        this.plugin = plugin;
    }

    /*
     * Creatrs a task that:
     *
     * * Runs async
     * * Performs its first run after 1 seconds
     * * Performs the next four runs after timeInSeconds seconds
     * * Has a nice name
     * * Sends a message to everyone
     * * On the fifth run, cancels itself
     *
     * Returns a Task object that can be used to cancel the tasks before hand
     */
    public Task createRecurringTaskThatBroadcastsAMessageEverySoOftenFiveTimes(Text message, int timeInSeconds) {
        return Task.builder()
            .async() // Sending text is (usually) safe to do async, so we'll do that
            .delay(1, TimeUnit.SECONDS) // First message fires after a second
            .interval(timeInSeconds, TimeUnit.SECONDS) // Subsequent messages first after timeInSeconds seconds
            .name("Tutorial - broadcast task") // Just an identifier
            .execute(new RecurringTaskOne(message)) // Non-lambda version, in case you need to use a class (which this does)
            .submit(this.plugin); // Creates and submits the task
    }

    /*
     * Could also be a Runnable, but we want access to its Task object
     */
    private static class RecurringTaskOne implements Consumer<Task> {

        private final Text message;
        private int counter = 0;

        private RecurringTaskOne(Text message) {
            this.message = message;
        }

        @Override
        public void accept(Task task) {
            // Sends the message to everyone
            MessageChannel.TO_ALL.send(this.message);

            // Loop the increments the counter and cancels the task if necessary.
            if (++this.counter >= 5) { // yes, prefix, increment THEN check
                task.cancel(); // cancel task preventing another run
            }
        }
    }

    /*
     * Creates an async task that sends a message after so many seconds
     */
    public Task sendADelayedMessage(Text message, int delayInSeconds) {
        return Task.builder()
                .async() // Sending text is (usually) safe to do async, so we'll do that
                .delay(delayInSeconds, TimeUnit.SECONDS) // First message fires after a second
                .name("Tutorial - delayed broadcast task") // Just an identifier
                .execute(task -> MessageChannel.TO_ALL.send(message)) // Lambda statement only, one thing to do, send a message!
                .submit(this.plugin); // Creates and submits the task
    }

    /*
     * Creates a task that heals a player every minute
     */
    public Task healPlayerEveryMinute(UUID uuid) {
        return Task.builder() // no async call
                .interval(1, TimeUnit.MINUTES)
                .name("Tutorial - heal " + uuid.toString() + "task") // Just an identifier
                .execute(task -> {
                    Optional<Player> player = Sponge.getServer().getPlayer(uuid);
                    if (player.isPresent()) {
                        // Heal them
                        player.get().offer(Keys.HEALTH, player.get().maxHealth().get()); // Heals to max health
                    } else {
                        task.cancel(); // Don't run this again if the player is no longer online.
                    }
                }) // Lambda method
                .submit(this.plugin); // Creates and submits the task
    }
}
