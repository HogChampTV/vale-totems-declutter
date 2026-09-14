package com.valetotemsdeclutter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class TreeTypeTest
{
	@Test
	public void matchesBothNamingStyles()
	{
		assertEquals(TreeType.OAK, TreeType.match("Oak"));
		assertEquals(TreeType.OAK, TreeType.match("Oak tree"));
		assertEquals(TreeType.MAPLE, TreeType.match("Maple tree"));
		assertEquals(TreeType.MAPLE, TreeType.match("Maple"));
		assertEquals(TreeType.MAGIC, TreeType.match("Magic tree"));
		assertEquals(TreeType.REDWOOD, TreeType.match("Redwood"));
	}

	@Test
	public void ignoresCaseAndPadding()
	{
		assertEquals(TreeType.YEW, TreeType.match("  YEW  "));
	}

	@Test
	public void foldsJunkTreesIntoTheGenericBucket()
	{
		assertEquals(TreeType.TREE, TreeType.match("Dead tree"));
		assertEquals(TreeType.TREE, TreeType.match("Tree"));
	}

	@Test
	public void leavesNonTreesAlone()
	{
		assertNull(TreeType.match("Vale totem"));
		assertNull(TreeType.match("Bank chest"));
		assertNull(TreeType.match(""));
		assertNull(TreeType.match(null));
	}
}
