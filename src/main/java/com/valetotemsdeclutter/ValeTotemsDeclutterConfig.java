package com.valetotemsdeclutter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ValeTotemsDeclutterConfig.GROUP)
public interface ValeTotemsDeclutterConfig extends Config
{
	String GROUP = "valetotemsdeclutter";
	String TARGET_KEY = "targetTree";

	@ConfigSection(
		name = "Hide",
		description = "What to take out of view while you are in the vale",
		position = 10
	)
	String hideSection = "hideSection";

	@ConfigSection(
		name = "Area",
		description = "Where the plugin is allowed to work",
		position = 20
	)
	String areaSection = "areaSection";

	@ConfigSection(
		name = "Name lists",
		description = "What each Hide option matches on. Most players never need to touch these.",
		position = 30,
		closedByDefault = true
	)
	String listSection = "listSection";

	@ConfigSection(
		name = "Debug",
		description = "Only needed when something is not being hidden",
		position = 40,
		closedByDefault = true
	)
	String debugSection = "debugSection";

	// ---------------------------------------------------------------- main

	@ConfigItem(
		keyName = TARGET_KEY,
		name = "Tree in use",
		description = "The tree you are chopping in the vale. Everything else counts as clutter. Pick Not chopping if you brought your own logs - then every tree is clutter, including the redwoods you cannot chop anyway.",
		position = 1
	)
	default TreeType targetTree()
	{
		return TreeType.NONE;
	}

	@ConfigItem(
		keyName = "mode",
		name = "Trees you are not using",
		description = "What happens when you click a tree you are not using. Walk here first still lets you chop it on right click, and is the one most people want.",
		position = 2
	)
	default MenuMode mode()
	{
		return MenuMode.LEAVE_ALONE;
	}

	// ---------------------------------------------------------------- hide

	@ConfigItem(
		keyName = "hideTrees",
		name = "Other trees",
		description = "Every tree you are not using disappears. You cannot click what is not there.",
		position = 11,
		section = hideSection
	)
	default boolean hideTrees()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideScenery",
		name = "Scenery trees",
		description = "Decorative trees you cannot chop, such as the pines and cypresses.",
		position = 12,
		section = hideSection
	)
	default boolean hideScenery()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideClutter",
		name = "Undergrowth",
		description = "Ferns, bushes, plants, rocks and other ground cover.",
		position = 13,
		section = hideSection
	)
	default boolean hideClutter()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideAdmire",
		name = "Admire spots",
		description = "The beautiful log, stump and rock the ents stop to admire. Hiding them costs you a cue for where an ent is headed.",
		position = 14,
		section = hideSection
	)
	default boolean hideAdmire()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideOfferings",
		name = "Spirit offerings",
		description = "The offering piles. Note you use logs on these, so hiding one means you cannot hand logs to it either.",
		position = 15,
		section = hideSection
	)
	default boolean hideOfferings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideFarmingTrees",
		name = "Farming patch",
		description = "The tree in the vale's farming patch. Left alone by default so you can still check and harvest it.",
		position = 16,
		section = hideSection
	)
	default boolean hideFarmingTrees()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideEnts",
		name = "Ents",
		description = "Ents disappear entirely. Their trails still show, so you can still follow one.",
		position = 17,
		section = hideSection
	)
	default boolean hideEnts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideSpirits",
		name = "Spirit animals",
		description = "Spirit animals disappear entirely.",
		position = 18,
		section = hideSection
	)
	default boolean hideSpirits()
	{
		return false;
	}

	// ---------------------------------------------------------------- area

	@ConfigItem(
		keyName = "auburnvaleOnly",
		name = "Auburnvale only",
		description = "Keep the plugin to the vale. Turning this off lets it hide trees and creatures everywhere else in the game.",
		position = 21,
		section = areaSection
	)
	default boolean auburnvaleOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "regionIds",
		name = "Regions",
		description = "The map regions counted as Auburnvale, collected by walking every totem site. Only worth changing if part of the vale is being missed.",
		position = 22,
		section = areaSection
	)
	default String regionIds()
	{
		return "5170,5171,5172,5173,5426,5427,5428,5429,5682,5683,5684,5685,5939,5940,5941";
	}

	// ---------------------------------------------------------------- lists

	@ConfigItem(
		keyName = "sceneryNames",
		name = "Scenery trees",
		description = "Names or object IDs hidden by the Scenery trees option.",
		position = 31,
		section = listSection
	)
	default String sceneryNames()
	{
		return "Pine tree,Cypress Tree,Redwood tree";
	}

	@ConfigItem(
		keyName = "clutterNames",
		name = "Undergrowth",
		description = "Names or object IDs. Use an ID when a name would also catch something you need, like the Logs you take an axe from.",
		position = 32,
		section = listSection
	)
	default String clutterNames()
	{
		return "Plant,Fern,Bush,Berry bush,Bullrushes,Fungi,Mushrooms,Roots,Flowers,Rockslide,Rock,Log pile,Hollow log";
	}

	@ConfigItem(
		keyName = "admireNames",
		name = "Admire spots",
		description = "Names or object IDs hidden by the Admire spots option.",
		position = 33,
		section = listSection
	)
	default String admireNames()
	{
		return "Beautiful log,Beautiful tree stump,Beautiful rock";
	}

	@ConfigItem(
		keyName = "offeringsNames",
		name = "Spirit offerings",
		description = "Names or object IDs hidden by the Spirit offerings option.",
		position = 34,
		section = listSection
	)
	default String offeringsNames()
	{
		return "Spirit offerings";
	}

	@ConfigItem(
		keyName = "entNames",
		name = "Ents",
		description = "Names or NPC IDs hidden by the Ents option.",
		position = 35,
		section = listSection
	)
	default String entNames()
	{
		return "Ent";
	}

	@ConfigItem(
		keyName = "spiritNames",
		name = "Spirit animals",
		description = "Names or NPC IDs hidden by the Spirit animals option.",
		position = 36,
		section = listSection
	)
	default String spiritNames()
	{
		return "Buffalo spirit,Jaguar spirit,Eagle spirit,Snake spirit,Scorpion spirit";
	}

	// ---------------------------------------------------------------- debug

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug mode",
		description = "Writes everything the plugin sees to the client log, so a missed object can be added to the lists above.",
		position = 41,
		section = debugSection
	)
	default boolean debugLogging()
	{
		return false;
	}
}
