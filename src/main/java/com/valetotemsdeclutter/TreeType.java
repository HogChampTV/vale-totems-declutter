package com.valetotemsdeclutter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The trees worth caring about in Auburnvale. Object names are matched against the alias list
 * rather than object IDs, because the tree multilocs change IDs between full/stump states and
 * there are far too many maple IDs in the vale to keep a hardcoded list honest.
 */
public enum TreeType
{
	NONE("Not chopping", new String[0]),
	TREE("Tree", new String[]{"tree"}),
	OAK("Oak", new String[]{"oak", "oak tree"}),
	WILLOW("Willow", new String[]{"willow", "willow tree"}),
	MAPLE("Maple", new String[]{"maple", "maple tree"}),
	YEW("Yew", new String[]{"yew", "yew tree"}),
	MAGIC("Magic", new String[]{"magic tree", "magic"}),
	REDWOOD("Redwood", new String[]{"redwood", "redwood tree"});

	private final String label;
	private final Set<String> aliases;

	TreeType(String label, String[] aliases)
	{
		this.label = label;
		Set<String> set = new HashSet<>(Arrays.asList(aliases));
		this.aliases = Collections.unmodifiableSet(set);
	}

	/**
	 * Dead trees are scattered around the hunter area and are never wanted, so they get folded
	 * into the generic tree bucket instead of getting their own entry in the config dropdown.
	 */
	private static final Set<String> EXTRA_TREE_NAMES = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList("dead tree", "dying tree", "evergreen")));

	public static TreeType match(String objectName)
	{
		if (objectName == null || objectName.isEmpty())
		{
			return null;
		}

		String name = objectName.toLowerCase(Locale.ROOT).trim();

		for (TreeType type : values())
		{
			if (type != NONE && type.aliases.contains(name))
			{
				return type;
			}
		}

		return EXTRA_TREE_NAMES.contains(name) ? TREE : null;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
