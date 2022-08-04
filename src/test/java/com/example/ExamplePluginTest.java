package com.example;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import universalDiscord.UniversalDiscordPlugin;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UniversalDiscordPlugin.class);
		RuneLite.main(args);
	}
}