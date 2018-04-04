package uk.co.drnaylor.sponge.tutorial;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

/*
 *  Configurate, the system used by Sponge has a feature called object mapping. This allows you to use
 *  an arbitary object as a template for your config file.
 *
 *  Objects that can be converted into config items are the primitive types, collections, enums and some
 *  other objects, including game objects and other objects annotated with @ConfigSerialzable. See
 *
 *  From the Docs: https://docs.spongepowered.org/stable/en/plugin/configuration/nodes.html
 *
 *  * Any List or Map of serializable types
 *  * The types java.util.UUID, java.net.URL, java.net.URI and java.util.regex.Pattern
 *  * Any type that has been made serializable as described on the config serialization page
 *
 *  If you need to create your own type serializer, see
 *  https://docs.spongepowered.org/stable/en/plugin/configuration/serialization.html
 *
 *  This will create the following config file:
 *
 *  ------
 *
 *  plugin-name=default-value
 *  # The number of seconds to countdown before sending a message on command.
 *  countdown=3
 *  sub-config {
 *      sub-name=sub-value
 *  }
 *
 *  ------
 */
@ConfigSerializable
public class SampleConfig {

    /*
     * The @Setting annotation defines a config field. Both value and comment are optional.
     * value indicates the name of the field. If omitted, uses the name of the field
     * command indicates the comment that is placed above the field in the config file
     */
    @Setting(value = "plugin-name")
    private String name = "default-value";

    @Setting(value = "countdown", comment = "The number of seconds to countdown before sending a message on command.")
    private int countdown = 3;

    /*
     * As SubConfig is a ConfigSerializable, this creates a config key "sub-config", and
     * it's items are stored within it.
     */
    @Setting(value = "sub-config")
    private SubConfig subConfig = new SubConfig();

    public String getName() {
        return this.name;
    }

    public String getSubName() {
        return this.subConfig.name;
    }

    public int getCountdown() {
        return this.countdown;
    }

    @ConfigSerializable
    public static class SubConfig {

        @Setting(value = "sub-name")
        private String name = "sub-value";

    }
}
