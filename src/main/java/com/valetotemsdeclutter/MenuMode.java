package com.valetotemsdeclutter;

public enum MenuMode
{
	/**
	 * Sink the tree's options below "Walk here" so left click walks, right click still chops.
	 */
	DEPRIORITISE("Walk here first"),

	/**
	 * Strip the chop option out of the menu completely.
	 */
	REMOVE_OPTION("Remove chop option"),

	/**
	 * Leave menus exactly as the game built them.
	 */
	LEAVE_ALONE("Leave alone");

	private final String label;

	MenuMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
