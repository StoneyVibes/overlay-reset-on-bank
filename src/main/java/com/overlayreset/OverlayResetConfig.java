package com.overlayreset;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(OverlayResetConfig.GROUP)
public interface OverlayResetConfig extends Config
{
	String GROUP = "overlayresetonbank";

	@ConfigSection(
		name = "Triggers",
		description = "When to temporarily reset overlays to default",
		position = 0
	)
	String triggersSection = "triggersSection";

	@ConfigSection(
		name = "Targets",
		description = "Which interface overlays to reset",
		position = 1
	)
	String targetsSection = "targetsSection";

	@ConfigItem(
		keyName = "resetOnBank",
		name = "While banking",
		description = "While the bank is open, use default overlay positions. When closed, restore your saved positions",
		section = triggersSection,
		position = 0
	)
	default boolean resetOnBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetOnShop",
		name = "While shopping",
		description = "While an NPC shop is open, use default overlay positions. When closed, restore your saved positions",
		section = triggersSection,
		position = 1
	)
	default boolean resetOnShop()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetOnFixedInterfaces",
		name = "On all fixed common interfaces",
		description = "Also reset for other common fixed/modal game interfaces that you cannot Alt-drag "
			+ "(Grand Exchange, deposit box, trade, smithing, collection log, seed vault, world map, rewards, …). "
			+ "Bank and shop have their own toggles above.",
		section = triggersSection,
		position = 2
	)
	default boolean resetOnFixedInterfaces()
	{
		return false;
	}

	@ConfigItem(
		keyName = "manageInventory",
		name = "Inventory / side tabs",
		description = "Reset inventory and side-tab overlays",
		section = targetsSection,
		position = 0
	)
	default boolean manageInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "manageChat",
		name = "Chat",
		description = "Reset the chatbox overlay",
		section = targetsSection,
		position = 1
	)
	default boolean manageChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "manageMinimap",
		name = "Minimap (+ orbs)",
		description = "Reset the minimap overlay block (includes orbs)",
		section = targetsSection,
		position = 2
	)
	default boolean manageMinimap()
	{
		return true;
	}
}
