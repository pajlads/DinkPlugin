package dinkplugin;

import dinkplugin.DinkPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DinkTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(DinkPlugin.class);
        RuneLite.main(args);
    }
}
