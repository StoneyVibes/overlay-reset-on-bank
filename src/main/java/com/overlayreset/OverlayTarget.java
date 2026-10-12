package com.overlayreset;

/**
 * Movable widget overlays that players commonly Alt-drag for dual-client layouts.
 * Names must match RuneLite's WidgetOverlays.
 */
enum OverlayTarget
{
	// Resizable classic
	CHAT_CLASSIC("RESIZABLE_VIEWPORT_CHATBOX_PARENT", TargetGroup.CHAT),
	INVENTORY_CLASSIC("RESIZABLE_VIEWPORT_INVENTORY_PARENT", TargetGroup.INVENTORY),
	MINIMAP_CLASSIC("RESIZABLE_MINIMAP_STONES_WIDGET", TargetGroup.MINIMAP),

	// Resizable modern
	CHAT_MODERN("RESIZABLE_VIEWPORT_BOTTOM_LINE_CHATBOX_PARENT", TargetGroup.CHAT),
	TABS1_MODERN("RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS1", TargetGroup.INVENTORY),
	TABS2_MODERN("RESIZABLE_VIEWPORT_BOTTOM_LINE_TABS2", TargetGroup.INVENTORY),
	INVENTORY_MODERN("RESIZABLE_VIEWPORT_BOTTOM_LINE_INVENTORY_PARENT", TargetGroup.INVENTORY),
	MINIMAP_MODERN("RESIZABLE_MINIMAP_WIDGET", TargetGroup.MINIMAP);

	private final String overlayName;
	private final TargetGroup group;

	OverlayTarget(String overlayName, TargetGroup group)
	{
		this.overlayName = overlayName;
		this.group = group;
	}

	String getOverlayName()
	{
		return overlayName;
	}

	TargetGroup getGroup()
	{
		return group;
	}

	enum TargetGroup
	{
		INVENTORY,
		CHAT,
		MINIMAP
	}
}
