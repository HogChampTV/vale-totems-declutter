package com.valetotemsdeclutter;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Vale Totems Declutter",
	description = "Pick the tree you are chopping for Vale Totems and hide the rest of the vale out of your way",
	tags = {"vale", "totems", "auburnvale", "varlamore", "fletching", "woodcutting", "declutter", "hide", "entity", "tree"}
)
public class ValeTotemsDeclutterPlugin extends Plugin
{
	private static final Set<MenuAction> OBJECT_ACTIONS = EnumSet.of(
		MenuAction.GAME_OBJECT_FIRST_OPTION,
		MenuAction.GAME_OBJECT_SECOND_OPTION,
		MenuAction.GAME_OBJECT_THIRD_OPTION,
		MenuAction.GAME_OBJECT_FOURTH_OPTION,
		MenuAction.GAME_OBJECT_FIFTH_OPTION);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Hooks hooks;

	@Inject
	private ValeTotemsDeclutterConfig config;

	// Only used to keep the debug log from repeating itself every scene load.
	private final Set<String> loggedNames = new HashSet<>();

	// The map regions that make up Auburnvale, collected by walking every totem site. Hardcoded
	// rather than taken from config because the plugin hub does not allow user-supplied IDs.
	private static final Set<Integer> VALE_REGIONS = new HashSet<>(Arrays.asList(
		5170, 5171, 5172, 5173, 5426, 5427, 5428, 5429, 5682, 5683, 5684, 5685, 5939, 5940, 5941));

	// Forestry event objects that are choppable but are a bonus, not clutter - the Rising Roots
	// event sprouts Anima-infused roots right in the woodcutting area. They carry a Chop action so
	// isTree treats them as a tree, which would otherwise hide or deprioritise them along with the
	// species you are not using. Always spared, whatever tree is selected.
	private static final Set<String> ALWAYS_KEEP_NAMES = new HashSet<>(Arrays.asList(
		"anima-infused roots"));

	private final NameList scenery = new NameList();
	private final NameList clutter = new NameList();
	private final NameList admire = new NameList();
	private final NameList offerings = new NameList();
	private final NameList ents = new NameList();
	private final NameList spirits = new NameList();

	// Hiding an NPC is nothing like removing an object - they are not in the scene graph, so the
	// only way is to decline to draw them, which is what core Entity Hider does.
	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;

