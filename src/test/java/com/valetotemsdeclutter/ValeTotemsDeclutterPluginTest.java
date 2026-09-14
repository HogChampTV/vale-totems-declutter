package com.valetotemsdeclutter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ValeTotemsDeclutterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ValeTotemsDeclutterPlugin.class);
		RuneLite.main(args);
	}
}
