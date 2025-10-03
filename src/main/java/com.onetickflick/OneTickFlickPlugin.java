package com.onetickflick;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@PluginDescriptor(
		name = "1 Tick Flick",
		description = "Visual tick bar and combo counter for 1-tick prayer flicking with the quick prayer orb",
		tags = {"1", "one", "tick", "flick", "prayer", "metronome", "visual", "quick", "orb"}
)
public class OneTickFlickPlugin extends Plugin
{
	private static final int TICK_LENGTH = 600;

	@Inject
	private OverlayManager overlayManager;
	@Inject
	private OneTickFlickOverlay overlay;
	@Inject
	private OneTickFlickConfig config;
	@Inject
	private Client client;

	private long lastTickTime;
	private long lastInteraction;
	private final List<Integer> currentTickClicks = new CopyOnWriteArrayList<>(); // A list of times the quick prayer orb was clicked, in milliseconds since the last onGameTick.
	private final List<Integer> nextTickClicks = new CopyOnWriteArrayList<>(); // Only used if the click delay config option causes the click to fall into the next tick.

	@Getter(AccessLevel.PACKAGE)
	private int combo;

	@Override
	protected void startUp()
	{
		lastTickTime = System.currentTimeMillis();
		lastInteraction = lastTickTime;
		combo = 0;

		overlay.setVisible(true);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		currentTickClicks.clear();
		nextTickClicks.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("onetickflick"))
		{
			return;
		}

		switch (event.getKey())
		{
			case "greenStart":
				overlay.setGreenStart(config.greenStart());
				break;
			case "greenEnd":
				overlay.setGreenEnd(config.greenEnd());
				break;
			case "showCombo":
				overlay.setShowCombo(config.showCombo());
				break;
			case "targetZoneColor":
				overlay.setTargetZoneColor(config.targetZoneColor());
				break;
			case "backgroundColor":
				overlay.setBackgroundColor(config.backgroundColor());
				break;
			case "clickColor":
				overlay.setClickColor(config.clickColor());
				break;
			case "swipeLineColor":
				overlay.setSwipeLineColor(config.swipeLineColor());
				break;
			case "borderColor":
				overlay.setBorderColor(config.borderColor());
				break;
			case "comboTextColor":
				overlay.setComboTextColor(config.comboTextColor());
				break;
			case "swipeLineWidth":
				overlay.setSwipeLineWidth(config.swipeLineWidth());
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (currentTickClicks.size() == 2 && currentTickClicks.stream().allMatch(this::inGreenZone))
		{
			combo++;
		}
		else
		{
			combo = 0;
		}

		currentTickClicks.clear();
		overlay.newTick();
		lastTickTime = System.currentTimeMillis();

		if (!nextTickClicks.isEmpty())
		{
			for (Integer offset : nextTickClicks)
			{
				currentTickClicks.add(offset);
				overlay.recordClick(offset);
			}
			nextTickClicks.clear();
		}

		if (config.enableTimeout() && overlay.isVisible())
		{
			long elapsed = System.currentTimeMillis() - lastInteraction;
			boolean timedOut = elapsed > config.overlayTimeoutSeconds() * 1000L;
			boolean prayerActive = client.getServerVarbitValue(VarbitID.PRAYER_ALLACTIVE) != 0;

			if (timedOut && (config.timeoutWhilePrayerActive() || !prayerActive)) {
				overlay.setVisible(false);
			}
		}
		else if (!config.enableTimeout() && !overlay.isVisible())
		{
			overlay.setVisible(true);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		Widget widget = e.getWidget();

		if (widget == null
				|| (widget.getId() != InterfaceID.Orbs.PRAYERBUTTON
				&& widget.getId() != InterfaceID.OrbsNomap.PRAYERBUTTON
				&& !(config.detectPrayerBookClicks() && isPrayerBookWidget(widget.getId()))))
		{
			return;
		}

		if (e.getMenuAction() != MenuAction.CC_OP && e.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
		{
			return;
		}

		int offset = (int) (System.currentTimeMillis() - lastTickTime);
		offset += config.clickDelayMilliseconds();

		if (offset >= TICK_LENGTH)
		{
			// If the click is past the current tick, we add it for the next tick instead. It will be added to the overlay on the next onGameTick.
			nextTickClicks.add(offset - TICK_LENGTH);
		}
		else
		{
			currentTickClicks.add(offset);
			overlay.recordClick(offset);
		}

		lastInteraction = System.currentTimeMillis();
		if (!overlay.isVisible())
		{
			// No matter what the overlay timeout options are, we want to show the overlay when the player clicks the quick prayer orb
			overlay.setVisible(true);
		}
	}

	/**
	 * Returns the number of milliseconds since the last tick.
	 */
	long millisSinceTick()
	{
		return System.currentTimeMillis() - lastTickTime;
	}

	/**
	 * Checks if the given milliseconds are within the green zone defined by the config.
	 *
	 * @param ms
	 * @return
	 */
	private boolean inGreenZone(int ms)
	{
		return ms >= config.greenStart() && ms <= config.greenEnd();
	}

	@Provides
	OneTickFlickConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(OneTickFlickConfig.class);
	}

	/**
	 * Checks if the given widget ID is a prayer book widget.
	 *
	 * @param widgetId
	 * @return
	 */
	private static boolean isPrayerBookWidget(int widgetId)
	{
		return widgetId >= InterfaceID.Prayerbook.PRAYER1 && widgetId <= InterfaceID.Prayerbook.PRAYER30;
	}
}
