# Vale Totems Declutter

A RuneLite plugin for Vale Totems.

Pick the tree you are cutting. Every other tree in the vale stops being a left-click target, so
running a totem route no longer means chopping a maple you did not want because it happened to be
in front of the willow you did.

## Screenshots

Before — the vale as the game draws it, every tree and scenery pile a click target:

![Before: the cluttered vale](images/before.jpg)

After — hide toggles on, with only the tree you are using left standing:

![After: clutter hidden, target tree kept](images/after.jpg)

The Hide options in the plugin panel, each toggle independent:

![Settings: the Hide section](images/settings.png)

## What it does

Pick the tree you are using. Then two independent things happen to everything else.

**Menus** — trees you are not using get their chop option pushed below "Walk here", removed from the
menu entirely, or left alone.

**Hiding** — separate toggles take things out of view altogether: other trees, scenery trees you
cannot chop, undergrowth, the farming patch, ents, and spirit animals. Each has its own name list
under Name lists, so anything the defaults miss can be added without a code change.

Trees outside the vale are left alone. The area check is a list of map region IDs, not a global
woodcutting override. Debug mode logs the regions you have loaded so the list can be corrected by
walking the place.

## Building

```
./gradlew build
```

## Running a dev client

```
./gradlew run
```

That launches RuneLite with the plugin side-loaded, in developer mode. Dev Tools is the quickest way
to confirm object names and world coordinates if something in the vale is not being matched.

## Notes

Tree matching is done on object name, not object ID. The vale has a lot of maples and the trees are
multilocs that swap IDs as they deplete, so an ID list would rot. Names are matched against a small
alias list per type — see `TreeType`.

Hide mode sweeps `Scene#getExtendedTiles` on every plane, removing game objects with
`Scene#removeGameObject` and clearing ground decals with `Tile#setGroundObject`. It runs on scene
load and is topped up from `GameObjectSpawned` once the scene is up - never during a load, which
races the map loader and tears the geometry. Objects pulled out only come back with a reload, so any
config change forces one via `GameState.LOADING`, and that means a short black screen.

NPCs are not in the scene graph and cannot be removed. Ents and spirits are hidden by declining to
draw them through a `Hooks.RenderableDrawListener`, the same mechanism core Entity Hider uses. A
renderable that is never drawn has no click box, so hidden creatures cannot be clicked either.

Every name list accepts object or NPC IDs as well as names. The vale reuses names across objects
that do very different jobs - the `Logs` you take an axe from share a name with two scenery piles -
so an ID is the way to hide one without the other.

`Spirit offerings` has no actions but is the object you use logs on. It is not scenery, and nothing
should be added to a list that would hide it.
