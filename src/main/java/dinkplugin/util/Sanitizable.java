package dinkplugin.util;

import java.util.Map;

/**
 * Marks that an object is sanitizable for usage within {@link net.runelite.client.events.PluginMessage} data.
 */
public interface Sanitizable {

    /**
     * @return {@link Map} representation of this object only using types that are available on RuneLite's common classloader
     * @implNote This approach is ugly but is meant to avoid reflection
     */
    Map<String, Object> sanitized();

}
