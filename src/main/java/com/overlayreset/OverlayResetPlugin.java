package com.overlayreset;

import com.google.inject.Provides;
import java.awt.Dimension;
import java.awt.Point;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Temporarily resets movable interface overlays (tabs/chat/minimap) to defaults
 * while banking or shopping, then restores the positions RuneLite already saved.
 * <p>
 * The pre-reset layout is persisted to this plugin's config so a crash/kill while
 * banking does not permanently wipe Alt-drag positions.
 */
@Slf4j
@PluginDescriptor(
	name = "Overlay Reset on Bank",
	description = "Temporarily reset tabs/chat/minimap to default while banking, shopping, or other fixed common interfaces, then restore your layout",
	tags = {"bank", "shop", "ge", "trade", "overlay", "layout", "tabs", "chat", "minimap", "dual", "multiclient"}
)
public class OverlayResetPlugin extends Plugin
{
	private static final String[] OVERLAY_SUFFIXES = {
		"_preferredLocation",
		"_preferredPosition",
		"_preferredSize",
		"_origin",
		"_originX",
		"_originY"
	};

	private static final String SUSPENDED_KEY = "suspended";
	private static final String BACKUP_NAMES_KEY = "backupNames";
	private static final String BACKUP_PREFIX = "backup_";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayResetConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	/** In-memory backup of runelite overlay config while suspended. */
	private final Map<String, String> backup = new HashMap<>();
	private boolean suspended;
	private final Set<Integer> activeTriggers = new HashSet<>();
	/**
	 * After world-hop, bank/shop widgets can briefly exist without being open.
	 * Ignore suspend until this client tick (exclusive).
	 */
	private int suppressSuspendBeforeTick = -1;

	@Override
	protected void startUp()
	{
		// Recover from crash/kill while bank/shop was open (defaults were persisted).
		if (hasDurableBackup())
		{
			log.info("Found overlay backup from previous session — restoring layout");
			restoreDurableBackupToRuneLiteConfig();
			clearDurableBackup();
			suspended = false;
			backup.clear();
		}

		clientThread.invoke(() ->
		{
			applyRuneLiteConfigToLiveOverlays();
			syncTriggerState();
		});
	}

	@Override
	protected void shutDown()
	{
		// Restore synchronously — clientThread.invoke may not finish during shutdown.
		if (suspended || hasDurableBackup())
		{
			restoreConfigFromBackup();
			clearDurableBackup();
		}

		activeTriggers.clear();
		backup.clear();
		suspended = false;
		suppressSuspendBeforeTick = -1;
	}