	@Provides
	ValeTotemsDeclutterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ValeTotemsDeclutterConfig.class);
	}

	@Override
	protected void startUp()
	{
		parseNames();
		hooks.registerRenderableDrawListener(drawListener);
		clientThread.invoke(this::sweepScene);
	}

	@Override
	protected void shutDown()
	{
		loggedNames.clear();
		hooks.unregisterRenderableDrawListener(drawListener);
		reloadScene();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			logRegions();
			sweepScene();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ValeTotemsDeclutterConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		loggedNames.clear();
		parseNames();

		// Trees already pulled out of the scene only come back with a reload, so take the hit
		// on every config change and let the sweep put the new selection back.
		reloadScene();
	}

	/**
	 * Catches trees that appear after the scene is already up.
	 *
	 * Deliberately does nothing while the state is LOADING. Scene loads fire a spawn event for
	 * every object, and pulling them out mid-load races the map loader thread uploading the scene,
	 * which leaves torn geometry behind. During a load the sweep on LOGGED_IN does the work.
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!hidingObjects() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		GameObject object = event.getGameObject();

		if (shouldCleanUp(object.getId(), object.getWorldLocation()))
		{
			client.getScene().removeGameObject(object);
		}
	}

	// Run late so anything else that wants to rewrite the entry has already had its turn.
	@Subscribe(priority = -1)
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (config.debugLogging())
		{
			logMenuEntry(event.getMenuEntry());
		}

		if (config.mode() == MenuMode.LEAVE_ALONE)
		{
			return;
		}

		MenuEntry entry = event.getMenuEntry();

		if (!OBJECT_ACTIONS.contains(entry.getType()))
		{
			return;
		}

		if (config.auburnvaleOnly() && !playerInTheVale())
		{
			return;
		}

		ObjectComposition composition = definition(entry.getIdentifier());
		if (!isTree(composition))
		{
			return;
		}

		// Forestry event trees are a bonus you want to click, never clutter.
		if (alwaysKeep(composition.getName()))
		{
			return;
		}

		// TreeType.NONE is "not chopping anything", so no tree is spared.
		if (TreeType.match(composition.getName()) == config.targetTree())
		{
			return;
		}

		if (config.mode() == MenuMode.REMOVE_OPTION)
		{
			removeLastEntry();
		}
		else
		{
			entry.setDeprioritized(true);
		}
	}

	private void sweepScene()
	{
		if (!hidingObjects() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Scene scene = client.getScene();

		// The extended scene, not getTiles(). With the GPU plugin's expanded map loading the
		// client holds far more than the standard 104x104, and everything past that edge is still
		// drawn - which is why distant trees survived a sweep that cleared the ones next to you.
		Tile[][][] tiles = scene.getExtendedTiles();
		Set<Long> removed = new HashSet<>();
		int hidden = 0;

		// Every plane, not just the one you are standing on. The vale has raised ground and
		// bridges, and an object a plane above or below is still very much in your line of sight.
		for (int plane = 0; plane < tiles.length; plane++)
		{
			for (int x = 0; x < tiles[plane].length; x++)
			{
				for (int y = 0; y < tiles[plane][x].length; y++)
				{
					Tile tile = tiles[plane][x][y];
					if (tile == null)
					{
						continue;
					}

					for (GameObject object : tile.getGameObjects())
					{
						if (object == null)
						{
							continue;
						}

						if (config.debugLogging())
						{
							logObject(object, plane);
						}

						// A tree wider than one tile appears on every tile it covers. Removing the
						// same object more than once is what tears the scene geometry up.
						if (removed.contains(object.getHash()))
						{
							continue;
						}

						if (shouldCleanUp(object.getId(), object.getWorldLocation()))
						{
							scene.removeGameObject(object);
							removed.add(object.getHash());
							hidden++;
						}
					}

					// Flat decals - moss, ground vegetation - sit on their own layer and are not
					// GameObjects, so removeGameObject never sees them. Clearing the reference is
					// the only handle the API gives us.
					GroundObject ground = tile.getGroundObject();
					if (ground != null)
					{
						if (config.debugLogging())
						{
							logTileObject("ground", ground, plane);
						}

						if (shouldCleanUp(ground.getId(), ground.getWorldLocation()))
						{
							tile.setGroundObject(null);
							hidden++;
						}
					}

					if (config.debugLogging())
					{
						// Cannot be removed through the API, but worth knowing they are there if
						// something visible refuses to disappear.
						if (tile.getDecorativeObject() != null)
						{
							logTileObject("decorative", tile.getDecorativeObject(), plane);
						}

						if (tile.getWallObject() != null)
						{
							logTileObject("wall", tile.getWallObject(), plane);
						}
					}
				}
			}
		}

		if (config.debugLogging())
		{
			log.info("Vale Totems Declutter: swept scene, removed {} objects (target {})",
				hidden, config.targetTree());
		}
	}

	/**
	 * Records what an object can actually do, which the cache will not tell you.
	 *
	 * Some objects carry no actions at all and still matter - Spirit offerings is the pile you use
	 * logs on, and from its definition it is indistinguishable from scenery. The only way to know
	 * is to watch the menu the game builds: select an item, hover the object, and a use-on entry
	 * appears here. Anything logged as use-target should stay out of the hide lists.
	 */
	private void logMenuEntry(MenuEntry entry)
	{
		MenuAction type = entry.getType();
		boolean useOn = type == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;

		if (!useOn && !OBJECT_ACTIONS.contains(type))
		{
			return;
		}

		String target = Text.removeTags(entry.getTarget());
		String option = Text.removeTags(entry.getOption());

		if (loggedNames.add("menu:" + option + ":" + target + "#" + entry.getIdentifier()))
		{
			log.info("Vale Totems Declutter: {} '{}' on '{}' id={}",
				useOn ? "USE-TARGET" : "menu option", option, target, entry.getIdentifier());
		}
	}

	private void logTileObject(String layer, TileObject object, int plane)
	{
		ObjectComposition composition = definition(object.getId());
		if (composition == null || composition.getName() == null)
		{
			return;
		}

		String name = Text.removeTags(composition.getName());
		if (name.isEmpty() || "null".equals(name))
		{
			return;
		}

		if (loggedNames.add(layer + ":" + name + "#" + object.getId()))
		{
			log.info("Vale Totems Declutter: {} object '{}' id={} plane={} actions={}",
				layer, name, object.getId(), plane, Arrays.toString(composition.getActions()));
		}
	}

	/**
	 * Dumps every distinct object in the scene once. This is how you find out what the vale calls
	 * its scenery, which is not something you can reliably guess.
	 */
	private void logObject(GameObject object, int plane)
	{
		ObjectComposition composition = definition(object.getId());
		if (composition == null)
		{
			return;
		}

		String name = Text.removeTags(composition.getName());
		if (name.isEmpty() || "null".equals(name))
		{
			return;
		}

		// Keyed on name AND id - the vale has decorative copies of trees that share a name with
		// the real thing but carry no actions, and a name-only key hides them from this log.
		if (loggedNames.add(name + "#" + object.getId() + "#" + plane))
		{
			log.info("Vale Totems Declutter: object '{}' id={} plane={} actions={} choppable={} farming={}",
				name, object.getId(), plane, Arrays.toString(composition.getActions()),
				isTree(composition), isFarmingPatch(composition));
		}
	}

	/**
	 * True when this object is a tree we are not using. Species are identified by the presence of
	 * a chop action rather than by a list of names, because the vale has conifers and scenery trees
	 * whose object names are anyone's guess. The selected tree is matched by name and spared.
	 */
	private boolean shouldCleanUp(int objectId, WorldPoint location)
	{
		TreeType target = config.targetTree();

		if (config.auburnvaleOnly() && !inTheVale(location))
		{
			return false;
		}

		ObjectComposition composition = definition(objectId);
		if (composition == null)
		{
			return false;
		}

		String name = Text.removeTags(composition.getName());

		// Forestry event trees (Anima-infused roots) are choppable but must never be swept up.
		if (alwaysKeep(name))
		{
			return false;
		}

		// The farming patch is opt-in. Its tree is choppable like any other, so without this it
		// would get swept up with the rest and you would lose sight of your own crop.
		if (isFarmingPatch(composition))
		{
			return config.hideFarmingTrees() && TreeType.match(name) != target;
		}

		// Decoration has no actions at all, so it can only be matched by name.
		if (!isTree(composition))
		{
			String lower = name.toLowerCase(Locale.ROOT);
			boolean wanted = (config.hideScenery() && scenery.matches(lower))
				|| (config.hideClutter() && clutter.matches(lower))
				|| (config.hideAdmire() && admire.matches(lower))
				|| (config.hideOfferings() && offerings.matches(lower));

			return wanted && TreeType.match(name) != target;
		}

		if (config.debugLogging() && loggedNames.add(name + "#" + objectId))
		{
			log.info("Vale Totems Declutter: choppable object '{}' (id {}) -> {}",
				name, objectId, TreeType.match(name) == target ? "kept" : "cleaned up");
		}

		return config.hideTrees() && TreeType.match(name) != target;
	}

	private boolean hidingObjects()
	{
		return config.hideTrees() || config.hideScenery() || config.hideClutter()
			|| config.hideAdmire() || config.hideOfferings() || config.hideFarmingTrees();
	}

	/**
	 * Farming patches carry actions no wild tree has. Check-health is the giveaway on a young
	 * tree, Clear on a grown one.
	 */
	private boolean isFarmingPatch(ObjectComposition composition)
	{
		return hasAnyAction(composition, "check-health", "clear", "guide", "rake", "prune", "cure-plant");
	}

	/**
	 * True for objects that stay clickable no matter what tree is selected - Forestry event trees
	 * that spawn in the middle of the woodcutting area and are always worth interacting with.
	 */
	private static boolean alwaysKeep(String name)
	{
		return name != null && ALWAYS_KEEP_NAMES.contains(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * Anything you can put an axe into. Covers species we have never heard of, which is the point.
	 */
	private boolean isTree(ObjectComposition composition)
	{
		if (composition == null)
		{
			return false;
		}

		String[] actions = composition.getActions();
		if (actions == null)
		{
			return false;
		}

		for (String action : actions)
		{
			if (action == null)
			{
				continue;
			}

			String lower = action.toLowerCase(Locale.ROOT);
			if (lower.startsWith("chop") || lower.startsWith("cut"))
			{
				return true;
			}
		}

		return false;
	}

	private boolean hasAnyAction(ObjectComposition composition, String... wanted)
	{
		if (composition == null || composition.getActions() == null)
		{
			return false;
		}

		for (String action : composition.getActions())
		{
			if (action == null)
			{
				continue;
			}

			String lower = action.toLowerCase(Locale.ROOT);
			for (String candidate : wanted)
			{
				if (lower.equals(candidate))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Trees are multilocs, so the definition we get back is usually the placeholder and the real
	 * name and actions live on the impostor.
	 */
	private ObjectComposition definition(int objectId)
	{
		ObjectComposition composition = client.getObjectDefinition(objectId);

		if (composition != null && composition.getImpostorIds() != null)
		{
			ObjectComposition impostor = composition.getImpostor();
			if (impostor != null)
			{
				return impostor;
			}
		}

		return composition;
	}

	/**
	 * Forces the scene to reload so anything we pulled out of it comes back.
	 */
	private void reloadScene()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}

	private boolean playerInTheVale()
	{
		Player local = client.getLocalPlayer();

		return local != null && inTheVale(local.getWorldLocation());
	}

	private boolean inTheVale(WorldPoint location)
	{
		return location != null && VALE_REGIONS.contains(location.getRegionID());
	}

	/**
	 * Called for everything the client is about to draw, so it stays cheap and bails early.
	 * Returning false skips drawing that entity for this frame.
	 */
	private boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (!(renderable instanceof NPC) || (!config.hideEnts() && !config.hideSpirits()))
		{
			return true;
		}

		if (config.auburnvaleOnly() && !playerInTheVale())
		{
			return true;
		}

		NPC npc = (NPC) renderable;
		String name = npc.getName();
		if (name == null)
		{
			return true;
		}

		String lower = Text.removeTags(name).toLowerCase(Locale.ROOT);

		if (config.hideEnts() && ents.matches(lower))
		{
			return false;
		}

		return !(config.hideSpirits() && spirits.matches(lower));
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!config.debugLogging())
		{
			return;
		}

		NPC npc = event.getNpc();
		String name = npc.getName();

		if (name != null && loggedNames.add("npc:" + name + "#" + npc.getId()))
		{
			log.info("Vale Totems Declutter: npc '{}' id={}", Text.removeTags(name), npc.getId());
		}
	}

	private void parseNames()
	{
		scenery.parse(config.sceneryNames());
		clutter.parse(config.clutterNames());
		admire.parse(config.admireNames());
		offerings.parse(config.offeringsNames());
		ents.parse(config.entNames());
		spirits.parse(config.spiritNames());
	}

	/**
	 * A list of object or NPC names to hide, matched case-insensitively.
	 */
	private static final class NameList
	{
		private final Set<String> names = new HashSet<>();

		void parse(String csv)
		{
			names.clear();

			for (String part : csv.split(","))
			{
				String trimmed = part.trim();
				if (!trimmed.isEmpty())
				{
					names.add(trimmed.toLowerCase(Locale.ROOT));
				}
			}
		}

		boolean matches(String lowerName)
		{
			return names.contains(lowerName);
		}
	}

	/**
	 * Prints the regions currently loaded, so area matching can be checked when debugging.
	 */
	private void logRegions()
	{
		if (!config.debugLogging())
		{
			return;
		}

		log.info("Vale Totems Declutter: loaded regions {}", Arrays.toString(client.getMapRegions()));
	}

	private void removeLastEntry()
	{
		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();

		if (entries.length > 0)
		{
			menu.setMenuEntries(Arrays.copyOf(entries, entries.length - 1));
		}
	}
}