	@Provides
	OverlayResetConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OverlayResetConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			// Hop/logout: drop triggers and put custom layout back immediately.
			// Do NOT re-scan interfaces here — hop briefly spawns bank/shop widgets
			// and would otherwise suspend (wiping live positions to default).
			activeTriggers.clear();
			if (suspended || hasDurableBackup())
			{
				restoreOverlays();
			}
			return;
		}

		// Ghost bank WidgetLoaded fires right after hop; ignore suspend for a few ticks.
		suppressSuspendBeforeTick = client.getTickCount() + 4;
		// Re-apply saved custom positions after hop; never auto-suspend from a scan.
		clientThread.invokeLater(this::applyRuneLiteConfigToLiveOverlays);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		int groupId = event.getGroupId();
		if (!shouldHandle(groupId))
		{
			return;
		}

		// WidgetLoaded fires during world-hop UI rebuilds too. Confirm the interface
		// is actually open on the next client pass before suspending.
		clientThread.invokeLater(() -> trySuspendForGroup(groupId));
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		int groupId = event.getGroupId();
		if (!activeTriggers.contains(groupId))
		{
			return;
		}

		// Rebuilds fire Closed→Loaded while still open — re-check before restore.
		clientThread.invokeLater(() -> tryRestoreAfterClose(groupId));
	}

	private void trySuspendForGroup(int groupId)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (client.getTickCount() < suppressSuspendBeforeTick)
		{
			return;
		}
		if (!isGroupOpen(groupId))
		{
			return;
		}

		activeTriggers.add(groupId);
		suspendIfNeeded();
	}

	private void tryRestoreAfterClose(int groupId)
	{
		if (isGroupOpen(groupId))
		{
			// Still open (rebuild) — keep suspended.
			return;
		}

		activeTriggers.remove(groupId);
		if (activeTriggers.isEmpty() && suspended)
		{
			restoreOverlays();
		}
	}

	private void syncTriggerState()
	{
		activeTriggers.clear();

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			if (suspended)
			{
				restoreOverlays();
			}
			return;
		}

		for (int groupId : enabledTriggerGroups())
		{
			if (isGroupOpen(groupId))
			{
				activeTriggers.add(groupId);
			}
		}

		if (!activeTriggers.isEmpty())
		{
			suspendIfNeeded();
		}
		else if (suspended)
		{
			restoreOverlays();
		}
	}

	private Set<Integer> enabledTriggerGroups()
	{
		Set<Integer> groups = new HashSet<>();
		if (config.resetOnBank())
		{
			groups.add(InterfaceID.BANKMAIN);
		}
		if (config.resetOnShop())
		{
			groups.add(InterfaceID.SHOPMAIN);
		}
		if (config.resetOnFixedInterfaces())
		{
			groups.addAll(FixedInterfaceGroups.all());
		}
		return groups;
	}

	private boolean shouldHandle(int groupId)
	{
		if (groupId == InterfaceID.BANKMAIN)
		{
			return config.resetOnBank();
		}
		if (groupId == InterfaceID.SHOPMAIN)
		{
			return config.resetOnShop();
		}
		return config.resetOnFixedInterfaces() && FixedInterfaceGroups.contains(groupId);
	}

	private boolean isGroupOpen(int groupId)
	{
		// Bank: require the bank item container — UNIVERSE alone is true for hop ghosts.
		if (groupId == InterfaceID.BANKMAIN)
		{
			return client.getItemContainer(InventoryID.BANK) != null
				&& isComponentOpen(InterfaceID.Bankmain.ITEMS);
		}
		if (groupId == InterfaceID.SHOPMAIN)
		{
			return isComponentOpen(InterfaceID.Shopmain.ITEMS);
		}

		Widget widget = client.getWidget(groupId, 0);
		return widget != null && !widget.isHidden();
	}

	private boolean isComponentOpen(int componentId)
	{
		Widget widget = client.getWidget(componentId);
		return widget != null && !widget.isHidden();
	}

	private void suspendIfNeeded()
	{
		if (suspended || activeTriggers.isEmpty())
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (client.getTickCount() < suppressSuspendBeforeTick)
		{
			return;
		}

		// Drop stale trigger IDs from hop/rebuild ghosts before resetting anything.
		activeTriggers.removeIf(id -> !isGroupOpen(id));
		if (activeTriggers.isEmpty())
		{
			return;
		}

		List<Overlay> overlays = findManagedOverlays();
		if (overlays.isEmpty())
		{
			return;
		}

		backup.clear();
		List<String> names = new ArrayList<>();
		for (Overlay overlay : overlays)
		{
			// Flush latest Alt-drag state, back it up (memory + disk), then reset.
			overlayManager.saveOverlay(overlay);
			backupOverlayConfig(overlay.getName());
			names.add(overlay.getName());
		}

		persistDurableBackup(names);
		for (Overlay overlay : overlays)
		{
			overlayManager.resetOverlay(overlay);
		}

		suspended = true;
		log.debug("Suspended overlays for bank/shop: {}", activeTriggers);
	}

	private void restoreOverlays()
	{
		if (!suspended && !hasDurableBackup())
		{
			return;
		}

		restoreConfigFromBackup();
		applyRuneLiteConfigToLiveOverlays();
		clearDurableBackup();
		backup.clear();
		suspended = false;
		log.debug("Restored overlays after bank/shop");
	}

	private void applyRuneLiteConfigToLiveOverlays()
	{
		for (Overlay overlay : findManagedOverlays())
		{
			if (loadOverlayFromRuneLiteConfig(overlay))
			{
				overlayManager.saveOverlay(overlay);
				overlay.revalidate();
			}
		}
	}

	private void backupOverlayConfig(String overlayName)
	{
		for (String suffix : OVERLAY_SUFFIXES)
		{
			String key = overlayName + suffix;
			String value = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, key);
			if (value != null)
			{
				backup.put(key, value);
			}
		}
	}

	private void persistDurableBackup(List<String> overlayNames)
	{
		configManager.setConfiguration(OverlayResetConfig.GROUP, SUSPENDED_KEY, "true");
		configManager.setConfiguration(OverlayResetConfig.GROUP, BACKUP_NAMES_KEY, String.join(",", overlayNames));

		for (String key : backup.keySet())
		{
			configManager.setConfiguration(OverlayResetConfig.GROUP, BACKUP_PREFIX + key, backup.get(key));
		}
	}

	private boolean hasDurableBackup()
	{
		return "true".equals(configManager.getConfiguration(OverlayResetConfig.GROUP, SUSPENDED_KEY));
	}

	private void restoreConfigFromBackup()
	{
		if (!backup.isEmpty())
		{
			for (Map.Entry<String, String> entry : backup.entrySet())
			{
				configManager.setConfiguration(RuneLiteConfig.GROUP_NAME, entry.getKey(), entry.getValue());
			}

			// Unset keys that existed as "absent" in the original layout for backed-up overlays.
			String names = backup.keySet().stream()
				.map(k ->
				{
					for (String suffix : OVERLAY_SUFFIXES)
					{
						if (k.endsWith(suffix))
						{
							return k.substring(0, k.length() - suffix.length());
						}
					}
					return null;
				})
				.filter(n -> n != null)
				.distinct()
				.collect(Collectors.joining(","));

			unsetMissingKeysForNames(names.split(","));
			return;
		}

		restoreDurableBackupToRuneLiteConfig();
	}

	private void restoreDurableBackupToRuneLiteConfig()
	{
		String namesCsv = configManager.getConfiguration(OverlayResetConfig.GROUP, BACKUP_NAMES_KEY);
		if (namesCsv == null || namesCsv.isEmpty())
		{
			return;
		}

		String[] names = namesCsv.split(",");
		for (String overlayName : names)
		{
			for (String suffix : OVERLAY_SUFFIXES)
			{
				String key = overlayName + suffix;
				String value = configManager.getConfiguration(OverlayResetConfig.GROUP, BACKUP_PREFIX + key);
				if (value != null)
				{
					configManager.setConfiguration(RuneLiteConfig.GROUP_NAME, key, value);
					backup.put(key, value);
				}
				else
				{
					configManager.unsetConfiguration(RuneLiteConfig.GROUP_NAME, key);
				}
			}
		}
	}

	private void unsetMissingKeysForNames(String[] names)
	{
		for (String overlayName : names)
		{
			if (overlayName == null || overlayName.isEmpty())
			{
				continue;
			}
			for (String suffix : OVERLAY_SUFFIXES)
			{
				String key = overlayName + suffix;
				if (!backup.containsKey(key))
				{
					configManager.unsetConfiguration(RuneLiteConfig.GROUP_NAME, key);
				}
			}
		}
	}

	private void clearDurableBackup()
	{
		String namesCsv = configManager.getConfiguration(OverlayResetConfig.GROUP, BACKUP_NAMES_KEY);
		if (namesCsv != null)
		{
			for (String overlayName : namesCsv.split(","))
			{
				for (String suffix : OVERLAY_SUFFIXES)
				{
					configManager.unsetConfiguration(OverlayResetConfig.GROUP, BACKUP_PREFIX + overlayName + suffix);
				}
			}
		}

		// Also clear any keys still present from the in-memory map.
		for (String key : new ArrayList<>(backup.keySet()))
		{
			configManager.unsetConfiguration(OverlayResetConfig.GROUP, BACKUP_PREFIX + key);
		}

		configManager.unsetConfiguration(OverlayResetConfig.GROUP, BACKUP_NAMES_KEY);
		configManager.unsetConfiguration(OverlayResetConfig.GROUP, SUSPENDED_KEY);
	}

	private boolean loadOverlayFromRuneLiteConfig(Overlay overlay)
	{
		String name = overlay.getName();
		Point location = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, name + "_preferredLocation", Point.class);
		OverlayPosition position = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, name + "_preferredPosition", OverlayPosition.class);
		Dimension size = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, name + "_preferredSize", Dimension.class);

		if (location == null && position == null)
		{
			return false;
		}

		overlay.setPreferredLocation(location);
		overlay.setPreferredPosition(position);
		overlay.setPreferredSize(size);

		Object origin = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, name + "_origin", findOverlayEnum("OverlayOrigin"));
		Object originX = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, name + "_originX", findOverlayEnum("OverlayOriginX"));
		Object originY = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, name + "_originY", findOverlayEnum("OverlayOriginY"));

		if (origin != null)
		{
			invokeOverlaySetter(overlay, "setOrigin", origin.getClass(), origin);
		}
		if (originX != null)
		{
			invokeOverlaySetter(overlay, "setOriginX", originX.getClass(), originX);
		}
		if (originY != null)
		{
			invokeOverlaySetter(overlay, "setOriginY", originY.getClass(), originY);
		}

		return true;
	}

	private static Class<?> findOverlayEnum(String simpleName)
	{
		try
		{
			return Class.forName("net.runelite.client.ui.overlay." + simpleName);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void invokeOverlaySetter(Overlay overlay, String methodName, Class<?> argType, Object value)
	{
		try
		{
			Method method = Overlay.class.getDeclaredMethod(methodName, argType);
			method.setAccessible(true);
			method.invoke(overlay, value);
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException("Failed to call " + methodName + " on overlay", e);
		}
	}

	private List<Overlay> findManagedOverlays()
	{
		Set<OverlayTarget.TargetGroup> enabledGroups = EnumSet.noneOf(OverlayTarget.TargetGroup.class);
		if (config.manageInventory())
		{
			enabledGroups.add(OverlayTarget.TargetGroup.INVENTORY);
		}
		if (config.manageChat())
		{
			enabledGroups.add(OverlayTarget.TargetGroup.CHAT);
		}
		if (config.manageMinimap())
		{
			enabledGroups.add(OverlayTarget.TargetGroup.MINIMAP);
		}

		Map<String, OverlayTarget.TargetGroup> wanted = new HashMap<>();
		for (OverlayTarget target : OverlayTarget.values())
		{
			if (enabledGroups.contains(target.getGroup()))
			{
				wanted.put(target.getOverlayName(), target.getGroup());
			}
		}

		List<Overlay> found = new ArrayList<>();
		overlayManager.anyMatch(overlay ->
		{
			if (wanted.containsKey(overlay.getName()))
			{
				found.add(overlay);
			}
			return false;
		});
		return found;
	}
}
